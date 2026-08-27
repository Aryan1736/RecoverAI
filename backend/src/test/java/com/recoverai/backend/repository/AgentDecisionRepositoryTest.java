package com.recoverai.backend.repository;

import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class AgentDecisionRepositoryTest {

    @Autowired
    private AgentDecisionRepository agentDecisionRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant merchant;
    private RecoveryCase recoveryCase;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("AI Merchant")
                .email("ai_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .name("AI Customer")
                .email("aicust_" + UUID.randomUUID() + "@test.com")
                .build());

        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_ai_" + UUID.randomUUID())
                .amount(new BigDecimal("999.00"))
                .status(PaymentStatus.FAILED)
                .build());

        recoveryCase = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("999.00"))
                .build());
    }

    @Test
    @DisplayName("Should persist and retrieve AI agent decision with reasoning and tokens")
    void testCreateAndFindAgentDecision() {
        AgentDecision decision = AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("SEND_WHATSAPP_LINK")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.9250"))
                .reasoning("Customer has high engagement on WhatsApp and card failure was due to momentary network timeout.")
                .modelName("gemini-3.7-flash")
                .modelVersion("2026-02")
                .promptTokens(350)
                .completionTokens(120)
                .decisionFactors("{\"customer_tier\":\"VIP\",\"failure_type\":\"TRANSIENT_NETWORK\"}")
                .rawResponse("{\"decision\":\"SEND_WHATSAPP_LINK\",\"confidence\":0.925}")
                .build();

        AgentDecision saved = agentDecisionRepository.saveAndFlush(decision);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<AgentDecision> found = agentDecisionRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SEND_WHATSAPP_LINK", found.get().getRecommendedAction());
        assertEquals(RecoveryChannel.WHATSAPP, found.get().getChannel());
        assertEquals(new BigDecimal("0.9250"), found.get().getConfidenceScore());
        assertEquals("gemini-3.7-flash", found.get().getModelName());
        assertEquals(350, found.get().getPromptTokens());
        assertEquals(120, found.get().getCompletionTokens());
    }

    @Test
    @DisplayName("Should retrieve latest decision for recovery case")
    void testFindLatestDecisionForCase() {
        AgentDecision d1 = AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("WAIT_AND_RETRY")
                .reasoning("First evaluation")
                .modelName("gemini-3.7-flash")
                .build();
        agentDecisionRepository.saveAndFlush(d1);

        AgentDecision d2 = AgentDecision.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("SEND_EMAIL_INCENTIVE")
                .reasoning("Second evaluation after wait period")
                .modelName("gemini-3.7-flash")
                .build();
        agentDecisionRepository.saveAndFlush(d2);

        List<AgentDecision> decisions = agentDecisionRepository.findByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCase.getId());
        assertEquals(2, decisions.size());

        Optional<AgentDecision> latest = agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(recoveryCase.getId());
        assertTrue(latest.isPresent());
    }
}
