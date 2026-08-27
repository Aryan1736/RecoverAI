package com.recoverai.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.client.dto.GeminiRequestDto;
import com.recoverai.backend.client.dto.GeminiResponseDto;
import com.recoverai.backend.config.GeminiProperties;
import com.recoverai.backend.dto.diagnosis.DiagnosisContext;
import com.recoverai.backend.dto.diagnosis.StructuredDiagnosisResponse;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.exception.DiagnosisValidationException;
import com.recoverai.backend.exception.GeminiApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class DefaultGeminiClient implements GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultGeminiClient.class);

    private final GeminiProperties geminiProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public DefaultGeminiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(geminiProperties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(geminiProperties.getReadTimeoutMs()));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public DefaultGeminiClient(GeminiProperties geminiProperties, RestClient restClient, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public StructuredDiagnosisResponse diagnose(DiagnosisContext context) {
        if (context == null) {
            throw new DiagnosisValidationException("Diagnosis context cannot be null");
        }

        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("Gemini API key is missing or unconfigured");
            throw new GeminiApiException("Gemini API key is not configured");
        }

        String model = geminiProperties.getModel() != null && !geminiProperties.getModel().trim().isEmpty()
                ? geminiProperties.getModel().trim()
                : "gemini-3.7-flash";

        String baseUrl = geminiProperties.getBaseUrl() != null && !geminiProperties.getBaseUrl().trim().isEmpty()
                ? geminiProperties.getBaseUrl().trim()
                : "https://generativelanguage.googleapis.com/v1beta";

        String prompt = buildPrompt(context);
        GeminiRequestDto requestDto = GeminiRequestDto.fromPrompt(prompt);

        String uri = String.format("%s/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        log.debug("Dispatching AI diagnosis request to Gemini model: {}", model);

        GeminiResponseDto responseDto;
        try {
            responseDto = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestDto)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int code = response.getStatusCode().value();
                        log.warn("Gemini API returned error status: {}", code);
                        throw new GeminiApiException("Gemini API returned HTTP status " + code, code);
                    })
                    .body(GeminiResponseDto.class);
        } catch (RestClientResponseException e) {
            log.error("Gemini HTTP error status={}", e.getStatusCode().value());
            throw new GeminiApiException("Gemini API call failed with status: " + e.getStatusCode().value(), e.getStatusCode().value(), e);
        } catch (GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini communication failure: {}", e.getMessage());
            throw new GeminiApiException("Failed to communicate with Gemini API: " + e.getMessage(), e);
        }

        if (responseDto == null) {
            throw new GeminiApiException("Received empty response from Gemini API");
        }

        return parseAndValidateGeminiResponse(responseDto, model);
    }

    private String buildPrompt(DiagnosisContext context) {
        return """
                You are RecoverAI's intelligent payment failure diagnosis engine.
                Analyze the following payment failure context and generate an optimal recovery recommendation in structured JSON format.

                PAYMENT AND RECOVERY CONTEXT:
                - Payment Amount: %s %s
                - Payment Method: %s
                - Payment Status: %s
                - Error Code: %s
                - Error Description: %s
                - Error Reason: %s
                - Error Source: %s
                - Risk Level: %s
                - Failure Reason Category: %s
                - Estimated Recoverable Amount: %s %s
                - Priority: %s
                - Recovery Status: %s
                - Customer Ref: %s

                INSTRUCTIONS:
                1. Determine the root cause of the payment failure.
                2. Select the optimal recovery action (e.g., 'RETRY_CHARGE', 'SEND_PAYMENT_LINK', 'SWITCH_PAYMENT_METHOD', 'CUSTOMER_SUPPORT_OUTREACH', 'COOLDOWN_AND_RETRY', 'MANUAL_INTERVENTION').
                3. Choose the most effective channel from exactly: ['WHATSAPP', 'EMAIL', 'SMS', 'RETRY_CHARGE', 'SMART_LINK', 'MANUAL'].
                4. Provide a confidence score between 0.0000 and 1.0000.
                5. Provide concise reasoning and structured decision factors.

                You MUST return ONLY a JSON object with this exact schema:
                {
                  "recommendedAction": "STRING",
                  "recommendedChannel": "WHATSAPP | EMAIL | SMS | RETRY_CHARGE | SMART_LINK | MANUAL",
                  "confidenceScore": 0.8500,
                  "reasoning": "STRING",
                  "decisionFactors": { "primaryReason": "STRING", "retryViability": "HIGH|MEDIUM|LOW", "urgency": "HIGH|MEDIUM|LOW" }
                }
                """.formatted(
                context.getAmount() != null ? context.getAmount().toPlainString() : "UNKNOWN",
                context.getCurrency() != null ? context.getCurrency() : "INR",
                context.getPaymentMethod() != null ? context.getPaymentMethod() : "UNKNOWN",
                context.getPaymentStatus() != null ? context.getPaymentStatus() : "FAILED",
                context.getErrorCode() != null ? context.getErrorCode() : "UNKNOWN",
                context.getErrorDescription() != null ? context.getErrorDescription() : "None provided",
                context.getErrorReason() != null ? context.getErrorReason() : "UNKNOWN",
                context.getErrorSource() != null ? context.getErrorSource() : "UNKNOWN",
                context.getRiskLevel() != null ? context.getRiskLevel() : "UNKNOWN",
                context.getFailureReasonCategory() != null ? context.getFailureReasonCategory() : "UNKNOWN",
                context.getEstimatedRecoverableAmount() != null ? context.getEstimatedRecoverableAmount().toPlainString() : "0",
                context.getCurrency() != null ? context.getCurrency() : "INR",
                context.getRecoveryPriority() != null ? context.getRecoveryPriority() : "MEDIUM",
                context.getRecoveryCaseStatus() != null ? context.getRecoveryCaseStatus() : "OPEN",
                context.getCustomerIdentifier() != null ? context.getCustomerIdentifier() : "ANONYMOUS"
        );
    }

    private StructuredDiagnosisResponse parseAndValidateGeminiResponse(GeminiResponseDto responseDto, String defaultModel) {
        String candidateText = responseDto.extractFirstCandidateText();
        if (candidateText == null || candidateText.trim().isEmpty()) {
            throw new DiagnosisValidationException("Gemini response contained no candidate text");
        }

        try {
            // Clean markdown fences if model returned them
            String jsonText = candidateText.trim();
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7);
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3);
            }
            if (jsonText.endsWith("```")) {
                jsonText = jsonText.substring(0, jsonText.length() - 3);
            }
            jsonText = jsonText.trim();

            JsonNode rootNode = objectMapper.readTree(jsonText);

            String recommendedAction = rootNode.hasNonNull("recommendedAction")
                    ? rootNode.get("recommendedAction").asText().trim()
                    : null;

            if (recommendedAction == null || recommendedAction.isEmpty()) {
                throw new DiagnosisValidationException("Missing recommendedAction in Gemini response");
            }

            String channelStr = rootNode.hasNonNull("recommendedChannel")
                    ? rootNode.get("recommendedChannel").asText().trim().toUpperCase()
                    : null;

            RecoveryChannel channel = null;
            if (channelStr != null) {
                try {
                    channel = RecoveryChannel.valueOf(channelStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown recovery channel '{}' from Gemini, defaulting to MANUAL", channelStr);
                    channel = RecoveryChannel.MANUAL;
                }
            }

            if (!rootNode.hasNonNull("confidenceScore")) {
                throw new DiagnosisValidationException("Missing confidenceScore in Gemini response");
            }

            BigDecimal confidenceScore = new BigDecimal(rootNode.get("confidenceScore").asText());
            if (confidenceScore.compareTo(BigDecimal.ZERO) < 0 || confidenceScore.compareTo(BigDecimal.ONE) > 0) {
                throw new DiagnosisValidationException("Confidence score out of bounds [0.0, 1.0]: " + confidenceScore);
            }

            String reasoning = rootNode.hasNonNull("reasoning")
                    ? rootNode.get("reasoning").asText().trim()
                    : null;

            if (reasoning == null || reasoning.isEmpty()) {
                throw new DiagnosisValidationException("Missing reasoning in Gemini response");
            }

            String decisionFactors = null;
            if (rootNode.hasNonNull("decisionFactors")) {
                JsonNode factorsNode = rootNode.get("decisionFactors");
                if (factorsNode.isObject() || factorsNode.isArray()) {
                    decisionFactors = objectMapper.writeValueAsString(factorsNode);
                } else {
                    decisionFactors = factorsNode.asText();
                }
            }

            Integer promptTokens = null;
            Integer completionTokens = null;
            if (responseDto.getUsageMetadata() != null) {
                promptTokens = responseDto.getUsageMetadata().getPromptTokenCount();
                completionTokens = responseDto.getUsageMetadata().getCandidatesTokenCount();
            }

            String modelVersion = responseDto.getModelVersion() != null ? responseDto.getModelVersion() : defaultModel;

            return StructuredDiagnosisResponse.builder()
                    .recommendedAction(recommendedAction)
                    .recommendedChannel(channel)
                    .confidenceScore(confidenceScore)
                    .reasoning(reasoning)
                    .decisionFactors(decisionFactors)
                    .modelName(defaultModel)
                    .modelVersion(modelVersion)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .rawResponse(jsonText)
                    .build();

        } catch (DiagnosisValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini diagnosis response: {}", e.getMessage());
            throw new DiagnosisValidationException("Malformed Gemini response: " + e.getMessage(), e);
        }
    }
}
