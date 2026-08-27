package com.recoverai.backend.controller;

import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.service.RecoveryOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RecoveryOrchestrationController {

    private final RecoveryOrchestratorService recoveryOrchestratorService;

    public RecoveryOrchestrationController(RecoveryOrchestratorService recoveryOrchestratorService) {
        this.recoveryOrchestratorService = recoveryOrchestratorService;
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
}
