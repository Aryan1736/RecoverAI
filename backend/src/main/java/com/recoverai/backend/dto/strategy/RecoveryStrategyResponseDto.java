package com.recoverai.backend.dto.strategy;

import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RecoveryStrategyResponseDto {

    private UUID id;
    private UUID recoveryCaseId;
    private UUID merchantId;
    private RecoveryChannel channel;
    private String recommendedAction;
    private BigDecimal confidenceScore;
    private RecoveryPriority priority;
    private int delaySeconds;
    private int maxAttempts;
    private String reason;
    private RecoveryChannel fallbackChannel;
    private String fallbackAction;
    private boolean terminal;
    private Instant createdAt;

    public RecoveryStrategyResponseDto() {
    }

    public RecoveryStrategyResponseDto(UUID id, UUID recoveryCaseId, UUID merchantId, RecoveryChannel channel,
                                       String recommendedAction, BigDecimal confidenceScore, RecoveryPriority priority,
                                       int delaySeconds, int maxAttempts, String reason, RecoveryChannel fallbackChannel,
                                       String fallbackAction, boolean terminal, Instant createdAt) {
        this.id = id;
        this.recoveryCaseId = recoveryCaseId;
        this.merchantId = merchantId;
        this.channel = channel;
        this.recommendedAction = recommendedAction;
        this.confidenceScore = confidenceScore;
        this.priority = priority;
        this.delaySeconds = delaySeconds;
        this.maxAttempts = maxAttempts;
        this.reason = reason;
        this.fallbackChannel = fallbackChannel;
        this.fallbackAction = fallbackAction;
        this.terminal = terminal;
        this.createdAt = createdAt;
    }

    public static RecoveryStrategyResponseDto fromEntity(RecoveryStrategy entity) {
        if (entity == null) {
            return null;
        }
        return new RecoveryStrategyResponseDto(
                entity.getId(),
                entity.getRecoveryCase() != null ? entity.getRecoveryCase().getId() : null,
                entity.getMerchant() != null ? entity.getMerchant().getId() : null,
                entity.getChannel(),
                entity.getRecommendedAction(),
                entity.getConfidenceScore(),
                entity.getPriority(),
                entity.getDelaySeconds(),
                entity.getMaxAttempts(),
                entity.getReason(),
                entity.getFallbackChannel(),
                entity.getFallbackAction(),
                entity.isTerminal(),
                entity.getCreatedAt()
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

    public RecoveryChannel getChannel() {
        return channel;
    }

    public void setChannel(RecoveryChannel channel) {
        this.channel = channel;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public RecoveryPriority getPriority() {
        return priority;
    }

    public void setPriority(RecoveryPriority priority) {
        this.priority = priority;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RecoveryChannel getFallbackChannel() {
        return fallbackChannel;
    }

    public void setFallbackChannel(RecoveryChannel fallbackChannel) {
        this.fallbackChannel = fallbackChannel;
    }

    public String getFallbackAction() {
        return fallbackAction;
    }

    public void setFallbackAction(String fallbackAction) {
        this.fallbackAction = fallbackAction;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
