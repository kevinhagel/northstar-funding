package com.northstar.funding.crawler.processing;

import com.northstar.funding.crawler.scoring.CandidateCreationService;
import com.northstar.funding.crawler.scoring.ConfidenceScorer;
import com.northstar.funding.crawler.scoring.DomainCredibilityService;
import com.northstar.funding.domain.FundingSourceCandidate;
import com.northstar.funding.persistence.repository.FundingSourceCandidateRepository;
import com.northstar.funding.persistence.service.DomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Main orchestrator for search result processing pipeline.
 *
 * Coordinates:
 * 1. Spam TLD filtering (before deduplication)
 * 2. Domain extraction and deduplication
 * 3. Blacklist checking
 * 4. Confidence scoring
 * 5. Candidate creation
 * 6. Statistics tracking
 *
 * Implements the two-phase judging workflow:
 * - Phase 1: Metadata-based confidence scoring (this class)
 * - Phase 2: Deep web crawling (future feature)
 */
@Service
public class SearchResultProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SearchResultProcessor.class);
    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.60");

    private final DomainCredibilityService domainCredibilityService;
    private final ConfidenceScorer confidenceScorer;
    private final CandidateCreationService candidateCreationService;
    private final DomainService domainService;
    private final FundingSourceCandidateRepository candidateRepository;

    public SearchResultProcessor(
        DomainCredibilityService domainCredibilityService,
        ConfidenceScorer confidenceScorer,
        CandidateCreationService candidateCreationService,
        DomainService domainService,
        FundingSourceCandidateRepository candidateRepository
    ) {
        this.domainCredibilityService = domainCredibilityService;
        this.confidenceScorer = confidenceScorer;
        this.candidateCreationService = candidateCreationService;
        this.domainService = domainService;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Process search results into candidates with statistics tracking.
     * <p>
     * Orchestrates 7-stage pipeline:
     * 1. Domain extraction and validation
     * 2. Spam TLD filtering
     * 3. Duplicate detection
     * 4. Blacklist checking
     * 5. Confidence scoring
     * 6. Threshold filtering
     * 7. Candidate creation
     *
     * @param searchResults list of search results to process
     * @param sessionId discovery session ID
     * @return processing statistics
     */
    public ProcessingStatistics processSearchResults(List<SearchResult> searchResults, UUID sessionId) {
        MDC.put("sessionId", sessionId.toString());
        try {
            logger.info("Processing {} search results for session {}",
                searchResults != null ? searchResults.size() : 0, sessionId);

            // Handle empty list
            if (searchResults == null || searchResults.isEmpty()) {
                return ProcessingStatistics.builder()
                    .totalResults(0)
                    .spamTldFiltered(0)
                    .blacklistedSkipped(0)
                    .duplicatesSkipped(0)
                    .highConfidenceCreated(0)
                    .lowConfidenceCreated(0)
                    .invalidUrlsSkipped(0)
                    .build();
            }

            ProcessingContext context = new ProcessingContext(sessionId);

            // Process each result through pipeline
            for (SearchResult result : searchResults) {
                // Stage 1: Extract domain
                java.util.Optional<String> domain = extractAndValidateDomain(result, context);
                if (domain.isEmpty()) {
                    continue;  // Invalid URL, skip
                }

                // Stage 2: Check spam TLD
                if (isSpamTld(result, context)) {
                    continue;  // Spam TLD, skip
                }

                // Stage 3: Check duplicate
                if (isDuplicate(domain.get(), context)) {
                    continue;  // Duplicate domain, skip
                }

                // Stage 4: Check blacklist
                if (isBlacklisted(domain.get(), context)) {
                    continue;  // Blacklisted, skip
                }

                // Stage 5: Calculate confidence
                java.math.BigDecimal confidence = calculateConfidence(result);

                // Stage 6: Classify confidence (records high/low in context)
                classifyConfidence(confidence, context);

                // Stage 7: Create candidate (BOTH high and low confidence create candidates)
                createAndSaveCandidate(result, domain.get(), confidence, context);
            }

            ProcessingStatistics stats = context.buildStatistics(searchResults.size());
            logger.info("Processing complete: {} total, {} spam filtered, {} duplicates, {} blacklisted, " +
                       "{} high confidence, {} low confidence, {} invalid URLs",
                       stats.getTotalResults(), stats.getSpamTldFiltered(), stats.getDuplicatesSkipped(),
                       stats.getBlacklistedSkipped(), stats.getHighConfidenceCreated(),
                       stats.getLowConfidenceCreated(), stats.getInvalidUrlsSkipped());

            return stats;
        } finally {
            MDC.remove("sessionId");
        }
    }

    /**
     * Extract and validate domain from search result URL.
     * <p>
     * Stage 1 of processing pipeline.
     * <p>
     * Package-private for unit testing.
     *
     * @param result Search result with URL
     * @param context Processing context for failure tracking
     * @return Optional domain if extracted successfully, empty if extraction failed
     */
    java.util.Optional<String> extractAndValidateDomain(SearchResult result, ProcessingContext context) {
        java.util.Optional<String> domain = domainService.extractDomainFromUrl(result.getUrl());
        if (domain.isEmpty()) {
            context.recordInvalidUrl();
            logger.warn("Failed to extract domain from URL: {}", result.getUrl());
        }
        return domain;
    }

    /**
     * Check if URL has spam TLD.
     * <p>
     * Stage 2 of processing pipeline (filters before deduplication).
     * <p>
     * Package-private for unit testing.
     *
     * @param result Search result with URL
     * @param context Processing context for tracking
     * @return true if spam TLD (should skip), false if legitimate
     */
    boolean isSpamTld(SearchResult result, ProcessingContext context) {
        if (domainCredibilityService.isSpamTld(result.getUrl())) {
            context.recordSpamTldFiltered();
            logger.info("Spam TLD filtered: {}", result.getUrl());
            return true;
        }
        return false;
    }

    /**
     * Check if domain is duplicate.
     * <p>
     * Stage 3 of processing pipeline.
     * <p>
     * Package-private for unit testing.
     *
     * @param domain Domain name
     * @param context Processing context for deduplication tracking
     * @return true if duplicate (should skip), false if unique
     */
    boolean isDuplicate(String domain, ProcessingContext context) {
        if (!context.markDomainAsSeen(domain)) {
            logger.debug("Duplicate domain skipped: {}", domain);
            return true;  // Duplicate
        }
        return false;  // Unique
    }

    /**
     * Check if domain is blacklisted.
     * <p>
     * Stage 4 of processing pipeline.
     * <p>
     * Package-private for unit testing.
     *
     * @param domain Domain name
     * @param context Processing context for tracking
     * @return true if blacklisted (should skip), false if allowed
     */
    boolean isBlacklisted(String domain, ProcessingContext context) {
        if (domainService.isBlacklisted(domain)) {
            context.recordBlacklisted();
            logger.warn("Blacklisted domain skipped: {}", domain);
            return true;
        }
        return false;
    }

    /**
     * Calculate confidence score for search result.
     * <p>
     * Stage 5 of processing pipeline.
     * <p>
     * Package-private for unit testing.
     *
     * @param result Search result with metadata
     * @return Confidence score (0.00 to 1.00)
     */
    java.math.BigDecimal calculateConfidence(SearchResult result) {
        java.math.BigDecimal confidence = confidenceScorer.calculateConfidence(
            result.getTitle(),
            result.getDescription(),
            result.getUrl()
        );
        logger.debug("Calculated confidence {} for {}", confidence, result.getUrl());
        return confidence;
    }

    /**
     * Classify confidence level for candidate creation.
     * <p>
     * Stage 6 of processing pipeline.
     * <p>
     * DESIGN: Both high and low confidence create candidates with different statuses:
     * - High (>= 0.60): PENDING_CRAWL status (proceed to Phase 2)
     * - Low (< 0.60): SKIPPED_LOW_CONFIDENCE status (saved but not crawled)
     * <p>
     * Package-private for unit testing.
     *
     * @param confidence Calculated confidence score
     * @param context Processing context for tracking
     * @return true if high confidence, false if low confidence
     */
    boolean classifyConfidence(java.math.BigDecimal confidence, ProcessingContext context) {
        if (confidence.compareTo(context.getConfidenceThreshold()) >= 0) {
            context.recordHighConfidence();
            logger.debug("High confidence: {}", confidence);
            return true;
        } else {
            context.recordLowConfidence();
            logger.debug("Low confidence: {}", confidence);
            return false;
        }
    }

    /**
     * Create and save funding source candidate.
     * <p>
     * Stage 7 of processing pipeline.
     * <p>
     * Package-private for unit testing.
     *
     * @param result Search result with metadata
     * @param domain Domain name
     * @param confidence Calculated confidence score
     * @param context Processing context (session ID)
     */
    void createAndSaveCandidate(SearchResult result, String domain,
                                       java.math.BigDecimal confidence, ProcessingContext context) {
        com.northstar.funding.domain.Domain registeredDomain = domainService.registerOrGetDomain(
            domain, context.getSessionId()
        );

        FundingSourceCandidate candidate = candidateCreationService.createCandidate(
            result.getTitle(),
            result.getDescription(),
            result.getUrl(),
            registeredDomain.getDomainId(),
            context.getSessionId(),
            confidence
        );

        candidateRepository.save(candidate);
        logger.info("Created candidate for {}", result.getUrl());
    }
}
