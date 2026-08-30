package com.recoverai.backend.service.notification.channel;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class EmailNotificationChannelSender implements NotificationChannelSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannelSender.class);

    private final EmailProvider emailProvider;

    public EmailNotificationChannelSender(EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    @Override
    public MerchantNotificationChannel getChannel() {
        return MerchantNotificationChannel.EMAIL;
    }

    @Override
    public NotificationDelivery deliver(Notification notification, Merchant merchant, NotificationDelivery delivery) {
        Instant now = Instant.now();
        delivery.setAttemptedAt(now);
        delivery.setRetryCount(delivery.getRetryCount() + 1);

        String recipientEmail = merchant != null ? merchant.getEmail() : null;
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Merchant {} has no email address configured, skipping email notification id={}",
                    merchant != null ? merchant.getId() : "null", notification.getId());
            delivery.setStatus(NotificationDeliveryStatus.FAILED);
            delivery.setErrorCode("MISSING_MERCHANT_EMAIL");
            delivery.setErrorMessage("Merchant email is missing or empty");
            delivery.setProvider("EMAIL");
            return delivery;
        }

        RecoveryCase rCase = notification.getRecoveryCase();
        BigDecimal amount = rCase != null && rCase.getRecoveredAmount() != null && rCase.getRecoveredAmount().compareTo(BigDecimal.ZERO) > 0
                ? rCase.getRecoveredAmount()
                : (rCase != null && rCase.getEstimatedRecoverableAmount() != null ? rCase.getEstimatedRecoverableAmount() : BigDecimal.ZERO);
        String currency = rCase != null && rCase.getCurrency() != null ? rCase.getCurrency() : "INR";
        String customerName = rCase != null && rCase.getCustomer() != null && rCase.getCustomer().getName() != null
                ? rCase.getCustomer().getName()
                : merchant.getName();

        EmailMessageRequest request = new EmailMessageRequest(
                recipientEmail,
                customerName,
                merchant.getName(),
                amount,
                currency,
                rCase != null ? "/cases/" + rCase.getId() : "/notifications",
                notification.getMessage()
        );

        try {
            CommunicationDeliveryResult result = emailProvider.sendEmail(request);
            delivery.setProvider(result.getProviderName() != null ? result.getProviderName() : "EMAIL");

            if (result.isSuccess()) {
                delivery.setStatus(NotificationDeliveryStatus.DELIVERED);
                delivery.setDeliveredAt(result.getTimestamp() != null ? result.getTimestamp() : Instant.now());
                delivery.setProviderMessageId(result.getDeliveryId());
                delivery.setErrorCode(null);
                delivery.setErrorMessage(null);
                log.info("Email notification id={} successfully delivered to merchant {} via {}",
                        notification.getId(), merchant.getId(), delivery.getProvider());
            } else {
                delivery.setErrorCode(result.getResultCode());
                delivery.setErrorMessage(result.getResultMessage());
                delivery.setProviderMessageId(result.getDeliveryId());

                boolean retryable = result.isRetryable() && delivery.getRetryCount() < delivery.getMaxRetries();
                delivery.setStatus(retryable ? NotificationDeliveryStatus.RETRYING : NotificationDeliveryStatus.FAILED);
                log.warn("Email notification id={} failed for merchant {}: status={}, code={}, retryable={}",
                        notification.getId(), merchant.getId(), delivery.getStatus(), result.getResultCode(), retryable);
            }
        } catch (Exception ex) {
            log.error("Exception during email notification id={} for merchant {}: {}",
                    notification.getId(), merchant.getId(), ex.getMessage(), ex);
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            boolean retryable = failureType.isRetryable() && delivery.getRetryCount() < delivery.getMaxRetries();

            delivery.setProvider("EMAIL");
            delivery.setErrorCode(failureType.name());
            delivery.setErrorMessage(ex.getMessage());
            delivery.setStatus(retryable ? NotificationDeliveryStatus.RETRYING : NotificationDeliveryStatus.FAILED);
        }

        return delivery;
    }
}
