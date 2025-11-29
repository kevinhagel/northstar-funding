package com.northstar.funding.crawler.adapter;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;

import java.util.List;

/**
 * Contract for search provider adapters.
 *
 * Each implementation (BraveSearchAdapter, SearxngAdapter, SerperAdapter, PerplexicaAdapter)
 * must implement this interface to provide standardized search capabilities.
 *
 * Thread Safety: Implementations MUST be thread-safe for Virtual Thread execution.
 * Error Handling: Use Vavr Try monad to encapsulate success/failure states.
 */
public interface SearchProviderAdapter {

    /**
     * Execute a search query and return normalized results.
     *
     * @param query Search query string (keyword or AI-optimized depending on provider)
     * @param maxResults Maximum number of results to return (provider-specific limit)
     * @param discoverySessionId UUID of the discovery session for tracking
     * @return Try containing List of SearchResult entities (empty list if no results, Failure if error)
     *
     * Contract:
     * - MUST return Success<List<SearchResult>> with empty list if no results found
     * - MUST return Failure<Exception> if provider error (network, auth, timeout, etc.)
     * - MUST respect maxResults limit (return at most maxResults items)
     * - MUST normalize all SearchResult entities (domain extraction, lowercase, etc.)
     * - MUST NOT return null (use Try.success(List.of()) for empty results)
     * - MUST set SearchResult.searchEngine to correct SearchEngineType value
     * - MUST set SearchResult.discoveredAt to current timestamp
     * - MUST set SearchResult.position based on provider's result ranking
     * - MUST handle rate limiting (throw RateLimitException if quota exceeded)
     * - MUST handle authentication errors (throw AuthenticationException if API key invalid)
     * - MUST complete within provider-specific timeout (see research.md)
     */
    Try<List<SearchResult>> executeSearch(String query, int maxResults, java.util.UUID discoverySessionId);

    /**
     * Get the search engine type for this adapter.
     *
     * @return SearchEngineType enum value (BRAVE_SEARCH, SEARXNG, SERPER, or PERPLEXICA)
     *
     * Contract:
     * - MUST return non-null SearchEngineType
     * - MUST return same value on every invocation (stateless)
     */
    SearchEngineType getProviderType();

    /**
     * Check if this provider supports keyword queries.
     *
     * Keyword queries are short, focused phrases optimized for traditional search engines.
     * Example: "Bulgaria education infrastructure grants funding opportunities"
     *
     * @return true if provider supports keyword queries, false otherwise
     *
     * Contract:
     * - BraveSearchAdapter MUST return true
     * - SearxngAdapter MUST return true
     * - SerperAdapter MUST return true
     * - PerplexicaAdapter MUST return false (AI-optimized only)
     */
    boolean supportsKeywordQueries();

    /**
     * Check if this provider supports AI-optimized queries.
     *
     * AI-optimized queries are longer, conceptual descriptions with context.
     * Example: "Educational infrastructure funding opportunities for modernizing
     * schools in Bulgaria and Eastern European transition economies"
     *
     * @return true if provider supports AI-optimized queries, false otherwise
     *
     * Contract:
     * - BraveSearchAdapter MUST return false (keyword only)
     * - SearxngAdapter MUST return false (keyword only)
     * - SerperAdapter MUST return false (keyword only)
     * - PerplexicaAdapter MUST return true (AI-optimized)
     */
    boolean supportsAIOptimizedQueries();

    /**
     * Get the provider's current API usage count for rate limiting.
     *
     * Used by Resilience4j RateLimiter to track API quota consumption.
     *
     * @return Number of API calls made in current rate limit window (e.g., daily count)
     *
     * Contract:
     * - MUST return accurate count of API calls made since last rate limit reset
     * - MUST return 0 if rate limit window has not been exceeded
     * - SearxngAdapter returns 0 (no rate limit, self-hosted)
     */
    int getCurrentUsageCount();

    /**
     * Get the provider's configured rate limit (calls per day).
     *
     * @return Maximum allowed API calls per day
     *
     * Contract:
     * - BraveSearchAdapter MUST return 50 (conservative daily limit)
     * - SerperAdapter MUST return 60 (conservative daily limit)
     * - PerplexicaAdapter MUST return 25 (conservative daily limit)
     * - SearxngAdapter MUST return Integer.MAX_VALUE (no limit, self-hosted)
     */
    int getRateLimit();
}
