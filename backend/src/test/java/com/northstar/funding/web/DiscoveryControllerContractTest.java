package com.northstar.funding.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Test: POST /api/discovery/trigger
 *
 * Tests the API contract for manually triggering discovery sessions
 * as defined in contracts/api-spec.yaml
 *
 * TDD: This test MUST FAIL until DiscoveryController is implemented
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscoveryControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldTriggerDiscoverySessionSuccessfully() throws Exception {
        String triggerRequest = """
                {
                    "searchEngines": ["searxng", "tavily"],
                    "targetRegions": ["Bulgaria", "Eastern Europe"]
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(triggerRequest))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.estimatedDurationMinutes").isNumber());
    }

    @Test
    void shouldAcceptMinimalTriggerRequest() throws Exception {
        String minimalRequest = """
                {
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(minimalRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.status").isString());
    }

    @Test
    void shouldAcceptCustomQueries() throws Exception {
        String customQueriesRequest = """
                {
                    "customQueries": [
                        "funding opportunities Eastern Europe NGO",
                        "grants for research institutions Bulgaria",
                        "EU funding programs Romania"
                    ]
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customQueriesRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    void shouldAcceptSpecificSearchEngines() throws Exception {
        String searchEnginesRequest = """
                {
                    "searchEngines": ["searxng", "perplexity"]
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchEnginesRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    void shouldAcceptTargetRegions() throws Exception {
        String targetRegionsRequest = """
                {
                    "targetRegions": ["Bulgaria", "Romania", "Greece", "Serbia"]
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(targetRegionsRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sessionId").exists());
    }

    @Test
    void shouldReturnSessionWithRunningStatus() throws Exception {
        String triggerRequest = """
                {
                    "searchEngines": ["searxng"]
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(triggerRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void shouldReturn400ForInvalidSearchEngine() throws Exception {
        String invalidRequest = """
                {
                    "searchEngines": ["invalid-search-engine"]
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn400ForMalformedRequest() throws Exception {
        String malformedRequest = "{ invalid json }";

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptCompleteDiscoveryConfiguration() throws Exception {
        String completeRequest = """
                {
                    "searchEngines": ["searxng", "tavily", "perplexity"],
                    "customQueries": [
                        "technology research grants Bulgaria",
                        "innovation funding Eastern Europe"
                    ],
                    "targetRegions": ["Bulgaria", "Romania", "Greece"]
                }
                """;

        mockMvc.perform(post("/api/discovery/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content(completeRequest))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.startedAt").exists());
    }
}
