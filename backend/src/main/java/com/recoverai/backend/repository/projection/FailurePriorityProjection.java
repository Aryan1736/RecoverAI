package com.recoverai.backend.repository.projection;

import com.recoverai.backend.entity.enums.RecoveryPriority;

import java.math.BigDecimal;

public interface FailurePriorityProjection {
    RecoveryPriority getPriority();
    Long getCaseCount();
    BigDecimal getEstimatedRecoverableAmount();
    BigDecimal getRecoveredAmount();
    Long getRecoveredCaseCount();
}
