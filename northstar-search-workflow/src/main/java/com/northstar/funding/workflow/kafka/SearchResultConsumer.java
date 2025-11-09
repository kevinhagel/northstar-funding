package com.northstar.funding.workflow.kafka;

import com.northstar.funding.kafka.events.SearchResultEvent;
import com.northstar.funding.kafka.events.WorkflowErrorEvent;
import com.northstar.funding.workflow.service.DomainBlacklistCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.net.URI;

/**
 * Kafka consumer for search-results-raw topic.
 *
 * <p>Validates search results, checks blacklist, and publishes
 * validated results to search-results-validated topic.
 *
 * <p>Configuration:
 * <ul>
 *   <li>Topic: search-results-raw</li>
 *   <li>Group ID: search-workflow</li>
 *   <li>Manual acknowledgment</li>
 *   <li>Blacklist filtering: Uses Valkey cache for fast lookups</li>
 * </ul>
 */
@Service
public class SearchResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(SearchResultConsumer.class);

    private final DomainBlacklistCache blacklistCache;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SearchResultConsumer(DomainBlacklistCache blacklistCache, KafkaTemplate<String, Object> kafkaTemplate) {
        this.blacklistCache = blacklistCache;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "search-results-raw",
            groupId = "search-workflow",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSearchResult(SearchResultEvent event, Acknowledgment acknowledgment) {
        try {
            // Input validation
            if (event == null) {
                log.warn("⚠️ Received null search result event");
                return;
            }

            if (event.getUrl() == null || event.getUrl().trim().isEmpty()) {
                log.warn("⚠️ Received search result with null/empty URL: {}", event);
                return;
            }

            // Extract domain from URL
            String domain = extractDomain(event.getUrl());
            if (domain == null) {
                log.warn("⚠️ Invalid URL, cannot extract domain: {}", event.getUrl());
                return;
            }

            log.debug("🔍 Checking blacklist for domain: {}", domain);

            // Check blacklist using Valkey cache
            boolean isBlacklisted = blacklistCache.isBlacklisted(domain);

            if (isBlacklisted) {
                log.info("🚫 Filtered blacklisted domain: {}", domain);
                return;
            }

            // Publish to validated topic
            kafkaTemplate.send("search-results-validated", event);
            log.debug("✅ Published validated result: domain={}, title='{}'", domain, event.getTitle());

        } catch (Exception e) {
            log.error("❌ Error processing search result: {}", e.getMessage());

            if (event != null) {
                WorkflowErrorEvent errorEvent = WorkflowErrorEvent.builder()
                        .sessionId(event.getSessionId())
                        .errorMessage("Result validation failed: " + e.getMessage())
                        .errorType("VALIDATION_ERROR")
                        .stage("DOMAIN_PROCESSING")
                        .stackTrace(getStackTrace(e))
                        .build();

                kafkaTemplate.send("workflow-errors", errorEvent);
            }

        } finally {
            acknowledgment.acknowledge();
        }
    }

    /**
     * Extract domain from URL.
     *
     * <p>Examples:
     * <ul>
     *   <li>https://education.gov.bg/grants → education.gov.bg</li>
     *   <li>https://www.example.org/page → www.example.org</li>
     *   <li>https://funding.research.edu.eu/opportunities → funding.research.edu.eu</li>
     * </ul>
     *
     * @return domain name or null if URL is invalid
     */
    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return (host != null && !host.isEmpty()) ? host : null;
        } catch (Exception e) {
            return null;
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
