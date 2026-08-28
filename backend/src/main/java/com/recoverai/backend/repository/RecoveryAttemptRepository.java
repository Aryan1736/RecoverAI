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
}
