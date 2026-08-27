package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentRetryProviderTest {

    private RecoveryCommunicationProperties properties;
    private MockPaymentRetryProvider provider;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        provider = new MockPaymentRetryProvider(properties);
    }

    @Test
    @DisplayName("Should return successful payment retry result when auto retry enabled")
    void shouldReturnSuccessWhenEnabled() {
        PaymentRetryRequest request = new PaymentRetryRequest(
                UUID.randomUUID(),
                "pay_12345",
                UUID.randomUUID(),
                new BigDecimal("1500.00"),
                "INR",
                PaymentMethod.CARD
        );

        PaymentRetryResult result = provider.retryCharge(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProviderName()).isEqualTo("MOCK_RAZORPAY");
        assertThat(result.getResultCode()).isEqualTo("PAYMENT_RETRY_CAPTURED");
        assertThat(result.getTransactionId()).startsWith("mock_pay_");
        assertThat(result.getMetadata()).contains("MOCK_RAZORPAY");
    }

    @Test
    @DisplayName("Should return failure when auto retry is disabled")
    void shouldReturnFailureWhenDisabled() {
        properties.getRetryCharge().setAutoRetryEnabled(false);

        PaymentRetryRequest request = new PaymentRetryRequest(
                UUID.randomUUID(),
                "pay_12345",
                UUID.randomUUID(),
                new BigDecimal("1500.00"),
                "INR",
                PaymentMethod.CARD
        );

        PaymentRetryResult result = provider.retryCharge(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResultCode()).isEqualTo("RETRY_DISABLED");
        assertThat(result.getResultMessage()).contains("disabled");
    }
}
