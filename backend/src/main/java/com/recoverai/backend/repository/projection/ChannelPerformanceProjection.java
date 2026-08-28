package com.recoverai.backend.repository.projection;

import com.recoverai.backend.entity.enums.RecoveryChannel;

import java.math.BigDecimal;

public interface ChannelPerformanceProjection {
    RecoveryChannel getChannel();
    Long getTotalAttempts();
    Long getSuccessfulAttempts();
    Long getFailedAttempts();
    Long getSentAttempts();
    Long getDeliveredAttempts();
    Long getClickedAttempts();
    BigDecimal getRecoveredAmount();
}
