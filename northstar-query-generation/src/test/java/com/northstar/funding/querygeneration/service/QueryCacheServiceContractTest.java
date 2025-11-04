package com.northstar.funding.querygeneration.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.persistence.repository.SearchQueryRepository;
import com.northstar.funding.querygeneration.model.QueryCacheKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract test for QueryCacheService interface.
 *
 * <p>Unit tests for QueryCacheServiceImpl using real Caffeine cache and mocked repository.
 */
class QueryCacheServiceContractTest {

    private QueryCacheService service;
    private Cache<QueryCacheKey, List<String>> cache;

    @Mock
    private SearchQueryRepository searchQueryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create real Caffeine cache for testing
        cache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();

        service = new QueryCacheServiceImpl(cache, searchQueryRepository);
    }

    @Test
    void getFromCache_whenEmpty_shouldReturnEmptyOptional() {
        // Arrange
        QueryCacheKey key = QueryCacheKey.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .build();

        // Act
        Optional<List<String>> result = service.getFromCache(key);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void cacheQueries_shouldStoreInCache() {
        // Arrange
        QueryCacheKey key = QueryCacheKey.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .build();

        List<String> queries = List.of("query1", "query2", "query3");

        // Act
        service.cacheQueries(key, queries);

        // Assert
        Optional<List<String>> cached = service.getFromCache(key);
        assertThat(cached).isPresent();
        assertThat(cached.get()).isEqualTo(queries);
    }

    @Test
    void getFromCache_shouldCompleteQuickly() {
        // Arrange
        QueryCacheKey key = QueryCacheKey.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .build();

        service.cacheQueries(key, List.of("query1", "query2"));

        // Act
        long startTime = System.currentTimeMillis();
        service.getFromCache(key);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(duration).isLessThan(50); // <50ms contract
    }

    @Test
    void persistQueries_shouldReturnCompletableFuture() {
        // Arrange
        QueryCacheKey key = QueryCacheKey.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .build();

        // Act
        CompletableFuture<Void> future = service.persistQueries(
                key,
                List.of("query1", "query2"),
                UUID.randomUUID()
        );

        // Assert
        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    void getStatistics_shouldReturnCacheMetrics() {
        // Act
        Map<String, Object> stats = service.getStatistics();

        // Assert
        assertThat(stats).containsKeys(
                "hitRate",
                "hitCount",
                "missCount",
                "requestCount",
                "evictionCount",
                "size"
        );
        assertThat(stats.get("hitRate")).isInstanceOf(Double.class);
    }

    @Test
    void clearCache_shouldBeIdempotent() {
        // Arrange
        QueryCacheKey key = QueryCacheKey.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .build();

        service.cacheQueries(key, List.of("query1", "query2"));

        // Act
        service.clearCache();
        service.clearCache(); // Should not throw exception

        // Assert
        Map<String, Object> stats = service.getStatistics();
        assertThat(stats.get("size")).isEqualTo(0L);
    }
}
