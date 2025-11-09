package com.northstar.funding.kafka.topics;

/**
 * Kafka topic constants for the funding discovery workflow.
 *
 * <p>Topics represent stages in the event-driven search workflow:
 * <ul>
 *   <li>{@link #SEARCH_REQUESTS} - Incoming search requests from REST API</li>
 *   <li>{@link #SEARCH_RESULTS_RAW} - Raw search results from search engines</li>
 *   <li>{@link #SEARCH_RESULTS_VALIDATED} - Results after domain validation and blacklist filtering</li>
 *   <li>{@link #WORKFLOW_ERRORS} - Dead letter queue for workflow failures</li>
 * </ul>
 */
public final class KafkaTopics {

    /**
     * Search requests topic.
     * <p>
     * Published by: REST API (SearchController)
     * Consumed by: SearchRequestConsumer (search-workflow module)
     * Retention: 7 days
     */
    public static final String SEARCH_REQUESTS = "search-requests";

    /**
     * Raw search results topic.
     * <p>
     * Published by: SearchRequestConsumer (after executing search via adapter)
     * Consumed by: DomainProcessorConsumer (search-workflow module)
     * Retention: 7 days
     */
    public static final String SEARCH_RESULTS_RAW = "search-results-raw";

    /**
     * Validated search results topic.
     * <p>
     * Published by: DomainProcessorConsumer (after domain validation)
     * Consumed by: ScoringConsumer (search-workflow module)
     * Retention: 7 days
     */
    public static final String SEARCH_RESULTS_VALIDATED = "search-results-validated";

    /**
     * Workflow errors topic (dead letter queue).
     * <p>
     * Published by: Any workflow consumer on error
     * Consumed by: ErrorMonitoringService (future)
     * Retention: 30 days (longer for error analysis)
     */
    public static final String WORKFLOW_ERRORS = "workflow-errors";

    private KafkaTopics() {
        // Utility class - prevent instantiation
    }
}
