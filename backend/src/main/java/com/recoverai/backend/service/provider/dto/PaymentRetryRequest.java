package com.recoverai.backend.service.provider.dto;

import com.recoverai.backend.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentRetryRequest {

    private final UUID paymentId;
    private final String razorpayPaymentId;
    private final UUID merchantId;
    private final BigDecimal amount;
    private final String currency;
    private final PaymentMethod paymentMethod;

    public PaymentRetryRequest(UUID paymentId,
                               String razorpayPaymentId,
                               UUID merchantId,
                               BigDecimal amount,
                               String currency,
                               PaymentMethod paymentMethod) {
        this.paymentId = paymentId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
}
