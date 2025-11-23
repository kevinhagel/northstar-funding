package com.northstar.funding.searchadapters.contract;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.searxng.SearXNGAdapter;
import com.northstar.funding.searchadapters.config.SearchAdapterProperties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Contract test for SearXNGAdapter.
 *
 * Tests MUST FAIL initially (TDD approach) - no implementation exists yet.
 *
 * SearXNG API: http://192.168.1.10:8080/search
 * Response format: JSON with "results" array containing search results
 * Note: SearXNG does not require authentication
 */
public class SearXNGAdapterContractTest extends SearchAdapterContractTest {

    @Override
    protected SearchAdapter createAdapter(String baseUrl) {
        // Create config with WireMock base URL
        SearchAdapterProperties.SearxngConfig config = new SearchAdapterProperties.SearxngConfig();
        config.setApiUrl(baseUrl);
        config.setTimeoutSeconds(10);

        // SearXNGAdapter doesn't exist yet - test will fail
        return new SearXNGAdapter(config);
    }

    @Override
    protected SearchEngineType getExpectedEngineType() {
        return SearchEngineType.SEARXNG;
    }

    @Override
    protected String getMockSuccessResponseBody() {
        // SearXNG API response format
        return """
            {
              "results": [
                {
                  "url": "https://horizon-europe.ec.europa.eu/",
                  "title": "Horizon Europe - European Commission",
                  "content": "EU's research and innovation funding program 2021-2027"
                },
                {
                  "url": "https://www.britishcouncil.bg/en/programmes",
                  "title": "British Council Bulgaria Programmes",
                  "content": "Education and cultural programs with UK funding support"
                },
                {
                  "url": "https://www.usembassy.gov/education-culture/",
                  "title": "US Embassy Education and Culture",
                  "content": "US government educational exchange programs for Bulgaria"
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
        wireMockServer.stubFor(get(urlPathEqualTo("/search"))
            .withQueryParam("q", matching(".*"))
            .withQueryParam("format", equalTo("json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getMockSuccessResponseBody())));
    }

    @Override
    protected void stubZeroResultsSearch() {
        wireMockServer.stubFor(get(urlPathEqualTo("/search"))
            .withQueryParam("q", matching(".*"))
            .withQueryParam("format", equalTo("json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getMockZeroResultsResponseBody())));
    }

    @Override
    protected void stubAuthenticationFailure() {
        // SearXNG doesn't require auth, but simulate server error for consistency
        wireMockServer.stubFor(get(urlPathEqualTo("/search"))
            .withQueryParam("q", matching(".*"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"Internal server error\"}")));
    }
}
