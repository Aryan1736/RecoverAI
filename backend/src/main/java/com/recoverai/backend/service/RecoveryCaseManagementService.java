package com.recoverai.backend.service;

import com.recoverai.backend.dto.diagnosis.AgentDecisionResponseDto;
import com.recoverai.backend.dto.orchestration.RecoveryAttemptResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseDetailResponseDto;
import com.recoverai.backend.dto.recoverycase.RecoveryCaseResponseDto;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
import com.recoverai.backend.entity.enums.RecoveryPriority;
import com.recoverai.backend.exception.InvalidRecoveryCaseStateException;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.specification.RecoveryCaseSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class RecoveryCaseManagementService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryCaseManagementService.class);

    private static final Set<RecoveryCaseStatus> NON_CANCELLABLE_STATUSES = Set.of(
            RecoveryCaseStatus.RECOVERED,
            RecoveryCaseStatus.EXPIRED,
            RecoveryCaseStatus.CANCELLED
    );

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final AgentDecisionRepository agentDecisionRepository;
    private final AuditService auditService;

    public RecoveryCaseManagementService(RecoveryCaseRepository recoveryCaseRepository,
                                         RecoveryAttemptRepository recoveryAttemptRepository,
                                         AgentDecisionRepository agentDecisionRepository,
                                         AuditService auditService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<RecoveryCaseResponseDto> listRecoveryCases(UUID merchantId,
                                                           RecoveryCaseStatus status,
                                                           RecoveryPriority priority,
                                                           String failureReasonCategory,
                                                           Pageable pageable) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Specification<RecoveryCase> spec = RecoveryCaseSpecifications.withFilters(
                merchantId, status, priority, failureReasonCategory
        );
        return recoveryCaseRepository.findAll(spec, pageable).map(RecoveryCaseResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public RecoveryCaseDetailResponseDto getRecoveryCaseDetails(UUID merchantId, UUID caseId) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(caseId, "Recovery Case ID cannot be null");

        RecoveryCase recoveryCase = recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)
                .orElseThrow(() -> new RecoveryCaseNotFoundException(
                        "Recovery case not found with id: " + caseId + " for merchant: " + merchantId));

        List<RecoveryAttemptResponseDto> attempts = recoveryAttemptRepository
                .findByRecoveryCaseIdAndMerchantIdOrderByAttemptNumberAsc(caseId, merchantId)
                .stream()
                .map(RecoveryAttemptResponseDto::fromEntity)
                .toList();

        AgentDecisionResponseDto latestDiagnosis = agentDecisionRepository
                .findFirstByRecoveryCaseIdOrderByCreatedAtDesc(caseId)
                .filter(decision -> decision.getMerchant() != null && decision.getMerchant().getId().equals(merchantId))
                .map(AgentDecisionResponseDto::fromEntity)
                .orElse(null);

        return RecoveryCaseDetailResponseDto.fromEntity(recoveryCase, attempts, latestDiagnosis);
    }

    @Transactional(readOnly = true)
    public List<RecoveryAttemptResponseDto> getRecoveryCaseAttempts(UUID merchantId, UUID caseId) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(caseId, "Recovery Case ID cannot be null");

        // Verify case existence and ownership first (404 on cross-tenant or missing case)
        if (!recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId).isPresent()) {
            throw new RecoveryCaseNotFoundException(
                    "Recovery case not found with id: " + caseId + " for merchant: " + merchantId);
        }

        return recoveryAttemptRepository
                .findByRecoveryCaseIdAndMerchantIdOrderByAttemptNumberAsc(caseId, merchantId)
                .stream()
                .map(RecoveryAttemptResponseDto::fromEntity)
                .toList();
    }

    @Transactional
    public RecoveryCaseResponseDto cancelRecoveryCase(UUID merchantId, UUID caseId) {
        Objects.requireNonNull(merchantId, "Merchant ID cannot be null");
        Objects.requireNonNull(caseId, "Recovery Case ID cannot be null");

        RecoveryCase recoveryCase = recoveryCaseRepository.findByIdAndMerchantId(caseId, merchantId)
                .orElseThrow(() -> new RecoveryCaseNotFoundException(
                        "Recovery case not found with id: " + caseId + " for merchant: " + merchantId));

        if (NON_CANCELLABLE_STATUSES.contains(recoveryCase.getStatus())) {
            log.warn("Cannot cancel case {} in terminal status {}", caseId, recoveryCase.getStatus());
            throw new InvalidRecoveryCaseStateException(
                    "Cannot cancel recovery case in status: " + recoveryCase.getStatus());
        }

        Instant now = Instant.now();
        recoveryCase.setStatus(RecoveryCaseStatus.CANCELLED);
        recoveryCase.setClosedAt(now);
        RecoveryCase savedCase = recoveryCaseRepository.save(recoveryCase);

        // Transition any pending SCHEDULED attempts to SKIPPED
        List<RecoveryAttempt> scheduledAttempts = recoveryAttemptRepository.findByRecoveryCaseIdAndStatus(
                caseId, RecoveryAttemptStatus.SCHEDULED
        );
        for (RecoveryAttempt attempt : scheduledAttempts) {
            attempt.setStatus(RecoveryAttemptStatus.SKIPPED);
            attempt.setCompletedAt(now);
            attempt.setResultCode("CASE_CANCELLED");
            attempt.setResultMessage("Case cancelled by merchant");
            recoveryAttemptRepository.save(attempt);
        }

        auditService.recordEvent(
                savedCase.getMerchant(),
                "RECOVERY_CASE_CANCELLED",
                ActorType.USER,
                "MerchantDashboard",
                "RecoveryCase",
                caseId.toString(),
                "CANCEL_CASE",
                "Recovery case cancelled by merchant",
                null
        );

        log.info("Successfully cancelled recoveryCaseId={} for merchantId={}", caseId, merchantId);
        return RecoveryCaseResponseDto.fromEntity(savedCase);
    }
}
