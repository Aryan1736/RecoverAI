package com.recoverai.backend.controller;

import com.recoverai.backend.dto.diagnosis.AgentDecisionResponseDto;
import com.recoverai.backend.exception.InvalidCredentialsException;
import com.recoverai.backend.exception.TenantMismatchException;
import com.recoverai.backend.security.MerchantPrincipal;
import com.recoverai.backend.security.SecurityUtils;
import com.recoverai.backend.service.AIDiagnosisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
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
            @RequestHeader(value = "X-Merchant-Id", required = false) UUID headerMerchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        UUID merchantId = resolveMerchantId(headerMerchantId);
        AgentDecisionResponseDto response = aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/merchants/{merchantId}/recovery-cases/{recoveryCaseId}/diagnose")
    public ResponseEntity<AgentDecisionResponseDto> diagnoseRecoveryCaseWithPath(
            @PathVariable("merchantId") UUID pathMerchantId,
            @PathVariable("recoveryCaseId") UUID recoveryCaseId) {
        UUID merchantId = resolveMerchantId(pathMerchantId);
        AgentDecisionResponseDto response = aiDiagnosisService.diagnoseRecoveryCase(merchantId, recoveryCaseId);
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
