package com.recoverai.backend.service;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.NotificationDelivery;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.NotificationDeliveryRepository;
import com.recoverai.backend.repository.NotificationRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.service.notification.MerchantNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class NotificationReliabilityIntegrationTest {

    @Autowired
    private PaymentReconciliationService reconciliationService;

    @MockitoSpyBean
    private MerchantNotificationService notificationService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        deliveryRepository.deleteAll();
        notificationRepository.deleteAll();
        recoveryCaseRepository.deleteAll();
        paymentRepository.deleteAll();
        merchantRepository.deleteAll();

        merchant = merchantRepository.save(Merchant.builder()
                .name("Reliability Merchant")
                .email("rel-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .webhookUrl("https://example.com/webhook")
                .build());
    }

    @Test
    @DisplayName("Notification failure during payment reconciliation must not roll back recovery")
    void testNotificationFailureDoesNotRollbackPaymentRecovery() {
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_fail_" + UUID.randomUUID())
                .amount(BigDecimal.valueOf(3000.00))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .payment(failedPayment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(BigDecimal.valueOf(3000.00))
                .currency("INR")
                .build());

        Payment capturedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_cap_" + UUID.randomUUID())
                .amount(BigDecimal.valueOf(3000.00))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .build());

        // Make notification service throw catastrophic error during reconciliation dispatch
        doThrow(new RuntimeException("Simulated catastrophic failure in notification dispatch"))
                .when(notificationService).notifyPaymentRecovered(any(), any(), any());

        // Execute reconciliation: must complete successfully without throwing
        RecoveryCase reconciled = reconciliationService.reconcileCaseRecovery(
                merchant,
                recoveryCase,
                capturedPayment,
                BigDecimal.valueOf(3000.00),
                "test-source",
                "127.0.0.1",
                null
        );

        // Assert payment and recovery case are in terminal RECOVERED state
        assertNotNull(reconciled);
        assertEquals(RecoveryCaseStatus.RECOVERED, reconciled.getStatus());

        RecoveryCase dbCase = recoveryCaseRepository.findById(recoveryCase.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.RECOVERED, dbCase.getStatus());
    }

    @Test
    @DisplayName("Permanent delivery failure does not retry indefinitely and stops at max retries")
    void testBoundedDeliveryRetries() {
        Notification notification = Notification.builder()
                .merchant(merchant)
                .eventType(MerchantNotificationEvent.CASE_EXHAUSTED)
                .title("Exhausted")
                .message("Test message")
                .build();
        notification = notificationRepository.save(notification);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notification(notification)
                .merchant(merchant)
                .channel(MerchantNotificationChannel.WEBHOOK)
                .status(NotificationDeliveryStatus.RETRYING)
                .retryCount(3)
                .maxRetries(3) // retryCount == maxRetries
                .attemptedAt(Instant.now().minusSeconds(600))
                .build();
        deliveryRepository.save(delivery);

        // Run retry processor
        int retriedCount = notificationService.retryPendingDeliveries();

        // Since retryCount >= maxRetries, retry count should not be retried further
        assertEquals(0, retriedCount);
    }
}
