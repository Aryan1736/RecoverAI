package com.recoverai.backend.controller;

import com.recoverai.backend.dto.diagnosis.AgentDecisionResponseDto;
import com.recoverai.backend.service.AIDiagnosisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AIDiagnosisController {

    private final AIDiagnosisService aiDiagnosisService;

    public AIDiagnosisController(AIDiagnosisService aiDiagnosisService) {
        this.aiDiagnosisService = aiDiagnosisService;
    }

    @PostMapping("/recovery-cases/{recoveryCaseId}/diagnose")
    public ResponseEntity<AgentDecisionResponseDto> diagnoseRecoveryCaseWithHeader(
            @RequestHeader("X-Merchant-Id") UUID merchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        AgentDecisionResponseDto response = aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/diagnose")
    public ResponseEntity<AgentDecisionResponseDto> diagnoseRecoveryCaseWithPath(
            @PathVariable("merchantId") UUID merchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        AgentDecisionResponseDto response = aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId);
        return ResponseEntity.ok(response);
    }
}
