package com.recoverai.backend.dto.analytics;

import com.recoverai.backend.entity.enums.RecoveryPriority;

import java.math.BigDecimal;

public record FailurePriorityMetricDto(
        RecoveryPriority priority,
        long caseCount,
        BigDecimal estimatedRecoverableAmount,
        BigDecimal recoveredAmount,
        long recoveredCaseCount,
        BigDecimal recoveryRate
) {
}
