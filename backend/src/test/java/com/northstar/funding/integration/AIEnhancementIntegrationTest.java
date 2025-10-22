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

import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.EnhancementRecord;
import com.northstar.funding.discovery.domain.EnhancementType;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.EnhancementRecordRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Test: AI-Assisted Enhancement Workflow
 *
 * Tests the AI enhancement system:
 * 1. Human reviews candidate
 * 2. AI suggests enhancements (simulated)
 * 3. Human accepts/modifies AI suggestions
 * 4. Track enhancement history
 * 5. Verify human-AI collaboration
 *
 * Constitutional requirement: AI assists, humans decide
 * Uses Testcontainers for PostgreSQL and full Spring Boot context
 */
@SpringBootTest
@Testcontainers
@Transactional
class AIEnhancementIntegrationTest {

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
    private FundingSourceCandidateRepository candidateRepository;

    @Autowired
    private EnhancementRecordRepository enhancementRepository;

    @BeforeEach
    void setUp() {
        enhancementRepository.deleteAll();
        candidateRepository.deleteAll();
    }

    @Test
    void shouldTrackAISuggestedEnhancements() {
        // Step 1: Create candidate with minimal information
        FundingSourceCandidate candidate = createMinimalCandidate();
        candidate = candidateRepository.save(candidate);

        // Step 2: AI suggests description enhancement (simulated)
        String aiSuggestedDescription = "The Bulgarian Innovation Fund provides seed funding for early-stage technology startups. " +
                "Focus areas include AI, blockchain, and green technology. Applications accepted quarterly.";

        EnhancementRecord aiSuggestion = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.AI_SUGGESTED)
                .fieldName("description")
                .originalValue(candidate.getDescription())
                .suggestedValue(aiSuggestedDescription)
                .enhancedBy(null) // AI has no user ID
                .enhancedAt(LocalDateTime.now())
                .aiModel("llama-3.1-8b")
                .confidenceScore(new BigDecimal("0.85"))
                .humanApproved(false)
                .build();

        enhancementRepository.save(aiSuggestion);

        // Step 3: Human reviews and accepts AI suggestion
        candidate.setDescription(aiSuggestedDescription);
        candidateRepository.save(candidate);

        // Mark enhancement as human-approved
        aiSuggestion.setHumanApproved(true);
        aiSuggestion.setApprovedBy(UUID.randomUUID());
        aiSuggestion.setApprovedAt(LocalDateTime.now());
        enhancementRepository.save(aiSuggestion);

        // Verify enhancement tracked
        List<EnhancementRecord> enhancements = enhancementRepository
                .findByCandidateId(candidate.getCandidateId());

        assertThat(enhancements).hasSize(1);
        assertThat(enhancements.get(0).getEnhancementType()).isEqualTo(EnhancementType.AI_SUGGESTED);
        assertThat(enhancements.get(0).isHumanApproved()).isTrue();
        assertThat(enhancements.get(0).getApprovedBy()).isNotNull();
    }

    @Test
    void shouldTrackHumanModificationOfAISuggestions() {
        // Create candidate
        FundingSourceCandidate candidate = createMinimalCandidate();
        candidate = candidateRepository.save(candidate);

        // AI suggests tags
        Set<String> aiSuggestedTags = Set.of("AI", "blockchain", "greentech", "B2B");

        EnhancementRecord aiSuggestion = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.AI_SUGGESTED)
                .fieldName("tags")
                .originalValue(candidate.getTags() != null ? candidate.getTags().toString() : "[]")
                .suggestedValue(aiSuggestedTags.toString())
                .enhancedAt(LocalDateTime.now())
                .aiModel("llama-3.1-8b")
                .confidenceScore(new BigDecimal("0.78"))
                .humanApproved(false)
                .build();

        enhancementRepository.save(aiSuggestion);

        // Human modifies AI suggestion (removes B2B, adds different tag)
        UUID humanUser = UUID.randomUUID();
        Set<String> humanModifiedTags = Set.of("AI", "blockchain", "greentech", "startup-focused");

        candidate.setTags(humanModifiedTags);
        candidateRepository.save(candidate);

        // Track human modification
        EnhancementRecord humanModification = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.HUMAN_MODIFIED)
                .fieldName("tags")
                .originalValue(aiSuggestedTags.toString())
                .suggestedValue(humanModifiedTags.toString())
                .enhancedBy(humanUser)
                .enhancedAt(LocalDateTime.now())
                .humanApproved(true)
                .approvedBy(humanUser)
                .approvedAt(LocalDateTime.now())
                .build();

        enhancementRepository.save(humanModification);

        // Verify both AI suggestion and human modification tracked
        List<EnhancementRecord> enhancements = enhancementRepository
                .findByCandidateId(candidate.getCandidateId());

        assertThat(enhancements).hasSize(2);

        EnhancementRecord aiRecord = enhancements.stream()
                .filter(e -> e.getEnhancementType() == EnhancementType.AI_SUGGESTED)
                .findFirst()
                .orElseThrow();
        assertThat(aiRecord.isHumanApproved()).isFalse(); // AI suggestion was modified, not accepted

        EnhancementRecord humanRecord = enhancements.stream()
                .filter(e -> e.getEnhancementType() == EnhancementType.HUMAN_MODIFIED)
                .findFirst()
                .orElseThrow();
        assertThat(humanRecord.getEnhancedBy()).isEqualTo(humanUser);
        assertThat(humanRecord.isHumanApproved()).isTrue();
    }

    @Test
    void shouldTrackManualHumanEnhancementsWithoutAI() {
        // Create candidate
        FundingSourceCandidate candidate = createMinimalCandidate();
        candidate = candidateRepository.save(candidate);

        // Human manually adds contact information (no AI involved)
        UUID humanUser = UUID.randomUUID();
        String originalProcess = candidate.getApplicationProcess();
        String enhancedProcess = "Submit application via online portal at https://fund.bg/apply. " +
                "Include business plan, financial projections, and team CVs. Review period: 30 days.";

        candidate.setApplicationProcess(enhancedProcess);
        candidateRepository.save(candidate);

        // Track manual enhancement
        EnhancementRecord manualEnhancement = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.MANUAL)
                .fieldName("applicationProcess")
                .originalValue(originalProcess)
                .suggestedValue(enhancedProcess)
                .enhancedBy(humanUser)
                .enhancedAt(LocalDateTime.now())
                .humanApproved(true)
                .approvedBy(humanUser)
                .approvedAt(LocalDateTime.now())
                .build();

        enhancementRepository.save(manualEnhancement);

        // Verify manual enhancement tracked
        List<EnhancementRecord> enhancements = enhancementRepository
                .findByCandidateId(candidate.getCandidateId());

        assertThat(enhancements).hasSize(1);
        assertThat(enhancements.get(0).getEnhancementType()).isEqualTo(EnhancementType.MANUAL);
        assertThat(enhancements.get(0).getAiModel()).isNull();
        assertThat(enhancements.get(0).getEnhancedBy()).isEqualTo(humanUser);
    }

    @Test
    void shouldTrackMultipleEnhancementsOverTime() {
        // Create candidate
        FundingSourceCandidate candidate = createMinimalCandidate();
        candidate = candidateRepository.save(candidate);

        UUID humanUser = UUID.randomUUID();

        // Enhancement 1: AI suggests description
        EnhancementRecord enhancement1 = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.AI_SUGGESTED)
                .fieldName("description")
                .originalValue("Minimal description")
                .suggestedValue("Enhanced AI description")
                .enhancedAt(LocalDateTime.now())
                .aiModel("llama-3.1-8b")
                .confidenceScore(new BigDecimal("0.85"))
                .humanApproved(true)
                .approvedBy(humanUser)
                .approvedAt(LocalDateTime.now())
                .build();
        enhancementRepository.save(enhancement1);

        // Enhancement 2: Human manually adds tags
        EnhancementRecord enhancement2 = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.MANUAL)
                .fieldName("tags")
                .originalValue("[]")
                .suggestedValue("[AI, blockchain]")
                .enhancedBy(humanUser)
                .enhancedAt(LocalDateTime.now().plusMinutes(5))
                .humanApproved(true)
                .approvedBy(humanUser)
                .approvedAt(LocalDateTime.now().plusMinutes(5))
                .build();
        enhancementRepository.save(enhancement2);

        // Enhancement 3: AI suggests funding amounts, human modifies
        EnhancementRecord enhancement3 = EnhancementRecord.builder()
                .candidateId(candidate.getCandidateId())
                .enhancementType(EnhancementType.HUMAN_MODIFIED)
                .fieldName("fundingAmounts")
                .originalValue("5000-50000")
                .suggestedValue("10000-100000")
                .enhancedBy(humanUser)
                .enhancedAt(LocalDateTime.now().plusMinutes(10))
                .humanApproved(true)
                .approvedBy(humanUser)
                .approvedAt(LocalDateTime.now().plusMinutes(10))
                .build();
        enhancementRepository.save(enhancement3);

        // Verify all enhancements tracked in order
        List<EnhancementRecord> enhancements = enhancementRepository
                .findByCandidateId(candidate.getCandidateId());

        assertThat(enhancements).hasSize(3);
        assertThat(enhancements)
                .extracting(EnhancementRecord::getEnhancementType)
                .containsExactlyInAnyOrder(
                        EnhancementType.AI_SUGGESTED,
                        EnhancementType.MANUAL,
                        EnhancementType.HUMAN_MODIFIED
                );

        // All should be human-approved
        assertThat(enhancements).allMatch(EnhancementRecord::isHumanApproved);
    }

    @Test
    void shouldQueryEnhancementsByType() {
        // Create multiple candidates with different enhancement types
        UUID humanUser = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            FundingSourceCandidate candidate = createMinimalCandidate();
            candidate.setOrganizationName("Org " + i);
            candidate = candidateRepository.save(candidate);

            // AI suggestion
            EnhancementRecord aiRecord = EnhancementRecord.builder()
                    .candidateId(candidate.getCandidateId())
                    .enhancementType(EnhancementType.AI_SUGGESTED)
                    .fieldName("test")
                    .enhancedAt(LocalDateTime.now())
                    .build();
            enhancementRepository.save(aiRecord);

            // Manual enhancement
            EnhancementRecord manualRecord = EnhancementRecord.builder()
                    .candidateId(candidate.getCandidateId())
                    .enhancementType(EnhancementType.MANUAL)
                    .fieldName("test")
                    .enhancedBy(humanUser)
                    .enhancedAt(LocalDateTime.now())
                    .build();
            enhancementRepository.save(manualRecord);
        }

        // Query by type
        List<EnhancementRecord> aiEnhancements = enhancementRepository
                .findByEnhancementType(EnhancementType.AI_SUGGESTED);
        List<EnhancementRecord> manualEnhancements = enhancementRepository
                .findByEnhancementType(EnhancementType.MANUAL);

        assertThat(aiEnhancements).hasSize(3);
        assertThat(manualEnhancements).hasSize(3);
    }

    private FundingSourceCandidate createMinimalCandidate() {
        return FundingSourceCandidate.builder()
                .organizationName("Bulgarian Innovation Fund")
                .programName("Seed Funding Program")
                .description("Minimal description")
                .sourceUrl("https://example.com/fund")
                .confidenceScore(new BigDecimal("0.75"))
                .fundingAmountMin(BigDecimal.valueOf(5000))
                .fundingAmountMax(BigDecimal.valueOf(50000))
                .currency("EUR")
                .geographicEligibility(List.of("Bulgaria"))
                .organizationTypes(List.of("Startup"))
                .tags(Set.of())
                .status(CandidateStatus.PENDING_REVIEW)
                .discoveredAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .extractedData("{}")
                .build();
    }
}
