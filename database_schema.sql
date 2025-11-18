-- =============================================================================
-- MagicTech Management System - Database Schema
-- New Tables for Notification & Approval Workflow
-- =============================================================================
-- Run this script in your PostgreSQL database if Hibernate doesn't auto-create
-- Or if you prefer manual table creation
-- =============================================================================

-- Table: notifications
-- Purpose: Store all system notifications with 3-month retention
-- =============================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    target_role VARCHAR(50),
    module VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    related_id BIGINT,
    related_type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_shown BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    created_by VARCHAR(100)
);

-- Indexes for notifications
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_target_role ON notifications(target_role);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);

-- =============================================================================
-- Table: pending_approvals
-- Purpose: Track approval requests between modules (2-day auto-timeout)
-- =============================================================================
CREATE TABLE IF NOT EXISTS pending_approvals (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    requested_by_user_id BIGINT,
    approver_role VARCHAR(50) NOT NULL,
    approver_user_id BIGINT,
    project_id BIGINT,
    storage_item_id BIGINT,
    quantity INTEGER,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    processed_by VARCHAR(100),
    processing_notes TEXT,
    notification_id BIGINT
);

-- Indexes for pending_approvals
CREATE INDEX IF NOT EXISTS idx_pending_approvals_status ON pending_approvals(status);
CREATE INDEX IF NOT EXISTS idx_pending_approvals_approver_role ON pending_approvals(approver_role);
CREATE INDEX IF NOT EXISTS idx_pending_approvals_project_id ON pending_approvals(project_id);
CREATE INDEX IF NOT EXISTS idx_pending_approvals_expires_at ON pending_approvals(expires_at);

-- =============================================================================
-- Table: project_cost_breakdowns
-- Purpose: Store cost breakdown for projects
-- =============================================================================
CREATE TABLE IF NOT EXISTS project_cost_breakdowns (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL UNIQUE,
    elements_subtotal NUMERIC(15, 2) DEFAULT 0.00,
    tax_rate NUMERIC(5, 4) DEFAULT 0.0000,
    sale_offer_rate NUMERIC(5, 4) DEFAULT 0.0000,
    installation_cost NUMERIC(15, 2) DEFAULT 0.00,
    licenses_cost NUMERIC(15, 2) DEFAULT 0.00,
    additional_cost NUMERIC(15, 2) DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) DEFAULT 0.00,
    discount_amount NUMERIC(15, 2) DEFAULT 0.00,
    total_cost NUMERIC(15, 2) DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Index for project_cost_breakdowns
CREATE INDEX IF NOT EXISTS idx_project_cost_breakdowns_project_id ON project_cost_breakdowns(project_id);

-- =============================================================================
-- Table: customer_cost_breakdowns
-- Purpose: Store cost breakdown for customer orders
-- =============================================================================
CREATE TABLE IF NOT EXISTS customer_cost_breakdowns (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    sales_order_id BIGINT UNIQUE,
    items_subtotal NUMERIC(15, 2) DEFAULT 0.00,
    tax_rate NUMERIC(5, 4) DEFAULT 0.0000,
    sale_offer_rate NUMERIC(5, 4) DEFAULT 0.0000,
    installation_cost NUMERIC(15, 2) DEFAULT 0.00,
    licenses_cost NUMERIC(15, 2) DEFAULT 0.00,
    additional_cost NUMERIC(15, 2) DEFAULT 0.00,
    tax_amount NUMERIC(15, 2) DEFAULT 0.00,
    discount_amount NUMERIC(15, 2) DEFAULT 0.00,
    total_cost NUMERIC(15, 2) DEFAULT 0.00,
    order_date TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Indexes for customer_cost_breakdowns
CREATE INDEX IF NOT EXISTS idx_customer_cost_breakdowns_customer_id ON customer_cost_breakdowns(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_cost_breakdowns_sales_order_id ON customer_cost_breakdowns(sales_order_id);
CREATE INDEX IF NOT EXISTS idx_customer_cost_breakdowns_order_date ON customer_cost_breakdowns(order_date);
CREATE INDEX IF NOT EXISTS idx_customer_cost_breakdowns_active ON customer_cost_breakdowns(active);

-- =============================================================================
-- Verify Tables Created
-- =============================================================================
SELECT
    table_name,
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_name = t.table_name) as column_count
FROM information_schema.tables t
WHERE table_schema = 'public'
AND table_name IN ('notifications', 'pending_approvals', 'project_cost_breakdowns', 'customer_cost_breakdowns')
ORDER BY table_name;

-- =============================================================================
-- Sample Data (Optional - for testing)
-- =============================================================================

-- Insert a test notification
-- INSERT INTO notifications (module, type, title, message, target_role, priority, created_by)
-- VALUES ('SALES', 'TEST', 'Test Notification', 'This is a test notification', 'MASTER', 'NORMAL', 'system');

-- =============================================================================
-- Cleanup Commands (Use with caution!)
-- =============================================================================

-- Drop all tables (DANGEROUS - will delete all data!)
-- DROP TABLE IF EXISTS customer_cost_breakdowns CASCADE;
-- DROP TABLE IF EXISTS project_cost_breakdowns CASCADE;
-- DROP TABLE IF EXISTS pending_approvals CASCADE;
-- DROP TABLE IF EXISTS notifications CASCADE;

-- Truncate tables (keeps structure, deletes data)
-- TRUNCATE TABLE notifications RESTART IDENTITY CASCADE;
-- TRUNCATE TABLE pending_approvals RESTART IDENTITY CASCADE;
-- TRUNCATE TABLE project_cost_breakdowns RESTART IDENTITY CASCADE;
-- TRUNCATE TABLE customer_cost_breakdowns RESTART IDENTITY CASCADE;

-- =============================================================================
-- End of Schema
-- =============================================================================
