package com.recoverai.backend.controller;

import com.recoverai.backend.config.RecoveryQueueProperties;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.strategy.RecoveryStrategySnapshot;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
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
import com.recoverai.backend.service.RecoveryExecutionQueueService;
import com.recoverai.backend.service.RecoveryExecutionQueueWorker;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoveryExecutionQueueIntegrationTest {

    @Autowired
    private RecoverySchedulerService recoverySchedulerService;

    @Autowired
    private RecoveryExecutionQueueService queueService;

    @Autowired
    private RecoveryExecutionQueueWorker queueWorker;

    @Autowired
    private RecoveryQueueProperties queueProperties;

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private RecoveryStrategyRepository recoveryStrategyRepository;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

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

    @BeforeEach
    void setUp() {
        queueProperties.setEnabled(false);

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
                .name("Queue Integration Merchant")
                .email("queue_integ_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .webhookSecret("sec_queue_123")
                .build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .email("cust_integ_" + UUID.randomUUID() + "@test.com")
                .name("Charlie Integration")
                .phone("+919999933333")
                .build());
    }

    private RecoveryCase createTestCase(BigDecimal amount) {
        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(amount)
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(amount)
                .build());

        agentDecisionRepository.saveAndFlush(AgentDecision.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .recommendedAction("WHATSAPP_SMART_LINK")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.9100"))
                .reasoning("High WhatsApp engagement expected")
                .modelName("gemini-3.7-flash")
                .build());

        return recoveryCase;
    }

    @Test
    @DisplayName("End-to-End: scheduleRecovery should create attempt and queue item, and queueWorker should execute it")
    void testScheduleRecoveryThenQueueWorkerExecutes() {
        RecoveryCase rCase = createTestCase(new BigDecimal("2500.00"));

        // 1. Schedule recovery
        RecoveryAttemptResponseDto scheduleResponse = recoverySchedulerService.scheduleRecovery(
                merchant.getId(), rCase.getId(), Instant.now().minusSeconds(5));

        assertThat(scheduleResponse).isNotNull();
        assertThat(scheduleResponse.getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);

        // 2. Verify queue item exists in READY status
        UUID attemptId = scheduleResponse.getId();
        Optional<RecoveryExecutionQueueItem> queueItemOpt = queueRepository.findByRecoveryAttemptId(attemptId);
        assertThat(queueItemOpt).isPresent();
        assertThat(queueItemOpt.get().getStatus()).isEqualTo(RecoveryQueueStatus.READY);

        // 3. Trigger queue worker cycle
        int processedCount = queueWorker.processDueQueueItems();
        assertThat(processedCount).isEqualTo(1);

        // 4. Verify attempt was executed and status is SENT
        RecoveryAttempt executedAttempt = recoveryAttemptRepository.findById(attemptId).orElseThrow();
        assertThat(executedAttempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(executedAttempt.getExecutedAt()).isNotNull();
        assertThat(executedAttempt.getCompletedAt()).isNotNull();

        // 5. Verify queue item is COMPLETED
        RecoveryExecutionQueueItem completedQueueItem = queueRepository.findById(queueItemOpt.get().getId()).orElseThrow();
        assertThat(completedQueueItem.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);
        assertThat(completedQueueItem.getCompletedAt()).isNotNull();

        // 6. Verify audit events recorded
        List<AuditEvent> auditEvents = auditEventRepository.findByMerchantId(merchant.getId());
        List<String> eventTypes = auditEvents.stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains(
                "RECOVERY_ATTEMPT_SCHEDULED",
                "RECOVERY_EXECUTION_QUEUED",
                "RECOVERY_EXECUTION_CLAIMED",
                "RECOVERY_EXECUTION_STARTED",
                "RECOVERY_EXECUTION_COMPLETED"
        );
    }

    @Test
    @DisplayName("Strategy snapshot authority: Worker executes persisted strategy snapshot without regenerating decision")
    void testStrategySnapshotAuthorityDuringQueueExecution() {
        RecoveryCase rCase = createTestCase(new BigDecimal("5000.00"));

        RecoveryStrategy snapshotStrategy = recoveryStrategyRepository.saveAndFlush(RecoveryStrategy.builder()
                .merchant(merchant)
                .recoveryCase(rCase)
                .channel(RecoveryChannel.EMAIL)
                .recommendedAction("SEND_EMAIL_DISCOUNT")
                .confidenceScore(new BigDecimal("0.8500"))
                .priority(RecoveryPriority.HIGH)
                .maxAttempts(3)
                .delaySeconds(0)
                .reason("Customer email responsiveness is high")
                .isTerminal(false)
                .build());

        RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.fromStrategy(snapshotStrategy);

        RecoveryAttempt attempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(rCase)
                .merchant(merchant)
                .strategy(snapshotStrategy)
                .strategySnapshot(snapshot.toJson())
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        // Now modify the underlying AgentDecision or add another strategy that prefers SMS
        agentDecisionRepository.saveAndFlush(AgentDecision.builder()
                .recoveryCase(rCase)
                .merchant(merchant)
                .recommendedAction("SEND_SMS_URGENT")
                .channel(RecoveryChannel.SMS)
                .confidenceScore(new BigDecimal("0.9900"))
                .reasoning("Switching to SMS")
                .modelName("gemini-3.7-flash")
                .build());

        // Enqueue and run queue worker
        queueService.enqueueAttempt(attempt, Instant.now().minusSeconds(10));
        int processed = queueWorker.processDueQueueItems();
        assertThat(processed).isEqualTo(1);

        // Verify the executed channel was EMAIL (from the persisted snapshot/attempt), NOT SMS
        RecoveryAttempt reloaded = recoveryAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloaded.getChannel()).isEqualTo(RecoveryChannel.EMAIL);
        assertThat(reloaded.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
    }

    @Test
    @DisplayName("Terminal case skip: Queue worker skips execution if RecoveryCase became terminal before execution")
    void testTerminalCaseSkipInQueueWorker() {
        RecoveryCase rCase = createTestCase(new BigDecimal("3000.00"));

        RecoveryAttempt attempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(rCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        RecoveryExecutionQueueItem item = queueService.enqueueAttempt(attempt, Instant.now().minusSeconds(10));

        // Case becomes RECOVERED out-of-band
        rCase.setStatus(RecoveryCaseStatus.RECOVERED);
        rCase.setRecoveredAt(Instant.now());
        recoveryCaseRepository.saveAndFlush(rCase);

        // Run worker
        int processed = queueWorker.processDueQueueItems();
        assertThat(processed).isEqualTo(1);

        // Attempt must be marked SKIPPED with CASE_TERMINAL
        RecoveryAttempt reloadedAttempt = recoveryAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SKIPPED);
        assertThat(reloadedAttempt.getResultCode()).isEqualTo("CASE_TERMINAL");

        // Queue item must be safely COMPLETED
        RecoveryExecutionQueueItem reloadedItem = queueRepository.findById(item.getId()).orElseThrow();
        assertThat(reloadedItem.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);

        // Audit events verify skipped execution
        List<AuditEvent> auditEvents = auditEventRepository.findByMerchantId(merchant.getId());
        List<String> eventTypes = auditEvents.stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_EXECUTION_SKIPPED", "RECOVERY_ATTEMPT_SKIPPED");
    }

    @Test
    @DisplayName("Multi-tenant security: Cross-tenant queue lookup and execution is rejected")
    void testMultiTenantQueueIsolation() {
        Merchant attackerMerchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Attacker Merchant")
                .email("attacker_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        RecoveryCase rCase = createTestCase(new BigDecimal("1200.00"));
        RecoveryAttempt attempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(rCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        RecoveryExecutionQueueItem item = queueService.enqueueAttempt(attempt, Instant.now().minusSeconds(10));

        // Merchant A's queue item cannot be accessed by Attacker
        Optional<RecoveryExecutionQueueItem> attackerAccess = queueService.getQueueItem(attackerMerchant.getId(), item.getId());
        assertThat(attackerAccess).isEmpty();

        // Merchant A can access their own item
        Optional<RecoveryExecutionQueueItem> ownAccess = queueService.getQueueItem(merchant.getId(), item.getId());
        assertThat(ownAccess).isPresent();
    }
}
