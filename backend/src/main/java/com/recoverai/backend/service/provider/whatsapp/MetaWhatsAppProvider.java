package com.recoverai.backend.service.provider.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.WhatsAppProvider;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.WhatsAppMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
import com.recoverai.backend.service.provider.http.ProviderHttpClientFactory;
import com.recoverai.backend.service.provider.util.CredentialMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Component("metaWhatsAppProvider")
public class MetaWhatsAppProvider implements WhatsAppProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppProvider.class);
    public static final String PROVIDER_NAME = "META_WHATSAPP";

    private final RecoveryCommunicationProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public MetaWhatsAppProvider(RecoveryCommunicationProperties properties,
                                ProviderHttpClientFactory clientFactory,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        String baseUrl = properties.getWhatsapp().getApiBaseUrl();
        if (baseUrl == null || baseUrl.isBlank() || baseUrl.contains("twilio")) {
            baseUrl = "https://graph.facebook.com";
        }
        this.restClient = clientFactory != null ? clientFactory.createClient(baseUrl) : null;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public MetaWhatsAppProvider(RecoveryCommunicationProperties properties,
                                RestClient restClient,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public CommunicationDeliveryResult sendWhatsApp(WhatsAppMessageRequest request) {
        if (request == null) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "INVALID_REQUEST",
                    "Request cannot be null", null, ProviderFailureType.VALIDATION);
        }

        String recipientPhone = request.getRecipientPhone();
        if (recipientPhone == null || recipientPhone.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "MISSING_RECIPIENT_PHONE",
                    "Recipient phone number is required", null, ProviderFailureType.VALIDATION);
        }

        String apiKey = properties.getWhatsapp().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "PROVIDER_MISCONFIGURED",
                    "Meta WhatsApp API Key is missing", null, ProviderFailureType.AUTHENTICATION);
        }

        String senderNumber = properties.getWhatsapp().getSenderNumber();
        String phoneNumberId = (senderNumber != null && !senderNumber.isBlank()) ? senderNumber.replaceAll("[^0-9]", "") : "current";

        String cleanPhone = recipientPhone.replaceAll("[^0-9]", "");
        String body = buildMessageBody(request);

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", cleanPhone);
        payload.put("type", "text");
        payload.put("text", Map.of("preview_url", true, "body", body));

        String path = String.format("/v18.0/%s/messages", phoneNumberId);

        log.info("[META_WHATSAPP] Dispatching Cloud API message to recipient={}", CredentialMasker.maskPhone(recipientPhone));

        try {
            String responseStr = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            String messageId = null;
            if (responseStr != null && !responseStr.isBlank()) {
                JsonNode root = objectMapper.readTree(responseStr);
                if (root.has("messages") && root.get("messages").isArray() && root.get("messages").size() > 0) {
                    JsonNode firstMsg = root.get("messages").get(0);
                    if (firstMsg.hasNonNull("id")) {
                        messageId = firstMsg.get("id").asText();
                    }
                }
            }

            if (messageId == null || messageId.isBlank()) {
                messageId = "wamid_" + System.currentTimeMillis();
            }

            String metadata = String.format("{\"provider\":\"%s\",\"deliveryId\":\"%s\"}",
                    PROVIDER_NAME, messageId);

            return CommunicationDeliveryResult.success(
                    messageId,
                    PROVIDER_NAME,
                    "WHATSAPP_DISPATCHED",
                    "Message accepted by Meta WhatsApp Cloud API",
                    metadata
            );

        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            ProviderFailureType failureType = ProviderErrorClassifier.classify(statusCode, responseBody, ex);

            String errorMessage = "Meta WhatsApp API request failed with HTTP " + statusCode;
            try {
                if (responseBody != null && !responseBody.isBlank()) {
                    JsonNode root = objectMapper.readTree(responseBody);
                    if (root.has("error") && root.get("error").hasNonNull("message")) {
                        errorMessage = root.get("error").get("message").asText();
                    }
                }
            } catch (Exception ignored) {
            }

            log.warn("[META_WHATSAPP] API error: status={}, failureType={}, error={}",
                    statusCode, failureType, errorMessage);

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "META_ERROR_" + statusCode,
                    errorMessage,
                    String.format("{\"provider\":\"%s\",\"httpStatus\":%d}", PROVIDER_NAME, statusCode),
                    failureType
            );

        } catch (Exception ex) {
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            log.error("[META_WHATSAPP] Communication failure: {}", ex.getMessage());

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    failureType == ProviderFailureType.TIMEOUT ? "WHATSAPP_TIMEOUT" : "WHATSAPP_DISPATCH_ERROR",
                    ex.getMessage(),
                    null,
                    failureType
            );
        }
    }

    private String buildMessageBody(WhatsAppMessageRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(request.getCustomerName() != null ? request.getCustomerName() : "there")
                .append(", payment of ")
                .append(request.getAmount() != null ? request.getAmount() : "")
                .append(" ")
                .append(request.getCurrency() != null ? request.getCurrency() : "INR")
                .append(" for ").append(request.getMerchantName() != null ? request.getMerchantName() : "your order")
                .append(" could not be completed.");

        if (request.getRecoveryLink() != null && !request.getRecoveryLink().isBlank()) {
            sb.append(" Recover here: ").append(request.getRecoveryLink());
        }
        return sb.toString();
    }

    @Override
    public ProviderHealthResult checkHealth() {
        String apiKey = properties.getWhatsapp().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return ProviderHealthResult.misconfigured(PROVIDER_NAME, "WHATSAPP", "Meta API key missing");
        }
        return ProviderHealthResult.available(PROVIDER_NAME, "WHATSAPP", "Meta WhatsApp adapter configured");
    }

    @Override
    public String getProviderIdentifier() {
        return "meta";
    }

    @Override
    public String getProviderCategory() {
        return "WHATSAPP";
    }
}
