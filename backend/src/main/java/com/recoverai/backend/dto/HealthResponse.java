package com.recoverai.backend.dto;

public record HealthResponse(
        String status,
        String service
) {
    public static HealthResponse up() {
        return new HealthResponse("UP", "recover-ai-backend");
    }
}
