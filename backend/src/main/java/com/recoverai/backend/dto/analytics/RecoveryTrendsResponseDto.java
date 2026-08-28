package com.recoverai.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RecoveryTrendsResponseDto(
        Instant from,
        Instant to,
        long totalCases,
        BigDecimal totalAmountAtRisk,
        BigDecimal totalRecoveredAmount,
        BigDecimal overallRecoveryRate,
        List<DailyRecoveryTrendDto> trends
) {
}
