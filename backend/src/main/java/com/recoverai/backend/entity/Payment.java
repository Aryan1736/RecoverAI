package com.recoverai.backend.entity;

import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RiskLevel;
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
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_payments_merchant_razorpay_payment", columnNames = {"merchant_id", "razorpay_payment_id"})
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Merchant reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotBlank(message = "Razorpay payment ID is required")
    @Column(name = "razorpay_payment_id", nullable = false)
    private String razorpayPaymentId;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_invoice_id")
    private String razorpayInvoiceId;

    @NotNull(message = "Payment amount is required")
    @PositiveOrZero(message = "Payment amount must be non-negative")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "Payment currency is required")
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 50)
    private PaymentMethod method;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "error_source")
    private String errorSource;

    @Column(name = "error_reason")
    private String errorReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 50)
    private RiskLevel riskLevel;

    @Column(name = "payment_created_at")
    private Instant paymentCreatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Payment() {
    }

    public Payment(UUID id, Merchant merchant, Customer customer, String razorpayPaymentId,
                   String razorpayOrderId, String razorpayInvoiceId, BigDecimal amount,
                   String currency, PaymentStatus status, PaymentMethod method,
                   String errorCode, String errorDescription, String errorSource,
                   String errorReason, RiskLevel riskLevel, Instant paymentCreatedAt,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchant = merchant;
        this.customer = customer;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayInvoiceId = razorpayInvoiceId;
        this.amount = amount;
        this.currency = currency != null ? currency : "INR";
        this.status = status;
        this.method = method;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorSource = errorSource;
        this.errorReason = errorReason;
        this.riskLevel = riskLevel;
        this.paymentCreatedAt = paymentCreatedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayInvoiceId() {
        return razorpayInvoiceId;
    }

    public void setRazorpayInvoiceId(String razorpayInvoiceId) {
        this.razorpayInvoiceId = razorpayInvoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorSource() {
        return errorSource;
    }

    public void setErrorSource(String errorSource) {
        this.errorSource = errorSource;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Instant getPaymentCreatedAt() {
        return paymentCreatedAt;
    }

    public void setPaymentCreatedAt(Instant paymentCreatedAt) {
        this.paymentCreatedAt = paymentCreatedAt;
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
        Payment payment = (Payment) o;
        return id != null && Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static class PaymentBuilder {
        private UUID id;
        private Merchant merchant;
        private Customer customer;
        private String razorpayPaymentId;
        private String razorpayOrderId;
        private String razorpayInvoiceId;
        private BigDecimal amount;
        private String currency = "INR";
        private PaymentStatus status;
        private PaymentMethod method;
        private String errorCode;
        private String errorDescription;
        private String errorSource;
        private String errorReason;
        private RiskLevel riskLevel;
        private Instant paymentCreatedAt;
        private Instant createdAt;
        private Instant updatedAt;

        PaymentBuilder() {
        }

        public PaymentBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public PaymentBuilder merchant(Merchant merchant) {
            this.merchant = merchant;
            return this;
        }

        public PaymentBuilder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public PaymentBuilder razorpayPaymentId(String razorpayPaymentId) {
            this.razorpayPaymentId = razorpayPaymentId;
            return this;
        }

        public PaymentBuilder razorpayOrderId(String razorpayOrderId) {
            this.razorpayOrderId = razorpayOrderId;
            return this;
        }

        public PaymentBuilder razorpayInvoiceId(String razorpayInvoiceId) {
            this.razorpayInvoiceId = razorpayInvoiceId;
            return this;
        }

        public PaymentBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public PaymentBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder method(PaymentMethod method) {
            this.method = method;
            return this;
        }

        public PaymentBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public PaymentBuilder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public PaymentBuilder errorSource(String errorSource) {
            this.errorSource = errorSource;
            return this;
        }

        public PaymentBuilder errorReason(String errorReason) {
            this.errorReason = errorReason;
            return this;
        }

        public PaymentBuilder riskLevel(RiskLevel riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public PaymentBuilder paymentCreatedAt(Instant paymentCreatedAt) {
            this.paymentCreatedAt = paymentCreatedAt;
            return this;
        }

        public PaymentBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Payment build() {
            return new Payment(id, merchant, customer, razorpayPaymentId, razorpayOrderId, razorpayInvoiceId,
                    amount, currency, status, method, errorCode, errorDescription, errorSource,
                    errorReason, riskLevel, paymentCreatedAt, createdAt, updatedAt);
        }
    }
}
