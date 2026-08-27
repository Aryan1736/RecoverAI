package com.recoverai.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.WebhookEvent;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.WebhookProcessingStatus;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RazorpayWebhookIntegrationTest {

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
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private RazorpaySignatureVerifier signatureVerifier;

    @Autowired
    private ObjectMapper objectMapper;

    private Merchant merchantA;
    private Merchant merchantB;
    private static final String SECRET_A = "webhook_secret_key_merchant_a";
    private static final String SECRET_B = "webhook_secret_key_merchant_b";
    private static final String ACCOUNT_ID_A = "acc_merchantA123";
    private static final String ACCOUNT_ID_B = "acc_merchantB456";

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.save(Merchant.builder()
                .name("Alpha Retail")
                .email("admin@alpharetail.com")
                .razorpayAccountId(ACCOUNT_ID_A)
                .webhookSecret(SECRET_A)
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Beta Store")
                .email("admin@betastore.com")
                .razorpayAccountId(ACCOUNT_ID_B)
                .webhookSecret(SECRET_B)
                .status(MerchantStatus.ACTIVE)
                .build());
    }

    private String createPaymentFailedPayload(String accountId, String paymentId, String email, long amountPaise) {
        return """
                {
                  "entity": "event",
                  "account_id": "%s",
                  "event_id": "evt_%s",
                  "event": "payment.failed",
                  "contains": ["payment"],
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "%s",
                        "entity": "payment",
                        "amount": %d,
                        "currency": "INR",
                        "status": "failed",
                        "order_id": "order_test_999",
                        "invoice_id": null,
                        "international": false,
                        "method": "card",
                        "amount_refunded": 0,
                        "refund_status": null,
                        "captured": false,
                        "description": "Payment test",
                        "card_id": "card_test_123",
                        "bank": "HDFC",
                        "wallet": null,
                        "vpa": null,
                        "email": "%s",
                        "contact": "+919876543210",
                        "customer_id": "cust_test_123",
                        "error_code": "BAD_REQUEST_ERROR",
                        "error_description": "Payment failed due to insufficient funds",
                        "error_source": "bank",
                        "error_step": "payment_authorization",
                        "error_reason": "insufficient_funds",
                        "created_at": 1600000000
                      }
                    }
                  },
                  "created_at": 1600000000
                }
                """.formatted(accountId, paymentId, paymentId, amountPaise, email);
    }

    private String createPaymentCapturedPayload(String accountId, String paymentId, String email, long amountPaise) {
        return """
                {
                  "entity": "event",
                  "account_id": "%s",
                  "event_id": "evt_%s",
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
                        "order_id": "order_test_999",
                        "invoice_id": null,
                        "international": false,
                        "method": "upi",
                        "amount_refunded": 0,
                        "refund_status": null,
                        "captured": true,
                        "email": "%s",
                        "contact": "+919876543210",
                        "created_at": 1600000000
                      }
                    }
                  },
                  "created_at": 1600000000
                }
                """.formatted(accountId, paymentId, paymentId, amountPaise, email);
    }

    @Test
    @DisplayName("Should successfully ingest payment.failed webhook and create Payment, Customer, and RecoveryCase")
    void testPaymentFailedIngestion() throws Exception {
        String payload = createPaymentFailedPayload(ACCOUNT_ID_A, "pay_fail_001", "john@example.com", 600000); // 6000.00 INR
        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        // Verify Customer persisted
        Optional<Customer> customerOpt = customerRepository.findByMerchantIdAndEmail(merchantA.getId(), "john@example.com");
        assertTrue(customerOpt.isPresent(), "Customer should be persisted under Merchant A");
        Customer customer = customerOpt.get();
        assertEquals("+919876543210", customer.getPhone());
        assertEquals("cust_test_123", customer.getRazorpayCustomerId());

        // Verify Payment persisted
        Optional<Payment> paymentOpt = paymentRepository.findByMerchantIdAndRazorpayPaymentId(merchantA.getId(), "pay_fail_001");
        assertTrue(paymentOpt.isPresent(), "Payment should be persisted under Merchant A");
        Payment payment = paymentOpt.get();
        assertEquals(new BigDecimal("6000.00"), payment.getAmount());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("insufficient_funds", payment.getErrorReason());
        assertEquals(customer.getId(), payment.getCustomer().getId());

        // Verify RecoveryCase created
        Optional<RecoveryCase> caseOpt = recoveryCaseRepository.findByPaymentId(payment.getId());
        assertTrue(caseOpt.isPresent(), "RecoveryCase should be created for failed payment");
        RecoveryCase recoveryCase = caseOpt.get();
        assertEquals(RecoveryCaseStatus.OPEN, recoveryCase.getStatus());
        assertEquals(RecoveryPriority.HIGH, recoveryCase.getPriority()); // 6000 >= 5000
        assertEquals("insufficient_funds", recoveryCase.getFailureReasonCategory());
        assertEquals(new BigDecimal("6000.00"), recoveryCase.getEstimatedRecoverableAmount());
        assertEquals(merchantA.getId(), recoveryCase.getMerchant().getId());

        // Verify WebhookEvent recorded
        Optional<WebhookEvent> webhookEventOpt = webhookEventRepository.findByMerchantIdAndRazorpayEventId(merchantA.getId(), "evt_pay_fail_001");
        assertTrue(webhookEventOpt.isPresent(), "WebhookEvent should be recorded");
        assertEquals(WebhookProcessingStatus.PROCESSED, webhookEventOpt.get().getProcessingStatus());

        // Verify Audit records
        List<AuditEvent> auditEvents = auditEventRepository.findByMerchantId(merchantA.getId());
        assertFalse(auditEvents.isEmpty(), "Audit records should be created");
        boolean hasRecoveryAudit = auditEvents.stream().anyMatch(e -> "RECOVERY_CASE_CREATED".equals(e.getEventType()));
        assertTrue(hasRecoveryAudit, "RECOVERY_CASE_CREATED audit event should be present");
    }

    @Test
    @DisplayName("Should successfully ingest payment.captured and not create a RecoveryCase")
    void testPaymentCapturedIngestion() throws Exception {
        String payload = createPaymentCapturedPayload(ACCOUNT_ID_A, "pay_cap_001", "sarah@example.com", 250000); // 2500.00 INR
        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        // Verify Payment persisted with CAPTURED status
        Optional<Payment> paymentOpt = paymentRepository.findByMerchantIdAndRazorpayPaymentId(merchantA.getId(), "pay_cap_001");
        assertTrue(paymentOpt.isPresent());
        assertEquals(PaymentStatus.CAPTURED, paymentOpt.get().getStatus());

        // Verify NO RecoveryCase is created
        Optional<RecoveryCase> caseOpt = recoveryCaseRepository.findByPaymentId(paymentOpt.get().getId());
        assertFalse(caseOpt.isPresent(), "No recovery case should be created for captured payments");
    }

    @Test
    @DisplayName("Idempotency: duplicate webhook should be skipped safely without creating duplicate records")
    void testWebhookIdempotency() throws Exception {
        String payload = createPaymentFailedPayload(ACCOUNT_ID_A, "pay_idem_001", "idem@example.com", 150000); // 1500.00 INR
        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        // First delivery
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk());

        long paymentCountBefore = paymentRepository.count();
        long customerCountBefore = customerRepository.count();
        long caseCountBefore = recoveryCaseRepository.count();

        // Second delivery (duplicate retry by Razorpay)
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        // Counts must not increase
        assertEquals(paymentCountBefore, paymentRepository.count(), "Payment count must not increase on duplicate");
        assertEquals(customerCountBefore, customerRepository.count(), "Customer count must not increase on duplicate");
        assertEquals(caseCountBefore, recoveryCaseRepository.count(), "RecoveryCase count must not increase on duplicate");
    }

    @Test
    @DisplayName("Invalid signature should be rejected with HTTP 401 and no state modification")
    void testInvalidSignatureRejected() throws Exception {
        String payload = createPaymentFailedPayload(ACCOUNT_ID_A, "pay_bad_sig", "hacker@example.com", 50000);
        String invalidSignature = "invalid_signature_hex_code_1234567890abcdef1234567890abcdef12345678";

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", invalidSignature)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Verify no payment was created
        Optional<Payment> paymentOpt = paymentRepository.findByMerchantIdAndRazorpayPaymentId(merchantA.getId(), "pay_bad_sig");
        assertFalse(paymentOpt.isPresent(), "Payment must NOT be saved on invalid signature");
    }

    @Test
    @DisplayName("Unknown merchant account_id should be rejected with HTTP 400")
    void testUnknownMerchantRejected() throws Exception {
        String payload = createPaymentFailedPayload("acc_nonexistent_999", "pay_unknown_merch", "test@example.com", 50000);
        String signature = "some_signature";

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Multi-tenant isolation: Merchant A signature cannot verify Merchant B's account")
    void testTenantIsolationOnSignature() throws Exception {
        // Payload belongs to Merchant B, but signed using Merchant A's secret
        String payload = createPaymentFailedPayload(ACCOUNT_ID_B, "pay_cross_tenant", "victim@example.com", 50000);
        String signatureFromA = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signatureFromA)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        // Merchant B's database must remain untouched
        Optional<Payment> paymentOpt = paymentRepository.findByMerchantIdAndRazorpayPaymentId(merchantB.getId(), "pay_cross_tenant");
        assertFalse(paymentOpt.isPresent());
    }

    @Test
    @DisplayName("Unsupported or unknown event type should be safely accepted without failure")
    void testUnsupportedEventTypeHandledSafely() throws Exception {
        String payload = """
                {
                  "entity": "event",
                  "account_id": "%s",
                  "event_id": "evt_unknown_999",
                  "event": "subscription.charged",
                  "contains": ["subscription"],
                  "payload": {},
                  "created_at": 1600000000
                }
                """.formatted(ACCOUNT_ID_A);

        String signature = signatureVerifier.calculateHmacSha256(payload, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        Optional<WebhookEvent> eventOpt = webhookEventRepository.findByMerchantIdAndRazorpayEventId(merchantA.getId(), "evt_unknown_999");
        assertTrue(eventOpt.isPresent());
        assertEquals(WebhookProcessingStatus.IGNORED, eventOpt.get().getProcessingStatus());
    }

    @Test
    @DisplayName("Malformed JSON payload should be rejected with HTTP 400")
    void testMalformedJsonPayload() throws Exception {
        String malformedJson = "{ invalid_json: true, ";

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "any_signature")
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
