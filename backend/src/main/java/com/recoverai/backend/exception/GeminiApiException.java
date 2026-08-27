package com.recoverai.backend.exception;

public class GeminiApiException extends RuntimeException {

    private final Integer statusCode;

    public GeminiApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public GeminiApiException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GeminiApiException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
