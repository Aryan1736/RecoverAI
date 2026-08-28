package com.recoverai.backend.service.strategy;

import com.recoverai.backend.config.RecoveryStrategyProperties;
import com.recoverai.backend.dto.strategy.RecoveryStrategyResponseDto;
import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.Merchant;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.RecoveryCase;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.ActorType;
import com.recoverai.backend.exception.RecoveryCaseNotFoundException;
import com.recoverai.backend.exception.RecoveryStrategyNotFoundException;
import com.recoverai.backend.repository.AgentDecisionRepository;
import com.recoverai.backend.repository.RecoveryAttemptRepository;
import com.recoverai.backend.repository.RecoveryCaseRepository;
import com.recoverai.backend.repository.RecoveryStrategyRepository;
import com.recoverai.backend.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RecoveryStrategyService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryStrategyService.class);

    private final RecoveryCaseRepository recoveryCaseRepository;
    private final AgentDecisionRepository agentDecisionRepository;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryStrategyRepository recoveryStrategyRepository;
    private final RecoveryStrategyEngine recoveryStrategyEngine;
    private final RecoveryStrategyProperties properties;
    private final AuditService auditService;

    public RecoveryStrategyService(RecoveryCaseRepository recoveryCaseRepository,
                                  AgentDecisionRepository agentDecisionRepository,
                                  RecoveryAttemptRepository recoveryAttemptRepository,
                                  RecoveryStrategyRepository recoveryStrategyRepository,
                                  RecoveryStrategyEngine recoveryStrategyEngine,
                                  RecoveryStrategyProperties properties,
                                  AuditService auditService) {
        this.recoveryCaseRepository = recoveryCaseRepository;
        this.agentDecisionRepository = agentDecisionRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.recoveryStrategyRepository = recoveryStrategyRepository;
        this.recoveryStrategyEngine = recoveryStrategyEngine;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Transactional
    public RecoveryStrategyResponseDto generateStrategy(UUID merchantId, UUID recoveryCaseId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (recoveryCaseId == null) {
            throw new IllegalArgumentException("Recovery Case ID cannot be null");
        }

        RecoveryCase recoveryCase = recoveryCaseRepository.findByIdAndMerchantId(recoveryCaseId, merchantId)
                .orElseThrow(() -> new RecoveryCaseNotFoundException(
                        "Recovery case not found with id: " + recoveryCaseId + " for merchant: " + merchantId));

        RecoveryStrategy strategy = computeAndPersistStrategy(recoveryCase);
        return RecoveryStrategyResponseDto.fromEntity(strategy);
    }

    @Transactional(readOnly = true)
    public RecoveryStrategyResponseDto getLatestStrategy(UUID merchantId, UUID recoveryCaseId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (recoveryCaseId == null) {
            throw new IllegalArgumentException("Recovery Case ID cannot be null");
        }

        // Validate recovery case exists for merchant
        boolean exists = recoveryCaseRepository.existsByIdAndMerchantId(recoveryCaseId, merchantId);
        if (!exists) {
            throw new RecoveryCaseNotFoundException(
                    "Recovery case not found with id: " + recoveryCaseId + " for merchant: " + merchantId);
        }

        RecoveryStrategy strategy = recoveryStrategyRepository
                .findFirstByRecoveryCaseIdAndMerchantIdOrderByCreatedAtDesc(recoveryCaseId, merchantId)
                .orElseThrow(() -> new RecoveryStrategyNotFoundException(
                        "No recovery strategy found for recovery case: " + recoveryCaseId));

        return RecoveryStrategyResponseDto.fromEntity(strategy);
    }

    @Transactional
    public RecoveryStrategy computeAndPersistStrategy(RecoveryCase recoveryCase) {
        Merchant merchant = recoveryCase.getMerchant();
        UUID caseId = recoveryCase.getId();

        AgentDecision agentDecision = agentDecisionRepository
                .findFirstByRecoveryCaseIdOrderByCreatedAtDesc(caseId)
                .orElse(null);

        List<RecoveryAttempt> previousAttempts = recoveryAttemptRepository
                .findByRecoveryCaseIdOrderByAttemptNumberAsc(caseId);

        RecoveryStrategy strategy = recoveryStrategyEngine.evaluate(
                recoveryCase, agentDecision, previousAttempts, properties);

        RecoveryStrategy savedStrategy = recoveryStrategyRepository.save(strategy);
        log.info("Persisted RecoveryStrategy id={} for recoveryCaseId={}, channel={}, action={}, isTerminal={}",
                savedStrategy.getId(), caseId, savedStrategy.getChannel(),
                savedStrategy.getRecommendedAction(), savedStrategy.isTerminal());

        recordStrategyAuditEvents(merchant, savedStrategy, agentDecision);

        return savedStrategy;
    }

    private void recordStrategyAuditEvents(Merchant merchant, RecoveryStrategy strategy, AgentDecision agentDecision) {
        String strategyId = strategy.getId() != null ? strategy.getId().toString() : "UNKNOWN";

        if (strategy.isTerminal()) {
            auditService.recordEvent(
                    merchant,
                    "RECOVERY_STRATEGY_TERMINAL",
                    ActorType.SYSTEM,
                    "RecoveryStrategyEngine",
                    "RecoveryStrategy",
                    strategyId,
                    "TERMINAL_STRATEGY",
                    String.format("Terminal strategy generated. Reason: %s", strategy.getReason()),
                    null
            );
        } else {
            // Check if fallback was selected instead of AI decision
            if (agentDecision != null && agentDecision.getChannel() != null
                    && agentDecision.getChannel() != strategy.getChannel()) {
                auditService.recordEvent(
                        merchant,
                        "RECOVERY_STRATEGY_FALLBACK",
                        ActorType.SYSTEM,
                        "RecoveryStrategyEngine",
                        "RecoveryStrategy",
                        strategyId,
                        "FALLBACK_STRATEGY",
                        String.format("Fallback channel %s selected (AI requested %s). Reason: %s",
                                strategy.getChannel(), agentDecision.getChannel(), strategy.getReason()),
                        null
                );
            }

            auditService.recordEvent(
                    merchant,
                    "RECOVERY_STRATEGY_GENERATED",
                    ActorType.SYSTEM,
                    "RecoveryStrategyEngine",
                    "RecoveryStrategy",
                    strategyId,
                    "GENERATE_STRATEGY",
                    String.format("Selected channel=%s, action=%s, priority=%s, confidence=%s, delay=%ds",
                            strategy.getChannel(), strategy.getRecommendedAction(),
                            strategy.getPriority(), strategy.getConfidenceScore(),
                            strategy.getDelaySeconds()),
                    null
            );
        }
    }
}
