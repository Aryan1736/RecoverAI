package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class RecoveryCaseRepositoryTest {

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Case Merchant")
                .email("casemerchant_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .name("Case Customer")
                .email("casecustomer_" + UUID.randomUUID() + "@test.com")
                .build());

        payment = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_case_" + UUID.randomUUID())
                .amount(new BigDecimal("2999.00"))
                .status(PaymentStatus.FAILED)
                .errorCode("PAYMENT_FAILED")
                .build());
    }

    @Test
    @DisplayName("Should persist and retrieve recovery case with relations")
    void testCreateAndFindRecoveryCase() {
        RecoveryCase recoveryCase = RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("INSUFFICIENT_FUNDS")
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        RecoveryCase saved = recoveryCaseRepository.saveAndFlush(recoveryCase);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<RecoveryCase> found = recoveryCaseRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(RecoveryCaseStatus.OPEN, found.get().getStatus());
        assertEquals(RecoveryPriority.HIGH, found.get().getPriority());
        assertEquals("INSUFFICIENT_FUNDS", found.get().getFailureReasonCategory());
        assertEquals(payment.getId(), found.get().getPayment().getId());
        assertEquals(merchant.getId(), found.get().getMerchant().getId());
    }

    @Test
    @DisplayName("Should find recovery case by paymentId")
    void testFindByPaymentId() {
        RecoveryCase recoveryCase = RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .build();
        recoveryCaseRepository.saveAndFlush(recoveryCase);

        Optional<RecoveryCase> found = recoveryCaseRepository.findByPaymentId(payment.getId());
        assertTrue(found.isPresent());
        assertEquals(merchant.getId(), found.get().getMerchant().getId());
    }

    @Test
    @DisplayName("Should count and filter cases by merchant and status")
    void testFilterAndCountByStatus() {
        RecoveryCase c1 = RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .build();
        recoveryCaseRepository.saveAndFlush(c1);

        List<RecoveryCase> openCases = recoveryCaseRepository.findByMerchantIdAndStatus(merchant.getId(), RecoveryCaseStatus.OPEN);
        assertEquals(1, openCases.size());

        long countOpen = recoveryCaseRepository.countByMerchantIdAndStatus(merchant.getId(), RecoveryCaseStatus.OPEN);
        assertEquals(1, countOpen);

        long countRecovered = recoveryCaseRepository.countByMerchantIdAndStatus(merchant.getId(), RecoveryCaseStatus.RECOVERED);
        assertEquals(0, countRecovered);
    }

    @Test
    @DisplayName("Should enforce 1:1 relationship between payment and recovery case")
    void testUniquePaymentRecoveryCaseConstraint() {
        RecoveryCase c1 = RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .build();
        recoveryCaseRepository.saveAndFlush(c1);

        RecoveryCase c2 = RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            recoveryCaseRepository.saveAndFlush(c2);
        });
    }
}
