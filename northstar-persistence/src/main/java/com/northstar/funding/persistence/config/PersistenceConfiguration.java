package com.northstar.funding.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * Spring Data JDBC Configuration for NorthStar Funding Persistence
 *
 * Pure persistence layer configuration with zero business logic.
 * Registers repositories and custom converters for PostgreSQL-Java mapping.
 *
 * Note: Custom converters will be added as needed for:
 * - TEXT[] to Set<String> conversions
 * - Enum conversions
 * - JSON/JSONB conversions
 */
@Configuration
@EnableJdbcRepositories(basePackages = "com.northstar.funding.persistence.repository")
public class PersistenceConfiguration extends AbstractJdbcConfiguration {

    // Custom converters will be registered here via jdbcCustomConversions()
    // when needed for specific type mappings
}
