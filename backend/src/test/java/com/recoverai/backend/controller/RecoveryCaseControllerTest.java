package com.recoverai.backend.controller;

import com.recoverai.backend.dto.diagnosis.AgentDecisionResponseDto;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.recoverycase.CustomerResponseDto;
import com.recoverai.backend.dto.recoverycase.PaymentResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseDetailResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseResponseDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.security.JwtTokenProvider;
import com.recoverai.backend.security.MerchantPrincipal;
import com.recoverai.backend.service.RecoveryCaseManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoveryCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RecoveryCaseManagementService recoveryCaseManagementService;

    private UUID merchantId;
    private String validToken;
    private RecoveryCaseResponseDto sampleCaseDto;
    private RecoveryCaseDetailResponseDto sampleDetailDto;
    private RecoveryAttemptResponseDto sampleAttemptDto;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .name("Test Merchant")
                .email("test@merchant.com")
                .status(com.recoverai.backend.entity.enums.MerchantStatus.ACTIVE)
                .build();
        validToken = jwtTokenProvider.generateToken(merchant);

        UUID caseId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();

        sampleCaseDto = new RecoveryCaseResponseDto(
                caseId, merchantId, paymentId, customerId, "John Doe", "john@example.com",
                RecoveryCaseStatus.OPEN, RecoveryPriority.HIGH, "PAYMENT_DECLINED",
                new BigDecimal("2500.00"), BigDecimal.ZERO, "INR",
                now.plusSeconds(86400), null, null, now, now
        );

        PaymentResponseDto paymentDto = new PaymentResponseDto(
                paymentId, "pay_123456", "order_123", "inv_123",
                new BigDecimal("2500.00"), "INR", PaymentStatus.FAILED, PaymentMethod.UPI,
                "BAD_REQUEST_ERROR", "Payment failed", "customer", "insufficient_funds",
                RiskLevel.LOW, now, now
        );

        CustomerResponseDto customerDto = new CustomerResponseDto(
                customerId, "cust_123", "John Doe", "john@example.com", "+919876543210", now
        );

        sampleAttemptDto = new RecoveryAttemptResponseDto(
                UUID.randomUUID(), caseId, merchantId, 1, RecoveryChannel.WHATSAPP,
                RecoveryAttemptStatus.SENT, now, now, null, "200", "Message sent",
                "https://recover.ai/pay/123", now, now
        );

        AgentDecisionResponseDto decisionDto = new AgentDecisionResponseDto(
                UUID.randomUUID(), caseId, merchantId, "SEND_DISCOUNT_LINK", RecoveryChannel.WHATSAPP,
                new BigDecimal("0.9200"), "High intent customer", "gemini-3.7-flash", "1.0",
                120, 45, "factors", now
        );

        sampleDetailDto = new RecoveryCaseDetailResponseDto(
                caseId, merchantId, RecoveryCaseStatus.OPEN, RecoveryPriority.HIGH,
                "PAYMENT_DECLINED", new BigDecimal("2500.00"), BigDecimal.ZERO, "INR",
                now.plusSeconds(86400), null, null, now, now,
                paymentDto, customerDto, List.of(sampleAttemptDto), decisionDto
        );
    }

    @Test
    @DisplayName("GET /api/v1/recovery-cases without token returns 401 Unauthorized")
    void testListRecoveryCasesUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/recovery-cases with valid token returns paginated cases")
    void testListRecoveryCasesAuthenticated() throws Exception {
        when(recoveryCaseManagementService.listRecoveryCases(
                eq(merchantId), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleCaseDto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/recovery-cases")
                        .header("Authorization", "Bearer " + validToken)
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("failureReasonCategory", "PAYMENT_DECLINED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleCaseDto.getId().toString()))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.content[0].customerName").value("John Doe"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/recovery-cases with mismatched X-Merchant-Id header returns 403 Forbidden")
    void testListRecoveryCasesMismatchedHeader() throws Exception {
        UUID otherMerchantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/recovery-cases")
                        .header("Authorization", "Bearer " + validToken)
                        .header("X-Merchant-Id", otherMerchantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("GET /api/v1/recovery-cases/{id} returns deep case details")
    void testGetRecoveryCaseDetailsSuccess() throws Exception {
        UUID caseId = sampleCaseDto.getId();
        when(recoveryCaseManagementService.getRecoveryCaseDetails(merchantId, caseId))
                .thenReturn(sampleDetailDto);

        mockMvc.perform(get("/api/v1/recovery-cases/{id}", caseId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.payment.razorpayPaymentId").value("pay_123456"))
                .andExpect(jsonPath("$.customer.email").value("john@example.com"))
                .andExpect(jsonPath("$.attempts[0].channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.latestDiagnosis.recommendedAction").value("SEND_DISCOUNT_LINK"));
    }

    @Test
    @DisplayName("GET /api/v1/recovery-cases/{id} returns 404 for missing or cross-tenant case")
    void testGetRecoveryCaseDetailsNotFound() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(recoveryCaseManagementService.getRecoveryCaseDetails(merchantId, caseId))
                .thenThrow(new RecoveryCaseNotFoundException("Recovery case not found"));

        mockMvc.perform(get("/api/v1/recovery-cases/{id}", caseId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/recovery-cases/{id}/attempts returns attempts list")
    void testGetRecoveryCaseAttemptsSuccess() throws Exception {
        UUID caseId = sampleCaseDto.getId();
        when(recoveryCaseManagementService.getRecoveryCaseAttempts(merchantId, caseId))
                .thenReturn(List.of(sampleAttemptDto));

        mockMvc.perform(get("/api/v1/recovery-cases/{id}/attempts", caseId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attemptNumber").value(1))
                .andExpect(jsonPath("$[0].channel").value("WHATSAPP"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    @Test
    @DisplayName("GET /api/v1/recovery-cases/{id}/attempts returns 404 when case not found or cross-tenant")
    void testGetRecoveryCaseAttemptsNotFound() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(recoveryCaseManagementService.getRecoveryCaseAttempts(merchantId, caseId))
                .thenThrow(new RecoveryCaseNotFoundException("Recovery case not found"));

        mockMvc.perform(get("/api/v1/recovery-cases/{id}/attempts", caseId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/v1/recovery-cases/{id}/cancel successfully cancels valid case")
    void testCancelRecoveryCaseSuccess() throws Exception {
        UUID caseId = sampleCaseDto.getId();
        sampleCaseDto.setStatus(RecoveryCaseStatus.CANCELLED);
        sampleCaseDto.setClosedAt(Instant.now());

        when(recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId))
                .thenReturn(sampleCaseDto);

        mockMvc.perform(patch("/api/v1/recovery-cases/{id}/cancel", caseId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /api/v1/recovery-cases/{id}/cancel returns 400 when cancelling terminal case")
    void testCancelRecoveryCaseTerminalState() throws Exception {
        UUID caseId = sampleCaseDto.getId();
        when(recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId))
                .thenThrow(new InvalidRecoveryCaseStateException("Cannot cancel recovery case in status: RECOVERED"));

        mockMvc.perform(patch("/api/v1/recovery-cases/{id}/cancel", caseId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Cannot cancel recovery case in status: RECOVERED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/recovery-cases/{id}/cancel returns 404 for cross-tenant or missing case")
    void testCancelRecoveryCaseNotFound() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(recoveryCaseManagementService.cancelRecoveryCase(merchantId, caseId))
                .thenThrow(new RecoveryCaseNotFoundException("Recovery case not found"));

        mockMvc.perform(patch("/api/v1/recovery-cases/{id}/cancel", caseId)
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
