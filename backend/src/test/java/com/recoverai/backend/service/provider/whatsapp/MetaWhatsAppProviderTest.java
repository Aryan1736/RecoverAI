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
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MetaWhatsAppProviderTest {

    private RecoveryCommunicationProperties properties;
    private MockRestServiceServer mockServer;
    private MetaWhatsAppProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.getWhatsapp().setProvider("meta");
        properties.getWhatsapp().setApiKey("EAAG_test_meta_token");
        properties.getWhatsapp().setSenderNumber("10987654321");
        properties.getWhatsapp().setApiBaseUrl("https://graph.facebook.com");

        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://graph.facebook.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new MetaWhatsAppProvider(properties, restClient, objectMapper);
    }

    @Test
    @DisplayName("Should successfully send WhatsApp message via Meta Cloud API")
    void shouldSendViaMetaSuccessfully() {
        mockServer.expect(requestTo("https://graph.facebook.com/v18.0/10987654321/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer EAAG_test_meta_token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.HBgLMTIzNDU2\"}]}", MediaType.APPLICATION_JSON));

        WhatsAppMessageRequest request = new WhatsAppMessageRequest(
                "+919876543210", "Bob", "Acme", new BigDecimal("500.00"), "INR", "https://pay.recoverai.io/r/456", null
        );

        CommunicationDeliveryResult result = provider.sendWhatsApp(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).isEqualTo("wamid.HBgLMTIzNDU2");
        assertThat(result.getProviderName()).isEqualTo("META_WHATSAPP");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 400 bad request from Meta as VALIDATION failure")
    void shouldHandleMetaBadRequest() {
        mockServer.expect(requestTo("https://graph.facebook.com/v18.0/10987654321/messages"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Invalid phone number format\",\"type\":\"OAuthException\",\"code\":100}}"));

        WhatsAppMessageRequest request = new WhatsAppMessageRequest(
                "+919876543210", "Bob", "Acme", new BigDecimal("500.00"), "INR", "https://pay.recoverai.io/r/456", null
        );

        CommunicationDeliveryResult result = provider.sendWhatsApp(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.VALIDATION);
        assertThat(result.isRetryable()).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should check Meta health status correctly")
    void shouldCheckHealth() {
        ProviderHealthResult healthy = provider.checkHealth();
        assertThat(healthy.getStatus()).isEqualTo(ProviderHealthStatus.AVAILABLE);

        properties.getWhatsapp().setApiKey("");
        ProviderHealthResult unhealthy = provider.checkHealth();
        assertThat(unhealthy.getStatus()).isEqualTo(ProviderHealthStatus.MISCONFIGURED);
    }
}
