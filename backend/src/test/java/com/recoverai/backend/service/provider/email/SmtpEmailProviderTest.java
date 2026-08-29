package com.recoverai.backend.service.provider.email;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpEmailProviderTest {

    private RecoveryCommunicationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.getEmail().setProvider("smtp");
        properties.getEmail().setFromAddress("recover@recoverai.io");
        properties.getEmail().getSmtp().setHost("smtp.mailtrap.io");
        properties.getEmail().getSmtp().setPort(587);
        properties.getEmail().getSmtp().setUsername("test_user");
        properties.getEmail().getSmtp().setPassword("test_pass");
        properties.getEmail().getSmtp().setTlsEnabled(true);
    }

    @Test
    @DisplayName("Should successfully dispatch email when transport succeeds")
    void shouldSendEmailSuccessfully() {
        SmtpEmailProvider.SmtpTransport mockTransport = (host, port, tls, username, password, from, to, subject, body) -> "smtp_delivery_12345";
        SmtpEmailProvider provider = new SmtpEmailProvider(properties, mockTransport);

        EmailMessageRequest request = new EmailMessageRequest(
                "customer@example.com", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDeliveryId()).isEqualTo("smtp_delivery_12345");
        assertThat(result.getProviderName()).isEqualTo("SMTP_EMAIL");
    }

    @Test
    @DisplayName("Should classify SMTP 535 authentication failure as non-retryable AUTHENTICATION")
    void shouldHandleSmtpAuthFailure() {
        SmtpEmailProvider.SmtpTransport authFailTransport = (host, port, tls, username, password, from, to, subject, body) -> {
            throw new SmtpEmailProvider.SmtpException(535, "535 Authentication credentials invalid", ProviderFailureType.AUTHENTICATION);
        };
        SmtpEmailProvider provider = new SmtpEmailProvider(properties, authFailTransport);

        EmailMessageRequest request = new EmailMessageRequest(
                "customer@example.com", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.AUTHENTICATION);
        assertThat(result.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("Should classify SMTP 421 / 451 transient failure as retryable TRANSIENT")
    void shouldHandleSmtpTransientFailure() {
        SmtpEmailProvider.SmtpTransport transientTransport = (host, port, tls, username, password, from, to, subject, body) -> {
            throw new SmtpEmailProvider.SmtpException(421, "421 Service not available, closing transmission channel", ProviderFailureType.TRANSIENT);
        };
        SmtpEmailProvider provider = new SmtpEmailProvider(properties, transientTransport);

        EmailMessageRequest request = new EmailMessageRequest(
                "customer@example.com", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.TRANSIENT);
        assertThat(result.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("Should classify SocketTimeoutException as TIMEOUT and retryable")
    void shouldHandleTimeout() {
        SmtpEmailProvider.SmtpTransport timeoutTransport = (host, port, tls, username, password, from, to, subject, body) -> {
            throw new SocketTimeoutException("Read timed out");
        };
        SmtpEmailProvider provider = new SmtpEmailProvider(properties, timeoutTransport);

        EmailMessageRequest request = new EmailMessageRequest(
                "customer@example.com", "Alice", "Acme", new BigDecimal("100.00"), "INR", "https://pay.recoverai.io/r/1", null
        );

        CommunicationDeliveryResult result = provider.sendEmail(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.TIMEOUT);
        assertThat(result.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("Should validate health status")
    void shouldCheckHealth() {
        SmtpEmailProvider provider = new SmtpEmailProvider(properties);
        ProviderHealthResult healthy = provider.checkHealth();
        assertThat(healthy.getStatus()).isEqualTo(ProviderHealthStatus.AVAILABLE);

        properties.getEmail().getSmtp().setHost("");
        ProviderHealthResult unhealthy = provider.checkHealth();
        assertThat(unhealthy.getStatus()).isEqualTo(ProviderHealthStatus.MISCONFIGURED);
    }
}
