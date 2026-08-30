package com.recoverai.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayOrderContainer {

    private RazorpayOrderEntityDto entity;

    public RazorpayOrderContainer() {
    }

    public RazorpayOrderContainer(RazorpayOrderEntityDto entity) {
        this.entity = entity;
    }

    public RazorpayOrderEntityDto getEntity() {
        return entity;
    }

    public void setEntity(RazorpayOrderEntityDto entity) {
        this.entity = entity;
    }
}
