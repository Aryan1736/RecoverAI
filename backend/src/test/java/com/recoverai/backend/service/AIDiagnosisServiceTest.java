package com.recoverai.backend.service;

import com.recoverai.backend.client.GeminiClient;
import com.recoverai.backend.dto.diagnosis.AgentDecisionResponseDto;
import com.recoverai.backend.dto.diagnosis.DiagnosisContext;
import com.recoverai.backend.dto.diagnosis.StructuredDiagnosisResponse;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.exception.DiagnosisValidationException;
import com.recoverai.backend.exception.GeminiApiException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIDiagnosisServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private AgentDecisionRepository agentDecisionRepository;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditService auditService;
    private AIDiagnosisService aiDiagnosisService;

    private Merchant merchant;
    private Payment payment;
    private Customer customer;
    private RecoveryCase recoveryCase;
    private UUID merchantId;
    private UUID recoveryCaseId;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditEventRepository);
        aiDiagnosisService = new AIDiagnosisService(
                recoveryCaseRepository,
                agentDecisionRepository,
                geminiClient,
                auditService
        );
        merchantId = UUID.randomUUID();
        recoveryCaseId = UUID.randomUUID();

        merchant = Merchant.builder()
                .id(merchantId)
                .name("Test Merchant")
                .email("merchant@test.com")
                .webhookSecret("test-secret")
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+919876543210")
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_abc123")
                .amount(new BigDecimal("2999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .errorCode("GATEWAY_ERROR")
                .errorDescription("Bank processing downtime")
                .errorSource("bank")
                .errorReason("bank_down")
                .riskLevel(RiskLevel.LOW)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(recoveryCaseId)
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("BANK_DOWNTIME")
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .currency("INR")
                .build();
    }

    @Test
    @DisplayName("Should successfully diagnose recovery case and persist AgentDecision with audit")
    void shouldSuccessfullyDiagnoseAndPersist() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));

        StructuredDiagnosisResponse diagnosisResponse = StructuredDiagnosisResponse.builder()
                .recommendedAction("RETRY_CHARGE")
                .recommendedChannel(RecoveryChannel.RETRY_CHARGE)
                .confidenceScore(new BigDecimal("0.9200"))
                .reasoning("Bank downtime is temporary. Auto-retry charge in 30 minutes.")
                .decisionFactors("{\"urgency\":\"HIGH\"}")
                .modelName("gemini-3.7-flash")
                .modelVersion("gemini-3.7-flash-001")
                .promptTokens(210)
                .completionTokens(45)
                .rawResponse("{\"recommendedAction\":\"RETRY_CHARGE\"}")
                .build();

        when(geminiClient.diagnose(any(DiagnosisContext.class))).thenReturn(diagnosisResponse);

        UUID decisionId = UUID.randomUUID();
        when(agentDecisionRepository.save(any(AgentDecision.class))).thenAnswer(invocation -> {
            AgentDecision toSave = invocation.getArgument(0);
            toSave.setId(decisionId);
            toSave.setCreatedAt(Instant.now());
            return toSave;
        });

        AgentDecisionResponseDto result = aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(decisionId);
        assertThat(result.getRecommendedAction()).isEqualTo("RETRY_CHARGE");
        assertThat(result.getChannel()).isEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(result.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.9200"));
        assertThat(result.getReasoning()).contains("Bank downtime is temporary");
        assertThat(result.getModelName()).isEqualTo("gemini-3.7-flash");
        assertThat(result.getPromptTokens()).isEqualTo(210);

        // Verify sanitized context masking
        ArgumentCaptor<DiagnosisContext> contextCaptor = ArgumentCaptor.forClass(DiagnosisContext.class);
        verify(geminiClient).diagnose(contextCaptor.capture());
        DiagnosisContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getCustomerIdentifier()).isEqualTo("j***@example.com");
        assertThat(capturedContext.getAmount()).isEqualByComparingTo(new BigDecimal("2999.00"));
        assertThat(capturedContext.getErrorCode()).isEqualTo("GATEWAY_ERROR");

        // Verify persistence
        verify(agentDecisionRepository).save(any(AgentDecision.class));

        // Verify audit event
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("Should enforce tenant isolation and reject case belonging to another merchant")
    void shouldEnforceTenantIsolation() {
        UUID otherMerchantId = UUID.randomUUID();
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, otherMerchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiDiagnosisService.diagnoseRecoveryCase(otherMerchantId, recoveryCaseId))
                .isInstanceOf(RecoveryCaseNotFoundException.class)
                .hasMessageContaining("Recovery case not found");

        verify(geminiClient, never()).diagnose(any());
        verify(agentDecisionRepository, never()).save(any());
        verify(auditEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when merchantId or recoveryCaseId is null")
    void shouldThrowWhenNullArguments() {
        assertThatThrownBy(() -> aiDiagnosisService.diagnoseRecoveryCase(null, recoveryCaseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merchant ID cannot be null");

        assertThatThrownBy(() -> aiDiagnosisService.diagnoseRecoveryCase(merchantId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recovery Case ID cannot be null");
    }

    @Test
    @DisplayName("Should throw DiagnosisValidationException when AI returns confidence score > 1.0")
    void shouldRejectInvalidConfidenceScoreAboveOne() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));

        StructuredDiagnosisResponse badResponse = StructuredDiagnosisResponse.builder()
                .recommendedAction("RETRY")
                .recommendedChannel(RecoveryChannel.EMAIL)
                .confidenceScore(new BigDecimal("1.05"))
                .reasoning("Overconfident")
                .build();

        when(geminiClient.diagnose(any())).thenReturn(badResponse);

        assertThatThrownBy(() -> aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("Confidence score must be between 0.0 and 1.0");

        verify(agentDecisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DiagnosisValidationException when AI returns confidence score < 0.0")
    void shouldRejectInvalidConfidenceScoreBelowZero() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));

        StructuredDiagnosisResponse badResponse = StructuredDiagnosisResponse.builder()
                .recommendedAction("RETRY")
                .recommendedChannel(RecoveryChannel.EMAIL)
                .confidenceScore(new BigDecimal("-0.1"))
                .reasoning("Negative confidence")
                .build();

        when(geminiClient.diagnose(any())).thenReturn(badResponse);

        assertThatThrownBy(() -> aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("Confidence score must be between 0.0 and 1.0");

        verify(agentDecisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should propagate GeminiApiException when Gemini client fails")
    void shouldPropagateGeminiApiException() {
        when(recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId))
                .thenReturn(Optional.of(recoveryCase));

        when(geminiClient.diagnose(any())).thenThrow(new GeminiApiException("Gemini quota exceeded", 429));

        assertThatThrownBy(() -> aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("Gemini quota exceeded");

        verify(agentDecisionRepository, never()).save(any());
    }
}
