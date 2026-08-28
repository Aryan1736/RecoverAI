package com.recoverai.backend.repository.projection;

public interface AttemptSummaryProjection {
    Long getTotalAttempts();
    Long getSuccessfulAttempts();
    Long getFailedAttempts();
    Long getScheduledAttempts();
    Long getInFlightAttempts();
    Long getSentAttempts();
    Long getDeliveredAttempts();
    Long getClickedAttempts();
    Long getSkippedAttempts();
    Long getDistinctCasesWithAttempts();
}
