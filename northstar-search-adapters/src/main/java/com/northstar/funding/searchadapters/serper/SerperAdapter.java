package com.northstar.funding.searchadapters.serper;

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
 * Search adapter for Serper.dev (Google Search API).
 *
 * API Documentation: https://serper.dev/
 * Endpoint: POST /search
 * Authentication: X-API-KEY header
 *
 * Response format:
 * {
 *   "organic": [
 *     {
 *       "link": "https://example.com",
 *       "title": "Page Title",
 *       "snippet": "Page description"
 *     }
 *   ]
 * }
 */
public class SerperAdapter implements SearchAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SerperAdapter.class);

    private final SearchAdapterProperties.SerperConfig config;
    private final WebClient webClient;

    public SerperAdapter(SearchAdapterProperties.SerperConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
            .baseUrl(config.getApiUrl())
            .defaultHeader("X-API-KEY", config.getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        logger.info("Serper search: query='{}', maxResults={}", query, maxResults);

        try {
            Map<String, Object> requestBody = Map.of(
                "q", query,
                "num", maxResults
            );

            Map<String, Object> response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();

            return extractResults(response);

        } catch (WebClientResponseException.Unauthorized e) {
            logger.error("Serper authentication failure: {}", e.getMessage());
            throw new SearchAdapterException(
                SearchEngineType.SERPER,
                query,
                "401 Unauthorized - Invalid API key",
                e
            );
        } catch (WebClientResponseException e) {
            logger.error("Serper API error: status={}, message={}", e.getStatusCode(), e.getMessage());
            throw new SearchAdapterException(
                SearchEngineType.SERPER,
                query,
                String.format("HTTP %d: %s", e.getStatusCode().value(), e.getMessage()),
                e
            );
        } catch (Exception e) {
            logger.error("Serper search failed: {}", e.getMessage(), e);
            throw new SearchAdapterException(
                SearchEngineType.SERPER,
                query,
                "Search failed: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.SERPER;
    }

    @Override
    public boolean isAvailable() {
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    /**
     * Extract search results from Serper API response.
     *
     * @param response API response map
     * @return List of SearchResult (empty if no results)
     */
    @SuppressWarnings("unchecked")
    private List<SearchResult> extractResults(Map<String, Object> response) {
        if (response == null) {
            logger.warn("Serper returned null response");
            return List.of();
        }

        List<Map<String, Object>> organic = (List<Map<String, Object>>) response.get("organic");
        if (organic == null || organic.isEmpty()) {
            logger.info("Serper returned zero results");
            return List.of();
        }

        LocalDateTime discoveredAt = LocalDateTime.now();

        return organic.stream()
            .map(result -> mapToSearchResult(result, discoveredAt))
            .collect(Collectors.toList());
    }

    /**
     * Map Serper API result to SearchResult domain entity.
     *
     * @param result API result map
     * @param discoveredAt Timestamp when discovered
     * @return SearchResult
     */
    private SearchResult mapToSearchResult(Map<String, Object> result, LocalDateTime discoveredAt) {
        return SearchResult.builder()
            .url((String) result.get("link"))
            .title((String) result.get("title"))
            .description((String) result.get("snippet"))
            .searchEngine(SearchEngineType.SERPER)
            .discoveredAt(discoveredAt)
            .build();
    }
}
