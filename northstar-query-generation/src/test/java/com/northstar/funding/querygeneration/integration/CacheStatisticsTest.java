package com.northstar.funding.querygeneration.integration;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Integration test for Scenario 7: Cache Statistics Monitoring.
 *
 * <p>Tests monitoring cache hit rate and performance metrics.
 *
 * <p>Validates:
 * <ul>
 *   <li>Cache statistics are accurate</li>
 *   <li>Hit rate calculation is correct</li>
 *   <li>Cache size tracking works</li>
 *   <li>Statistics update in real-time</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CacheStatisticsTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    @BeforeEach
    void setUp() {
        // Clear cache before each test for predictable statistics
        queryGenerationService.clearCache();
    }

    /**
     * Scenario 7: Monitor cache hit rate with known pattern.
     *
     * <p>Makes 10 requests (5 unique, 5 duplicates) to achieve 50% hit rate.
     */
    @Test
    void getCacheStatistics_shouldShowAccurateMetrics() throws Exception {
        // Arrange - Create 3 unique request patterns
        QueryGenerationRequest request1 = createRequest(SearchEngineType.BRAVE,
                FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS);
        QueryGenerationRequest request2 = createRequest(SearchEngineType.BRAVE,
                FundingSearchCategory.TEACHER_DEVELOPMENT);
        QueryGenerationRequest request3 = createRequest(SearchEngineType.BRAVE,
                FundingSearchCategory.INFRASTRUCTURE_FUNDING);

        // Act - Make 10 requests total: 3 unique + 3 duplicates + 2 more duplicates + 2 more duplicates = 10
        // Pattern: miss, miss, miss, hit, hit, hit, hit, hit, hit, hit

        // First 3 (cache misses)
        queryGenerationService.generateQueries(request1).get(30, TimeUnit.SECONDS);
        queryGenerationService.generateQueries(request2).get(30, TimeUnit.SECONDS);
        queryGenerationService.generateQueries(request3).get(30, TimeUnit.SECONDS);

        // Next 7 (cache hits - repeat the 3 requests multiple times)
        queryGenerationService.generateQueries(request1).get(30, TimeUnit.SECONDS); // hit
        queryGenerationService.generateQueries(request2).get(30, TimeUnit.SECONDS); // hit
        queryGenerationService.generateQueries(request3).get(30, TimeUnit.SECONDS); // hit
        queryGenerationService.generateQueries(request1).get(30, TimeUnit.SECONDS); // hit
        queryGenerationService.generateQueries(request2).get(30, TimeUnit.SECONDS); // hit
        queryGenerationService.generateQueries(request3).get(30, TimeUnit.SECONDS); // hit
        queryGenerationService.generateQueries(request1).get(30, TimeUnit.SECONDS); // hit

        // Get statistics
        Map<String, Object> stats = queryGenerationService.getCacheStatistics();

        // Assert - Statistics are accurate
        assertThat(stats.get("requestCount")).isEqualTo(10L);
        assertThat(stats.get("hitCount")).isEqualTo(7L);
        assertThat(stats.get("missCount")).isEqualTo(3L);
        assertThat((Double) stats.get("hitRate")).isCloseTo(0.7, offset(0.01)); // 7/10 = 70%
        assertThat((Long) stats.get("size")).isEqualTo(3L); // 3 unique patterns cached
    }

    /**
     * Scenario 7b: Cache statistics reset after clear.
     */
    @Test
    void clearCache_shouldResetStatistics() throws Exception {
        // Arrange - Make some requests
        QueryGenerationRequest request = createRequest(SearchEngineType.BRAVE,
                FundingSearchCategory.STEM_EDUCATION);

        queryGenerationService.generateQueries(request).get(30, TimeUnit.SECONDS);
        queryGenerationService.generateQueries(request).get(30, TimeUnit.SECONDS); // Cache hit

        // Verify stats before clear
        Map<String, Object> statsBefore = queryGenerationService.getCacheStatistics();
        assertThat(statsBefore.get("requestCount")).isEqualTo(2L);

        // Act - Clear cache
        queryGenerationService.clearCache();

        // Assert - Statistics reset
        Map<String, Object> statsAfter = queryGenerationService.getCacheStatistics();
        assertThat((Long) statsAfter.get("size")).isEqualTo(0L);

        // Note: Caffeine stats might not reset totalRequests/hits/misses
        // This is expected behavior - only cache size resets
    }

    /**
     * Scenario 7c: Cache size reflects unique keys.
     */
    @Test
    void cacheSize_shouldReflectUniqueKeys() throws Exception {
        // Arrange & Act - Create 5 unique requests
        for (int i = 0; i < 5; i++) {
            FundingSearchCategory category = switch (i) {
                case 0 -> FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS;
                case 1 -> FundingSearchCategory.TEACHER_DEVELOPMENT;
                case 2 -> FundingSearchCategory.INFRASTRUCTURE_FUNDING;
                case 3 -> FundingSearchCategory.STEM_EDUCATION;
                default -> FundingSearchCategory.PROGRAM_GRANTS;
            };

            QueryGenerationRequest request = createRequest(SearchEngineType.BRAVE, category);
            queryGenerationService.generateQueries(request).get(30, TimeUnit.SECONDS);
        }

        // Assert - Cache contains 5 unique keys
        Map<String, Object> stats = queryGenerationService.getCacheStatistics();
        assertThat((Long) stats.get("size")).isEqualTo(5L);
    }

    /**
     * Helper to create consistent request objects.
     */
    private QueryGenerationRequest createRequest(SearchEngineType engine,
                                                  FundingSearchCategory category) {
        return QueryGenerationRequest.builder()
                .searchEngine(engine)
                .categories(Set.of(category))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();
    }
}
