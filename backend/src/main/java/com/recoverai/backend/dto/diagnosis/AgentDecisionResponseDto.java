package com.recoverai.backend.dto.diagnosis;

import com.recoverai.backend.entity.AgentDecision;
import com.recoverai.backend.entity.enums.RecoveryChannel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AgentDecisionResponseDto {

    private UUID id;
    private UUID recoveryCaseId;
    private UUID merchantId;
    private String recommendedAction;
    private RecoveryChannel channel;
    private BigDecimal confidenceScore;
    private String reasoning;
    private String modelName;
    private String modelVersion;
    private Integer promptTokens;
    private Integer completionTokens;
    private String decisionFactors;
    private Instant createdAt;

    public AgentDecisionResponseDto() {
    }

    public AgentDecisionResponseDto(UUID id, UUID recoveryCaseId, UUID merchantId, String recommendedAction,
                                    RecoveryChannel channel, BigDecimal confidenceScore, String reasoning,
                                    String modelName, String modelVersion, Integer promptTokens,
                                    Integer completionTokens, String decisionFactors, Instant createdAt) {
        this.id = id;
        this.recoveryCaseId = recoveryCaseId;
        this.merchantId = merchantId;
        this.recommendedAction = recommendedAction;
        this.channel = channel;
        this.confidenceScore = confidenceScore;
        this.reasoning = reasoning;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.decisionFactors = decisionFactors;
        this.createdAt = createdAt;
    }

    public static AgentDecisionResponseDto fromEntity(AgentDecision decision) {
        if (decision == null) {
            return null;
        }
        return new AgentDecisionResponseDto(
                decision.getId(),
                decision.getRecoveryCase() != null ? decision.getRecoveryCase().getId() : null,
                decision.getMerchant() != null ? decision.getMerchant().getId() : null,
                decision.getRecommendedAction(),
                decision.getChannel(),
                decision.getConfidenceScore(),
                decision.getReasoning(),
                decision.getModelName(),
                decision.getModelVersion(),
                decision.getPromptTokens(),
                decision.getCompletionTokens(),
                decision.getDecisionFactors(),
                decision.getCreatedAt()
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

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public RecoveryChannel getChannel() {
        return channel;
    }

    public void setChannel(RecoveryChannel channel) {
        this.channel = channel;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public String getDecisionFactors() {
        return decisionFactors;
    }

    public void setDecisionFactors(String decisionFactors) {
        this.decisionFactors = decisionFactors;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
