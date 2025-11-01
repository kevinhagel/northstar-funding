package com.northstar.funding.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;

/**
 * Domain Repository
 *
 * Spring Data JDBC repository for domain-level deduplication and blacklisting.
 * Supports the core domain registry service for preventing duplicate processing.
 *
 * Constitutional Principles:
 * - Domain-level deduplication (not URL-level)
 * - Permanent blacklist storage (PostgreSQL, not Redis with TTL)
 * - Quality-based filtering to avoid low-quality domains
 */
@Repository
public interface DomainRepository extends CrudRepository<Domain, UUID> {

    /**
     * Find domain by domain name
     * Primary query for domain deduplication check
     *
     * Example: "us-bulgaria.org"
     */
    Optional<Domain> findByDomainName(String domainName);

    /**
     * Check if domain exists (fast existence check)
     */
    boolean existsByDomainName(String domainName);

    /**
     * Find all blacklisted domains
     * Used for blacklist management UI
     */
    List<Domain> findByStatus(DomainStatus status);

    /**
     * Find domains ready for retry after processing failure
     * Exponential backoff: retry_after time has passed
     */
    @Query("""
        SELECT * FROM domain
        WHERE status = 'PROCESSING_FAILED'
        AND retry_after IS NOT NULL
        AND retry_after <= :now
        ORDER BY retry_after ASC
    """)
    List<Domain> findDomainsReadyForRetry(@Param("now") LocalDateTime now);

    /**
     * Find high-quality domains (for analysis)
     * Domains that yielded good candidates
     */
    @Query("""
        SELECT * FROM domain
        WHERE status = 'PROCESSED_HIGH_QUALITY'
        AND high_quality_candidate_count >= :minCount
        ORDER BY best_confidence_score DESC
    """)
    List<Domain> findHighQualityDomains(@Param("minCount") Integer minCandidateCount);

    /**
     * Find low-quality domains that should be avoided
     * Returns domains with many low-quality candidates and zero high-quality ones
     */
    @Query("""
        SELECT * FROM domain
        WHERE low_quality_candidate_count >= :threshold
        AND high_quality_candidate_count = 0
    """)
    List<Domain> findLowQualityDomains(@Param("threshold") Integer lowQualityThreshold);

    /**
     * Find domains with "no funds this year" status for specific year
     * Used to re-check these domains in subsequent years
     */
    @Query("""
        SELECT * FROM domain
        WHERE status = 'NO_FUNDS_THIS_YEAR'
        AND no_funds_year = :year
        ORDER BY domain_name ASC
    """)
    List<Domain> findNoFundsForYear(@Param("year") Integer year);

    /**
     * Count domains by status for metrics dashboard
     */
    @Query("SELECT COUNT(*) FROM domain WHERE status = :status")
    long countByStatus(@Param("status") DomainStatus status);

    /**
     * Find recently discovered domains
     * Used for monitoring new domain discovery
     */
    @Query("""
        SELECT * FROM domain
        WHERE discovered_at >= :since
        ORDER BY discovered_at DESC
    """)
    List<Domain> findRecentlyDiscovered(@Param("since") LocalDateTime since);

    /**
     * Find domains that have been processed multiple times
     * Identifies potential issues or changing content
     */
    @Query("""
        SELECT * FROM domain
        WHERE processing_count > :threshold
        ORDER BY processing_count DESC, last_processed_at DESC
    """)
    List<Domain> findFrequentlyProcessedDomains(@Param("threshold") Integer processingCountThreshold);

    /**
     * Find domains by discovery session
     * Used for audit trails and session analysis
     */
    List<Domain> findByDiscoverySessionId(UUID discoverySessionId);

    /**
     * Search domains by name pattern
     * Used for admin UI search
     */
    @Query("""
        SELECT * FROM domain
        WHERE domain_name LIKE CONCAT('%', :pattern, '%')
        ORDER BY domain_name ASC
    """)
    List<Domain> searchByDomainNamePattern(@Param("pattern") String pattern);

    /**
     * Find domains blacklisted by specific admin user
     * Used for admin activity tracking
     */
    List<Domain> findByBlacklistedBy(UUID adminUserId);

    /**
     * Get average confidence score across all domains
     * Quality metric for discovery effectiveness
     */
    @Query("""
        SELECT AVG(best_confidence_score) FROM domain
        WHERE best_confidence_score IS NOT NULL
        AND status = 'PROCESSED_HIGH_QUALITY'
    """)
    Double getAverageConfidenceScore();

    /**
     * Count total candidates discovered from a domain
     * Sum of high and low quality candidates
     */
    @Query("""
        SELECT (high_quality_candidate_count + low_quality_candidate_count) as total
        FROM domain
        WHERE domain_id = :domainId
    """)
    Integer getTotalCandidateCount(@Param("domainId") UUID domainId);
}
