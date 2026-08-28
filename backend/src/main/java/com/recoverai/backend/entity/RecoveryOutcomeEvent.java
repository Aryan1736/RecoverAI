package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.WebhookProcessingStatus;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recovery_outcome_events", uniqueConstraints = {
    @UniqueConstraint(name = "uq_recovery_outcome_merchant_provider_event", columnNames = {"merchant_id", "provider", "provider_event_id"})
})
public class RecoveryOutcomeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Merchant reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @NotNull(message = "Recovery attempt reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_attempt_id", nullable = false)
    private RecoveryAttempt recoveryAttempt;

    @NotBlank(message = "Provider is required")
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @NotBlank(message = "Provider event ID is required")
    @Column(name = "provider_event_id", nullable = false, length = 100)
    private String providerEventId;

    @NotNull(message = "Processing status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.PENDING;

    @NotBlank(message = "Payload hash is required")
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RecoveryOutcomeEvent() {
    }

    public RecoveryOutcomeEvent(UUID id, Merchant merchant, RecoveryAttempt recoveryAttempt,
                                String provider, String providerEventId, WebhookProcessingStatus processingStatus,
                                String payloadHash, String errorMessage, Instant createdAt,
                                Instant processedAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.recoveryAttempt = recoveryAttempt;
        this.provider = provider;
        this.providerEventId = providerEventId;
        this.processingStatus = processingStatus != null ? processingStatus : WebhookProcessingStatus.PENDING;
        this.payloadHash = payloadHash;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.updatedAt = updatedAt;
    }

    public static RecoveryOutcomeEventBuilder builder() {
        return new RecoveryOutcomeEventBuilder();
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
        if (processingStatus == null) {
            processingStatus = WebhookProcessingStatus.PENDING;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderEventId() {
        return providerEventId;
    }

    public void setProviderEventId(String providerEventId) {
        this.providerEventId = providerEventId;
    }

    public WebhookProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(WebhookProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
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
        RecoveryOutcomeEvent that = (RecoveryOutcomeEvent) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class RecoveryOutcomeEventBuilder {
        private UUID id;
        private Merchant merchant;
        private RecoveryAttempt recoveryAttempt;
        private String provider;
        private String providerEventId;
        private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.PENDING;
        private String payloadHash;
        private String errorMessage;
        private Instant createdAt;
        private Instant processedAt;
        private Instant updatedAt;

        RecoveryOutcomeEventBuilder() {
        }

        public RecoveryOutcomeEventBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RecoveryOutcomeEventBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public RecoveryOutcomeEventBuilder recoveryAttempt(RecoveryAttempt recoveryAttempt) {
            this.recoveryAttempt = recoveryAttempt;
            return this;
        }

        public RecoveryOutcomeEventBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public RecoveryOutcomeEventBuilder providerEventId(String providerEventId) {
            this.providerEventId = providerEventId;
            return this;
        }

        public RecoveryOutcomeEventBuilder processingStatus(WebhookProcessingStatus processingStatus) {
            this.processingStatus = processingStatus;
            return this;
        }

        public RecoveryOutcomeEventBuilder payloadHash(String payloadHash) {
            this.payloadHash = payloadHash;
            return this;
        }

        public RecoveryOutcomeEventBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public RecoveryOutcomeEventBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RecoveryOutcomeEventBuilder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public RecoveryOutcomeEventBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RecoveryOutcomeEvent build() {
            return new RecoveryOutcomeEvent(id, merchant, recoveryAttempt, provider, providerEventId,
                    processingStatus, payloadHash, errorMessage, createdAt, processedAt, updatedAt);
        }
    }
}
