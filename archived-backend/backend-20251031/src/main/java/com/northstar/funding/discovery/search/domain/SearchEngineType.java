package com.northstar.funding.discovery.search.domain;

import java.util.Objects;

/**
 * Search engine types supported by the discovery system (Feature 003)
 *
 * Each engine has different characteristics:
 * - SEARXNG: Self-hosted meta-search engine (no API key required)
 * - TAVILY: AI-optimized search API (requires API key)
 * - PERPLEXITY: AI-enhanced search with reasoning (requires API key)
 * - BROWSERBASE: Browser automation service (requires API key, deferred to Phase 2)
 *
 * Constitutional Compliance:
 * - Enum with properties and methods
 * - Defensive programming for invalid inputs
 * - Configuration-friendly (base URLs, API key requirements)
 */
public enum SearchEngineType {
    /**
     * Searxng self-hosted meta-search engine
     * - Hosted on Mac Studio (192.168.1.10:8080)
     * - No API key required
     * - Returns aggregated results from multiple engines
     */
    SEARXNG(
        "Searxng",
        false,
        "http://192.168.1.10:8080"
    ),

    /**
     * Browserbase browser automation service
     * - API-based browser automation
     * - Requires API key
     * - DEFERRED to Phase 2 (complex implementation)
     */
    BROWSERBASE(
        "Browserbase",
        true,
        "https://api.browserbase.com"
    ),

    /**
     * Tavily AI-optimized search API
     * - Optimized for AI/LLM applications
     * - Requires API key
     * - Returns clean, structured results
     */
    TAVILY(
        "Tavily",
        true,
        "https://api.tavily.com"
    ),

    /**
     * Perplexity AI-enhanced search with reasoning
     * - AI-powered search with citations
     * - Requires API key
     * - Returns results with reasoning and sources
     */
    PERPLEXITY(
        "Perplexity",
        true,
        "https://api.perplexity.ai"
    );

    private final String displayName;
    private final boolean requiresApiKey;
    private final String defaultBaseUrl;

    SearchEngineType(String displayName, boolean requiresApiKey, String defaultBaseUrl) {
        this.displayName = displayName;
        this.requiresApiKey = requiresApiKey;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    /**
     * Get user-friendly display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this engine requires an API key
     */
    public boolean requiresApiKey() {
        return requiresApiKey;
    }

    /**
     * Get default base URL for this engine
     */
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * Parse search engine type from string (case-insensitive)
     *
     * @param value The string value to parse
     * @return The corresponding SearchEngineType
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank or unknown
     */
    public static SearchEngineType fromString(String value) {
        Objects.requireNonNull(value, "value cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be blank");
        }

        try {
            return SearchEngineType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("Unknown search engine type: %s", value),
                e
            );
        }
    }
}
