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
 * Contract Test: POST /api/candidates/{id}/reject
 *
 * Tests the API contract for rejecting candidates from review queue
 * as defined in contracts/api-spec.yaml
 *
 * TDD: This test MUST FAIL until CandidateActionController is implemented
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateRejectionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectCandidateSuccessfully() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String rejectionRequest = """
                {
                    "rejectionReason": "Funding program not applicable to Eastern Europe"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/reject", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectionRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.candidateId").value(candidateId));
    }

    @Test
    void shouldRequireRejectionReason() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String requestWithoutReason = """
                {
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/reject", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithoutReason))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectEmptyRejectionReason() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String requestWithEmptyReason = """
                {
                    "rejectionReason": ""
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/reject", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithEmptyReason))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForNonexistentCandidate() throws Exception {
        String nonexistentId = "99999999-9999-9999-9999-999999999999";
        String rejectionRequest = """
                {
                    "rejectionReason": "This should fail"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/reject", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rejectionRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldEnforceMaxLengthOnRejectionReason() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String tooLongReason = "x".repeat(501); // Max is 500 per api-spec.yaml
        String requestWithLongReason = String.format("""
                {
                    "rejectionReason": "%s"
                }
                """, tooLongReason);

        mockMvc.perform(post("/api/candidates/{candidateId}/reject", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithLongReason))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowCommonRejectionReasons() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String[] commonReasons = {
            "Duplicate of existing funding source",
            "Geographic eligibility does not match Eastern Europe",
            "Funding amount too small for organizational needs",
            "Application deadline has passed",
            "Organization type not eligible",
            "Low confidence score - insufficient data quality"
        };

        for (String reason : commonReasons) {
            String rejectionRequest = String.format("""
                    {
                        "rejectionReason": "%s"
                    }
                    """, reason);

            mockMvc.perform(post("/api/candidates/{candidateId}/reject", candidateId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rejectionRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
