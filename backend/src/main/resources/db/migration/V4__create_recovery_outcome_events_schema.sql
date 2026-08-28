-- RecoverAI Recovery Outcome Events & Idempotency Schema Migration (V4)
-- Supports persistent tracking of incoming asynchronous provider recovery outcome events.

CREATE TABLE IF NOT EXISTS recovery_outcome_events (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    recovery_attempt_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(100) NOT NULL,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payload_hash VARCHAR(64) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_outcome_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_outcome_attempt FOREIGN KEY (recovery_attempt_id) REFERENCES recovery_attempts(id) ON DELETE CASCADE,
    CONSTRAINT uq_recovery_outcome_merchant_provider_event UNIQUE (merchant_id, provider, provider_event_id),
    CONSTRAINT chk_recovery_outcome_events_status CHECK (processing_status IN ('PENDING', 'PROCESSED', 'DUPLICATE', 'FAILED', 'IGNORED'))
);

CREATE INDEX IF NOT EXISTS idx_recovery_outcome_events_merchant_id ON recovery_outcome_events(merchant_id);
CREATE INDEX IF NOT EXISTS idx_recovery_outcome_events_attempt_id ON recovery_outcome_events(recovery_attempt_id);
CREATE INDEX IF NOT EXISTS idx_recovery_outcome_events_provider_event ON recovery_outcome_events(merchant_id, provider, provider_event_id);
CREATE INDEX IF NOT EXISTS idx_recovery_outcome_events_payload_hash ON recovery_outcome_events(merchant_id, payload_hash);
CREATE INDEX IF NOT EXISTS idx_recovery_outcome_events_status ON recovery_outcome_events(processing_status);
CREATE INDEX IF NOT EXISTS idx_recovery_outcome_events_created_at ON recovery_outcome_events(created_at);