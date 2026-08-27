package com.recoverai.backend.controller;

import com.recoverai.backend.client.GeminiClient;
import com.recoverai.backend.dto.diagnosis.DiagnosisContext;
import com.recoverai.backend.dto.diagnosis.StructuredDiagnosisResponse;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.exception.DiagnosisValidationException;
import com.recoverai.backend.exception.GeminiApiException;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AIDiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @MockBean
    private GeminiClient geminiClient;

    private Merchant merchant;
    private RecoveryCase recoveryCase;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.save(Merchant.builder()
                .name("Controller Test Merchant")
                .email("merchant_" + UUID.randomUUID() + "@test.com")
                .webhookSecret("secret-123")
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("Alice Controller")
                .email("alice_" + UUID.randomUUID() + "@example.com")
                .build());

        Payment payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_ctrl_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
                .errorCode("PAYMENT_DECLINED")
                .errorDescription("Declined by bank")
                .riskLevel(RiskLevel.LOW)
                .build());

        recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("PAYMENT_DECLINED")
                .estimatedRecoverableAmount(new BigDecimal("1500.00"))
                .currency("INR")
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/diagnose with X-Merchant-Id header should return 200 OK")
    void shouldDiagnoseWithHeader() throws Exception {
        StructuredDiagnosisResponse diagnosisResponse = StructuredDiagnosisResponse.builder()
                .recommendedAction("SEND_PAYMENT_LINK")
                .recommendedChannel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.9000"))
                .reasoning("High recovery probability via WhatsApp payment link")
                .modelName("gemini-3.7-flash")
                .modelVersion("gemini-3.7-flash-001")
                .promptTokens(180)
                .completionTokens(50)
                .decisionFactors("{\"urgency\":\"HIGH\"}")
                .rawResponse("{\"recommendedAction\":\"SEND_PAYMENT_LINK\"}")
                .build();

        when(geminiClient.diagnose(any(DiagnosisContext.class))).thenReturn(diagnosisResponse);

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", recoveryCase.getId())
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recommendedAction").value("SEND_PAYMENT_LINK"))
                .andExpect(jsonPath("$.channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.confidenceScore").value(0.9000))
                .andExpect(jsonPath("$.reasoning").value("High recovery probability via WhatsApp payment link"))
                .andExpect(jsonPath("$.modelName").value("gemini-3.7-flash"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/{merchantId}/recovery-cases/{id}/diagnose with path variables should return 200 OK")
    void shouldDiagnoseWithPath() throws Exception {
        StructuredDiagnosisResponse diagnosisResponse = StructuredDiagnosisResponse.builder()
                .recommendedAction("RETRY_CHARGE")
                .recommendedChannel(RecoveryChannel.RETRY_CHARGE)
                .confidenceScore(new BigDecimal("0.8500"))
                .reasoning("Retry after 15 minutes cooldown")
                .modelName("gemini-3.7-flash")
                .modelVersion("gemini-3.7-flash-001")
                .promptTokens(120)
                .completionTokens(30)
                .decisionFactors("{\"method\":\"UPI\"}")
                .rawResponse("{\"recommendedAction\":\"RETRY_CHARGE\"}")
                .build();

        when(geminiClient.diagnose(any(DiagnosisContext.class))).thenReturn(diagnosisResponse);

        mockMvc.perform(post("/api/v1/merchants/{merchantId}/recovery-cases/{id}/diagnose", merchant.getId(), recoveryCase.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recommendedAction").value("RETRY_CHARGE"))
                .andExpect(jsonPath("$.channel").value("RETRY_CHARGE"));
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/diagnose without header should return 400 Bad Request")
    void shouldReturn400WhenHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", recoveryCase.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/diagnose should return 404 when case not found")
    void shouldReturn404WhenCaseNotFound() throws Exception {
        UUID nonExistentCaseId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", nonExistentCaseId)
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/diagnose should return 400 on diagnosis validation failure")
    void shouldReturn400OnValidationFailure() throws Exception {
        when(geminiClient.diagnose(any(DiagnosisContext.class)))
                .thenThrow(new DiagnosisValidationException("Confidence score out of bounds"));

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", recoveryCase.getId())
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Confidence score out of bounds"));
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/diagnose should return 502 Bad Gateway on Gemini API failure")
    void shouldReturn502OnGeminiFailure() throws Exception {
        when(geminiClient.diagnose(any(DiagnosisContext.class)))
                .thenThrow(new GeminiApiException("Gemini API returned HTTP status 503", 503));

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/diagnose", recoveryCase.getId())
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("AI diagnosis service failure: Gemini API returned HTTP status 503"));
    }
}
