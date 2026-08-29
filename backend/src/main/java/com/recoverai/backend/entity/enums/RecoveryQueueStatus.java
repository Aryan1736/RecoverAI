package com.recoverai.backend.entity.enums;

public enum RecoveryQueueStatus {
    READY,
    CLAIMED,
    PROCESSING,
    COMPLETED,
    FAILED,
    DEAD_LETTER
}
