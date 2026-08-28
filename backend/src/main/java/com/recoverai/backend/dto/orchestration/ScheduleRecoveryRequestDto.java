package com.recoverai.backend.dto.orchestration;

import java.time.Instant;

public class ScheduleRecoveryRequestDto {

    private Instant scheduledAt;

    public ScheduleRecoveryRequestDto() {
    }

    public ScheduleRecoveryRequestDto(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
}
