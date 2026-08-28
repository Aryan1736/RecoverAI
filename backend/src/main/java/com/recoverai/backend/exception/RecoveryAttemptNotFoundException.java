package com.recoverai.backend.exception;

public class RecoveryAttemptNotFoundException extends RuntimeException {

    public RecoveryAttemptNotFoundException(String message) {
        super(message);
    }

    public RecoveryAttemptNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}