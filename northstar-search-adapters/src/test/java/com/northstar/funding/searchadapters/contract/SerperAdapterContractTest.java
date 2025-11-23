package com.northstar.funding.searchadapters.contract;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.serper.SerperAdapter;
import com.northstar.funding.searchadapters.config.SearchAdapterProperties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Contract test for SerperAdapter.
 *
 * Tests MUST FAIL initially (TDD approach) - no implementation exists yet.
 *
 * Serper.dev API: https://google.serper.dev/search
 * Response format: JSON with "organic" array containing search results
 */
public class SerperAdapterContractTest extends SearchAdapterContractTest {

    @Override
    protected SearchAdapter createAdapter(String baseUrl) {
        // Create config with WireMock base URL
        SearchAdapterProperties.SerperConfig config = new SearchAdapterProperties.SerperConfig();
        config.setApiUrl(baseUrl + "/search");
        config.setApiKey("test-api-key");
        config.setTimeoutSeconds(10);

        // SerperAdapter doesn't exist yet - test will fail
        return new SerperAdapter(config);
    }

    @Override
    protected SearchEngineType getExpectedEngineType() {
        return SearchEngineType.SERPER;
    }

    @Override
    protected String getMockSuccessResponseBody() {
        // Serper.dev API response format (Google Search results)
        return """
            {
              "organic": [
                {
                  "link": "https://ec.europa.eu/info/funding-tenders",
                  "title": "EU Funding and Tenders Portal",
                  "snippet": "Search and apply for EU funding opportunities for education and research"
                },
                {
                  "link": "https://www.fulbright.bg/",
                  "title": "Fulbright Bulgaria",
                  "snippet": "US government scholarships for Bulgarian educators and students"
                },
                {
                  "link": "https://eacea.ec.europa.eu/homepage",
                  "title": "European Education and Culture Executive Agency",
                  "snippet": "Managing EU funding programs for education, culture and audiovisual sectors"
                }
              ]
            }
            """;
    }

    @Override
    protected String getMockZeroResultsResponseBody() {
        // Valid JSON but empty organic array
        return """
            {
              "organic": []
            }
            """;
    }

    @Override
    protected void stubSuccessfulSearch() {
        wireMockServer.stubFor(post(urlPathEqualTo("/search"))
            .withHeader("X-API-KEY", equalTo("test-api-key"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getMockSuccessResponseBody())));
    }

    @Override
    protected void stubZeroResultsSearch() {
        wireMockServer.stubFor(post(urlPathEqualTo("/search"))
            .withHeader("X-API-KEY", equalTo("test-api-key"))
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
                .withBody("{\"message\": \"Unauthorized - Invalid API key\"}")));
    }
}
