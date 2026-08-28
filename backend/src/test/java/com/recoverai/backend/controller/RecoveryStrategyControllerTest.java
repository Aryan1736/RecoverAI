package com.recoverai.backend.controller;

import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecoveryStrategyControllerTest {

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
    private RecoveryStrategyRepository recoveryStrategyRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Merchant merchant;
    private Merchant otherMerchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private AgentDecision agentDecision;
    private String token;
    private String otherToken;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.save(Merchant.builder()
                .name("Strategy Test Merchant")
                .email("merchant_" + UUID.randomUUID() + "@strategy.test")
                .webhookSecret("secret-strategy")
                .build());

        otherMerchant = merchantRepository.save(Merchant.builder()
                .name("Other Test Merchant")
                .email("other_" + UUID.randomUUID() + "@strategy.test")
                .webhookSecret("secret-other")
                .build());

        token = jwtTokenProvider.generateToken(merchant);
        otherToken = jwtTokenProvider.generateToken(otherMerchant);

        customer = customerRepository.save(Customer.builder()
                .merchant(merchant)
                .name("Alice Wonderland")
                .email("alice@strategy.test")
                .phone("+919876543210")
                .build());

        payment = paymentRepository.save(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_strat_" + UUID.randomUUID())
                .amount(new BigDecimal("3500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .build());

        recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("insufficient_funds")
                .estimatedRecoverableAmount(new BigDecimal("3500.00"))
                .currency("INR")
                .build());

        agentDecision = agentDecisionRepository.save(AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("RETRY_CHARGE")
                .channel(RecoveryChannel.RETRY_CHARGE)
                .confidenceScore(new BigDecimal("0.8500"))
                .reasoning("High probability of funds restored")
                .modelName("gemini-3.7-flash")
                .build());
    }

    @Test
    @DisplayName("POST /recovery-cases/{id}/strategy succeeds with JWT authentication")
    void testPostStrategyAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.recoveryCaseId").value(recoveryCase.getId().toString()))
                .andExpect(jsonPath("$.merchantId").value(merchant.getId().toString()))
                .andExpect(jsonPath("$.channel").value("RETRY_CHARGE"))
                .andExpect(jsonPath("$.recommendedAction").value("RETRY_CHARGE"))
                .andExpect(jsonPath("$.terminal").value(false))
                .andExpect(jsonPath("$.rawResponse").doesNotExist());
    }

    @Test
    @DisplayName("POST /merchants/{merchantId}/recovery-cases/{id}/strategy succeeds with path parameters")
    void testPostStrategyWithPath() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/{merchantId}/recovery-cases/{id}/strategy",
                        merchant.getId(), recoveryCase.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("RETRY_CHARGE"));
    }

    @Test
    @DisplayName("POST /recovery-cases/{id}/strategy without auth token returns 401")
    void testPostStrategyUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /recovery-cases/{id}/strategy with cross-tenant token returns 404")
    void testPostStrategyCrossTenant() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /recovery-cases/{id}/strategy returns latest strategy")
    void testGetStrategyAuthenticated() throws Exception {
        RecoveryStrategy strategy = recoveryStrategyRepository.save(RecoveryStrategy.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .priority(RecoveryPriority.HIGH)
                .confidenceScore(new BigDecimal("0.8500"))
                .reason("AI recommended WhatsApp")
                .delaySeconds(0)
                .maxAttempts(3)
                .isTerminal(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(strategy.getId().toString()))
                .andExpect(jsonPath("$.channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.recommendedAction").value("SEND_WHATSAPP_REMINDER"));
    }

    @Test
    @DisplayName("GET /recovery-cases/{id}/strategy for non-existent strategy returns 404")
    void testGetStrategyNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /recovery-cases/{id}/strategy for terminal case returns terminal strategy response safely")
    void testPostStrategyTerminalCase() throws Exception {
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
        recoveryCaseRepository.save(recoveryCase);

        mockMvc.perform(post("/api/v1/recovery-cases/{id}/strategy", recoveryCase.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminal").value(true))
                .andExpect(jsonPath("$.recommendedAction").value("NO_ACTION_TERMINAL"));
    }
}
