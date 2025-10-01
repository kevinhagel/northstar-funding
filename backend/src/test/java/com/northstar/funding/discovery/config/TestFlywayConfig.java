package com.northstar.funding.discovery.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration to ensure Flyway migrations run before tests.
 */
@TestConfiguration
public class TestFlywayConfig {
    
    @Bean
    @Primary
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .load();
        
        // Execute migrations immediately
        flyway.migrate();
        
        return flyway;
    }
}
