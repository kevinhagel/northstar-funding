package com.northstar.funding.discovery.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.discovery.domain.Domain;
import com.northstar.funding.discovery.domain.DomainStatus;
import com.northstar.funding.discovery.infrastructure.DomainRepository;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Domain Registry Service
 *
 * Core service for domain-level deduplication and blacklist management.
 * Prevents repeated processing of same domain across multiple URLs.
 *
 * Constitutional Principles:
 * - Domain-level deduplication (us-bulgaria.org, not us-bulgaria.org/programs)
 * - Permanent blacklist storage (PostgreSQL, not Redis with TTL)
 * - Quality-based filtering (avoid low-quality domains)
 * - Human-AI collaboration (humans can blacklist/approve domains)
 *
 * Key Responsibilities:
 * - Extract domain from URL
 * - Check if domain already processed
 * - Check if domain blacklisted
 * - Track domain quality metrics
 * - Manage blacklist (add/remove)
 * - Handle "no funds this year" status
 *
 * Example Usage:
 * <pre>
 * String url = "https://us-bulgaria.org/programs/education";
 * if (domainService.shouldProcessDomain(url)) {
 *     // Process search result
 * } else {
 *     // Skip - already processed or blacklisted
 * }
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DomainRegistryService {

    private final DomainRepository domainRepository;

    /**
     * Check if a domain should be processed based on URL
     *
     * Returns false if:
     * - Domain is blacklisted
     * - Domain already processed with low quality
     * - Domain marked as "no funds this year"
     *
     * @param url Search result URL
     * @return true if domain should be processed, false if should skip
     */
    public boolean shouldProcessDomain(String url) {
        return extractDomainName(url)
            .map(this::shouldProcessDomainByName)
            .getOrElse(true); // If domain extraction fails, allow processing (may be unusual URL)
    }

    /**
     * Check if a domain should be processed
     *
     * @param domainName Domain name (e.g., "us-bulgaria.org")
     * @return true if should process, false if should skip
     */
    public boolean shouldProcessDomainByName(String domainName) {
        Optional<Domain> existingDomain = domainRepository.findByDomainName(domainName);

        if (existingDomain.isEmpty()) {
            return true; // New domain, should process
        }

        Domain domain = existingDomain.get();
        DomainStatus status = domain.getStatus();

        // Skip if blacklisted
        if (status == DomainStatus.BLACKLISTED) {
            log.debug("Skipping blacklisted domain: {} (reason: {})",
                domainName, domain.getBlacklistReason());
            return false;
        }

        // Skip if already processed with low quality
        if (status == DomainStatus.PROCESSED_LOW_QUALITY) {
            log.debug("Skipping low-quality domain: {} (high: {}, low: {})",
                domainName,
                domain.getHighQualityCandidateCount(),
                domain.getLowQualityCandidateCount());
            return false;
        }

        // Skip if no funds this year
        if (status == DomainStatus.NO_FUNDS_THIS_YEAR) {
            int currentYear = LocalDateTime.now().getYear();
            if (domain.getNoFundsYear() != null && domain.getNoFundsYear() == currentYear) {
                log.debug("Skipping domain with no funds for {}: {}", currentYear, domainName);
                return false;
            }
            // If it's a new year, allow re-processing
            log.info("Re-checking domain {} for new year (marked no funds in {})",
                domainName, domain.getNoFundsYear());
            return true;
        }

        // Skip if processing failed too many times (exponential backoff)
        if (status == DomainStatus.PROCESSING_FAILED) {
            if (domain.getRetryAfter() != null && LocalDateTime.now().isBefore(domain.getRetryAfter())) {
                log.debug("Skipping domain {} - retry after {}",
                    domainName, domain.getRetryAfter());
                return false;
            }
            // Retry time has passed, allow retry
            log.info("Retrying failed domain: {} (failures: {})",
                domainName, domain.getFailureCount());
            return true;
        }

        // Allow processing for DISCOVERED, PROCESSING, or PROCESSED_HIGH_QUALITY
        // High-quality domains can be re-checked periodically
        return true;
    }

    /**
     * Register a newly discovered domain from URL
     *
     * Creates a Domain record for tracking and deduplication.
     * If domain already exists, returns existing record.
     *
     * @param url URL from search result
     * @param discoverySessionId Discovery session that found this domain
     * @return Domain entity (new or existing)
     */
    @Transactional
    public Try<Domain> registerDomainFromUrl(String url, UUID discoverySessionId) {
        return extractDomainName(url)
            .flatMap(domainName -> registerDomain(domainName, discoverySessionId));
    }

    /**
     * Register a domain by name
     *
     * @param domainName Domain name (e.g., "us-bulgaria.org")
     * @param discoverySessionId Discovery session ID
     * @return Domain entity
     */
    @Transactional
    public Try<Domain> registerDomain(String domainName, UUID discoverySessionId) {
        return Try.of(() -> {
            Optional<Domain> existing = domainRepository.findByDomainName(domainName);

            if (existing.isPresent()) {
                log.debug("Domain already registered: {}", domainName);
                return existing.get();
            }

            Domain newDomain = Domain.builder()
                .domainName(domainName)
                .status(DomainStatus.DISCOVERED)
                .discoveredAt(LocalDateTime.now())
                .discoverySessionId(discoverySessionId)
                .processingCount(0)
                .highQualityCandidateCount(0)
                .lowQualityCandidateCount(0)
                .failureCount(0)
                .build();

            Domain saved = domainRepository.save(newDomain);
            log.info("Registered new domain: {} (session: {})", domainName, discoverySessionId);
            return saved;
        });
    }

    /**
     * Update domain quality metrics based on candidate confidence score
     *
     * Called after judging a candidate from this domain.
     * Updates best confidence score and quality counters.
     *
     * @param domainId Domain ID
     * @param confidenceScore Candidate confidence score (0.00-1.00, BigDecimal with scale 2)
     * @param isHighQuality True if confidence >= 0.60
     */
    @Transactional
    public void updateDomainQuality(UUID domainId, BigDecimal confidenceScore, boolean isHighQuality) {
        domainRepository.findById(domainId).ifPresent(domain -> {
            // Update best confidence score (use compareTo for BigDecimal comparison)
            if (domain.getBestConfidenceScore() == null ||
                confidenceScore.compareTo(domain.getBestConfidenceScore()) > 0) {
                domain.setBestConfidenceScore(confidenceScore);
            }

            // Update quality counters
            if (isHighQuality) {
                domain.setHighQualityCandidateCount(
                    domain.getHighQualityCandidateCount() + 1);
            } else {
                domain.setLowQualityCandidateCount(
                    domain.getLowQualityCandidateCount() + 1);
            }

            // Update processing timestamp
            domain.setLastProcessedAt(LocalDateTime.now());
            domain.setProcessingCount(domain.getProcessingCount() + 1);

            // Update status based on quality
            if (isHighQuality) {
                domain.setStatus(DomainStatus.PROCESSED_HIGH_QUALITY);
            } else if (domain.getHighQualityCandidateCount() == 0 &&
                       domain.getLowQualityCandidateCount() >= 3) {
                // After 3 low-quality candidates with no high-quality, mark as low quality
                domain.setStatus(DomainStatus.PROCESSED_LOW_QUALITY);
                log.info("Marking domain as low quality: {} (low count: {})",
                    domain.getDomainName(), domain.getLowQualityCandidateCount());
            }

            domainRepository.save(domain);
            log.debug("Updated domain quality: {} (best: {}, high: {}, low: {})",
                domain.getDomainName(),
                domain.getBestConfidenceScore(),
                domain.getHighQualityCandidateCount(),
                domain.getLowQualityCandidateCount());
        });
    }

    /**
     * Blacklist a domain
     *
     * Permanently prevents processing of this domain.
     * Used for known scams, spam aggregators, or irrelevant sites.
     *
     * @param domainName Domain to blacklist
     * @param reason Human-provided reason
     * @param adminUserId Admin user who blacklisted
     * @return Blacklisted domain
     */
    @Transactional
    public Try<Domain> blacklistDomain(String domainName, String reason, UUID adminUserId) {
        return Try.of(() -> {
            Domain domain = domainRepository.findByDomainName(domainName)
                .orElseGet(() -> Domain.builder()
                    .domainName(domainName)
                    .status(DomainStatus.BLACKLISTED)
                    .discoveredAt(LocalDateTime.now())
                    .processingCount(0)
                    .highQualityCandidateCount(0)
                    .lowQualityCandidateCount(0)
                    .failureCount(0)
                    .build());

            domain.setStatus(DomainStatus.BLACKLISTED);
            domain.setBlacklistedBy(adminUserId);
            domain.setBlacklistedAt(LocalDateTime.now());
            domain.setBlacklistReason(reason);

            Domain saved = domainRepository.save(domain);
            log.warn("Domain blacklisted: {} by {} (reason: {})",
                domainName, adminUserId, reason);
            return saved;
        });
    }

    /**
     * Mark domain as "no funds this year"
     *
     * Legitimate funder but no funds available this year.
     * Can be re-checked in future years.
     *
     * @param domainName Domain name
     * @param year Year when no funds available
     * @param notes Admin notes
     * @return Updated domain
     */
    @Transactional
    public Try<Domain> markNoFundsThisYear(String domainName, Integer year, String notes) {
        return Try.of(() -> {
            Domain domain = domainRepository.findByDomainName(domainName)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Domain not found: " + domainName));

            domain.setStatus(DomainStatus.NO_FUNDS_THIS_YEAR);
            domain.setNoFundsYear(year);
            domain.setNotes(notes);

            Domain saved = domainRepository.save(domain);
            log.info("Domain marked as no funds for {}: {} (notes: {})",
                year, domainName, notes);
            return saved;
        });
    }

    /**
     * Record processing failure for a domain
     *
     * Implements exponential backoff for retries:
     * - 1st failure: retry in 1 hour
     * - 2nd failure: retry in 4 hours
     * - 3rd failure: retry in 1 day
     * - 4th+ failure: retry in 1 week
     *
     * @param domainId Domain ID
     * @param failureReason Reason for failure
     */
    @Transactional
    public void recordProcessingFailure(UUID domainId, String failureReason) {
        domainRepository.findById(domainId).ifPresent(domain -> {
            int failureCount = domain.getFailureCount() + 1;
            domain.setFailureCount(failureCount);
            domain.setFailureReason(failureReason);
            domain.setStatus(DomainStatus.PROCESSING_FAILED);

            // Exponential backoff
            LocalDateTime retryAfter = switch (failureCount) {
                case 1 -> LocalDateTime.now().plusHours(1);
                case 2 -> LocalDateTime.now().plusHours(4);
                case 3 -> LocalDateTime.now().plusDays(1);
                default -> LocalDateTime.now().plusWeeks(1);
            };
            domain.setRetryAfter(retryAfter);

            domainRepository.save(domain);
            log.warn("Domain processing failed (attempt {}): {} - {} (retry after: {})",
                failureCount, domain.getDomainName(), failureReason, retryAfter);
        });
    }

    /**
     * Extract domain name from URL
     *
     * Example: "https://us-bulgaria.org/programs/education" → "us-bulgaria.org"
     *
     * @param url Full URL
     * @return Domain name wrapped in Try
     */
    public Try<String> extractDomainName(String url) {
        return Try.of(() -> {
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host == null) {
                throw new URISyntaxException(url, "No host in URL");
            }

            // Remove www. prefix if present
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host.toLowerCase();
        }).onFailure(e ->
            log.warn("Failed to extract domain from URL: {} - {}", url, e.getMessage())
        );
    }

    /**
     * Get domain by name
     *
     * @param domainName Domain name
     * @return Optional Domain
     */
    public Optional<Domain> getDomainByName(String domainName) {
        return domainRepository.findByDomainName(domainName);
    }

    /**
     * Get domain by ID
     *
     * @param domainId Domain ID
     * @return Optional Domain
     */
    public Optional<Domain> getDomainById(UUID domainId) {
        return domainRepository.findById(domainId);
    }
}
