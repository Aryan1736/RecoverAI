package com.recoverai.backend.service.provider.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.service.provider.SmsProvider;
import com.recoverai.backend.service.provider.classification.ProviderErrorClassifier;
import com.recoverai.backend.service.provider.classification.ProviderFailureType;
import com.recoverai.backend.service.provider.dto.CommunicationDeliveryResult;
import com.recoverai.backend.service.provider.dto.SmsMessageRequest;
import com.recoverai.backend.service.provider.health.ProviderHealthCheck;
import com.recoverai.backend.service.provider.health.ProviderHealthResult;
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

@Component("twilioSmsProvider")
public class TwilioSmsProvider implements SmsProvider, ProviderHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);
    public static final String PROVIDER_NAME = "TWILIO_SMS";

    private final RecoveryCommunicationProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public TwilioSmsProvider(RecoveryCommunicationProperties properties,
                             ProviderHttpClientFactory clientFactory,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = clientFactory != null ? clientFactory.createClient(properties.getSms().getApiBaseUrl()) : null;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public TwilioSmsProvider(RecoveryCommunicationProperties properties,
                             RestClient restClient,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public CommunicationDeliveryResult sendSms(SmsMessageRequest request) {
        if (request == null) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "INVALID_REQUEST",
                    "Request cannot be null", null, ProviderFailureType.VALIDATION);
        }

        String recipientPhone = request.getRecipientPhone();
        if (recipientPhone == null || recipientPhone.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "MISSING_RECIPIENT_PHONE",
                    "Recipient phone number is required", null, ProviderFailureType.VALIDATION);
        }

        String accountSid = properties.getSms().getAccountSid();
        String authToken = properties.getSms().getAuthToken();
        if (authToken == null || authToken.isBlank()) {
            authToken = properties.getSms().getApiKey();
        }

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            return CommunicationDeliveryResult.failure(null, PROVIDER_NAME, "PROVIDER_MISCONFIGURED",
                    "Twilio SMS Account SID or Auth Token is missing", null, ProviderFailureType.AUTHENTICATION);
        }

        String senderId = properties.getSms().getSenderId();
        if (senderId == null || senderId.isBlank()) {
            senderId = "RECOVER";
        }

        String body = buildSmsBody(request);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("To", recipientPhone);
        formData.add("From", senderId);
        formData.add("Body", body);

        String basicAuth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        String path = String.format("/2010-04-01/Accounts/%s/Messages.json", accountSid);

        log.info("[TWILIO_SMS] Dispatching SMS to recipient={}", CredentialMasker.maskPhone(recipientPhone));

        try {
            String responseStr = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            String sid = null;
            if (responseStr != null && !responseStr.isBlank()) {
                JsonNode root = objectMapper.readTree(responseStr);
                if (root.hasNonNull("sid")) {
                    sid = root.get("sid").asText();
                }
            }

            if (sid == null || sid.isBlank()) {
                sid = "tw_sms_" + System.currentTimeMillis();
            }

            String metadata = String.format("{\"provider\":\"%s\",\"deliveryId\":\"%s\"}",
                    PROVIDER_NAME, sid);

            return CommunicationDeliveryResult.success(
                    sid,
                    PROVIDER_NAME,
                    "SMS_DISPATCHED",
                    "SMS accepted by Twilio API",
                    metadata
            );

        } catch (RestClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            String responseBody = ex.getResponseBodyAsString();
            ProviderFailureType failureType = ProviderErrorClassifier.classify(statusCode, responseBody, ex);

            String errorMessage = "Twilio SMS request failed with HTTP " + statusCode;
            try {
                if (responseBody != null && !responseBody.isBlank()) {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    if (errorNode.hasNonNull("message")) {
                        errorMessage = errorNode.get("message").asText();
                    }
                }
            } catch (Exception ignored) {
            }

            log.warn("[TWILIO_SMS] API error: status={}, failureType={}, error={}",
                    statusCode, failureType, errorMessage);

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    "TWILIO_SMS_ERROR_" + statusCode,
                    errorMessage,
                    String.format("{\"provider\":\"%s\",\"httpStatus\":%d}", PROVIDER_NAME, statusCode),
                    failureType
            );

        } catch (Exception ex) {
            ProviderFailureType failureType = ProviderErrorClassifier.classifyException(ex);
            log.error("[TWILIO_SMS] Transport failure: {}", ex.getMessage());

            return CommunicationDeliveryResult.failure(
                    null,
                    PROVIDER_NAME,
                    failureType == ProviderFailureType.TIMEOUT ? "SMS_TIMEOUT" : "SMS_DISPATCH_ERROR",
                    ex.getMessage(),
                    null,
                    failureType
            );
        }
    }

    private String buildSmsBody(SmsMessageRequest request) {
        return String.format("%s: Payment of %s %s failed. Complete now: %s",
                request.getMerchantName() != null ? request.getMerchantName() : "RecoverAI",
                request.getAmount() != null ? request.getAmount() : "",
                request.getCurrency() != null ? request.getCurrency() : "INR",
                request.getRecoveryLink() != null ? request.getRecoveryLink() : "");
    }

    @Override
    public ProviderHealthResult checkHealth() {
        String accountSid = properties.getSms().getAccountSid();
        String authToken = properties.getSms().getAuthToken();
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            return ProviderHealthResult.misconfigured(PROVIDER_NAME, "SMS", "Twilio credentials missing");
        }
        return ProviderHealthResult.available(PROVIDER_NAME, "SMS", "Twilio SMS adapter configured");
    }

    @Override
    public String getProviderIdentifier() {
        return "twilio";
    }

    @Override
    public String getProviderCategory() {
        return "SMS";
    }
}
