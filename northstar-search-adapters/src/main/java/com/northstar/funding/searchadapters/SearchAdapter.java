package com.northstar.funding.searchadapters;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.searchadapters.exception.SearchAdapterException;

import java.util.List;

/**
 * Common interface for all search engine adapters.
 *
 * Implementations: BraveSearchAdapter, SerperAdapter, SearXNGAdapter, TavilyAdapter
 *
 * Contract:
 * - search() returns empty list for zero results (NOT an exception)
 * - search() throws SearchAdapterException for API failures (auth, network, timeout)
 * - getEngineType() identifies which search engine this adapter uses
 * - isAvailable() checks if adapter is configured and reachable
 */
public interface SearchAdapter {

    /**
     * Execute search query and return results.
     *
     * @param query The search query string (keyword or natural language)
     * @param maxResults Maximum number of results to return (1-50)
     * @return List of search results (empty list if zero results - NOT an error)
     * @throws SearchAdapterException if API call fails (network, auth, rate limit, timeout)
     *         NOT thrown for zero results
     */
    List<SearchResult> search(String query, int maxResults);

    /**
     * Get the search engine type this adapter implements.
     *
     * @return SearchEngineType enum (BRAVE, SERPER, SEARXNG, TAVILY)
     */
    SearchEngineType getEngineType();

    /**
     * Check if this adapter is available (API key configured, service reachable).
     *
     * @return true if adapter can be used, false otherwise
     */
    boolean isAvailable();
}
