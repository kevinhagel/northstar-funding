package com.northstar.funding.discovery.search.infrastructure;

import com.northstar.funding.discovery.search.domain.SearchEngineType;
import com.northstar.funding.discovery.search.domain.SearchSessionStatistics;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Search Session Statistics Repository (Feature 003)
 *
 * Spring Data JDBC repository for SearchSessionStatistics entity.
 * Manages per-engine performance metrics for analytics and learning loop.
 *
 * Key Features:
 * - Per-session, per-engine statistics tracking
 * - Historical performance analytics
 * - Engine comparison queries
 * - Success/failure rate analysis
 *
 * Constitutional Compliance:
 * - Spring Data JDBC (no JPA/Hibernate)
 * - Supports human-AI collaboration (Kevin reviews analytics to improve query library)
 * - Learning loop enabler: identify productive engines and query patterns
 *
 * @author NorthStar Funding Team
 */
@Repository
public interface SearchSessionStatisticsRepository extends CrudRepository<SearchSessionStatistics, Long> {

    /**
     * Find all statistics for a specific discovery session
     * Returns one record per search engine used in that session
     *
     * @param sessionId The discovery session UUID
     * @return List of statistics (one per engine)
     */
    @Query("""
        SELECT * FROM search_session_statistics
        WHERE session_id = :sessionId
        ORDER BY engine_type ASC
    """)
    List<SearchSessionStatistics> findBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Find statistics for a specific engine across all sessions
     * For historical analysis: "How has Tavily performed over time?"
     *
     * @param engineType The search engine type
     * @return List of statistics for that engine
     */
    @Query("""
        SELECT * FROM search_session_statistics
        WHERE engine_type = CAST(:engineType AS VARCHAR)
        ORDER BY created_at DESC
    """)
    List<SearchSessionStatistics> findByEngineType(@Param("engineType") SearchEngineType engineType);

    /**
     * Find statistics within a time range
     * For analytics dashboard: performance over last 7 days, 30 days, etc.
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of statistics in that time range
     */
    @Query("""
        SELECT * FROM search_session_statistics
        WHERE created_at BETWEEN :startTime AND :endTime
        ORDER BY created_at DESC
    """)
    List<SearchSessionStatistics> findByCreatedAtBetween(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Get aggregate statistics for an engine over time
     * Returns total queries, total results, average hit rate
     *
     * @param engineType The search engine type
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Aggregated statistics (single row)
     */
    @Query("""
        SELECT
            :engineType AS engine_type,
            SUM(queries_executed) AS queries_executed,
            SUM(results_returned) AS results_returned,
            AVG(avg_response_time_ms) AS avg_response_time_ms,
            SUM(failure_count) AS failure_count,
            NOW() AS created_at
        FROM search_session_statistics
        WHERE engine_type = CAST(:engineType AS VARCHAR)
          AND created_at BETWEEN :startTime AND :endTime
    """)
    SearchSessionStatistics aggregateByEngineAndTimeRange(
        @Param("engineType") String engineType,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find most recent statistics for each engine
     * For dashboard: current state of all engines
     *
     * @return List of most recent statistics per engine
     */
    @Query("""
        SELECT DISTINCT ON (engine_type) *
        FROM search_session_statistics
        ORDER BY engine_type, created_at DESC
    """)
    List<SearchSessionStatistics> findMostRecentByEngine();

    /**
     * Count total sessions where a specific engine was used
     * For analytics: engine adoption rate
     *
     * @param engineType The search engine type
     * @return Count of sessions using that engine
     */
    @Query("""
        SELECT COUNT(DISTINCT session_id) FROM search_session_statistics
        WHERE engine_type = CAST(:engineType AS VARCHAR)
    """)
    int countSessionsByEngineType(@Param("engineType") SearchEngineType engineType);

    /**
     * Find statistics with high failure rates (for troubleshooting)
     * Failure rate > 20% indicates potential issues
     *
     * @param minFailureRate Minimum failure rate (0.0 to 1.0)
     * @return List of statistics with high failure rates
     */
    @Query("""
        SELECT * FROM search_session_statistics
        WHERE queries_executed > 0
          AND CAST(failure_count AS FLOAT) / CAST(queries_executed AS FLOAT) >= :minFailureRate
        ORDER BY created_at DESC
    """)
    List<SearchSessionStatistics> findByFailureRateGreaterThan(
        @Param("minFailureRate") double minFailureRate
    );
}
