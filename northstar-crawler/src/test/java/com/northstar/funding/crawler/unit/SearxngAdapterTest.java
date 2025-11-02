package com.northstar.funding.crawler.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.northstar.funding.crawler.adapter.SearxngAdapter;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import com.northstar.funding.crawler.exception.ProviderTimeoutException;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SearxngAdapter using WireMock for HTTP mocking.
 *
 * Tests:
 * - Successful search returns SearchResult entities
 * - JSON format parameter is sent
 * - No authentication required (self-hosted)
 * - No rate limiting (unlimited quota)
 * - Timeout throws ProviderTimeoutException
 * - Domain normalization (www removal, lowercase)
 * - Empty response returns empty list
 * - Multiple results parsed correctly
 */
@DisplayName("SearxngAdapter Unit Tests")
class SearxngAdapterTest {

    private static WireMockServer wireMockServer;
    private SearxngAdapter adapter;
    private SearchProviderConfig config;
    private ObjectMapper objectMapper;
    private UUID discoverySessionId;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        discoverySessionId = UUID.randomUUID();
        objectMapper = new ObjectMapper();

        // Configure test config pointing to WireMock
        config = new SearchProviderConfig();
        SearchProviderConfig.SearxngConfig searxngConfig = new SearchProviderConfig.SearxngConfig();
        searxngConfig.setBaseUrl("http://localhost:8090/search");
        searxngConfig.setTimeout(7000);
        searxngConfig.setMaxResults(25);
        // No rate limit configuration needed - SearXNG has unlimited quota
        config.setSearxng(searxngConfig);

        adapter = new SearxngAdapter(config, objectMapper);
    }

    @Test
    @DisplayName("Successful search returns SearchResult entities")
    void executeSearch_Success_ReturnsSearchResults() {
        // Given: Mock successful SearXNG response
        String mockResponse = """
                {
                    "results": [
                        {
                            "title": "European Research Council",
                            "url": "https://erc.europa.eu/funding",
                            "content": "ERC funding opportunities for researchers"
                        },
                        {
                            "title": "Horizon Europe Grants",
                            "url": "https://www.research.eu/grants",
                            "content": "EU funding programs for innovation"
                        }
                    ]
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("european research grants", 25, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        List<SearchResult> results = result.get();
        assertThat(results).hasSize(2);

        // Verify first result
        SearchResult first = results.get(0);
        assertThat(first.getTitle()).isEqualTo("European Research Council");
        assertThat(first.getUrl()).isEqualTo("https://erc.europa.eu/funding");
        assertThat(first.getDomain()).isEqualTo("erc.europa.eu");
        assertThat(first.getDescription()).isEqualTo("ERC funding opportunities for researchers");
        assertThat(first.getRankPosition()).isEqualTo(1);
        assertThat(first.getSearchEngine()).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(first.getDiscoverySessionId()).isEqualTo(discoverySessionId);

        // Verify second result
        SearchResult second = results.get(1);
        assertThat(second.getDomain()).isEqualTo("research.eu"); // www removed
        assertThat(second.getRankPosition()).isEqualTo(2);

        // Verify HTTP request includes format=json parameter (no number_of_results - limited in code)
        verify(getRequestedFor(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("european research grants"))
                .withQueryParam("format", equalTo("json")));
    }

    @Test
    @DisplayName("JSON format parameter is sent")
    void executeSearch_IncludesFormatParameter() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "results": []
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        adapter.executeSearch("test query", 25, discoverySessionId);

        // Then: Verify format=json parameter
        verify(getRequestedFor(urlPathEqualTo("/search"))
                .withQueryParam("format", equalTo("json")));
    }

    @Test
    @DisplayName("No authentication required - self-hosted")
    void executeSearch_NoAuthenticationRequired() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "results": [
                        {
                            "title": "Test",
                            "url": "https://test.org",
                            "content": "Test content"
                        }
                    ]
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 25, discoverySessionId);

        // Then: Should succeed without API key
        assertThat(result.isSuccess()).isTrue();

        // Verify NO authentication headers sent
        verify(getRequestedFor(urlPathEqualTo("/search"))
                .withoutHeader("Authorization")
                .withoutHeader("X-Api-Key")
                .withoutHeader("X-Subscription-Token"));
    }

    @Test
    @DisplayName("Timeout throws ProviderTimeoutException")
    void executeSearch_Timeout_ThrowsProviderTimeoutException() {
        // Given: Mock slow response (longer than 7 second timeout)
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(8000))); // 8 second delay

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 25, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(ProviderTimeoutException.class);
    }

    @Test
    @DisplayName("Domain normalization - lowercase conversion")
    void executeSearch_DomainNormalization_Lowercase() {
        // Given: Response with uppercase domain
        String mockResponse = """
                {
                    "results": [
                        {
                            "title": "Test",
                            "url": "https://EXAMPLE.ORG/page",
                            "content": "Test description"
                        }
                    ]
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 25, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().get(0).getDomain()).isEqualTo("example.org"); // Lowercase
    }

    @Test
    @DisplayName("Domain normalization - www removal")
    void executeSearch_DomainNormalization_WwwRemoval() {
        // Given: Response with www prefix
        String mockResponse = """
                {
                    "results": [
                        {
                            "title": "Test",
                            "url": "https://www.example.org/page",
                            "content": "Test description"
                        }
                    ]
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 25, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().get(0).getDomain()).isEqualTo("example.org"); // www removed
    }

    @Test
    @DisplayName("Empty response returns empty list")
    void executeSearch_EmptyResults_ReturnsEmptyList() {
        // Given: Response with no results
        String mockResponse = """
                {
                    "results": []
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("obscure query", 25, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("No rate limiting - unlimited quota")
    void executeSearch_NoRateLimit_UnlimitedQuota() {
        // Given: Mock successful responses
        String mockResponse = """
                {
                    "results": [
                        {
                            "title": "Test",
                            "url": "https://example.org/page",
                            "content": "Test"
                        }
                    ]
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When: Execute 100 requests (should all succeed - no rate limit)
        for (int i = 0; i < 100; i++) {
            Try<List<SearchResult>> result = adapter.executeSearch("query" + i, 25, discoverySessionId);
            assertThat(result.isSuccess())
                    .withFailMessage("Request " + (i + 1) + " should succeed - SearXNG has unlimited quota")
                    .isTrue();
        }

        // Then: All requests succeeded (no rate limiting)
        verify(100, getRequestedFor(urlPathEqualTo("/search")));
    }

    @Test
    @DisplayName("Multiple results parsed correctly with correct rank positions")
    void executeSearch_MultipleResults_CorrectRankPositions() {
        // Given: Response with 5 results
        String mockResponse = """
                {
                    "results": [
                        {
                            "title": "Result 1",
                            "url": "https://example1.org",
                            "content": "Content 1"
                        },
                        {
                            "title": "Result 2",
                            "url": "https://example2.org",
                            "content": "Content 2"
                        },
                        {
                            "title": "Result 3",
                            "url": "https://example3.org",
                            "content": "Content 3"
                        },
                        {
                            "title": "Result 4",
                            "url": "https://example4.org",
                            "content": "Content 4"
                        },
                        {
                            "title": "Result 5",
                            "url": "https://example5.org",
                            "content": "Content 5"
                        }
                    ]
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 25, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        List<SearchResult> results = result.get();
        assertThat(results).hasSize(5);

        // Verify rank positions (1-based indexing)
        for (int i = 0; i < 5; i++) {
            assertThat(results.get(i).getRankPosition()).isEqualTo(i + 1);
            assertThat(results.get(i).getTitle()).isEqualTo("Result " + (i + 1));
            assertThat(results.get(i).getDomain()).isEqualTo("example" + (i + 1) + ".org");
        }
    }

    @Test
    @DisplayName("HTTP 500 internal server error is handled gracefully")
    void executeSearch_InternalServerError_HandledGracefully() {
        // Given: Mock 500 response
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(500)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 25, discoverySessionId);

        // Then: Should fail but not crash
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause().getMessage()).containsIgnoringCase("500");
    }

    @Test
    @DisplayName("Adapter reports correct provider type")
    void getProviderType_ReturnsSEARXNG() {
        assertThat(adapter.getProviderType()).isEqualTo(SearchEngineType.SEARXNG);
    }

    @Test
    @DisplayName("Adapter supports keyword queries only")
    void supportsKeywordQueries_ReturnsTrue() {
        assertThat(adapter.supportsKeywordQueries()).isTrue();
        assertThat(adapter.supportsAIOptimizedQueries()).isFalse();
    }

    @Test
    @DisplayName("Rate limit tracking shows unlimited quota")
    void getCurrentUsageCount_ShowsUnlimitedQuota() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "results": []
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When: Execute 10 searches
        for (int i = 0; i < 10; i++) {
            adapter.executeSearch("query" + i, 25, discoverySessionId);
        }

        // Then: Usage count increases but rate limit is Integer.MAX_VALUE (unlimited)
        assertThat(adapter.getCurrentUsageCount()).isEqualTo(10);
        assertThat(adapter.getRateLimit()).isEqualTo(Integer.MAX_VALUE); // Unlimited
    }
}
