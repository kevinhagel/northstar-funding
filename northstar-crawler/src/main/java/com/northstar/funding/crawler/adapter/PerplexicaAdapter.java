package com.northstar.funding.crawler.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.funding.crawler.adapter.perplexica.PerplexicaResponse;
import com.northstar.funding.crawler.config.SearchProviderConfig;
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
 * Perplexica AI Search adapter implementation.
 *
 * Perplexica provides:
 * - Self-hosted AI-powered search (no API costs)
 * - Integration with LM Studio for local LLM processing
 * - AI-generated answers with source citations
 * - Web search via SearXNG backend
 *
 * Rate Limit: Unlimited (self-hosted)
 * Timeout: 15 seconds (LM Studio + SearXNG processing)
 */
@Component
@Slf4j
public class PerplexicaAdapter extends AbstractSearchProviderAdapter {

    private final SearchProviderConfig.PerplexicaConfig config;
    private final ObjectMapper objectMapper;

    public PerplexicaAdapter(
            SearchProviderConfig searchProviderConfig,
            ObjectMapper objectMapper
    ) {
        super(
                searchProviderConfig.getPerplexica().getTimeout() / 1000, // convert ms to seconds
                searchProviderConfig.getPerplexica().getMaxResults(),
                searchProviderConfig.getPerplexica().getRateLimit().getDaily()
        );
        this.config = searchProviderConfig.getPerplexica();
        this.objectMapper = objectMapper;

        log.info("PerplexicaAdapter initialized - baseUrl={}, timeout={}ms, focusMode={}",
                config.getBaseUrl(), config.getTimeout(), config.getFocusMode());
    }

    @Override
    public Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID discoverySessionId) {
        return Try.of(() -> {
            // Check and increment rate limit (unlimited for self-hosted, but keep for consistency)
            incrementUsageAndCheckLimit();

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("focusMode", config.getFocusMode());
            requestBody.put("optimizationMode", config.getOptimizationMode());

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(config.getTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.debug("Executing Perplexica query: {} (focusMode={}, optimizationMode={})",
                    query, config.getFocusMode(), config.getOptimizationMode());

            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() != 200) {
                throw new RuntimeException("Perplexica API returned status " + response.statusCode()
                        + ": " + response.body());
            }

            // Parse JSON response
            PerplexicaResponse perplexicaResponse = objectMapper.readValue(
                    response.body(),
                    PerplexicaResponse.class
            );

            // Convert to SearchResult entities
            List<SearchResult> results = convertToSearchResults(perplexicaResponse, query, discoverySessionId, maxResults);

            log.info("Perplexica completed: {} -> {} results (answer: {})",
                    query, results.size(),
                    perplexicaResponse.getMessage() != null ? "yes" : "no");

            return results;

        }).recoverWith(throwable -> {
            if (throwable instanceof java.net.http.HttpTimeoutException) {
                return Try.failure(new ProviderTimeoutException(
                        "Perplexica",
                        Duration.ofMillis(config.getTimeout())
                ));
            }
            return Try.failure(throwable);
        });
    }

    @Override
    public SearchEngineType getProviderType() {
        return SearchEngineType.PERPLEXICA;
    }

    @Override
    public boolean supportsKeywordQueries() {
        return true; // Perplexica handles both keyword and natural language
    }

    @Override
    public boolean supportsAIOptimizedQueries() {
        return true; // Perplexica is specifically designed for AI-optimized queries
    }

    /**
     * Convert Perplexica API response to SearchResult entities.
     */
    private List<SearchResult> convertToSearchResults(
            PerplexicaResponse response,
            String query,
            UUID discoverySessionId,
            int maxResults
    ) {
        if (response.getSources() == null) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        return response.getSources().stream()
                .limit(maxResults)
                .filter(source -> source.getMetadata() != null && source.getMetadata().getUrl() != null)
                .map(source -> {
                    String url = source.getMetadata().getUrl();
                    String domain = normalizeDomain(url);
                    int position = response.getSources().indexOf(source) + 1;

                    String title = source.getMetadata().getTitle() != null
                            ? source.getMetadata().getTitle()
                            : "";
                    String description = source.getPageContent() != null
                            ? source.getPageContent()
                            : "";

                    return SearchResult.builder()
                            .url(url)
                            .domain(domain)
                            .title(title)
                            .description(description)
                            .rankPosition(position)
                            .searchEngine(SearchEngineType.PERPLEXICA)
                            .discoveredAt(now)
                            .searchDate(today)
                            .discoverySessionId(discoverySessionId)
                            .build();
                })
                .toList();
    }
}
