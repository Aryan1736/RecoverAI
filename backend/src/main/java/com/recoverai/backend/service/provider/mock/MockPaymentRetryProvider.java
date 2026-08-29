package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("mockPaymentRetryProvider")
public class MockPaymentRetryProvider implements PaymentRetryProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentRetryProvider.class);

    private final RecoveryCommunicationProperties properties;

    public MockPaymentRetryProvider(RecoveryCommunicationProperties properties) {
        this.properties = properties;
    }

    @Override
    public PaymentRetryResult retryCharge(PaymentRetryRequest request) {
        boolean retryEnabled = properties.getRetryCharge().isAutoRetryEnabled();
        String transactionId = "mock_pay_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[MOCK_PAYMENT_RETRY] Executing payment retry: transactionId={}, paymentId={}, amount={} {}, enabled={}",
                transactionId, request != null ? request.getPaymentId() : "N/A",
                request != null ? request.getAmount() : "N/A",
                request != null ? request.getCurrency() : "INR",
                retryEnabled);

        if (!retryEnabled) {
            String metadata = String.format("{\"provider\":\"MOCK_RAZORPAY\",\"transactionId\":\"%s\",\"simulated\":true,\"reason\":\"AUTO_RETRY_DISABLED\"}",
                    transactionId);
            return PaymentRetryResult.failure(
                    transactionId,
                    "MOCK_RAZORPAY",
                    "RETRY_DISABLED",
                    "Automated payment retry is disabled by merchant configuration",
                    metadata
            );
        }

        String metadata = String.format("{\"provider\":\"MOCK_RAZORPAY\",\"transactionId\":\"%s\",\"simulated\":true,\"status\":\"captured\"}",
                    transactionId);

        return PaymentRetryResult.success(
                transactionId,
                "MOCK_RAZORPAY",
                "PAYMENT_RETRY_CAPTURED",
                "Simulated payment retry captured successfully",
                metadata
        );
    }

    @Override
    public ProviderHealthResult checkHealth() {
        if (!properties.getRetryCharge().isAutoRetryEnabled()) {
            return ProviderHealthResult.disabled("MOCK_RAZORPAY", "PAYMENT_RETRY", "Mock payment retry disabled");
        }
        return ProviderHealthResult.available("MOCK_RAZORPAY", "PAYMENT_RETRY", "Mock payment retry provider active");
    }

    @Override
    public String getProviderIdentifier() {
        return "mock";
    }

    @Override
    public String getProviderCategory() {
        return "PAYMENT_RETRY";
    }
}

