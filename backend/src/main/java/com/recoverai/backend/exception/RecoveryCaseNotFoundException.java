package com.recoverai.backend.exception;

public class RecoveryCaseNotFoundException extends RuntimeException {

    public RecoveryCaseNotFoundException(String message) {
        super(message);
    }

    public RecoveryCaseNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
