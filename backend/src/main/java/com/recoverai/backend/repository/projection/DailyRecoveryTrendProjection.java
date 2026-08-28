package com.recoverai.backend.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRecoveryTrendProjection {
    LocalDate getDate();
    Long getRecoveryCasesCreated();
    BigDecimal getAmountAtRisk();
    BigDecimal getAmountRecovered();
    Long getRecoveredCaseCount();
}
