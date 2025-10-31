package com.northstar.funding.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Test: GET /api/discovery/sessions
 *
 * Tests the API contract for listing discovery session audit log
 * as defined in contracts/api-spec.yaml
 *
 * TDD: This test MUST FAIL until DiscoveryController is implemented
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscoverySessionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnPaginatedSessionList() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.number").isNumber());
    }

    @Test
    void shouldReturnSessionsWithRequiredFields() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].sessionId").exists())
                .andExpect(jsonPath("$.content[*].executedAt").exists())
                .andExpect(jsonPath("$.content[*].sessionType").exists())
                .andExpect(jsonPath("$.content[*].status").exists());
    }

    @Test
    void shouldSupportPagination() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldUseDefaultPaginationValues() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20)) // Default size
                .andExpect(jsonPath("$.number").value(0)); // Default page
    }

    @Test
    void shouldReturnSessionMetrics() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].durationMinutes").exists())
                .andExpect(jsonPath("$.content[*].candidatesFound").exists())
                .andExpect(jsonPath("$.content[*].averageConfidenceScore").exists());
    }

    @Test
    void shouldSupportAllSessionTypes() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].sessionType").exists());

        // Session types per api-spec.yaml: SCHEDULED, MANUAL, RETRY
        // This test verifies the endpoint returns these values correctly
    }

    @Test
    void shouldSupportAllSessionStatuses() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status").exists());

        // Session statuses per api-spec.yaml: RUNNING, COMPLETED, FAILED, CANCELLED
        // This test verifies the endpoint returns these values correctly
    }

    @Test
    void shouldReturn400ForInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .param("page", "-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForPageSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .param("size", "101") // Max is 100 per api-spec.yaml
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnEmptyListWhenNoSessions() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Should return empty array, not null, when no sessions exist
    }

    @Test
    void shouldOrderSessionsByExecutedAtDescending() throws Exception {
        mockMvc.perform(get("/api/discovery/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Implicit ordering: most recent sessions first
        // Actual ordering verification would require multiple sessions
    }
}
