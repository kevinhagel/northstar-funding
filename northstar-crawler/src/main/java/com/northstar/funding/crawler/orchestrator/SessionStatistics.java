package com.northstar.funding.crawler.orchestrator;

/**
 * Statistics for updating DiscoverySession.
 */
public record SessionStatistics(
    int totalResultsFound,            // Total SearchResult entities created
    int newDomainsDiscovered,         // New Domain entities created
    int duplicateDomainsSkipped,      // Existing domains (isDuplicate=true)
    int spamResultsFiltered,          // Anti-spam rejections

    // Per-provider counts
    int braveSearchResults,
    int searxngResults,
    int serperResults,
    int tavilyResults
) {
    /**
     * Calculate total queries executed (1 per provider with results + errors).
     */
    public int getTotalQueriesExecuted() {
        int providersWithResults = 0;
        if (braveSearchResults > 0) providersWithResults++;
        if (searxngResults > 0) providersWithResults++;
        if (serperResults > 0) providersWithResults++;
        if (tavilyResults > 0) providersWithResults++;
        return providersWithResults;
    }
}
