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
 * Contract Test: GET /api/candidates/{id}
 *
 * Tests the API contract for retrieving full candidate details
 * as defined in contracts/api-spec.yaml
 *
 * TDD: This test MUST FAIL until CandidateController is implemented
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateDetailControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnCandidateDetails() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";

        mockMvc.perform(get("/api/candidates/{candidateId}", candidateId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.confidenceScore").isNumber())
                .andExpect(jsonPath("$.organizationName").isString())
                .andExpect(jsonPath("$.programName").isString())
                .andExpect(jsonPath("$.sourceUrl").isString())
                .andExpect(jsonPath("$.discoveredAt").isString());
    }

    @Test
    void shouldIncludeOptionalFields() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";

        mockMvc.perform(get("/api/candidates/{candidateId}", candidateId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundingAmountMin").exists())
                .andExpect(jsonPath("$.fundingAmountMax").exists())
                .andExpect(jsonPath("$.currency").exists())
                .andExpect(jsonPath("$.geographicEligibility").isArray())
                .andExpect(jsonPath("$.organizationTypes").isArray())
                .andExpect(jsonPath("$.requirements").isArray())
                .andExpect(jsonPath("$.tags").isArray());
    }

    @Test
    void shouldIncludeDiscoveryMetadata() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";

        mockMvc.perform(get("/api/candidates/{candidateId}", candidateId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discoveryMethod").exists())
                .andExpect(jsonPath("$.searchQuery").exists())
                .andExpect(jsonPath("$.extractedData").exists());
    }

    @Test
    void shouldReturn404ForNonexistentCandidate() throws Exception {
        String nonexistentId = "99999999-9999-9999-9999-999999999999";

        mockMvc.perform(get("/api/candidates/{candidateId}", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400ForInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/candidates/{candidateId}", "invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
