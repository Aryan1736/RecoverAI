package com.recoverai.backend.observability;

import com.recoverai.backend.config.ObservabilityProperties;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthService;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderHealthIndicatorTest {

    @Mock
    private ProviderHealthService providerHealthService;

    private ObservabilityProperties observabilityProperties;
    private ProviderHealthIndicator providerHealthIndicator;

    @BeforeEach
    void setUp() {
        observabilityProperties = new ObservabilityProperties();
        observabilityProperties.getProviderHealth().setCacheTtlSeconds(10L);

        providerHealthIndicator = new ProviderHealthIndicator(providerHealthService, observabilityProperties);
    }

    @Test
    @DisplayName("Should report UP when all registered providers are AVAILABLE")
    void shouldReportUpWhenAllProvidersAvailable() {
        List<ProviderHealthResult> results = List.of(
                ProviderHealthResult.available("TWILIO", "WHATSAPP", "Connected"),
                ProviderHealthResult.available("SENDGRID", "EMAIL", "SMTP ready"),
                ProviderHealthResult.available("RAZORPAY", "PAYMENT", "API reachable")
        );
        when(providerHealthService.checkAll()).thenReturn(results);

        Health health = providerHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) health.getDetails().get("components");
        assertThat(components).isNotNull();
        assertThat(components).containsEntry("WHATSAPP_TWILIO", "UP");
        assertThat(components).containsEntry("EMAIL_SENDGRID", "UP");
        assertThat(components).containsEntry("PAYMENT_RAZORPAY", "UP");
    }

    @Test
    @DisplayName("Should report DEGRADED when any provider is transiently degraded")
    void shouldReportDegradedWhenProviderFailsTransiently() {
        List<ProviderHealthResult> results = List.of(
                ProviderHealthResult.available("TWILIO", "WHATSAPP", "Connected"),
                ProviderHealthResult.degraded("SENDGRID", "EMAIL", "High latency observed (1500ms)")
        );
        when(providerHealthService.checkAll()).thenReturn(results);

        Health health = providerHealthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) health.getDetails().get("components");
        assertThat(components).containsEntry("EMAIL_SENDGRID", "DEGRADED");
    }

    @Test
    @DisplayName("Should report DOWN when a provider is misconfigured")
    void shouldReportDownWhenProviderMisconfigured() {
        List<ProviderHealthResult> results = List.of(
                ProviderHealthResult.available("TWILIO", "WHATSAPP", "Connected"),
                ProviderHealthResult.misconfigured("SENDGRID", "EMAIL", "Missing API key in configuration")
        );
        when(providerHealthService.checkAll()).thenReturn(results);

        Health health = providerHealthIndicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) health.getDetails().get("components");
        assertThat(components).containsEntry("EMAIL_SENDGRID", "DOWN");
    }

    @Test
    @DisplayName("Should sanitize sensitive credentials and query parameters in diagnostic messages")
    void shouldSanitizeCredentialsInMessages() {
        String sensitiveMsg = "Failed connecting to https://api.twilio.com/v1/Accounts?auth_token=super_secret_123 with key=sk_live_999";
        List<ProviderHealthResult> results = List.of(
                ProviderHealthResult.degraded("TWILIO", "SMS", sensitiveMsg)
        );
        when(providerHealthService.checkAll()).thenReturn(results);

        Health health = providerHealthIndicator.health();

        Map<?, ?> messages = (Map<?, ?>) health.getDetails().get("messages");
        assertThat(messages).isNotNull();
        String sanitized = (String) messages.get("SMS_TWILIO");
        assertThat(sanitized).doesNotContain("super_secret_123");
        assertThat(sanitized).doesNotContain("sk_live_999");
    }

    @Test
    @DisplayName("Should reuse cached results within TTL to prevent hammering external providers")
    void shouldCacheResultsWithinTtl() {
        List<ProviderHealthResult> results = List.of(
                ProviderHealthResult.available("MOCK", "WHATSAPP", "OK")
        );
        when(providerHealthService.checkAll()).thenReturn(results);

        Health health1 = providerHealthIndicator.health();
        Health health2 = providerHealthIndicator.health();

        assertThat(health1.getStatus()).isEqualTo(Status.UP);
        assertThat(health2.getStatus()).isEqualTo(Status.UP);
        // Only 1 invocation of checkAll() within TTL
        verify(providerHealthService, times(1)).checkAll();
    }

    @Test
    @DisplayName("Should return UNKNOWN when provider health is disabled")
    void shouldReturnUnknownWhenDisabled() {
        observabilityProperties.getProviderHealth().setEnabled(false);

        Health health = providerHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("status", "DISABLED");
        verifyNoInteractions(providerHealthService);
    }
}
