package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.RecoveryQueueStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recovery_execution_queue", uniqueConstraints = {
        @UniqueConstraint(name = "uq_recovery_queue_attempt", columnNames = {"recovery_attempt_id"})
})
public class RecoveryExecutionQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Merchant reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @NotNull(message = "Recovery attempt reference is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_attempt_id", nullable = false, unique = true)
    private RecoveryAttempt recoveryAttempt;

    @NotNull(message = "Recovery case reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_case_id", nullable = false)
    private RecoveryCase recoveryCase;

    @NotNull(message = "Queue status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecoveryQueueStatus status = RecoveryQueueStatus.READY;

    @NotNull(message = "Available at timestamp is required")
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by", length = 255)
    private String claimedBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RecoveryExecutionQueueItem() {
    }

    public RecoveryExecutionQueueItem(UUID id, Merchant merchant, RecoveryAttempt recoveryAttempt,
                                     RecoveryCase recoveryCase, RecoveryQueueStatus status,
                                     Instant availableAt, Instant claimedAt, String claimedBy,
                                     Instant startedAt, Instant completedAt, int retryCount,
                                     int maxRetries, String lastErrorCode, String lastErrorMessage,
                                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.recoveryAttempt = recoveryAttempt;
        this.recoveryCase = recoveryCase;
        this.status = status != null ? status : RecoveryQueueStatus.READY;
        this.availableAt = availableAt != null ? availableAt : Instant.now();
        this.claimedAt = claimedAt;
        this.claimedBy = claimedBy;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RecoveryExecutionQueueItemBuilder builder() {
        return new RecoveryExecutionQueueItemBuilder();
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
        if (availableAt == null) {
            availableAt = now;
        }
        if (status == null) {
            status = RecoveryQueueStatus.READY;
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

    public RecoveryAttempt getRecoveryAttempt() {
        return recoveryAttempt;
    }

    public void setRecoveryAttempt(RecoveryAttempt recoveryAttempt) {
        this.recoveryAttempt = recoveryAttempt;
    }

    public RecoveryCase getRecoveryCase() {
        return recoveryCase;
    }

    public void setRecoveryCase(RecoveryCase recoveryCase) {
        this.recoveryCase = recoveryCase;
    }

    public RecoveryQueueStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryQueueStatus status) {
        this.status = status;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public void setAvailableAt(Instant availableAt) {
        this.availableAt = availableAt;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
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
        RecoveryExecutionQueueItem that = (RecoveryExecutionQueueItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RecoveryExecutionQueueItem{" +
                "id=" + id +
                ", status=" + status +
                ", availableAt=" + availableAt +
                ", retryCount=" + retryCount +
                ", maxRetries=" + maxRetries +
                ", claimedBy='" + claimedBy + '\'' +
                '}';
    }

    public static class RecoveryExecutionQueueItemBuilder {
        private UUID id;
        private Merchant merchant;
        private RecoveryAttempt recoveryAttempt;
        private RecoveryCase recoveryCase;
        private RecoveryQueueStatus status = RecoveryQueueStatus.READY;
        private Instant availableAt;
        private Instant claimedAt;
        private String claimedBy;
        private Instant startedAt;
        private Instant completedAt;
        private int retryCount = 0;
        private int maxRetries = 3;
        private String lastErrorCode;
        private String lastErrorMessage;
        private Instant createdAt;
        private Instant updatedAt;

        RecoveryExecutionQueueItemBuilder() {
        }

        public RecoveryExecutionQueueItemBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder recoveryAttempt(RecoveryAttempt recoveryAttempt) {
            this.recoveryAttempt = recoveryAttempt;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder recoveryCase(RecoveryCase recoveryCase) {
            this.recoveryCase = recoveryCase;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder status(RecoveryQueueStatus status) {
            this.status = status;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder availableAt(Instant availableAt) {
            this.availableAt = availableAt;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder claimedAt(Instant claimedAt) {
            this.claimedAt = claimedAt;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder claimedBy(String claimedBy) {
            this.claimedBy = claimedBy;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder lastErrorCode(String lastErrorCode) {
            this.lastErrorCode = lastErrorCode;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder lastErrorMessage(String lastErrorMessage) {
            this.lastErrorMessage = lastErrorMessage;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RecoveryExecutionQueueItemBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RecoveryExecutionQueueItem build() {
            return new RecoveryExecutionQueueItem(
                    id, merchant, recoveryAttempt, recoveryCase, status,
                    availableAt, claimedAt, claimedBy, startedAt, completedAt,
                    retryCount, maxRetries, lastErrorCode, lastErrorMessage,
                    createdAt, updatedAt
            );
        }
    }
}
