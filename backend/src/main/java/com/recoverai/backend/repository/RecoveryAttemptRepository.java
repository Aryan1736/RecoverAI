package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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

    long countByRecoveryCaseId(UUID recoveryCaseId);
}
