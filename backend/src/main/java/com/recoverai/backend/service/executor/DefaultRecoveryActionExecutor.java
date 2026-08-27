package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultRecoveryActionExecutor implements RecoveryActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultRecoveryActionExecutor.class);

    private final RecoveryLinkService recoveryLinkService;

    public DefaultRecoveryActionExecutor(RecoveryLinkService recoveryLinkService) {
        this.recoveryLinkService = recoveryLinkService;
    }

    @Override
    public boolean supports(RecoveryChannel channel) {
        // Supports all standard channels as the default mock/simulated executor fallback
        return true;
    }

    @Override
    public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase recoveryCase) {
        RecoveryChannel channel = attempt.getChannel() != null ? attempt.getChannel() : RecoveryChannel.MANUAL;
        UUID caseId = recoveryCase.getId();
        Customer customer = recoveryCase.getCustomer();
        String customerRef = customer != null && customer.getEmail() != null ? maskEmail(customer.getEmail()) : "N/A";

        log.info("Executing fallback recovery action for attemptId={}, caseId={}, channel={}, customer={}",
                attempt.getId(), caseId, channel, customerRef);

        String recoveryLink = recoveryLinkService.generateRecoveryLink(recoveryCase);


        switch (channel) {
            case WHATSAPP:
                return ExecutionResult.sent(
                        "WHATSAPP_DISPATCHED",
                        "Simulated WhatsApp message dispatched to customer",
                        recoveryLink,
                        "{\"channel\":\"WHATSAPP\",\"mock\":true}"
                );
            case EMAIL:
                return ExecutionResult.sent(
                        "EMAIL_DISPATCHED",
                        "Simulated recovery email dispatched to customer",
                        recoveryLink,
                        "{\"channel\":\"EMAIL\",\"mock\":true}"
                );
            case SMS:
                return ExecutionResult.sent(
                        "SMS_DISPATCHED",
                        "Simulated SMS recovery message dispatched to customer",
                        recoveryLink,
                        "{\"channel\":\"SMS\",\"mock\":true}"
                );
            case SMART_LINK:
                return ExecutionResult.sent(
                        "SMART_LINK_GENERATED",
                        "Generated dynamic smart recovery payment link",
                        recoveryLink,
                        "{\"channel\":\"SMART_LINK\",\"mock\":true}"
                );
            case RETRY_CHARGE:
                return ExecutionResult.sent(
                        "PAYMENT_RETRY_SCHEDULED",
                        "Simulated automated payment charge retry initiated",
                        recoveryLink,
                        "{\"channel\":\"RETRY_CHARGE\",\"mock\":true}"
                );
            case MANUAL:
            default:
                return ExecutionResult.sent(
                        "MANUAL_REVIEW_QUEUED",
                        "Queued case for merchant manual recovery review",
                        recoveryLink,
                        "{\"channel\":\"MANUAL\",\"mock\":true}"
                );
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "ANONYMOUS";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "*@" + email.substring(atIndex + 1);
        }
        return email.charAt(0) + "***@" + email.substring(atIndex + 1);
    }
}
