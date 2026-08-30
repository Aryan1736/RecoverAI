package com.recoverai.backend.entity.enums;

/**
 * Supported merchant notification lifecycle event types.
 */
public enum MerchantNotificationEvent {
    PAYMENT_RECOVERED("Payment Recovered", "A failed payment was successfully recovered."),
    CASE_EXHAUSTED("Recovery Case Exhausted", "A recovery case reached terminal state after exhausting all attempts or channels."),
    HIGH_PRIORITY_FAILURE("High Priority Failure", "A high priority recovery case encountered a failure requiring merchant attention."),
    PROVIDER_DEGRADED("Provider Degraded", "An upstream provider is experiencing degraded health or outage.");

    private final String defaultTitle;
    private final String defaultDescription;

    MerchantNotificationEvent(String defaultTitle, String defaultDescription) {
        this.defaultTitle = defaultTitle;
        this.defaultDescription = defaultDescription;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }
}
