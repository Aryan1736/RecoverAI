package com.recoverai.backend.service;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Notification;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.NotificationDeliveryRepository;
import com.recoverai.backend.repository.NotificationRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.service.notification.ProviderHealthAlertService;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class NotificationLifecycleIntegrationTest {

    @Autowired
    private PaymentReconciliationService reconciliationService;

    @Autowired
    private RecoveryExecutionQueueService queueService;

    @Autowired
    private ProviderHealthAlertService providerHealthAlertService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private com.recoverai.backend.repository.AuditEventRepository auditEventRepository;

    @MockitoBean
    private ProviderHealthService mockProviderHealthService;

    @MockitoBean
    private com.recoverai.backend.service.provider.WhatsAppProvider mockWhatsAppProvider;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        deliveryRepository.deleteAll();
        notificationRepository.deleteAll();
        queueRepository.deleteAll();
        recoveryAttemptRepository.deleteAll();
        recoveryCaseRepository.deleteAll();
        paymentRepository.deleteAll();
        customerRepository.deleteAll();
        merchantRepository.deleteAll();

        merchant = merchantRepository.save(Merchant.builder()
                .name("Lifecycle Merchant")
                .email("lifecycle-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .webhookUrl("https://example.com/webhook")
                .build());

        providerHealthAlertService.clearAllCooldowns();
    }

    @Test
    @DisplayName("Payment reconciliation should trigger PAYMENT_RECOVERED notification idempotently")
    void testPaymentRecoveredNotificationTriggerAndDeduplication() {
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_failed_" + UUID.randomUUID())
                .amount(BigDecimal.valueOf(5000.00))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .payment(failedPayment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.MEDIUM)
                .estimatedRecoverableAmount(BigDecimal.valueOf(5000.00))
                .currency("INR")
                .build());

        Payment capturedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .razorpayPaymentId("pay_captured_" + UUID.randomUUID())
                .razorpayOrderId("order_test_123")
                .amount(BigDecimal.valueOf(5000.00))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .build());

        // 1. Initial reconciliation: case transitions to RECOVERED
        RecoveryCase result = reconciliationService.reconcileCaseRecovery(
                merchant,
                recoveryCase,
                capturedPayment,
                BigDecimal.valueOf(5000.00),
                "test-event",
                "127.0.0.1",
                null
        );

        assertNotNull(result);
        assertEquals(RecoveryCaseStatus.RECOVERED, result.getStatus());

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        Notification notification = notifications.get(0);
        assertEquals(MerchantNotificationEvent.PAYMENT_RECOVERED, notification.getEventType());
        assertEquals(merchant.getId(), notification.getMerchant().getId());

        // 2. Duplicate reconciliation invocation (idempotency check)
        reconciliationService.reconcileCaseRecovery(
                merchant,
                result,
                capturedPayment,
                BigDecimal.valueOf(5000.00),
                "test-event-dup",
                "127.0.0.1",
                null
        );

        // Verification: no duplicate notification was created
        List<Notification> notificationsAfterDup = notificationRepository.findAll();
        assertEquals(1, notificationsAfterDup.size());
    }

    @Test
    @DisplayName("Exhausted fallback chain should trigger CASE_EXHAUSTED notification")
    void testCaseExhaustedNotificationTrigger() {
        Customer customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("Exhausted Customer")
                .email("exhaust@example.com")
                .phone("+919999999999")
                .build());

        Payment payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_exhaust_" + UUID.randomUUID())
                .amount(BigDecimal.valueOf(2000.00))
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.LOW)
                .estimatedRecoverableAmount(BigDecimal.valueOf(2000.00))
                .build());

        // Create 3 attempts to reach max attempts threshold (maxAttempts default is 3)
        for (int i = 1; i <= 2; i++) {
            recoveryAttemptRepository.save(RecoveryAttempt.builder()
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .attemptNumber(i)
                    .channel(RecoveryChannel.WHATSAPP)
                    .status(RecoveryAttemptStatus.FAILED)
                    .build());
        }

        RecoveryAttempt attempt3 = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(3)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build());

        RecoveryExecutionQueueItem queueItem = queueRepository.save(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recoveryAttempt(attempt3)
                .status(RecoveryQueueStatus.PROCESSING)
                .availableAt(Instant.now())
                .retryCount(3)
                .maxRetries(3) // retry exhaustion triggers dead letter and fallback evaluation
                .build());

        when(mockWhatsAppProvider.sendWhatsApp(any()))
                .thenReturn(com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult.failure(
                        "fail-exhaust", "WHATSAPP", "SERVICE_UNAVAILABLE", "Outage", "{}",
                        com.recoverai.backend.service.provider.classification.ProviderFailureType.TRANSIENT
                ));

        // Process failure that dead-letters and exhausts fallback
        queueService.processQueueItem(queueItem.getId());

        List<Notification> notifications = notificationRepository.findAll();
        boolean hasExhaustedNotif = notifications.stream()
                .anyMatch(n -> n.getEventType() == MerchantNotificationEvent.CASE_EXHAUSTED);
        assertTrue(hasExhaustedNotif, "Expected CASE_EXHAUSTED notification to be triggered");
    }

    @Test
    @DisplayName("High priority case failure should trigger HIGH_PRIORITY_FAILURE notification")
    void testHighPriorityFailureNotificationTrigger() {
        Customer customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("VIP Customer")
                .email("vip@example.com")
                .phone("+919876543210")
                .build());

        Payment payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_vip_" + UUID.randomUUID())
                .amount(BigDecimal.valueOf(100000.00))
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase highPriorityCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.CRITICAL)
                .estimatedRecoverableAmount(BigDecimal.valueOf(100000.00))
                .build());

        RecoveryAttempt attempt = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(highPriorityCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build());

        RecoveryExecutionQueueItem queueItem = queueRepository.save(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryCase(highPriorityCase)
                .recoveryAttempt(attempt)
                .status(RecoveryQueueStatus.PROCESSING)
                .availableAt(Instant.now())
                .retryCount(3)
                .maxRetries(3) // retry exhaustion triggers dead letter
                .build());

        when(mockWhatsAppProvider.sendWhatsApp(any()))
                .thenReturn(com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult.failure(
                        "fail-1", "WHATSAPP", "SERVICE_UNAVAILABLE", "Outage", "{}",
                        com.recoverai.backend.service.provider.classification.ProviderFailureType.TRANSIENT
                ));

        queueService.processQueueItem(queueItem.getId());

        List<Notification> notifications = notificationRepository.findAll();
        boolean hasHighPriorityNotif = notifications.stream()
                .anyMatch(n -> n.getEventType() == MerchantNotificationEvent.HIGH_PRIORITY_FAILURE);
        assertTrue(hasHighPriorityNotif, "Expected HIGH_PRIORITY_FAILURE notification to be triggered");
    }

    @Test
    @DisplayName("Degraded provider should trigger PROVIDER_DEGRADED notification and respect cooldown deduplication")
    void testProviderDegradedNotificationAndCooldown() {
        ProviderHealthResult degradedResult = ProviderHealthResult.degraded(
                "SENDGRID", "COMMUNICATION", "SMTP connection timeout"
        );
        when(mockProviderHealthService.checkAll()).thenReturn(List.of(degradedResult));

        // First check: should alert
        int alertedCount1 = providerHealthAlertService.checkAndAlertDegradedProviders();
        assertEquals(1, alertedCount1);

        List<Notification> notifs1 = notificationRepository.findAll();
        assertEquals(1, notifs1.size());
        assertEquals(MerchantNotificationEvent.PROVIDER_DEGRADED, notifs1.get(0).getEventType());

        // Second check immediately: should be suppressed by cooldown!
        int alertedCount2 = providerHealthAlertService.checkAndAlertDegradedProviders();
        assertEquals(0, alertedCount2);

        List<Notification> notifs2 = notificationRepository.findAll();
        assertEquals(1, notifs2.size(), "No additional notification should be created within cooldown");
    }
}
