package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryAttemptRepository extends JpaRepository<RecoveryAttempt, UUID> {

    List<RecoveryAttempt> findByRecoveryCaseId(UUID recoveryCaseId);

    List<RecoveryAttempt> findByRecoveryCaseIdOrderByAttemptNumberAsc(UUID recoveryCaseId);
    
    List<RecoveryAttempt> findByRecoveryCaseIdAndMerchantIdOrderByAttemptNumberAsc(UUID recoveryCaseId, UUID merchantId);

    List<RecoveryAttempt> findByRecoveryCaseIdAndStatus(UUID recoveryCaseId, RecoveryAttemptStatus status);

    List<RecoveryAttempt> findByMerchantId(UUID merchantId);

    Page<RecoveryAttempt> findByMerchantId(UUID merchantId, Pageable pageable);

    List<RecoveryAttempt> findByMerchantIdAndStatus(UUID merchantId, RecoveryAttemptStatus status);

    Optional<RecoveryAttempt> findByIdAndMerchantId(UUID id, UUID merchantId);

    Optional<RecoveryAttempt> findByRecoveryCaseIdAndAttemptNumber(UUID recoveryCaseId, int attemptNumber);

    Optional<RecoveryAttempt> findTopByRecoveryCaseIdOrderByAttemptNumberDesc(UUID recoveryCaseId);

    boolean existsByRecoveryCaseIdAndStatusIn(UUID recoveryCaseId, Collection<RecoveryAttemptStatus> statuses);

    long countByRecoveryCaseId(UUID recoveryCaseId);

    @Query("SELECT r.id FROM RecoveryAttempt r WHERE r.status = :status AND (r.scheduledAt IS NULL OR r.scheduledAt <= :now) ORDER BY r.scheduledAt ASC")
    List<UUID> findDueScheduledAttemptIds(@Param("status") RecoveryAttemptStatus status, @Param("now") Instant now, Pageable pageable);

    @org.springframework.transaction.annotation.Transactional
    @Modifying
    @Query("UPDATE RecoveryAttempt r SET r.status = :inFlightStatus, r.executedAt = :now, r.updatedAt = :now WHERE r.id = :id AND r.status = :scheduledStatus")
    int claimAttemptForExecution(@Param("id") UUID id,
                                 @Param("scheduledStatus") RecoveryAttemptStatus scheduledStatus,
                                 @Param("inFlightStatus") RecoveryAttemptStatus inFlightStatus,
                                 @Param("now") Instant now);

    long countByStatusAndScheduledAtLessThanEqual(RecoveryAttemptStatus status, Instant now);
    
    @Query("SELECT " +
            "ra.channel AS channel, " +
            "COUNT(ra) AS totalAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.SUCCESS THEN 1L ELSE 0L END) AS successfulAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.FAILED THEN 1L ELSE 0L END) AS failedAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.SENT THEN 1L ELSE 0L END) AS sentAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.DELIVERED THEN 1L ELSE 0L END) AS deliveredAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.CLICKED THEN 1L ELSE 0L END) AS clickedAttempts, " +
            "COALESCE(SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.SUCCESS THEN ra.recoveryCase.recoveredAmount ELSE 0.0 END), 0.0) AS recoveredAmount " +
            "FROM RecoveryAttempt ra " +
            "WHERE ra.merchant.id = :merchantId AND ra.createdAt >= :from AND ra.createdAt <= :to " +
            "GROUP BY ra.channel " +
            "ORDER BY COUNT(ra) DESC, ra.channel ASC")
    List<com.recoverai.backend.repository.projection.ChannelPerformanceProjection> getChannelPerformanceAnalytics(
            @Param("merchantId") UUID merchantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT " +
            "COUNT(ra) AS totalAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.SUCCESS THEN 1L ELSE 0L END) AS successfulAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.FAILED THEN 1L ELSE 0L END) AS failedAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.SCHEDULED THEN 1L ELSE 0L END) AS scheduledAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.IN_FLIGHT THEN 1L ELSE 0L END) AS inFlightAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.SENT THEN 1L ELSE 0L END) AS sentAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.DELIVERED THEN 1L ELSE 0L END) AS deliveredAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.CLICKED THEN 1L ELSE 0L END) AS clickedAttempts, " +
            "SUM(CASE WHEN ra.status = com.recoverai.backend.entity.enums.RecoveryAttemptStatus.SKIPPED THEN 1L ELSE 0L END) AS skippedAttempts, " +
            "COUNT(DISTINCT ra.recoveryCase.id) AS distinctCasesWithAttempts " +
            "FROM RecoveryAttempt ra " +
            "WHERE ra.merchant.id = :merchantId AND ra.createdAt >= :from AND ra.createdAt <= :to")
    com.recoverai.backend.repository.projection.AttemptSummaryProjection getAttemptSummaryAnalytics(
            @Param("merchantId") UUID merchantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT ra.status AS status, COUNT(ra) AS count " +
            "FROM RecoveryAttempt ra " +
            "WHERE ra.merchant.id = :merchantId AND ra.createdAt >= :from AND ra.createdAt <= :to " +
            "GROUP BY ra.status")
    List<com.recoverai.backend.repository.projection.StatusCountProjection> countAttemptsByStatus(
            @Param("merchantId") UUID merchantId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT ra.channel AS channel, COUNT(ra) AS count " +
            "FROM RecoveryAttempt ra " +
            "WHERE ra.merchant.id = :merchantId AND ra.createdAt >= :from AND ra.createdAt <= :to " +
            "GROUP BY ra.channel")
    List<com.recoverai.backend.repository.projection.ChannelCountProjection> countAttemptsByChannel(
            @Param("merchantId") UUID merchantId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
