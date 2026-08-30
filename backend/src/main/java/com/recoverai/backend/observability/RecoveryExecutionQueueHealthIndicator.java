package com.recoverai.backend.observability;

import com.recoverai.backend.config.ObservabilityProperties;
import com.recoverai.backend.config.RecoveryQueueProperties;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot Actuator HealthIndicator for the Recovery Execution Queue.
 * <p>
 * Evaluates queue health based on operational thresholds:
 * - Ready backlog depth
 * - Stale/abandoned claims
 * - Dead-letter queue (DLQ) accumulation
 * - Database connectivity and query responsiveness
 * <p>
 * Emits strictly safe, non-sensitive metrics only.
 */
@Component("recoveryExecutionQueueHealthIndicator")
public class RecoveryExecutionQueueHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RecoveryExecutionQueueHealthIndicator.class);

    private final RecoveryExecutionQueueRepository queueRepository;
    private final ObservabilityProperties observabilityProperties;
    private final RecoveryQueueProperties queueProperties;

    public RecoveryExecutionQueueHealthIndicator(RecoveryExecutionQueueRepository queueRepository,
                                                ObservabilityProperties observabilityProperties,
                                                RecoveryQueueProperties queueProperties) {
        this.queueRepository = queueRepository;
        this.observabilityProperties = observabilityProperties != null ? observabilityProperties : new ObservabilityProperties();
        this.queueProperties = queueProperties != null ? queueProperties : new RecoveryQueueProperties();
    }

    @Override
    public Health health() {
        ObservabilityProperties.QueueHealthProperties config = observabilityProperties.getQueueHealth();
        if (!observabilityProperties.isEnabled() || (config != null && !config.isEnabled())) {
            return Health.unknown().withDetail("status", "DISABLED").build();
        }

        try {
            long readyCount = queueRepository.countByStatus(RecoveryQueueStatus.READY);
            long deadLetterCount = queueRepository.countByStatus(RecoveryQueueStatus.DEAD_LETTER);

            long claimTimeoutSeconds = queueProperties != null ? queueProperties.getClaimTimeoutSeconds() : 300L;
            Instant staleThreshold = Instant.now().minusSeconds(claimTimeoutSeconds);
            long staleClaimCount = queueRepository.countStaleClaims(staleThreshold);

            long maxReady = config != null ? config.getMaxReadyItems() : 1000L;
            long maxStale = config != null ? config.getMaxStaleClaims() : 10L;
            long maxDeadLetter = config != null ? config.getMaxDeadLetterItems() : 50L;

            List<String> degradationReasons = new ArrayList<>();

            if (readyCount > maxReady) {
                degradationReasons.add(String.format("Excessive ready items: %d exceeds threshold %d", readyCount, maxReady));
            }
            if (staleClaimCount > maxStale) {
                degradationReasons.add(String.format("Excessive stale claims: %d exceeds threshold %d", staleClaimCount, maxStale));
            }
            if (deadLetterCount > maxDeadLetter) {
                degradationReasons.add(String.format("Excessive dead letter items: %d exceeds threshold %d", deadLetterCount, maxDeadLetter));
            }

            Health.Builder builder;
            if (degradationReasons.isEmpty()) {
                builder = Health.up();
            } else {
                builder = Health.status("DEGRADED")
                        .withDetail("issues", degradationReasons);
            }

            return builder
                    .withDetail("readyItems", readyCount)
                    .withDetail("staleClaims", staleClaimCount)
                    .withDetail("deadLetterItems", deadLetterCount)
                    .withDetail("maxReadyThreshold", maxReady)
                    .withDetail("maxStaleThreshold", maxStale)
                    .withDetail("maxDeadLetterThreshold", maxDeadLetter)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to evaluate recovery execution queue health: {}", ex.getMessage());
            return Health.down()
                    .withDetail("error", "Queue database access failed")
                    .build();
        }
    }
}
