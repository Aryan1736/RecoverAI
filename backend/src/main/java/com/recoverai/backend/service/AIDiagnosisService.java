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
import com.recoverai.backend.exception.DiagnosisValidationException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AIDiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(AIDiagnosisService.class);

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final AgentDecisionRepository agentDecisionRepository;
    private final GeminiClient geminiClient;
    private final AuditService auditService;

    public AIDiagnosisService(RecoveryCaseRepository recoveryCaseRepository,
                              AgentDecisionRepository agentDecisionRepository,
                              GeminiClient geminiClient,
                              AuditService auditService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.geminiClient = geminiClient;
        this.auditService = auditService;
    }

    @Transactional
    public AgentDecisionResponseDto diagnoseRecoveryCase(UUID merchantId, UUID recoveryCaseId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (recoveryCaseId == null) {
            throw new IllegalArgumentException("Recovery Case ID cannot be null");
        }

        // Multi-tenant check: load recovery case strictly scoped to merchant
        RecoveryCase recoveryCase = recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId)
                .orElseThrow(() -> new RecoveryCaseNotFoundException(
                        "Recovery case not found with id: " + recoveryCaseId + " for merchant: " + merchantId));

        Merchant merchant = recoveryCase.getMerchant();
        Payment payment = recoveryCase.getPayment();
        Customer customer = recoveryCase.getCustomer();

        log.info("Starting AI diagnosis for recoveryCaseId={}, merchantId={}", recoveryCaseId, merchantId);

        // Build sanitized diagnosis context
        DiagnosisContext context = buildDiagnosisContext(recoveryCase, payment, customer, merchant);

        // Invoke Gemini Client
        StructuredDiagnosisResponse diagnosisResponse = geminiClient.diagnose(context);

        // Validate AI response invariants
        validateDiagnosisResponse(diagnosisResponse);

        // Map and persist AgentDecision
        AgentDecision agentDecision = AgentDecision.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .recommendedAction(diagnosisResponse.getRecommendedAction())
                .channel(diagnosisResponse.getRecommendedChannel())
                .confidenceScore(diagnosisResponse.getConfidenceScore())
                .reasoning(diagnosisResponse.getReasoning())
                .modelName(diagnosisResponse.getModelName() != null ? diagnosisResponse.getModelName() : "gemini-3.7-flash")
                .modelVersion(diagnosisResponse.getModelVersion())
                .promptTokens(diagnosisResponse.getPromptTokens())
                .completionTokens(diagnosisResponse.getCompletionTokens())
                .rawResponse(diagnosisResponse.getRawResponse())
                .decisionFactors(diagnosisResponse.getDecisionFactors())
                .build();

        AgentDecision savedDecision = agentDecisionRepository.save(agentDecision);
        log.info("Saved AI AgentDecision id={} for recoveryCaseId={}", savedDecision.getId(), recoveryCaseId);

        // Audit Trail
        String auditDetails = String.format("AI Diagnosis generated. Action=%s, Channel=%s, Confidence=%s, Model=%s",
                savedDecision.getRecommendedAction(),
                savedDecision.getChannel(),
                savedDecision.getConfidenceScore(),
                savedDecision.getModelName());

        auditService.recordEvent(
                merchant,
                "AGENT_DECISION_GENERATED",
                ActorType.AGENT,
                "Gemini-AI",
                "AgentDecision",
                savedDecision.getId().toString(),
                "GENERATE_DIAGNOSIS",
                auditDetails,
                null
        );

        return AgentDecisionResponseDto.fromEntity(savedDecision);
    }

    private DiagnosisContext buildDiagnosisContext(RecoveryCase recoveryCase, Payment payment, Customer customer, Merchant merchant) {
        String customerIdentifier = "ANONYMOUS";
        if (customer != null) {
            if (customer.getEmail() != null) {
                customerIdentifier = maskEmail(customer.getEmail());
            } else if (customer.getId() != null) {
                customerIdentifier = "Cust-" + customer.getId().toString().substring(0, 8);
            }
        }

        return DiagnosisContext.builder()
                .recoveryCaseId(recoveryCase.getId())
                .merchantId(merchant.getId())
                .merchantName(merchant.getName())
                .paymentId(payment != null ? payment.getId() : null)
                .razorpayPaymentId(payment != null ? payment.getRazorpayPaymentId() : null)
                .amount(payment != null ? payment.getAmount() : recoveryCase.getEstimatedRecoverableAmount())
                .currency(recoveryCase.getCurrency())
                .paymentMethod(payment != null && payment.getMethod() != null ? payment.getMethod().name() : null)
                .paymentStatus(payment != null && payment.getStatus() != null ? payment.getStatus().name() : null)
                .errorCode(payment != null ? payment.getErrorCode() : null)
                .errorDescription(payment != null ? payment.getErrorDescription() : null)
                .errorSource(payment != null ? payment.getErrorSource() : null)
                .errorReason(payment != null ? payment.getErrorReason() : null)
                .riskLevel(payment != null && payment.getRiskLevel() != null ? payment.getRiskLevel().name() : null)
                .failureReasonCategory(recoveryCase.getFailureReasonCategory())
                .estimatedRecoverableAmount(recoveryCase.getEstimatedRecoverableAmount())
                .recoveryPriority(recoveryCase.getPriority() != null ? recoveryCase.getPriority().name() : null)
                .recoveryCaseStatus(recoveryCase.getStatus() != null ? recoveryCase.getStatus().name() : null)
                .customerIdentifier(customerIdentifier)
                .build();
    }

    private void validateDiagnosisResponse(StructuredDiagnosisResponse response) {
        if (response == null) {
            throw new DiagnosisValidationException("Diagnosis response cannot be null");
        }
        if (response.getRecommendedAction() == null || response.getRecommendedAction().trim().isEmpty()) {
            throw new DiagnosisValidationException("Recommended action is required in diagnosis response");
        }
        if (response.getReasoning() == null || response.getReasoning().trim().isEmpty()) {
            throw new DiagnosisValidationException("Reasoning is required in diagnosis response");
        }
        BigDecimal score = response.getConfidenceScore();
        if (score == null) {
            throw new DiagnosisValidationException("Confidence score cannot be null");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new DiagnosisValidationException("Confidence score must be between 0.0 and 1.0, got: " + score);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "ANONYMOUS";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*@" + email.substring(atIndex + 1);
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }
}
