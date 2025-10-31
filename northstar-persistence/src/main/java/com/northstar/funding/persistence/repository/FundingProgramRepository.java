package com.northstar.funding.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.northstar.funding.domain.FundingProgram;
import com.northstar.funding.domain.ProgramStatus;

/**
 * Funding Program Repository
 *
 * Spring Data JDBC repository for FundingProgram entity.
 * Programs are specific funding opportunities within organizations.
 *
 * Key Features:
 * - URL-level deduplication for programs
 * - Status and deadline-based querying
 * - Organization relationship tracking
 * - Program validation and confidence filtering
 */
@Repository
public interface FundingProgramRepository extends CrudRepository<FundingProgram, UUID> {

    /**
     * Find program by URL
     * Primary query for URL-level deduplication
     */
    Optional<FundingProgram> findByProgramUrl(String programUrl);

    /**
     * Check if program exists by URL
     */
    boolean existsByProgramUrl(String programUrl);

    /**
     * Find all programs for an organization
     */
    List<FundingProgram> findByOrganizationId(UUID organizationId);

    /**
     * Find all programs on a domain
     */
    List<FundingProgram> findByDomain(String domain);

    /**
     * Find programs by status
     */
    List<FundingProgram> findByStatus(ProgramStatus status);

    /**
     * Find active programs
     */
    List<FundingProgram> findByIsActive(Boolean isActive);

    /**
     * Find valid funding opportunities
     */
    List<FundingProgram> findByIsValidFundingOpportunity(Boolean isValid);

    /**
     * Find programs by discovery session
     */
    List<FundingProgram> findByDiscoverySessionId(UUID discoverySessionId);

    /**
     * Find programs by search result
     */
    List<FundingProgram> findBySearchResultId(UUID searchResultId);

    /**
     * Find programs with upcoming deadlines
     */
    @Query("""
        SELECT * FROM funding_program
        WHERE application_deadline IS NOT NULL
        AND application_deadline > :now
        AND application_deadline <= :threshold
        AND status = 'ACTIVE'
        ORDER BY application_deadline ASC
    """)
    List<FundingProgram> findProgramsWithUpcomingDeadlines(
        @Param("now") LocalDateTime now,
        @Param("threshold") LocalDateTime threshold
    );

    /**
     * Find expired programs that need archival
     */
    @Query("""
        SELECT * FROM funding_program
        WHERE application_deadline < :now
        AND status NOT IN ('EXPIRED', 'ARCHIVED')
        ORDER BY application_deadline ASC
    """)
    List<FundingProgram> findExpiredPrograms(@Param("now") LocalDateTime now);

    /**
     * Find high-confidence programs
     */
    @Query("""
        SELECT * FROM funding_program
        WHERE program_confidence >= :minConfidence
        AND is_valid_funding_opportunity = true
        ORDER BY program_confidence DESC
    """)
    List<FundingProgram> findHighConfidencePrograms(@Param("minConfidence") BigDecimal minConfidence, Pageable pageable);

    /**
     * Find recurring programs
     */
    List<FundingProgram> findByIsRecurring(Boolean isRecurring);

    /**
     * Find programs discovered after date
     */
    List<FundingProgram> findByDiscoveredAtAfter(LocalDateTime since);

    /**
     * Search programs by name
     */
    @Query("""
        SELECT * FROM funding_program
        WHERE LOWER(program_name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY program_name ASC
    """)
    List<FundingProgram> searchByProgramName(@Param("searchTerm") String searchTerm);

    /**
     * Find programs needing refresh
     */
    @Query("""
        SELECT * FROM funding_program
        WHERE last_refreshed_at IS NULL
        OR last_refreshed_at < :threshold
        ORDER BY last_refreshed_at ASC NULLS FIRST
    """)
    List<FundingProgram> findProgramsNeedingRefresh(@Param("threshold") LocalDateTime threshold);

    /**
     * Count programs by status
     */
    @Query("SELECT COUNT(*) FROM funding_program WHERE status = :status")
    long countByStatus(@Param("status") ProgramStatus status);

    /**
     * Count active programs for organization
     */
    @Query("""
        SELECT COUNT(*) FROM funding_program
        WHERE organization_id = :organizationId
        AND status = 'ACTIVE'
    """)
    long countActiveByOrganization(@Param("organizationId") UUID organizationId);
}
