package com.recoverai.backend.dto.analytics;

import java.time.Instant;
import java.util.List;

public record ChannelAnalyticsResponseDto(
        Instant from,
        Instant to,
        long totalAttempts,
        List<ChannelMetricDto> channels
) {
}
