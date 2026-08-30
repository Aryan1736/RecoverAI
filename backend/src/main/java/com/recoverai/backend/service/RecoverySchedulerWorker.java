package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoverySchedulerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Legacy Recovery Scheduler Background Worker.
 *
 * <p>DECOMMISSIONED in PR #17 (Worker Consolidation):
 * The duplicate direct-execution polling mechanism has been decommissioned so that there is
 * only ONE authoritative background execution mechanism:
 * {@link RecoverySchedulerService} -&gt; {@link RecoveryExecutionQueueService} -&gt;
 * {@link RecoveryExecutionQueueWorker} -&gt; Action Executors.
 *
 * <p>Retained for backward compatibility without active {@code @Scheduled} background execution.
 */
@Deprecated(since = "PR-17", forRemoval = false)
@Component
public class RecoverySchedulerWorker {

    private static final Logger log = LoggerFactory.getLogger(RecoverySchedulerWorker.class);

    private final RecoverySchedulerService recoverySchedulerService;
    private final RecoverySchedulerProperties properties;

    public RecoverySchedulerWorker(RecoverySchedulerService recoverySchedulerService,
                                   RecoverySchedulerProperties properties) {
        this.recoverySchedulerService = recoverySchedulerService;
        this.properties = properties;
    }

    /**
     * Legacy cycle invocation.
     * Decommissioned to prevent duplicate execution of recovery attempts.
     * Background execution is authoritatively handled by {@link RecoveryExecutionQueueWorker}.
     */
    public void runSchedulerCycle() {
        log.debug("Legacy RecoverySchedulerWorker cycle called, but direct execution is decommissioned in favor of RecoveryExecutionQueueWorker.");
        // Decommissioned: no duplicate execution against recovery_attempts directly.
    }

    public boolean isDecommissioned() {
        return true;
    }

    public RecoverySchedulerService getRecoverySchedulerService() {
        return recoverySchedulerService;
    }

    public RecoverySchedulerProperties getProperties() {
        return properties;
    }
}
