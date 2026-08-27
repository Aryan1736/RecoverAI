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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "razorpay_event_id", length = 100)
    private String razorpayEventId;

    @NotBlank(message = "Event type is required")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @NotBlank(message = "Payload hash is required")
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @NotNull(message = "Processing status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 50)
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public WebhookEvent() {
    }

    public WebhookEvent(UUID id, Merchant merchant, String razorpayEventId, String eventType,
                        String payloadHash, WebhookProcessingStatus processingStatus,
                        String errorMessage, Instant receivedAt, Instant processedAt,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.razorpayEventId = razorpayEventId;
        this.eventType = eventType;
        this.payloadHash = payloadHash;
        this.processingStatus = processingStatus != null ? processingStatus : WebhookProcessingStatus.PENDING;
        this.errorMessage = errorMessage;
        this.receivedAt = receivedAt != null ? receivedAt : Instant.now();
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WebhookEventBuilder builder() {
        return new WebhookEventBuilder();
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
        if (receivedAt == null) {
            receivedAt = now;
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

    public String getRazorpayEventId() {
        return razorpayEventId;
    }

    public void setRazorpayEventId(String razorpayEventId) {
        this.razorpayEventId = razorpayEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public WebhookProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(WebhookProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
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
        WebhookEvent that = (WebhookEvent) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class WebhookEventBuilder {
        private UUID id;
        private Merchant merchant;
        private String razorpayEventId;
        private String eventType;
        private String payloadHash;
        private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.PENDING;
        private String errorMessage;
        private Instant receivedAt;
        private Instant processedAt;
        private Instant createdAt;
        private Instant updatedAt;

        WebhookEventBuilder() {
        }

        public WebhookEventBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public WebhookEventBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public WebhookEventBuilder razorpayEventId(String razorpayEventId) {
            this.razorpayEventId = razorpayEventId;
            return this;
        }

        public WebhookEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public WebhookEventBuilder payloadHash(String payloadHash) {
            this.payloadHash = payloadHash;
            return this;
        }

        public WebhookEventBuilder processingStatus(WebhookProcessingStatus processingStatus) {
            this.processingStatus = processingStatus;
            return this;
        }

        public WebhookEventBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public WebhookEventBuilder receivedAt(Instant receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public WebhookEventBuilder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public WebhookEventBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public WebhookEventBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public WebhookEvent build() {
            return new WebhookEvent(id, merchant, razorpayEventId, eventType, payloadHash,
                    processingStatus, errorMessage, receivedAt, processedAt, createdAt, updatedAt);
        }
    }
}
