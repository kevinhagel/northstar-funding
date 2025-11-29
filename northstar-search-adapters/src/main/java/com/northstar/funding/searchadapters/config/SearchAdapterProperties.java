package com.northstar.funding.searchadapters.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration properties for all search adapters.
 *
 * Maps to application.yml prefix: search-adapters
 */
@Data
@Component
@ConfigurationProperties(prefix = "search-adapters")
public class SearchAdapterProperties {

    private BraveConfig brave = new BraveConfig();
    private SerperConfig serper = new SerperConfig();
    private SearxngConfig searxng = new SearxngConfig();

    @Data
    public static class BraveConfig {
        private String apiKey;
        private String apiUrl = "https://api.search.brave.com/res/v1/web/search";
        private int timeoutSeconds = 10;
    }

    @Data
    public static class SerperConfig {
        private String apiKey;
        private String apiUrl = "https://google.serper.dev/search";
        private int timeoutSeconds = 10;
    }

    @Data
    public static class SearxngConfig {
        private String apiUrl = "http://192.168.1.10:8080";
        private int timeoutSeconds = 10;
    }
}
