package com.northstar.funding.discovery.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for search engine integrations.
 *
 * Supports multiple search engines:
 * - Searxng (self-hosted on Mac Studio)
 * - Browserbase (API-based)
 * - Tavily (API-based)
 * - Perplexity (API-based)
 */
@Component
@ConfigurationProperties(prefix = "search")
@Data
@ToString
@Slf4j
public class SearchEngineProperties {

    private Map<String, EngineConfig> engines = new HashMap<>();

    @Data
    @ToString
    public static class EngineConfig {
        /**
         * Whether this search engine is enabled.
         */
        private boolean enabled = true;

        /**
         * Base URL for the search engine API.
         */
        private String baseUrl;

        /**
         * API key (if required).
         */
        private String apiKey;

        /**
         * Maximum number of results to return.
         */
        private int maxResults = 25;

        /**
         * Request timeout in seconds.
         */
        private int timeoutSeconds = 10;

        /**
         * Check if this engine is properly configured.
         */
        public boolean isConfigured() {
            return baseUrl != null && !baseUrl.isBlank();
        }

        /**
         * Check if API key is required and configured.
         */
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    @PostConstruct
    public void logConfiguration() {
        log.info("🔧 Search Engine Configuration:");
        engines.forEach((name, config) -> {
            log.info("  - {}: enabled={}, baseUrl={}, hasApiKey={}",
                name, config.enabled, config.baseUrl, config.hasApiKey());
        });

        validateConfiguration();
    }

    private void validateConfiguration() {
        if (engines.isEmpty()) {
            log.warn("⚠️ No search engines configured!");
        }

        engines.forEach((name, config) -> {
            if (config.enabled && !config.isConfigured()) {
                log.warn("⚠️ {} is enabled but not properly configured (missing baseUrl)", name);
            }
        });

        log.info("✅ Search engine configuration validated");
    }

    /**
     * Get configuration for Searxng (self-hosted).
     */
    public EngineConfig getSearxng() {
        return engines.getOrDefault("searxng", new EngineConfig());
    }

    /**
     * Get configuration for Browserbase.
     */
    public EngineConfig getBrowserbase() {
        return engines.getOrDefault("browserbase", new EngineConfig());
    }

    /**
     * Get configuration for Tavily.
     */
    public EngineConfig getTavily() {
        return engines.getOrDefault("tavily", new EngineConfig());
    }

    /**
     * Get configuration for Perplexity.
     */
    public EngineConfig getPerplexity() {
        return engines.getOrDefault("perplexity", new EngineConfig());
    }

    /**
     * Check if any search engine is configured.
     */
    public boolean hasAnyEngineConfigured() {
        return engines.values().stream()
            .anyMatch(config -> config.enabled && config.isConfigured());
    }
}
