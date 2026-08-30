package com.recoverai.backend.service;

import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private RecoveryExecutionQueueRepository queueRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    private RecoveryAttemptStateMachine stateMachine;
    private AuditService auditService;
    private PaymentReconciliationService reconciliationService;

    private Merchant merchant;
    private UUID merchantId;
    private Payment failedPayment;
    private Payment capturedPayment;
    private RecoveryCase recoveryCase;
    private UUID caseId;

    @BeforeEach
    void setUp() {
        stateMachine = new RecoveryAttemptStateMachine();
        auditService = new AuditService(auditEventRepository);
        reconciliationService = new PaymentReconciliationService(
                recoveryCaseRepository,
                recoveryAttemptRepository,
                queueRepository,
                paymentRepository,
                stateMachine,
                auditService
        );

        merchantId = UUID.randomUUID();
        merchant = Merchant.builder()
                .id(merchantId)
                .name("Acme Corp")
                .email("acme@test.com")
                .status(MerchantStatus.ACTIVE)
                .build();

        caseId = UUID.randomUUID();
        failedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .razorpayPaymentId("pay_fail_001")
                .razorpayOrderId("order_test_100")
                .amount(new BigDecimal("2500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build();

        capturedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .razorpayPaymentId("pay_success_002")
                .razorpayOrderId("order_test_100")
                .amount(new BigDecimal("2500.00"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(caseId)
                .merchant(merchant)
                .payment(failedPayment)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(new BigDecimal("2500.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .build();
    }

    @Test
    @DisplayName("Direct match: resolves OPEN recovery case, skips scheduled attempt, succeeds in-flight attempt, and completes queue")
    void testDirectMatch_ResolvesCaseAndAttemptsAndQueue() {
        // Arrange
        recoveryCase.setPayment(capturedPayment);
        when(recoveryCaseRepository.findByPaymentIdAndMerchantId(capturedPayment.getId(), merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryCaseRepository.save(any(RecoveryCase.class))).thenAnswer(i -> i.getArgument(0));

        RecoveryAttempt scheduledAttempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build();

        RecoveryAttempt inFlightAttempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .build();

        when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                .thenReturn(List.of(inFlightAttempt, scheduledAttempt));
        when(queueRepository.markPendingItemsCompletedForCase(eq(caseId), eq(merchantId), eq("CASE_TERMINAL_RECOVERED"), any(), any()))
                .thenReturn(1);

        // Act
        Optional<RecoveryCase> result = reconciliationService.reconcilePaymentSuccess(
                merchant, capturedPayment, "payment.captured", "127.0.0.1");

        // Assert
        assertTrue(result.isPresent());
        RecoveryCase reconciled = result.get();
        assertEquals(RecoveryCaseStatus.RECOVERED, reconciled.getStatus());
        assertEquals(new BigDecimal("2500.00"), reconciled.getRecoveredAmount());
        assertNotNull(reconciled.getRecoveredAt());

        // Verify attempts
        assertEquals(RecoveryAttemptStatus.SUCCESS, inFlightAttempt.getStatus());
        assertEquals("PAYMENT_RECONCILED", inFlightAttempt.getResultCode());
        assertNotNull(inFlightAttempt.getCompletedAt());

        assertEquals(RecoveryAttemptStatus.SKIPPED, scheduledAttempt.getStatus());
        assertEquals("CASE_RECOVERED", scheduledAttempt.getResultCode());
        assertNotNull(scheduledAttempt.getCompletedAt());

        verify(recoveryAttemptRepository).save(inFlightAttempt);
        verify(recoveryAttemptRepository).save(scheduledAttempt);

        // Verify queue completion
        verify(queueRepository).markPendingItemsCompletedForCase(eq(caseId), eq(merchantId), eq("CASE_TERMINAL_RECOVERED"), any(), any());

        // Verify audit event
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        AuditEvent audit = auditCaptor.getAllValues().stream()
                .filter(e -> "RECOVERY_PAYMENT_RECONCILED".equals(e.getEventType()))
                .findFirst().orElseThrow();
        assertEquals("RECOVERY_PAYMENT_RECONCILED", audit.getEventType());
        assertEquals(ActorType.WEBHOOK, audit.getActorType());
        assertTrue(audit.getDetails().contains("order_test_100"));
        assertTrue(audit.getDetails().contains("pay_success_002"));
    }

    @Test
    @DisplayName("Order-based match: matches recovery link new payment attempt on existing order to active IN_PROGRESS case")
    void testOrderBasedMatch_MatchesCaseByRazorpayOrderId() {
        recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
        when(recoveryCaseRepository.findByPaymentIdAndMerchantId(capturedPayment.getId(), merchantId))
                .thenReturn(Optional.empty());
        when(recoveryCaseRepository.findActiveByMerchantIdAndRazorpayOrderId(eq(merchantId), eq("order_test_100"), any()))
                .thenReturn(List.of(recoveryCase));
        when(recoveryCaseRepository.save(any(RecoveryCase.class))).thenAnswer(i -> i.getArgument(0));

        RecoveryAttempt sentAttempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.SMART_LINK)
                .status(RecoveryAttemptStatus.SENT)
                .build();

        when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                .thenReturn(List.of(sentAttempt));
        when(queueRepository.markPendingItemsCompletedForCase(eq(caseId), eq(merchantId), eq("CASE_TERMINAL_RECOVERED"), any(), any()))
                .thenReturn(1);

        // Act
        Optional<RecoveryCase> result = reconciliationService.reconcilePaymentSuccess(
                merchant, capturedPayment, "order.paid", "127.0.0.1");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(RecoveryCaseStatus.RECOVERED, result.get().getStatus());
        assertEquals(new BigDecimal("2500.00"), result.get().getRecoveredAmount());

        // Associated failed payment on case is marked CAPTURED
        assertEquals(PaymentStatus.CAPTURED, failedPayment.getStatus());
        verify(paymentRepository).save(failedPayment);

        // Sent attempt is marked SUCCESS
        assertEquals(RecoveryAttemptStatus.SUCCESS, sentAttempt.getStatus());
        verify(recoveryAttemptRepository).save(sentAttempt);
    }

    @Test
    @DisplayName("Idempotency: case already RECOVERED returns immediately without modifying timestamps or re-auditing")
    void testIdempotency_AlreadyRecoveredCaseIsNotReModified() {
        Instant originalRecoveredAt = Instant.now().minusSeconds(3600);
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
        recoveryCase.setRecoveredAt(originalRecoveredAt);
        recoveryCase.setRecoveredAmount(new BigDecimal("2500.00"));

        when(recoveryCaseRepository.findByPaymentIdAndMerchantId(capturedPayment.getId(), merchantId))
                .thenReturn(Optional.of(recoveryCase));

        Optional<RecoveryCase> result = reconciliationService.reconcilePaymentSuccess(
                merchant, capturedPayment, "payment.captured", "127.0.0.1");

        assertTrue(result.isPresent());
        assertEquals(RecoveryCaseStatus.RECOVERED, result.get().getStatus());
        assertEquals(originalRecoveredAt, result.get().getRecoveredAt());
        verify(recoveryCaseRepository, never()).save(any(RecoveryCase.class));
        verify(recoveryAttemptRepository, never()).save(any(RecoveryAttempt.class));
        verify(queueRepository, never()).markPendingItemsCompletedForCase(any(), any(), any(), any(), any());
        verify(auditEventRepository, never()).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("Non-recoverable case: CANCELLED or EXPIRED case is not transitioned to RECOVERED")
    void testNonRecoverableTerminalCase_NotTransitioned() {
        recoveryCase.setStatus(RecoveryCaseStatus.CANCELLED);
        when(recoveryCaseRepository.findByPaymentIdAndMerchantId(capturedPayment.getId(), merchantId))
                .thenReturn(Optional.of(recoveryCase));

        Optional<RecoveryCase> result = reconciliationService.reconcilePaymentSuccess(
                merchant, capturedPayment, "payment.captured", "127.0.0.1");

        assertTrue(result.isPresent());
        assertEquals(RecoveryCaseStatus.CANCELLED, result.get().getStatus());
        verify(recoveryCaseRepository, never()).save(any(RecoveryCase.class));
        verify(queueRepository, never()).markPendingItemsCompletedForCase(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Multi-tenant security: cross-tenant reconciliation attempt throws SecurityException")
    void testCrossTenantReconciliation_ThrowsSecurityException() {
        Merchant merchantB = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Attacker Corp")
                .build();

        assertThrows(SecurityException.class, () -> reconciliationService.reconcileCaseRecovery(
                merchantB, recoveryCase, capturedPayment, new BigDecimal("2500.00"), "payment.captured", "127.0.0.1", null
        ));
    }

    @Test
    @DisplayName("Terminal attempts are preserved: SUCCESS, FAILED, and SKIPPED attempts are not mutated")
    void testTerminalAttempts_AreNotMutated() {
        when(recoveryCaseRepository.findByPaymentIdAndMerchantId(capturedPayment.getId(), merchantId))
                .thenReturn(Optional.of(recoveryCase));
        when(recoveryCaseRepository.save(any(RecoveryCase.class))).thenAnswer(i -> i.getArgument(0));

        RecoveryAttempt failedAttempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .resultCode("INVALID_NUMBER")
                .build();

        RecoveryAttempt alreadySuccessfulAttempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SUCCESS)
                .resultCode("ORIGINAL_SUCCESS")
                .build();

        when(recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId))
                .thenReturn(List.of(failedAttempt, alreadySuccessfulAttempt));
        when(queueRepository.markPendingItemsCompletedForCase(eq(caseId), eq(merchantId), eq("CASE_TERMINAL_RECOVERED"), any(), any()))
                .thenReturn(0);

        reconciliationService.reconcilePaymentSuccess(merchant, capturedPayment, "payment.captured", "127.0.0.1");

        assertEquals(RecoveryAttemptStatus.FAILED, failedAttempt.getStatus());
        assertEquals("INVALID_NUMBER", failedAttempt.getResultCode());
        assertEquals(RecoveryAttemptStatus.SUCCESS, alreadySuccessfulAttempt.getStatus());
        assertEquals("ORIGINAL_SUCCESS", alreadySuccessfulAttempt.getResultCode());

        // Verify save was not called on terminal attempts
        verify(recoveryAttemptRepository, never()).save(failedAttempt);
        verify(recoveryAttemptRepository, never()).save(alreadySuccessfulAttempt);
    }
}
