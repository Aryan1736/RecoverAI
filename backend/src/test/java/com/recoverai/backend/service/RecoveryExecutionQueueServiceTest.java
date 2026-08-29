package com.recoverai.backend.service;

import com.recoverai.backend.config.RecoveryCommunicationProperties;
import com.recoverai.backend.config.RecoveryQueueProperties;
import com.recoverai.backend.entity.AuditEvent;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryExecutionQueueItem;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import com.recoverai.backend.repository.AuditEventRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.service.executor.DefaultRecoveryActionExecutor;
import com.recoverai.backend.service.executor.ExecutionResult;
import com.recoverai.backend.service.executor.RecoveryActionExecutor;
import com.recoverai.backend.service.link.DefaultRecoveryLinkService;
import com.recoverai.backend.service.link.RecoveryLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryExecutionQueueServiceTest {

    @Mock
    private RecoveryExecutionQueueRepository queueRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private RecoveryActionExecutor mockExecutor;

    private DefaultRecoveryActionExecutor defaultActionExecutor;
    private AuditService auditService;
    private RecoveryQueueProperties properties;
    private RecoveryExecutionQueueService queueService;

    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private RecoveryAttempt attempt;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditEventRepository);
        RecoveryCommunicationProperties commProps = new RecoveryCommunicationProperties();
        commProps.setBaseUrl("https://pay.recoverai.io/r/");
        RecoveryLinkService recoveryLinkService = new DefaultRecoveryLinkService(commProps);
        defaultActionExecutor = new DefaultRecoveryActionExecutor(recoveryLinkService);
        properties = new RecoveryQueueProperties(true, 3000L, 25, 3, 300L, "worker-test-1", 300L);

        queueService = new RecoveryExecutionQueueService(
                queueRepository,
                recoveryAttemptRepository,
                recoveryCaseRepository,
                List.of(mockExecutor),
                defaultActionExecutor,
                auditService,
                properties
        );

        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .name("Acme Corp")
                .email("acme@test.com")
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .email("cust@test.com")
                .name("Alice")
                .phone("+919999911111")
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_123")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("1500.00"))
                .build();

        attempt = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("enqueueAttempt should create queue item and record RECOVERY_EXECUTION_QUEUED audit event")
    void shouldEnqueueAttemptSuccessfully() {
        when(queueRepository.findByRecoveryAttemptId(attempt.getId())).thenReturn(Optional.empty());
        when(queueRepository.saveAndFlush(any(RecoveryExecutionQueueItem.class))).thenAnswer(inv -> {
            RecoveryExecutionQueueItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        Instant availableAt = Instant.now();
        RecoveryExecutionQueueItem result = queueService.enqueueAttempt(attempt, availableAt);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RecoveryQueueStatus.READY);
        assertThat(result.getRecoveryAttempt().getId()).isEqualTo(attempt.getId());

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        List<String> eventTypes = auditCaptor.getAllValues().stream().map(AuditEvent::getEventType).toList();
        assertThat(eventTypes).contains("RECOVERY_EXECUTION_QUEUED");
    }

    @Test
    @DisplayName("enqueueAttempt should be idempotent when queue item already exists")
    void shouldReturnExistingQueueItemWhenAlreadyEnqueued() {
        RecoveryExecutionQueueItem existing = RecoveryExecutionQueueItem.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now())
                .build();

        when(queueRepository.findByRecoveryAttemptId(attempt.getId())).thenReturn(Optional.of(existing));

        RecoveryExecutionQueueItem result = queueService.enqueueAttempt(attempt, Instant.now());

        assertThat(result).isSameAs(existing);
        verify(queueRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("enqueueAttempt should handle DataIntegrityViolationException on concurrent enqueue and return existing")
    void shouldHandleConcurrentEnqueueGracefully() {
        RecoveryExecutionQueueItem concurrentItem = RecoveryExecutionQueueItem.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now())
                .build();

        when(queueRepository.findByRecoveryAttemptId(attempt.getId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentItem));
        when(queueRepository.saveAndFlush(any(RecoveryExecutionQueueItem.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key violation"));

        RecoveryExecutionQueueItem result = queueService.enqueueAttempt(attempt, Instant.now());
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(concurrentItem.getId());
    }

    @Test
    @DisplayName("claimItem should return true and record audit event when update succeeds")
    void shouldClaimItemSuccessfully() {
        UUID queueItemId = UUID.randomUUID();
        when(queueRepository.claimItem(eq(queueItemId), eq("worker-test-1"), any(Instant.class)))
                .thenReturn(1);

        RecoveryExecutionQueueItem item = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .build();
        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(item));

        boolean claimed = queueService.claimItem(queueItemId, "worker-test-1");

        assertThat(claimed).isTrue();
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().stream().map(AuditEvent::getEventType))
                .contains("RECOVERY_EXECUTION_CLAIMED");
    }

    @Test
    @DisplayName("claimItem should return false when item is already claimed")
    void shouldReturnFalseWhenClaimFails() {
        UUID queueItemId = UUID.randomUUID();
        when(queueRepository.claimItem(eq(queueItemId), eq("worker-test-1"), any(Instant.class)))
                .thenReturn(0);

        boolean claimed = queueService.claimItem(queueItemId, "worker-test-1");

        assertThat(claimed).isFalse();
        verify(auditEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("processQueueItem should skip execution if RecoveryCase is terminal (RECOVERED)")
    void shouldSkipExecutionWhenCaseIsTerminal() {
        UUID queueItemId = UUID.randomUUID();
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);

        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .build();

        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(queueItem));

        boolean processed = queueService.processQueueItem(queueItemId);

        assertThat(processed).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SKIPPED);
        assertThat(attempt.getResultCode()).isEqualTo("CASE_TERMINAL");
        verify(queueRepository).markCompleted(eq(queueItemId), any(Instant.class));
        verify(mockExecutor, never()).execute(any(), any());

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().stream().map(AuditEvent::getEventType))
                .contains("RECOVERY_EXECUTION_SKIPPED", "RECOVERY_ATTEMPT_SKIPPED");
    }

    @Test
    @DisplayName("processQueueItem should execute attempt and mark queue item COMPLETED on success")
    void shouldExecuteAttemptAndCompleteQueueItem() {
        UUID queueItemId = UUID.randomUUID();
        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .build();

        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(queueItem));
        when(mockExecutor.supports(RecoveryChannel.WHATSAPP)).thenReturn(true);
        when(mockExecutor.execute(any(), any()))
                .thenReturn(ExecutionResult.sent("WHATSAPP_SENT", "WhatsApp sent successfully", "https://pay.recoverai.io/r/abc", "{}"));

        boolean processed = queueService.processQueueItem(queueItemId);

        assertThat(processed).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SENT);
        verify(queueRepository).markProcessing(eq(queueItemId), any(Instant.class));
        verify(queueRepository).markCompleted(eq(queueItemId), any(Instant.class));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().stream().map(AuditEvent::getEventType))
                .contains("RECOVERY_EXECUTION_STARTED", "RECOVERY_EXECUTION_COMPLETED", "RECOVERY_ATTEMPT_SENT");
    }

    @Test
    @DisplayName("processQueueItem should recover case when charge retry succeeds")
    void shouldRecoverCaseWhenChargeRetrySucceeds() {
        UUID queueItemId = UUID.randomUUID();
        attempt.setChannel(RecoveryChannel.RETRY_CHARGE);

        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .build();

        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(queueItem));
        when(mockExecutor.supports(RecoveryChannel.RETRY_CHARGE)).thenReturn(true);
        when(mockExecutor.execute(any(), any()))
                .thenReturn(ExecutionResult.success("PAYMENT_CAPTURED", "Payment charge retry captured", null, "{}"));

        boolean processed = queueService.processQueueItem(queueItemId);

        assertThat(processed).isTrue();
        assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SUCCESS);
        assertThat(recoveryCase.getStatus()).isEqualTo(RecoveryCaseStatus.RECOVERED);
        verify(recoveryCaseRepository, atLeastOnce()).save(recoveryCase);
        verify(queueRepository).markCompleted(eq(queueItemId), any(Instant.class));
    }

    @Test
    @DisplayName("processQueueItem should schedule retry for transient provider error when retries remain")
    void shouldScheduleRetryForTransientError() {
        UUID queueItemId = UUID.randomUUID();
        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .retryCount(0)
                .maxRetries(3)
                .build();

        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(queueItem));
        when(mockExecutor.supports(RecoveryChannel.WHATSAPP)).thenReturn(true);
        when(mockExecutor.execute(any(), any()))
                .thenThrow(new RuntimeException("Connection timeout while contacting provider"));

        boolean processed = queueService.processQueueItem(queueItemId);

        assertThat(processed).isFalse();
        verify(queueRepository).rescheduleForRetry(eq(queueItemId), any(Instant.class), eq("EXECUTION_ERROR"), any(), any(Instant.class));
        verify(queueRepository, never()).moveToDeadLetter(any(), any(), any(), any());

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().stream().map(AuditEvent::getEventType))
                .contains("RECOVERY_EXECUTION_RETRY_SCHEDULED");
    }

    @Test
    @DisplayName("processQueueItem should move to DEAD_LETTER when retries are exhausted")
    void shouldMoveToDeadLetterWhenRetriesExhausted() {
        UUID queueItemId = UUID.randomUUID();
        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .retryCount(3)
                .maxRetries(3)
                .build();

        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(queueItem));
        when(mockExecutor.supports(RecoveryChannel.WHATSAPP)).thenReturn(true);
        when(mockExecutor.execute(any(), any()))
                .thenThrow(new RuntimeException("Gateway 504 Timeout"));

        boolean processed = queueService.processQueueItem(queueItemId);

        assertThat(processed).isFalse();
        verify(queueRepository).moveToDeadLetter(eq(queueItemId), eq("EXECUTION_ERROR"), any(), any(Instant.class));
        assertThat(attempt.getStatus()).isEqualTo(RecoveryAttemptStatus.FAILED);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().stream().map(AuditEvent::getEventType))
                .contains("RECOVERY_EXECUTION_DEAD_LETTERED", "RECOVERY_ATTEMPT_FAILED");
    }

    @Test
    @DisplayName("processQueueItem should immediately dead-letter permanent business failures without retrying")
    void shouldDeadLetterPermanentFailureImmediately() {
        UUID queueItemId = UUID.randomUUID();
        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .retryCount(0)
                .maxRetries(3)
                .build();

        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(queueItem));
        when(mockExecutor.supports(RecoveryChannel.WHATSAPP)).thenReturn(true);
        when(mockExecutor.execute(any(), any()))
                .thenReturn(ExecutionResult.failed("INVALID_CUSTOMER_CONTACT", "Phone number is blacklisted or invalid", null, "{}"));

        boolean processed = queueService.processQueueItem(queueItemId);

        assertThat(processed).isFalse();
        verify(queueRepository).moveToDeadLetter(eq(queueItemId), eq("INVALID_CUSTOMER_CONTACT"), any(), any(Instant.class));
        verify(queueRepository, never()).rescheduleForRetry(any(), any(), any(), any(), any());

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository, atLeastOnce()).save(auditCaptor.capture());
        assertThat(auditCaptor.getAllValues().stream().map(AuditEvent::getEventType))
                .contains("RECOVERY_EXECUTION_FAILED", "RECOVERY_ATTEMPT_FAILED");
    }

    @Test
    @DisplayName("requeueStaleClaims should find and requeue abandoned claimed items")
    void shouldRequeueStaleClaims() {
        UUID stale1 = UUID.randomUUID();
        UUID stale2 = UUID.randomUUID();
        when(queueRepository.findStaleClaimIds(any(Instant.class))).thenReturn(List.of(stale1, stale2));
        when(queueRepository.requeueStaleClaim(eq(stale1), any(Instant.class))).thenReturn(1);
        when(queueRepository.requeueStaleClaim(eq(stale2), any(Instant.class))).thenReturn(1);

        int count = queueService.requeueStaleClaims();

        assertThat(count).isEqualTo(2);
        verify(queueRepository).requeueStaleClaim(eq(stale1), any(Instant.class));
        verify(queueRepository).requeueStaleClaim(eq(stale2), any(Instant.class));
    }

    @Test
    @DisplayName("processQueueItem should reject cross-tenant queue execution and move to dead-letter")
    void shouldRejectCrossTenantExecution() {
        UUID queueItemId = UUID.randomUUID();
        Merchant otherMerchant = Merchant.builder().id(UUID.randomUUID()).name("Other").build();

        // Attempt belongs to otherMerchant
        attempt.setMerchant(otherMerchant);

        RecoveryExecutionQueueItem queueItem = RecoveryExecutionQueueItem.builder()
                .id(queueItemId)
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .build();

        when(queueRepository.findById(queueItemId)).thenReturn(Optional.of(queueItem));

        boolean processed = queueService.processQueueItem(queueItemId);

        assertThat(processed).isFalse();
        verify(queueRepository).moveToDeadLetter(eq(queueItemId), eq("TENANT_MISMATCH"), any(), any(Instant.class));
        verify(mockExecutor, never()).execute(any(), any());
    }
}
