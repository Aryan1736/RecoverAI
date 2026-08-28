package com.recoverai.backend.repository.projection;

import java.math.BigDecimal;

public interface DashboardSummaryProjection {

    Long getTotalCases();

    Long getOpenCases();

    Long getInProgressCases();

    Long getRecoveredCases();

    Long getExpiredCases();

    Long getCancelledCases();

    Long getFailedCases();

    BigDecimal getTotalEstimatedRecoverableAmount();

    BigDecimal getTotalRecoveredAmount();
}
