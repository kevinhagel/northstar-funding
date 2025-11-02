package com.northstar.funding.crawler.contract;

import com.northstar.funding.crawler.adapter.SearchProviderAdapter;
import com.northstar.funding.crawler.orchestrator.*;
import com.northstar.funding.domain.*;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests for MultiProviderSearchOrchestrator interface.
 *
 * TDD Approach: Written BEFORE implementation. Must PASS with test double.
 */
@DisplayName("MultiProviderSearchOrchestrator Contract Tests")
class MultiProviderSearchOrchestratorContractTest {

    private TestOrchestrator orchestrator;
    private UUID discoverySessionId;

    @BeforeEach
    void setUp() {
        discoverySessionId = UUID.randomUUID();
        orchestrator = new TestOrchestrator();
    }

    @Test
    @DisplayName("executeMultiProviderSearch() must return Try<SearchExecutionResult>")
    void executeMultiProviderSearch_MustReturnTry() {
        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai query",
                20,
                discoverySessionId
        );

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("executeMultiProviderSearch() with partial success must return Success with partial results")
    void executeMultiProviderSearch_PartialSuccess_MustReturnSuccessWithPartialResults() {
        // Given: Simulate 1 provider failure
        orchestrator.simulateProviderFailure = true;
        orchestrator.failedProviderCount = 1;

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai query",
                20,
                discoverySessionId
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        SearchExecutionResult execResult = result.get();
        assertThat(execResult.isPartialSuccess()).isTrue();
        assertThat(execResult.successfulResults()).isNotEmpty();
        assertThat(execResult.providerErrors()).hasSize(1);
    }

    @Test
    @DisplayName("executeMultiProviderSearch() with complete failure must return Failure only if all providers fail")
    void executeMultiProviderSearch_CompleteFailure_MustReturnFailure() {
        // Given: All providers fail
        orchestrator.simulateProviderFailure = true;
        orchestrator.failedProviderCount = 4;

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai query",
                20,
                discoverySessionId
        );

        // Then
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    @DisplayName("executeMultiProviderSearch() must complete within 10 seconds")
    void executeMultiProviderSearch_MustCompleteQuickly() {
        // When
        long startTime = System.nanoTime();
        orchestrator.executeMultiProviderSearch("query", "ai query", 20, discoverySessionId);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then
        assertThat(durationMs).isLessThan(10000);
    }

    @Test
    @DisplayName("aggregateResults() must deduplicate by domain")
    void aggregateResults_MustDeduplicateByDomain() {
        // Given: Same domain from 2 providers
        Map<SearchEngineType, List<SearchResult>> providerResults = Map.of(
                SearchEngineType.BRAVE, List.of(createSearchResult("example.org", 1)),
                SearchEngineType.SEARXNG, List.of(createSearchResult("example.org", 2))
        );

        // When
        List<SearchResult> aggregated = orchestrator.aggregateResults(providerResults);

        // Then
        assertThat(aggregated).hasSize(1); // Deduplicated
        assertThat(aggregated.get(0).getRankPosition()).isEqualTo(1); // Kept lowest position
    }

    @Test
    @DisplayName("aggregateResults() must return empty list if all providers returned empty")
    void aggregateResults_EmptyProviders_MustReturnEmptyList() {
        // Given
        Map<SearchEngineType, List<SearchResult>> emptyResults = Map.of(
                SearchEngineType.BRAVE, List.of(),
                SearchEngineType.SEARXNG, List.of()
        );

        // When
        List<SearchResult> aggregated = orchestrator.aggregateResults(emptyResults);

        // Then
        assertThat(aggregated).isEmpty();
    }

    @Test
    @DisplayName("SearchExecutionResult.isFullSuccess() must return true when no errors")
    void searchExecutionResult_IsFullSuccess() {
        // Given
        SearchExecutionResult result = new SearchExecutionResult(
                List.of(createSearchResult("example.org", 1)),
                List.of(), // No errors
                createStatistics(1, 1, 0, 0)
        );

        // Then
        assertThat(result.isFullSuccess()).isTrue();
        assertThat(result.isPartialSuccess()).isFalse();
        assertThat(result.isCompleteFailure()).isFalse();
    }

    @Test
    @DisplayName("SearchExecutionResult.isPartialSuccess() must return true when some providers fail")
    void searchExecutionResult_IsPartialSuccess() {
        // Given
        SearchExecutionResult result = new SearchExecutionResult(
                List.of(createSearchResult("example.org", 1)),
                List.of(createProviderError(SearchEngineType.SERPER)),
                createStatistics(1, 1, 0, 0)
        );

        // Then
        assertThat(result.isPartialSuccess()).isTrue();
        assertThat(result.isFullSuccess()).isFalse();
        assertThat(result.isCompleteFailure()).isFalse();
    }

    @Test
    @DisplayName("SearchExecutionResult.isCompleteFailure() must return true when all providers fail")
    void searchExecutionResult_IsCompleteFailure() {
        // Given
        SearchExecutionResult result = new SearchExecutionResult(
                List.of(), // No results
                List.of(
                        createProviderError(SearchEngineType.BRAVE),
                        createProviderError(SearchEngineType.SEARXNG),
                        createProviderError(SearchEngineType.SERPER),
                        createProviderError(SearchEngineType.TAVILY)
                ),
                createStatistics(0, 0, 0, 0)
        );

        // Then
        assertThat(result.isCompleteFailure()).isTrue();
        assertThat(result.isFullSuccess()).isFalse();
        assertThat(result.isPartialSuccess()).isFalse();
    }

    // Helper methods

    private SearchResult createSearchResult(String domain, int position) {
        return SearchResult.builder()
                .url("https://" + domain + "/page")
                .domain(domain)
                .title("Test Title")
                .description("Test description")
                .rankPosition(position)
                .searchEngine(SearchEngineType.BRAVE)
                .discoveredAt(LocalDateTime.now())
                .searchDate(LocalDate.now())
                .discoverySessionId(discoverySessionId)
                .build();
    }

    private ProviderError createProviderError(SearchEngineType provider) {
        return new ProviderError(
                provider,
                "Test error",
                ProviderError.ErrorType.TIMEOUT,
                LocalDateTime.now(),
                "test query"
        );
    }

    private SessionStatistics createStatistics(int totalResults, int newDomains, int duplicates, int spam) {
        return new SessionStatistics(
                totalResults,
                newDomains,
                duplicates,
                spam,
                totalResults / 4, // braveSearchResults
                totalResults / 4, // searxngResults
                totalResults / 4, // serperResults
                totalResults / 4  // tavilyResults
        );
    }

    /**
     * Test double implementation of MultiProviderSearchOrchestrator.
     */
    private static class TestOrchestrator implements MultiProviderSearchOrchestrator {

        boolean simulateProviderFailure = false;
        int failedProviderCount = 0;

        @Override
        public Try<SearchExecutionResult> executeMultiProviderSearch(
                String keywordQuery,
                String aiOptimizedQuery,
                int maxResultsPerProvider,
                UUID discoverySessionId
        ) {
            if (simulateProviderFailure && failedProviderCount == 4) {
                return Try.failure(new RuntimeException("All providers failed"));
            }

            List<SearchResult> results = new ArrayList<>();
            List<ProviderError> errors = new ArrayList<>();

            // Simulate successful providers
            int successfulProviders = 4 - failedProviderCount;
            for (int i = 0; i < successfulProviders; i++) {
                results.add(createResult("example" + i + ".org", i + 1, discoverySessionId));
            }

            // Simulate failed providers
            if (simulateProviderFailure) {
                for (int i = 0; i < failedProviderCount; i++) {
                    errors.add(new ProviderError(
                            SearchEngineType.BRAVE,
                            "Simulated failure",
                            ProviderError.ErrorType.TIMEOUT,
                            LocalDateTime.now(),
                            keywordQuery
                    ));
                }
            }

            SessionStatistics stats = new SessionStatistics(
                    results.size(), results.size(), 0, 0,
                    results.size() / 4, results.size() / 4,
                    results.size() / 4, results.size() / 4
            );

            return Try.success(new SearchExecutionResult(results, errors, stats));
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
            Map<String, SearchResult> deduped = new HashMap<>();

            for (List<SearchResult> results : providerResults.values()) {
                for (SearchResult result : results) {
                    String domain = result.getDomain();
                    if (!deduped.containsKey(domain) ||
                            result.getRankPosition() < deduped.get(domain).getRankPosition()) {
                        deduped.put(domain, result);
                    }
                }
            }

            return new ArrayList<>(deduped.values());
        }

        @Override
        public DiscoverySession updateSessionStatistics(UUID sessionId, SearchExecutionResult result) {
            // Stub implementation for contract test
            return DiscoverySession.builder()
                    .sessionId(sessionId)
                    .sessionType(SessionType.SCHEDULED)
                    .status(SessionStatus.COMPLETED)
                    .startedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .candidatesFound(result.statistics().totalResultsFound())
                    .duplicatesDetected(result.statistics().duplicateDomainsSkipped())
                    .build();
        }

        private static SearchResult createResult(String domain, int position, UUID sessionId) {
            return SearchResult.builder()
                    .url("https://" + domain + "/page")
                    .domain(domain)
                    .title("Title")
                    .description("Description")
                    .rankPosition(position)
                    .searchEngine(SearchEngineType.BRAVE)
                    .discoveredAt(LocalDateTime.now())
                    .searchDate(LocalDate.now())
                    .discoverySessionId(sessionId)
                    .build();
        }
    }
}
