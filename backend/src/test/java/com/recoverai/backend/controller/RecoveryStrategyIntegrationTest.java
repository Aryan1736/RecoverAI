package com.recoverai.backend.controller;

import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryStrategyRepository;
import com.recoverai.backend.security.JwtTokenProvider;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecoveryStrategyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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

    @Autowired
    private RecoveryStrategyRepository recoveryStrategyRepository;

    private Merchant merchant;
    private Merchant otherMerchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Strategy Integration Merchant")
                .email("strat_int_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .webhookSecret("whsec_strat")
                .build());

        otherMerchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Other Merchant")
                .email("other_int_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .webhookSecret("whsec_other")
                .build());

        token = jwtTokenProvider.generateToken(merchant);
        otherToken = jwtTokenProvider.generateToken(otherMerchant);

        customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .name("Bruce Wayne")
                .email("bruce@wayne-enterprises.test")
                .phone("+919876543210")
                .build());

        payment = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_int_" + UUID.randomUUID())
                .amount(new BigDecimal("4999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .build());

        recoveryCase = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("insufficient_funds")
                .estimatedRecoverableAmount(new BigDecimal("4999.00"))
                .currency("INR")
                .build());
    }

    @Test
    @DisplayName("Complete Diagnosis -> Strategy -> Orchestration flow")
    void testDiagnosisToStrategyToOrchestrationFlow() throws Exception {
        // 1. Seed Agent Decision
        AgentDecision decision = agentDecisionRepository.saveAndFlush(AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("RETRY_CHARGE")
                .channel(RecoveryChannel.RETRY_CHARGE)
                .confidenceScore(new BigDecimal("0.9000"))
                .reasoning("High probability of funds restored")
                .modelName("gemini-3.7-flash")
                .build());

        // 2. Generate Strategy via API
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("RETRY_CHARGE"))
                .andExpect(jsonPath("$.terminal").value(false));

        // 3. Orchestrate Recovery Case
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("RETRY_CHARGE"))
                .andExpect(jsonPath("$.status").isNotEmpty());

        // Verify strategy was persisted
        List<RecoveryStrategy> strategies = recoveryStrategyRepository
                .findByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(recoveryCase.getId(), merchant.getId());
        assertThat(strategies).isNotEmpty();
    }

    @Test
    @DisplayName("Strategy prevents unsafe RETRY_CHARGE on authentication failure and uses communication channel")
    void testStrategyPreventsUnsafeRetryOnAuthFailure() throws Exception {
        recoveryCase.setFailureReasonCategory("authentication_failure");
        recoveryCaseRepository.saveAndFlush(recoveryCase);

        // Agent requested RETRY_CHARGE
        agentDecisionRepository.saveAndFlush(AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("RETRY_CHARGE")
                .channel(RecoveryChannel.RETRY_CHARGE)
                .confidenceScore(new BigDecimal("0.8500"))
                .reasoning("Agent incorrectly suggested retry")
                .modelName("gemini-3.7-flash")
                .build());

        // Strategy engine overrides unsafe RETRY_CHARGE to WHATSAPP because customer has phone
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("ineligible")));
    }

    @Test
    @DisplayName("Strategy fallback triggers alternative channel after previous attempt fails")
    void testStrategyFallbackAfterFailedAttempt() throws Exception {
        // First attempt on WhatsApp failed
        recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .resultCode("DELIVERY_FAILED")
                .resultMessage("Recipient unreachable")
                .build());

        agentDecisionRepository.saveAndFlush(AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("SEND_WHATSAPP")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.8500"))
                .reasoning("Suggested WhatsApp")
                .modelName("gemini-3.7-flash")
                .build());

        // Evaluated strategy should fallback to EMAIL
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.fallbackChannel").value("SMS"));
    }

    @Test
    @DisplayName("Max attempts enforcement blocks attempt creation and returns terminal strategy")
    void testMaxAttemptsEnforcement() throws Exception {
        for (int i = 1; i <= 3; i++) {
            recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                    .merchant(merchant)
                    .recoveryCase(recoveryCase)
                    .attemptNumber(i)
                    .channel(RecoveryChannel.WHATSAPP)
                    .status(RecoveryAttemptStatus.FAILED)
                    .build());
        }

        agentDecisionRepository.saveAndFlush(AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("SEND_WHATSAPP")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.8000"))
                .reasoning("Suggested WhatsApp")
                .modelName("gemini-3.7-flash")
                .build());

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminal").value(true))
                .andExpect(jsonPath("$.recommendedAction").value("MAX_ATTEMPTS_EXCEEDED"));

        // Orchestration should reject when max attempts exceeded
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Terminal RECOVERED case is protected from orchestration and returns terminal strategy")
    void testTerminalCaseProtection() throws Exception {
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
        recoveryCaseRepository.saveAndFlush(recoveryCase);

        agentDecisionRepository.saveAndFlush(AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("SEND_WHATSAPP")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.8000"))
                .reasoning("Suggested WhatsApp")
                .modelName("gemini-3.7-flash")
                .build());

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminal").value(true));

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/orchestrate", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cross-tenant isolation prevents accessing strategy endpoints across merchants")
    void testCrossTenantIsolation() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }
}
