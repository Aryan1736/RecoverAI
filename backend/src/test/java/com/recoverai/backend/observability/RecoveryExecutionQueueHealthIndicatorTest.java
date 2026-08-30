package com.recoverai.backend.observability;

import com.recoverai.backend.config.ObservabilityProperties;
import com.recoverai.backend.config.RecoveryQueueProperties;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoveryExecutionQueueHealthIndicatorTest {

    @Mock
    private RecoveryExecutionQueueRepository queueRepository;

    private ObservabilityProperties observabilityProperties;
    private RecoveryQueueProperties queueProperties;
    private RecoveryExecutionQueueHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        observabilityProperties = new ObservabilityProperties();
        observabilityProperties.getQueueHealth().setMaxReadyItems(100L);
        observabilityProperties.getQueueHealth().setMaxStaleClaims(5L);
        observabilityProperties.getQueueHealth().setMaxDeadLetterItems(10L);

        queueProperties = new RecoveryQueueProperties();
        queueProperties.setClaimTimeoutSeconds(300L);

        healthIndicator = new RecoveryExecutionQueueHealthIndicator(
                queueRepository, observabilityProperties, queueProperties);
    }

    @Test
    @DisplayName("Should report UP when all queue counts are below configured thresholds")
    void shouldReportUpWhenWithinThresholds() {
        when(queueRepository.countByStatus(RecoveryQueueStatus.READY)).thenReturn(20L);
        when(queueRepository.countByStatus(RecoveryQueueStatus.DEAD_LETTER)).thenReturn(2L);
        when(queueRepository.countStaleClaims(any(Instant.class))).thenReturn(1L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("readyItems", 20L);
        assertThat(health.getDetails()).containsEntry("staleClaims", 1L);
        assertThat(health.getDetails()).containsEntry("deadLetterItems", 2L);
        assertThat(health.getDetails()).doesNotContainKey("issues");
    }

    @Test
    @DisplayName("Should report DEGRADED when ready backlog exceeds threshold")
    void shouldReportDegradedWhenReadyBacklogExcessive() {
        when(queueRepository.countByStatus(RecoveryQueueStatus.READY)).thenReturn(150L);
        when(queueRepository.countByStatus(RecoveryQueueStatus.DEAD_LETTER)).thenReturn(0L);
        when(queueRepository.countStaleClaims(any(Instant.class))).thenReturn(0L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails()).containsKey("issues");
        List<?> issues = (List<?>) health.getDetails().get("issues");
        assertThat(issues).anyMatch(i -> i.toString().contains("Excessive ready items"));
    }

    @Test
    @DisplayName("Should report DEGRADED when stale claims exceed threshold")
    void shouldReportDegradedWhenStaleClaimsExcessive() {
        when(queueRepository.countByStatus(RecoveryQueueStatus.READY)).thenReturn(5L);
        when(queueRepository.countByStatus(RecoveryQueueStatus.DEAD_LETTER)).thenReturn(1L);
        when(queueRepository.countStaleClaims(any(Instant.class))).thenReturn(12L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        List<?> issues = (List<?>) health.getDetails().get("issues");
        assertThat(issues).anyMatch(i -> i.toString().contains("Excessive stale claims"));
    }

    @Test
    @DisplayName("Should report DEGRADED when dead letter items exceed threshold")
    void shouldReportDegradedWhenDeadLettersExcessive() {
        when(queueRepository.countByStatus(RecoveryQueueStatus.READY)).thenReturn(5L);
        when(queueRepository.countByStatus(RecoveryQueueStatus.DEAD_LETTER)).thenReturn(25L);
        when(queueRepository.countStaleClaims(any(Instant.class))).thenReturn(0L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        List<?> issues = (List<?>) health.getDetails().get("issues");
        assertThat(issues).anyMatch(i -> i.toString().contains("Excessive dead letter items"));
    }

    @Test
    @DisplayName("Should report DOWN safely when database repository throws exception")
    void shouldReportDownWhenDatabaseAccessFails() {
        when(queueRepository.countByStatus(RecoveryQueueStatus.READY))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Postgres connection timeout"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", "Queue database access failed");
        // Ensure sensitive database connection strings or stack traces are not leaked
        assertThat(health.getDetails().get("error").toString()).doesNotContain("password", "timeout");
    }

    @Test
    @DisplayName("Should return UNKNOWN when queue health monitoring is disabled")
    void shouldReturnUnknownWhenDisabled() {
        observabilityProperties.getQueueHealth().setEnabled(false);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("status", "DISABLED");
        verifyNoInteractions(queueRepository);
    }
}
