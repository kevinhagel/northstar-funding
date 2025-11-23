package com.northstar.funding.searchadapters.workflow;

import com.northstar.funding.crawler.processing.ProcessingStatistics;
import com.northstar.funding.crawler.processing.SearchResult;
import com.northstar.funding.crawler.processing.SearchResultProcessor;
import com.northstar.funding.domain.*;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.model.ManualSearchRequest;
import com.northstar.funding.searchadapters.model.SearchWorkflowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Orchestrates the complete search workflow for funding discovery.
 *
 * <p>Workflow Steps:
 * 1. Get categories for day from DayOfWeekCategories
 * 2. Create DiscoverySession (type: NIGHTLY_AUTOMATED)
 * 3. For each category: generate queries via QueryGenerationService
 * 4. For each query: execute searches in parallel across all adapters (Virtual Threads)
 * 5. Process results with SearchResultProcessor (confidence scoring, deduplication, domain registration)
 * 6. Track statistics (zero results, results by engine)
 * 7. Update session with final statistics
 * 8. Return SearchWorkflowResult
 *
 * <p>Features:
 * - Java 25 Virtual Threads for parallel execution
 * - Adapter failure resilience (skip failed, continue with successful)
 * - Zero-result tracking for adapter effectiveness analysis
 * - Comprehensive statistics and metrics
 */
@Service
@Transactional
public class SearchWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(SearchWorkflowService.class);

    private final QueryGenerationService queryGenerationService;
    private final List<SearchAdapter> searchAdapters;
    private final SearchResultProcessor searchResultProcessor;
    private final DiscoverySessionService discoverySessionService;
    private final ExecutorService executorService;

    public SearchWorkflowService(
            QueryGenerationService queryGenerationService,
            List<SearchAdapter> searchAdapters,
            SearchResultProcessor searchResultProcessor,
            DiscoverySessionService discoverySessionService) {

        this.queryGenerationService = queryGenerationService;
        this.searchAdapters = searchAdapters != null ? searchAdapters : List.of();
        this.searchResultProcessor = searchResultProcessor;
        this.discoverySessionService = discoverySessionService;
        // Java 25 Virtual Threads for efficient concurrent execution
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Execute nightly automated search for a specific day of week.
     *
     * @param dayOfWeek Day of week (determines which categories to search)
     * @return SearchWorkflowResult with comprehensive statistics
     */
    public SearchWorkflowResult executeNightlySearch(DayOfWeek dayOfWeek) {
        logger.info("=== Starting Nightly Search for {} ===", dayOfWeek);
        Instant startTime = Instant.now();

        // Get categories for this day
        List<FundingSearchCategory> categories = DayOfWeekCategories.getCategories(dayOfWeek);
        logger.info("Categories for {}: {}", dayOfWeek, categories);

        // Create discovery session
        DiscoverySession session = DiscoverySession.builder()
            .sessionType(SessionType.SCHEDULED)
            .executedBy("SYSTEM")
            .status(SessionStatus.RUNNING)
            .build();
        session = discoverySessionService.createSession(session);
        logger.info("Created DiscoverySession: sessionId={}", session.getSessionId());

        // Execute workflow
        SearchWorkflowResult result = executeWorkflow(session, categories, searchAdapters, 10);

        // Calculate duration
        Duration duration = Duration.between(startTime, Instant.now());
        result = SearchWorkflowResult.builder()
            .sessionId(result.getSessionId())
            .queriesGenerated(result.getQueriesGenerated())
            .totalResultsFound(result.getTotalResultsFound())
            .candidatesCreated(result.getCandidatesCreated())
            .duplicatesSkipped(result.getDuplicatesSkipped())
            .blacklistedSkipped(result.getBlacklistedSkipped())
            .lowConfidenceSkipped(result.getLowConfidenceSkipped())
            .zeroResultCount(result.getZeroResultCount())
            .resultsByEngine(result.getResultsByEngine())
            .zeroResultsByEngine(result.getZeroResultsByEngine())
            .executionDuration(duration)
            .hasFailures(result.isHasFailures())
            .failureMessages(result.getFailureMessages())
            .build();

        logger.info("=== Nightly Search Complete: {} queries, {} results, {} candidates, {} minutes ===",
            result.getQueriesGenerated(),
            result.getTotalResultsFound(),
            result.getCandidatesCreated(),
            duration.toMinutes());

        return result;
    }

    /**
     * Execute manual search for specific categories and engines.
     *
     * @param request ManualSearchRequest with categories, engines, parameters
     * @return SearchWorkflowResult with comprehensive statistics
     */
    public SearchWorkflowResult executeManualSearch(ManualSearchRequest request) {
        logger.info("=== Starting Manual Search: {} categories, {} engines ===",
            request.getCategories().size(), request.getEngines().size());
        Instant startTime = Instant.now();

        // Create discovery session
        DiscoverySession session = DiscoverySession.builder()
            .sessionType(SessionType.MANUAL)
            .executedBy("ADMIN")
            .status(SessionStatus.RUNNING)
            .build();
        session = discoverySessionService.createSession(session);
        logger.info("Created DiscoverySession: sessionId={}", session.getSessionId());

        // Filter adapters to only requested engines
        List<SearchAdapter> requestedAdapters = searchAdapters.stream()
            .filter(adapter -> request.getEngines().contains(adapter.getEngineType()))
            .toList();

        logger.info("Requested adapters: {}",
            requestedAdapters.stream()
                .map(a -> a.getEngineType().name())
                .collect(Collectors.joining(", ")));

        // Execute workflow
        SearchWorkflowResult result = executeWorkflow(
            session,
            request.getCategories(),
            requestedAdapters,
            request.getMaxResultsPerQuery()
        );

        // Calculate duration
        Duration duration = Duration.between(startTime, Instant.now());
        result = SearchWorkflowResult.builder()
            .sessionId(result.getSessionId())
            .queriesGenerated(result.getQueriesGenerated())
            .totalResultsFound(result.getTotalResultsFound())
            .candidatesCreated(result.getCandidatesCreated())
            .duplicatesSkipped(result.getDuplicatesSkipped())
            .blacklistedSkipped(result.getBlacklistedSkipped())
            .lowConfidenceSkipped(result.getLowConfidenceSkipped())
            .zeroResultCount(result.getZeroResultCount())
            .resultsByEngine(result.getResultsByEngine())
            .zeroResultsByEngine(result.getZeroResultsByEngine())
            .executionDuration(duration)
            .hasFailures(result.isHasFailures())
            .failureMessages(result.getFailureMessages())
            .build();

        logger.info("=== Manual Search Complete: {} minutes ===", duration.toMinutes());

        return result;
    }

    /**
     * Core workflow execution logic (used by both nightly and manual searches).
     */
    private SearchWorkflowResult executeWorkflow(
            DiscoverySession session,
            List<FundingSearchCategory> categories,
            List<SearchAdapter> adapters,
            int maxResultsPerQuery) {

        // Statistics tracking
        int queriesGenerated = 0;
        int totalResultsFound = 0;
        int zeroResultCount = 0;
        int candidatesCreated = 0;
        int duplicatesSkipped = 0;
        int blacklistedSkipped = 0;
        int lowConfidenceSkipped = 0;
        Map<SearchEngineType, Integer> resultsByEngine = new HashMap<>();
        Map<SearchEngineType, Integer> zeroResultsByEngine = new HashMap<>();
        List<String> failureMessages = new ArrayList<>();

        // Initialize engine counters
        for (SearchAdapter adapter : adapters) {
            resultsByEngine.put(adapter.getEngineType(), 0);
            zeroResultsByEngine.put(adapter.getEngineType(), 0);
        }

        // Step 1: Generate queries for all categories
        List<String> allQueries = new ArrayList<>();
        for (FundingSearchCategory category : categories) {
            logger.info("Generating queries for category: {}", category);

            // Generate queries for each search engine
            for (SearchAdapter adapter : adapters) {
                QueryGenerationRequest request = QueryGenerationRequest.builder()
                    .searchEngine(adapter.getEngineType())
                    .categories(Set.of(category))
                    .geographic(GeographicScope.BULGARIA)
                    .maxQueries(maxResultsPerQuery)
                    .sessionId(session.getSessionId())
                    .build();

                try {
                    QueryGenerationResponse response = queryGenerationService.generateQueries(request).join();
                    List<String> queries = response.getQueries();
                    allQueries.addAll(queries);
                    queriesGenerated += queries.size();
                    logger.info("Generated {} queries for {} using {}", queries.size(), category, adapter.getEngineType());
                } catch (Exception e) {
                    logger.error("Failed to generate queries for {} using {}: {}",
                        category, adapter.getEngineType(), e.getMessage());
                    failureMessages.add(String.format("Query generation failed for %s: %s",
                        adapter.getEngineType(), e.getMessage()));
                }
            }
        }

        logger.info("Total queries generated: {}", queriesGenerated);

        // Step 2: Execute searches in parallel across all adapters
        logger.info("Executing {} queries across {} adapters in parallel", allQueries.size(), adapters.size());

        for (String query : allQueries) {
            List<CompletableFuture<SearchExecutionResult>> futures = new ArrayList<>();

            for (SearchAdapter adapter : adapters) {
                if (!adapter.isAvailable()) {
                    logger.warn("Adapter {} is not available, skipping", adapter.getEngineType());
                    continue;
                }

                CompletableFuture<SearchExecutionResult> future = CompletableFuture.supplyAsync(
                    () -> executeSearchSafely(adapter, query, maxResultsPerQuery),
                    executorService
                );
                futures.add(future);
            }

            // Wait for all adapters to complete this query
            for (CompletableFuture<SearchExecutionResult> future : futures) {
                try {
                    SearchExecutionResult execResult = future.join();

                    if (execResult.isSuccess()) {
                        List<SearchResult> results = execResult.getResults();
                        SearchEngineType engineType = execResult.getEngineType();

                        if (results.isEmpty()) {
                            // Zero results - track it
                            zeroResultCount++;
                            zeroResultsByEngine.merge(engineType, 1, Integer::sum);
                            logger.info("Zero results: engine={}, query='{}'", engineType, query);
                        } else {
                            // Process results
                            totalResultsFound += results.size();
                            resultsByEngine.merge(engineType, results.size(), Integer::sum);
                            logger.info("Search success: engine={}, query='{}', results={}",
                                engineType, query, results.size());

                            // Step 3: Process results (confidence scoring, deduplication, domain registration)
                            ProcessingStatistics stats = searchResultProcessor.processSearchResults(
                                results,
                                session.getSessionId()
                            );

                            // Accumulate statistics
                            candidatesCreated += stats.getHighConfidenceCreated() + stats.getLowConfidenceCreated();
                            duplicatesSkipped += stats.getDuplicatesSkipped();
                            blacklistedSkipped += stats.getBlacklistedSkipped();
                            lowConfidenceSkipped += stats.getLowConfidenceCreated();
                        }
                    } else {
                        // Adapter failure
                        failureMessages.add(execResult.getErrorMessage());
                        logger.error("Search failed: {}", execResult.getErrorMessage());
                    }

                } catch (Exception e) {
                    logger.error("Failed to retrieve search results: {}", e.getMessage());
                    failureMessages.add("Unknown error: " + e.getMessage());
                }
            }
        }

        // Step 4: Return final statistics
        return SearchWorkflowResult.builder()
            .sessionId(session.getSessionId())
            .queriesGenerated(queriesGenerated)
            .totalResultsFound(totalResultsFound)
            .candidatesCreated(candidatesCreated)
            .duplicatesSkipped(duplicatesSkipped)
            .blacklistedSkipped(blacklistedSkipped)
            .lowConfidenceSkipped(lowConfidenceSkipped)
            .zeroResultCount(zeroResultCount)
            .resultsByEngine(resultsByEngine)
            .zeroResultsByEngine(zeroResultsByEngine)
            .executionDuration(Duration.ZERO) // Will be set by caller
            .hasFailures(!failureMessages.isEmpty())
            .failureMessages(failureMessages)
            .build();
    }

    /**
     * Execute single search with exception handling.
     */
    private SearchExecutionResult executeSearchSafely(SearchAdapter adapter, String query, int maxResults) {
        try {
            List<com.northstar.funding.domain.SearchResult> domainResults = adapter.search(query, maxResults);

            // Convert domain SearchResult to crawler SearchResult
            List<SearchResult> crawlerResults = domainResults.stream()
                .map(dr -> SearchResult.builder()
                    .title(dr.getTitle())
                    .description(dr.getDescription())
                    .url(dr.getUrl())
                    .build())
                .toList();

            return new SearchExecutionResult(true, adapter.getEngineType(), crawlerResults, null);
        } catch (Exception e) {
            String errorMsg = String.format("%s: %s", adapter.getEngineType(), e.getMessage());
            return new SearchExecutionResult(false, adapter.getEngineType(), List.of(), errorMsg);
        }
    }

    /**
     * Internal result holder for search execution.
     */
    private static class SearchExecutionResult {
        private final boolean success;
        private final SearchEngineType engineType;
        private final List<SearchResult> results;
        private final String errorMessage;

        public SearchExecutionResult(boolean success, SearchEngineType engineType,
                                    List<SearchResult> results, String errorMessage) {
            this.success = success;
            this.engineType = engineType;
            this.results = results;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public SearchEngineType getEngineType() { return engineType; }
        public List<SearchResult> getResults() { return results; }
        public String getErrorMessage() { return errorMessage; }
    }
}
