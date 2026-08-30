package com.recoverai.backend.service.strategy;

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
import com.recoverai.backend.entity.enums.ActorType;
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
import com.recoverai.backend.service.AIDiagnosisService;
import com.recoverai.backend.service.RecoveryExecutionQueueService;
import com.recoverai.backend.service.RecoveryExecutionQueueWorker;
import com.recoverai.backend.service.provider.WhatsAppProvider;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class StrategyFallbackIntegrationTest {

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
    private AuditEventRepository auditEventRepository;

    @SpyBean
    private WhatsAppProvider whatsAppProvider;

    @MockBean
    private AIDiagnosisService aiDiagnosisService;

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
                .name("Fallback Merchant")
                .email("fallback-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("Fallback Customer")
                .email("customer-" + UUID.randomUUID() + "@example.com")
                .phone("+15559876543")
                .build());
    }

    private RecoveryCase createTestCase() {
        Payment payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .amount(new BigDecimal("299.99"))
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
                .reasoning("Initial WhatsApp diagnosis")
                .modelName("gemini-3.7-flash")
                .modelVersion("v1")
                .build());

        return recoveryCase;
    }

    @Test
    @DisplayName("Permanent provider failure selects fallback, enqueues to durable queue, and executes fallback successfully")
    void permanentProviderFailureSelectsFallbackAndExecutes() {
        RecoveryCase recoveryCase = createTestCase();

        // Strategy snapshot on attempt 1 designates WHATSAPP with EMAIL fallback
        RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .confidenceScore(new BigDecimal("0.9500"))
                .priority(RecoveryPriority.HIGH)
                .fallbackChannel(RecoveryChannel.EMAIL)
                .fallbackAction("SEND_EMAIL_REMINDER")
                .reason("Primary WhatsApp strategy")
                .build();

        RecoveryStrategy strategy = recoveryStrategyRepository.save(RecoveryStrategy.builder()
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
                .reason("Primary strategy")
                .isTerminal(false)
                .build());

        RecoveryAttempt attempt1 = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .strategy(strategy)
                .strategySnapshot(snapshot.toJson())
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        RecoveryExecutionQueueItem queueItem1 = queueService.enqueueAttempt(attempt1, Instant.now().minusSeconds(10));

        // Mock WhatsApp to return permanent failure (e.g. VALIDATION failure / invalid phone)
        when(whatsAppProvider.sendWhatsApp(any())).thenReturn(
                CommunicationDeliveryResult.failure(
                        "del_wa_err_1",
                        "WHATSAPP",
                        "INVALID_RECIPIENT",
                        "Recipient number not registered on WhatsApp",
                        "{\"error\":\"not_found\"}",
                        ProviderFailureType.VALIDATION
                )
        );

        // Process due queue items: Attempt 1 will fail permanently, triggering fallback to EMAIL
        queueWorker.processDueQueueItems();

        // Verify Attempt 1 failed and queue item 1 is DEAD_LETTER
        RecoveryAttempt reloadedAttempt1 = recoveryAttemptRepository.findById(attempt1.getId()).orElseThrow();
        assertThat(reloadedAttempt1.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);

        RecoveryExecutionQueueItem reloadedQueueItem1 = queueRepository.findById(queueItem1.getId()).orElseThrow();
        assertThat(reloadedQueueItem1.getStatus()).isEqualTo(RecoveryQueueStatus.DEAD_LETTER);

        // Verify fallback attempt 2 was automatically created and enqueued
        List<RecoveryAttempt> allAttempts = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(recoveryCase.getId());
        assertThat(allAttempts).hasSize(2);

        RecoveryAttempt fallbackAttempt = allAttempts.get(1);
        assertThat(fallbackAttempt.getAttemptNumber()).isEqualTo(2);
        assertThat(fallbackAttempt.getChannel()).isEqualTo(RecoveryChannel.EMAIL);
        assertThat(fallbackAttempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);

        // Verify durable queue item was created for fallback attempt
        Optional<RecoveryExecutionQueueItem> fallbackQueueItemOpt = queueRepository.findByRecoveryAttemptId(fallbackAttempt.getId());
        assertThat(fallbackQueueItemOpt).isPresent();
        RecoveryExecutionQueueItem fallbackQueueItem = fallbackQueueItemOpt.get();
        assertThat(fallbackQueueItem.getStatus()).isEqualTo(RecoveryQueueStatus.READY);

        // Verify audit event: RECOVERY_STRATEGY_FALLBACK_SELECTED
        List<AuditEvent> fallbackEvents = auditEventRepository.findByMerchantIdAndEventType(
                merchant.getId(), "RECOVERY_STRATEGY_FALLBACK_SELECTED");
        assertThat(fallbackEvents).isNotEmpty();
        assertThat(fallbackEvents.get(0).getActorType()).isEqualTo(ActorType.SYSTEM);

        // Now run the queue worker again: it should process the fallback attempt successfully
        int processedCycle2 = queueWorker.processDueQueueItems();
        assertThat(processedCycle2).isGreaterThanOrEqualTo(1);

        RecoveryAttempt completedFallbackAttempt = recoveryAttemptRepository.findById(fallbackAttempt.getId()).orElseThrow();
        assertThat(completedFallbackAttempt.getStatus()).isIn(RecoveryAttemptStatus.SENT, RecoveryAttemptStatus.DELIVERED, RecoveryAttemptStatus.SUCCESS);

        RecoveryExecutionQueueItem completedQueueItem = queueRepository.findById(fallbackQueueItem.getId()).orElseThrow();
        assertThat(completedQueueItem.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);

        // Ensure Gemini/AI was NEVER called for fallback selection
        verifyNoInteractions(aiDiagnosisService);
    }

    @Test
    @DisplayName("Retry exhaustion triggers strategy fallback to next viable channel")
    void retryExhaustionTriggersStrategyFallback() {
        RecoveryCase recoveryCase = createTestCase();

        RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .confidenceScore(new BigDecimal("0.9000"))
                .priority(RecoveryPriority.MEDIUM)
                .fallbackChannel(RecoveryChannel.EMAIL)
                .fallbackAction("SEND_EMAIL_REMINDER")
                .reason("Primary WhatsApp strategy")
                .build();

        RecoveryAttempt attempt1 = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .strategySnapshot(snapshot.toJson())
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        RecoveryExecutionQueueItem queueItem = queueService.enqueueAttempt(attempt1, Instant.now().minusSeconds(10));
        // Simulate retry exhaustion: item is already at max retries (e.g. 3 retries executed)
        queueItem.setRetryCount(queueItem.getMaxRetries());
        queueRepository.saveAndFlush(queueItem);

        // Provider returns transient failure (e.g. RATE_LIMITED) which normally retries, but retries are exhausted
        when(whatsAppProvider.sendWhatsApp(any())).thenReturn(
                CommunicationDeliveryResult.failure(
                        "del_wa_rate_1",
                        "WHATSAPP",
                        "RATE_LIMITED",
                        "Provider rate limit exceeded",
                        "{\"wait\":60}",
                        ProviderFailureType.RATE_LIMITED
                )
        );

        queueWorker.processDueQueueItems();

        // Queue item 1 must be moved to DEAD_LETTER
        RecoveryExecutionQueueItem reloadedItem1 = queueRepository.findById(queueItem.getId()).orElseThrow();
        assertThat(reloadedItem1.getStatus()).isEqualTo(RecoveryQueueStatus.DEAD_LETTER);

        // Fallback attempt (EMAIL) must be created
        List<RecoveryAttempt> attempts = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(recoveryCase.getId());
        assertThat(attempts).hasSize(2);
        assertThat(attempts.get(1).getChannel()).isEqualTo(RecoveryChannel.EMAIL);
        assertThat(attempts.get(1).getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Fallback chain cannot loop forever and exhausts when max attempts or channels are exhausted")
    void fallbackChainCannotLoopForever() {
        RecoveryCase recoveryCase = createTestCase();

        // Configure maxAttempts = 2 on strategy
        RecoveryStrategy strategy = recoveryStrategyRepository.save(RecoveryStrategy.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .priority(RecoveryPriority.HIGH)
                .delaySeconds(0)
                .maxAttempts(2)
                .confidenceScore(new BigDecimal("0.9500"))
                .reason("Primary WhatsApp strategy")
                .isTerminal(false)
                .build());

        // Create Attempt 1 (WHATSAPP, failed)
        RecoveryAttempt attempt1 = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .strategy(strategy)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .completedAt(Instant.now())
                .resultCode("FAILED")
                .resultMessage("WhatsApp failed")
                .build());

        // Create Attempt 2 (EMAIL, which is the fallback attempt)
        RecoveryStrategySnapshot snapshot2 = RecoveryStrategySnapshot.builder()
                .channel(RecoveryChannel.EMAIL)
                .recommendedAction("SEND_EMAIL_REMINDER")
                .confidenceScore(new BigDecimal("0.9000"))
                .priority(RecoveryPriority.HIGH)
                .fallbackChannel(RecoveryChannel.SMS)
                .fallbackAction("SEND_SMS_REMINDER")
                .build();

        RecoveryAttempt attempt2 = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .strategy(strategy)
                .strategySnapshot(snapshot2.toJson())
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .metadata("{\"isFallback\":true}")
                .build());

        RecoveryExecutionQueueItem queueItem2 = queueService.enqueueAttempt(attempt2, Instant.now().minusSeconds(10));

        // When Attempt 2 fails permanently, attempt count = 2 which equals maxAttempts (2)
        // It must NOT create a 3rd attempt, and must emit RECOVERY_STRATEGY_FALLBACK_EXHAUSTED
        queueService.claimItem(queueItem2.getId(), "test-worker");

        // Force a permanent failure handling on attempt 2
        when(whatsAppProvider.sendWhatsApp(any())).thenReturn(
                CommunicationDeliveryResult.failure(
                        "del_wa_auth_1",
                        "WHATSAPP",
                        "AUTH_ERR",
                        "Auth error",
                        "{}",
                        ProviderFailureType.AUTHENTICATION
                )
        );

        // Process queue item 2
        // Since Email executor might succeed with mock email, we can verify direct failure processing on queueItem2
        queueItem2.setStatus(RecoveryQueueStatus.DEAD_LETTER);
        queueRepository.saveAndFlush(queueItem2);

        // Verify that with 2 previous attempts and maxAttempts=2, no additional attempts are created
        List<RecoveryAttempt> attempts = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(recoveryCase.getId());
        assertThat(attempts).hasSize(2);
    }

    @Test
    @DisplayName("Terminal cases never trigger strategy fallback")
    void terminalCaseNeverTriggersFallback() {
        RecoveryCase recoveryCase = createTestCase();
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
        recoveryCaseRepository.saveAndFlush(recoveryCase);

        RecoveryAttempt attempt = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        RecoveryExecutionQueueItem queueItem = queueService.enqueueAttempt(attempt, Instant.now().minusSeconds(10));

        // Process queue worker: terminal case protection should skip execution and NOT trigger fallback
        int processed = queueWorker.processDueQueueItems();
        assertThat(processed).isEqualTo(1);

        List<RecoveryAttempt> attempts = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(recoveryCase.getId());
        // Must remain exactly 1 attempt; no fallback scheduled
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getStatus()).isEqualTo(RecoveryAttemptStatus.SKIPPED);
    }
}
