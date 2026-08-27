package com.recoverai.backend.service;

import com.recoverai.backend.entity.enums.RecoveryPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class FailureReasonClassifier {

    public static final String CATEGORY_INSUFFICIENT_FUNDS = "insufficient_funds";
    public static final String CATEGORY_BANK_DECLINED = "bank_declined";
    public static final String CATEGORY_AUTHENTICATION_FAILURE = "authentication_failure";
    public static final String CATEGORY_NETWORK_ERROR = "network_error";
    public static final String CATEGORY_INVALID_REQUEST = "invalid_request";
    public static final String CATEGORY_UNKNOWN = "unknown";

    private static final BigDecimal THRESHOLD_CRITICAL = new BigDecimal("10000.00");
    private static final BigDecimal THRESHOLD_HIGH = new BigDecimal("5000.00");
    private static final BigDecimal THRESHOLD_MEDIUM = new BigDecimal("1000.00");

    /**
     * Deterministically categorizes the failure reason based on Razorpay error fields.
     *
     * @param errorCode        Razorpay error code (e.g. BAD_REQUEST_ERROR, GATEWAY_ERROR)
     * @param errorSource      Razorpay error source (e.g. bank, gateway, customer, business)
     * @param errorReason      Razorpay error reason (e.g. payment_failed, payment_declined_by_bank)
     * @param errorDescription Razorpay human-readable error description
     * @return Deterministic failure category string
     */
    public String classifyFailure(String errorCode, String errorSource, String errorReason, String errorDescription) {
        String combined = String.join(" ",
                nullToEmpty(errorCode),
                nullToEmpty(errorSource),
                nullToEmpty(errorReason),
                nullToEmpty(errorDescription)
        ).toLowerCase(Locale.ROOT);

        if (combined.contains("insufficient_funds") || combined.contains("insufficient_balance")
                || combined.contains("low_balance") || combined.contains("limit_exceeded")) {
            return CATEGORY_INSUFFICIENT_FUNDS;
        }

        if (combined.contains("3d_secure") || combined.contains("otp") || combined.contains("auth")
                || combined.contains("pin") || combined.contains("password") || combined.contains("2fa")
                || combined.contains("authentication_failed")) {
            return CATEGORY_AUTHENTICATION_FAILURE;
        }

        if (combined.contains("timeout") || combined.contains("network") || combined.contains("gateway")
                || combined.contains("connection") || combined.contains("server_error")) {
            return CATEGORY_NETWORK_ERROR;
        }

        if ("bank".equalsIgnoreCase(errorSource) || combined.contains("declined")
                || combined.contains("do_not_honor") || combined.contains("card_declined")
                || combined.contains("transaction_not_permitted") || combined.contains("bank")) {
            return CATEGORY_BANK_DECLINED;
        }

        if (combined.contains("bad_request") || combined.contains("invalid")
                || combined.contains("expired_card") || combined.contains("format")) {
            return CATEGORY_INVALID_REQUEST;
        }

        return CATEGORY_UNKNOWN;
    }

    /**
     * Determines the recovery priority deterministically from the recoverable amount.
     *
     * @param amount Payment amount in major currency units (e.g. INR)
     * @return RecoveryPriority
     */
    public RecoveryPriority determinePriority(BigDecimal amount) {
        if (amount == null) {
            return RecoveryPriority.MEDIUM;
        }

        if (amount.compareTo(THRESHOLD_CRITICAL) >= 0) {
            return RecoveryPriority.CRITICAL;
        } else if (amount.compareTo(THRESHOLD_HIGH) >= 0) {
            return RecoveryPriority.HIGH;
        } else if (amount.compareTo(THRESHOLD_MEDIUM) >= 0) {
            return RecoveryPriority.MEDIUM;
        } else {
            return RecoveryPriority.LOW;
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
