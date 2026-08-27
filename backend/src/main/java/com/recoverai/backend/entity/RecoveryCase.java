package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.RecoveryCaseStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "recovery_cases", uniqueConstraints = {
    @UniqueConstraint(name = "uq_recovery_cases_payment", columnNames = {"payment_id"})
})
public class RecoveryCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Merchant reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @NotNull(message = "Payment reference is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull(message = "Recovery case status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecoveryCaseStatus status = RecoveryCaseStatus.OPEN;

    @NotNull(message = "Recovery priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private RecoveryPriority priority = RecoveryPriority.MEDIUM;

    @Column(name = "failure_reason_category", length = 100)
    private String failureReasonCategory;

    @NotNull(message = "Estimated recoverable amount is required")
    @PositiveOrZero(message = "Estimated recoverable amount must be non-negative")
    @Column(name = "estimated_recoverable_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal estimatedRecoverableAmount;

    @PositiveOrZero(message = "Recovered amount must be non-negative")
    @Column(name = "recovered_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal recoveredAmount = BigDecimal.ZERO;

    @NotBlank(message = "Currency is required")
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "recovered_at")
    private Instant recoveredAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RecoveryCase() {
    }

    public RecoveryCase(UUID id, Merchant merchant, Payment payment, Customer customer,
                        RecoveryCaseStatus status, RecoveryPriority priority,
                        String failureReasonCategory, BigDecimal estimatedRecoverableAmount,
                        BigDecimal recoveredAmount, String currency, Instant expiresAt,
                        Instant recoveredAt, Instant closedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.payment = payment;
        this.customer = customer;
        this.status = status != null ? status : RecoveryCaseStatus.OPEN;
        this.priority = priority != null ? priority : RecoveryPriority.MEDIUM;
        this.failureReasonCategory = failureReasonCategory;
        this.estimatedRecoverableAmount = estimatedRecoverableAmount;
        this.recoveredAmount = recoveredAmount != null ? recoveredAmount : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "INR";
        this.expiresAt = expiresAt;
        this.recoveredAt = recoveredAt;
        this.closedAt = closedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RecoveryCaseBuilder builder() {
        return new RecoveryCaseBuilder();
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
            status = RecoveryCaseStatus.OPEN;
        }
        if (priority == null) {
            priority = RecoveryPriority.MEDIUM;
        }
        if (recoveredAmount == null) {
            recoveredAmount = BigDecimal.ZERO;
        }
        if (currency == null) {
            currency = "INR";
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

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public RecoveryCaseStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryCaseStatus status) {
        this.status = status;
    }

    public RecoveryPriority getPriority() {
        return priority;
    }

    public void setPriority(RecoveryPriority priority) {
        this.priority = priority;
    }

    public String getFailureReasonCategory() {
        return failureReasonCategory;
    }

    public void setFailureReasonCategory(String failureReasonCategory) {
        this.failureReasonCategory = failureReasonCategory;
    }

    public BigDecimal getEstimatedRecoverableAmount() {
        return estimatedRecoverableAmount;
    }

    public void setEstimatedRecoverableAmount(BigDecimal estimatedRecoverableAmount) {
        this.estimatedRecoverableAmount = estimatedRecoverableAmount;
    }

    public BigDecimal getRecoveredAmount() {
        return recoveredAmount;
    }

    public void setRecoveredAmount(BigDecimal recoveredAmount) {
        this.recoveredAmount = recoveredAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRecoveredAt() {
        return recoveredAt;
    }

    public void setRecoveredAt(Instant recoveredAt) {
        this.recoveredAt = recoveredAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
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
        RecoveryCase that = (RecoveryCase) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class RecoveryCaseBuilder {
        private UUID id;
        private Merchant merchant;
        private Payment payment;
        private Customer customer;
        private RecoveryCaseStatus status = RecoveryCaseStatus.OPEN;
        private RecoveryPriority priority = RecoveryPriority.MEDIUM;
        private String failureReasonCategory;
        private BigDecimal estimatedRecoverableAmount;
        private BigDecimal recoveredAmount = BigDecimal.ZERO;
        private String currency = "INR";
        private Instant expiresAt;
        private Instant recoveredAt;
        private Instant closedAt;
        private Instant createdAt;
        private Instant updatedAt;

        RecoveryCaseBuilder() {
        }

        public RecoveryCaseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RecoveryCaseBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public RecoveryCaseBuilder payment(Payment payment) {
            this.payment = payment;
            return this;
        }

        public RecoveryCaseBuilder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public RecoveryCaseBuilder status(RecoveryCaseStatus status) {
            this.status = status;
            return this;
        }

        public RecoveryCaseBuilder priority(RecoveryPriority priority) {
            this.priority = priority;
            return this;
        }

        public RecoveryCaseBuilder failureReasonCategory(String failureReasonCategory) {
            this.failureReasonCategory = failureReasonCategory;
            return this;
        }

        public RecoveryCaseBuilder estimatedRecoverableAmount(BigDecimal estimatedRecoverableAmount) {
            this.estimatedRecoverableAmount = estimatedRecoverableAmount;
            return this;
        }

        public RecoveryCaseBuilder recoveredAmount(BigDecimal recoveredAmount) {
            this.recoveredAmount = recoveredAmount;
            return this;
        }

        public RecoveryCaseBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public RecoveryCaseBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public RecoveryCaseBuilder recoveredAt(Instant recoveredAt) {
            this.recoveredAt = recoveredAt;
            return this;
        }

        public RecoveryCaseBuilder closedAt(Instant closedAt) {
            this.closedAt = closedAt;
            return this;
        }

        public RecoveryCaseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RecoveryCaseBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public RecoveryCase build() {
            return new RecoveryCase(id, merchant, payment, customer, status, priority,
                    failureReasonCategory, estimatedRecoverableAmount, recoveredAmount,
                    currency, expiresAt, recoveredAt, closedAt, createdAt, updatedAt);
        }
    }
}
