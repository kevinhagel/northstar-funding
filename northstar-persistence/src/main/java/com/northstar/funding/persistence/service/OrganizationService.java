package com.northstar.funding.persistence.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.domain.Organization;
import com.northstar.funding.persistence.repository.OrganizationRepository;


/**
 * Service layer for Organization entity operations.
 *
 * Provides business logic and transaction management for organization
 * validation tracking and quality filtering.
 *
 * This is the public API for external modules to interact with Organization persistence.
 */
@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    // ============================================================================
    // CREATE & UPDATE Operations
    // ============================================================================

    /**
     * Register a new organization if it doesn't already exist for this domain.
     *
     * @param name organization name
     * @param domain domain name
     * @param discoverySessionId optional session ID that discovered this organization
     * @return the existing or newly created Organization
     */
    public Organization registerOrganization(String name, String domain, UUID discoverySessionId) {

        return organizationRepository.findByDomain(domain)
            .orElseGet(() -> {
                Organization org = Organization.builder()
                    .name(name)
                    .domain(domain)
                    .discoverySessionId(discoverySessionId)
                    .discoveredAt(LocalDateTime.now())
                    .isActive(true)
                    .build();

                Organization saved = organizationRepository.save(org);
                return saved;
            });
    }

    /**
     * Update organization metadata.
     *
     * @param organizationId the organization ID
     * @param mission optional mission statement
     * @param geographicFocus optional geographic focus
     * @param homepageUrl optional homepage URL
     * @return updated Organization
     */
    public Organization updateMetadata(UUID organizationId, String mission,
                                       String geographicFocus, String homepageUrl) {

        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        if (mission != null) {
            org.setMission(mission);
        }
        if (geographicFocus != null) {
            org.setGeographicFocus(geographicFocus);
        }
        if (homepageUrl != null) {
            org.setHomepageUrl(homepageUrl);
        }

        org.setLastRefreshedAt(LocalDateTime.now());
        return organizationRepository.save(org);
    }

    /**
     * Mark organization as validated funding source.
     *
     * @param organizationId the organization ID
     * @param confidence AI confidence score (0.00-1.00)
     * @return updated Organization
     */
    public Organization markAsValidFundingSource(UUID organizationId, BigDecimal confidence) {
        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        org.setIsValidFundingSource(true);
        org.setOrganizationConfidence(confidence);
        org.setLastRefreshedAt(LocalDateTime.now());

        return organizationRepository.save(org);
    }

    /**
     * Increment program count for an organization.
     *
     * @param organizationId the organization ID
     */
    public void incrementProgramCount(UUID organizationId) {

        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        Integer currentCount = org.getProgramCount() != null ? org.getProgramCount() : 0;
        org.setProgramCount(currentCount + 1);

        organizationRepository.save(org);
    }

    /**
     * Deactivate an organization.
     *
     * @param organizationId the organization ID
     * @return updated Organization
     */
    public Organization deactivate(UUID organizationId) {

        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        org.setIsActive(false);
        return organizationRepository.save(org);
    }

    // ============================================================================
    // READ Operations
    // ============================================================================

    /**
     * Check if an organization exists for a domain.
     *
     * @param domain the domain name
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean organizationExistsForDomain(String domain) {
        return organizationRepository.existsByDomain(domain);
    }

    /**
     * Find organization by domain.
     *
     * @param domain the domain name
     * @return Optional of Organization
     */
    @Transactional(readOnly = true)
    public Optional<Organization> findByDomain(String domain) {
        return organizationRepository.findByDomain(domain);
    }

    /**
     * Find organization by ID.
     *
     * @param organizationId the organization ID
     * @return Optional of Organization
     */
    @Transactional(readOnly = true)
    public Optional<Organization> findById(UUID organizationId) {
        return organizationRepository.findById(organizationId);
    }

    /**
     * Get all validated funding sources.
     *
     * @return list of valid funding source organizations
     */
    @Transactional(readOnly = true)
    public List<Organization> getValidFundingSources() {
        return organizationRepository.findByIsValidFundingSource(true);
    }

    /**
     * Get active organizations.
     *
     * @return list of active organizations
     */
    @Transactional(readOnly = true)
    public List<Organization> getActiveOrganizations() {
        return organizationRepository.findByIsActive(true);
    }

    /**
     * Get high-confidence organizations (sorted by confidence score).
     *
     * @param minConfidence minimum confidence threshold
     * @param limit max number of results
     * @return list of high-confidence organizations
     */
    @Transactional(readOnly = true)
    public List<Organization> getHighConfidenceOrganizations(BigDecimal minConfidence, int limit) {
        return organizationRepository.findHighConfidenceOrganizations(
            minConfidence,
            PageRequest.of(0, limit)
        );
    }

    /**
     * Get organizations with multiple programs.
     *
     * @param minPrograms minimum number of programs
     * @return list of organizations
     */
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsWithMultiplePrograms(int minPrograms) {
        return organizationRepository.findOrganizationsWithMultiplePrograms(minPrograms);
    }

    /**
     * Get organizations needing metadata refresh.
     *
     * @param daysThreshold number of days since last refresh
     * @return list of organizations needing refresh
     */
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsNeedingRefresh(int daysThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);
        return organizationRepository.findOrganizationsNeedingRefresh(threshold);
    }

    /**
     * Get organizations discovered in a specific session.
     *
     * @param sessionId the discovery session ID
     * @return list of organizations
     */
    @Transactional(readOnly = true)
    public List<Organization> getOrganizationsBySession(UUID sessionId) {
        return organizationRepository.findByDiscoverySessionId(sessionId);
    }

    /**
     * Search organizations by name pattern (ILIKE).
     *
     * @param pattern search pattern
     * @return list of matching organizations
     */
    @Transactional(readOnly = true)
    public List<Organization> searchOrganizations(String pattern) {
        return organizationRepository.searchByName(pattern);
    }

    /**
     * Count organizations by validation status.
     *
     * @param isValid validation status
     * @return count
     */
    @Transactional(readOnly = true)
    public long countByValidationStatus(boolean isValid) {
        return organizationRepository.countByValidationStatus(isValid);
    }
}
