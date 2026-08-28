package com.recoverai.backend.controller;

import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.orchestration.ScheduleRecoveryRequestDto;
import com.recoverai.backend.exception.InvalidCredentialsException;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.MerchantPrincipal;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.RecoveryOrchestratorService;
import com.recoverai.backend.service.RecoverySchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RecoveryOrchestrationController {

    private final RecoveryOrchestratorService recoveryOrchestratorService;
    private final RecoverySchedulerService recoverySchedulerService;

    public RecoveryOrchestrationController(RecoveryOrchestratorService recoveryOrchestratorService,
                                           RecoverySchedulerService recoverySchedulerService) {
        this.recoveryOrchestratorService = recoveryOrchestratorService;
        this.recoverySchedulerService = recoverySchedulerService;
    }

    @PostMapping("/recovery-cases/{recoveryCaseId}/orchestrate")
    public ResponseEntity<RecoveryAttemptResponseDto> orchestrateRecoveryWithHeader(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/orchestrate")
    public ResponseEntity<RecoveryAttemptResponseDto> orchestrateRecoveryWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recovery-cases/{recoveryCaseId}/schedule")
    public ResponseEntity<RecoveryAttemptResponseDto> scheduleRecoveryWithHeader(
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId,
            @RequestBody(required = false) ScheduleRecoveryRequestDto request) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        Instant scheduledAt = request != null ? request.getScheduledAt() : null;
        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, scheduledAt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/schedule")
    public ResponseEntity<RecoveryAttemptResponseDto> scheduleRecoveryWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId,
            @RequestBody(required = false) ScheduleRecoveryRequestDto request) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        Instant scheduledAt = request != null ? request.getScheduledAt() : null;
        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, scheduledAt);
        return ResponseEntity.ok(response);
    }

    private UUID resolveMerchantId(UUID explicitMerchantId) {
        Optional<MerchantPrincipal> principalOpt = SecurityUtils.getCurrentMerchantPrincipal();
        if (principalOpt.isPresent()) {
            UUID authMerchantId = principalOpt.get().getId();
            if (explicitMerchantId != null && !explicitMerchantId.equals(authMerchantId)) {
                throw new TenantMismatchException("Authenticated merchant '" + authMerchantId + "' cannot access resources of merchant '" + explicitMerchantId + "'");
            }
            return authMerchantId;
        }
        if (explicitMerchantId != null) {
            return explicitMerchantId;
        }
        throw new InvalidCredentialsException("No merchant identity provided in authentication token or request");
    }
}
