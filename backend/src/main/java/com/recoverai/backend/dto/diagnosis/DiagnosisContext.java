package com.recoverai.backend.dto.diagnosis;

import java.math.BigDecimal;
import java.util.UUID;

public class DiagnosisContext {

    private UUID recoveryCaseId;
    private UUID merchantId;
    private String merchantName;
    private UUID paymentId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentStatus;
    private String errorCode;
    private String errorDescription;
    private String errorSource;
    private String errorReason;
    private String riskLevel;
    private String failureReasonCategory;
    private BigDecimal estimatedRecoverableAmount;
    private String recoveryPriority;
    private String recoveryCaseStatus;
    private String customerIdentifier;

    public DiagnosisContext() {
    }

    public DiagnosisContext(UUID recoveryCaseId, UUID merchantId, String merchantName, UUID paymentId,
                            String razorpayPaymentId, BigDecimal amount, String currency, String paymentMethod,
                            String paymentStatus, String errorCode, String errorDescription, String errorSource,
                            String errorReason, String riskLevel, String failureReasonCategory,
                            BigDecimal estimatedRecoverableAmount, String recoveryPriority,
                            String recoveryCaseStatus, String customerIdentifier) {
        this.recoveryCaseId = recoveryCaseId;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.paymentId = paymentId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.errorSource = errorSource;
        this.errorReason = errorReason;
        this.riskLevel = riskLevel;
        this.failureReasonCategory = failureReasonCategory;
        this.estimatedRecoverableAmount = estimatedRecoverableAmount;
        this.recoveryPriority = recoveryPriority;
        this.recoveryCaseStatus = recoveryCaseStatus;
        this.customerIdentifier = customerIdentifier;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getRecoveryCaseId() {
        return recoveryCaseId;
    }

    public void setRecoveryCaseId(UUID recoveryCaseId) {
        this.recoveryCaseId = recoveryCaseId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
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

    public String getRecoveryPriority() {
        return recoveryPriority;
    }

    public void setRecoveryPriority(String recoveryPriority) {
        this.recoveryPriority = recoveryPriority;
    }

    public String getRecoveryCaseStatus() {
        return recoveryCaseStatus;
    }

    public void setRecoveryCaseStatus(String recoveryCaseStatus) {
        this.recoveryCaseStatus = recoveryCaseStatus;
    }

    public String getCustomerIdentifier() {
        return customerIdentifier;
    }

    public void setCustomerIdentifier(String customerIdentifier) {
        this.customerIdentifier = customerIdentifier;
    }

    public static class Builder {
        private UUID recoveryCaseId;
        private UUID merchantId;
        private String merchantName;
        private UUID paymentId;
        private String razorpayPaymentId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String paymentStatus;
        private String errorCode;
        private String errorDescription;
        private String errorSource;
        private String errorReason;
        private String riskLevel;
        private String failureReasonCategory;
        private BigDecimal estimatedRecoverableAmount;
        private String recoveryPriority;
        private String recoveryCaseStatus;
        private String customerIdentifier;

        public Builder recoveryCaseId(UUID recoveryCaseId) {
            this.recoveryCaseId = recoveryCaseId;
            return this;
        }

        public Builder merchantId(UUID merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder merchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder paymentId(UUID paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder razorpayPaymentId(String razorpayPaymentId) {
            this.razorpayPaymentId = razorpayPaymentId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder paymentStatus(String paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
            return this;
        }

        public Builder errorSource(String errorSource) {
            this.errorSource = errorSource;
            return this;
        }

        public Builder errorReason(String errorReason) {
            this.errorReason = errorReason;
            return this;
        }

        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder failureReasonCategory(String failureReasonCategory) {
            this.failureReasonCategory = failureReasonCategory;
            return this;
        }

        public Builder estimatedRecoverableAmount(BigDecimal estimatedRecoverableAmount) {
            this.estimatedRecoverableAmount = estimatedRecoverableAmount;
            return this;
        }

        public Builder recoveryPriority(String recoveryPriority) {
            this.recoveryPriority = recoveryPriority;
            return this;
        }

        public Builder recoveryCaseStatus(String recoveryCaseStatus) {
            this.recoveryCaseStatus = recoveryCaseStatus;
            return this;
        }

        public Builder customerIdentifier(String customerIdentifier) {
            this.customerIdentifier = customerIdentifier;
            return this;
        }

        public DiagnosisContext build() {
            return new DiagnosisContext(recoveryCaseId, merchantId, merchantName, paymentId,
                    razorpayPaymentId, amount, currency, paymentMethod, paymentStatus,
                    errorCode, errorDescription, errorSource, errorReason, riskLevel,
                    failureReasonCategory, estimatedRecoverableAmount, recoveryPriority,
                    recoveryCaseStatus, customerIdentifier);
        }
    }
}
