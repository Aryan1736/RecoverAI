package com.recoverai.backend.dto.orchestration;

import com.recoverai.backend.dto.strategy.RecoveryStrategySnapshot;
import com.recoverai.backend.entity.RecoveryAttempt;
import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RecoveryAttemptResponseDto {

    private UUID id;
    private UUID recoveryCaseId;
    private UUID merchantId;
    private int attemptNumber;
    private RecoveryChannel channel;
    private RecoveryAttemptStatus status;
    private Instant scheduledAt;
    private Instant executedAt;
    private Instant completedAt;
    private String resultCode;
    private String resultMessage;
    private String recoveryLink;
    private UUID strategyId;
    private RecoveryStrategySnapshot strategySnapshot;
    private RecoveryPriority strategyPriority;
    private BigDecimal confidenceScore;
    private String recommendedAction;
    private Instant createdAt;
    private Instant updatedAt;

    public RecoveryAttemptResponseDto() {
    }

    public RecoveryAttemptResponseDto(UUID id, UUID recoveryCaseId, UUID merchantId, int attemptNumber,
                                     RecoveryChannel channel, RecoveryAttemptStatus status,
                                     Instant scheduledAt, Instant executedAt, Instant completedAt,
                                     String resultCode, String resultMessage, String recoveryLink,
                                     Instant createdAt, Instant updatedAt) {
        this(id, recoveryCaseId, merchantId, attemptNumber, channel, status,
                scheduledAt, executedAt, completedAt, resultCode, resultMessage, recoveryLink,
                null, null, null, null, null, createdAt, updatedAt);
    }

    public RecoveryAttemptResponseDto(UUID id, UUID recoveryCaseId, UUID merchantId, int attemptNumber,
                                      RecoveryChannel channel, RecoveryAttemptStatus status,
                                      Instant scheduledAt, Instant executedAt, Instant completedAt,
                                      String resultCode, String resultMessage, String recoveryLink,
                                      UUID strategyId, RecoveryStrategySnapshot strategySnapshot,
                                      RecoveryPriority strategyPriority, BigDecimal confidenceScore,
                                      String recommendedAction,
                                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.recoveryCaseId = recoveryCaseId;
        this.merchantId = merchantId;
        this.attemptNumber = attemptNumber;
        this.channel = channel;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.executedAt = executedAt;
        this.completedAt = completedAt;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.recoveryLink = recoveryLink;
        this.strategyId = strategyId;
        this.strategySnapshot = strategySnapshot;
        this.strategyPriority = strategyPriority;
        this.confidenceScore = confidenceScore;
        this.recommendedAction = recommendedAction;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RecoveryAttemptResponseDto fromEntity(RecoveryAttempt entity) {
        if (entity == null) {
            return null;
        }

        UUID strategyId = entity.getStrategy() != null ? entity.getStrategy().getId() : null;
        RecoveryStrategySnapshot snapshot = RecoveryStrategySnapshot.fromJson(entity.getStrategySnapshot());
        if (strategyId == null && snapshot != null) {
            strategyId = snapshot.getStrategyId();
        }

        RecoveryPriority priority = null;
        BigDecimal confidence = null;
        String action = null;

        if (snapshot != null) {
            priority = snapshot.getPriority();
            confidence = snapshot.getConfidenceScore();
            action = snapshot.getRecommendedAction();
        } else if (entity.getStrategy() != null) {
            priority = entity.getStrategy().getPriority();
            confidence = entity.getStrategy().getConfidenceScore();
            action = entity.getStrategy().getRecommendedAction();
        }

        return new RecoveryAttemptResponseDto(
                entity.getId(),
                entity.getRecoveryCase() != null ? entity.getRecoveryCase().getId() : null,
                entity.getMerchant() != null ? entity.getMerchant().getId() : null,
                entity.getAttemptNumber(),
                entity.getChannel(),
                entity.getStatus(),
                entity.getScheduledAt(),
                entity.getExecutedAt(),
                entity.getCompletedAt(),
                entity.getResultCode(),
                entity.getResultMessage(),
                entity.getRecoveryLink(),
                strategyId,
                snapshot,
                priority,
                confidence,
                action,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRecoveryCaseId() {
        return recoveryCaseId;
    }

    public void setRecoveryCaseId(UUID recoveryCaseId) {
        this.recoveryCaseId = recoveryCaseId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public RecoveryChannel getChannel() {
        return channel;
    }

    public void setChannel(RecoveryChannel channel) {
        this.channel = channel;
    }

    public RecoveryAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryAttemptStatus status) {
        this.status = status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public String getRecoveryLink() {
        return recoveryLink;
    }

    public void setRecoveryLink(String recoveryLink) {
        this.recoveryLink = recoveryLink;
    }

    public UUID getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(UUID strategyId) {
        this.strategyId = strategyId;
    }

    public RecoveryStrategySnapshot getStrategySnapshot() {
        return strategySnapshot;
    }

    public void setStrategySnapshot(RecoveryStrategySnapshot strategySnapshot) {
        this.strategySnapshot = strategySnapshot;
    }

    public RecoveryPriority getStrategyPriority() {
        return strategyPriority;
    }

    public void setStrategyPriority(RecoveryPriority strategyPriority) {
        this.strategyPriority = strategyPriority;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
