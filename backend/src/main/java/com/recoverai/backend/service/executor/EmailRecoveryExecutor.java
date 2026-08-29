package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EmailRecoveryExecutor implements RecoveryActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(EmailRecoveryExecutor.class);

    private final EmailProvider emailProvider;
    private final RecoveryLinkService recoveryLinkService;

    public EmailRecoveryExecutor(EmailProvider emailProvider,
                                RecoveryLinkService recoveryLinkService) {
        this.emailProvider = emailProvider;
        this.recoveryLinkService = recoveryLinkService;
    }

    @Override
    public boolean supports(RecoveryChannel channel) {
        return channel == RecoveryChannel.EMAIL;
    }

    @Override
    public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase recoveryCase) {
        log.info("Executing Email recovery action for attemptId={}, caseId={}",
                attempt.getId(), recoveryCase.getId());

        String recoveryLink = recoveryLinkService.generateRecoveryLink(recoveryCase);
        Customer customer = recoveryCase.getCustomer();
        Merchant merchant = recoveryCase.getMerchant();
        Payment payment = recoveryCase.getPayment();

        String recipientEmail = customer != null ? customer.getEmail() : null;
        String customerName = customer != null ? customer.getName() : "Valued Customer";
        String merchantName = merchant != null ? merchant.getName() : "Merchant";
        BigDecimal amount = payment != null ? payment.getAmount() : recoveryCase.getEstimatedRecoverableAmount();
        String currency = payment != null ? payment.getCurrency() : recoveryCase.getCurrency();
        String failureReason = recoveryCase.getFailureReasonCategory();

        EmailMessageRequest request = new EmailMessageRequest(
                recipientEmail,
                customerName,
                merchantName,
                amount,
                currency,
                recoveryLink,
                failureReason
        );

        CommunicationDeliveryResult deliveryResult = emailProvider.sendEmail(request);

        if (deliveryResult.isSuccess()) {
            return ExecutionResult.sent(
                    deliveryResult.getResultCode(),
                    deliveryResult.getResultMessage(),
                    recoveryLink,
                    deliveryResult.getMetadata()
            );
        } else {
            return ExecutionResult.failed(
                    deliveryResult.getResultCode() != null ? deliveryResult.getResultCode() : "EMAIL_DELIVERY_FAILED",
                    deliveryResult.getResultMessage(),
                    recoveryLink,
                    deliveryResult.getMetadata(),
                    deliveryResult.getFailureType()
            );
        }
    }
}
