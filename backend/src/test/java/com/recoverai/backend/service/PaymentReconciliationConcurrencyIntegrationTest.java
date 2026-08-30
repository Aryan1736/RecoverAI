package com.recoverai.backend.service;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PaymentReconciliationConcurrencyIntegrationTest {

    @Autowired
    private PaymentReconciliationService reconciliationService;

    @Autowired
    private RecoveryExecutionQueueService queueService;

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
    private jakarta.persistence.EntityManager entityManager;

    private Merchant merchant;
    private Customer customer;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.findByRazorpayAccountId("acc_conc_recon_test").orElseGet(() ->
                merchantRepository.save(Merchant.builder()
                        .name("Concurrency Merchant")
                        .email("conc_recon@test.com")
                        .razorpayAccountId("acc_conc_recon_test")
                        .webhookSecret("conc_recon_secret_123")
                        .status(MerchantStatus.ACTIVE)
                        .build()));

        customer = customerRepository.findByMerchantIdAndEmail(merchant.getId(), "conc_user@test.com").orElseGet(() ->
                customerRepository.save(Customer.builder()
                        .merchant(merchant)
                        .email("conc_user@test.com")
                        .phone("+919333333333")
                        .build()));
    }

    @Test
    @DisplayName("Concurrency: Payment webhook reconciliation racing with queue worker execution ends in safe terminal state")
    void testConcurrency_PaymentReconciliationRacingWithQueueWorker() throws Exception {
        // 1. Setup initial failed payment and recovery case
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_fail_conc_01")
                .razorpayOrderId("order_conc_001")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .payment(failedPayment)
                .customer(customer)
                .status(RecoveryCaseStatus.IN_PROGRESS)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("1500.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .build());

        RecoveryAttempt attempt = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build());

        RecoveryExecutionQueueItem queueItem = queueRepository.save(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recoveryAttempt(attempt)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now().minusSeconds(10)) // Due now
                .build());

        // Setup successful payment entity that will be processed by webhook thread
        Payment capturedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_succ_conc_02")
                .razorpayOrderId("order_conc_001")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .build());

        // 2. Concurrently execute webhook reconciliation and queue worker
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<?> reconciliationFuture = executor.submit(() -> {
            try {
                startLatch.await();
                reconciliationService.reconcilePaymentSuccess(merchant, capturedPayment, "payment.captured", "127.0.0.1");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future<?> workerFuture = executor.submit(() -> {
            try {
                startLatch.await();
                if (queueService.claimItem(queueItem.getId(), "test-worker")) {
                    queueService.processQueueItem(queueItem.getId());
                }
            } catch (Exception e) {
                // Concurrency lock contention or rollback is handled gracefully by worker runtime
            }
        });

        startLatch.countDown();

        reconciliationFuture.get(10, TimeUnit.SECONDS);
        workerFuture.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        entityManager.clear();

        // 3. Verify final safe state
        RecoveryCase finalCase = recoveryCaseRepository.findById(recoveryCase.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.RECOVERED, finalCase.getStatus(), "Case must end in RECOVERED status");
        assertEquals(new BigDecimal("1500.00"), finalCase.getRecoveredAmount());

        RecoveryExecutionQueueItem finalQueueItem = queueRepository.findById(queueItem.getId()).orElseThrow();
        assertEquals(RecoveryQueueStatus.COMPLETED, finalQueueItem.getStatus(), "Queue item must be COMPLETED");

        RecoveryAttempt finalAttempt = recoveryAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertTrue(finalAttempt.getStatus() == RecoveryAttemptStatus.SUCCESS || finalAttempt.getStatus() == RecoveryAttemptStatus.SKIPPED,
                "Attempt status must be terminal SUCCESS or SKIPPED, was: " + finalAttempt.getStatus());
    }
}
