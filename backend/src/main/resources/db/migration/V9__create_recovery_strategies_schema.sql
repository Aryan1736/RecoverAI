-- RecoverAI Recovery Strategies Schema Migration (V9)
-- Deterministic Recovery Strategy Engine persistence for auditable and reproducible recovery execution

CREATE TABLE IF NOT EXISTS recovery_strategies (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    recovery_case_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recommended_action VARCHAR(100) NOT NULL,
    confidence_score NUMERIC(5, 4),
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    delay_seconds INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    reason TEXT NOT NULL,
    fallback_channel VARCHAR(50),
    fallback_action VARCHAR(100),
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_strategies_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_strategies_case FOREIGN KEY (recovery_case_id) REFERENCES recovery_cases(id) ON DELETE CASCADE,
    CONSTRAINT chk_recovery_strategies_channel CHECK (channel IN ('WHATSAPP', 'EMAIL', 'SMS', 'RETRY_CHARGE', 'SMART_LINK', 'MANUAL')),
    CONSTRAINT chk_recovery_strategies_fallback_channel CHECK (fallback_channel IS NULL OR fallback_channel IN ('WHATSAPP', 'EMAIL', 'SMS', 'RETRY_CHARGE', 'SMART_LINK', 'MANUAL')),
    CONSTRAINT chk_recovery_strategies_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_recovery_strategies_confidence CHECK (confidence_score IS NULL OR (confidence_score >= 0.0 AND confidence_score <= 1.0)),
    CONSTRAINT chk_recovery_strategies_delay CHECK (delay_seconds >= 0),
    CONSTRAINT chk_recovery_strategies_max_attempts CHECK (max_attempts > 0)
);

CREATE INDEX IF NOT EXISTS idx_recovery_strategies_merchant_id ON recovery_strategies(merchant_id);
CREATE INDEX IF NOT EXISTS idx_recovery_strategies_case_id ON recovery_strategies(recovery_case_id);
CREATE INDEX IF NOT EXISTS idx_recovery_strategies_created_at ON recovery_strategies(created_at);
CREATE INDEX IF NOT EXISTS idx_recovery_strategies_merchant_case ON recovery_strategies(merchant_id, recovery_case_id, created_at DESC);
