package com.northstar.funding.persistence;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for repository integration tests in the persistence module.
 * <p>
 * Provides shared TestContainers configuration for PostgreSQL.
 * Uses @DataJdbcTest for fast repository slice testing.
 * <p>
 * All repository integration tests should extend this class instead of
 * configuring TestContainers directly.
 */
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPersistenceIntegrationTest {

    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeAll
    static void verifyContainer() {
        if (!postgres.isRunning()) {
            throw new IllegalStateException("PostgreSQL container failed to start");
        }
    }
}
