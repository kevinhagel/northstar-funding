package com.northstar.funding.searchadapters.contract;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.searchadapters.SearchAdapter;
import com.northstar.funding.searchadapters.brave.BraveSearchAdapter;
import com.northstar.funding.searchadapters.config.SearchAdapterProperties;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Contract test for BraveSearchAdapter.
 *
 * Tests MUST FAIL initially (TDD approach) - no implementation exists yet.
 *
 * Brave Search API: https://api.search.brave.com/res/v1/web/search
 * Response format: JSON with "web" object containing "results" array
 */
public class BraveAdapterContractTest extends SearchAdapterContractTest {

    @Override
    protected SearchAdapter createAdapter(String baseUrl) {
        // Create config with WireMock base URL
        // BraveSearchAdapter expects full API endpoint URL (not just base)
        SearchAdapterProperties.BraveConfig config = new SearchAdapterProperties.BraveConfig();
        config.setApiUrl(baseUrl + "/res/v1/web/search");
        config.setApiKey("test-api-key");
        config.setTimeoutSeconds(10);

        return new BraveSearchAdapter(config);
    }

    @Override
    protected SearchEngineType getExpectedEngineType() {
        return SearchEngineType.BRAVE;
    }

    @Override
    protected String getMockSuccessResponseBody() {
        // Brave Search API response format
        return """
            {
              "web": {
                "results": [
                  {
                    "url": "https://education.ec.europa.eu/funding",
                    "title": "EU Education Funding Opportunities",
                    "description": "Discover funding for education projects in Bulgaria and Eastern Europe"
                  },
                  {
                    "url": "https://www.mon.bg/en/grants",
                    "title": "Bulgarian Ministry of Education Grants",
                    "description": "National education funding programs for teachers and schools"
                  },
                  {
                    "url": "https://ec.europa.eu/programmes/erasmus-plus/",
                    "title": "Erasmus+ Programme",
                    "description": "EU funding for education, training, youth and sport"
                  }
                ]
              }
            }
            """;
    }

    @Override
    protected String getMockZeroResultsResponseBody() {
        // Valid JSON but empty results array
        return """
            {
              "web": {
                "results": []
              }
            }
            """;
    }

    @Override
    protected void stubSuccessfulSearch() {
        wireMockServer.stubFor(get(urlPathEqualTo("/res/v1/web/search"))
            .withQueryParam("q", matching(".*"))
            .withHeader("X-Subscription-Token", equalTo("test-api-key"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getMockSuccessResponseBody())));
    }

    @Override
    protected void stubZeroResultsSearch() {
        wireMockServer.stubFor(get(urlPathEqualTo("/res/v1/web/search"))
            .withQueryParam("q", matching(".*"))
            .withHeader("X-Subscription-Token", equalTo("test-api-key"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getMockZeroResultsResponseBody())));
    }

    @Override
    protected void stubAuthenticationFailure() {
        wireMockServer.stubFor(get(urlPathEqualTo("/res/v1/web/search"))
            .withQueryParam("q", matching(".*"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"Invalid API key\"}")));
    }
}
