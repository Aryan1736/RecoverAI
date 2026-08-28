package com.recoverai.backend.service.strategy;

import com.recoverai.backend.config.RecoverySchedulerProperties;
import com.recoverai.backend.config.RecoveryStrategyProperties;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.strategy.RecoveryStrategySnapshot;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.NoViableStrategyException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.exception.StrategyExecutionDisabledException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryStrategyRepository;
import com.recoverai.backend.service.AuditService;
import com.recoverai.backend.service.RecoveryOrchestratorService;
import com.recoverai.backend.service.RecoverySchedulerService;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

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
class StrategyExecutionIntegrationTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private AgentDecisionRepository agentDecisionRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private RecoveryStrategyRepository recoveryStrategyRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private WhatsAppProvider whatsAppProvider;

    @Mock
    private EmailProvider emailProvider;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private PaymentRetryProvider paymentRetryProvider;

    private RecoveryStrategyProperties strategyProperties;
    private RecoverySchedulerProperties schedulerProperties;
    private RecoveryStrategyEngine strategyEngine;
    private RecoveryStrategyService strategyService;
    private AuditService auditService;
    private RecoveryLinkService linkService;
    private DefaultRecoveryActionExecutor defaultExecutor;
    private WhatsAppRecoveryExecutor whatsAppExecutor;
    private EmailRecoveryExecutor emailExecutor;
    private SmsRecoveryExecutor smsExecutor;
    private RetryChargeRecoveryExecutor retryChargeExecutor;
    private ManualRecoveryExecutor manualExecutor;
    private SmartLinkRecoveryExecutor smartLinkExecutor;

    private RecoveryOrchestratorService orchestratorService;
    private RecoverySchedulerService schedulerService;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private AgentDecision agentDecision;
    private UUID merchantId;
    private UUID caseId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        caseId = UUID.randomUUID();

        merchant = Merchant.builder()
                .id(merchantId)
                .name("Acme Corp")
                .email("admin@acme.com")
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .name("John Doe")
                .email("john@example.com")
                .phone("+919876543210")
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_123")
                .amount(new BigDecimal("2499.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .errorCode("BAD_REQUEST_ERROR")
                .errorDescription("Card issuer timeout")
                .riskLevel(RiskLevel.LOW)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(caseId)
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("2499.00"))
                .currency("INR")
                .failureReasonCategory("network_error")
                .build();

        agentDecision = AgentDecision.builder()
                .id(UUID.randomUUID())
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .confidenceScore(new BigDecimal("0.92"))
                .reasoning("High probability of mobile engagement")
                .build();

        auditService = new AuditService(auditEventRepository);
        strategyProperties = new RecoveryStrategyProperties();
        strategyProperties.setEnabled(true);
        strategyProperties.setExecutionEnabled(true);
        strategyProperties.setMaxAttempts(3);
        strategyProperties.setRetryChargeEnabled(true);
        strategyProperties.setFallbackEnabled(true);

        schedulerProperties = new RecoverySchedulerProperties();
        schedulerProperties.setEnabled(true);
        schedulerProperties.setBatchSize(50);

        strategyEngine = new RecoveryStrategyEngine(strategyProperties);
        strategyService = new RecoveryStrategyService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                recoveryStrategyRepository,
                strategyEngine,
                strategyProperties,
                auditService
        );

        com.recoverai.backend.config.RecoveryCommunicationProperties commProps =
                new com.recoverai.backend.config.RecoveryCommunicationProperties();
        commProps.setBaseUrl("https://pay.recoverai.io/r/");
        linkService = new DefaultRecoveryLinkService(commProps);

        defaultExecutor = new DefaultRecoveryActionExecutor(linkService);
        whatsAppExecutor = new WhatsAppRecoveryExecutor(whatsAppProvider, linkService);
        emailExecutor = new EmailRecoveryExecutor(emailProvider, linkService);
        smsExecutor = new SmsRecoveryExecutor(smsProvider, linkService);
        smartLinkExecutor = new SmartLinkRecoveryExecutor(linkService);
        retryChargeExecutor = new RetryChargeRecoveryExecutor(paymentRetryProvider);
        manualExecutor = new ManualRecoveryExecutor(linkService);

        List<RecoveryActionExecutor> executors = List.of(
                whatsAppExecutor,
                emailExecutor,
                smsExecutor,
                smartLinkExecutor,
                retryChargeExecutor,
                manualExecutor,
                defaultExecutor
        );

        orchestratorService = new RecoveryOrchestratorService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                recoveryStrategyRepository,
                strategyService,
                strategyProperties,
                executors,
                defaultExecutor,
                auditService
        );

        schedulerService = new RecoverySchedulerService(
                recoveryCaseRepository,
                agentDecisionRepository,
                recoveryAttemptRepository,
                recoveryStrategyRepository,
                strategyService,
                strategyProperties,
                executors,
                defaultExecutor,
                auditService,
                schedulerProperties
        );
    }

    @Nested
    @DisplayName("Strategy-Aware Orchestration Tests")
    class StrategyAwareOrchestrationTests {

        @Test
        @DisplayName("Should execute persisted strategy with snapshot and audit events")
        void shouldExecutePersistedStrategySuccessfully() {
            RecoveryStrategy strategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.WHATSAPP)
                    .recommendedAction("SEND_WHATSAPP_REMINDER")
                    .confidenceScore(new BigDecimal("0.9500"))
                    .priority(RecoveryPriority.HIGH)
                    .maxAttempts(3)
                    .reason("High confidence user channel")
                    .fallbackChannel(RecoveryChannel.EMAIL)
                    .fallbackAction("SEND_EMAIL_REMINDER")
                    .isTerminal(false)
                    .build();

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.of(strategy));
            when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                    .thenReturn(List.of());
            when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(caseId))
                    .thenReturn(Optional.empty());
            when(recoveryAttemptRepository.save(any(RecoveryAttempt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(whatsAppProvider.sendWhatsApp(any()))
                    .thenReturn(CommunicationDeliveryResult.success("wa_1", "MOCK_WHATSAPP", "WHATSAPP_DISPATCHED", "Delivered", "{}"));

            RecoveryAttemptResponseDto response = orchestratorService.orchestrateRecovery(merchantId, caseId);

            assertThat(response).isNotNull();
            assertThat(response.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
            assertThat(response.getStrategyId()).isEqualTo(strategy.getId());
            assertThat(response.getStrategySnapshot()).isNotNull();
            assertThat(response.getStrategySnapshot().getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
            assertThat(response.getStrategySnapshot().getFallbackChannel()).isEqualTo(RecoveryChannel.EMAIL);
            assertThat(response.getStrategySnapshot().getConfidenceScore()).isEqualTo(new BigDecimal("0.9500"));

            // Verify audit trail
            ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository, atLeastOnce()).save(eventCaptor.capture());
            List<String> eventTypes = eventCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();

            assertThat(eventTypes).contains(
                    "RECOVERY_ATTEMPT_CREATED",
                    "RECOVERY_ATTEMPT_STARTED",
                    "RECOVERY_STRATEGY_EXECUTION_STARTED",
                    "RECOVERY_ATTEMPT_SENT",
                    "RECOVERY_STRATEGY_EXECUTION_SUCCEEDED"
            );
        }

        @Test
        @DisplayName("Should generate fresh strategy when none exists and execute it")
        void shouldGenerateFreshStrategyWhenNoneExists() {
            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.empty());
            when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(caseId))
                    .thenReturn(Optional.of(agentDecision));
            when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                    .thenReturn(List.of());
            when(recoveryStrategyRepository.save(any(RecoveryStrategy.class)))
                    .thenAnswer(inv -> {
                        RecoveryStrategy s = inv.getArgument(0);
                        s.setId(UUID.randomUUID());
                        return s;
                    });
            when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(caseId))
                    .thenReturn(Optional.empty());
            when(recoveryAttemptRepository.save(any(RecoveryAttempt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(whatsAppProvider.sendWhatsApp(any()))
                    .thenReturn(CommunicationDeliveryResult.success("wa_1", "MOCK_WHATSAPP", "WHATSAPP_DISPATCHED", "Delivered", "{}"));

            RecoveryAttemptResponseDto response = orchestratorService.orchestrateRecovery(merchantId, caseId);

            assertThat(response).isNotNull();
            assertThat(response.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
            assertThat(response.getStrategyId()).isNotNull();
        }

        @Test
        @DisplayName("Should reject terminal strategy without creating attempt")
        void shouldRejectTerminalStrategy() {
            RecoveryStrategy terminalStrategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.MANUAL)
                    .recommendedAction("NO_ACTION_TERMINAL")
                    .maxAttempts(3)
                    .reason("Case is already expired")
                    .isTerminal(true)
                    .build();

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.of(terminalStrategy));

            assertThatThrownBy(() -> orchestratorService.orchestrateRecovery(merchantId, caseId))
                    .isInstanceOf(NoViableStrategyException.class)
                    .hasMessageContaining("Cannot orchestrate recovery: Case is already expired");

            verify(recoveryAttemptRepository, never()).save(any(RecoveryAttempt.class));

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues().stream().map(AuditEvent::getEventType))
                    .contains("RECOVERY_STRATEGY_EXECUTION_REJECTED");
        }

        @Test
        @DisplayName("Should reject execution when max attempts exceeded")
        void shouldRejectWhenMaxAttemptsExceeded() {
            RecoveryStrategy strategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.EMAIL)
                    .recommendedAction("SEND_EMAIL_REMINDER")
                    .maxAttempts(2)
                    .reason("Max 2 attempts allowed")
                    .isTerminal(false)
                    .build();

            RecoveryAttempt attempt1 = RecoveryAttempt.builder().attemptNumber(1).status(RecoveryAttemptStatus.FAILED).build();
            RecoveryAttempt attempt2 = RecoveryAttempt.builder().attemptNumber(2).status(RecoveryAttemptStatus.FAILED).build();

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.of(strategy));
            when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                    .thenReturn(List.of(attempt1, attempt2));

            assertThatThrownBy(() -> orchestratorService.orchestrateRecovery(merchantId, caseId))
                    .isInstanceOf(NoViableStrategyException.class)
                    .hasMessageContaining("maximum attempts exceeded");

            verify(recoveryAttemptRepository, never()).save(any(RecoveryAttempt.class));
        }

        @Test
        @DisplayName("Should regenerate strategy when persisted strategy channel is not viable")
        void shouldRegenerateWhenChannelNotViable() {
            // Customer has no phone, only email
            Customer emailOnlyCustomer = Customer.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .name("Bob")
                    .email("bob@example.com")
                    .phone(null)
                    .build();

            recoveryCase.setCustomer(emailOnlyCustomer);

            // Stale strategy points to WHATSAPP (unviable without phone)
            RecoveryStrategy unviableStrategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.WHATSAPP)
                    .recommendedAction("SEND_WHATSAPP")
                    .maxAttempts(3)
                    .reason("Stale WhatsApp strategy")
                    .isTerminal(false)
                    .build();

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.of(unviableStrategy));

            when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(caseId))
                    .thenReturn(Optional.of(agentDecision));
            when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                    .thenReturn(List.of());
            when(recoveryStrategyRepository.save(any(RecoveryStrategy.class)))
                    .thenAnswer(inv -> {
                        RecoveryStrategy s = inv.getArgument(0);
                        s.setId(UUID.randomUUID());
                        return s;
                    });
            when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(caseId))
                    .thenReturn(Optional.empty());
            when(recoveryAttemptRepository.save(any(RecoveryAttempt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(emailProvider.sendEmail(any()))
                    .thenReturn(CommunicationDeliveryResult.success("email_1", "MOCK_EMAIL", "EMAIL_DISPATCHED", "Delivered", "{}"));

            RecoveryAttemptResponseDto response = orchestratorService.orchestrateRecovery(merchantId, caseId);

            // Fresh strategy regenerated: EMAIL selected instead of WHATSAPP
            assertThat(response.getChannel()).isEqualTo(RecoveryChannel.EMAIL);
        }
    }

    @Nested
    @DisplayName("Fallback Execution & Audit Tests")
    class FallbackExecutionTests {

        @Test
        @DisplayName("Should record RECOVERY_STRATEGY_FALLBACK_SELECTED audit event when channel execution fails")
        void shouldRecordFallbackAuditEventOnFailure() {
            RecoveryStrategy strategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.WHATSAPP)
                    .recommendedAction("SEND_WHATSAPP_REMINDER")
                    .confidenceScore(new BigDecimal("0.9000"))
                    .priority(RecoveryPriority.HIGH)
                    .maxAttempts(3)
                    .reason("High priority recovery")
                    .fallbackChannel(RecoveryChannel.EMAIL)
                    .fallbackAction("SEND_EMAIL_REMINDER")
                    .isTerminal(false)
                    .build();

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.of(strategy));
            when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                    .thenReturn(List.of());
            when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(caseId))
                    .thenReturn(Optional.empty());
            when(recoveryAttemptRepository.save(any(RecoveryAttempt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // WhatsApp provider fails
            when(whatsAppProvider.sendWhatsApp(any()))
                    .thenReturn(CommunicationDeliveryResult.failure("wa_err", "MOCK_WHATSAPP", "FAILED", "Failed delivery", "{}"));

            RecoveryAttemptResponseDto response = orchestratorService.orchestrateRecovery(merchantId, caseId);

            assertThat(response.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository, atLeastOnce()).save(captor.capture());
            List<String> eventTypes = captor.getAllValues().stream().map(AuditEvent::getEventType).toList();

            assertThat(eventTypes).contains(
                    "RECOVERY_ATTEMPT_FAILED",
                    "RECOVERY_STRATEGY_EXECUTION_FAILED",
                    "RECOVERY_STRATEGY_FALLBACK_SELECTED"
            );
        }
    }

    @Nested
    @DisplayName("Strategy-Driven Scheduling Tests")
    class StrategyDrivenSchedulingTests {

        @Test
        @DisplayName("Should apply strategy delaySeconds to scheduledAt time")
        void shouldApplyStrategyDelaySeconds() {
            RecoveryStrategy strategyWithDelay = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.RETRY_CHARGE)
                    .recommendedAction("RETRY_CHARGE")
                    .delaySeconds(300)
                    .maxAttempts(3)
                    .reason("Auto retry scheduled")
                    .isTerminal(false)
                    .build();

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.of(strategyWithDelay));
            when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                    .thenReturn(List.of());
            when(recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(caseId))
                    .thenReturn(Optional.empty());
            when(recoveryAttemptRepository.save(any(RecoveryAttempt.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Instant before = Instant.now();
            RecoveryAttemptResponseDto response = schedulerService.scheduleRecovery(merchantId, caseId, null);
            Instant after = Instant.now();

            assertThat(response.getScheduledAt()).isAfterOrEqualTo(before.plusSeconds(299));
            assertThat(response.getScheduledAt()).isBeforeOrEqualTo(after.plusSeconds(301));
            assertThat(response.getChannel()).isEqualTo(RecoveryChannel.RETRY_CHARGE);
        }

        @Test
        @DisplayName("Background worker should claim and execute persisted strategy snapshot channel")
        void workerShouldExecutePersistedStrategyChannel() {
            UUID attemptId = UUID.randomUUID();
            RecoveryStrategy strategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.EMAIL)
                    .recommendedAction("SEND_EMAIL_REMINDER")
                    .build();

            RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.fromStrategy(strategy);

            RecoveryAttempt attempt = RecoveryAttempt.builder()
                    .id(attemptId)
                    .recoveryCase(recoveryCase)
                    .merchant(merchant)
                    .strategy(strategy)
                    .strategySnapshot(snapshot.toJson())
                    .attemptNumber(1)
                    .channel(RecoveryChannel.EMAIL)
                    .status(RecoveryAttemptStatus.SCHEDULED)
                    .build();

            when(recoveryAttemptRepository.claimAttemptForExecution(eq(attemptId), eq(RecoveryAttemptStatus.SCHEDULED), eq(RecoveryAttemptStatus.IN_FLIGHT), any()))
                    .thenReturn(1);
            when(recoveryAttemptRepository.findById(attemptId))
                    .thenReturn(Optional.of(attempt));
            when(emailProvider.sendEmail(any()))
                    .thenReturn(CommunicationDeliveryResult.success("email_1", "MOCK_EMAIL", "EMAIL_DISPATCHED", "Delivered", "{}"));

            boolean executed = schedulerService.claimAndExecuteAttempt(attemptId);

            assertThat(executed).isTrue();
            assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues().stream().map(AuditEvent::getEventType))
                    .contains("RECOVERY_STRATEGY_EXECUTION_STARTED", "RECOVERY_STRATEGY_EXECUTION_SUCCEEDED");
        }

        @Test
        @DisplayName("Concurrent worker claiming should only execute once")
        void concurrentWorkerClaimingShouldExecuteOnlyOnce() {
            UUID attemptId = UUID.randomUUID();
            // First worker gets claim count = 1, second worker gets claim count = 0
            when(recoveryAttemptRepository.claimAttemptForExecution(eq(attemptId), eq(RecoveryAttemptStatus.SCHEDULED), eq(RecoveryAttemptStatus.IN_FLIGHT), any()))
                    .thenReturn(0);

            boolean executed = schedulerService.claimAndExecuteAttempt(attemptId);
            assertThat(executed).isFalse();
            verify(recoveryAttemptRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Strategy Snapshot Safety Tests")
    class StrategySnapshotSafetyTests {

        @Test
        @DisplayName("Strategy snapshot must only store safe policy attributes without credentials or prompts")
        void shouldOnlyStoreSafePolicyAttributesInSnapshot() {
            RecoveryStrategy strategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .channel(RecoveryChannel.WHATSAPP)
                    .recommendedAction("SEND_WHATSAPP_REMINDER")
                    .confidenceScore(new BigDecimal("0.8800"))
                    .priority(RecoveryPriority.HIGH)
                    .fallbackChannel(RecoveryChannel.EMAIL)
                    .fallbackAction("SEND_EMAIL_REMINDER")
                    .reason("Safe policy reasoning based on user mobile affinity")
                    .build();

            RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.fromStrategy(strategy);
            String json = snapshot.toJson();

            assertThat(json).doesNotContain("apiKey", "api_key", "secret", "password", "token", "rawPrompt", "rawCompletion");
            assertThat(json).contains("strategyId", "WHATSAPP", "SEND_WHATSAPP_REMINDER", "0.8800", "HIGH", "EMAIL");

            RecoveryStrategySnapshot parsed = RecoveryStrategySnapshot.fromJson(json);
            assertThat(parsed).isNotNull();
            assertThat(parsed.getStrategyId()).isEqualTo(strategy.getId());
            assertThat(parsed.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
            assertThat(parsed.getFallbackChannel()).isEqualTo(RecoveryChannel.EMAIL);
            assertThat(parsed.getPriority()).isEqualTo(RecoveryPriority.HIGH);
        }
    }

    @Nested
    @DisplayName("Configuration & Security Multi-Tenant Tests")
    class ConfigurationAndSecurityTests {

        @Test
        @DisplayName("Should reject orchestration when execution is disabled by configuration")
        void shouldRejectWhenExecutionDisabled() {
            strategyProperties.setExecutionEnabled(false);

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);

            assertThatThrownBy(() -> orchestratorService.orchestrateRecovery(merchantId, caseId))
                    .isInstanceOf(StrategyExecutionDisabledException.class)
                    .hasMessageContaining("Recovery strategy execution is disabled by configuration");
        }

        @Test
        @DisplayName("Should reject scheduling when execution is disabled by configuration")
        void shouldRejectSchedulingWhenExecutionDisabled() {
            strategyProperties.setExecutionEnabled(false);

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);

            assertThatThrownBy(() -> schedulerService.scheduleRecovery(merchantId, caseId, null))
                    .isInstanceOf(StrategyExecutionDisabledException.class)
                    .hasMessageContaining("Recovery strategy execution is disabled by configuration");
        }

        @Test
        @DisplayName("Should reject cross-tenant orchestration when case belongs to another merchant")
        void shouldRejectCrossTenantCaseAccess() {
            UUID otherMerchantId = UUID.randomUUID();
            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, otherMerchantId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> orchestratorService.orchestrateRecovery(otherMerchantId, caseId))
                    .isInstanceOf(RecoveryCaseNotFoundException.class);
        }

        @Test
        @DisplayName("Should reject cross-tenant scheduling when case belongs to another merchant")
        void shouldRejectCrossTenantScheduling() {
            UUID otherMerchantId = UUID.randomUUID();
            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, otherMerchantId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> schedulerService.scheduleRecovery(otherMerchantId, caseId, null))
                    .isInstanceOf(RecoveryCaseNotFoundException.class);
        }

        @Test
        @DisplayName("Should reject cross-tenant strategy execution when strategy belongs to another merchant")
        void shouldRejectCrossTenantStrategyAccess() {
            Merchant otherMerchant = Merchant.builder().id(UUID.randomUUID()).name("Other").build();
            RecoveryStrategy crossTenantStrategy = RecoveryStrategy.builder()
                    .id(UUID.randomUUID())
                    .merchant(otherMerchant)
                    .recoveryCase(recoveryCase)
                    .channel(RecoveryChannel.EMAIL)
                    .recommendedAction("SEND_EMAIL")
                    .build();

            when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId))
                    .thenReturn(Optional.of(recoveryCase));
            when(recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(eq(caseId), anyCollection()))
                    .thenReturn(false);
            when(recoveryStrategyRepository.findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId))
                    .thenReturn(Optional.of(crossTenantStrategy));

            assertThatThrownBy(() -> orchestratorService.orchestrateRecovery(merchantId, caseId))
                    .isInstanceOf(RecoveryCaseNotFoundException.class)
                    .hasMessageContaining("merchant mismatch");
        }
    }
}
