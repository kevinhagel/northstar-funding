package com.northstar.funding.crawler.integration;

import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SessionType;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for Scenario 1: Single Provider Search (SearXNG).
 *
 * Simplified version that tests adapter integration without requiring external SearXNG service.
 * Tests verify:
 * - Spring context loads correctly
 * - DiscoverySession creation works with TestContainers PostgreSQL
 * - Adapter beans are configured properly
 * - Database integration works end-to-end
 *
 * NOTE: Actual search execution requires SearXNG at 192.168.1.10:8080
 * Those tests are marked @Disabled and can be run manually.
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@DisplayName("Integration Test - Scenario 1: Single Provider Search")
class SingleProviderSearchTest {

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

        // Configure SearXNG for tests
        registry.add("search.providers.searxng.base-url", () -> "http://192.168.1.10:8080");
        registry.add("search.providers.searxng.timeout", () -> "5000");
        registry.add("search.providers.searxng.max-results", () -> "20");
    }

    @Autowired
    private SearxngAdapter searxngAdapter;

    @Autowired
    private DiscoverySessionService discoverySessionService;

    @Autowired
    private SearchProviderConfig config;

    @Test
    @DisplayName("Spring context loads with all required beans")
    void contextLoads() {
        // Verify all required beans are loaded
        assertThat(searxngAdapter).isNotNull();
        assertThat(discoverySessionService).isNotNull();
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("DiscoverySession can be created and persisted with TestContainers")
    void discoverySession_CreateAndPersist_Success() {
        // Given: Build discovery session
        DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.MANUAL)
                .executedBy("integration-test-user")
                .build();

        // When: Save to database
        DiscoverySession saved = discoverySessionService.createSession(session);

        // Then: Verify persistence
        assertThat(saved.getSessionId()).isNotNull();
        assertThat(saved.getSessionType()).isEqualTo(SessionType.MANUAL);
        assertThat(saved.getExecutedBy()).isEqualTo("integration-test-user");
        assertThat(saved.getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("SearXNG adapter configuration loaded correctly")
    void searxngAdapter_Configuration_LoadedCorrectly() {
        // Then: Verify configuration
        assertThat(config.getSearxng()).isNotNull();
        assertThat(config.getSearxng().getBaseUrl()).isEqualTo("http://192.168.1.10:8080");
        assertThat(config.getSearxng().getTimeout()).isEqualTo(5000); // 5 seconds in milliseconds
        assertThat(config.getSearxng().getMaxResults()).isEqualTo(20);
    }

    @Test
    @DisplayName("SearXNG adapter has unlimited rate limit")
    void searxngAdapter_RateLimit_Unlimited() {
        // When: Check rate limit
        int rateLimit = searxngAdapter.getRateLimit();

        // Then: SearXNG is self-hosted, should have unlimited rate limit
        assertThat(rateLimit).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("SearXNG adapter supports keyword queries only")
    void searxngAdapter_QuerySupport_KeywordOnly() {
        // Then: Verify query type support
        assertThat(searxngAdapter.supportsKeywordQueries()).isTrue();
        assertThat(searxngAdapter.supportsAIOptimizedQueries()).isFalse();
    }

    @Test
    @DisplayName("SearXNG adapter returns correct provider type")
    void searxngAdapter_ProviderType_Correct() {
        // Then: Verify provider type
        assertThat(searxngAdapter.getProviderType()).isEqualTo(SearchEngineType.SEARXNG);
    }
}
