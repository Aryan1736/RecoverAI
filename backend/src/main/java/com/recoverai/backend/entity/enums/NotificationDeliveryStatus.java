package com.recoverai.backend.entity.enums;

/**
 * Delivery attempt status for a notification channel dispatch.
 */
public enum NotificationDeliveryStatus {
    PENDING,
    DELIVERED,
    FAILED,
    RETRYING,
    SKIPPED
}
