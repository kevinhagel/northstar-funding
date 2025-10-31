package com.northstar.funding.persistence.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;

/**
 * Search Result Repository
 *
 * Spring Data JDBC repository for SearchResult entity.
 * Critical for deduplication tracking and preventing reprocessing.
 *
 * Deduplication Logic:
 * - Same domain + same URL + same day = Skip (duplicate)
 * - Same domain + different URL = Process (different program)
 *
 * Key Features:
 * - Deduplication key-based lookups
 * - Search engine tracking
 * - Processing status management
 * - Blacklist tracking
 */
@Repository
public interface SearchResultRepository extends CrudRepository<SearchResult, UUID> {

    /**
     * Find search result by deduplication key
     * Primary query for deduplication check
     * Format: "domain:url:YYYY-MM-DD"
     */
    Optional<SearchResult> findByDeduplicationKey(String deduplicationKey);

    /**
     * Check if result exists by deduplication key
     */
    boolean existsByDeduplicationKey(String deduplicationKey);

    /**
     * Find all results for a domain
     */
    List<SearchResult> findByDomain(String domain);

    /**
     * Find results by URL
     */
    List<SearchResult> findByUrl(String url);

    /**
     * Find results for specific discovery session
     */
    List<SearchResult> findByDiscoverySessionId(UUID discoverySessionId);

    /**
     * Find results by search engine
     */
    List<SearchResult> findBySearchEngine(SearchEngineType searchEngine);

    /**
     * Find duplicate results
     */
    List<SearchResult> findByIsDuplicate(Boolean isDuplicate);

    /**
     * Find unprocessed results
     */
    @Query("""
        SELECT * FROM search_result
        WHERE is_processed = false
        AND is_duplicate = false
        AND is_blacklisted = false
        ORDER BY discovered_at ASC
    """)
    List<SearchResult> findUnprocessedResults(Pageable pageable);

    /**
     * Find blacklisted results
     */
    List<SearchResult> findByIsBlacklisted(Boolean isBlacklisted);

    /**
     * Find results discovered on specific date
     */
    List<SearchResult> findBySearchDate(LocalDate searchDate);

    /**
     * Find results by domain and date (for deduplication analysis)
     */
    @Query("""
        SELECT * FROM search_result
        WHERE domain = :domain
        AND search_date = :searchDate
        ORDER BY discovered_at ASC
    """)
    List<SearchResult> findByDomainAndSearchDate(
        @Param("domain") String domain,
        @Param("searchDate") LocalDate searchDate
    );

    /**
     * Find results that created organizations
     */
    @Query("""
        SELECT * FROM search_result
        WHERE organization_id IS NOT NULL
        ORDER BY discovered_at DESC
    """)
    List<SearchResult> findResultsWithOrganizations(Pageable pageable);

    /**
     * Find results that created programs
     */
    @Query("""
        SELECT * FROM search_result
        WHERE program_id IS NOT NULL
        ORDER BY discovered_at DESC
    """)
    List<SearchResult> findResultsWithPrograms(Pageable pageable);

    /**
     * Find results that created candidates
     */
    @Query("""
        SELECT * FROM search_result
        WHERE candidate_id IS NOT NULL
        ORDER BY discovered_at DESC
    """)
    List<SearchResult> findResultsWithCandidates(Pageable pageable);

    /**
     * Count duplicates for session
     */
    @Query("""
        SELECT COUNT(*) FROM search_result
        WHERE discovery_session_id = :sessionId
        AND is_duplicate = true
    """)
    long countDuplicatesBySession(@Param("sessionId") UUID sessionId);

    /**
     * Count results by search engine
     */
    @Query("SELECT COUNT(*) FROM search_result WHERE search_engine = :engine")
    long countBySearchEngine(@Param("engine") SearchEngineType engine);

    /**
     * Find recent search results
     */
    @Query("""
        SELECT * FROM search_result
        WHERE discovered_at >= :since
        ORDER BY discovered_at DESC
    """)
    List<SearchResult> findRecentResults(@Param("since") LocalDateTime since);

    /**
     * Get deduplication statistics for session
     */
    @Query("""
        SELECT
            COUNT(*) as total_results,
            COUNT(*) FILTER (WHERE is_duplicate = true) as duplicates,
            COUNT(DISTINCT domain) as unique_domains,
            COUNT(*) FILTER (WHERE is_processed = true) as processed_count
        FROM search_result
        WHERE discovery_session_id = :sessionId
    """)
    DeduplicationStats getDeduplicationStats(@Param("sessionId") UUID sessionId);

    /**
     * Find results needing processing
     */
    @Query("""
        SELECT * FROM search_result
        WHERE is_processed = false
        AND is_duplicate = false
        AND is_blacklisted = false
        AND discovered_at >= :minAge
        ORDER BY discovered_at ASC
    """)
    List<SearchResult> findResultsReadyForProcessing(@Param("minAge") LocalDateTime minAge, Pageable pageable);

    /**
     * Deduplication statistics interface
     */
    interface DeduplicationStats {
        long getTotalResults();
        long getDuplicates();
        long getUniqueDomains();
        long getProcessedCount();
    }
}
