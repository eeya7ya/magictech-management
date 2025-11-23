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
    public void fixSiteSurveyTableSchema() {
        try {
            logger.info("Checking site_survey_data table schema...");

            // Check if table exists and has the wrong type
            String checkSql = "SELECT column_name, data_type FROM information_schema.columns " +
                            "WHERE table_name = 'site_survey_data' AND column_name = 'excel_file'";

            try {
                String dataType = jdbcTemplate.queryForObject(
                    checkSql,
                    (rs, rowNum) -> rs.getString("data_type")
                );

                logger.info("Found excel_file column with type: {}", dataType);

                // If the column exists but is not bytea, we need to recreate the table
                if (!"bytea".equals(dataType)) {
                    logger.warn("excel_file column has wrong type ({}), recreating table...", dataType);
                    recreateSiteSurveyTable();
                } else {
                    logger.info("site_survey_data table schema is correct");
                }
            } catch (Exception e) {
                // Table doesn't exist yet, that's fine - Hibernate will create it
                logger.info("site_survey_data table doesn't exist yet - will be created by Hibernate");
            }

        } catch (Exception e) {
            logger.error("Error fixing site_survey_data table schema", e);
            // Don't throw - allow application to start
        }
    }

    private void recreateSiteSurveyTable() {
        logger.info("Dropping and recreating site_survey_data table...");

        // Drop the table
        jdbcTemplate.execute("DROP TABLE IF EXISTS site_survey_data CASCADE");
        logger.info("Dropped site_survey_data table");

        // Create with correct schema
        String createTableSql = """
            CREATE TABLE site_survey_data (
                id BIGSERIAL PRIMARY KEY,
                project_id BIGINT NOT NULL,
                workflow_id BIGINT NOT NULL,
                excel_file BYTEA,
                file_name VARCHAR(255),
                file_size BIGINT,
                mime_type VARCHAR(100),
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
}
