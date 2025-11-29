package com.northstar.funding.crawler.orchestrator;

import com.northstar.funding.crawler.adapter.SearchProviderAdapter;
import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;

import java.util.List;
import java.util.UUID;

/**
 * Contract for orchestrating parallel search execution across multiple providers.
 *
 * Executes searches across all 4 providers (BraveSearch, SearXNG, Serper, Perplexica)
 * concurrently using Java 25 Virtual Threads, aggregates results, handles partial
 * failures, and updates discovery session statistics.
 */
public interface MultiProviderSearchOrchestrator {

    /**
     * Execute search query across all configured providers in parallel.
     *
     * @param keywordQuery Query for traditional search engines (BraveSearch, SearXNG, Serper)
     * @param aiOptimizedQuery Query for AI-optimized search (Perplexica)
     * @param maxResultsPerProvider Maximum results to request from each provider
     * @param discoverySessionId UUID of discovery session for tracking
     * @return Try containing SearchExecutionResult with aggregated results and errors
     *
     * Contract:
     * - MUST execute all 4 providers in parallel using Virtual Thread executor
     * - MUST use CompletableFuture.allOf() to wait for all providers to complete
     * - MUST continue with successful providers even if some fail (partial success)
     * - MUST apply anti-spam filtering to all results BEFORE domain deduplication
     * - MUST perform domain deduplication using DomainService
     * - MUST update DiscoverySession statistics (totalResultsFound, spamResultsFiltered, etc.)
     * - MUST complete within 10 seconds total timeout (all providers)
     * - MUST return Success<SearchExecutionResult> with partial results if 1-3 providers fail
     * - MUST return Failure<Exception> only if all 4 providers fail
     * - MUST track provider-specific errors for monitoring
     */
    Try<SearchExecutionResult> executeMultiProviderSearch(
        String keywordQuery,
        String aiOptimizedQuery,
        int maxResultsPerProvider,
        UUID discoverySessionId
    );

    /**
     * Execute search on single provider (used internally by orchestrator).
     *
     * @param adapter SearchProviderAdapter instance
     * @param query Search query string
     * @param maxResults Maximum results to request
     * @param discoverySessionId UUID of discovery session
     * @return Try containing List of SearchResult entities
     *
     * Contract:
     * - MUST execute search using adapter.executeSearch()
     * - MUST apply anti-spam filtering to results
     * - MUST handle provider timeout (5-7 seconds depending on provider)
     * - MUST handle rate limiting (RateLimitException)
     * - MUST handle authentication errors (AuthenticationException)
     * - MUST return Success with empty list if provider returns no results
     * - MUST return Failure if provider fails (network, timeout, auth, etc.)
     */
    Try<List<SearchResult>> executeSingleProvider(
        SearchProviderAdapter adapter,
        String query,
        int maxResults,
        UUID discoverySessionId
    );

    /**
     * Aggregate search results from all providers, removing duplicates.
     *
     * @param providerResults Map of SearchEngineType to List<SearchResult>
     * @return Aggregated, deduplicated list of SearchResult entities
     *
     * Contract:
     * - MUST combine results from all providers
     * - MUST deduplicate by domain using DomainService
     * - MUST preserve highest-ranked result per domain (lowest position number)
     * - MUST set isDuplicate flag on SearchResult entities
     * - MUST return empty list if all providers returned empty results
     */
    List<SearchResult> aggregateResults(java.util.Map<SearchEngineType, List<SearchResult>> providerResults);

    /**
     * Update discovery session statistics after search execution.
     *
     * @param sessionId UUID of discovery session
     * @param result SearchExecutionResult with aggregated results and errors
     * @return Updated DiscoverySession entity
     *
     * Contract:
     * - MUST update totalResultsFound (sum of all SearchResult entities created)
     * - MUST update newDomainsDiscovered (count of new Domain entities)
     * - MUST update duplicateDomainsSkipped (count of isDuplicate=true results)
     * - MUST update spamResultsFiltered (count of anti-spam rejections)
     * - MUST update per-provider result counts (braveSearchResults, perplexicaResults, etc.)
     * - MUST set status to COMPLETED if all providers succeeded
     * - MUST set status to PARTIAL_SUCCESS if 1-3 providers failed
     * - MUST set status to FAILED if all providers failed
     * - MUST set completedAt timestamp
     * - MUST save error messages for failed providers
     */
    DiscoverySession updateSessionStatistics(UUID sessionId, SearchExecutionResult result);
}
