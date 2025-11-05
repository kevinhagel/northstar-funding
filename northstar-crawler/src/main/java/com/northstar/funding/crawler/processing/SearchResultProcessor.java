package com.northstar.funding.crawler.processing;

import com.northstar.funding.crawler.scoring.CandidateCreationService;
import com.northstar.funding.crawler.scoring.ConfidenceScorer;
import com.northstar.funding.crawler.scoring.DomainCredibilityService;
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

    public SearchResultProcessor(
        DomainCredibilityService domainCredibilityService,
        ConfidenceScorer confidenceScorer,
        CandidateCreationService candidateCreationService,
        DomainService domainService
    ) {
        this.domainCredibilityService = domainCredibilityService;
        this.confidenceScorer = confidenceScorer;
        this.candidateCreationService = candidateCreationService;
        this.domainService = domainService;
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

        // Extract domains and track duplicates
        for (SearchResult result : searchResults) {
            java.util.Optional<String> domainOpt = domainService.extractDomainFromUrl(result.getUrl());
            if (domainOpt.isPresent()) {
                String domain = domainOpt.get();
                if (seenDomains.contains(domain)) {
                    duplicatesSkipped++;
                } else {
                    seenDomains.add(domain);
                }
            }
        }

        // TODO: Implement full processing pipeline
        return ProcessingStatistics.builder()
            .totalResults(searchResults.size())
            .spamTldFiltered(0)
            .blacklistedSkipped(0)
            .duplicatesSkipped(duplicatesSkipped)
            .highConfidenceCreated(0)
            .lowConfidenceCreated(0)
            .build();
    }
}
