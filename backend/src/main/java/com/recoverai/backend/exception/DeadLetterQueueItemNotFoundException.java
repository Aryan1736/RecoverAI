package com.recoverai.backend.exception;

public class DeadLetterQueueItemNotFoundException extends RuntimeException {

    public DeadLetterQueueItemNotFoundException(String message) {
        super(message);
    }

    public DeadLetterQueueItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
