package com.northstar.funding.crawler.integration;

import com.northstar.funding.crawler.adapter.BraveSearchAdapter;
import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.adapter.SerperAdapter;
import com.northstar.funding.crawler.adapter.TavilyAdapter;
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
 * Integration test for Scenario 6: Rate Limiting.
 *
 * T044: Tests that adapters have correct rate limits configured.
 * This smoke test verifies adapter configuration without actual API calls.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@DisplayName("Integration Test - Scenario 6: Rate Limiting")
class RateLimitingTest {

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

        // Configure providers
        registry.add("search.providers.brave.api-key", () -> "test-key");
        registry.add("search.providers.searxng.base-url", () -> "http://192.168.1.10:8080");
        registry.add("search.providers.serper.api-key", () -> "test-key");
        registry.add("search.providers.tavily.api-key", () -> "test-key");
    }

    @Autowired
    private BraveSearchAdapter braveAdapter;

    @Autowired
    private SearxngAdapter searxngAdapter;

    @Autowired
    private SerperAdapter serperAdapter;

    @Autowired
    private TavilyAdapter tavilyAdapter;

    @Test
    @DisplayName("Spring context loads with all adapters")
    void contextLoads_AllAdapters() {
        assertThat(braveAdapter).isNotNull();
        assertThat(searxngAdapter).isNotNull();
        assertThat(serperAdapter).isNotNull();
        assertThat(tavilyAdapter).isNotNull();
    }

    @Test
    @DisplayName("Brave adapter has correct daily rate limit (50)")
    void braveAdapter_HasCorrectRateLimit() {
        assertThat(braveAdapter.getRateLimit()).isEqualTo(50); // 50/day conservative limit
    }

    @Test
    @DisplayName("SearXNG adapter has unlimited rate limit (self-hosted)")
    void searxngAdapter_HasUnlimitedRateLimit() {
        assertThat(searxngAdapter.getRateLimit()).isEqualTo(Integer.MAX_VALUE); // Unlimited
    }

    @Test
    @DisplayName("Serper adapter has correct daily rate limit (60)")
    void serperAdapter_HasCorrectRateLimit() {
        assertThat(serperAdapter.getRateLimit()).isEqualTo(60); // 60/day conservative limit
    }

    @Test
    @DisplayName("Tavily adapter has correct daily rate limit (25)")
    void tavilyAdapter_HasCorrectRateLimit() {
        assertThat(tavilyAdapter.getRateLimit()).isEqualTo(25); // 25/day conservative limit
    }

    @Test
    @DisplayName("Adapters ordered by rate limit: SearXNG > Serper > Brave > Tavily")
    void adapters_OrderedByRateLimit() {
        // For weekly simulation, we should prioritize unlimited providers
        assertThat(searxngAdapter.getRateLimit()).isGreaterThan(serperAdapter.getRateLimit());
        assertThat(serperAdapter.getRateLimit()).isGreaterThan(braveAdapter.getRateLimit());
        assertThat(braveAdapter.getRateLimit()).isGreaterThan(tavilyAdapter.getRateLimit());
    }

    @Test
    @DisplayName("At least one adapter has unlimited rate limit (SearXNG)")
    void atLeastOneAdapter_HasUnlimitedRateLimit() {
        // Important for development and testing - we have at least one unlimited provider
        boolean hasUnlimited = searxngAdapter.getRateLimit() == Integer.MAX_VALUE ||
                               braveAdapter.getRateLimit() == Integer.MAX_VALUE ||
                               serperAdapter.getRateLimit() == Integer.MAX_VALUE ||
                               tavilyAdapter.getRateLimit() == Integer.MAX_VALUE;

        assertThat(hasUnlimited).isTrue();
    }
}
