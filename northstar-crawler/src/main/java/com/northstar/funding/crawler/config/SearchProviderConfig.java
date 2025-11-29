package com.northstar.funding.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for search provider adapters.
 *
 * Externalized configuration following research.md decision to use
 * Spring Boot @ConfigurationProperties instead of environment variables
 * or Consul/Vault for single-user Mac Studio deployment.
 */
@Data
@Component
@ConfigurationProperties(prefix = "search.providers")
public class SearchProviderConfig {

    private BraveSearchConfig braveSearch = new BraveSearchConfig();
    private SerperConfig serper = new SerperConfig();
    private SearxngConfig searxng = new SearxngConfig();
    private PerplexicaConfig perplexica = new PerplexicaConfig();

    /**
     * BraveSearch configuration (keyword queries).
     */
    @Data
    public static class BraveSearchConfig {
        private String apiKey;
        private String baseUrl = "https://api.search.brave.com/res/v1/web/search";
        private int timeout = 5000;  // 5 seconds (milliseconds)
        private int maxResults = 20;
        private RateLimit rateLimit = new RateLimit(50);  // Conservative: 50/day
    }

    /**
     * Serper (Google Search API) configuration (keyword queries).
     */
    @Data
    public static class SerperConfig {
        private String apiKey;
        private String baseUrl = "https://google.serper.dev/search";
        private int timeout = 5000;  // 5 seconds (milliseconds)
        private int maxResults = 20;
        private RateLimit rateLimit = new RateLimit(60);  // Conservative: 60/day
    }

    /**
     * SearXNG configuration (self-hosted metasearch, keyword queries).
     */
    @Data
    public static class SearxngConfig {
        private String baseUrl = "http://192.168.1.10:8080/search";
        private int timeout = 7000;  // 7 seconds (aggregates multiple engines)
        private int maxResults = 20;
        private String format = "json";
        private RateLimit rateLimit = new RateLimit(Integer.MAX_VALUE);  // Unlimited (self-hosted)
    }

    /**
     * Perplexica configuration (self-hosted AI search with LM Studio).
     */
    @Data
    public static class PerplexicaConfig {
        private String baseUrl = "http://192.168.1.10:3001/api/search";
        private int timeout = 15000;  // 15 seconds (LM Studio + SearXNG processing)
        private int maxResults = 20;
        private String focusMode = "webSearch";  // Perplexica focus mode
        private String optimizationMode = "balanced";  // Perplexica optimization mode
        private RateLimit rateLimit = new RateLimit(Integer.MAX_VALUE);  // Unlimited (self-hosted)
    }

    /**
     * Rate limiting configuration.
     */
    @Data
    public static class RateLimit {
        private int daily;  // Daily quota

        public RateLimit() {
            this.daily = 0;
        }

        public RateLimit(int daily) {
            this.daily = daily;
        }
    }
}
