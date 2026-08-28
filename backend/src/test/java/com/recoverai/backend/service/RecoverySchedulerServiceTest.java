package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.config.RecoverySchedulerProperties;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.AuditEvent;
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
import com.recoverai.backend.exception.AgentDecisionNotFoundException;
import com.recoverai.backend.exception.DuplicateOrchestrationException;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.InvalidScheduledTimeException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.service.executor.DefaultRecoveryActionExecutor;
import com.recoverai.backend.service.executor.EmailRecoveryExecutor;
import com.recoverai.backend.service.executor.ManualRecoveryExecutor;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import com.recoverai.backend.service.executor.RetryChargeRecoveryExecutor;
import com.recoverai.backend.service.executor.SmartLinkRecoveryExecutor;
import com.recoverai.backend.service.executor.SmsRecoveryExecutor;
import com.recoverai.backend.service.executor.WhatsAppRecoveryExecutor;
import com.recoverai.backend.service.link.DefaultRecoveryLinkService;
import com.recoverai.backend.service.link.RecoveryLinkService;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.WhatsAppProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoverySchedulerServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private AgentDecisionRepository agentDecisionRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private WhatsAppProvider whatsappProvider;

    @Mock
    private EmailProvider emailProvider;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private PaymentRetryProvider paymentRetryProvider;

    private RecoverySchedulerProperties schedulerProperties;
    private RecoveryLinkService recoveryLinkService;
    private AuditService auditService;
    private DefaultRecoveryActionExecutor defaultActionExecutor;
    private WhatsAppRecoveryExecutor whatsAppExecutor;
    private EmailRecoveryExecutor emailExecutor;
    private SmsRecoveryExecutor smsExecutor;
    private SmartLinkRecoveryExecutor smartLinkExecutor;
    private RetryChargeRecoveryExecutor retryChargeExecutor;
    private ManualRecoveryExecutor manualExecutor;
    private RecoverySchedulerService recoverySchedulerService;

    private Merchant merchant;
    private Payment payment;
    private Customer customer;
    private RecoveryCase recoveryCase;
    private AgentDecision agentDecision;
    private UUID merchantId;
    private UUID recoveryCaseId;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditEventRepository);
        RecoveryCommunicationProperties commProperties = new RecoveryCommunicationProperties();
        commProperties.setBaseUrl("https://pay.recoverai.io/r/");
        recoveryLinkService = new DefaultRecoveryLinkService(commProperties);

        defaultActionExecutor = new DefaultRecoveryActionExecutor(recoveryLinkService);
        whatsAppExecutor = new WhatsAppRecoveryExecutor(whatsappProvider, recoveryLinkService);
        emailExecutor = new EmailRecoveryExecutor(emailProvider, recoveryLinkService);
        smsExecutor = new SmsRecoveryExecutor(smsProvider, recoveryLinkService);
        smartLinkExecutor = new SmartLinkRecoveryExecutor(recoveryLinkService);
        retryChargeExecutor = new RetryChargeRecoveryExecutor(paymentRetryProvider);
        manualExecutor = new ManualRecoveryExecutor(recoveryLinkService);

        List<RecoveryActionExecutor> executors = List.of(
                whatsAppExecutor,
                emailExecutor,
                smsExecutor,
                smartLinkExecutor,
                retryChargeExecutor,
                manualExecutor,
                defaultActionExecutor
        );

        schedulerProperties = new RecoverySchedulerProperties(true, 5000L, 50);

        recoverySchedulerService = new RecoverySchedulerService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                executors,
                defaultActionExecutor,
                auditService,
                schedulerProperties
        );

        merchantId = UUID.randomUUID();
        recoveryCaseId = UUID.randomUUID();

        merchant = Merchant.builder()
                .id(merchantId)
                .name("Test Merchant")
                .email("merchant@test.com")
                .webhookSecret("whsec_123")
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .name("Alice Smith")
                .email("alice@example.com")
                .phone("+919876543210")
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_12345")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
                .errorCode("BAD_REQUEST_ERROR")
                .errorDescription("Payment timeout")
                .riskLevel(RiskLevel.LOW)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(recoveryCaseId)
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("TIMEOUT")
                .estimatedRecoverableAmount(new BigDecimal("1500.00"))
                .currency("INR")
                .build();

        agentDecision = AgentDecision.builder()
                .id(UUID.randomUUID())
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .recommendedAction("WHATSAPP_SMART_LINK")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.89"))
                .reasoning("High UPI success rate on WhatsApp payment links")
                .modelName("gemini-3.7-flash")
                .build();
    }

    @Test
    @DisplayName("Should successfully schedule a recovery attempt for a future time")
    void shouldSuccessfullyScheduleRecoveryAttempt() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        UUID attemptId = UUID.randomUUID();
        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(attemptId);
                toSave.setCreatedAt(Instant.now());
                toSave.setUpdatedAt(Instant.now());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, futureTime);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(attemptId);
        assertThat(response.getRecoveryCaseId()).isEqualTo(recoveryCaseId);
        assertThat(response.getMerchantId()).isEqualTo(merchantId);
        assertThat(response.getAttemptNumber()).isEqualTo(1);
        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);
        assertThat(response.getScheduledAt()).isEqualTo(futureTime);
        assertThat(response.getExecutedAt()).isNull();

        // Recovery case transitioned to IN_PROGRESS
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.IN_PROGRESS);
        verify(recoveryCaseRepository).save(recoveryCase);

        // Audit event recorded
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_SCHEDULED");
    }

    @Test
    @DisplayName("Should schedule recovery attempt with current timestamp when scheduledAt is null")
    void shouldScheduleWithDefaultTimestampWhenNull() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, null);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);
        assertThat(response.getScheduledAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject scheduling with past timestamp and throw InvalidScheduledTimeException")
    void shouldRejectPastScheduledTime() {
        Instant pastTime = Instant.now().minus(10, ChronoUnit.MINUTES);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));

        assertThatThrownBy(() -> recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, pastTime))
                .isInstanceOf(InvalidScheduledTimeException.class)
                .hasMessageContaining("Scheduled time cannot be in the past");

        verify(recoveryAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when merchantId or recoveryCaseId is null")
    void shouldThrowWhenNullIds() {
        assertThatThrownBy(() -> recoverySchedulerService.scheduleRecovery(null, recoveryCaseId, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merchant ID cannot be null");

        assertThatThrownBy(() -> recoverySchedulerService.scheduleRecovery(merchantId, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recovery Case ID cannot be null");
    }

    @Test
    @DisplayName("Should reject scheduling for terminal cases (RECOVERED, CANCELLED, EXPIRED)")
    void shouldRejectTerminalCaseStatuses() {
        for (RecoveryCaseStatus terminalStatus : List.of(RecoveryCaseStatus.RECOVERED, RecoveryCaseStatus.CANCELLED, RecoveryCaseStatus.EXPIRED)) {
            recoveryCase.setStatus(terminalStatus);
            when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));

            assertThatThrownBy(() -> recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, Instant.now().plusSeconds(300)))
                    .isInstanceOf(InvalidRecoveryCaseStateException.class)
                    .hasMessageContaining("Cannot schedule recovery for case in terminal status: " + terminalStatus);

            verify(recoveryAttemptRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should reject duplicate scheduling when an attempt is already SCHEDULED or IN_FLIGHT")
    void shouldRejectDuplicateActiveAttempt() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, Instant.now().plusSeconds(300)))
                .isInstanceOf(DuplicateOrchestrationException.class)
                .hasMessageContaining("An active recovery attempt is already scheduled or in-flight");

        verify(recoveryAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AgentDecisionNotFoundException when decision is missing")
    void shouldThrowWhenAgentDecisionMissing() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, Instant.now().plusSeconds(300)))
                .isInstanceOf(AgentDecisionNotFoundException.class)
                .hasMessageContaining("No AgentDecision found for recovery case");

        verify(recoveryAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject scheduling if AgentDecision merchant does not match")
    void shouldRejectAgentDecisionMerchantMismatch() {
        Merchant otherMerchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Other Merchant")
                .email("other@merchant.com")
                .build();
        agentDecision.setMerchant(otherMerchant);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));

        assertThatThrownBy(() -> recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, Instant.now().plusSeconds(300)))
                .isInstanceOf(RecoveryCaseNotFoundException.class)
                .hasMessageContaining("AgentDecision merchant mismatch");

        verify(recoveryAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate next attempt number correctly (e.g., attempt #3 after #2)")
    void shouldCalculateNextAttemptNumberCorrectly() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));

        RecoveryAttempt previousAttempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(2)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .build();
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.of(previousAttempt));

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, Instant.now().plusSeconds(300));
        assertThat(response.getAttemptNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("pollAndExecuteDueAttempts should return 0 when no due attempts exist")
    void pollAndExecuteDueAttemptsShouldReturnZeroWhenEmpty() {
        when(recoveryAttemptRepository.findDueScheduledAttemptIds(eq(RecoveryAttemptStatus.SCHEDULED), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());

        int processed = recoverySchedulerService.pollAndExecuteDueAttempts();

        assertThat(processed).isEqualTo(0);
    }

    @Test
    @DisplayName("pollAndExecuteDueAttempts should claim and execute due attempts")
    void pollAndExecuteDueAttemptsShouldProcessDueAttempts() {
        UUID attemptId1 = UUID.randomUUID();
        UUID attemptId2 = UUID.randomUUID();

        when(recoveryAttemptRepository.findDueScheduledAttemptIds(eq(RecoveryAttemptStatus.SCHEDULED), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(attemptId1, attemptId2));

        when(recoveryAttemptRepository.claimAttemptForExecution(eq(attemptId1), eq(RecoveryAttemptStatus.SCHEDULED), eq(RecoveryAttemptStatus.IN_FLIGHT), any(Instant.class)))
                .thenReturn(1);
        when(recoveryAttemptRepository.claimAttemptForExecution(eq(attemptId2), eq(RecoveryAttemptStatus.SCHEDULED), eq(RecoveryAttemptStatus.IN_FLIGHT), any(Instant.class)))
                .thenReturn(0); // already claimed by another worker

        RecoveryAttempt attempt1 = RecoveryAttempt.builder()
                .id(attemptId1)
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build();

        when(recoveryAttemptRepository.findById(attemptId1)).thenReturn(Optional.of(attempt1));
        when(whatsappProvider.sendWhatsApp(any()))
                .thenReturn(CommunicationDeliveryResult.success("wa_123", "MOCK_WHATSAPP", "WHATSAPP_DISPATCHED", "Delivered", "{}"));

        int processed = recoverySchedulerService.pollAndExecuteDueAttempts();

        assertThat(processed).isEqualTo(1);
        assertThat(attempt1.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        verify(recoveryAttemptRepository).save(attempt1);
    }

    @Test
    @DisplayName("claimAndExecuteAttempt should skip execution if RecoveryCase is already terminal (RECOVERED/CANCELLED/EXPIRED)")
    void shouldSkipAttemptIfCaseIsTerminal() {
        UUID attemptId = UUID.randomUUID();
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);

        when(recoveryAttemptRepository.claimAttemptForExecution(eq(attemptId), eq(RecoveryAttemptStatus.SCHEDULED), eq(RecoveryAttemptStatus.IN_FLIGHT), any(Instant.class)))
                .thenReturn(1);

        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .id(attemptId)
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .scheduledAt(Instant.now().minusSeconds(60))
                .build();

        when(recoveryAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

        boolean result = recoverySchedulerService.claimAndExecuteAttempt(attemptId);

        assertThat(result).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SKIPPED);
        assertThat(attempt.getResultCode()).isEqualTo("CASE_TERMINAL");
        verify(whatsappProvider, never()).sendWhatsApp(any());
        verify(recoveryAttemptRepository).save(attempt);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_SKIPPED");
    }

    @Test
    @DisplayName("claimAndExecuteAttempt should transition case to RECOVERED when RETRY_CHARGE succeeds")
    void shouldRecoverCaseOnRetryChargeSuccess() {
        UUID attemptId = UUID.randomUUID();

        when(recoveryAttemptRepository.claimAttemptForExecution(eq(attemptId), eq(RecoveryAttemptStatus.SCHEDULED), eq(RecoveryAttemptStatus.IN_FLIGHT), any(Instant.class)))
                .thenReturn(1);

        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .id(attemptId)
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.RETRY_CHARGE)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build();

        when(recoveryAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(paymentRetryProvider.retryCharge(any()))
                .thenReturn(PaymentRetryResult.success("txn_999", "MOCK_RAZORPAY", "PAYMENT_RETRY_CAPTURED", "Success", "{}"));

        boolean result = recoverySchedulerService.claimAndExecuteAttempt(attemptId);

        assertThat(result).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SUCCESS);
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.RECOVERED);
        assertThat(recoveryCase.getRecoveredAt()).isNotNull();

        verify(recoveryCaseRepository, atLeastOnce()).save(recoveryCase);
        verify(recoveryAttemptRepository).save(attempt);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_CLAIMED", "RECOVERY_ATTEMPT_STARTED", "RECOVERY_ATTEMPT_SUCCEEDED");
    }

    @Test
    @DisplayName("claimAndExecuteAttempt should mark attempt FAILED on executor exception without corrupting case")
    void shouldHandleExecutorExceptionGracefullyInScheduler() {
        UUID attemptId = UUID.randomUUID();

        when(recoveryAttemptRepository.claimAttemptForExecution(eq(attemptId), eq(RecoveryAttemptStatus.SCHEDULED), eq(RecoveryAttemptStatus.IN_FLIGHT), any(Instant.class)))
                .thenReturn(1);

        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .id(attemptId)
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build();

        when(recoveryAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(emailProvider.sendEmail(any()))
                .thenThrow(new RuntimeException("Mail server network timeout"));

        boolean result = recoverySchedulerService.claimAndExecuteAttempt(attemptId);

        assertThat(result).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(attempt.getResultCode()).isEqualTo("EXECUTION_ERROR");
        assertThat(attempt.getResultMessage()).contains("Mail server network timeout");
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.IN_PROGRESS);

        verify(recoveryAttemptRepository).save(attempt);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_FAILED");
    }
}
