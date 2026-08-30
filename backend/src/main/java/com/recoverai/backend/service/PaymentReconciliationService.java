package com.recoverai.backend.service;

import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for closed-loop payment reconciliation.
 * Reconciles successful payments (from Razorpay webhooks or recovery outcomes)
 * with RecoveryCase, RecoveryAttempt, and RecoveryExecutionQueueItem entities.
 */
@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryExecutionQueueRepository queueRepository;
    private final PaymentRepository paymentRepository;
    private final RecoveryAttemptStateMachine stateMachine;
    private final AuditService auditService;

    public PaymentReconciliationService(RecoveryCaseRepository recoveryCaseRepository,
                                        RecoveryAttemptRepository recoveryAttemptRepository,
                                        RecoveryExecutionQueueRepository queueRepository,
                                        PaymentRepository paymentRepository,
                                        RecoveryAttemptStateMachine stateMachine,
                                        AuditService auditService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.queueRepository = queueRepository;
        this.paymentRepository = paymentRepository;
        this.stateMachine = stateMachine;
        this.auditService = auditService;
    }

    /**
     * Reconciles a successful payment event with any associated active RecoveryCase.
     * Uses primary direct payment matching followed by order-based matching for recovery link attempts.
     *
     * @param merchant    the authenticated merchant
     * @param payment     the persisted payment entity in CAPTURED status
     * @param sourceEvent the webhook event type (e.g. "payment.captured", "order.paid")
     * @param clientIp    the client IP address
     * @return the reconciled RecoveryCase if matched, or empty if no active recovery case matched
     */
    @Transactional
    public Optional<RecoveryCase> reconcilePaymentSuccess(Merchant merchant, Payment payment, String sourceEvent, String clientIp) {
        Objects.requireNonNull(merchant, "Merchant cannot be null");
        Objects.requireNonNull(payment, "Payment cannot be null");

        UUID merchantId = merchant.getId();
        log.info("Starting payment recovery reconciliation for merchantId={}, paymentId={}, razorpayPaymentId={}, orderId={}, sourceEvent={}",
                merchantId, payment.getId(), payment.getRazorpayPaymentId(), payment.getRazorpayOrderId(), sourceEvent);

        // 1. Primary match: Direct payment association
        Optional<RecoveryCase> directCaseOpt = recoveryCaseRepository.findByPaymentIdAndMerchantId(payment.getId(), merchantId);
        if (directCaseOpt.isPresent()) {
            RecoveryCase directCase = directCaseOpt.get();
            if (isCaseActive(directCase)) {
                log.info("Direct payment match found active RecoveryCase id={} for payment id={}", directCase.getId(), payment.getId());
                return Optional.of(reconcileCaseRecovery(merchant, directCase, payment, payment.getAmount(), sourceEvent, clientIp, null));
            } else if (directCase.getStatus() == RecoveryCaseStatus.RECOVERED) {
                log.info("RecoveryCase id={} is already RECOVERED (idempotency hit). Skipping duplicate transition.", directCase.getId());
                return Optional.of(directCase);
            } else {
                log.info("RecoveryCase id={} is in non-recoverable terminal status {}. Skipping reconciliation.", directCase.getId(), directCase.getStatus());
                return Optional.of(directCase);
            }
        }

        // 2. Order-based match (recovery link new payment attempt on existing order)
        String orderId = payment.getRazorpayOrderId();
        if (orderId != null && !orderId.isBlank()) {
            List<RecoveryCase> activeCases = recoveryCaseRepository.findActiveByMerchantIdAndRazorpayOrderId(
                    merchantId, orderId, List.of(RecoveryCaseStatus.OPEN, RecoveryCaseStatus.IN_PROGRESS));

            if (!activeCases.isEmpty()) {
                RecoveryCase orderCase = activeCases.get(0);
                log.info("Order-based match found active RecoveryCase id={} for razorpayOrderId={} and payment id={}",
                        orderCase.getId(), orderId, payment.getId());
                return Optional.of(reconcileCaseRecovery(merchant, orderCase, payment, payment.getAmount(), sourceEvent, clientIp, null));
            }

            // Check if already recovered under this order
            List<RecoveryCase> allCasesForOrder = recoveryCaseRepository.findByMerchantIdAndRazorpayOrderId(merchantId, orderId);
            if (!allCasesForOrder.isEmpty()) {
                RecoveryCase existingCase = allCasesForOrder.get(0);
                if (existingCase.getStatus() == RecoveryCaseStatus.RECOVERED) {
                    log.info("RecoveryCase id={} for orderId={} is already RECOVERED (idempotency hit). Skipping duplicate transition.",
                            existingCase.getId(), orderId);
                    return Optional.of(existingCase);
                } else {
                    log.info("RecoveryCase id={} for orderId={} is terminal ({}). Skipping reconciliation.",
                            existingCase.getId(), orderId, existingCase.getStatus());
                    return Optional.of(existingCase);
                }
            }
        }

        log.debug("No active RecoveryCase associated with payment id={}, razorpayPaymentId={}, orderId={}. Standard payment flow.",
                payment.getId(), payment.getRazorpayPaymentId(), orderId);
        return Optional.empty();
    }

    /**
     * Atomically transitions a RecoveryCase to RECOVERED, resolves active RecoveryAttempts according
     * to the RecoveryAttemptStateMachine, and completes pending RecoveryExecutionQueueItems.
     *
     * @param merchant       the merchant owning the case
     * @param recoveryCase   the recovery case to reconcile
     * @param payment        the successful payment entity (may be null if triggered by provider outcome)
     * @param trustedAmount  trusted recovered amount
     * @param sourceEvent    source event or provider name
     * @param clientIp       client IP address
     * @param sourceAttempt  the attempt that initiated the outcome, if applicable
     * @return the reconciled RecoveryCase
     */
    @Transactional
    public RecoveryCase reconcileCaseRecovery(Merchant merchant,
                                             RecoveryCase recoveryCase,
                                             Payment payment,
                                             BigDecimal trustedAmount,
                                             String sourceEvent,
                                             String clientIp,
                                             RecoveryAttempt sourceAttempt) {
        Objects.requireNonNull(merchant, "Merchant cannot be null");
        Objects.requireNonNull(recoveryCase, "RecoveryCase cannot be null");

        // Enforce strict merchant isolation
        if (!recoveryCase.getMerchant().getId().equals(merchant.getId())) {
            log.error("Cross-tenant violation: case merchant {} does not match authenticated merchant {}",
                    recoveryCase.getMerchant().getId(), merchant.getId());
            throw new SecurityException("Merchant cannot reconcile recovery case belonging to another tenant");
        }

        // Idempotency check: if case is already RECOVERED, do not re-transition or re-audit
        if (recoveryCase.getStatus() == RecoveryCaseStatus.RECOVERED) {
            log.info("RecoveryCase id={} is already RECOVERED. Idempotent no-op.", recoveryCase.getId());
            return recoveryCase;
        }

        if (recoveryCase.getStatus() == RecoveryCaseStatus.CANCELLED || recoveryCase.getStatus() == RecoveryCaseStatus.EXPIRED) {
            log.info("RecoveryCase id={} is in terminal state {}. Cannot transition to RECOVERED.",
                    recoveryCase.getId(), recoveryCase.getStatus());
            return recoveryCase;
        }

        Instant now = Instant.now();

        // 1. Reconcile RecoveryCase status and trusted amount
        BigDecimal effectiveRecoveredAmount;
        if (trustedAmount != null && trustedAmount.compareTo(BigDecimal.ZERO) > 0) {
            effectiveRecoveredAmount = trustedAmount;
        } else if (payment != null && payment.getAmount() != null && payment.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            effectiveRecoveredAmount = payment.getAmount();
        } else {
            effectiveRecoveredAmount = recoveryCase.getEstimatedRecoverableAmount() != null
                    ? recoveryCase.getEstimatedRecoverableAmount()
                    : BigDecimal.ZERO;
        }

        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
        recoveryCase.setRecoveredAt(now);
        recoveryCase.setRecoveredAmount(effectiveRecoveredAmount);
        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        // Update payment status to CAPTURED if associated and not yet updated
        if (payment != null && payment.getStatus() != PaymentStatus.CAPTURED) {
            payment.setStatus(PaymentStatus.CAPTURED);
            paymentRepository.save(payment);
        }
        if (savedCase.getPayment() != null && savedCase.getPayment().getStatus() != PaymentStatus.CAPTURED) {
            savedCase.getPayment().setStatus(PaymentStatus.CAPTURED);
            paymentRepository.save(savedCase.getPayment());
        }

        // 2. Reconcile RecoveryAttempts belonging to this case
        List<RecoveryAttempt> attempts = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(savedCase.getId());
        List<UUID> reconciledAttemptIds = new ArrayList<>();

        for (RecoveryAttempt attempt : attempts) {
            if (!attempt.getMerchant().getId().equals(merchant.getId())) {
                continue;
            }

            RecoveryAttemptStatus currentStatus = attempt.getStatus();
            if (stateMachine.isTerminal(currentStatus)) {
                // If attempt is already SUCCESS, FAILED, or SKIPPED, do not mutate
                continue;
            }

            if (currentStatus == RecoveryAttemptStatus.SCHEDULED) {
                // Scheduled attempt has not been sent yet: transition to SKIPPED to prevent future execution
                stateMachine.validateTransition(currentStatus, RecoveryAttemptStatus.SKIPPED);
                attempt.setStatus(RecoveryAttemptStatus.SKIPPED);
                attempt.setCompletedAt(now);
                attempt.setResultCode("CASE_RECOVERED");
                attempt.setResultMessage("Recovery case resolved via " + sourceEvent + "; scheduled attempt skipped.");
                recoveryAttemptRepository.save(attempt);
                reconciledAttemptIds.add(attempt.getId());
                log.info("Skipped scheduled RecoveryAttempt id={} for recovered case id={}", attempt.getId(), savedCase.getId());

            } else if (currentStatus == RecoveryAttemptStatus.IN_FLIGHT || currentStatus == RecoveryAttemptStatus.SENT
                    || currentStatus == RecoveryAttemptStatus.DELIVERED || currentStatus == RecoveryAttemptStatus.CLICKED) {
                // Active communication attempt: transition to SUCCESS to reflect successful recovery
                stateMachine.validateTransition(currentStatus, RecoveryAttemptStatus.SUCCESS);
                attempt.setStatus(RecoveryAttemptStatus.SUCCESS);
                attempt.setCompletedAt(now);
                attempt.setResultCode("PAYMENT_RECONCILED");
                attempt.setResultMessage("Payment successfully recovered and reconciled via " + sourceEvent);
                recoveryAttemptRepository.save(attempt);
                reconciledAttemptIds.add(attempt.getId());
                log.info("Transitioned active RecoveryAttempt id={} to SUCCESS for recovered case id={}", attempt.getId(), savedCase.getId());
            }
        }
        recoveryAttemptRepository.flush();

        // 3. Atomically reconcile pending RecoveryExecutionQueueItems
        int completedQueueItems = queueRepository.markPendingItemsCompletedForCase(
                savedCase.getId(),
                merchant.getId(),
                "CASE_TERMINAL_RECOVERED",
                "Payment recovered via " + sourceEvent + "; pending execution completed.",
                now
        );
        log.info("Completed {} pending queue item(s) for recovered case id={}", completedQueueItems, savedCase.getId());

        // 4. Record RECOVERY_PAYMENT_RECONCILED Audit Event
        String paymentIdStr = payment != null ? payment.getId().toString() : (savedCase.getPayment() != null ? savedCase.getPayment().getId().toString() : "UNKNOWN");
        String razorpayPaymentIdStr = payment != null ? payment.getRazorpayPaymentId() : (savedCase.getPayment() != null ? savedCase.getPayment().getRazorpayPaymentId() : "UNKNOWN");
        String razorpayOrderIdStr = payment != null && payment.getRazorpayOrderId() != null ? payment.getRazorpayOrderId() :
                (savedCase.getPayment() != null && savedCase.getPayment().getRazorpayOrderId() != null ? savedCase.getPayment().getRazorpayOrderId() : "NONE");

        String attemptIdsJson = reconciledAttemptIds.stream()
                .map(id -> "\"" + id.toString() + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        String metadataJson = String.format(
                "{\"paymentId\":\"%s\",\"razorpayPaymentId\":\"%s\",\"razorpayOrderId\":\"%s\",\"recoveryCaseId\":\"%s\",\"reconciledAttemptIds\":%s,\"recoveredAmount\":%s,\"reconciliationSource\":\"%s\"}",
                paymentIdStr,
                razorpayPaymentIdStr,
                razorpayOrderIdStr,
                savedCase.getId().toString(),
                attemptIdsJson,
                effectiveRecoveredAmount,
                sourceEvent != null ? sourceEvent : "UNKNOWN"
        );

        auditService.recordEvent(
                merchant,
                "RECOVERY_PAYMENT_RECONCILED",
                ActorType.WEBHOOK,
                "RazorpayWebhook",
                "RecoveryCase",
                savedCase.getId().toString(),
                "RECONCILE_PAYMENT",
                metadataJson,
                clientIp
        );

        log.info("Closed-loop recovery reconciliation complete for case id={}, recoveredAmount={}, reconciledAttempts={}",
                savedCase.getId(), effectiveRecoveredAmount, reconciledAttemptIds.size());

        return savedCase;
    }

    private boolean isCaseActive(RecoveryCase recoveryCase) {
        return recoveryCase.getStatus() == RecoveryCaseStatus.OPEN
                || recoveryCase.getStatus() == RecoveryCaseStatus.IN_PROGRESS;
    }
}
