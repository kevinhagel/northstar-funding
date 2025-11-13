package com.northstar.funding.search.searxng;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.search.adapter.SearchAdapter;
import com.northstar.funding.search.config.SearchAdapterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Search adapter for SearXNG metasearch engine.
 *
 * <p>SearXNG API documentation: https://docs.searxng.org/dev/search_api.html
 *
 * <p>Configuration:
 * <ul>
 *   <li>Base URL: http://192.168.1.10:8080 (Mac Studio)</li>
 *   <li>Format: JSON</li>
 *   <li>Timeout: 10 seconds</li>
 * </ul>
 */
@Service
public class SearXNGAdapter implements SearchAdapter {

    private static final Logger log = LoggerFactory.getLogger(SearXNGAdapter.class);

    private final RestTemplate restTemplate;
    private final SearchAdapterConfig config;
    private final ObjectMapper objectMapper;

    public SearXNGAdapter(RestTemplate restTemplate, SearchAdapterConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        log.info("🔍 Executing SearXNG search: query='{}', maxResults={}", query, maxResults);

        try {
            String url = buildSearchUrl(query);
            String jsonResponse = restTemplate.getForObject(url, String.class);

            return parseResults(jsonResponse, maxResults);

        } catch (Exception e) {
            log.error("❌ SearXNG search failed: {}", e.getMessage());
            throw new RuntimeException("SearXNG search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.SEARXNG;
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = buildSearchUrl("test");
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            log.warn("⚠️ SearXNG availability check failed: {}", e.getMessage());
            return false;
        }
    }

    private String buildSearchUrl(String query) {
        return UriComponentsBuilder.fromHttpUrl(config.getSearxngBaseUrl() + "/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("pageno", 1)
                .toUriString();
    }

    private List<SearchResult> parseResults(String jsonResponse, int maxResults) throws Exception {
        List<SearchResult> results = new ArrayList<>();

        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode resultsArray = root.get("results");

        if (resultsArray == null || !resultsArray.isArray()) {
            log.warn("⚠️ No results array in SearXNG response");
            return results;
        }

        int count = 0;
        for (JsonNode resultNode : resultsArray) {
            if (count >= maxResults) {
                break;
            }

            String url = resultNode.has("url") ? resultNode.get("url").asText() : "";
            String title = resultNode.has("title") ? resultNode.get("title").asText() : "";
            String description = resultNode.has("content") ? resultNode.get("content").asText() : "";

            if (!url.isEmpty()) {
                results.add(new SearchResult(url, title, description));
                count++;
            }
        }

        log.info("✅ Parsed {} results from SearXNG", results.size());
        return results;
    }
}
