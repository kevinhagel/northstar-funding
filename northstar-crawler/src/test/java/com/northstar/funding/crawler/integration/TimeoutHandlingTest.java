package com.northstar.funding.crawler.integration;

import com.northstar.funding.crawler.adapter.BraveSearchAdapter;
import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.adapter.SerperAdapter;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for Scenario 7: Timeout Handling.
 *
 * T045: Tests that adapters have appropriate timeout configurations.
 * This smoke test verifies configuration without actual HTTP timeouts.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@DisplayName("Integration Test - Scenario 7: Timeout Handling")
class TimeoutHandlingTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");

        // Configure providers with custom timeouts (in milliseconds)
        registry.add("search.providers.brave.api-key", () -> "test-key");
        registry.add("search.providers.brave.timeout", () -> "5000"); // 5 seconds

        registry.add("search.providers.searxng.base-url", () -> "http://192.168.1.10:8080");
        registry.add("search.providers.searxng.timeout", () -> "7000"); // 7 seconds

        registry.add("search.providers.serper.api-key", () -> "test-key");
        registry.add("search.providers.serper.timeout", () -> "5000"); // 5 seconds
    }

    @Autowired
    private BraveSearchAdapter braveAdapter;

    @Autowired
    private SearxngAdapter searxngAdapter;

    @Autowired
    private SerperAdapter serperAdapter;

    @Autowired
    private SearchProviderConfig config;

    @Test
    @DisplayName("Spring context loads with all adapters")
    void contextLoads_AllAdapters() {
        assertThat(braveAdapter).isNotNull();
        assertThat(searxngAdapter).isNotNull();
        assertThat(serperAdapter).isNotNull();
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Brave adapter has 5 second timeout configured")
    void braveAdapter_HasCorrectTimeout() {
        assertThat(config.getBraveSearch().getTimeout()).isEqualTo(5000); // 5 seconds in millis
    }

    @Test
    @DisplayName("SearXNG adapter has 7 second timeout (metasearch aggregation)")
    void searxngAdapter_HasLongerTimeout() {
        // SearXNG needs longer timeout because it aggregates multiple engines
        assertThat(config.getSearxng().getTimeout()).isEqualTo(7000); // 7 seconds in millis
    }

    @Test
    @DisplayName("Serper adapter has 5 second timeout configured")
    void serperAdapter_HasCorrectTimeout() {
        assertThat(config.getSerper().getTimeout()).isEqualTo(5000); // 5 seconds in millis
    }

    @Test
    @DisplayName("All adapters have timeouts configured (none are unlimited)")
    void allAdapters_HaveFiniteTimeouts() {
        assertThat(config.getBraveSearch().getTimeout()).isLessThan(60000); // Less than 1 minute
        assertThat(config.getSearxng().getTimeout()).isLessThan(60000);
        assertThat(config.getSerper().getTimeout()).isLessThan(60000);
    }

    @Test
    @DisplayName("SearXNG has longest timeout for metasearch aggregation")
    void searxng_HasLongestTimeout() {
        // SearXNG aggregates multiple engines so needs more time
        assertThat(config.getSearxng().getTimeout()).isGreaterThan(config.getBraveSearch().getTimeout());
        assertThat(config.getBraveSearch().getTimeout()).isEqualTo(config.getSerper().getTimeout());
    }
}
