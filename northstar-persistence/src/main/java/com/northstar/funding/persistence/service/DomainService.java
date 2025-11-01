package com.northstar.funding.persistence.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.DomainStatus;
import com.northstar.funding.persistence.repository.DomainRepository;


/**
 * Service layer for Domain entity operations.
 *
 * Provides business logic and transaction management for domain-level deduplication,
 * blacklist management, and quality tracking.
 *
 * This is the public API for external modules to interact with Domain persistence.
 */
@Service
@Transactional
public class DomainService {

    private final DomainRepository domainRepository;

    public DomainService(DomainRepository domainRepository) {
        this.domainRepository = domainRepository;
    }

    // ============================================================================
    // CREATE & UPDATE Operations
    // ============================================================================

    /**
     * Register a new domain if it doesn't already exist.
     *
     * @param domainName the domain name to register
     * @param discoverySessionId optional session ID that discovered this domain
     * @return the existing or newly created Domain
     */
    public Domain registerDomain(String domainName, UUID discoverySessionId) {

        return domainRepository.findByDomainName(domainName)
            .orElseGet(() -> {
                Domain domain = Domain.builder()
                    .domainName(domainName)
                    .status(DomainStatus.DISCOVERED)
                    .discoverySessionId(discoverySessionId)
                    .discoveredAt(LocalDateTime.now())
                    .build();

                Domain saved = domainRepository.save(domain);
                return saved;
            });
    }

    /**
     * Update domain status.
     *
     * @param domainId the domain ID
     * @param status the new status
     * @return updated Domain
     */
    public Domain updateStatus(UUID domainId, DomainStatus status) {

        Domain domain = domainRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainId));

        domain.setStatus(status);
        domain.setLastProcessedAt(LocalDateTime.now());

        return domainRepository.save(domain);
    }

    /**
     * Blacklist a domain.
     *
     * @param domainName the domain to blacklist
     * @param blacklistedBy admin user ID who blacklisted it
     * @param blacklistReason reason for blacklisting
     * @return updated Domain
     */
    public Domain blacklistDomain(String domainName, UUID blacklistedBy, String blacklistReason) {

        Domain domain = domainRepository.findByDomainName(domainName)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainName));

        domain.setStatus(DomainStatus.BLACKLISTED);
        domain.setBlacklistedAt(LocalDateTime.now());
        domain.setBlacklistedBy(blacklistedBy);
        domain.setBlacklistReason(blacklistReason);

        return domainRepository.save(domain);
    }

    /**
     * Increment candidate counts after judging.
     *
     * @param domainName the domain name
     * @param highQualityCount number of high-quality candidates found
     * @param lowQualityCount number of low-quality candidates found
     * @param bestConfidence best confidence score found
     */
    public void updateCandidateCounts(String domainName, int highQualityCount, int lowQualityCount,
                                      java.math.BigDecimal bestConfidence) {

        Domain domain = domainRepository.findByDomainName(domainName)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainName));

        domain.setHighQualityCandidateCount(
            domain.getHighQualityCandidateCount() + highQualityCount);
        domain.setLowQualityCandidateCount(
            domain.getLowQualityCandidateCount() + lowQualityCount);

        if (bestConfidence != null &&
            (domain.getBestConfidenceScore() == null ||
             bestConfidence.compareTo(domain.getBestConfidenceScore()) > 0)) {
            domain.setBestConfidenceScore(bestConfidence);
        }

        // Update status based on quality
        if (highQualityCount > 0) {
            domain.setStatus(DomainStatus.PROCESSED_HIGH_QUALITY);
        } else if (lowQualityCount > 0) {
            domain.setStatus(DomainStatus.PROCESSED_LOW_QUALITY);
        }

        domain.setLastProcessedAt(LocalDateTime.now());
        domainRepository.save(domain);
    }

    // ============================================================================
    // READ Operations
    // ============================================================================

    /**
     * Check if a domain already exists.
     *
     * @param domainName the domain name
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean domainExists(String domainName) {
        return domainRepository.existsByDomainName(domainName);
    }

    /**
     * Find domain by name.
     *
     * @param domainName the domain name
     * @return Optional of Domain
     */
    @Transactional(readOnly = true)
    public Optional<Domain> findByDomainName(String domainName) {
        return domainRepository.findByDomainName(domainName);
    }

    /**
     * Find domain by ID.
     *
     * @param domainId the domain ID
     * @return Optional of Domain
     */
    @Transactional(readOnly = true)
    public Optional<Domain> findById(UUID domainId) {
        return domainRepository.findById(domainId);
    }

    /**
     * Get all blacklisted domains.
     *
     * @return list of blacklisted domains
     */
    @Transactional(readOnly = true)
    public List<Domain> getBlacklistedDomains() {
        return domainRepository.findByStatus(DomainStatus.BLACKLISTED);
    }

    /**
     * Get high-quality domains (sorted by candidate count and confidence).
     *
     * @param minCandidates minimum number of high-quality candidates
     * @return list of high-quality domains
     */
    @Transactional(readOnly = true)
    public List<Domain> getHighQualityDomains(int minCandidates) {
        return domainRepository.findHighQualityDomains(minCandidates);
    }

    /**
     * Get domains ready for retry (failed processing, retry time passed).
     *
     * @return list of domains ready for retry
     */
    @Transactional(readOnly = true)
    public List<Domain> getDomainsReadyForRetry() {
        return domainRepository.findDomainsReadyForRetry(LocalDateTime.now());
    }

    /**
     * Get domains discovered in a specific session.
     *
     * @param sessionId the discovery session ID
     * @return list of domains
     */
    @Transactional(readOnly = true)
    public List<Domain> getDomainsBySession(UUID sessionId) {
        return domainRepository.findByDiscoverySessionId(sessionId);
    }

    /**
     * Count domains by status.
     *
     * @param status the domain status
     * @return count
     */
    @Transactional(readOnly = true)
    public long countByStatus(DomainStatus status) {
        return domainRepository.countByStatus(status);
    }

    /**
     * Get average confidence score across all domains.
     *
     * @return average confidence score
     */
    @Transactional(readOnly = true)
    public Double getAverageConfidenceScore() {
        return domainRepository.getAverageConfidenceScore();
    }

    /**
     * Search domains by name pattern (ILIKE).
     *
     * @param pattern search pattern
     * @return list of matching domains
     */
    @Transactional(readOnly = true)
    public List<Domain> searchDomains(String pattern) {
        return domainRepository.searchByDomainNamePattern(pattern);
    }
}
