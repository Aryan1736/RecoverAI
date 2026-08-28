package com.recoverai.backend.dto.analytics;

import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record AttemptAnalyticsResponseDto(
        Instant from,
        Instant to,
        long totalAttempts,
        long successfulAttempts,
        long failedAttempts,
        long scheduledAttempts,
        long inFlightAttempts,
        long sentAttempts,
        long deliveredAttempts,
        long clickedAttempts,
        long skippedAttempts,
        BigDecimal successRate,
        Double averageAttemptsPerRecoveryCase,
        Map<RecoveryAttemptStatus, Long> attemptsByStatus,
        Map<RecoveryChannel, Long> attemptsByChannel
) {
}
