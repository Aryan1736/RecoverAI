package com.recoverai.backend.controller;

import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecoveryOrchestrationControllerTest {

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

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    private Merchant merchant;
    private RecoveryCase recoveryCase;
    private AgentDecision agentDecision;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.save(Merchant.builder()
                .name("Orchestration Test Merchant")
                .email("merchant_" + UUID.randomUUID() + "@test.com")
                .webhookSecret("secret-456")
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("Bob Test")
                .email("bob_" + UUID.randomUUID() + "@example.com")
                .build());

        Payment payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_orch_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("3500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .errorCode("INSUFFICIENT_FUNDS")
                .errorDescription("Not enough balance")
                .riskLevel(RiskLevel.LOW)
                .build());

        recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("INSUFFICIENT_FUNDS")
                .estimatedRecoverableAmount(new BigDecimal("3500.00"))
                .currency("INR")
                .build());

        agentDecision = agentDecisionRepository.save(AgentDecision.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .recommendedAction("WHATSAPP_SMART_LINK")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.9100"))
                .reasoning("High response rate via WhatsApp payment link with fallback discount")
                .modelName("gemini-3.7-flash")
                .modelVersion("v1")
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/orchestrate with X-Merchant-Id header should return 200 OK")
    void shouldOrchestrateWithHeader() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", recoveryCase.getId())
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recoveryCaseId").value(recoveryCase.getId().toString()))
                .andExpect(jsonPath("$.merchantId").value(merchant.getId().toString()))
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.resultCode").value("WHATSAPP_DISPATCHED"))
                .andExpect(jsonPath("$.recoveryLink").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/merchants/{merchantId}/recovery-cases/{id}/orchestrate with path variable should return 200 OK")
    void shouldOrchestrateWithPath() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/{merchantId}/recovery-cases/{id}/orchestrate", merchant.getId(), recoveryCase.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recoveryCaseId").value(recoveryCase.getId().toString()))
                .andExpect(jsonPath("$.merchantId").value(merchant.getId().toString()))
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andExpect(jsonPath("$.channel").value("WHATSAPP"));
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/orchestrate without header should return 400 Bad Request")
    void shouldReturn400WhenHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", recoveryCase.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/orchestrate with non-existent ID should return 404 Not Found")
    void shouldReturn404WhenCaseNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", nonExistentId)
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/orchestrate should return 404 when no AgentDecision exists")
    void shouldReturn404WhenAgentDecisionMissing() throws Exception {
        // Create another payment and case without AgentDecision
        Payment payment2 = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(recoveryCase.getCustomer())
                .razorpayPaymentId("pay_orch2_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
                .errorCode("BAD_REQUEST")
                .errorDescription("Failed payment")
                .riskLevel(RiskLevel.LOW)
                .build());

        RecoveryCase caseWithoutDecision = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment2)
                .customer(recoveryCase.getCustomer())
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.LOW)
                .estimatedRecoverableAmount(new BigDecimal("1000.00"))
                .currency("INR")
                .build());


        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", caseWithoutDecision.getId())
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No AgentDecision found for recovery case: " + caseWithoutDecision.getId() + ". Run AI diagnosis first."));
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/orchestrate should return 400 when case is already RECOVERED")
    void shouldReturn400WhenCaseIsTerminal() throws Exception {
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
        recoveryCaseRepository.save(recoveryCase);

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", recoveryCase.getId())
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot orchestrate recovery for case in terminal status: RECOVERED"));
    }

    @Test
    @DisplayName("POST /api/v1/recovery-cases/{id}/orchestrate should return 409 Conflict when duplicate active attempt exists")
    void shouldReturn409WhenActiveAttemptExists() throws Exception {
        recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .build());

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", recoveryCase.getId())
                        .header("X-Merchant-Id", merchant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An active recovery attempt is already scheduled or in-flight for case: " + recoveryCase.getId()));
    }
}
