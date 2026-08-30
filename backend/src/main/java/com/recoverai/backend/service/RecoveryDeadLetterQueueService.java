package com.recoverai.backend.service;

import com.recoverai.backend.dto.queue.DeadLetterQueueItemResponseDto;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.exception.DeadLetterQueueItemNotFoundException;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoveryDeadLetterQueueService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryDeadLetterQueueService.class);

    private static final Set<RecoveryCaseStatus> TERMINAL_CASE_STATUSES = Set.of(
            RecoveryCaseStatus.RECOVERED,
            RecoveryCaseStatus.CANCELLED,
            RecoveryCaseStatus.EXPIRED
    );

    private final RecoveryExecutionQueueRepository queueRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final AuditService auditService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public RecoveryDeadLetterQueueService(RecoveryExecutionQueueRepository queueRepository,
                                         RecoveryAttemptRepository recoveryAttemptRepository,
                                         AuditService auditService) {
        this.queueRepository = queueRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<DeadLetterQueueItemResponseDto> getDeadLetterItems(UUID merchantId,
                                                                   UUID caseId,
                                                                   String errorCode,
                                                                   Pageable pageable) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }

        Page<RecoveryExecutionQueueItem> items = queueRepository.findDeadLetterItems(
                merchantId, caseId, errorCode, pageable);

        return items.map(DeadLetterQueueItemResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public DeadLetterQueueItemResponseDto getDeadLetterItem(UUID merchantId, UUID queueItemId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (queueItemId == null) {
            throw new IllegalArgumentException("Queue item ID cannot be null");
        }

        // Enforce tenant isolation: return 404 if item does not exist or belongs to another merchant
        RecoveryExecutionQueueItem item = queueRepository.findByIdAndMerchantIdAndStatus(
                queueItemId, merchantId, RecoveryQueueStatus.DEAD_LETTER)
                .orElseThrow(() -> new DeadLetterQueueItemNotFoundException(
                        "Dead-letter queue item not found: " + queueItemId));

        return DeadLetterQueueItemResponseDto.fromEntity(item);
    }

    @Transactional
    public DeadLetterQueueItemResponseDto redriveDeadLetterItem(UUID merchantId, UUID queueItemId, String actorId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (queueItemId == null) {
            throw new IllegalArgumentException("Queue item ID cannot be null");
        }

        // 1. Strict tenant lookup: return 404 if not found or cross-tenant access
        RecoveryExecutionQueueItem item = queueRepository.findByIdAndMerchantIdWithDetails(queueItemId, merchantId)
                .orElseThrow(() -> new DeadLetterQueueItemNotFoundException(
                        "Dead-letter queue item not found: " + queueItemId));

        RecoveryCase recoveryCase = item.getRecoveryCase();

        // 2. Terminal case check: Cannot redrive if case is RECOVERED / CANCELLED / EXPIRED
        if (recoveryCase != null && TERMINAL_CASE_STATUSES.contains(recoveryCase.getStatus())) {
            throw new InvalidRecoveryCaseStateException(
                    "Cannot redrive dead-letter item for recovery case in terminal status: " + recoveryCase.getStatus());
        }

        // 3. Idempotent check: if already READY, return safe existing state
        if (item.getStatus() == RecoveryQueueStatus.READY) {
            log.info("Queue item id={} is already in READY status, returning idempotent response", queueItemId);
            return DeadLetterQueueItemResponseDto.fromEntity(item);
        }

        // Ensure item is in DEAD_LETTER
        if (item.getStatus() != RecoveryQueueStatus.DEAD_LETTER) {
            throw new InvalidRecoveryCaseStateException(
                    "Only DEAD_LETTER items can be redriven. Current status: " + item.getStatus());
        }

        String queueItemIdStr = queueItemId.toString();
        String effectiveActor = (actorId != null && !actorId.trim().isEmpty()) ? actorId : merchantId.toString();

        // 4. Record audit event: RECOVERY_EXECUTION_REDRIVE_REQUESTED
        auditService.recordEvent(
                item.getMerchant(),
                "RECOVERY_EXECUTION_REDRIVE_REQUESTED",
                ActorType.USER,
                effectiveActor,
                "RecoveryExecutionQueueItem",
                queueItemIdStr,
                "REQUEST_REDRIVE",
                "Redrive requested for dead-letter queue item " + queueItemIdStr,
                null
        );

        // 5. Atomic conditional update: status DEAD_LETTER -> READY with reset counters
        Instant now = Instant.now();
        int updatedRows = queueRepository.redriveItem(queueItemId, merchantId, now, now);

        if (updatedRows > 0) {
            item.setStatus(RecoveryQueueStatus.READY);
            item.setRetryCount(0);
            item.setAvailableAt(now);
            item.setClaimedAt(null);
            item.setClaimedBy(null);
            item.setStartedAt(null);
            item.setCompletedAt(null);
            item.setLastErrorCode(null);
            item.setLastErrorMessage(null);
            item.setUpdatedAt(now);

            // First winner in concurrent execution: update associated attempt and record audit
            RecoveryAttempt attempt = item.getRecoveryAttempt();
            if (attempt != null) {
                attempt.setStatus(RecoveryAttemptStatus.SCHEDULED);
                attempt.setScheduledAt(now);
                attempt.setCompletedAt(null);
                attempt.setResultCode(null);
                attempt.setResultMessage("Redriven from dead-letter queue");
                recoveryAttemptRepository.save(attempt);
            }

            auditService.recordEvent(
                    item.getMerchant(),
                    "RECOVERY_EXECUTION_REDRIVEN",
                    ActorType.USER,
                    effectiveActor,
                    "RecoveryExecutionQueueItem",
                    queueItemIdStr,
                    "EXECUTE_REDRIVE",
                    "Queue item successfully redriven from DEAD_LETTER to READY",
                    null
            );

            log.info("Queue item id={} successfully redriven to READY by actor={}", queueItemIdStr, effectiveActor);
            return DeadLetterQueueItemResponseDto.fromEntity(item);
        } else {
            log.warn("Concurrent duplicate redrive on queue item id={}, returning existing state", queueItemIdStr);
            if (entityManager != null) {
                entityManager.clear();
            }
            RecoveryExecutionQueueItem reloaded = queueRepository.findByIdAndMerchantIdWithDetails(queueItemId, merchantId)
                    .orElse(item);
            return DeadLetterQueueItemResponseDto.fromEntity(reloaded);
        }
    }
}
