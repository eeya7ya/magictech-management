-- ================================================
-- MagicTech Management System
-- Notification System Database Migration
-- Version: 1.0
-- Date: 2025-11-21
-- ================================================

-- This script creates the necessary tables for the pub/sub notification system.
-- Run this script if tables are not auto-created by Hibernate.

-- ================================================
-- 1. NOTIFICATIONS TABLE
-- ================================================

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    target_device_id VARCHAR(100),
    target_module VARCHAR(50),
    read_status BOOLEAN NOT NULL DEFAULT false,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    metadata TEXT,
    active BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT chk_notification_type CHECK (notification_type IN ('INFO', 'WARNING', 'SUCCESS', 'ERROR')),
    CONSTRAINT chk_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_notifications_target_module ON notifications(target_module) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_notifications_timestamp ON notifications(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_read_status ON notifications(read_status, target_module) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_notifications_entity ON notifications(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_notifications_module ON notifications(module, active);

-- ================================================
-- 2. DEVICE REGISTRATIONS TABLE
-- ================================================

CREATE TABLE IF NOT EXISTS device_registrations (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL UNIQUE,
    device_name VARCHAR(200),
    module_type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    username VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    hostname VARCHAR(200),
    application_version VARCHAR(50),
    subscribed_channels TEXT,
    active BOOLEAN NOT NULL DEFAULT true,

    CONSTRAINT chk_status CHECK (status IN ('ONLINE', 'OFFLINE', 'IDLE')),
    CONSTRAINT chk_module_type CHECK (module_type IN ('sales', 'projects', 'storage', 'maintenance', 'pricing'))
);

-- Create indexes for device queries
CREATE INDEX IF NOT EXISTS idx_devices_status ON device_registrations(status, module_type) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_devices_user ON device_registrations(user_id) WHERE active = true;
CREATE INDEX IF NOT EXISTS idx_devices_heartbeat ON device_registrations(last_heartbeat) WHERE status = 'ONLINE' AND active = true;

-- ================================================
-- 3. POSTGRESQL LISTEN/NOTIFY TRIGGERS (OPTIONAL)
-- ================================================

-- Create trigger function to notify on new notifications
CREATE OR REPLACE FUNCTION notify_new_notification()
RETURNS TRIGGER AS $$
DECLARE
    payload JSON;
BEGIN
    -- Build JSON payload
    payload := json_build_object(
        'id', NEW.id,
        'type', NEW.notification_type,
        'module', NEW.module,
        'action', NEW.action,
        'entity_type', NEW.entity_type,
        'entity_id', NEW.entity_id,
        'title', NEW.title,
        'message', NEW.message,
        'target_module', NEW.target_module,
        'priority', NEW.priority,
        'timestamp', NEW.timestamp
    );

    -- Notify on module-specific channel
    IF NEW.target_module IS NOT NULL THEN
        PERFORM pg_notify(
            CONCAT(NEW.target_module, '_notifications'),
            payload::text
        );
    END IF;

    -- Always notify on all_notifications channel
    PERFORM pg_notify('all_notifications', payload::text);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger on notifications table
DROP TRIGGER IF EXISTS trigger_notify_new_notification ON notifications;
CREATE TRIGGER trigger_notify_new_notification
    AFTER INSERT ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION notify_new_notification();

-- ================================================
-- 4. UTILITY VIEWS
-- ================================================

-- View for active notifications by module
CREATE OR REPLACE VIEW v_active_notifications AS
SELECT
    n.id,
    n.notification_type,
    n.module,
    n.action,
    n.entity_type,
    n.entity_id,
    n.title,
    n.message,
    n.target_module,
    n.read_status,
    n.timestamp,
    n.created_by,
    n.priority
FROM notifications n
WHERE n.active = true
ORDER BY n.timestamp DESC;

-- View for online devices by module
CREATE OR REPLACE VIEW v_online_devices AS
SELECT
    d.device_id,
    d.device_name,
    d.module_type,
    d.username,
    d.last_heartbeat,
    d.registered_at,
    EXTRACT(EPOCH FROM (NOW() - d.last_heartbeat)) AS seconds_since_heartbeat
FROM device_registrations d
WHERE d.status = 'ONLINE'
  AND d.active = true
ORDER BY d.module_type, d.username;

-- View for unread notifications by module
CREATE OR REPLACE VIEW v_unread_notifications AS
SELECT
    target_module,
    COUNT(*) AS unread_count,
    MAX(timestamp) AS latest_notification
FROM notifications
WHERE read_status = false
  AND active = true
  AND target_module IS NOT NULL
GROUP BY target_module;

-- ================================================
-- 5. HELPER FUNCTIONS
-- ================================================

-- Function to mark notifications as read
CREATE OR REPLACE FUNCTION mark_notifications_read(p_module VARCHAR, p_device_id VARCHAR DEFAULT NULL)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE notifications
    SET read_status = true
    WHERE target_module = p_module
      AND read_status = false
      AND active = true
      AND (p_device_id IS NULL OR target_device_id = p_device_id);

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Function to clean old notifications (older than 30 days)
CREATE OR REPLACE FUNCTION cleanup_old_notifications(days_to_keep INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE notifications
    SET active = false
    WHERE timestamp < (CURRENT_TIMESTAMP - INTERVAL '1 day' * days_to_keep)
      AND active = true;

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to mark stale devices as offline
CREATE OR REPLACE FUNCTION mark_stale_devices_offline(timeout_seconds INTEGER DEFAULT 300)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE device_registrations
    SET status = 'OFFLINE'
    WHERE status = 'ONLINE'
      AND last_heartbeat < (CURRENT_TIMESTAMP - INTERVAL '1 second' * timeout_seconds)
      AND active = true;

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- ================================================
-- 6. SAMPLE DATA (OPTIONAL - FOR TESTING)
-- ================================================

-- Uncomment below to insert sample notifications for testing

/*
INSERT INTO notifications (notification_type, module, action, entity_type, entity_id, title, message, target_module, priority, created_by)
VALUES
    ('INFO', 'sales', 'created', 'project', 1, 'New Project Created', 'A new project has been created from Sales module', 'projects', 'HIGH', 'admin'),
    ('WARNING', 'projects', 'confirmation_requested', 'project', 1, 'Confirmation Requested', 'Project requires confirmation from Sales', 'sales', 'URGENT', 'project_manager'),
    ('SUCCESS', 'projects', 'completed', 'project', 1, 'Project Completed', 'Project has been completed and is ready for analysis', 'storage', 'HIGH', 'project_manager'),
    ('SUCCESS', 'projects', 'completed', 'project', 1, 'Project Completed', 'Project has been completed and is ready for pricing analysis', 'pricing', 'HIGH', 'project_manager');
*/

-- ================================================
-- 7. MAINTENANCE QUERIES
-- ================================================

-- Query to view notification statistics
-- SELECT target_module, notification_type, COUNT(*) as count
-- FROM notifications
-- WHERE active = true
-- GROUP BY target_module, notification_type
-- ORDER BY target_module, notification_type;

-- Query to view device statistics
-- SELECT module_type, status, COUNT(*) as count
-- FROM device_registrations
-- WHERE active = true
-- GROUP BY module_type, status
-- ORDER BY module_type, status;

-- Query to find devices that need to be marked offline
-- SELECT device_id, device_name, module_type, last_heartbeat,
--        EXTRACT(EPOCH FROM (NOW() - last_heartbeat)) AS seconds_since_heartbeat
-- FROM device_registrations
-- WHERE status = 'ONLINE'
--   AND last_heartbeat < (CURRENT_TIMESTAMP - INTERVAL '5 minutes')
--   AND active = true;

-- ================================================
-- END OF MIGRATION SCRIPT
-- ================================================

-- Verify tables were created
SELECT 'Notifications table created' AS status
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notifications');

SELECT 'Device registrations table created' AS status
WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'device_registrations');

-- Display table counts
SELECT
    'notifications' AS table_name,
    COUNT(*) AS row_count
FROM notifications
UNION ALL
SELECT
    'device_registrations' AS table_name,
    COUNT(*) AS row_count
FROM device_registrations;
