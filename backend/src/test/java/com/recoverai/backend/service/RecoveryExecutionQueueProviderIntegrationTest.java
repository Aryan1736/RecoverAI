package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.config.RecoveryQueueProperties;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.service.executor.ExecutionResult;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.repository.AuditEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RecoveryExecutionQueueProviderIntegrationTest {

    @Autowired
    private RecoveryExecutionQueueService queueService;

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private RecoveryAttemptRepository attemptRepository;

    @Autowired
    private RecoveryCaseRepository caseRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private RecoveryCommunicationProperties commProperties;

    @Autowired
    private RecoveryQueueProperties queueProperties;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;

    @BeforeEach
    void setUp() {
        cleanDb();

        merchant = merchantRepository.save(Merchant.builder()
                .name("Integration Merchant " + UUID.randomUUID())
                .email("merch_" + UUID.randomUUID() + "@test.com")
                .webhookSecret("secret_" + UUID.randomUUID())
                .build());

        customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("Customer " + UUID.randomUUID())
                .email("cust_" + UUID.randomUUID() + "@test.com")
                .phone("+919876543210")
                .build());

        payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .amount(new BigDecimal("999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .razorpayPaymentId("pay_int_" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        recoveryCase = caseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .currency("INR")
                .estimatedRecoverableAmount(new BigDecimal("999.00"))
                .failureReasonCategory("INSUFFICIENT_FUNDS")
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        auditEventRepository.deleteAll();
        queueRepository.deleteAll();
        attemptRepository.deleteAll();
        caseRepository.deleteAll();
        paymentRepository.deleteAll();
        customerRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    @DisplayName("Should execute mock provider end-to-end via queue with SENT attempt and COMPLETED queue item")
    void shouldExecuteMockProviderEndToEnd() {
        RecoveryAttempt attempt = attemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        RecoveryExecutionQueueItem queueItem = queueService.enqueueAttempt(attempt, Instant.now().minusSeconds(10));
        assertThat(queueItem.getStatus()).isEqualTo(RecoveryQueueStatus.READY);

        boolean processed = queueService.processQueueItem(queueItem.getId());
        assertThat(processed).isTrue();

        RecoveryExecutionQueueItem finishedItem = queueRepository.findById(queueItem.getId()).orElseThrow();
        assertThat(finishedItem.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);

        RecoveryAttempt finishedAttempt = attemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(finishedAttempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(finishedAttempt.getResultCode()).isEqualTo("WHATSAPP_DISPATCHED");
        assertThat(finishedAttempt.getRecoveryLink()).isNotEmpty();
    }

    @Test
    @DisplayName("Should calculate deterministic exponential backoff delays with upper bound")
    void shouldCalculateExponentialBackoffCorrectly() {
        commProperties.getRetry().setBaseDelaySeconds(10L);
        commProperties.getRetry().setMaxDelaySeconds(50L);

        // retry 0: 10 * 1 = 10
        assertThat(queueService.calculateRetryDelay(0)).isEqualTo(10L);
        // retry 1: 10 * 2 = 20
        assertThat(queueService.calculateRetryDelay(1)).isEqualTo(20L);
        // retry 2: 10 * 4 = 40
        assertThat(queueService.calculateRetryDelay(2)).isEqualTo(40L);
        // retry 3: 10 * 8 = 80 -> capped at maxDelay 50
        assertThat(queueService.calculateRetryDelay(3)).isEqualTo(50L);
    }

    @Test
    @DisplayName("Cross-tenant attempt execution must be rejected and dead-lettered immediately")
    void shouldRejectCrossTenantExecution() {
        Merchant foreignMerchant = merchantRepository.save(Merchant.builder()
                .name("Foreign Merchant " + UUID.randomUUID())
                .email("foreign_" + UUID.randomUUID() + "@test.com")
                .webhookSecret("foreign_sec_" + UUID.randomUUID())
                .build());

        RecoveryAttempt foreignAttempt = attemptRepository.save(RecoveryAttempt.builder()
                .merchant(foreignMerchant) // Different merchant!
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now())
                .build());

        RecoveryExecutionQueueItem corruptItem = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recoveryAttempt(foreignAttempt)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now().minusSeconds(5))
                .retryCount(0)
                .maxRetries(3)
                .build());

        boolean result = queueService.processQueueItem(corruptItem.getId());
        assertThat(result).isFalse();

        RecoveryExecutionQueueItem deadLettered = queueRepository.findById(corruptItem.getId()).orElseThrow();
        assertThat(deadLettered.getStatus()).isEqualTo(RecoveryQueueStatus.DEAD_LETTER);
        assertThat(deadLettered.getLastErrorCode()).isEqualTo("TENANT_MISMATCH");
    }
}
