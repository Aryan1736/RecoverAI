package com.recoverai.backend.service;

import com.recoverai.backend.entity.enums.RecoveryAttemptStatus;
import com.recoverai.backend.exception.InvalidRecoveryAttemptStateException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class RecoveryAttemptStateMachine {

    private static final Map<RecoveryAttemptStatus, Set<RecoveryAttemptStatus>> VALID_TRANSITIONS;

    static {
        Map<RecoveryAttemptStatus, Set<RecoveryAttemptStatus>> map = new EnumMap<>(RecoveryAttemptStatus.class);

        // SCHEDULED can move to IN_FLIGHT, FAILED, or SKIPPED
        map.put(RecoveryAttemptStatus.SCHEDULED, EnumSet.of(
                RecoveryAttemptStatus.SCHEDULED,
                RecoveryAttemptStatus.IN_FLIGHT,
                RecoveryAttemptStatus.FAILED,
                RecoveryAttemptStatus.SKIPPED
        ));

        // IN_FLIGHT can move to SENT, DELIVERED, CLICKED, SUCCESS, or FAILED
        map.put(RecoveryAttemptStatus.IN_FLIGHT, EnumSet.of(
                RecoveryAttemptStatus.IN_FLIGHT,
                RecoveryAttemptStatus.SENT,
                RecoveryAttemptStatus.DELIVERED,
                RecoveryAttemptStatus.CLICKED,
                RecoveryAttemptStatus.SUCCESS,
                RecoveryAttemptStatus.FAILED
        ));

        // SENT can advance to DELIVERED, CLICKED, SUCCESS, or FAILED
        map.put(RecoveryAttemptStatus.SENT, EnumSet.of(
                RecoveryAttemptStatus.SENT,
                RecoveryAttemptStatus.DELIVERED,
                RecoveryAttemptStatus.CLICKED,
                RecoveryAttemptStatus.SUCCESS,
                RecoveryAttemptStatus.FAILED
        ));

        // DELIVERED can advance to CLICKED, SUCCESS, or FAILED
        map.put(RecoveryAttemptStatus.DELIVERED, EnumSet.of(
                RecoveryAttemptStatus.DELIVERED,
                RecoveryAttemptStatus.CLICKED,
                RecoveryAttemptStatus.SUCCESS,
                RecoveryAttemptStatus.FAILED
        ));

        // CLICKED can advance to SUCCESS or FAILED
        map.put(RecoveryAttemptStatus.CLICKED, EnumSet.of(
                RecoveryAttemptStatus.CLICKED,
                RecoveryAttemptStatus.SUCCESS,
                RecoveryAttemptStatus.FAILED
        ));

        // Terminal states: SUCCESS, FAILED, SKIPPED (only no-op identity allowed, no state transitions)
        map.put(RecoveryAttemptStatus.SUCCESS, EnumSet.of(RecoveryAttemptStatus.SUCCESS));
        map.put(RecoveryAttemptStatus.FAILED, EnumSet.of(RecoveryAttemptStatus.FAILED));
        map.put(RecoveryAttemptStatus.SKIPPED, EnumSet.of(RecoveryAttemptStatus.SKIPPED));

        VALID_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * Checks if transitioning from currentStatus to targetStatus is valid.
     */
    public boolean isValidTransition(RecoveryAttemptStatus currentStatus, RecoveryAttemptStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        Set<RecoveryAttemptStatus> allowed = VALID_TRANSITIONS.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    /**
     * Validates state transition; throws InvalidRecoveryAttemptStateException if invalid.
     */
    public void validateTransition(RecoveryAttemptStatus currentStatus, RecoveryAttemptStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            throw new InvalidRecoveryAttemptStateException("Attempt status cannot be null");
        }

        if (isTerminal(currentStatus) && currentStatus != targetStatus) {
            throw new InvalidRecoveryAttemptStateException(String.format(
                    "Cannot transition RecoveryAttempt from terminal state '%s' to '%s'",
                    currentStatus, targetStatus));
        }

        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidRecoveryAttemptStateException(String.format(
                    "Invalid RecoveryAttempt state transition from '%s' to '%s'",
                    currentStatus, targetStatus));
        }
    }

    public boolean isTerminal(RecoveryAttemptStatus status) {
        return status == RecoveryAttemptStatus.SUCCESS
                || status == RecoveryAttemptStatus.FAILED
                || status == RecoveryAttemptStatus.SKIPPED;
    }
}
