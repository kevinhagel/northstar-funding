package com.northstar.funding.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event representing a raw search result from a search engine.
 *
 * <p>Published to: {@code search-results-raw} topic
 * <p>Published by: SearchRequestConsumer (after search execution)
 * <p>Consumed by: DomainProcessorConsumer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultEvent {

    /**
     * Original search request ID for correlation.
     */
    private UUID requestId;

    /**
     * Discovery session ID for grouping.
     */
    private UUID sessionId;

    /**
     * Search result URL.
     */
    private String url;

    /**
     * Page title from search result.
     */
    private String title;

    /**
     * Description snippet from search result.
     */
    private String description;

    /**
     * Search engine that provided this result.
     */
    private String searchEngine;

    /**
     * Timestamp when result was retrieved.
     */
    private Instant timestamp;

    /**
     * Search query that produced this result.
     */
    private String query;
}
