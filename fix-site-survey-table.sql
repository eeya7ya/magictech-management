-- Fix site_survey_data table schema
-- This script drops and recreates the table with the correct data types

-- Drop the table if it exists
DROP TABLE IF EXISTS site_survey_data CASCADE;

-- Recreate with correct schema including ZIP file support
CREATE TABLE site_survey_data (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    excel_file BYTEA,
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(100),
    zip_file BYTEA,
    zip_file_name VARCHAR(255),
    zip_file_size BIGINT,
    zip_mime_type VARCHAR(100),
    file_type VARCHAR(20),
    parsed_data TEXT,
    survey_done_by VARCHAR(50),
    survey_done_by_user VARCHAR(100),
    survey_done_by_user_id BIGINT,
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_by_id BIGINT,
    uploaded_at TIMESTAMP NOT NULL,
    notes TEXT,
    last_updated_at TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT true
);

-- Create index on project_id for faster lookups
CREATE INDEX idx_site_survey_project_id ON site_survey_data(project_id);
CREATE INDEX idx_site_survey_workflow_id ON site_survey_data(workflow_id);
