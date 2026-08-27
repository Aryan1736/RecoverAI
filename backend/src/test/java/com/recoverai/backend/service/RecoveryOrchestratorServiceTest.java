package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
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
import com.recoverai.backend.service.executor.EmailRecoveryExecutor;
import com.recoverai.backend.service.executor.ExecutionResult;
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

import java.math.BigDecimal;
import java.time.Instant;
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

    @Mock
    private WhatsAppProvider whatsappProvider;

    @Mock
    private EmailProvider emailProvider;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private PaymentRetryProvider paymentRetryProvider;

    private RecoveryLinkService recoveryLinkService;
    private AuditService auditService;
    private DefaultRecoveryActionExecutor defaultActionExecutor;
    private WhatsAppRecoveryExecutor whatsAppExecutor;
    private EmailRecoveryExecutor emailExecutor;
    private SmsRecoveryExecutor smsExecutor;
    private SmartLinkRecoveryExecutor smartLinkExecutor;
    private RetryChargeRecoveryExecutor retryChargeExecutor;
    private ManualRecoveryExecutor manualExecutor;
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
        RecoveryCommunicationProperties properties = new RecoveryCommunicationProperties();
        properties.setBaseUrl("https://pay.recoverai.io/r/");
        recoveryLinkService = new DefaultRecoveryLinkService(properties);

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

        recoveryOrchestratorService = new RecoveryOrchestratorService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                executors,
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
    @DisplayName("Should successfully orchestrate WhatsApp recovery attempt and record RECOVERY_ATTEMPT_SENT audit event")
    void shouldSuccessfullyOrchestrateWhatsAppAttempt() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        when(whatsappProvider.sendWhatsApp(any()))
                .thenReturn(CommunicationDeliveryResult.success("wa_001", "MOCK_WHATSAPP", "WHATSAPP_DISPATCHED", "Delivered", "{}"));

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
        assertThat(response.getRecoveryLink()).isEqualTo("https://pay.recoverai.io/r/" + recoveryCaseId);

        // Verify case transitioned to IN_PROGRESS (not RECOVERED, because money is not collected yet)
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.IN_PROGRESS);
        verify(recoveryCaseRepository).save(recoveryCase);

        // Verify audit trail (created, started, sent)
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_CREATED", "RECOVERY_ATTEMPT_STARTED", "RECOVERY_ATTEMPT_SENT");
    }

    @Test
    @DisplayName("Should successfully orchestrate EMAIL recovery attempt")
    void shouldSuccessfullyOrchestrateEmailAttempt() {
        agentDecision.setChannel(RecoveryChannel.EMAIL);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        when(emailProvider.sendEmail(any()))
                .thenReturn(CommunicationDeliveryResult.success("email_001", "MOCK_EMAIL", "EMAIL_DISPATCHED", "Delivered", "{}"));

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.EMAIL);
        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(response.getResultCode()).isEqualTo("EMAIL_DISPATCHED");
    }

    @Test
    @DisplayName("Should successfully orchestrate SMS recovery attempt")
    void shouldSuccessfullyOrchestrateSmsAttempt() {
        agentDecision.setChannel(RecoveryChannel.SMS);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        when(smsProvider.sendSms(any()))
                .thenReturn(CommunicationDeliveryResult.success("sms_001", "MOCK_SMS", "SMS_DISPATCHED", "Delivered", "{}"));

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.SMS);
        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(response.getResultCode()).isEqualTo("SMS_DISPATCHED");
    }

    @Test
    @DisplayName("Should successfully orchestrate SMART_LINK recovery attempt")
    void shouldSuccessfullyOrchestrateSmartLinkAttempt() {
        agentDecision.setChannel(RecoveryChannel.SMART_LINK);

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

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.SMART_LINK);
        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(response.getResultCode()).isEqualTo("SMART_LINK_GENERATED");
        assertThat(response.getRecoveryLink()).isEqualTo("https://pay.recoverai.io/r/" + recoveryCaseId);
    }

    @Test
    @DisplayName("Should successfully orchestrate MANUAL recovery attempt")
    void shouldSuccessfullyOrchestrateManualAttempt() {
        agentDecision.setChannel(RecoveryChannel.MANUAL);

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

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getChannel()).isEqualTo(RecoveryChannel.MANUAL);
        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(response.getResultCode()).isEqualTo("MANUAL_REVIEW_QUEUED");
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
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SENT)
                .build();
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.of(previousAttempt));

        when(whatsappProvider.sendWhatsApp(any()))
                .thenReturn(CommunicationDeliveryResult.success("wa_002", "MOCK_WHATSAPP", "WHATSAPP_DISPATCHED", "Delivered", "{}"));

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
    @DisplayName("Should transition case to RECOVERED and record RECOVERY_ATTEMPT_SUCCEEDED when RETRY_CHARGE succeeds")
    void shouldTransitionCaseToRecoveredOnRetryChargeSuccess() {
        agentDecision.setChannel(RecoveryChannel.RETRY_CHARGE);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        when(paymentRetryProvider.retryCharge(any()))
                .thenReturn(PaymentRetryResult.success("mock_txn_123", "MOCK_RAZORPAY", "PAYMENT_RETRY_CAPTURED", "Payment re-charged successfully", "{}"));

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.SUCCESS);
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.RECOVERED);
        assertThat(recoveryCase.getRecoveredAt()).isNotNull();
        verify(recoveryCaseRepository, atLeastOnce()).save(recoveryCase);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_SUCCEEDED");
    }

    @Test
    @DisplayName("Should mark attempt as FAILED and record RECOVERY_ATTEMPT_FAILED when RETRY_CHARGE fails")
    void shouldHandleRetryChargeFailure() {
        agentDecision.setChannel(RecoveryChannel.RETRY_CHARGE);

        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        when(paymentRetryProvider.retryCharge(any()))
                .thenReturn(PaymentRetryResult.failure("mock_txn_123", "MOCK_RAZORPAY", "RETRY_DECLINED", "Card expired or insufficient funds", "{}"));

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(response.getResultCode()).isEqualTo("RETRY_DECLINED");
        // Case remains IN_PROGRESS so further attempts can be made
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.IN_PROGRESS);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_FAILED");
    }

    @Test
    @DisplayName("Should handle executor exception gracefully and mark attempt as FAILED")
    void shouldHandleExecutorExceptionGracefully() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(recoveryCaseId), anyCollection()))
                .thenReturn(false);
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId))
                .thenReturn(Optional.of(agentDecision));
        when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId))
                .thenReturn(Optional.empty());

        when(whatsappProvider.sendWhatsApp(any()))
                .thenThrow(new RuntimeException("Simulated provider outage"));

        when(recoveryAttemptRepository.save(any(RecoveryAttempt.class))).thenAnswer(invocation -> {
            RecoveryAttempt toSave = invocation.getArgument(0);
            if (toSave.getId() == null) {
                toSave.setId(UUID.randomUUID());
            }
            return toSave;
        });

        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);

        assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);
        assertThat(response.getResultCode()).isEqualTo("EXECUTION_ERROR");
        assertThat(response.getResultMessage()).contains("Simulated provider outage");

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_ATTEMPT_FAILED");
    }
}
