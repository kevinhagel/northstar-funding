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
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test: Duplicate Detection and Rejection Workflow
 *
 * Tests the duplicate detection system:
 * 1. Create and approve an initial candidate
 * 2. Discover duplicate candidate in later session
 * 3. System marks duplicate and links to master
 * 4. Verify duplicate is not saved
 * 5. Test rejection workflow for inappropriate candidates
 *
 * Uses Testcontainers for PostgreSQL and full Spring Boot context
 */
@SpringBootTest
@Testcontainers
@Transactional
class DuplicateDetectionIntegrationTest {

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

    @BeforeEach
    void setUp() {
        candidateRepository.deleteAll();
    }

    @Test
    void shouldDetectAndPreventDuplicateCandidates() {
        // Step 1: Create and approve original candidate
        FundingSourceCandidate original = createCandidate(
                "Innovation Bulgaria Foundation",
                "Startup Growth Program",
                0.85
        );
        original.setStatus(CandidateStatus.PENDING_REVIEW);
        original = candidateRepository.save(original);

        UUID adminUser = UUID.randomUUID();
        FundingSourceCandidate approved = validationService.approveCandidate(
                original.getCandidateId(),
                adminUser,
                "Excellent funding opportunity"
        );

        assertThat(approved.getStatus()).isEqualTo(CandidateStatus.APPROVED);

        // Step 2: Discover duplicate in new session
        DiscoverySession session = orchestrationService.triggerDiscovery(
                List.of("tavily"),
                List.of("Bulgarian innovation programs")
        );

        // Create duplicate candidate (same org + program name)
        List<FundingSourceCandidate> newCandidates = List.of(
                createCandidate(
                        "Innovation Bulgaria Foundation",  // Same organization
                        "Startup Growth Program",           // Same program
                        0.90                                // Different confidence
                ),
                createCandidate(
                        "New Foundation",
                        "Different Program",
                        0.80
                )
        );

        // Step 3: Process candidates - duplicate should be filtered
        orchestrationService.processDiscoveryResults(session.getSessionId(), newCandidates);

        // Step 4: Verify duplicate was not saved
        List<FundingSourceCandidate> allPending = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW);

        // Should only have 1 pending (the new one), duplicate was filtered
        assertThat(allPending).hasSize(1);
        assertThat(allPending.get(0).getOrganizationName()).isEqualTo("New Foundation");

        // Verify approved candidate still exists
        List<FundingSourceCandidate> allApproved = candidateRepository
                .findByStatus(CandidateStatus.APPROVED);
        assertThat(allApproved).hasSize(1);
        assertThat(allApproved.get(0).getOrganizationName()).isEqualTo("Innovation Bulgaria Foundation");
    }

    @Test
    void shouldDetectDuplicatesAcrossMultipleSessions() {
        // Session 1: Discover and approve first candidate
        DiscoverySession session1 = orchestrationService.triggerDiscovery(
                List.of("searxng"),
                List.of("tech funding")
        );

        List<FundingSourceCandidate> candidates1 = List.of(
                createCandidate("Tech Fund", "Innovation Grant", 0.85)
        );
        orchestrationService.processDiscoveryResults(session1.getSessionId(), candidates1);

        FundingSourceCandidate candidate1 = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW)
                .get(0);

        validationService.approveCandidate(
                candidate1.getCandidateId(),
                UUID.randomUUID(),
                "Good program"
        );

        // Session 2: Discover same candidate again
        DiscoverySession session2 = orchestrationService.triggerDiscovery(
                List.of("tavily"),
                List.of("technology grants")
        );

        List<FundingSourceCandidate> candidates2 = List.of(
                createCandidate("Tech Fund", "Innovation Grant", 0.90),  // Duplicate
                createCandidate("Another Fund", "Another Program", 0.80)  // New
        );
        orchestrationService.processDiscoveryResults(session2.getSessionId(), candidates2);

        // Session 3: Discover same candidate yet again
        DiscoverySession session3 = orchestrationService.triggerDiscovery(
                List.of("perplexity"),
                List.of("startup support")
        );

        List<FundingSourceCandidate> candidates3 = List.of(
                createCandidate("Tech Fund", "Innovation Grant", 0.75)  // Duplicate again
        );
        orchestrationService.processDiscoveryResults(session3.getSessionId(), candidates3);

        // Verify only 2 candidates total (1 original + 1 new)
        long totalCandidates = candidateRepository.count();
        assertThat(totalCandidates).isEqualTo(2);

        // Verify approved candidate
        List<FundingSourceCandidate> approved = candidateRepository
                .findByStatus(CandidateStatus.APPROVED);
        assertThat(approved).hasSize(1);

        // Verify pending candidate
        List<FundingSourceCandidate> pending = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getOrganizationName()).isEqualTo("Another Fund");
    }

    @Test
    void shouldRejectInappropriateCandidates() {
        // Create candidate
        FundingSourceCandidate candidate = createCandidate(
                "Questionable Organization",
                "Suspicious Program",
                0.65
        );
        candidate.setStatus(CandidateStatus.PENDING_REVIEW);
        candidate = candidateRepository.save(candidate);

        // Reject with detailed notes
        UUID adminUser = UUID.randomUUID();
        String rejectionReason = "Organization website is non-functional. No verifiable contact information. Program details are too vague.";

        FundingSourceCandidate rejected = validationService.rejectCandidate(
                candidate.getCandidateId(),
                adminUser,
                rejectionReason
        );

        // Verify rejection
        assertThat(rejected.getStatus()).isEqualTo(CandidateStatus.REJECTED);
        assertThat(rejected.getRejectedBy()).isEqualTo(adminUser);
        assertThat(rejected.getRejectedAt()).isNotNull();
        assertThat(rejected.getRejectionReason()).isEqualTo(rejectionReason);

        // Verify persistence
        FundingSourceCandidate persisted = candidateRepository
                .findById(candidate.getCandidateId())
                .orElse(null);

        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(CandidateStatus.REJECTED);

        // Verify rejected candidates can be queried
        List<FundingSourceCandidate> rejectedCandidates = candidateRepository
                .findByStatus(CandidateStatus.REJECTED);
        assertThat(rejectedCandidates).hasSize(1);
    }

    @Test
    void shouldPreventApprovalOfAlreadyRejectedCandidates() {
        // Create and reject candidate
        FundingSourceCandidate candidate = createCandidate("Org", "Program", 0.70);
        candidate.setStatus(CandidateStatus.PENDING_REVIEW);
        candidate = candidateRepository.save(candidate);

        UUID adminUser = UUID.randomUUID();
        UUID candidateId = candidate.getCandidateId();
        validationService.rejectCandidate(candidateId, adminUser, "Not suitable");

        // Attempt to approve rejected candidate
        assertThatThrownBy(() ->
                validationService.approveCandidate(candidateId, adminUser, "Changed mind")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already rejected");
    }

    @Test
    void shouldTrackDuplicateStatisticsInSession() {
        // Create and approve original
        FundingSourceCandidate original = createCandidate("Fund A", "Program X", 0.80);
        original.setStatus(CandidateStatus.APPROVED);
        candidateRepository.save(original);

        // Process batch with duplicates
        DiscoverySession session = orchestrationService.triggerDiscovery(
                List.of("searxng"),
                List.of("test")
        );

        List<FundingSourceCandidate> candidates = List.of(
                createCandidate("Fund A", "Program X", 0.85),    // Duplicate
                createCandidate("Fund B", "Program Y", 0.75),    // New
                createCandidate("Fund A", "Program X", 0.90),    // Duplicate
                createCandidate("Fund C", "Program Z", 0.70)     // New
        );

        orchestrationService.processDiscoveryResults(session.getSessionId(), candidates);

        // Verify duplicate statistics
        // Note: The actual duplicate detection logic will filter these out during processing
        // For now, we verify that only non-duplicates are saved
        List<FundingSourceCandidate> pending = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW);

        assertThat(pending).hasSize(2); // Only the 2 new candidates
        assertThat(pending)
                .extracting(FundingSourceCandidate::getOrganizationName)
                .containsExactlyInAnyOrder("Fund B", "Fund C");
    }

    private FundingSourceCandidate createCandidate(String orgName, String programName, double confidence) {
        return FundingSourceCandidate.builder()
                .organizationName(orgName)
                .programName(programName)
                .description("Test funding program")
                .sourceUrl("https://example.com/" + orgName.toLowerCase().replace(" ", "-"))
                .confidenceScore(new BigDecimal(String.valueOf(confidence)))
                .fundingAmountMin(BigDecimal.valueOf(5000))
                .fundingAmountMax(BigDecimal.valueOf(50000))
                .currency("EUR")
                .geographicEligibility(List.of("Bulgaria"))
                .organizationTypes(List.of("Startup"))
                .tags(Set.of("test"))
                .discoveredAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .extractedData("{}")
                .build();
    }
}
