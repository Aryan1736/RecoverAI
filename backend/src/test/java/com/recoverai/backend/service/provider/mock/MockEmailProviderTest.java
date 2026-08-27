package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockEmailProviderTest {

    private final MockEmailProvider provider = new MockEmailProvider();

    @Test
    @DisplayName("Should return successful delivery result for email request")
    void shouldReturnSuccessfulDeliveryResult() {
        EmailMessageRequest request = new EmailMessageRequest(
                "alice@example.com",
                "Alice",
                "Merchant A",
                new BigDecimal("2500.00"),
                "INR",
                "https://pay.recoverai.io/r/case-456",
                "AUTHENTICATION_FAILURE"
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProviderName()).isEqualTo("MOCK_EMAIL");
        assertThat(result.getResultCode()).isEqualTo("EMAIL_DISPATCHED");
        assertThat(result.getDeliveryId()).startsWith("mock_email_");
        assertThat(result.getMetadata()).contains("MOCK_EMAIL");
    }

    @Test
    @DisplayName("Should handle null request gracefully")
    void shouldHandleNullRequest() {
        CommunicationDeliveryResult result = provider.sendEmail(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).startsWith("mock_email_");
    }
}
