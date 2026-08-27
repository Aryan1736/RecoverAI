package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recovery_attempts", uniqueConstraints = {
    @UniqueConstraint(name = "uq_recovery_attempts_case_attempt_num", columnNames = {"recovery_case_id", "attempt_number"})
})
public class RecoveryAttempt {

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

    @Positive(message = "Attempt number must be positive")
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @NotNull(message = "Recovery channel is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private RecoveryChannel channel;

    @NotNull(message = "Attempt status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecoveryAttemptStatus status = RecoveryAttemptStatus.SCHEDULED;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "result_code")
    private String resultCode;

    @Column(name = "result_message", columnDefinition = "TEXT")
    private String resultMessage;

    @Column(name = "recovery_link", length = 1000)
    private String recoveryLink;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RecoveryAttempt() {
    }

    public RecoveryAttempt(UUID id, RecoveryCase recoveryCase, Merchant merchant, int attemptNumber,
                           RecoveryChannel channel, RecoveryAttemptStatus status, Instant scheduledAt,
                           Instant executedAt, Instant completedAt, String resultCode,
                           String resultMessage, String recoveryLink, String metadata,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.recoveryCase = recoveryCase;
        this.merchant = merchant;
        this.attemptNumber = attemptNumber;
        this.channel = channel;
        this.status = status != null ? status : RecoveryAttemptStatus.SCHEDULED;
        this.scheduledAt = scheduledAt;
        this.executedAt = executedAt;
        this.completedAt = completedAt;
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
        this.recoveryLink = recoveryLink;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RecoveryAttemptBuilder builder() {
        return new RecoveryAttemptBuilder();
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
        if (status == null) {
            status = RecoveryAttemptStatus.SCHEDULED;
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

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
        RecoveryAttempt that = (RecoveryAttempt) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class RecoveryAttemptBuilder {
        private UUID id;
        private RecoveryCase recoveryCase;
        private Merchant merchant;
        private int attemptNumber = 1;
        private RecoveryChannel channel;
        private RecoveryAttemptStatus status = RecoveryAttemptStatus.SCHEDULED;
        private Instant scheduledAt;
        private Instant executedAt;
        private Instant completedAt;
        private String resultCode;
        private String resultMessage;
        private String recoveryLink;
        private String metadata;
        private Instant createdAt;
        private Instant updatedAt;

        RecoveryAttemptBuilder() {
        }

        public RecoveryAttemptBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RecoveryAttemptBuilder recoveryCase(RecoveryCase recoveryCase) {
            this.recoveryCase = recoveryCase;
            return this;
        }

        public RecoveryAttemptBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public RecoveryAttemptBuilder attemptNumber(int attemptNumber) {
            this.attemptNumber = attemptNumber;
            return this;
        }

        public RecoveryAttemptBuilder channel(RecoveryChannel channel) {
            this.channel = channel;
            return this;
        }

        public RecoveryAttemptBuilder status(RecoveryAttemptStatus status) {
            this.status = status;
            return this;
        }

        public RecoveryAttemptBuilder scheduledAt(Instant scheduledAt) {
            this.scheduledAt = scheduledAt;
            return this;
        }

        public RecoveryAttemptBuilder executedAt(Instant executedAt) {
            this.executedAt = executedAt;
            return this;
        }

        public RecoveryAttemptBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public RecoveryAttemptBuilder resultCode(String resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public RecoveryAttemptBuilder resultMessage(String resultMessage) {
            this.resultMessage = resultMessage;
            return this;
        }

        public RecoveryAttemptBuilder recoveryLink(String recoveryLink) {
            this.recoveryLink = recoveryLink;
            return this;
        }

        public RecoveryAttemptBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public RecoveryAttemptBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RecoveryAttemptBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RecoveryAttempt build() {
            return new RecoveryAttempt(id, recoveryCase, merchant, attemptNumber, channel,
                    status, scheduledAt, executedAt, completedAt, resultCode, resultMessage,
                    recoveryLink, metadata, createdAt, updatedAt);
        }
    }
}
