package com.northstar.funding.discovery.infrastructure.config;

import com.northstar.funding.discovery.infrastructure.converters.AdminRoleConverter;
import com.northstar.funding.discovery.infrastructure.converters.CandidateStatusConverter;
import com.northstar.funding.discovery.infrastructure.converters.QueryTagSetConverter;
import com.northstar.funding.discovery.infrastructure.converters.SearchEngineTypeConverter;
import com.northstar.funding.discovery.infrastructure.converters.SearchEngineTypeSetConverter;
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
@EnableJdbcRepositories(basePackages = {
    "com.northstar.funding.discovery.infrastructure",
    "com.northstar.funding.discovery.search.infrastructure"
})
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    @Override
    public JdbcCustomConversions jdbcCustomConversions() {
        return new JdbcCustomConversions(Arrays.asList(
            // AdminRole enum converters
            new AdminRoleConverter.AdminRoleReadingConverter(),
            new AdminRoleConverter.AdminRoleWritingConverter(),
            // CandidateStatus enum converters
            new CandidateStatusConverter.CandidateStatusReadingConverter(),
            new CandidateStatusConverter.CandidateStatusWritingConverter(),
            // SearchEngineType enum converters (for Feature 003)
            new SearchEngineTypeConverter.SearchEngineTypeReadingConverter(),
            new SearchEngineTypeConverter.SearchEngineTypeWritingConverter(),
            // Set<String> TEXT[] converters for target_engines (for Feature 003)
            new SearchEngineTypeSetConverter.SearchEngineTypeSetReadingConverter(),
            new SearchEngineTypeSetConverter.SearchEngineTypeSetWritingConverter(),
            // Set<String> TEXT[] converters for tags (for Feature 003)
            new QueryTagSetConverter.QueryTagSetReadingConverter(),
            new QueryTagSetConverter.QueryTagSetWritingConverter()
            // Note: Set<String> for target_engines stores enum names: {"SEARXNG", "TAVILY"}
            // Note: Set<String> for tags stores "TYPE:value" format: {"GEOGRAPHY:Bulgaria"}
        ));
    }
}
