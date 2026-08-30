package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.config.RecoveryQueueProperties;
import com.recoverai.backend.config.RecoveryStrategyProperties;
import com.recoverai.backend.dto.strategy.RecoveryStrategySnapshot;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.service.executor.DefaultRecoveryActionExecutor;
import com.recoverai.backend.service.executor.ExecutionResult;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoveryExecutionQueueService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryExecutionQueueService.class);

    private static final Set<RecoveryCaseStatus> TERMINAL_CASE_STATUSES = Set.of(
            RecoveryCaseStatus.RECOVERED,
            RecoveryCaseStatus.CANCELLED,
            RecoveryCaseStatus.EXPIRED
    );

    private final RecoveryExecutionQueueRepository queueRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryCaseRepository recoveryCaseRepository;
    private final List<RecoveryActionExecutor> actionExecutors;
    private final DefaultRecoveryActionExecutor defaultActionExecutor;
    private final AuditService auditService;
    private final RecoveryQueueProperties properties;
    private final RecoveryCommunicationProperties communicationProperties;
    private final RecoveryStrategyProperties strategyProperties;
    private final com.recoverai.backend.service.notification.MerchantNotificationService notificationService;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private RecoveryExecutionQueueService self;

    public RecoveryExecutionQueueService(RecoveryExecutionQueueRepository queueRepository,
                                         RecoveryAttemptRepository recoveryAttemptRepository,
                                         RecoveryCaseRepository recoveryCaseRepository,
                                         List<RecoveryActionExecutor> actionExecutors,
                                         DefaultRecoveryActionExecutor defaultActionExecutor,
                                         AuditService auditService,
                                         RecoveryQueueProperties properties) {
        this(queueRepository, recoveryAttemptRepository, recoveryCaseRepository,
                actionExecutors, defaultActionExecutor, auditService, properties, null, null, null);
    }

    public RecoveryExecutionQueueService(RecoveryExecutionQueueRepository queueRepository,
                                         RecoveryAttemptRepository recoveryAttemptRepository,
                                         RecoveryCaseRepository recoveryCaseRepository,
                                         List<RecoveryActionExecutor> actionExecutors,
                                         DefaultRecoveryActionExecutor defaultActionExecutor,
                                         AuditService auditService,
                                         RecoveryQueueProperties properties,
                                         RecoveryCommunicationProperties communicationProperties) {
        this(queueRepository, recoveryAttemptRepository, recoveryCaseRepository,
                actionExecutors, defaultActionExecutor, auditService, properties, communicationProperties, null, null);
    }

    public RecoveryExecutionQueueService(RecoveryExecutionQueueRepository queueRepository,
                                         RecoveryAttemptRepository recoveryAttemptRepository,
                                         RecoveryCaseRepository recoveryCaseRepository,
                                         List<RecoveryActionExecutor> actionExecutors,
                                         DefaultRecoveryActionExecutor defaultActionExecutor,
                                         AuditService auditService,
                                         RecoveryQueueProperties properties,
                                         RecoveryCommunicationProperties communicationProperties,
                                         RecoveryStrategyProperties strategyProperties) {
        this(queueRepository, recoveryAttemptRepository, recoveryCaseRepository,
                actionExecutors, defaultActionExecutor, auditService, properties, communicationProperties, strategyProperties, null);
    }

    @Autowired
    public RecoveryExecutionQueueService(RecoveryExecutionQueueRepository queueRepository,
                                         RecoveryAttemptRepository recoveryAttemptRepository,
                                         RecoveryCaseRepository recoveryCaseRepository,
                                         List<RecoveryActionExecutor> actionExecutors,
                                         DefaultRecoveryActionExecutor defaultActionExecutor,
                                         AuditService auditService,
                                         RecoveryQueueProperties properties,
                                         @Autowired(required = false) RecoveryCommunicationProperties communicationProperties,
                                         @Autowired(required = false) RecoveryStrategyProperties strategyProperties,
                                         @Autowired(required = false) com.recoverai.backend.service.notification.MerchantNotificationService notificationService) {
        this.queueRepository = queueRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.actionExecutors = actionExecutors;
        this.defaultActionExecutor = defaultActionExecutor;
        this.auditService = auditService;
        this.properties = properties != null ? properties : new RecoveryQueueProperties();
        this.communicationProperties = communicationProperties;
        this.strategyProperties = strategyProperties != null ? strategyProperties : new RecoveryStrategyProperties();
        this.notificationService = notificationService;
    }

    public RecoveryExecutionQueueItem enqueueAttempt(RecoveryAttempt attempt, Instant availableAt) {
        if (attempt == null) {
            throw new IllegalArgumentException("RecoveryAttempt cannot be null");
        }
        if (attempt.getId() == null) {
            throw new IllegalArgumentException("RecoveryAttempt must have a valid ID before enqueueing");
        }

        UUID attemptId = attempt.getId();
        Optional<RecoveryExecutionQueueItem> existingOpt = queueRepository.findByRecoveryAttemptId(attemptId);
        if (existingOpt.isPresent()) {
            log.info("RecoveryAttempt id={} already has queue item id={}, returning existing item",
                    attemptId, existingOpt.get().getId());
            return existingOpt.get();
        }

        Merchant merchant = attempt.getMerchant();
        RecoveryCase recoveryCase = attempt.getRecoveryCase();
        if (merchant == null || recoveryCase == null) {
            throw new IllegalArgumentException("RecoveryAttempt must have associated Merchant and RecoveryCase");
        }

        Instant effectiveAvailableAt = availableAt != null ? availableAt : Instant.now();
        int maxRetries = properties != null ? Math.max(0, properties.getMaxRetries()) : 3;

        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(effectiveAvailableAt)
                .retryCount(0)
                .maxRetries(maxRetries)
                .build();

        try {
            RecoveryExecutionQueueItem saved = queueRepository.saveAndFlush(queueItem);
            if (saved == null) {
                saved = queueItem;
            }
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            String queueItemIdStr = saved.getId().toString();
            String attemptIdStr = attemptId.toString();

            log.info("Enqueued RecoveryAttempt id={} into queue item id={} (availableAt={}, maxRetries={})",
                    attemptIdStr, queueItemIdStr, effectiveAvailableAt, maxRetries);

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_EXECUTION_QUEUED",
                    ActorType.SYSTEM,
                    "RecoveryExecutionQueue",
                    "RecoveryExecutionQueueItem",
                    queueItemIdStr,
                    "ENQUEUE_ATTEMPT",
                    String.format("Enqueued recovery attempt #%d for case %s (availableAt=%s)",
                            attempt.getAttemptNumber(), recoveryCase.getId(), effectiveAvailableAt),
                    null
            );

            return saved;
        } catch (DataIntegrityViolationException dive) {
            // Concurrent enqueue occurred: load and return existing queue item
            log.warn("Concurrent duplicate enqueue detected for attempt id={}, fetching existing record", attemptId);
            return queueRepository.findByRecoveryAttemptId(attemptId)
                    .orElseThrow(() -> new IllegalStateException("Duplicate queue item indicated but not found for attempt: " + attemptId, dive));
        }
    }

    @Transactional
    public boolean claimItem(UUID queueItemId, String workerId) {
        if (queueItemId == null) {
            return false;
        }
        Instant now = Instant.now();
        String effectiveWorkerId = workerId != null ? workerId : properties.getWorkerId();

        int updatedRows = queueRepository.claimItem(queueItemId, effectiveWorkerId, now);
        if (updatedRows > 0) {
            log.info("Queue item id={} claimed by worker={}", queueItemId, effectiveWorkerId);

            Optional<RecoveryExecutionQueueItem> itemOpt = queueRepository.findById(queueItemId);
            itemOpt.ifPresent(item -> auditService.recordEvent(
                    item.getMerchant(),
                    "RECOVERY_EXECUTION_CLAIMED",
                    ActorType.SYSTEM,
                    effectiveWorkerId,
                    "RecoveryExecutionQueueItem",
                    queueItemId.toString(),
                    "CLAIM_QUEUE_ITEM",
                    String.format("Claimed queue item for attempt #%d by worker %s",
                            item.getRecoveryAttempt() != null ? item.getRecoveryAttempt().getAttemptNumber() : -1,
                            effectiveWorkerId),
                    null
            ));
            return true;
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processQueueItem(UUID queueItemId) {
        if (queueItemId == null) {
            return false;
        }

        RecoveryExecutionQueueItem queueItem = queueRepository.findById(queueItemId).orElse(null);
        if (queueItem == null) {
            log.warn("Queue item id={} not found for processing", queueItemId);
            return false;
        }

        Instant startTime = Instant.now();
        String queueItemIdStr = queueItem.getId().toString();
        Merchant merchant = queueItem.getMerchant();
        RecoveryAttempt attempt = queueItem.getRecoveryAttempt();
        RecoveryCase recoveryCase = queueItem.getRecoveryCase();

        // 1. Strict multi-tenant isolation validation
        if (merchant == null || attempt == null || recoveryCase == null) {
            log.error("Corrupted queue item id={}: missing merchant, attempt, or case", queueItemIdStr);
            queueRepository.moveToDeadLetter(queueItemId, "CORRUPTED_QUEUE_ITEM", "Missing relationship references", Instant.now());
            return false;
        }

        if (attempt.getMerchant() == null || !attempt.getMerchant().getId().equals(merchant.getId())) {
            log.error("Cross-tenant violation: attempt merchant does not match queue item merchant for id={}", queueItemIdStr);
            queueRepository.moveToDeadLetter(queueItemId, "TENANT_MISMATCH", "Attempt merchant mismatch", Instant.now());
            return false;
        }

        if (recoveryCase.getMerchant() == null || !recoveryCase.getMerchant().getId().equals(merchant.getId())) {
            log.error("Cross-tenant violation: case merchant does not match queue item merchant for id={}", queueItemIdStr);
            queueRepository.moveToDeadLetter(queueItemId, "TENANT_MISMATCH", "Case merchant mismatch", Instant.now());
            return false;
        }

        String attemptIdStr = attempt.getId().toString();

        // 2. Terminal Case Protection: RECOVERED / CANCELLED / EXPIRED
        if (TERMINAL_CASE_STATUSES.contains(recoveryCase.getStatus())) {
            log.info("RecoveryCase id={} is terminal ({}), skipping queue execution for item id={}, attempt id={}",
                    recoveryCase.getId(), recoveryCase.getStatus(), queueItemIdStr, attemptIdStr);

            Instant now = Instant.now();
            attempt.setStatus(RecoveryAttemptStatus.SKIPPED);
            attempt.setCompletedAt(now);
            attempt.setResultCode("CASE_TERMINAL");
            attempt.setResultMessage("Skipped execution: recovery case is already in terminal state " + recoveryCase.getStatus());
            recoveryAttemptRepository.save(attempt);

            queueRepository.markCompleted(queueItemId, now);

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_EXECUTION_SKIPPED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryExecutionQueueItem",
                    queueItemIdStr,
                    "SKIP_EXECUTION",
                    String.format("Skipped queue execution for attempt #%d because case is %s",
                            attempt.getAttemptNumber(), recoveryCase.getStatus()),
                    null
            );

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_ATTEMPT_SKIPPED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryAttempt",
                    attemptIdStr,
                    "SKIP_ATTEMPT",
                    String.format("Skipped attempt #%d because case is %s",
                            attempt.getAttemptNumber(), recoveryCase.getStatus()),
                    null
            );
            return true;
        }

        // 3. Verify attempt is still executable
        if (attempt.getStatus() == RecoveryAttemptStatus.SUCCESS || attempt.getStatus() == RecoveryAttemptStatus.SENT
                || attempt.getStatus() == RecoveryAttemptStatus.DELIVERED || attempt.getStatus() == RecoveryAttemptStatus.SKIPPED) {
            log.info("Attempt id={} is already terminal ({}), completing queue item id={}",
                    attemptIdStr, attempt.getStatus(), queueItemIdStr);
            queueRepository.markCompleted(queueItemId, Instant.now());
            return true;
        }

        // 4. Mark PROCESSING and IN_FLIGHT
        Instant now = Instant.now();
        queueRepository.markProcessing(queueItemId, now);

        attempt.setStatus(RecoveryAttemptStatus.IN_FLIGHT);
        attempt.setExecutedAt(now);
        recoveryAttemptRepository.save(attempt);

        if (recoveryCase.getStatus() == RecoveryCaseStatus.OPEN) {
            recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
            recoveryCaseRepository.save(recoveryCase);
        }

        auditService.recordEvent(
                merchant,
                "RECOVERY_EXECUTION_STARTED",
                ActorType.SYSTEM,
                properties.getWorkerId(),
                "RecoveryExecutionQueueItem",
                queueItemIdStr,
                "START_EXECUTION",
                String.format("Started execution of attempt #%d on channel %s",
                        attempt.getAttemptNumber(), attempt.getChannel()),
                null
        );

        auditService.recordEvent(
                merchant,
                "RECOVERY_ATTEMPT_STARTED",
                ActorType.SYSTEM,
                properties.getWorkerId(),
                "RecoveryAttempt",
                attemptIdStr,
                "EXECUTE_ATTEMPT",
                String.format("Executing attempt #%d via %s", attempt.getAttemptNumber(), attempt.getChannel()),
                null
        );

        // 5. Execute via channel-specific Action Executor (using authoritative strategy snapshot persisted on attempt)
        try {
            auditService.recordEvent(
                    merchant,
                    "RECOVERY_PROVIDER_DISPATCH_STARTED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryAttempt",
                    attemptIdStr,
                    "DISPATCH_PROVIDER",
                    String.format("Dispatching attempt #%d to channel %s", attempt.getAttemptNumber(), attempt.getChannel()),
                    null
            );

            RecoveryActionExecutor executor = findExecutor(attempt.getChannel());
            ExecutionResult result = executor.execute(attempt, recoveryCase);

            Instant completionTime = Instant.now();
            attempt.setStatus(result.getStatus());
            attempt.setCompletedAt(completionTime);
            attempt.setResultCode(result.getResultCode());
            attempt.setResultMessage(result.getResultMessage());
            if (result.getRecoveryLink() != null) {
                attempt.setRecoveryLink(result.getRecoveryLink());
            }
            if (result.getMetadata() != null) {
                attempt.setMetadata(result.getMetadata());
            }

            // If payment charge retry succeeded immediately
            if (result.getStatus() == RecoveryAttemptStatus.SUCCESS) {
                recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
                recoveryCase.setRecoveredAt(completionTime);
                recoveryCaseRepository.save(recoveryCase);
                log.info("RecoveryCase id={} marked as RECOVERED after successful queue attempt id={}",
                        recoveryCase.getId(), attemptIdStr);
            }

            recoveryAttemptRepository.save(attempt);

            if (result.getStatus() == RecoveryAttemptStatus.SUCCESS
                    || result.getStatus() == RecoveryAttemptStatus.SENT
                    || result.getStatus() == RecoveryAttemptStatus.DELIVERED) {

                queueRepository.markCompleted(queueItemId, completionTime);

                auditService.recordEvent(
                        merchant,
                        "RECOVERY_PROVIDER_DISPATCH_SUCCEEDED",
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryAttempt",
                        attemptIdStr,
                        "PROVIDER_SUCCESS",
                        String.format("Provider dispatch succeeded for attempt #%d: %s",
                                attempt.getAttemptNumber(), result.getResultCode()),
                        null
                );

                auditService.recordEvent(
                        merchant,
                        "RECOVERY_EXECUTION_COMPLETED",
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryExecutionQueueItem",
                        queueItemIdStr,
                        "COMPLETE_EXECUTION",
                        String.format("Queue execution completed with status %s: %s",
                                result.getStatus(), result.getResultMessage()),
                        null
                );

                String attemptEventType = result.getStatus() == RecoveryAttemptStatus.SUCCESS
                        ? "RECOVERY_ATTEMPT_SUCCEEDED" : "RECOVERY_ATTEMPT_SENT";
                auditService.recordEvent(
                        merchant,
                        attemptEventType,
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryAttempt",
                        attemptIdStr,
                        "COMPLETE_ATTEMPT",
                        String.format("Attempt #%d completed with status %s: %s",
                                attempt.getAttemptNumber(), result.getStatus(), result.getResultMessage()),
                        null
                );

                long durationMs = Duration.between(startTime, completionTime).toMillis();
                log.info("Queue item id={} successfully processed in {} ms with status {}",
                        queueItemIdStr, durationMs, result.getStatus());
                return true;

            } else if (result.getStatus() == RecoveryAttemptStatus.SKIPPED) {
                queueRepository.markCompleted(queueItemId, completionTime);
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_EXECUTION_SKIPPED",
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryExecutionQueueItem",
                        queueItemIdStr,
                        "SKIP_EXECUTION",
                        result.getResultMessage(),
                        null
                );
                return true;
            } else {
                // Provider failure returned as result
                ProviderFailureType failureType = result.getFailureType() != null
                        ? result.getFailureType()
                        : ProviderErrorClassifier.classifyResultCode(result.getResultCode());
                boolean isRetryable = ProviderErrorClassifier.isRetryable(failureType);

                auditService.recordEvent(
                        merchant,
                        "RECOVERY_PROVIDER_DISPATCH_FAILED",
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryAttempt",
                        attemptIdStr,
                        "PROVIDER_FAILED",
                        String.format("Provider dispatch failed (%s): %s", failureType, result.getResultMessage()),
                        null
                );

                if (failureType == ProviderFailureType.RATE_LIMITED) {
                    auditService.recordEvent(
                            merchant,
                            "RECOVERY_PROVIDER_RATE_LIMITED",
                            ActorType.SYSTEM,
                            properties.getWorkerId(),
                            "RecoveryAttempt",
                            attemptIdStr,
                            "RATE_LIMITED",
                            "Provider rate limit encountered: " + result.getResultMessage(),
                            null
                    );
                }

                if (isRetryable) {
                    auditService.recordEvent(
                            merchant,
                            "RECOVERY_PROVIDER_RETRYABLE_FAILURE",
                            ActorType.SYSTEM,
                            properties.getWorkerId(),
                            "RecoveryAttempt",
                            attemptIdStr,
                            "RETRYABLE_FAILURE",
                            String.format("Retryable provider error (%s): %s", failureType, result.getResultMessage()),
                            null
                    );
                } else {
                    auditService.recordEvent(
                            merchant,
                            "RECOVERY_PROVIDER_PERMANENT_FAILURE",
                            ActorType.SYSTEM,
                            properties.getWorkerId(),
                            "RecoveryAttempt",
                            attemptIdStr,
                            "PERMANENT_FAILURE",
                            String.format("Permanent provider error (%s): %s", failureType, result.getResultMessage()),
                            null
                    );
                }

                handleProcessingFailure(queueItem, attempt, merchant, result.getResultCode(), result.getResultMessage(), isRetryable, null);
                return false;
            }

        } catch (Exception ex) {
            log.error("Execution error processing queue item id={}: {}", queueItemIdStr, ex.getMessage(), ex);
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            boolean isTransient = isTransientError(null, ex.getMessage(), ex) || ProviderErrorClassifier.isRetryable(failureType);

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_PROVIDER_DISPATCH_FAILED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryAttempt",
                    attemptIdStr,
                    "PROVIDER_ERROR",
                    String.format("Provider exception (%s): %s", failureType, ex.getMessage()),
                    null
            );

            if (isTransient) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_PROVIDER_RETRYABLE_FAILURE",
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryAttempt",
                        attemptIdStr,
                        "RETRYABLE_ERROR",
                        "Transient execution error: " + ex.getMessage(),
                        null
                );
            } else {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_PROVIDER_PERMANENT_FAILURE",
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryAttempt",
                        attemptIdStr,
                        "PERMANENT_ERROR",
                        "Permanent execution error: " + ex.getMessage(),
                        null
                );
            }

            handleProcessingFailure(queueItem, attempt, merchant, "EXECUTION_ERROR", ex.getMessage(), isTransient, ex);
            return false;
        }
    }

    private void handleProcessingFailure(RecoveryExecutionQueueItem queueItem,
                                         RecoveryAttempt attempt,
                                         Merchant merchant,
                                         String errorCode,
                                         String errorMessage,
                                         boolean isTransient,
                                         Exception cause) {
        Instant now = Instant.now();
        UUID queueItemId = queueItem.getId();
        String queueItemIdStr = queueItemId.toString();
        String attemptIdStr = attempt.getId() != null ? attempt.getId().toString() : "UNKNOWN";

        int currentRetry = queueItem.getRetryCount();
        int maxRetries = queueItem.getMaxRetries();

        if (isTransient && currentRetry < maxRetries) {
            long delaySec = calculateRetryDelay(currentRetry);
            Instant nextAvailableAt = now.plusSeconds(delaySec);

            queueRepository.rescheduleForRetry(queueItemId, nextAvailableAt, errorCode, errorMessage, now);

            // Revert attempt back to SCHEDULED with scheduledAt = nextAvailableAt so attempt identity remains stable
            attempt.setStatus(RecoveryAttemptStatus.SCHEDULED);
            attempt.setScheduledAt(nextAvailableAt);
            attempt.setResultCode(errorCode);
            attempt.setResultMessage("Retry scheduled (" + (currentRetry + 1) + "/" + maxRetries + "): " + errorMessage);
            recoveryAttemptRepository.save(attempt);

            log.warn("Rescheduled queue item id={} for retry #{}/{} at {} (error: {})",
                    queueItemIdStr, currentRetry + 1, maxRetries, nextAvailableAt, errorMessage);

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_EXECUTION_RETRY_SCHEDULED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryExecutionQueueItem",
                    queueItemIdStr,
                    "RETRY_SCHEDULED",
                    String.format("Scheduled retry #%d for %s (error: %s)",
                            currentRetry + 1, nextAvailableAt, errorMessage),
                    null
            );
        } else {
            // Either retry exhaustion or permanent failure
            boolean exhausted = isTransient && currentRetry >= maxRetries;
            String eventType = exhausted ? "RECOVERY_EXECUTION_DEAD_LETTERED" : "RECOVERY_EXECUTION_FAILED";

            queueRepository.moveToDeadLetter(queueItemId, errorCode, errorMessage, now);

            attempt.setStatus(RecoveryAttemptStatus.FAILED);
            attempt.setCompletedAt(now);
            attempt.setResultCode(errorCode != null ? errorCode : "EXECUTION_ERROR");
            attempt.setResultMessage(errorMessage);
            recoveryAttemptRepository.save(attempt);

            log.error("Moved queue item id={} to DEAD_LETTER (exhausted={}, error: {})",
                    queueItemIdStr, exhausted, errorMessage);

            auditService.recordEvent(
                    merchant,
                    eventType,
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryExecutionQueueItem",
                    queueItemIdStr,
                    exhausted ? "DEAD_LETTER" : "FAIL_EXECUTION",
                    String.format("Queue execution failed permanently (%s): %s",
                            exhausted ? "retries exhausted" : "business error", errorMessage),
                    null
            );

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_ATTEMPT_FAILED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryAttempt",
                    attemptIdStr,
                    "FAIL_ATTEMPT",
                    String.format("Attempt failed: %s", errorMessage),
                    null
            );

            // Audit fallback failure if the failing attempt was itself a fallback
            boolean wasFallbackAttempt = (attempt.getAttemptNumber() > 1)
                    || (attempt.getMetadata() != null && attempt.getMetadata().contains("\"isFallback\":true"));
            if (wasFallbackAttempt) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_EXECUTION_FALLBACK_FAILED",
                        ActorType.SYSTEM,
                        properties.getWorkerId(),
                        "RecoveryAttempt",
                        attemptIdStr,
                        "FAIL_FALLBACK",
                        String.format("Fallback execution on channel %s failed for attempt #%d: %s",
                                attempt.getChannel(), attempt.getAttemptNumber(), errorMessage),
                        null
                );
            }

            // Dispatch HIGH_PRIORITY_FAILURE notification if case priority is HIGH or CRITICAL
            RecoveryCase recoveryCase = queueItem.getRecoveryCase();
            if (notificationService != null && recoveryCase != null
                    && (recoveryCase.getPriority() == com.recoverai.backend.entity.enums.RecoveryPriority.HIGH
                    || recoveryCase.getPriority() == com.recoverai.backend.entity.enums.RecoveryPriority.CRITICAL)) {
                try {
                    notificationService.notifyHighPriorityFailure(merchant, recoveryCase, attempt, errorMessage);
                } catch (Exception notifEx) {
                    log.error("Failed to dispatch HIGH_PRIORITY_FAILURE notification for case {}: {}",
                            recoveryCase.getId(), notifEx.getMessage());
                }
            }

            // Trigger deterministic Strategy Fallback
            triggerStrategyFallbackIfEligible(queueItem, attempt, merchant, errorCode, errorMessage);
        }
    }

    private void triggerStrategyFallbackIfEligible(RecoveryExecutionQueueItem queueItem,
                                                   RecoveryAttempt failedAttempt,
                                                   Merchant merchant,
                                                   String errorCode,
                                                   String errorMessage) {
        RecoveryCase recoveryCase = queueItem.getRecoveryCase();
        String attemptIdStr = failedAttempt.getId() != null ? failedAttempt.getId().toString() : "UNKNOWN";

        // 1. Guard: Check terminal recovery case
        if (recoveryCase == null || TERMINAL_CASE_STATUSES.contains(recoveryCase.getStatus())) {
            log.info("RecoveryCase id={} is terminal ({}), aborting fallback strategy selection",
                    recoveryCase != null ? recoveryCase.getId() : null,
                    recoveryCase != null ? recoveryCase.getStatus() : null);
            return;
        }

        // 2. Guard: Strict multi-tenant isolation
        if (recoveryCase.getMerchant() == null || !recoveryCase.getMerchant().getId().equals(merchant.getId())) {
            log.error("Tenant mismatch between recovery case and merchant during fallback for attempt id={}", attemptIdStr);
            return;
        }

        // 3. Guard: Strategy fallback enabled configuration
        if (strategyProperties != null && !strategyProperties.isFallbackEnabled()) {
            log.info("Strategy fallback is disabled by configuration, skipping fallback for case id={}", recoveryCase.getId());
            return;
        }

        // 4. Inspect strategy snapshot and strategy
        RecoveryStrategy strategy = failedAttempt.getStrategy();
        RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.fromJson(failedAttempt.getStrategySnapshot());

        int maxAttempts = 3;
        if (strategy != null && strategy.getMaxAttempts() > 0) {
            maxAttempts = strategy.getMaxAttempts();
        } else if (strategyProperties != null && strategyProperties.getMaxAttempts() > 0) {
            maxAttempts = strategyProperties.getMaxAttempts();
        }

        List<RecoveryAttempt> previousAttempts = recoveryAttemptRepository
                .findByRecoveryCaseIdOrderByAttemptNumberAsc(recoveryCase.getId());
        int previousAttemptCount = previousAttempts != null ? previousAttempts.size() : 1;

        if (previousAttemptCount >= maxAttempts) {
            log.info("Maximum recovery attempts reached ({}/{}) for case id={}, exhausting fallback",
                    previousAttemptCount, maxAttempts, recoveryCase.getId());
            auditService.recordEvent(
                    merchant,
                    "RECOVERY_STRATEGY_FALLBACK_EXHAUSTED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryCase",
                    recoveryCase.getId().toString(),
                    "EXHAUST_FALLBACK",
                    String.format("Maximum recovery attempts (%d) reached for case %s; no further fallback attempts will be scheduled",
                            maxAttempts, recoveryCase.getId()),
                    null
            );

            if (notificationService != null) {
                try {
                    notificationService.notifyCaseExhausted(merchant, recoveryCase,
                            String.format("Maximum recovery attempts (%d) reached", maxAttempts));
                } catch (Exception notifEx) {
                    log.error("Failed to dispatch CASE_EXHAUSTED notification for case {}: {}",
                            recoveryCase.getId(), notifEx.getMessage());
                }
            }

            return;
        }

        // 5. Compute channel failure counts from previous attempts
        Map<RecoveryChannel, Integer> channelFailures = new EnumMap<>(RecoveryChannel.class);
        if (previousAttempts != null) {
            for (RecoveryAttempt prev : previousAttempts) {
                if (prev.getStatus() == RecoveryAttemptStatus.FAILED && prev.getChannel() != null) {
                    channelFailures.put(prev.getChannel(), channelFailures.getOrDefault(prev.getChannel(), 0) + 1);
                }
            }
        }
        if (failedAttempt.getChannel() != null) {
            channelFailures.put(failedAttempt.getChannel(), channelFailures.getOrDefault(failedAttempt.getChannel(), 0) + 1);
        }

        int maxChannelFailures = strategyProperties != null && strategyProperties.getMaxChannelFailures() > 0
                ? strategyProperties.getMaxChannelFailures() : 1;

        Customer customer = recoveryCase.getCustomer();
        boolean hasPhone = customer != null && customer.getPhone() != null && !customer.getPhone().trim().isEmpty();
        boolean hasEmail = customer != null && customer.getEmail() != null && !customer.getEmail().trim().isEmpty();

        // 6. Resolve next fallback channel deterministically
        RecoveryChannel selectedFallbackChannel = null;
        String selectedFallbackAction = null;

        // Try snapshot fallback first if designated and viable
        if (snapshot != null && snapshot.getFallbackChannel() != null) {
            RecoveryChannel candidate = snapshot.getFallbackChannel();
            if (isChannelViable(candidate, hasPhone, hasEmail, channelFailures, maxChannelFailures)) {
                selectedFallbackChannel = candidate;
                selectedFallbackAction = snapshot.getFallbackAction() != null
                        ? snapshot.getFallbackAction()
                        : defaultActionForChannel(candidate);
            }
        }

        // If snapshot fallback was not viable or absent, use the deterministic hierarchy:
        // WHATSAPP -> EMAIL -> SMS -> SMART_LINK -> MANUAL
        if (selectedFallbackChannel == null) {
            List<RecoveryChannel> hierarchy = List.of(
                    RecoveryChannel.WHATSAPP,
                    RecoveryChannel.EMAIL,
                    RecoveryChannel.SMS,
                    RecoveryChannel.SMART_LINK,
                    RecoveryChannel.MANUAL
            );

            for (RecoveryChannel candidate : hierarchy) {
                if (candidate != failedAttempt.getChannel() && isChannelViable(candidate, hasPhone, hasEmail, channelFailures, maxChannelFailures)) {
                    selectedFallbackChannel = candidate;
                    selectedFallbackAction = defaultActionForChannel(candidate);
                    break;
                }
            }
        }

        // 7. If no viable fallback channel remains, record exhaustion
        if (selectedFallbackChannel == null) {
            log.info("No viable fallback channel remains for case id={}, exhausting fallback", recoveryCase.getId());
            auditService.recordEvent(
                    merchant,
                    "RECOVERY_STRATEGY_FALLBACK_EXHAUSTED",
                    ActorType.SYSTEM,
                    properties.getWorkerId(),
                    "RecoveryCase",
                    recoveryCase.getId().toString(),
                    "EXHAUST_FALLBACK",
                    String.format("All fallback channels exhausted for case %s (phoneAvailable=%s, emailAvailable=%s)",
                            recoveryCase.getId(), hasPhone, hasEmail),
                    null
            );

            if (notificationService != null) {
                try {
                    notificationService.notifyCaseExhausted(merchant, recoveryCase,
                            String.format("All fallback channels exhausted (phoneAvailable=%s, emailAvailable=%s)",
                                    hasPhone, hasEmail));
                } catch (Exception notifEx) {
                    log.error("Failed to dispatch CASE_EXHAUSTED notification for case {}: {}",
                            recoveryCase.getId(), notifEx.getMessage());
                }
            }

            return;
        }

        // 8. Create next strategy snapshot & attempt deterministically
        int nextAttemptNumber = previousAttemptCount + 1;
        Instant now = Instant.now();

        RecoveryChannel nextFallback = computeNextFallbackInHierarchy(selectedFallbackChannel);
        String nextFallbackAction = defaultActionForChannel(nextFallback);

        RecoveryStrategySnapshot nextSnapshot = RecoveryStrategySnapshot.builder()
                .strategyId(snapshot != null && snapshot.getStrategyId() != null
                        ? snapshot.getStrategyId()
                        : (strategy != null ? strategy.getId() : null))
                .channel(selectedFallbackChannel)
                .recommendedAction(selectedFallbackAction)
                .confidenceScore(snapshot != null ? snapshot.getConfidenceScore() : java.math.BigDecimal.valueOf(0.50))
                .priority(snapshot != null ? snapshot.getPriority() : RecoveryPriority.MEDIUM)
                .fallbackChannel(nextFallback)
                .fallbackAction(nextFallbackAction)
                .reason(String.format("Deterministic fallback to %s following execution failure on %s",
                        selectedFallbackChannel, failedAttempt.getChannel()))
                .build();

        String metadataJson = String.format("{\"isFallback\":true,\"fallbackFromAttemptId\":\"%s\",\"previousChannel\":\"%s\"}",
                failedAttempt.getId(), failedAttempt.getChannel());

        RecoveryAttempt fallbackAttempt = RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .strategy(strategy)
                .strategySnapshot(nextSnapshot.toJson())
                .attemptNumber(nextAttemptNumber)
                .channel(selectedFallbackChannel)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(now)
                .metadata(metadataJson)
                .build();

        RecoveryAttempt savedAttempt = recoveryAttemptRepository.save(fallbackAttempt);
        if (savedAttempt == null) {
            savedAttempt = fallbackAttempt;
        }
        if (savedAttempt.getId() == null) {
            savedAttempt.setId(UUID.randomUUID());
        }
        String newAttemptIdStr = savedAttempt.getId().toString();

        // 9. Enqueue into durable execution queue
        enqueueAttempt(savedAttempt, now);

        // 10. Record audit event: RECOVERY_STRATEGY_FALLBACK_SELECTED
        auditService.recordEvent(
                merchant,
                "RECOVERY_STRATEGY_FALLBACK_SELECTED",
                ActorType.SYSTEM,
                properties.getWorkerId(),
                "RecoveryAttempt",
                newAttemptIdStr,
                "SELECT_FALLBACK",
                String.format("Fallback channel %s (action: %s) selected for attempt #%d following failure on %s",
                        selectedFallbackChannel, selectedFallbackAction, nextAttemptNumber, failedAttempt.getChannel()),
                null
        );

        log.info("Scheduled and enqueued fallback attempt id={} (attempt #{}, channel={}) for case id={}",
                newAttemptIdStr, nextAttemptNumber, selectedFallbackChannel, recoveryCase.getId());
    }

    private boolean isChannelViable(RecoveryChannel channel,
                                    boolean hasPhone,
                                    boolean hasEmail,
                                    Map<RecoveryChannel, Integer> channelFailures,
                                    int maxChannelFailures) {
        if (channel == null) {
            return false;
        }
        int failures = channelFailures.getOrDefault(channel, 0);
        if (failures >= maxChannelFailures) {
            return false;
        }
        return switch (channel) {
            case WHATSAPP, SMS -> hasPhone;
            case EMAIL -> hasEmail;
            case SMART_LINK -> hasPhone || hasEmail;
            case RETRY_CHARGE -> false;
            case MANUAL -> true;
        };
    }

    private RecoveryChannel computeNextFallbackInHierarchy(RecoveryChannel current) {
        if (current == null) {
            return null;
        }
        return switch (current) {
            case WHATSAPP -> RecoveryChannel.EMAIL;
            case EMAIL -> RecoveryChannel.SMS;
            case SMS -> RecoveryChannel.SMART_LINK;
            case SMART_LINK -> RecoveryChannel.MANUAL;
            case RETRY_CHARGE -> RecoveryChannel.WHATSAPP;
            case MANUAL -> null;
        };
    }

    private String defaultActionForChannel(RecoveryChannel channel) {
        if (channel == null) {
            return "EXECUTE_RECOVERY";
        }
        return switch (channel) {
            case WHATSAPP -> "SEND_WHATSAPP_REMINDER";
            case EMAIL -> "SEND_EMAIL_REMINDER";
            case SMS -> "SEND_SMS_REMINDER";
            case SMART_LINK -> "SEND_PAYMENT_LINK";
            case RETRY_CHARGE -> "RETRY_CHARGE_IMMEDIATE";
            case MANUAL -> "MANUAL_ESCALATION";
        };
    }

    public boolean isTransientError(String errorCode, String errorMessage, Exception ex) {
        if (ex != null) {
            String exMsg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (exMsg.contains("timeout") || exMsg.contains("connection") || exMsg.contains("network")
                    || exMsg.contains("unavailable") || exMsg.contains("502") || exMsg.contains("503")
                    || exMsg.contains("504") || exMsg.contains("rate limit") || exMsg.contains("temporary")
                    || exMsg.contains("retryable")) {
                return true;
            }
        }
        if (errorCode != null) {
            String code = errorCode.toUpperCase();
            if (code.contains("TIMEOUT") || code.contains("NETWORK") || code.contains("TEMPORARY")
                    || code.contains("PROVIDER_UNAVAILABLE") || code.contains("RATE_LIMITED")
                    || code.contains("GATEWAY_TIMEOUT") || code.contains("SERVICE_UNAVAILABLE")) {
                return true;
            }
        }
        if (errorMessage != null) {
            String msg = errorMessage.toLowerCase();
            if (msg.contains("timeout") || msg.contains("connection") || msg.contains("network")
                    || msg.contains("unavailable") || msg.contains("temporary") || msg.contains("retry")) {
                return true;
            }
        }
        return false;
    }

    public long calculateRetryDelay(int currentRetry) {
        long baseDelay = 60L;
        long maxDelay = 3600L;

        if (communicationProperties != null && communicationProperties.getRetry() != null) {
            if (communicationProperties.getRetry().getBaseDelaySeconds() > 0) {
                baseDelay = communicationProperties.getRetry().getBaseDelaySeconds();
            }
            if (communicationProperties.getRetry().getMaxDelaySeconds() > 0) {
                maxDelay = communicationProperties.getRetry().getMaxDelaySeconds();
            }
        } else if (properties != null && properties.getRetryDelaySeconds() > 0) {
            baseDelay = properties.getRetryDelaySeconds();
        }

        long multiplier = 1L << Math.min(Math.max(0, currentRetry), 20);
        long exponentialDelay = baseDelay * multiplier;
        return Math.min(exponentialDelay, maxDelay);
    }

    @Transactional
    public int requeueStaleClaims() {
        long claimTimeoutSec = properties.getClaimTimeoutSeconds() > 0 ? properties.getClaimTimeoutSeconds() : 300L;
        Instant staleThreshold = Instant.now().minusSeconds(claimTimeoutSec);

        List<UUID> staleIds = queueRepository.findStaleClaimIds(staleThreshold);
        if (staleIds.isEmpty()) {
            return 0;
        }

        log.warn("Found {} stale/abandoned claimed queue items older than threshold {}", staleIds.size(), staleThreshold);
        int requeuedCount = 0;
        Instant now = Instant.now();

        for (UUID staleId : staleIds) {
            int updated = queueRepository.requeueStaleClaim(staleId, now);
            if (updated > 0) {
                requeuedCount++;
                log.info("Requeued stale queue item id={} back to READY", staleId);
            }
        }

        return requeuedCount;
    }

    @Transactional(readOnly = true)
    public List<UUID> findDueReadyItemIds(int batchSize) {
        int effectiveBatch = Math.max(1, batchSize);
        return queueRepository.findDueReadyItemIds(Instant.now(), PageRequest.of(0, effectiveBatch));
    }

    @Transactional(readOnly = true)
    public Optional<RecoveryExecutionQueueItem> getQueueItem(UUID merchantId, UUID queueItemId) {
        if (merchantId == null || queueItemId == null) {
            return Optional.empty();
        }
        return queueRepository.findByIdAndMerchantId(queueItemId, merchantId);
    }

    @Transactional(readOnly = true)
    public Page<RecoveryExecutionQueueItem> getMerchantQueueItems(UUID merchantId, Pageable pageable) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        return queueRepository.findByMerchantId(merchantId, pageable);
    }

    @Transactional(readOnly = true)
    public long getQueueDepth() {
        return queueRepository.countByStatus(RecoveryQueueStatus.READY);
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
}
