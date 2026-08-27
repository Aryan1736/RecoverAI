package com.recoverai.backend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayWebhookPayload {

    private String entity;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("event_id")
    private String eventId;

    private String event;

    private List<String> contains;

    private RazorpayPayloadContent payload;

    @JsonProperty("created_at")
    private Long createdAt;

    public RazorpayWebhookPayload() {
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public List<String> getContains() {
        return contains;
    }

    public void setContains(List<String> contains) {
        this.contains = contains;
    }

    public RazorpayPayloadContent getPayload() {
        return payload;
    }

    public void setPayload(RazorpayPayloadContent payload) {
        this.payload = payload;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
