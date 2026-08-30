package com.recoverai.backend.exception;

public class NotificationPreferenceException extends RuntimeException {

    public NotificationPreferenceException(String message) {
        super(message);
    }

    public NotificationPreferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
