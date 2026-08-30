package com.recoverai.backend.controller;

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
import com.recoverai.backend.repository.CustomerRepository;
import com.recoverai.backend.repository.MerchantRepository;
import com.recoverai.backend.repository.PaymentRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryExecutionQueueRepository;
import com.recoverai.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoveryDeadLetterQueueSecurityTest {

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
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private com.recoverai.backend.repository.AuditEventRepository auditEventRepository;

    private Merchant merchantA;
    private Merchant merchantB;
    private String tokenA;
    private String tokenB;
    private RecoveryExecutionQueueItem dlqItemA;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        queueRepository.deleteAll();
        recoveryAttemptRepository.deleteAll();
        recoveryCaseRepository.deleteAll();
        paymentRepository.deleteAll();
        customerRepository.deleteAll();
        merchantRepository.deleteAll();

        merchantA = merchantRepository.save(Merchant.builder()
                .name("Security Merchant A")
                .email("sec-a-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Security Merchant B")
                .email("sec-b-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateToken(merchantA);
        tokenB = jwtTokenProvider.generateToken(merchantB);

        Customer customerA = customerRepository.save(Customer.builder()
                .merchant(merchantA)
                .name("Alice")
                .email("alice@example.com")
                .phone("+15551234567")
                .build());

        Payment paymentA = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .status(PaymentStatus.FAILED)
                .razorpayPaymentId("pay_" + UUID.randomUUID())
                .build());

        RecoveryCase caseA = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .customer(customerA)
                .payment(paymentA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.MEDIUM)
                .estimatedRecoverableAmount(paymentA.getAmount())
                .currency("USD")
                .build());

        RecoveryAttempt attemptA = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(caseA)
                .merchant(merchantA)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .resultCode("AUTH_FAILED")
                .resultMessage("Authorization Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz failed with key=super_secret_api_key_456")
                .completedAt(Instant.now())
                .build());

        dlqItemA = queueRepository.save(RecoveryExecutionQueueItem.builder()
                .merchant(merchantA)
                .recoveryCase(caseA)
                .recoveryAttempt(attemptA)
                .status(RecoveryQueueStatus.DEAD_LETTER)
                .availableAt(Instant.now())
                .retryCount(3)
                .maxRetries(3)
                .lastErrorCode("AUTH_FAILED")
                .lastErrorMessage("Authorization Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz failed with key=super_secret_api_key_456")
                .completedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Unauthenticated request to list DLQ items returns 401 Unauthorized")
    void unauthenticatedListReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to get DLQ item detail returns 401 Unauthorized")
    void unauthenticatedGetDetailReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter/{id}", dlqItemA.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to redrive DLQ item returns 401 Unauthorized")
    void unauthenticatedRedriveReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-queue/dead-letter/{id}/redrive", dlqItemA.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cross-tenant attempt to get detail returns safe 404 Not Found")
    void crossTenantGetDetailReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter/{id}", dlqItemA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-tenant attempt to redrive returns safe 404 Not Found")
    void crossTenantRedriveReturns404() throws Exception {
        mockMvc.perform(post("/api/v1/recovery-queue/dead-letter/{id}/redrive", dlqItemA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Explicit X-Merchant-Id header mismatching JWT returns 403 Forbidden")
    void tenantMismatchHeaderReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .header("X-Merchant-Id", merchantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sensitive credentials, API keys, and bearer tokens are never exposed in responses")
    void sensitiveCredentialsAreNotExposedInResponses() throws Exception {
        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter/{id}", dlqItemA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastErrorMessage", not(containsString("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xyz"))))
                .andExpect(jsonPath("$.lastErrorMessage", not(containsString("super_secret_api_key_456"))))
                .andExpect(jsonPath("$.lastErrorMessage", containsString("[REDACTED]")));
    }
}
