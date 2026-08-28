-- RecoverAI Recovery Scheduler Schema Migration (V5)
-- Creates composite index on recovery_attempts(status, scheduled_at) for efficient scheduled attempt polling.

CREATE INDEX IF NOT EXISTS idx_recovery_attempts_status_scheduled_at
    ON recovery_attempts(status, scheduled_at);
