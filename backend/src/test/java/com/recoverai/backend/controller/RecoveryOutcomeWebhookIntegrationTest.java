package com.recoverai.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.dto.webhook.RecoveryOutcomeWebhookRequest;
import com.recoverai.backend.entity.AuditEvent;
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
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryOutcomeEventRepository;
import com.recoverai.backend.security.RecoveryOutcomeSignatureVerifier;
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
import java.util.UUID;

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
class RecoveryOutcomeWebhookIntegrationTest {

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
    private RecoveryOutcomeEventRepository recoveryOutcomeEventRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private RecoveryOutcomeSignatureVerifier signatureVerifier;

    @Autowired
    private ObjectMapper objectMapper;

    private Merchant merchantA;
    private Merchant merchantB;
    private RecoveryAttempt attemptA;
    private RecoveryAttempt attemptB;
    private RecoveryCase caseA;
    private RecoveryCase caseB;
    private Payment paymentA;

    private static final String SECRET_A = "webhook_secret_for_merchant_a_123";
    private static final String SECRET_B = "webhook_secret_for_merchant_b_456";

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Alpha Corp")
                .email("alpha@alphacorp.com")
                .webhookSecret(SECRET_A)
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Beta Store")
                .email("beta@betastore.com")
                .webhookSecret(SECRET_B)
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer customerA = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchantA)
                .name("Customer A")
                .email("cust_a@domain.com")
                .build());

        paymentA = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_alpha_1001")
                .amount(new BigDecimal("2999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        caseA = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(paymentA)
                .customer(customerA)
                .status(RecoveryCaseStatus.IN_PROGRESS)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .recoveredAmount(BigDecimal.ZERO)
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
                .name("Customer B")
                .email("cust_b@domain.com")
                .build());

        Payment paymentB = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchantB)
                .customer(customerB)
                .razorpayPaymentId("pay_beta_2001")
                .amount(new BigDecimal("4999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        caseB = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchantB)
                .payment(paymentB)
                .customer(customerB)
                .status(RecoveryCaseStatus.IN_PROGRESS)
                .priority(RecoveryPriority.MEDIUM)
                .estimatedRecoverableAmount(new BigDecimal("4999.00"))
                .recoveredAmount(BigDecimal.ZERO)
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
    @DisplayName("Valid SUCCESS webhook should reconcile attempt to SUCCESS and case to RECOVERED with trusted amount")
    void testValidSuccessOutcome() throws Exception {
        RecoveryOutcomeWebhookRequest payload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_success_101")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptA.getId())
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .providerReference("wa_msg_98765")
                .occurredAt(Instant.now())
                .resultCode("PAID_VIA_SMART_LINK")
                .resultMessage("Customer paid full invoice via link")
                .build();

        String rawJson = objectMapper.writeValueAsString(payload);
        String signature = signatureVerifier.calculateHmacSha256(rawJson, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", signature)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        // Verify RecoveryAttempt status
        RecoveryAttempt updatedAttempt = recoveryAttemptRepository.findById(attemptA.getId()).orElseThrow();
        assertEquals(RecoveryAttemptStatus.SUCCESS, updatedAttempt.getStatus());
        assertEquals("PAID_VIA_SMART_LINK", updatedAttempt.getResultCode());
        assertNotNull(updatedAttempt.getCompletedAt());

        // Verify RecoveryCase status & trusted amount integrity
        RecoveryCase updatedCase = recoveryCaseRepository.findById(caseA.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.RECOVERED, updatedCase.getStatus());
        assertEquals(new BigDecimal("2999.00"), updatedCase.getRecoveredAmount());
        assertNotNull(updatedCase.getRecoveredAt());

        // Verify Payment status
        Payment updatedPayment = paymentRepository.findById(paymentA.getId()).orElseThrow();
        assertEquals(PaymentStatus.CAPTURED, updatedPayment.getStatus());

        // Verify RecoveryOutcomeEvent
        Optional<RecoveryOutcomeEvent> outcomeEvent = recoveryOutcomeEventRepository
                .findByMerchantIdAndProviderAndProviderEventId(merchantA.getId(), "WHATSAPP", "evt_wa_success_101");
        assertTrue(outcomeEvent.isPresent());
        assertEquals(WebhookProcessingStatus.PROCESSED, outcomeEvent.get().getProcessingStatus());

        // Verify Audit Events
        List<AuditEvent> audits = auditEventRepository.findByMerchantId(merchantA.getId());
        assertTrue(audits.stream().anyMatch(a -> "RECOVERY_OUTCOME_RECEIVED".equals(a.getEventType())));
        assertTrue(audits.stream().anyMatch(a -> "RECOVERY_ATTEMPT_SUCCEEDED".equals(a.getEventType())));
        assertTrue(audits.stream().anyMatch(a -> "RECOVERY_OUTCOME_PROCESSED".equals(a.getEventType())));
    }

    @Test
    @DisplayName("Intermediate outcome (CLICKED) updates attempt and leaves case IN_PROGRESS")
    void testIntermediateOutcomeClicked() throws Exception {
        RecoveryOutcomeWebhookRequest payload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_clicked_102")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptA.getId())
                .outcomeStatus(RecoveryAttemptStatus.CLICKED)
                .provider("WHATSAPP")
                .build();

        String rawJson = objectMapper.writeValueAsString(payload);
        String signature = signatureVerifier.calculateHmacSha256(rawJson, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", signature)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        RecoveryAttempt updatedAttempt = recoveryAttemptRepository.findById(attemptA.getId()).orElseThrow();
        assertEquals(RecoveryAttemptStatus.CLICKED, updatedAttempt.getStatus());

        RecoveryCase updatedCase = recoveryCaseRepository.findById(caseA.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.IN_PROGRESS, updatedCase.getStatus());
        assertEquals(BigDecimal.ZERO, updatedCase.getRecoveredAmount());
    }

    @Test
    @DisplayName("Failed outcome updates attempt to FAILED and leaves case IN_PROGRESS (not recovered)")
    void testFailedOutcome() throws Exception {
        RecoveryOutcomeWebhookRequest payload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_failed_103")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptA.getId())
                .outcomeStatus(RecoveryAttemptStatus.FAILED)
                .provider("WHATSAPP")
                .resultCode("FAILED_TO_DELIVER")
                .resultMessage("User blocked the WhatsApp business number")
                .build();

        String rawJson = objectMapper.writeValueAsString(payload);
        String signature = signatureVerifier.calculateHmacSha256(rawJson, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", signature)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        RecoveryAttempt updatedAttempt = recoveryAttemptRepository.findById(attemptA.getId()).orElseThrow();
        assertEquals(RecoveryAttemptStatus.FAILED, updatedAttempt.getStatus());
        assertEquals("FAILED_TO_DELIVER", updatedAttempt.getResultCode());

        RecoveryCase updatedCase = recoveryCaseRepository.findById(caseA.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.IN_PROGRESS, updatedCase.getStatus());
    }

    @Test
    @DisplayName("Duplicate webhook returns accepted without duplicate processing or audit events")
    void testDuplicateWebhookIdempotency() throws Exception {
        RecoveryOutcomeWebhookRequest payload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_dup_104")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptA.getId())
                .outcomeStatus(RecoveryAttemptStatus.DELIVERED)
                .provider("WHATSAPP")
                .build();

        String rawJson = objectMapper.writeValueAsString(payload);
        String signature = signatureVerifier.calculateHmacSha256(rawJson, SECRET_A);

        // 1st delivery
        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", signature)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        // 2nd delivery (Duplicate)
        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", signature)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("accepted"));

        List<AuditEvent> duplicateAudits = auditEventRepository.findByMerchantId(merchantA.getId()).stream()
                .filter(a -> "RECOVERY_OUTCOME_DUPLICATE".equals(a.getEventType()))
                .toList();

        assertEquals(1, duplicateAudits.size());
    }

    @Test
    @DisplayName("Invalid signature is rejected with 401 Unauthorized")
    void testInvalidSignature() throws Exception {
        RecoveryOutcomeWebhookRequest payload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_105")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptA.getId())
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawJson = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", "invalid_signature_hex")
                        .content(rawJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Missing signature header is rejected with 401 Unauthorized")
    void testMissingSignature() throws Exception {
        RecoveryOutcomeWebhookRequest payload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_106")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptA.getId())
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawJson = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tenant Isolation: Merchant A cannot update Merchant B's attempt")
    void testTenantIsolation_CrossTenantRejection() throws Exception {
        // Merchant A signs a payload attempting to modify Merchant B's attempt
        RecoveryOutcomeWebhookRequest crossTenantPayload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_cross_107")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptB.getId()) // Belongs to Merchant B!
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawJson = objectMapper.writeValueAsString(crossTenantPayload);
        String signature = signatureVerifier.calculateHmacSha256(rawJson, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", signature)
                        .content(rawJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Ensure Merchant B's attempt and case were NOT modified
        RecoveryAttempt merchantBAttempt = recoveryAttemptRepository.findById(attemptB.getId()).orElseThrow();
        assertEquals(RecoveryAttemptStatus.SENT, merchantBAttempt.getStatus());

        RecoveryCase merchantBCase = recoveryCaseRepository.findById(caseB.getId()).orElseThrow();
        assertEquals(RecoveryCaseStatus.IN_PROGRESS, merchantBCase.getStatus());
        assertEquals(BigDecimal.ZERO, merchantBCase.getRecoveredAmount());
    }

    @Test
    @DisplayName("Invalid backward state transition is rejected with 400 Bad Request")
    void testInvalidBackwardStateTransition() throws Exception {
        attemptA.setStatus(RecoveryAttemptStatus.SUCCESS);
        recoveryAttemptRepository.saveAndFlush(attemptA);

        // Attempting to move from SUCCESS back to IN_FLIGHT
        RecoveryOutcomeWebhookRequest invalidPayload = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_invalid_trans_108")
                .merchantId(merchantA.getId())
                .recoveryAttemptId(attemptA.getId())
                .outcomeStatus(RecoveryAttemptStatus.IN_FLIGHT)
                .provider("WHATSAPP")
                .build();

        String rawJson = objectMapper.writeValueAsString(invalidPayload);
        String signature = signatureVerifier.calculateHmacSha256(rawJson, SECRET_A);

        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", signature)
                        .content(rawJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Malformed JSON returns 400 Bad Request")
    void testMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/recovery-outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Recovery-Signature", "any_sig")
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
