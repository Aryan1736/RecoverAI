package com.recoverai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.dto.webhook.RecoveryOutcomeWebhookRequest;
import com.recoverai.backend.dto.webhook.WebhookResponse;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryOutcomeEvent;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.WebhookProcessingStatus;
import com.recoverai.backend.exception.MerchantResolutionException;
import com.recoverai.backend.exception.RecoveryAttemptNotFoundException;
import com.recoverai.backend.exception.WebhookProcessingException;
import com.recoverai.backend.exception.WebhookSignatureException;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryOutcomeEventRepository;
import com.recoverai.backend.security.RecoveryOutcomeSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecoveryOutcomeService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryOutcomeService.class);

    private final MerchantRepository merchantRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final PaymentRepository paymentRepository;
    private final RecoveryOutcomeEventRepository recoveryOutcomeEventRepository;
    private final RecoveryOutcomeSignatureVerifier signatureVerifier;
    private final RecoveryAttemptStateMachine stateMachine;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final PaymentReconciliationService paymentReconciliationService;

    @Autowired
    public RecoveryOutcomeService(MerchantRepository merchantRepository,
                                  RecoveryAttemptRepository recoveryAttemptRepository,
                                  RecoveryCaseRepository recoveryCaseRepository,
                                  PaymentRepository paymentRepository,
                                  RecoveryOutcomeEventRepository recoveryOutcomeEventRepository,
                                  RecoveryOutcomeSignatureVerifier signatureVerifier,
                                  RecoveryAttemptStateMachine stateMachine,
                                  AuditService auditService,
                                  ObjectMapper objectMapper,
                                  PaymentReconciliationService paymentReconciliationService) {
        this.merchantRepository = merchantRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.paymentRepository = paymentRepository;
        this.recoveryOutcomeEventRepository = recoveryOutcomeEventRepository;
        this.signatureVerifier = signatureVerifier;
        this.stateMachine = stateMachine;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.paymentReconciliationService = paymentReconciliationService;
    }

    public RecoveryOutcomeService(MerchantRepository merchantRepository,
                                  RecoveryAttemptRepository recoveryAttemptRepository,
                                  RecoveryCaseRepository recoveryCaseRepository,
                                  PaymentRepository paymentRepository,
                                  RecoveryOutcomeEventRepository recoveryOutcomeEventRepository,
                                  RecoveryOutcomeSignatureVerifier signatureVerifier,
                                  RecoveryAttemptStateMachine stateMachine,
                                  AuditService auditService,
                                  ObjectMapper objectMapper) {
        this(merchantRepository, recoveryAttemptRepository, recoveryCaseRepository, paymentRepository,
                recoveryOutcomeEventRepository, signatureVerifier, stateMachine, auditService, objectMapper, null);
    }

    /**
     * Reconciles an asynchronous provider outcome for a RecoveryAttempt with HMAC signature verification,
     * tenant-scoped authorization, durable idempotency, and state machine validation.
     */
    @Transactional
    public WebhookResponse processOutcomeWebhook(String rawPayload, String signatureHeader, String clientIp) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new WebhookProcessingException("Webhook payload cannot be empty");
        }

        // 1. Parse JSON payload
        RecoveryOutcomeWebhookRequest request;
        try {
            request = objectMapper.readValue(rawPayload, RecoveryOutcomeWebhookRequest.class);
        } catch (Exception e) {
            log.warn("Failed to parse recovery outcome JSON payload from IP {}: {}", clientIp, e.getMessage());
            auditService.recordEvent(null, "RECOVERY_OUTCOME_REJECTED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                    "WEBHOOK", "UNKNOWN", "REJECTED", "Malformed JSON payload: " + e.getMessage(), clientIp);
            throw new WebhookProcessingException("Invalid or malformed JSON payload: " + e.getMessage(), e);
        }

        // Validate essential fields
        validateRequestFields(request);

        // 2. Calculate SHA-256 hash of payload
        String payloadHash = calculateSha256(rawPayload);

        // 3. Resolve Merchant
        UUID merchantId = request.getMerchantId();
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> {
                    log.warn("Recovery outcome rejected: unknown merchantId '{}' from IP {}", merchantId, clientIp);
                    auditService.recordEvent(null, "RECOVERY_OUTCOME_REJECTED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                            "MERCHANT", merchantId.toString(), "REJECTED", "Merchant not found", clientIp);
                    return new MerchantResolutionException("Merchant not found for id: " + merchantId);
                });

        if (merchant.getStatus() != MerchantStatus.ACTIVE) {
            log.warn("Recovery outcome rejected: merchant '{}' is not ACTIVE (status: {})", merchantId, merchant.getStatus());
            auditService.recordEvent(merchant, "RECOVERY_OUTCOME_REJECTED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                    "MERCHANT", merchantId.toString(), "REJECTED", "Merchant is inactive or suspended", clientIp);
            throw new MerchantResolutionException("Merchant account is inactive or suspended");
        }

        // 4. Verify HMAC-SHA256 signature
        try {
            signatureVerifier.verifySignature(rawPayload, signatureHeader, merchant.getWebhookSecret());
        } catch (WebhookSignatureException e) {
            auditService.recordEvent(merchant, "RECOVERY_OUTCOME_REJECTED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                    "WEBHOOK", request.getProviderEventId(), "SIGNATURE_VERIFICATION_FAILED", e.getMessage(), clientIp);
            throw e;
        }

        // Record successful receipt
        auditService.recordEvent(merchant, "RECOVERY_OUTCOME_RECEIVED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                "RecoveryAttempt", request.getRecoveryAttemptId().toString(), "RECEIVED",
                String.format("{\"provider\":\"%s\",\"eventId\":\"%s\",\"outcomeStatus\":\"%s\"}",
                        request.getProvider(), request.getProviderEventId(), request.getOutcomeStatus()), clientIp);

        // 5. Idempotency Check
        Optional<RecoveryOutcomeEvent> existingEvent = recoveryOutcomeEventRepository
                .findByMerchantIdAndProviderAndProviderEventId(merchant.getId(), request.getProvider(), request.getProviderEventId());
        if (existingEvent.isEmpty()) {
            existingEvent = recoveryOutcomeEventRepository.findByMerchantIdAndPayloadHash(merchant.getId(), payloadHash);
        }

        if (existingEvent.isPresent() && existingEvent.get().getProcessingStatus() == WebhookProcessingStatus.PROCESSED) {
            log.info("Recovery outcome event already processed (idempotency hit). Merchant: {}, Event: {}, Attempt: {}",
                    merchant.getId(), request.getProviderEventId(), request.getRecoveryAttemptId());
            auditService.recordEvent(merchant, "RECOVERY_OUTCOME_DUPLICATE", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                    "RecoveryAttempt", request.getRecoveryAttemptId().toString(), "DUPLICATE_SKIPPED",
                    "Duplicate recovery outcome delivery skipped", clientIp);
            return WebhookResponse.accepted("Duplicate recovery outcome event already processed");
        }

        // 6. Merchant-scoped RecoveryAttempt Lookup (Strict Multi-tenant Isolation)
        RecoveryAttempt attempt = recoveryAttemptRepository.findByIdAndMerchantId(request.getRecoveryAttemptId(), merchant.getId())
                .orElseThrow(() -> {
                    log.warn("Recovery outcome rejected: RecoveryAttempt '{}' not found for merchant '{}'",
                            request.getRecoveryAttemptId(), merchant.getId());
                    auditService.recordEvent(merchant, "RECOVERY_OUTCOME_REJECTED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                            "RecoveryAttempt", request.getRecoveryAttemptId().toString(), "REJECTED",
                            "Recovery attempt not found for merchant", clientIp);
                    return new RecoveryAttemptNotFoundException(String.format(
                            "Recovery attempt not found with id: %s for merchant: %s",
                            request.getRecoveryAttemptId(), merchant.getId()));
                });

        RecoveryCase recoveryCase = attempt.getRecoveryCase();
        if (!recoveryCase.getMerchant().getId().equals(merchant.getId())) {
            log.warn("Tenant violation: RecoveryCase '{}' does not belong to merchant '{}'",
                    recoveryCase.getId(), merchant.getId());
            throw new RecoveryAttemptNotFoundException("Recovery attempt not found for merchant: " + merchant.getId());
        }

        // 7. Concurrency Protection & Event Record Persistence
        RecoveryOutcomeEvent outcomeEvent = existingEvent.orElseGet(() -> RecoveryOutcomeEvent.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .provider(request.getProvider())
                .providerEventId(request.getProviderEventId())
                .payloadHash(payloadHash)
                .processingStatus(WebhookProcessingStatus.PENDING)
                .build());

        try {
            outcomeEvent = recoveryOutcomeEventRepository.saveAndFlush(outcomeEvent);
        } catch (DataIntegrityViolationException dive) {
            log.info("Concurrent duplicate outcome event detected via DB uniqueness. Merchant: {}, Event: {}",
                    merchant.getId(), request.getProviderEventId());
            auditService.recordEvent(merchant, "RECOVERY_OUTCOME_DUPLICATE", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                    "RecoveryAttempt", request.getRecoveryAttemptId().toString(), "DUPLICATE_SKIPPED",
                    "Concurrent duplicate event skipped", clientIp);
            return WebhookResponse.accepted("Duplicate recovery outcome event already processed");
        }

        // 8. State Machine Transition Validation
        RecoveryAttemptStatus currentStatus = attempt.getStatus();
        RecoveryAttemptStatus targetStatus = request.getOutcomeStatus();
        stateMachine.validateTransition(currentStatus, targetStatus);

        // 9. Reconcile Attempt & Recovery Case
        try {
            Instant eventTime = request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now();

            attempt.setStatus(targetStatus);
            if (request.getResultCode() != null) {
                attempt.setResultCode(request.getResultCode());
            }
            if (request.getResultMessage() != null) {
                attempt.setResultMessage(request.getResultMessage());
            }
            if (request.getMetadata() != null) {
                attempt.setMetadata(request.getMetadata());
            }

            if (targetStatus == RecoveryAttemptStatus.SUCCESS || targetStatus == RecoveryAttemptStatus.FAILED) {
                attempt.setCompletedAt(eventTime);
            }

            recoveryAttemptRepository.save(attempt);

            // Reconcile RecoveryCase based on outcome
            if (targetStatus == RecoveryAttemptStatus.SUCCESS) {
                if (paymentReconciliationService != null) {
                    paymentReconciliationService.reconcileCaseRecovery(
                            merchant,
                            recoveryCase,
                            recoveryCase.getPayment(),
                            recoveryCase.getEstimatedRecoverableAmount(),
                            "RECOVERY_OUTCOME:" + request.getProvider(),
                            clientIp,
                            attempt
                    );
                } else {
                    recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
                    recoveryCase.setRecoveredAt(eventTime);
                    // Trusted amount derived from existing trusted case data
                    recoveryCase.setRecoveredAmount(recoveryCase.getEstimatedRecoverableAmount());
                    recoveryCaseRepository.save(recoveryCase);

                    // Update associated payment status if present
                    Payment payment = recoveryCase.getPayment();
                    if (payment != null) {
                        payment.setStatus(PaymentStatus.CAPTURED);
                        paymentRepository.save(payment);
                    }
                }

                auditService.recordEvent(merchant, "RECOVERY_ATTEMPT_SUCCEEDED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                        "RecoveryAttempt", attempt.getId().toString(), "SUCCESS",
                        String.format("Recovery succeeded via %s. Case marked as RECOVERED.", request.getProvider()), clientIp);

            } else if (targetStatus == RecoveryAttemptStatus.FAILED) {
                // Keep case IN_PROGRESS for potential subsequent retry attempts
                if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
                    recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
                    recoveryCaseRepository.save(recoveryCase);
                }

                auditService.recordEvent(merchant, "RECOVERY_ATTEMPT_FAILED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                        "RecoveryAttempt", attempt.getId().toString(), "FAILED",
                        String.format("Attempt failed via %s: %s", request.getProvider(), request.getResultMessage()), clientIp);

            } else {
                // Intermediate states: SENT, DELIVERED, CLICKED, IN_FLIGHT -> case remains IN_PROGRESS
                if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
                    recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
                    recoveryCaseRepository.save(recoveryCase);
                }

                auditService.recordEvent(merchant, "RECOVERY_ATTEMPT_STATUS_UPDATED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                        "RecoveryAttempt", attempt.getId().toString(), "STATUS_UPDATE",
                        String.format("Attempt transitioned to %s via %s", targetStatus, request.getProvider()), clientIp);
            }

            // 10. Mark event as PROCESSED
            outcomeEvent.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
            outcomeEvent.setProcessedAt(Instant.now());
            recoveryOutcomeEventRepository.save(outcomeEvent);

            auditService.recordEvent(merchant, "RECOVERY_OUTCOME_PROCESSED", ActorType.WEBHOOK, "RecoveryOutcomeWebhook",
                    "RecoveryAttempt", attempt.getId().toString(), "PROCESSED",
                    String.format("Successfully processed outcome for event %s", request.getProviderEventId()), clientIp);

            return WebhookResponse.accepted();

        } catch (Exception e) {
            log.error("Failed to reconcile recovery outcome for attempt {}: {}", attempt.getId(), e.getMessage(), e);
            outcomeEvent.setProcessingStatus(WebhookProcessingStatus.FAILED);
            outcomeEvent.setErrorMessage(e.getMessage());
            outcomeEvent.setProcessedAt(Instant.now());
            recoveryOutcomeEventRepository.save(outcomeEvent);
            throw e;
        }
    }

    private void validateRequestFields(RecoveryOutcomeWebhookRequest request) {
        if (request.getProviderEventId() == null || request.getProviderEventId().isBlank()) {
            throw new WebhookProcessingException("Provider event ID cannot be blank");
        }
        if (request.getMerchantId() == null) {
            throw new WebhookProcessingException("Merchant ID cannot be null");
        }
        if (request.getRecoveryAttemptId() == null) {
            throw new WebhookProcessingException("Recovery attempt ID cannot be null");
        }
        if (request.getOutcomeStatus() == null) {
            throw new WebhookProcessingException("Outcome status cannot be null");
        }
        if (request.getProvider() == null || request.getProvider().isBlank()) {
            throw new WebhookProcessingException("Provider cannot be blank");
        }
    }

    private String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
