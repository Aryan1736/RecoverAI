-- RecoverAI Asynchronous Recovery Execution Queue Migration (V11)
-- Creates database-backed durable queue for asynchronous recovery execution with atomic claiming,
-- retry tracking, dead-letter support, and multi-tenant isolation.

CREATE TABLE IF NOT EXISTS recovery_execution_queue (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    recovery_attempt_id UUID NOT NULL,
    recovery_case_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'READY',
    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE,
    claimed_by VARCHAR(255),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    last_error_code VARCHAR(100),
    last_error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_queue_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_queue_attempt FOREIGN KEY (recovery_attempt_id) REFERENCES recovery_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_queue_case FOREIGN KEY (recovery_case_id) REFERENCES recovery_cases(id) ON DELETE CASCADE,
    CONSTRAINT uq_recovery_queue_attempt UNIQUE (recovery_attempt_id),
    CONSTRAINT chk_recovery_queue_status CHECK (status IN ('READY', 'CLAIMED', 'PROCESSING', 'COMPLETED', 'FAILED', 'DEAD_LETTER')),
    CONSTRAINT chk_recovery_queue_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_recovery_queue_max_retries CHECK (max_retries >= 0)
);

CREATE INDEX IF NOT EXISTS idx_recovery_queue_status_available_at
    ON recovery_execution_queue(status, available_at);

CREATE INDEX IF NOT EXISTS idx_recovery_queue_merchant_status
    ON recovery_execution_queue(merchant_id, status);

CREATE INDEX IF NOT EXISTS idx_recovery_queue_claimed_at
    ON recovery_execution_queue(claimed_at);

CREATE INDEX IF NOT EXISTS idx_recovery_queue_case_id
    ON recovery_execution_queue(recovery_case_id);
