package com.recoverai.backend.service;

import com.recoverai.backend.dto.analytics.AnalyticsOverviewResponseDto;
import com.recoverai.backend.dto.analytics.AttemptAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.ChannelAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.DateRange;
import com.recoverai.backend.dto.analytics.FailureAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.RecoveryTrendsResponseDto;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.projection.AnalyticsOverviewProjection;
import com.recoverai.backend.repository.projection.AttemptSummaryProjection;
import com.recoverai.backend.repository.projection.ChannelCountProjection;
import com.recoverai.backend.repository.projection.ChannelPerformanceProjection;
import com.recoverai.backend.repository.projection.DailyRecoveryTrendProjection;
import com.recoverai.backend.repository.projection.FailureCategoryProjection;
import com.recoverai.backend.repository.projection.FailurePriorityProjection;
import com.recoverai.backend.repository.projection.StatusCountProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private RecoveryCaseRepository recoveryCaseRepository;

    @Mock
    private RecoveryAttemptRepository recoveryAttemptRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID merchantId;
    private DateRange dateRange;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        Instant now = Instant.now();
        dateRange = new DateRange(now.minus(30, ChronoUnit.DAYS), now);
    }

    @Test
    @DisplayName("getAnalyticsOverview returns populated metrics and average recovery time")
    void testGetAnalyticsOverviewPopulated() {
        AnalyticsOverviewProjection projection = mock(AnalyticsOverviewProjection.class);
        when(projection.getTotalCases()).thenReturn(20L);
        when(projection.getOpenCases()).thenReturn(4L);
        when(projection.getInProgressCases()).thenReturn(6L);
        when(projection.getRecoveredCases()).thenReturn(8L);
        when(projection.getFailedCases()).thenReturn(1L);
        when(projection.getExpiredCases()).thenReturn(1L);
        when(projection.getCancelledCases()).thenReturn(0L);
        when(projection.getTotalEstimatedRecoverableAmount()).thenReturn(new BigDecimal("10000.00"));
        when(projection.getTotalRecoveredAmount()).thenReturn(new BigDecimal("4000.00"));

        when(recoveryCaseRepository.getAnalyticsOverview(eq(merchantId), any(), any())).thenReturn(projection);

        Instant created1 = Instant.now().minusSeconds(7200);
        Instant recovered1 = Instant.now().minusSeconds(3600); // 3600s
        Instant created2 = Instant.now().minusSeconds(10800);
        Instant recovered2 = Instant.now().minusSeconds(3600); // 7200s
        when(recoveryCaseRepository.findRecoveredTimestamps(eq(merchantId), any(), any()))
                .thenReturn(List.of(new Object[]{created1, recovered1}, new Object[]{created2, recovered2}));

        AnalyticsOverviewResponseDto result = analyticsService.getAnalyticsOverview(merchantId, dateRange);

        assertNotNull(result);
        assertEquals(20L, result.totalCases());
        assertEquals(4L, result.openCases());
        assertEquals(6L, result.inProgressCases());
        assertEquals(8L, result.recoveredCases());
        assertEquals(1L, result.failedCases());
        assertEquals(1L, result.expiredCases());
        assertEquals(0L, result.cancelledCases());
        assertEquals(1L, result.expiredOrCancelledCases());
        assertEquals(new BigDecimal("10000.00"), result.totalEstimatedRecoverableAmount());
        assertEquals(new BigDecimal("4000.00"), result.totalRecoveredAmount());
        assertEquals(new BigDecimal("40.00"), result.recoveryRate()); // 8/20 = 40%
        assertEquals(new BigDecimal("500.00"), result.averageRecoveredAmount()); // 4000/8 = 500
        assertEquals(5400.0, result.averageTimeToRecoverySeconds()); // (3600 + 7200) / 2 = 5400
    }

    @Test
    @DisplayName("getAnalyticsOverview with zero data returns zero values safely")
    void testGetAnalyticsOverviewZeroData() {
        when(recoveryCaseRepository.getAnalyticsOverview(eq(merchantId), any(), any())).thenReturn(null);
        when(recoveryCaseRepository.findRecoveredTimestamps(eq(merchantId), any(), any())).thenReturn(List.of());

        AnalyticsOverviewResponseDto result = analyticsService.getAnalyticsOverview(merchantId, dateRange);

        assertNotNull(result);
        assertEquals(0L, result.totalCases());
        assertEquals(0L, result.openCases());
        assertEquals(0L, result.recoveredCases());
        assertEquals(new BigDecimal("0.00"), result.totalEstimatedRecoverableAmount());
        assertEquals(new BigDecimal("0.00"), result.totalRecoveredAmount());
        assertEquals(new BigDecimal("0.00"), result.recoveryRate());
        assertEquals(new BigDecimal("0.00"), result.averageRecoveredAmount());
        assertNull(result.averageTimeToRecoverySeconds());
    }

    @Test
    @DisplayName("getRecoveryTrends maps daily items and calculates overall totals correctly")
    void testGetRecoveryTrends() {
        DailyRecoveryTrendProjection trend1 = mock(DailyRecoveryTrendProjection.class);
        when(trend1.getDate()).thenReturn(LocalDate.of(2026, 8, 1));
        when(trend1.getRecoveryCasesCreated()).thenReturn(10L);
        when(trend1.getAmountAtRisk()).thenReturn(new BigDecimal("5000.00"));
        when(trend1.getAmountRecovered()).thenReturn(new BigDecimal("2500.00"));
        when(trend1.getRecoveredCaseCount()).thenReturn(5L);

        DailyRecoveryTrendProjection trend2 = mock(DailyRecoveryTrendProjection.class);
        when(trend2.getDate()).thenReturn(LocalDate.of(2026, 8, 2));
        when(trend2.getRecoveryCasesCreated()).thenReturn(10L);
        when(trend2.getAmountAtRisk()).thenReturn(new BigDecimal("5000.00"));
        when(trend2.getAmountRecovered()).thenReturn(new BigDecimal("1000.00"));
        when(trend2.getRecoveredCaseCount()).thenReturn(2L);

        when(recoveryCaseRepository.getDailyRecoveryTrends(eq(merchantId), any(), any()))
                .thenReturn(List.of(trend1, trend2));

        RecoveryTrendsResponseDto result = analyticsService.getRecoveryTrends(merchantId, dateRange);

        assertNotNull(result);
        assertEquals(20L, result.totalCases());
        assertEquals(new BigDecimal("10000.00"), result.totalAmountAtRisk());
        assertEquals(new BigDecimal("3500.00"), result.totalRecoveredAmount());
        assertEquals(new BigDecimal("35.00"), result.overallRecoveryRate()); // 7 / 20 = 35%
        assertEquals(2, result.trends().size());
        assertEquals(new BigDecimal("50.00"), result.trends().get(0).recoveryRate());
        assertEquals(new BigDecimal("20.00"), result.trends().get(1).recoveryRate());
    }

    @Test
    @DisplayName("getFailureAnalytics returns categorized failures and priority breakdown")
    void testGetFailureAnalytics() {
        FailureCategoryProjection cat1 = mock(FailureCategoryProjection.class);
        when(cat1.getFailureReasonCategory()).thenReturn("INSUFFICIENT_FUNDS");
        when(cat1.getCaseCount()).thenReturn(15L);
        when(cat1.getEstimatedRecoverableAmount()).thenReturn(new BigDecimal("15000.00"));
        when(cat1.getRecoveredAmount()).thenReturn(new BigDecimal("6000.00"));
        when(cat1.getRecoveredCaseCount()).thenReturn(6L);

        FailurePriorityProjection prio1 = mock(FailurePriorityProjection.class);
        when(prio1.getPriority()).thenReturn(RecoveryPriority.HIGH);
        when(prio1.getCaseCount()).thenReturn(15L);
        when(prio1.getEstimatedRecoverableAmount()).thenReturn(new BigDecimal("15000.00"));
        when(prio1.getRecoveredAmount()).thenReturn(new BigDecimal("6000.00"));
        when(prio1.getRecoveredCaseCount()).thenReturn(6L);

        when(recoveryCaseRepository.getFailureCategoryAnalytics(eq(merchantId), any(), any()))
                .thenReturn(List.of(cat1));
        when(recoveryCaseRepository.getFailurePriorityAnalytics(eq(merchantId), any(), any()))
                .thenReturn(List.of(prio1));

        FailureAnalyticsResponseDto result = analyticsService.getFailureAnalytics(merchantId, dateRange);

        assertNotNull(result);
        assertEquals(15L, result.totalCases());
        assertEquals(1, result.categories().size());
        assertEquals("INSUFFICIENT_FUNDS", result.categories().get(0).failureReasonCategory());
        assertEquals(new BigDecimal("40.00"), result.categories().get(0).recoveryRate());
        assertEquals(1, result.priorities().size());
        assertEquals(RecoveryPriority.HIGH, result.priorities().get(0).priority());
        assertEquals(new BigDecimal("40.00"), result.priorities().get(0).recoveryRate());
    }

    @Test
    @DisplayName("getChannelAnalytics aggregates attempt stats per recovery channel")
    void testGetChannelAnalytics() {
        ChannelPerformanceProjection chan1 = mock(ChannelPerformanceProjection.class);
        when(chan1.getChannel()).thenReturn(RecoveryChannel.WHATSAPP);
        when(chan1.getTotalAttempts()).thenReturn(50L);
        when(chan1.getSuccessfulAttempts()).thenReturn(25L);
        when(chan1.getFailedAttempts()).thenReturn(10L);
        when(chan1.getSentAttempts()).thenReturn(10L);
        when(chan1.getDeliveredAttempts()).thenReturn(5L);
        when(chan1.getClickedAttempts()).thenReturn(0L);
        when(chan1.getRecoveredAmount()).thenReturn(new BigDecimal("25000.00"));

        when(recoveryAttemptRepository.getChannelPerformanceAnalytics(eq(merchantId), any(), any()))
                .thenReturn(List.of(chan1));

        ChannelAnalyticsResponseDto result = analyticsService.getChannelAnalytics(merchantId, dateRange);

        assertNotNull(result);
        assertEquals(50L, result.totalAttempts());
        assertEquals(1, result.channels().size());
        assertEquals(RecoveryChannel.WHATSAPP, result.channels().get(0).channel());
        assertEquals(new BigDecimal("50.00"), result.channels().get(0).successRate());
        assertEquals(new BigDecimal("25000.00"), result.channels().get(0).recoveredAmount());
    }

    @Test
    @DisplayName("getAttemptAnalytics returns comprehensive attempt breakdown and average attempts per case")
    void testGetAttemptAnalytics() {
        AttemptSummaryProjection summary = mock(AttemptSummaryProjection.class);
        when(summary.getTotalAttempts()).thenReturn(100L);
        when(summary.getSuccessfulAttempts()).thenReturn(40L);
        when(summary.getFailedAttempts()).thenReturn(20L);
        when(summary.getScheduledAttempts()).thenReturn(15L);
        when(summary.getInFlightAttempts()).thenReturn(5L);
        when(summary.getSentAttempts()).thenReturn(10L);
        when(summary.getDeliveredAttempts()).thenReturn(5L);
        when(summary.getClickedAttempts()).thenReturn(3L);
        when(summary.getSkippedAttempts()).thenReturn(2L);

        when(recoveryAttemptRepository.getAttemptSummaryAnalytics(eq(merchantId), any(), any()))
                .thenReturn(summary);

        StatusCountProjection sc1 = mock(StatusCountProjection.class);
        when(sc1.getStatus()).thenReturn(RecoveryAttemptStatus.SUCCESS);
        when(sc1.getCount()).thenReturn(40L);
        when(recoveryAttemptRepository.countAttemptsByStatus(eq(merchantId), any(), any()))
                .thenReturn(List.of(sc1));

        ChannelCountProjection cc1 = mock(ChannelCountProjection.class);
        when(cc1.getChannel()).thenReturn(RecoveryChannel.WHATSAPP);
        when(cc1.getCount()).thenReturn(70L);
        when(recoveryAttemptRepository.countAttemptsByChannel(eq(merchantId), any(), any()))
                .thenReturn(List.of(cc1));

        when(recoveryCaseRepository.countByMerchantIdAndCreatedAtBetween(eq(merchantId), any(), any()))
                .thenReturn(50L);

        AttemptAnalyticsResponseDto result = analyticsService.getAttemptAnalytics(merchantId, dateRange);

        assertNotNull(result);
        assertEquals(100L, result.totalAttempts());
        assertEquals(40L, result.successfulAttempts());
        assertEquals(new BigDecimal("40.00"), result.successRate());
        assertEquals(2.0, result.averageAttemptsPerRecoveryCase()); // 100 attempts / 50 cases = 2.0
        assertEquals(40L, result.attemptsByStatus().get(RecoveryAttemptStatus.SUCCESS));
        assertEquals(70L, result.attemptsByChannel().get(RecoveryChannel.WHATSAPP));
    }
}
