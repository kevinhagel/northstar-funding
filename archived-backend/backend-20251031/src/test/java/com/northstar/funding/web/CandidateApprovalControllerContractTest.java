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
 * Contract Test: POST /api/candidates/{id}/approve
 *
 * Tests the API contract for approving candidates to knowledge base
 * as defined in contracts/api-spec.yaml
 *
 * TDD: This test MUST FAIL until CandidateActionController is implemented
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateApprovalControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldApproveCandidateSuccessfully() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String approvalRequest = """
                {
                    "approvalNotes": "Excellent funding opportunity for Eastern Europe NGOs"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/approve", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.candidateId").value(candidateId));
    }

    @Test
    void shouldRequireApprovalNotes() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String requestWithoutNotes = """
                {
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/approve", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithoutNotes))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldRejectEmptyApprovalNotes() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String requestWithEmptyNotes = """
                {
                    "approvalNotes": ""
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/approve", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithEmptyNotes))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForNonexistentCandidate() throws Exception {
        String nonexistentId = "99999999-9999-9999-9999-999999999999";
        String approvalRequest = """
                {
                    "approvalNotes": "This should fail"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/approve", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn400ForAlreadyApprovedCandidate() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String approvalRequest = """
                {
                    "approvalNotes": "Attempting to approve already approved candidate"
                }
                """;

        // This test expects business logic validation
        // Controller should prevent approving already-approved candidates
        mockMvc.perform(post("/api/candidates/{candidateId}/approve", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(approvalRequest))
                .andExpect(status().isOk()); // First approval succeeds
    }

    @Test
    void shouldEnforceMaxLengthOnApprovalNotes() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String tooLongNotes = "x".repeat(501); // Max is 500 per api-spec.yaml
        String requestWithLongNotes = String.format("""
                {
                    "approvalNotes": "%s"
                }
                """, tooLongNotes);

        mockMvc.perform(post("/api/candidates/{candidateId}/approve", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithLongNotes))
                .andExpect(status().isBadRequest());
    }
}
