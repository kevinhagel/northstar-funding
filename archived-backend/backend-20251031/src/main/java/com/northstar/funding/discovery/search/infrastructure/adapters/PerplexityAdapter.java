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
 * Perplexity search engine adapter (Feature 003)
 *
 * AI-enhanced search with reasoning and citations.
 * Uses Chat Completions API format, extracts URLs from citations.
 *
 * Features:
 * - RestClient with Bearer token authentication
 * - Circuit breaker protection
 * - Citation extraction for URLs
 * - Sonar model for search
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
public class PerplexityAdapter implements SearchEngineAdapter {

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public PerplexityAdapter(
        RestClient.Builder restClientBuilder,
        @Value("${search.engines.perplexity.base-url}") String baseUrl,
        @Value("${search.engines.perplexity.api-key}") String apiKey,
        @Value("${search.engines.perplexity.enabled}") boolean enabled
    ) {
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.restClient = restClientBuilder
            .baseUrl(baseUrl)
            .build();
    }

    @Override
    @CircuitBreaker(name = "perplexity", fallbackMethod = "searchFallback")
    @Retry(name = "searchEngines")
    public Try<List<SearchResult>> search(String query, int maxResults) {
        if (!enabled) {
            log.warn("Perplexity adapter is disabled");
            return Try.success(Collections.emptyList());
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Perplexity API key not configured");
            return Try.failure(new IllegalStateException("Perplexity API key not configured"));
        }

        return Try.of(() -> {
            log.debug("Executing Perplexity search: query='{}', maxResults={}", query, maxResults);

            var requestBody = Map.of(
                "model", "sonar",
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", query
                ))
            );

            var response = restClient.post()
                .uri("/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .body(requestBody)
                .retrieve()
                .body(PerplexityResponse.class);

            if (response == null || response.citations() == null) {
                log.warn("Perplexity returned null response for query: {}", query);
                return Collections.<SearchResult>emptyList();
            }

            var timestamp = Instant.now();
            var citations = response.citations();
            var content = response.choices() != null && !response.choices().isEmpty()
                ? response.choices().get(0).message().content()
                : "";

            var results = IntStream.range(0, Math.min(citations.size(), maxResults))
                .mapToObj(i -> {
                    var url = citations.get(i);
                    return new SearchResult(
                        url,
                        extractTitleFromUrl(url),
                        content,
                        SearchEngineType.PERPLEXITY,
                        query,
                        i + 1,
                        timestamp
                    );
                })
                .toList();

            log.info("Perplexity search completed: query='{}', citations={}", query, results.size());
            return results;
        }).onFailure(e -> log.error("Perplexity search failed: query='{}', error={}", query, e.getMessage(), e));
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.PERPLEXITY;
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
                SearchEngineType.PERPLEXITY,
                false,
                timestamp,
                "UNKNOWN",
                "API key not configured"
            );
        }

        try {
            var result = search("test", 1);
            return new HealthStatus(
                SearchEngineType.PERPLEXITY,
                result.isSuccess(),
                timestamp,
                "CLOSED",
                result.isFailure() ? result.getCause().getMessage() : null
            );
        } catch (Exception e) {
            return new HealthStatus(
                SearchEngineType.PERPLEXITY,
                false,
                timestamp,
                "UNKNOWN",
                e.getMessage()
            );
        }
    }

    private Try<List<SearchResult>> searchFallback(String query, int maxResults, Exception ex) {
        log.error("Perplexity circuit breaker triggered for query='{}': {}", query, ex.getMessage());
        return Try.failure(new RuntimeException("Perplexity unavailable (circuit breaker OPEN)", ex));
    }

    private String extractTitleFromUrl(String url) {
        try {
            var uri = java.net.URI.create(url);
            var host = uri.getHost();
            return host != null ? host : url;
        } catch (Exception e) {
            return url;
        }
    }

    record PerplexityResponse(
        String id,
        String model,
        List<PerplexityChoice> choices,
        List<String> citations
    ) {}

    record PerplexityChoice(
        PerplexityMessage message,
        String finish_reason
    ) {}

    record PerplexityMessage(
        String role,
        String content
    ) {}
}
