package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.MerchantNotificationChannel;
import com.recoverai.backend.entity.enums.MerchantNotificationEvent;
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "merchant_notification_preferences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_notification_pref_merchant_event_channel",
                        columnNames = {"merchant_id", "event_type", "channel"}
                )
        }
)
public class NotificationPreference {

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

    @NotNull(message = "Channel is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private MerchantNotificationChannel channel;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationPreference() {
    }

    public NotificationPreference(UUID id, Merchant merchant, MerchantNotificationEvent eventType,
                                  MerchantNotificationChannel channel, boolean enabled,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.eventType = eventType;
        this.channel = channel;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NotificationPreferenceBuilder builder() {
        return new NotificationPreferenceBuilder();
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

    public MerchantNotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(MerchantNotificationChannel channel) {
        this.channel = channel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        NotificationPreference that = (NotificationPreference) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class NotificationPreferenceBuilder {
        private UUID id;
        private Merchant merchant;
        private MerchantNotificationEvent eventType;
        private MerchantNotificationChannel channel;
        private boolean enabled = true;
        private Instant createdAt;
        private Instant updatedAt;

        NotificationPreferenceBuilder() {
        }

        public NotificationPreferenceBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public NotificationPreferenceBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public NotificationPreferenceBuilder eventType(MerchantNotificationEvent eventType) {
            this.eventType = eventType;
            return this;
        }

        public NotificationPreferenceBuilder channel(MerchantNotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public NotificationPreferenceBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public NotificationPreferenceBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NotificationPreferenceBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public NotificationPreference build() {
            return new NotificationPreference(id, merchant, eventType, channel, enabled, createdAt, updatedAt);
        }
    }
}
