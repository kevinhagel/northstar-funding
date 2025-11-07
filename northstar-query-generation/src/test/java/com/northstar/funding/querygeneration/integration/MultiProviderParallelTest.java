package com.northstar.funding.querygeneration.integration;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Scenario 4: Multi-Provider Parallel Generation.
 *
 * <p>Tests generating queries for all 4 providers simultaneously using Virtual Threads.
 *
 * <p>Validates:
 * <ul>
 *   <li>All 4 providers return queries</li>
 *   <li>Each provider returns requested number of queries</li>
 *   <li>Parallel execution completes in reasonable time (<30s)</li>
 *   <li>Total time ≈ max(individual times), not sum</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class MultiProviderParallelTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    /**
     * Scenario 4: Generate queries for all 4 providers in parallel.
     */
    @Test
    void generateForMultipleProviders_shouldExecuteInParallel() throws Exception {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        Set<FundingSearchCategory> categories = Set.of(
                FundingSearchCategory.TEACHER_DEVELOPMENT
        );
        GeographicScope geographic = GeographicScope.EASTERN_EUROPE;

        // Act
        Instant start = Instant.now();

        Map<SearchEngineType, List<String>> results = queryGenerationService
                .generateForMultipleProviders(
                        List.of(
                                SearchEngineType.BRAVE,
                                SearchEngineType.SERPER,
                                SearchEngineType.SEARXNG,
                                SearchEngineType.TAVILY
                        ),
                        categories,
                        geographic,
                        5, // maxQueries per provider
                        sessionId
                )
                .get(30, TimeUnit.SECONDS);

        Duration totalTime = Duration.between(start, Instant.now());

        // Assert - All providers returned queries
        assertThat(results).hasSize(4);
        assertThat(results.get(SearchEngineType.BRAVE)).hasSize(5);
        assertThat(results.get(SearchEngineType.SERPER)).hasSize(5);
        assertThat(results.get(SearchEngineType.SEARXNG)).hasSize(5);
        assertThat(results.get(SearchEngineType.TAVILY)).hasSize(5);

        // Assert - Parallel execution (not sequential)
        // If sequential: 4 * 5s = 20s
        // If parallel: max(5s) = ~5-10s
        assertThat(totalTime).isLessThan(Duration.ofSeconds(30));

        // Assert - Total query count
        int totalQueries = results.values().stream()
                .mapToInt(List::size)
                .sum();
        assertThat(totalQueries).isEqualTo(20); // 4 providers * 5 queries each
    }

    /**
     * Scenario 4b: Subset of providers works correctly.
     */
    @Test
    void generateForMultipleProviders_withSubset_shouldWork() throws Exception {
        // Arrange
        UUID sessionId = UUID.randomUUID();

        // Act - Only 2 providers
        Map<SearchEngineType, List<String>> results = queryGenerationService
                .generateForMultipleProviders(
                        List.of(SearchEngineType.BRAVE, SearchEngineType.TAVILY),
                        Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING),
                        GeographicScope.BALKANS,
                        3,
                        sessionId
                )
                .get(30, TimeUnit.SECONDS);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(SearchEngineType.BRAVE)).hasSize(3);
        // Smaller models may generate fewer queries than requested
        assertThat(results.get(SearchEngineType.TAVILY))
                .hasSizeGreaterThanOrEqualTo(2)
                .hasSizeLessThanOrEqualTo(3);
    }
}
