package com.recoverai.backend.dto.analytics;

import java.math.BigDecimal;

public record FailureCategoryMetricDto(
        String failureReasonCategory,
        long caseCount,
        BigDecimal estimatedRecoverableAmount,
        BigDecimal recoveredAmount,
        long recoveredCaseCount,
        BigDecimal recoveryRate
) {
}
