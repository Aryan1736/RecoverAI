package com.recoverai.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayPayloadContent {

    private RazorpayPaymentContainer payment;
    private RazorpayOrderContainer order;

    public RazorpayPayloadContent() {
    }

    public RazorpayPayloadContent(RazorpayPaymentContainer payment) {
        this.payment = payment;
    }

    public RazorpayPayloadContent(RazorpayPaymentContainer payment, RazorpayOrderContainer order) {
        this.payment = payment;
        this.order = order;
    }

    public RazorpayPaymentContainer getPayment() {
        return payment;
    }

    public void setPayment(RazorpayPaymentContainer payment) {
        this.payment = payment;
    }

    public RazorpayOrderContainer getOrder() {
        return order;
    }

    public void setOrder(RazorpayOrderContainer order) {
        this.order = order;
    }
}
