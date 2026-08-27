package com.recoverai.backend.service.executor;

import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class RetryChargeRecoveryExecutor implements RecoveryActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryChargeRecoveryExecutor.class);

    private final PaymentRetryProvider paymentRetryProvider;

    public RetryChargeRecoveryExecutor(PaymentRetryProvider paymentRetryProvider) {
        this.paymentRetryProvider = paymentRetryProvider;
    }

    @Override
    public boolean supports(RecoveryChannel channel) {
        return channel == RecoveryChannel.RETRY_CHARGE;
    }

    @Override
    public ExecutionResult execute(RecoveryAttempt attempt, RecoveryCase recoveryCase) {
        log.info("Executing automated payment retry for attemptId={}, caseId={}",
                attempt.getId(), recoveryCase.getId());

        Payment payment = recoveryCase.getPayment();
        UUID paymentId = payment != null ? payment.getId() : null;
        String razorpayPaymentId = payment != null ? payment.getRazorpayPaymentId() : null;
        UUID merchantId = recoveryCase.getMerchant() != null ? recoveryCase.getMerchant().getId() : null;
        BigDecimal amount = payment != null ? payment.getAmount() : recoveryCase.getEstimatedRecoverableAmount();
        String currency = payment != null ? payment.getCurrency() : recoveryCase.getCurrency();
        PaymentMethod paymentMethod = payment != null ? payment.getMethod() : null;

        PaymentRetryRequest request = new PaymentRetryRequest(
                paymentId,
                razorpayPaymentId,
                merchantId,
                amount,
                currency,
                paymentMethod
        );

        PaymentRetryResult retryResult = paymentRetryProvider.retryCharge(request);

        if (retryResult.isSuccess()) {
            return ExecutionResult.success(
                    retryResult.getResultCode(),
                    retryResult.getResultMessage(),
                    null,
                    retryResult.getMetadata()
            );
        } else {
            return ExecutionResult.failed(
                    retryResult.getResultCode() != null ? retryResult.getResultCode() : "PAYMENT_RETRY_FAILED",
                    retryResult.getResultMessage(),
                    null,
                    retryResult.getMetadata()
            );
        }
    }
}
