-- RecoverAI Core Recovery Domain Schema Migration (V2)
-- Supports Multi-tenant Merchant, Customer, Payment, Recovery Case, Recovery Attempt, AI Decision, and Audit Event models.

-- 1. Merchants Table
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    razorpay_account_id VARCHAR(100),
    webhook_secret VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_merchants_email UNIQUE (email),
    CONSTRAINT uq_merchants_razorpay_account UNIQUE (razorpay_account_id),
    CONSTRAINT chk_merchants_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX IF NOT EXISTS idx_merchants_status ON merchants(status);
CREATE INDEX IF NOT EXISTS idx_merchants_razorpay_account ON merchants(razorpay_account_id);

-- 2. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    razorpay_customer_id VARCHAR(100),
    name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customers_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT uq_customers_merchant_email UNIQUE (merchant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_customers_merchant_id ON customers(merchant_id);
CREATE INDEX IF NOT EXISTS idx_customers_merchant_razorpay ON customers(merchant_id, razorpay_customer_id);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);

-- 3. Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    customer_id UUID,
    razorpay_payment_id VARCHAR(100) NOT NULL,
    razorpay_order_id VARCHAR(100),
    razorpay_invoice_id VARCHAR(100),
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(50) NOT NULL,
    method VARCHAR(50),
    error_code VARCHAR(100),
    error_description TEXT,
    error_source VARCHAR(100),
    error_reason VARCHAR(100),
    risk_level VARCHAR(50),
    payment_created_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT uq_payments_merchant_razorpay_payment UNIQUE (merchant_id, razorpay_payment_id),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('CREATED', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'PENDING'))
);

CREATE INDEX IF NOT EXISTS idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_merchant_status ON payments(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_payments_razorpay_payment_id ON payments(razorpay_payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_razorpay_order_id ON payments(razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);

-- 4. Recovery Cases Table
CREATE TABLE IF NOT EXISTS recovery_cases (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    customer_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    failure_reason_category VARCHAR(100),
    estimated_recoverable_amount NUMERIC(15, 2) NOT NULL,
    recovered_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    expires_at TIMESTAMP WITH TIME ZONE,
    recovered_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_cases_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_cases_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_cases_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT uq_recovery_cases_payment UNIQUE (payment_id),
    CONSTRAINT chk_recovery_cases_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RECOVERED', 'FAILED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_recovery_cases_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_recovery_cases_est_amount CHECK (estimated_recoverable_amount >= 0),
    CONSTRAINT chk_recovery_cases_rec_amount CHECK (recovered_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_recovery_cases_merchant_id ON recovery_cases(merchant_id);
CREATE INDEX IF NOT EXISTS idx_recovery_cases_customer_id ON recovery_cases(customer_id);
CREATE INDEX IF NOT EXISTS idx_recovery_cases_merchant_status ON recovery_cases(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_recovery_cases_priority ON recovery_cases(priority);
CREATE INDEX IF NOT EXISTS idx_recovery_cases_payment_id ON recovery_cases(payment_id);
CREATE INDEX IF NOT EXISTS idx_recovery_cases_created_at ON recovery_cases(created_at);

-- 5. Recovery Attempts Table
CREATE TABLE IF NOT EXISTS recovery_attempts (
    id UUID PRIMARY KEY,
    recovery_case_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    scheduled_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    result_code VARCHAR(100),
    result_message TEXT,
    recovery_link VARCHAR(1000),
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_attempts_case FOREIGN KEY (recovery_case_id) REFERENCES recovery_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_recovery_attempts_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT uq_recovery_attempts_case_attempt_num UNIQUE (recovery_case_id, attempt_number),
    CONSTRAINT chk_recovery_attempts_num CHECK (attempt_number > 0),
    CONSTRAINT chk_recovery_attempts_channel CHECK (channel IN ('WHATSAPP', 'EMAIL', 'SMS', 'RETRY_CHARGE', 'SMART_LINK', 'MANUAL')),
    CONSTRAINT chk_recovery_attempts_status CHECK (status IN ('SCHEDULED', 'IN_FLIGHT', 'SENT', 'DELIVERED', 'CLICKED', 'SUCCESS', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS idx_recovery_attempts_case_id ON recovery_attempts(recovery_case_id);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_merchant_id ON recovery_attempts(merchant_id);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_status ON recovery_attempts(status);
CREATE INDEX IF NOT EXISTS idx_recovery_attempts_scheduled_at ON recovery_attempts(scheduled_at);

-- 6. Agent / AI Decisions Table
CREATE TABLE IF NOT EXISTS agent_decisions (
    id UUID PRIMARY KEY,
    recovery_case_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    recommended_action VARCHAR(100) NOT NULL,
    channel VARCHAR(50),
    confidence_score NUMERIC(5, 4),
    reasoning TEXT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50),
    prompt_tokens INT,
    completion_tokens INT,
    raw_response TEXT,
    decision_factors TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_decisions_case FOREIGN KEY (recovery_case_id) REFERENCES recovery_cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_decisions_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT chk_agent_decisions_confidence CHECK (confidence_score IS NULL OR (confidence_score >= 0.0 AND confidence_score <= 1.0))
);

CREATE INDEX IF NOT EXISTS idx_agent_decisions_case_id ON agent_decisions(recovery_case_id);
CREATE INDEX IF NOT EXISTS idx_agent_decisions_merchant_id ON agent_decisions(merchant_id);
CREATE INDEX IF NOT EXISTS idx_agent_decisions_created_at ON agent_decisions(created_at);

-- 7. Audit Events Table (Immutable Record)
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY,
    merchant_id UUID,
    event_type VARCHAR(100) NOT NULL,
    actor_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100),
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_events_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT chk_audit_events_actor_type CHECK (actor_type IN ('SYSTEM', 'AGENT', 'USER', 'WEBHOOK'))
);

CREATE INDEX IF NOT EXISTS idx_audit_events_merchant_id ON audit_events(merchant_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at);
