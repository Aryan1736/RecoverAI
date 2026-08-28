package com.recoverai.backend.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRecoveryTrendDto(
        LocalDate date,
        long recoveryCasesCreated,
        BigDecimal amountAtRisk,
        BigDecimal amountRecovered,
        long recoveredCaseCount,
        BigDecimal recoveryRate
) {
}
