package com.northstar.funding.crawler.processing;

import lombok.Builder;
import lombok.Data;

/**
 * Statistics tracking for search result processing operations.
 *
 * Tracks counts during search result processing:
 * - Total results received
 * - Spam TLDs filtered out
 * - Blacklisted domains skipped
 * - Duplicate domains skipped
 * - High-confidence candidates created (PENDING_CRAWL)
 * - Low-confidence candidates created (SKIPPED_LOW_CONFIDENCE)
 *
 * Used for session analytics and monitoring.
 */
@Data
@Builder
public class ProcessingStatistics {

    /**
     * Total search results received for processing
     */
    private int totalResults;

    /**
     * Results filtered due to spam TLDs (Tier 5)
     */
    private int spamTldFiltered;

    /**
     * Results skipped due to blacklisted domains
     */
    private int blacklistedSkipped;

    /**
     * Results skipped due to duplicate domains
     */
    private int duplicatesSkipped;

    /**
     * High-confidence candidates created (>= 0.60 confidence)
     */
    private int highConfidenceCreated;

    /**
     * Low-confidence candidates created (< 0.60 confidence)
     */
    private int lowConfidenceCreated;

    /**
     * Results skipped due to invalid URLs (domain extraction failed)
     */
    private int invalidUrlsSkipped;

    /**
     * Get total candidates created (high + low confidence)
     *
     * @return total candidates created
     */
    public int getTotalCandidatesCreated() {
        return highConfidenceCreated + lowConfidenceCreated;
    }

    /**
     * Get total results processed (skipped + created)
     *
     * @return total results processed
     */
    public int getTotalProcessed() {
        return spamTldFiltered + blacklistedSkipped + duplicatesSkipped + getTotalCandidatesCreated();
    }
}
