package com.recoverai.backend.service.provider.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RazorpayPaymentRetryProviderTest {

    private RecoveryCommunicationProperties properties;
    private MockRestServiceServer mockServer;
    private RazorpayPaymentRetryProvider provider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RecoveryCommunicationProperties();
        properties.getRetryCharge().setProvider("razorpay");
        properties.getRetryCharge().setAutoRetryEnabled(true);
        properties.getRetryCharge().setKeyId("rzp_test_key_id");
        properties.getRetryCharge().setKeySecret("rzp_test_secret");
        properties.getRetryCharge().setApiBaseUrl("https://api.razorpay.com");

        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.razorpay.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new RazorpayPaymentRetryProvider(properties, restClient, objectMapper);
    }

    @Test
    @DisplayName("Should fail safely when auto-retry is disabled by merchant configuration")
    void shouldReturnDisabledWhenAutoRetryOff() {
        properties.getRetryCharge().setAutoRetryEnabled(false);

        PaymentRetryRequest request = new PaymentRetryRequest(
                UUID.randomUUID(), "pay_orig_123", UUID.randomUUID(), new BigDecimal("1500.00"), "INR", PaymentMethod.CARD
        );

        PaymentRetryResult result = provider.retryCharge(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResultCode()).isEqualTo("RETRY_DISABLED");
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.PERMANENT);
        assertThat(result.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("Should reject non-recurring payment method (Netbanking) as ineligible for headless retry")
    void shouldRejectIneligiblePaymentMethod() {
        PaymentRetryRequest request = new PaymentRetryRequest(
                UUID.randomUUID(), "pay_orig_123", UUID.randomUUID(), new BigDecimal("1500.00"), "INR", PaymentMethod.NETBANKING
        );

        PaymentRetryResult result = provider.retryCharge(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResultCode()).isEqualTo("PAYMENT_METHOD_NOT_ELIGIBLE");
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.PERMANENT);
        assertThat(result.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("Should successfully retry and capture eligible payment charge")
    void shouldRetryChargeSuccessfully() {
        mockServer.expect(requestTo("https://api.razorpay.com/v1/payments/pay_orig_123/retry"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Basic ")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"id\":\"pay_captured_789\",\"status\":\"captured\"}", MediaType.APPLICATION_JSON));

        PaymentRetryRequest request = new PaymentRetryRequest(
                UUID.randomUUID(), "pay_orig_123", UUID.randomUUID(), new BigDecimal("1500.00"), "INR", PaymentMethod.CARD
        );

        PaymentRetryResult result = provider.retryCharge(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).isEqualTo("pay_captured_789");
        assertThat(result.getResultCode()).isEqualTo("PAYMENT_RETRY_CAPTURED");
        assertThat(result.getProviderName()).isEqualTo("RAZORPAY");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 429 rate limit as RATE_LIMITED and retryable")
    void shouldHandleRazorpayRateLimit() {
        mockServer.expect(requestTo("https://api.razorpay.com/v1/payments/pay_orig_123/retry"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"BAD_REQUEST_ERROR\",\"description\":\"Too Many Requests\"}}"));

        PaymentRetryRequest request = new PaymentRetryRequest(
                UUID.randomUUID(), "pay_orig_123", UUID.randomUUID(), new BigDecimal("1500.00"), "INR", PaymentMethod.CARD
        );

        PaymentRetryResult result = provider.retryCharge(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.RATE_LIMITED);
        assertThat(result.isRetryable()).isTrue();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should classify HTTP 400 validation error as PERMANENT and non-retryable")
    void shouldHandleRazorpayBadRequest() {
        mockServer.expect(requestTo("https://api.razorpay.com/v1/payments/pay_orig_123/retry"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"BAD_REQUEST_ERROR\",\"description\":\"Payment not found or expired\"}}"));

        PaymentRetryRequest request = new PaymentRetryRequest(
                UUID.randomUUID(), "pay_orig_123", UUID.randomUUID(), new BigDecimal("1500.00"), "INR", PaymentMethod.CARD
        );

        PaymentRetryResult result = provider.retryCharge(request);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureType()).isEqualTo(ProviderFailureType.VALIDATION);
        assertThat(result.isRetryable()).isFalse();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should check Razorpay health status correctly")
    void shouldCheckHealth() {
        ProviderHealthResult healthy = provider.checkHealth();
        assertThat(healthy.getStatus()).isEqualTo(ProviderHealthStatus.AVAILABLE);

        properties.getRetryCharge().setAutoRetryEnabled(false);
        ProviderHealthResult disabled = provider.checkHealth();
        assertThat(disabled.getStatus()).isEqualTo(ProviderHealthStatus.DISABLED);

        properties.getRetryCharge().setAutoRetryEnabled(true);
        properties.getRetryCharge().setKeyId("");
        ProviderHealthResult misconfigured = provider.checkHealth();
        assertThat(misconfigured.getStatus()).isEqualTo(ProviderHealthStatus.MISCONFIGURED);
    }
}
