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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class RecoveryAttemptRepositoryTest {

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

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
                .name("Attempt Merchant")
                .email("attempt_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .name("Attempt Customer")
                .email("attcust_" + UUID.randomUUID() + "@test.com")
                .build());

        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_att_" + UUID.randomUUID())
                .amount(new BigDecimal("1499.00"))
                .status(PaymentStatus.FAILED)
                .build());

        recoveryCase = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("1499.00"))
                .build());
    }

    @Test
    @DisplayName("Should persist and retrieve recovery attempt")
    void testCreateAndFindRecoveryAttempt() {
        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SENT)
                .scheduledAt(Instant.now())
                .executedAt(Instant.now())
                .recoveryLink("https://recoverai.io/pay/rec_123")
                .resultCode("DELIVERED")
                .resultMessage("Message delivered via WhatsApp Business API")
                .metadata("{\"template_id\":\"recovery_v1\"}")
                .build();

        RecoveryAttempt saved = recoveryAttemptRepository.saveAndFlush(attempt);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Optional<RecoveryAttempt> found = recoveryAttemptRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(RecoveryChannel.WHATSAPP, found.get().getChannel());
        assertEquals(RecoveryAttemptStatus.SENT, found.get().getStatus());
        assertEquals(1, found.get().getAttemptNumber());
        assertEquals("https://recoverai.io/pay/rec_123", found.get().getRecoveryLink());
    }

    @Test
    @DisplayName("Should retrieve attempts ordered by attempt number")
    void testFindOrderedByAttemptNumber() {
        RecoveryAttempt a1 = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .build();
        RecoveryAttempt a2 = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SENT)
                .build();

        recoveryAttemptRepository.saveAndFlush(a2);
        recoveryAttemptRepository.saveAndFlush(a1);

        List<RecoveryAttempt> ordered = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(recoveryCase.getId());
        assertEquals(2, ordered.size());
        assertEquals(1, ordered.get(0).getAttemptNumber());
        assertEquals(2, ordered.get(1).getAttemptNumber());
    }

    @Test
    @DisplayName("Should enforce uniqueness of attempt_number per recovery_case")
    void testUniqueAttemptNumberPerCase() {
        RecoveryAttempt a1 = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .build();
        recoveryAttemptRepository.saveAndFlush(a1);

        RecoveryAttempt a2 = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            recoveryAttemptRepository.saveAndFlush(a2);
        });
    }

    @Test
    @DisplayName("Should find due scheduled attempt IDs and ignore future scheduled attempts")
    void testFindDueScheduledAttemptIds() {
        RecoveryAttempt dueAttempt = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(100))
                .build();
        recoveryAttemptRepository.saveAndFlush(dueAttempt);

        Payment payment2 = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(recoveryCase.getCustomer())
                .razorpayPaymentId("pay_att_2_" + UUID.randomUUID())
                .amount(new BigDecimal("2000.00"))
                .status(PaymentStatus.FAILED)
                .build());
        RecoveryCase case2 = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchant)
                .payment(payment2)
                .customer(recoveryCase.getCustomer())
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("2000.00"))
                .build());

        RecoveryAttempt futureAttempt = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(case2)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().plusSeconds(3600))
                .build();
        recoveryAttemptRepository.saveAndFlush(futureAttempt);

        List<UUID> dueIds = recoveryAttemptRepository.findDueScheduledAttemptIds(
                RecoveryAttemptStatus.SCHEDULED, Instant.now(), org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(1, dueIds.size());
        assertEquals(dueAttempt.getId(), dueIds.get(0));
    }

    @Test
    @DisplayName("Should atomically claim attempt for execution")
    void testClaimAttemptForExecution() {
        RecoveryAttempt attempt = RecoveryAttempt.builder()
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().minusSeconds(50))
                .build();
        recoveryAttemptRepository.saveAndFlush(attempt);

        int claimed = recoveryAttemptRepository.claimAttemptForExecution(
                attempt.getId(), RecoveryAttemptStatus.SCHEDULED, RecoveryAttemptStatus.IN_FLIGHT, Instant.now());
        assertEquals(1, claimed);

        // Second claim attempt should return 0
        int claimedAgain = recoveryAttemptRepository.claimAttemptForExecution(
                attempt.getId(), RecoveryAttemptStatus.SCHEDULED, RecoveryAttemptStatus.IN_FLIGHT, Instant.now());
        assertEquals(0, claimedAgain);
    }
}
