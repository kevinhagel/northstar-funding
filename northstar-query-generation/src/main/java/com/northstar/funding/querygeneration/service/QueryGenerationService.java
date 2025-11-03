package com.northstar.funding.querygeneration.service;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main service API for generating search queries.
 *
 * <p>This contract defines what the service MUST do, not how it's implemented.
 */
public interface QueryGenerationService {

    /**
     * Generate queries for a single search engine.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST return CompletableFuture for async execution</li>
     *   <li>MUST check cache before generation</li>
     *   <li>MUST cache successful results</li>
     *   <li>MUST persist queries asynchronously</li>
     *   <li>MUST provide fallback queries on LLM failure</li>
     *   <li>MUST complete within 30 seconds or timeout</li>
     *   <li>MUST validate request parameters</li>
     * </ul>
     *
     * @param request Query generation request with engine, categories, geographic, etc.
     * @return CompletableFuture containing response with generated queries
     * @throws IllegalArgumentException if request validation fails
     */
    CompletableFuture<QueryGenerationResponse> generateQueries(
            QueryGenerationRequest request
    );

    /**
     * Generate queries for multiple search engines in parallel.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST execute all engines in parallel using Virtual Threads</li>
     *   <li>MUST return map of engine → queries</li>
     *   <li>MUST handle individual engine failures gracefully</li>
     *   <li>MUST use same cache and persistence as single-engine method</li>
     *   <li>MUST complete all engines within 30 seconds</li>
     * </ul>
     *
     * @param searchEngines List of search engines
     * @param categories Funding categories to target
     * @param geographic Geographic scope
     * @param maxQueries Number of queries per engine (1-50)
     * @param sessionId Discovery session identifier
     * @return CompletableFuture containing map of engine → query list
     * @throws IllegalArgumentException if parameters are invalid
     */
    CompletableFuture<Map<SearchEngineType, List<String>>> generateForMultipleProviders(
            List<SearchEngineType> searchEngines,
            Set<FundingSearchCategory> categories,
            GeographicScope geographic,
            int maxQueries,
            UUID sessionId
    );

    /**
     * Get cache statistics for monitoring.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST return current cache hit rate</li>
     *   <li>MUST return cache size</li>
     *   <li>MUST return total requests/hits/misses</li>
     * </ul>
     *
     * @return Map of statistic name → value
     */
    Map<String, Object> getCacheStatistics();

    /**
     * Clear the query cache (for testing/admin purposes).
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST remove all cached queries</li>
     *   <li>MUST reset cache statistics</li>
     *   <li>MUST be idempotent</li>
     * </ul>
     */
    void clearCache();
}
