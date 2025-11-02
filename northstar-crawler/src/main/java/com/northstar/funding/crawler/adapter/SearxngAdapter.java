package com.northstar.funding.crawler.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.funding.crawler.adapter.searxng.SearxngResponse;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import com.northstar.funding.crawler.exception.ProviderTimeoutException;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SearXNG adapter implementation.
 *
 * SearXNG provides:
 * - Privacy-respecting metasearch (aggregates results from multiple engines)
 * - Self-hosted instance (no API key required)
 * - JSON API support
 * - No rate limiting (self-hosted)
 *
 * Rate Limit: Unlimited (self-hosted at 192.168.1.10:8080)
 * Timeout: 7 seconds (higher than commercial APIs due to metasearch aggregation)
 */
@Component
@Slf4j
public class SearxngAdapter extends AbstractSearchProviderAdapter {

    private final SearchProviderConfig.SearxngConfig config;
    private final ObjectMapper objectMapper;

    public SearxngAdapter(
            SearchProviderConfig searchProviderConfig,
            ObjectMapper objectMapper
    ) {
        super(
                searchProviderConfig.getSearxng().getTimeout() / 1000, // convert ms to seconds
                searchProviderConfig.getSearxng().getMaxResults(),
                Integer.MAX_VALUE // No rate limit for self-hosted
        );
        this.config = searchProviderConfig.getSearxng();
        this.objectMapper = objectMapper;

        log.info("SearxngAdapter initialized - baseUrl={}, timeout={}ms, format={}",
                config.getBaseUrl(), config.getTimeout(), config.getFormat());
    }

    @Override
    public Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID discoverySessionId) {
        return Try.of(() -> {
            // No rate limiting for self-hosted SearXNG
            incrementUsageAndCheckLimit();

            // Build request URL
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s?q=%s&format=%s",
                    config.getBaseUrl(),
                    encodedQuery,
                    config.getFormat()
            );

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .GET()
                    .build();

            log.debug("Executing SearXNG query: {}", query);

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() != 200) {
                throw new RuntimeException("SearXNG returned status " + response.statusCode());
            }

            // Parse JSON response
            SearxngResponse searxngResponse = objectMapper.readValue(
                    response.body(),
                    SearxngResponse.class
            );

            // Convert to SearchResult entities
            List<SearchResult> results = convertToSearchResults(searxngResponse, query, discoverySessionId);

            // Limit to maxResults
            List<SearchResult> limitedResults = results.stream()
                    .limit(maxResults)
                    .toList();

            log.info("SearXNG completed: {} -> {} results (usage: {})",
                    query, limitedResults.size(), getCurrentUsageCount());

            return limitedResults;

        }).recoverWith(throwable -> {
            if (throwable instanceof java.net.http.HttpTimeoutException) {
                return Try.failure(new ProviderTimeoutException(
                        "SearXNG",
                        Duration.ofMillis(config.getTimeout())
                ));
            }
            return Try.failure(throwable);
        });
    }

    @Override
    public SearchEngineType getProviderType() {
        return SearchEngineType.SEARXNG;
    }

    @Override
    public boolean supportsKeywordQueries() {
        return true;
    }

    @Override
    public boolean supportsAIOptimizedQueries() {
        return false; // SearXNG is traditional keyword search
    }

    /**
     * Convert SearXNG API response to SearchResult entities.
     */
    private List<SearchResult> convertToSearchResults(
            SearxngResponse response,
            String query,
            UUID discoverySessionId
    ) {
        if (response.getResults() == null) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        return response.getResults().stream()
                .map(result -> {
                    String domain = normalizeDomain(result.getUrl());
                    int position = response.getResults().indexOf(result) + 1;

                    return SearchResult.builder()
                            .url(result.getUrl())
                            .domain(domain)
                            .title(result.getTitle() != null ? result.getTitle() : "")
                            .description(result.getContent() != null ? result.getContent() : "")
                            .rankPosition(position)
                            .searchEngine(SearchEngineType.SEARXNG)
                            .discoveredAt(now)
                            .searchDate(today)
                            .discoverySessionId(discoverySessionId)
                            .build();
                })
                .toList();
    }
}
