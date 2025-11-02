package com.northstar.funding.crawler.integration;

import com.northstar.funding.crawler.adapter.BraveSearchAdapter;
import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.adapter.SerperAdapter;
import com.northstar.funding.crawler.adapter.TavilyAdapter;
import com.northstar.funding.crawler.orchestrator.MultiProviderSearchOrchestrator;
import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.SessionType;
import com.northstar.funding.persistence.service.DiscoverySessionService;
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
 * Integration test for Scenario 8: Weekly Simulation.
 *
 * T046: Smoke test for weekly discovery simulation infrastructure.
 * This test verifies all components needed for weekly automated discovery are present.
 * Actual weekly simulation would run 4 queries per day for 7 days (28 total queries).
 */
@SpringBootTest(classes = TestApplication.class)
@Testcontainers
@DisplayName("Integration Test - Scenario 8: Weekly Simulation")
class WeeklySimulationTest {

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

        // Configure all providers
        registry.add("search.providers.brave.api-key", () -> "test-key");
        registry.add("search.providers.searxng.base-url", () -> "http://192.168.1.10:8080");
        registry.add("search.providers.serper.api-key", () -> "test-key");
        registry.add("search.providers.tavily.api-key", () -> "test-key");
    }

    @Autowired
    private MultiProviderSearchOrchestrator orchestrator;

    @Autowired
    private DiscoverySessionService discoverySessionService;

    @Autowired
    private BraveSearchAdapter braveAdapter;

    @Autowired
    private SearxngAdapter searxngAdapter;

    @Autowired
    private SerperAdapter serperAdapter;

    @Autowired
    private TavilyAdapter tavilyAdapter;

    @Test
    @DisplayName("Spring context loads with all required components for weekly simulation")
    void contextLoads_AllWeeklySimulationComponents() {
        assertThat(orchestrator).isNotNull();
        assertThat(discoverySessionService).isNotNull();
        assertThat(braveAdapter).isNotNull();
        assertThat(searxngAdapter).isNotNull();
        assertThat(serperAdapter).isNotNull();
        assertThat(tavilyAdapter).isNotNull();
    }

    @Test
    @DisplayName("Discovery session can be created for scheduled runs")
    void discoverySession_CanBeCreatedForScheduled() {
        DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.SCHEDULED)
                .executedBy("weekly-scheduler")
                .build();

        DiscoverySession saved = discoverySessionService.createSession(session);

        assertThat(saved.getSessionId()).isNotNull();
        assertThat(saved.getSessionType()).isEqualTo(SessionType.SCHEDULED);
        assertThat(saved.getExecutedBy()).isEqualTo("weekly-scheduler");
    }

    @Test
    @DisplayName("Rate limits support daily searches for weekly simulation")
    void rateLimits_SupportWeeklySimulation() {
        // Weekly simulation: 4 queries/day × 7 days = 28 queries/week
        // Daily limits are conservative to avoid exhausting free tier quotas
        // With daily limits: 50, 60, 25 we need to rotate across providers

        // SearXNG: Unlimited (self-hosted) - primary provider
        assertThat(searxngAdapter.getRateLimit()).isEqualTo(Integer.MAX_VALUE);

        // Brave: 50/day (plenty for backup)
        assertThat(braveAdapter.getRateLimit()).isGreaterThan(4);

        // Serper: 60/day (plenty for backup)
        assertThat(serperAdapter.getRateLimit()).isGreaterThan(4);

        // Tavily: 25/day (plenty for backup)
        assertThat(tavilyAdapter.getRateLimit()).isGreaterThan(4);
    }

    @Test
    @DisplayName("All adapters ready for parallel execution in weekly simulation")
    void allAdapters_ReadyForParallelExecution() {
        // Weekly simulation uses CompletableFuture to run 4 providers in parallel
        // Verify all adapters are available

        assertThat(braveAdapter).isNotNull();
        assertThat(searxngAdapter).isNotNull();
        assertThat(serperAdapter).isNotNull();
        assertThat(tavilyAdapter).isNotNull();
    }

    @Test
    @DisplayName("Orchestrator ready for weekly automated discovery")
    void orchestrator_ReadyForWeeklyDiscovery() {
        // Verify orchestrator is properly configured
        // In production, this would be called by a scheduler (cron/Spring @Scheduled)
        assertThat(orchestrator).isNotNull();
    }

    @Test
    @DisplayName("Session service can track weekly discovery metrics")
    void sessionService_CanTrackWeeklyMetrics() {
        // Create session and verify we can track it
        DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.SCHEDULED)
                .executedBy("integration-test")
                .build();

        DiscoverySession saved = discoverySessionService.createSession(session);

        assertThat(saved.getSessionId()).isNotNull();
        assertThat(saved.getExecutedAt()).isNotNull();

        // In production, session would be updated with:
        // - totalRawResults
        // - uniqueDomainsFound
        // - spamResultsFiltered
        // - duplicatesRemoved
    }
}
