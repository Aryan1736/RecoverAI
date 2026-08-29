package com.recoverai.backend.controller;

import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.service.RecoverySchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoverySchedulerIntegrationTest {

    @Autowired
    private RecoverySchedulerService recoverySchedulerService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private com.recoverai.backend.repository.AuditEventRepository auditEventRepository;

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    private Merchant merchant;
    private Customer customer;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        queueRepository.deleteAll();
        recoveryAttemptRepository.deleteAll();
        agentDecisionRepository.deleteAll();
        recoveryCaseRepository.deleteAll();
        paymentRepository.deleteAll();
        customerRepository.deleteAll();
        merchantRepository.deleteAll();

        merchant = merchantRepository.save(Merchant.builder()
                .name("Scheduler Integration Merchant")
                .email("scheduler_" + UUID.randomUUID() + "@test.com")
                .webhookSecret("sec_sched_123")
                .build());

        customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("John Scheduler")
                .email("john_" + UUID.randomUUID() + "@test.com")
                .phone("+919999988888")
                .build());
    }

    private RecoveryCase createTestCase(RecoveryCaseStatus status) {
        Payment payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_sc_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("2500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
                .errorCode("GATEWAY_TIMEOUT")
                .errorDescription("Timeout")
                .riskLevel(RiskLevel.LOW)
                .build());

        RecoveryCase rCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(status)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("TIMEOUT")
                .estimatedRecoverableAmount(new BigDecimal("2500.00"))
                .currency("INR")
                .build());

        agentDecisionRepository.save(AgentDecision.builder()
                .recoveryCase(rCase)
                .merchant(merchant)
                .recommendedAction("WHATSAPP_SMART_LINK")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.9500"))
                .reasoning("High WhatsApp engagement")
                .modelName("gemini-3.7-flash")
                .modelVersion("v1")
                .build());

        return rCase;
    }

    @Test
    @DisplayName("Scheduler should pick up due attempts and ignore future attempts")
    void testSchedulerPicksUpDueAndIgnoresFuture() {
        RecoveryCase case1 = createTestCase(RecoveryCaseStatus.OPEN);
        RecoveryCase case2 = createTestCase(RecoveryCaseStatus.OPEN);

        // Attempt 1: Due (scheduled 5 minutes ago)
        RecoveryAttempt dueAttempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(case1)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build());

        // Attempt 2: Future (scheduled 2 hours from now)
        RecoveryAttempt futureAttempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(case2)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build());

        int processed = recoverySchedulerService.pollAndExecuteDueAttempts();

        assertThat(processed).isEqualTo(1);

        RecoveryAttempt reloadedDue = recoveryAttemptRepository.findById(dueAttempt.getId()).orElseThrow();
        assertThat(reloadedDue.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(reloadedDue.getExecutedAt()).isNotNull();
        assertThat(reloadedDue.getCompletedAt()).isNotNull();

        RecoveryAttempt reloadedFuture = recoveryAttemptRepository.findById(futureAttempt.getId()).orElseThrow();
        assertThat(reloadedFuture.getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);
        assertThat(reloadedFuture.getExecutedAt()).isNull();
    }

    @Test
    @DisplayName("Scheduler should mark attempt as SKIPPED when RecoveryCase became terminal before execution")
    void testSchedulerSkipsTerminalCaseAttempt() {
        RecoveryCase terminalCase = createTestCase(RecoveryCaseStatus.RECOVERED);

        RecoveryAttempt dueAttempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(terminalCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minus(2, ChronoUnit.MINUTES))
                .build());

        int processed = recoverySchedulerService.pollAndExecuteDueAttempts();

        assertThat(processed).isEqualTo(1);

        RecoveryAttempt reloaded = recoveryAttemptRepository.findById(dueAttempt.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RecoveryAttemptStatus.SKIPPED);
        assertThat(reloaded.getResultCode()).isEqualTo("CASE_TERMINAL");
    }

    @Test
    @DisplayName("Concurrent execution: Multiple threads competing for the same due attempt should execute exactly once")
    void testConcurrentExecutionPreventsDuplicateProcessing() throws InterruptedException, ExecutionException {
        RecoveryCase rCase = createTestCase(RecoveryCaseStatus.OPEN);

        RecoveryAttempt dueAttempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(rCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build());

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> recoverySchedulerService.claimAndExecuteAttempt(dueAttempt.getId()));
        }

        List<Future<Boolean>> results = executorService.invokeAll(tasks);
        executorService.shutdown();

        AtomicInteger successCount = new AtomicInteger(0);
        for (Future<Boolean> future : results) {
            if (future.get()) {
                successCount.incrementAndGet();
            }
        }

        // Exactly one thread should successfully claim and execute the attempt
        assertThat(successCount.get()).isEqualTo(1);

        // Verify attempt is SENT
        RecoveryAttempt reloaded = recoveryAttemptRepository.findById(dueAttempt.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);

        // Verify no duplicate attempts were created
        List<RecoveryAttempt> allAttempts = recoveryAttemptRepository.findByRecoveryCaseId(rCase.getId());
        assertThat(allAttempts).hasSize(1);
    }
}
