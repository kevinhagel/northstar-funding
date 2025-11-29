package com.northstar.funding.querygeneration.strategy;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Search Strategy interface for provider-specific query generation.
 *
 * <p>Part of NorthStar Ubiquitous Language:
 * <ul>
 *   <li><b>Keyword Search</b> - Short keyword-based queries for traditional search engines</li>
 *   <li><b>Prompt Search</b> - Engineered prompts for AI-powered search engines</li>
 * </ul>
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link KeywordSearchStrategy} - For Brave, Serper, SearXNG</li>
 *   <li>{@link PromptSearchStrategy} - For Perplexica and future AI search engines</li>
 * </ul>
 *
 * <p>Implementations must be stateless and thread-safe.
 */
public interface SearchStrategy {

    /**
     * Generate search queries for this strategy's search engine.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST return CompletableFuture for async execution</li>
     *   <li>MUST map categories to appropriate search terms</li>
     *   <li>MUST map geographic scope to location terms</li>
     *   <li>MUST use provider-specific prompt template</li>
     *   <li>MUST call LM Studio via LangChain4j</li>
     *   <li>MUST parse LLM response into query list</li>
     *   <li>MUST validate generated queries</li>
     *   <li>MUST be stateless and thread-safe</li>
     *   <li>MUST complete within 30 seconds</li>
     * </ul>
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
     * Get the search engine this strategy supports.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST return the specific search engine enum value</li>
     *   <li>MUST be consistent (always return same value)</li>
     * </ul>
     *
     * @return Search engine type
     */
    SearchEngineType getSearchEngine();

    /**
     * Get the search type for logging/metrics.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST return either "keyword" or "prompt"</li>
     *   <li>MUST be consistent with search engine type</li>
     * </ul>
     *
     * @return Search type string ("keyword" or "prompt")
     */
    String getSearchType();
}
