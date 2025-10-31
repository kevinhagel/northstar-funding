package com.northstar.funding.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract Test: GET/POST /api/candidates/{id}/contacts
 *
 * Tests the API contract for contact intelligence management
 * as defined in contracts/api-spec.yaml
 *
 * TDD: This test MUST FAIL until ContactIntelligenceController is implemented
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactIntelligenceControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetContactsForCandidate() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";

        mockMvc.perform(get("/api/candidates/{candidateId}/contacts", candidateId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturnContactWithRequiredFields() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";

        mockMvc.perform(get("/api/candidates/{candidateId}/contacts", candidateId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].contactId").exists())
                .andExpect(jsonPath("$[*].candidateId").exists())
                .andExpect(jsonPath("$[*].contactType").exists())
                .andExpect(jsonPath("$[*].fullName").exists());
    }

    @Test
    void shouldReturn404ForNonexistentCandidate() throws Exception {
        String nonexistentId = "99999999-9999-9999-9999-999999999999";

        mockMvc.perform(get("/api/candidates/{candidateId}/contacts", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldAddNewContactSuccessfully() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String contactRequest = """
                {
                    "contactType": "PROGRAM_OFFICER",
                    "fullName": "Dr. Maria Ivanova",
                    "title": "Senior Program Officer",
                    "email": "m.ivanova@foundation.bg",
                    "phone": "+359 2 123 4567",
                    "organization": "Bulgarian Research Foundation",
                    "officeAddress": "Sofia, Bulgaria",
                    "communicationPreference": "email",
                    "referralSource": "Foundation website",
                    "decisionAuthority": "DECISION_MAKER",
                    "relationshipNotes": "Handles Eastern Europe funding applications"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/contacts", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(contactRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.contactId").exists())
                .andExpect(jsonPath("$.candidateId").value(candidateId))
                .andExpect(jsonPath("$.fullName").value("Dr. Maria Ivanova"))
                .andExpect(jsonPath("$.email").value("m.ivanova@foundation.bg"));
    }

    @Test
    void shouldRequireFullNameAndContactType() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String incompleteRequest = """
                {
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/contacts", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldAllowMinimalContactWithOnlyRequiredFields() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String minimalRequest = """
                {
                    "contactType": "FOUNDATION_STAFF",
                    "fullName": "John Smith"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/contacts", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(minimalRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactId").exists())
                .andExpect(jsonPath("$.fullName").value("John Smith"));
    }

    @Test
    void shouldValidateEmailFormat() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String invalidEmailRequest = """
                {
                    "contactType": "PROGRAM_OFFICER",
                    "fullName": "Test User",
                    "email": "not-a-valid-email"
                }
                """;

        mockMvc.perform(post("/api/candidates/{candidateId}/contacts", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEmailRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldEnforceMaxLengthConstraints() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String tooLongName = "x".repeat(256); // Max is 255
        String invalidLengthRequest = String.format("""
                {
                    "contactType": "FOUNDATION_STAFF",
                    "fullName": "%s"
                }
                """, tooLongName);

        mockMvc.perform(post("/api/candidates/{candidateId}/contacts", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidLengthRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSupportAllContactTypes() throws Exception {
        String candidateId = "123e4567-e89b-12d3-a456-426614174000";
        String[] contactTypes = {
            "PROGRAM_OFFICER",
            "FOUNDATION_STAFF",
            "GOVERNMENT_OFFICIAL",
            "ACADEMIC_CONTACT",
            "CORPORATE_CONTACT"
        };

        for (String contactType : contactTypes) {
            String contactRequest = String.format("""
                    {
                        "contactType": "%s",
                        "fullName": "Test Contact for %s"
                    }
                    """, contactType, contactType);

            mockMvc.perform(post("/api/candidates/{candidateId}/contacts", candidateId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(contactRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.contactType").value(contactType));
        }
    }
}
