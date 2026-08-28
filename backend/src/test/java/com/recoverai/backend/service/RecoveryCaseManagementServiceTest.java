package com.recoverai.backend.service;

import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseDetailResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseResponseDto;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryCaseManagementServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Mock
    private AgentDecisionRepository agentDecisionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RecoveryCaseManagementService recoveryCaseManagementService;

    private UUID merchantId;
    private Merchant merchant;
    private Customer customer;
    private Payment payment;
    private RecoveryCase recoveryCase;
    private RecoveryAttempt attempt1;
    private RecoveryAttempt attempt2;
    private AgentDecision agentDecision;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        merchant = Merchant.builder()
                .id(merchantId)
                .name("Alpha Store")
                .email("alpha@test.com")
                .status(MerchantStatus.ACTIVE)
                .build();

        customer = Customer.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .name("John Doe")
                .email("john@example.com")
                .phone("+919876543210")
                .build();

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_123456")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
                .build();

        recoveryCase = RecoveryCase.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .payment(payment)
                .customer(customer)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("PAYMENT_DECLINED")
                .estimatedRecoverableAmount(new BigDecimal("1500.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .build();

        attempt1 = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .build();

        attempt2 = RecoveryAttempt.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .build();

        agentDecision = AgentDecision.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .recoveryCase(recoveryCase)
                .recommendedAction("SEND_DISCOUNT_LINK")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.8500"))
                .reasoning("High probability customer")
                .modelName("gemini-3.7-flash")
                .build();
    }

    @Test
    @DisplayName("listRecoveryCases returns paginated recovery cases")
    void testListRecoveryCases() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecoveryCase> pagedCases = new PageImpl<>(List.of(recoveryCase), pageable, 1);

        when(recoveryCaseRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(pagedCases);

        Page<RecoveryCaseResponseDto> result = recoveryCaseManagementService.listRecoveryCases(
                merchantId, RecoveryCaseStatus.OPEN, RecoveryPriority.HIGH, "PAYMENT_DECLINED", pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(recoveryCase.getId(), result.getContent().get(0).getId());
        assertEquals("PAYMENT_DECLINED", result.getContent().get(0).getFailureReasonCategory());
        assertEquals(new BigDecimal("1500.00"), result.getContent().get(0).getEstimatedRecoverableAmount());
    }

    @Test
    @DisplayName("getRecoveryCaseDetails returns complete case details including attempts and AI diagnosis")
    void testGetRecoveryCaseDetails() {
        UUID caseId = recoveryCase.getId();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.findByRecoveryCaseIdAndMerchantIdOrderByAttemptNumberAsc(caseId, merchantId))
                .thenReturn(List.of(attempt1, attempt2));
        when(agentDecisionRepository.findFirstByRecoveryCaseIdOrderByCreatedAtDesc(caseId))
                .thenReturn(Optional.of(agentDecision));

        RecoveryCaseDetailResponseDto result = recoveryCaseManagementService.getRecoveryCaseDetails(merchantId, caseId);

        assertNotNull(result);
        assertEquals(caseId, result.getId());
        assertEquals(RecoveryCaseStatus.OPEN, result.getStatus());
        assertEquals(RecoveryPriority.HIGH, result.getPriority());

        assertNotNull(result.getPayment());
        assertEquals(payment.getId(), result.getPayment().getId());
        assertEquals("pay_123456", result.getPayment().getRazorpayPaymentId());

        assertNotNull(result.getCustomer());
        assertEquals("John Doe", result.getCustomer().getName());
        assertEquals("john@example.com", result.getCustomer().getEmail());

        assertEquals(2, result.getAttempts().size());
        assertEquals(1, result.getAttempts().get(0).getAttemptNumber());
        assertEquals(2, result.getAttempts().get(1).getAttemptNumber());

        assertNotNull(result.getLatestDiagnosis());
        assertEquals("SEND_DISCOUNT_LINK", result.getLatestDiagnosis().getRecommendedAction());
        assertEquals("gemini-3.7-flash", result.getLatestDiagnosis().getModelName());
    }

    @Test
    @DisplayName("getRecoveryCaseDetails throws RecoveryCaseNotFoundException when case is not found or cross-tenant")
    void testGetRecoveryCaseDetailsNotFound() {
        UUID caseId = UUID.randomUUID();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.empty());

        assertThrows(RecoveryCaseNotFoundException.class, () ->
                recoveryCaseManagementService.getRecoveryCaseDetails(merchantId, caseId));
    }

    @Test
    @DisplayName("getRecoveryCaseAttempts returns attempts ordered by attempt number")
    void testGetRecoveryCaseAttempts() {
        UUID caseId = recoveryCase.getId();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.of(recoveryCase));
        when(recoveryAttemptRepository.findByRecoveryCaseIdAndMerchantIdOrderByAttemptNumberAsc(caseId, merchantId))
                .thenReturn(List.of(attempt1, attempt2));

        List<RecoveryAttemptResponseDto> result = recoveryCaseManagementService.getRecoveryCaseAttempts(merchantId, caseId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getAttemptNumber());
        assertEquals(2, result.get(1).getAttemptNumber());
    }

    @Test
    @DisplayName("getRecoveryCaseAttempts throws RecoveryCaseNotFoundException when case is missing")
    void testGetRecoveryCaseAttemptsNotFound() {
        UUID caseId = UUID.randomUUID();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.empty());

        assertThrows(RecoveryCaseNotFoundException.class, () ->
                recoveryCaseManagementService.getRecoveryCaseAttempts(merchantId, caseId));
    }

    @Test
    @DisplayName("cancelRecoveryCase cancels OPEN case and skips scheduled attempts")
    void testCancelRecoveryCaseOpen() {
        UUID caseId = recoveryCase.getId();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.of(recoveryCase));
        when(recoveryCaseRepository.save(any(RecoveryCase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recoveryAttemptRepository.findByRecoveryCaseIdAndStatus(caseId, RecoveryAttemptStatus.SCHEDULED))
                .thenReturn(List.of(attempt2));

        RecoveryCaseResponseDto response = recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId);

        assertNotNull(response);
        assertEquals(RecoveryCaseStatus.CANCELLED, response.getStatus());
        assertNotNull(response.getClosedAt());

        // Verify attempt2 was marked SKIPPED
        assertEquals(RecoveryAttemptStatus.SKIPPED, attempt2.getStatus());
        assertEquals("CASE_CANCELLED", attempt2.getResultCode());
        verify(recoveryAttemptRepository).save(attempt2);

        // Verify audit event recorded
        verify(auditService).recordEvent(eq(merchant), eq("RECOVERY_CASE_CANCELLED"), eq(ActorType.USER),
                eq("MerchantDashboard"), eq("RecoveryCase"), eq(caseId.toString()), eq("CANCEL_CASE"), any(), any());
    }

    @Test
    @DisplayName("cancelRecoveryCase cancels IN_PROGRESS case")
    void testCancelRecoveryCaseInProgress() {
        recoveryCase.setStatus(RecoveryCaseStatus.IN_PROGRESS);
        UUID caseId = recoveryCase.getId();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.of(recoveryCase));
        when(recoveryCaseRepository.save(any(RecoveryCase.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recoveryAttemptRepository.findByRecoveryCaseIdAndStatus(caseId, RecoveryAttemptStatus.SCHEDULED))
                .thenReturn(List.of());

        RecoveryCaseResponseDto response = recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId);

        assertNotNull(response);
        assertEquals(RecoveryCaseStatus.CANCELLED, response.getStatus());
        assertNotNull(response.getClosedAt());
    }

    @Test
    @DisplayName("cancelRecoveryCase rejects RECOVERED case")
    void testCancelRecoveryCaseRejectRecovered() {
        recoveryCase.setStatus(RecoveryCaseStatus.RECOVERED);
        UUID caseId = recoveryCase.getId();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.of(recoveryCase));

        InvalidRecoveryCaseStateException ex = assertThrows(InvalidRecoveryCaseStateException.class, () ->
                recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId));
        assertEquals("Cannot cancel recovery case in status: RECOVERED", ex.getMessage());
        verify(recoveryCaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelRecoveryCase rejects EXPIRED case")
    void testCancelRecoveryCaseRejectExpired() {
        recoveryCase.setStatus(RecoveryCaseStatus.EXPIRED);
        UUID caseId = recoveryCase.getId();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.of(recoveryCase));

        InvalidRecoveryCaseStateException ex = assertThrows(InvalidRecoveryCaseStateException.class, () ->
                recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId));
        assertEquals("Cannot cancel recovery case in status: EXPIRED", ex.getMessage());
        verify(recoveryCaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelRecoveryCase rejects already CANCELLED case")
    void testCancelRecoveryCaseRejectCancelled() {
        recoveryCase.setStatus(RecoveryCaseStatus.CANCELLED);
        UUID caseId = recoveryCase.getId();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.of(recoveryCase));

        InvalidRecoveryCaseStateException ex = assertThrows(InvalidRecoveryCaseStateException.class, () ->
                recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId));
        assertEquals("Cannot cancel recovery case in status: CANCELLED", ex.getMessage());
        verify(recoveryCaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelRecoveryCase throws RecoveryCaseNotFoundException for cross-tenant case")
    void testCancelRecoveryCaseNotFound() {
        UUID caseId = UUID.randomUUID();
        when(recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)).thenReturn(Optional.empty());

        assertThrows(RecoveryCaseNotFoundException.class, () ->
                recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId));
        verify(recoveryCaseRepository, never()).save(any());
    }
}
