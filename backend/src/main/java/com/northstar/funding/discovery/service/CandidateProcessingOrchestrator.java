package com.northstar.funding.discovery.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.Domain;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;
import com.northstar.funding.discovery.service.dto.MetadataJudgment;
import com.northstar.funding.discovery.service.dto.ProcessingStats;
import com.northstar.funding.discovery.service.dto.SearchResult;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Candidate Processing Orchestrator
 *
 * Orchestrates Phase 1 of Two-Phase Processing Pipeline:
 * - Process search results in parallel using Virtual Threads
 * - Check domain deduplication
 * - Judge metadata (no web crawling)
 * - Save high-confidence candidates as PENDING_CRAWL
 * - Skip low-confidence as SKIPPED_LOW_CONFIDENCE
 * - Update domain quality metrics
 *
 * Constitutional Principles:
 * - Virtual Threads (Java 25) for I/O concurrency
 * - Domain-level deduplication (avoid reprocessing)
 * - Simple orchestrator pattern (no Spring Integration)
 * - PostgreSQL for persistence (no Kafka)
 *
 * Example Usage:
 * <pre>
 * List<SearchResult> results = searchService.search("Bulgaria education grants");
 * ProcessingStats stats = orchestrator.processSearchResults(results, sessionId);
 * log.info(stats.getSummary());
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateProcessingOrchestrator {

    private final DomainRegistryService domainRegistryService;
    private final MetadataJudgingService metadataJudgingService;
    private final FundingSourceCandidateRepository candidateRepository;

    /**
     * Process search results using Phase 1 metadata judging
     *
     * Uses Virtual Threads for parallel processing (Java 25).
     * No web crawling - only analyzes search engine metadata.
     *
     * @param searchResults List of search results (typically 20-25 per query)
     * @param discoverySessionId Discovery session that generated these results
     * @return Processing statistics
     */
    public ProcessingStats processSearchResults(List<SearchResult> searchResults, UUID discoverySessionId) {
        long startTime = System.currentTimeMillis();

        log.info("Processing {} search results for session {}",
            searchResults.size(), discoverySessionId);

        // Use Virtual Threads for parallel processing
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ProcessingResult>> futures = searchResults.stream()
                .map(result -> CompletableFuture.supplyAsync(
                    () -> processSearchResult(result, discoverySessionId),
                    executor
                ))
                .toList();

            // Wait for all processing to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Aggregate results
            List<ProcessingResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

            // Calculate statistics
            ProcessingStats stats = aggregateStats(results, System.currentTimeMillis() - startTime);

            log.info("Phase 1 processing complete: {}", stats.getSummary());

            return stats;
        }
    }

    /**
     * Process a single search result
     *
     * Phase 1 workflow:
     * 1. Extract domain from URL
     * 2. Check if domain already processed or blacklisted
     * 3. Register domain if new
     * 4. Judge metadata (no HTTP request)
     * 5. If high confidence: Save candidate as PENDING_CRAWL
     * 6. If low confidence: Save as SKIPPED_LOW_CONFIDENCE (optional) or just skip
     * 7. Update domain quality metrics
     *
     * @param searchResult Search result to process
     * @param discoverySessionId Discovery session ID
     * @return Processing result
     */
    @Transactional
    public ProcessingResult processSearchResult(SearchResult searchResult, UUID discoverySessionId) {
        try {
            // Step 1: Check if domain should be processed
            if (!domainRegistryService.shouldProcessDomain(searchResult.getUrl())) {
                String domainName = domainRegistryService.extractDomainName(searchResult.getUrl())
                    .getOrElse("unknown");
                log.debug("Skipping result - domain already processed or blacklisted: {}", domainName);
                return ProcessingResult.skippedDomain(searchResult.getUrl(), domainName);
            }

            // Step 2: Register domain
            Try<Domain> domainTry = domainRegistryService.registerDomainFromUrl(
                searchResult.getUrl(),
                discoverySessionId
            );

            if (domainTry.isFailure()) {
                log.error("Failed to register domain for {}: {}",
                    searchResult.getUrl(), domainTry.getCause().getMessage());
                return ProcessingResult.failed(searchResult.getUrl(), domainTry.getCause().getMessage());
            }

            Domain domain = domainTry.get();

            // Step 3: Judge metadata (no web crawling)
            MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

            // Step 4: Decide whether to create candidate
            if (judgment.getShouldCrawl()) {
                // High confidence - create candidate for Phase 2 crawling
                FundingSourceCandidate candidate = createCandidate(
                    searchResult,
                    judgment,
                    domain.getDomainId(),
                    discoverySessionId
                );

                candidateRepository.save(candidate);

                // Update domain quality metrics
                domainRegistryService.updateDomainQuality(
                    domain.getDomainId(),
                    judgment.getConfidenceScore(),
                    true // high quality
                );

                log.debug("Created candidate (PENDING_CRAWL): {} (confidence: {})",
                    searchResult.getUrl(), judgment.getConfidenceScore());

                return ProcessingResult.candidateCreated(
                    searchResult.getUrl(),
                    candidate.getCandidateId(),
                    judgment.getConfidenceScore()
                );

            } else {
                // Low confidence - skip (optionally save as SKIPPED_LOW_CONFIDENCE)
                // For now, just update domain quality and skip
                domainRegistryService.updateDomainQuality(
                    domain.getDomainId(),
                    judgment.getConfidenceScore(),
                    false // low quality
                );

                log.debug("Skipped candidate (low confidence): {} (confidence: {})",
                    searchResult.getUrl(), judgment.getConfidenceScore());

                return ProcessingResult.skippedLowConfidence(
                    searchResult.getUrl(),
                    judgment.getConfidenceScore()
                );
            }

        } catch (Exception e) {
            log.error("Error processing search result {}: {}",
                searchResult.getUrl(), e.getMessage(), e);
            return ProcessingResult.failed(searchResult.getUrl(), e.getMessage());
        }
    }

    /**
     * Create FundingSourceCandidate from search result and judgment
     *
     * Status: PENDING_CRAWL (Phase 1 complete, awaiting Phase 2 crawl)
     */
    private FundingSourceCandidate createCandidate(
        SearchResult searchResult,
        MetadataJudgment judgment,
        UUID domainId,
        UUID discoverySessionId
    ) {
        return FundingSourceCandidate.builder()
            .status(CandidateStatus.PENDING_CRAWL)
            .confidenceScore(judgment.getConfidenceScore())
            .domainId(domainId)
            .discoverySessionId(discoverySessionId)
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .organizationName(judgment.getExtractedOrganizationName())
            .programName(judgment.getExtractedProgramName())
            .sourceUrl(searchResult.getUrl())
            .description(searchResult.getSnippet()) // Use snippet as initial description
            .discoveryMethod("METADATA_JUDGING")
            .searchQuery(searchResult.getSearchQuery())
            .validationNotes(judgment.getReasoning())
            .build();
    }

    /**
     * Aggregate processing results into statistics
     *
     * Uses BigDecimal for all confidence calculations with scale 2 and HALF_UP rounding
     */
    private ProcessingStats aggregateStats(List<ProcessingResult> results, long processingTimeMs) {
        int totalProcessed = results.size();
        int candidatesCreated = 0;
        int skippedLowConfidence = 0;
        int skippedDomain = 0;
        int failures = 0;

        List<BigDecimal> confidenceScores = new ArrayList<>();

        for (ProcessingResult result : results) {
            switch (result.getStatus()) {
                case CANDIDATE_CREATED:
                    candidatesCreated++;
                    if (result.getConfidenceScore() != null) {
                        confidenceScores.add(result.getConfidenceScore());
                    }
                    break;
                case SKIPPED_LOW_CONFIDENCE:
                    skippedLowConfidence++;
                    break;
                case SKIPPED_DOMAIN:
                    skippedDomain++;
                    break;
                case FAILED:
                    failures++;
                    break;
            }
        }

        // Calculate confidence statistics using BigDecimal arithmetic
        BigDecimal averageConfidence = null;
        BigDecimal maxConfidence = null;
        BigDecimal minConfidence = null;

        if (!confidenceScores.isEmpty()) {
            // Calculate average
            BigDecimal sum = confidenceScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageConfidence = sum.divide(
                BigDecimal.valueOf(confidenceScores.size()),
                2,
                RoundingMode.HALF_UP
            );

            // Find max and min
            maxConfidence = confidenceScores.stream()
                .max(BigDecimal::compareTo)
                .orElse(null);
            minConfidence = confidenceScores.stream()
                .min(BigDecimal::compareTo)
                .orElse(null);
        }

        return ProcessingStats.builder()
            .totalProcessed(totalProcessed)
            .candidatesCreated(candidatesCreated)
            .skippedLowConfidence(skippedLowConfidence)
            .skippedDomainAlreadyProcessed(skippedDomain)
            .skippedBlacklisted(0) // Not tracked separately yet
            .failures(failures)
            .averageConfidence(averageConfidence)
            .maxConfidence(maxConfidence)
            .minConfidence(minConfidence)
            .processingTimeMs(processingTimeMs)
            .build();
    }

    /**
     * Internal processing result for a single search result
     *
     * Uses BigDecimal for confidence scores (scale 2)
     */
    private static class ProcessingResult {
        private final String url;
        private final ResultStatus status;
        private final UUID candidateId;
        private final BigDecimal confidenceScore;
        private final String message;

        private ProcessingResult(String url, ResultStatus status, UUID candidateId,
                                BigDecimal confidenceScore, String message) {
            this.url = url;
            this.status = status;
            this.candidateId = candidateId;
            this.confidenceScore = confidenceScore;
            this.message = message;
        }

        static ProcessingResult candidateCreated(String url, UUID candidateId, BigDecimal confidence) {
            return new ProcessingResult(url, ResultStatus.CANDIDATE_CREATED,
                candidateId, confidence, null);
        }

        static ProcessingResult skippedLowConfidence(String url, BigDecimal confidence) {
            return new ProcessingResult(url, ResultStatus.SKIPPED_LOW_CONFIDENCE,
                null, confidence, null);
        }

        static ProcessingResult skippedDomain(String url, String domainName) {
            return new ProcessingResult(url, ResultStatus.SKIPPED_DOMAIN,
                null, null, domainName);
        }

        static ProcessingResult failed(String url, String message) {
            return new ProcessingResult(url, ResultStatus.FAILED,
                null, null, message);
        }

        public String getUrl() { return url; }
        public ResultStatus getStatus() { return status; }
        public UUID getCandidateId() { return candidateId; }
        public BigDecimal getConfidenceScore() { return confidenceScore; }
        public String getMessage() { return message; }
    }

    private enum ResultStatus {
        CANDIDATE_CREATED,
        SKIPPED_LOW_CONFIDENCE,
        SKIPPED_DOMAIN,
        FAILED
    }
}
