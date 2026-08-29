package com.recoverai.backend.service.provider.classification;

public enum ProviderFailureType {
    TRANSIENT,
    RATE_LIMITED,
    AUTHENTICATION,
    VALIDATION,
    NOT_FOUND,
    PERMANENT,
    TIMEOUT,
    UNKNOWN;

    public boolean isRetryable() {
        return this == TRANSIENT || this == RATE_LIMITED || this == TIMEOUT;
    }
}
