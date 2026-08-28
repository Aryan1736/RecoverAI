package com.recoverai.backend.dto.analytics;

import com.recoverai.backend.entity.enums.RecoveryChannel;

import java.math.BigDecimal;

public record ChannelMetricDto(
        RecoveryChannel channel,
        long totalAttempts,
        long successfulAttempts,
        long failedAttempts,
        long sentAttempts,
        long deliveredAttempts,
        long clickedAttempts,
        BigDecimal successRate,
        BigDecimal recoveredAmount
) {
}
