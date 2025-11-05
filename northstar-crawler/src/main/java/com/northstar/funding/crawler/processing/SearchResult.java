package com.northstar.funding.crawler.processing;

import lombok.Builder;
import lombok.Data;

/**
 * Simple search result DTO for processing.
 *
 * Represents metadata from search engine results before
 * conversion to FundingSourceCandidate entities.
 */
@Data
@Builder
public class SearchResult {

    /**
     * Search result title
     */
    private String title;

    /**
     * Search result description/snippet
     */
    private String description;

    /**
     * Full URL to the result
     */
    private String url;
}
