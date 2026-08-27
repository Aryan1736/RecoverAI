package com.recoverai.backend.exception;

public class AgentDecisionNotFoundException extends RuntimeException {

    public AgentDecisionNotFoundException(String message) {
        super(message);
    }

    public AgentDecisionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
