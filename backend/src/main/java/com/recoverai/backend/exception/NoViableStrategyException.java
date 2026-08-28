package com.recoverai.backend.exception;

public class NoViableStrategyException extends RuntimeException {

    public NoViableStrategyException(String message) {
        super(message);
    }

    public NoViableStrategyException(String message, Throwable cause) {
        super(message, cause);
    }
}
