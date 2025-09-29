package com.northstar.funding.discovery.infrastructure.config;

import com.northstar.funding.discovery.infrastructure.converters.AdminRoleConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

import java.util.Arrays;

/**
 * Spring Data JDBC Configuration for NorthStar Funding Discovery
 * 
 * Registers custom converters for proper mapping between PostgreSQL types and Java types.
 */
@Configuration
@EnableJdbcRepositories(basePackages = "com.northstar.funding.discovery.infrastructure")
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
            // AdminRole enum converters
            new AdminRoleConverter.AdminRoleReadingConverter(),
            new AdminRoleConverter.AdminRoleWritingConverter()
            // No specializations converters needed - Spring Data JDBC handles Set<String> <-> TEXT[] natively
        ));
    }
}
