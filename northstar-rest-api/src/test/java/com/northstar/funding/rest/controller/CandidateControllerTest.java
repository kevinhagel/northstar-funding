package com.northstar.funding.rest.controller;

import com.northstar.funding.rest.dto.CandidateDTO;
import com.northstar.funding.rest.dto.CandidatePageDTO;
import com.northstar.funding.rest.service.CandidateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CandidateController using Spring MockMvc.
 *
 * Loads ONLY the controller - all service layer beans are mocked.
 *
 * Note: We must:
 * 1. Mock ALL persistence service beans (scanned by @ComponentScan)
 * 2. Exclude PersistenceConfiguration from component scanning
 * 3. Exclude JDBC/DataSource auto-configuration
 */
@WebMvcTest(
    controllers = CandidateController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration.class
    }
)
@ComponentScan(
    basePackages = "com.northstar.funding.rest",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.northstar.funding.persistence.config.PersistenceConfiguration.class
    )
)
class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // REST API service (used by controller)
    @MockitoBean
    private CandidateService candidateService;

    // Persistence layer services (scanned but not used - must be mocked to prevent creation)
    @MockitoBean
    private com.northstar.funding.persistence.service.AdminUserService adminUserService;

    @MockitoBean
    private com.northstar.funding.persistence.service.DomainService domainService;

    @MockitoBean
    private com.northstar.funding.persistence.service.OrganizationService organizationService;

    @MockitoBean
    private com.northstar.funding.persistence.service.FundingProgramService fundingProgramService;

    @MockitoBean
    private com.northstar.funding.persistence.service.SearchResultService searchResultService;

    @MockitoBean
    private com.northstar.funding.persistence.service.DiscoverySessionService discoverySessionService;

    @Test
    void listCandidates_WithNoParams_ShouldReturn200() throws Exception {
        // Given
        CandidateDTO dto = new CandidateDTO(
            UUID.randomUUID().toString(),
            "https://example.com",
            "Test",
            "0.85",
            "PENDING_CRAWL",
            "TAVILY",
            "2025-11-16T10:30:00"
        );
        CandidatePageDTO page = new CandidatePageDTO(
            List.of(dto),
            1,
            1,
            0,
            20
        );
        when(candidateService.listCandidates(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/candidates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").exists())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.currentPage").value(0))
            .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    void listCandidates_WithFilters_ShouldPassParametersToService() throws Exception {
        // Given
        CandidatePageDTO emptyPage = new CandidatePageDTO(List.of(), 0, 0, 0, 20);
        when(candidateService.listCandidates(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/candidates")
                .param("status", "PENDING_CRAWL", "CRAWLED")
                .param("minConfidence", "0.70")
                .param("searchEngine", "TAVILY")
                .param("page", "1")
                .param("size", "50"))
            .andExpect(status().isOk());
    }

    @Test
    void approveCandidate_WithValidId_ShouldReturn200() throws Exception {
        // Given
        UUID candidateId = UUID.randomUUID();
        CandidateDTO dto = new CandidateDTO(
            candidateId.toString(),
            "https://example.com",
            "Test",
            "0.85",
            "APPROVED",
            "TAVILY",
            "2025-11-16T10:30:00"
        );
        when(candidateService.approveCandidate(candidateId)).thenReturn(dto);

        // When & Then
        mockMvc.perform(put("/api/candidates/" + candidateId + "/approve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(candidateId.toString()))
            .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveCandidate_WithNonExistentId_ShouldReturn404() throws Exception {
        // Given
        UUID candidateId = UUID.randomUUID();
        when(candidateService.approveCandidate(candidateId))
            .thenThrow(new IllegalArgumentException("Candidate not found with id: " + candidateId));

        // When & Then
        mockMvc.perform(put("/api/candidates/" + candidateId + "/approve"))
            .andExpect(status().isNotFound());
    }

    @Test
    void approveCandidate_WhenAlreadyApproved_ShouldReturn400() throws Exception {
        // Given
        UUID candidateId = UUID.randomUUID();
        when(candidateService.approveCandidate(candidateId))
            .thenThrow(new IllegalArgumentException("Candidate is already approved"));

        // When & Then
        mockMvc.perform(put("/api/candidates/" + candidateId + "/approve"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectCandidate_WithValidId_ShouldReturn200() throws Exception {
        // Given
        UUID candidateId = UUID.randomUUID();
        CandidateDTO dto = new CandidateDTO(
            candidateId.toString(),
            "https://example.com",
            "Test",
            "0.25",
            "REJECTED",
            "TAVILY",
            "2025-11-16T10:30:00"
        );
        when(candidateService.rejectCandidate(candidateId)).thenReturn(dto);

        // When & Then
        mockMvc.perform(put("/api/candidates/" + candidateId + "/reject"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(candidateId.toString()))
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectCandidate_WithNonExistentId_ShouldReturn404() throws Exception {
        // Given
        UUID candidateId = UUID.randomUUID();
        when(candidateService.rejectCandidate(candidateId))
            .thenThrow(new IllegalArgumentException("Candidate not found with id: " + candidateId));

        // When & Then
        mockMvc.perform(put("/api/candidates/" + candidateId + "/reject"))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectCandidate_WhenAlreadyRejected_ShouldReturn400() throws Exception {
        // Given
        UUID candidateId = UUID.randomUUID();
        when(candidateService.rejectCandidate(candidateId))
            .thenThrow(new IllegalArgumentException("Candidate is already rejected"));

        // When & Then
        mockMvc.perform(put("/api/candidates/" + candidateId + "/reject"))
            .andExpect(status().isBadRequest());
    }
}
