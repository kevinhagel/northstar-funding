package com.northstar.funding.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event representing a search result that has passed domain validation.
 *
 * <p>Published to: {@code search-results-validated} topic
 * <p>Published by: DomainProcessorConsumer (after blacklist check)
 * <p>Consumed by: ScoringConsumer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatedResultEvent {

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
     * Extracted domain name (e.g., "example.edu").
     */
    private String domain;

    /**
     * Domain ID from database (after registration).
     */
    private Long domainId;

    /**
     * Search engine that provided this result.
     */
    private String searchEngine;

    /**
     * Timestamp when validation completed.
     */
    private Instant timestamp;

    /**
     * Search query that produced this result.
     */
    private String query;
}
