package com.recoverai.backend.controller;

import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseDetailResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseResponseDto;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.RecoveryCaseManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery-cases")
public class RecoveryCaseController {

    private final RecoveryCaseManagementService recoveryCaseManagementService;

    public RecoveryCaseController(RecoveryCaseManagementService recoveryCaseManagementService) {
        this.recoveryCaseManagementService = recoveryCaseManagementService;
    }

    @GetMapping
    public ResponseEntity<Page<RecoveryCaseResponseDto>> listRecoveryCases(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @RequestParam(value = "status", required = false) RecoveryCaseStatus status,
            @RequestParam(value = "priority", required = false) RecoveryPriority priority,
            @RequestParam(value = "failureReasonCategory", required = false) String failureReasonCategory,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        Page<RecoveryCaseResponseDto> cases = recoveryCaseManagementService.listRecoveryCases(
                merchantId, status, priority, failureReasonCategory, pageable
        );
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecoveryCaseDetailResponseDto> getRecoveryCaseDetails(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("id") UUID id) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        RecoveryCaseDetailResponseDto caseDetails = recoveryCaseManagementService.getRecoveryCaseDetails(merchantId, id);
        return ResponseEntity.ok(caseDetails);
    }

    @GetMapping("/{id}/attempts")
    public ResponseEntity<List<RecoveryAttemptResponseDto>> getRecoveryCaseAttempts(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("id") UUID id) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        List<RecoveryAttemptResponseDto> attempts = recoveryCaseManagementService.getRecoveryCaseAttempts(merchantId, id);
        return ResponseEntity.ok(attempts);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RecoveryCaseResponseDto> cancelRecoveryCase(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("id") UUID id) {

        UUID merchantId = resolveMerchantId(headerMerchantId);
        RecoveryCaseResponseDto cancelledCase = recoveryCaseManagementService.cancelRecoveryCase(merchantId, id);
        return ResponseEntity.ok(cancelledCase);
    }

    private UUID resolveMerchantId(UUID explicitMerchantId) {
        UUID authMerchantId = SecurityUtils.getCurrentMerchantId();
        if (explicitMerchantId != null && !explicitMerchantId.equals(authMerchantId)) {
            throw new TenantMismatchException("Authenticated merchant '" + authMerchantId + "' cannot access resources of merchant '" + explicitMerchantId + "'");
        }
        return authMerchantId;
    }
}
