package com.northstar.funding.discovery.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.northstar.funding.discovery.config.TestDataFactory;
import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;

/**
 * Integration Tests for FundingSourceCandidateRepository
 * 
 * Tests PostgreSQL-specific functionality using TestContainers including:
 * - VARCHAR enum mapping with CHECK constraints (CandidateStatus)
 * - PostgreSQL array operations (TEXT[] for geographic_eligibility, organization_types, requirements, tags)
 * - Complex queries with status filtering
 * - Duplicate detection logic
 * - Review queue management
 * - Advanced search capabilities
 * - Spring Data JDBC enum compatibility
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class FundingSourceCandidateRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("northstar_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private FundingSourceCandidateRepository repository;
    
    @Autowired
    private DiscoverySessionRepository sessionRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private TestDataFactory testDataFactory;
    
    private UUID reviewerId1;
    private UUID reviewerId2;
    private UUID reviewerId3;
    
    private FundingSourceCandidate pendingCandidate;
    private FundingSourceCandidate inReviewCandidate;
    private FundingSourceCandidate approvedCandidate;
    private FundingSourceCandidate rejectedCandidate;
    
    @BeforeEach
    void setUp() {
        // @Transactional handles cleanup - no manual cleanup needed
        
        // Create admin users for FK references
        reviewerId1 = UUID.randomUUID();
        reviewerId2 = UUID.randomUUID();
        reviewerId3 = UUID.randomUUID();
        
        jdbcTemplate.execute(String.format(
            "INSERT INTO admin_user (user_id, username, full_name, email, role, is_active, created_at, " +
            "candidates_reviewed, average_review_time_minutes, approval_rate, current_workload, max_concurrent_assignments) " +
            "VALUES ('%s', 'reviewer1', 'Test Reviewer 1', 'reviewer1@test.com', 'REVIEWER', true, NOW(), 0, 0, 0.0, 0, 10)",
            reviewerId1
        ));
        
        jdbcTemplate.execute(String.format(
            "INSERT INTO admin_user (user_id, username, full_name, email, role, is_active, created_at, " +
            "candidates_reviewed, average_review_time_minutes, approval_rate, current_workload, max_concurrent_assignments) " +
            "VALUES ('%s', 'reviewer2', 'Test Reviewer 2', 'reviewer2@test.com', 'REVIEWER', true, NOW(), 0, 0, 0.0, 0, 10)",
            reviewerId2
        ));
        
        jdbcTemplate.execute(String.format(
            "INSERT INTO admin_user (user_id, username, full_name, email, role, is_active, created_at, " +
            "candidates_reviewed, average_review_time_minutes, approval_rate, current_workload, max_concurrent_assignments) " +
            "VALUES ('%s', 'reviewer3', 'Test Reviewer 3', 'reviewer3@test.com', 'REVIEWER', true, NOW(), 0, 0, 0.0, 0, 10)",
            reviewerId3
        ));
        
        // Create test candidates with different statuses
        pendingCandidate = FundingSourceCandidate.builder()
            .status(CandidateStatus.PENDING_REVIEW)
            .confidenceScore(new BigDecimal("0.85"))
            .discoveredAt(LocalDateTime.now().minusDays(2))
            .lastUpdatedAt(LocalDateTime.now().minusDays(2))
            .organizationName("European Innovation Council")
            .programName("EIC Accelerator 2024")
            .sourceUrl("https://eic.ec.europa.eu/accelerator")
            .description("Grant for breakthrough innovations with high-risk, high-reward potential")
            .fundingAmountMin(new BigDecimal("500000"))
            .fundingAmountMax(new BigDecimal("2500000"))
            .currency("EUR")
            .applicationDeadline(LocalDate.now().plusMonths(3))
            .applicationProcess("Two-stage application: short proposal then full proposal")
            .geographicEligibility(List.of("EU", "H2020 Associated Countries"))
            .organizationTypes(List.of("SME", "Startup", "Scale-up"))
            .requirements(List.of("Innovative technology", "Market potential", "Team capability"))
            .tags(Set.of("innovation", "technology", "EU", "high-risk"))
            .discoveryMethod("web_search")
            .searchQuery("EU technology funding innovation")
            .extractedData("{}")
            .build();
            
        inReviewCandidate = FundingSourceCandidate.builder()
            .status(CandidateStatus.IN_REVIEW)
            .confidenceScore(new BigDecimal("0.75"))
            .discoveredAt(LocalDateTime.now().minusDays(5))
            .lastUpdatedAt(LocalDateTime.now().minusDays(1))
            .assignedReviewerId(reviewerId1)
            .reviewStartedAt(LocalDateTime.now().minusDays(1))
            .organizationName("Horizon Europe")
            .programName("MSCA Doctoral Networks 2024")
            .sourceUrl("https://marie-sklodowska-curie-actions.ec.europa.eu/")
            .description("Research and innovation staff exchange program")
            .fundingAmountMin(new BigDecimal("1000000"))
            .fundingAmountMax(new BigDecimal("4000000"))
            .currency("EUR")
            .applicationDeadline(LocalDate.now().plusMonths(6))
            .applicationProcess("Online application via Funding & Tenders Portal")
            .geographicEligibility(List.of("EU", "Associated Countries", "Third Countries"))
            .organizationTypes(List.of("University", "Research Institute", "Private Sector"))
            .requirements(List.of("Research excellence", "Collaborative network", "Training program"))
            .tags(Set.of("research", "academia", "EU", "doctoral"))
            .discoveryMethod("llm_extraction")
            .searchQuery("Horizon Europe research funding")
            .validationNotes("Under review - checking eligibility criteria")
            .extractedData("{}")
            .build();
            
        approvedCandidate = FundingSourceCandidate.builder()
            .status(CandidateStatus.APPROVED)
            .confidenceScore(new BigDecimal("0.92"))
            .discoveredAt(LocalDateTime.now().minusDays(10))
            .lastUpdatedAt(LocalDateTime.now().minusDays(3))
            .assignedReviewerId(reviewerId2)
            .reviewStartedAt(LocalDateTime.now().minusDays(8))
            .organizationName("German Research Foundation")
            .programName("DFG Individual Research Grants")
            .sourceUrl("https://www.dfg.de/en/")
            .description("Funding for individual research projects in all scientific disciplines")
            .fundingAmountMin(new BigDecimal("50000"))
            .fundingAmountMax(new BigDecimal("500000"))
            .currency("EUR")
            .applicationProcess("Continuous submission via elan portal")
            .geographicEligibility(List.of("Germany", "International researchers at German institutions"))
            .organizationTypes(List.of("University", "Research Institute"))
            .requirements(List.of("Scientific merit", "Feasibility", "Institutional support"))
            .tags(Set.of("research", "Germany", "all-disciplines", "individual"))
            .discoveryMethod("web_scraping")
            .searchQuery("German research funding opportunities")
            .validationNotes("Verified and approved - excellent funding source")
            .extractedData("{}")
            .build();
            
        rejectedCandidate = FundingSourceCandidate.builder()
            .status(CandidateStatus.REJECTED)
            .confidenceScore(new BigDecimal("0.45"))
            .discoveredAt(LocalDateTime.now().minusDays(15))
            .lastUpdatedAt(LocalDateTime.now().minusDays(12))
            .assignedReviewerId(reviewerId3)
            .reviewStartedAt(LocalDateTime.now().minusDays(14))
            .organizationName("Local Community Fund")
            .programName("Small Grants Program")
            .sourceUrl("https://local-community-fund.example")
            .description("Small grants for local community projects")
            .fundingAmountMin(new BigDecimal("1000"))
            .fundingAmountMax(new BigDecimal("5000"))
            .currency("EUR")
            .geographicEligibility(List.of("Local region only"))
            .organizationTypes(List.of("Community group", "Non-profit"))
            .requirements(List.of("Local impact"))
            .tags(Set.of("local", "small-scale", "community"))
            .discoveryMethod("web_search")
            .searchQuery("community funding")
            .rejectionReason("Too small scale and local - not relevant for target audience")
            .extractedData("{}")
            .build();
            
        repository.saveAll(List.of(pendingCandidate, inReviewCandidate, approvedCandidate, rejectedCandidate));
    }
    
    @Test
    @DisplayName("Should save and retrieve funding source candidate with all fields")
    void shouldSaveAndRetrieveFundingSourceCandidateWithAllFields() {
        // When: Finding the pending candidate
        var found = repository.findById(pendingCandidate.getCandidateId());
        
        // Then: All fields should be preserved
        assertThat(found).isPresent();
        var candidate = found.get();
        
        assertAll("Funding source candidate fields",
            () -> assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING_REVIEW),
            () -> assertThat(candidate.getConfidenceScore()).isEqualByComparingTo(new BigDecimal("0.85")),
            () -> assertThat(candidate.getOrganizationName()).isEqualTo("European Innovation Council"),
            () -> assertThat(candidate.getProgramName()).isEqualTo("EIC Accelerator 2024"),
            () -> assertThat(candidate.getSourceUrl()).isEqualTo("https://eic.ec.europa.eu/accelerator"),
            () -> assertThat(candidate.getFundingAmountMin()).isEqualByComparingTo(new BigDecimal("500000")),
            () -> assertThat(candidate.getFundingAmountMax()).isEqualByComparingTo(new BigDecimal("2500000")),
            () -> assertThat(candidate.getCurrency()).isEqualTo("EUR"),
            () -> assertThat(candidate.getApplicationDeadline()).isNotNull(),
            () -> assertThat(candidate.getGeographicEligibility()).containsExactlyInAnyOrder("EU", "H2020 Associated Countries"),
            () -> assertThat(candidate.getOrganizationTypes()).containsExactlyInAnyOrder("SME", "Startup", "Scale-up"),
            () -> assertThat(candidate.getRequirements()).hasSize(3),
            () -> assertThat(candidate.getTags()).containsExactlyInAnyOrder("innovation", "technology", "EU", "high-risk"),
            () -> assertThat(candidate.getDiscoveryMethod()).isEqualTo("web_search")
        );
    }
    
    @Test
    @DisplayName("Should handle enum values as VARCHAR with CHECK constraints")
    void shouldHandleEnumValuesAsVarchar() {
        // When: Creating candidates with all possible enum values
        var testPending = FundingSourceCandidate.builder()
            .status(CandidateStatus.PENDING_REVIEW)
            .confidenceScore(new BigDecimal("0.8"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .organizationName("Test Org")
            .programName("Test Program")
            .sourceUrl("http://test.org")
            .extractedData("{}")
            .build();
            
        var testInReview = FundingSourceCandidate.builder()
            .status(CandidateStatus.IN_REVIEW)
            .confidenceScore(new BigDecimal("0.7"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .assignedReviewerId(reviewerId1)
            .reviewStartedAt(LocalDateTime.now())
            .organizationName("Test Org 2")
            .programName("Test Program 2")
            .sourceUrl("http://test2.org")
            .extractedData("{}")
            .build();
            
        var testApproved = FundingSourceCandidate.builder()
            .status(CandidateStatus.APPROVED)
            .confidenceScore(new BigDecimal("0.9"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .assignedReviewerId(reviewerId2)
            .reviewStartedAt(LocalDateTime.now())
            .organizationName("Test Org 3")
            .programName("Test Program 3")
            .sourceUrl("http://test3.org")
            .extractedData("{}")
            .build();
            
        var testRejected = FundingSourceCandidate.builder()
            .status(CandidateStatus.REJECTED)
            .confidenceScore(new BigDecimal("0.5"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .assignedReviewerId(reviewerId3)
            .reviewStartedAt(LocalDateTime.now())
            .organizationName("Test Org 4")
            .programName("Test Program 4")
            .sourceUrl("http://test4.org")
            .rejectionReason("Test rejection")
            .extractedData("{}")
            .build();
            
        var saved = repository.saveAll(List.of(testPending, testInReview, testApproved, testRejected));
        
        // Then: All enum values should be preserved correctly
        assertThat(saved).hasSize(4);
        assertThat(saved).extracting(FundingSourceCandidate::getStatus)
            .containsExactlyInAnyOrder(
                CandidateStatus.PENDING_REVIEW,
                CandidateStatus.IN_REVIEW,
                CandidateStatus.APPROVED,
                CandidateStatus.REJECTED
            );
    }
    
    @Test
    @DisplayName("Should find candidates by status ordered by confidence score")
    void shouldFindCandidatesByStatusOrderedByConfidenceScore() {
        // When: Finding pending review candidates
        var pendingCandidates = repository.findByStatusOrderByConfidenceScoreDesc(
            CandidateStatus.PENDING_REVIEW,
            PageRequest.of(0, 10)
        );
        
        // Then: Should return pending candidates ordered by confidence score descending
        assertThat(pendingCandidates).isNotEmpty();
        assertThat(pendingCandidates.getContent())
            .allMatch(c -> c.getStatus() == CandidateStatus.PENDING_REVIEW)
            .isSortedAccordingTo((c1, c2) -> c2.getConfidenceScore().compareTo(c1.getConfidenceScore()));
    }
    
    @Test
    @DisplayName("Should find candidates by assigned reviewer")
    void shouldFindCandidatesByAssignedReviewer() {
        // Given: A reviewer ID
        var reviewerId = inReviewCandidate.getAssignedReviewerId();
        
        // When: Finding candidates assigned to reviewer
        var assignedCandidates = repository.findByAssignedReviewerId(reviewerId, PageRequest.of(0, 10));
        
        // Then: Should return candidates assigned to that reviewer
        assertThat(assignedCandidates).isNotEmpty();
        assertThat(assignedCandidates.getContent())
            .allMatch(c -> c.getAssignedReviewerId().equals(reviewerId));
    }
    
    @Test
    @DisplayName("Should find unassigned candidates for assignment")
    void shouldFindUnassignedCandidatesForAssignment() {
        // When: Finding unassigned pending candidates
        var unassignedCandidates = repository.findUnassignedCandidatesForAssignment(PageRequest.of(0, 10));
        
        // Then: Should return pending candidates without reviewer assignment
        assertThat(unassignedCandidates).isNotEmpty();
        assertThat(unassignedCandidates)
            .allMatch(c -> c.getStatus() == CandidateStatus.PENDING_REVIEW)
            .allMatch(c -> c.getAssignedReviewerId() == null);
    }
    
    @Test
    @DisplayName("Should find potential duplicates by organization and program name")
    void shouldFindPotentialDuplicates() {
        // Given: Create a potential duplicate
        var duplicateCandidate = FundingSourceCandidate.builder()
            .status(CandidateStatus.PENDING_REVIEW)
            .confidenceScore(new BigDecimal("0.88"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .organizationName("european innovation council") // Different case
            .programName("EIC ACCELERATOR 2024") // Different case
            .sourceUrl("https://different-url.example")
            .extractedData("{}")
            .build();
        repository.save(duplicateCandidate);
        
        // When: Finding potential duplicates for pending candidate
        var duplicates = repository.findPotentialDuplicates(
            pendingCandidate.getOrganizationName(),
            pendingCandidate.getProgramName(),
            pendingCandidate.getCandidateId()
        );
        
        // Then: Should find the duplicate (case-insensitive match)
        assertThat(duplicates).isNotEmpty();
        assertThat(duplicates)
            .anyMatch(c -> c.getCandidateId().equals(duplicateCandidate.getCandidateId()));
    }
    
    @Test
    @DisplayName("Should find stale candidates pending too long")
    void shouldFindStaleCandidates() {
        // When: Finding candidates older than 4 days (will include 5-day old inReviewCandidate)
        var threshold = LocalDateTime.now().minusDays(4);
        var staleCandidates = repository.findStaleCandidates(threshold);
        
        // Then: Should return candidates discovered before threshold
        assertThat(staleCandidates).isNotEmpty();
        assertThat(staleCandidates)
            .allMatch(c -> c.getDiscoveredAt().isBefore(threshold))
            .allMatch(c -> c.getStatus() == CandidateStatus.PENDING_REVIEW || 
                          c.getStatus() == CandidateStatus.IN_REVIEW);
    }
    
    @Test
    @DisplayName("Should find candidates by discovery session")
    void shouldFindCandidatesByDiscoverySession() {
        // Given: Create a discovery session first to satisfy FK constraint
        var discoverySession = testDataFactory.discoverySessionBuilder()
            .sessionType(com.northstar.funding.discovery.domain.SessionType.MANUAL)
            .status(com.northstar.funding.discovery.domain.SessionStatus.COMPLETED)
            .completedAt(LocalDateTime.now())
            .build();
        var savedSession = sessionRepository.save(discoverySession);
        
        // And: Candidate with that discovery session
        var sessionCandidate = FundingSourceCandidate.builder()
            .status(CandidateStatus.PENDING_REVIEW)
            .confidenceScore(new BigDecimal("0.8"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .discoverySessionId(savedSession.getSessionId())
            .organizationName("Test Organization")
            .programName("Test Program")
            .sourceUrl("http://test.org")
            .extractedData("{}")
            .build();
        repository.save(sessionCandidate);
        
        // When: Finding candidates by session
        var sessionCandidates = repository.findByDiscoverySessionId(savedSession.getSessionId());
        
        // Then: Should return candidates from that session
        assertThat(sessionCandidates).hasSize(1);
        assertThat(sessionCandidates.get(0).getDiscoverySessionId()).isEqualTo(savedSession.getSessionId());
    }
    
    @Test
    @DisplayName("Should count candidates by status")
    void shouldCountCandidatesByStatus() {
        // When: Counting candidates by each status
        var pendingCount = repository.countByStatus(CandidateStatus.PENDING_REVIEW);
        var inReviewCount = repository.countByStatus(CandidateStatus.IN_REVIEW);
        var approvedCount = repository.countByStatus(CandidateStatus.APPROVED);
        var rejectedCount = repository.countByStatus(CandidateStatus.REJECTED);
        
        // Then: Should return accurate counts
        assertAll("Status counts",
            () -> assertThat(pendingCount).isGreaterThanOrEqualTo(1L),
            () -> assertThat(inReviewCount).isGreaterThanOrEqualTo(1L),
            () -> assertThat(approvedCount).isGreaterThanOrEqualTo(1L),
            () -> assertThat(rejectedCount).isGreaterThanOrEqualTo(1L)
        );
    }
    
    @Test
    @DisplayName("Should get average confidence score")
    void shouldGetAverageConfidenceScore() {
        // When: Getting average confidence score for last 30 days
        var avgScore = repository.getAverageConfidenceScore(LocalDateTime.now().minusDays(30));
        
        // Then: Should return average excluding rejected candidates
        assertThat(avgScore).isNotNull();
        assertThat(avgScore).isGreaterThan(0.0);
        assertThat(avgScore).isLessThanOrEqualTo(1.0);
    }
    
    @Test
    @DisplayName("Should find high quality approved candidates")
    void shouldFindHighQualityApprovedCandidates() {
        // When: Finding top quality approved candidates
        var highQualityCandidates = repository.findHighQualityApprovedCandidates(PageRequest.of(0, 10));
        
        // Then: Should return approved candidates with confidence >= 0.8
        assertThat(highQualityCandidates).isNotEmpty();
        assertThat(highQualityCandidates)
            .allMatch(c -> c.getStatus() == CandidateStatus.APPROVED)
            .allMatch(c -> c.getConfidenceScore().compareTo(new BigDecimal("0.8")) >= 0)
            .isSortedAccordingTo((c1, c2) -> c2.getConfidenceScore().compareTo(c1.getConfidenceScore()));
    }
    
    @Test
    @DisplayName("Should find candidates by tag and status")
    void shouldFindCandidatesByTagAndStatus() {
        // When: Finding EU-tagged pending candidates
        var euCandidates = repository.findByTagAndStatus(
            "EU",
            CandidateStatus.PENDING_REVIEW,
            PageRequest.of(0, 10)
        );
        
        // Then: Should return pending candidates with EU tag
        assertThat(euCandidates).isNotEmpty();
        assertThat(euCandidates)
            .allMatch(c -> c.getStatus() == CandidateStatus.PENDING_REVIEW);
    }
    
    @Test
    @DisplayName("Should find candidates by geographic eligibility")
    void shouldFindCandidatesByGeographicEligibility() {
        // When: Finding approved candidates with Germany eligibility
        var germanyEligibleCandidates = repository.findByGeographicEligibility(
            "Germany",
            PageRequest.of(0, 10)
        );
        
        // Then: Should return approved candidates eligible for Germany
        assertThat(germanyEligibleCandidates).isNotEmpty();
        assertThat(germanyEligibleCandidates)
            .allMatch(c -> c.getStatus() == CandidateStatus.APPROVED);
    }
    
    @Test
    @DisplayName("Should find duplicates by organization and program name (simplified)")
    void shouldFindDuplicatesByOrganizationNameAndProgramName() {
        // Given: Multiple candidates with same org/program
        var duplicate1 = FundingSourceCandidate.builder()
            .status(CandidateStatus.PENDING_REVIEW)
            .confidenceScore(new BigDecimal("0.8"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .organizationName("Duplicate Test Org")
            .programName("Duplicate Program")
            .sourceUrl("http://dup1.org")
            .extractedData("{}")
            .build();
            
        var duplicate2 = FundingSourceCandidate.builder()
            .status(CandidateStatus.APPROVED)
            .confidenceScore(new BigDecimal("0.85"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .assignedReviewerId(reviewerId1)
            .reviewStartedAt(LocalDateTime.now())
            .organizationName("duplicate test org") // Different case
            .programName("DUPLICATE PROGRAM") // Different case
            .sourceUrl("http://dup2.org")
            .extractedData("{}")
            .build();
            
        repository.saveAll(List.of(duplicate1, duplicate2));
        
        // When: Finding duplicates
        var duplicates = repository.findDuplicatesByOrganizationNameAndProgramName(
            "Duplicate Test Org",
            "Duplicate Program"
        );
        
        // Then: Should find both (case-insensitive), excluding rejected
        assertThat(duplicates).hasSizeGreaterThanOrEqualTo(2);
    }
    
    @Test
    @DisplayName("Should find candidates discovered before threshold")
    void shouldFindCandidatesDiscoveredBeforeThreshold() {
        // When: Finding candidates discovered more than 3 days ago
        var threshold = LocalDateTime.now().minusDays(3);
        var oldCandidates = repository.findByDiscoveredAtBefore(threshold);
        
        // Then: Should return candidates discovered before threshold
        assertThat(oldCandidates).isNotEmpty();
        assertThat(oldCandidates)
            .allMatch(c -> c.getDiscoveredAt().isBefore(threshold));
    }
    
    @Test
    @DisplayName("Should find candidates with confidence score greater than threshold")
    void shouldFindCandidatesWithHighConfidenceScore() {
        // When: Finding candidates with confidence > 0.8
        var highConfidenceCandidates = repository.findByConfidenceScoreGreaterThanOrderByConfidenceScoreDesc(new BigDecimal("0.8"));

        // Then: Should return candidates with high confidence scores
        assertThat(highConfidenceCandidates).isNotEmpty();
        assertThat(highConfidenceCandidates)
            .allMatch(c -> c.getConfidenceScore().compareTo(new BigDecimal("0.8")) > 0)
            .isSortedAccordingTo((c1, c2) -> c2.getConfidenceScore().compareTo(c1.getConfidenceScore()));
    }
    
    @Test
    @DisplayName("Should handle PostgreSQL array fields correctly")
    void shouldHandlePostgreSQLArrayFields() {
        // Given: Candidate with array fields
        var candidate = repository.findById(pendingCandidate.getCandidateId()).orElseThrow();
        
        // Then: Array fields should be properly mapped
        assertAll("Array field mapping",
            () -> assertThat(candidate.getGeographicEligibility()).isNotNull().isNotEmpty(),
            () -> assertThat(candidate.getOrganizationTypes()).isNotNull().isNotEmpty(),
            () -> assertThat(candidate.getRequirements()).isNotNull().isNotEmpty(),
            () -> assertThat(candidate.getTags()).isNotNull().isNotEmpty()
        );
    }
    
    @Test
    @DisplayName("Should check if candidate exists by ID")
    void shouldCheckIfCandidateExistsById() {
        // When: Checking existence
        var exists = repository.existsById(pendingCandidate.getCandidateId());
        var notExists = repository.existsById(UUID.randomUUID());
        
        // Then: Should return correct existence status
        assertAll("Existence checks",
            () -> assertThat(exists).isTrue(),
            () -> assertThat(notExists).isFalse()
        );
    }
    
    @Test
    @DisplayName("Should count total candidates")
    void shouldCountTotalCandidates() {
        // When: Counting total candidates
        var count = repository.count();
        
        // Then: Should return correct count
        assertThat(count).isGreaterThanOrEqualTo(4L);
    }
    
    @Test
    @DisplayName("Should find all candidates")
    void shouldFindAllCandidates() {
        // When: Finding all candidates
        var allCandidates = repository.findAll();
        
        // Then: Should return all test candidates
        assertThat(allCandidates).hasSizeGreaterThanOrEqualTo(4);
    }
    
    @Test
    @DisplayName("Should find candidates by multiple IDs")
    void shouldFindCandidatesByMultipleIds() {
        // When: Finding by multiple IDs
        var ids = List.of(pendingCandidate.getCandidateId(), approvedCandidate.getCandidateId());
        var candidates = repository.findAllById(ids);
        
        // Then: Should return specified candidates
        assertThat(candidates).hasSize(2);
        assertThat(candidates)
            .extracting(FundingSourceCandidate::getCandidateId)
            .containsExactlyInAnyOrder(ids.toArray(new UUID[0]));
    }
    
    @Test
    @DisplayName("Should delete candidate by ID")
    void shouldDeleteCandidateById() {
        // Given: A candidate to delete
        UUID candidateId = rejectedCandidate.getCandidateId();
        
        // When: Deleting the candidate
        repository.deleteById(candidateId);
        
        // Then: Candidate should no longer exist
        assertThat(repository.existsById(candidateId)).isFalse();
    }
    
    @Test
    @DisplayName("Should delete multiple candidates")
    void shouldDeleteMultipleCandidates() {
        // Given: Candidates to delete
        var toDelete = List.of(pendingCandidate, rejectedCandidate);
        
        // When: Deleting multiple candidates
        repository.deleteAll(toDelete);
        
        // Then: Candidates should no longer exist
        assertAll("Delete verification",
            () -> assertThat(repository.existsById(pendingCandidate.getCandidateId())).isFalse(),
            () -> assertThat(repository.existsById(rejectedCandidate.getCandidateId())).isFalse()
        );
    }
    
    @Test
    @DisplayName("Should handle null optional fields gracefully")
    void shouldHandleNullOptionalFieldsGracefully() {
        // Given: Candidate with minimal required fields
        var minimalCandidate = FundingSourceCandidate.builder()
            .status(CandidateStatus.PENDING_REVIEW)
            .confidenceScore(new BigDecimal("0.7"))
            .discoveredAt(LocalDateTime.now())
            .lastUpdatedAt(LocalDateTime.now())
            .organizationName("Minimal Org")
            .programName("Minimal Program")
            .sourceUrl("http://minimal.org")
            .extractedData("{}")
            .build();
            
        // When: Saving and retrieving
        var saved = repository.save(minimalCandidate);
        var retrieved = repository.findById(saved.getCandidateId()).orElseThrow();
        
        // Then: Optional fields should be null
        assertAll("Null optional fields",
            () -> assertThat(retrieved.getAssignedReviewerId()).isNull(),
            () -> assertThat(retrieved.getReviewStartedAt()).isNull(),
            () -> assertThat(retrieved.getFundingAmountMin()).isNull(),
            () -> assertThat(retrieved.getFundingAmountMax()).isNull(),
            () -> assertThat(retrieved.getApplicationDeadline()).isNull(),
            () -> assertThat(retrieved.getValidationNotes()).isNull(),
            () -> assertThat(retrieved.getRejectionReason()).isNull()
        );
    }
}
