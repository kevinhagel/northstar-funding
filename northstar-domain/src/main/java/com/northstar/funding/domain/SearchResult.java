package com.northstar.funding.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search Result Domain Entity
 *
 * Tracks raw search results from search engines for deduplication.
 * Critical for preventing reprocessing of same URLs on same day.
 *
 * Deduplication Logic:
 * - Same domain + same URL + same day = Skip (duplicate)
 * - Same domain + different URL = Process (different program)
 * - New domain = Process (new organization)
 *
 * Search Engines:
 * - BraveSearch
 * - SearXNG
 * - Serper (Google Search API)
 * - Tavily
 *
 * This entity is the first point of contact with external data.
 * It feeds into the judgment pipeline for creating Organizations,
 * FundingPrograms, and FundingSourceCandidates.
 */
@Table("search_result")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    // Primary Identity
    @Id
    @Column("search_result_id")
    private UUID searchResultId;

    /**
     * Discovery session that found this result
     */
    private UUID discoverySessionId;

    // Search Metadata

    /**
     * Which search engine provided this result
     */
    private SearchEngineType searchEngine;

    /**
     * Query that produced this result
     */
    private String searchQuery;

    /**
     * When this result was discovered
     */
    private LocalDateTime discoveredAt;

    /**
     * Which day this search was executed (for deduplication)
     */
    private java.time.LocalDate searchDate;

    // URL & Domain Information

    /**
     * Full URL of the search result
     * Example: "https://us-bulgaria.org/education-grant"
     */
    private String url;

    /**
     * Extracted domain from URL
     * Example: "us-bulgaria.org"
     */
    private String domain;

    /**
     * URL path component
     * Example: "/education-grant"
     */
    private String urlPath;

    // Search Engine Metadata (Phase 1 - Metadata Judging)

    /**
     * Title from search result snippet
     */
    private String title;

    /**
     * Description/snippet from search result
     */
    private String description;

    /**
     * Search engine ranking position (1-based)
     */
    private Integer rankPosition;

    // Deduplication Tracking

    /**
     * Was this result skipped as duplicate?
     * True if: same domain + same URL + same day
     */
    private Boolean isDuplicate;

    /**
     * If duplicate, reference to original result
     */
    private UUID duplicateOfResultId;

    /**
     * Deduplication key for lookup
     * Format: "domain:url:YYYY-MM-DD"
     */
    private String deduplicationKey;

    // Processing Status

    /**
     * Has this result been processed?
     */
    private Boolean isProcessed;

    /**
     * When was this result processed?
     */
    private LocalDateTime processedAt;

    /**
     * Was an Organization created from this result?
     */
    private UUID organizationId;

    /**
     * Was a FundingProgram created from this result?
     */
    private UUID programId;

    /**
     * Was a FundingSourceCandidate created from this result?
     */
    private UUID candidateId;

    // Quality Tracking

    /**
     * Was this result's domain blacklisted?
     */
    private Boolean isBlacklisted;

    /**
     * Reason for blacklisting if applicable
     */
    private String blacklistReason;

    /**
     * Admin notes about this search result
     */
    private String notes;

    // Business Methods

    /**
     * Generate deduplication key for this result
     */
    public String generateDeduplicationKey() {
        if (domain == null || url == null || searchDate == null) {
            return null;
        }
        return String.format("%s:%s:%s", domain, url, searchDate);
    }

    /**
     * Check if this result should be skipped as duplicate
     */
    public boolean shouldSkipAsDuplicate() {
        return Boolean.TRUE.equals(isDuplicate);
    }

    /**
     * Check if this result has been processed
     */
    public boolean hasBeenProcessed() {
        return Boolean.TRUE.equals(isProcessed);
    }

    /**
     * Check if this result is blacklisted
     */
    public boolean isBlacklistedDomain() {
        return Boolean.TRUE.equals(isBlacklisted);
    }
}
