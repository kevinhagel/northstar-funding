package com.northstar.funding.crawler.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.funding.crawler.adapter.brave.BraveSearchResponse;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import com.northstar.funding.crawler.exception.AuthenticationException;
import com.northstar.funding.crawler.exception.ProviderTimeoutException;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * BraveSearch API adapter implementation.
 *
 * BraveSearch provides:
 * - Independent search index (not Google-based)
 * - Privacy-focused search
 * - Web Search API v1 with JSON responses
 *
 * Rate Limit: 50 requests/day (conservative - actual limit may be higher)
 * Timeout: 5 seconds
 */
@Component
@Slf4j
public class BraveSearchAdapter extends AbstractSearchProviderAdapter {

    private final SearchProviderConfig.BraveSearchConfig config;
    private final ObjectMapper objectMapper;

    public BraveSearchAdapter(
            SearchProviderConfig searchProviderConfig,
            ObjectMapper objectMapper
    ) {
        super(
                searchProviderConfig.getBraveSearch().getTimeout() / 1000, // convert ms to seconds
                searchProviderConfig.getBraveSearch().getMaxResults(),
                searchProviderConfig.getBraveSearch().getRateLimit().getDaily()
        );
        this.config = searchProviderConfig.getBraveSearch();
        this.objectMapper = objectMapper;

        log.info("BraveSearchAdapter initialized - baseUrl={}, timeout={}ms, rateLimit={}",
                config.getBaseUrl(), config.getTimeout(), config.getRateLimit().getDaily());
    }

    @Override
    public Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID discoverySessionId) {
        // Validate API key
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return Try.failure(new AuthenticationException("BraveSearch", "API key not configured"));
        }

        return Try.of(() -> {
            // Check and increment rate limit (throws RateLimitException if exceeded)
            incrementUsageAndCheckLimit();
            // Build request URL
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format("%s?q=%s&count=%d",
                    config.getBaseUrl(),
                    encodedQuery,
                    maxResults
            );

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("X-Subscription-Token", config.getApiKey())
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .GET()
                    .build();

            log.debug("Executing BraveSearch query: {}", query);

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new AuthenticationException("BraveSearch", "API key invalid or unauthorized");
            }

            if (response.statusCode() == 429) {
                throw new com.northstar.funding.crawler.exception.RateLimitException(
                        "BraveSearch",
                        config.getRateLimit().getDaily()
                );
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException("BraveSearch API returned status " + response.statusCode());
            }

            // Parse JSON response
            BraveSearchResponse braveResponse = objectMapper.readValue(
                    response.body(),
                    BraveSearchResponse.class
            );

            // Convert to SearchResult entities
            List<SearchResult> results = convertToSearchResults(braveResponse, query, discoverySessionId);

            log.info("BraveSearch completed: {} -> {} results (usage: {}/{})",
                    query, results.size(), getCurrentUsageCount(), getRateLimit());

            return results;

        }).recoverWith(throwable -> {
            if (throwable instanceof java.net.http.HttpTimeoutException) {
                return Try.failure(new ProviderTimeoutException(
                        "BraveSearch",
                        Duration.ofMillis(config.getTimeout())
                ));
            }
            return Try.failure(throwable);
        });
    }

    @Override
    public SearchEngineType getProviderType() {
        return SearchEngineType.BRAVE;
    }

    @Override
    public boolean supportsKeywordQueries() {
        return true;
    }

    @Override
    public boolean supportsAIOptimizedQueries() {
        return false; // BraveSearch is traditional keyword search
    }

    /**
     * Convert BraveSearch API response to SearchResult entities.
     */
    private List<SearchResult> convertToSearchResults(
            BraveSearchResponse response,
            String query,
            UUID discoverySessionId
    ) {
        if (response.getWeb() == null || response.getWeb().getResults() == null) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        return response.getWeb().getResults().stream()
                .map(webResult -> {
                    String domain = normalizeDomain(webResult.getUrl());
                    int position = response.getWeb().getResults().indexOf(webResult) + 1;

                    return SearchResult.builder()
                            .url(webResult.getUrl())
                            .domain(domain)
                            .title(webResult.getTitle() != null ? webResult.getTitle() : "")
                            .description(webResult.getDescription() != null ? webResult.getDescription() : "")
                            .rankPosition(position)
                            .searchEngine(SearchEngineType.BRAVE)
                            .discoveredAt(now)
                            .searchDate(today)
                            .discoverySessionId(discoverySessionId)
                            .build();
                })
                .toList();
    }
}
