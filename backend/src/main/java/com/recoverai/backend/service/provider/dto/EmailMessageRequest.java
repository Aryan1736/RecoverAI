package com.recoverai.backend.service.provider.dto;

import java.math.BigDecimal;

public class EmailMessageRequest {

    private final String recipientEmail;
    private final String customerName;
    private final String merchantName;
    private final BigDecimal amount;
    private final String currency;
    private final String recoveryLink;
    private final String failureReason;

    public EmailMessageRequest(String recipientEmail,
                               String customerName,
                               String merchantName,
                               BigDecimal amount,
                               String currency,
                               String recoveryLink,
                               String failureReason) {
        this.recipientEmail = recipientEmail;
        this.customerName = customerName;
        this.merchantName = merchantName;
        this.amount = amount;
        this.currency = currency;
        this.recoveryLink = recoveryLink;
        this.failureReason = failureReason;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getRecoveryLink() {
        return recoveryLink;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
