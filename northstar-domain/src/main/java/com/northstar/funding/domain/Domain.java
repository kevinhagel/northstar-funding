package com.northstar.funding.domain;

import java.math.BigDecimal;
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
 * Domain Registry Entity
 *
 * Tracks discovered domains to implement domain-level deduplication.
 * Prevents repeated processing of same domain across multiple URLs.
 * Supports blacklisting and quality-based filtering.
 *
 * Constitutional Principles:
 * - Domain-level deduplication (not URL-level)
 * - Permanent blacklist storage (PostgreSQL, not Redis with TTL)
 * - "No funds this year" tracking for legitimate funders
 * - Human-AI collaboration (humans can blacklist/approve domains)
 *
 * Example:
 * - us-bulgaria.org discovered → Domain record created
 * - us-bulgaria.org/programs/education → Skip (domain already processed)
 * - us-bulgaria.org/grants/2025 → Skip (domain already processed)
 */
@Table("domain")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Domain {

    // Primary Identity
    @Id
    private UUID domainId;

    /**
     * Domain name (e.g., "us-bulgaria.org", "example.com")
     * Extracted from search result URLs
     */
    @Column("domain_name")
    private String domainName;

    /**
     * Current processing status of this domain
     */
    private DomainStatus status;

    // Discovery and Processing Tracking

    /**
     * When this domain was first discovered
     */
    @Column("discovered_at")
    private LocalDateTime discoveredAt;

    /**
     * Discovery session that first found this domain
     */
    @Column("discovery_session_id")
    private UUID discoverySessionId;

    /**
     * When domain processing was last attempted
     */
    @Column("last_processed_at")
    private LocalDateTime lastProcessedAt;

    /**
     * How many times we've processed search results from this domain
     */
    @Column("processing_count")
    @Builder.Default
    private Integer processingCount = 0;

    // Quality and Filtering

    /**
     * Highest confidence score from any candidate from this domain (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     * Used to determine domain quality
     */
    @Column("best_confidence_score")
    private BigDecimal bestConfidenceScore;

    /**
     * Number of high-quality candidates (confidence > 0.6) from this domain
     */
    @Column("high_quality_candidate_count")
    @Builder.Default
    private Integer highQualityCandidateCount = 0;

    /**
     * Number of low-quality candidates (confidence <= 0.6) from this domain
     */
    @Column("low_quality_candidate_count")
    @Builder.Default
    private Integer lowQualityCandidateCount = 0;

    // Blacklist and Administrative Actions

    /**
     * Admin user who blacklisted this domain (nullable)
     */
    @Column("blacklisted_by")
    private UUID blacklistedBy;

    /**
     * When domain was blacklisted (nullable)
     */
    @Column("blacklisted_at")
    private LocalDateTime blacklistedAt;

    /**
     * Human-provided reason for blacklisting
     * Examples: "Known scam site", "Spam aggregator", "Irrelevant to Eastern Europe"
     */
    @Column("blacklist_reason")
    private String blacklistReason;

    /**
     * Year when "no funds this year" status was set (nullable)
     * Example: 2025 - can re-check in 2026
     */
    @Column("no_funds_year")
    private Integer noFundsYear;

    /**
     * Notes from admin users about this domain
     * Examples: "Good source, check annually", "Contact information hard to find"
     */
    private String notes;

    // Error Tracking

    /**
     * If PROCESSING_FAILED, reason for failure
     * Examples: "Timeout", "404 Not Found", "SSL certificate error"
     */
    @Column("failure_reason")
    private String failureReason;

    /**
     * Number of consecutive processing failures
     * After N failures, may auto-blacklist or skip
     */
    @Column("failure_count")
    @Builder.Default
    private Integer failureCount = 0;

    /**
     * When to retry processing after failure (nullable)
     * Exponential backoff: 1 hour, 4 hours, 1 day, etc.
     */
    @Column("retry_after")
    private LocalDateTime retryAfter;
}
