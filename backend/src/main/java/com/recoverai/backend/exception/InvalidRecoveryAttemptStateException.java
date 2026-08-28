package com.recoverai.backend.exception;

public class InvalidRecoveryAttemptStateException extends RuntimeException {

    public InvalidRecoveryAttemptStateException(String message) {
        super(message);
    }

    public InvalidRecoveryAttemptStateException(String message, Throwable cause) {
        super(message, cause);
    }
}