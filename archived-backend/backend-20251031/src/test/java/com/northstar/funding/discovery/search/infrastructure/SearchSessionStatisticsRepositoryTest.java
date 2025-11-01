package com.northstar.funding.discovery.search.infrastructure;

import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.domain.SessionType;
import com.northstar.funding.discovery.infrastructure.DiscoverySessionRepository;
import java.util.Set;
import com.northstar.funding.discovery.search.domain.SearchEngineType;
import com.northstar.funding.discovery.search.domain.SearchSessionStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for SearchSessionStatisticsRepository (Feature 003)
 *
 * Uses @Testcontainers with PostgreSQL for realistic testing
 * Tests per-session statistics, engine-specific queries, time-range analytics
 *
 * Constitutional Compliance:
 * - TestContainers for isolated PostgreSQL testing
 * - Spring Data JDBC repository testing
 * - Analytics query validation
 */
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SearchSessionStatisticsRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SearchSessionStatisticsRepository repository;

    @Autowired
    private DiscoverySessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        sessionRepository.deleteAll();
    }

    private DiscoverySession createDiscoverySession() {
        var now = LocalDateTime.now();
        return sessionRepository.save(DiscoverySession.builder()
            .status(SessionStatus.RUNNING)
            .sessionType(SessionType.SCHEDULED)
            .executedAt(now)
            .executedBy("TEST")
            .startedAt(now)
            .searchEnginesUsed(Set.of())
            .searchQueries(List.of())
            .candidatesFound(0)
            .duplicatesDetected(0)
            .sourcesScraped(0)
            .build());
    }

    @Test
    void testSaveAndFindById() {
        // Given
        var session = createDiscoverySession();
        var stats = SearchSessionStatistics.builder()
            .sessionId(session.getSessionId())
            .engineType(SearchEngineType.SEARXNG)
            .queriesExecuted(10)
            .resultsReturned(250)
            .avgResponseTimeMs(2500L)
            .failureCount(1)
            .createdAt(Instant.now())
            .build();

        // When
        var saved = repository.save(stats);

        // Then
        assertThat(saved.getId()).isNotNull();

        var found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSessionId()).isEqualTo(session.getSessionId());
        assertThat(found.get().getEngineType()).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(found.get().getQueriesExecuted()).isEqualTo(10);
        assertThat(found.get().getResultsReturned()).isEqualTo(250);
    }

    @Test
    void testFindBySessionId() {
        // Given
        var session1 = createDiscoverySession();
        var session2 = createDiscoverySession();
        repository.save(createStats(session1.getSessionId(), SearchEngineType.SEARXNG, 10, 250));
        repository.save(createStats(session1.getSessionId(), SearchEngineType.TAVILY, 10, 200));
        repository.save(createStats(session2.getSessionId(), SearchEngineType.PERPLEXITY, 5, 100));

        // When
        var stats = repository.findBySessionId(session1.getSessionId());

        // Then
        assertThat(stats).hasSize(2);
        assertThat(stats).allMatch(s -> s.getSessionId().equals(session1.getSessionId()));
    }

    @Test
    void testFindByEngineType() {
        // Given
        var session1 = createDiscoverySession();
        var session2 = createDiscoverySession();
        var session3 = createDiscoverySession();
        repository.save(createStats(session1.getSessionId(), SearchEngineType.TAVILY, 10, 200));
        repository.save(createStats(session2.getSessionId(), SearchEngineType.TAVILY, 15, 300));
        repository.save(createStats(session3.getSessionId(), SearchEngineType.SEARXNG, 10, 250));

        // When
        var tavilyStats = repository.findByEngineType(SearchEngineType.TAVILY);

        // Then
        assertThat(tavilyStats).hasSize(2);
        assertThat(tavilyStats).allMatch(s -> s.getEngineType() == SearchEngineType.TAVILY);
    }

    @Test
    void testFindByCreatedAtBetween() {
        // Given
        var now = Instant.now();
        var yesterday = now.minus(1, ChronoUnit.DAYS);
        var twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        var session1 = createDiscoverySession();
        var session2 = createDiscoverySession();
        var session3 = createDiscoverySession();

        var stats1 = createStats(session1.getSessionId(), SearchEngineType.SEARXNG, 10, 250);
        stats1.setCreatedAt(yesterday);
        repository.save(stats1);

        var stats2 = createStats(session2.getSessionId(), SearchEngineType.TAVILY, 10, 200);
        stats2.setCreatedAt(now);
        repository.save(stats2);

        var stats3 = createStats(session3.getSessionId(), SearchEngineType.PERPLEXITY, 5, 100);
        stats3.setCreatedAt(twoDaysAgo);
        repository.save(stats3);

        // When
        var recentStats = repository.findByCreatedAtBetween(
            yesterday.minus(1, ChronoUnit.HOURS),
            now.plus(1, ChronoUnit.HOURS)
        );

        // Then
        assertThat(recentStats).hasSize(2);
    }

    @Test
    void testCountSessionsByEngineType() {
        // Given
        var session1 = createDiscoverySession();
        var session2 = createDiscoverySession();
        repository.save(createStats(session1.getSessionId(), SearchEngineType.TAVILY, 10, 200));
        repository.save(createStats(session2.getSessionId(), SearchEngineType.TAVILY, 15, 300));
        repository.save(createStats(session1.getSessionId(), SearchEngineType.SEARXNG, 10, 250));

        // When
        var count = repository.countSessionsByEngineType(SearchEngineType.TAVILY);

        // Then
        assertThat(count).isEqualTo(2); // Two distinct sessions used Tavily
    }

    @Test
    void testFindByFailureRateGreaterThan() {
        // Given - Create stats with different failure rates
        var session1 = createDiscoverySession();
        var session2 = createDiscoverySession();
        var session3 = createDiscoverySession();
        // 20% failure rate (2/10)
        repository.save(createStatsWithFailures(session1.getSessionId(), SearchEngineType.SEARXNG, 10, 250, 2));
        // 50% failure rate (5/10)
        repository.save(createStatsWithFailures(session2.getSessionId(), SearchEngineType.TAVILY, 10, 200, 5));
        // 0% failure rate
        repository.save(createStatsWithFailures(session3.getSessionId(), SearchEngineType.PERPLEXITY, 10, 100, 0));

        // When - Find stats with >15% failure rate
        var highFailureStats = repository.findByFailureRateGreaterThan(0.15);

        // Then
        assertThat(highFailureStats).hasSize(2);
    }

    // Helper methods
    private SearchSessionStatistics createStats(UUID sessionId, SearchEngineType engineType,
                                                 int queries, int results) {
        return SearchSessionStatistics.builder()
            .sessionId(sessionId)
            .engineType(engineType)
            .queriesExecuted(queries)
            .resultsReturned(results)
            .avgResponseTimeMs(2500L)
            .failureCount(0)
            .createdAt(Instant.now())
            .build();
    }

    private SearchSessionStatistics createStatsWithFailures(UUID sessionId, SearchEngineType engineType,
                                                            int queries, int results, int failures) {
        return SearchSessionStatistics.builder()
            .sessionId(sessionId)
            .engineType(engineType)
            .queriesExecuted(queries)
            .resultsReturned(results)
            .avgResponseTimeMs(2500L)
            .failureCount(failures)
            .createdAt(Instant.now())
            .build();
    }
}
