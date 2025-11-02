package com.northstar.funding.crawler.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.northstar.funding.crawler.adapter.SerperAdapter;
import com.northstar.funding.crawler.config.SearchProviderConfig;
import com.northstar.funding.crawler.exception.AuthenticationException;
import com.northstar.funding.crawler.exception.ProviderTimeoutException;
import com.northstar.funding.crawler.exception.RateLimitException;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import io.vavr.control.Try;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SerperAdapter using WireMock for HTTP mocking.
 *
 * Tests:
 * - Successful search returns SearchResult entities
 * - POST with JSON body (not GET)
 * - X-API-KEY header authentication
 * - HTTP 401/403 throws AuthenticationException
 * - HTTP 429 throws RateLimitException
 * - Timeout throws ProviderTimeoutException
 * - Empty API key throws AuthenticationException
 * - Domain normalization (www removal, lowercase)
 * - Rate limit enforcement (61st request fails)
 * - Position field parsing from Serper response
 */
@DisplayName("SerperAdapter Unit Tests")
class SerperAdapterTest {

    private static WireMockServer wireMockServer;
    private SerperAdapter adapter;
    private SearchProviderConfig config;
    private ObjectMapper objectMapper;
    private UUID discoverySessionId;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8091);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8091);
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
        SearchProviderConfig.SerperConfig serperConfig = new SearchProviderConfig.SerperConfig();
        serperConfig.setApiKey("test-serper-key");
        serperConfig.setBaseUrl("http://localhost:8091/search");
        serperConfig.setTimeout(5000);
        serperConfig.setMaxResults(20);
        serperConfig.setRateLimit(new SearchProviderConfig.RateLimit(60));
        config.setSerper(serperConfig);

        adapter = new SerperAdapter(config, objectMapper);
    }

    @Test
    @DisplayName("Successful search returns SearchResult entities")
    void executeSearch_Success_ReturnsSearchResults() {
        // Given: Mock successful Serper response
        String mockResponse = """
                {
                    "organic": [
                        {
                            "title": "EU Grants Database",
                            "link": "https://ec.europa.eu/grants",
                            "snippet": "Official EU funding opportunities database",
                            "position": 1
                        },
                        {
                            "title": "Erasmus+ Funding",
                            "link": "https://www.erasmusplus.org/funding",
                            "snippet": "Education and training funding programs",
                            "position": 2
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("eu funding grants", 20, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        List<SearchResult> results = result.get();
        assertThat(results).hasSize(2);

        // Verify first result
        SearchResult first = results.get(0);
        assertThat(first.getTitle()).isEqualTo("EU Grants Database");
        assertThat(first.getUrl()).isEqualTo("https://ec.europa.eu/grants");
        assertThat(first.getDomain()).isEqualTo("ec.europa.eu");
        assertThat(first.getDescription()).isEqualTo("Official EU funding opportunities database");
        assertThat(first.getRankPosition()).isEqualTo(1);
        assertThat(first.getSearchEngine()).isEqualTo(SearchEngineType.SERPER);
        assertThat(first.getDiscoverySessionId()).isEqualTo(discoverySessionId);

        // Verify second result - domain normalization
        SearchResult second = results.get(1);
        assertThat(second.getDomain()).isEqualTo("erasmusplus.org"); // www removed
        assertThat(second.getRankPosition()).isEqualTo(2);

        // Verify HTTP request was POST with JSON body
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-API-KEY", equalTo("test-serper-key"))
                .withRequestBody(matchingJsonPath("$.q", equalTo("eu funding grants")))
                .withRequestBody(matchingJsonPath("$.num", equalTo("20"))));
    }

    @Test
    @DisplayName("POST with JSON body (not GET)")
    void executeSearch_UsesPostWithJsonBody() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "organic": []
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        adapter.executeSearch("test query", 20, discoverySessionId);

        // Then: Verify POST method used
        verify(postRequestedFor(urlPathEqualTo("/search")));
        verify(0, getRequestedFor(urlPathEqualTo("/search"))); // NO GET requests

        // Verify JSON body structure
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withRequestBody(matchingJsonPath("$.q"))
                .withRequestBody(matchingJsonPath("$.num")));
    }

    @Test
    @DisplayName("X-API-KEY header authentication")
    void executeSearch_SendsApiKeyHeader() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "organic": [
                        {
                            "title": "Test",
                            "link": "https://test.org",
                            "snippet": "Test snippet"
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 20, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();

        // Verify X-API-KEY header sent
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withHeader("X-API-KEY", equalTo("test-serper-key")));
    }

    @Test
    @DisplayName("HTTP 401 throws AuthenticationException")
    void executeSearch_Unauthorized_ThrowsAuthenticationException() {
        // Given: Mock 401 response
        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(401)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 20, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(AuthenticationException.class);
        assertThat(result.getCause().getMessage()).contains("Serper");
    }

    @Test
    @DisplayName("HTTP 403 throws AuthenticationException")
    void executeSearch_Forbidden_ThrowsAuthenticationException() {
        // Given: Mock 403 response
        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(403)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 20, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("HTTP 429 throws RateLimitException")
    void executeSearch_RateLimited_ThrowsRateLimitException() {
        // Given: Mock 429 response
        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(429)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 20, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(RateLimitException.class);
        assertThat(result.getCause().getMessage()).contains("Serper");
    }

    @Test
    @DisplayName("Timeout throws ProviderTimeoutException")
    void executeSearch_Timeout_ThrowsProviderTimeoutException() {
        // Given: Mock slow response (longer than 5 second timeout)
        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000))); // 6 second delay

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 20, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(ProviderTimeoutException.class);
    }

    @Test
    @DisplayName("Empty API key throws AuthenticationException")
    void executeSearch_NoApiKey_ThrowsAuthenticationException() {
        // Given: Adapter with empty API key
        SearchProviderConfig.SerperConfig serperConfig = new SearchProviderConfig.SerperConfig();
        serperConfig.setApiKey(""); // Empty API key
        serperConfig.setBaseUrl("http://localhost:8091/search");
        serperConfig.setTimeout(5000);
        serperConfig.setMaxResults(20);
        serperConfig.setRateLimit(new SearchProviderConfig.RateLimit(60));
        config.setSerper(serperConfig);

        SerperAdapter adapterWithoutKey = new SerperAdapter(config, objectMapper);

        // When
        Try<List<SearchResult>> result = adapterWithoutKey.executeSearch("test", 20, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(AuthenticationException.class);
        assertThat(result.getCause().getMessage()).contains("API key not configured");
    }

    @Test
    @DisplayName("Domain normalization - lowercase conversion")
    void executeSearch_DomainNormalization_Lowercase() {
        // Given: Response with uppercase domain
        String mockResponse = """
                {
                    "organic": [
                        {
                            "title": "Test",
                            "link": "https://EXAMPLE.ORG/page",
                            "snippet": "Test description"
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 20, discoverySessionId);

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
                    "organic": [
                        {
                            "title": "Test",
                            "link": "https://www.example.org/page",
                            "snippet": "Test description"
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 20, discoverySessionId);

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
                    "organic": []
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("obscure query", 20, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("Rate limit enforcement - 61st request fails")
    void executeSearch_RateLimitEnforcement_61stRequestFails() {
        // Given: Mock successful responses
        String mockResponse = """
                {
                    "organic": [
                        {
                            "title": "Test",
                            "link": "https://example.org/page",
                            "snippet": "Test"
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When: Execute 60 requests (should all succeed)
        for (int i = 0; i < 60; i++) {
            Try<List<SearchResult>> result = adapter.executeSearch("query" + i, 20, discoverySessionId);
            assertThat(result.isSuccess())
                    .withFailMessage("Request " + (i + 1) + " should succeed")
                    .isTrue();
        }

        // Then: 61st request should fail with RateLimitException
        Try<List<SearchResult>> result61 = adapter.executeSearch("query61", 20, discoverySessionId);
        assertThat(result61.isFailure()).isTrue();
        assertThat(result61.getCause()).isInstanceOf(RateLimitException.class);
        assertThat(result61.getCause().getMessage()).contains("60");
    }

    @Test
    @DisplayName("Position field parsing from Serper response")
    void executeSearch_ParsesPositionField() {
        // Given: Response with explicit position fields
        String mockResponse = """
                {
                    "organic": [
                        {
                            "title": "Result 1",
                            "link": "https://example1.org",
                            "snippet": "Snippet 1",
                            "position": 5
                        },
                        {
                            "title": "Result 2",
                            "link": "https://example2.org",
                            "snippet": "Snippet 2",
                            "position": 10
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 20, discoverySessionId);

        // Then: Position fields should be used (not array index)
        assertThat(result.isSuccess()).isTrue();
        List<SearchResult> results = result.get();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getRankPosition()).isEqualTo(5);  // Uses position field
        assertThat(results.get(1).getRankPosition()).isEqualTo(10); // Uses position field
    }

    @Test
    @DisplayName("Fallback to array index when position field missing")
    void executeSearch_FallbackToArrayIndex_WhenPositionMissing() {
        // Given: Response without position fields
        String mockResponse = """
                {
                    "organic": [
                        {
                            "title": "Result 1",
                            "link": "https://example1.org",
                            "snippet": "Snippet 1"
                        },
                        {
                            "title": "Result 2",
                            "link": "https://example2.org",
                            "snippet": "Snippet 2"
                        },
                        {
                            "title": "Result 3",
                            "link": "https://example3.org",
                            "snippet": "Snippet 3"
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test", 20, discoverySessionId);

        // Then: Should fall back to 1-based array indexing
        assertThat(result.isSuccess()).isTrue();
        List<SearchResult> results = result.get();
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getRankPosition()).isEqualTo(1); // Array index + 1
        assertThat(results.get(1).getRankPosition()).isEqualTo(2);
        assertThat(results.get(2).getRankPosition()).isEqualTo(3);
    }

    @Test
    @DisplayName("Adapter reports correct provider type")
    void getProviderType_ReturnsSERPER() {
        assertThat(adapter.getProviderType()).isEqualTo(SearchEngineType.SERPER);
    }

    @Test
    @DisplayName("Adapter supports keyword queries only")
    void supportsKeywordQueries_ReturnsTrue() {
        assertThat(adapter.supportsKeywordQueries()).isTrue();
        assertThat(adapter.supportsAIOptimizedQueries()).isFalse();
    }

    @Test
    @DisplayName("Rate limit tracking works correctly")
    void getCurrentUsageCount_TracksCorrectly() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "organic": []
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When: Execute 5 searches
        assertThat(adapter.getCurrentUsageCount()).isEqualTo(0);

        for (int i = 0; i < 5; i++) {
            adapter.executeSearch("query" + i, 20, discoverySessionId);
        }

        // Then
        assertThat(adapter.getCurrentUsageCount()).isEqualTo(5);
        assertThat(adapter.getRateLimit()).isEqualTo(60);
    }
}
