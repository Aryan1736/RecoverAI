package com.recoverai.backend.dto.recoverycase;

import com.recoverai.backend.entity.Payment;
import com.recoverai.backend.entity.enums.PaymentMethod;
import com.recoverai.backend.entity.enums.PaymentStatus;
import com.recoverai.backend.entity.enums.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentResponseDto {

    private UUID id;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpayInvoiceId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private PaymentMethod method;
    private String errorCode;
    private String errorDescription;
    private String errorSource;
    private String errorReason;
    private RiskLevel riskLevel;
    private Instant paymentCreatedAt;
    private Instant createdAt;

    public PaymentResponseDto() {
    }

    public PaymentResponseDto(UUID id, String razorpayPaymentId, String razorpayOrderId, String razorpayInvoiceId,
                              BigDecimal amount, String currency, PaymentStatus status, PaymentMethod method,
                              String errorCode, String errorDescription, String errorSource, String errorReason,
                              RiskLevel riskLevel, Instant paymentCreatedAt, Instant createdAt) {
        this.id = id;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayInvoiceId = razorpayInvoiceId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.method = method;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorSource = errorSource;
        this.errorReason = errorReason;
        this.riskLevel = riskLevel;
        this.paymentCreatedAt = paymentCreatedAt;
        this.createdAt = createdAt;
    }

    public static PaymentResponseDto fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponseDto(
                payment.getId(),
                payment.getRazorpayPaymentId(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayInvoiceId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getErrorCode(),
                payment.getErrorDescription(),
                payment.getErrorSource(),
                payment.getErrorReason(),
                payment.getRiskLevel(),
                payment.getPaymentCreatedAt(),
                payment.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
