package com.recoverai.backend.exception;

public class DuplicateOrchestrationException extends RuntimeException {

    public DuplicateOrchestrationException(String message) {
        super(message);
    }

    public DuplicateOrchestrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
