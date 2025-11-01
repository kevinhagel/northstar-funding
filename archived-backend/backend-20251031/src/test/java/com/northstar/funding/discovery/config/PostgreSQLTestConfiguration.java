package com.northstar.funding.discovery.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL TestContainers Configuration for Integration Tests
 * 
 * Provides production-parity PostgreSQL testing environment with:
 * - PostgreSQL 16 matching Mac Studio deployment
 * - Container reuse for performance optimization  
 * - Automatic Spring Boot integration via @ServiceConnection
 * - Flyway migration execution for schema consistency
 */
@TestConfiguration
@Testcontainers
public class PostgreSQLTestConfiguration {
    
    /**
     * Shared PostgreSQL container with reuse for performance.
     * Uses PostgreSQL 16 to match production environment.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("northstar_test")
            .withUsername("test_user") 
            .withPassword("test_password")
            .withReuse(true)  // Reuse container across test classes for performance
            .withCreateContainerCmdModifier(cmd -> 
                // Increase shared memory for PostgreSQL performance
                cmd.getHostConfig().withShmSize(256 * 1024 * 1024L)); // 256MB
    
    /**
     * Dynamic property configuration for Spring Boot integration.
     * Automatically configures DataSource properties from the container.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername); 
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Flyway configuration for consistent schema
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }
}
