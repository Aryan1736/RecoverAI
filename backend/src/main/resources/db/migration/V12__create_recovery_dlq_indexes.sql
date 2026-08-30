-- RecoverAI Dead-Letter Queue Optimization Indexes (V12)
-- Adds composite index supporting merchant-scoped dead-letter listing,
-- deterministic ordering, and tenant lookup.

CREATE INDEX IF NOT EXISTS idx_recovery_queue_dlq_lookup
    ON recovery_execution_queue(merchant_id, status, created_at DESC);
