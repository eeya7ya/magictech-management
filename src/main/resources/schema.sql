-- Drop and recreate notifications table to fix schema issues
DROP TABLE IF EXISTS notifications CASCADE;

-- Table will be recreated by Hibernate with proper schema

-- ================================================
-- FIX: Alter excel_file columns from OID to BYTEA
-- This is needed because Hibernate 6.x @Lob incorrectly created OID columns
-- and ddl-auto=update doesn't change column types
-- ================================================

-- Fix site_survey_data table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'site_survey_data'
        AND column_name = 'excel_file'
        AND data_type = 'oid'
    ) THEN
        -- Drop the old OID column and recreate as BYTEA
        ALTER TABLE site_survey_data DROP COLUMN IF EXISTS excel_file;
        ALTER TABLE site_survey_data ADD COLUMN excel_file BYTEA;
        RAISE NOTICE 'Fixed site_survey_data.excel_file column type';
    END IF;
END $$;

-- Fix sizing_pricing_data table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sizing_pricing_data'
        AND column_name = 'excel_file'
        AND data_type = 'oid'
    ) THEN
        ALTER TABLE sizing_pricing_data DROP COLUMN IF EXISTS excel_file;
        ALTER TABLE sizing_pricing_data ADD COLUMN excel_file BYTEA;
        RAISE NOTICE 'Fixed sizing_pricing_data.excel_file column type';
    END IF;
END $$;

-- Fix bank_guarantee_data table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bank_guarantee_data'
        AND column_name = 'excel_file'
        AND data_type = 'oid'
    ) THEN
        ALTER TABLE bank_guarantee_data DROP COLUMN IF EXISTS excel_file;
        ALTER TABLE bank_guarantee_data ADD COLUMN excel_file BYTEA;
        RAISE NOTICE 'Fixed bank_guarantee_data.excel_file column type';
    END IF;
END $$;

-- Fix project_cost_data table
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'project_cost_data'
        AND column_name = 'excel_file'
        AND data_type = 'oid'
    ) THEN
        ALTER TABLE project_cost_data DROP COLUMN IF EXISTS excel_file;
        ALTER TABLE project_cost_data ADD COLUMN excel_file BYTEA;
        RAISE NOTICE 'Fixed project_cost_data.excel_file column type';
    END IF;
END $$;
