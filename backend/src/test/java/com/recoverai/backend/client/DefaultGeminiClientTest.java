package com.recoverai.backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.config.GeminiProperties;
import com.recoverai.backend.dto.diagnosis.DiagnosisContext;
import com.recoverai.backend.dto.diagnosis.StructuredDiagnosisResponse;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.exception.DiagnosisValidationException;
import com.recoverai.backend.exception.GeminiApiException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DefaultGeminiClientTest {

    private GeminiProperties properties;
    private ObjectMapper objectMapper;
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private DefaultGeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties("test-secret-api-key", "gemini-3.7-flash", "https://generativelanguage.googleapis.com/v1beta", 5000, 15000);
        objectMapper = new ObjectMapper();
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        geminiClient = new DefaultGeminiClient(properties, restClientBuilder.build(), objectMapper);
    }

    private DiagnosisContext sampleContext() {
        return DiagnosisContext.builder()
                .recoveryCaseId(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .merchantName("Acme Corp")
                .paymentId(UUID.randomUUID())
                .razorpayPaymentId("pay_test_123")
                .amount(new BigDecimal("1499.00"))
                .currency("INR")
                .paymentMethod("card")
                .paymentStatus("FAILED")
                .errorCode("BAD_REQUEST_ERROR")
                .errorDescription("Payment failed due to insufficient funds")
                .errorReason("payment_failed")
                .errorSource("customer")
                .riskLevel("NORMAL")
                .failureReasonCategory("INSUFFICIENT_FUNDS")
                .estimatedRecoverableAmount(new BigDecimal("1499.00"))
                .recoveryPriority("HIGH")
                .recoveryCaseStatus("OPEN")
                .customerIdentifier("a***@example.com")
                .build();
    }

    @Test
    @DisplayName("Should successfully parse valid structured diagnosis response from Gemini")
    void shouldSuccessfullyDiagnoseFailedPayment() {
        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"recommendedAction\\": \\"SEND_PAYMENT_LINK\\", \\"recommendedChannel\\": \\"WHATSAPP\\", \\"confidenceScore\\": 0.8850, \\"reasoning\\": \\"Customer has insufficient funds, WhatsApp payment link provides frictionless retry.\\", \\"decisionFactors\\": {\\"urgency\\": \\"HIGH\\", \\"method\\": \\"UPI\\"}}"
                          }
                        ]
                      },
                      "finishReason": "STOP"
                    }
                  ],
                  "usageMetadata": {
                    "promptTokenCount": 240,
                    "candidatesTokenCount": 65,
                    "totalTokenCount": 305
                  },
                  "modelVersion": "gemini-3.7-flash-001"
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=test-secret-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        StructuredDiagnosisResponse response = geminiClient.diagnose(sampleContext());

        assertThat(response).isNotNull();
        assertThat(response.getRecommendedAction()).isEqualTo("SEND_PAYMENT_LINK");
        assertThat(response.getRecommendedChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
        assertThat(response.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.8850"));
        assertThat(response.getReasoning()).contains("frictionless retry");
        assertThat(response.getModelName()).isEqualTo("gemini-3.7-flash");
        assertThat(response.getModelVersion()).isEqualTo("gemini-3.7-flash-001");
        assertThat(response.getPromptTokens()).isEqualTo(240);
        assertThat(response.getCompletionTokens()).isEqualTo(65);
        assertThat(response.getDecisionFactors()).contains("urgency");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should handle JSON response wrapped in markdown code blocks")
    void shouldHandleMarkdownFencedJson() {
        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n{\\"recommendedAction\\": \\"RETRY_CHARGE\\", \\"recommendedChannel\\": \\"RETRY_CHARGE\\", \\"confidenceScore\\": 0.7500, \\"reasoning\\": \\"Transient network error during charge.\\"}\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=test-secret-api-key"))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        StructuredDiagnosisResponse response = geminiClient.diagnose(sampleContext());

        assertThat(response.getRecommendedAction()).isEqualTo("RETRY_CHARGE");
        assertThat(response.getRecommendedChannel()).isEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(response.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.7500"));
    }

    @Test
    @DisplayName("Should throw GeminiApiException when API key is missing")
    void shouldThrowWhenApiKeyMissing() {
        properties.setApiKey("");

        assertThatThrownBy(() -> geminiClient.diagnose(sampleContext()))
                .isInstanceOf(GeminiApiException.class)
                .hasMessageContaining("Gemini API key is not configured");
    }

    @Test
    @DisplayName("Should throw DiagnosisValidationException when context is null")
    void shouldThrowWhenContextIsNull() {
        assertThatThrownBy(() -> geminiClient.diagnose(null))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("Diagnosis context cannot be null");
    }

    @Test
    @DisplayName("Should throw DiagnosisValidationException when confidence score is out of range")
    void shouldThrowWhenConfidenceScoreOutOfRange() {
        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"recommendedAction\\": \\"SEND_LINK\\", \\"recommendedChannel\\": \\"EMAIL\\", \\"confidenceScore\\": 1.5, \\"reasoning\\": \\"Score is too high\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=test-secret-api-key"))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiClient.diagnose(sampleContext()))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("Confidence score out of bounds");
    }

    @Test
    @DisplayName("Should throw DiagnosisValidationException when required action is missing")
    void shouldThrowWhenRecommendedActionMissing() {
        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"confidenceScore\\": 0.8, \\"reasoning\\": \\"Missing action\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=test-secret-api-key"))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiClient.diagnose(sampleContext()))
                .isInstanceOf(DiagnosisValidationException.class)
                .hasMessageContaining("Missing recommendedAction");
    }

    @Test
    @DisplayName("Should fallback unknown channel to MANUAL")
    void shouldFallbackUnknownChannelToManual() {
        String geminiResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"recommendedAction\\": \\"CALL_USER\\", \\"recommendedChannel\\": \\"CARRIER_PIGEON\\", \\"confidenceScore\\": 0.5, \\"reasoning\\": \\"Unrecognized channel\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=test-secret-api-key"))
                .andRespond(withSuccess(geminiResponseBody, MediaType.APPLICATION_JSON));

        StructuredDiagnosisResponse response = geminiClient.diagnose(sampleContext());
        assertThat(response.getRecommendedChannel()).isEqualTo(RecoveryChannel.MANUAL);
    }

    @Test
    @DisplayName("Should handle Gemini HTTP 429 Rate Limit error cleanly")
    void shouldHandleGeminiRateLimitError() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=test-secret-api-key"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> geminiClient.diagnose(sampleContext()))
                .isInstanceOf(GeminiApiException.class)
                .satisfies(ex -> {
                    GeminiApiException gEx = (GeminiApiException) ex;
                    assertThat(gEx.getStatusCode()).isEqualTo(429);
                    assertThat(gEx.getMessage()).doesNotContain("test-secret-api-key");
                });
    }

    @Test
    @DisplayName("Should handle Gemini HTTP 500 Internal Server Error cleanly")
    void shouldHandleGemini500Error() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=test-secret-api-key"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> geminiClient.diagnose(sampleContext()))
                .isInstanceOf(GeminiApiException.class)
                .satisfies(ex -> {
                    GeminiApiException gEx = (GeminiApiException) ex;
                    assertThat(gEx.getStatusCode()).isEqualTo(500);
                    assertThat(gEx.getMessage()).doesNotContain("test-secret-api-key");
                });
    }
}
