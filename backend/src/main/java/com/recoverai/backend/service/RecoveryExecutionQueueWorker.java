package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryQueueProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RecoveryExecutionQueueWorker {

    private static final Logger log = LoggerFactory.getLogger(RecoveryExecutionQueueWorker.class);

    private final RecoveryExecutionQueueService queueService;
    private final RecoveryQueueProperties properties;

    public RecoveryExecutionQueueWorker(RecoveryExecutionQueueService queueService,
                                        RecoveryQueueProperties properties) {
        this.queueService = queueService;
        this.properties = properties != null ? properties : new RecoveryQueueProperties();
    }

    @Scheduled(fixedDelayString = "${recoverai.recovery.queue.poll-interval-ms:3000}")
    public void runQueueCycle() {
        if (properties != null && !properties.isEnabled()) {
            log.trace("Recovery execution queue worker is disabled, skipping cycle.");
            return;
        }

        try {
            processDueQueueItems();
        } catch (Exception ex) {
            log.error("Unexpected error during recovery execution queue polling cycle: {}", ex.getMessage(), ex);
        }
    }

    public int processDueQueueItems() {
        // 1. Crash recovery: requeue stale/abandoned claims
        try {
            int requeued = queueService.requeueStaleClaims();
            if (requeued > 0) {
                log.info("Crash recovery cycle requeued {} stale claims back to READY", requeued);
            }
        } catch (Exception ex) {
            log.error("Error during stale claim recovery: {}", ex.getMessage(), ex);
        }

        // 2. Find due READY queue items
        int batchSize = properties != null ? Math.max(1, properties.getBatchSize()) : 25;
        List<UUID> dueItemIds = queueService.findDueReadyItemIds(batchSize);

        if (dueItemIds == null || dueItemIds.isEmpty()) {
            return 0;
        }

        log.debug("Found {} due queue item(s) eligible for claiming", dueItemIds.size());
        String workerId = properties != null ? properties.getWorkerId() : "default-worker";
        int processedCount = 0;

        for (UUID itemId : dueItemIds) {
            try {
                // 3. Atomically claim
                boolean claimed = queueService.claimItem(itemId, workerId);
                if (!claimed) {
                    log.debug("Queue item id={} was claimed by another worker or is no longer READY, skipping", itemId);
                    continue;
                }

                // 4. Process claimed item
                boolean processed = queueService.processQueueItem(itemId);
                if (processed) {
                    processedCount++;
                }
            } catch (Exception ex) {
                log.error("Unhandled error processing queue item id={}: {}", itemId, ex.getMessage(), ex);
            }
        }

        if (processedCount > 0) {
            log.info("Finished queue worker cycle: {} item(s) processed by worker={}", processedCount, workerId);
        }
        return processedCount;
    }
}
