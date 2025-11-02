package com.northstar.funding.crawler.orchestrator;

import com.northstar.funding.crawler.adapter.BraveSearchAdapter;
import com.northstar.funding.crawler.adapter.SearchProviderAdapter;
import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.adapter.SerperAdapter;
import com.northstar.funding.crawler.adapter.TavilyAdapter;
import com.northstar.funding.crawler.antispam.AntiSpamFilter;
import com.northstar.funding.crawler.antispam.SpamAnalysisResult;
import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.Domain;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import com.northstar.funding.persistence.service.DomainService;
import com.northstar.funding.persistence.service.SearchResultService;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of MultiProviderSearchOrchestrator.
 *
 * Orchestrates parallel search execution across all 4 providers using Virtual Threads,
 * applies anti-spam filtering, performs domain deduplication, and updates session statistics.
 *
 * Flow:
 * 1. Execute all 4 providers in parallel (CompletableFuture.allOf)
 * 2. Apply anti-spam filtering to all results
 * 3. Check blacklist and perform domain deduplication
 * 4. Save SearchResult entities
 * 5. Update DiscoverySession statistics
 */
@Service
@Slf4j
public class MultiProviderSearchOrchestratorImpl implements MultiProviderSearchOrchestrator {

    private final BraveSearchAdapter braveSearchAdapter;
    private final SearxngAdapter searxngAdapter;
    private final SerperAdapter serperAdapter;
    private final TavilyAdapter tavilyAdapter;
    private final AntiSpamFilter antiSpamFilter;
    private final DomainService domainService;
    private final SearchResultService searchResultService;
    private final DiscoverySessionService discoverySessionService;
    private final ExecutorService virtualThreadExecutor;

    public MultiProviderSearchOrchestratorImpl(
            BraveSearchAdapter braveSearchAdapter,
            SearxngAdapter searxngAdapter,
            SerperAdapter serperAdapter,
            TavilyAdapter tavilyAdapter,
            AntiSpamFilter antiSpamFilter,
            DomainService domainService,
            SearchResultService searchResultService,
            DiscoverySessionService discoverySessionService,
            @Qualifier("searchExecutor") ExecutorService virtualThreadExecutor
    ) {
        this.braveSearchAdapter = braveSearchAdapter;
        this.searxngAdapter = searxngAdapter;
        this.serperAdapter = serperAdapter;
        this.tavilyAdapter = tavilyAdapter;
        this.antiSpamFilter = antiSpamFilter;
        this.domainService = domainService;
        this.searchResultService = searchResultService;
        this.discoverySessionService = discoverySessionService;
        this.virtualThreadExecutor = virtualThreadExecutor;

        log.info("MultiProviderSearchOrchestratorImpl initialized with 4 providers and Virtual Thread executor");
    }

    @Override
    public Try<SearchExecutionResult> executeMultiProviderSearch(
            String keywordQuery,
            String aiOptimizedQuery,
            int maxResultsPerProvider,
            UUID discoverySessionId
    ) {
        log.info("Starting multi-provider search: keyword='{}', ai='{}', maxResults={}, sessionId={}",
                keywordQuery, aiOptimizedQuery, maxResultsPerProvider, discoverySessionId);

        long startTime = System.currentTimeMillis();

        // Execute all 4 providers in parallel using Virtual Threads
        CompletableFuture<ProviderSearchResult> braveFuture = executeProviderAsync(
                braveSearchAdapter, keywordQuery, maxResultsPerProvider, discoverySessionId);

        CompletableFuture<ProviderSearchResult> searxngFuture = executeProviderAsync(
                searxngAdapter, keywordQuery, maxResultsPerProvider, discoverySessionId);

        CompletableFuture<ProviderSearchResult> serperFuture = executeProviderAsync(
                serperAdapter, keywordQuery, maxResultsPerProvider, discoverySessionId);

        CompletableFuture<ProviderSearchResult> tavilyFuture = executeProviderAsync(
                tavilyAdapter, aiOptimizedQuery, maxResultsPerProvider, discoverySessionId);

        // Wait for all providers to complete (or timeout after 10 seconds)
        try {
            CompletableFuture.allOf(braveFuture, searxngFuture, serperFuture, tavilyFuture)
                    .orTimeout(10, TimeUnit.SECONDS)
                    .join();

            // Collect results from all providers
            List<ProviderSearchResult> providerResults = List.of(
                    braveFuture.join(),
                    searxngFuture.join(),
                    serperFuture.join(),
                    tavilyFuture.join()
            );

            // Aggregate results and collect errors
            Map<SearchEngineType, List<SearchResult>> successfulResults = new HashMap<>();
            List<ProviderError> errors = new ArrayList<>();

            for (ProviderSearchResult result : providerResults) {
                if (result.isSuccess()) {
                    successfulResults.put(result.provider(), result.results());
                } else {
                    errors.add(result.error());
                }
            }

            // Check if all providers failed
            if (successfulResults.isEmpty()) {
                return Try.failure(new RuntimeException("All search providers failed: " + errors));
            }

            // Aggregate and deduplicate results
            List<SearchResult> aggregatedResults = aggregateResults(successfulResults);

            // Calculate statistics
            SessionStatistics statistics = calculateStatistics(successfulResults, aggregatedResults, errors.size());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Multi-provider search completed in {}ms: {} results, {} errors",
                    duration, aggregatedResults.size(), errors.size());

            SearchExecutionResult executionResult = new SearchExecutionResult(
                    aggregatedResults,
                    errors,
                    statistics
            );

            return Try.success(executionResult);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Multi-provider search failed after {}ms: {}", duration, e.getMessage(), e);
            return Try.failure(e);
        }
    }

    @Override
    public Try<List<SearchResult>> executeSingleProvider(
            SearchProviderAdapter adapter,
            String query,
            int maxResults,
            UUID discoverySessionId
    ) {
        return adapter.executeSearch(query, maxResults, discoverySessionId);
    }

    @Override
    public List<SearchResult> aggregateResults(Map<SearchEngineType, List<SearchResult>> providerResults) {
        log.debug("Aggregating results from {} providers", providerResults.size());

        // Combine all results
        List<SearchResult> allResults = providerResults.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (allResults.isEmpty()) {
            return List.of();
        }

        // Step 1: Apply anti-spam filtering
        List<SearchResult> nonSpamResults = allResults.stream()
                .filter(result -> {
                    SpamAnalysisResult spamAnalysis = antiSpamFilter.analyzeForSpam(result);
                    if (spamAnalysis.isSpam()) {
                        log.debug("Spam filtered: domain={}, reason={}",
                                result.getDomain(), spamAnalysis.rejectionReason());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        log.info("Anti-spam filtering: {} total -> {} non-spam ({} filtered)",
                allResults.size(), nonSpamResults.size(), allResults.size() - nonSpamResults.size());

        // Step 2: Domain deduplication
        Map<String, SearchResult> deduplicatedMap = new HashMap<>();

        for (SearchResult result : nonSpamResults) {
            String domain = result.getDomain();

            // Check if domain is blacklisted
            Optional<Domain> existingDomain = domainService.findByDomainName(domain);
            if (existingDomain.isPresent() && existingDomain.get().getStatus() == com.northstar.funding.domain.DomainStatus.BLACKLISTED) {
                log.debug("Blacklisted domain skipped: {}", domain);
                continue;
            }

            // Keep highest-ranked result per domain (lowest position number)
            if (!deduplicatedMap.containsKey(domain) ||
                    result.getRankPosition() < deduplicatedMap.get(domain).getRankPosition()) {
                deduplicatedMap.put(domain, result);
            }
        }

        List<SearchResult> deduplicatedResults = new ArrayList<>(deduplicatedMap.values());

        log.info("Domain deduplication: {} non-spam -> {} unique domains ({} duplicates)",
                nonSpamResults.size(), deduplicatedResults.size(), nonSpamResults.size() - deduplicatedResults.size());

        return deduplicatedResults;
    }

    @Override
    public DiscoverySession updateSessionStatistics(UUID sessionId, SearchExecutionResult result) {
        SessionStatistics stats = result.statistics();

        DiscoverySession session = discoverySessionService.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("DiscoverySession not found: " + sessionId));

        // Update statistics (using setter methods)
        session.setCandidatesFound(stats.totalResultsFound());
        session.setDuplicatesDetected(stats.duplicateDomainsSkipped());
        session.setCompletedAt(LocalDateTime.now());

        // Set status based on provider errors
        SessionStatus status;
        if (result.isFullSuccess()) {
            status = SessionStatus.COMPLETED;
        } else if (result.isCompleteFailure()) {
            status = SessionStatus.FAILED;
        } else {
            // Partial success - some providers worked, some failed
            // Since we don't have PARTIAL_SUCCESS, use COMPLETED with error tracking
            status = SessionStatus.COMPLETED;
        }

        return discoverySessionService.updateStatus(sessionId, status);
    }

    /**
     * Execute a single provider asynchronously using Virtual Threads.
     */
    private CompletableFuture<ProviderSearchResult> executeProviderAsync(
            SearchProviderAdapter adapter,
            String query,
            int maxResults,
            UUID discoverySessionId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            Try<List<SearchResult>> result = adapter.executeSearch(query, maxResults, discoverySessionId);

            if (result.isSuccess()) {
                return new ProviderSearchResult(
                        adapter.getProviderType(),
                        result.get(),
                        null
                );
            } else {
                // Convert Throwable to ProviderError
                Throwable error = result.getCause();
                ProviderError providerError = new ProviderError(
                        adapter.getProviderType(),
                        error.getMessage(),
                        classifyErrorType(error),
                        LocalDateTime.now(),
                        query
                );

                return new ProviderSearchResult(
                        adapter.getProviderType(),
                        List.of(),
                        providerError
                );
            }
        }, virtualThreadExecutor);
    }

    /**
     * Classify error type from Throwable.
     */
    private ProviderError.ErrorType classifyErrorType(Throwable error) {
        String className = error.getClass().getSimpleName();
        String message = error.getMessage() != null ? error.getMessage().toLowerCase() : "";

        if (className.contains("RateLimit") || message.contains("rate limit")) {
            return ProviderError.ErrorType.RATE_LIMIT;
        } else if (className.contains("Timeout") || message.contains("timeout")) {
            return ProviderError.ErrorType.TIMEOUT;
        } else if (className.contains("Authentication") || message.contains("unauthorized") || message.contains("forbidden")) {
            return ProviderError.ErrorType.AUTH_FAILURE;
        } else if (className.contains("IOException") || className.contains("ConnectException")) {
            return ProviderError.ErrorType.NETWORK_ERROR;
        } else if (message.contains("json") || message.contains("parse")) {
            return ProviderError.ErrorType.INVALID_RESPONSE;
        } else {
            return ProviderError.ErrorType.UNKNOWN;
        }
    }

    /**
     * Calculate session statistics from provider results.
     */
    private SessionStatistics calculateStatistics(
            Map<SearchEngineType, List<SearchResult>> successfulResults,
            List<SearchResult> aggregatedResults,
            int errorCount
    ) {
        int braveResults = successfulResults.getOrDefault(SearchEngineType.BRAVE, List.of()).size();
        int searxngResults = successfulResults.getOrDefault(SearchEngineType.SEARXNG, List.of()).size();
        int serperResults = successfulResults.getOrDefault(SearchEngineType.SERPER, List.of()).size();
        int tavilyResults = successfulResults.getOrDefault(SearchEngineType.TAVILY, List.of()).size();

        int totalRawResults = braveResults + searxngResults + serperResults + tavilyResults;
        int spamFiltered = totalRawResults - aggregatedResults.size();

        return new SessionStatistics(
                aggregatedResults.size(),  // totalResultsFound (after spam + dedup)
                aggregatedResults.size(),  // newDomainsDiscovered (approximation - actual count done in DB)
                0,  // duplicateDomainsSkipped (will be calculated by DomainService)
                spamFiltered,  // spamResultsFiltered
                braveResults,
                searxngResults,
                serperResults,
                tavilyResults
        );
    }

    /**
     * Internal record for provider search results.
     */
    private record ProviderSearchResult(
            SearchEngineType provider,
            List<SearchResult> results,
            ProviderError error
    ) {
        boolean isSuccess() {
            return error == null;
        }
    }
}
