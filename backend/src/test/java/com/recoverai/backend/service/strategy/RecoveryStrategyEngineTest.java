package com.recoverai.backend.service.strategy;

import com.recoverai.backend.config.RecoveryStrategyProperties;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.service.FailureReasonClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecoveryStrategyEngineTest {

    private RecoveryStrategyEngine engine;
    private RecoveryStrategyProperties properties;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;

    @BeforeEach
    void setUp() {
        properties = new RecoveryStrategyProperties();
        properties.setEnabled(true);
        properties.setMinAiConfidence(new BigDecimal("0.70"));
        properties.setMaxAttempts(3);
        properties.setRetryChargeEnabled(true);
        properties.setFallbackEnabled(true);
        properties.setMaxChannelFailures(1);
        properties.setDefaultDelaySeconds(0);
        properties.setRetryDelaySeconds(300);

        engine = new RecoveryStrategyEngine(properties);

        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Acme Corp")
                .email("acme@example.com")
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .name("Jane Doe")
                .email("jane@example.com")
                .phone("+919876543210")
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_123456789")
                .amount(new BigDecimal("2500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.CARD)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory(FailureReasonClassifier.CATEGORY_INSUFFICIENT_FUNDS)
                .estimatedRecoverableAmount(new BigDecimal("2500.00"))
                .currency("INR")
                .build();
    }

    @Test
    @DisplayName("Terminal RECOVERED case should return terminal strategy with no further action")
    void testTerminalRecoveredCase() {
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_REMINDER")
                .confidenceScore(new BigDecimal("0.95"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.isTerminal()).isTrue();
        assertThat(strategy.getRecommendedAction()).isEqualTo("NO_ACTION_TERMINAL");
        assertThat(strategy.getReason()).contains("terminal status");
    }

    @Test
    @DisplayName("Terminal CANCELLED case should return terminal strategy with no further action")
    void testTerminalCancelledCase() {
        recoveryCase.setStatus(RecoveryCaseStatus.CANCELLED);

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, null, List.of(), properties);

        assertThat(strategy.isTerminal()).isTrue();
        assertThat(strategy.getRecommendedAction()).isEqualTo("NO_ACTION_TERMINAL");
    }

    @Test
    @DisplayName("Terminal EXPIRED case should return terminal strategy with no further action")
    void testTerminalExpiredCase() {
        recoveryCase.setStatus(RecoveryCaseStatus.EXPIRED);

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, null, List.of(), properties);

        assertThat(strategy.isTerminal()).isTrue();
        assertThat(strategy.getRecommendedAction()).isEqualTo("NO_ACTION_TERMINAL");
    }

    @Test
    @DisplayName("High-confidence RETRY_CHARGE strategy for insufficient funds failure")
    void testHighConfidenceRetryChargeStrategy() {
        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.88"))
                .reasoning("Card has funds likely restored")
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.isTerminal()).isFalse();
        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(strategy.getRecommendedAction()).isEqualTo("RETRY_CHARGE");
        assertThat(strategy.getDelaySeconds()).isEqualTo(300);
        assertThat(strategy.getFallbackChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
    }

    @Test
    @DisplayName("Low-confidence AI decision should fallback to conservative communication strategy")
    void testLowConfidenceAiDecision() {
        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.45"))
                .reasoning("Uncertain failure context")
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.isTerminal()).isFalse();
        assertThat(strategy.getChannel()).isNotEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
        assertThat(strategy.getReason()).contains("below threshold");
    }

    @Test
    @DisplayName("Authentication failure should prevent RETRY_CHARGE even if AI requested it")
    void testAuthenticationFailurePreventsRetryCharge() {
        recoveryCase.setFailureReasonCategory(FailureReasonClassifier.CATEGORY_AUTHENTICATION_FAILURE);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.90"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isNotEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
        assertThat(strategy.getReason()).contains("ineligible");
    }

    @Test
    @DisplayName("Network error is eligible for RETRY_CHARGE with high confidence")
    void testNetworkErrorEligibleForRetryCharge() {
        recoveryCase.setFailureReasonCategory(FailureReasonClassifier.CATEGORY_NETWORK_ERROR);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.85"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.RETRY_CHARGE);
    }

    @Test
    @DisplayName("Bank decline failure should avoid RETRY_CHARGE and choose communication channel")
    void testBankDeclineFailure() {
        recoveryCase.setFailureReasonCategory(FailureReasonClassifier.CATEGORY_BANK_DECLINED);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.80"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
    }

    @Test
    @DisplayName("Invalid request failure should avoid RETRY_CHARGE")
    void testInvalidRequestFailure() {
        recoveryCase.setFailureReasonCategory(FailureReasonClassifier.CATEGORY_INVALID_REQUEST);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.85"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isNotEqualTo(RecoveryChannel.RETRY_CHARGE);
    }

    @Test
    @DisplayName("Unknown failure category should avoid RETRY_CHARGE")
    void testUnknownFailureCategory() {
        recoveryCase.setFailureReasonCategory(FailureReasonClassifier.CATEGORY_UNKNOWN);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.85"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isNotEqualTo(RecoveryChannel.RETRY_CHARGE);
    }

    @Test
    @DisplayName("Max attempts reached should produce terminal strategy")
    void testMaxAttemptsReached() {
        List<RecoveryAttempt> previousAttempts = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            previousAttempts.add(RecoveryAttempt.builder()
                    .attemptNumber(i)
                    .channel(RecoveryChannel.WHATSAPP)
                    .status(RecoveryAttemptStatus.FAILED)
                    .build());
        }

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_REMINDER")
                .confidenceScore(new BigDecimal("0.90"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, previousAttempts, properties);

        assertThat(strategy.isTerminal()).isTrue();
        assertThat(strategy.getRecommendedAction()).isEqualTo("MAX_ATTEMPTS_EXCEEDED");
        assertThat(strategy.getReason()).contains("Maximum recovery attempts");
    }

    @Test
    @DisplayName("Previous RETRY_CHARGE failure prevents repeated RETRY_CHARGE")
    void testPreviousRetryChargeFailurePreventsSecondRetryCharge() {
        List<RecoveryAttempt> previousAttempts = List.of(
                RecoveryAttempt.builder()
                        .attemptNumber(1)
                        .channel(RecoveryChannel.RETRY_CHARGE)
                        .status(RecoveryAttemptStatus.FAILED)
                        .build()
        );

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.90"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, previousAttempts, properties);

        assertThat(strategy.getChannel()).isNotEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
    }

    @Test
    @DisplayName("Previous communication failure on WhatsApp should fallback to Email")
    void testPreviousWhatsAppFailureFallsBackToEmail() {
        List<RecoveryAttempt> previousAttempts = List.of(
                RecoveryAttempt.builder()
                        .attemptNumber(1)
                        .channel(RecoveryChannel.WHATSAPP)
                        .status(RecoveryAttemptStatus.FAILED)
                        .build()
        );

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP")
                .confidenceScore(new BigDecimal("0.85"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, previousAttempts, properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.EMAIL);
        assertThat(strategy.getFallbackChannel()).isEqualTo(RecoveryChannel.SMS);
    }

    @Test
    @DisplayName("Previous communication failure on WhatsApp and Email should fallback to SMS")
    void testWhatsAppAndEmailFailureFallsBackToSms() {
        List<RecoveryAttempt> previousAttempts = List.of(
                RecoveryAttempt.builder()
                        .attemptNumber(1)
                        .channel(RecoveryChannel.WHATSAPP)
                        .status(RecoveryAttemptStatus.FAILED)
                        .build(),
                RecoveryAttempt.builder()
                        .attemptNumber(2)
                        .channel(RecoveryChannel.EMAIL)
                        .status(RecoveryAttemptStatus.FAILED)
                        .build()
        );

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP")
                .confidenceScore(new BigDecimal("0.85"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, previousAttempts, properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.SMS);
        assertThat(strategy.getFallbackChannel()).isEqualTo(RecoveryChannel.SMART_LINK);
    }

    @Test
    @DisplayName("No phone available should prevent WhatsApp and SMS, selecting Email")
    void testNoPhoneAvailableSelectsEmail() {
        customer.setPhone(null);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP")
                .confidenceScore(new BigDecimal("0.90"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.EMAIL);
    }

    @Test
    @DisplayName("No email available should prevent Email, selecting WhatsApp")
    void testNoEmailAvailableSelectsWhatsApp() {
        customer.setEmail(null);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.EMAIL)
                .recommendedAction("SEND_EMAIL")
                .confidenceScore(new BigDecimal("0.90"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
    }

    @Test
    @DisplayName("No phone and no email available should select MANUAL")
    void testNoContactAvailableSelectsManual() {
        customer.setPhone(null);
        customer.setEmail(null);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP")
                .confidenceScore(new BigDecimal("0.90"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.MANUAL);
    }

    @Test
    @DisplayName("Retry charge disabled in properties should prevent RETRY_CHARGE")
    void testRetryChargeDisabledInProperties() {
        properties.setRetryChargeEnabled(false);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.95"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isNotEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
    }

    @Test
    @DisplayName("Strategy feature disabled should provide safe direct passthrough")
    void testStrategyFeatureDisabled() {
        properties.setEnabled(false);

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.EMAIL)
                .recommendedAction("CUSTOM_ACTION")
                .confidenceScore(new BigDecimal("0.80"))
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.EMAIL);
        assertThat(strategy.getRecommendedAction()).isEqualTo("CUSTOM_ACTION");
        assertThat(strategy.getReason()).contains("disabled");
    }

    @Test
    @DisplayName("Custom configurable confidence threshold is respected")
    void testCustomConfidenceThreshold() {
        properties.setMinAiConfidence(new BigDecimal("0.90"));

        AgentDecision decision = AgentDecision.builder()
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.85")) // below 0.90 threshold
                .build();

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, decision, List.of(), properties);

        assertThat(strategy.getChannel()).isNotEqualTo(RecoveryChannel.RETRY_CHARGE);
        assertThat(strategy.getChannel()).isEqualTo(RecoveryChannel.WHATSAPP);
    }

    @Test
    @DisplayName("Custom configurable max attempts is respected")
    void testCustomMaxAttempts() {
        properties.setMaxAttempts(2);

        List<RecoveryAttempt> previousAttempts = List.of(
                RecoveryAttempt.builder().attemptNumber(1).channel(RecoveryChannel.WHATSAPP).status(RecoveryAttemptStatus.FAILED).build(),
                RecoveryAttempt.builder().attemptNumber(2).channel(RecoveryChannel.EMAIL).status(RecoveryAttemptStatus.FAILED).build()
        );

        RecoveryStrategy strategy = engine.evaluate(recoveryCase, null, previousAttempts, properties);

        assertThat(strategy.isTerminal()).isTrue();
        assertThat(strategy.getReason()).contains("Maximum recovery attempts (2) reached");
    }
}
