package com.northstar.funding.crawler.processing;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates all processing state for a single search result batch.
 * <p>
 * This domain object eliminates primitive obsession by managing:
 * - Session correlation (UUID)
 * - Confidence threshold (constant)
 * - Domain deduplication tracking (HashSet)
 * - Processing outcome counters (spam, blacklist, duplicates, candidates)
 * <p>
 * Responsibilities:
 * - Track session ID for logging correlation
 * - Maintain confidence threshold (0.60 constant)
 * - Track seen domains for deduplication
 * - Count processing outcomes (spam, blacklist, duplicates, candidates, invalid URLs)
 * - Generate final ProcessingStatistics
 */
public class ProcessingContext {
    private final UUID sessionId;
    private final BigDecimal confidenceThreshold;
    private final Set<String> seenDomains;
    private int spamTldFiltered;
    private int blacklistedSkipped;
    private int duplicatesSkipped;
    private int highConfidenceCreated;
    private int lowConfidenceCreated;
    private int invalidUrlsSkipped;

    /**
     * Creates a new processing context for a search result batch.
     *
     * @param sessionId Session UUID for correlation in logging and persistence
     */
    public ProcessingContext(UUID sessionId) {
        this.sessionId = sessionId;
        this.confidenceThreshold = new BigDecimal("0.60");
        this.seenDomains = new HashSet<>();
        // All counters initialized to 0 by default
    }

    /**
     * Registers a domain as seen, detecting duplicates.
     * <p>
     * Uses single HashSet operation (Set.add) which returns false if element already exists.
     * This is more efficient than using contains() + add() (two operations).
     *
     * @param domain Domain name to register
     * @return true if unique (first time), false if duplicate
     */
    public boolean markDomainAsSeen(String domain) {
        if (!seenDomains.add(domain)) {
            duplicatesSkipped++;
            return false;  // Duplicate
        }
        return true;  // Unique
    }

    /**
     * Records that a spam TLD was filtered.
     */
    public void recordSpamTldFiltered() {
        spamTldFiltered++;
    }

    /**
     * Records that a blacklisted domain was skipped.
     */
    public void recordBlacklisted() {
        blacklistedSkipped++;
    }

    /**
     * Records that a low-confidence result was created.
     * <p>
     * BUG FIX: This counter was never incremented in original implementation
     * (hardcoded to 0 in statistics builder).
     */
    public void recordLowConfidence() {
        lowConfidenceCreated++;
    }

    /**
     * Records that a high-confidence candidate was created.
     */
    public void recordHighConfidence() {
        highConfidenceCreated++;
    }

    /**
     * Records that an invalid URL was skipped (domain extraction failed).
     * <p>
     * NEW FEATURE: Track invalid URLs that were previously silently skipped.
     */
    public void recordInvalidUrl() {
        invalidUrlsSkipped++;
    }

    /**
     * Generates immutable statistics summary from processing state.
     *
     * @param totalResults Total number of search results processed
     * @return ProcessingStatistics with all counters
     */
    public ProcessingStatistics buildStatistics(int totalResults) {
        return ProcessingStatistics.builder()
                .totalResults(totalResults)
                .spamTldFiltered(spamTldFiltered)
                .blacklistedSkipped(blacklistedSkipped)
                .duplicatesSkipped(duplicatesSkipped)
                .highConfidenceCreated(highConfidenceCreated)
                .lowConfidenceCreated(lowConfidenceCreated)
                .invalidUrlsSkipped(invalidUrlsSkipped)
                .build();
    }

    // Getters

    public UUID getSessionId() {
        return sessionId;
    }

    public BigDecimal getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public int getSpamTldFiltered() {
        return spamTldFiltered;
    }

    public int getBlacklistedSkipped() {
        return blacklistedSkipped;
    }

    public int getDuplicatesSkipped() {
        return duplicatesSkipped;
    }

    public int getHighConfidenceCreated() {
        return highConfidenceCreated;
    }

    public int getLowConfidenceCreated() {
        return lowConfidenceCreated;
    }

    public int getInvalidUrlsSkipped() {
        return invalidUrlsSkipped;
    }
}
