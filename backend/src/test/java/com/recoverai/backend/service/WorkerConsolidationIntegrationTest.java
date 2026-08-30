package com.recoverai.backend.service;

import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.strategy.RecoveryStrategySnapshot;
import com.recoverai.backend.entity.AgentDecision;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WorkerConsolidationIntegrationTest {

    @Autowired
    private RecoverySchedulerService recoverySchedulerService;

    @Autowired
    private RecoverySchedulerWorker recoverySchedulerWorker;

    @Autowired
    private RecoveryExecutionQueueService queueService;

    @Autowired
    private RecoveryExecutionQueueWorker queueWorker;

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
    private RecoveryStrategyRepository recoveryStrategyRepository;

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private com.recoverai.backend.repository.AuditEventRepository auditEventRepository;

    private Merchant merchant;
    private Customer customer;

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

        merchant = merchantRepository.save(Merchant.builder()
                .name("Worker Consolidation Merchant")
                .email("consolidation-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("Jane Doe")
                .email("jane@example.com")
                .phone("+15551234567")
                .build());
    }

    private RecoveryCase createCase() {
        Payment payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .amount(new BigDecimal("149.99"))
                .currency("USD")
                .status(PaymentStatus.FAILED)
                .razorpayPaymentId("pay_" + UUID.randomUUID())
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(payment.getAmount())
                .currency("USD")
                .build());

        agentDecisionRepository.save(AgentDecision.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .confidenceScore(new BigDecimal("0.9500"))
                .reasoning("High engagement channel")
                .modelName("gemini-3.7-flash")
                .modelVersion("v1")
                .build());

        RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .confidenceScore(new BigDecimal("0.9500"))
                .priority(RecoveryPriority.HIGH)
                .fallbackChannel(RecoveryChannel.EMAIL)
                .fallbackAction("SEND_EMAIL_REMINDER")
                .reason("Primary WhatsApp strategy")
                .build();

        recoveryStrategyRepository.save(RecoveryStrategy.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .priority(RecoveryPriority.HIGH)
                .delaySeconds(0)
                .maxAttempts(3)
                .confidenceScore(new BigDecimal("0.9500"))
                .fallbackChannel(RecoveryChannel.EMAIL)
                .fallbackAction("SEND_EMAIL_REMINDER")
                .reason(snapshot.toJson())
                .isTerminal(false)
                .build());

        return recoveryCase;
    }

    @Test
    @DisplayName("Legacy scheduler worker is decommissioned and does not perform duplicate execution")
    void legacySchedulerWorkerDoesNotPerformDuplicateExecution() {
        RecoveryCase recoveryCase = createCase();

        // Create a scheduled attempt manually
        RecoveryAttempt attempt = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(60))
                .build());

        // Invoke legacy scheduler cycle
        recoverySchedulerWorker.runSchedulerCycle();

        // The attempt must remain SCHEDULED because the legacy direct execution cycle is decommissioned
        RecoveryAttempt reloaded = recoveryAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);
        assertThat(recoverySchedulerWorker.isDecommissioned()).isTrue();
    }

    @Test
    @DisplayName("Scheduling recovery creates attempt AND enqueues a durable READY queue item")
    void schedulingCreatesDurableQueueItem() {
        RecoveryCase recoveryCase = createCase();

        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(
                merchant.getId(), recoveryCase.getId(), Instant.now());

        assertThat(response).isNotNull();
        UUID attemptId = response.getId();

        // Verify attempt is in DB
        Optional<RecoveryAttempt> attemptOpt = recoveryAttemptRepository.findById(attemptId);
        assertThat(attemptOpt).isPresent();
        assertThat(attemptOpt.get().getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);

        // Verify queue item exists in READY state
        Optional<RecoveryExecutionQueueItem> queueItemOpt = queueRepository.findByRecoveryAttemptId(attemptId);
        assertThat(queueItemOpt).isPresent();
        RecoveryExecutionQueueItem queueItem = queueItemOpt.get();
        assertThat(queueItem.getStatus()).isEqualTo(RecoveryQueueStatus.READY);
        assertThat(queueItem.getMerchant().getId()).isEqualTo(merchant.getId());
        assertThat(queueItem.getRecoveryCase().getId()).isEqualTo(recoveryCase.getId());
    }

    @Test
    @DisplayName("RecoveryExecutionQueueWorker is the authoritative background execution mechanism")
    void queueWorkerIsAuthoritativeBackgroundExecution() {
        RecoveryCase recoveryCase = createCase();

        // Schedule recovery (creates attempt and queue item)
        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(
                merchant.getId(), recoveryCase.getId(), Instant.now().minusSeconds(10));

        UUID attemptId = response.getId();

        // Process due items using the authoritative queue worker
        int processed = queueWorker.processDueQueueItems();
        assertThat(processed).isGreaterThanOrEqualTo(1);

        // Verify queue item transitioned to COMPLETED
        RecoveryExecutionQueueItem queueItem = queueRepository.findByRecoveryAttemptId(attemptId).orElseThrow();
        assertThat(queueItem.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);

        // Verify attempt transitioned to SENT or SUCCESS
        RecoveryAttempt attempt = recoveryAttemptRepository.findById(attemptId).orElseThrow();
        assertThat(attempt.getStatus()).isIn(RecoveryAttemptStatus.SENT, RecoveryAttemptStatus.DELIVERED, RecoveryAttemptStatus.SUCCESS);
    }
}
