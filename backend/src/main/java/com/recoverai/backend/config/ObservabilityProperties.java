package com.recoverai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recoverai.observability")
public class ObservabilityProperties {

    private boolean enabled = true;
    private CorrelationIdProperties correlationId = new CorrelationIdProperties();
    private MetricsProperties metrics = new MetricsProperties();
    private QueueHealthProperties queueHealth = new QueueHealthProperties();
    private ProviderHealthProperties providerHealth = new ProviderHealthProperties();

    public ObservabilityProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CorrelationIdProperties getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(CorrelationIdProperties correlationId) {
        this.correlationId = correlationId;
    }

    public MetricsProperties getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    public QueueHealthProperties getQueueHealth() {
        return queueHealth;
    }

    public void setQueueHealth(QueueHealthProperties queueHealth) {
        this.queueHealth = queueHealth;
    }

    public ProviderHealthProperties getProviderHealth() {
        return providerHealth;
    }

    public void setProviderHealth(ProviderHealthProperties providerHealth) {
        this.providerHealth = providerHealth;
    }

    public static class CorrelationIdProperties {
        private boolean enabled = true;
        private String headerName = "X-Correlation-ID";
        private int maxLength = 64;

        public CorrelationIdProperties() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }
    }

    public static class MetricsProperties {
        private boolean enabled = true;

        public MetricsProperties() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class QueueHealthProperties {
        private boolean enabled = true;
        private long maxDeadLetterItems = 50L;
        private long maxStaleClaims = 10L;
        private long maxReadyItems = 1000L;

        public QueueHealthProperties() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getMaxDeadLetterItems() {
            return maxDeadLetterItems;
        }

        public void setMaxDeadLetterItems(long maxDeadLetterItems) {
            this.maxDeadLetterItems = maxDeadLetterItems;
        }

        public long getMaxStaleClaims() {
            return maxStaleClaims;
        }

        public void setMaxStaleClaims(long maxStaleClaims) {
            this.maxStaleClaims = maxStaleClaims;
        }

        public long getMaxReadyItems() {
            return maxReadyItems;
        }

        public void setMaxReadyItems(long maxReadyItems) {
            this.maxReadyItems = maxReadyItems;
        }
    }

    public static class ProviderHealthProperties {
        private boolean enabled = true;
        private long cacheTtlSeconds = 10L;

        public ProviderHealthProperties() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }
}
