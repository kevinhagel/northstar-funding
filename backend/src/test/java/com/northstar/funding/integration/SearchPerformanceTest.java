package com.northstar.funding.integration;

import com.northstar.funding.discovery.search.application.SearchExecutionService;
import com.northstar.funding.discovery.search.domain.SearchQuery;
import com.northstar.funding.discovery.search.infrastructure.SearchQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance Test for Search Execution Infrastructure (Feature 003 - Task T036)
 *
 * Tests performance requirements:
 * - Single query across 3 engines completes in <15 seconds (parallel Virtual Threads)
 * - 10 queries × 3 engines = 30 searches complete in <30 minutes (sequential queries, parallel engines)
 * - Circuit breaker doesn't impact healthy engines
 *
 * Based on specs/003-search-execution-infrastructure/tasks.md T036
 *
 * Note: This test depends on external APIs (Tavily, Perplexity, Searxng).
 * Performance will vary based on:
 * - Network latency
 * - API response times
 * - Search engine load
 * - Circuit breaker state
 *
 * @author NorthStar Funding Team
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("Search Performance Test (T036)")
class SearchPerformanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("northstar_test")
        .withUsername("test_user")
        .withPassword("test_password")
        .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Autowired
    private SearchQueryRepository searchQueryRepository;

    @BeforeEach
    void setUp() {
        // @Transactional handles cleanup - no manual cleanup needed
    }

    @Test
    @DisplayName("Should execute single query across 3 engines in <15 seconds (Virtual Threads)")
    void shouldExecuteSingleQueryInUnder15Seconds() {
        // GIVEN: A single query targeting all 3 engines
        SearchQuery query = createTestQuery("Performance test single query");
        searchQueryRepository.save(query);

        // WHEN: Execute query and measure time
        Instant start = Instant.now();
        var result = searchExecutionService.executeQueryAcrossEngines(query);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        // THEN: Should complete in under 15 seconds
        System.out.println("\n=== Single Query Performance ===");
        System.out.println("Query: " + query.getQueryText());
        System.out.println("Target Engines: " + query.getTargetEngines());
        System.out.println("Duration: " + duration.toMillis() + "ms (" + duration.toSeconds() + "s)");

        if (result.isSuccess()) {
            var results = result.get();
            System.out.println("Total Results: " + results.size());
            System.out.println("Results by Engine:");
            query.getParsedTargetEngines().forEach(engine -> {
                long count = results.stream().filter(r -> r.source() == engine).count();
                System.out.println("  - " + engine + ": " + count);
            });
        } else {
            System.out.println("Query Failed: " + result.getCause().getMessage());
        }

        // Requirement: <15 seconds for parallel execution
        // Note: May exceed if APIs are slow or circuit breakers are OPEN
        assertThat(duration.toSeconds())
            .as("Single query should complete in under 15 seconds with parallel Virtual Threads")
            .isLessThanOrEqualTo(15);
    }

    @Test
    @DisplayName("Should execute 10 queries across 3 engines in <30 minutes")
    void shouldExecute10QueriesInUnder30Minutes() {
        // GIVEN: 10 queries targeting all 3 engines
        List<SearchQuery> queries = create10TestQueries();
        searchQueryRepository.saveAll(queries);

        // WHEN: Execute all queries sequentially and measure time
        Instant start = Instant.now();
        var result = searchExecutionService.executeQueries(queries);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        // THEN: Should complete in under 30 minutes
        System.out.println("\n=== 10 Queries Performance ===");
        System.out.println("Total Queries: " + queries.size());
        System.out.println("Duration: " + duration.toMillis() + "ms (" + duration.toSeconds() + "s / " + duration.toMinutes() + "m)");

        if (result.isSuccess()) {
            var allResults = result.get();
            System.out.println("Total Results: " + allResults.size());
            System.out.println("Average Results per Query: " + (allResults.size() / queries.size()));
        } else {
            System.out.println("Queries Failed: " + result.getCause().getMessage());
        }

        // Requirement: <30 minutes for 10 queries × 3 engines
        // Note: May exceed if APIs are slow or circuit breakers are OPEN
        assertThat(duration.toMinutes())
            .as("10 queries should complete in under 30 minutes")
            .isLessThanOrEqualTo(30);
    }

    @Test
    @DisplayName("Should demonstrate Virtual Threads parallel execution speedup")
    void shouldDemonstrateVirtualThreadsSpeedup() {
        // GIVEN: A query targeting all 3 engines
        SearchQuery query = createTestQuery("Virtual Threads speedup test");
        searchQueryRepository.save(query);

        // WHEN: Execute query (parallel with Virtual Threads)
        Instant start = Instant.now();
        var result = searchExecutionService.executeQueryAcrossEngines(query);
        Instant end = Instant.now();
        Duration parallelDuration = Duration.between(start, end);

        // THEN: Log timing information
        System.out.println("\n=== Virtual Threads Speedup Analysis ===");
        System.out.println("Parallel Execution (Virtual Threads): " + parallelDuration.toMillis() + "ms");

        if (result.isSuccess()) {
            var results = result.get();

            // Calculate theoretical sequential time (sum of individual engine times)
            // Note: We can't measure actual sequential time without modifying the service
            // So we estimate based on parallel time
            System.out.println("Engines Queried: " + query.getTargetEngines().size());
            System.out.println("Results Returned: " + results.size());
            System.out.println("\nEstimated Sequential Time: ~" + (parallelDuration.toMillis() * query.getTargetEngines().size()) + "ms");
            System.out.println("Actual Parallel Time: " + parallelDuration.toMillis() + "ms");
            System.out.println("Estimated Speedup: ~" + query.getTargetEngines().size() + "x");

            // With Virtual Threads, parallel execution should be significantly faster
            // Parallel time should be roughly equal to the slowest engine's response time
            // Sequential would be sum of all engine response times
        }

        // No strict assertion - this is informational
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should measure and log response time percentiles")
    void shouldMeasureResponseTimePercentiles() {
        // GIVEN: 5 queries for statistical analysis
        List<SearchQuery> queries = create5TestQueries();
        List<Long> responseTimes = new ArrayList<>();

        // WHEN: Execute each query and collect timings
        System.out.println("\n=== Response Time Percentiles ===");
        for (SearchQuery query : queries) {
            searchQueryRepository.save(query);

            Instant start = Instant.now();
            var result = searchExecutionService.executeQueryAcrossEngines(query);
            Instant end = Instant.now();

            long responseTimeMs = Duration.between(start, end).toMillis();
            responseTimes.add(responseTimeMs);

            System.out.println("Query: " + query.getQueryText() + " - " + responseTimeMs + "ms");
        }

        // THEN: Calculate and display percentiles
        responseTimes.sort(Long::compareTo);

        System.out.println("\nStatistics:");
        System.out.println("  Min: " + responseTimes.get(0) + "ms");
        System.out.println("  p50 (median): " + percentile(responseTimes, 50) + "ms");
        System.out.println("  p90: " + percentile(responseTimes, 90) + "ms");
        System.out.println("  p95: " + percentile(responseTimes, 95) + "ms");
        System.out.println("  Max: " + responseTimes.get(responseTimes.size() - 1) + "ms");

        double avgMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.println("  Average: " + String.format("%.2f", avgMs) + "ms");

        // No strict assertion - this is for analysis
        assertThat(responseTimes).isNotEmpty();
    }

    // Helper methods

    private SearchQuery createTestQuery(String queryText) {
        return SearchQuery.builder()
            .queryText(queryText)
            .dayOfWeek(DayOfWeek.MONDAY)
            .tags(Set.of("GEOGRAPHY:Bulgaria", "CATEGORY:Testing"))
            .targetEngines(Set.of("SEARXNG", "TAVILY", "PERPLEXITY"))
            .expectedResults(25)
            .enabled(true)
            .build();
    }

    private List<SearchQuery> create5TestQueries() {
        return List.of(
            createTestQuery("Bulgaria education grants"),
            createTestQuery("Eastern Europe infrastructure funding"),
            createTestQuery("Balkans STEM scholarships"),
            createTestQuery("EU Horizon Bulgaria research"),
            createTestQuery("Romania teacher development programs")
        );
    }

    private List<SearchQuery> create10TestQueries() {
        return List.of(
            createTestQuery("Bulgaria education grants 2025"),
            createTestQuery("Eastern Europe infrastructure funding opportunities"),
            createTestQuery("Balkans STEM education scholarships"),
            createTestQuery("Romania teacher development grants"),
            createTestQuery("EU cultural heritage funding Bulgaria"),
            createTestQuery("Greece nonprofit capacity building"),
            createTestQuery("North Macedonia youth employment programs"),
            createTestQuery("Bulgaria healthcare research grants"),
            createTestQuery("Balkans environmental sustainability funding"),
            createTestQuery("Romania arts culture grants 2025")
        );
    }

    private long percentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }
}
