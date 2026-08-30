package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
import com.recoverai.backend.entity.enums.NotificationStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Merchant is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Merchant merchant;

    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private MerchantNotificationEvent eventType;

    @NotBlank(message = "Title is required")
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank(message = "Message is required")
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_case_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private RecoveryCase recoveryCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_attempt_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
    private RecoveryAttempt recoveryAttempt;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<NotificationDelivery> deliveries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Notification() {
    }

    public Notification(UUID id, Merchant merchant, MerchantNotificationEvent eventType,
                        String title, String message, RecoveryCase recoveryCase,
                        RecoveryAttempt recoveryAttempt, NotificationStatus status,
                        String idempotencyKey, String metadata, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.recoveryCase = recoveryCase;
        this.recoveryAttempt = recoveryAttempt;
        this.status = status != null ? status : NotificationStatus.UNREAD;
        this.idempotencyKey = idempotencyKey;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationBuilder builder() {
        return new NotificationBuilder();
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
            status = NotificationStatus.UNREAD;
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

    public MerchantNotificationEvent getEventType() {
        return eventType;
    }

    public void setEventType(MerchantNotificationEvent eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RecoveryCase getRecoveryCase() {
        return recoveryCase;
    }

    public void setRecoveryCase(RecoveryCase recoveryCase) {
        this.recoveryCase = recoveryCase;
    }

    public RecoveryAttempt getRecoveryAttempt() {
        return recoveryAttempt;
    }

    public void setRecoveryAttempt(RecoveryAttempt recoveryAttempt) {
        this.recoveryAttempt = recoveryAttempt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public List<NotificationDelivery> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<NotificationDelivery> deliveries) {
        this.deliveries = deliveries != null ? deliveries : new ArrayList<>();
    }

    public void addDelivery(NotificationDelivery delivery) {
        if (delivery != null) {
            delivery.setNotification(this);
            this.deliveries.add(delivery);
        }
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
        Notification that = (Notification) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class NotificationBuilder {
        private UUID id;
        private Merchant merchant;
        private MerchantNotificationEvent eventType;
        private String title;
        private String message;
        private RecoveryCase recoveryCase;
        private RecoveryAttempt recoveryAttempt;
        private NotificationStatus status = NotificationStatus.UNREAD;
        private String idempotencyKey;
        private String metadata;
        private Instant createdAt;
        private Instant updatedAt;

        NotificationBuilder() {
        }

        public NotificationBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public NotificationBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public NotificationBuilder eventType(MerchantNotificationEvent eventType) {
            this.eventType = eventType;
            return this;
        }

        public NotificationBuilder title(String title) {
            this.title = title;
            return this;
        }

        public NotificationBuilder message(String message) {
            this.message = message;
            return this;
        }

        public NotificationBuilder recoveryCase(RecoveryCase recoveryCase) {
            this.recoveryCase = recoveryCase;
            return this;
        }

        public NotificationBuilder recoveryAttempt(RecoveryAttempt recoveryAttempt) {
            this.recoveryAttempt = recoveryAttempt;
            return this;
        }

        public NotificationBuilder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public NotificationBuilder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public NotificationBuilder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public NotificationBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NotificationBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Notification build() {
            return new Notification(id, merchant, eventType, title, message, recoveryCase, recoveryAttempt,
                    status, idempotencyKey, metadata, createdAt, updatedAt);
        }
    }
}
