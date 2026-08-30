package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.MerchantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Merchant name is required")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Merchant email is required")
    @Email(message = "Merchant email must be valid")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "razorpay_account_id", unique = true)
    private String razorpayAccountId;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "password_hash")
    private String passwordHash;

    @NotNull(message = "Merchant status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MerchantStatus status = MerchantStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Merchant() {
    }

    public Merchant(UUID id, String name, String email, String razorpayAccountId, String webhookSecret,
                    MerchantStatus status, Instant createdAt, Instant updatedAt) {
        this(id, name, email, razorpayAccountId, webhookSecret, null, null, status, createdAt, updatedAt);
    }

    public Merchant(UUID id, String name, String email, String razorpayAccountId, String webhookSecret,
                    String passwordHash, MerchantStatus status, Instant createdAt, Instant updatedAt) {
        this(id, name, email, razorpayAccountId, webhookSecret, passwordHash, null, status, createdAt, updatedAt);
    }

    public Merchant(UUID id, String name, String email, String razorpayAccountId, String webhookSecret,
                    String passwordHash, String webhookUrl, MerchantStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.razorpayAccountId = razorpayAccountId;
        this.webhookSecret = webhookSecret;
        this.passwordHash = passwordHash;
        this.webhookUrl = webhookUrl;
        this.status = status != null ? status : MerchantStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MerchantBuilder builder() {
        return new MerchantBuilder();
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
            status = MerchantStatus.ACTIVE;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRazorpayAccountId() {
        return razorpayAccountId;
    }

    public void setRazorpayAccountId(String razorpayAccountId) {
        this.razorpayAccountId = razorpayAccountId;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public void setStatus(MerchantStatus status) {
        this.status = status;
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
        Merchant merchant = (Merchant) o;
        return id != null && Objects.equals(id, merchant.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class MerchantBuilder {
        private UUID id;
        private String name;
        private String email;
        private String razorpayAccountId;
        private String webhookSecret;
        private String webhookUrl;
        private String passwordHash;
        private MerchantStatus status = MerchantStatus.ACTIVE;
        private Instant createdAt;
        private Instant updatedAt;

        MerchantBuilder() {
        }

        public MerchantBuilder id(UUID id) {
            this.id = id;
            return this;
            }

        public MerchantBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MerchantBuilder email(String email) {
            this.email = email;
            return this;
        }

        public MerchantBuilder razorpayAccountId(String razorpayAccountId) {
            this.razorpayAccountId = razorpayAccountId;
            return this;
        }

        public MerchantBuilder webhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
            return this;
        }

        public MerchantBuilder webhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public MerchantBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public MerchantBuilder status(MerchantStatus status) {
            this.status = status;
            return this;
        }

        public MerchantBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MerchantBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Merchant build() {
            return new Merchant(id, name, email, razorpayAccountId, webhookSecret, passwordHash, webhookUrl, status, createdAt, updatedAt);
        }
    }
}
