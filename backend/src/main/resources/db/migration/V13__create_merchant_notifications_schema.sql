-- RecoverAI Merchant Alert & Notification Engine Migration (V13)
-- Supports merchant notification preferences, notification lifecycle,
-- and multi-channel delivery tracking (Email, Webhook, In-App).

-- 1. Add configurable webhook_url to merchants table
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS webhook_url VARCHAR(1000);

-- 2. Merchant Notification Preferences Table
CREATE TABLE IF NOT EXISTS merchant_notification_preferences (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_pref_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT uq_notification_pref_merchant_event_channel UNIQUE (merchant_id, event_type, channel),
    CONSTRAINT chk_notification_pref_event CHECK (event_type IN ('PAYMENT_RECOVERED', 'CASE_EXHAUSTED', 'HIGH_PRIORITY_FAILURE', 'PROVIDER_DEGRADED')),
    CONSTRAINT chk_notification_pref_channel CHECK (channel IN ('EMAIL', 'WEBHOOK', 'IN_APP'))
);

CREATE INDEX IF NOT EXISTS idx_notification_pref_merchant
    ON merchant_notification_preferences(merchant_id);

CREATE INDEX IF NOT EXISTS idx_notification_pref_event_channel
    ON merchant_notification_preferences(merchant_id, event_type, channel);

-- 3. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    recovery_case_id UUID,
    recovery_attempt_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'UNREAD',
    idempotency_key VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_case FOREIGN KEY (recovery_case_id) REFERENCES recovery_cases(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_attempt FOREIGN KEY (recovery_attempt_id) REFERENCES recovery_attempts(id) ON DELETE SET NULL,
    CONSTRAINT chk_notifications_status CHECK (status IN ('UNREAD', 'READ', 'ARCHIVED')),
    CONSTRAINT chk_notifications_event CHECK (event_type IN ('PAYMENT_RECOVERED', 'CASE_EXHAUSTED', 'HIGH_PRIORITY_FAILURE', 'PROVIDER_DEGRADED'))
);

CREATE INDEX IF NOT EXISTS idx_notifications_merchant_status_created
    ON notifications(merchant_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_merchant_idempotency
    ON notifications(merchant_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_notifications_case_id
    ON notifications(recovery_case_id);

CREATE INDEX IF NOT EXISTS idx_notifications_attempt_id
    ON notifications(recovery_attempt_id);

-- 4. Notification Deliveries Table
CREATE TABLE IF NOT EXISTS notification_deliveries (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    provider VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(255),
    error_code VARCHAR(100),
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    attempted_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deliveries_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_deliveries_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT chk_deliveries_channel CHECK (channel IN ('EMAIL', 'WEBHOOK', 'IN_APP')),
    CONSTRAINT chk_deliveries_status CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED', 'RETRYING', 'SKIPPED')),
    CONSTRAINT chk_deliveries_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_deliveries_max_retries CHECK (max_retries >= 0)
);

CREATE INDEX IF NOT EXISTS idx_deliveries_notification_id
    ON notification_deliveries(notification_id);

CREATE INDEX IF NOT EXISTS idx_deliveries_merchant_status
    ON notification_deliveries(merchant_id, status);

CREATE INDEX IF NOT EXISTS idx_deliveries_channel_status
    ON notification_deliveries(channel, status);
