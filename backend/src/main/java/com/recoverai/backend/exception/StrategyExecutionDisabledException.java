package com.recoverai.backend.exception;

public class StrategyExecutionDisabledException extends RuntimeException {
    public StrategyExecutionDisabledException(String message) {
        super(message);
    }
}
