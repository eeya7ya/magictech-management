-- Drop and recreate notifications table to fix schema issues
DROP TABLE IF EXISTS notifications CASCADE;

-- Table will be recreated by Hibernate with proper schema

-- Drop the users_role_check constraint to allow new UserRole enum values
-- The constraint was created with an older set of roles and needs to be updated
-- Hibernate will recreate it with all current enum values (MASTER, PRESALES, SALES,
-- SALES_MANAGER, QUALITY_ASSURANCE, FINANCE, MAINTENANCE, PROJECTS, PROJECT_SUPPLIER, STORAGE, CLIENT, PRICING)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
