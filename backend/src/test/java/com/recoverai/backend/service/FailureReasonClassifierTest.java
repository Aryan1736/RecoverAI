package com.recoverai.backend.service;

import com.recoverai.backend.entity.enums.RecoveryPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailureReasonClassifierTest {

    private FailureReasonClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new FailureReasonClassifier();
    }

    @Test
    @DisplayName("Should classify insufficient funds errors")
    void testInsufficientFundsClassification() {
        assertEquals("insufficient_funds",
                classifier.classifyFailure("BAD_REQUEST_ERROR", "customer", "insufficient_balance", "Customer balance low"));
        assertEquals("insufficient_funds",
                classifier.classifyFailure("INSUFFICIENT_FUNDS", "bank", "payment_failed", "Insufficient balance in account"));
    }

    @Test
    @DisplayName("Should classify authentication failures")
    void testAuthenticationFailureClassification() {
        assertEquals("authentication_failure",
                classifier.classifyFailure("BAD_REQUEST_ERROR", "customer", "otp_incorrect", "3D Secure auth failed"));
        assertEquals("authentication_failure",
                classifier.classifyFailure("AUTH_FAILED", "customer", "pin_incorrect", "Customer entered incorrect 2FA"));
    }

    @Test
    @DisplayName("Should classify network/gateway errors")
    void testNetworkErrorClassification() {
        assertEquals("network_error",
                classifier.classifyFailure("GATEWAY_ERROR", "gateway", "timeout", "Connection timed out with bank"));
        assertEquals("network_error",
                classifier.classifyFailure("SERVER_ERROR", "network", "server_error", "Internal network error"));
    }

    @Test
    @DisplayName("Should classify bank decline errors")
    void testBankDeclinedClassification() {
        assertEquals("bank_declined",
                classifier.classifyFailure("TRANSACTION_DECLINED", "bank", "card_declined", "Bank declined transaction"));
        assertEquals("bank_declined",
                classifier.classifyFailure("DO_NOT_HONOR", "bank", "do_not_honor", "Issuer bank declined"));
    }

    @Test
    @DisplayName("Should classify invalid request errors")
    void testInvalidRequestClassification() {
        assertEquals("invalid_request",
                classifier.classifyFailure("BAD_REQUEST", "customer", "invalid_card", "Expired card details"));
    }

    @Test
    @DisplayName("Should classify unknown errors as fallback")
    void testUnknownClassification() {
        assertEquals("unknown",
                classifier.classifyFailure(null, null, null, null));
        assertEquals("unknown",
                classifier.classifyFailure("SOME_RANDOM_CODE", "other", "other_reason", "something weird"));
    }

    @Test
    @DisplayName("Should determine recovery priority by amount tiers")
    void testPriorityDetermination() {
        assertEquals(RecoveryPriority.CRITICAL, classifier.determinePriority(new BigDecimal("15000.00")));
        assertEquals(RecoveryPriority.CRITICAL, classifier.determinePriority(new BigDecimal("10000.00")));

        assertEquals(RecoveryPriority.HIGH, classifier.determinePriority(new BigDecimal("7500.00")));
        assertEquals(RecoveryPriority.HIGH, classifier.determinePriority(new BigDecimal("5000.00")));

        assertEquals(RecoveryPriority.MEDIUM, classifier.determinePriority(new BigDecimal("2500.00")));
        assertEquals(RecoveryPriority.MEDIUM, classifier.determinePriority(new BigDecimal("1000.00")));

        assertEquals(RecoveryPriority.LOW, classifier.determinePriority(new BigDecimal("999.99")));
        assertEquals(RecoveryPriority.LOW, classifier.determinePriority(new BigDecimal("100.00")));
        assertEquals(RecoveryPriority.MEDIUM, classifier.determinePriority(null));
    }
}
