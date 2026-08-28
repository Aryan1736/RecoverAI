package com.recoverai.backend.repository.projection;

import java.math.BigDecimal;

public interface FailureCategoryProjection {
    String getFailureReasonCategory();
    Long getCaseCount();
    BigDecimal getEstimatedRecoverableAmount();
    BigDecimal getRecoveredAmount();
    Long getRecoveredCaseCount();
}
