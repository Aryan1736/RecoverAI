package com.recoverai.backend.dto.webhook;

public class WebhookResponse {

    private String status;
    private String message;

    public WebhookResponse() {
    }

    public WebhookResponse(String status) {
        this.status = status;
    }

    public WebhookResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static WebhookResponse accepted() {
        return new WebhookResponse("accepted");
    }

    public static WebhookResponse accepted(String message) {
        return new WebhookResponse("accepted", message);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
