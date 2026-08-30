package com.recoverai.backend.observability;

import com.recoverai.backend.config.ObservabilityProperties;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Production Micrometer metrics service for RecoverAI.
 * <p>
 * Implements strict low-cardinality protection: ONLY bounded operational dimensions
 * (channel, provider, failureType, status, operation) are permitted as metric tags.
 * High-cardinality values (merchantId, customerId, recoveryAttemptId, correlationId, etc.)
 * are STRICTLY FORBIDDEN to prevent metric registry memory leaks and TSDB degradation.
 */
@Component
public class RecoveryMetrics {

    private static final Logger log = LoggerFactory.getLogger(RecoveryMetrics.class);

    // Metric names
    public static final String METRIC_ATTEMPTS_STARTED = "recoverai.recovery.attempts.started";
    public static final String METRIC_ATTEMPTS_SUCCEEDED = "recoverai.recovery.attempts.succeeded";
    public static final String METRIC_ATTEMPTS_FAILED = "recoverai.recovery.attempts.failed";
    public static final String METRIC_ATTEMPTS_SKIPPED = "recoverai.recovery.attempts.skipped";
    public static final String METRIC_CASES_RECOVERED = "recoverai.recovery.cases.recovered";

    public static final String METRIC_QUEUE_CLAIMS = "recoverai.recovery.queue.claims";
    public static final String METRIC_QUEUE_RETRIES = "recoverai.recovery.queue.retries";
    public static final String METRIC_QUEUE_DEAD_LETTERS = "recoverai.recovery.queue.dead_letters";
    public static final String METRIC_QUEUE_PROCESSING_FAILURES = "recoverai.recovery.queue.processing_failures";
    public static final String METRIC_QUEUE_DEPTH = "recoverai.recovery.queue.depth";

    public static final String METRIC_PROVIDER_DISPATCH_SUCCESS = "recoverai.provider.dispatch.success";
    public static final String METRIC_PROVIDER_DISPATCH_FAILURE = "recoverai.provider.dispatch.failure";
    public static final String METRIC_PROVIDER_DISPATCH_RETRYABLE = "recoverai.provider.dispatch.retryable_failure";
    public static final String METRIC_PROVIDER_DISPATCH_PERMANENT = "recoverai.provider.dispatch.permanent_failure";
    public static final String METRIC_PROVIDER_DISPATCH_DURATION = "recoverai.provider.dispatch.duration";

    // Tag keys
    public static final String TAG_CHANNEL = "channel";
    public static final String TAG_PROVIDER = "provider";
    public static final String TAG_FAILURE_TYPE = "failureType";
    public static final String TAG_STATUS = "status";
    public static final String TAG_OPERATION = "operation";

    private final MeterRegistry registry;
    private final ObservabilityProperties properties;
    private final RecoveryExecutionQueueRepository queueRepository;

    // Bounded cached supplier for queue depth to protect DB from scrape storms (2-second cache)
    private final CachedQueueDepthSupplier cachedQueueDepthSupplier;

    public RecoveryMetrics(MeterRegistry registry,
                           ObservabilityProperties properties,
                           RecoveryExecutionQueueRepository queueRepository) {
        this.registry = registry;
        this.properties = properties != null ? properties : new ObservabilityProperties();
        this.queueRepository = queueRepository;
        this.cachedQueueDepthSupplier = new CachedQueueDepthSupplier(queueRepository, Duration.ofSeconds(2));

        registerQueueDepthGauge();
    }

    private void registerQueueDepthGauge() {
        if (registry != null && isMetricsEnabled()) {
            Gauge.builder(METRIC_QUEUE_DEPTH, cachedQueueDepthSupplier, CachedQueueDepthSupplier::getDepth)
                    .description("Current number of executable queue items in READY status")
                    .tag(TAG_STATUS, "READY")
                    .register(registry);
        }
    }

    private boolean isMetricsEnabled() {
        return properties.isEnabled() && properties.getMetrics().isEnabled();
    }

    // ==========================================
    // RECOVERY LIFECYCLE METRICS
    // ==========================================

    public void recordAttemptStarted(RecoveryChannel channel) {
        if (!isMetricsEnabled()) return;
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        registry.counter(METRIC_ATTEMPTS_STARTED, TAG_CHANNEL, safeChannel).increment();
    }

    public void recordAttemptSucceeded(RecoveryChannel channel) {
        if (!isMetricsEnabled()) return;
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        registry.counter(METRIC_ATTEMPTS_SUCCEEDED, TAG_CHANNEL, safeChannel).increment();
    }

    public void recordAttemptFailed(RecoveryChannel channel, ProviderFailureType failureType) {
        if (!isMetricsEnabled()) return;
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        String safeFailure = sanitizeTag(failureType != null ? failureType.name() : null);
        registry.counter(METRIC_ATTEMPTS_FAILED,
                TAG_CHANNEL, safeChannel,
                TAG_FAILURE_TYPE, safeFailure).increment();
    }

    public void recordAttemptSkipped(RecoveryChannel channel) {
        if (!isMetricsEnabled()) return;
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        registry.counter(METRIC_ATTEMPTS_SKIPPED, TAG_CHANNEL, safeChannel).increment();
    }

    public void recordCaseRecovered(RecoveryChannel channel) {
        if (!isMetricsEnabled()) return;
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        registry.counter(METRIC_CASES_RECOVERED, TAG_CHANNEL, safeChannel).increment();
    }

    // ==========================================
    // QUEUE METRICS
    // ==========================================

    public void recordQueueClaim() {
        if (!isMetricsEnabled()) return;
        registry.counter(METRIC_QUEUE_CLAIMS).increment();
    }

    public void recordQueueRetry() {
        if (!isMetricsEnabled()) return;
        registry.counter(METRIC_QUEUE_RETRIES).increment();
    }

    public void recordQueueDeadLetter(ProviderFailureType failureType) {
        if (!isMetricsEnabled()) return;
        String safeFailure = sanitizeTag(failureType != null ? failureType.name() : null);
        registry.counter(METRIC_QUEUE_DEAD_LETTERS, TAG_FAILURE_TYPE, safeFailure).increment();
    }

    public void recordQueueProcessingFailure() {
        if (!isMetricsEnabled()) return;
        registry.counter(METRIC_QUEUE_PROCESSING_FAILURES).increment();
    }

    // ==========================================
    // PROVIDER METRICS & TIMERS
    // ==========================================

    public void recordProviderDispatchSuccess(String provider, RecoveryChannel channel) {
        if (!isMetricsEnabled()) return;
        String safeProvider = sanitizeTag(provider);
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        registry.counter(METRIC_PROVIDER_DISPATCH_SUCCESS,
                TAG_PROVIDER, safeProvider,
                TAG_CHANNEL, safeChannel).increment();
    }

    public void recordProviderDispatchFailure(String provider, RecoveryChannel channel, ProviderFailureType failureType) {
        if (!isMetricsEnabled()) return;
        String safeProvider = sanitizeTag(provider);
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        String safeFailure = sanitizeTag(failureType != null ? failureType.name() : null);
        registry.counter(METRIC_PROVIDER_DISPATCH_FAILURE,
                TAG_PROVIDER, safeProvider,
                TAG_CHANNEL, safeChannel,
                TAG_FAILURE_TYPE, safeFailure).increment();
    }

    public void recordProviderDispatchRetryableFailure(String provider, RecoveryChannel channel, ProviderFailureType failureType) {
        if (!isMetricsEnabled()) return;
        String safeProvider = sanitizeTag(provider);
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        String safeFailure = sanitizeTag(failureType != null ? failureType.name() : null);
        registry.counter(METRIC_PROVIDER_DISPATCH_RETRYABLE,
                TAG_PROVIDER, safeProvider,
                TAG_CHANNEL, safeChannel,
                TAG_FAILURE_TYPE, safeFailure).increment();
    }

    public void recordProviderDispatchPermanentFailure(String provider, RecoveryChannel channel, ProviderFailureType failureType) {
        if (!isMetricsEnabled()) return;
        String safeProvider = sanitizeTag(provider);
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        String safeFailure = sanitizeTag(failureType != null ? failureType.name() : null);
        registry.counter(METRIC_PROVIDER_DISPATCH_PERMANENT,
                TAG_PROVIDER, safeProvider,
                TAG_CHANNEL, safeChannel,
                TAG_FAILURE_TYPE, safeFailure).increment();
    }

    public void recordProviderDispatchDuration(String provider, RecoveryChannel channel, String status, Duration duration) {
        if (!isMetricsEnabled() || duration == null) return;
        String safeProvider = sanitizeTag(provider);
        String safeChannel = sanitizeTag(channel != null ? channel.name() : null);
        String safeStatus = sanitizeTag(status);
        Timer timer = registry.timer(METRIC_PROVIDER_DISPATCH_DURATION,
                TAG_PROVIDER, safeProvider,
                TAG_CHANNEL, safeChannel,
                TAG_STATUS, safeStatus);
        timer.record(duration);
    }

    public double getQueueDepth() {
        return cachedQueueDepthSupplier.getDepth();
    }

    /**
     * Sanitizes metric tag values to ensure bounded low-cardinality.
     * Prevents nulls, trims whitespace, and limits length.
     */
    public static String sanitizeTag(String val) {
        if (val == null || val.isBlank()) {
            return "UNKNOWN";
        }
        String clean = val.trim().toUpperCase();
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    /**
     * Bounded time-based cached supplier to compute queue depth
     * without thrashing the database on high-frequency Prometheus/Actuator scrapes.
     */
    static class CachedQueueDepthSupplier {
        private final RecoveryExecutionQueueRepository repo;
        private final Duration ttl;
        private final AtomicReference<CachedValue> cache = new AtomicReference<>();

        CachedQueueDepthSupplier(RecoveryExecutionQueueRepository repo, Duration ttl) {
            this.repo = repo;
            this.ttl = ttl != null ? ttl : Duration.ofSeconds(2);
        }

        double getDepth() {
            if (repo == null) {
                return 0.0;
            }
            Instant now = Instant.now();
            CachedValue current = cache.get();
            if (current != null && current.expiresAt.isAfter(now)) {
                return current.value;
            }
            try {
                long count = repo.countByStatus(RecoveryQueueStatus.READY);
                cache.set(new CachedValue((double) count, now.plus(ttl)));
                return (double) count;
            } catch (Exception ex) {
                log.warn("Failed to fetch queue depth for gauge metric: {}", ex.getMessage());
                return current != null ? current.value : 0.0;
            }
        }

        private static class CachedValue {
            final double value;
            final Instant expiresAt;

            CachedValue(double value, Instant expiresAt) {
                this.value = value;
                this.expiresAt = expiresAt;
            }
        }
    }
}
