package com.recoverai.backend.repository;

import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryOutcomeEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.WebhookProcessingStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class RecoveryOutcomeEventRepositoryTest {

    @Autowired
    private RecoveryOutcomeEventRepository recoveryOutcomeEventRepository;

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
    private RecoveryAttempt attemptA;
    private RecoveryAttempt attemptB;

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Alpha Merchant")
                .email("alpha@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Beta Merchant")
                .email("beta@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer customerA = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchantA)
                .email("custA@test.com")
                .name("Cust A")
                .build());

        Payment paymentA = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_alpha_1")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase caseA = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(paymentA)
                .customer(customerA)
                .status(RecoveryCaseStatus.IN_PROGRESS)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("1500.00"))
                .build());

        attemptA = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(caseA)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SENT)
                .build());

        Customer customerB = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchantB)
                .email("custB@test.com")
                .name("Cust B")
                .build());

        Payment paymentB = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantB)
                .customer(customerB)
                .razorpayPaymentId("pay_beta_1")
                .amount(new BigDecimal("2500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase caseB = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantB)
                .payment(paymentB)
                .customer(customerB)
                .status(RecoveryCaseStatus.IN_PROGRESS)
                .priority(RecoveryPriority.MEDIUM)
                .estimatedRecoverableAmount(new BigDecimal("2500.00"))
                .build());

        attemptB = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .merchant(merchantB)
                .recoveryCase(caseB)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SENT)
                .build());
    }

    @Test
    @DisplayName("Persist and find RecoveryOutcomeEvent by merchant, provider, and providerEventId")
    void testFindByMerchantIdAndProviderAndProviderEventId() {
        RecoveryOutcomeEvent event = RecoveryOutcomeEvent.builder()
                .merchant(merchantA)
                .recoveryAttempt(attemptA)
                .provider("WHATSAPP")
                .providerEventId("evt_wa_1001")
                .payloadHash("hash1234567890abcdef")
                .processingStatus(WebhookProcessingStatus.PROCESSED)
                .processedAt(Instant.now())
                .build();

        recoveryOutcomeEventRepository.saveAndFlush(event);

        Optional<RecoveryOutcomeEvent> found = recoveryOutcomeEventRepository
                .findByMerchantIdAndProviderAndProviderEventId(merchantA.getId(), "WHATSAPP", "evt_wa_1001");

        assertTrue(found.isPresent());
        assertEquals("evt_wa_1001", found.get().getProviderEventId());
        assertEquals(merchantA.getId(), found.get().getMerchant().getId());
        assertEquals(attemptA.getId(), found.get().getRecoveryAttempt().getId());

        // Merchant isolation: Merchant B cannot find Merchant A's event
        Optional<RecoveryOutcomeEvent> notFoundForMerchantB = recoveryOutcomeEventRepository
                .findByMerchantIdAndProviderAndProviderEventId(merchantB.getId(), "WHATSAPP", "evt_wa_1001");
        assertFalse(notFoundForMerchantB.isPresent());
    }

    @Test
    @DisplayName("Find RecoveryOutcomeEvent by merchantId and payloadHash")
    void testFindByMerchantIdAndPayloadHash() {
        RecoveryOutcomeEvent event = RecoveryOutcomeEvent.builder()
                .merchant(merchantA)
                .recoveryAttempt(attemptA)
                .provider("WHATSAPP")
                .providerEventId("evt_wa_1002")
                .payloadHash("unique_payload_hash_alpha")
                .processingStatus(WebhookProcessingStatus.PROCESSED)
                .build();

        recoveryOutcomeEventRepository.saveAndFlush(event);

        Optional<RecoveryOutcomeEvent> found = recoveryOutcomeEventRepository
                .findByMerchantIdAndPayloadHash(merchantA.getId(), "unique_payload_hash_alpha");

        assertTrue(found.isPresent());
        assertEquals("evt_wa_1002", found.get().getProviderEventId());

        Optional<RecoveryOutcomeEvent> notFoundForMerchantB = recoveryOutcomeEventRepository
                .findByMerchantIdAndPayloadHash(merchantB.getId(), "unique_payload_hash_alpha");
        assertFalse(notFoundForMerchantB.isPresent());
    }

    @Test
    @DisplayName("Find RecoveryOutcomeEvents by recoveryAttemptId")
    void testFindByRecoveryAttemptId() {
        RecoveryOutcomeEvent event1 = RecoveryOutcomeEvent.builder()
                .merchant(merchantA)
                .recoveryAttempt(attemptA)
                .provider("WHATSAPP")
                .providerEventId("evt_wa_2001")
                .payloadHash("hash_event_1")
                .processingStatus(WebhookProcessingStatus.PROCESSED)
                .build();

        RecoveryOutcomeEvent event2 = RecoveryOutcomeEvent.builder()
                .merchant(merchantA)
                .recoveryAttempt(attemptA)
                .provider("WHATSAPP")
                .providerEventId("evt_wa_2002")
                .payloadHash("hash_event_2")
                .processingStatus(WebhookProcessingStatus.PROCESSED)
                .build();

        recoveryOutcomeEventRepository.saveAndFlush(event1);
        recoveryOutcomeEventRepository.saveAndFlush(event2);

        List<RecoveryOutcomeEvent> events = recoveryOutcomeEventRepository.findByRecoveryAttemptId(attemptA.getId());
        assertEquals(2, events.size());
    }
}
