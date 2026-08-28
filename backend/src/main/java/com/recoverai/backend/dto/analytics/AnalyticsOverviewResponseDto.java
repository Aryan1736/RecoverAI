package com.recoverai.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsOverviewResponseDto(
        long totalCases,
        long openCases,
        long inProgressCases,
        long recoveredCases,
        long failedCases,
        long expiredCases,
        long cancelledCases,
        long expiredOrCancelledCases,
        BigDecimal totalEstimatedRecoverableAmount,
        BigDecimal totalRecoveredAmount,
        BigDecimal recoveryRate,
        BigDecimal averageRecoveredAmount,
        Double averageTimeToRecoverySeconds,
        Instant from,
        Instant to
) {
}
