package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.RecoveryChannel;
import com.recoverai.backend.entity.enums.RecoveryPriority;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recovery_strategies")
public class RecoveryStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Merchant reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @NotNull(message = "Recovery case reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_case_id", nullable = false)
    private RecoveryCase recoveryCase;

    @NotNull(message = "Recovery channel is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private RecoveryChannel channel;

    @NotBlank(message = "Recommended action is required")
    @Column(name = "recommended_action", nullable = false, length = 100)
    private String recommendedAction;

    @DecimalMin(value = "0.0", message = "Confidence score must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Confidence score must be at most 1.0")
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @NotNull(message = "Recovery priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private RecoveryPriority priority = RecoveryPriority.MEDIUM;

    @PositiveOrZero(message = "Delay seconds must be non-negative")
    @Column(name = "delay_seconds", nullable = false)
    private int delaySeconds = 0;

    @Positive(message = "Max attempts must be positive")
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @NotBlank(message = "Reason is required")
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_channel", length = 50)
    private RecoveryChannel fallbackChannel;

    @Column(name = "fallback_action", length = 100)
    private String fallbackAction;

    @Column(name = "is_terminal", nullable = false)
    private boolean isTerminal = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RecoveryStrategy() {
    }

    public RecoveryStrategy(UUID id, Merchant merchant, RecoveryCase recoveryCase, RecoveryChannel channel,
                            String recommendedAction, BigDecimal confidenceScore, RecoveryPriority priority,
                            int delaySeconds, int maxAttempts, String reason, RecoveryChannel fallbackChannel,
                            String fallbackAction, boolean isTerminal, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.recoveryCase = recoveryCase;
        this.channel = channel;
        this.recommendedAction = recommendedAction;
        this.confidenceScore = confidenceScore;
        this.priority = priority != null ? priority : RecoveryPriority.MEDIUM;
        this.delaySeconds = delaySeconds;
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : 3;
        this.reason = reason;
        this.fallbackChannel = fallbackChannel;
        this.fallbackAction = fallbackAction;
        this.isTerminal = isTerminal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RecoveryStrategyBuilder builder() {
        return new RecoveryStrategyBuilder();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (priority == null) {
            priority = RecoveryPriority.MEDIUM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public RecoveryCase getRecoveryCase() {
        return recoveryCase;
    }

    public void setRecoveryCase(RecoveryCase recoveryCase) {
        this.recoveryCase = recoveryCase;
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
        return isTerminal;
    }

    public void setTerminal(boolean terminal) {
        isTerminal = terminal;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecoveryStrategy that = (RecoveryStrategy) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class RecoveryStrategyBuilder {
        private UUID id;
        private Merchant merchant;
        private RecoveryCase recoveryCase;
        private RecoveryChannel channel;
        private String recommendedAction;
        private BigDecimal confidenceScore;
        private RecoveryPriority priority = RecoveryPriority.MEDIUM;
        private int delaySeconds = 0;
        private int maxAttempts = 3;
        private String reason;
        private RecoveryChannel fallbackChannel;
        private String fallbackAction;
        private boolean isTerminal = false;
        private Instant createdAt;
        private Instant updatedAt;

        RecoveryStrategyBuilder() {
        }

        public RecoveryStrategyBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RecoveryStrategyBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public RecoveryStrategyBuilder recoveryCase(RecoveryCase recoveryCase) {
            this.recoveryCase = recoveryCase;
            return this;
        }

        public RecoveryStrategyBuilder channel(RecoveryChannel channel) {
            this.channel = channel;
            return this;
        }

        public RecoveryStrategyBuilder recommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
            return this;
        }

        public RecoveryStrategyBuilder confidenceScore(BigDecimal confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public RecoveryStrategyBuilder priority(RecoveryPriority priority) {
            this.priority = priority;
            return this;
        }

        public RecoveryStrategyBuilder delaySeconds(int delaySeconds) {
            this.delaySeconds = delaySeconds;
            return this;
        }

        public RecoveryStrategyBuilder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public RecoveryStrategyBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public RecoveryStrategyBuilder fallbackChannel(RecoveryChannel fallbackChannel) {
            this.fallbackChannel = fallbackChannel;
            return this;
        }

        public RecoveryStrategyBuilder fallbackAction(String fallbackAction) {
            this.fallbackAction = fallbackAction;
            return this;
        }

        public RecoveryStrategyBuilder isTerminal(boolean isTerminal) {
            this.isTerminal = isTerminal;
            return this;
        }

        public RecoveryStrategyBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RecoveryStrategyBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RecoveryStrategy build() {
            return new RecoveryStrategy(id, merchant, recoveryCase, channel, recommendedAction,
                    confidenceScore, priority, delaySeconds, maxAttempts, reason,
                    fallbackChannel, fallbackAction, isTerminal, createdAt, updatedAt);
        }
    }
}
