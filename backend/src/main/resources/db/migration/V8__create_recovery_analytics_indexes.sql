-- RecoverAI Recovery Analytics & Reporting Indexes Migration (V8)
-- Composite indexes for high performance multi-tenant analytics and reporting queries

CREATE INDEX IF NOT EXISTS idx_recovery_attempts_merchant_channel ON recovery_attempts(merchant_id, channel);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_merchant_created ON recovery_attempts(merchant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_merchant_status ON recovery_attempts(merchant_id, status);
