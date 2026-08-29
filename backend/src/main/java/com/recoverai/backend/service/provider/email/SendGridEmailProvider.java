package com.recoverai.backend.service.provider.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.EmailProvider;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.EmailMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.http.ProviderHttpClientFactory;
import com.recoverai.backend.service.provider.util.CredentialMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component("sendGridEmailProvider")
public class SendGridEmailProvider implements EmailProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);
    public static final String PROVIDER_NAME = "SENDGRID_EMAIL";

    private final RecoveryCommunicationProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public SendGridEmailProvider(RecoveryCommunicationProperties properties,
                                 ProviderHttpClientFactory clientFactory,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = clientFactory != null ? clientFactory.createClient(properties.getEmail().getApiBaseUrl()) : null;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public SendGridEmailProvider(RecoveryCommunicationProperties properties,
                                 RestClient restClient,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public CommunicationDeliveryResult sendEmail(EmailMessageRequest request) {
        if (request == null) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "INVALID_REQUEST",
                    "Request cannot be null", null, ProviderFailureType.VALIDATION);
        }

        String recipientEmail = request.getRecipientEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "MISSING_RECIPIENT_EMAIL",
                    "Recipient email is required", null, ProviderFailureType.VALIDATION);
        }

        String apiKey = properties.getEmail().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "PROVIDER_MISCONFIGURED",
                    "SendGrid API Key is not configured", null, ProviderFailureType.AUTHENTICATION);
        }

        String fromAddress = properties.getEmail().getFromAddress();
        String fromName = properties.getEmail().getFromName();

        Map<String, Object> payload = buildSendGridPayload(request, fromAddress, fromName);

        log.info("[SENDGRID_EMAIL] Dispatching email to recipient={}", CredentialMasker.maskEmail(recipientEmail));

        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("/v3/mail/send")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            String messageId = null;
            HttpHeaders headers = response.getHeaders();
            if (headers != null && headers.containsKey("X-Message-Id")) {
                messageId = headers.getFirst("X-Message-Id");
            }
            if (messageId == null || messageId.isBlank()) {
                messageId = "sg_" + UUID.randomUUID().toString().substring(0, 12);
            }

            String metadata = String.format("{\"provider\":\"%s\",\"deliveryId\":\"%s\",\"statusCode\":%d}",
                    PROVIDER_NAME, messageId, response.getStatusCode().value());

            return CommunicationDeliveryResult.success(
                    messageId,
                    PROVIDER_NAME,
                    "EMAIL_DISPATCHED",
                    "Email accepted by SendGrid API",
                    metadata
            );

        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            ProviderFailureType failureType = ProviderErrorClassifier.classify(statusCode, responseBody, ex);

            String errorMessage = "SendGrid API failed with status " + statusCode;
            try {
                if (responseBody != null && !responseBody.isBlank()) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    if (root.has("errors") && root.get("errors").isArray() && root.get("errors").size() > 0) {
                        JsonNode firstErr = root.get("errors").get(0);
                        if (firstErr.hasNonNull("message")) {
                            errorMessage = firstErr.get("message").asText();
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            log.warn("[SENDGRID_EMAIL] API error: status={}, failureType={}, error={}",
                    statusCode, failureType, errorMessage);

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "SENDGRID_ERROR_" + statusCode,
                    errorMessage,
                    String.format("{\"provider\":\"%s\",\"httpStatus\":%d}", PROVIDER_NAME, statusCode),
                    failureType
            );

        } catch (Exception ex) {
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            log.error("[SENDGRID_EMAIL] Communication failure: {}", ex.getMessage());

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    failureType == ProviderFailureType.TIMEOUT ? "EMAIL_TIMEOUT" : "EMAIL_DISPATCH_ERROR",
                    ex.getMessage(),
                    null,
                    failureType
            );
        }
    }

    private Map<String, Object> buildSendGridPayload(EmailMessageRequest request, String fromAddress, String fromName) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, String> to = new HashMap<>();
        to.put("email", request.getRecipientEmail());
        if (request.getCustomerName() != null && !request.getCustomerName().isBlank()) {
            to.put("name", request.getCustomerName());
        }

        payload.put("personalizations", List.of(Map.of("to", List.of(to))));

        Map<String, String> from = new HashMap<>();
        from.put("email", fromAddress != null && !fromAddress.isBlank() ? fromAddress : "recover@recoverai.io");
        from.put("name", fromName != null && !fromName.isBlank() ? fromName : "RecoverAI Payment Recovery");
        payload.put("from", from);

        payload.put("subject", "Action Required: Complete your payment with " + (request.getMerchantName() != null ? request.getMerchantName() : "RecoverAI"));

        String textContent = String.format("Hello %s,\n\nYour recent payment of %s %s to %s could not be completed.\n\nPlease use the following secure link to complete your payment:\n%s\n\nThank you,\n%s",
                request.getCustomerName() != null ? request.getCustomerName() : "Valued Customer",
                request.getAmount() != null ? request.getAmount() : "",
                request.getCurrency() != null ? request.getCurrency() : "INR",
                request.getMerchantName() != null ? request.getMerchantName() : "our service",
                request.getRecoveryLink() != null ? request.getRecoveryLink() : "",
                request.getMerchantName() != null ? request.getMerchantName() : "RecoverAI");

        payload.put("content", List.of(Map.of("type", "text/plain", "value", textContent)));
        return payload;
    }

    @Override
    public ProviderHealthResult checkHealth() {
        String apiKey = properties.getEmail().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return ProviderHealthResult.misconfigured(PROVIDER_NAME, "EMAIL", "SendGrid API key missing");
        }
        return ProviderHealthResult.available(PROVIDER_NAME, "EMAIL", "SendGrid Email adapter configured");
    }

    @Override
    public String getProviderIdentifier() {
        return "sendgrid";
    }

    @Override
    public String getProviderCategory() {
        return "EMAIL";
    }
}
