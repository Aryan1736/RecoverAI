package com.recoverai.backend.service.provider.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TwilioSmsProviderTest {

    private RecoveryCommunicationProperties properties;
    private MockRestServiceServer mockServer;
    private TwilioSmsProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.getSms().setProvider("twilio");
        properties.getSms().setAccountSid("AC_test_sms_sid");
        properties.getSms().setAuthToken("test_sms_auth_token");
        properties.getSms().setSenderId("RECOVER");
        properties.getSms().setApiBaseUrl("https://api.twilio.com");

        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.twilio.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new TwilioSmsProvider(properties, restClient, objectMapper);
    }

    @Test
    @DisplayName("Should successfully send SMS via Twilio API")
    void shouldSendSmsSuccessfully() {
        mockServer.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_test_sms_sid/Messages.json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Basic ")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"sid\":\"SM_sms_123456\",\"status\":\"queued\"}"));

        SmsMessageRequest request = new SmsMessageRequest(
                "+919876543210", "Carol", "Acme Store", new BigDecimal("750.00"), "INR", "https://pay.recoverai.io/r/sms1"
        );

        CommunicationDeliveryResult result = provider.sendSms(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).isEqualTo("SM_sms_123456");
        assertThat(result.getProviderName()).isEqualTo("TWILIO_SMS");
        assertThat(result.getResultCode()).isEqualTo("SMS_DISPATCHED");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify 401 as AUTHENTICATION failure")
    void shouldHandleAuthFailure() {
        mockServer.expect(requestTo("https://api.twilio.com/2010-04-01/Accounts/AC_test_sms_sid/Messages.json"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":20003,\"message\":\"Authentication Error\"}"));

        SmsMessageRequest request = new SmsMessageRequest(
                "+919876543210", "Carol", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1"
        );

        CommunicationDeliveryResult result = provider.sendSms(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.AUTHENTICATION);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should validate health status")
    void shouldCheckHealth() {
        ProviderHealthResult healthy = provider.checkHealth();
        assertThat(healthy.getStatus()).isEqualTo(ProviderHealthStatus.AVAILABLE);

        properties.getSms().setAccountSid("");
        ProviderHealthResult unhealthy = provider.checkHealth();
        assertThat(unhealthy.getStatus()).isEqualTo(ProviderHealthStatus.MISCONFIGURED);
    }
}
