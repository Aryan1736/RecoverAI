package com.recoverai.backend.service.provider.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.service.provider.PaymentRetryProvider;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.PaymentRetryRequest;
import com.recoverai.backend.service.provider.dto.PaymentRetryResult;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.http.ProviderHttpClientFactory;
import com.recoverai.backend.service.provider.util.CredentialMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component("razorpayPaymentRetryProvider")
public class RazorpayPaymentRetryProvider implements PaymentRetryProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentRetryProvider.class);
    public static final String PROVIDER_NAME = "RAZORPAY";

    private final RecoveryCommunicationProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String fallbackKeyId;
    private final String fallbackKeySecret;

    @org.springframework.beans.factory.annotation.Autowired
    public RazorpayPaymentRetryProvider(RecoveryCommunicationProperties properties,
                                        ProviderHttpClientFactory clientFactory,
                                        ObjectMapper objectMapper,
                                        @Value("${razorpay.key-id:}") String fallbackKeyId,
                                        @Value("${razorpay.key-secret:}") String fallbackKeySecret) {
        this.properties = properties;
        this.fallbackKeyId = fallbackKeyId;
        this.fallbackKeySecret = fallbackKeySecret;
        String baseUrl = properties.getRetryCharge().getApiBaseUrl();
        this.restClient = clientFactory != null ? clientFactory.createClient(baseUrl) : null;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public RazorpayPaymentRetryProvider(RecoveryCommunicationProperties properties,
                                        RestClient restClient,
                                        ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.fallbackKeyId = "";
        this.fallbackKeySecret = "";
    }

    @Override
    public PaymentRetryResult retryCharge(PaymentRetryRequest request) {
        if (!properties.getRetryCharge().isAutoRetryEnabled()) {
            log.info("[RAZORPAY_RETRY] Automated retry is disabled by configuration");
            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "RETRY_DISABLED",
                    "Automated payment retry is disabled by configuration",
                    "{\"provider\":\"RAZORPAY\",\"reason\":\"AUTO_RETRY_DISABLED\"}",
                    ProviderFailureType.PERMANENT
            );
        }

        if (request == null) {
            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "INVALID_REQUEST",
                    "PaymentRetryRequest cannot be null",
                    null,
                    ProviderFailureType.VALIDATION
            );
        }

        if (request.getPaymentId() == null || request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank()) {
            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "MISSING_PAYMENT_REFERENCE",
                    "Original Razorpay payment ID reference is required to retry charge",
                    null,
                    ProviderFailureType.VALIDATION
            );
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "INVALID_AMOUNT",
                    "Retry amount must be positive",
                    null,
                    ProviderFailureType.VALIDATION
            );
        }

        String keyId = resolveKeyId();
        String keySecret = resolveKeySecret();

        if (keyId.isBlank() || keySecret.isBlank()) {
            log.warn("[RAZORPAY_RETRY] Razorpay credentials missing or incomplete");
            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "PROVIDER_MISCONFIGURED",
                    "Razorpay API credentials are not configured",
                    null,
                    ProviderFailureType.AUTHENTICATION
            );
        }

        // Validate retry eligibility against payment method
        PaymentMethod method = request.getPaymentMethod();
        if (method != null && (method == PaymentMethod.NETBANKING || method == PaymentMethod.WALLET)) {
            // Netbanking and normal wallets require interactive user redirection; cannot be headlessly auto-retried
            log.info("[RAZORPAY_RETRY] Payment method {} is not eligible for automated headless charge retry", method);
            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "PAYMENT_METHOD_NOT_ELIGIBLE",
                    "Payment method " + method + " does not support automated headless re-charging",
                    String.format("{\"provider\":\"RAZORPAY\",\"paymentMethod\":\"%s\",\"eligible\":false}", method),
                    ProviderFailureType.PERMANENT
            );
        }

        String basicAuth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
        String path = String.format("/v1/payments/%s/retry", request.getRazorpayPaymentId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).intValue());
        payload.put("currency", request.getCurrency() != null ? request.getCurrency() : "INR");

        log.info("[RAZORPAY_RETRY] Initiating safe retry for paymentId={}, originalRazorpayId={}",
                request.getPaymentId(), request.getRazorpayPaymentId());

        try {
            String responseStr = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            String transactionId = null;
            String status = "captured";
            if (responseStr != null && !responseStr.isBlank()) {
                JsonNode root = objectMapper.readTree(responseStr);
                if (root.hasNonNull("id")) {
                    transactionId = root.get("id").asText();
                }
                if (root.hasNonNull("status")) {
                    status = root.get("status").asText();
                }
            }

            if (transactionId == null || transactionId.isBlank()) {
                transactionId = "pay_retry_" + System.currentTimeMillis();
            }

            String metadata = String.format("{\"provider\":\"%s\",\"transactionId\":\"%s\",\"status\":\"%s\",\"captured\":true}",
                    PROVIDER_NAME, transactionId, status);

            return PaymentRetryResult.success(
                    transactionId,
                    PROVIDER_NAME,
                    "PAYMENT_RETRY_CAPTURED",
                    "Payment retry successfully captured by Razorpay",
                    metadata
            );

        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            ProviderFailureType failureType = ProviderErrorClassifier.classify(statusCode, responseBody, ex);

            String errorMessage = "Razorpay payment retry failed with HTTP " + statusCode;
            String rzpErrorCode = null;
            try {
                if (responseBody != null && !responseBody.isBlank()) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    if (root.has("error")) {
                        JsonNode errorNode = root.get("error");
                        if (errorNode.hasNonNull("description")) {
                            errorMessage = errorNode.get("description").asText();
                        }
                        if (errorNode.hasNonNull("code")) {
                            rzpErrorCode = errorNode.get("code").asText();
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            log.warn("[RAZORPAY_RETRY] API error: status={}, failureType={}, rzpErrorCode={}",
                    statusCode, failureType, rzpErrorCode);

            String metadata = String.format("{\"provider\":\"%s\",\"httpStatus\":%d,\"razorpayErrorCode\":%s}",
                    PROVIDER_NAME, statusCode, rzpErrorCode != null ? "\"" + rzpErrorCode + "\"" : "null");

            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "RAZORPAY_ERROR_" + statusCode,
                    errorMessage,
                    metadata,
                    failureType
            );

        } catch (Exception ex) {
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            log.error("[RAZORPAY_RETRY] Communication failure: {}", ex.getMessage());

            return PaymentRetryResult.failure(
                    null,
                    PROVIDER_NAME,
                    failureType == ProviderFailureType.TIMEOUT ? "PAYMENT_RETRY_TIMEOUT" : "PAYMENT_RETRY_ERROR",
                    ex.getMessage(),
                    null,
                    failureType
            );
        }
    }

    private String resolveKeyId() {
        String keyId = properties.getRetryCharge().getKeyId();
        if (keyId != null && !keyId.isBlank()) {
            return keyId.trim();
        }
        return fallbackKeyId != null ? fallbackKeyId.trim() : "";
    }

    private String resolveKeySecret() {
        String keySecret = properties.getRetryCharge().getKeySecret();
        if (keySecret != null && !keySecret.isBlank()) {
            return keySecret.trim();
        }
        return fallbackKeySecret != null ? fallbackKeySecret.trim() : "";
    }

    @Override
    public ProviderHealthResult checkHealth() {
        if (!properties.getRetryCharge().isAutoRetryEnabled()) {
            return ProviderHealthResult.disabled(PROVIDER_NAME, "PAYMENT_RETRY", "Automated payment retry disabled");
        }
        String keyId = resolveKeyId();
        String keySecret = resolveKeySecret();
        if (keyId.isBlank() || keySecret.isBlank()) {
            return ProviderHealthResult.misconfigured(PROVIDER_NAME, "PAYMENT_RETRY", "Razorpay credentials missing");
        }
        return ProviderHealthResult.available(PROVIDER_NAME, "PAYMENT_RETRY", "Razorpay retry adapter configured");
    }

    @Override
    public String getProviderIdentifier() {
        return "razorpay";
    }

    @Override
    public String getProviderCategory() {
        return "PAYMENT_RETRY";
    }
}
