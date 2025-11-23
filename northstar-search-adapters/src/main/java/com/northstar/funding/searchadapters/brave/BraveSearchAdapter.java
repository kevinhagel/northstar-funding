package com.northstar.funding.searchadapters.brave;

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
 * Search adapter for Brave Search API.
 *
 * API Documentation: https://api.search.brave.com/
 * Endpoint: GET /res/v1/web/search
 * Authentication: X-Subscription-Token header
 *
 * Response format:
 * {
 *   "web": {
 *     "results": [
 *       {
 *         "url": "https://example.com",
 *         "title": "Page Title",
 *         "description": "Page description"
 *       }
 *     ]
 *   }
 * }
 */
public class BraveSearchAdapter implements SearchAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BraveSearchAdapter.class);

    private final SearchAdapterProperties.BraveConfig config;
    private final WebClient webClient;

    public BraveSearchAdapter(SearchAdapterProperties.BraveConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
            .baseUrl(config.getApiUrl())
            .defaultHeader("X-Subscription-Token", config.getApiKey())
            .build();
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        logger.info("Brave search: query='{}', maxResults={}", query, maxResults);

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("")  // Use empty path since baseUrl already contains full endpoint
                    .queryParam("q", query)
                    .queryParam("count", maxResults)
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();

            return extractResults(response);

        } catch (WebClientResponseException.Unauthorized e) {
            logger.error("Brave authentication failure: {}", e.getMessage());
            throw new SearchAdapterException(
                SearchEngineType.BRAVE,
                query,
                "401 Unauthorized - Invalid API key",
                e
            );
        } catch (WebClientResponseException e) {
            logger.error("Brave API error: status={}, message={}", e.getStatusCode(), e.getMessage());
            throw new SearchAdapterException(
                SearchEngineType.BRAVE,
                query,
                String.format("HTTP %d: %s", e.getStatusCode().value(), e.getMessage()),
                e
            );
        } catch (Exception e) {
            logger.error("Brave search failed: {}", e.getMessage(), e);
            throw new SearchAdapterException(
                SearchEngineType.BRAVE,
                query,
                "Search failed: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.BRAVE;
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    /**
     * Extract search results from Brave API response.
     *
     * @param response API response map
     * @return List of SearchResult (empty if no results)
     */
    @SuppressWarnings("unchecked")
    private List<SearchResult> extractResults(Map<String, Object> response) {
        if (response == null) {
            logger.warn("Brave returned null response");
            return List.of();
        }

        Map<String, Object> web = (Map<String, Object>) response.get("web");
        if (web == null) {
            logger.warn("Brave response missing 'web' field");
            return List.of();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) web.get("results");
        if (results == null || results.isEmpty()) {
            logger.info("Brave returned zero results");
            return List.of();
        }

        LocalDateTime discoveredAt = LocalDateTime.now();

        return results.stream()
            .map(result -> mapToSearchResult(result, discoveredAt))
            .collect(Collectors.toList());
    }

    /**
     * Map Brave API result to SearchResult domain entity.
     *
     * @param result API result map
     * @param discoveredAt Timestamp when discovered
     * @return SearchResult
     */
    private SearchResult mapToSearchResult(Map<String, Object> result, LocalDateTime discoveredAt) {
        return SearchResult.builder()
            .url((String) result.get("url"))
            .title((String) result.get("title"))
            .description((String) result.get("description"))
            .searchEngine(SearchEngineType.BRAVE)
            .discoveredAt(discoveredAt)
            .build();
    }
}
