package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryStrategyProperties;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.strategy.RecoveryStrategySnapshot;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.exception.AgentDecisionNotFoundException;
import com.recoverai.backend.exception.DuplicateOrchestrationException;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.NoViableStrategyException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.exception.StrategyExecutionDisabledException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryStrategyRepository;
import com.recoverai.backend.service.executor.DefaultRecoveryActionExecutor;
import com.recoverai.backend.service.executor.ExecutionResult;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import com.recoverai.backend.service.strategy.RecoveryStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final RecoveryStrategyRepository recoveryStrategyRepository;
    private final RecoveryStrategyService recoveryStrategyService;
    private final RecoveryStrategyProperties recoveryStrategyProperties;
    private final List<RecoveryActionExecutor> actionExecutors;
    private final DefaultRecoveryActionExecutor defaultActionExecutor;
    private final AuditService auditService;

    @Autowired
    public RecoveryOrchestratorService(RecoveryCaseRepository recoveryCaseRepository,
                                       AgentDecisionRepository agentDecisionRepository,
                                       RecoveryAttemptRepository recoveryAttemptRepository,
                                       @Autowired(required = false) RecoveryStrategyRepository recoveryStrategyRepository,
                                       @Autowired(required = false) RecoveryStrategyService recoveryStrategyService,
                                       @Autowired(required = false) RecoveryStrategyProperties recoveryStrategyProperties,
                                       List<RecoveryActionExecutor> actionExecutors,
                                       DefaultRecoveryActionExecutor defaultActionExecutor,
                                       AuditService auditService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.recoveryStrategyRepository = recoveryStrategyRepository;
        this.recoveryStrategyService = recoveryStrategyService;
        this.recoveryStrategyProperties = recoveryStrategyProperties != null ? recoveryStrategyProperties : new RecoveryStrategyProperties();
        this.actionExecutors = actionExecutors;
        this.defaultActionExecutor = defaultActionExecutor;
        this.auditService = auditService;
    }

    public RecoveryOrchestratorService(RecoveryCaseRepository recoveryCaseRepository,
                                       AgentDecisionRepository agentDecisionRepository,
                                       RecoveryAttemptRepository recoveryAttemptRepository,
                                       RecoveryStrategyService recoveryStrategyService,
                                       List<RecoveryActionExecutor> actionExecutors,
                                       DefaultRecoveryActionExecutor defaultActionExecutor,
                                       AuditService auditService) {
        this(recoveryCaseRepository, agentDecisionRepository, recoveryAttemptRepository, null, recoveryStrategyService, new RecoveryStrategyProperties(), actionExecutors, defaultActionExecutor, auditService);
    }

    public RecoveryOrchestratorService(RecoveryCaseRepository recoveryCaseRepository,
                                       AgentDecisionRepository agentDecisionRepository,
                                       RecoveryAttemptRepository recoveryAttemptRepository,
                                       List<RecoveryActionExecutor> actionExecutors,
                                       DefaultRecoveryActionExecutor defaultActionExecutor,
                                       AuditService auditService) {
        this(recoveryCaseRepository, agentDecisionRepository, recoveryAttemptRepository, null, null, new RecoveryStrategyProperties(), actionExecutors, defaultActionExecutor, auditService);
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

        // 4. Configuration check
        if (recoveryStrategyProperties != null && !recoveryStrategyProperties.isExecutionEnabled()) {
            throw new StrategyExecutionDisabledException(
                    "Recovery strategy execution is disabled by configuration");
        }

        // 5. Strategy resolution and validation
        ResolvedStrategyResult strategyResult = resolveStrategy(recoveryCase, merchantId);
        RecoveryStrategy strategy = strategyResult.strategy;
        RecoveryChannel channel = strategyResult.channel;
        String metadataJson = strategyResult.metadataJson;
        String strategySnapshotJson = strategyResult.strategySnapshotJson;

        // 6. Safe attempt numbering via DB-backed sequence
        int nextAttemptNumber = calculateNextAttemptNumber(recoveryCaseId);

        // 7. Update RecoveryCase from OPEN -> IN_PROGRESS
        if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
            recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
            recoveryCaseRepository.save(recoveryCase);
            log.info("Transitioned recoveryCaseId={} status to IN_PROGRESS", recoveryCaseId);
        }

        // 8. Create and persist initial RecoveryAttempt
        Instant now = Instant.now();
        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .strategy(strategy)
                .strategySnapshot(strategySnapshotJson)
                .attemptNumber(nextAttemptNumber)
                .channel(channel)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(now)
                .metadata(metadataJson)
                .build();

        RecoveryAttempt savedAttempt = recoveryAttemptRepository.save(attempt);
        String attemptIdStr = savedAttempt.getId() != null ? savedAttempt.getId().toString() : "UNKNOWN";
        log.info("Created RecoveryAttempt id={} for caseId={}, attemptNumber={}, channel={}, strategyId={}",
                attemptIdStr, recoveryCaseId, nextAttemptNumber, channel,
                strategy != null ? strategy.getId() : "NONE");

        // Record creation audit event
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

        // 9. Execute via Action Executor abstraction
        executeAttempt(savedAttempt, recoveryCase, merchant, strategy);

        return RecoveryAttemptResponseDto.fromEntity(savedAttempt);
    }

    private ResolvedStrategyResult resolveStrategy(RecoveryCase recoveryCase, UUID merchantId) {
        UUID caseId = recoveryCase.getId();
        Merchant merchant = recoveryCase.getMerchant();
        RecoveryStrategy strategy = null;

        // 1. Try finding latest merchant-scoped strategy
        if (recoveryStrategyRepository != null) {
            strategy = recoveryStrategyRepository
                    .findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseId, merchantId)
                    .orElse(null);
        }

        // 2. If strategy exists, check viability; if not exists, check AgentDecision and compute strategy
        if (strategy != null) {
            if (strategy.getMerchant() != null && !strategy.getMerchant().getId().equals(merchantId)) {
                throw new RecoveryCaseNotFoundException("Recovery strategy merchant mismatch for case: " + caseId);
            }
            if (strategy.getRecoveryCase() != null && !strategy.getRecoveryCase().getId().equals(caseId)) {
                throw new RecoveryCaseNotFoundException("Recovery strategy case mismatch for case: " + caseId);
            }

            if (!isStrategyViable(strategy, recoveryCase) && recoveryStrategyService != null) {
                log.info("Existing strategy id={} is not viable, regenerating fresh strategy for case id={}",
                        strategy.getId(), caseId);
                strategy = recoveryStrategyService.computeAndPersistStrategy(recoveryCase);
            }
        } else {
            // No strategy exists yet. Check if AgentDecision exists before generating
            AgentDecision agentDecision = agentDecisionRepository
                    .findFirstByRecoveryCaseIdOrderByCreatedAtDesc(caseId)
                    .orElseThrow(() -> new AgentDecisionNotFoundException(
                            "No AgentDecision found for recovery case: " + caseId + ". Run AI diagnosis first."));

            if (!agentDecision.getMerchant().getId().equals(merchantId)) {
                throw new RecoveryCaseNotFoundException(
                        "AgentDecision merchant mismatch for recovery case: " + caseId);
            }

            if (recoveryStrategyService != null) {
                strategy = recoveryStrategyService.computeAndPersistStrategy(recoveryCase);
            } else {
                // Fallback for unit tests without strategy service wired
                RecoveryChannel channel = agentDecision.getChannel() != null ? agentDecision.getChannel() : RecoveryChannel.MANUAL;
                String recommendedAction = agentDecision.getRecommendedAction();
                String metadata = String.format("{\"agentDecisionId\":\"%s\",\"strategyId\":\"DIRECT\",\"recommendedAction\":\"%s\"}",
                        agentDecision.getId(), recommendedAction);

                RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.builder()
                        .channel(channel)
                        .recommendedAction(recommendedAction)
                        .confidenceScore(agentDecision.getConfidenceScore())
                        .reason(agentDecision.getReasoning())
                        .build();

                return new ResolvedStrategyResult(
                        null,
                        channel,
                        recommendedAction,
                        metadata,
                        snapshot.toJson()
                );
            }
        }

        // 3. If strategy is present, validate it
        if (strategy != null) {
            String strategyIdStr = strategy.getId() != null ? strategy.getId().toString() : "UNKNOWN";

            if (strategy.isTerminal()) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_STRATEGY_EXECUTION_REJECTED",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryStrategy",
                        strategyIdStr,
                        "REJECT_STRATEGY",
                        "Terminal strategy cannot be executed: " + strategy.getReason(),
                        null
                );

                if (TERMINAL_CASE_STATUSES.contains(recoveryCase.getStatus())) {
                    throw new InvalidRecoveryCaseStateException(
                            "Cannot orchestrate recovery for case in terminal status: " + recoveryCase.getStatus());
                }
                throw new NoViableStrategyException("Cannot orchestrate recovery: " + strategy.getReason());
            }

            if (strategy.getChannel() == null || strategy.getRecommendedAction() == null) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_STRATEGY_EXECUTION_REJECTED",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryStrategy",
                        strategyIdStr,
                        "REJECT_STRATEGY",
                        "Strategy missing required channel or recommended action",
                        null
                );
                throw new NoViableStrategyException("Cannot orchestrate recovery: strategy channel or action is invalid");
            }

            List<RecoveryAttempt> previousAttempts = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId);
            if (previousAttempts != null && previousAttempts.size() >= strategy.getMaxAttempts()) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_STRATEGY_EXECUTION_REJECTED",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryStrategy",
                        strategyIdStr,
                        "REJECT_STRATEGY",
                        String.format("Maximum recovery attempts (%d) reached for case %s", strategy.getMaxAttempts(), caseId),
                        null
                );
                throw new NoViableStrategyException("Cannot orchestrate recovery: maximum attempts exceeded");
            }

            RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.fromStrategy(strategy);
            String snapshotJson = snapshot != null ? snapshot.toJson() : null;

            return new ResolvedStrategyResult(
                    strategy,
                    strategy.getChannel(),
                    strategy.getRecommendedAction(),
                    snapshotJson,
                    snapshotJson
            );
        }

        throw new NoViableStrategyException("No viable recovery strategy could be resolved for case: " + caseId);
    }

    private boolean isStrategyViable(RecoveryStrategy strategy, RecoveryCase recoveryCase) {
        if (strategy == null || strategy.getChannel() == null) {
            return false;
        }
        if (strategy.isTerminal()) {
            return true; // Terminal strategies are valid policy decisions and must be rejected directly
        }

        Customer customer = recoveryCase.getCustomer();
        boolean hasPhone = customer != null && customer.getPhone() != null && !customer.getPhone().trim().isEmpty();
        boolean hasEmail = customer != null && customer.getEmail() != null && !customer.getEmail().trim().isEmpty();

        return switch (strategy.getChannel()) {
            case WHATSAPP, SMS -> hasPhone;
            case EMAIL -> hasEmail;
            case SMART_LINK -> hasPhone || hasEmail;
            case RETRY_CHARGE, MANUAL -> true;
        };
    }

    private void executeAttempt(RecoveryAttempt attempt, RecoveryCase recoveryCase, Merchant merchant, RecoveryStrategy strategy) {
        attempt.setStatus(RecoveryAttemptStatus.IN_FLIGHT);
        attempt.setExecutedAt(Instant.now());
        recoveryAttemptRepository.save(attempt);

        String attemptIdStr = attempt.getId() != null ? attempt.getId().toString() : "UNKNOWN";
        String strategyIdStr = strategy != null && strategy.getId() != null ? strategy.getId().toString() : "DIRECT";

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

        auditService.recordEvent(
                merchant,
                "RECOVERY_STRATEGY_EXECUTION_STARTED",
                ActorType.SYSTEM,
                "RecoveryOrchestrator",
                "RecoveryStrategy",
                strategyIdStr,
                "EXECUTE_STRATEGY",
                String.format("Executing strategy decision on channel %s for attempt #%d",
                        attempt.getChannel(), attempt.getAttemptNumber()),
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

            if (result.getStatus() == RecoveryAttemptStatus.SUCCESS) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_ATTEMPT_SUCCEEDED",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryAttempt",
                        attemptIdStr,
                        "COMPLETE_ATTEMPT",
                        String.format("Attempt #%d completed with status SUCCESS: %s",
                                attempt.getAttemptNumber(), result.getResultMessage()),
                        null
                );

                auditService.recordEvent(
                        merchant,
                        "RECOVERY_STRATEGY_EXECUTION_SUCCEEDED",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryStrategy",
                        strategyIdStr,
                        "COMPLETE_STRATEGY_EXECUTION",
                        String.format("Strategy execution on channel %s succeeded for attempt #%d",
                                attempt.getChannel(), attempt.getAttemptNumber()),
                        null
                );
            } else if (result.getStatus() == RecoveryAttemptStatus.SENT || result.getStatus() == RecoveryAttemptStatus.DELIVERED) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_ATTEMPT_SENT",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryAttempt",
                        attemptIdStr,
                        "COMPLETE_ATTEMPT",
                        String.format("Attempt #%d completed with status %s: %s",
                                attempt.getAttemptNumber(), result.getStatus(), result.getResultMessage()),
                        null
                );

                auditService.recordEvent(
                        merchant,
                        "RECOVERY_STRATEGY_EXECUTION_SUCCEEDED",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryStrategy",
                        strategyIdStr,
                        "DISPATCH_STRATEGY",
                        String.format("Strategy action dispatched on channel %s for attempt #%d",
                                attempt.getChannel(), attempt.getAttemptNumber()),
                        null
                );
            } else if (result.getStatus() == RecoveryAttemptStatus.SKIPPED) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_ATTEMPT_SKIPPED",
                        ActorType.SYSTEM,
                        "RecoveryOrchestrator",
                        "RecoveryAttempt",
                        attemptIdStr,
                        "COMPLETE_ATTEMPT",
                        String.format("Attempt #%d skipped: %s", attempt.getAttemptNumber(), result.getResultMessage()),
                        null
                );
            } else {
                handleExecutionFailure(attempt, merchant, attemptIdStr, strategyIdStr, strategy, result.getResultMessage());
            }

        } catch (Exception ex) {
            log.error("Execution error during recovery attempt id={}: {}", attemptIdStr, ex.getMessage(), ex);
            attempt.setStatus(RecoveryAttemptStatus.FAILED);
            attempt.setCompletedAt(Instant.now());
            attempt.setResultCode("EXECUTION_ERROR");
            attempt.setResultMessage("Execution failed: " + ex.getMessage());
            recoveryAttemptRepository.save(attempt);

            handleExecutionFailure(attempt, merchant, attemptIdStr, strategyIdStr, strategy, ex.getMessage());
        }
    }

    private void handleExecutionFailure(RecoveryAttempt attempt,
                                        Merchant merchant,
                                        String attemptIdStr,
                                        String strategyIdStr,
                                        RecoveryStrategy strategy,
                                        String errorMessage) {
        auditService.recordEvent(
                merchant,
                "RECOVERY_ATTEMPT_FAILED",
                ActorType.SYSTEM,
                "RecoveryOrchestrator",
                "RecoveryAttempt",
                attemptIdStr,
                "FAIL_ATTEMPT",
                "Execution failed: " + errorMessage,
                null
        );

        auditService.recordEvent(
                merchant,
                "RECOVERY_STRATEGY_EXECUTION_FAILED",
                ActorType.SYSTEM,
                "RecoveryOrchestrator",
                "RecoveryStrategy",
                strategyIdStr,
                "FAIL_STRATEGY_EXECUTION",
                String.format("Strategy execution failed on channel %s for attempt #%d: %s",
                        attempt.getChannel(), attempt.getAttemptNumber(), errorMessage),
                null
        );

        if (strategy != null && strategy.getFallbackChannel() != null && recoveryStrategyProperties.isFallbackEnabled()) {
            auditService.recordEvent(
                    merchant,
                    "RECOVERY_STRATEGY_FALLBACK_SELECTED",
                    ActorType.SYSTEM,
                    "RecoveryOrchestrator",
                    "RecoveryStrategy",
                    strategyIdStr,
                    "SELECT_FALLBACK",
                    String.format("Fallback channel %s (action: %s) selected for subsequent recovery attempt following failure on %s",
                            strategy.getFallbackChannel(), strategy.getFallbackAction(), attempt.getChannel()),
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

    private record ResolvedStrategyResult(
            RecoveryStrategy strategy,
            RecoveryChannel channel,
            String recommendedAction,
            String metadataJson,
            String strategySnapshotJson
    ) {}
}
