package com.northstar.funding.crawler.adapter.brave;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * BraveSearch API response structure.
 *
 * Based on BraveSearch Web Search API v1 documentation.
 * https://api.search.brave.com/app/documentation/web-search/get-started
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BraveSearchResponse {

    @JsonProperty("web")
    private WebResults web;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebResults {
        @JsonProperty("results")
        private List<WebResult> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebResult {
        @JsonProperty("title")
        private String title;

        @JsonProperty("url")
        private String url;

        @JsonProperty("description")
        private String description;

        @JsonProperty("page_age")
        private String pageAge;

        @JsonProperty("profile")
        private Profile profile;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        @JsonProperty("name")
        private String name;

        @JsonProperty("url")
        private String url;
    }
}
