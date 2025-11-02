package com.northstar.funding.crawler.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.funding.crawler.adapter.serper.SerperResponse;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import com.northstar.funding.crawler.exception.AuthenticationException;
import com.northstar.funding.crawler.exception.ProviderTimeoutException;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serper.dev Google Search API adapter implementation.
 *
 * Serper provides:
 * - Access to Google search results via JSON API
 * - Real-time search data
 * - Cost-effective alternative to Google Custom Search API
 *
 * Rate Limit: 60 requests/day (conservative estimate)
 * Timeout: 5 seconds
 */
@Component
@Slf4j
public class SerperAdapter extends AbstractSearchProviderAdapter {

    private final SearchProviderConfig.SerperConfig config;
    private final ObjectMapper objectMapper;

    public SerperAdapter(
            SearchProviderConfig searchProviderConfig,
            ObjectMapper objectMapper
    ) {
        super(
                searchProviderConfig.getSerper().getTimeout() / 1000, // convert ms to seconds
                searchProviderConfig.getSerper().getMaxResults(),
                searchProviderConfig.getSerper().getRateLimit().getDaily()
        );
        this.config = searchProviderConfig.getSerper();
        this.objectMapper = objectMapper;

        log.info("SerperAdapter initialized - baseUrl={}, timeout={}ms, rateLimit={}",
                config.getBaseUrl(), config.getTimeout(), config.getRateLimit().getDaily());
    }

    @Override
    public Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID discoverySessionId) {
        // Validate API key
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return Try.failure(new AuthenticationException("Serper", "API key not configured"));
        }

        return Try.of(() -> {
            // Check and increment rate limit
            incrementUsageAndCheckLimit();

            // Build request body (Serper uses POST with JSON body)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", query);
            requestBody.put("num", maxResults);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-API-KEY", config.getApiKey())
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.debug("Executing Serper query: {}", query);

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new AuthenticationException("Serper", "API key invalid or unauthorized");
            }

            if (response.statusCode() == 429) {
                throw new com.northstar.funding.crawler.exception.RateLimitException(
                        "Serper",
                        config.getRateLimit().getDaily()
                );
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException("Serper API returned status " + response.statusCode());
            }

            // Parse JSON response
            SerperResponse serperResponse = objectMapper.readValue(
                    response.body(),
                    SerperResponse.class
            );

            // Convert to SearchResult entities
            List<SearchResult> results = convertToSearchResults(serperResponse, query, discoverySessionId);

            log.info("Serper completed: {} -> {} results (usage: {}/{})",
                    query, results.size(), getCurrentUsageCount(), getRateLimit());

            return results;

        }).recoverWith(throwable -> {
            if (throwable instanceof java.net.http.HttpTimeoutException) {
                return Try.failure(new ProviderTimeoutException(
                        "Serper",
                        Duration.ofMillis(config.getTimeout())
                ));
            }
            return Try.failure(throwable);
        });
    }

    @Override
    public SearchEngineType getProviderType() {
        return SearchEngineType.SERPER;
    }

    @Override
    public boolean supportsKeywordQueries() {
        return true;
    }

    @Override
    public boolean supportsAIOptimizedQueries() {
        return false; // Serper is traditional Google search
    }

    /**
     * Convert Serper API response to SearchResult entities.
     */
    private List<SearchResult> convertToSearchResults(
            SerperResponse response,
            String query,
            UUID discoverySessionId
    ) {
        if (response.getOrganic() == null) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        return response.getOrganic().stream()
                .map(result -> {
                    String domain = normalizeDomain(result.getLink());
                    int position = result.getPosition() != null ? result.getPosition() :
                                   response.getOrganic().indexOf(result) + 1;

                    return SearchResult.builder()
                            .url(result.getLink())
                            .domain(domain)
                            .title(result.getTitle() != null ? result.getTitle() : "")
                            .description(result.getSnippet() != null ? result.getSnippet() : "")
                            .rankPosition(position)
                            .searchEngine(SearchEngineType.SERPER)
                            .discoveredAt(now)
                            .searchDate(today)
                            .discoverySessionId(discoverySessionId)
                            .build();
                })
                .toList();
    }
}
