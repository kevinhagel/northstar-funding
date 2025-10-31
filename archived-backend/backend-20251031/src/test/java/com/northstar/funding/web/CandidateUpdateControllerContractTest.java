package com.northstar.funding.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Test: PUT /api/candidates/{id}
 *
 * Tests the API contract for updating candidate with admin enhancements
 * as defined in contracts/api-spec.yaml
 *
 * TDD: This test MUST FAIL until CandidateController is implemented
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateUpdateControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUpdateCandidateWithEnhancements() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String updateRequest = """
                {
                    "organizationName": "Enhanced Foundation Name",
                    "programName": "Enhanced Program Name",
                    "description": "Enhanced detailed description of the funding program",
                    "fundingAmountMin": 10000.00,
                    "fundingAmountMax": 50000.00,
                    "currency": "EUR",
                    "geographicEligibility": ["Bulgaria", "Romania", "Eastern Europe"],
                    "organizationTypes": ["NGO", "Research Institution"],
                    "applicationProcess": "Submit via online portal",
                    "requirements": ["Registered NGO", "Project proposal", "Budget plan"],
                    "tags": ["research", "innovation", "eastern-europe"],
                    "validationNotes": "Verified contact information and deadline"
                }
                """;

        mockMvc.perform(put("/api/candidates/{candidateId}", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.organizationName").value("Enhanced Foundation Name"))
                .andExpect(jsonPath("$.programName").value("Enhanced Program Name"));
    }

    @Test
    void shouldAllowPartialUpdate() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String partialUpdate = """
                {
                    "validationNotes": "Added new validation notes"
                }
                """;

        mockMvc.perform(put("/api/candidates/{candidateId}", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(partialUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateId").value(candidateId));
    }

    @Test
    void shouldReturn404ForNonexistentCandidate() throws Exception {
        String nonexistentId = "99999999-9999-9999-9999-999999999999";
        String updateRequest = """
                {
                    "validationNotes": "This should fail"
                }
                """;

        mockMvc.perform(put("/api/candidates/{candidateId}", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn400ForInvalidData() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String invalidRequest = """
                {
                    "organizationName": "",
                    "fundingAmountMin": -1000
                }
                """;

        mockMvc.perform(put("/api/candidates/{candidateId}", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForMalformedJson() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String malformedJson = "{ invalid json }";

        mockMvc.perform(put("/api/candidates/{candidateId}", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest());
    }
}
