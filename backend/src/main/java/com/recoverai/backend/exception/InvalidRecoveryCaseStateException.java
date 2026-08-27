package com.recoverai.backend.exception;

public class InvalidRecoveryCaseStateException extends RuntimeException {

    public InvalidRecoveryCaseStateException(String message) {
        super(message);
    }

    public InvalidRecoveryCaseStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
