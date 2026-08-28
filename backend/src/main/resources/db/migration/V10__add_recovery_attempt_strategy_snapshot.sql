-- RecoverAI Strategy-Driven Recovery Execution Integration (V10)
-- Add strategy reference, strategy snapshot, and supporting indexes for recovery execution

ALTER TABLE recovery_attempts
    ADD COLUMN IF NOT EXISTS strategy_id UUID;

ALTER TABLE recovery_attempts
    ADD COLUMN IF NOT EXISTS strategy_snapshot TEXT;

ALTER TABLE recovery_attempts
    ADD CONSTRAINT fk_recovery_attempts_strategy
    FOREIGN KEY (strategy_id) REFERENCES recovery_strategies(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_recovery_attempts_strategy_id ON recovery_attempts(strategy_id);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_merchant_case ON recovery_attempts(merchant_id, recovery_case_id);
