package com.northstar.funding.discovery.search.infrastructure.adapters;

import com.northstar.funding.discovery.search.domain.SearchEngineType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Tavily search engine adapter (Feature 003)
 *
 * AI-optimized search API designed for LLM applications.
 * Requires API key, returns clean structured results.
 *
 * Features:
 * - RestClient with Bearer token authentication
 * - Circuit breaker protection
 * - Retry logic for rate limits
 * - JSON request/response
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
public class TavilyAdapter implements SearchEngineAdapter {

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public TavilyAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${search.engines.tavily.base-url}") String baseUrl,
        @Value("${search.engines.tavily.api-key}") String apiKey,
        @Value("${search.engines.tavily.enabled}") boolean enabled
    ) {
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();
    }

    @Override
    @CircuitBreaker(name = "tavily", fallbackMethod = "searchFallback")
    @Retry(name = "searchEngines")
    public Try<List<SearchResult>> search(String query, int maxResults) {
        if (!enabled) {
            log.warn("Tavily adapter is disabled");
            return Try.success(Collections.emptyList());
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Tavily API key not configured");
            return Try.failure(new IllegalStateException("Tavily API key not configured"));
        }

        return Try.of(() -> {
            log.debug("Executing Tavily search: query='{}', maxResults={}", query, maxResults);

            var requestBody = Map.of(
                "query", query,
                "max_results", maxResults,
                "include_answer", false,
                "include_raw_content", false,
                "include_images", false
            );

            var response = restClient.post()
                .uri("/search")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(TavilyResponse.class);

            if (response == null || response.results() == null) {
                log.warn("Tavily returned null response for query: {}", query);
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
                        SearchEngineType.TAVILY,
                        query,
                        i + 1,
                        timestamp
                    );
                })
                .toList();

            log.info("Tavily search completed: query='{}', results={}", query, results.size());
            return results;
        }).onFailure(e -> log.error("Tavily search failed: query='{}', error={}", query, e.getMessage(), e));
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.TAVILY;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public HealthStatus checkHealth() {
        var timestamp = Instant.now();

        if (apiKey == null || apiKey.isBlank()) {
            return new HealthStatus(
                SearchEngineType.TAVILY,
                false,
                timestamp,
                "UNKNOWN",
                "API key not configured"
            );
        }

        try {
            var result = search("test", 1);
            return new HealthStatus(
                SearchEngineType.TAVILY,
                result.isSuccess(),
                timestamp,
                "CLOSED",
                result.isFailure() ? result.getCause().getMessage() : null
            );
        } catch (Exception e) {
            return new HealthStatus(
                SearchEngineType.TAVILY,
                false,
                timestamp,
                "UNKNOWN",
                e.getMessage()
            );
        }
    }

    private Try<List<SearchResult>> searchFallback(String query, int maxResults, Exception ex) {
        log.error("Tavily circuit breaker triggered for query='{}': {}", query, ex.getMessage());
        return Try.failure(new RuntimeException("Tavily unavailable (circuit breaker OPEN)", ex));
    }

    record TavilyResponse(
        String query,
        List<TavilyResult> results
    ) {}

    record TavilyResult(
        String url,
        String title,
        String content,
        Double score
    ) {}
}
