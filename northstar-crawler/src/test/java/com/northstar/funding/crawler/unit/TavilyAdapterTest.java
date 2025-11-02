package com.northstar.funding.crawler.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.northstar.funding.crawler.adapter.TavilyAdapter;
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
 * Unit tests for TavilyAdapter using WireMock for HTTP mocking.
 *
 * Tests:
 * - Successful search returns SearchResult entities
 * - POST with JSON body (api_key in body, not header)
 * - HTTP 401/403 throws AuthenticationException
 * - HTTP 429 throws RateLimitException
 * - Timeout throws ProviderTimeoutException
 * - Empty API key throws AuthenticationException
 * - Domain normalization (www removal, lowercase)
 * - Rate limit enforcement (26th request fails - most restrictive)
 * - AI-generated answer parsing
 * - Supports both keyword and AI-optimized queries
 * - search_depth parameter sent
 */
@DisplayName("TavilyAdapter Unit Tests")
class TavilyAdapterTest {

    private static WireMockServer wireMockServer;
    private TavilyAdapter adapter;
    private SearchProviderConfig config;
    private ObjectMapper objectMapper;
    private UUID discoverySessionId;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8092);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8092);
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
        SearchProviderConfig.TavilyConfig tavilyConfig = new SearchProviderConfig.TavilyConfig();
        tavilyConfig.setApiKey("test-tavily-key");
        tavilyConfig.setBaseUrl("http://localhost:8092/search");
        tavilyConfig.setTimeout(6000);
        tavilyConfig.setMaxResults(20);
        tavilyConfig.setSearchDepth("basic");
        tavilyConfig.setRateLimit(new SearchProviderConfig.RateLimit(25));
        config.setTavily(tavilyConfig);

        adapter = new TavilyAdapter(config, objectMapper);
    }

    @Test
    @DisplayName("Successful search returns SearchResult entities")
    void executeSearch_Success_ReturnsSearchResults() {
        // Given: Mock successful Tavily response with AI answer
        String mockResponse = """
                {
                    "answer": "Educational grants in the EU are primarily funded through programs like Horizon Europe and Erasmus+",
                    "results": [
                        {
                            "title": "Horizon Europe Official Portal",
                            "url": "https://ec.europa.eu/horizon-europe",
                            "content": "Horizon Europe is the EU's research and innovation program"
                        },
                        {
                            "title": "Erasmus+ Programme Guide",
                            "url": "https://www.erasmus-plus.org/guide",
                            "content": "Erasmus+ supports education, training, youth and sport"
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
        Try<List<SearchResult>> result = adapter.executeSearch("What are the main educational grant programs in the EU?", 20, discoverySessionId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        List<SearchResult> results = result.get();
        assertThat(results).hasSize(2);

        // Verify first result
        SearchResult first = results.get(0);
        assertThat(first.getTitle()).isEqualTo("Horizon Europe Official Portal");
        assertThat(first.getUrl()).isEqualTo("https://ec.europa.eu/horizon-europe");
        assertThat(first.getDomain()).isEqualTo("ec.europa.eu");
        assertThat(first.getDescription()).isEqualTo("Horizon Europe is the EU's research and innovation program");
        assertThat(first.getRankPosition()).isEqualTo(1);
        assertThat(first.getSearchEngine()).isEqualTo(SearchEngineType.TAVILY);
        assertThat(first.getDiscoverySessionId()).isEqualTo(discoverySessionId);

        // Verify second result - domain normalization
        SearchResult second = results.get(1);
        assertThat(second.getDomain()).isEqualTo("erasmus-plus.org"); // www removed
        assertThat(second.getRankPosition()).isEqualTo(2);

        // Verify HTTP request was POST with JSON body
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.api_key", equalTo("test-tavily-key")))
                .withRequestBody(matchingJsonPath("$.query", equalTo("What are the main educational grant programs in the EU?")))
                .withRequestBody(matchingJsonPath("$.max_results", equalTo("20")))
                .withRequestBody(matchingJsonPath("$.search_depth", equalTo("basic")))
                .withRequestBody(matchingJsonPath("$.include_answer", equalTo("true"))));
    }

    @Test
    @DisplayName("POST with JSON body (api_key in body, not header)")
    void executeSearch_SendsApiKeyInBody() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "results": []
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

        // Verify api_key in JSON body (not in header)
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withRequestBody(matchingJsonPath("$.api_key", equalTo("test-tavily-key"))));

        // Verify NO authorization headers
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withoutHeader("Authorization")
                .withoutHeader("X-API-KEY")
                .withoutHeader("X-Subscription-Token"));
    }

    @Test
    @DisplayName("search_depth parameter sent correctly")
    void executeSearch_SendsSearchDepthParameter() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "results": []
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        adapter.executeSearch("test", 20, discoverySessionId);

        // Then: Verify search_depth parameter
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withRequestBody(matchingJsonPath("$.search_depth", equalTo("basic"))));
    }

    @Test
    @DisplayName("include_answer and include_raw_content parameters set correctly")
    void executeSearch_SendsCorrectIncludeParameters() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "results": []
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        adapter.executeSearch("test", 20, discoverySessionId);

        // Then: Verify include parameters
        verify(postRequestedFor(urlPathEqualTo("/search"))
                .withRequestBody(matchingJsonPath("$.include_answer", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.include_raw_content", equalTo("false"))));
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
        assertThat(result.getCause().getMessage()).contains("Tavily");
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
        assertThat(result.getCause().getMessage()).contains("Tavily");
    }

    @Test
    @DisplayName("Timeout throws ProviderTimeoutException")
    void executeSearch_Timeout_ThrowsProviderTimeoutException() {
        // Given: Mock slow response (longer than 6 second timeout)
        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(7000))); // 7 second delay

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
        SearchProviderConfig.TavilyConfig tavilyConfig = new SearchProviderConfig.TavilyConfig();
        tavilyConfig.setApiKey(""); // Empty API key
        tavilyConfig.setBaseUrl("http://localhost:8092/search");
        tavilyConfig.setTimeout(6000);
        tavilyConfig.setMaxResults(20);
        tavilyConfig.setSearchDepth("basic");
        tavilyConfig.setRateLimit(new SearchProviderConfig.RateLimit(25));
        config.setTavily(tavilyConfig);

        TavilyAdapter adapterWithoutKey = new TavilyAdapter(config, objectMapper);

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
                    "results": [
                        {
                            "title": "Test",
                            "url": "https://EXAMPLE.ORG/page",
                            "content": "Test description"
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
                    "results": [
                        {
                            "title": "Test",
                            "url": "https://www.example.org/page",
                            "content": "Test description"
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
                    "results": []
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
    @DisplayName("Rate limit enforcement - 26th request fails (most restrictive)")
    void executeSearch_RateLimitEnforcement_26thRequestFails() {
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

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When: Execute 25 requests (should all succeed)
        for (int i = 0; i < 25; i++) {
            Try<List<SearchResult>> result = adapter.executeSearch("query" + i, 20, discoverySessionId);
            assertThat(result.isSuccess())
                    .withFailMessage("Request " + (i + 1) + " should succeed")
                    .isTrue();
        }

        // Then: 26th request should fail with RateLimitException
        Try<List<SearchResult>> result26 = adapter.executeSearch("query26", 20, discoverySessionId);
        assertThat(result26.isFailure()).isTrue();
        assertThat(result26.getCause()).isInstanceOf(RateLimitException.class);
        assertThat(result26.getCause().getMessage()).contains("25");
    }

    @Test
    @DisplayName("AI-generated answer parsing")
    void executeSearch_ParsesAiGeneratedAnswer() {
        // Given: Response with AI answer
        String mockResponse = """
                {
                    "answer": "The European Union offers several major funding programs including Horizon Europe for research and Erasmus+ for education",
                    "results": [
                        {
                            "title": "Horizon Europe",
                            "url": "https://ec.europa.eu/horizon",
                            "content": "EU research program"
                        }
                    ]
                }
                """;

        stubFor(post(urlPathEqualTo("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockResponse)));

        // When
        Try<List<SearchResult>> result = adapter.executeSearch("EU funding programs", 20, discoverySessionId);

        // Then: Results should be returned (answer is logged but not stored in SearchResult)
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).hasSize(1);
    }

    @Test
    @DisplayName("Adapter supports both keyword and AI-optimized queries")
    void supportsQueries_SupportsBothTypes() {
        assertThat(adapter.supportsKeywordQueries()).isTrue();
        assertThat(adapter.supportsAIOptimizedQueries()).isTrue();
    }

    @Test
    @DisplayName("Adapter reports correct provider type")
    void getProviderType_ReturnsTAVILY() {
        assertThat(adapter.getProviderType()).isEqualTo(SearchEngineType.TAVILY);
    }

    @Test
    @DisplayName("Rate limit tracking works correctly")
    void getCurrentUsageCount_TracksCorrectly() {
        // Given: Mock successful response
        String mockResponse = """
                {
                    "results": []
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
        assertThat(adapter.getRateLimit()).isEqualTo(25);
    }
}
