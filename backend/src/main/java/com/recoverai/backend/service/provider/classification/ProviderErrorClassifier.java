package com.recoverai.backend.service.provider.classification;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;

public final class ProviderErrorClassifier {

    private ProviderErrorClassifier() {
    }

    public static ProviderFailureType classifyHttpStatus(int statusCode) {
        if (statusCode == 429) {
            return ProviderFailureType.RATE_LIMITED;
        }
        if (statusCode == 401 || statusCode == 403) {
            return ProviderFailureType.AUTHENTICATION;
        }
        if (statusCode == 404) {
            return ProviderFailureType.NOT_FOUND;
        }
        if (statusCode >= 400 && statusCode < 500) {
            return ProviderFailureType.VALIDATION;
        }
        if (statusCode == 504 || statusCode == 502 || statusCode == 503 || statusCode == 500) {
            return ProviderFailureType.TRANSIENT;
        }
        return ProviderFailureType.UNKNOWN;
    }

    public static ProviderFailureType classifyException(Throwable ex) {
        if (ex == null) {
            return ProviderFailureType.UNKNOWN;
        }

        if (ex instanceof RestClientResponseException rcre) {
            return classifyHttpStatus(rcre.getStatusCode().value());
        }

        if (ex instanceof SocketTimeoutException) {
            return ProviderFailureType.TIMEOUT;
        }

        if (ex instanceof ConnectException || ex instanceof ResourceAccessException) {
            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase(Locale.ROOT) : "";
            if (msg.contains("timeout") || msg.contains("timed out")) {
                return ProviderFailureType.TIMEOUT;
            }
            return ProviderFailureType.TRANSIENT;
        }

        if (ex instanceof IOException) {
            return ProviderFailureType.TRANSIENT;
        }

        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase(Locale.ROOT) : "";
        if (message.contains("timeout") || message.contains("timed out")) {
            return ProviderFailureType.TIMEOUT;
        }
        if (message.contains("rate limit") || message.contains("too many requests") || message.contains("429")) {
            return ProviderFailureType.RATE_LIMITED;
        }
        if (message.contains("unauthorized") || message.contains("forbidden") || message.contains("auth")) {
            return ProviderFailureType.AUTHENTICATION;
        }
        if (message.contains("connection reset") || message.contains("broken pipe") || message.contains("temporary")
                || message.contains("service unavailable") || message.contains("gateway timeout")) {
            return ProviderFailureType.TRANSIENT;
        }

        return ProviderFailureType.UNKNOWN;
    }

    public static ProviderFailureType classifyResultCode(String resultCode) {
        if (resultCode == null || resultCode.isBlank()) {
            return ProviderFailureType.UNKNOWN;
        }

        String code = resultCode.toUpperCase(Locale.ROOT);
        if (code.contains("TIMEOUT")) {
            return ProviderFailureType.TIMEOUT;
        }
        if (code.contains("RATE_LIMIT") || code.contains("THROTTLED")) {
            return ProviderFailureType.RATE_LIMITED;
        }
        if (code.contains("AUTH") || code.contains("UNAUTHORIZED") || code.contains("FORBIDDEN")) {
            return ProviderFailureType.AUTHENTICATION;
        }
        if (code.contains("NOT_FOUND")) {
            return ProviderFailureType.NOT_FOUND;
        }
        if (code.contains("TRANSIENT") || code.contains("NETWORK") || code.contains("TEMPORARY")
                || code.contains("SERVICE_UNAVAILABLE") || code.contains("GATEWAY")) {
            return ProviderFailureType.TRANSIENT;
        }
        if (code.contains("VALIDATION") || code.contains("INVALID") || code.contains("MALFORMED")
                || code.contains("BAD_REQUEST") || code.contains("PERMANENT") || code.contains("RETRY_DISABLED")
                || code.contains("CASE_TERMINAL") || code.contains("NOT_ELIGIBLE") || code.contains("UNSUPPORTED")) {
            return ProviderFailureType.PERMANENT;
        }

        return ProviderFailureType.UNKNOWN;
    }

    public static ProviderFailureType classify(int statusCode, String responseBody, Throwable ex) {
        if (ex != null) {
            ProviderFailureType exceptionType = classifyException(ex);
            if (exceptionType != ProviderFailureType.UNKNOWN) {
                return exceptionType;
            }
        }

        if (statusCode > 0) {
            ProviderFailureType statusType = classifyHttpStatus(statusCode);
            if (statusType != ProviderFailureType.UNKNOWN) {
                return statusType;
            }
        }

        if (responseBody != null && !responseBody.isBlank()) {
            String bodyLower = responseBody.toLowerCase(Locale.ROOT);
            if (bodyLower.contains("rate limit") || bodyLower.contains("too many requests")) {
                return ProviderFailureType.RATE_LIMITED;
            }
            if (bodyLower.contains("authenticate") || bodyLower.contains("invalid credentials")
                    || bodyLower.contains("unauthorized") || bodyLower.contains("api key")) {
                return ProviderFailureType.AUTHENTICATION;
            }
            if (bodyLower.contains("timeout") || bodyLower.contains("timed out")) {
                return ProviderFailureType.TIMEOUT;
            }
        }

        return ProviderFailureType.UNKNOWN;
    }

    public static boolean isRetryable(ProviderFailureType type) {
        return type != null && type.isRetryable();
    }
}
