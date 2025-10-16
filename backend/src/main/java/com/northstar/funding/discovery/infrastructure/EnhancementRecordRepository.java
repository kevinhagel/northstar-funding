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

import com.northstar.funding.discovery.domain.EnhancementRecord;
import com.northstar.funding.discovery.domain.EnhancementType;

/**
 * Enhancement Record Repository
 * 
 * Spring Data JDBC repository for EnhancementRecord entity.
 * Critical for human-AI collaboration tracking and quality metrics.
 * 
 * Key Features:
 * - Immutable audit trail of human improvements
 * - Quality metrics and performance analytics
 * - Admin user productivity tracking
 * - Candidate enhancement history
 * - Constitutional compliance: Human validation requirement support
 */
@Repository
public interface EnhancementRecordRepository extends CrudRepository<EnhancementRecord, UUID>, PagingAndSortingRepository<EnhancementRecord, UUID> {

    /**
     * Find enhancements by candidate ID
     * Primary query for candidate enhancement history
     */
    List<EnhancementRecord> findByCandidateIdOrderByEnhancedAtDesc(UUID candidateId);

    /**
     * Find enhancements by candidate ID (simple version for tests)
     */
    List<EnhancementRecord> findByCandidateId(UUID candidateId);

    /**
     * Find enhancements by admin user
     * Primary query for user performance tracking
     */
    List<EnhancementRecord> findByEnhancedByOrderByEnhancedAtDesc(UUID enhancedBy);

    /**
     * Find enhancements by type
     */
    List<EnhancementRecord> findByEnhancementTypeOrderByEnhancedAtDesc(EnhancementType enhancementType);

    /**
     * Find enhancements by type (simple version for tests)
     */
    List<EnhancementRecord> findByEnhancementType(EnhancementType enhancementType);
    
    /**
     * Find recent enhancements for dashboard monitoring
     */
    @Query("""
        SELECT * FROM enhancement_record 
        ORDER BY enhanced_at DESC
    """)
    List<EnhancementRecord> findRecentEnhancements(Pageable pageable);
    
    /**
     * Find top 10 most recent enhancements for test compatibility
     */
    List<EnhancementRecord> findTop10ByOrderByEnhancedAtDesc();
    
    /**
     * Find enhancements within date range
     */
    List<EnhancementRecord> findByEnhancedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find enhancements by candidate and admin user
     */
    List<EnhancementRecord> findByCandidateIdAndEnhancedByOrderByEnhancedAtDesc(UUID candidateId, UUID enhancedBy);
    
    /**
     * Find enhancements with significant confidence improvement
     */
    @Query("""
        SELECT * FROM enhancement_record 
        WHERE confidence_improvement > :minImprovement
        ORDER BY confidence_improvement DESC, enhanced_at DESC
    """)
    List<EnhancementRecord> findSignificantImprovements(@Param("minImprovement") Double minImprovement, Pageable pageable);
    
    /**
     * Find enhancements by type and time range
     */
    @Query("""
        SELECT * FROM enhancement_record 
        WHERE enhancement_type = :enhancementType
        AND enhanced_at >= :since
        ORDER BY enhanced_at DESC
    """)
    List<EnhancementRecord> findByTypeAndDateRange(
        @Param("enhancementType") String enhancementType,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Find AI-assisted enhancements for productivity analysis
     */
    @Query("""
        SELECT * FROM enhancement_record 
        WHERE ai_assistance_used = true
        ORDER BY enhanced_at DESC
    """)
    List<EnhancementRecord> findAiAssistedEnhancements(Pageable pageable);
    
    /**
     * Get admin user productivity metrics
     */
    @Query("""
        SELECT 
            enhanced_by,
            COUNT(*) as total_enhancements,
            AVG(time_spent_minutes) as avg_time_per_enhancement,
            SUM(time_spent_minutes) as total_time_spent,
            AVG(confidence_improvement) as avg_confidence_improvement,
            COUNT(*) FILTER (WHERE ai_assistance_used = true) as ai_assisted_count
        FROM enhancement_record 
        WHERE enhanced_at > :since
        GROUP BY enhanced_by
        ORDER BY total_enhancements DESC
    """)
    List<AdminProductivityMetrics> getAdminProductivityMetrics(@Param("since") LocalDateTime since);
    
    /**
     * Get enhancement type distribution
     */
    @Query("""
        SELECT 
            enhancement_type,
            COUNT(*) as enhancement_count,
            AVG(time_spent_minutes) as avg_time_spent,
            AVG(confidence_improvement) as avg_confidence_improvement
        FROM enhancement_record 
        WHERE enhanced_at > :since
        GROUP BY enhancement_type
        ORDER BY enhancement_count DESC
    """)
    List<EnhancementTypeStats> getEnhancementTypeDistribution(@Param("since") LocalDateTime since);
    
    /**
     * Get candidate enhancement summary
     */
    @Query("""
        SELECT 
            candidate_id,
            COUNT(*) as total_enhancements,
            SUM(confidence_improvement) as total_confidence_improvement,
            MAX(enhanced_at) as last_enhanced_at,
            COUNT(DISTINCT enhanced_by) as unique_reviewers
        FROM enhancement_record 
        WHERE enhanced_at > :since
        GROUP BY candidate_id
        HAVING COUNT(*) >= :minEnhancements
        ORDER BY total_enhancements DESC
    """)
    List<CandidateEnhancementSummary> getCandidateEnhancementSummary(
        @Param("since") LocalDateTime since,
        @Param("minEnhancements") Integer minEnhancements
    );
    
    /**
     * Find complex enhancements requiring significant time
     */
    @Query("""
        SELECT * FROM enhancement_record 
        WHERE time_spent_minutes > :minMinutes
        AND review_complexity = 'COMPLEX'
        ORDER BY time_spent_minutes DESC, enhanced_at DESC
    """)
    List<EnhancementRecord> findComplexEnhancements(
        @Param("minMinutes") Integer minMinutes,
        Pageable pageable
    );
    
    /**
     * Get daily enhancement trends for analytics dashboard
     */
    @Query("""
        SELECT 
            DATE(enhanced_at) as enhancement_date,
            COUNT(*) as total_enhancements,
            AVG(time_spent_minutes) as avg_time_spent,
            AVG(confidence_improvement) as avg_confidence_improvement,
            COUNT(DISTINCT enhanced_by) as active_reviewers,
            COUNT(DISTINCT candidate_id) as unique_candidates
        FROM enhancement_record 
        WHERE enhanced_at > :since
        GROUP BY DATE(enhanced_at)
        ORDER BY enhancement_date DESC
    """)
    List<DailyEnhancementTrends> getDailyTrends(@Param("since") LocalDateTime since);
    
    /**
     * Find enhancements by field name for data quality analysis
     */
    @Query("""
        SELECT * FROM enhancement_record 
        WHERE field_name = :fieldName
        AND enhanced_at > :since
        ORDER BY enhanced_at DESC
    """)
    List<EnhancementRecord> findByFieldName(
        @Param("fieldName") String fieldName,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Get validation method effectiveness
     */
    @Query("""
        SELECT 
            validation_method,
            COUNT(*) as usage_count,
            AVG(confidence_improvement) as avg_confidence_improvement,
            AVG(time_spent_minutes) as avg_time_spent
        FROM enhancement_record 
        WHERE validation_method IS NOT NULL
        AND enhanced_at > :since
        GROUP BY validation_method
        ORDER BY usage_count DESC
    """)
    List<ValidationMethodStats> getValidationMethodStats(@Param("since") LocalDateTime since);
    
    /**
     * Count enhancements by candidate
     */
    @Query("""
        SELECT COUNT(*) FROM enhancement_record 
        WHERE candidate_id = :candidateId
    """)
    Long countByCandidateId(@Param("candidateId") UUID candidateId);
    
    /**
     * Count enhancements by admin user
     */
    @Query("""
        SELECT COUNT(*) FROM enhancement_record 
        WHERE enhanced_by = :enhancedBy
    """)
    Long countByEnhancedBy(@Param("enhancedBy") UUID enhancedBy);
    
    /**
     * Find enhancements with notes containing search term (full-text search)
     */
    @Query("""
        SELECT * FROM enhancement_record
        WHERE to_tsvector('english',
            COALESCE(field_name, '') || ' ' ||
            COALESCE(suggested_value, '') || ' ' ||
            COALESCE(notes, '')
        ) @@ plainto_tsquery('english', :searchTerm)
        ORDER BY enhanced_at DESC
    """)
    List<EnhancementRecord> searchEnhancements(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Records for query result mapping (Spring Data JDBC compatible)
    record AdminProductivityMetrics(
        UUID enhancedBy,
        Long totalEnhancements,
        Double avgTimePerEnhancement,
        Long totalTimeSpent,
        Double avgConfidenceImprovement,
        Long aiAssistedCount
    ) {}
    
    record EnhancementTypeStats(
        String enhancementType,
        Long enhancementCount,
        Double avgTimeSpent,
        Double avgConfidenceImprovement
    ) {}
    
    record CandidateEnhancementSummary(
        UUID candidateId,
        Long totalEnhancements,
        Double totalConfidenceImprovement,
        LocalDateTime lastEnhancedAt,
        Long uniqueReviewers
    ) {}
    
    record DailyEnhancementTrends(
        java.sql.Date enhancementDate,
        Long totalEnhancements,
        Double avgTimeSpent,
        Double avgConfidenceImprovement,
        Long activeReviewers,
        Long uniqueCandidates
    ) {}
    
    record ValidationMethodStats(
        String validationMethod,
        Long usageCount,
        Double avgConfidenceImprovement,
        Double avgTimeSpent
    ) {}
}
