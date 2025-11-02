package com.northstar.funding.crawler.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.funding.crawler.adapter.tavily.TavilyResponse;
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
 * Tavily AI Search API adapter implementation.
 *
 * Tavily provides:
 * - AI-optimized search with semantic understanding
 * - Deep content extraction (raw content access)
 * - Educational domain prioritization
 * - AI-generated answers for relevance assessment
 *
 * Rate Limit: 25 requests/day (conservative - Tavily is AI-powered and more expensive)
 * Timeout: 6 seconds (slightly higher due to AI processing)
 */
@Component
@Slf4j
public class TavilyAdapter extends AbstractSearchProviderAdapter {

    private final SearchProviderConfig.TavilyConfig config;
    private final ObjectMapper objectMapper;

    public TavilyAdapter(
            SearchProviderConfig searchProviderConfig,
            ObjectMapper objectMapper
    ) {
        super(
                searchProviderConfig.getTavily().getTimeout() / 1000, // convert ms to seconds
                searchProviderConfig.getTavily().getMaxResults(),
                searchProviderConfig.getTavily().getRateLimit().getDaily()
        );
        this.config = searchProviderConfig.getTavily();
        this.objectMapper = objectMapper;

        log.info("TavilyAdapter initialized - baseUrl={}, timeout={}ms, rateLimit={}, searchDepth={}",
                config.getBaseUrl(), config.getTimeout(), config.getRateLimit().getDaily(), config.getSearchDepth());
    }

    @Override
    public Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID discoverySessionId) {
        // Validate API key
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return Try.failure(new AuthenticationException("Tavily", "API key not configured"));
        }

        return Try.of(() -> {
            // Check and increment rate limit
            incrementUsageAndCheckLimit();

            // Build request body (Tavily uses POST with JSON body)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("api_key", config.getApiKey());
            requestBody.put("query", query);
            requestBody.put("max_results", maxResults);
            requestBody.put("search_depth", config.getSearchDepth());
            requestBody.put("include_answer", true);
            requestBody.put("include_raw_content", false); // Phase 1 uses metadata only

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.debug("Executing Tavily query: {}", query);

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new AuthenticationException("Tavily", "API key invalid or unauthorized");
            }

            if (response.statusCode() == 429) {
                throw new com.northstar.funding.crawler.exception.RateLimitException(
                        "Tavily",
                        config.getRateLimit().getDaily()
                );
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException("Tavily API returned status " + response.statusCode());
            }

            // Parse JSON response
            TavilyResponse tavilyResponse = objectMapper.readValue(
                    response.body(),
                    TavilyResponse.class
            );

            // Convert to SearchResult entities
            List<SearchResult> results = convertToSearchResults(tavilyResponse, query, discoverySessionId);

            log.info("Tavily completed: {} -> {} results (usage: {}/{}, answer: {})",
                    query, results.size(), getCurrentUsageCount(), getRateLimit(),
                    tavilyResponse.getAnswer() != null ? "yes" : "no");

            return results;

        }).recoverWith(throwable -> {
            if (throwable instanceof java.net.http.HttpTimeoutException) {
                return Try.failure(new ProviderTimeoutException(
                        "Tavily",
                        Duration.ofMillis(config.getTimeout())
                ));
            }
            return Try.failure(throwable);
        });
    }

    @Override
    public SearchEngineType getProviderType() {
        return SearchEngineType.TAVILY;
    }

    @Override
    public boolean supportsKeywordQueries() {
        return true; // Tavily accepts both keyword and natural language queries
    }

    @Override
    public boolean supportsAIOptimizedQueries() {
        return true; // Tavily is specifically designed for AI-optimized queries
    }

    /**
     * Convert Tavily API response to SearchResult entities.
     */
    private List<SearchResult> convertToSearchResults(
            TavilyResponse response,
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
                            .searchEngine(SearchEngineType.TAVILY)
                            .discoveredAt(now)
                            .searchDate(today)
                            .discoverySessionId(discoverySessionId)
                            .build();
                })
                .toList();
    }
}
