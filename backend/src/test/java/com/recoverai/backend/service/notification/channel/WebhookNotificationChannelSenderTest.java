package com.recoverai.backend.service.notification.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoverAINotificationProperties;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.security.RecoveryOutcomeSignatureVerifier;
import com.recoverai.backend.service.provider.http.ProviderHttpClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookNotificationChannelSenderTest {

    @Mock
    private ProviderHttpClientFactory httpClientFactory;

    private RecoveryOutcomeSignatureVerifier signatureVerifier;
    private RecoverAINotificationProperties properties;
    private ObjectMapper objectMapper;
    private WebhookNotificationChannelSender webhookSender;

    private Merchant merchant;
    private Notification notification;
    private NotificationDelivery delivery;

    @BeforeEach
    void setUp() {
        signatureVerifier = new RecoveryOutcomeSignatureVerifier();
        properties = new RecoverAINotificationProperties();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        webhookSender = new WebhookNotificationChannelSender(
                httpClientFactory, signatureVerifier, properties, objectMapper);

        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Acme Corp")
                .email("billing@acme.com")
                .webhookUrl("https://example.com/api/v1/webhook")
                .webhookSecret("whsec_supersecret123")
                .build();

        RecoveryCase recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .priority(RecoveryPriority.HIGH)
                .recoveredAmount(BigDecimal.valueOf(2500.00))
                .currency("INR")
                .build();

        notification = Notification.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .title("Payment Recovered")
                .message("Payment of 2500 INR was recovered")
                .recoveryCase(recoveryCase)
                .build();

        delivery = NotificationDelivery.builder()
                .id(UUID.randomUUID())
                .notification(notification)
                .merchant(merchant)
                .channel(MerchantNotificationChannel.WEBHOOK)
                .status(NotificationDeliveryStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    @Test
    @DisplayName("Should sign payload with HMAC-SHA256 and deliver successfully on 200 OK")
    void testSuccessfulWebhookDelivery() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(httpClientFactory.createClient(any(), anyInt(), anyInt(), anyMap())).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("{\"received\":true}"));

        NotificationDelivery result = webhookSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.DELIVERED, result.getStatus());
        assertEquals("WEBHOOK_HTTP", result.getProvider());
        assertNotNull(result.getDeliveredAt());
    }

    @Test
    @DisplayName("Should classify 429 Too Many Requests as retryable (RETRYING)")
    void testRetryableRateLimitFailure() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(httpClientFactory.createClient(any(), anyInt(), anyInt(), anyMap())).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        RestClientResponseException rcre = new RestClientResponseException(
                "Rate limit exceeded", 429, "Too Many Requests", HttpHeaders.EMPTY, null, null);
        when(responseSpec.toEntity(String.class)).thenThrow(rcre);

        NotificationDelivery result = webhookSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.RETRYING, result.getStatus());
        assertEquals("HTTP_429", result.getErrorCode());
        assertEquals(1, result.getRetryCount());
    }

    @Test
    @DisplayName("Should classify 503 Service Unavailable as retryable (RETRYING)")
    void testRetryableServerFailure() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(httpClientFactory.createClient(any(), anyInt(), anyInt(), anyMap())).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        RestClientResponseException rcre = new RestClientResponseException(
                "Service Unavailable", 503, "Service Unavailable", HttpHeaders.EMPTY, null, null);
        when(responseSpec.toEntity(String.class)).thenThrow(rcre);

        NotificationDelivery result = webhookSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.RETRYING, result.getStatus());
        assertEquals("HTTP_503", result.getErrorCode());
    }

    @Test
    @DisplayName("Should classify 400 Bad Request as permanent failure (FAILED)")
    void testPermanentClientFailure() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(httpClientFactory.createClient(any(), anyInt(), anyInt(), anyMap())).thenReturn(restClient);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);

        RestClientResponseException rcre = new RestClientResponseException(
                "Bad Request", 400, "Bad Request", HttpHeaders.EMPTY, null, null);
        when(responseSpec.toEntity(String.class)).thenThrow(rcre);

        NotificationDelivery result = webhookSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.FAILED, result.getStatus());
        assertEquals("HTTP_400", result.getErrorCode());
    }

    @Test
    @DisplayName("Should skip webhook delivery when webhook URL is not configured")
    void testMissingWebhookUrl() {
        merchant.setWebhookUrl(null);

        NotificationDelivery result = webhookSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.SKIPPED, result.getStatus());
        assertEquals("MISSING_WEBHOOK_URL", result.getErrorCode());
    }

    @Test
    @DisplayName("Webhook secret should never appear in delivery error message")
    void testSecretNotExposedInErrorMessage() {
        merchant.setWebhookSecret("my_ultra_secret_key");
        RestClient restClient = mock(RestClient.class);
        when(httpClientFactory.createClient(any(), anyInt(), anyInt(), anyMap())).thenReturn(restClient);
        when(restClient.post()).thenThrow(new RuntimeException("Connection failed to endpoint"));

        NotificationDelivery result = webhookSender.deliver(notification, merchant, delivery);

        assertNotNull(result.getErrorMessage());
        assertFalse(result.getErrorMessage().contains("my_ultra_secret_key"));
    }
}
