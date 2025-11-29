package com.northstar.funding.crawler.integration;

import com.northstar.funding.crawler.adapter.BraveSearchAdapter;
import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.adapter.SerperAdapter;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import com.northstar.funding.crawler.orchestrator.MultiProviderSearchOrchestrator;
import com.northstar.funding.domain.SearchEngineType;
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
 * Integration test for Scenario 2: Multi-Provider Search.
 *
 * T040: Tests that orchestrator can coordinate multiple providers (Brave, SearXNG, Serper).
 * Simplified version testing Spring context and bean configuration without external services.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@DisplayName("Integration Test - Scenario 2: Multi-Provider Search")
class MultiProviderSearchTest {

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

        // Configure all providers (values don't matter for smoke tests)
        registry.add("search.providers.brave-search.api-key", () -> "test-key");
        registry.add("search.providers.brave-search.timeout", () -> "5000");
        registry.add("search.providers.brave-search.max-results", () -> "20");

        registry.add("search.providers.searxng.base-url", () -> "http://192.168.1.10:8080");
        registry.add("search.providers.searxng.timeout", () -> "5000");
        registry.add("search.providers.searxng.max-results", () -> "20");

        registry.add("search.providers.serper.api-key", () -> "test-key");
        registry.add("search.providers.serper.timeout", () -> "5000");
        registry.add("search.providers.serper.max-results", () -> "20");
    }

    @Autowired
    private MultiProviderSearchOrchestrator orchestrator;

    @Autowired
    private BraveSearchAdapter braveAdapter;

    @Autowired
    private SearxngAdapter searxngAdapter;

    @Autowired
    private SerperAdapter serperAdapter;

    @Autowired
    private SearchProviderConfig config;

    @Test
    @DisplayName("Spring context loads with all adapter beans")
    void contextLoads_AllAdaptersPresent() {
        assertThat(orchestrator).isNotNull();
        assertThat(braveAdapter).isNotNull();
        assertThat(searxngAdapter).isNotNull();
        assertThat(serperAdapter).isNotNull();
    }

    @Test
    @DisplayName("All adapters return correct provider types")
    void allAdapters_HaveCorrectProviderTypes() {
        assertThat(braveAdapter.getProviderType()).isEqualTo(SearchEngineType.BRAVE);
        assertThat(searxngAdapter.getProviderType()).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(serperAdapter.getProviderType()).isEqualTo(SearchEngineType.SERPER);
    }

    @Test
    @DisplayName("Provider configuration loaded for all adapters")
    void providerConfiguration_LoadedForAllAdapters() {
        assertThat(config.getBraveSearch()).isNotNull();
        assertThat(config.getSearxng()).isNotNull();
        assertThat(config.getSerper()).isNotNull();

        // Verify basic configuration values (timeout is in milliseconds in config)
        // Test sets all providers to 5000ms for consistency
        assertThat(config.getBraveSearch().getTimeout()).isEqualTo(5000); // 5 seconds
        assertThat(config.getSearxng().getTimeout()).isEqualTo(5000); // 5 seconds (test override)
        assertThat(config.getSerper().getTimeout()).isEqualTo(5000); // 5 seconds
    }

    @Test
    @DisplayName("All adapters support keyword queries")
    void allAdapters_SupportKeywordQueries() {
        assertThat(braveAdapter.supportsKeywordQueries()).isTrue();
        assertThat(searxngAdapter.supportsKeywordQueries()).isTrue();
        assertThat(serperAdapter.supportsKeywordQueries()).isTrue();
    }

    @Test
    @DisplayName("Standard adapters don't support AI-optimized queries")
    void standardAdapters_DontSupportAIOptimizedQueries() {
        assertThat(braveAdapter.supportsAIOptimizedQueries()).isFalse();
        assertThat(searxngAdapter.supportsAIOptimizedQueries()).isFalse();
        assertThat(serperAdapter.supportsAIOptimizedQueries()).isFalse();
    }

    @Test
    @DisplayName("Rate limits configured correctly per provider")
    void rateLimits_ConfiguredCorrectly() {
        assertThat(braveAdapter.getRateLimit()).isEqualTo(50); // 50/day conservative limit
        assertThat(searxngAdapter.getRateLimit()).isEqualTo(Integer.MAX_VALUE); // Unlimited (self-hosted)
        assertThat(serperAdapter.getRateLimit()).isEqualTo(60); // 60/day conservative limit
    }
}
