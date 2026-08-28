package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoverySchedulerProperties;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.exception.AgentDecisionNotFoundException;
import com.recoverai.backend.exception.DuplicateOrchestrationException;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.InvalidScheduledTimeException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.service.executor.DefaultRecoveryActionExecutor;
import com.recoverai.backend.service.executor.ExecutionResult;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoverySchedulerService {

    private static final Logger log = LoggerFactory.getLogger(RecoverySchedulerService.class);

    private static final Set<RecoveryCaseStatus> TERMINAL_CASE_STATUSES = Set.of(
            RecoveryCaseStatus.RECOVERED,
            RecoveryCaseStatus.CANCELLED,
            RecoveryCaseStatus.EXPIRED
    );

    private static final Set<RecoveryAttemptStatus> ACTIVE_ATTEMPT_STATUSES = Set.of(
            RecoveryAttemptStatus.SCHEDULED,
            RecoveryAttemptStatus.IN_FLIGHT
    );

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final AgentDecisionRepository agentDecisionRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final List<RecoveryActionExecutor> actionExecutors;
    private final DefaultRecoveryActionExecutor defaultActionExecutor;
    private final AuditService auditService;
    private final RecoverySchedulerProperties properties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private RecoverySchedulerService self;

    public RecoverySchedulerService(RecoveryCaseRepository recoveryCaseRepository,
                                  AgentDecisionRepository agentDecisionRepository,
                                  RecoveryAttemptRepository recoveryAttemptRepository,
                                  List<RecoveryActionExecutor> actionExecutors,
                                  DefaultRecoveryActionExecutor defaultActionExecutor,
                                  AuditService auditService,
                                  RecoverySchedulerProperties properties) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.actionExecutors = actionExecutors;
        this.defaultActionExecutor = defaultActionExecutor;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public RecoveryAttemptResponseDto scheduleRecovery(UUID merchantId, UUID recoveryCaseId, Instant scheduledAt) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (recoveryCaseId == null) {
            throw new IllegalArgumentException("Recovery Case ID cannot be null");
        }

        // 1. Multi-tenant lookup: strictly enforce merchant ownership
        RecoveryCase recoveryCase = recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId)
                .orElseThrow(() -> new RecoveryCaseNotFoundException(
                        "Recovery case not found with id: " + recoveryCaseId + " for merchant: " + merchantId));

        Merchant merchant = recoveryCase.getMerchant();

        // 2. Validate RecoveryCase state
        if (TERMINAL_CASE_STATUSES.contains(recoveryCase.getStatus())) {
            throw new InvalidRecoveryCaseStateException(
                    "Cannot schedule recovery for case in terminal status: " + recoveryCase.getStatus());
        }

        // 3. Validate scheduled time (cannot be significantly in the past)
        Instant now = Instant.now();
        Instant effectiveScheduledAt = scheduledAt != null ? scheduledAt : now;
        if (scheduledAt != null && scheduledAt.isBefore(now.minusSeconds(60))) {
            throw new InvalidScheduledTimeException(
                    "Scheduled time cannot be in the past: " + scheduledAt);
        }

        // 4. Idempotency guard: prevent duplicate in-flight / scheduled attempts
        boolean hasActiveAttempt = recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(
                recoveryCaseId, ACTIVE_ATTEMPT_STATUSES);
        if (hasActiveAttempt) {
            throw new DuplicateOrchestrationException(
                    "An active recovery attempt is already scheduled or in-flight for case: " + recoveryCaseId);
        }

        // 5. Retrieve and validate AgentDecision
        AgentDecision agentDecision = agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId)
                .orElseThrow(() -> new AgentDecisionNotFoundException(
                        "No AgentDecision found for recovery case: " + recoveryCaseId + ". Run AI diagnosis first."));

        if (!agentDecision.getMerchant().getId().equals(merchantId)) {
            throw new RecoveryCaseNotFoundException(
                    "AgentDecision merchant mismatch for recovery case: " + recoveryCaseId);
        }

        RecoveryChannel channel = agentDecision.getChannel() != null ? agentDecision.getChannel() : RecoveryChannel.MANUAL;

        // 6. Safe attempt numbering via DB-backed sequence
        int nextAttemptNumber = calculateNextAttemptNumber(recoveryCaseId);

        // 7. Update RecoveryCase from OPEN -> IN_PROGRESS
        if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
            recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
            recoveryCaseRepository.save(recoveryCase);
            log.info("Transitioned recoveryCaseId={} status to IN_PROGRESS", recoveryCaseId);
        }

        // 8. Create and persist SCHEDULED RecoveryAttempt
        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(nextAttemptNumber)
                .channel(channel)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(effectiveScheduledAt)
                .metadata(String.format("{\"agentDecisionId\":\"%s\",\"recommendedAction\":\"%s\"}",
                        agentDecision.getId(), agentDecision.getRecommendedAction()))
                .build();

        RecoveryAttempt savedAttempt = recoveryAttemptRepository.save(attempt);
        String attemptIdStr = savedAttempt.getId() != null ? savedAttempt.getId().toString() : "UNKNOWN";
        log.info("Scheduled RecoveryAttempt id={} for caseId={}, attemptNumber={}, channel={}, scheduledAt={}",
                attemptIdStr, recoveryCaseId, nextAttemptNumber, channel, effectiveScheduledAt);

        // Record scheduling audit event
        auditService.recordEvent(
                merchant,
                "RECOVERY_ATTEMPT_SCHEDULED",
                ActorType.SYSTEM,
                "RecoveryScheduler",
                "RecoveryAttempt",
                attemptIdStr,
                "SCHEDULE_ATTEMPT",
                String.format("Scheduled attempt #%d on channel %s for %s", nextAttemptNumber, channel, effectiveScheduledAt),
                null
        );

        return RecoveryAttemptResponseDto.fromEntity(savedAttempt);
    }

    public int pollAndExecuteDueAttempts() {
        Instant now = Instant.now();
        int batchSize = properties != null ? Math.max(1, properties.getBatchSize()) : 50;
        List<UUID> dueAttemptIds = recoveryAttemptRepository.findDueScheduledAttemptIds(
                RecoveryAttemptStatus.SCHEDULED, now, PageRequest.of(0, batchSize));

        if (dueAttemptIds.isEmpty()) {
            return 0;
        }

        log.info("Found {} due recovery attempt(s) eligible for execution at {}", dueAttemptIds.size(), now);

        int processedCount = 0;
        for (UUID attemptId : dueAttemptIds) {
            try {
                boolean executed = self != null ? self.claimAndExecuteAttempt(attemptId) : claimAndExecuteAttempt(attemptId);
                if (executed) {
                    processedCount++;
                }
            } catch (Exception ex) {
                log.error("Unhandled error executing recovery attempt id={}: {}", attemptId, ex.getMessage(), ex);
            }
        }

        log.info("Finished polling cycle: {} attempt(s) claimed and executed.", processedCount);
        return processedCount;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimAndExecuteAttempt(UUID attemptId) {
        if (attemptId == null) {
            return false;
        }

        Instant now = Instant.now();

        // 1. Atomic claim in DB: SCHEDULED -> IN_FLIGHT
        int claimed = recoveryAttemptRepository.claimAttemptForExecution(
                attemptId, RecoveryAttemptStatus.SCHEDULED, RecoveryAttemptStatus.IN_FLIGHT, now);
        if (claimed == 0) {
            log.debug("Attempt id={} was already claimed or is no longer SCHEDULED", attemptId);
            return false;
        }

        RecoveryAttempt attempt = recoveryAttemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) {
            log.warn("Claimed attempt id={} not found in database", attemptId);
            return false;
        }

        RecoveryCase recoveryCase = attempt.getRecoveryCase();
        Merchant merchant = attempt.getMerchant();
        String attemptIdStr = attempt.getId().toString();

        // 2. Guard against terminal RecoveryCases
        if (TERMINAL_CASE_STATUSES.contains(recoveryCase.getStatus())) {
            log.info("RecoveryCase id={} is terminal ({}), skipping execution for attempt id={}",
                    recoveryCase.getId(), recoveryCase.getStatus(), attemptIdStr);

            attempt.setStatus(RecoveryAttemptStatus.SKIPPED);
            attempt.setCompletedAt(now);
            attempt.setResultCode("CASE_TERMINAL");
            attempt.setResultMessage("Skipped execution: recovery case is already in terminal state " + recoveryCase.getStatus());
            recoveryAttemptRepository.save(attempt);

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_ATTEMPT_SKIPPED",
                    ActorType.SYSTEM,
                    "RecoveryScheduler",
                    "RecoveryAttempt",
                    attemptIdStr,
                    "SKIP_ATTEMPT",
                    "Skipped attempt #" + attempt.getAttemptNumber() + " because case is " + recoveryCase.getStatus(),
                    null
            );
            return true;
        }

        // 3. Ensure RecoveryCase is marked IN_PROGRESS if OPEN
        if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
            recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
            recoveryCaseRepository.save(recoveryCase);
        }

        // 4. Record claim and start audit events
        auditService.recordEvent(
                merchant,
                "RECOVERY_ATTEMPT_CLAIMED",
                ActorType.SYSTEM,
                "RecoveryScheduler",
                "RecoveryAttempt",
                attemptIdStr,
                "CLAIM_ATTEMPT",
                String.format("Claimed scheduled attempt #%d for execution via %s",
                        attempt.getAttemptNumber(), attempt.getChannel()),
                null
        );

        auditService.recordEvent(
                merchant,
                "RECOVERY_ATTEMPT_STARTED",
                ActorType.SYSTEM,
                "RecoveryScheduler",
                "RecoveryAttempt",
                attemptIdStr,
                "EXECUTE_ATTEMPT",
                String.format("Executing attempt #%d via %s", attempt.getAttemptNumber(), attempt.getChannel()),
                null
        );

        // 5. Execute via channel-specific Action Executor
        try {
            RecoveryActionExecutor executor = findExecutor(attempt.getChannel());
            ExecutionResult result = executor.execute(attempt, recoveryCase);

            attempt.setStatus(result.getStatus());
            attempt.setCompletedAt(Instant.now());
            attempt.setResultCode(result.getResultCode());
            attempt.setResultMessage(result.getResultMessage());
            if (result.getRecoveryLink() != null) {
                attempt.setRecoveryLink(result.getRecoveryLink());
            }
            if (result.getMetadata() != null) {
                attempt.setMetadata(result.getMetadata());
            }

            // If execution succeeded immediately (e.g. payment retry success)
            if (result.getStatus() == RecoveryAttemptStatus.SUCCESS) {
                recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
                recoveryCase.setRecoveredAt(Instant.now());
                recoveryCaseRepository.save(recoveryCase);
                log.info("RecoveryCase id={} marked as RECOVERED after successful scheduled attempt id={}",
                        recoveryCase.getId(), attemptIdStr);
            }

            recoveryAttemptRepository.save(attempt);

            String eventType = switch (result.getStatus()) {
                case SUCCESS -> "RECOVERY_ATTEMPT_SUCCEEDED";
                case SENT, DELIVERED -> "RECOVERY_ATTEMPT_SENT";
                case SKIPPED -> "RECOVERY_ATTEMPT_SKIPPED";
                default -> "RECOVERY_ATTEMPT_FAILED";
            };

            auditService.recordEvent(
                    merchant,
                    eventType,
                    ActorType.SYSTEM,
                    "RecoveryScheduler",
                    "RecoveryAttempt",
                    attemptIdStr,
                    "COMPLETE_ATTEMPT",
                    String.format("Attempt #%d completed with status %s: %s",
                            attempt.getAttemptNumber(), result.getStatus(), result.getResultMessage()),
                    null
            );

        } catch (Exception ex) {
            log.error("Execution error during recovery attempt id={}: {}", attemptIdStr, ex.getMessage(), ex);
            attempt.setStatus(RecoveryAttemptStatus.FAILED);
            attempt.setCompletedAt(Instant.now());
            attempt.setResultCode("EXECUTION_ERROR");
            attempt.setResultMessage("Execution failed: " + ex.getMessage());
            recoveryAttemptRepository.save(attempt);

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_ATTEMPT_FAILED",
                    ActorType.SYSTEM,
                    "RecoveryScheduler",
                    "RecoveryAttempt",
                    attemptIdStr,
                    "FAIL_ATTEMPT",
                    "Execution failed: " + ex.getMessage(),
                    null
            );
        }

        return true;
    }

    private RecoveryActionExecutor findExecutor(RecoveryChannel channel) {
        if (actionExecutors != null) {
            for (RecoveryActionExecutor executor : actionExecutors) {
                if (executor != defaultActionExecutor && executor.supports(channel)) {
                    return executor;
                }
            }
        }
        return defaultActionExecutor;
    }

    private int calculateNextAttemptNumber(UUID recoveryCaseId) {
        Optional<RecoveryAttempt> topAttempt = recoveryAttemptRepository.findTopByRecoveryCaseIdOrderByAttemptNumberDesc(recoveryCaseId);
        return topAttempt.map(attempt -> attempt.getAttemptNumber() + 1).orElse(1);
    }
}
