package com.recoverai.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayPaymentContainer {

    private RazorpayPaymentEntityDto entity;

    public RazorpayPaymentContainer() {
    }

    public RazorpayPaymentContainer(RazorpayPaymentEntityDto entity) {
        this.entity = entity;
    }

    public RazorpayPaymentEntityDto getEntity() {
        return entity;
    }

    public void setEntity(RazorpayPaymentEntityDto entity) {
        this.entity = entity;
    }
}
