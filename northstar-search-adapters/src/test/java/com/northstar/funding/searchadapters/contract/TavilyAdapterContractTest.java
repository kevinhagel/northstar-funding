package com.northstar.funding.searchadapters.contract;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.tavily.TavilyAdapter;
import com.northstar.funding.searchadapters.config.SearchAdapterProperties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Contract test for TavilyAdapter.
 *
 * Tests MUST FAIL initially (TDD approach) - no implementation exists yet.
 *
 * Tavily AI Search API: https://api.tavily.com/search
 * Response format: JSON with "results" array containing AI-enhanced search results
 */
public class TavilyAdapterContractTest extends SearchAdapterContractTest {

    @Override
    protected SearchAdapter createAdapter(String baseUrl) {
        // Create config with WireMock base URL
        SearchAdapterProperties.TavilyConfig config = new SearchAdapterProperties.TavilyConfig();
        config.setApiUrl(baseUrl + "/search");
        config.setApiKey("test-api-key");
        config.setTimeoutSeconds(15);

        // TavilyAdapter doesn't exist yet - test will fail
        return new TavilyAdapter(config);
    }

    @Override
    protected SearchEngineType getExpectedEngineType() {
        return SearchEngineType.TAVILY;
    }

    @Override
    protected String getMockSuccessResponseBody() {
        // Tavily AI Search API response format
        return """
            {
              "results": [
                {
                  "url": "https://www.eif.org/what_we_do/guarantees/index.htm",
                  "title": "European Investment Fund - Guarantees and Funding",
                  "content": "EIF provides funding and guarantees for SMEs, including education sector projects in EU member states"
                },
                {
                  "url": "https://www.americancouncils.bg/programs",
                  "title": "American Councils Bulgaria Programs",
                  "content": "Educational exchange programs with US government funding for Bulgarian educators"
                },
                {
                  "url": "https://www.uni-foundation.eu/funding",
                  "title": "University Foundation European Grants",
                  "content": "Foundation providing educational grants for modernization and innovation in Eastern Europe"
                }
              ]
            }
            """;
    }

    @Override
    protected String getMockZeroResultsResponseBody() {
        // Valid JSON but empty results array
        return """
            {
              "results": []
            }
            """;
    }

    @Override
    protected void stubSuccessfulSearch() {
        wireMockServer.stubFor(post(urlPathEqualTo("/search"))
            .withHeader("Authorization", equalTo("Bearer test-api-key"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getMockSuccessResponseBody())));
    }

    @Override
    protected void stubZeroResultsSearch() {
        wireMockServer.stubFor(post(urlPathEqualTo("/search"))
            .withHeader("Authorization", equalTo("Bearer test-api-key"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getMockZeroResultsResponseBody())));
    }

    @Override
    protected void stubAuthenticationFailure() {
        wireMockServer.stubFor(post(urlPathEqualTo("/search"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"Invalid or missing API key\"}")));
    }
}
