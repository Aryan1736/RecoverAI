package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoverySchedulerProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RecoverySchedulerWorkerTest {

    @Test
    @DisplayName("runSchedulerCycle should invoke pollAndExecuteDueAttempts when enabled")
    void shouldInvokePollAndExecuteDueAttemptsWhenEnabled() {
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

        assertThat(pollCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("runSchedulerCycle should not invoke pollAndExecuteDueAttempts when disabled")
    void shouldNotInvokePollWhenDisabled() {
        AtomicInteger pollCalls = new AtomicInteger(0);

        RecoverySchedulerProperties properties = new RecoverySchedulerProperties(false, 5000L, 50);
        RecoverySchedulerService testService = new RecoverySchedulerService(
                null, null, null, null, null, null, properties) {
            @Override
            public int pollAndExecuteDueAttempts() {
                return pollCalls.incrementAndGet();
            }
        };

        RecoverySchedulerWorker worker = new RecoverySchedulerWorker(testService, properties);
        worker.runSchedulerCycle();

        assertThat(pollCalls.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("runSchedulerCycle should handle unexpected exception gracefully without rethrowing")
    void shouldHandleUnexpectedExceptionGracefully() {
        RecoverySchedulerProperties properties = new RecoverySchedulerProperties(true, 5000L, 50);
        RecoverySchedulerService failingService = new RecoverySchedulerService(
                null, null, null, null, null, null, properties) {
            @Override
            public int pollAndExecuteDueAttempts() {
                throw new RuntimeException("Simulated unexpected polling failure");
            }
        };

        RecoverySchedulerWorker worker = new RecoverySchedulerWorker(failingService, properties);

        assertDoesNotThrow(worker::runSchedulerCycle);
    }
}
