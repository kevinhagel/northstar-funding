package com.northstar.funding.discovery.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.northstar.funding.discovery.domain.AuthorityLevel;
import com.northstar.funding.discovery.domain.ContactIntelligence;
import com.northstar.funding.discovery.domain.ContactType;

/**
 * Contact Intelligence Repository
 * 
 * Spring Data JDBC repository for ContactIntelligence entity.
 * Constitutional priority: Contact Intelligence as first-class, highest-value asset.
 * 
 * Key Features:
 * - Relationship-focused queries for network mapping
 * - Validation status tracking
 * - Authority level filtering for decision-maker identification
 * - Organization-based contact discovery
 * - PII-protected data access (encryption handled at service layer)
 */
@Repository
public interface ContactIntelligenceRepository extends CrudRepository<ContactIntelligence, UUID> {

    /**
     * Find all contacts for a specific funding source candidate
     * Primary relationship query for candidate detail views
     */
    List<ContactIntelligence> findByCandidateId(UUID candidateId);
    
    /**
     * Find active contacts for a candidate ordered by authority level
     * Used for prioritizing contact outreach
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE candidate_id = :candidateId 
        AND is_active = true
        ORDER BY 
            CASE authority_level 
                WHEN 'DECISION_MAKER' THEN 1 
                WHEN 'INFLUENCER' THEN 2 
                WHEN 'INFORMATION_ONLY' THEN 3 
            END,
            full_name
    """)
    List<ContactIntelligence> findActiveByCandidateIdOrderedByAuthority(@Param("candidateId") UUID candidateId);
    
    /**
     * Find decision makers across all candidates for high-priority contacts
     */
    List<ContactIntelligence> findByDecisionAuthorityAndIsActive(
        AuthorityLevel decisionAuthority, 
        Boolean isActive
    );
    
    /**
     * Find contacts by type for specialized outreach campaigns
     */
    List<ContactIntelligence> findByContactTypeAndIsActive(
        ContactType contactType, 
        Boolean isActive,
        Pageable pageable
    );
    
    /**
     * Find contacts that need validation (stale or never validated)
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE is_active = true 
        AND (validated_at IS NULL OR validated_at < :threshold)
        ORDER BY 
            CASE WHEN validated_at IS NULL THEN 0 ELSE 1 END,
            validated_at ASC
    """)
    List<ContactIntelligence> findContactsNeedingValidation(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find contacts by validation status for test compatibility
     */
    List<ContactIntelligence> findByValidatedAtBeforeOrValidatedAtIsNull(LocalDateTime threshold);
    
    /**
     * Search contacts by organization name (case-insensitive)
     * Used for finding connections across different funding sources
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE LOWER(organization) LIKE LOWER(CONCAT('%', :organization, '%'))
        AND is_active = true
        ORDER BY authority_level, full_name
    """)
    List<ContactIntelligence> findByOrganizationContainingIgnoreCase(@Param("organization") String organization);
    
    /**
     * Full-text search across contact information
     * Leverages PostgreSQL full-text search index
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE to_tsvector('english', 
                          full_name || ' ' || 
                          COALESCE(title, '') || ' ' || 
                          COALESCE(organization, '') || ' ' ||
                          COALESCE(relationship_notes, '')
                         ) @@ plainto_tsquery('english', :searchTerm)
        AND is_active = true
        ORDER BY authority_level, full_name
    """)
    List<ContactIntelligence> fullTextSearch(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Find contacts with referral connections for network mapping
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE referral_connections IS NOT NULL 
        AND referral_connections != ''
        AND is_active = true
        ORDER BY authority_level, organization
    """)
    List<ContactIntelligence> findContactsWithReferralNetworks();
    
    /**
     * Find recently contacted contacts for follow-up tracking
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE last_contacted_at IS NOT NULL 
        AND last_contacted_at > :since
        AND is_active = true
        ORDER BY last_contacted_at DESC
    """)
    List<ContactIntelligence> findRecentlyContacted(@Param("since") LocalDateTime since);
    
    /**
     * Find contacts by email domain for organization relationship mapping
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE email LIKE CONCAT('%@', :domain)
        AND is_active = true
        ORDER BY authority_level, full_name
    """)
    List<ContactIntelligence> findByEmailDomain(@Param("domain") String domain);
    
    /**
     * Count contacts by authority level for dashboard metrics
     */
    @Query("""
        SELECT COUNT(*) FROM contact_intelligence 
        WHERE authority_level = :authorityLevel 
        AND is_active = true
    """)
    long countByAuthorityLevel(@Param("authorityLevel") AuthorityLevel authorityLevel);
    
    /**
     * Find contacts created by specific admin user for performance tracking
     */
    List<ContactIntelligence> findByCreatedByAndIsActive(UUID createdBy, Boolean isActive);
    
    /**
     * Find contacts that have never been contacted for outreach prioritization
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE last_contacted_at IS NULL 
        AND is_active = true
        AND validated_at IS NOT NULL
        ORDER BY 
            CASE authority_level 
                WHEN 'DECISION_MAKER' THEN 1 
                WHEN 'INFLUENCER' THEN 2 
                WHEN 'INFORMATION_ONLY' THEN 3 
            END,
            created_at ASC
    """)
    List<ContactIntelligence> findUncontactedValidatedContacts(Pageable pageable);
    
    /**
     * Advanced contact search with multiple criteria
     */
    @Query("""
        SELECT * FROM contact_intelligence 
        WHERE (:contactType IS NULL OR contact_type = :contactType)
        AND (:authorityLevel IS NULL OR authority_level = :authorityLevel)
        AND (:isActive IS NULL OR is_active = :isActive)
        AND (:organizationFilter IS NULL OR 
             LOWER(organization) LIKE LOWER(CONCAT('%', :organizationFilter, '%')))
        ORDER BY authority_level, full_name
    """)
    List<ContactIntelligence> findWithAdvancedFilters(
        @Param("contactType") ContactType contactType,
        @Param("authorityLevel") AuthorityLevel authorityLevel,
        @Param("isActive") Boolean isActive,
        @Param("organizationFilter") String organizationFilter,
        Pageable pageable
    );
    
    /**
     * Get contact intelligence statistics for dashboard
     */
    @Query("""
        SELECT 
            COUNT(*) as total_contacts,
            COUNT(*) FILTER (WHERE authority_level = 'DECISION_MAKER') as decision_makers,
            COUNT(*) FILTER (WHERE last_contacted_at IS NOT NULL) as contacted,
            COUNT(*) FILTER (WHERE validated_at IS NOT NULL) as validated
        FROM contact_intelligence 
        WHERE is_active = true
    """)
    ContactIntelligenceStats getContactStats();
    
    /**
     * Inner interface for contact statistics
     */
    interface ContactIntelligenceStats {
        long getTotalContacts();
        long getDecisionMakers(); 
        long getContacted();
        long getValidated();
    }
}
