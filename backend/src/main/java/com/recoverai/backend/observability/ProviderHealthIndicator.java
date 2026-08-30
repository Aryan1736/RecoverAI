package com.recoverai.backend.observability;

import com.recoverai.backend.config.ObservabilityProperties;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthService;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Spring Boot Actuator HealthIndicator for external communication and payment providers.
 * <p>
 * Reuses the existing {@link ProviderHealthService}.
 * Strictly read-only: does NOT send notifications, trigger alerts, or invoke recovery actions.
 * Employs a bounded TTL cache to prevent provider rate limiting on repeated Actuator health polls.
 * Sanitizes all output to ensure no secrets, tokens, or credentials can ever be exposed.
 */
@Component("providerHealthIndicator")
public class ProviderHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ProviderHealthIndicator.class);

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(key|secret|token|password|auth|bearer|credential)[=:\\s]+[^\\s,;]+");

    private final ProviderHealthService providerHealthService;
    private final ObservabilityProperties observabilityProperties;

    // Cache wrapper to prevent hammering third-party provider APIs on rapid scrapes
    private final AtomicReference<CachedHealthCheck> cachedCheck = new AtomicReference<>();

    public ProviderHealthIndicator(ProviderHealthService providerHealthService,
                                   ObservabilityProperties observabilityProperties) {
        this.providerHealthService = providerHealthService;
        this.observabilityProperties = observabilityProperties != null ? observabilityProperties : new ObservabilityProperties();
    }

    @Override
    public Health health() {
        ObservabilityProperties.ProviderHealthProperties config = observabilityProperties.getProviderHealth();
        if (!observabilityProperties.isEnabled() || (config != null && !config.isEnabled())) {
            return Health.unknown().withDetail("status", "DISABLED").build();
        }

        long ttlSeconds = config != null ? config.getCacheTtlSeconds() : 10L;
        List<ProviderHealthResult> results = getCachedOrFreshResults(Duration.ofSeconds(Math.max(1, ttlSeconds)));

        if (results == null || results.isEmpty()) {
            return Health.up().withDetail("providers", Collections.emptyMap()).build();
        }

        Map<String, String> componentStatuses = new LinkedHashMap<>();
        Map<String, String> componentDetails = new LinkedHashMap<>();

        boolean hasDegraded = false;
        boolean hasDown = false;

        for (ProviderHealthResult result : results) {
            String key = (result.getCategory() != null ? result.getCategory().toUpperCase() : "UNKNOWN")
                    + "_" + (result.getProviderName() != null ? result.getProviderName().toUpperCase() : "PROVIDER");

            ProviderHealthStatus status = result.getStatus();
            String mappedStatus;

            if (status == ProviderHealthStatus.AVAILABLE) {
                mappedStatus = "UP";
            } else if (status == ProviderHealthStatus.DEGRADED) {
                mappedStatus = "DEGRADED";
                hasDegraded = true;
            } else if (status == ProviderHealthStatus.MISCONFIGURED) {
                mappedStatus = "DOWN";
                hasDown = true;
            } else if (status == ProviderHealthStatus.DISABLED) {
                mappedStatus = "DISABLED";
            } else {
                mappedStatus = "UNKNOWN";
            }

            componentStatuses.put(key, mappedStatus);

            if (result.getMessage() != null && !result.getMessage().isBlank()) {
                componentDetails.put(key, sanitizeMessage(result.getMessage()));
            }
        }

        Health.Builder builder;
        if (hasDown || hasDegraded) {
            builder = Health.status("DEGRADED");
        } else {
            builder = Health.up();
        }

        builder.withDetail("components", componentStatuses);
        if (!componentDetails.isEmpty()) {
            builder.withDetail("messages", componentDetails);
        }

        return builder.build();
    }

    private List<ProviderHealthResult> getCachedOrFreshResults(Duration ttl) {
        Instant now = Instant.now();
        CachedHealthCheck current = cachedCheck.get();
        if (current != null && current.expiresAt.isAfter(now)) {
            return current.results;
        }

        try {
            List<ProviderHealthResult> fresh = providerHealthService.checkAll();
            List<ProviderHealthResult> safeList = fresh != null ? fresh : Collections.emptyList();
            cachedCheck.set(new CachedHealthCheck(safeList, now.plus(ttl)));
            return safeList;
        } catch (Exception ex) {
            log.error("Failed to run provider health checks: {}", ex.getMessage());
            return current != null ? current.results : Collections.emptyList();
        }
    }

    /**
     * Strips credentials, tokens, or URL query parameters from health messages.
     */
    public static String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        String sanitized = SENSITIVE_PATTERN.matcher(message).replaceAll("$1=[REDACTED]");
        // Strip URLs with potential query params
        sanitized = sanitized.replaceAll("https?://[^\\s]+[?][^\\s]+", "[REDACTED_URL]");
        return sanitized.length() > 256 ? sanitized.substring(0, 256) : sanitized;
    }

    private static class CachedHealthCheck {
        final List<ProviderHealthResult> results;
        final Instant expiresAt;

        CachedHealthCheck(List<ProviderHealthResult> results, Instant expiresAt) {
            this.results = results;
            this.expiresAt = expiresAt;
        }
    }
}
