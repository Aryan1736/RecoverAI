package com.recoverai.backend.service.provider.mock;

import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.WhatsAppMessageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MockWhatsAppProviderTest {

    private final MockWhatsAppProvider provider = new MockWhatsAppProvider();

    @Test
    @DisplayName("Should return successful delivery result with masked logging and simulated delivery ID")
    void shouldReturnSuccessfulDeliveryResult() {
        WhatsAppMessageRequest request = new WhatsAppMessageRequest(
                "+919876543210",
                "Alice",
                "Merchant A",
                new BigDecimal("1500.00"),
                "INR",
                "https://pay.recoverai.io/r/case-123",
                "TIMEOUT"
        );

        CommunicationDeliveryResult result = provider.sendWhatsApp(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProviderName()).isEqualTo("MOCK_WHATSAPP");
        assertThat(result.getResultCode()).isEqualTo("WHATSAPP_DISPATCHED");
        assertThat(result.getDeliveryId()).startsWith("mock_wa_");
        assertThat(result.getMetadata()).contains("MOCK_WHATSAPP");
    }

    @Test
    @DisplayName("Should handle null request gracefully")
    void shouldHandleNullRequest() {
        CommunicationDeliveryResult result = provider.sendWhatsApp(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).startsWith("mock_wa_");
    }
}
