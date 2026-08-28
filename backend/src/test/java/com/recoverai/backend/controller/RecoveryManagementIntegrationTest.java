package com.recoverai.backend.controller;

import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Customer;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.entity.enums.RiskLevel;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.security.JwtTokenProvider;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecoveryManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

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
    private AgentDecisionRepository agentDecisionRepository;

    private Merchant merchantA;
    private Merchant merchantB;
    private Merchant merchantZero;
    private String tokenA;
    private String tokenB;
    private String tokenZero;

    private RecoveryCase caseA1;
    private RecoveryCase caseA2;
    private RecoveryCase caseB1;
    private RecoveryAttempt attemptA1_1;
    private RecoveryAttempt attemptA1_2;

    @BeforeEach
    void setUp() {
        merchantA = merchantRepository.save(Merchant.builder()
                .name("Alpha Corp")
                .email("alpha_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Beta Corp")
                .email("beta_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantZero = merchantRepository.save(Merchant.builder()
                .name("Zero Corp")
                .email("zero_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateToken(merchantA);
        tokenB = jwtTokenProvider.generateToken(merchantB);
        tokenZero = jwtTokenProvider.generateToken(merchantZero);

        Customer customerA = customerRepository.save(Customer.builder()
                .merchant(merchantA)
                .name("Customer A")
                .email("custA_" + UUID.randomUUID() + "@example.com")
                .phone("+919999911111")
                .build());

        Customer customerB = customerRepository.save(Customer.builder()
                .merchant(merchantB)
                .name("Customer B")
                .email("custB_" + UUID.randomUUID() + "@example.com")
                .phone("+919999922222")
                .build());

        Payment paymentA1 = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_a1_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
                .errorCode("BAD_REQUEST")
                .errorDescription("Bank server down")
                .riskLevel(RiskLevel.LOW)
                .build());

        Payment paymentA2 = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_a2_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("2000.00"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .method(PaymentMethod.CARD)
                .riskLevel(RiskLevel.LOW)
                .build());

        Payment paymentB1 = paymentRepository.save(Payment.builder()
                .merchant(merchantB)
                .customer(customerB)
                .razorpayPaymentId("pay_b1_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.NETBANKING)
                .riskLevel(RiskLevel.LOW)
                .build());

        caseA1 = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(paymentA1)
                .customer(customerA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("PAYMENT_DECLINED")
                .estimatedRecoverableAmount(new BigDecimal("1000.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .build());

        caseA2 = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(paymentA2)
                .customer(customerA)
                .status(RecoveryCaseStatus.RECOVERED)
                .priority(RecoveryPriority.MEDIUM)
                .failureReasonCategory("CARD_NETWORK_ERROR")
                .estimatedRecoverableAmount(new BigDecimal("2000.00"))
                .recoveredAmount(new BigDecimal("2000.00"))
                .currency("INR")
                .build());

        caseB1 = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantB)
                .payment(paymentB1)
                .customer(customerB)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("PAYMENT_DECLINED")
                .estimatedRecoverableAmount(new BigDecimal("5000.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .build());

        attemptA1_1 = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(caseA1)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SENT)
                .scheduledAt(Instant.now().minusSeconds(3600))
                .executedAt(Instant.now().minusSeconds(3500))
                .build());

        attemptA1_2 = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(caseA1)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now().plusSeconds(3600))
                .build());

        agentDecisionRepository.save(AgentDecision.builder()
                .merchant(merchantA)
                .recoveryCase(caseA1)
                .recommendedAction("SEND_PAYMENT_LINK")
                .channel(RecoveryChannel.WHATSAPP)
                .confidenceScore(new BigDecimal("0.8800"))
                .reasoning("High recovery chance via WhatsApp")
                .modelName("gemini-3.7-flash")
                .modelVersion("v1")
                .promptTokens(100)
                .completionTokens(35)
                .build());
    }

    @Test
    @DisplayName("Integration: Merchant A lists only their recovery cases")
    void testListRecoveryCasesMultiTenant() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Integration: Filter recovery cases by status, priority, and failure category")
    void testFilterRecoveryCases() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("failureReasonCategory", "PAYMENT_DECLINED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseA1.getId().toString()))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"));
    }

    @Test
    @DisplayName("Integration: Case details returns full structured DTO and AI diagnosis")
    void testCaseDetailsIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases/{id}", caseA1.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseA1.getId().toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.payment.amount").value(1000.00))
                .andExpect(jsonPath("$.customer.name").value("Customer A"))
                .andExpect(jsonPath("$.attempts", hasSize(2)))
                .andExpect(jsonPath("$.attempts[0].attemptNumber").value(1))
                .andExpect(jsonPath("$.attempts[1].attemptNumber").value(2))
                .andExpect(jsonPath("$.latestDiagnosis.recommendedAction").value("SEND_PAYMENT_LINK"));
    }

    @Test
    @DisplayName("Integration: Cross-tenant case details access returns 404")
    void testCrossTenantCaseDetailsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases/{id}", caseB1.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration: Case attempts returns ordered attempts list")
    void testCaseAttemptsIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases/{id}/attempts", caseA1.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].attemptNumber").value(1))
                .andExpect(jsonPath("$[1].attemptNumber").value(2));
    }

    @Test
    @DisplayName("Integration: Cross-tenant case attempts access returns 404")
    void testCrossTenantCaseAttemptsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-cases/{id}/attempts", caseB1.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration: Case cancellation transitions OPEN case to CANCELLED and marks scheduled attempt as SKIPPED")
    void testCancelCaseIntegration() throws Exception {
        mockMvc.perform(patch("/api/v1/recovery-cases/{id}/cancel", caseA1.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        // Verify attemptA1_2 is now SKIPPED in DB
        RecoveryAttempt updatedAttempt2 = recoveryAttemptRepository.findById(attemptA1_2.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(RecoveryAttemptStatus.SKIPPED, updatedAttempt2.getStatus());

        // Attempting to cancel already CANCELLED case returns 400 Bad Request
        mockMvc.perform(patch("/api/v1/recovery-cases/{id}/cancel", caseA1.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel recovery case in status: CANCELLED"));
    }

    @Test
    @DisplayName("Integration: Cancellation rejects RECOVERED case")
    void testCancelRecoveredCaseRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/recovery-cases/{id}/cancel", caseA2.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot cancel recovery case in status: RECOVERED"));
    }

    @Test
    @DisplayName("Integration: Cross-tenant cancellation returns 404")
    void testCancelCrossTenantCaseNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/recovery-cases/{id}/cancel", caseB1.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Integration: Dashboard summary aggregates strictly for authenticated merchant")
    void testDashboardSummaryMultiTenant() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecoveryCases").value(2))
                .andExpect(jsonPath("$.openCases").value(1))
                .andExpect(jsonPath("$.recoveredCases").value(1))
                .andExpect(jsonPath("$.totalEstimatedRecoverableAmount").value(3000.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(2000.00))
                .andExpect(jsonPath("$.recoveryRate").value(50.00)); // 1 / 2 = 50.00%
    }

    @Test
    @DisplayName("Integration: Zero-case dashboard summary returns zeroes safely without error")
    void testZeroCaseDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenZero)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecoveryCases").value(0))
                .andExpect(jsonPath("$.openCases").value(0))
                .andExpect(jsonPath("$.recoveredCases").value(0))
                .andExpect(jsonPath("$.totalEstimatedRecoverableAmount").value(0.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(0.00))
                .andExpect(jsonPath("$.recoveryRate").value(0.00));
    }
}
