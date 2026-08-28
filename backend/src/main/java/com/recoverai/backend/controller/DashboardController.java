package com.recoverai.backend.controller;

import com.recoverai.backend.dto.dashboard.DashboardSummaryResponseDto;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponseDto> getDashboardSummary(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        DashboardSummaryResponseDto summary = dashboardService.getDashboardSummary(merchantId);
        return ResponseEntity.ok(summary);
    }

    private UUID resolveMerchantId(UUID explicitMerchantId) {
        UUID authMerchantId = SecurityUtils.getCurrentMerchantId();
        if (explicitMerchantId != null && !explicitMerchantId.equals(authMerchantId)) {
            throw new TenantMismatchException("Authenticated merchant '" + authMerchantId + "' cannot access resources of merchant '" + explicitMerchantId + "'");
        }
        return authMerchantId;
    }
}
