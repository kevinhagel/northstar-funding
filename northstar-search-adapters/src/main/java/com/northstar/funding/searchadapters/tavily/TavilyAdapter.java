package com.northstar.funding.searchadapters.tavily;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.config.SearchAdapterProperties;
import com.northstar.funding.searchadapters.exception.SearchAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Search adapter for Tavily AI Search API.
 *
 * API Documentation: https://tavily.com/
 * Endpoint: POST /search
 * Authentication: Bearer token in Authorization header
 *
 * Response format:
 * {
 *   "results": [
 *     {
 *       "url": "https://example.com",
 *       "title": "Page Title",
 *       "content": "AI-enhanced page description"
 *     }
 *   ]
 * }
 */
public class TavilyAdapter implements SearchAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TavilyAdapter.class);

    private final SearchAdapterProperties.TavilyConfig config;
    private final WebClient webClient;

    public TavilyAdapter(SearchAdapterProperties.TavilyConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
            .baseUrl(config.getApiUrl())
            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        logger.info("Tavily search: query='{}', maxResults={}", query, maxResults);

        try {
            Map<String, Object> requestBody = Map.of(
                "query", query,
                "max_results", maxResults
            );

            Map<String, Object> response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();

            return extractResults(response);

        } catch (WebClientResponseException.Unauthorized e) {
            logger.error("Tavily authentication failure: {}", e.getMessage());
            throw new SearchAdapterException(
                SearchEngineType.TAVILY,
                query,
                "401 Unauthorized - Invalid API key",
                e
            );
        } catch (WebClientResponseException e) {
            logger.error("Tavily API error: status={}, message={}", e.getStatusCode(), e.getMessage());
            throw new SearchAdapterException(
                SearchEngineType.TAVILY,
                query,
                String.format("HTTP %d: %s", e.getStatusCode().value(), e.getMessage()),
                e
            );
        } catch (Exception e) {
            logger.error("Tavily search failed: {}", e.getMessage(), e);
            throw new SearchAdapterException(
                SearchEngineType.TAVILY,
                query,
                "Search failed: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.TAVILY;
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    /**
     * Extract search results from Tavily API response.
     *
     * @param response API response map
     * @return List of SearchResult (empty if no results)
     */
    @SuppressWarnings("unchecked")
    private List<SearchResult> extractResults(Map<String, Object> response) {
        if (response == null) {
            logger.warn("Tavily returned null response");
            return List.of();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            logger.info("Tavily returned zero results");
            return List.of();
        }

        LocalDateTime discoveredAt = LocalDateTime.now();

        return results.stream()
            .map(result -> mapToSearchResult(result, discoveredAt))
            .collect(Collectors.toList());
    }

    /**
     * Map Tavily API result to SearchResult domain entity.
     *
     * @param result API result map
     * @param discoveredAt Timestamp when discovered
     * @return SearchResult
     */
    private SearchResult mapToSearchResult(Map<String, Object> result, LocalDateTime discoveredAt) {
        return SearchResult.builder()
            .url((String) result.get("url"))
            .title((String) result.get("title"))
            .description((String) result.get("content"))
            .searchEngine(SearchEngineType.TAVILY)
            .discoveredAt(discoveredAt)
            .build();
    }
}
