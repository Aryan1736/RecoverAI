package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoverySchedulerProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RecoverySchedulerWorkerTest {

    @Test
    @DisplayName("runSchedulerCycle should NOT invoke pollAndExecuteDueAttempts (worker consolidation)")
    void runSchedulerCycleShouldNotInvokeDirectExecution() {
        AtomicInteger pollCalls = new AtomicInteger(0);

        RecoverySchedulerProperties properties = new RecoverySchedulerProperties(true, 5000L, 50);
        RecoverySchedulerService testService = new RecoverySchedulerService(
                null, null, null, null, null, null, properties) {
            @Override
            public int pollAndExecuteDueAttempts() {
                return pollCalls.incrementAndGet();
            }
        };

        RecoverySchedulerWorker worker = new RecoverySchedulerWorker(testService, properties);
        worker.runSchedulerCycle();

        // Must be 0 because legacy direct execution polling is decommissioned
        assertThat(pollCalls.get()).isEqualTo(0);
        assertThat(worker.isDecommissioned()).isTrue();
    }

    @Test
    @DisplayName("runSchedulerCycle should handle calls safely without throwing exceptions")
    void shouldHandleCallsGracefullyWithoutExceptions() {
        RecoverySchedulerProperties properties = new RecoverySchedulerProperties(true, 5000L, 50);
        RecoverySchedulerService testService = new RecoverySchedulerService(
                null, null, null, null, null, null, properties);

        RecoverySchedulerWorker worker = new RecoverySchedulerWorker(testService, properties);
        assertDoesNotThrow(worker::runSchedulerCycle);
    }
}
