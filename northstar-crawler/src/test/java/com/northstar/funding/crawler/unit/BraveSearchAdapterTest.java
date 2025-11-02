package com.northstar.funding.crawler.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.northstar.funding.crawler.adapter.BraveSearchAdapter;
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
 * Unit tests for BraveSearchAdapter using WireMock for HTTP mocking.
 *
 * Tests:
 * - Successful search returns SearchResult entities
 * - HTTP 401 throws AuthenticationException
 * - HTTP 429 throws RateLimitException
 * - Timeout throws ProviderTimeoutException
 * - Domain normalization (www removal, lowercase)
 * - Rate limit enforcement (51st request fails)
 */
@DisplayName("BraveSearchAdapter Unit Tests")
class BraveSearchAdapterTest {

    private static WireMockServer wireMockServer;
    private BraveSearchAdapter adapter;
    private SearchProviderConfig config;
    private ObjectMapper objectMapper;
    private UUID discoverySessionId;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
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
        SearchProviderConfig.BraveSearchConfig braveConfig = new SearchProviderConfig.BraveSearchConfig();
        braveConfig.setApiKey("test-api-key");
        braveConfig.setBaseUrl("http://localhost:8089/search");
        braveConfig.setTimeout(2000);
        braveConfig.setMaxResults(20);
        braveConfig.setRateLimit(new SearchProviderConfig.RateLimit(50));
        config.setBraveSearch(braveConfig);

        adapter = new BraveSearchAdapter(config, objectMapper);
    }

    @Test
    @DisplayName("Successful search returns SearchResult entities")
    void executeSearch_Success_ReturnsSearchResults() {
        // Given: Mock successful BraveSearch response
        String mockResponse = """
                {
                    "web": {
                        "results": [
                            {
                                "title": "Education Funding Program",
                                "url": "https://example.org/funding",
                                "description": "Grant opportunities for education"
                            },
                            {
                                "title": "Scholarship Information",
                                "url": "https://www.university.edu/scholarships",
                                "description": "Financial aid for students"
                            }
                        ]
                    }
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("education grants", 20, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        List<SearchResult> results = result.get();
        assertThat(results).hasSize(2);

        // Verify first result
        SearchResult first = results.get(0);
        assertThat(first.getTitle()).isEqualTo("Education Funding Program");
        assertThat(first.getUrl()).isEqualTo("https://example.org/funding");
        assertThat(first.getDomain()).isEqualTo("example.org"); // Normalized
        assertThat(first.getDescription()).isEqualTo("Grant opportunities for education");
        assertThat(first.getRankPosition()).isEqualTo(1);
        assertThat(first.getSearchEngine()).isEqualTo(SearchEngineType.BRAVE);
        assertThat(first.getDiscoverySessionId()).isEqualTo(discoverySessionId);

        // Verify second result - domain normalization
        SearchResult second = results.get(1);
        assertThat(second.getDomain()).isEqualTo("university.edu"); // www removed
        assertThat(second.getRankPosition()).isEqualTo(2);

        // Verify HTTP request
        verify(getRequestedFor(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("education grants")) // WireMock decodes the URL
                .withQueryParam("count", equalTo("20"))
                .withHeader("X-Subscription-Token", equalTo("test-api-key")));
    }

    @Test
    @DisplayName("HTTP 401 throws AuthenticationException")
    void executeSearch_Unauthorized_ThrowsAuthenticationException() {
        // Given: Mock 401 response
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(401)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 20, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(AuthenticationException.class);
        assertThat(result.getCause().getMessage()).contains("BraveSearch");
    }

    @Test
    @DisplayName("HTTP 403 throws AuthenticationException")
    void executeSearch_Forbidden_ThrowsAuthenticationException() {
        // Given: Mock 403 response
        stubFor(get(urlPathEqualTo("/search"))
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
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse().withStatus(429)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("test query", 20, discoverySessionId);

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isInstanceOf(RateLimitException.class);
        assertThat(result.getCause().getMessage()).contains("BraveSearch");
    }

    @Test
    @DisplayName("Timeout throws ProviderTimeoutException")
    void executeSearch_Timeout_ThrowsProviderTimeoutException() {
        // Given: Mock slow response (longer than 2 second timeout)
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(3000))); // 3 second delay

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
        SearchProviderConfig.BraveSearchConfig braveConfig = new SearchProviderConfig.BraveSearchConfig();
        braveConfig.setApiKey(""); // Empty API key
        braveConfig.setBaseUrl("http://localhost:8089/search");
        braveConfig.setTimeout(2000);
        braveConfig.setMaxResults(20);
        braveConfig.setRateLimit(new SearchProviderConfig.RateLimit(50));
        config.setBraveSearch(braveConfig);

        BraveSearchAdapter adapterWithoutKey = new BraveSearchAdapter(config, objectMapper);

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
                    "web": {
                        "results": [
                            {
                                "title": "Test",
                                "url": "https://EXAMPLE.ORG/page",
                                "description": "Test description"
                            }
                        ]
                    }
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
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
                    "web": {
                        "results": [
                            {
                                "title": "Test",
                                "url": "https://www.example.org/page",
                                "description": "Test description"
                            }
                        ]
                    }
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
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
                    "web": {
                        "results": []
                    }
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
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
    @DisplayName("Rate limit enforcement - 51st request fails")
    void executeSearch_RateLimitEnforcement_51stRequestFails() {
        // Given: Mock successful responses
        String mockResponse = """
                {
                    "web": {
                        "results": [
                            {
                                "title": "Test",
                                "url": "https://example.org/page",
                                "description": "Test"
                            }
                        ]
                    }
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When: Execute 50 requests (should all succeed)
        for (int i = 0; i < 50; i++) {
            Try<List<SearchResult>> result = adapter.executeSearch("query" + i, 20, discoverySessionId);
            assertThat(result.isSuccess())
                    .withFailMessage("Request " + (i + 1) + " should succeed")
                    .isTrue();
        }

        // Then: 51st request should fail with RateLimitException
        Try<List<SearchResult>> result51 = adapter.executeSearch("query51", 20, discoverySessionId);
        assertThat(result51.isFailure()).isTrue();
        assertThat(result51.getCause()).isInstanceOf(RateLimitException.class);
        assertThat(result51.getCause().getMessage()).contains("50");
    }

    @Test
    @DisplayName("Adapter reports correct provider type")
    void getProviderType_ReturnsBRAVE() {
        assertThat(adapter.getProviderType()).isEqualTo(SearchEngineType.BRAVE);
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
                    "web": {
                        "results": []
                    }
                }
                """;

        stubFor(get(urlPathEqualTo("/search"))
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
        assertThat(adapter.getRateLimit()).isEqualTo(50);
    }
}
