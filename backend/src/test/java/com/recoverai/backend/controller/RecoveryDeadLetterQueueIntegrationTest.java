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
import com.recoverai.backend.service.RecoveryExecutionQueueWorker;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoveryDeadLetterQueueIntegrationTest {

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
    private RecoveryExecutionQueueWorker queueWorker;

    @Autowired
    private com.recoverai.backend.service.RecoveryDeadLetterQueueService dlqService;

    @Autowired
    private com.recoverai.backend.repository.AuditEventRepository auditEventRepository;

    private Merchant merchantA;
    private Merchant merchantB;
    private String tokenA;
    private String tokenB;
    private Customer customerA;
    private RecoveryCase caseA;

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
                .name("Merchant A")
                .email("merch-a-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Merchant B")
                .email("merch-b-" + UUID.randomUUID() + "@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        tokenA = jwtTokenProvider.generateToken(merchantA);
        tokenB = jwtTokenProvider.generateToken(merchantB);

        customerA = customerRepository.save(Customer.builder()
                .merchant(merchantA)
                .name("Customer A")
                .email("customer-a@example.com")
                .phone("+15550001111")
                .build());

        Payment paymentA = paymentRepository.save(Payment.builder()
                .merchant(merchantA)
                .customer(customerA)
                .amount(new BigDecimal("199.99"))
                .currency("USD")
                .status(PaymentStatus.FAILED)
                .razorpayPaymentId("pay_" + UUID.randomUUID())
                .build());

        caseA = recoveryCaseRepository.save(RecoveryCase.builder()
                .merchant(merchantA)
                .customer(customerA)
                .payment(paymentA)
                .status(RecoveryCaseStatus.OPEN)
                .priority(RecoveryPriority.HIGH)
                .estimatedRecoverableAmount(paymentA.getAmount())
                .currency("USD")
                .build());
    }

    private RecoveryExecutionQueueItem createDeadLetterItem(Merchant merchant, RecoveryCase rCase, String errorCode, String errorMessage) {
        int nextAttempt = recoveryAttemptRepository.findByRecoveryCaseIdOrderByAttemptNumberAsc(rCase.getId()).size() + 1;
        RecoveryAttempt attempt = recoveryAttemptRepository.save(RecoveryAttempt.builder()
                .recoveryCase(rCase)
                .merchant(merchant)
                .attemptNumber(nextAttempt)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.FAILED)
                .resultCode(errorCode)
                .resultMessage(errorMessage)
                .completedAt(Instant.now())
                .build());

        return queueRepository.save(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryCase(rCase)
                .recoveryAttempt(attempt)
                .status(RecoveryQueueStatus.DEAD_LETTER)
                .availableAt(Instant.now())
                .retryCount(3)
                .maxRetries(3)
                .lastErrorCode(errorCode)
                .lastErrorMessage(errorMessage)
                .completedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Merchant can list own DLQ items with deterministic pagination and filtering")
    void merchantCanListOwnDeadLetterItems() throws Exception {
        RecoveryExecutionQueueItem dlqItem1 = createDeadLetterItem(merchantA, caseA, "AUTH_FAILURE", "Authentication failed with provider secret=my_super_secret_123");
        RecoveryExecutionQueueItem dlqItem2 = createDeadLetterItem(merchantA, caseA, "RATE_LIMITED", "Provider rate limited");

        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].lastErrorMessage", not(containsString("my_super_secret_123"))));

        // Filter by errorCode
        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .param("errorCode", "AUTH_FAILURE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].lastErrorCode", is("AUTH_FAILURE")));
    }

    @Test
    @DisplayName("Detail endpoint returns safe DTO for merchant's DLQ item")
    void detailEndpointReturnsSafeDto() throws Exception {
        RecoveryExecutionQueueItem dlqItem = createDeadLetterItem(merchantA, caseA, "PERMANENT_ERROR", "Token Bearer sensitive_token_xyz invalid");

        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter/{id}", dlqItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(dlqItem.getId().toString())))
                .andExpect(jsonPath("$.status", is("DEAD_LETTER")))
                .andExpect(jsonPath("$.recoveryCaseId", is(caseA.getId().toString())))
                .andExpect(jsonPath("$.channel", is("WHATSAPP")))
                .andExpect(jsonPath("$.lastErrorMessage", not(containsString("sensitive_token_xyz"))));
    }

    @Test
    @DisplayName("Cross-tenant detail access returns safe 404")
    void crossTenantDetailAccessReturns404() throws Exception {
        RecoveryExecutionQueueItem dlqItemA = createDeadLetterItem(merchantA, caseA, "ERROR", "Failed");

        mockMvc.perform(get("/api/v1/recovery-queue/dead-letter/{id}", dlqItemA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Redrive transitions DEAD_LETTER -> READY, resets retry count, and updates attempt to SCHEDULED")
    void redriveTransitionsDeadLetterToReady() throws Exception {
        RecoveryExecutionQueueItem dlqItem = createDeadLetterItem(merchantA, caseA, "TEMP_ERROR", "Temporary outage");

        mockMvc.perform(post("/api/v1/recovery-queue/dead-letter/{id}/redrive", dlqItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(dlqItem.getId().toString())))
                .andExpect(jsonPath("$.status", is("READY")))
                .andExpect(jsonPath("$.retryCount", is(0)));

        // Verify in DB
        RecoveryExecutionQueueItem reloadedItem = queueRepository.findById(dlqItem.getId()).orElseThrow();
        assertThat(reloadedItem.getStatus()).isEqualTo(RecoveryQueueStatus.READY);
        assertThat(reloadedItem.getRetryCount()).isEqualTo(0);

        RecoveryAttempt reloadedAttempt = recoveryAttemptRepository.findById(dlqItem.getRecoveryAttempt().getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(RecoveryAttemptStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Redrive is idempotent: calling redrive on already READY item returns safe DTO")
    void redriveIsIdempotent() throws Exception {
        RecoveryExecutionQueueItem dlqItem = createDeadLetterItem(merchantA, caseA, "TEMP_ERROR", "Temporary outage");

        // First redrive
        mockMvc.perform(post("/api/v1/recovery-queue/dead-letter/{id}/redrive", dlqItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READY")));

        // Second redrive (idempotent)
        mockMvc.perform(post("/api/v1/recovery-queue/dead-letter/{id}/redrive", dlqItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("READY")));
    }

    @Test
    @DisplayName("Concurrent redrive execution produces exactly one winner and safe responses")
    void concurrentRedriveHasOnlyOneWinner() throws InterruptedException, ExecutionException {
        RecoveryExecutionQueueItem dlqItem = createDeadLetterItem(merchantA, caseA, "ERROR", "Failed");

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Callable<com.recoverai.backend.dto.queue.DeadLetterQueueItemResponseDto>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            tasks.add(() -> dlqService.redriveDeadLetterItem(merchantA.getId(), dlqItem.getId(), "worker-" + index));
        }

        List<Future<com.recoverai.backend.dto.queue.DeadLetterQueueItemResponseDto>> futures = executorService.invokeAll(tasks);
        executorService.shutdown();

        for (Future<com.recoverai.backend.dto.queue.DeadLetterQueueItemResponseDto> f : futures) {
            com.recoverai.backend.dto.queue.DeadLetterQueueItemResponseDto res = f.get();
            assertThat(res.getStatus()).isEqualTo(RecoveryQueueStatus.READY);
        }

        RecoveryExecutionQueueItem finalItem = queueRepository.findById(dlqItem.getId()).orElseThrow();
        assertThat(finalItem.getStatus()).isEqualTo(RecoveryQueueStatus.READY);

        // Exactly one RECOVERY_EXECUTION_REDRIVEN audit event recorded
        assertThat(auditEventRepository.findByMerchantIdAndEventType(merchantA.getId(), "RECOVERY_EXECUTION_REDRIVEN")).hasSize(1);
    }

    @Test
    @DisplayName("Terminal case cannot be redriven (returns 400)")
    void terminalCaseCannotBeRedriven() throws Exception {
        caseA.setStatus(RecoveryCaseStatus.RECOVERED);
        recoveryCaseRepository.saveAndFlush(caseA);

        RecoveryExecutionQueueItem dlqItem = createDeadLetterItem(merchantA, caseA, "ERROR", "Failed");

        mockMvc.perform(post("/api/v1/recovery-queue/dead-letter/{id}/redrive", dlqItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Redriven item is processed by the normal queue worker")
    void redrivenItemProcessedByQueueWorker() throws Exception {
        RecoveryExecutionQueueItem dlqItem = createDeadLetterItem(merchantA, caseA, "ERROR", "Failed");

        // Redrive
        mockMvc.perform(post("/api/v1/recovery-queue/dead-letter/{id}/redrive", dlqItem.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Queue worker cycle should pick up the redriven READY item
        int processed = queueWorker.processDueQueueItems();
        assertThat(processed).isGreaterThanOrEqualTo(1);

        RecoveryExecutionQueueItem reloaded = queueRepository.findById(dlqItem.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);
    }
}
