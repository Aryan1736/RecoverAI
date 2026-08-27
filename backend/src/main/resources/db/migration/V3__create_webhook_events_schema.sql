-- RecoverAI Webhook Events & Idempotency Schema Migration (V3)
-- Supports persistent tracking of incoming webhook events to guarantee safe, idempotent ingestion.

CREATE TABLE IF NOT EXISTS webhook_events (
    id UUID PRIMARY KEY,
    merchant_id UUID,
    razorpay_event_id VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_events_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT chk_webhook_events_status CHECK (processing_status IN ('PENDING', 'PROCESSED', 'DUPLICATE', 'FAILED', 'IGNORED'))
);

CREATE INDEX IF NOT EXISTS idx_webhook_events_merchant_id ON webhook_events(merchant_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_razorpay_event ON webhook_events(merchant_id, razorpay_event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_payload_hash ON webhook_events(merchant_id, payload_hash);
CREATE INDEX IF NOT EXISTS idx_webhook_events_status ON webhook_events(processing_status);
CREATE INDEX IF NOT EXISTS idx_webhook_events_created_at ON webhook_events(created_at);
