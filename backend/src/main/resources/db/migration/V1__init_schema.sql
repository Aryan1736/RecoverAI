-- RecoverAI Initial Schema Migration (Foundation)
-- Verification table for database connection and Flyway migration testing

CREATE TABLE IF NOT EXISTS system_health_check (
    id VARCHAR(36) PRIMARY KEY,
    component VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO system_health_check (id, component, status)
VALUES ('init-001', 'database_migration', 'INITIALIZED');
