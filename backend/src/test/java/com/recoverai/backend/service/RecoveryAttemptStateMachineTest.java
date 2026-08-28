package com.recoverai.backend.service;

import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.exception.InvalidRecoveryAttemptStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryAttemptStateMachineTest {

    private RecoveryAttemptStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new RecoveryAttemptStateMachine();
    }

    @ParameterizedTest(name = "Valid transition from {0} to {1}")
    @CsvSource({
            "SCHEDULED, SCHEDULED",
            "SCHEDULED, IN_FLIGHT",
            "SCHEDULED, FAILED",
            "SCHEDULED, SKIPPED",
            "IN_FLIGHT, IN_FLIGHT",
            "IN_FLIGHT, SENT",
            "IN_FLIGHT, DELIVERED",
            "IN_FLIGHT, CLICKED",
            "IN_FLIGHT, SUCCESS",
            "IN_FLIGHT, FAILED",
            "SENT, SENT",
            "SENT, DELIVERED",
            "SENT, CLICKED",
            "SENT, SUCCESS",
            "SENT, FAILED",
            "DELIVERED, DELIVERED",
            "DELIVERED, CLICKED",
            "DELIVERED, SUCCESS",
            "DELIVERED, FAILED",
            "CLICKED, CLICKED",
            "CLICKED, SUCCESS",
            "CLICKED, FAILED",
            "SUCCESS, SUCCESS",
            "FAILED, FAILED",
            "SKIPPED, SKIPPED"
    })
    void testValidTransitions(RecoveryAttemptStatus from, RecoveryAttemptStatus to) {
        assertTrue(stateMachine.isValidTransition(from, to));
        assertDoesNotThrow(() -> stateMachine.validateTransition(from, to));
    }

    @ParameterizedTest(name = "Invalid transition from {0} to {1}")
    @CsvSource({
            "SUCCESS, IN_FLIGHT",
            "SUCCESS, SENT",
            "SUCCESS, DELIVERED",
            "SUCCESS, CLICKED",
            "SUCCESS, FAILED",
            "FAILED, SCHEDULED",
            "FAILED, IN_FLIGHT",
            "FAILED, SENT",
            "FAILED, DELIVERED",
            "FAILED, CLICKED",
            "FAILED, SUCCESS",
            "SKIPPED, IN_FLIGHT",
            "SKIPPED, SENT",
            "CLICKED, SENT",
            "CLICKED, DELIVERED",
            "CLICKED, IN_FLIGHT",
            "DELIVERED, SENT",
            "DELIVERED, IN_FLIGHT",
            "SENT, SCHEDULED",
            "SENT, IN_FLIGHT"
    })
    void testInvalidTransitions(RecoveryAttemptStatus from, RecoveryAttemptStatus to) {
        assertFalse(stateMachine.isValidTransition(from, to));
        assertThrows(InvalidRecoveryAttemptStateException.class, () ->
                stateMachine.validateTransition(from, to));
    }

    @Test
    @DisplayName("Null status validation throws InvalidRecoveryAttemptStateException")
    void testNullStatusValidation() {
        assertThrows(InvalidRecoveryAttemptStateException.class, () ->
                stateMachine.validateTransition(null, RecoveryAttemptStatus.SUCCESS));
        assertThrows(InvalidRecoveryAttemptStateException.class, () ->
                stateMachine.validateTransition(RecoveryAttemptStatus.IN_FLIGHT, null));
    }

    @Test
    @DisplayName("isTerminal checks for terminal statuses")
    void testIsTerminal() {
        assertTrue(stateMachine.isTerminal(RecoveryAttemptStatus.SUCCESS));
        assertTrue(stateMachine.isTerminal(RecoveryAttemptStatus.FAILED));
        assertTrue(stateMachine.isTerminal(RecoveryAttemptStatus.SKIPPED));

        assertFalse(stateMachine.isTerminal(RecoveryAttemptStatus.SCHEDULED));
        assertFalse(stateMachine.isTerminal(RecoveryAttemptStatus.IN_FLIGHT));
        assertFalse(stateMachine.isTerminal(RecoveryAttemptStatus.SENT));
        assertFalse(stateMachine.isTerminal(RecoveryAttemptStatus.DELIVERED));
        assertFalse(stateMachine.isTerminal(RecoveryAttemptStatus.CLICKED));
    }
}
