package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualRecoveryExecutorTest {

    @Mock
    private RecoveryLinkService recoveryLinkService;

    private ManualRecoveryExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ManualRecoveryExecutor(recoveryLinkService);
    }

    @Test
    @DisplayName("Should support MANUAL channel only")
    void shouldSupportManual() {
        assertThat(executor.supports(RecoveryChannel.MANUAL)).isTrue();
        assertThat(executor.supports(RecoveryChannel.WHATSAPP)).isFalse();
    }

    @Test
    @DisplayName("Should return SENT status with queued manual review")
    void shouldQueueManualReview() {
        RecoveryCase recoveryCase = RecoveryCase.builder().id(UUID.randomUUID()).build();
        RecoveryAttempt attempt = RecoveryAttempt.builder().id(UUID.randomUUID()).build();

        String expectedLink = "https://pay.recoverai.io/r/" + recoveryCase.getId();
        when(recoveryLinkService.generateRecoveryLink(recoveryCase)).thenReturn(expectedLink);

        ExecutionResult result = executor.execute(attempt, recoveryCase);

        assertThat(result.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        assertThat(result.getResultCode()).isEqualTo("MANUAL_REVIEW_QUEUED");
        assertThat(result.getRecoveryLink()).isEqualTo(expectedLink);
        assertThat(result.getMetadata()).contains("MANUAL");
    }
}
