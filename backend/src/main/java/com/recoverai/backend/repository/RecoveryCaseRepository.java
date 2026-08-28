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
            "FROM RecoveryCase rc " +
            "WHERE rc.merchant.id = :merchantId AND rc.createdAt >= :from AND rc.createdAt <= :to")
    com.recoverai.backend.repository.projection.AnalyticsOverviewProjection getAnalyticsOverview(
            @Param("merchantId") UUID merchantId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query("SELECT " +
            "CAST(rc.createdAt AS LocalDate) AS date, " +
            "COUNT(rc) AS recoveryCasesCreated, " +
            "COALESCE(SUM(rc.estimatedRecoverableAmount), 0.0) AS amountAtRisk, " +
            "COALESCE(SUM(rc.recoveredAmount), 0.0) AS amountRecovered, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.RECOVERED THEN 1L ELSE 0L END) AS recoveredCaseCount " +
            "FROM RecoveryCase rc " +
            "WHERE rc.merchant.id = :merchantId AND rc.createdAt >= :from AND rc.createdAt <= :to " +
            "GROUP BY CAST(rc.createdAt AS LocalDate) " +
            "ORDER BY CAST(rc.createdAt AS LocalDate) ASC")
    java.util.List<com.recoverai.backend.repository.projection.DailyRecoveryTrendProjection> getDailyRecoveryTrends(
            @Param("merchantId") UUID merchantId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query("SELECT " +
            "COALESCE(rc.failureReasonCategory, 'UNKNOWN') AS failureReasonCategory, " +
            "COUNT(rc) AS caseCount, " +
            "COALESCE(SUM(rc.estimatedRecoverableAmount), 0.0) AS estimatedRecoverableAmount, " +
            "COALESCE(SUM(rc.recoveredAmount), 0.0) AS recoveredAmount, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.RECOVERED THEN 1L ELSE 0L END) AS recoveredCaseCount " +
            "FROM RecoveryCase rc " +
            "WHERE rc.merchant.id = :merchantId AND rc.createdAt >= :from AND rc.createdAt <= :to " +
            "GROUP BY COALESCE(rc.failureReasonCategory, 'UNKNOWN') " +
            "ORDER BY COUNT(rc) DESC, COALESCE(rc.failureReasonCategory, 'UNKNOWN') ASC")
    java.util.List<com.recoverai.backend.repository.projection.FailureCategoryProjection> getFailureCategoryAnalytics(
            @Param("merchantId") UUID merchantId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query("SELECT " +
            "rc.priority AS priority, " +
            "COUNT(rc) AS caseCount, " +
            "COALESCE(SUM(rc.estimatedRecoverableAmount), 0.0) AS estimatedRecoverableAmount, " +
            "COALESCE(SUM(rc.recoveredAmount), 0.0) AS recoveredAmount, " +
            "SUM(CASE WHEN rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.RECOVERED THEN 1L ELSE 0L END) AS recoveredCaseCount " +
            "FROM RecoveryCase rc " +
            "WHERE rc.merchant.id = :merchantId AND rc.createdAt >= :from AND rc.createdAt <= :to " +
            "GROUP BY rc.priority " +
            "ORDER BY COUNT(rc) DESC, rc.priority ASC")
    java.util.List<com.recoverai.backend.repository.projection.FailurePriorityProjection> getFailurePriorityAnalytics(
            @Param("merchantId") UUID merchantId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query("SELECT COUNT(rc) FROM RecoveryCase rc WHERE rc.merchant.id = :merchantId AND rc.createdAt >= :from AND rc.createdAt <= :to")
    long countByMerchantIdAndCreatedAtBetween(
            @Param("merchantId") UUID merchantId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);

    @Query("SELECT rc.createdAt, rc.recoveredAt FROM RecoveryCase rc " +
            "WHERE rc.merchant.id = :merchantId AND rc.status = com.recoverai.backend.entity.enums.RecoveryCaseStatus.RECOVERED " +
            "AND rc.recoveredAt IS NOT NULL AND rc.createdAt >= :from AND rc.createdAt <= :to")
    java.util.List<Object[]> findRecoveredTimestamps(
            @Param("merchantId") UUID merchantId,
            @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to);
}
