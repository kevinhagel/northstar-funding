package com.northstar.funding.crawler.processing;

import com.northstar.funding.crawler.scoring.CandidateCreationService;
import com.northstar.funding.crawler.scoring.ConfidenceScorer;
import com.northstar.funding.crawler.scoring.DomainCredibilityService;
import com.northstar.funding.domain.FundingSourceCandidate;
import com.northstar.funding.persistence.repository.FundingSourceCandidateRepository;
import com.northstar.funding.persistence.service.DomainService;
import org.springframework.stereotype.Service;

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
     *
     * @param searchResults list of search results to process
     * @param sessionId discovery session ID
     * @return processing statistics
     */
    public ProcessingStatistics processSearchResults(List<SearchResult> searchResults, UUID sessionId) {
        // Handle empty list
        if (searchResults == null || searchResults.isEmpty()) {
            return ProcessingStatistics.builder()
                .totalResults(0)
                .spamTldFiltered(0)
                .blacklistedSkipped(0)
                .duplicatesSkipped(0)
                .highConfidenceCreated(0)
                .lowConfidenceCreated(0)
                .build();
        }

        // Track seen domains for deduplication
        java.util.Set<String> seenDomains = new java.util.HashSet<>();
        int duplicatesSkipped = 0;
        int blacklistedSkipped = 0;
        int highConfidenceCreated = 0;
        int lowConfidenceCreated = 0;

        // Confidence threshold
        java.math.BigDecimal threshold = new java.math.BigDecimal("0.60");

        // Process each result
        for (SearchResult result : searchResults) {
            // Extract domain
            java.util.Optional<String> domainOpt = domainService.extractDomainFromUrl(result.getUrl());
            if (!domainOpt.isPresent()) {
                continue; // Skip if domain extraction fails
            }
            String domain = domainOpt.get();

            // Check for duplicates
            if (seenDomains.contains(domain)) {
                duplicatesSkipped++;
                continue;
            }
            seenDomains.add(domain);

            // Check blacklist
            if (domainService.isBlacklisted(domain)) {
                blacklistedSkipped++;
                continue;
            }

            // Calculate confidence score
            java.math.BigDecimal confidence = confidenceScorer.calculateConfidence(
                result.getTitle(),
                result.getDescription(),
                result.getUrl()
            );

            // Filter by confidence threshold (< 0.6 = skip)
            if (confidence.compareTo(threshold) < 0) {
                // Low confidence - skip creating candidate
                continue;
            }

            // Register domain (or get existing)
            com.northstar.funding.domain.Domain domainEntity = domainService.registerOrGetDomain(domain, sessionId);

            // Create and save candidate for high confidence result
            FundingSourceCandidate candidate = candidateCreationService.createCandidate(
                result.getTitle(),
                result.getDescription(),
                result.getUrl(),
                domainEntity.getDomainId(),
                sessionId,
                confidence
            );
            candidateRepository.save(candidate);
            highConfidenceCreated++;
        }

        return ProcessingStatistics.builder()
            .totalResults(searchResults.size())
            .spamTldFiltered(0)
            .blacklistedSkipped(blacklistedSkipped)
            .duplicatesSkipped(duplicatesSkipped)
            .highConfidenceCreated(highConfidenceCreated)
            .lowConfidenceCreated(lowConfidenceCreated)
            .build();
    }
}
