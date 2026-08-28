package com.recoverai.backend.controller;

import com.recoverai.backend.dto.analytics.AnalyticsOverviewResponseDto;
import com.recoverai.backend.dto.analytics.AttemptAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.ChannelAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.DateRange;
import com.recoverai.backend.dto.analytics.FailureAnalyticsResponseDto;
import com.recoverai.backend.dto.analytics.RecoveryTrendsResponseDto;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponseDto> getOverview(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        DateRange dateRange = DateRange.fromStrings(fromStr, toStr);
        AnalyticsOverviewResponseDto response = analyticsService.getAnalyticsOverview(merchantId, dateRange);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recovery-trends")
    public ResponseEntity<RecoveryTrendsResponseDto> getRecoveryTrends(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        DateRange dateRange = DateRange.fromStrings(fromStr, toStr);
        RecoveryTrendsResponseDto response = analyticsService.getRecoveryTrends(merchantId, dateRange);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/failures")
    public ResponseEntity<FailureAnalyticsResponseDto> getFailures(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        DateRange dateRange = DateRange.fromStrings(fromStr, toStr);
        FailureAnalyticsResponseDto response = analyticsService.getFailureAnalytics(merchantId, dateRange);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/channels")
    public ResponseEntity<ChannelAnalyticsResponseDto> getChannels(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        DateRange dateRange = DateRange.fromStrings(fromStr, toStr);
        ChannelAnalyticsResponseDto response = analyticsService.getChannelAnalytics(merchantId, dateRange);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attempts")
    public ResponseEntity<AttemptAnalyticsResponseDto> getAttempts(
            @RequestParam(value = "from", required = false) String fromStr,
            @RequestParam(value = "to", required = false) String toStr,
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        DateRange dateRange = DateRange.fromStrings(fromStr, toStr);
        AttemptAnalyticsResponseDto response = analyticsService.getAttemptAnalytics(merchantId, dateRange);
        return ResponseEntity.ok(response);
    }

    private UUID resolveMerchantId(UUID explicitMerchantId) {
        UUID authMerchantId = SecurityUtils.getCurrentMerchantId();
        if (explicitMerchantId != null && !explicitMerchantId.equals(authMerchantId)) {
            throw new TenantMismatchException("Authenticated merchant '" + authMerchantId + "' cannot access resources of merchant '" + explicitMerchantId + "'");
        }
        return authMerchantId;
    }
}
