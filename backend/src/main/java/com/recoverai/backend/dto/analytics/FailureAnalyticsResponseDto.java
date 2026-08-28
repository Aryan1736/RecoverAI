package com.recoverai.backend.dto.analytics;

import java.time.Instant;
import java.util.List;

public record FailureAnalyticsResponseDto(
        Instant from,
        Instant to,
        long totalCases,
        List<FailureCategoryMetricDto> categories,
        List<FailurePriorityMetricDto> priorities
) {
}
