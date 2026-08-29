package com.recoverai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConfigurationProperties(prefix = "recoverai.recovery.queue")
public class RecoveryQueueProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 3000L;
    private int batchSize = 25;
    private int maxRetries = 3;
    private long retryDelaySeconds = 300L;
    private String workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private long claimTimeoutSeconds = 300L;

    public RecoveryQueueProperties() {
    }

    public RecoveryQueueProperties(boolean enabled, long pollIntervalMs, int batchSize,
                                   int maxRetries, long retryDelaySeconds, String workerId,
                                   long claimTimeoutSeconds) {
        this.enabled = enabled;
        this.pollIntervalMs = pollIntervalMs;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
        this.workerId = workerId;
        this.claimTimeoutSeconds = claimTimeoutSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(long retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public long getClaimTimeoutSeconds() {
        return claimTimeoutSeconds;
    }

    public void setClaimTimeoutSeconds(long claimTimeoutSeconds) {
        this.claimTimeoutSeconds = claimTimeoutSeconds;
    }
}
