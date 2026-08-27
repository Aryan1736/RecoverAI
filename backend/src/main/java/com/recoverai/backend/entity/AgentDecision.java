package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.RecoveryChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "agent_decisions")
public class AgentDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Recovery case reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_case_id", nullable = false)
    private RecoveryCase recoveryCase;

    @NotNull(message = "Merchant reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @NotBlank(message = "Recommended action is required")
    @Column(name = "recommended_action", nullable = false)
    private String recommendedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 50)
    private RecoveryChannel channel;

    @DecimalMin(value = "0.0", message = "Confidence score must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Confidence score must be at most 1.0")
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @NotBlank(message = "Reasoning is required")
    @Column(name = "reasoning", nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @NotBlank(message = "Model name is required")
    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "decision_factors", columnDefinition = "TEXT")
    private String decisionFactors;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AgentDecision() {
    }

    public AgentDecision(UUID id, RecoveryCase recoveryCase, Merchant merchant, String recommendedAction,
                         RecoveryChannel channel, BigDecimal confidenceScore, String reasoning,
                         String modelName, String modelVersion, Integer promptTokens,
                         Integer completionTokens, String rawResponse, String decisionFactors,
                         Instant createdAt) {
        this.id = id;
        this.recoveryCase = recoveryCase;
        this.merchant = merchant;
        this.recommendedAction = recommendedAction;
        this.channel = channel;
        this.confidenceScore = confidenceScore;
        this.reasoning = reasoning;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.rawResponse = rawResponse;
        this.decisionFactors = decisionFactors;
        this.createdAt = createdAt;
    }

    public static AgentDecisionBuilder builder() {
        return new AgentDecisionBuilder();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RecoveryCase getRecoveryCase() {
        return recoveryCase;
    }

    public void setRecoveryCase(RecoveryCase recoveryCase) {
        this.recoveryCase = recoveryCase;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
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

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentDecision that = (AgentDecision) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class AgentDecisionBuilder {
        private UUID id;
        private RecoveryCase recoveryCase;
        private Merchant merchant;
        private String recommendedAction;
        private RecoveryChannel channel;
        private BigDecimal confidenceScore;
        private String reasoning;
        private String modelName;
        private String modelVersion;
        private Integer promptTokens;
        private Integer completionTokens;
        private String rawResponse;
        private String decisionFactors;
        private Instant createdAt;

        AgentDecisionBuilder() {
        }

        public AgentDecisionBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AgentDecisionBuilder recoveryCase(RecoveryCase recoveryCase) {
            this.recoveryCase = recoveryCase;
            return this;
        }

        public AgentDecisionBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public AgentDecisionBuilder recommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
            return this;
        }

        public AgentDecisionBuilder channel(RecoveryChannel channel) {
            this.channel = channel;
            return this;
        }

        public AgentDecisionBuilder confidenceScore(BigDecimal confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public AgentDecisionBuilder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public AgentDecisionBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public AgentDecisionBuilder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        public AgentDecisionBuilder promptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public AgentDecisionBuilder completionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public AgentDecisionBuilder rawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        public AgentDecisionBuilder decisionFactors(String decisionFactors) {
            this.decisionFactors = decisionFactors;
            return this;
        }

        public AgentDecisionBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AgentDecision build() {
            return new AgentDecision(id, recoveryCase, merchant, recommendedAction, channel,
                    confidenceScore, reasoning, modelName, modelVersion, promptTokens,
                    completionTokens, rawResponse, decisionFactors, createdAt);
        }
    }
}
