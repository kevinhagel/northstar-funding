package com.northstar.funding.searchadapters.searxng;

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
 * Search adapter for SearXNG metasearch engine.
 *
 * SearXNG: https://github.com/searxng/searxng
 * Endpoint: GET /search?q=...&format=json
 * Authentication: None (self-hosted instance)
 *
 * Response format:
 * {
 *   "results": [
 *     {
 *       "url": "https://example.com",
 *       "title": "Page Title",
 *       "content": "Page description"
 *     }
 *   ]
 * }
 */
public class SearXNGAdapter implements SearchAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SearXNGAdapter.class);

    private final SearchAdapterProperties.SearxngConfig config;
    private final WebClient webClient;

    public SearXNGAdapter(SearchAdapterProperties.SearxngConfig config) {
        this.config = config;
        this.webClient = WebClient.builder()
            .baseUrl(config.getApiUrl())
            .build();
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        logger.info("SearXNG search: query='{}', maxResults={}", query, maxResults);

        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search")
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("pageno", "1")
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .block();

            return extractResults(response, maxResults);

        } catch (WebClientResponseException e) {
            logger.error("SearXNG API error: status={}, message={}", e.getStatusCode(), e.getMessage());
            throw new SearchAdapterException(
                SearchEngineType.SEARXNG,
                query,
                String.format("HTTP %d: %s", e.getStatusCode().value(), e.getMessage()),
                e
            );
        } catch (Exception e) {
            logger.error("SearXNG search failed: {}", e.getMessage(), e);
            throw new SearchAdapterException(
                SearchEngineType.SEARXNG,
                query,
                "Search failed: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.SEARXNG;
    }

    @Override
    public boolean isAvailable() {
        // SearXNG doesn't require API key, check if URL is configured
        return config.getApiUrl() != null && !config.getApiUrl().isBlank();
    }

    /**
     * Extract search results from SearXNG API response.
     *
     * @param response API response map
     * @param maxResults Maximum number of results to return
     * @return List of SearchResult (empty if no results)
     */
    @SuppressWarnings("unchecked")
    private List<SearchResult> extractResults(Map<String, Object> response, int maxResults) {
        if (response == null) {
            logger.warn("SearXNG returned null response");
            return List.of();
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            logger.info("SearXNG returned zero results");
            return List.of();
        }

        LocalDateTime discoveredAt = LocalDateTime.now();

        return results.stream()
            .limit(maxResults)
            .map(result -> mapToSearchResult(result, discoveredAt))
            .collect(Collectors.toList());
    }

    /**
     * Map SearXNG API result to SearchResult domain entity.
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
            .searchEngine(SearchEngineType.SEARXNG)
            .discoveredAt(discoveredAt)
            .build();
    }
}
