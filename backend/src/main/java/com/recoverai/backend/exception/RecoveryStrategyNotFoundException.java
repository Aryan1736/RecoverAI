package com.recoverai.backend.exception;

public class RecoveryStrategyNotFoundException extends RuntimeException {

    public RecoveryStrategyNotFoundException(String message) {
        super(message);
    }

    public RecoveryStrategyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
