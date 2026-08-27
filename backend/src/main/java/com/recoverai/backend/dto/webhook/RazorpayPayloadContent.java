package com.recoverai.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayPayloadContent {

    private RazorpayPaymentContainer payment;

    public RazorpayPayloadContent() {
    }

    public RazorpayPayloadContent(RazorpayPaymentContainer payment) {
        this.payment = payment;
    }

    public RazorpayPaymentContainer getPayment() {
        return payment;
    }

    public void setPayment(RazorpayPaymentContainer payment) {
        this.payment = payment;
    }
}
