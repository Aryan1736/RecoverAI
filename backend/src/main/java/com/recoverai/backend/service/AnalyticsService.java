package com.recoverai.backend.service;

import com.recoverai.backend.dto.analytics.AnalyticsOverviewResponseDto;
import com.recoverai.backend.dto.analytics.AttemptAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.ChannelAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.ChannelMetricDto;
import com.recoverai.backend.dto.analytics.DailyRecoveryTrendDto;
import com.recoverai.backend.dto.analytics.DateRange;
import com.recoverai.backend.dto.analytics.FailureAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.FailureCategoryMetricDto;
import com.recoverai.backend.dto.analytics.FailurePriorityMetricDto;
import com.recoverai.backend.dto.analytics.RecoveryTrendsResponseDto;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.projection.AnalyticsOverviewProjection;
import com.recoverai.backend.repository.projection.AttemptSummaryProjection;
import com.recoverai.backend.repository.projection.ChannelCountProjection;
import com.recoverai.backend.repository.projection.ChannelPerformanceProjection;
import com.recoverai.backend.repository.projection.DailyRecoveryTrendProjection;
import com.recoverai.backend.repository.projection.FailureCategoryProjection;
import com.recoverai.backend.repository.projection.FailurePriorityProjection;
import com.recoverai.backend.repository.projection.StatusCountProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;

    public AnalyticsService(RecoveryCaseRepository recoveryCaseRepository,
                            RecoveryAttemptRepository recoveryAttemptRepository) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponseDto getAnalyticsOverview(UUID merchantId, DateRange dateRange) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(dateRange, "DateRange cannot be null");

        AnalyticsOverviewProjection projection = recoveryCaseRepository.getAnalyticsOverview(
                merchantId, dateRange.from(), dateRange.to());

        long totalCases = projection != null && projection.getTotalCases() != null ? projection.getTotalCases() : 0L;
        long openCases = projection != null && projection.getOpenCases() != null ? projection.getOpenCases() : 0L;
        long inProgressCases = projection != null && projection.getInProgressCases() != null ? projection.getInProgressCases() : 0L;
        long recoveredCases = projection != null && projection.getRecoveredCases() != null ? projection.getRecoveredCases() : 0L;
        long failedCases = projection != null && projection.getFailedCases() != null ? projection.getFailedCases() : 0L;
        long expiredCases = projection != null && projection.getExpiredCases() != null ? projection.getExpiredCases() : 0L;
        long cancelledCases = projection != null && projection.getCancelledCases() != null ? projection.getCancelledCases() : 0L;
        long expiredOrCancelledCases = expiredCases + cancelledCases;

        BigDecimal totalEstimated = projection != null && projection.getTotalEstimatedRecoverableAmount() != null
                ? projection.getTotalEstimatedRecoverableAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalRecovered = projection != null && projection.getTotalRecoveredAmount() != null
                ? projection.getTotalRecoveredAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal recoveryRate = calculatePercentage(recoveredCases, totalCases);

        BigDecimal averageRecoveredAmount;
        if (recoveredCases > 0) {
            averageRecoveredAmount = totalRecovered.divide(BigDecimal.valueOf(recoveredCases), 2, RoundingMode.HALF_UP);
        } else {
            averageRecoveredAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        Double averageTimeToRecoverySeconds = calculateAverageTimeToRecovery(merchantId, dateRange);

        return new AnalyticsOverviewResponseDto(
                totalCases,
                openCases,
                inProgressCases,
                recoveredCases,
                failedCases,
                expiredCases,
                cancelledCases,
                expiredOrCancelledCases,
                totalEstimated,
                totalRecovered,
                recoveryRate,
                averageRecoveredAmount,
                averageTimeToRecoverySeconds,
                dateRange.from(),
                dateRange.to()
        );
    }

    @Transactional(readOnly = true)
    public RecoveryTrendsResponseDto getRecoveryTrends(UUID merchantId, DateRange dateRange) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(dateRange, "DateRange cannot be null");

        List<DailyRecoveryTrendProjection> projections = recoveryCaseRepository.getDailyRecoveryTrends(
                merchantId, dateRange.from(), dateRange.to());

        List<DailyRecoveryTrendDto> trendDtos = new ArrayList<>();
        long totalCases = 0L;
        BigDecimal totalAmountAtRisk = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRecoveredAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long totalRecoveredCases = 0L;

        if (projections != null) {
            for (DailyRecoveryTrendProjection p : projections) {
                long casesCreated = p.getRecoveryCasesCreated() != null ? p.getRecoveryCasesCreated() : 0L;
                BigDecimal atRisk = p.getAmountAtRisk() != null ? p.getAmountAtRisk().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal recovered = p.getAmountRecovered() != null ? p.getAmountRecovered().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                long recoveredCount = p.getRecoveredCaseCount() != null ? p.getRecoveredCaseCount() : 0L;
                BigDecimal dailyRate = calculatePercentage(recoveredCount, casesCreated);

                trendDtos.add(new DailyRecoveryTrendDto(
                        p.getDate(),
                        casesCreated,
                        atRisk,
                        recovered,
                        recoveredCount,
                        dailyRate
                ));

                totalCases += casesCreated;
                totalAmountAtRisk = totalAmountAtRisk.add(atRisk);
                totalRecoveredAmount = totalRecoveredAmount.add(recovered);
                totalRecoveredCases += recoveredCount;
            }
        }

        BigDecimal overallRecoveryRate = calculatePercentage(totalRecoveredCases, totalCases);

        return new RecoveryTrendsResponseDto(
                dateRange.from(),
                dateRange.to(),
                totalCases,
                totalAmountAtRisk,
                totalRecoveredAmount,
                overallRecoveryRate,
                trendDtos
        );
    }

    @Transactional(readOnly = true)
    public FailureAnalyticsResponseDto getFailureAnalytics(UUID merchantId, DateRange dateRange) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(dateRange, "DateRange cannot be null");

        List<FailureCategoryProjection> categoryProjections = recoveryCaseRepository.getFailureCategoryAnalytics(
                merchantId, dateRange.from(), dateRange.to());
        List<FailurePriorityProjection> priorityProjections = recoveryCaseRepository.getFailurePriorityAnalytics(
                merchantId, dateRange.from(), dateRange.to());

        List<FailureCategoryMetricDto> categoryDtos = new ArrayList<>();
        long totalCases = 0L;

        if (categoryProjections != null) {
            for (FailureCategoryProjection cp : categoryProjections) {
                long caseCount = cp.getCaseCount() != null ? cp.getCaseCount() : 0L;
                BigDecimal estimated = cp.getEstimatedRecoverableAmount() != null ? cp.getEstimatedRecoverableAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal recovered = cp.getRecoveredAmount() != null ? cp.getRecoveredAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                long recoveredCount = cp.getRecoveredCaseCount() != null ? cp.getRecoveredCaseCount() : 0L;
                BigDecimal rate = calculatePercentage(recoveredCount, caseCount);

                categoryDtos.add(new FailureCategoryMetricDto(
                        cp.getFailureReasonCategory(),
                        caseCount,
                        estimated,
                        recovered,
                        recoveredCount,
                        rate
                ));

                totalCases += caseCount;
            }
        }

        List<FailurePriorityMetricDto> priorityDtos = new ArrayList<>();
        if (priorityProjections != null) {
            for (FailurePriorityProjection pp : priorityProjections) {
                long caseCount = pp.getCaseCount() != null ? pp.getCaseCount() : 0L;
                BigDecimal estimated = pp.getEstimatedRecoverableAmount() != null ? pp.getEstimatedRecoverableAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal recovered = pp.getRecoveredAmount() != null ? pp.getRecoveredAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                long recoveredCount = pp.getRecoveredCaseCount() != null ? pp.getRecoveredCaseCount() : 0L;
                BigDecimal rate = calculatePercentage(recoveredCount, caseCount);

                priorityDtos.add(new FailurePriorityMetricDto(
                        pp.getPriority(),
                        caseCount,
                        estimated,
                        recovered,
                        recoveredCount,
                        rate
                ));
            }
        }

        return new FailureAnalyticsResponseDto(
                dateRange.from(),
                dateRange.to(),
                totalCases,
                categoryDtos,
                priorityDtos
        );
    }

    @Transactional(readOnly = true)
    public ChannelAnalyticsResponseDto getChannelAnalytics(UUID merchantId, DateRange dateRange) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(dateRange, "DateRange cannot be null");

        List<ChannelPerformanceProjection> projections = recoveryAttemptRepository.getChannelPerformanceAnalytics(
                merchantId, dateRange.from(), dateRange.to());

        List<ChannelMetricDto> channelDtos = new ArrayList<>();
        long totalAttempts = 0L;

        if (projections != null) {
            for (ChannelPerformanceProjection cp : projections) {
                long attempts = cp.getTotalAttempts() != null ? cp.getTotalAttempts() : 0L;
                long successful = cp.getSuccessfulAttempts() != null ? cp.getSuccessfulAttempts() : 0L;
                long failed = cp.getFailedAttempts() != null ? cp.getFailedAttempts() : 0L;
                long sent = cp.getSentAttempts() != null ? cp.getSentAttempts() : 0L;
                long delivered = cp.getDeliveredAttempts() != null ? cp.getDeliveredAttempts() : 0L;
                long clicked = cp.getClickedAttempts() != null ? cp.getClickedAttempts() : 0L;
                BigDecimal recovered = cp.getRecoveredAmount() != null ? cp.getRecoveredAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal successRate = calculatePercentage(successful, attempts);

                channelDtos.add(new ChannelMetricDto(
                        cp.getChannel(),
                        attempts,
                        successful,
                        failed,
                        sent,
                        delivered,
                        clicked,
                        successRate,
                        recovered
                ));

                totalAttempts += attempts;
            }
        }

        return new ChannelAnalyticsResponseDto(
                dateRange.from(),
                dateRange.to(),
                totalAttempts,
                channelDtos
        );
    }

    @Transactional(readOnly = true)
    public AttemptAnalyticsResponseDto getAttemptAnalytics(UUID merchantId, DateRange dateRange) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(dateRange, "DateRange cannot be null");

        AttemptSummaryProjection summary = recoveryAttemptRepository.getAttemptSummaryAnalytics(
                merchantId, dateRange.from(), dateRange.to());

        long totalAttempts = summary != null && summary.getTotalAttempts() != null ? summary.getTotalAttempts() : 0L;
        long successfulAttempts = summary != null && summary.getSuccessfulAttempts() != null ? summary.getSuccessfulAttempts() : 0L;
        long failedAttempts = summary != null && summary.getFailedAttempts() != null ? summary.getFailedAttempts() : 0L;
        long scheduledAttempts = summary != null && summary.getScheduledAttempts() != null ? summary.getScheduledAttempts() : 0L;
        long inFlightAttempts = summary != null && summary.getInFlightAttempts() != null ? summary.getInFlightAttempts() : 0L;
        long sentAttempts = summary != null && summary.getSentAttempts() != null ? summary.getSentAttempts() : 0L;
        long deliveredAttempts = summary != null && summary.getDeliveredAttempts() != null ? summary.getDeliveredAttempts() : 0L;
        long clickedAttempts = summary != null && summary.getClickedAttempts() != null ? summary.getClickedAttempts() : 0L;
        long skippedAttempts = summary != null && summary.getSkippedAttempts() != null ? summary.getSkippedAttempts() : 0L;

        BigDecimal successRate = calculatePercentage(successfulAttempts, totalAttempts);

        Map<RecoveryAttemptStatus, Long> attemptsByStatus = new EnumMap<>(RecoveryAttemptStatus.class);
        for (RecoveryAttemptStatus status : RecoveryAttemptStatus.values()) {
            attemptsByStatus.put(status, 0L);
        }
        List<StatusCountProjection> statusCounts = recoveryAttemptRepository.countAttemptsByStatus(
                merchantId, dateRange.from(), dateRange.to());
        if (statusCounts != null) {
            for (StatusCountProjection sc : statusCounts) {
                if (sc.getStatus() != null && sc.getCount() != null) {
                    attemptsByStatus.put(sc.getStatus(), sc.getCount());
                }
            }
        }

        Map<RecoveryChannel, Long> attemptsByChannel = new EnumMap<>(RecoveryChannel.class);
        for (RecoveryChannel channel : RecoveryChannel.values()) {
            attemptsByChannel.put(channel, 0L);
        }
        List<ChannelCountProjection> channelCounts = recoveryAttemptRepository.countAttemptsByChannel(
                merchantId, dateRange.from(), dateRange.to());
        if (channelCounts != null) {
            for (ChannelCountProjection cc : channelCounts) {
                if (cc.getChannel() != null && cc.getCount() != null) {
                    attemptsByChannel.put(cc.getChannel(), cc.getCount());
                }
            }
        }

        long totalCasesInRange = recoveryCaseRepository.countByMerchantIdAndCreatedAtBetween(
                merchantId, dateRange.from(), dateRange.to());

        Double averageAttemptsPerRecoveryCase;
        if (totalCasesInRange > 0) {
            BigDecimal avg = BigDecimal.valueOf(totalAttempts)
                    .divide(BigDecimal.valueOf(totalCasesInRange), 2, RoundingMode.HALF_UP);
            averageAttemptsPerRecoveryCase = avg.doubleValue();
        } else {
            averageAttemptsPerRecoveryCase = 0.0;
        }

        return new AttemptAnalyticsResponseDto(
                dateRange.from(),
                dateRange.to(),
                totalAttempts,
                successfulAttempts,
                failedAttempts,
                scheduledAttempts,
                inFlightAttempts,
                sentAttempts,
                deliveredAttempts,
                clickedAttempts,
                skippedAttempts,
                successRate,
                averageAttemptsPerRecoveryCase,
                attemptsByStatus,
                attemptsByChannel
        );
    }

    private BigDecimal calculatePercentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private Double calculateAverageTimeToRecovery(UUID merchantId, DateRange dateRange) {
        List<Object[]> timestamps = recoveryCaseRepository.findRecoveredTimestamps(
                merchantId, dateRange.from(), dateRange.to());
        if (timestamps == null || timestamps.isEmpty()) {
            return null;
        }

        long totalSeconds = 0L;
        long count = 0L;

        for (Object[] pair : timestamps) {
            if (pair.length >= 2 && pair[0] instanceof Instant createdAt && pair[1] instanceof Instant recoveredAt) {
                long seconds = Duration.between(createdAt, recoveredAt).toSeconds();
                if (seconds >= 0) {
                    totalSeconds += seconds;
                    count++;
                }
            }
        }

        if (count == 0) {
            return null;
        }

        BigDecimal avg = BigDecimal.valueOf(totalSeconds)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return avg.doubleValue();
    }
}
