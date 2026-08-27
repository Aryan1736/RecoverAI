package com.recoverai.backend.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.recoverai.backend.entity.enums.RecoveryChannel;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredDiagnosisResponse {

    private String recommendedAction;
    private RecoveryChannel recommendedChannel;
    private BigDecimal confidenceScore;
    private String reasoning;
    private String decisionFactors;
    private String modelName;
    private String modelVersion;
    private Integer promptTokens;
    private Integer completionTokens;
    private String rawResponse;

    public StructuredDiagnosisResponse() {
    }

    public StructuredDiagnosisResponse(String recommendedAction, RecoveryChannel recommendedChannel,
                                       BigDecimal confidenceScore, String reasoning, String decisionFactors,
                                       String modelName, String modelVersion, Integer promptTokens,
                                       Integer completionTokens, String rawResponse) {
        this.recommendedAction = recommendedAction;
        this.recommendedChannel = recommendedChannel;
        this.confidenceScore = confidenceScore;
        this.reasoning = reasoning;
        this.decisionFactors = decisionFactors;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.rawResponse = rawResponse;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public RecoveryChannel getRecommendedChannel() {
        return recommendedChannel;
    }

    public void setRecommendedChannel(RecoveryChannel recommendedChannel) {
        this.recommendedChannel = recommendedChannel;
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

    public String getDecisionFactors() {
        return decisionFactors;
    }

    public void setDecisionFactors(String decisionFactors) {
        this.decisionFactors = decisionFactors;
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

    public static class Builder {
        private String recommendedAction;
        private RecoveryChannel recommendedChannel;
        private BigDecimal confidenceScore;
        private String reasoning;
        private String decisionFactors;
        private String modelName;
        private String modelVersion;
        private Integer promptTokens;
        private Integer completionTokens;
        private String rawResponse;

        public Builder recommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
            return this;
        }

        public Builder recommendedChannel(RecoveryChannel recommendedChannel) {
            this.recommendedChannel = recommendedChannel;
            return this;
        }

        public Builder confidenceScore(BigDecimal confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public Builder decisionFactors(String decisionFactors) {
            this.decisionFactors = decisionFactors;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }

        public Builder promptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder completionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public Builder rawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        public StructuredDiagnosisResponse build() {
            return new StructuredDiagnosisResponse(recommendedAction, recommendedChannel,
                    confidenceScore, reasoning, decisionFactors, modelName, modelVersion,
                    promptTokens, completionTokens, rawResponse);
        }
    }
}
