package com.recoverai.backend.exception;

public class MerchantResolutionException extends RuntimeException {

    public MerchantResolutionException(String message) {
        super(message);
    }

    public MerchantResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
