package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ManualRecoveryExecutor implements RecoveryActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ManualRecoveryExecutor.class);

    private final RecoveryLinkService recoveryLinkService;

    public ManualRecoveryExecutor(RecoveryLinkService recoveryLinkService) {
        this.recoveryLinkService = recoveryLinkService;
    }

    @Override
    public boolean supports(RecoveryChannel channel) {
        return channel == RecoveryChannel.MANUAL;
    }

    @Override
    public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase recoveryCase) {
        log.info("Queueing case for merchant manual review: attemptId={}, caseId={}",
                attempt.getId(), recoveryCase.getId());

        String recoveryLink = recoveryLinkService.generateRecoveryLink(recoveryCase);

        return ExecutionResult.sent(
                "MANUAL_REVIEW_QUEUED",
                "Queued case for merchant manual recovery review",
                recoveryLink,
                "{\"channel\":\"MANUAL\",\"action\":\"manual_review\"}"
        );
    }
}
