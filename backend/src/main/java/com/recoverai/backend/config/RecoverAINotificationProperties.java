package com.recoverai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recoverai.notifications")
public class RecoverAINotificationProperties {

    private boolean enabled = true;
    private int webhookConnectTimeoutMs = 5000;
    private int webhookReadTimeoutMs = 10000;
    private int maxRetries = 3;
    private long retryDelaySeconds = 300L;
    private int providerHealthCooldownMinutes = 30;

    public RecoverAINotificationProperties() {
    }

    public RecoverAINotificationProperties(boolean enabled, int webhookConnectTimeoutMs,
                                           int webhookReadTimeoutMs, int maxRetries,
                                           long retryDelaySeconds, int providerHealthCooldownMinutes) {
        this.enabled = enabled;
        this.webhookConnectTimeoutMs = webhookConnectTimeoutMs;
        this.webhookReadTimeoutMs = webhookReadTimeoutMs;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
        this.providerHealthCooldownMinutes = providerHealthCooldownMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWebhookConnectTimeoutMs() {
        return webhookConnectTimeoutMs;
    }

    public void setWebhookConnectTimeoutMs(int webhookConnectTimeoutMs) {
        this.webhookConnectTimeoutMs = webhookConnectTimeoutMs;
    }

    public int getWebhookReadTimeoutMs() {
        return webhookReadTimeoutMs;
    }

    public void setWebhookReadTimeoutMs(int webhookReadTimeoutMs) {
        this.webhookReadTimeoutMs = webhookReadTimeoutMs;
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

    public int getProviderHealthCooldownMinutes() {
        return providerHealthCooldownMinutes;
    }

    public void setProviderHealthCooldownMinutes(int providerHealthCooldownMinutes) {
        this.providerHealthCooldownMinutes = providerHealthCooldownMinutes;
    }
}
