package com.recoverai.backend.controller;

import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.orchestration.ScheduleRecoveryRequestDto;
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
            @RequestHeader("X-Merchant-Id") UUID merchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/orchestrate")
    public ResponseEntity<RecoveryAttemptResponseDto> orchestrateRecoveryWithPath(
            @PathVariable("merchantId") UUID merchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        RecoveryAttemptResponseDto response = recoveryOrchestratorService.orchestrateRecovery(merchantId, recoveryCaseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recovery-cases/{recoveryCaseId}/schedule")
    public ResponseEntity<RecoveryAttemptResponseDto> scheduleRecoveryWithHeader(
            @RequestHeader("X-Merchant-Id") UUID merchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId,
            @RequestBody(required = false) ScheduleRecoveryRequestDto request) {
        Instant scheduledAt = request != null ? request.getScheduledAt() : null;
        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, scheduledAt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/schedule")
    public ResponseEntity<RecoveryAttemptResponseDto> scheduleRecoveryWithPath(
            @PathVariable("merchantId") UUID merchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId,
            @RequestBody(required = false) ScheduleRecoveryRequestDto request) {
        Instant scheduledAt = request != null ? request.getScheduledAt() : null;
        RecoveryAttemptResponseDto response = recoverySchedulerService.scheduleRecovery(merchantId, recoveryCaseId, scheduledAt);
        return ResponseEntity.ok(response);
    }
}

