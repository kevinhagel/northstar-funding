package com.northstar.funding.discovery.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.domain.SessionType;

/**
 * Discovery Session Repository
 * 
 * Spring Data JDBC repository for DiscoverySession entity.
 * Critical for AI process improvement and error analysis.
 * 
 * Key Features:
 * - Performance analytics queries for process optimization
 * - Error tracking and diagnostics for system reliability  
 * - Search engine integration monitoring
 * - LM Studio model performance tracking
 * - Constitutional compliance: <500ms API requirement support
 */
@Repository
public interface DiscoverySessionRepository extends CrudRepository<DiscoverySession, UUID>, PagingAndSortingRepository<DiscoverySession, UUID> {

    /**
     * Find recent sessions for dashboard monitoring
     * Primary query for admin oversight of discovery process
     */
    @Query("""
        SELECT * FROM discovery_session 
        ORDER BY executed_at DESC
    """)
    List<DiscoverySession> findRecentSessions(Pageable pageable);
    
    /**
     * Find top 10 most recent sessions for test compatibility
     */
    List<DiscoverySession> findTop10ByOrderByExecutedAtDesc();
    
    /**
     * Find sessions by status
     */
    List<DiscoverySession> findByStatus(SessionStatus status);
    
    /**
     * Find sessions by type
     */
    List<DiscoverySession> findBySessionType(SessionType sessionType);
    
    /**
     * Find sessions within date range
     */
    List<DiscoverySession> findByExecutedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find sessions by status for error monitoring and cleanup
     */
    List<DiscoverySession> findByStatusOrderByExecutedAtDesc(SessionStatus status);
    
    /**
     * Find failed sessions for error analysis
     */
    @Query("""
        SELECT * FROM discovery_session 
        WHERE status = 'FAILED' 
        OR jsonb_array_length(error_messages) > 0
        ORDER BY executed_at DESC
    """)
    List<DiscoverySession> findFailedSessions(Pageable pageable);
    
    /**
     * Find running sessions for monitoring active processes
     */
    @Query("""
        SELECT * FROM discovery_session 
        WHERE status = 'RUNNING' 
        AND started_at < :threshold
        ORDER BY started_at ASC
    """)
    List<DiscoverySession> findLongRunningSessions(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Get performance metrics for dashboard analytics
     */
    @Query("""
        SELECT 
            AVG(candidates_found) as avg_candidates_found,
            AVG(duration_minutes) as avg_duration_minutes, 
            AVG(average_confidence_score) as avg_confidence_score,
            COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful_sessions,
            COUNT(*) FILTER (WHERE status = 'FAILED') as failed_sessions
        FROM discovery_session 
        WHERE executed_at > :since
    """)
    DiscoveryMetrics getPerformanceMetrics(@Param("since") LocalDateTime since);
    
    /**
     * Find sessions by type for workflow analysis
     */
    List<DiscoverySession> findBySessionTypeAndStatusOrderByExecutedAtDesc(
        SessionType sessionType, 
        SessionStatus status
    );
    
    /**
     * Find high-performing sessions for process improvement
     */
    @Query("""
        SELECT * FROM discovery_session 
        WHERE status = 'COMPLETED'
        AND candidates_found > :minCandidates
        AND average_confidence_score > :minConfidence
        ORDER BY 
            (candidates_found * average_confidence_score) DESC,
            duration_minutes ASC
    """)
    List<DiscoverySession> findHighPerformingSessions(
        @Param("minCandidates") Integer minCandidates,
        @Param("minConfidence") Double minConfidence,
        Pageable pageable
    );
    
    /**
     * Find sessions with search engine failures for reliability analysis
     */
    @Query("""
        SELECT * FROM discovery_session 
        WHERE search_engine_failures IS NOT NULL 
        AND search_engine_failures != '{}'::jsonb
        ORDER BY executed_at DESC
    """)
    List<DiscoverySession> findSessionsWithSearchEngineFailures(Pageable pageable);
    
    /**
     * Get daily discovery trends for analytics dashboard
     */
    @Query("""
        SELECT 
            DATE(executed_at) as discovery_date,
            COUNT(*) as total_sessions,
            AVG(candidates_found) as avg_candidates,
            AVG(duration_minutes) as avg_duration,
            COUNT(*) FILTER (WHERE status = 'COMPLETED') as successful
        FROM discovery_session 
        WHERE executed_at > :since
        GROUP BY DATE(executed_at)
        ORDER BY discovery_date DESC
    """)
    List<DailyDiscoveryTrends> getDailyTrends(@Param("since") LocalDateTime since);
    
    /**
     * Find sessions by LLM model for AI performance comparison
     */
    @Query("""
        SELECT * FROM discovery_session 
        WHERE llm_model_used = :modelName
        AND status = 'COMPLETED'
        ORDER BY executed_at DESC
    """)
    List<DiscoverySession> findByLlmModel(@Param("modelName") String modelName, Pageable pageable);
    
    /**
     * Find sessions with specific search engines for integration analysis
     */
    @Query("""
        SELECT * FROM discovery_session 
        WHERE search_engines_used::text LIKE CONCAT('%', :searchEngine, '%')
        AND status = 'COMPLETED'
        ORDER BY executed_at DESC
    """)
    List<DiscoverySession> findBySearchEngine(@Param("searchEngine") String searchEngine, Pageable pageable);
    
    /**
     * Get search engine reliability statistics
     */
    @Query("""
        SELECT 
            search_engines_used,
            COUNT(*) as usage_count,
            AVG(candidates_found) as avg_candidates,
            COUNT(*) FILTER (WHERE status = 'FAILED') as failure_count
        FROM discovery_session 
        WHERE executed_at > :since
        GROUP BY search_engines_used
        ORDER BY usage_count DESC
    """)
    List<SearchEngineStats> getSearchEngineStats(@Param("since") LocalDateTime since);
    
    /**
     * Find duplicate detection effectiveness
     */
    @Query("""
        SELECT 
            AVG(duplicates_detected::float / NULLIF(candidates_found, 0)) as avg_duplicate_rate,
            SUM(duplicates_detected) as total_duplicates_detected,
            SUM(candidates_found) as total_candidates_found
        FROM discovery_session 
        WHERE executed_at > :since 
        AND status = 'COMPLETED'
        AND candidates_found > 0
    """)
    DuplicationStats getDuplicationStats(@Param("since") LocalDateTime since);
    
    /**
     * Find sessions needing retry (failed with specific error patterns)
     */
    @Query("""
        SELECT * FROM discovery_session 
        WHERE status = 'FAILED'
        AND executed_at > :minAge
        AND NOT (error_messages::text LIKE '%permanent%' 
                OR error_messages::text LIKE '%invalid%')
        ORDER BY executed_at DESC
    """)
    List<DiscoverySession> findSessionsEligibleForRetry(@Param("minAge") LocalDateTime minAge);
    
    /**
     * Get optimization recommendations based on historical data
     */
    @Query("""
        SELECT 
            query_generation_prompt,
            AVG(candidates_found) as avg_effectiveness,
            COUNT(*) as usage_count,
            AVG(average_confidence_score) as avg_quality
        FROM discovery_session 
        WHERE status = 'COMPLETED'
        AND executed_at > :since
        AND query_generation_prompt IS NOT NULL
        GROUP BY query_generation_prompt
        HAVING COUNT(*) >= 3
        ORDER BY 
            (AVG(candidates_found) * AVG(average_confidence_score)) DESC
    """)
    List<PromptEffectiveness> getPromptEffectivenessAnalysis(@Param("since") LocalDateTime since);
    
    // Inner interfaces for query result mapping
    interface DiscoveryMetrics {
        Double getAvgCandidatesFound();
        Double getAvgDurationMinutes();
        Double getAvgConfidenceScore();
        Long getSuccessfulSessions();
        Long getFailedSessions();
    }
    
    interface DailyDiscoveryTrends {
        LocalDateTime getDiscoveryDate();
        Long getTotalSessions();
        Double getAvgCandidates();
        Double getAvgDuration();
        Long getSuccessful();
    }
    
    interface SearchEngineStats {
        String getSearchEnginesUsed();
        Long getUsageCount();
        Double getAvgCandidates();
        Long getFailureCount();
    }
    
    interface DuplicationStats {
        Double getAvgDuplicateRate();
        Long getTotalDuplicatesDetected();
        Long getTotalCandidatesFound();
    }
    
    interface PromptEffectiveness {
        String getQueryGenerationPrompt();
        Double getAvgEffectiveness();
        Long getUsageCount();
        Double getAvgQuality();
    }
}
