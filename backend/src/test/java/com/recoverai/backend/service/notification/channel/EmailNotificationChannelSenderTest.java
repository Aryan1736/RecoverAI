package com.recoverai.backend.service.notification.channel;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelSenderTest {

    @Mock
    private EmailProvider emailProvider;

    private EmailNotificationChannelSender emailSender;

    private Merchant merchant;
    private Notification notification;
    private NotificationDelivery delivery;

    @BeforeEach
    void setUp() {
        emailSender = new EmailNotificationChannelSender(emailProvider);

        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Acme Corp")
                .email("billing@acme.com")
                .build();

        RecoveryCase recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .priority(RecoveryPriority.HIGH)
                .recoveredAmount(BigDecimal.valueOf(1500.00))
                .currency("INR")
                .build();

        notification = Notification.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.PAYMENT_RECOVERED)
                .title("Payment Recovered")
                .message("Recovered 1500 INR")
                .recoveryCase(recoveryCase)
                .build();

        delivery = NotificationDelivery.builder()
                .id(UUID.randomUUID())
                .notification(notification)
                .merchant(merchant)
                .channel(MerchantNotificationChannel.EMAIL)
                .status(NotificationDeliveryStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    @Test
    @DisplayName("Should successfully dispatch email alert and mark delivery DELIVERED")
    void testSuccessfulEmailDispatch() {
        CommunicationDeliveryResult successResult = CommunicationDeliveryResult.success(
                "msg-12345", "SENDGRID", "SUCCESS", "Email delivered", "{}"
        );
        when(emailProvider.sendEmail(any(EmailMessageRequest.class))).thenReturn(successResult);

        NotificationDelivery result = emailSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.DELIVERED, result.getStatus());
        assertEquals("SENDGRID", result.getProvider());
        assertEquals("msg-12345", result.getProviderMessageId());
        assertNotNull(result.getDeliveredAt());
        assertNull(result.getErrorCode());

        ArgumentCaptor<EmailMessageRequest> captor = ArgumentCaptor.forClass(EmailMessageRequest.class);
        verify(emailProvider).sendEmail(captor.capture());
        EmailMessageRequest sentRequest = captor.getValue();
        assertEquals("billing@acme.com", sentRequest.getRecipientEmail());
        assertEquals("Acme Corp", sentRequest.getMerchantName());
        assertEquals(BigDecimal.valueOf(1500.00), sentRequest.getAmount());
    }

    @Test
    @DisplayName("Should classify retryable provider failure and mark delivery RETRYING")
    void testRetryableEmailFailure() {
        CommunicationDeliveryResult retryableResult = CommunicationDeliveryResult.failure(
                "msg-fail-1", "SENDGRID", "RATE_LIMIT_EXCEEDED", "Too many requests", "{}",
                ProviderFailureType.RATE_LIMITED
        );
        when(emailProvider.sendEmail(any(EmailMessageRequest.class))).thenReturn(retryableResult);

        NotificationDelivery result = emailSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.RETRYING, result.getStatus());
        assertEquals("RATE_LIMIT_EXCEEDED", result.getErrorCode());
        assertEquals(1, result.getRetryCount());
    }

    @Test
    @DisplayName("Should classify permanent provider failure and mark delivery FAILED")
    void testPermanentEmailFailure() {
        CommunicationDeliveryResult permanentResult = CommunicationDeliveryResult.failure(
                "msg-fail-2", "SMTP", "AUTHENTICATION_FAILED", "Invalid credentials", "{}",
                ProviderFailureType.AUTHENTICATION
        );
        when(emailProvider.sendEmail(any(EmailMessageRequest.class))).thenReturn(permanentResult);

        NotificationDelivery result = emailSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.FAILED, result.getStatus());
        assertEquals("AUTHENTICATION_FAILED", result.getErrorCode());
    }

    @Test
    @DisplayName("Should mark delivery FAILED when merchant email is missing")
    void testMissingMerchantEmail() {
        merchant.setEmail(null);

        NotificationDelivery result = emailSender.deliver(notification, merchant, delivery);

        assertEquals(NotificationDeliveryStatus.FAILED, result.getStatus());
        assertEquals("MISSING_MERCHANT_EMAIL", result.getErrorCode());
    }
}
