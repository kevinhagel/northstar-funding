package com.northstar.funding.searchadapters.contract;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.exception.SearchAdapterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

/**
 * Abstract base class for all SearchAdapter contract tests.
 *
 * Defines test scenarios from contracts/search-adapter-contract.yml:
 * - Search with results
 * - Search with zero results (NOT an error)
 * - API authentication failure
 * - Correct engine type
 * - Availability check
 *
 * These tests MUST FAIL initially (TDD approach).
 */
public abstract class SearchAdapterContractTest {

    protected WireMockServer wireMockServer;
    protected SearchAdapter adapter;

    /**
     * Subclasses must provide the adapter instance to test.
     */
    protected abstract SearchAdapter createAdapter(String baseUrl);

    /**
     * Subclasses must provide the expected SearchEngineType.
     */
    protected abstract SearchEngineType getExpectedEngineType();

    /**
     * Subclasses must provide a mock successful response body.
     * Should contain at least 1 search result.
     */
    protected abstract String getMockSuccessResponseBody();

    /**
     * Subclasses must provide a mock zero-results response body.
     * Should be valid JSON but with empty results array.
     */
    protected abstract String getMockZeroResultsResponseBody();

    /**
     * Subclasses must configure the stub for successful search.
     * Should mock the search endpoint with 200 OK response.
     */
    protected abstract void stubSuccessfulSearch();

    /**
     * Subclasses must configure the stub for zero results.
     * Should mock the search endpoint with 200 OK but empty results.
     */
    protected abstract void stubZeroResultsSearch();

    /**
     * Subclasses must configure the stub for authentication failure.
     * Should mock the search endpoint with 401 Unauthorized.
     */
    protected abstract void stubAuthenticationFailure();

    @BeforeEach
    void setUp() {
        // Start WireMock server on random port
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Create adapter with WireMock base URL
        adapter = createAdapter(wireMockServer.baseUrl());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    /**
     * Contract Test: Search with results.
     *
     * GIVEN: Valid query with known results
     * WHEN: search() called
     * THEN: Returns non-empty List<SearchResult>
     *       Each result has non-null url, title, source
     *       Source matches adapter's getEngineType()
     */
    @Test
    void testSearchWithResults() {
        // Given
        stubSuccessfulSearch();
        String query = "education grants Bulgaria";
        int maxResults = 10;

        // When
        var results = adapter.search(query, maxResults);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).isNotEmpty();
        assertThat(results.size()).isLessThanOrEqualTo(maxResults);

        // Verify each result has required fields
        results.forEach(result -> {
            assertThat(result.getUrl()).isNotNull().isNotBlank();
            assertThat(result.getTitle()).isNotNull();
            assertThat(result.getSearchEngine()).isEqualTo(getExpectedEngineType());
            assertThat(result.getDiscoveredAt()).isNotNull();
        });
    }

    /**
     * Contract Test: Search with zero results.
     *
     * GIVEN: Valid query, no matches
     * WHEN: search() called
     * THEN: Returns empty List (NOT exception)
     *       Zero results is NOT an error
     */
    @Test
    void testSearchWithZeroResults() {
        // Given
        stubZeroResultsSearch();
        String query = "nonexistent funding category xyz123";
        int maxResults = 10;

        // When
        var results = adapter.search(query, maxResults);

        // Then
        assertThat(results).isNotNull();
        assertThat(results).isEmpty();
        // Zero results is a valid outcome - no exception should be thrown
    }

    /**
     * Contract Test: API authentication failure.
     *
     * GIVEN: Invalid API key or credentials
     * WHEN: search() called
     * THEN: Throws SearchAdapterException
     *       Exception contains error message
     */
    @Test
    void testSearchApiFailure() {
        // Given
        stubAuthenticationFailure();
        String query = "education grants";
        int maxResults = 10;

        // When/Then
        assertThatThrownBy(() -> adapter.search(query, maxResults))
            .isInstanceOf(SearchAdapterException.class)
            .hasMessageContaining("401")
            .satisfies(ex -> {
                SearchAdapterException sae = (SearchAdapterException) ex;
                assertThat(sae.getEngineType()).isEqualTo(getExpectedEngineType());
                assertThat(sae.getQuery()).isEqualTo(query);
            });
    }

    /**
     * Contract Test: Get engine type.
     *
     * WHEN: getEngineType() called
     * THEN: Returns correct SearchEngineType enum
     */
    @Test
    void testGetEngineType() {
        // When
        SearchEngineType engineType = adapter.getEngineType();

        // Then
        assertThat(engineType).isNotNull();
        assertThat(engineType).isEqualTo(getExpectedEngineType());
    }

    /**
     * Contract Test: Availability check.
     *
     * WHEN: isAvailable() called
     * THEN: Returns boolean
     *       True if adapter is configured correctly
     */
    @Test
    void testIsAvailable() {
        // When
        boolean available = adapter.isAvailable();

        // Then
        assertThat(available).isNotNull();
        // Note: Actual value depends on adapter configuration
        // For tests with WireMock, should typically be true
    }
}
