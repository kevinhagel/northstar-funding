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
import com.northstar.funding.discovery.domain.EnhancementRecord;
import com.northstar.funding.discovery.domain.EnhancementType;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.infrastructure.DiscoverySessionRepository;
import com.northstar.funding.discovery.infrastructure.EnhancementRecordRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test: Audit Trail and Enhancement Tracking
 *
 * Tests the comprehensive audit trail system:
 * 1. Track discovery session metadata
 * 2. Link candidates to discovery sessions
 * 3. Track approval/rejection workflow
 * 4. Track enhancement history
 * 5. Query audit data for compliance and analysis
 *
 * Constitutional requirement: Complete audit trail for AI decisions
 * Uses Testcontainers for PostgreSQL and full Spring Boot context
 */
@SpringBootTest
@Testcontainers
@Transactional
class AuditTrailIntegrationTest {

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

    @Autowired
    private EnhancementRecordRepository enhancementRepository;

    @BeforeEach
    void setUp() {
        enhancementRepository.deleteAll();
        candidateRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    @Test
    void shouldMaintainCompleteAuditTrailForCandidateLifecycle() {
        // Step 1: Discovery session
        DiscoverySession session = orchestrationService.triggerDiscovery(
                List.of("searxng"),
                List.of("Bulgarian tech funding")
        );

        assertThat(session.getExecutedAt()).isNotNull();
        assertThat(session.getSearchEnginesUsed()).contains("searxng");

        // Step 2: Process candidates
        List<FundingSourceCandidate> candidates = List.of(
                createCandidate("Tech Fund", "Innovation Program", 0.85)
        );

        orchestrationService.processDiscoveryResults(session.getSessionId(), candidates);

        // Verify session audit trail
        DiscoverySession completedSession = sessionRepository.findById(session.getSessionId()).orElseThrow();
        assertThat(completedSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(completedSession.getCandidatesFound()).isEqualTo(1);
        assertThat(completedSession.getAverageConfidenceScore()).isEqualTo(0.85);

        // Step 3: Candidate discovered - verify linkage
        FundingSourceCandidate candidate = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW)
                .get(0);

        assertThat(candidate.getDiscoverySessionId()).isEqualTo(session.getSessionId());
        assertThat(candidate.getDiscoveredAt()).isNotNull();

        // Step 4: AI enhancement
        EnhancementRecord aiEnhancement = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.AI_SUGGESTED)
                .fieldName("description")
                .originalValue("Basic description")
                .suggestedValue("Enhanced description with AI insights")
                .enhancedAt(LocalDateTime.now())
                .aiModel("llama-3.1-8b")
                .confidenceScore(new BigDecimal("0.88"))
                .humanApproved(true)
                .build();
        enhancementRepository.save(aiEnhancement);

        // Step 5: Human approval
        UUID adminUser = UUID.randomUUID();
        FundingSourceCandidate approved = validationService.approveCandidate(
                candidate.getCandidateId(),
                adminUser,
                "Excellent opportunity"
        );

        // Verify complete audit trail
        assertThat(approved.getStatus()).isEqualTo(CandidateStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo(adminUser);
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.getDiscoveredAt()).isNotNull();
        assertThat(approved.getDiscoverySessionId()).isEqualTo(session.getSessionId());

        // Query audit trail
        List<EnhancementRecord> enhancements = enhancementRepository
                .findByCandidateId(candidate.getCandidateId());
        assertThat(enhancements).hasSize(1);
        assertThat(enhancements.get(0).isHumanApproved()).isTrue();
    }

    @Test
    void shouldTrackDiscoverySessionPerformanceMetrics() {
        // Create multiple sessions with different characteristics

        // Session 1: Successful with high confidence
        DiscoverySession session1 = orchestrationService.triggerDiscovery(
                List.of("searxng", "tavily"),
                List.of("query1")
        );

        List<FundingSourceCandidate> highQuality = List.of(
                createCandidate("Org 1", "Prog 1", 0.95),
                createCandidate("Org 2", "Prog 2", 0.90)
        );
        orchestrationService.processDiscoveryResults(session1.getSessionId(), highQuality);

        // Session 2: Mixed quality
        DiscoverySession session2 = orchestrationService.triggerDiscovery(
                List.of("perplexity"),
                List.of("query2")
        );

        List<FundingSourceCandidate> mixedQuality = List.of(
                createCandidate("Org 3", "Prog 3", 0.75),
                createCandidate("Org 4", "Prog 4", 0.60),
                createCandidate("Org 5", "Prog 5", 0.40)  // Below threshold
        );
        orchestrationService.processDiscoveryResults(session2.getSessionId(), mixedQuality);

        // Query session metrics
        List<DiscoverySession> recentSessions = orchestrationService.getRecentSessions();
        assertThat(recentSessions).hasSize(2);

        DiscoverySession completed1 = recentSessions.stream()
                .filter(s -> s.getSessionId().equals(session1.getSessionId()))
                .findFirst()
                .orElseThrow();

        assertThat(completed1.getCandidatesFound()).isEqualTo(2);
        assertThat(completed1.getAverageConfidenceScore())
                .isGreaterThanOrEqualTo(new BigDecimal("0.90"))
                .isLessThanOrEqualTo(new BigDecimal("0.95"));

        DiscoverySession completed2 = recentSessions.stream()
                .filter(s -> s.getSessionId().equals(session2.getSessionId()))
                .findFirst()
                .orElseThrow();

        // Only 2 candidates saved (above 0.5 threshold)
        assertThat(completed2.getCandidatesFound()).isEqualTo(2);
    }

    @Test
    void shouldTrackMultipleReviewersAndTheirActions() {
        // Create candidates
        FundingSourceCandidate candidate1 = createCandidate("Fund A", "Program 1", 0.80);
        candidate1.setStatus(CandidateStatus.PENDING_REVIEW);
        candidate1 = candidateRepository.save(candidate1);

        FundingSourceCandidate candidate2 = createCandidate("Fund B", "Program 2", 0.75);
        candidate2.setStatus(CandidateStatus.PENDING_REVIEW);
        candidate2 = candidateRepository.save(candidate2);

        // Different reviewers take actions
        UUID reviewer1 = UUID.randomUUID();
        UUID reviewer2 = UUID.randomUUID();

        // Reviewer 1 approves candidate 1
        FundingSourceCandidate approved = validationService.approveCandidate(
                candidate1.getCandidateId(),
                reviewer1,
                "Reviewer 1 approval"
        );

        assertThat(approved.getApprovedBy()).isEqualTo(reviewer1);

        // Reviewer 2 rejects candidate 2
        FundingSourceCandidate rejected = validationService.rejectCandidate(
                candidate2.getCandidateId(),
                reviewer2,
                "Reviewer 2 rejection"
        );

        assertThat(rejected.getRejectedBy()).isEqualTo(reviewer2);

        // Verify audit trail shows different reviewers
        FundingSourceCandidate persistedApproved = candidateRepository
                .findById(candidate1.getCandidateId())
                .orElseThrow();
        FundingSourceCandidate persistedRejected = candidateRepository
                .findById(candidate2.getCandidateId())
                .orElseThrow();

        assertThat(persistedApproved.getApprovedBy()).isEqualTo(reviewer1);
        assertThat(persistedRejected.getRejectedBy()).isEqualTo(reviewer2);
        assertThat(persistedApproved.getApprovedAt()).isNotNull();
        assertThat(persistedRejected.getRejectedAt()).isNotNull();
    }

    @Test
    void shouldQueryAuditDataForComplianceReporting() {
        // Create historical data
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);

        // Create 5 sessions over past month
        for (int i = 0; i < 5; i++) {
            DiscoverySession session = orchestrationService.triggerDiscovery(
                    List.of("searxng"),
                    List.of("query " + i)
            );

            List<FundingSourceCandidate> candidates = List.of(
                    createCandidate("Org " + i, "Program " + i, 0.80)
            );

            orchestrationService.processDiscoveryResults(session.getSessionId(), candidates);
        }

        // Query average metrics
        Double avgMetrics = orchestrationService.getAverageDiscoveryMetrics(startTime);
        assertThat(avgMetrics).isNotNull();
        assertThat(avgMetrics).isGreaterThan(0);

        // Query recent sessions
        List<DiscoverySession> recentSessions = orchestrationService.getRecentSessions();
        assertThat(recentSessions).hasSize(5);
        assertThat(recentSessions).allMatch(s -> s.getStatus() == SessionStatus.COMPLETED);
    }

    @Test
    void shouldTrackEnhancementHistoryForAudit() {
        // Create candidate
        FundingSourceCandidate candidate = createCandidate("Test Org", "Test Program", 0.75);
        candidate = candidateRepository.save(candidate);

        UUID humanUser = UUID.randomUUID();

        // Track series of enhancements
        for (int i = 0; i < 3; i++) {
            EnhancementRecord record = EnhancementRecord.builder()
                    .candidateId(candidate.getCandidateId())
                    .enhancementType(i % 2 == 0 ? EnhancementType.AI_SUGGESTED : EnhancementType.MANUAL)
                    .fieldName("field" + i)
                    .originalValue("original" + i)
                    .suggestedValue("enhanced" + i)
                    .enhancedBy(i % 2 == 0 ? null : humanUser)
                    .enhancedAt(LocalDateTime.now().plusMinutes(i))
                    .aiModel(i % 2 == 0 ? "llama-3.1-8b" : null)
                    .confidenceScore(i % 2 == 0 ? new BigDecimal("0.85") : null)
                    .humanApproved(true)
                    .approvedBy(humanUser)
                    .approvedAt(LocalDateTime.now().plusMinutes(i + 1))
                    .build();
            enhancementRepository.save(record);
        }

        // Query enhancement history
        List<EnhancementRecord> history = enhancementRepository
                .findByCandidateId(candidate.getCandidateId());

        assertThat(history).hasSize(3);
        assertThat(history).allMatch(EnhancementRecord::isHumanApproved);

        // Verify AI vs Manual split
        long aiCount = history.stream()
                .filter(e -> e.getEnhancementType() == EnhancementType.AI_SUGGESTED)
                .count();
        long manualCount = history.stream()
                .filter(e -> e.getEnhancementType() == EnhancementType.MANUAL)
                .count();

        assertThat(aiCount).isEqualTo(2);
        assertThat(manualCount).isEqualTo(1);
    }

    @Test
    void shouldLinkAllAuditEntitiesForTraceability() {
        // Create complete audit chain
        DiscoverySession session = orchestrationService.triggerDiscovery(
                List.of("searxng"),
                List.of("test query")
        );

        List<FundingSourceCandidate> candidates = List.of(
                createCandidate("Test Fund", "Test Program", 0.85)
        );
        orchestrationService.processDiscoveryResults(session.getSessionId(), candidates);

        FundingSourceCandidate candidate = candidateRepository
                .findByStatus(CandidateStatus.PENDING_REVIEW)
                .get(0);

        // Add enhancement
        UUID humanUser = UUID.randomUUID();
        EnhancementRecord enhancement = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.MANUAL)
                .fieldName("test")
                .enhancedBy(humanUser)
                .enhancedAt(LocalDateTime.now())
                .humanApproved(true)
                .build();
        enhancementRepository.save(enhancement);

        // Approve
        validationService.approveCandidate(
                candidate.getCandidateId(),
                humanUser,
                "Test approval"
        );

        // Verify complete traceability
        FundingSourceCandidate finalCandidate = candidateRepository
                .findById(candidate.getCandidateId())
                .orElseThrow();

        // Can trace back to session
        assertThat(finalCandidate.getDiscoverySessionId()).isEqualTo(session.getSessionId());

        // Can find enhancements
        List<EnhancementRecord> candidateEnhancements = enhancementRepository
                .findByCandidateId(candidate.getCandidateId());
        assertThat(candidateEnhancements).hasSize(1);

        // Can verify approval chain
        assertThat(finalCandidate.getApprovedBy()).isEqualTo(humanUser);
        assertThat(finalCandidate.getApprovedAt()).isNotNull();

        // Complete audit trail exists
        DiscoverySession auditSession = sessionRepository
                .findById(session.getSessionId())
                .orElseThrow();
        assertThat(auditSession.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    private FundingSourceCandidate createCandidate(String orgName, String programName, double confidence) {
        return FundingSourceCandidate.builder()
                .organizationName(orgName)
                .programName(programName)
                .description("Test program description")
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
