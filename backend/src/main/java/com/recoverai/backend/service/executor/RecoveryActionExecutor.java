package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryChannel;

public interface RecoveryActionExecutor {

    boolean supports(RecoveryChannel channel);

    ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase recoveryCase);
}
