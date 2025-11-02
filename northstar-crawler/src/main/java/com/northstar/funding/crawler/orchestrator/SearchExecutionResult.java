package com.northstar.funding.crawler.orchestrator;

import com.northstar.funding.domain.SearchResult;

import java.util.List;

/**
 * Result of multi-provider search execution.
 */
public record SearchExecutionResult(
    List<SearchResult> successfulResults,      // Aggregated results from successful providers
    List<ProviderError> providerErrors,        // Errors from failed providers
    SessionStatistics statistics               // Statistics for DiscoverySession update
) {
    /**
     * Check if search was fully successful (all providers returned results).
     */
    public boolean isFullSuccess() {
        return providerErrors.isEmpty();
    }

    /**
     * Check if search was partially successful (some providers succeeded, some failed).
     */
    public boolean isPartialSuccess() {
        return !successfulResults.isEmpty() && !providerErrors.isEmpty();
    }

    /**
     * Check if search failed completely (all providers failed).
     */
    public boolean isCompleteFailure() {
        return successfulResults.isEmpty() && !providerErrors.isEmpty();
    }
}
