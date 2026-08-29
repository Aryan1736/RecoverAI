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
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.repository.RecoveryStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RecoveryExecutionQueueConcurrencyTest {

    @Autowired
    private RecoveryExecutionQueueService queueService;

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private RecoveryStrategyRepository recoveryStrategyRepository;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private RecoveryAttempt attempt;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        queueRepository.deleteAll();
        recoveryAttemptRepository.deleteAll();
        recoveryStrategyRepository.deleteAll();
        agentDecisionRepository.deleteAll();
        recoveryCaseRepository.deleteAll();
        paymentRepository.deleteAll();
        customerRepository.deleteAll();
        merchantRepository.deleteAll();

        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Concurrency Test Merchant")
                .email("concurrency_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .email("cust_conc_" + UUID.randomUUID() + "@test.com")
                .name("Bob Concurrency")
                .phone("+919999922222")
                .build());

        payment = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_conc_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("4999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        recoveryCase = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("4999.00"))
                .build());

        attempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());
    }

    @Test
    @DisplayName("At least 10 parallel workers competing for the same queue item should result in exactly 1 claim and 0 duplicate executions")
    void testParallelWorkersCompetingForSameItem() throws Exception {
        RecoveryExecutionQueueItem item = queueService.enqueueAttempt(attempt, Instant.now().minusSeconds(10));
        UUID itemId = item.getId();

        int workerCount = 12; // At least 10
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger successfulClaims = new AtomicInteger(0);
        AtomicInteger duplicateExecutions = new AtomicInteger(0);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < workerCount; i++) {
            final String workerId = "worker-node-" + i;
            tasks.add(() -> {
                startSignal.await(5, TimeUnit.SECONDS);
                boolean claimed = queueService.claimItem(itemId, workerId);
                if (claimed) {
                    successfulClaims.incrementAndGet();
                    boolean processed = queueService.processQueueItem(itemId);
                    if (!processed) {
                        duplicateExecutions.incrementAndGet();
                    }
                }
                return claimed;
            });
        }

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(executor.submit(task));
        }

        // Release all 12 worker threads simultaneously
        startSignal.countDown();

        for (Future<Boolean> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly 1 worker successfully claimed the item
        assertThat(successfulClaims.get()).isEqualTo(1);
        assertThat(duplicateExecutions.get()).isEqualTo(0);

        // Verify the queue item was completed and not left claimed or ready
        RecoveryExecutionQueueItem finalItem = queueRepository.findById(itemId).orElseThrow();
        assertThat(finalItem.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);

        // Verify attempt is SENT and only 1 attempt exists
        RecoveryAttempt finalAttempt = recoveryAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(finalAttempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(recoveryAttemptRepository.countByRecoveryCaseId(recoveryCase.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("10 parallel threads attempting duplicate enqueue for same attempt should create exactly 1 queue item")
    void testConcurrentDuplicateEnqueueCreatesExactlyOneItem() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<UUID> returnedQueueIds = Collections.synchronizedList(new ArrayList<>());

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                startSignal.await(5, TimeUnit.SECONDS);
                RecoveryExecutionQueueItem enqueued = queueService.enqueueAttempt(attempt, Instant.now());
                returnedQueueIds.add(enqueued.getId());
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }

        // Release threads simultaneously
        startSignal.countDown();

        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // All 10 threads succeeded and returned the SAME queue item ID
        assertThat(returnedQueueIds).hasSize(10);
        UUID expectedId = returnedQueueIds.get(0);
        for (UUID id : returnedQueueIds) {
            assertThat(id).isEqualTo(expectedId);
        }

        // Database has exactly 1 queue item for this attempt
        long count = queueRepository.count();
        assertThat(count).isEqualTo(1);
        assertThat(queueRepository.findByRecoveryAttemptId(attempt.getId())).isPresent();
    }
}
