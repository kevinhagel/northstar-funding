package com.northstar.funding.discovery.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search Result DTO
 *
 * Represents a search engine result with metadata for Phase 1 judging.
 * NO web page content - only what the search engine provides.
 *
 * Two-Phase Processing:
 * - Phase 1: Judge based on this metadata only (no HTTP request)
 * - Phase 2: If high confidence, crawl URL for full details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /**
     * URL of the search result
     * Will be used for domain extraction and eventual crawling
     */
    private String url;

    /**
     * Title from search engine
     * Example: "Bulgaria Education Grant Program - US-Bulgaria Foundation"
     */
    private String title;

    /**
     * Snippet/description from search engine
     * Example: "Support for education initiatives in Bulgaria. Awards range from €10,000 to €50,000..."
     */
    private String snippet;

    /**
     * Search engine that provided this result
     * Examples: "searxng", "browserbase", "tavily"
     */
    private String searchEngine;

    /**
     * Original search query that found this result
     * Used for relevance assessment
     */
    private String searchQuery;

    /**
     * Position in search results (1-based)
     * Lower numbers = higher search engine relevance
     */
    private Integer position;
}
