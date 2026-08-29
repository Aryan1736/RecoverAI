package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryQueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryExecutionQueueWorkerTest {

    @Mock
    private RecoveryExecutionQueueService queueService;

    private RecoveryQueueProperties properties;
    private RecoveryExecutionQueueWorker worker;

    @BeforeEach
    void setUp() {
        properties = new RecoveryQueueProperties(true, 3000L, 25, 3, 300L, "worker-test-1", 300L);
        worker = new RecoveryExecutionQueueWorker(queueService, properties);
    }

    @Test
    @DisplayName("Worker cycle should do nothing when disabled via configuration")
    void shouldDoNothingWhenDisabled() {
        properties.setEnabled(false);

        worker.runQueueCycle();

        verify(queueService, never()).findDueReadyItemIds(anyInt());
        verify(queueService, never()).requeueStaleClaims();
        verify(queueService, never()).claimItem(any(), any());
    }

    @Test
    @DisplayName("Worker cycle should recover stale claims and process due READY items")
    void shouldClaimAndProcessDueItems() {
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();

        when(queueService.findDueReadyItemIds(25)).thenReturn(List.of(item1, item2));
        when(queueService.claimItem(eq(item1), eq("worker-test-1"))).thenReturn(true);
        when(queueService.claimItem(eq(item2), eq("worker-test-1"))).thenReturn(false); // already claimed by another worker

        when(queueService.processQueueItem(item1)).thenReturn(true);

        int processed = worker.processDueQueueItems();

        assertThat(processed).isEqualTo(1);
        verify(queueService).requeueStaleClaims();
        verify(queueService).processQueueItem(item1);
        verify(queueService, never()).processQueueItem(item2);
    }

    @Test
    @DisplayName("Worker cycle should return 0 when no due items are found")
    void shouldReturnZeroWhenNoDueItems() {
        when(queueService.findDueReadyItemIds(25)).thenReturn(List.of());

        int processed = worker.processDueQueueItems();

        assertThat(processed).isEqualTo(0);
        verify(queueService).requeueStaleClaims();
        verify(queueService, never()).claimItem(any(), any());
    }

    @Test
    @DisplayName("Worker cycle should catch unexpected exceptions gracefully without throwing")
    void shouldHandleExceptionsGracefully() {
        when(queueService.requeueStaleClaims()).thenThrow(new RuntimeException("DB connection error"));

        assertDoesNotThrow(() -> worker.runQueueCycle());
    }
}
