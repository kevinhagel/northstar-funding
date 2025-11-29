package com.northstar.funding.crawler.contract;

import com.northstar.funding.crawler.adapter.AbstractSearchProviderAdapter;
import com.northstar.funding.crawler.adapter.SearchProviderAdapter;
import com.northstar.funding.crawler.exception.AuthenticationException;
import com.northstar.funding.crawler.exception.RateLimitException;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests for SearchProviderAdapter interface.
 *
 * These tests verify that ALL adapter implementations (BraveSearch, Serper, Perplexica, SearXNG)
 * conform to the SearchProviderAdapter contract.
 *
 * TDD Approach: These tests are written BEFORE the concrete implementations.
 * They MUST FAIL initially, then pass once implementations are complete.
 */
@DisplayName("SearchProviderAdapter Contract Tests")
class SearchProviderAdapterContractTest {

    private TestSearchProviderAdapter adapter;
    private UUID discoverySessionId;

    @BeforeEach
    void setUp() {
        discoverySessionId = UUID.randomUUID();
        adapter = new TestSearchProviderAdapter(5, 20, 50);
    }

    @Test
    @DisplayName("executeSearch() must return Try<List<SearchResult>>, never null")
    void executeSearch_MustReturnTry_NotNull() {
        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 10, discoverySessionId);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("executeSearch() with empty results must return Try.success(List.of()), NOT null")
    void executeSearch_EmptyResults_MustReturnSuccessWithEmptyList() {
        // Given
        adapter.simulateEmptyResults = true;

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("no results query", 10, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isNotNull();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("executeSearch() must respect maxResults limit")
    void executeSearch_MustRespectMaxResultsLimit() {
        // Given
        int maxResults = 5;

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", maxResults, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).hasSizeLessThanOrEqualTo(maxResults);
    }

    @Test
    @DisplayName("executeSearch() must populate all required SearchResult fields")
    void executeSearch_MustPopulateRequiredFields() {
        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 10, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isNotEmpty();

        SearchResult firstResult = result.get().get(0);
        assertThat(firstResult.getUrl()).isNotBlank();
        assertThat(firstResult.getDomain()).isNotBlank();
        assertThat(firstResult.getTitle()).isNotBlank();
        assertThat(firstResult.getDescription()).isNotNull(); // Can be empty string
        assertThat(firstResult.getRankPosition()).isPositive();
        assertThat(firstResult.getSearchEngine()).isNotNull();
        assertThat(firstResult.getDiscoveredAt()).isNotNull();
        assertThat(firstResult.getDiscoverySessionId()).isEqualTo(discoverySessionId);
        assertThat(firstResult.getSearchDate()).isNotNull();
    }

    @Test
    @DisplayName("executeSearch() must normalize domain (lowercase, no www, no protocol)")
    void executeSearch_MustNormalizeDomain() {
        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 10, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isNotEmpty();

        for (SearchResult searchResult : result.get()) {
            String domain = searchResult.getDomain();

            // Domain must be lowercase
            assertThat(domain).isEqualTo(domain.toLowerCase());

            // Domain must not start with www.
            assertThat(domain).doesNotStartWith("www.");

            // Domain must not contain protocol
            assertThat(domain).doesNotContain("http://");
            assertThat(domain).doesNotContain("https://");
        }
    }

    @Test
    @DisplayName("executeSearch() must set correct SearchEngineType")
    void executeSearch_MustSetCorrectSearchEngineType() {
        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 10, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isNotEmpty();

        for (SearchResult searchResult : result.get()) {
            assertThat(searchResult.getSearchEngine()).isEqualTo(adapter.getProviderType());
        }
    }

    @Test
    @DisplayName("executeSearch() must throw RateLimitException when quota exceeded")
    void executeSearch_MustThrowRateLimitException_WhenQuotaExceeded() {
        // Given - exhaust rate limit
        for (int i = 0; i < 50; i++) {
            adapter.executeSearch("query " + i, 10, discoverySessionId);
        }

        // When - one more request should exceed quota
        Try<List<SearchResult>> result = adapter.executeSearch("exceed quota", 10, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(RateLimitException.class);

        RateLimitException exception = (RateLimitException) result.getCause();
        assertThat(exception.getProviderName()).isNotBlank();
        assertThat(exception.getDailyLimit()).isEqualTo(50);
    }

    @Test
    @DisplayName("executeSearch() must throw AuthenticationException for invalid API key")
    void executeSearch_MustThrowAuthenticationException_ForInvalidApiKey() {
        // Given
        adapter.simulateAuthenticationError = true;

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 10, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(AuthenticationException.class);

        AuthenticationException exception = (AuthenticationException) result.getCause();
        assertThat(exception.getProviderName()).isNotBlank();
    }

    @Test
    @DisplayName("getProviderType() must return non-null SearchEngineType")
    void getProviderType_MustReturnNonNull() {
        // When
        SearchEngineType providerType = adapter.getProviderType();

        // Then
        assertThat(providerType).isNotNull();
    }

    @Test
    @DisplayName("getProviderType() must return same value on every invocation (stateless)")
    void getProviderType_MustBeStateless() {
        // When
        SearchEngineType first = adapter.getProviderType();
        SearchEngineType second = adapter.getProviderType();

        // Then
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("getCurrentUsageCount() must track API calls accurately")
    void getCurrentUsageCount_MustTrackApiCalls() {
        // Given
        int initialCount = adapter.getCurrentUsageCount();

        // When
        adapter.executeSearch("query 1", 10, discoverySessionId);
        adapter.executeSearch("query 2", 10, discoverySessionId);

        // Then
        assertThat(adapter.getCurrentUsageCount()).isEqualTo(initialCount + 2);
    }

    @Test
    @DisplayName("getRateLimit() must return configured daily limit")
    void getRateLimit_MustReturnConfiguredLimit() {
        // When
        int rateLimit = adapter.getRateLimit();

        // Then
        assertThat(rateLimit).isEqualTo(50);
    }

    /**
     * Test double implementation of SearchProviderAdapter for contract testing.
     *
     * This is a minimal concrete implementation used ONLY for testing the contract.
     * Real implementations (BraveSearchAdapter, etc.) will be created after these tests pass.
     */
    private static class TestSearchProviderAdapter extends AbstractSearchProviderAdapter {

        boolean simulateEmptyResults = false;
        boolean simulateAuthenticationError = false;

        public TestSearchProviderAdapter(int timeoutSeconds, int maxResults, int dailyRateLimit) {
            super(timeoutSeconds, maxResults, dailyRateLimit);
        }

        @Override
        public Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID discoverySessionId) {
            return Try.of(() -> {
                // Check rate limit
                incrementUsageAndCheckLimit();

                // Simulate authentication error
                if (simulateAuthenticationError) {
                    throw new AuthenticationException(getProviderType().name(), "Invalid API key");
                }

                // Simulate empty results
                if (simulateEmptyResults) {
                    return List.of();
                }

                // Generate test results
                return List.of(
                        createBaseSearchResult(
                                "https://www.example.com/funding",
                                "Example Funding Program",
                                "A test funding program description",
                                1,
                                discoverySessionId
                        ).build(),
                        createBaseSearchResult(
                                "http://TESTDOMAIN.ORG/grants",
                                "Test Grant Program",
                                "Another test description",
                                2,
                                discoverySessionId
                        ).build()
                );
            });
        }

        @Override
        public SearchEngineType getProviderType() {
            return SearchEngineType.BRAVE;
        }

        @Override
        public boolean supportsKeywordQueries() {
            return true;
        }

        @Override
        public boolean supportsAIOptimizedQueries() {
            return false;
        }
    }
}
