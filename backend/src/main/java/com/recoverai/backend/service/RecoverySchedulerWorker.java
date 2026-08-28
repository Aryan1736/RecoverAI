package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoverySchedulerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Scheduled(fixedDelayString = "${recoverai.recovery.scheduler.polling-interval-ms:5000}")
    public void runSchedulerCycle() {
        if (properties != null && !properties.isEnabled()) {
            log.trace("Recovery scheduler background worker is disabled, skipping cycle.");
            return;
        }

        try {
            recoverySchedulerService.pollAndExecuteDueAttempts();
        } catch (Exception ex) {
            log.error("Unexpected error in recovery scheduler polling cycle: {}", ex.getMessage(), ex);
        }
    }
}
