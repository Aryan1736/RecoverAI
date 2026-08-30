package com.recoverai.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.repository.WebhookEventRepository;
import com.recoverai.backend.security.RazorpaySignatureVerifier;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RazorpayPaymentReconciliationIntegrationTest {

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
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private RazorpaySignatureVerifier signatureVerifier;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Merchant merchantA;
    private Merchant merchantB;
    private Customer customerA;
    private Customer customerB;

    private static final String ACCOUNT_ID_A = "acc_rec_recon_A";
    private static final String SECRET_A = "secret_recon_A_12345";
    private static final String ACCOUNT_ID_B = "acc_rec_recon_B";
    private static final String SECRET_B = "secret_recon_B_67890";

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.findByRazorpayAccountId(ACCOUNT_ID_A).orElseGet(() ->
                merchantRepository.save(Merchant.builder()
                        .name("Reconciliation Merchant A")
                        .email("recon_a@merchant.com")
                        .razorpayAccountId(ACCOUNT_ID_A)
                        .webhookSecret(SECRET_A)
                        .status(MerchantStatus.ACTIVE)
                        .build()));

        merchantB = merchantRepository.findByRazorpayAccountId(ACCOUNT_ID_B).orElseGet(() ->
                merchantRepository.save(Merchant.builder()
                        .name("Reconciliation Merchant B")
                        .email("recon_b@merchant.com")
                        .razorpayAccountId(ACCOUNT_ID_B)
                        .webhookSecret(SECRET_B)
                        .status(MerchantStatus.ACTIVE)
                        .build()));

        customerA = customerRepository.findByMerchantIdAndEmail(merchantA.getId(), "alice@example.com").orElseGet(() ->
                customerRepository.save(Customer.builder()
                        .merchant(merchantA)
                        .email("alice@example.com")
                        .phone("+919111111111")
                        .build()));

        customerB = customerRepository.findByMerchantIdAndEmail(merchantB.getId(), "bob@example.com").orElseGet(() ->
                customerRepository.save(Customer.builder()
                        .merchant(merchantB)
                        .email("bob@example.com")
                        .phone("+919222222222")
                        .build()));
    }

    private String createPaymentCapturedPayload(String accountId, String eventId, String paymentId, String orderId, long amountPaise, String email) {
        return """
                {
                  "entity": "event",
                  "account_id": "%s",
                  "event_id": "%s",
                  "event": "payment.captured",
                  "contains": ["payment"],
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "entity": "payment",
                        "amount": %d,
                        "currency": "INR",
                        "status": "captured",
                        "order_id": "%s",
                        "method": "upi",
                        "captured": true,
                        "email": "%s",
                        "contact": "+919111111111",
                        "created_at": 1600000000
                      }
                    }
                  },
                  "created_at": 1600000000
                }
                """.formatted(accountId, eventId, paymentId, amountPaise, orderId, email);
    }

    private String createOrderPaidPayload(String accountId, String eventId, String paymentId, String orderId, long amountPaise, String email) {
        return """
                {
                  "entity": "event",
                  "account_id": "%s",
                  "event_id": "%s",
                  "event": "order.paid",
                  "contains": ["order", "payment"],
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "entity": "payment",
                        "amount": %d,
                        "currency": "INR",
                        "status": "captured",
                        "order_id": "%s",
                        "method": "upi",
                        "captured": true,
                        "email": "%s",
                        "contact": "+919111111111",
                        "created_at": 1600000000
                      }
                    },
                    "order": {
                      "entity": {
                        "id": "%s",
                        "entity": "order",
                        "amount": %d,
                        "amount_paid": %d,
                        "amount_due": 0,
                        "currency": "INR",
                        "status": "paid",
                        "created_at": 1600000000
                      }
                    }
                  },
                  "created_at": 1600000000
                }
                """.formatted(accountId, eventId, paymentId, amountPaise, orderId, email, orderId, amountPaise, amountPaise);
    }

    @Test
    @DisplayName("Closed-loop: payment.captured reconciles OPEN RecoveryCase, active attempts, and queued executions")
    void testPaymentCapturedReconcilesOpenRecoveryCase() throws Exception {
        // 1. Setup failed payment & recovery case
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_fail_init_01")
                .razorpayOrderId("order_recon_001")
                .amount(new BigDecimal("3500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(failedPayment)
                .customer(customerA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("3500.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .build());

        // Setup attempts: 1 in-flight, 1 scheduled
        RecoveryAttempt inFlightAttempt = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .build());

        RecoveryAttempt scheduledAttempt = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(recoveryCase)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build());

        // Setup pending queue item for scheduled attempt
        RecoveryExecutionQueueItem queueItem = queueRepository.save(RecoveryExecutionQueueItem.builder()
                .merchant(merchantA)
                .recoveryCase(recoveryCase)
                .recoveryAttempt(scheduledAttempt)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now().plusSeconds(300))
                .build());

        // 2. Deliver payment.captured webhook for this payment/order
        String payload = createPaymentCapturedPayload(ACCOUNT_ID_A, "evt_cap_001", "pay_fail_init_01", "order_recon_001", 350000, "alice@example.com");
        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        entityManager.flush();
        entityManager.clear();

        // 3. Verify Payment is CAPTURED
        Payment updatedPayment = paymentRepository.findById(failedPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.CAPTURED, updatedPayment.getStatus());

        // 4. Verify RecoveryCase is RECOVERED with trusted amount
        RecoveryCase updatedCase = recoveryCaseRepository.findById(recoveryCase.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.RECOVERED, updatedCase.getStatus());
        assertEquals(new BigDecimal("3500.00"), updatedCase.getRecoveredAmount());
        assertNotNull(updatedCase.getRecoveredAt());

        // 5. Verify Attempts reconciled
        RecoveryAttempt updatedInFlight = recoveryAttemptRepository.findById(inFlightAttempt.getId()).orElseThrow();
        assertEquals(RecoveryAttemptStatus.SUCCESS, updatedInFlight.getStatus());
        assertEquals("PAYMENT_RECONCILED", updatedInFlight.getResultCode());

        RecoveryAttempt updatedScheduled = recoveryAttemptRepository.findById(scheduledAttempt.getId()).orElseThrow();
        assertEquals(RecoveryAttemptStatus.SKIPPED, updatedScheduled.getStatus());
        assertEquals("CASE_RECOVERED", updatedScheduled.getResultCode());

        // 6. Verify Queue Item is COMPLETED with CASE_TERMINAL_RECOVERED
        RecoveryExecutionQueueItem updatedQueueItem = queueRepository.findById(queueItem.getId()).orElseThrow();
        assertEquals(RecoveryQueueStatus.COMPLETED, updatedQueueItem.getStatus());
        assertEquals("CASE_TERMINAL_RECOVERED", updatedQueueItem.getLastErrorCode());
        assertNotNull(updatedQueueItem.getCompletedAt());

        // 7. Verify Audit Event recorded
        List<AuditEvent> audits = auditEventRepository.findByMerchantId(merchantA.getId());
        boolean hasReconAudit = audits.stream()
                .anyMatch(a -> "RECOVERY_PAYMENT_RECONCILED".equals(a.getEventType()) && a.getDetails().contains("order_recon_001"));
        assertTrue(hasReconAudit, "RECOVERY_PAYMENT_RECONCILED audit event must be present");
    }

    @Test
    @DisplayName("Recovery link flow: new Razorpay payment attempt on same order reconciles original RecoveryCase")
    void testRecoveryLinkNewPaymentAttemptReconcilesOriginalCase() throws Exception {
        // Initial failed payment created a case
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_fail_link_01")
                .razorpayOrderId("order_link_999")
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(failedPayment)
                .customer(customerA)
                .status(RecoveryCaseStatus.IN_PROGRESS)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("5000.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .build());

        // Customer completed checkout via recovery link -> Razorpay assigns NEW payment ID "pay_success_link_02" for same order
        String payload = createPaymentCapturedPayload(ACCOUNT_ID_A, "evt_link_pay_02", "pay_success_link_02", "order_link_999", 500000, "alice@example.com");
        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        // New payment persisted as CAPTURED
        Optional<Payment> newPaymentOpt = paymentRepository.findByMerchantIdAndRazorpayPaymentId(merchantA.getId(), "pay_success_link_02");
        assertTrue(newPaymentOpt.isPresent());
        assertEquals(PaymentStatus.CAPTURED, newPaymentOpt.get().getStatus());

        // RecoveryCase correctly resolved via order relationship
        RecoveryCase reconciledCase = recoveryCaseRepository.findById(recoveryCase.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.RECOVERED, reconciledCase.getStatus());
        assertEquals(new BigDecimal("5000.00"), reconciledCase.getRecoveredAmount());
    }

    @Test
    @DisplayName("order.paid webhook reconciles RecoveryCase")
    void testOrderPaidReconcilesCase() throws Exception {
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_fail_ord_01")
                .razorpayOrderId("order_paid_777")
                .amount(new BigDecimal("1200.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(failedPayment)
                .customer(customerA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.MEDIUM)
                .estimatedRecoverableAmount(new BigDecimal("1200.00"))
                .currency("INR")
                .build());

        String payload = createOrderPaidPayload(ACCOUNT_ID_A, "evt_ord_paid_1", "pay_ord_succ_1", "order_paid_777", 120000, "alice@example.com");
        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        RecoveryCase reconciledCase = recoveryCaseRepository.findById(recoveryCase.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.RECOVERED, reconciledCase.getStatus());
        assertEquals(new BigDecimal("1200.00"), reconciledCase.getRecoveredAmount());
    }

    @Test
    @DisplayName("Multi-tenant security: Merchant A payment cannot reconcile Merchant B recovery case with same order ID")
    void testCrossTenantIsolation() throws Exception {
        // Merchant B has an active recovery case for order "shared_order_123"
        Payment merchantBPayment = paymentRepository.save(Payment.builder()
                .merchant(merchantB)
                .customer(customerB)
                .razorpayPaymentId("pay_b_fail_01")
                .razorpayOrderId("shared_order_123")
                .amount(new BigDecimal("4000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase caseB = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantB)
                .payment(merchantBPayment)
                .customer(customerB)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("4000.00"))
                .currency("INR")
                .build());

        // Merchant A sends payment.captured for order "shared_order_123"
        String payloadA = createPaymentCapturedPayload(ACCOUNT_ID_A, "evt_tenant_a_1", "pay_a_succ_01", "shared_order_123", 400000, "alice@example.com");
        String signatureA = signatureVerifier.calculateHmacSha256(payloadA, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signatureA)
                        .content(payloadA))
                .andExpect(status().isOk());

        // Assert: Merchant B's recovery case is STILL OPEN and was NOT reconciled
        RecoveryCase untouchedCaseB = recoveryCaseRepository.findById(caseB.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.OPEN, untouchedCaseB.getStatus());
        assertEquals(BigDecimal.ZERO, untouchedCaseB.getRecoveredAmount());
    }

    @Test
    @DisplayName("Idempotency: duplicate payment.captured does not repeat transitions or create duplicate audit entries")
    void testWebhookDuplicateIdempotency() throws Exception {
        Payment failedPayment = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_idem_fail_1")
                .razorpayOrderId("order_idem_100")
                .amount(new BigDecimal("2000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        RecoveryCase recoveryCase = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(failedPayment)
                .customer(customerA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.MEDIUM)
                .estimatedRecoverableAmount(new BigDecimal("2000.00"))
                .currency("INR")
                .build());

        String payload = createPaymentCapturedPayload(ACCOUNT_ID_A, "evt_idem_recon_1", "pay_idem_fail_1", "order_idem_100", 200000, "alice@example.com");
        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        // First delivery
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        long auditCountAfterFirst = auditEventRepository.findByMerchantId(merchantA.getId()).stream()
                .filter(a -> "RECOVERY_PAYMENT_RECONCILED".equals(a.getEventType()))
                .count();
        assertEquals(1, auditCountAfterFirst);

        // Second delivery (duplicate event)
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        long auditCountAfterSecond = auditEventRepository.findByMerchantId(merchantA.getId()).stream()
                .filter(a -> "RECOVERY_PAYMENT_RECONCILED".equals(a.getEventType()))
                .count();
        assertEquals(1, auditCountAfterSecond, "Duplicate webhook delivery must not produce duplicate recovery reconciliation audits");
    }
}
