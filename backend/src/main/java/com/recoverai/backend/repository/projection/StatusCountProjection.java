package com.recoverai.backend.repository.projection;

import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;

public interface StatusCountProjection {
    RecoveryAttemptStatus getStatus();
    Long getCount();
}
