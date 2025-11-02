package com.northstar.funding.crawler.adapter.tavily;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Tavily AI Search API response structure.
 *
 * Tavily provides AI-optimized search with deep content extraction.
 * https://tavily.com/documentation
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TavilyResponse {

    @JsonProperty("results")
    private List<Result> results;

    @JsonProperty("answer")
    private String answer; // AI-generated answer (optional)

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("title")
        private String title;

        @JsonProperty("url")
        private String url;

        @JsonProperty("content")
        private String content; // Extracted content from page

        @JsonProperty("score")
        private Double score; // Relevance score

        @JsonProperty("published_date")
        private String publishedDate; // When content was published (optional)
    }
}
