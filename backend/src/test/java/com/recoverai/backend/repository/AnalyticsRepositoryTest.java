package com.recoverai.backend.repository;

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
import com.recoverai.backend.repository.projection.AnalyticsOverviewProjection;
import com.recoverai.backend.repository.projection.AttemptSummaryProjection;
import com.recoverai.backend.repository.projection.ChannelPerformanceProjection;
import com.recoverai.backend.repository.projection.DailyRecoveryTrendProjection;
import com.recoverai.backend.repository.projection.FailureCategoryProjection;
import com.recoverai.backend.repository.projection.FailurePriorityProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsRepositoryTest {

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
    private Instant now;
    private Instant thirtyDaysAgo;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        merchantA = merchantRepository.save(Merchant.builder()
                .name("Repo Merchant A")
                .email("repomerchant_a_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        merchantB = merchantRepository.save(Merchant.builder()
                .name("Repo Merchant B")
                .email("repomerchant_b_" + UUID.randomUUID() + "@test.com")
                .status(MerchantStatus.ACTIVE)
                .build());

        Customer customerA = customerRepository.save(Customer.builder()
                .merchant(merchantA)
                .name("Customer A")
                .email("customer_a_" + UUID.randomUUID() + "@example.com")
                .build());

        Customer customerB = customerRepository.save(Customer.builder()
                .merchant(merchantB)
                .name("Customer B")
                .email("customer_b_" + UUID.randomUUID() + "@example.com")
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
    @DisplayName("Analytics Overview query aggregates strictly for given merchant and date range")
    void testGetAnalyticsOverview() {
        AnalyticsOverviewProjection overviewA = recoveryCaseRepository.getAnalyticsOverview(
                merchantA.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(overviewA);
        assertEquals(2L, overviewA.getTotalCases());
        assertEquals(1L, overviewA.getOpenCases());
        assertEquals(1L, overviewA.getRecoveredCases());
        assertEquals(0, new BigDecimal("3000.00").compareTo(overviewA.getTotalEstimatedRecoverableAmount()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(overviewA.getTotalRecoveredAmount()));

        AnalyticsOverviewProjection overviewB = recoveryCaseRepository.getAnalyticsOverview(
                merchantB.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(overviewB);
        assertEquals(1L, overviewB.getTotalCases());
        assertEquals(1L, overviewB.getOpenCases());
        assertEquals(0L, overviewB.getRecoveredCases());
    }

    @Test
    @DisplayName("Daily Recovery Trends query groups by date with deterministic ordering")
    void testGetDailyRecoveryTrends() {
        List<DailyRecoveryTrendProjection> trends = recoveryCaseRepository.getDailyRecoveryTrends(
                merchantA.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(trends);
        assertEquals(2, trends.size());
        assertTrue(trends.get(0).getDate().isBefore(trends.get(1).getDate())
                || trends.get(0).getDate().isEqual(trends.get(1).getDate()));
    }

    @Test
    @DisplayName("Failure Category and Priority analytics group correctly")
    void testGetFailureAnalytics() {
        List<FailureCategoryProjection> categories = recoveryCaseRepository.getFailureCategoryAnalytics(
                merchantA.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(categories);
        assertEquals(2, categories.size());

        List<FailurePriorityProjection> priorities = recoveryCaseRepository.getFailurePriorityAnalytics(
                merchantA.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(priorities);
        assertEquals(2, priorities.size());
    }

    @Test
    @DisplayName("Channel performance analytics aggregates by channel for merchant")
    void testGetChannelPerformance() {
        List<ChannelPerformanceProjection> channelsA = recoveryAttemptRepository.getChannelPerformanceAnalytics(
                merchantA.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(channelsA);
        assertEquals(1, channelsA.size());
        assertEquals(RecoveryChannel.WHATSAPP, channelsA.get(0).getChannel());
        assertEquals(2L, channelsA.get(0).getTotalAttempts());
        assertEquals(1L, channelsA.get(0).getSuccessfulAttempts());
        assertEquals(1L, channelsA.get(0).getSentAttempts());

        List<ChannelPerformanceProjection> channelsB = recoveryAttemptRepository.getChannelPerformanceAnalytics(
                merchantB.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(channelsB);
        assertEquals(1, channelsB.size());
        assertEquals(RecoveryChannel.EMAIL, channelsB.get(0).getChannel());
        assertEquals(1L, channelsB.get(0).getTotalAttempts());
        assertEquals(1L, channelsB.get(0).getFailedAttempts());
    }

    @Test
    @DisplayName("Attempt summary analytics aggregates status counts")
    void testGetAttemptSummary() {
        AttemptSummaryProjection summaryA = recoveryAttemptRepository.getAttemptSummaryAnalytics(
                merchantA.getId(), thirtyDaysAgo, now.plus(1, ChronoUnit.DAYS));

        assertNotNull(summaryA);
        assertEquals(2L, summaryA.getTotalAttempts());
        assertEquals(1L, summaryA.getSuccessfulAttempts());
        assertEquals(1L, summaryA.getSentAttempts());
        assertEquals(0L, summaryA.getFailedAttempts());
    }
}
