package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.ActorType;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @NotBlank(message = "Event type is required")
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @NotNull(message = "Actor type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 50)
    private ActorType actorType;

    @Column(name = "actor_id", length = 100)
    private String actorId;

    @NotBlank(message = "Entity type is required")
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @NotBlank(message = "Entity ID is required")
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @NotBlank(message = "Action is required")
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditEvent() {
    }

    public AuditEvent(UUID id, Merchant merchant, String eventType, ActorType actorType,
                      String actorId, String entityType, String entityId, String action,
                      String details, String ipAddress, Instant createdAt) {
        this.id = id;
        this.merchant = merchant;
        this.eventType = eventType;
        this.actorType = actorType;
        this.actorId = actorId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditEvent that = (AuditEvent) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class AuditEventBuilder {
        private UUID id;
        private Merchant merchant;
        private String eventType;
        private ActorType actorType;
        private String actorId;
        private String entityType;
        private String entityId;
        private String action;
        private String details;
        private String ipAddress;
        private Instant createdAt;

        AuditEventBuilder() {
        }

        public AuditEventBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AuditEventBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public AuditEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public AuditEventBuilder actorType(ActorType actorType) {
            this.actorType = actorType;
            return this;
        }

        public AuditEventBuilder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }

        public AuditEventBuilder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public AuditEventBuilder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public AuditEventBuilder action(String action) {
            this.action = action;
            return this;
        }

        public AuditEventBuilder details(String details) {
            this.details = details;
            return this;
        }

        public AuditEventBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public AuditEventBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(id, merchant, eventType, actorType, actorId, entityType,
                    entityId, action, details, ipAddress, createdAt);
        }
    }
}
