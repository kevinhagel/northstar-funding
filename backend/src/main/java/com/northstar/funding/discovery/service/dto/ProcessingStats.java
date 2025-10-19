package com.northstar.funding.discovery.service.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Processing Statistics
 *
 * Summary of Phase 1 metadata judging results.
 * Used for logging and monitoring crawler effectiveness.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingStats {

    /**
     * Total number of search results processed
     */
    private Integer totalProcessed;

    /**
     * Number of candidates created (high confidence, pending crawl)
     * Status: PENDING_CRAWL
     */
    private Integer candidatesCreated;

    /**
     * Number of results skipped due to low confidence
     * Status: SKIPPED_LOW_CONFIDENCE
     */
    private Integer skippedLowConfidence;

    /**
     * Number of results skipped due to domain already processed
     */
    private Integer skippedDomainAlreadyProcessed;

    /**
     * Number of results skipped due to blacklisted domain
     */
    private Integer skippedBlacklisted;

    /**
     * Number of processing failures (errors)
     */
    private Integer failures;

    /**
     * Average confidence score of created candidates (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     */
    private BigDecimal averageConfidence;

    /**
     * Highest confidence score (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     */
    private BigDecimal maxConfidence;

    /**
     * Lowest confidence score (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     */
    private BigDecimal minConfidence;

    /**
     * Processing time in milliseconds
     */
    private Long processingTimeMs;

    /**
     * Get summary string for logging
     */
    public String getSummary() {
        return String.format(
            "Processed %d results: %d candidates created (avg confidence: %.2f), " +
            "%d skipped (low confidence), %d skipped (domain), %d blacklisted, %d failures",
            totalProcessed,
            candidatesCreated,
            averageConfidence != null ? averageConfidence.doubleValue() : 0.0,
            skippedLowConfidence,
            skippedDomainAlreadyProcessed,
            skippedBlacklisted,
            failures
        );
    }
}
