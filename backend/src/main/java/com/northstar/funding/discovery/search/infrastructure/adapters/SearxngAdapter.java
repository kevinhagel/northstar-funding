package com.northstar.funding.discovery.search.infrastructure.adapters;

import com.northstar.funding.discovery.search.domain.SearchEngineType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Searxng search engine adapter (Feature 003)
 *
 * Self-hosted meta-search engine on Mac Studio (192.168.1.10:8080).
 * No API key required, aggregates results from multiple search engines.
 *
 * Features:
 * - RestClient for HTTP communication
 * - Circuit breaker protection via Resilience4j
 * - Retry logic for transient failures
 * - JSON response parsing
 *
 * Constitutional Compliance:
 * - NO langchain4j (plain Spring RestClient)
 * - Vavr Try for error handling
 * - Resilience4j circuit breaker
 *
 * @author NorthStar Funding Team
 */
@Service
@Slf4j
public class SearxngAdapter implements SearchEngineAdapter {

    private final RestClient restClient;
    private final String baseUrl;
    private final boolean enabled;
    private final int timeoutSeconds;

    public SearxngAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${search.engines.searxng.base-url}") String baseUrl,
        @Value("${search.engines.searxng.enabled}") boolean enabled,
        @Value("${search.engines.searxng.timeout-seconds}") int timeoutSeconds
    ) {
        this.baseUrl = baseUrl;
        this.enabled = enabled;
        this.timeoutSeconds = timeoutSeconds;
        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();
    }

    @Override
    @CircuitBreaker(name = "searxng", fallbackMethod = "searchFallback")
    @Retry(name = "searchEngines")
    public Try<List<SearchResult>> search(String query, int maxResults) {
        if (!enabled) {
            log.warn("Searxng adapter is disabled");
            return Try.success(Collections.emptyList());
        }

        return Try.of(() -> {
            log.debug("Executing Searxng search: query='{}', maxResults={}", query, maxResults);

            var response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search")
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("language", "en")
                    .queryParam("number_of_results", maxResults)
                    .build())
                .retrieve()
                .body(SearxngResponse.class);

            if (response == null || response.results() == null) {
                log.warn("Searxng returned null response for query: {}", query);
                return Collections.<SearchResult>emptyList();
            }

            var timestamp = Instant.now();
            var results = IntStream.range(0, Math.min(response.results().size(), maxResults))
                .mapToObj(i -> {
                    var result = response.results().get(i);
                    return new SearchResult(
                        result.url(),
                        result.title(),
                        result.content(),
                        SearchEngineType.SEARXNG,
                        query,
                        i + 1,
                        timestamp
                    );
                })
                .toList();

            log.info("Searxng search completed: query='{}', results={}", query, results.size());
            return results;
        }).onFailure(e -> log.error("Searxng search failed: query='{}', error={}", query, e.getMessage(), e));
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.SEARXNG;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public HealthStatus checkHealth() {
        var timestamp = Instant.now();

        try {
            // Simple health check: test search with minimal query
            var result = search("test", 1);

            return new HealthStatus(
                SearchEngineType.SEARXNG,
                result.isSuccess(),
                timestamp,
                "CLOSED",
                result.isFailure() ? result.getCause().getMessage() : null
            );
        } catch (Exception e) {
            return new HealthStatus(
                SearchEngineType.SEARXNG,
                false,
                timestamp,
                "UNKNOWN",
                e.getMessage()
            );
        }
    }

    /**
     * Fallback method for circuit breaker
     * Called when circuit is OPEN or search fails after retries
     */
    private Try<List<SearchResult>> searchFallback(String query, int maxResults, Exception ex) {
        log.error("Searxng circuit breaker triggered for query='{}': {}", query, ex.getMessage());
        return Try.failure(new RuntimeException("Searxng unavailable (circuit breaker OPEN)", ex));
    }

    /**
     * Searxng JSON response format
     */
    record SearxngResponse(
        String query,
        Integer number_of_results,
        List<SearxngResult> results
    ) {}

    record SearxngResult(
        String url,
        String title,
        String content,
        String engine,
        Double score
    ) {}
}
