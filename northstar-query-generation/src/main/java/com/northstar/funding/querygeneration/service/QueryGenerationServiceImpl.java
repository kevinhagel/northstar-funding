package com.northstar.funding.querygeneration.service;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.exception.QueryGenerationException;
import com.northstar.funding.querygeneration.model.QueryCacheKey;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.strategy.SearchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service for AI-powered query generation.
 *
 * <p>Orchestrates:
 * <ul>
 *   <li>Cache checks via QueryCacheService</li>
 *   <li>Strategy selection based on search engine type</li>
 *   <li>Async query generation via Virtual Threads</li>
 *   <li>Optional PostgreSQL persistence</li>
 * </ul>
 */
@Service
public class QueryGenerationServiceImpl implements QueryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QueryGenerationServiceImpl.class);

    private final QueryCacheService cacheService;
    private final Map<SearchEngineType, SearchStrategy> strategies;

    @Value("${query-generation.max-queries-limit:50}")
    private int maxQueriesLimit;

    @Value("${query-generation.min-queries-limit:1}")
    private int minQueriesLimit;

    @Value("${query-generation.default-queries:10}")
    private int defaultQueries;

    public QueryGenerationServiceImpl(
            QueryCacheService cacheService,
            Map<SearchEngineType, SearchStrategy> strategies) {
        this.cacheService = cacheService;
        this.strategies = strategies;
    }

    @Override
    public CompletableFuture<QueryGenerationResponse> generateQueries(QueryGenerationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🚀 Generating queries for {} using {} (max: {})",
                        request.getCategories(), request.getSearchEngine(), request.getMaxQueries());

                // Validate request
                request.validate();
                validateQueryLimit(request.getMaxQueries());

                // Check cache first
                QueryCacheKey cacheKey = QueryCacheKey.from(request);
                Optional<List<String>> cached = cacheService.getFromCache(cacheKey);

                if (cached.isPresent()) {
                    log.info("✅ Returning {} cached queries", cached.get().size());
                    return buildResponse(request, cached.get(), true);
                }

                // Get strategy for search engine
                SearchStrategy strategy = getStrategy(request.getSearchEngine());

                // Generate queries asynchronously
                List<String> queries = strategy.generateQueries(
                        request.getCategories(),
                        request.getGeographic(),
                        request.getMaxQueries()
                ).join(); // Block here since we're already in async context

                // Cache the results
                cacheService.cacheQueries(cacheKey, queries);

                // Optionally persist to PostgreSQL (async, fire-and-forget)
                if (request.getSessionId() != null) {
                    cacheService.persistQueries(cacheKey, queries, request.getSessionId());
                }

                log.info("✅ Generated {} fresh queries successfully", queries.size());
                return buildResponse(request, queries, false);

            } catch (IllegalArgumentException e) {
                log.error("❌ Invalid request: {}", e.getMessage());
                throw new QueryGenerationException("Invalid request: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("❌ Query generation failed", e);
                throw new QueryGenerationException("Query generation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Map<SearchEngineType, List<String>>> generateForMultipleProviders(
            List<SearchEngineType> searchEngines,
            Set<FundingSearchCategory> categories,
            GeographicScope geographic,
            int maxQueries,
            UUID sessionId) {

        log.info("🔄 Generating queries for {} search engines in parallel", searchEngines.size());

        // Create parallel requests for each search engine
        Map<SearchEngineType, CompletableFuture<List<String>>> futures =
                new java.util.EnumMap<>(SearchEngineType.class);

        for (SearchEngineType engine : searchEngines) {
            QueryGenerationRequest engineRequest = QueryGenerationRequest.builder()
                    .searchEngine(engine)
                    .categories(categories)
                    .geographic(geographic)
                    .maxQueries(maxQueries)
                    .sessionId(sessionId)
                    .build();

            // Extract just the queries from the response
            CompletableFuture<List<String>> queryFuture = generateQueries(engineRequest)
                    .thenApply(QueryGenerationResponse::getQueries);

            futures.put(engine, queryFuture);
        }

        // Wait for all to complete
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<SearchEngineType, List<String>> results =
                            new java.util.EnumMap<>(SearchEngineType.class);
                    futures.forEach((engine, future) -> results.put(engine, future.join()));

                    int totalQueries = results.values().stream()
                            .mapToInt(List::size)
                            .sum();

                    log.info("✅ Generated {} total queries across {} engines",
                            totalQueries, results.size());

                    return results;
                });
    }

    @Override
    public Map<String, Object> getCacheStatistics() {
        return cacheService.getStatistics();
    }

    @Override
    public void clearCache() {
        cacheService.clearCache();
        log.info("🧹 Cleared entire query generation cache");
    }

    /**
     * Gets the appropriate strategy for the given search engine.
     *
     * @param searchEngine Search engine type
     * @return Search strategy
     * @throws QueryGenerationException if no strategy found
     */
    private SearchStrategy getStrategy(SearchEngineType searchEngine) {
        SearchStrategy strategy = strategies.get(searchEngine);
        if (strategy == null) {
            throw new QueryGenerationException(
                    "No strategy configured for search engine: " + searchEngine);
        }
        return strategy;
    }

    /**
     * Validates query limit is within configured bounds.
     *
     * @param maxQueries Requested max queries
     * @throws IllegalArgumentException if out of bounds
     */
    private void validateQueryLimit(int maxQueries) {
        if (maxQueries < minQueriesLimit || maxQueries > maxQueriesLimit) {
            throw new IllegalArgumentException(
                    String.format("maxQueries must be between %d and %d, got: %d",
                            minQueriesLimit, maxQueriesLimit, maxQueries));
        }
    }

    /**
     * Builds a QueryGenerationResponse from generated queries.
     *
     * @param request Original request
     * @param queries Generated queries
     * @param fromCache Whether queries came from cache
     * @return Response object
     */
    private QueryGenerationResponse buildResponse(
            QueryGenerationRequest request,
            List<String> queries,
            boolean fromCache) {

        return QueryGenerationResponse.builder()
                .searchEngine(request.getSearchEngine())
                .queries(queries)
                .sessionId(request.getSessionId())
                .generatedAt(Instant.now())
                .fromCache(fromCache)
                .build();
    }
}
