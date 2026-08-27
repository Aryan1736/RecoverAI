package com.recoverai.backend.service;

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
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.service.executor.DefaultRecoveryActionExecutor;
import com.recoverai.backend.service.executor.ExecutionResult;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoveryOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryOrchestratorService.class);

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

    public RecoveryOrchestratorService(RecoveryCaseRepository recoveryCaseRepository,
                                       AgentDecisionRepository agentDecisionRepository,
                                       RecoveryAttemptRepository recoveryAttemptRepository,
                                       List<RecoveryActionExecutor> actionExecutors,
                                       DefaultRecoveryActionExecutor defaultActionExecutor,
                                       AuditService auditService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.actionExecutors = actionExecutors;
        this.defaultActionExecutor = defaultActionExecutor;
        this.auditService = auditService;
    }

    @Transactional
    public RecoveryAttemptResponseDto orchestrateRecovery(UUID merchantId, UUID recoveryCaseId) {
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
                    "Cannot orchestrate recovery for case in terminal status: " + recoveryCase.getStatus());
        }

        // 3. Idempotency guard: prevent duplicate in-flight / scheduled attempts
        boolean hasActiveAttempt = recoveryAttemptRepository.existsByRecoveryCaseIdAndStatusIn(
                recoveryCaseId, ACTIVE_ATTEMPT_STATUSES);
        if (hasActiveAttempt) {
            throw new DuplicateOrchestrationException(
                    "An active recovery attempt is already scheduled or in-flight for case: " + recoveryCaseId);
        }

        // 4. Retrieve and validate AgentDecision
        AgentDecision agentDecision = agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCaseId)
                .orElseThrow(() -> new AgentDecisionNotFoundException(
                        "No AgentDecision found for recovery case: " + recoveryCaseId + ". Run AI diagnosis first."));

        if (!agentDecision.getMerchant().getId().equals(merchantId)) {
            throw new RecoveryCaseNotFoundException(
                    "AgentDecision merchant mismatch for recovery case: " + recoveryCaseId);
        }

        RecoveryChannel channel = agentDecision.getChannel() != null ? agentDecision.getChannel() : RecoveryChannel.MANUAL;

        // 5. Safe attempt numbering via DB-backed sequence
        int nextAttemptNumber = calculateNextAttemptNumber(recoveryCaseId);

        // 6. Update RecoveryCase from OPEN -> IN_PROGRESS
        if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
            recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
            recoveryCaseRepository.save(recoveryCase);
            log.info("Transitioned recoveryCaseId={} status to IN_PROGRESS", recoveryCaseId);
        }

        // 7. Create and persist initial RecoveryAttempt
        Instant now = Instant.now();
        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(nextAttemptNumber)
                .channel(channel)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(now)
                .metadata(String.format("{\"agentDecisionId\":\"%s\",\"recommendedAction\":\"%s\"}",
                        agentDecision.getId(), agentDecision.getRecommendedAction()))
                .build();

        RecoveryAttempt savedAttempt = recoveryAttemptRepository.save(attempt);
        log.info("Created RecoveryAttempt id={} for caseId={}, attemptNumber={}, channel={}",
                savedAttempt.getId(), recoveryCaseId, nextAttemptNumber, channel);

        // Record creation audit event
        String attemptIdStr = savedAttempt.getId() != null ? savedAttempt.getId().toString() : "UNKNOWN";
        auditService.recordEvent(
                merchant,
                "RECOVERY_ATTEMPT_CREATED",
                ActorType.SYSTEM,
                "RecoveryOrchestrator",
                "RecoveryAttempt",
                attemptIdStr,
                "CREATE_ATTEMPT",
                String.format("Scheduled attempt #%d on channel %s", nextAttemptNumber, channel),
                null
        );

        // 8. Execute via Action Executor abstraction
        executeAttempt(savedAttempt, recoveryCase, merchant);

        return RecoveryAttemptResponseDto.fromEntity(savedAttempt);
    }

    private void executeAttempt(RecoveryAttempt attempt, RecoveryCase recoveryCase, Merchant merchant) {
        attempt.setStatus(RecoveryAttemptStatus.IN_FLIGHT);
        attempt.setExecutedAt(Instant.now());
        recoveryAttemptRepository.save(attempt);

        String attemptIdStr = attempt.getId() != null ? attempt.getId().toString() : "UNKNOWN";

        auditService.recordEvent(
                merchant,
                "RECOVERY_ATTEMPT_STARTED",
                ActorType.SYSTEM,
                "RecoveryOrchestrator",
                "RecoveryAttempt",
                attemptIdStr,
                "EXECUTE_ATTEMPT",
                String.format("Executing attempt #%d via %s", attempt.getAttemptNumber(), attempt.getChannel()),
                null
        );

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
                log.info("RecoveryCase id={} marked as RECOVERED after successful attempt id={}",
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
                    "RecoveryOrchestrator",
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
                    "RecoveryOrchestrator",
                    "RecoveryAttempt",
                    attemptIdStr,
                    "FAIL_ATTEMPT",
                    "Execution failed: " + ex.getMessage(),
                    null
            );
        }
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
