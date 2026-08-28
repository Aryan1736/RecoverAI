-- RecoverAI Merchant Authentication Schema Migration (V6)
-- Adds password_hash column to merchants table for secure merchant credentials storage.

ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
