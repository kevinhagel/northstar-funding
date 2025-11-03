/**
 * API Contract: QueryGenerationService
 *
 * This is the main service API for generating search queries.
 * This contract defines what the service MUST do, not how it's implemented.
 *
 * Module: northstar-query-generation
 * Package: com.northstar.funding.querygeneration.service
 */

package com.northstar.funding.querygeneration.service;

import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.model.SearchProvider;
import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface QueryGenerationService {

    /**
     * Generate queries for a single search provider.
     *
     * CONTRACT:
     * - MUST return CompletableFuture for async execution
     * - MUST check cache before generation
     * - MUST cache successful results
     * - MUST persist queries asynchronously
     * - MUST provide fallback queries on LLM failure
     * - MUST complete within 30 seconds or timeout
     * - MUST validate request parameters
     *
     * @param request Query generation request with provider, categories, geographic, etc.
     * @return CompletableFuture containing response with generated queries
     * @throws IllegalArgumentException if request validation fails
     */
    CompletableFuture<QueryGenerationResponse> generateQueries(
        QueryGenerationRequest request
    );

    /**
     * Generate queries for multiple providers in parallel.
     *
     * CONTRACT:
     * - MUST execute all providers in parallel using Virtual Threads
     * - MUST return map of provider → queries
     * - MUST handle individual provider failures gracefully
     * - MUST use same cache and persistence as single-provider method
     * - MUST complete all providers within 30 seconds
     *
     * @param providers List of search providers
     * @param categories Funding categories to target
     * @param geographic Geographic scope
     * @param maxQueries Number of queries per provider (1-50)
     * @param sessionId Discovery session identifier
     * @return CompletableFuture containing map of provider → query list
     * @throws IllegalArgumentException if parameters are invalid
     */
    CompletableFuture<Map<SearchProvider, List<String>>> generateForMultipleProviders(
        List<SearchProvider> providers,
        Set<FundingSearchCategory> categories,
        GeographicScope geographic,
        int maxQueries,
        UUID sessionId
    );

    /**
     * Get cache statistics for monitoring.
     *
     * CONTRACT:
     * - MUST return current cache hit rate
     * - MUST return cache size
     * - MUST return total requests/hits/misses
     *
     * @return Map of statistic name → value
     */
    Map<String, Object> getCacheStatistics();

    /**
     * Clear the query cache (for testing/admin purposes).
     *
     * CONTRACT:
     * - MUST remove all cached queries
     * - MUST reset cache statistics
     * - MUST be idempotent
     */
    void clearCache();
}
