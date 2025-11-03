/**
 * API Contract: QueryGenerationStrategy
 *
 * Strategy interface for provider-specific query generation.
 * Implementations must be stateless and thread-safe.
 *
 * Module: northstar-query-generation
 * Package: com.northstar.funding.querygeneration.strategy
 */

package com.northstar.funding.querygeneration.strategy;

import com.northstar.funding.querygeneration.model.SearchProvider;
import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface QueryGenerationStrategy {

    /**
     * Generate search queries for this strategy's provider.
     *
     * CONTRACT:
     * - MUST return CompletableFuture for async execution
     * - MUST map categories to appropriate search terms
     * - MUST map geographic scope to location terms
     * - MUST use provider-specific prompt template
     * - MUST call LM Studio via LangChain4j
     * - MUST parse LLM response into query list
     * - MUST validate generated queries
     * - MUST be stateless and thread-safe
     * - MUST complete within 30 seconds
     *
     * @param categories Funding categories to target
     * @param geographic Geographic scope
     * @param maxQueries Number of queries to generate (1-50)
     * @return CompletableFuture containing list of generated queries
     */
    CompletableFuture<List<String>> generateQueries(
        Set<FundingSearchCategory> categories,
        GeographicScope geographic,
        int maxQueries
    );

    /**
     * Get the search provider this strategy supports.
     *
     * CONTRACT:
     * - MUST return the specific provider enum value
     * - MUST be consistent (always return same value)
     *
     * @return Search provider enum
     */
    SearchProvider getProvider();

    /**
     * Get the query type for logging/metrics.
     *
     * CONTRACT:
     * - MUST return either "keyword" or "ai-optimized"
     * - MUST be consistent with provider type
     *
     * @return Query type string
     */
    String getQueryType();
}
