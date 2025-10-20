package com.northstar.funding.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.northstar.funding.discovery.application.CandidateValidationService;
import com.northstar.funding.discovery.application.DiscoveryOrchestrationService;
import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.infrastructure.DiscoverySessionRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test: Complete Discovery to Approval Workflow
 *
 * Tests the complete end-to-end workflow:
 * 1. Trigger discovery session
 * 2. Process discovery results (candidates)
 * 3. Review candidate (manual step simulation)
 * 4. Approve candidate
 * 5. Verify final state in database
 *
 * Uses Testcontainers for PostgreSQL and full Spring Boot context
 */
@SpringBootTest
@Testcontainers
@Transactional
class DiscoveryWorkflowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DiscoveryOrchestrationService orchestrationService;

    @Autowired
    private CandidateValidationService validationService;

    @Autowired
    private FundingSourceCandidateRepository candidateRepository;

    @Autowired
    private DiscoverySessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        candidateRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void shouldCompleteFullDiscoveryToApprovalWorkflow() {
        // Step 1: Trigger discovery session
        List<String> searchEngines = List.of("searxng", "tavily");
        List<String> customQueries = List.of("Bulgarian startup funding");

        DiscoverySession session = orchestrationService.triggerDiscovery(searchEngines, customQueries);

        assertThat(session).isNotNull();
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.RUNNING);

        // Verify session saved to database
        DiscoverySession savedSession = sessionRepository.findById(session.getSessionId()).orElse(null);
        assertThat(savedSession).isNotNull();
        assertThat(savedSession.getSearchEnginesUsed()).contains("searxng", "tavily");

        // Step 2: Process discovery results (simulate found candidates)
        List<FundingSourceCandidate> discoveredCandidates = List.of(
                createCandidate("Bulgarian Innovation Fund", "Startup Accelerator 2024", 0.85),
                createCandidate("Sofia Tech Hub", "Early Stage Funding", 0.75),
                createCandidate("Balkan Ventures", "Seed Investment Program", 0.90)
        );

        orchestrationService.processDiscoveryResults(session.getSessionId(), discoveredCandidates);

        // Verify session completed
        DiscoverySession completedSession = sessionRepository.findById(session.getSessionId()).orElse(null);
        assertThat(completedSession).isNotNull();
        assertThat(completedSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completedSession.getCandidatesFound()).isEqualTo(3);

        // Verify candidates saved with PENDING_REVIEW status
        List<FundingSourceCandidate> savedCandidates = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW);
        assertThat(savedCandidates).hasSize(3);
        assertThat(savedCandidates).allMatch(c -> c.getDiscoverySessionId().equals(session.getSessionId()));

        // Step 3: Human reviewer examines candidate (simulated by selecting one)
        FundingSourceCandidate candidateToReview = savedCandidates.stream()
                .filter(c -> c.getOrganizationName().equals("Balkan Ventures"))
                .findFirst()
                .orElseThrow();

        assertThat(candidateToReview.getConfidenceScore()).isEqualTo(0.90);
        assertThat(candidateToReview.getStatus()).isEqualTo(CandidateStatus.PENDING_REVIEW);

        // Step 4: Approve candidate with notes
        UUID adminUserId = UUID.randomUUID(); // Simulate admin user
        String approvalNotes = "Excellent match for our target criteria. Strong track record in Bulgarian market.";

        FundingSourceCandidate approvedCandidate = validationService.approveCandidate(
                candidateToReview.getCandidateId(),
                adminUserId,
                approvalNotes
        );

        // Step 5: Verify final state
        assertThat(approvedCandidate.getStatus()).isEqualTo(CandidateStatus.APPROVED);
        assertThat(approvedCandidate.getApprovedBy()).isEqualTo(adminUserId);
        assertThat(approvedCandidate.getApprovedAt()).isNotNull();
        assertThat(approvedCandidate.getValidationNotes()).isEqualTo(approvalNotes);

        // Verify database persistence
        FundingSourceCandidate persistedCandidate = candidateRepository
                .findById(approvedCandidate.getCandidateId())
                .orElse(null);

        assertThat(persistedCandidate).isNotNull();
        assertThat(persistedCandidate.getStatus()).isEqualTo(CandidateStatus.APPROVED);
        assertThat(persistedCandidate.getApprovedBy()).isEqualTo(adminUserId);

        // Verify other candidates remain pending
        List<FundingSourceCandidate> pendingCandidates = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW);
        assertThat(pendingCandidates).hasSize(2);
    }

    @Test
    void shouldHandleMultipleDiscoverySessionsWithDifferentEngines() {
        // First discovery with Searxng only
        DiscoverySession session1 = orchestrationService.triggerDiscovery(
                List.of("searxng"),
                List.of("tech startups Bulgaria")
        );

        List<FundingSourceCandidate> candidates1 = List.of(
                createCandidate("Org A", "Program 1", 0.85)
        );
        orchestrationService.processDiscoveryResults(session1.getSessionId(), candidates1);

        // Second discovery with Tavily only
        DiscoverySession session2 = orchestrationService.triggerDiscovery(
                List.of("tavily"),
                List.of("innovation grants Eastern Europe")
        );

        List<FundingSourceCandidate> candidates2 = List.of(
                createCandidate("Org B", "Program 2", 0.75)
        );
        orchestrationService.processDiscoveryResults(session2.getSessionId(), candidates2);

        // Verify both sessions completed independently
        DiscoverySession completed1 = sessionRepository.findById(session1.getSessionId()).orElse(null);
        DiscoverySession completed2 = sessionRepository.findById(session2.getSessionId()).orElse(null);

        assertThat(completed1).isNotNull();
        assertThat(completed1.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed1.getCandidatesFound()).isEqualTo(1);

        assertThat(completed2).isNotNull();
        assertThat(completed2.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completed2.getCandidatesFound()).isEqualTo(1);

        // Verify candidates are associated with correct sessions
        List<FundingSourceCandidate> allCandidates = candidateRepository.findByStatus(CandidateStatus.PENDING_REVIEW);
        assertThat(allCandidates).hasSize(2);

        FundingSourceCandidate candidate1 = allCandidates.stream()
                .filter(c -> c.getOrganizationName().equals("Org A"))
                .findFirst()
                .orElseThrow();
        assertThat(candidate1.getDiscoverySessionId()).isEqualTo(session1.getSessionId());

        FundingSourceCandidate candidate2 = allCandidates.stream()
                .filter(c -> c.getOrganizationName().equals("Org B"))
                .findFirst()
                .orElseThrow();
        assertThat(candidate2.getDiscoverySessionId()).isEqualTo(session2.getSessionId());
    }

    @Test
    void shouldFilterLowConfidenceCandidatesDuringProcessing() {
        // Trigger session
        DiscoverySession session = orchestrationService.triggerDiscovery(
                List.of("searxng"),
                List.of("test query")
        );

        // Process candidates with mixed confidence scores
        List<FundingSourceCandidate> candidates = List.of(
                createCandidate("High Quality", "Program A", 0.95),
                createCandidate("Low Quality", "Program B", 0.25),  // Below 0.5 threshold
                createCandidate("Acceptable", "Program C", 0.65),
                createCandidate("Very Low", "Program D", 0.10)      // Below 0.5 threshold
        );

        orchestrationService.processDiscoveryResults(session.getSessionId(), candidates);

        // Verify only candidates above 0.5 threshold were saved
        List<FundingSourceCandidate> savedCandidates = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW);

        assertThat(savedCandidates).hasSize(2);
        assertThat(savedCandidates)
                .extracting(FundingSourceCandidate::getOrganizationName)
                .containsExactlyInAnyOrder("High Quality", "Acceptable");

        // Verify session reflects correct count
        DiscoverySession completedSession = sessionRepository.findById(session.getSessionId()).orElse(null);
        assertThat(completedSession.getCandidatesFound()).isEqualTo(2);
    }

    private FundingSourceCandidate createCandidate(String orgName, String programName, double confidence) {
        return FundingSourceCandidate.builder()
                .candidateId(UUID.randomUUID())
                .organizationName(orgName)
                .programName(programName)
                .description("Funding program for startups and innovation")
                .sourceUrl("https://example.com/" + orgName.toLowerCase().replace(" ", "-"))
                .confidenceScore(new BigDecimal(String.valueOf(confidence)))
                .fundingAmountMin(BigDecimal.valueOf(10000))
                .fundingAmountMax(BigDecimal.valueOf(100000))
                .currency("EUR")
                .geographicEligibility(List.of("Bulgaria", "Eastern Europe"))
                .organizationTypes(List.of("Startup", "SME"))
                .tags(Set.of("innovation", "technology"))
                .status(CandidateStatus.PENDING_REVIEW)
                .discoveredAt(LocalDateTime.now())
                .build();
    }
}
