package com.recoverai.backend.exception;

public class InvalidScheduledTimeException extends RuntimeException {
    public InvalidScheduledTimeException(String message) {
        super(message);
    }
}
