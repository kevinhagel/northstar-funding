package com.northstar.funding.crawler.service;

import com.northstar.funding.crawler.orchestrator.MultiProviderSearchOrchestrator;
import com.northstar.funding.crawler.orchestrator.SearchExecutionResult;
import com.northstar.funding.crawler.processing.ProcessingStatistics;
import com.northstar.funding.crawler.processing.SearchResultProcessor;
import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.domain.SessionType;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Main orchestrator for scheduled crawl execution.
 *
 * Implements the complete flow described in Feature 014:
 * 1. Query generation (currently uses simple default query)
 * 2. Multi-provider search execution
 * 3. Result processing with confidence scoring
 * 4. Candidate creation (high confidence >= 0.60)
 * 5. Statistics tracking
 *
 * Can be triggered via:
 * - CLI (SimpleCrawlRunner)
 * - REST API (future)
 * - Scheduled job (future)
 */
@Service
@Slf4j
@Transactional
public class ScheduledCrawlService {

    private final MultiProviderSearchOrchestrator searchOrchestrator;
    private final SearchResultProcessor resultProcessor;
    private final DiscoverySessionService discoverySessionService;

    public ScheduledCrawlService(
            MultiProviderSearchOrchestrator searchOrchestrator,
            SearchResultProcessor resultProcessor,
            DiscoverySessionService discoverySessionService
    ) {
        this.searchOrchestrator = searchOrchestrator;
        this.resultProcessor = resultProcessor;
        this.discoverySessionService = discoverySessionService;
    }

    /**
     * Execute a complete scheduled crawl.
     *
     * @param query Search query to execute
     * @param sessionType Type of session (MANUAL, SCHEDULED, etc.)
     * @param executedBy Who triggered the session
     * @return Crawl result with statistics
     */
    public CrawlResult executeCrawl(String query, SessionType sessionType, String executedBy) {
        log.info("Starting scheduled crawl: query='{}', type={}, executedBy={}",
                query, sessionType, executedBy);

        long startTime = System.currentTimeMillis();

        // Step 1: Create discovery session
        DiscoverySession session = DiscoverySession.builder()
                .sessionType(sessionType)
                .executedBy(executedBy)
                .startedAt(LocalDateTime.now())
                .status(SessionStatus.RUNNING)
                .build();

        DiscoverySession savedSession = discoverySessionService.createSession(session);
        UUID sessionId = savedSession.getSessionId();

        try {
            // Step 2: Execute multi-provider search
            log.info("Executing multi-provider search for session {}", sessionId);
            Try<SearchExecutionResult> searchResult = searchOrchestrator.executeMultiProviderSearch(
                    query,
                    null,  // aiOptimizedQuery - future enhancement
                    20,    // maxResultsPerProvider
                    sessionId
            );

            if (searchResult.isFailure()) {
                log.error("Multi-provider search failed for session {}: {}",
                        sessionId, searchResult.getCause().getMessage());
                markSessionFailed(sessionId, searchResult.getCause().getMessage());
                return CrawlResult.failure(sessionId, searchResult.getCause().getMessage());
            }

            SearchExecutionResult executionResult = searchResult.get();
            List<com.northstar.funding.domain.SearchResult> domainSearchResults =
                    executionResult.successfulResults();

            log.info("Multi-provider search completed for session {}: {} results from {} providers",
                    sessionId, domainSearchResults.size(),
                    executionResult.statistics().totalResultsFound());

            // Step 3: Convert domain SearchResult to processing SearchResult DTO
            List<com.northstar.funding.crawler.processing.SearchResult> processingResults =
                    domainSearchResults.stream()
                            .map(this::convertToProcessingResult)
                            .toList();

            // Step 4: Process results (confidence scoring + candidate creation)
            log.info("Processing {} search results for session {}", processingResults.size(), sessionId);
            ProcessingStatistics processingStats = resultProcessor.processSearchResults(
                    processingResults,
                    sessionId
            );

            log.info("Search result processing completed for session {}: {} candidates created " +
                            "(high: {}, low: {}), {} spam filtered, {} duplicates, {} blacklisted",
                    sessionId,
                    processingStats.getHighConfidenceCreated() + processingStats.getLowConfidenceCreated(),
                    processingStats.getHighConfidenceCreated(),
                    processingStats.getLowConfidenceCreated(),
                    processingStats.getSpamTldFiltered(),
                    processingStats.getDuplicatesSkipped(),
                    processingStats.getBlacklistedSkipped());

            // Step 5: Update session statistics and mark complete
            discoverySessionService.completeSession(
                    sessionId,
                    processingStats.getHighConfidenceCreated(),
                    processingStats.getDuplicatesSkipped(),
                    0  // sourcesScraped - not yet implemented (Phase 2)
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info("Scheduled crawl completed successfully for session {} in {}ms: " +
                            "{} total results → {} candidates created",
                    sessionId, duration, domainSearchResults.size(),
                    processingStats.getHighConfidenceCreated());

            return CrawlResult.success(
                    sessionId,
                    processingStats.getHighConfidenceCreated(),
                    processingStats.getLowConfidenceCreated(),
                    processingStats.getSpamTldFiltered(),
                    processingStats.getDuplicatesSkipped(),
                    processingStats.getBlacklistedSkipped(),
                    duration
            );

        } catch (Exception e) {
            log.error("Scheduled crawl failed for session {}: {}", sessionId, e.getMessage(), e);
            markSessionFailed(sessionId, e.getMessage());
            return CrawlResult.failure(sessionId, e.getMessage());
        }
    }

    /**
     * Convert domain SearchResult entity to processing SearchResult DTO.
     */
    private com.northstar.funding.crawler.processing.SearchResult convertToProcessingResult(
            com.northstar.funding.domain.SearchResult domainResult) {
        return com.northstar.funding.crawler.processing.SearchResult.builder()
                .title(domainResult.getTitle())
                .description(domainResult.getDescription())
                .url(domainResult.getUrl())
                .build();
    }

    /**
     * Mark session as failed.
     */
    private void markSessionFailed(UUID sessionId, String errorMessage) {
        try {
            discoverySessionService.failSession(sessionId, List.of(errorMessage));
        } catch (Exception e) {
            log.error("Failed to mark session {} as failed: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Result of a scheduled crawl execution.
     */
    public record CrawlResult(
            UUID sessionId,
            boolean success,
            String errorMessage,
            int highConfidenceCandidatesCreated,
            int lowConfidenceCandidatesCreated,
            int spamFiltered,
            int duplicatesSkipped,
            int blacklistedSkipped,
            long durationMs
    ) {
        public static CrawlResult success(
                UUID sessionId,
                int highConfidence,
                int lowConfidence,
                int spamFiltered,
                int duplicates,
                int blacklisted,
                long duration
        ) {
            return new CrawlResult(
                    sessionId,
                    true,
                    null,
                    highConfidence,
                    lowConfidence,
                    spamFiltered,
                    duplicates,
                    blacklisted,
                    duration
            );
        }

        public static CrawlResult failure(UUID sessionId, String errorMessage) {
            return new CrawlResult(
                    sessionId,
                    false,
                    errorMessage,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );
        }
    }
}
