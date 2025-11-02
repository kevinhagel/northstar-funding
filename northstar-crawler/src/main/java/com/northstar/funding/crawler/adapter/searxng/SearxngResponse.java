package com.northstar.funding.crawler.adapter.searxng;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * SearXNG API response structure.
 *
 * SearXNG is a privacy-respecting metasearch engine that aggregates results
 * from multiple search engines.
 *
 * Response format: JSON with "results" array
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearxngResponse {

    @JsonProperty("results")
    private List<Result> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("title")
        private String title;

        @JsonProperty("url")
        private String url;

        @JsonProperty("content")
        private String content;

        @JsonProperty("engine")
        private String engine; // Which search engine provided this result

        @JsonProperty("score")
        private Double score; // Relevance score (optional)
    }
}
