package com.northstar.funding.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event representing a workflow error (dead letter queue).
 *
 * <p>Published to: {@code workflow-errors} topic
 * <p>Published by: Any workflow consumer on error
 * <p>Consumed by: ErrorMonitoringService (future)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowErrorEvent {

    /**
     * Unique identifier for this error event.
     */
    private UUID errorId;

    /**
     * Original request ID that caused the error (if applicable).
     */
    private UUID requestId;

    /**
     * Discovery session ID for grouping.
     */
    private UUID sessionId;

    /**
     * Workflow stage where error occurred.
     * Examples: SEARCH_EXECUTION, DOMAIN_PROCESSING, SCORING, PERSISTENCE
     */
    private String stage;

    /**
     * Error type classification.
     * Examples: HTTP_TIMEOUT, PARSE_ERROR, DATABASE_ERROR, BLACKLIST_CHECK_FAILED
     */
    private String errorType;

    /**
     * Detailed error message.
     */
    private String errorMessage;

    /**
     * Stack trace (optional, for debugging).
     */
    private String stackTrace;

    /**
     * Number of retry attempts made before publishing to DLQ.
     */
    private int retryCount;

    /**
     * Original event payload that caused the error (JSON string).
     */
    private String originalPayload;

    /**
     * Timestamp when error occurred.
     */
    private Instant timestamp;

    /**
     * Additional context (optional, JSON string).
     * Example: {"searchEngine": "SEARXNG", "query": "Bulgaria education grants"}
     */
    private String context;
}
