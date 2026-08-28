package com.recoverai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "recoverai.recovery.strategy")
public class RecoveryStrategyProperties {

    private boolean enabled = true;
    private BigDecimal minAiConfidence = new BigDecimal("0.70");
    private int maxAttempts = 3;
    private boolean retryChargeEnabled = true;
    private boolean fallbackEnabled = true;
    private int maxChannelFailures = 1;
    private int defaultDelaySeconds = 0;
    private int retryDelaySeconds = 300;

    public RecoveryStrategyProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getMinAiConfidence() {
        return minAiConfidence;
    }

    public void setMinAiConfidence(BigDecimal minAiConfidence) {
        this.minAiConfidence = minAiConfidence;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isRetryChargeEnabled() {
        return retryChargeEnabled;
    }

    public void setRetryChargeEnabled(boolean retryChargeEnabled) {
        this.retryChargeEnabled = retryChargeEnabled;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public int getMaxChannelFailures() {
        return maxChannelFailures;
    }

    public void setMaxChannelFailures(int maxChannelFailures) {
        this.maxChannelFailures = maxChannelFailures;
    }

    public int getDefaultDelaySeconds() {
        return defaultDelaySeconds;
    }

    public void setDefaultDelaySeconds(int defaultDelaySeconds) {
        this.defaultDelaySeconds = defaultDelaySeconds;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }
}
