package com.recoverai.backend.service;

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
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.service.executor.DefaultRecoveryActionExecutor;
import com.recoverai.backend.service.executor.ExecutionResult;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
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
class RecoveryOrchestratorServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private AgentDecisionRepository agentDecisionRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditService auditService;
    private DefaultRecoveryActionExecutor defaultActionExecutor;
    private RecoveryOrchestratorService recoveryOrchestratorService;

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
        defaultActionExecutor = new DefaultRecoveryActionExecutor();
        recoveryOrchestratorService = new RecoveryOrchestratorService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                List.of(defaultActionExecutor),
                defaultActionExecutor,
                auditService
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
    @DisplayName("Should successfully orchestrate first recovery attempt (attempt #1) and transition case to IN_PROGRESS")
    void shouldSuccessfullyOrchestrateFirstAttempt() {
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

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(attemptId);
        assertThat(response.getRecoveryCaseId()).isEqualTo(recoveryCaseId);
        assertThat(response.getMerchantId()).isEqualTo(merchantId);
        assertThat(response.getAttemptNumber()).isEqualTo(1);
        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(response.getResultCode()).isEqualTo("WHATSAPP_DISPATCHED");
        assertThat(response.getRecoveryLink()).contains(recoveryCaseId.toString());

        // Verify case transitioned to IN_PROGRESS
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.IN_PROGRESS);
        verify(recoveryCaseRepository).save(recoveryCase);

        // Verify audit trail (at least 3 events: created, started, completed)
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_CREATED", "RECOVERY_ATTEMPT_STARTED", "RECOVERY_ATTEMPT_COMPLETED");
    }

    @Test
    @DisplayName("Should increment attempt number on subsequent orchestrations (attempt #3 after #2)")
    void shouldIncrementAttemptNumberOnSubsequentOrchestration() {
        recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
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
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SENT)
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

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getAttemptNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should enforce tenant isolation and throw RecoveryCaseNotFoundException when merchant does not own case")
    void shouldEnforceTenantIsolation() {
        UUID otherMerchantId = UUID.randomUUID();
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, otherMerchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recoveryOrchestratorService.orchestrateRecovery(otherMerchantId, recoveryCaseId))
                .isInstanceOf(RecoveryCaseNotFoundException.class)
                .hasMessageContaining("Recovery case not found");

        verify(recoveryAttemptRepository, never()).save(any());
        verify(auditEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when merchantId or recoveryCaseId is null")
    void shouldThrowWhenNullIds() {
        assertThatThrownBy(() -> recoveryOrchestratorService.orchestrateRecovery(null, recoveryCaseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merchant ID cannot be null");

        assertThatThrownBy(() -> recoveryOrchestratorService.orchestrateRecovery(merchantId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recovery Case ID cannot be null");
    }

    @Test
    @DisplayName("Should reject orchestration if recovery case is in terminal status (RECOVERED, CANCELLED, EXPIRED)")
    void shouldRejectTerminalCaseStatuses() {
        for (RecoveryCaseStatus terminalStatus : List.of(RecoveryCaseStatus.RECOVERED, RecoveryCaseStatus.CANCELLED, RecoveryCaseStatus.EXPIRED)) {
            recoveryCase.setStatus(terminalStatus);
            when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));

            assertThatThrownBy(() -> recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId))
                    .isInstanceOf(InvalidRecoveryCaseStateException.class)
                    .hasMessageContaining("Cannot orchestrate recovery for case in terminal status: " + terminalStatus);

            verify(recoveryAttemptRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should reject duplicate orchestration when an attempt is already IN_FLIGHT or SCHEDULED")
    void shouldRejectDuplicateActiveAttempt() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId))
                .isInstanceOf(DuplicateOrchestrationException.class)
                .hasMessageContaining("An active recovery attempt is already scheduled or in-flight");

        verify(recoveryAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AgentDecisionNotFoundException when no prior AI diagnosis decision exists")
    void shouldThrowWhenAgentDecisionMissing() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId))
                .isInstanceOf(AgentDecisionNotFoundException.class)
                .hasMessageContaining("No AgentDecision found for recovery case");

        verify(recoveryAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject AgentDecision if merchant ID does not match")
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

        assertThatThrownBy(() -> recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId))
                .isInstanceOf(RecoveryCaseNotFoundException.class)
                .hasMessageContaining("AgentDecision merchant mismatch");

        verify(recoveryAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should transition case to RECOVERED if executor returns SUCCESS status")
    void shouldTransitionCaseToRecoveredOnSuccess() {
        agentDecision.setChannel(RecoveryChannel.RETRY_CHARGE);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        // Custom mock executor returning SUCCESS
        RecoveryActionExecutor successExecutor = new RecoveryActionExecutor() {
            @Override
            public boolean supports(RecoveryChannel channel) {
                return channel == RecoveryChannel.RETRY_CHARGE;
            }

            @Override
            public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase rc) {
                return ExecutionResult.success("RETRY_CAPTURED", "Payment re-charged successfully", null, "{}");
            }
        };

        RecoveryOrchestratorService customOrchestrator = new RecoveryOrchestratorService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                List.of(successExecutor, defaultActionExecutor),
                defaultActionExecutor,
                auditService
        );

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = customOrchestrator.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SUCCESS);
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.RECOVERED);
        assertThat(recoveryCase.getRecoveredAt()).isNotNull();
        verify(recoveryCaseRepository, atLeastOnce()).save(recoveryCase);
    }

    @Test
    @DisplayName("Should handle executor failure gracefully and mark attempt as FAILED")
    void shouldHandleExecutorFailureGracefully() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        // Custom mock executor throwing runtime exception
        RecoveryActionExecutor failingExecutor = new RecoveryActionExecutor() {
            @Override
            public boolean supports(RecoveryChannel channel) {
                return true;
            }

            @Override
            public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase rc) {
                throw new RuntimeException("Simulated provider outage");
            }
        };

        RecoveryOrchestratorService failingOrchestrator = new RecoveryOrchestratorService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                List.of(failingExecutor),
                defaultActionExecutor,
                auditService
        );

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = failingOrchestrator.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(response.getResultCode()).isEqualTo("EXECUTION_ERROR");
        assertThat(response.getResultMessage()).contains("Simulated provider outage");

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_FAILED");
    }
}
