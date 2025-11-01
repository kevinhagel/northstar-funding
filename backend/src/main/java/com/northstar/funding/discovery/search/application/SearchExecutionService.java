package com.northstar.funding.discovery.search.application;

import com.northstar.funding.discovery.search.domain.SearchQuery;
import com.northstar.funding.discovery.search.infrastructure.adapters.SearchEngineAdapter;
import com.northstar.funding.discovery.search.infrastructure.adapters.SearchEngineAdapter.SearchResult;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Search execution service with Virtual Threads parallelism (Feature 003)
 *
 * Orchestrates search execution across multiple search engines in parallel.
 * Uses Java 25 Virtual Threads for efficient concurrent I/O operations.
 *
 * Key Features:
 * - Parallel execution across all enabled search engines
 * - Virtual Threads (lightweight, efficient for I/O-bound operations)
 * - Domain-level deduplication (same domain from multiple engines)
 * - Graceful degradation (continues if one engine fails)
 *
 * Performance Goals:
 * - Single query across 3 engines: <5 seconds (parallel)
 * - 10 queries × 3 engines: <30 minutes (sequential queries, parallel engines)
 *
 * Constitutional Compliance:
 * - Java 25 Virtual Threads (constitutional requirement)
 * - Vavr Try for error handling
 * - Simple orchestration (no Kafka, no Spring Integration)
 *
 * @author NorthStar Funding Team
 */
@Service
@Slf4j
public class SearchExecutionService {

    private final List<SearchEngineAdapter> adapters;

    public SearchExecutionService(List<SearchEngineAdapter> adapters) {
        this.adapters = adapters;
        log.info("SearchExecutionService initialized with {} adapters: {}",
            adapters.size(),
            adapters.stream().map(a -> a.getEngineType().name()).toList());
    }

    /**
     * Execute a single query across all enabled search engines in parallel
     *
     * @param query The search query to execute
     * @return List of deduplicated search results from all engines
     */
    public Try<List<SearchResult>> executeQueryAcrossEngines(SearchQuery query) {
        var queryText = query.getQueryText();
        var targetEngines = query.getParsedTargetEngines();
        var maxResults = query.getExpectedResults();

        log.info("Executing query across engines: query='{}', targetEngines={}, maxResults={}",
            queryText, targetEngines, maxResults);

        return Try.of(() -> {
            // Filter adapters to only target engines specified in query
            var targetAdapters = adapters.stream()
                .filter(SearchEngineAdapter::isEnabled)
                .filter(adapter -> targetEngines.isEmpty() || targetEngines.contains(adapter.getEngineType()))
                .toList();

            if (targetAdapters.isEmpty()) {
                log.warn("No enabled adapters available for query: {}", queryText);
                return List.<SearchResult>of();
            }

            // Execute searches in parallel using Virtual Threads
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = targetAdapters.stream()
                    .map(adapter -> executor.submit(() -> {
                        log.debug("Submitting search to {}: query='{}'",
                            adapter.getEngineType(), queryText);
                        return adapter.search(queryText, maxResults);
                    }))
                    .toList();

                // Collect results from all engines
                var allResults = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(); // Wait for result
                        } catch (Exception e) {
                            log.error("Error getting future result: {}", e.getMessage(), e);
                            return Try.<List<SearchResult>>failure(e);
                        }
                    })
                    .filter(Try::isSuccess)
                    .flatMap(t -> t.get().stream())
                    .toList();

                // Deduplicate by domain
                var deduplicated = deduplicateByDomain(allResults);

                log.info("Query execution complete: query='{}', engines={}, total={}, unique={}",
                    queryText, targetAdapters.size(), allResults.size(), deduplicated.size());

                return deduplicated;
            }
        });
    }

    /**
     * Execute multiple queries sequentially, each across all engines in parallel
     *
     * @param queries List of queries to execute
     * @return List of all search results (deduplicated per query)
     */
    public Try<List<SearchResult>> executeQueries(List<SearchQuery> queries) {
        log.info("Executing {} queries sequentially", queries.size());

        return Try.of(() -> queries.stream()
            .flatMap(query -> {
                var result = executeQueryAcrossEngines(query);
                if (result.isSuccess()) {
                    return result.get().stream();
                } else {
                    log.error("Query failed: query='{}', error={}",
                        query.getQueryText(), result.getCause().getMessage());
                    return List.<SearchResult>of().stream();
                }
            })
            .toList());
    }

    /**
     * Deduplicate search results by domain
     * Keeps first occurrence of each domain
     *
     * @param results List of search results
     * @return Deduplicated list
     */
    private List<SearchResult> deduplicateByDomain(List<SearchResult> results) {
        Set<String> seenDomains = new java.util.HashSet<>();

        return results.stream()
            .filter(result -> {
                var domain = extractDomain(result.url());
                if (seenDomains.contains(domain)) {
                    log.debug("Duplicate domain found: {} (from {})", domain, result.source());
                    return false;
                }
                seenDomains.add(domain);
                return true;
            })
            .toList();
    }

    /**
     * Extract domain from URL
     *
     * @param url The URL
     * @return Domain (e.g., "example.org")
     */
    private String extractDomain(String url) {
        try {
            var uri = java.net.URI.create(url);
            var host = uri.getHost();
            return host != null ? host.toLowerCase() : url;
        } catch (Exception e) {
            log.warn("Failed to extract domain from URL: {}", url);
            return url;
        }
    }

    /**
     * Get statistics for all adapters
     *
     * @return Map of engine type to enabled status
     */
    public java.util.Map<String, Boolean> getAdapterStatuses() {
        return adapters.stream()
            .collect(Collectors.toMap(
                adapter -> adapter.getEngineType().name(),
                SearchEngineAdapter::isEnabled
            ));
    }
}
