package com.recoverai.backend.service.notification.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoverAINotificationProperties;
import com.recoverai.backend.dto.notification.MerchantWebhookPayloadDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.security.RecoveryOutcomeSignatureVerifier;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.http.ProviderHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
public class WebhookNotificationChannelSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationChannelSender.class);
    private static final String SIGNATURE_HEADER = "X-RecoverAI-Signature";
    private static final String EVENT_HEADER = "X-RecoverAI-Event";
    private static final String DELIVERY_ID_HEADER = "X-RecoverAI-Delivery-Id";

    private final ProviderHttpClientFactory httpClientFactory;
    private final RecoveryOutcomeSignatureVerifier signatureVerifier;
    private final RecoverAINotificationProperties notificationProperties;
    private final ObjectMapper objectMapper;

    public WebhookNotificationChannelSender(ProviderHttpClientFactory httpClientFactory,
                                            RecoveryOutcomeSignatureVerifier signatureVerifier,
                                            RecoverAINotificationProperties notificationProperties,
                                            ObjectMapper objectMapper) {
        this.httpClientFactory = httpClientFactory;
        this.signatureVerifier = signatureVerifier;
        this.notificationProperties = notificationProperties;
        ObjectMapper mapper = objectMapper != null ? objectMapper.copy() : new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper = mapper;
    }

    @Override
    public MerchantNotificationChannel getChannel() {
        return MerchantNotificationChannel.WEBHOOK;
    }

    @Override
    public NotificationDelivery deliver(Notification notification, Merchant merchant, NotificationDelivery delivery) {
        Instant now = Instant.now();
        delivery.setAttemptedAt(now);
        delivery.setRetryCount(delivery.getRetryCount() + 1);
        delivery.setProvider("WEBHOOK_HTTP");

        String webhookUrl = merchant != null ? merchant.getWebhookUrl() : null;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.info("Merchant {} has no webhook URL configured; skipping webhook notification id={}",
                    merchant != null ? merchant.getId() : "null", notification.getId());
            delivery.setStatus(NotificationDeliveryStatus.SKIPPED);
            delivery.setErrorCode("MISSING_WEBHOOK_URL");
            delivery.setErrorMessage("Merchant webhook URL is not configured");
            return delivery;
        }

        String webhookSecret = merchant.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Merchant {} has no webhook secret configured; failing webhook delivery id={}",
                    merchant.getId(), notification.getId());
            delivery.setStatus(NotificationDeliveryStatus.FAILED);
            delivery.setErrorCode("MISSING_WEBHOOK_SECRET");
            delivery.setErrorMessage("Merchant webhook signing secret is not configured");
            return delivery;
        }

        RecoveryCase rCase = notification.getRecoveryCase();
        BigDecimal amount = rCase != null && rCase.getRecoveredAmount() != null && rCase.getRecoveredAmount().compareTo(BigDecimal.ZERO) > 0
                ? rCase.getRecoveredAmount()
                : (rCase != null && rCase.getEstimatedRecoverableAmount() != null ? rCase.getEstimatedRecoverableAmount() : null);
        String currency = rCase != null ? rCase.getCurrency() : null;
        UUID caseId = rCase != null ? rCase.getId() : null;
        UUID attemptId = notification.getRecoveryAttempt() != null ? notification.getRecoveryAttempt().getId() : null;

        MerchantWebhookPayloadDto payloadDto = new MerchantWebhookPayloadDto(
                notification.getEventType().name(),
                notification.getId(),
                merchant.getId(),
                caseId,
                attemptId,
                amount,
                currency,
                notification.getTitle(),
                notification.getMessage(),
                notification.getCreatedAt() != null ? notification.getCreatedAt() : now
        );

        String rawPayload;
        String signature;
        try {
            rawPayload = objectMapper.writeValueAsString(payloadDto);
            signature = signatureVerifier.calculateHmacSha256(rawPayload, webhookSecret);
        } catch (Exception ex) {
            log.error("Failed to serialize or sign webhook payload for notification id={}: {}",
                    notification.getId(), ex.getMessage());
            delivery.setStatus(NotificationDeliveryStatus.FAILED);
            delivery.setErrorCode("PAYLOAD_SIGNING_ERROR");
            delivery.setErrorMessage("Failed to serialize or sign webhook payload");
            return delivery;
        }

        int connectTimeout = notificationProperties != null ? notificationProperties.getWebhookConnectTimeoutMs() : 5000;
        int readTimeout = notificationProperties != null ? notificationProperties.getWebhookReadTimeoutMs() : 10000;

        String deliveryIdStr = delivery.getId() != null ? delivery.getId().toString() : UUID.randomUUID().toString();
        delivery.setProviderMessageId(deliveryIdStr);

        try {
            RestClient restClient = httpClientFactory.createClient(null, connectTimeout, readTimeout, Collections.emptyMap());
            ResponseEntity<String> response = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(SIGNATURE_HEADER, signature)
                    .header(EVENT_HEADER, notification.getEventType().name())
                    .header(DELIVERY_ID_HEADER, deliveryIdStr)
                    .header("User-Agent", "RecoverAI-Webhook/1.0")
                    .body(rawPayload)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            if (statusCode >= 200 && statusCode < 300) {
                delivery.setStatus(NotificationDeliveryStatus.DELIVERED);
                delivery.setDeliveredAt(Instant.now());
                delivery.setErrorCode(null);
                delivery.setErrorMessage(null);
                log.info("Webhook notification id={} successfully dispatched to merchant {} (status={})",
                        notification.getId(), merchant.getId(), statusCode);
            } else {
                handleHttpFailure(delivery, statusCode, "Unexpected HTTP status: " + statusCode);
            }
        } catch (RestClientResponseException rcre) {
            int statusCode = rcre.getStatusCode().value();
            handleHttpFailure(delivery, statusCode, "Webhook endpoint responded with HTTP " + statusCode);
        } catch (Exception ex) {
            log.error("Network or timeout error dispatching webhook for notification id={} to {}: {}",
                    notification.getId(), webhookUrl, ex.getMessage());
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            boolean retryable = failureType.isRetryable() && delivery.getRetryCount() < delivery.getMaxRetries();

            delivery.setErrorCode(failureType.name());
            delivery.setErrorMessage("Webhook delivery error: " + ex.getMessage());
            delivery.setStatus(retryable ? NotificationDeliveryStatus.RETRYING : NotificationDeliveryStatus.FAILED);
        }

        return delivery;
    }

    private void handleHttpFailure(NotificationDelivery delivery, int statusCode, String message) {
        ProviderFailureType failureType = ProviderErrorClassifier.classifyHttpStatus(statusCode);
        boolean retryable = (statusCode == 429 || statusCode >= 500) && delivery.getRetryCount() < delivery.getMaxRetries();

        delivery.setErrorCode("HTTP_" + statusCode);
        delivery.setErrorMessage(message + " (" + failureType + ")");
        delivery.setStatus(retryable ? NotificationDeliveryStatus.RETRYING : NotificationDeliveryStatus.FAILED);
        log.warn("Webhook delivery failed: statusCode={}, failureType={}, retryable={}",
                statusCode, failureType, retryable);
    }
}
