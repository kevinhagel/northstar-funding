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

import com.northstar.funding.domain.Organization;

/**
 * Organization Repository
 *
 * Spring Data JDBC repository for Organization entity.
 * Organizations own domains and host multiple funding programs.
 *
 * Key Features:
 * - Domain-level organization lookup
 * - Organization validation tracking
 * - Discovery session audit trails
 * - Quality-based filtering
 */
@Repository
public interface OrganizationRepository extends CrudRepository<Organization, UUID> {

    /**
     * Find organization by domain
     * Primary query for domain-to-organization mapping
     */
    Optional<Organization> findByDomain(String domain);

    /**
     * Check if organization exists for domain
     */
    boolean existsByDomain(String domain);

    /**
     * Find all valid funding source organizations
     */
    List<Organization> findByIsValidFundingSource(Boolean isValid);

    /**
     * Find active organizations
     */
    List<Organization> findByIsActive(Boolean isActive);

    /**
     * Find organizations by discovery session
     */
    List<Organization> findByDiscoverySessionId(UUID discoverySessionId);

    /**
     * Find organizations discovered after date
     */
    List<Organization> findByDiscoveredAtAfter(LocalDateTime since);

    /**
     * Find high-confidence organizations
     */
    @Query("""
        SELECT * FROM organization
        WHERE organization_confidence >= :minConfidence
        AND is_valid_funding_source = true
        ORDER BY organization_confidence DESC
    """)
    List<Organization> findHighConfidenceOrganizations(@Param("minConfidence") BigDecimal minConfidence, Pageable pageable);

    /**
     * Find organizations with multiple programs
     */
    @Query("""
        SELECT * FROM organization
        WHERE program_count >= :minPrograms
        ORDER BY program_count DESC
    """)
    List<Organization> findOrganizationsWithMultiplePrograms(@Param("minPrograms") Integer minPrograms);

    /**
     * Search organizations by name
     */
    @Query("""
        SELECT * FROM organization
        WHERE LOWER(name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY name ASC
    """)
    List<Organization> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Count organizations by validation status
     */
    @Query("SELECT COUNT(*) FROM organization WHERE is_valid_funding_source = :isValid")
    long countByValidationStatus(@Param("isValid") Boolean isValid);

    /**
     * Find organizations needing refresh (stale metadata)
     */
    @Query("""
        SELECT * FROM organization
        WHERE last_refreshed_at IS NULL
        OR last_refreshed_at < :threshold
        ORDER BY last_refreshed_at ASC NULLS FIRST
    """)
    List<Organization> findOrganizationsNeedingRefresh(@Param("threshold") LocalDateTime threshold);
}
