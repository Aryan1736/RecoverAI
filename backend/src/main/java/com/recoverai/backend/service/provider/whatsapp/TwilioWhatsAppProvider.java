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
import com.recoverai.backend.service.provider.health.ProviderHealthStatus;
import com.recoverai.backend.service.provider.http.ProviderHttpClientFactory;
import com.recoverai.backend.service.provider.util.CredentialMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component("twilioWhatsAppProvider")
public class TwilioWhatsAppProvider implements WhatsAppProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppProvider.class);
    public static final String PROVIDER_NAME = "TWILIO_WHATSAPP";

    private final RecoveryCommunicationProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public TwilioWhatsAppProvider(RecoveryCommunicationProperties properties,
                                  ProviderHttpClientFactory clientFactory,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = clientFactory != null ? clientFactory.createClient(properties.getWhatsapp().getApiBaseUrl()) : null;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public TwilioWhatsAppProvider(RecoveryCommunicationProperties properties,
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

        String accountSid = properties.getWhatsapp().getAccountSid();
        String authToken = properties.getWhatsapp().getAuthToken();
        if (authToken == null || authToken.isBlank()) {
            authToken = properties.getWhatsapp().getApiKey();
        }

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "PROVIDER_MISCONFIGURED",
                    "Twilio WhatsApp account SID or Auth Token is missing", null, ProviderFailureType.AUTHENTICATION);
        }

        String senderNumber = properties.getWhatsapp().getSenderNumber();
        if (senderNumber == null || senderNumber.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "MISSING_SENDER_NUMBER",
                    "Twilio sender number is not configured", null, ProviderFailureType.VALIDATION);
        }

        String toFormatted = recipientPhone.startsWith("whatsapp:") ? recipientPhone : "whatsapp:" + recipientPhone;
        String fromFormatted = senderNumber.startsWith("whatsapp:") ? senderNumber : "whatsapp:" + senderNumber;

        String body = buildMessageBody(request);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("To", toFormatted);
        formData.add("From", fromFormatted);
        formData.add("Body", body);

        String basicAuth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        String path = String.format("/2010-04-01/Accounts/%s/Messages.json", accountSid);

        log.info("[TWILIO_WHATSAPP] Dispatching message to recipient={}", CredentialMasker.maskPhone(recipientPhone));

        try {
            String responseStr = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            String sid = null;
            String status = "queued";
            if (responseStr != null && !responseStr.isBlank()) {
                JsonNode root = objectMapper.readTree(responseStr);
                if (root.hasNonNull("sid")) {
                    sid = root.get("sid").asText();
                }
                if (root.hasNonNull("status")) {
                    status = root.get("status").asText();
                }
            }

            if (sid == null || sid.isBlank()) {
                sid = "tw_wa_" + System.currentTimeMillis();
            }

            String metadata = String.format("{\"provider\":\"%s\",\"deliveryId\":\"%s\",\"status\":\"%s\"}",
                    PROVIDER_NAME, sid, status);

            return CommunicationDeliveryResult.success(
                    sid,
                    PROVIDER_NAME,
                    "WHATSAPP_DISPATCHED",
                    "Message accepted by Twilio WhatsApp API",
                    metadata
            );

        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            ProviderFailureType failureType = ProviderErrorClassifier.classify(statusCode, responseBody, ex);

            String errorMessage = "Twilio WhatsApp request failed with HTTP " + statusCode;
            String twilioErrorCode = null;
            try {
                if (responseBody != null && !responseBody.isBlank()) {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    if (errorNode.hasNonNull("message")) {
                        errorMessage = errorNode.get("message").asText();
                    }
                    if (errorNode.hasNonNull("code")) {
                        twilioErrorCode = errorNode.get("code").asText();
                    }
                }
            } catch (Exception ignored) {
            }

            log.warn("[TWILIO_WHATSAPP] API error: status={}, failureType={}, twilioCode={}",
                    statusCode, failureType, twilioErrorCode);

            String metadata = String.format("{\"provider\":\"%s\",\"httpStatus\":%d,\"twilioErrorCode\":%s}",
                    PROVIDER_NAME, statusCode, twilioErrorCode != null ? "\"" + twilioErrorCode + "\"" : "null");

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "TWILIO_ERROR_" + statusCode,
                    errorMessage,
                    metadata,
                    failureType
            );

        } catch (Exception ex) {
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            log.error("[TWILIO_WHATSAPP] Network / communication failure: {}", ex.getMessage());

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
        sb.append("Hello ").append(request.getCustomerName() != null ? request.getCustomerName() : "Valued Customer")
                .append(", your payment of ")
                .append(request.getAmount() != null ? request.getAmount() : "")
                .append(" ")
                .append(request.getCurrency() != null ? request.getCurrency() : "INR")
                .append(" to ")
                .append(request.getMerchantName() != null ? request.getMerchantName() : "us")
                .append(" could not be processed.");

        if (request.getRecoveryLink() != null && !request.getRecoveryLink().isBlank()) {
            sb.append(" Please complete your payment securely here: ").append(request.getRecoveryLink());
        }
        return sb.toString();
    }

    @Override
    public ProviderHealthResult checkHealth() {
        String accountSid = properties.getWhatsapp().getAccountSid();
        String authToken = properties.getWhatsapp().getAuthToken();
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            return ProviderHealthResult.misconfigured(PROVIDER_NAME, "WHATSAPP", "Twilio credentials missing");
        }
        return ProviderHealthResult.available(PROVIDER_NAME, "WHATSAPP", "Twilio WhatsApp adapter configured");
    }

    @Override
    public String getProviderIdentifier() {
        return "twilio";
    }

    @Override
    public String getProviderCategory() {
        return "WHATSAPP";
    }
}
