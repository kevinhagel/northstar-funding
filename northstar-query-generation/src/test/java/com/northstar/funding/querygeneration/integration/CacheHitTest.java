package com.northstar.funding.querygeneration.integration;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Scenario 2: Cache Hit Behavior.
 *
 * <p>Tests that identical requests within 24 hours return cached queries.
 *
 * <p>Validates:
 * <ul>
 *   <li>First request generates fresh queries (fromCache=false)</li>
 *   <li>Second identical request returns cached queries (fromCache=true)</li>
 *   <li>Cached queries are identical to first response</li>
 *   <li>Cache retrieval is fast (<50ms)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheHitTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    @BeforeEach
    void setUp() {
        // Clear cache before each test to ensure clean state
        queryGenerationService.clearCache();
    }

    /**
     * Scenario 2: Identical request within 24 hours returns cached queries.
     *
     * <p>First call: Generates fresh queries from LM Studio
     * <p>Second call: Returns cached queries without calling LM Studio
     */
    @Test
    void generateQueries_whenCalledTwiceWithSameRequest_shouldReturnCachedQueriesOnSecondCall() throws Exception {
        // Arrange
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .sessionId(UUID.randomUUID())
                .build();

        // Act - First request
        QueryGenerationResponse firstResponse = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Wait briefly to ensure cache is populated
        Thread.sleep(100);

        // Act - Second identical request
        Instant beforeSecondCall = Instant.now();
        QueryGenerationResponse secondResponse = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);
        Instant afterSecondCall = Instant.now();

        // Assert - First response
        assertThat(firstResponse.isFromCache()).isFalse();
        assertThat(firstResponse.getQueries()).hasSize(5);

        // Assert - Second response is from cache
        assertThat(secondResponse.isFromCache()).isTrue();
        assertThat(secondResponse.getQueries()).isEqualTo(firstResponse.getQueries());

        // Assert - Cache retrieval is fast (<50ms)
        Duration retrievalTime = Duration.between(beforeSecondCall, afterSecondCall);
        assertThat(retrievalTime).isLessThan(Duration.ofMillis(50));
    }

    /**
     * Scenario 2b: Different requests are not cached together.
     *
     * <p>Validates that cache key differentiation works correctly.
     */
    @Test
    void generateQueries_withDifferentCategories_shouldNotShareCache() throws Exception {
        // Arrange - Request 1
        QueryGenerationRequest request1 = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Arrange - Request 2 (different category)
        QueryGenerationRequest request2 = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.TEACHER_DEVELOPMENT))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response1 = queryGenerationService
                .generateQueries(request1)
                .get(30, TimeUnit.SECONDS);

        QueryGenerationResponse response2 = queryGenerationService
                .generateQueries(request2)
                .get(30, TimeUnit.SECONDS);

        // Assert - Both are fresh (not cached from each other)
        assertThat(response1.isFromCache()).isFalse();
        assertThat(response2.isFromCache()).isFalse();
        assertThat(response1.getQueries()).isNotEqualTo(response2.getQueries());
    }

    /**
     * Scenario 2c: Different maxQueries creates different cache keys.
     */
    @Test
    void generateQueries_withDifferentMaxQueries_shouldNotShareCache() throws Exception {
        // Arrange - Request with maxQueries=3
        QueryGenerationRequest request3 = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BALKANS)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Arrange - Same request but maxQueries=5
        QueryGenerationRequest request5 = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BALKANS)
                .maxQueries(5)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response3 = queryGenerationService
                .generateQueries(request3)
                .get(30, TimeUnit.SECONDS);

        QueryGenerationResponse response5 = queryGenerationService
                .generateQueries(request5)
                .get(30, TimeUnit.SECONDS);

        // Assert - Different cache keys, both fresh
        assertThat(response3.isFromCache()).isFalse();
        assertThat(response5.isFromCache()).isFalse();
        assertThat(response3.getQueries()).hasSize(3);
        assertThat(response5.getQueries()).hasSize(5);
    }
}
