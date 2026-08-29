package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.link.RecoveryLinkService;
import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SmsRecoveryExecutor implements RecoveryActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(SmsRecoveryExecutor.class);

    private final SmsProvider smsProvider;
    private final RecoveryLinkService recoveryLinkService;

    public SmsRecoveryExecutor(SmsProvider smsProvider,
                              RecoveryLinkService recoveryLinkService) {
        this.smsProvider = smsProvider;
        this.recoveryLinkService = recoveryLinkService;
    }

    @Override
    public boolean supports(RecoveryChannel channel) {
        return channel == RecoveryChannel.SMS;
    }

    @Override
    public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase recoveryCase) {
        log.info("Executing SMS recovery action for attemptId={}, caseId={}",
                attempt.getId(), recoveryCase.getId());

        String recoveryLink = recoveryLinkService.generateRecoveryLink(recoveryCase);
        Customer customer = recoveryCase.getCustomer();
        Merchant merchant = recoveryCase.getMerchant();
        Payment payment = recoveryCase.getPayment();

        String recipientPhone = customer != null ? customer.getPhone() : null;
        String customerName = customer != null ? customer.getName() : "Valued Customer";
        String merchantName = merchant != null ? merchant.getName() : "Merchant";
        BigDecimal amount = payment != null ? payment.getAmount() : recoveryCase.getEstimatedRecoverableAmount();
        String currency = payment != null ? payment.getCurrency() : recoveryCase.getCurrency();

        SmsMessageRequest request = new SmsMessageRequest(
                recipientPhone,
                customerName,
                merchantName,
                amount,
                currency,
                recoveryLink
        );

        CommunicationDeliveryResult deliveryResult = smsProvider.sendSms(request);

        if (deliveryResult.isSuccess()) {
            return ExecutionResult.sent(
                    deliveryResult.getResultCode(),
                    deliveryResult.getResultMessage(),
                    recoveryLink,
                    deliveryResult.getMetadata()
            );
        } else {
            return ExecutionResult.failed(
                    deliveryResult.getResultCode() != null ? deliveryResult.getResultCode() : "SMS_DELIVERY_FAILED",
                    deliveryResult.getResultMessage(),
                    recoveryLink,
                    deliveryResult.getMetadata(),
                    deliveryResult.getFailureType()
            );
        }
    }
}
