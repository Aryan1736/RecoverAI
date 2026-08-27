package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmartLinkRecoveryExecutor implements RecoveryActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(SmartLinkRecoveryExecutor.class);

    private final RecoveryLinkService recoveryLinkService;

    public SmartLinkRecoveryExecutor(RecoveryLinkService recoveryLinkService) {
        this.recoveryLinkService = recoveryLinkService;
    }

    @Override
    public boolean supports(RecoveryChannel channel) {
        return channel == RecoveryChannel.SMART_LINK;
    }

    @Override
    public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase recoveryCase) {
        log.info("Generating smart recovery link for attemptId={}, caseId={}",
                attempt.getId(), recoveryCase.getId());

        String recoveryLink = recoveryLinkService.generateRecoveryLink(recoveryCase);

        return ExecutionResult.sent(
                "SMART_LINK_GENERATED",
                "Generated dynamic smart recovery payment link",
                recoveryLink,
                "{\"channel\":\"SMART_LINK\",\"status\":\"active\"}"
        );
    }
}
