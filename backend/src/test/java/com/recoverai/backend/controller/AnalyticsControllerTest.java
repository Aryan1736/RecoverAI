package com.recoverai.backend.controller;

import com.recoverai.backend.dto.analytics.AnalyticsOverviewResponseDto;
import com.recoverai.backend.dto.analytics.AttemptAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.ChannelAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.ChannelMetricDto;
import com.recoverai.backend.dto.analytics.DailyRecoveryTrendDto;
import com.recoverai.backend.dto.analytics.FailureAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.FailureCategoryMetricDto;
import com.recoverai.backend.dto.analytics.FailurePriorityMetricDto;
import com.recoverai.backend.dto.analytics.RecoveryTrendsResponseDto;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.enums.MerchantStatus;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.security.JwtTokenProvider;
import com.recoverai.backend.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AnalyticsService analyticsService;

    private UUID merchantId;
    private String validToken;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        Merchant merchant = Merchant.builder()
                .id(merchantId)
                .name("Analytics Merchant")
                .email("analytics@merchant.com")
                .status(MerchantStatus.ACTIVE)
                .build();
        validToken = jwtTokenProvider.generateToken(merchant);
    }

    @Test
    @DisplayName("GET /api/v1/analytics/overview unauthenticated returns 401 Unauthorized")
    void testGetOverviewUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/analytics/overview authenticated returns overview metrics")
    void testGetOverviewAuthenticated() throws Exception {
        Instant now = Instant.now();
        AnalyticsOverviewResponseDto response = new AnalyticsOverviewResponseDto(
                20L, 5L, 7L, 8L, 0L, 0L, 0L, 0L,
                new BigDecimal("20000.00"), new BigDecimal("8000.00"),
                new BigDecimal("40.00"), new BigDecimal("1000.00"), 3600.0,
                now.minusSeconds(86400), now
        );
        when(analyticsService.getAnalyticsOverview(eq(merchantId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(20))
                .andExpect(jsonPath("$.openCases").value(5))
                .andExpect(jsonPath("$.recoveredCases").value(8))
                .andExpect(jsonPath("$.totalEstimatedRecoverableAmount").value(20000.00))
                .andExpect(jsonPath("$.totalRecoveredAmount").value(8000.00))
                .andExpect(jsonPath("$.recoveryRate").value(40.00))
                .andExpect(jsonPath("$.averageRecoveredAmount").value(1000.00))
                .andExpect(jsonPath("$.averageTimeToRecoverySeconds").value(3600.0));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/recovery-trends authenticated returns trend list")
    void testGetRecoveryTrendsAuthenticated() throws Exception {
        Instant now = Instant.now();
        DailyRecoveryTrendDto daily = new DailyRecoveryTrendDto(
                LocalDate.of(2026, 8, 1), 10L, new BigDecimal("10000.00"),
                new BigDecimal("5000.00"), 5L, new BigDecimal("50.00")
        );
        RecoveryTrendsResponseDto response = new RecoveryTrendsResponseDto(
                now.minusSeconds(86400), now, 10L, new BigDecimal("10000.00"),
                new BigDecimal("5000.00"), new BigDecimal("50.00"), List.of(daily)
        );
        when(analyticsService.getRecoveryTrends(eq(merchantId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/recovery-trends")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(10))
                .andExpect(jsonPath("$.totalAmountAtRisk").value(10000.00))
                .andExpect(jsonPath("$.trends[0].recoveryCasesCreated").value(10))
                .andExpect(jsonPath("$.trends[0].recoveryRate").value(50.00));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/failures authenticated returns category and priority breakdowns")
    void testGetFailuresAuthenticated() throws Exception {
        Instant now = Instant.now();
        FailureCategoryMetricDto cat = new FailureCategoryMetricDto(
                "CARD_DECLINED", 10L, new BigDecimal("10000.00"),
                new BigDecimal("6000.00"), 6L, new BigDecimal("60.00")
        );
        FailurePriorityMetricDto prio = new FailurePriorityMetricDto(
                RecoveryPriority.HIGH, 10L, new BigDecimal("10000.00"),
                new BigDecimal("6000.00"), 6L, new BigDecimal("60.00")
        );
        FailureAnalyticsResponseDto response = new FailureAnalyticsResponseDto(
                now.minusSeconds(86400), now, 10L, List.of(cat), List.of(prio)
        );
        when(analyticsService.getFailureAnalytics(eq(merchantId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/failures")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(10))
                .andExpect(jsonPath("$.categories[0].failureReasonCategory").value("CARD_DECLINED"))
                .andExpect(jsonPath("$.priorities[0].priority").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/channels authenticated returns channel performance list")
    void testGetChannelsAuthenticated() throws Exception {
        Instant now = Instant.now();
        ChannelMetricDto channel = new ChannelMetricDto(
                RecoveryChannel.WHATSAPP, 30L, 15L, 5L, 5L, 5L, 0L,
                new BigDecimal("50.00"), new BigDecimal("15000.00")
        );
        ChannelAnalyticsResponseDto response = new ChannelAnalyticsResponseDto(
                now.minusSeconds(86400), now, 30L, List.of(channel)
        );
        when(analyticsService.getChannelAnalytics(eq(merchantId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/channels")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(30))
                .andExpect(jsonPath("$.channels[0].channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.channels[0].successRate").value(50.00));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/attempts authenticated returns attempt breakdown")
    void testGetAttemptsAuthenticated() throws Exception {
        Instant now = Instant.now();
        var statusMap = new EnumMap<RecoveryAttemptStatus, Long>(RecoveryAttemptStatus.class);
        statusMap.put(RecoveryAttemptStatus.SUCCESS, 20L);
        var channelMap = new EnumMap<RecoveryChannel, Long>(RecoveryChannel.class);
        channelMap.put(RecoveryChannel.WHATSAPP, 30L);

        AttemptAnalyticsResponseDto response = new AttemptAnalyticsResponseDto(
                now.minusSeconds(86400), now, 40L, 20L, 10L, 5L, 2L, 3L, 0L, 0L, 0L,
                new BigDecimal("50.00"), 2.0, statusMap, channelMap
        );
        when(analyticsService.getAttemptAnalytics(eq(merchantId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/attempts")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttempts").value(40))
                .andExpect(jsonPath("$.successfulAttempts").value(20))
                .andExpect(jsonPath("$.averageAttemptsPerRecoveryCase").value(2.0))
                .andExpect(jsonPath("$.attemptsByStatus.SUCCESS").value(20))
                .andExpect(jsonPath("$.attemptsByChannel.WHATSAPP").value(30));
    }

    @Test
    @DisplayName("Invalid date range parameter returns 400 Bad Request with ApiErrorResponse")
    void testInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + validToken)
                        .param("from", "2026-08-20")
                        .param("to", "2026-08-10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Mismatched X-Merchant-Id header returns 403 Forbidden")
    void testMismatchedMerchantHeader() throws Exception {
        UUID otherMerchantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + validToken)
                        .header("X-Merchant-Id", otherMerchantId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
