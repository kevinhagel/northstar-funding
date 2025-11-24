package com.northstar.funding.crawler.unit;

import com.northstar.funding.crawler.adapter.BraveSearchAdapter;
import com.northstar.funding.crawler.adapter.PerplexicaAdapter;
import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.adapter.SerperAdapter;
import com.northstar.funding.crawler.adapter.TavilyAdapter;
import com.northstar.funding.crawler.antispam.AntiSpamFilter;
import com.northstar.funding.crawler.antispam.SpamAnalysisResult;
import com.northstar.funding.crawler.antispam.SpamIndicator;
import com.northstar.funding.crawler.orchestrator.*;
import com.northstar.funding.domain.*;
import com.northstar.funding.persistence.service.DiscoverySessionService;
import com.northstar.funding.persistence.service.DomainService;
import com.northstar.funding.persistence.service.SearchResultService;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MultiProviderSearchOrchestratorImpl.
 *
 * Tests:
 * - Multi-provider parallel execution using Virtual Threads
 * - Partial success handling (some providers fail, some succeed)
 * - Anti-spam filtering before domain deduplication
 * - Domain deduplication (keep highest rank per domain)
 * - Blacklist checking
 * - Session statistics calculation
 * - Error classification and tracking
 * - Timeout handling
 * - Full success, partial success, and complete failure scenarios
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MultiProviderSearchOrchestratorImpl Unit Tests")
class MultiProviderSearchOrchestratorImplTest {

    @Mock
    private BraveSearchAdapter braveSearchAdapter;

    @Mock
    private SearxngAdapter searxngAdapter;

    @Mock
    private SerperAdapter serperAdapter;

    @Mock
    private TavilyAdapter tavilyAdapter;

    @Mock
    private PerplexicaAdapter perplexicaAdapter;

    @Mock
    private AntiSpamFilter antiSpamFilter;

    @Mock
    private DomainService domainService;

    @Mock
    private SearchResultService searchResultService;

    @Mock
    private DiscoverySessionService discoverySessionService;

    private ExecutorService virtualThreadExecutor;

    private MultiProviderSearchOrchestratorImpl orchestrator;

    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        // Use real Virtual Thread executor for parallel execution testing
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        testSessionId = UUID.randomUUID();

        // Configure adapters to return their provider type
        when(braveSearchAdapter.getProviderType()).thenReturn(SearchEngineType.BRAVE);
        when(searxngAdapter.getProviderType()).thenReturn(SearchEngineType.SEARXNG);
        when(serperAdapter.getProviderType()).thenReturn(SearchEngineType.SERPER);
        when(tavilyAdapter.getProviderType()).thenReturn(SearchEngineType.TAVILY);
        when(perplexicaAdapter.getProviderType()).thenReturn(SearchEngineType.PERPLEXICA);

        orchestrator = new MultiProviderSearchOrchestratorImpl(
                braveSearchAdapter,
                searxngAdapter,
                serperAdapter,
                tavilyAdapter,
                perplexicaAdapter,
                antiSpamFilter,
                domainService,
                searchResultService,
                discoverySessionService,
                virtualThreadExecutor
        );
    }

    @Test
    @DisplayName("Full success - all 4 providers return results")
    void executeMultiProviderSearch_AllProvidersSucceed_ReturnsFullSuccess() {
        // Given: All providers return successful results
        List<SearchResult> braveResults = List.of(createSearchResult("brave.com", 1, SearchEngineType.BRAVE));
        List<SearchResult> searxngResults = List.of(createSearchResult("searxng.org", 2, SearchEngineType.SEARXNG));
        List<SearchResult> serperResults = List.of(createSearchResult("serper.dev", 3, SearchEngineType.SERPER));
        List<SearchResult> tavilyResults = List.of(createSearchResult("tavily.com", 4, SearchEngineType.TAVILY));

        when(braveSearchAdapter.executeSearch(eq("keyword query"), eq(20), any()))
                .thenReturn(Try.success(braveResults));
        when(searxngAdapter.executeSearch(eq("keyword query"), eq(20), any()))
                .thenReturn(Try.success(searxngResults));
        when(serperAdapter.executeSearch(eq("keyword query"), eq(20), any()))
                .thenReturn(Try.success(serperResults));
        when(tavilyAdapter.executeSearch(eq("ai optimized query"), eq(20), any()))
                .thenReturn(Try.success(tavilyResults));

        // Anti-spam filter passes all results
        when(antiSpamFilter.analyzeForSpam(any())).thenReturn(SpamAnalysisResult.notSpam());

        // Domain service finds no blacklisted domains
        when(domainService.findByDomainName(anyString())).thenReturn(Optional.empty());

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai optimized query",
                20,
                testSessionId
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        SearchExecutionResult execResult = result.get();

        assertThat(execResult.isFullSuccess()).isTrue();
        assertThat(execResult.isPartialSuccess()).isFalse();
        assertThat(execResult.isCompleteFailure()).isFalse();
        assertThat(execResult.providerErrors()).isEmpty();
        assertThat(execResult.successfulResults()).hasSize(4);

        // Verify all 4 providers were called
        verify(braveSearchAdapter).executeSearch(eq("keyword query"), eq(20), any());
        verify(searxngAdapter).executeSearch(eq("keyword query"), eq(20), any());
        verify(serperAdapter).executeSearch(eq("keyword query"), eq(20), any());
        verify(tavilyAdapter).executeSearch(eq("ai optimized query"), eq(20), any());
    }

    @Test
    @DisplayName("Partial success - 1 provider fails, 3 succeed")
    void executeMultiProviderSearch_OneProviderFails_ReturnsPartialSuccess() {
        // Given: Brave fails, others succeed
        when(braveSearchAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(new RuntimeException("Rate limit exceeded")));

        List<SearchResult> searxngResults = List.of(createSearchResult("searxng.org", 1, SearchEngineType.SEARXNG));
        List<SearchResult> serperResults = List.of(createSearchResult("serper.dev", 2, SearchEngineType.SERPER));
        List<SearchResult> tavilyResults = List.of(createSearchResult("tavily.com", 3, SearchEngineType.TAVILY));

        when(searxngAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(searxngResults));
        when(serperAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(serperResults));
        when(tavilyAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(tavilyResults));

        when(antiSpamFilter.analyzeForSpam(any())).thenReturn(SpamAnalysisResult.notSpam());
        when(domainService.findByDomainName(anyString())).thenReturn(Optional.empty());

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai optimized query",
                20,
                testSessionId
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        SearchExecutionResult execResult = result.get();

        assertThat(execResult.isPartialSuccess()).isTrue();
        assertThat(execResult.isFullSuccess()).isFalse();
        assertThat(execResult.successfulResults()).hasSize(3);
        assertThat(execResult.providerErrors()).hasSize(1);

        ProviderError error = execResult.providerErrors().get(0);
        assertThat(error.provider()).isEqualTo(SearchEngineType.BRAVE);
        assertThat(error.errorMessage()).contains("Rate limit");
        assertThat(error.errorType()).isEqualTo(ProviderError.ErrorType.RATE_LIMIT);
    }

    @Test
    @DisplayName("Complete failure - all 4 providers fail")
    void executeMultiProviderSearch_AllProvidersFail_ReturnsFailure() {
        // Given: All providers fail
        RuntimeException braveError = new RuntimeException("Authentication failed");
        RuntimeException searxngError = new RuntimeException("Timeout");
        RuntimeException serperError = new RuntimeException("Network error");
        RuntimeException tavilyError = new RuntimeException("Invalid API key");

        when(braveSearchAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(braveError));
        when(searxngAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(searxngError));
        when(serperAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(serperError));
        when(tavilyAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(tavilyError));

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai optimized query",
                20,
                testSessionId
        );

        // Then: Should return failure since all providers failed
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause().getMessage()).contains("All search providers failed");
    }

    @Test
    @DisplayName("Anti-spam filtering applied before deduplication")
    void aggregateResults_AntiSpamFiltering_FiltersBeforeDeduplication() {
        // Given: 3 results, 1 is spam
        SearchResult legitResult1 = createSearchResult("legitimate1.org", 1, SearchEngineType.BRAVE);
        SearchResult spamResult = createSearchResult("casino.com", 2, SearchEngineType.BRAVE);
        SearchResult legitResult2 = createSearchResult("legitimate2.org", 3, SearchEngineType.BRAVE);

        Map<SearchEngineType, List<SearchResult>> providerResults = Map.of(
                SearchEngineType.BRAVE, List.of(legitResult1, spamResult, legitResult2)
        );

        // Anti-spam filter rejects casino.com
        when(antiSpamFilter.analyzeForSpam(any())).thenAnswer(invocation -> {
            SearchResult result = invocation.getArgument(0);
            if (result != null && "casino.com".equals(result.getDomain())) {
                return SpamAnalysisResult.spam(
                        SpamIndicator.CROSS_CATEGORY_SPAM,
                        "Gambling domain with education content",
                        0.7
                );
            }
            return SpamAnalysisResult.notSpam();
        });

        when(domainService.findByDomainName(anyString())).thenReturn(Optional.empty());

        // When
        List<SearchResult> aggregatedResults = orchestrator.aggregateResults(providerResults);

        // Then: Spam result should be filtered out
        assertThat(aggregatedResults).hasSize(2);
        assertThat(aggregatedResults).extracting(SearchResult::getDomain)
                .containsExactlyInAnyOrder("legitimate1.org", "legitimate2.org");
        assertThat(aggregatedResults).extracting(SearchResult::getDomain)
                .doesNotContain("casino.com");

        // Verify spam filter was called for all 3 results
        verify(antiSpamFilter, times(3)).analyzeForSpam(any());
    }

    @Test
    @DisplayName("Domain deduplication keeps highest rank (lowest position)")
    void aggregateResults_DomainDeduplication_KeepsHighestRank() {
        // Given: Same domain appears 3 times with different ranks
        SearchResult rank1 = createSearchResult("example.org", 1, SearchEngineType.BRAVE);
        SearchResult rank5 = createSearchResult("example.org", 5, SearchEngineType.SEARXNG);
        SearchResult rank3 = createSearchResult("example.org", 3, SearchEngineType.SERPER);

        Map<SearchEngineType, List<SearchResult>> providerResults = Map.of(
                SearchEngineType.BRAVE, List.of(rank1),
                SearchEngineType.SEARXNG, List.of(rank5),
                SearchEngineType.SERPER, List.of(rank3)
        );

        when(antiSpamFilter.analyzeForSpam(any())).thenReturn(SpamAnalysisResult.notSpam());
        when(domainService.findByDomainName("example.org")).thenReturn(Optional.empty());

        // When
        List<SearchResult> aggregatedResults = orchestrator.aggregateResults(providerResults);

        // Then: Should keep rank 1 result (lowest position = highest rank)
        assertThat(aggregatedResults).hasSize(1);
        assertThat(aggregatedResults.get(0).getRankPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("Blacklisted domains skipped")
    void aggregateResults_BlacklistedDomain_Skipped() {
        // Given: One domain is blacklisted
        SearchResult legitResult = createSearchResult("legitimate.org", 1, SearchEngineType.BRAVE);
        SearchResult blacklistedResult = createSearchResult("blacklisted.com", 2, SearchEngineType.BRAVE);

        Map<SearchEngineType, List<SearchResult>> providerResults = Map.of(
                SearchEngineType.BRAVE, List.of(legitResult, blacklistedResult)
        );

        when(antiSpamFilter.analyzeForSpam(any())).thenReturn(SpamAnalysisResult.notSpam());

        // Domain service returns blacklisted domain
        Domain blacklistedDomain = Domain.builder()
                .domainName("blacklisted.com")
                .status(DomainStatus.BLACKLISTED)
                .build();

        when(domainService.findByDomainName("blacklisted.com"))
                .thenReturn(Optional.of(blacklistedDomain));
        when(domainService.findByDomainName("legitimate.org"))
                .thenReturn(Optional.empty());

        // When
        List<SearchResult> aggregatedResults = orchestrator.aggregateResults(providerResults);

        // Then: Blacklisted domain should be skipped
        assertThat(aggregatedResults).hasSize(1);
        assertThat(aggregatedResults.get(0).getDomain()).isEqualTo("legitimate.org");
    }

    @Test
    @DisplayName("Empty results handled gracefully")
    void aggregateResults_EmptyProviderResults_ReturnsEmptyList() {
        // Given: All providers return empty results
        Map<SearchEngineType, List<SearchResult>> providerResults = Map.of(
                SearchEngineType.BRAVE, List.of(),
                SearchEngineType.SEARXNG, List.of()
        );

        // When
        List<SearchResult> aggregatedResults = orchestrator.aggregateResults(providerResults);

        // Then
        assertThat(aggregatedResults).isEmpty();
    }

    @Test
    @DisplayName("Session statistics calculated correctly")
    void executeMultiProviderSearch_StatisticsCalculation_Correct() {
        // Given: 2 providers with results, 1 spam filtered, 1 duplicate
        List<SearchResult> braveResults = List.of(
                createSearchResult("example.org", 1, SearchEngineType.BRAVE),
                createSearchResult("spam.com", 2, SearchEngineType.BRAVE)
        );
        List<SearchResult> serperResults = List.of(
                createSearchResult("example.org", 5, SearchEngineType.SERPER)
        );

        when(braveSearchAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(braveResults));
        when(searxngAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));
        when(serperAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(serperResults));
        when(tavilyAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));

        // Spam filter rejects spam.com
        when(antiSpamFilter.analyzeForSpam(any())).thenAnswer(invocation -> {
            SearchResult result = invocation.getArgument(0);
            if (result != null && "spam.com".equals(result.getDomain())) {
                return SpamAnalysisResult.spam(SpamIndicator.KEYWORD_STUFFING, "Spam", 0.8);
            }
            return SpamAnalysisResult.notSpam();
        });

        when(domainService.findByDomainName(anyString())).thenReturn(Optional.empty());

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai optimized query",
                20,
                testSessionId
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        SessionStatistics stats = result.get().statistics();

        // 2 brave + 1 serper = 3 raw results
        // 1 spam filtered + 1 duplicate = 2 filtered
        // 1 final result (example.org from brave)
        assertThat(stats.totalResultsFound()).isEqualTo(1);
        // NOTE: spamFiltered = totalRaw - aggregated (includes both spam AND duplicates)
        assertThat(stats.spamResultsFiltered()).isEqualTo(2);
        assertThat(stats.braveSearchResults()).isEqualTo(2);
        assertThat(stats.searxngResults()).isEqualTo(0);
        assertThat(stats.serperResults()).isEqualTo(1);
        assertThat(stats.tavilyResults()).isEqualTo(0);
    }

    @Test
    @DisplayName("Error classification - rate limit")
    void executeMultiProviderSearch_RateLimitError_ClassifiedCorrectly() {
        // Given: Provider fails with rate limit error
        when(braveSearchAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(new RuntimeException("Rate limit exceeded")));

        when(searxngAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of(createSearchResult("example.org", 1, SearchEngineType.SEARXNG))));
        when(serperAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));
        when(tavilyAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));

        when(antiSpamFilter.analyzeForSpam(any())).thenReturn(SpamAnalysisResult.notSpam());
        when(domainService.findByDomainName(anyString())).thenReturn(Optional.empty());

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai optimized query",
                20,
                testSessionId
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        List<ProviderError> errors = result.get().providerErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorType()).isEqualTo(ProviderError.ErrorType.RATE_LIMIT);
    }

    @Test
    @DisplayName("Error classification - timeout")
    void executeMultiProviderSearch_TimeoutError_ClassifiedCorrectly() {
        // Given: Provider fails with timeout
        when(braveSearchAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(new RuntimeException("Request timeout after 5 seconds")));

        when(searxngAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of(createSearchResult("example.org", 1, SearchEngineType.SEARXNG))));
        when(serperAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));
        when(tavilyAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));

        when(antiSpamFilter.analyzeForSpam(any())).thenReturn(SpamAnalysisResult.notSpam());
        when(domainService.findByDomainName(anyString())).thenReturn(Optional.empty());

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai optimized query",
                20,
                testSessionId
        );

        // Then
        List<ProviderError> errors = result.get().providerErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorType()).isEqualTo(ProviderError.ErrorType.TIMEOUT);
    }

    @Test
    @DisplayName("Error classification - authentication")
    void executeMultiProviderSearch_AuthError_ClassifiedCorrectly() {
        // Given: Provider fails with auth error
        when(braveSearchAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.failure(new RuntimeException("401 Unauthorized")));

        when(searxngAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of(createSearchResult("example.org", 1, SearchEngineType.SEARXNG))));
        when(serperAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));
        when(tavilyAdapter.executeSearch(anyString(), anyInt(), any()))
                .thenReturn(Try.success(List.of()));

        when(antiSpamFilter.analyzeForSpam(any())).thenReturn(SpamAnalysisResult.notSpam());
        when(domainService.findByDomainName(anyString())).thenReturn(Optional.empty());

        // When
        Try<SearchExecutionResult> result = orchestrator.executeMultiProviderSearch(
                "keyword query",
                "ai optimized query",
                20,
                testSessionId
        );

        // Then
        List<ProviderError> errors = result.get().providerErrors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorType()).isEqualTo(ProviderError.ErrorType.AUTH_FAILURE);
    }

    @Test
    @DisplayName("Update session statistics - full success")
    void updateSessionStatistics_FullSuccess_SetsCompletedStatus() {
        // Given: Full success result
        List<SearchResult> results = List.of(createSearchResult("example.org", 1, SearchEngineType.BRAVE));
        SessionStatistics stats = new SessionStatistics(
                1,  // totalResultsFound
                1,  // newDomainsDiscovered
                0,  // duplicateDomainsSkipped
                0,  // spamResultsFiltered
                1, 0, 0, 0  // per-provider counts
        );
        SearchExecutionResult executionResult = new SearchExecutionResult(results, List.of(), stats);

        DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.SCHEDULED)
                .status(SessionStatus.RUNNING)
                .build();

        when(discoverySessionService.findById(testSessionId)).thenReturn(Optional.of(session));
        when(discoverySessionService.updateStatus(eq(testSessionId), eq(SessionStatus.COMPLETED)))
                .thenReturn(session);

        // When
        DiscoverySession updatedSession = orchestrator.updateSessionStatistics(testSessionId, executionResult);

        // Then
        verify(discoverySessionService).updateStatus(testSessionId, SessionStatus.COMPLETED);
        assertThat(session.getCandidatesFound()).isEqualTo(1);
        assertThat(session.getDuplicatesDetected()).isEqualTo(0);
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Update session statistics - partial success")
    void updateSessionStatistics_PartialSuccess_SetsCompletedStatus() {
        // Given: Partial success result (some providers failed)
        List<SearchResult> results = List.of(createSearchResult("example.org", 1, SearchEngineType.BRAVE));
        List<ProviderError> errors = List.of(
                new ProviderError(SearchEngineType.SERPER, "Rate limit", ProviderError.ErrorType.RATE_LIMIT, LocalDateTime.now(), "query")
        );
        SessionStatistics stats = new SessionStatistics(1, 1, 0, 0, 1, 0, 0, 0);
        SearchExecutionResult executionResult = new SearchExecutionResult(results, errors, stats);

        DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.SCHEDULED)
                .status(SessionStatus.RUNNING)
                .build();

        when(discoverySessionService.findById(testSessionId)).thenReturn(Optional.of(session));
        when(discoverySessionService.updateStatus(eq(testSessionId), eq(SessionStatus.COMPLETED)))
                .thenReturn(session);

        // When
        orchestrator.updateSessionStatistics(testSessionId, executionResult);

        // Then: Partial success still sets COMPLETED (no PARTIAL_SUCCESS status in enum)
        verify(discoverySessionService).updateStatus(testSessionId, SessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Update session statistics - complete failure")
    void updateSessionStatistics_CompleteFailure_SetsFailedStatus() {
        // Given: Complete failure result (all providers failed)
        List<ProviderError> errors = List.of(
                new ProviderError(SearchEngineType.BRAVE, "Error 1", ProviderError.ErrorType.TIMEOUT, LocalDateTime.now(), "query"),
                new ProviderError(SearchEngineType.SEARXNG, "Error 2", ProviderError.ErrorType.TIMEOUT, LocalDateTime.now(), "query"),
                new ProviderError(SearchEngineType.SERPER, "Error 3", ProviderError.ErrorType.TIMEOUT, LocalDateTime.now(), "query"),
                new ProviderError(SearchEngineType.TAVILY, "Error 4", ProviderError.ErrorType.TIMEOUT, LocalDateTime.now(), "query")
        );
        SessionStatistics stats = new SessionStatistics(0, 0, 0, 0, 0, 0, 0, 0);
        SearchExecutionResult executionResult = new SearchExecutionResult(List.of(), errors, stats);

        DiscoverySession session = DiscoverySession.builder()
                .sessionType(SessionType.SCHEDULED)
                .status(SessionStatus.RUNNING)
                .build();

        when(discoverySessionService.findById(testSessionId)).thenReturn(Optional.of(session));
        when(discoverySessionService.updateStatus(eq(testSessionId), eq(SessionStatus.FAILED)))
                .thenReturn(session);

        // When
        orchestrator.updateSessionStatistics(testSessionId, executionResult);

        // Then
        verify(discoverySessionService).updateStatus(testSessionId, SessionStatus.FAILED);
    }

    @Test
    @DisplayName("Execute single provider - delegates to adapter")
    void executeSingleProvider_DelegatesToAdapter() {
        // Given
        List<SearchResult> expectedResults = List.of(createSearchResult("example.org", 1, SearchEngineType.BRAVE));
        when(braveSearchAdapter.executeSearch(eq("test query"), eq(10), eq(testSessionId)))
                .thenReturn(Try.success(expectedResults));

        // When
        Try<List<SearchResult>> result = orchestrator.executeSingleProvider(
                braveSearchAdapter,
                "test query",
                10,
                testSessionId
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(expectedResults);
        verify(braveSearchAdapter).executeSearch("test query", 10, testSessionId);
    }

    /**
     * Helper method to create SearchResult for testing.
     */
    private SearchResult createSearchResult(String domain, int position, SearchEngineType engine) {
        return SearchResult.builder()
                .url("https://" + domain)
                .domain(domain)
                .title("Title for " + domain)
                .description("Description for " + domain)
                .rankPosition(position)
                .searchEngine(engine)
                .discoveredAt(LocalDateTime.now())
                .searchDate(LocalDate.now())
                .discoverySessionId(testSessionId)
                .build();
    }
}
