package com.northstar.funding.querygeneration.service;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.exception.QueryGenerationException;
import com.northstar.funding.querygeneration.model.QueryCacheKey;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.strategy.QueryGenerationStrategy;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contract test for QueryGenerationService interface.
 *
 * <p>Unit tests for QueryGenerationServiceImpl with mocked dependencies.
 */
class QueryGenerationServiceContractTest {

    private QueryGenerationService service;

    @Mock
    private QueryCacheService cacheService;

    @Mock
    private QueryGenerationStrategy mockStrategy;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Mock strategy for BRAVE engine
        when(mockStrategy.getSearchEngine()).thenReturn(SearchEngineType.BRAVE);
        when(mockStrategy.generateQueries(any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        List.of("query1", "query2", "query3", "query4", "query5")
                ));

        Map<SearchEngineType, QueryGenerationStrategy> strategies = Map.of(
                SearchEngineType.BRAVE, mockStrategy
        );

        service = new QueryGenerationServiceImpl(cacheService, strategies);

        // Use reflection to set @Value fields for unit testing
        setField(service, "maxQueriesLimit", 50);
        setField(service, "minQueriesLimit", 1);
        setField(service, "defaultQueries", 10);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void generateQueries_shouldReturnCompletableFuture() throws Exception {
        // Arrange
        when(cacheService.getFromCache(any(QueryCacheKey.class)))
                .thenReturn(Optional.empty());

        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        CompletableFuture<QueryGenerationResponse> future = service.generateQueries(request);

        // Assert
        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);

        QueryGenerationResponse response = future.get(5, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.getQueries()).hasSize(5);
    }

    @Test
    void generateQueries_shouldValidateRequest() {
        // Arrange
        QueryGenerationRequest invalidRequest = QueryGenerationRequest.builder()
                .searchEngine(null) // Invalid - null
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .sessionId(UUID.randomUUID())
                .build();

        // Act & Assert
        assertThatThrownBy(() -> service.generateQueries(invalidRequest).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .cause()
                .isInstanceOf(QueryGenerationException.class)
                .hasMessageContaining("Search engine is required");
    }

    @Test
    void generateQueries_shouldCheckCacheFirst() throws Exception {
        // Arrange
        List<String> cachedQueries = List.of("cached1", "cached2", "cached3", "cached4", "cached5");

        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .sessionId(UUID.randomUUID())
                .build();

        // First call - cache miss
        when(cacheService.getFromCache(any(QueryCacheKey.class)))
                .thenReturn(Optional.empty());

        QueryGenerationResponse firstResponse = service.generateQueries(request).get(5, TimeUnit.SECONDS);
        assertThat(firstResponse.isFromCache()).isFalse();

        // Second call - cache hit
        when(cacheService.getFromCache(any(QueryCacheKey.class)))
                .thenReturn(Optional.of(cachedQueries));

        QueryGenerationResponse secondResponse = service.generateQueries(request).get(5, TimeUnit.SECONDS);
        assertThat(secondResponse.isFromCache()).isTrue();
        assertThat(secondResponse.getQueries()).isEqualTo(cachedQueries);
    }

    @Test
    void generateForMultipleProviders_shouldExecuteInParallel() throws Exception {
        // Arrange
        when(cacheService.getFromCache(any(QueryCacheKey.class)))
                .thenReturn(Optional.empty());

        // Mock strategies for all engines
        QueryGenerationStrategy serperStrategy = org.mockito.Mockito.mock(QueryGenerationStrategy.class);
        when(serperStrategy.getSearchEngine()).thenReturn(SearchEngineType.SERPER);
        when(serperStrategy.generateQueries(any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("serper1", "serper2")));

        QueryGenerationStrategy searxngStrategy = org.mockito.Mockito.mock(QueryGenerationStrategy.class);
        when(searxngStrategy.getSearchEngine()).thenReturn(SearchEngineType.SEARXNG);
        when(searxngStrategy.generateQueries(any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("searxng1", "searxng2")));

        QueryGenerationStrategy tavilyStrategy = org.mockito.Mockito.mock(QueryGenerationStrategy.class);
        when(tavilyStrategy.getSearchEngine()).thenReturn(SearchEngineType.TAVILY);
        when(tavilyStrategy.generateQueries(any(), any(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(List.of("tavily1", "tavily2")));

        Map<SearchEngineType, QueryGenerationStrategy> allStrategies = Map.of(
                SearchEngineType.BRAVE, mockStrategy,
                SearchEngineType.SERPER, serperStrategy,
                SearchEngineType.SEARXNG, searxngStrategy,
                SearchEngineType.TAVILY, tavilyStrategy
        );

        QueryGenerationService multiProviderService = new QueryGenerationServiceImpl(cacheService, allStrategies);

        // Use reflection to set @Value fields for unit testing
        setField(multiProviderService, "maxQueriesLimit", 50);
        setField(multiProviderService, "minQueriesLimit", 1);
        setField(multiProviderService, "defaultQueries", 10);

        // Act
        long startTime = System.currentTimeMillis();

        CompletableFuture<Map<SearchEngineType, List<String>>> future =
                multiProviderService.generateForMultipleProviders(
                        List.of(
                                SearchEngineType.BRAVE,
                                SearchEngineType.SERPER,
                                SearchEngineType.SEARXNG,
                                SearchEngineType.TAVILY
                        ),
                        Set.of(FundingSearchCategory.STEM_EDUCATION),
                        GeographicScope.BULGARIA,
                        5,
                        UUID.randomUUID()
                );

        Map<SearchEngineType, List<String>> results = future.get(10, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Assert
        assertThat(results).hasSize(4);
        assertThat(results).containsKeys(
                SearchEngineType.BRAVE,
                SearchEngineType.SERPER,
                SearchEngineType.SEARXNG,
                SearchEngineType.TAVILY
        );

        // Parallel execution should complete quickly (< 10 seconds)
        assertThat(duration).isLessThan(10000);
    }

    @Test
    void getCacheStatistics_shouldReturnMetrics() {
        // Arrange
        Map<String, Object> mockStats = Map.of(
                "hitRate", 0.75,
                "hitCount", 15L,
                "missCount", 5L,
                "requestCount", 20L,
                "size", 10L
        );
        when(cacheService.getStatistics()).thenReturn(mockStats);

        // Act
        Map<String, Object> stats = service.getCacheStatistics();

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats).containsKeys("hitRate", "hitCount", "missCount");
        assertThat(stats.get("hitRate")).isEqualTo(0.75);
    }

    @Test
    void clearCache_shouldResetCacheAndStatistics() {
        // Act
        service.clearCache();

        // Assert
        verify(cacheService).clearCache();
    }
}
