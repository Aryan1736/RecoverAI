package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RecoveryStrategyRepositoryTest {

    @Autowired
    private RecoveryStrategyRepository recoveryStrategyRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant merchantA;
    private Merchant merchantB;
    private RecoveryCase caseA;
    private RecoveryCase caseB;

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Merchant A")
                .email("merch_a_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Merchant B")
                .email("merch_b_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer customerA = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchantA)
                .name("Customer A")
                .email("cust_a@test.com")
                .build());

        Payment paymentA = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_a_" + UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        caseA = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantA)
                .customer(customerA)
                .payment(paymentA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("1000.00"))
                .currency("INR")
                .build());

        Customer customerB = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchantB)
                .name("Customer B")
                .email("cust_b@test.com")
                .build());

        Payment paymentB = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantB)
                .customer(customerB)
                .razorpayPaymentId("pay_b_" + UUID.randomUUID())
                .amount(new BigDecimal("2000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        caseB = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantB)
                .customer(customerB)
                .payment(paymentB)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.MEDIUM)
                .estimatedRecoverableAmount(new BigDecimal("2000.00"))
                .currency("INR")
                .build());
    }

    @Test
    @DisplayName("Persisting and finding latest RecoveryStrategy by case and merchant")
    void testPersistAndFindLatestStrategy() {
        RecoveryStrategy strat1 = recoveryStrategyRepository.saveAndFlush(RecoveryStrategy.builder()
                .merchant(merchantA)
                .recoveryCase(caseA)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .confidenceScore(new BigDecimal("0.7500"))
                .priority(RecoveryPriority.HIGH)
                .delaySeconds(0)
                .maxAttempts(3)
                .reason("First strategy")
                .fallbackChannel(RecoveryChannel.EMAIL)
                .fallbackAction("SEND_EMAIL_REMINDER")
                .isTerminal(false)
                .createdAt(Instant.now().minusSeconds(60))
                .build());

        RecoveryStrategy strat2 = recoveryStrategyRepository.saveAndFlush(RecoveryStrategy.builder()
                .merchant(merchantA)
                .recoveryCase(caseA)
                .channel(RecoveryChannel.RETRY_CHARGE)
                .recommendedAction("RETRY_CHARGE")
                .confidenceScore(new BigDecimal("0.9000"))
                .priority(RecoveryPriority.HIGH)
                .delaySeconds(300)
                .maxAttempts(3)
                .reason("Second strategy")
                .fallbackChannel(RecoveryChannel.WHATSAPP)
                .fallbackAction("SEND_WHATSAPP_REMINDER")
                .isTerminal(false)
                .createdAt(Instant.now())
                .build());

        Optional<RecoveryStrategy> latest = recoveryStrategyRepository
                .findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseA.getId(), merchantA.getId());

        assertThat(latest).isPresent();
        assertThat(latest.get().getId()).isEqualTo(strat2.getId());
        assertThat(latest.get().getChannel()).isEqualTo(RecoveryChannel.RETRY_CHARGE);

        List<RecoveryStrategy> all = recoveryStrategyRepository
                .findByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseA.getId(), merchantA.getId());
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Cross-tenant isolation: Merchant B cannot access Merchant A's strategies")
    void testCrossTenantIsolation() {
        RecoveryStrategy stratA = recoveryStrategyRepository.saveAndFlush(RecoveryStrategy.builder()
                .merchant(merchantA)
                .recoveryCase(caseA)
                .channel(RecoveryChannel.WHATSAPP)
                .recommendedAction("SEND_WHATSAPP_REMINDER")
                .confidenceScore(new BigDecimal("0.8000"))
                .reason("Strategy for A")
                .build());

        Optional<RecoveryStrategy> result = recoveryStrategyRepository
                .findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(caseA.getId(), merchantB.getId());

        assertThat(result).isEmpty();

        Optional<RecoveryStrategy> byIdAndMerchant = recoveryStrategyRepository
                .findByIdAndMerchantId(stratA.getId(), merchantB.getId());

        assertThat(byIdAndMerchant).isEmpty();
    }
}
