package com.northstar.funding.crawler.adapter.perplexica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Perplexica AI Search API response structure.
 *
 * Perplexica provides self-hosted AI-powered search with:
 * - AI-generated answers
 * - Source citations with metadata
 * - Integration with local LLM (LM Studio)
 *
 * API: POST http://192.168.1.10:3001/api/search
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PerplexicaResponse {

    @JsonProperty("message")
    private String message; // AI-generated answer

    @JsonProperty("sources")
    private List<Source> sources;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source {
        @JsonProperty("pageContent")
        private String pageContent; // Extracted content snippet

        @JsonProperty("metadata")
        private Metadata metadata;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        @JsonProperty("title")
        private String title;

        @JsonProperty("url")
        private String url;
    }
}
