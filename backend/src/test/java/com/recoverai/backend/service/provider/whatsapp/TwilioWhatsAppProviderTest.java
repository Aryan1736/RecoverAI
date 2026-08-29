package com.recoverai.backend.service.provider.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.WhatsAppMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TwilioWhatsAppProviderTest {

    private RecoveryCommunicationProperties properties;
    private MockRestServiceServer mockServer;
    private TwilioWhatsAppProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.getWhatsapp().setProvider("twilio");
        properties.getWhatsapp().setAccountSid("AC_test_account_sid");
        properties.getWhatsapp().setAuthToken("test_auth_token");
        properties.getWhatsapp().setSenderNumber("+14155238886");
        properties.getWhatsapp().setApiBaseUrl("https://api.twilio.com");

        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.twilio.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new TwilioWhatsAppProvider(properties, restClient, objectMapper);
    }

    @Test
    @DisplayName("Should successfully send WhatsApp message and return delivery ID")
    void shouldSendWhatsAppSuccessfully() {
        mockServer.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_test_account_sid/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Basic ")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"sid\":\"SM1234567890\",\"status\":\"queued\"}"));

        WhatsAppMessageRequest request = new WhatsAppMessageRequest(
                "+919876543210", "Alice", "Acme Store", new BigDecimal("1200.00"), "INR", "https://pay.recoverai.io/r/123", "INSUFFICIENT_FUNDS"
        );

        CommunicationDeliveryResult result = provider.sendWhatsApp(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).isEqualTo("SM1234567890");
        assertThat(result.getProviderName()).isEqualTo("TWILIO_WHATSAPP");
        assertThat(result.getResultCode()).isEqualTo("WHATSAPP_DISPATCHED");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 429 as RATE_LIMITED and retryable")
    void shouldHandleRateLimit() {
        mockServer.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_test_account_sid/Messages.json"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":20429,\"message\":\"Too Many Requests\"}"));

        WhatsAppMessageRequest request = new WhatsAppMessageRequest(
                "+919876543210", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendWhatsApp(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.RATE_LIMITED);
        assertThat(result.isRetryable()).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 401 as AUTHENTICATION and non-retryable")
    void shouldHandleAuthFailure() {
        mockServer.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_test_account_sid/Messages.json"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":20003,\"message\":\"Authenticate failure\"}"));

        WhatsAppMessageRequest request = new WhatsAppMessageRequest(
                "+919876543210", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendWhatsApp(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.AUTHENTICATION);
        assertThat(result.isRetryable()).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 500 as TRANSIENT and retryable")
    void shouldHandleServerError() {
        mockServer.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_test_account_sid/Messages.json"))
                .andRespond(withServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Internal Server Error\"}"));

        WhatsAppMessageRequest request = new WhatsAppMessageRequest(
                "+919876543210", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendWhatsApp(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.TRANSIENT);
        assertThat(result.isRetryable()).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should reject invalid or null request with VALIDATION failure")
    void shouldRejectNullOrBlankRequest() {
        CommunicationDeliveryResult nullResult = provider.sendWhatsApp(null);
        assertThat(nullResult.isSuccess()).isFalse();
        assertThat(nullResult.getFailureType()).isEqualTo(ProviderFailureType.VALIDATION);

        WhatsAppMessageRequest noPhone = new WhatsAppMessageRequest(
                "", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );
        CommunicationDeliveryResult noPhoneResult = provider.sendWhatsApp(noPhone);
        assertThat(noPhoneResult.isSuccess()).isFalse();
        assertThat(noPhoneResult.getFailureType()).isEqualTo(ProviderFailureType.VALIDATION);
    }

    @Test
    @DisplayName("Should check provider health correctly")
    void shouldCheckHealth() {
        ProviderHealthResult healthy = provider.checkHealth();
        assertThat(healthy.getStatus()).isEqualTo(ProviderHealthStatus.AVAILABLE);

        properties.getWhatsapp().setAccountSid("");
        ProviderHealthResult unhealthy = provider.checkHealth();
        assertThat(unhealthy.getStatus()).isEqualTo(ProviderHealthStatus.MISCONFIGURED);
    }
}
