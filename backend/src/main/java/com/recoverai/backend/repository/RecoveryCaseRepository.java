package com.recoverai.backend.repository;

import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.repository.projection.DashboardSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase, UUID>, JpaSpecificationExecutor<RecoveryCase> {

    List<RecoveryCase> findByMerchantId(UUID merchantId);

    Page<RecoveryCase> findByMerchantId(UUID merchantId, Pageable pageable);

    List<RecoveryCase> findByMerchantIdAndStatus(UUID merchantId, RecoveryCaseStatus status);

    Page<RecoveryCase> findByMerchantIdAndStatus(UUID merchantId, RecoveryCaseStatus status, Pageable pageable);

    List<RecoveryCase> findByMerchantIdAndPriority(UUID merchantId, RecoveryPriority priority);

    Optional<RecoveryCase> findByPaymentId(UUID paymentId);

    Optional<RecoveryCase> findByIdAndMerchantId(UUID id, UUID merchantId);

    List<RecoveryCase> findByMerchantIdAndCustomerId(UUID merchantId, UUID customerId);

    long countByMerchantIdAndStatus(UUID merchantId, RecoveryCaseStatus status);

    @Query("SELECT " +
            "COUNT(rc) AS totalCases, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.OPEN THEN 1L ELSE 0L END) AS openCases, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.IN_PROGRESS THEN 1L ELSE 0L END) AS inProgressCases, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.RECOVERED THEN 1L ELSE 0L END) AS recoveredCases, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.EXPIRED THEN 1L ELSE 0L END) AS expiredCases, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.CANCELLED THEN 1L ELSE 0L END) AS cancelledCases, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.FAILED THEN 1L ELSE 0L END) AS failedCases, " +
            "COALESCE(SUM(rc.estimatedRecoverableAmount), 0.0) AS totalEstimatedRecoverableAmount, " +
            "COALESCE(SUM(rc.recoveredAmount), 0.0) AS totalRecoveredAmount " +
            "FROM RecoveryCase rc WHERE rc.merchant.id = :merchantId")
    DashboardSummaryProjection getDashboardSummary(@Param("merchantId") UUID merchantId);
}
