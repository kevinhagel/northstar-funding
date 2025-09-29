package com.northstar.funding.discovery.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;

/**
 * Funding Source Candidate Repository
 * 
 * Spring Data JDBC repository for FundingSourceCandidate aggregate root.
 * Follows DDD repository patterns with domain-focused data access.
 * 
 * Key Features:
 * - Pagination support for review queue
 * - Status and confidence filtering
 * - Duplicate detection queries
 * - Performance optimized for <500ms requirement
 * - Constitutional compliance: Spring Data JDBC (no ORM complexity)
 */
@Repository
public interface FundingSourceCandidateRepository extends CrudRepository<FundingSourceCandidate, UUID>, PagingAndSortingRepository<FundingSourceCandidate, UUID> {

    /**
     * Find candidates by status with confidence score ordering for review queue
     * Primary query for admin review dashboard
     */
    Page<FundingSourceCandidate> findByStatusOrderByConfidenceScoreDesc(
        CandidateStatus status, 
        Pageable pageable
    );
    
    /**
     * Find candidates by status and minimum confidence threshold
     * Used for filtering high-quality candidates
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE status = :status 
        AND confidence_score >= :minConfidence 
        ORDER BY confidence_score DESC, discovered_at DESC
    """)
    List<FundingSourceCandidate> findByStatusAndMinimumConfidence(
        @Param("status") CandidateStatus status,
        @Param("minConfidence") Double minConfidence,
        Pageable pageable
    );
    
    /**
     * Find candidates assigned to specific reviewer
     * Used for workload management and personal queue
     */
    Page<FundingSourceCandidate> findByAssignedReviewerId(UUID reviewerId, Pageable pageable);
    
    /**
     * Find unassigned candidates for automatic assignment
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE status = 'PENDING_REVIEW' 
        AND assigned_reviewer_id IS NULL 
        ORDER BY confidence_score DESC, discovered_at ASC
    """)
    List<FundingSourceCandidate> findUnassignedCandidatesForAssignment(Pageable pageable);
    
    /**
     * Find potential duplicates by organization and program name
     * Critical for duplicate detection workflow
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE LOWER(organization_name) = LOWER(:orgName) 
        AND LOWER(program_name) = LOWER(:programName)
        AND candidate_id != :excludeId
        AND status != 'REJECTED'
    """)
    List<FundingSourceCandidate> findPotentialDuplicates(
        @Param("orgName") String organizationName,
        @Param("programName") String programName,
        @Param("excludeId") UUID excludeCandidateId
    );
    
    /**
     * Find stale candidates that have been pending too long
     * Used for cleanup and escalation workflows
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE status IN ('PENDING_REVIEW', 'IN_REVIEW')
        AND discovered_at < :threshold
        ORDER BY discovered_at ASC
    """)
    List<FundingSourceCandidate> findStaleCandidates(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find candidates by discovery session for audit trails
     */
    List<FundingSourceCandidate> findByDiscoverySessionId(UUID discoverySessionId);
    
    /**
     * Advanced search with multiple filters
     * Used by complex search scenarios in admin UI
     */
    @Query("""
        SELECT * FROM funding_source_candidates c
        WHERE (:status IS NULL OR c.status = :status)
        AND (:minConfidence IS NULL OR c.confidence_score >= :minConfidence)
        AND (:assignedTo IS NULL OR c.assigned_reviewer_id = :assignedTo)
        AND (:searchTerm IS NULL OR 
             LOWER(c.organization_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
             LOWER(c.program_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY 
            CASE WHEN :status = 'PENDING_REVIEW' THEN c.confidence_score END DESC,
            c.discovered_at DESC
    """)
    List<FundingSourceCandidate> findWithAdvancedFilters(
        @Param("status") CandidateStatus status,
        @Param("minConfidence") Double minConfidence, 
        @Param("assignedTo") UUID assignedTo,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
    
    /**
     * Count candidates by status for dashboard metrics
     */
    @Query("SELECT COUNT(*) FROM funding_source_candidates WHERE status = :status")
    long countByStatus(@Param("status") CandidateStatus status);
    
    /**
     * Get average confidence score for quality metrics
     */
    @Query("""
        SELECT AVG(confidence_score) FROM funding_source_candidates 
        WHERE discovered_at > :since AND status != 'REJECTED'
    """)
    Double getAverageConfidenceScore(@Param("since") LocalDateTime since);
    
    /**
     * Find top performing candidates for quality analysis
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE status = 'APPROVED' 
        AND confidence_score >= 0.8
        ORDER BY confidence_score DESC
    """)
    List<FundingSourceCandidate> findHighQualityApprovedCandidates(Pageable pageable);
    
    /**
     * Find candidates with specific tags for categorization
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE tags::text LIKE CONCAT('%', :tag, '%')
        AND status = :status
        ORDER BY confidence_score DESC
    """)
    List<FundingSourceCandidate> findByTagAndStatus(
        @Param("tag") String tag,
        @Param("status") CandidateStatus status,
        Pageable pageable
    );
    
    /**
     * Find candidates within geographic eligibility
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE geographic_eligibility::text LIKE CONCAT('%', :region, '%')
        AND status = 'APPROVED'
        ORDER BY confidence_score DESC
    """)
    List<FundingSourceCandidate> findByGeographicEligibility(
        @Param("region") String region,
        Pageable pageable
    );
    
    /**
     * Find duplicates by organization name and program name (simplified version)
     * Used by tests for basic duplicate detection
     */
    @Query("""
        SELECT * FROM funding_source_candidates 
        WHERE LOWER(organization_name) = LOWER(:orgName) 
        AND LOWER(program_name) = LOWER(:programName)
        AND status != 'REJECTED'
    """)
    List<FundingSourceCandidate> findDuplicatesByOrganizationNameAndProgramName(
        @Param("orgName") String organizationName,
        @Param("programName") String programName
    );
    
    /**
     * Find candidates discovered before a specific date
     * Used for cleanup and archival processes
     */
    List<FundingSourceCandidate> findByDiscoveredAtBefore(LocalDateTime threshold);
    
    /**
     * Find candidates with confidence score greater than threshold
     * Used for filtering high-confidence candidates
     */
    List<FundingSourceCandidate> findByConfidenceScoreGreaterThanOrderByConfidenceScoreDesc(Double confidenceScore);
}
