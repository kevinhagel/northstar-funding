package com.northstar.funding.discovery.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Test configuration that ensures clean database state for tests.
 * Forces Flyway to clean and migrate the test schema before running tests.
 */
@TestConfiguration
public class TestFlywayConfig {

    @Bean
    public Flyway testFlyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .schemas("test_schema")
            .defaultSchema("test_schema")
            .createSchemas(true)
            .locations("classpath:db/migration")
            .load();
        
        // Clean the schema first to remove old migrations
        flyway.clean();
        
        // Run migrations with clean schema
        flyway.migrate();
        
        return flyway;
    }
}
