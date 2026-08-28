-- RecoverAI Dashboard & Recovery Case Query Index Migration (V7)
-- Indexes for efficient multi-tenant filtering and deterministic sorting on recovery cases

CREATE INDEX IF NOT EXISTS idx_recovery_cases_merchant_priority ON recovery_cases(merchant_id, priority);
CREATE INDEX IF NOT EXISTS idx_recovery_cases_merchant_category ON recovery_cases(merchant_id, failure_reason_category);
CREATE INDEX IF NOT EXISTS idx_recovery_cases_merchant_created ON recovery_cases(merchant_id, created_at DESC);
