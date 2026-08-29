package com.recoverai.backend.repository;

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
import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ActiveProfiles("test")
class RecoveryExecutionQueueRepositoryTest {

    @Autowired
    private RecoveryExecutionQueueRepository queueRepository;

    @Autowired
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @Autowired
    private RecoveryCaseRepository recoveryCaseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Merchant merchant;
    private RecoveryCase recoveryCase;
    private RecoveryAttempt attempt;

    @BeforeEach
    void setUp() {
        merchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Queue Test Merchant")
                .email("merchant_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer customer = customerRepository.saveAndFlush(Customer.builder()
                .merchant(merchant)
                .email("cust_" + UUID.randomUUID() + "@test.com")
                .name("Customer Queue")
                .phone("+919876543210")
                .build());

        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .merchant(merchant)
                .customer(customer)
                .razorpayPaymentId("pay_queue_" + UUID.randomUUID().toString().substring(0, 8))
                .amount(new BigDecimal("2999.00"))
                .currency("INR")
                .status(PaymentStatus.FAILED)
                .build());

        recoveryCase = recoveryCaseRepository.saveAndFlush(RecoveryCase.builder()
                .merchant(merchant)
                .customer(customer)
                .payment(payment)
                .status(RecoveryCaseStatus.OPEN)
                .estimatedRecoverableAmount(new BigDecimal("2999.00"))
                .build());

        attempt = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(1)
                .channel(RecoveryChannel.WHATSAPP)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Should successfully enqueue and retrieve a queue item")
    void testEnqueueAndRetrieveQueueItem() {
        Instant availableAt = Instant.now().minus(5, ChronoUnit.SECONDS);
        RecoveryExecutionQueueItem item = RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(availableAt)
                .retryCount(0)
                .maxRetries(3)
                .build();

        RecoveryExecutionQueueItem saved = queueRepository.saveAndFlush(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(RecoveryQueueStatus.READY);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getMaxRetries()).isEqualTo(3);

        Optional<RecoveryExecutionQueueItem> retrieved = queueRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRecoveryAttempt().getId()).isEqualTo(attempt.getId());
        assertThat(retrieved.get().getMerchant().getId()).isEqualTo(merchant.getId());
    }

    @Test
    @DisplayName("Should enforce uniqueness on recovery_attempt_id preventing duplicate queue items")
    void testDuplicateEnqueueProtectionViaUniqueConstraint() {
        RecoveryExecutionQueueItem item1 = RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now())
                .build();
        queueRepository.saveAndFlush(item1);

        RecoveryExecutionQueueItem item2 = RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now().plusSeconds(60))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            queueRepository.saveAndFlush(item2);
        });
    }

    @Test
    @DisplayName("findDueReadyItemIds should return only READY items where availableAt <= now")
    void testFindDueReadyItemIds() {
        Instant now = Instant.now();

        // 1. Due item: available 10 seconds ago, status READY
        RecoveryExecutionQueueItem dueItem = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(now.minusSeconds(10))
                .build());

        // 2. Future item: available in 10 minutes, status READY
        RecoveryAttempt attempt2 = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.SCHEDULED)
                .scheduledAt(now.plus(10, ChronoUnit.MINUTES))
                .build());

        queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt2)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(now.plus(10, ChronoUnit.MINUTES))
                .build());

        // 3. Already claimed item: available 20 seconds ago, status CLAIMED
        RecoveryAttempt attempt3 = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(3)
                .channel(RecoveryChannel.SMS)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .scheduledAt(now.minusSeconds(20))
                .build());

        queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt3)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .availableAt(now.minusSeconds(20))
                .claimedAt(now.minusSeconds(5))
                .claimedBy("worker-x")
                .build());

        List<UUID> dueIds = queueRepository.findDueReadyItemIds(now, PageRequest.of(0, 10));

        assertThat(dueIds).hasSize(1);
        assertThat(dueIds).containsExactly(dueItem.getId());
    }

    @Test
    @DisplayName("claimItem should atomically update READY to CLAIMED and return 1, subsequent claim should return 0")
    void testAtomicClaimSingleWinnerAndSubsequentClaimFails() {
        RecoveryExecutionQueueItem item = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now().minusSeconds(1))
                .build());

        Instant claimTime = Instant.now();
        int firstClaim = queueRepository.claimItem(item.getId(), "worker-alpha", claimTime);
        assertThat(firstClaim).isEqualTo(1);

        entityManager.clear();
        RecoveryExecutionQueueItem claimedItem = queueRepository.findById(item.getId()).orElseThrow();
        assertThat(claimedItem.getStatus()).isEqualTo(RecoveryQueueStatus.CLAIMED);
        assertThat(claimedItem.getClaimedBy()).isEqualTo("worker-alpha");
        assertThat(claimedItem.getClaimedAt()).isNotNull();

        // Second worker tries to claim the same item
        int secondClaim = queueRepository.claimItem(item.getId(), "worker-beta", Instant.now());
        assertThat(secondClaim).isEqualTo(0);
    }

    @Test
    @DisplayName("markProcessing and markCompleted should update status and timestamps accordingly")
    void testMarkProcessingAndCompleted() {
        RecoveryExecutionQueueItem item = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .availableAt(Instant.now().minusSeconds(5))
                .claimedAt(Instant.now().minusSeconds(2))
                .claimedBy("worker-1")
                .build());

        Instant start = Instant.now();
        int processingUpdated = queueRepository.markProcessing(item.getId(), start);
        assertThat(processingUpdated).isEqualTo(1);

        entityManager.clear();
        RecoveryExecutionQueueItem processingItem = queueRepository.findById(item.getId()).orElseThrow();
        assertThat(processingItem.getStatus()).isEqualTo(RecoveryQueueStatus.PROCESSING);
        assertThat(processingItem.getStartedAt()).isNotNull();

        Instant done = Instant.now();
        int completedUpdated = queueRepository.markCompleted(item.getId(), done);
        assertThat(completedUpdated).isEqualTo(1);

        entityManager.clear();
        RecoveryExecutionQueueItem completedItem = queueRepository.findById(item.getId()).orElseThrow();
        assertThat(completedItem.getStatus()).isEqualTo(RecoveryQueueStatus.COMPLETED);
        assertThat(completedItem.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("rescheduleForRetry should increment retry count, set status READY and update availableAt")
    void testRescheduleForRetry() {
        RecoveryExecutionQueueItem item = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.PROCESSING)
                .availableAt(Instant.now().minusSeconds(10))
                .claimedAt(Instant.now().minusSeconds(5))
                .claimedBy("worker-1")
                .startedAt(Instant.now().minusSeconds(4))
                .retryCount(0)
                .maxRetries(3)
                .build());

        Instant nextAvailableAt = Instant.now().plusSeconds(300);
        Instant now = Instant.now();

        int updated = queueRepository.rescheduleForRetry(
                item.getId(), nextAvailableAt, "PROVIDER_TIMEOUT", "Connection timed out", now);
        assertThat(updated).isEqualTo(1);

        entityManager.clear();
        RecoveryExecutionQueueItem reloaded = queueRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RecoveryQueueStatus.READY);
        assertThat(reloaded.getRetryCount()).isEqualTo(1);
        assertThat(reloaded.getAvailableAt()).isNotNull();
        assertThat(reloaded.getClaimedAt()).isNull();
        assertThat(reloaded.getClaimedBy()).isNull();
        assertThat(reloaded.getStartedAt()).isNull();
        assertThat(reloaded.getLastErrorCode()).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(reloaded.getLastErrorMessage()).isEqualTo("Connection timed out");
    }

    @Test
    @DisplayName("moveToDeadLetter should transition item to DEAD_LETTER status")
    void testMoveToDeadLetter() {
        RecoveryExecutionQueueItem item = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.PROCESSING)
                .availableAt(Instant.now().minusSeconds(10))
                .retryCount(3)
                .maxRetries(3)
                .build());

        Instant now = Instant.now();
        int updated = queueRepository.moveToDeadLetter(
                item.getId(), "RETRIES_EXHAUSTED", "Max retries exceeded", now);
        assertThat(updated).isEqualTo(1);

        entityManager.clear();
        RecoveryExecutionQueueItem reloaded = queueRepository.findById(item.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RecoveryQueueStatus.DEAD_LETTER);
        assertThat(reloaded.getCompletedAt()).isNotNull();
        assertThat(reloaded.getLastErrorCode()).isEqualTo("RETRIES_EXHAUSTED");
    }

    @Test
    @DisplayName("findStaleClaimIds and requeueStaleClaim should safely recover abandoned items")
    void testStaleClaimRecovery() {
        Instant staleThreshold = Instant.now().minusSeconds(300);

        // Stale claimed item (claimed 600s ago)
        RecoveryExecutionQueueItem staleClaimed = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .availableAt(staleThreshold.minusSeconds(100))
                .claimedAt(staleThreshold.minusSeconds(50))
                .claimedBy("crashed-worker")
                .build());

        // Recent claimed item (claimed 10s ago)
        RecoveryAttempt attempt2 = recoveryAttemptRepository.saveAndFlush(RecoveryAttempt.builder()
                .recoveryCase(recoveryCase)
                .merchant(merchant)
                .attemptNumber(2)
                .channel(RecoveryChannel.EMAIL)
                .status(RecoveryAttemptStatus.IN_FLIGHT)
                .scheduledAt(Instant.now().minusSeconds(10))
                .build());

        RecoveryExecutionQueueItem recentClaimed = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt2)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.CLAIMED)
                .availableAt(Instant.now().minusSeconds(10))
                .claimedAt(Instant.now().minusSeconds(5))
                .claimedBy("alive-worker")
                .build());

        List<UUID> staleIds = queueRepository.findStaleClaimIds(staleThreshold);
        assertThat(staleIds).containsExactly(staleClaimed.getId());
        assertThat(staleIds).doesNotContain(recentClaimed.getId());

        int requeued = queueRepository.requeueStaleClaim(staleClaimed.getId(), Instant.now());
        assertThat(requeued).isEqualTo(1);

        entityManager.clear();
        RecoveryExecutionQueueItem recovered = queueRepository.findById(staleClaimed.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(RecoveryQueueStatus.READY);
        assertThat(recovered.getClaimedAt()).isNull();
        assertThat(recovered.getClaimedBy()).isNull();
    }

    @Test
    @DisplayName("Merchant-scoped queries should strictly isolate items across merchants")
    void testMerchantScopedLookups() {
        Merchant otherMerchant = merchantRepository.saveAndFlush(Merchant.builder()
                .name("Other Merchant")
                .email("other_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        RecoveryExecutionQueueItem item = queueRepository.saveAndFlush(RecoveryExecutionQueueItem.builder()
                .merchant(merchant)
                .recoveryAttempt(attempt)
                .recoveryCase(recoveryCase)
                .status(RecoveryQueueStatus.READY)
                .availableAt(Instant.now())
                .build());

        // Accessible with correct merchant
        Optional<RecoveryExecutionQueueItem> foundOwn = queueRepository.findByIdAndMerchantId(item.getId(), merchant.getId());
        assertThat(foundOwn).isPresent();

        // Inaccessible with another merchant
        Optional<RecoveryExecutionQueueItem> foundOther = queueRepository.findByIdAndMerchantId(item.getId(), otherMerchant.getId());
        assertThat(foundOther).isEmpty();
    }
}
