package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockSmsProviderTest {

    private final MockSmsProvider provider = new MockSmsProvider();

    @Test
    @DisplayName("Should return successful delivery result for SMS request")
    void shouldReturnSuccessfulDeliveryResult() {
        SmsMessageRequest request = new SmsMessageRequest(
                "+919876543210",
                "Alice",
                "Merchant A",
                new BigDecimal("500.00"),
                "INR",
                "https://pay.recoverai.io/r/case-789"
        );

        CommunicationDeliveryResult result = provider.sendSms(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProviderName()).isEqualTo("MOCK_SMS");
        assertThat(result.getResultCode()).isEqualTo("SMS_DISPATCHED");
        assertThat(result.getDeliveryId()).startsWith("mock_sms_");
        assertThat(result.getMetadata()).contains("MOCK_SMS");
    }

    @Test
    @DisplayName("Should handle null request gracefully")
    void shouldHandleNullRequest() {
        CommunicationDeliveryResult result = provider.sendSms(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).startsWith("mock_sms_");
    }
}
