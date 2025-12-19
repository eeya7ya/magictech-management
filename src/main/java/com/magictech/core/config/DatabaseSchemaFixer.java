package com.magictech.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixes database schema issues on application startup
 */
@Component
public class DatabaseSchemaFixer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaFixer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1) // Run early
    @Transactional
    public void fixWorkflowTableSchemas() {
        try {
            logger.info("Checking workflow-related table schemas...");

            // Fix all workflow data tables
            fixTableSchema("site_survey_data", this::recreateSiteSurveyTable);
            fixTableSchema("sizing_pricing_data", this::recreateSizingPricingTable);
            fixTableSchema("bank_guarantee_data", this::recreateBankGuaranteeTable);
            fixTableSchema("project_cost_data", this::recreateProjectCostTable);

            logger.info("Workflow table schema checks completed");
        } catch (Exception e) {
            logger.error("Error fixing workflow table schemas", e);
            // Don't throw - allow application to start
        }
    }

    private void fixTableSchema(String tableName, Runnable recreateFunction) {
        try {
            logger.info("Checking {} table schema...", tableName);

            // Check if table exists and has zip_file column
            String checkSql = "SELECT column_name FROM information_schema.columns " +
                            "WHERE table_name = ? AND column_name = 'zip_file'";

            try {
                jdbcTemplate.queryForObject(checkSql, String.class, tableName);
                logger.info("{} table has zip_file column - schema is correct", tableName);
            } catch (Exception e) {
                // zip_file column doesn't exist - need to recreate table
                logger.warn("{} table is missing zip_file column, recreating table...", tableName);
                recreateFunction.run();
            }
        } catch (Exception e) {
            logger.error("Error checking {} table schema", tableName, e);
        }
    }

    private void recreateSiteSurveyTable() {
        logger.info("Dropping and recreating site_survey_data table...");

        // Drop the table
        jdbcTemplate.execute("DROP TABLE IF EXISTS site_survey_data CASCADE");
        logger.info("Dropped site_survey_data table");

        // Create with correct schema including ZIP file support
        String createTableSql = """
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
            )
            """;

        jdbcTemplate.execute(createTableSql);
        logger.info("Created site_survey_data table with correct schema");

        // Create indexes
        jdbcTemplate.execute("CREATE INDEX idx_site_survey_project_id ON site_survey_data(project_id)");
        jdbcTemplate.execute("CREATE INDEX idx_site_survey_workflow_id ON site_survey_data(workflow_id)");
        logger.info("Created indexes on site_survey_data table");

        logger.info("Successfully recreated site_survey_data table");
    }

    private void recreateSizingPricingTable() {
        logger.info("Dropping and recreating sizing_pricing_data table...");

        // Drop the table
        jdbcTemplate.execute("DROP TABLE IF EXISTS sizing_pricing_data CASCADE");
        logger.info("Dropped sizing_pricing_data table");

        // Create with correct schema including ZIP file support
        String createTableSql = """
            CREATE TABLE sizing_pricing_data (
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
                uploaded_by VARCHAR(100) NOT NULL,
                uploaded_by_id BIGINT,
                uploaded_at TIMESTAMP NOT NULL,
                approved_by_sales VARCHAR(100),
                approved_at TIMESTAMP,
                notes TEXT,
                last_updated_at TIMESTAMP,
                active BOOLEAN NOT NULL DEFAULT true
            )
            """;

        jdbcTemplate.execute(createTableSql);
        logger.info("Created sizing_pricing_data table with correct schema");

        // Create indexes
        jdbcTemplate.execute("CREATE INDEX idx_sizing_pricing_project_id ON sizing_pricing_data(project_id)");
        jdbcTemplate.execute("CREATE INDEX idx_sizing_pricing_workflow_id ON sizing_pricing_data(workflow_id)");
        logger.info("Created indexes on sizing_pricing_data table");

        logger.info("Successfully recreated sizing_pricing_data table");
    }

    private void recreateBankGuaranteeTable() {
        logger.info("Dropping and recreating bank_guarantee_data table...");

        // Drop the table
        jdbcTemplate.execute("DROP TABLE IF EXISTS bank_guarantee_data CASCADE");
        logger.info("Dropped bank_guarantee_data table");

        // Create with correct schema including ZIP file support
        String createTableSql = """
            CREATE TABLE bank_guarantee_data (
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
                uploaded_by VARCHAR(100) NOT NULL,
                uploaded_by_id BIGINT,
                uploaded_at TIMESTAMP NOT NULL,
                approved_by_sales VARCHAR(100),
                approved_at TIMESTAMP,
                notes TEXT,
                last_updated_at TIMESTAMP,
                active BOOLEAN NOT NULL DEFAULT true
            )
            """;

        jdbcTemplate.execute(createTableSql);
        logger.info("Created bank_guarantee_data table with correct schema");

        // Create indexes
        jdbcTemplate.execute("CREATE INDEX idx_bank_guarantee_project_id ON bank_guarantee_data(project_id)");
        jdbcTemplate.execute("CREATE INDEX idx_bank_guarantee_workflow_id ON bank_guarantee_data(workflow_id)");
        logger.info("Created indexes on bank_guarantee_data table");

        logger.info("Successfully recreated bank_guarantee_data table");
    }

    private void recreateProjectCostTable() {
        logger.info("Dropping and recreating project_cost_data table...");

        // Drop the table
        jdbcTemplate.execute("DROP TABLE IF EXISTS project_cost_data CASCADE");
        logger.info("Dropped project_cost_data table");

        // Create with correct schema including ZIP file support
        String createTableSql = """
            CREATE TABLE project_cost_data (
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
                uploaded_by VARCHAR(100) NOT NULL,
                uploaded_by_id BIGINT,
                uploaded_at TIMESTAMP NOT NULL,
                project_received_confirmation BOOLEAN DEFAULT false,
                notes TEXT,
                last_updated_at TIMESTAMP,
                active BOOLEAN NOT NULL DEFAULT true
            )
            """;

        jdbcTemplate.execute(createTableSql);
        logger.info("Created project_cost_data table with correct schema");

        // Create indexes
        jdbcTemplate.execute("CREATE INDEX idx_project_cost_project_id ON project_cost_data(project_id)");
        jdbcTemplate.execute("CREATE INDEX idx_project_cost_workflow_id ON project_cost_data(workflow_id)");
        logger.info("Created indexes on project_cost_data table");

        logger.info("Successfully recreated project_cost_data table");
    }
}
