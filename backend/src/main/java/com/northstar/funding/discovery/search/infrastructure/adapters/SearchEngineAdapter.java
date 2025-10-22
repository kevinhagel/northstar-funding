package com.northstar.funding.discovery.search.infrastructure.adapters;

import com.northstar.funding.discovery.search.domain.SearchEngineType;
import io.vavr.control.Try;

import java.util.List;

/**
 * Unified interface for all search engine adapters (Feature 003)
 *
 * Adapter Pattern: Hides engine-specific APIs behind unified interface.
 * All search engines (Searxng, Tavily, Perplexity, Browserbase) implement this contract.
 *
 * Design Principles:
 * - Vavr Try<T> for functional error handling (no exceptions thrown)
 * - Circuit breakers via Resilience4j annotations on implementations
 * - Virtual Threads for parallel execution across multiple engines
 * - Immutable SearchResult DTOs
 *
 * Constitutional Compliance:
 * - NO langchain4j (using Spring RestClient)
 * - Vavr 0.10.7 for functional error handling
 * - Resilience4j for circuit breakers and retry logic
 *
 * @author NorthStar Funding Team
 */
public interface SearchEngineAdapter {

    /**
     * Execute a search query against this search engine
     *
     * Returns Try<List<SearchResult>> to handle:
     * - Success: List of search results (may be empty)
     * - Failure: Network errors, timeouts, circuit breaker trips, API errors
     *
     * @param query The search query text
     * @param maxResults Maximum number of results to return (1-100)
     * @return Try containing list of search results or error
     */
    Try<List<SearchResult>> search(String query, int maxResults);

    /**
     * Get the search engine type for this adapter
     *
     * @return The engine type (SEARXNG, TAVILY, PERPLEXITY, BROWSERBASE)
     */
    SearchEngineType getEngineType();

    /**
     * Check if this search engine is currently enabled
     * Allows runtime enable/disable of engines via configuration
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Perform health check on this search engine
     * Used for monitoring and circuit breaker state tracking
     *
     * @return HealthStatus with current state
     */
    HealthStatus checkHealth();

    /**
     * Search result DTO (shared across all engines)
     */
    record SearchResult(
        String url,
        String title,
        String snippet,
        SearchEngineType source,
        String originatingQuery,
        int resultPosition,
        java.time.Instant timestamp
    ) {}

    /**
     * Health status for search engine monitoring
     */
    record HealthStatus(
        SearchEngineType engine,
        boolean isHealthy,
        java.time.Instant lastChecked,
        String circuitBreakerState,
        String errorMessage
    ) {}
}
