package com.recoverai.backend.service.provider.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class SendGridEmailProviderTest {

    private RecoveryCommunicationProperties properties;
    private MockRestServiceServer mockServer;
    private SendGridEmailProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.getEmail().setProvider("sendgrid");
        properties.getEmail().setApiKey("SG.test_key_12345");
        properties.getEmail().setFromAddress("recover@recoverai.io");
        properties.getEmail().setFromName("RecoverAI");
        properties.getEmail().setApiBaseUrl("https://api.sendgrid.com");

        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.sendgrid.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new SendGridEmailProvider(properties, restClient, objectMapper);
    }

    @Test
    @DisplayName("Should successfully send email via SendGrid API with 202 Accepted")
    void shouldSendEmailSuccessfully() {
        HttpHeaders respHeaders = new HttpHeaders();
        respHeaders.add("X-Message-Id", "sg_msg_id_98765");

        mockServer.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer SG.test_key_12345"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .headers(respHeaders)
                        .body(""));

        EmailMessageRequest request = new EmailMessageRequest(
                "customer@example.com", "John Doe", "Acme Corp", new BigDecimal("250.00"), "USD", "https://pay.recoverai.io/r/link1", "CARD_DECLINED"
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).isEqualTo("sg_msg_id_98765");
        assertThat(result.getProviderName()).isEqualTo("SENDGRID_EMAIL");
        assertThat(result.getResultCode()).isEqualTo("EMAIL_DISPATCHED");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 401 as AUTHENTICATION failure")
    void shouldHandleSendGridAuthFailure() {
        mockServer.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errors\":[{\"message\":\"The provided authorization grant is invalid, expired, or revoked\"}]}"));

        EmailMessageRequest request = new EmailMessageRequest(
                "customer@example.com", "John", "Acme", new BigDecimal("100.00"), "USD", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.AUTHENTICATION);
        assertThat(result.isRetryable()).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 429 as RATE_LIMITED and retryable")
    void shouldHandleSendGridRateLimit() {
        mockServer.expect(requestTo("https://api.sendgrid.com/v3/mail/send"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errors\":[{\"message\":\"Too many requests\"}]}"));

        EmailMessageRequest request = new EmailMessageRequest(
                "customer@example.com", "John", "Acme", new BigDecimal("100.00"), "USD", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.RATE_LIMITED);
        assertThat(result.isRetryable()).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should validate health status")
    void shouldCheckHealth() {
        ProviderHealthResult healthy = provider.checkHealth();
        assertThat(healthy.getStatus()).isEqualTo(ProviderHealthStatus.AVAILABLE);

        properties.getEmail().setApiKey("");
        ProviderHealthResult unhealthy = provider.checkHealth();
        assertThat(unhealthy.getStatus()).isEqualTo(ProviderHealthStatus.MISCONFIGURED);
    }
}
