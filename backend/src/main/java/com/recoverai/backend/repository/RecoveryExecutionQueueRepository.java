package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryExecutionQueueRepository extends JpaRepository<RecoveryExecutionQueueItem, UUID> {

    Optional<RecoveryExecutionQueueItem> findByRecoveryAttemptId(UUID recoveryAttemptId);

    boolean existsByRecoveryAttemptId(UUID recoveryAttemptId);

    boolean existsByRecoveryAttemptIdAndStatusIn(UUID recoveryAttemptId, Collection<RecoveryQueueStatus> statuses);

    Optional<RecoveryExecutionQueueItem> findByIdAndMerchantId(UUID id, UUID merchantId);

    List<RecoveryExecutionQueueItem> findByMerchantId(UUID merchantId);

    List<RecoveryExecutionQueueItem> findByMerchantIdAndStatus(UUID merchantId, RecoveryQueueStatus status);

    Page<RecoveryExecutionQueueItem> findByMerchantId(UUID merchantId, Pageable pageable);

    long countByStatus(RecoveryQueueStatus status);

    long countByMerchantIdAndStatus(UUID merchantId, RecoveryQueueStatus status);

    @Query("SELECT q.id FROM RecoveryExecutionQueueItem q WHERE q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY AND q.availableAt <= :now ORDER BY q.availableAt ASC")
    List<UUID> findDueReadyItemIds(@Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("UPDATE RecoveryExecutionQueueItem q SET q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED, " +
            "q.claimedAt = :now, q.claimedBy = :workerId, q.updatedAt = :now " +
            "WHERE q.id = :id AND q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY")
    int claimItem(@Param("id") UUID id, @Param("workerId") String workerId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RecoveryExecutionQueueItem q SET q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.PROCESSING, " +
            "q.startedAt = :now, q.updatedAt = :now " +
            "WHERE q.id = :id AND (q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY)")
    int markProcessing(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RecoveryExecutionQueueItem q SET q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.COMPLETED, " +
            "q.completedAt = :now, q.updatedAt = :now " +
            "WHERE q.id = :id AND (q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.PROCESSING OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY)")
    int markCompleted(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RecoveryExecutionQueueItem q SET q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY, " +
            "q.retryCount = q.retryCount + 1, q.availableAt = :nextAvailableAt, q.claimedAt = NULL, q.claimedBy = NULL, q.startedAt = NULL, " +
            "q.lastErrorCode = :errorCode, q.lastErrorMessage = :errorMessage, q.updatedAt = :now " +
            "WHERE q.id = :id AND (q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.PROCESSING OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY)")
    int rescheduleForRetry(@Param("id") UUID id,
                           @Param("nextAvailableAt") Instant nextAvailableAt,
                           @Param("errorCode") String errorCode,
                           @Param("errorMessage") String errorMessage,
                           @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RecoveryExecutionQueueItem q SET q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.DEAD_LETTER, " +
            "q.completedAt = :now, q.lastErrorCode = :errorCode, q.lastErrorMessage = :errorMessage, q.updatedAt = :now " +
            "WHERE q.id = :id AND (q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.PROCESSING OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY)")
    int moveToDeadLetter(@Param("id") UUID id,
                         @Param("errorCode") String errorCode,
                         @Param("errorMessage") String errorMessage,
                         @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RecoveryExecutionQueueItem q SET q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.FAILED, " +
            "q.completedAt = :now, q.lastErrorCode = :errorCode, q.lastErrorMessage = :errorMessage, q.updatedAt = :now " +
            "WHERE q.id = :id AND (q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.PROCESSING OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED)")
    int markFailed(@Param("id") UUID id,
                   @Param("errorCode") String errorCode,
                   @Param("errorMessage") String errorMessage,
                   @Param("now") Instant now);

    @Query("SELECT q.id FROM RecoveryExecutionQueueItem q " +
            "WHERE (q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.PROCESSING) " +
            "AND ((q.claimedAt IS NOT NULL AND q.claimedAt <= :staleThreshold) OR (q.startedAt IS NOT NULL AND q.startedAt <= :staleThreshold))")
    List<UUID> findStaleClaimIds(@Param("staleThreshold") Instant staleThreshold);

    @Modifying
    @Query("UPDATE RecoveryExecutionQueueItem q SET q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.READY, " +
            "q.claimedAt = NULL, q.claimedBy = NULL, q.startedAt = NULL, q.updatedAt = :now " +
            "WHERE q.id = :id AND (q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.CLAIMED OR q.status = com.recoverai.backend.entity.enums.RecoveryQueueStatus.PROCESSING)")
    int requeueStaleClaim(@Param("id") UUID id, @Param("now") Instant now);
}
