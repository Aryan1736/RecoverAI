package com.recoverai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recoverai.recovery.scheduler")
public class RecoverySchedulerProperties {

    private boolean enabled = true;
    private long pollingIntervalMs = 5000L;
    private int batchSize = 50;

    public RecoverySchedulerProperties() {
    }

    public RecoverySchedulerProperties(boolean enabled, long pollingIntervalMs, int batchSize) {
        this.enabled = enabled;
        this.pollingIntervalMs = pollingIntervalMs;
        this.batchSize = batchSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollingIntervalMs() {
        return pollingIntervalMs;
    }

    public void setPollingIntervalMs(long pollingIntervalMs) {
        this.pollingIntervalMs = pollingIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
