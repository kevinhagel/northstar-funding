package com.northstar.funding.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Test configuration for @DataJdbcTest repository tests.
 *
 * @DataJdbcTest requires a @SpringBootConfiguration class to bootstrap the context.
 * Since northstar-persistence is a library module without an application class,
 * we need this test configuration for repository integration tests.
 *
 * Enables:
 * - Spring Boot auto-configuration
 * - Spring Data JDBC repositories in the persistence.repository package
 * - Flyway migrations (via auto-configuration)
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EnableJdbcRepositories(basePackages = "com.northstar.funding.persistence.repository")
public class TestDataJdbcConfiguration {
}
