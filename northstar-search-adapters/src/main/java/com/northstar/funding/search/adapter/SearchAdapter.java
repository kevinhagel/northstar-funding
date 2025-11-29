package com.northstar.funding.search.adapter;

import com.northstar.funding.domain.SearchEngineType;

import java.util.List;

/**
 * Strategy interface for search engine adapters.
 *
 * <p>Implementations provide access to different search engines:
 * <ul>
 *   <li>SearXNGAdapter - Self-hosted metasearch engine</li>
 *   <li>BraveAdapter - Brave Search API (future)</li>
 *   <li>SerperAdapter - Google via Serper API (future)</li>
 *   <li>PerplexicaAdapter - AI-powered search API (future)</li>
 * </ul>
 */
public interface SearchAdapter {

    /**
     * Execute search query and return raw results (metadata only, no crawling).
     *
     * @param query Search query text
     * @param maxResults Maximum number of results to retrieve (typically 20-25)
     * @return List of search results with URL, title, description
     */
    List<SearchResult> search(String query, int maxResults);

    /**
     * Get the search engine type this adapter represents.
     *
     * @return Search engine type enum value
     */
    SearchEngineType getEngineType();

    /**
     * Check if adapter is available (healthy, API key valid, etc.).
     *
     * @return true if adapter is ready to execute searches
     */
    boolean isAvailable();

    /**
     * Simple search result DTO (metadata only).
     */
    record SearchResult(
            String url,
            String title,
            String description
    ) {}
}
