package com.recoverai.backend.service.provider.dto;

import java.math.BigDecimal;

public class WhatsAppMessageRequest {

    private final String recipientPhone;
    private final String customerName;
    private final String merchantName;
    private final BigDecimal amount;
    private final String currency;
    private final String recoveryLink;
    private final String failureReason;

    public WhatsAppMessageRequest(String recipientPhone,
                                  String customerName,
                                  String merchantName,
                                  BigDecimal amount,
                                  String currency,
                                  String recoveryLink,
                                  String failureReason) {
        this.recipientPhone = recipientPhone;
        this.customerName = customerName;
        this.merchantName = merchantName;
        this.amount = amount;
        this.currency = currency;
        this.recoveryLink = recoveryLink;
        this.failureReason = failureReason;
    }

    public String getRecipientPhone() {
        return recipientPhone;
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
