package com.northstar.funding.crawler.adapter.serper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Serper.dev Google Search API response structure.
 *
 * Serper provides access to Google search results via JSON API.
 * https://serper.dev/documentation
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SerperResponse {

    @JsonProperty("organic")
    private List<OrganicResult> organic;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrganicResult {
        @JsonProperty("title")
        private String title;

        @JsonProperty("link")
        private String link;

        @JsonProperty("snippet")
        private String snippet;

        @JsonProperty("position")
        private Integer position;

        @JsonProperty("date")
        private String date; // Last updated date (optional)
    }
}
