package com.northstar.funding.discovery.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.northstar.funding.discovery.domain.AdminRole;
import com.northstar.funding.discovery.domain.AdminUser;
import com.northstar.funding.discovery.domain.AuthorityLevel;
import com.northstar.funding.discovery.domain.ContactIntelligence;
import com.northstar.funding.discovery.domain.ContactType;
import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.EnhancementRecord;
import com.northstar.funding.discovery.domain.EnhancementType;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.domain.SessionType;

/**
 * Test Data Factory for Integration Tests
 * 
 * Provides fluent builder pattern for creating consistent test data
 * with sensible defaults and customization options.
 * 
 * Supports AdminUser, ContactIntelligence, DiscoverySession, and EnhancementRecord domain entities.
 */
@Component
public class TestDataFactory {

    /**
     * Create a basic AdminUser builder with sensible defaults.
     * All required fields are pre-populated, optional fields can be customized.
     */
    public AdminUser.AdminUserBuilder adminUserBuilder() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return AdminUser.builder()
            .username("test.user." + uniqueId) // Unique username
            .fullName("Test User")
            .email("test.user." + uniqueId + "@northstar.com")
            .role(AdminRole.REVIEWER) // Default role
            .isActive(true)
            .specializations(Set.of("general"))
            .candidatesReviewed(0)
            .averageReviewTimeMinutes(0)
            .approvalRate(0.0)
            .currentWorkload(0)
            .maxConcurrentAssignments(10)
            .createdAt(LocalDateTime.now());
    }

    /**
     * Create a reviewer-specific AdminUser builder with reviewer defaults.
     */
    public AdminUser.AdminUserBuilder reviewerBuilder() {
        return adminUserBuilder()
            .role(AdminRole.REVIEWER)
            .specializations(Set.of("technology", "research"))
            .maxConcurrentAssignments(15)
            .candidatesReviewed(25)
            .averageReviewTimeMinutes(30)
            .approvalRate(0.72);
    }

    /**
     * Create an administrator-specific AdminUser builder with admin defaults.
     */
    public AdminUser.AdminUserBuilder administratorBuilder() {
        return adminUserBuilder()
            .role(AdminRole.ADMINISTRATOR)
            .specializations(Set.of("management", "system", "policy"))
            .maxConcurrentAssignments(25)
            .candidatesReviewed(100)
            .averageReviewTimeMinutes(20)
            .approvalRate(0.80);
    }

    /**
     * Create a high-performance reviewer with excellent statistics.
     */
    public AdminUser.AdminUserBuilder highPerformanceReviewerBuilder() {
        return reviewerBuilder()
            .specializations(Set.of("fintech", "blockchain", "ai-ml"))
            .candidatesReviewed(200)
            .averageReviewTimeMinutes(15) // Fast reviews
            .approvalRate(0.90) // High approval rate
            .currentWorkload(5); // Manageable workload
    }

    /**
     * Create a specialized reviewer with specific expertise.
     */
    public AdminUser.AdminUserBuilder specialistBuilder(String... specializations) {
        return reviewerBuilder()
            .specializations(Set.of(specializations))
            .candidatesReviewed(75)
            .averageReviewTimeMinutes(25)
            .approvalRate(0.85);
    }

    /**
     * Create an overloaded reviewer for workload testing.
     */
    public AdminUser.AdminUserBuilder overloadedReviewerBuilder() {
        return reviewerBuilder()
            .currentWorkload(15) // At capacity
            .maxConcurrentAssignments(15)
            .candidatesReviewed(300)
            .averageReviewTimeMinutes(45) // Slower due to workload
            .approvalRate(0.65); // Lower quality due to pressure
    }

    // ===============================
    // ContactIntelligence Builders
    // ===============================

    /**
     * Create a basic ContactIntelligence builder with sensible defaults.
     * All required fields are pre-populated, optional fields can be customized.
     */
    public ContactIntelligence.ContactIntelligenceBuilder contactIntelligenceBuilder() {
        return ContactIntelligence.builder()
            .candidateId(UUID.randomUUID()) // Will be overridden in tests
            .contactType(ContactType.PROGRAM_OFFICER)
            .decisionAuthority(AuthorityLevel.INFORMATION_ONLY)
            .fullName("Test Contact " + System.currentTimeMillis())
            .title("Program Officer")
            .organization("Test Foundation")
            .email("test.contact." + System.currentTimeMillis() + "@foundation.org")
            .phone("+1-555-0199")
            .communicationPreference("email")
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now());
    }

    /**
     * Create a decision maker contact builder.
     */
    public ContactIntelligence.ContactIntelligenceBuilder decisionMakerBuilder() {
        return contactIntelligenceBuilder()
            .decisionAuthority(AuthorityLevel.DECISION_MAKER)
            .title("Program Director")
            .contactType(ContactType.PROGRAM_OFFICER)
            .validatedAt(LocalDateTime.now())
            .relationshipNotes("Primary decision maker for technology grants");
    }

    /**
     * Create an influencer contact builder.
     */
    public ContactIntelligence.ContactIntelligenceBuilder influencerBuilder() {
        return contactIntelligenceBuilder()
            .decisionAuthority(AuthorityLevel.INFLUENCER)
            .title("Senior Program Manager")
            .contactType(ContactType.FOUNDATION_STAFF)
            .validatedAt(LocalDateTime.now())
            .relationshipNotes("Strong influence on funding decisions");
    }

    /**
     * Create a government official contact builder.
     */
    public ContactIntelligence.ContactIntelligenceBuilder governmentOfficialBuilder() {
        return contactIntelligenceBuilder()
            .contactType(ContactType.GOVERNMENT_OFFICIAL)
            .organization("Department of Innovation")
            .title("Grant Program Coordinator")
            .decisionAuthority(AuthorityLevel.DECISION_MAKER)
            .validatedAt(LocalDateTime.now());
    }

    /**
     * Create an academic contact builder.
     */
    public ContactIntelligence.ContactIntelligenceBuilder academicContactBuilder() {
        return contactIntelligenceBuilder()
            .contactType(ContactType.ACADEMIC_CONTACT)
            .organization("University Research Office")
            .title("Research Partnerships Director")
            .decisionAuthority(AuthorityLevel.INFLUENCER);
    }

    /**
     * Create a corporate contact builder.
     */
    public ContactIntelligence.ContactIntelligenceBuilder corporateContactBuilder() {
        return contactIntelligenceBuilder()
            .contactType(ContactType.CORPORATE_CONTACT)
            .organization("Corporate Foundation")
            .title("CSR Program Manager")
            .decisionAuthority(AuthorityLevel.DECISION_MAKER);
    }

    /**
     * Create a contact with referral network.
     */
    public ContactIntelligence.ContactIntelligenceBuilder contactWithReferralBuilder() {
        return contactIntelligenceBuilder()
            .referralConnections("{\"mutual_contacts\": [\"John Smith\", \"Jane Doe\"], \"referral_path\": \"LinkedIn -> Conference\"}")
            .referralSource("Industry conference introduction");
    }

    /**
     * Create a recently contacted contact.
     */
    public ContactIntelligence.ContactIntelligenceBuilder recentlyContactedBuilder() {
        return contactIntelligenceBuilder()
            .lastContactedAt(LocalDateTime.now().minusDays(7))
            .validatedAt(LocalDateTime.now().minusDays(30))
            .responsePattern("Responds within 2-3 days, prefers email");
    }

    /**
     * Create an unvalidated contact for validation testing.
     */
    public ContactIntelligence.ContactIntelligenceBuilder unvalidatedContactBuilder() {
        return contactIntelligenceBuilder()
            .validatedAt(null) // Never validated
            .lastContactedAt(null); // Never contacted
    }

    /**
     * Create a stale contact (needs revalidation).
     */
    public ContactIntelligence.ContactIntelligenceBuilder staleContactBuilder() {
        return contactIntelligenceBuilder()
            .validatedAt(LocalDateTime.now().minusMonths(12)) // Old validation
            .lastContactedAt(LocalDateTime.now().minusMonths(6))
            .responsePattern("Previously responsive, needs revalidation");
    }

    // ===============================
    // DiscoverySession Builders
    // ===============================

    /**
     * Create a basic DiscoverySession builder with sensible defaults.
     * All required fields are pre-populated, optional fields can be customized.
     */
    public DiscoverySession.DiscoverySessionBuilder discoverySessionBuilder() {
        return DiscoverySession.builder()
            .executedAt(LocalDateTime.now())
            .executedBy("test-system")
            .sessionType(SessionType.SCHEDULED)
            .status(SessionStatus.RUNNING)
            .durationMinutes(0)
            .startedAt(LocalDateTime.now())
            .completedAt(null)
            .searchEnginesUsed(new HashSet<>(Set.of("searxng")))
            .searchQueries(new ArrayList<>(List.of("test funding query")))
            .queryGenerationPrompt("Find test funding opportunities")
            .candidatesFound(0)
            .duplicatesDetected(0)
            .sourcesScraped(0)
            .averageConfidenceScore(0.0)
            .errorMessages(new ArrayList<>())
            .searchEngineFailures("{}")
            .llmModelUsed("llama-3.1-8b")
            .searchParameters("{\"region\":\"test\",\"sector\":\"test\"}");
    }

    /**
     * Create a completed discovery session builder.
     */
    public DiscoverySession.DiscoverySessionBuilder completedSessionBuilder() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(30);
        
        return discoverySessionBuilder()
            .status(SessionStatus.COMPLETED)
            .startedAt(startTime)
            .completedAt(endTime)
            .durationMinutes(30)
            .candidatesFound(15)
            .duplicatesDetected(2)
            .sourcesScraped(45)
            .averageConfidenceScore(0.78)
            .searchEnginesUsed(new HashSet<>(Set.of("searxng", "tavily")))
            .searchQueries(new ArrayList<>(List.of("EU funding technology", "innovation grants Europe")));
    }

    /**
     * Create a failed discovery session builder.
     */
    public DiscoverySession.DiscoverySessionBuilder failedSessionBuilder() {
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        LocalDateTime endTime = LocalDateTime.now().minusHours(1).minusMinutes(55);
        
        return discoverySessionBuilder()
            .status(SessionStatus.FAILED)
            .startedAt(startTime)
            .completedAt(endTime)
            .durationMinutes(5)
            .candidatesFound(0)
            .duplicatesDetected(0)
            .sourcesScraped(3)
            .averageConfidenceScore(0.0)
            .errorMessages(new ArrayList<>(List.of("Connection timeout", "Rate limit exceeded")))
            .searchEngineFailures("{\"searxng\":[\"Connection timeout\"],\"tavily\":[\"Rate limit exceeded\",\"Authentication failed\"]}");
    }

    /**
     * Create a high-performance discovery session builder.
     */
    public DiscoverySession.DiscoverySessionBuilder highPerformanceSessionBuilder() {
        return completedSessionBuilder()
            .candidatesFound(50)
            .duplicatesDetected(5)
            .sourcesScraped(120)
            .averageConfidenceScore(0.92)
            .durationMinutes(20)
            .searchEnginesUsed(new HashSet<>(Set.of("searxng", "tavily", "perplexity")))
            .llmModelUsed("llama-3.1-70b")
            .searchQueries(new ArrayList<>(List.of(
                "European technology funding programs 2024",
                "startup innovation grants EU",
                "research development funding opportunities"
            )));
    }

    /**
     * Create a manual discovery session builder.
     */
    public DiscoverySession.DiscoverySessionBuilder manualSessionBuilder() {
        return discoverySessionBuilder()
            .sessionType(SessionType.MANUAL)
            .executedBy("admin-user")
            .searchEnginesUsed(new HashSet<>(Set.of("perplexity")))
            .queryGenerationPrompt("Manual search for specific funding domain");
    }

    /**
     * Create a retry discovery session builder.
     */
    public DiscoverySession.DiscoverySessionBuilder retrySessionBuilder() {
        return discoverySessionBuilder()
            .sessionType(SessionType.RETRY)
            .executedBy("retry-scheduler")
            .searchParameters("{\"retry_count\":\"2\",\"original_session\":\"" + UUID.randomUUID().toString() + "\"}");
    }

    /**
     * Create a discovery session with multiple search engines.
     */
    public DiscoverySession.DiscoverySessionBuilder multiEngineSessionBuilder() {
        return discoverySessionBuilder()
            .searchEnginesUsed(new HashSet<>(Set.of("searxng", "tavily", "perplexity")))
            .searchQueries(new ArrayList<>(List.of(
                "funding query 1 via searxng",
                "funding query 2 via tavily", 
                "funding query 3 via perplexity"
            )))
            .searchParameters("{\"searxng_timeout\":\"30\",\"tavily_limit\":\"50\",\"perplexity_depth\":\"comprehensive\"}");
    }

    /**
     * Create a long-running discovery session builder.
     */
    public DiscoverySession.DiscoverySessionBuilder longRunningSessionBuilder() {
        return discoverySessionBuilder()
            .status(SessionStatus.RUNNING)
            .startedAt(LocalDateTime.now().minusHours(2))
            .completedAt(null)
            .durationMinutes(0) // Will be calculated when completed
            .candidatesFound(25) // Partial results
            .duplicatesDetected(3)
            .sourcesScraped(75);
    }

    /**
     * Create a discovery session with search engine failures.
     */
    public DiscoverySession.DiscoverySessionBuilder sessionWithFailuresBuilder() {
        return discoverySessionBuilder()
            .status(SessionStatus.COMPLETED) // Completed despite some failures
            .candidatesFound(12)
            .duplicatesDetected(1)
            .sourcesScraped(30)
            .averageConfidenceScore(0.65)
            .searchEngineFailures("{\"searxng\":[\"Occasional timeout\"],\"tavily\":[\"Rate limit warning\"]}")
            .errorMessages(new ArrayList<>(List.of("Some search engines experienced issues but session completed")));
    }

    // ===============================
    // EnhancementRecord Builders
    // ===============================

    /**
     * Create a basic EnhancementRecord with sensible defaults.
     * All required fields are pre-populated, optional fields can be customized.
     * NOTE: enhancementId is NOT set - Spring Data JDBC will generate it on insert
     */
    public EnhancementRecord enhancementRecordBuilder() {
        return EnhancementRecord.builder()
            // .enhancementId() - omitted, Spring Data JDBC generates on insert
            .candidateId(UUID.randomUUID()) // Will be overridden in tests
            .enhancedBy(UUID.randomUUID()) // Will be overridden in tests
            .enhancementType(EnhancementType.MANUAL)
            .fieldName("test_field")
            .originalValue("old value")
            .suggestedValue("new value")
            .notes("Test enhancement")
            .enhancedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Create a contact added enhancement (manual human enhancement).
     * NOTE: enhancementId is NOT set - Spring Data JDBC will generate it on insert
     */
    public EnhancementRecord contactAddedEnhancement(UUID candidateId, UUID enhancedBy) {
        return EnhancementRecord.builder()
            // .enhancementId() - omitted, Spring Data JDBC generates on insert
            .candidateId(candidateId)
            .enhancedBy(enhancedBy)
            .enhancementType(EnhancementType.MANUAL)
            .fieldName("contact_email")
            .originalValue(null)
            .suggestedValue("contact@foundation.org")
            .notes("Added primary contact email from foundation website")
            .timeSpentMinutes(15)
            .enhancedAt(LocalDateTime.now().minusMinutes(5))
            .humanApproved(true)
            .approvedBy(enhancedBy)
            .approvedAt(LocalDateTime.now().minusMinutes(5))
            .build();
    }

    /**
     * Create a data corrected enhancement (manual human correction).
     * NOTE: enhancementId is NOT set - Spring Data JDBC will generate it on insert
     */
    public EnhancementRecord dataCorrectedEnhancement(UUID candidateId, UUID enhancedBy) {
        return EnhancementRecord.builder()
            // .enhancementId() - omitted, Spring Data JDBC generates on insert
            .candidateId(candidateId)
            .enhancedBy(enhancedBy)
            .enhancementType(EnhancementType.MANUAL)
            .fieldName("organization_name")
            .originalValue("Old Foundation Name")
            .suggestedValue("Correct Foundation Name")
            .notes("Corrected organization name from official website")
            .timeSpentMinutes(10)
            .enhancedAt(LocalDateTime.now().minusMinutes(10))
            .humanApproved(true)
            .approvedBy(enhancedBy)
            .approvedAt(LocalDateTime.now().minusMinutes(10))
            .build();
    }

    /**
     * Create a notes added enhancement (manual human enhancement).
     * NOTE: enhancementId is NOT set - Spring Data JDBC will generate it on insert
     */
    public EnhancementRecord notesAddedEnhancement(UUID candidateId, UUID enhancedBy) {
        return EnhancementRecord.builder()
            // .enhancementId() - omitted, Spring Data JDBC generates on insert
            .candidateId(candidateId)
            .enhancedBy(enhancedBy)
            .enhancementType(EnhancementType.MANUAL)
            .fieldName("validation_notes")
            .originalValue(null)
            .suggestedValue("Verified eligibility criteria match our requirements. Previous applicant mentioned positive experience.")
            .notes("Added context from research and network contacts")
            .timeSpentMinutes(20)
            .enhancedAt(LocalDateTime.now().minusMinutes(2))
            .humanApproved(true)
            .approvedBy(enhancedBy)
            .approvedAt(LocalDateTime.now().minusMinutes(2))
            .build();
    }

    /**
     * Create a duplicate merged enhancement (manual human action).
     * NOTE: enhancementId is NOT set - Spring Data JDBC will generate it on insert
     */
    public EnhancementRecord duplicateMergedEnhancement(UUID candidateId, UUID enhancedBy) {
        return EnhancementRecord.builder()
            // .enhancementId() - omitted, Spring Data JDBC generates on insert
            .candidateId(candidateId)
            .enhancedBy(enhancedBy)
            .enhancementType(EnhancementType.MANUAL)
            .fieldName("duplicate_of_candidate_id")
            .originalValue(null)
            .suggestedValue(UUID.randomUUID().toString())
            .notes("Merged duplicate entry - same foundation, different program name format")
            .timeSpentMinutes(25)
            .enhancedAt(LocalDateTime.now())
            .humanApproved(true)
            .approvedBy(enhancedBy)
            .approvedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Create a status changed enhancement (manual human action).
     * NOTE: enhancementId is NOT set - Spring Data JDBC will generate it on insert
     */
    public EnhancementRecord statusChangedEnhancement(UUID candidateId, UUID enhancedBy) {
        return EnhancementRecord.builder()
            // .enhancementId() - omitted, Spring Data JDBC generates on insert
            .candidateId(candidateId)
            .enhancedBy(enhancedBy)
            .enhancementType(EnhancementType.MANUAL)
            .fieldName("status")
            .originalValue("PENDING_REVIEW")
            .suggestedValue("VALIDATED")
            .notes("Manual review completed - candidate meets all criteria")
            .timeSpentMinutes(30)
            .enhancedAt(LocalDateTime.now())
            .humanApproved(true)
            .approvedBy(enhancedBy)
            .approvedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Create a validation completed enhancement (manual human validation).
     * NOTE: enhancementId is NOT set - Spring Data JDBC will generate it on insert
     */
    public EnhancementRecord validationCompletedEnhancement(UUID candidateId, UUID enhancedBy) {
        return EnhancementRecord.builder()
            // .enhancementId() - omitted, Spring Data JDBC generates on insert
            .candidateId(candidateId)
            .enhancedBy(enhancedBy)
            .enhancementType(EnhancementType.MANUAL)
            .fieldName("validation_status")
            .originalValue("not_validated")
            .suggestedValue("validated")
            .notes("Full validation completed including website verification and contact confirmation")
            .timeSpentMinutes(45)
            .enhancedAt(LocalDateTime.now())
            .humanApproved(true)
            .approvedBy(enhancedBy)
            .approvedAt(LocalDateTime.now())
            .build();
    }
}
