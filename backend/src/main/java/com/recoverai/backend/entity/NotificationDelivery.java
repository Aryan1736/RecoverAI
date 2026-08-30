package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.NotificationDeliveryStatus;
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
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Notification is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Notification notification;

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Merchant merchant;

    @NotNull(message = "Channel is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private MerchantNotificationChannel channel;

    @Column(name = "provider", length = 100)
    private String provider;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "attempted_at")
    private Instant attemptedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationDelivery() {
    }

    public NotificationDelivery(UUID id, Notification notification, Merchant merchant,
                                MerchantNotificationChannel channel, String provider,
                                NotificationDeliveryStatus status, String providerMessageId,
                                String errorCode, String errorMessage, int retryCount,
                                int maxRetries, Instant attemptedAt, Instant deliveredAt,
                                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.notification = notification;
        this.merchant = merchant;
        this.channel = channel;
        this.provider = provider;
        this.status = status != null ? status : NotificationDeliveryStatus.PENDING;
        this.providerMessageId = providerMessageId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.attemptedAt = attemptedAt;
        this.deliveredAt = deliveredAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationDeliveryBuilder builder() {
        return new NotificationDeliveryBuilder();
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
            status = NotificationDeliveryStatus.PENDING;
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

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public Merchant getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant merchant) {
        this.merchant = merchant;
    }

    public MerchantNotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(MerchantNotificationChannel channel) {
        this.channel = channel;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationDeliveryStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
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
        NotificationDelivery that = (NotificationDelivery) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class NotificationDeliveryBuilder {
        private UUID id;
        private Notification notification;
        private Merchant merchant;
        private MerchantNotificationChannel channel;
        private String provider;
        private NotificationDeliveryStatus status = NotificationDeliveryStatus.PENDING;
        private String providerMessageId;
        private String errorCode;
        private String errorMessage;
        private int retryCount = 0;
        private int maxRetries = 3;
        private Instant attemptedAt;
        private Instant deliveredAt;
        private Instant createdAt;
        private Instant updatedAt;

        NotificationDeliveryBuilder() {
        }

        public NotificationDeliveryBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public NotificationDeliveryBuilder notification(Notification notification) {
            this.notification = notification;
            return this;
        }

        public NotificationDeliveryBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public NotificationDeliveryBuilder channel(MerchantNotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public NotificationDeliveryBuilder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public NotificationDeliveryBuilder status(NotificationDeliveryStatus status) {
            this.status = status;
            return this;
        }

        public NotificationDeliveryBuilder providerMessageId(String providerMessageId) {
            this.providerMessageId = providerMessageId;
            return this;
        }

        public NotificationDeliveryBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public NotificationDeliveryBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public NotificationDeliveryBuilder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public NotificationDeliveryBuilder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public NotificationDeliveryBuilder attemptedAt(Instant attemptedAt) {
            this.attemptedAt = attemptedAt;
            return this;
        }

        public NotificationDeliveryBuilder deliveredAt(Instant deliveredAt) {
            this.deliveredAt = deliveredAt;
            return this;
        }

        public NotificationDeliveryBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NotificationDeliveryBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public NotificationDelivery build() {
            return new NotificationDelivery(id, notification, merchant, channel, provider,
                    status, providerMessageId, errorCode, errorMessage, retryCount, maxRetries,
                    attemptedAt, deliveredAt, createdAt, updatedAt);
        }
    }
}
