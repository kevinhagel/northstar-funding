package com.northstar.funding.workflow.kafka;

import com.northstar.funding.kafka.events.SearchRequestEvent;
import com.northstar.funding.kafka.events.SearchResultEvent;
import com.northstar.funding.kafka.events.WorkflowErrorEvent;
import com.northstar.funding.search.adapter.SearchAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kafka consumer for search-requests topic.
 *
 * <p>Receives search requests, delegates to available search adapters,
 * and publishes results to search-results-raw topic.
 *
 * <p>Configuration:
 * <ul>
 *   <li>Topic: search-requests</li>
 *   <li>Group ID: search-workflow</li>
 *   <li>Manual acknowledgment (consumer acknowledges after processing)</li>
 *   <li>Error handling: Publishes errors to workflow-errors topic</li>
 * </ul>
 */
@Service
public class SearchRequestConsumer {

    private static final Logger log = LoggerFactory.getLogger(SearchRequestConsumer.class);

    private final List<SearchAdapter> searchAdapters;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SearchRequestConsumer(List<SearchAdapter> searchAdapters, KafkaTemplate<String, Object> kafkaTemplate) {
        this.searchAdapters = searchAdapters;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "search-requests",
            groupId = "search-workflow",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSearchRequest(SearchRequestEvent event, Acknowledgment acknowledgment) {
        try {
            // Input validation
            if (event == null) {
                log.warn("⚠️ Received null search request event");
                return;
            }

            log.info("🔍 Processing search request: sessionId={}, query='{}', maxResults={}",
                    event.getSessionId(), event.getQuery(), event.getMaxResults());

            // Check if any adapters are available
            List<SearchAdapter> availableAdapters = searchAdapters.stream()
                    .filter(SearchAdapter::isAvailable)
                    .toList();

            if (availableAdapters.isEmpty()) {
                log.warn("⚠️ No search adapters available for query: {}", event.getQuery());
                return;
            }

            // Process with each available adapter
            for (SearchAdapter adapter : availableAdapters) {
                try {
                    List<SearchAdapter.SearchResult> results = adapter.search(event.getQuery(), event.getMaxResults());

                    if (results.isEmpty()) {
                        log.debug("No results from adapter: {}", adapter.getEngineType());
                        continue;
                    }

                    // Publish each result to search-results-raw topic
                    for (SearchAdapter.SearchResult result : results) {
                        SearchResultEvent resultEvent = SearchResultEvent.builder()
                                .sessionId(event.getSessionId())
                                .url(result.url())
                                .title(result.title())
                                .description(result.description())
                                .build();

                        kafkaTemplate.send("search-results-raw", resultEvent);
                    }

                    log.info("✅ Published {} results from {} to search-results-raw",
                            results.size(), adapter.getEngineType());

                } catch (Exception e) {
                    log.error("❌ Adapter {} failed for query '{}': {}",
                            adapter.getEngineType(), event.getQuery(), e.getMessage());

                    // Publish error event
                    WorkflowErrorEvent errorEvent = WorkflowErrorEvent.builder()
                            .sessionId(event.getSessionId())
                            .errorMessage("Search adapter failed: " + e.getMessage())
                            .errorType("SEARCH_ADAPTER_ERROR")
                            .stage("SEARCH_EXECUTION")
                            .stackTrace(getStackTrace(e))
                            .build();

                    kafkaTemplate.send("workflow-errors", errorEvent);
                }
            }

        } catch (Exception e) {
            log.error("❌ Unexpected error processing search request: {}", e.getMessage());

            if (event != null) {
                WorkflowErrorEvent errorEvent = WorkflowErrorEvent.builder()
                        .sessionId(event.getSessionId())
                        .errorMessage("Unexpected error: " + e.getMessage())
                        .errorType("UNEXPECTED_ERROR")
                        .stage("SEARCH_EXECUTION")
                        .stackTrace(getStackTrace(e))
                        .build();

                kafkaTemplate.send("workflow-errors", errorEvent);
            }

        } finally {
            acknowledgment.acknowledge();
        }
    }

    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
