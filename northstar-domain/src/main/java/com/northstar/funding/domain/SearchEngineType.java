package com.northstar.funding.domain;

/**
 * Search Engine Type Enum
 *
 * Supported search engines for Phase 1 metadata discovery.
 * All engines provide title/description metadata for judging.
 *
 * API Requirements:
 * - BRAVE: Brave Search API (requires API key)
 * - SEARXNG: Self-hosted @ http://192.168.1.10:8080 (no API key)
 * - SERPER: Serper.dev Google Search API (requires API key)
 * - TAVILY: Tavily API (requires API key, AI-optimized)
 */
public enum SearchEngineType {

    /**
     * Brave Search API
     * Privacy-focused search engine
     */
    BRAVE,

    /**
     * SearXNG metasearch engine
     * Self-hosted, no API key required
     * Aggregates results from multiple sources
     */
    SEARXNG,

    /**
     * Serper.dev Google Search API
     * Google search results via API
     */
    SERPER,

    /**
     * Tavily Search API
     * AI-optimized search for LLM applications
     * Best paired with AI-generated prompts (not keywords)
     */
    TAVILY
}
