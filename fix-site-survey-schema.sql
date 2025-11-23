-- Fix site_survey_data table schema
-- The excel_file column was created as bigint instead of BYTEA

-- Drop the existing table to recreate with correct schema
DROP TABLE IF EXISTS site_survey_data CASCADE;

-- Hibernate will recreate it with correct schema on next application start
-- Alternative: Manual column alteration (use this if you want to preserve existing data)
-- ALTER TABLE site_survey_data ALTER COLUMN excel_file TYPE BYTEA USING NULL;
