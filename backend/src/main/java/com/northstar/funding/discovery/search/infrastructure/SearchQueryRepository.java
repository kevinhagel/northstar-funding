package com.northstar.funding.discovery.search.infrastructure;

import com.northstar.funding.discovery.search.domain.SearchQuery;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Search Query Repository (Feature 003)
 *
 * Spring Data JDBC repository for SearchQuery entity.
 * Manages hardcoded query library for nightly search execution.
 *
 * Key Features:
 * - Day-of-week filtering for nightly scheduler
 * - Enabled/disabled query management
 * - JSONB tag queries for analytics
 * - Target engine filtering
 *
 * Constitutional Compliance:
 * - Spring Data JDBC (no JPA/Hibernate)
 * - PostgreSQL JSONB queries for flexible tag filtering
 * - Supports learning loop (Kevin can enable/disable queries based on analytics)
 *
 * @author NorthStar Funding Team
 */
@Repository
public interface SearchQueryRepository extends CrudRepository<SearchQuery, Long> {

    /**
     * Find all enabled queries for a specific day of week
     * Primary query for nightly scheduler
     *
     * @param dayOfWeek The day of week (MONDAY-SUNDAY)
     * @return List of enabled queries for that day
     */
    @Query("""
        SELECT * FROM search_queries
        WHERE day_of_week = CAST(:dayOfWeek AS VARCHAR)
          AND enabled = TRUE
        ORDER BY created_at ASC
    """)
    List<SearchQuery> findByDayOfWeekAndEnabled(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    /**
     * Find all enabled queries (for manual execution or testing)
     *
     * @return List of all enabled queries
     */
    @Query("""
        SELECT * FROM search_queries
        WHERE enabled = TRUE
        ORDER BY day_of_week, created_at ASC
    """)
    List<SearchQuery> findAllEnabled();

    /**
     * Find all disabled queries (for analytics/review)
     *
     * @return List of all disabled queries
     */
    @Query("""
        SELECT * FROM search_queries
        WHERE enabled = FALSE
        ORDER BY updated_at DESC
    """)
    List<SearchQuery> findAllDisabled();

    /**
     * Find queries containing a specific tag (JSONB query)
     * For analytics: "Which queries target Bulgaria?"
     *
     * Example usage: findByTagContaining("GEOGRAPHY", "Bulgaria")
     *
     * @param tagType The tag type (GEOGRAPHY, CATEGORY, AUTHORITY)
     * @param tagValue The tag value (e.g., "Bulgaria", "Education", "EU")
     * @return List of queries with that tag
     */
    @Query("""
        SELECT * FROM search_queries
        WHERE tags @> CAST(:tagJson AS JSONB)
        ORDER BY day_of_week, created_at ASC
    """)
    List<SearchQuery> findByTagContaining(@Param("tagJson") String tagJson);

    /**
     * Find queries targeting a specific search engine (TEXT[] array query)
     * For analytics: "Which queries use Tavily?"
     *
     * @param engineType The search engine type (SEARXNG, TAVILY, PERPLEXITY)
     * @return List of queries targeting that engine
     */
    @Query("""
        SELECT * FROM search_queries
        WHERE :engineType = ANY(target_engines)
        ORDER BY day_of_week, created_at ASC
    """)
    List<SearchQuery> findByTargetEngine(@Param("engineType") String engineType);

    /**
     * Count enabled queries by day of week
     * For dashboard analytics: query distribution across the week
     *
     * @param dayOfWeek The day of week
     * @return Count of enabled queries for that day
     */
    @Query("""
        SELECT COUNT(*) FROM search_queries
        WHERE day_of_week = CAST(:dayOfWeek AS VARCHAR)
          AND enabled = TRUE
    """)
    int countByDayOfWeekAndEnabled(@Param("dayOfWeek") DayOfWeek dayOfWeek);
}
