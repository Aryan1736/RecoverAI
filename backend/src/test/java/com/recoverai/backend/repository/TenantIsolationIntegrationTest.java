package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    private Merchant merchantA;
    private Merchant merchantB;

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Merchant Alpha")
                .email("alpha@domain.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Merchant Beta")
                .email("beta@domain.com")
                .status(MerchantStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Queries for Merchant Alpha must never return Merchant Beta's data")
    void testTenantDataIsolation() {
        // Customer isolation
        Customer custA = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchantA)
                .email("customer@client.com")
                .name("Client A")
                .build());

        Customer custB = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchantB)
                .email("customer@client.com")
                .name("Client B")
                .build());

        List<Customer> alphaCustomers = customerRepository.findByMerchantId(merchantA.getId());
        assertEquals(1, alphaCustomers.size());
        assertEquals("Client A", alphaCustomers.get(0).getName());

        Optional<Customer> alphaFindById = customerRepository.findByIdAndMerchantId(custB.getId(), merchantA.getId());
        assertFalse(alphaFindById.isPresent(), "Merchant Alpha cannot access Merchant Beta's customer");

        // Payment isolation
        Payment payA = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantA)
                .customer(custA)
                .razorpayPaymentId("pay_alpha_001")
                .amount(new BigDecimal("1500.00"))
                .status(PaymentStatus.FAILED)
                .build());

        Payment payB = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantB)
                .customer(custB)
                .razorpayPaymentId("pay_beta_001")
                .amount(new BigDecimal("2500.00"))
                .status(PaymentStatus.FAILED)
                .build());

        List<Payment> alphaPayments = paymentRepository.findByMerchantId(merchantA.getId());
        assertEquals(1, alphaPayments.size());
        assertEquals("pay_alpha_001", alphaPayments.get(0).getRazorpayPaymentId());

        Optional<Payment> alphaPaymentLookup = paymentRepository.findByIdAndMerchantId(payB.getId(), merchantA.getId());
        assertFalse(alphaPaymentLookup.isPresent(), "Merchant Alpha cannot access Merchant Beta's payment");

        // Recovery Case isolation
        RecoveryCase caseA = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(payA)
                .customer(custA)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("1500.00"))
                .build());

        RecoveryCase caseB = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantB)
                .payment(payB)
                .customer(custB)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("2500.00"))
                .build());

        List<RecoveryCase> alphaCases = recoveryCaseRepository.findByMerchantId(merchantA.getId());
        assertEquals(1, alphaCases.size());
        assertEquals(caseA.getId(), alphaCases.get(0).getId());

        Optional<RecoveryCase> alphaCaseLookup = recoveryCaseRepository.findByIdAndMerchantId(caseB.getId(), merchantA.getId());
        assertFalse(alphaCaseLookup.isPresent(), "Merchant Alpha cannot access Merchant Beta's recovery case");

        // Recovery Attempt isolation
        RecoveryAttempt attA = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(caseA)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build());

        RecoveryAttempt attB = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .merchant(merchantB)
                .recoveryCase(caseB)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build());

        List<RecoveryAttempt> alphaAttempts = recoveryAttemptRepository.findByMerchantId(merchantA.getId());
        assertEquals(1, alphaAttempts.size());
        assertEquals(attA.getId(), alphaAttempts.get(0).getId());

        Optional<RecoveryAttempt> alphaAttemptLookup = recoveryAttemptRepository.findByIdAndMerchantId(attB.getId(), merchantA.getId());
        assertFalse(alphaAttemptLookup.isPresent(), "Merchant Alpha cannot access Merchant Beta's recovery attempt");
    }
}
