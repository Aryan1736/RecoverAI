package com.recoverai.backend.dto.strategy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recoverai.backend.entity.RecoveryStrategy;
import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecoveryStrategySnapshot {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private UUID strategyId;
    private RecoveryChannel channel;
    private String recommendedAction;
    private BigDecimal confidenceScore;
    private RecoveryPriority priority;
    private RecoveryChannel fallbackChannel;
    private String fallbackAction;
    private String reason;

    public RecoveryStrategySnapshot() {
    }

    public RecoveryStrategySnapshot(UUID strategyId,
                                    RecoveryChannel channel,
                                    String recommendedAction,
                                    BigDecimal confidenceScore,
                                    RecoveryPriority priority,
                                    RecoveryChannel fallbackChannel,
                                    String fallbackAction,
                                    String reason) {
        this.strategyId = strategyId;
        this.channel = channel;
        this.recommendedAction = recommendedAction;
        this.confidenceScore = confidenceScore;
        this.priority = priority;
        this.fallbackChannel = fallbackChannel;
        this.fallbackAction = fallbackAction;
        this.reason = reason;
    }

    public static RecoveryStrategySnapshot fromStrategy(RecoveryStrategy strategy) {
        if (strategy == null) {
            return null;
        }
        return builder()
                .strategyId(strategy.getId())
                .channel(strategy.getChannel())
                .recommendedAction(strategy.getRecommendedAction())
                .confidenceScore(strategy.getConfidenceScore())
                .priority(strategy.getPriority())
                .fallbackChannel(strategy.getFallbackChannel())
                .fallbackAction(strategy.getFallbackAction())
                .reason(strategy.getReason())
                .build();
    }

    public static RecoveryStrategySnapshotBuilder builder() {
        return new RecoveryStrategySnapshotBuilder();
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return String.format("{\"strategyId\":\"%s\",\"channel\":\"%s\",\"recommendedAction\":\"%s\"}",
                    strategyId, channel, recommendedAction);
        }
    }

    public static RecoveryStrategySnapshot fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, RecoveryStrategySnapshot.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public UUID getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(UUID strategyId) {
        this.strategyId = strategyId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecoveryStrategySnapshot that = (RecoveryStrategySnapshot) o;
        return Objects.equals(strategyId, that.strategyId) &&
                channel == that.channel &&
                Objects.equals(recommendedAction, that.recommendedAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategyId, channel, recommendedAction);
    }

    public static class RecoveryStrategySnapshotBuilder {
        private UUID strategyId;
        private RecoveryChannel channel;
        private String recommendedAction;
        private BigDecimal confidenceScore;
        private RecoveryPriority priority;
        private RecoveryChannel fallbackChannel;
        private String fallbackAction;
        private String reason;

        RecoveryStrategySnapshotBuilder() {
        }

        public RecoveryStrategySnapshotBuilder strategyId(UUID strategyId) {
            this.strategyId = strategyId;
            return this;
        }

        public RecoveryStrategySnapshotBuilder channel(RecoveryChannel channel) {
            this.channel = channel;
            return this;
        }

        public RecoveryStrategySnapshotBuilder recommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
            return this;
        }

        public RecoveryStrategySnapshotBuilder confidenceScore(BigDecimal confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public RecoveryStrategySnapshotBuilder priority(RecoveryPriority priority) {
            this.priority = priority;
            return this;
        }

        public RecoveryStrategySnapshotBuilder fallbackChannel(RecoveryChannel fallbackChannel) {
            this.fallbackChannel = fallbackChannel;
            return this;
        }

        public RecoveryStrategySnapshotBuilder fallbackAction(String fallbackAction) {
            this.fallbackAction = fallbackAction;
            return this;
        }

        public RecoveryStrategySnapshotBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public RecoveryStrategySnapshot build() {
            return new RecoveryStrategySnapshot(strategyId, channel, recommendedAction,
                    confidenceScore, priority, fallbackChannel, fallbackAction, reason);
        }
    }
}
