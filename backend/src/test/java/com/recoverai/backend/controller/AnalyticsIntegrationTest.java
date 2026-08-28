package com.recoverai.backend.controller;

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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsIntegrationTest {

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

    private Merchant merchantA;
    private Merchant merchantB;
    private Merchant merchantZero;
    private String tokenA;
    private String tokenB;
    private String tokenZero;

    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        merchantA = merchantRepository.save(Merchant.builder()
                .name("Analytics Corp A")
                .email("corpA_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Analytics Corp B")
                .email("corpB_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantZero = merchantRepository.save(Merchant.builder()
                .name("Analytics Corp Zero")
                .email("corpZero_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateToken(merchantA);
        tokenB = jwtTokenProvider.generateToken(merchantB);
        tokenZero = jwtTokenProvider.generateToken(merchantZero);

        Customer customerA = customerRepository.save(Customer.builder()
                .merchant(merchantA)
                .name("Customer Alpha")
                .email("alpha_" + UUID.randomUUID() + "@example.com")
                .build());

        Customer customerB = customerRepository.save(Customer.builder()
                .merchant(merchantB)
                .name("Customer Beta")
                .email("beta_" + UUID.randomUUID() + "@example.com")
                .build());

        Payment paymentA1 = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .razorpayPaymentId("pay_a1_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .method(PaymentMethod.UPI)
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

        RecoveryCase caseA1 = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(paymentA1)
                .customer(customerA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .failureReasonCategory("PAYMENT_DECLINED")
                .estimatedRecoverableAmount(new BigDecimal("1000.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .createdAt(now.minus(5, ChronoUnit.DAYS))
                .build());

        RecoveryCase caseA2 = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .payment(paymentA2)
                .customer(customerA)
                .status(RecoveryCaseStatus.RECOVERED)
                .priority(RecoveryPriority.MEDIUM)
                .failureReasonCategory("CARD_NETWORK_ERROR")
                .estimatedRecoverableAmount(new BigDecimal("2000.00"))
                .recoveredAmount(new BigDecimal("2000.00"))
                .currency("INR")
                .recoveredAt(now.minus(1, ChronoUnit.DAYS))
                .createdAt(now.minus(2, ChronoUnit.DAYS))
                .build());

        RecoveryCase caseB1 = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantB)
                .payment(paymentB1)
                .customer(customerB)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.CRITICAL)
                .failureReasonCategory("PAYMENT_DECLINED")
                .estimatedRecoverableAmount(new BigDecimal("5000.00"))
                .recoveredAmount(BigDecimal.ZERO)
                .currency("INR")
                .createdAt(now.minus(3, ChronoUnit.DAYS))
                .build());

        recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(caseA1)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SENT)
                .createdAt(now.minus(4, ChronoUnit.DAYS))
                .build());

        recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchantA)
                .recoveryCase(caseA2)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SUCCESS)
                .createdAt(now.minus(2, ChronoUnit.DAYS))
                .build());

        recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .merchant(merchantB)
                .recoveryCase(caseB1)
                .attemptNumber(1)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.FAILED)
                .createdAt(now.minus(3, ChronoUnit.DAYS))
                .build());
    }

    @Test
    @DisplayName("Integration: GET /api/v1/analytics/overview returns isolated overview metrics")
    void testOverviewIntegration() throws Exception {
        // Merchant A
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(2))
                .andExpect(jsonPath("$.openCases").value(1))
                .andExpect(jsonPath("$.recoveredCases").value(1))
                .andExpect(jsonPath("$.totalEstimatedRecoverableAmount").value(3000.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(2000.00))
                .andExpect(jsonPath("$.recoveryRate").value(50.00))
                .andExpect(jsonPath("$.averageRecoveredAmount").value(2000.00))
                .andExpect(jsonPath("$.averageTimeToRecoverySeconds").isNotEmpty());

        // Merchant B
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(1))
                .andExpect(jsonPath("$.openCases").value(1))
                .andExpect(jsonPath("$.recoveredCases").value(0))
                .andExpect(jsonPath("$.totalEstimatedRecoverableAmount").value(5000.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(0.00))
                .andExpect(jsonPath("$.recoveryRate").value(0.00));

        // Merchant Zero
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + tokenZero)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(0))
                .andExpect(jsonPath("$.totalEstimatedRecoverableAmount").value(0.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(0.00))
                .andExpect(jsonPath("$.recoveryRate").value(0.00));
    }

    @Test
    @DisplayName("Integration: GET /api/v1/analytics/recovery-trends returns daily trends and respects date filter")
    void testRecoveryTrendsIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/recovery-trends")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(2))
                .andExpect(jsonPath("$.totalAmountAtRisk").value(3000.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(2000.00))
                .andExpect(jsonPath("$.overallRecoveryRate").value(50.00))
                .andExpect(jsonPath("$.trends", hasSize(2)));
    }

    @Test
    @DisplayName("Integration: GET /api/v1/analytics/failures returns category and priority breakdown")
    void testFailuresIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/failures")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(2))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.priorities", hasSize(2)));
    }

    @Test
    @DisplayName("Integration: GET /api/v1/analytics/channels returns channel metrics")
    void testChannelsIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/channels")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(2))
                .andExpect(jsonPath("$.channels", hasSize(1)))
                .andExpect(jsonPath("$.channels[0].channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.channels[0].totalAttempts").value(2))
                .andExpect(jsonPath("$.channels[0].successfulAttempts").value(1))
                .andExpect(jsonPath("$.channels[0].sentAttempts").value(1))
                .andExpect(jsonPath("$.channels[0].successRate").value(50.00))
                .andExpect(jsonPath("$.channels[0].recoveredAmount").value(2000.00));
    }

    @Test
    @DisplayName("Integration: GET /api/v1/analytics/attempts returns attempt metrics and status map")
    void testAttemptsIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/attempts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(2))
                .andExpect(jsonPath("$.successfulAttempts").value(1))
                .andExpect(jsonPath("$.sentAttempts").value(1))
                .andExpect(jsonPath("$.successRate").value(50.00))
                .andExpect(jsonPath("$.averageAttemptsPerRecoveryCase").value(1.0)) // 2 attempts / 2 cases = 1.0
                .andExpect(jsonPath("$.attemptsByStatus.SUCCESS").value(1))
                .andExpect(jsonPath("$.attemptsByStatus.SENT").value(1))
                .andExpect(jsonPath("$.attemptsByChannel.WHATSAPP").value(2));
    }

    @Test
    @DisplayName("Integration: Invalid date range returns 400 Bad Request")
    void testInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("from", "2026-08-20")
                        .param("to", "2026-08-10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Integration: Cross-tenant header spoofing returns 403 Forbidden")
    void testCrossTenantHeaderSpoofing() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Merchant-Id", merchantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
