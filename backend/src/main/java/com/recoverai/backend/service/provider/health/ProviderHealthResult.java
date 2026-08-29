package com.recoverai.backend.service.provider.health;

import java.time.Instant;

public class ProviderHealthResult {

    private final String providerName;
    private final String category;
    private final ProviderHealthStatus status;
    private final String message;
    private final Instant timestamp;

    public ProviderHealthResult(String providerName, String category, ProviderHealthStatus status, String message) {
        this(providerName, category, status, message, Instant.now());
    }

    public ProviderHealthResult(String providerName, String category, ProviderHealthStatus status, String message, Instant timestamp) {
        this.providerName = providerName;
        this.category = category;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static ProviderHealthResult available(String providerName, String category, String message) {
        return new ProviderHealthResult(providerName, category, ProviderHealthStatus.AVAILABLE, message);
    }

    public static ProviderHealthResult disabled(String providerName, String category, String message) {
        return new ProviderHealthResult(providerName, category, ProviderHealthStatus.DISABLED, message);
    }

    public static ProviderHealthResult misconfigured(String providerName, String category, String message) {
        return new ProviderHealthResult(providerName, category, ProviderHealthStatus.MISCONFIGURED, message);
    }

    public static ProviderHealthResult degraded(String providerName, String category, String message) {
        return new ProviderHealthResult(providerName, category, ProviderHealthStatus.DEGRADED, message);
    }

    public String getProviderName() {
        return providerName;
    }

    public String getCategory() {
        return category;
    }

    public ProviderHealthStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
