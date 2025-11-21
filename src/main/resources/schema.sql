-- Drop and recreate notifications table to fix schema issues
DROP TABLE IF EXISTS notifications CASCADE;

-- Table will be recreated by Hibernate with proper schema
