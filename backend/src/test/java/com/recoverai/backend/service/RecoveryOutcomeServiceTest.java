package com.recoverai.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.backend.dto.webhook.RecoveryOutcomeWebhookRequest;
import com.recoverai.backend.dto.webhook.WebhookResponse;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryOutcomeEvent;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.WebhookProcessingStatus;
import com.recoverai.backend.exception.InvalidRecoveryAttemptStateException;
import com.recoverai.backend.exception.MerchantResolutionException;
import com.recoverai.backend.exception.RecoveryAttemptNotFoundException;
import com.recoverai.backend.exception.WebhookProcessingException;
import com.recoverai.backend.exception.WebhookSignatureException;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryOutcomeEventRepository;
import com.recoverai.backend.security.RecoveryOutcomeSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryOutcomeServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RecoveryOutcomeEventRepository recoveryOutcomeEventRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditService auditService;
    private RecoveryOutcomeSignatureVerifier signatureVerifier;
    private RecoveryAttemptStateMachine stateMachine;
    private ObjectMapper objectMapper;
    private RecoveryOutcomeService recoveryOutcomeService;

    private Merchant merchant;
    private RecoveryCase recoveryCase;
    private RecoveryAttempt recoveryAttempt;
    private Payment payment;
    private UUID merchantId;
    private UUID caseId;
    private UUID attemptId;
    private static final String WEBHOOK_SECRET = "test_secret_12345";

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditEventRepository);
        signatureVerifier = new RecoveryOutcomeSignatureVerifier();
        stateMachine = new RecoveryAttemptStateMachine();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        recoveryOutcomeService = new RecoveryOutcomeService(
                merchantRepository,
                recoveryAttemptRepository,
                recoveryCaseRepository,
                paymentRepository,
                recoveryOutcomeEventRepository,
                signatureVerifier,
                stateMachine,
                auditService,
                objectMapper
        );

        merchantId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        attemptId = UUID.randomUUID();

        merchant = Merchant.builder()
                .id(merchantId)
                .name("Acme Corp")
                .email("acme@test.com")
                .webhookSecret(WEBHOOK_SECRET)
                .status(MerchantStatus.ACTIVE)
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .razorpayPaymentId("pay_12345")
                .amount(new BigDecimal("1250.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(caseId)
                .merchant(merchant)
                .payment(payment)
                .status(RecoveryCaseStatus.IN_PROGRESS)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("1250.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .build();

        recoveryAttempt = RecoveryAttempt.builder()
                .id(attemptId)
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SENT)
                .build();
    }

    @Test
    @DisplayName("Process successful outcome: transitions attempt to SUCCESS, case to RECOVERED with trusted amount")
    void testProcessOutcome_SuccessReconciliation() throws Exception {
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_success_1")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .providerReference("msg_sid_999")
                .occurredAt(Instant.now())
                .resultCode("DELIVERY_PAID")
                .resultMessage("Customer paid via WhatsApp smart link")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);
        String signature = signatureVerifier.calculateHmacSha256(rawPayload, WEBHOOK_SECRET);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(recoveryOutcomeEventRepository.findByMerchantIdAndProviderAndProviderEventId(merchantId, "WHATSAPP", "evt_wa_success_1"))
                .thenReturn(Optional.empty());
        when(recoveryOutcomeEventRepository.findByMerchantIdAndPayloadHash(eq(merchantId), anyString()))
                .thenReturn(Optional.empty());
        when(recoveryAttemptRepository.findByIdAndMerchantId(attemptId, merchantId)).thenReturn(Optional.of(recoveryAttempt));
        when(recoveryOutcomeEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookResponse response = recoveryOutcomeService.processOutcomeWebhook(rawPayload, signature, "127.0.0.1");

        assertNotNull(response);
        assertEquals("accepted", response.getStatus());

        // Verify attempt updated
        assertEquals(RecoveryAttemptStatus.SUCCESS, recoveryAttempt.getStatus());
        assertEquals("DELIVERY_PAID", recoveryAttempt.getResultCode());
        assertNotNull(recoveryAttempt.getCompletedAt());
        verify(recoveryAttemptRepository).save(recoveryAttempt);

        // Verify case updated to RECOVERED with trusted amount
        assertEquals(RecoveryCaseStatus.RECOVERED, recoveryCase.getStatus());
        assertEquals(new BigDecimal("1250.00"), recoveryCase.getRecoveredAmount());
        assertNotNull(recoveryCase.getRecoveredAt());
        verify(recoveryCaseRepository).save(recoveryCase);

        // Verify payment updated
        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        verify(paymentRepository).save(payment);

        // Verify audit event repository interactions
        verify(auditEventRepository, atLeastOnce()).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("Process intermediate outcome (DELIVERED): attempt is DELIVERED, case remains IN_PROGRESS")
    void testProcessOutcome_IntermediateStatus() throws Exception {
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_del_1")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.DELIVERED)
                .provider("WHATSAPP")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);
        String signature = signatureVerifier.calculateHmacSha256(rawPayload, WEBHOOK_SECRET);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(recoveryOutcomeEventRepository.findByMerchantIdAndProviderAndProviderEventId(merchantId, "WHATSAPP", "evt_wa_del_1"))
                .thenReturn(Optional.empty());
        when(recoveryOutcomeEventRepository.findByMerchantIdAndPayloadHash(eq(merchantId), anyString()))
                .thenReturn(Optional.empty());
        when(recoveryAttemptRepository.findByIdAndMerchantId(attemptId, merchantId)).thenReturn(Optional.of(recoveryAttempt));
        when(recoveryOutcomeEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookResponse response = recoveryOutcomeService.processOutcomeWebhook(rawPayload, signature, "127.0.0.1");

        assertNotNull(response);
        assertEquals("accepted", response.getStatus());

        assertEquals(RecoveryAttemptStatus.DELIVERED, recoveryAttempt.getStatus());
        assertEquals(RecoveryCaseStatus.IN_PROGRESS, recoveryCase.getStatus());
        assertEquals(BigDecimal.ZERO, recoveryCase.getRecoveredAmount());

        verify(recoveryAttemptRepository).save(recoveryAttempt);
        verify(auditEventRepository, atLeastOnce()).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("Process failed outcome: attempt is FAILED, case remains IN_PROGRESS (not recovered)")
    void testProcessOutcome_FailedAttempt() throws Exception {
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_fail_1")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.FAILED)
                .provider("WHATSAPP")
                .resultCode("UNDELIVERABLE_NUMBER")
                .resultMessage("Phone number is not registered on WhatsApp")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);
        String signature = signatureVerifier.calculateHmacSha256(rawPayload, WEBHOOK_SECRET);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(recoveryOutcomeEventRepository.findByMerchantIdAndProviderAndProviderEventId(merchantId, "WHATSAPP", "evt_wa_fail_1"))
                .thenReturn(Optional.empty());
        when(recoveryOutcomeEventRepository.findByMerchantIdAndPayloadHash(eq(merchantId), anyString()))
                .thenReturn(Optional.empty());
        when(recoveryAttemptRepository.findByIdAndMerchantId(attemptId, merchantId)).thenReturn(Optional.of(recoveryAttempt));
        when(recoveryOutcomeEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookResponse response = recoveryOutcomeService.processOutcomeWebhook(rawPayload, signature, "127.0.0.1");

        assertNotNull(response);
        assertEquals("accepted", response.getStatus());

        assertEquals(RecoveryAttemptStatus.FAILED, recoveryAttempt.getStatus());
        assertEquals(RecoveryCaseStatus.IN_PROGRESS, recoveryCase.getStatus());
        assertEquals(BigDecimal.ZERO, recoveryCase.getRecoveredAmount());

        verify(recoveryAttemptRepository).save(recoveryAttempt);
        verify(auditEventRepository, atLeastOnce()).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("Duplicate already processed event returns accepted without re-processing mutations")
    void testProcessOutcome_DuplicateEventSkipped() throws Exception {
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_dup_1")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);
        String signature = signatureVerifier.calculateHmacSha256(rawPayload, WEBHOOK_SECRET);

        RecoveryOutcomeEvent existing = RecoveryOutcomeEvent.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryAttempt(recoveryAttempt)
                .provider("WHATSAPP")
                .providerEventId("evt_wa_dup_1")
                .processingStatus(WebhookProcessingStatus.PROCESSED)
                .build();

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(recoveryOutcomeEventRepository.findByMerchantIdAndProviderAndProviderEventId(merchantId, "WHATSAPP", "evt_wa_dup_1"))
                .thenReturn(Optional.of(existing));

        WebhookResponse response = recoveryOutcomeService.processOutcomeWebhook(rawPayload, signature, "127.0.0.1");

        assertNotNull(response);
        assertEquals("accepted", response.getStatus());

        // Ensure attempt and case were NOT mutated
        verify(recoveryAttemptRepository, never()).save(any());
        verify(recoveryCaseRepository, never()).save(any());
        verify(auditEventRepository, atLeastOnce()).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("Invalid signature throws WebhookSignatureException and records REJECTED audit")
    void testProcessOutcome_InvalidSignatureRejected() throws Exception {
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_wa_bad_sig")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);
        String signature = "invalid_signature_hex_12345";

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        assertThrows(WebhookSignatureException.class, () ->
                recoveryOutcomeService.processOutcomeWebhook(rawPayload, signature, "127.0.0.1"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        assertEquals("RECOVERY_OUTCOME_REJECTED", captor.getValue().getEventType());
    }

    @Test
    @DisplayName("Unknown merchant throws MerchantResolutionException")
    void testProcessOutcome_UnknownMerchant() throws Exception {
        UUID unknownMerchantId = UUID.randomUUID();
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_1")
                .merchantId(unknownMerchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);

        when(merchantRepository.findById(unknownMerchantId)).thenReturn(Optional.empty());

        assertThrows(MerchantResolutionException.class, () ->
                recoveryOutcomeService.processOutcomeWebhook(rawPayload, "sig", "127.0.0.1"));
    }

    @Test
    @DisplayName("Inactive merchant throws MerchantResolutionException")
    void testProcessOutcome_InactiveMerchant() throws Exception {
        merchant.setStatus(MerchantStatus.INACTIVE);
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_1")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));

        assertThrows(MerchantResolutionException.class, () ->
                recoveryOutcomeService.processOutcomeWebhook(rawPayload, "sig", "127.0.0.1"));
    }

    @Test
    @DisplayName("Cross-tenant attempt lookup throws RecoveryAttemptNotFoundException")
    void testProcessOutcome_CrossTenantAttempt() throws Exception {
        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_cross_tenant")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.SUCCESS)
                .provider("WHATSAPP")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);
        String signature = signatureVerifier.calculateHmacSha256(rawPayload, WEBHOOK_SECRET);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(recoveryOutcomeEventRepository.findByMerchantIdAndProviderAndProviderEventId(merchantId, "WHATSAPP", "evt_cross_tenant"))
                .thenReturn(Optional.empty());
        when(recoveryOutcomeEventRepository.findByMerchantIdAndPayloadHash(eq(merchantId), anyString()))
                .thenReturn(Optional.empty());

        // Merchant A cannot access attempt because repository returns empty for merchantId
        when(recoveryAttemptRepository.findByIdAndMerchantId(attemptId, merchantId)).thenReturn(Optional.empty());

        assertThrows(RecoveryAttemptNotFoundException.class, () ->
                recoveryOutcomeService.processOutcomeWebhook(rawPayload, signature, "127.0.0.1"));
    }

    @Test
    @DisplayName("Invalid state transition throws InvalidRecoveryAttemptStateException")
    void testProcessOutcome_InvalidStateTransition() throws Exception {
        recoveryAttempt.setStatus(RecoveryAttemptStatus.SUCCESS); // Terminal

        RecoveryOutcomeWebhookRequest req = RecoveryOutcomeWebhookRequest.builder()
                .providerEventId("evt_invalid_transition")
                .merchantId(merchantId)
                .recoveryAttemptId(attemptId)
                .outcomeStatus(RecoveryAttemptStatus.IN_FLIGHT) // Backward transition from SUCCESS to IN_FLIGHT
                .provider("WHATSAPP")
                .build();

        String rawPayload = objectMapper.writeValueAsString(req);
        String signature = signatureVerifier.calculateHmacSha256(rawPayload, WEBHOOK_SECRET);

        when(merchantRepository.findById(merchantId)).thenReturn(Optional.of(merchant));
        when(recoveryOutcomeEventRepository.findByMerchantIdAndProviderAndProviderEventId(merchantId, "WHATSAPP", "evt_invalid_transition"))
                .thenReturn(Optional.empty());
        when(recoveryOutcomeEventRepository.findByMerchantIdAndPayloadHash(eq(merchantId), anyString()))
                .thenReturn(Optional.empty());
        when(recoveryAttemptRepository.findByIdAndMerchantId(attemptId, merchantId)).thenReturn(Optional.of(recoveryAttempt));
        when(recoveryOutcomeEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(InvalidRecoveryAttemptStateException.class, () ->
                recoveryOutcomeService.processOutcomeWebhook(rawPayload, signature, "127.0.0.1"));
    }

    @Test
    @DisplayName("Malformed JSON payload throws WebhookProcessingException")
    void testProcessOutcome_MalformedJson() {
        String invalidJson = "{ bad json ";

        assertThrows(WebhookProcessingException.class, () ->
                recoveryOutcomeService.processOutcomeWebhook(invalidJson, "sig", "127.0.0.1"));
    }
}
