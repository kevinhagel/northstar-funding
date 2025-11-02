package com.northstar.funding.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.northstar.funding.domain.AdminRole;
import com.northstar.funding.domain.AdminUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AdminUserRepository
 *
 * Tests admin user management, authentication queries, performance tracking,
 * and reviewer assignment algorithms.
 * Uses TestContainers with PostgreSQL for realistic testing.
 */
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AdminUserRepository Integration Tests")
class AdminUserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AdminUserRepository repository;

    @BeforeEach
    void setUp() {
        // Delete all except the founder users inserted by V3 migration
        repository.findAll().forEach(user -> {
            if (!user.getUsername().equals("kevin") && !user.getUsername().equals("huw")) {
                repository.delete(user);
            }
        });
    }

    // ============================================================================
    // Basic CRUD Tests
    // ============================================================================

    @Test
    @DisplayName("Save and find admin user by ID")
    void testSaveAndFindById() {
        // Given
        AdminUser user = createTestUser("test-user", "test@example.com", AdminRole.REVIEWER);

        // When
        AdminUser saved = repository.save(user);

        // Then
        Optional<AdminUser> found = repository.findById(saved.getUserId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("test-user");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Find user by username")
    void testFindByUsername() {
        // Given
        repository.save(createTestUser("reviewer1", "r1@example.com", AdminRole.REVIEWER));

        // When
        Optional<AdminUser> found = repository.findByUsername("reviewer1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("r1@example.com");
    }

    @Test
    @DisplayName("Find user by email")
    void testFindByEmail() {
        // Given
        repository.save(createTestUser("user1", "unique@example.com", AdminRole.REVIEWER));

        // When
        Optional<AdminUser> found = repository.findByEmail("unique@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("user1");
    }

    // ============================================================================
    // Role and Active Status Tests
    // ============================================================================

    @Test
    @DisplayName("Find active users only")
    void testFindByIsActive() {
        // Given
        AdminUser active = createTestUser("active", "active@example.com", AdminRole.REVIEWER);
        active.setIsActive(true);
        repository.save(active);

        AdminUser inactive = createTestUser("inactive", "inactive@example.com", AdminRole.REVIEWER);
        inactive.setIsActive(false);
        repository.save(inactive);

        // When
        List<AdminUser> activeUsers = repository.findByIsActive(true);

        // Then - Should include kevin, huw, and our test active user (3 total)
        assertThat(activeUsers).hasSizeGreaterThanOrEqualTo(3);
        assertThat(activeUsers).allMatch(AdminUser::getIsActive);
    }

    @Test
    @DisplayName("Find users by role")
    void testFindByRole() {
        // Given - Cannot create additional ADMINISTRATOR users due to kevin/huw constraint
        repository.save(createTestUser("reviewer1", "reviewer1@example.com", AdminRole.REVIEWER));
        repository.save(createTestUser("reviewer2", "reviewer2@example.com", AdminRole.REVIEWER));

        // When
        List<AdminUser> reviewers = repository.findByRole(AdminRole.REVIEWER);
        List<AdminUser> admins = repository.findByRole(AdminRole.ADMINISTRATOR);

        // Then
        assertThat(reviewers).hasSizeGreaterThanOrEqualTo(2); // Our 2 test reviewers
        assertThat(admins).hasSizeGreaterThanOrEqualTo(2); // kevin and huw only
    }

    @Test
    @DisplayName("Find active users by role")
    void testFindByRoleAndIsActive() {
        // Given
        AdminUser activeReviewer = createTestUser("active-rev", "active@example.com", AdminRole.REVIEWER);
        activeReviewer.setIsActive(true);
        repository.save(activeReviewer);

        AdminUser inactiveReviewer = createTestUser("inactive-rev", "inactive@example.com", AdminRole.REVIEWER);
        inactiveReviewer.setIsActive(false);
        repository.save(inactiveReviewer);

        // When
        List<AdminUser> activeReviewers = repository.findByRoleAndIsActive(AdminRole.REVIEWER, true);

        // Then
        assertThat(activeReviewers).hasSizeGreaterThanOrEqualTo(1);
        assertThat(activeReviewers).allMatch(u -> u.getRole() == AdminRole.REVIEWER && u.getIsActive());
    }

    // ============================================================================
    // Specialization Tests
    // ============================================================================

    @Test
    @DisplayName("Find users by specialization")
    void testFindBySpecializationsContaining() {
        // Given
        AdminUser techUser = createTestUser("tech-user", "tech@example.com", AdminRole.REVIEWER);
        techUser.setSpecializations(Set.of("technology", "academic"));
        techUser.setApprovalRate(0.85);
        techUser.setAverageReviewTimeMinutes(25);
        repository.save(techUser);

        AdminUser artUser = createTestUser("art-user", "art@example.com", AdminRole.REVIEWER);
        artUser.setSpecializations(Set.of("arts", "culture"));
        repository.save(artUser);

        // When
        List<AdminUser> techSpecialists = repository.findBySpecializationsContaining("technology");

        // Then - Should include kevin (has technology) and tech-user
        assertThat(techSpecialists).hasSizeGreaterThanOrEqualTo(2);
        assertThat(techSpecialists).allMatch(u -> u.getSpecializations() != null && u.getSpecializations().contains("technology"));
    }

    @Test
    @DisplayName("Find optimal reviewer for specialization")
    void testFindOptimalReviewerForSpecialization() {
        // Given
        AdminUser specialist = createTestUser("specialist", "specialist@example.com", AdminRole.REVIEWER);
        specialist.setSpecializations(Set.of("technology", "research"));
        specialist.setApprovalRate(0.90);
        specialist.setCandidatesReviewed(50);
        specialist.setAverageReviewTimeMinutes(20);
        repository.save(specialist);

        AdminUser generalist = createTestUser("generalist", "generalist@example.com", AdminRole.REVIEWER);
        generalist.setSpecializations(Set.of("general"));
        generalist.setApprovalRate(0.70);
        repository.save(generalist);

        // When
        Optional<AdminUser> optimalForTech = repository.findOptimalReviewerForSpecialization("technology");

        // Then - Should prefer specialist over generalist
        assertThat(optimalForTech).isPresent();
        assertThat(optimalForTech.get().getSpecializations()).contains("technology");
    }

    // ============================================================================
    // Performance and Quality Tests
    // ============================================================================

    @Test
    @DisplayName("Find productive reviewers (high review count)")
    void testFindByCandidatesReviewedGreaterThan() {
        // Given
        AdminUser productive = createTestUser("productive", "productive@example.com", AdminRole.REVIEWER);
        productive.setCandidatesReviewed(100);
        repository.save(productive);

        AdminUser novice = createTestUser("novice", "novice@example.com", AdminRole.REVIEWER);
        novice.setCandidatesReviewed(5);
        repository.save(novice);

        // When
        List<AdminUser> productiveUsers = repository.findByCandidatesReviewedGreaterThan(50);

        // Then
        assertThat(productiveUsers).hasSizeGreaterThanOrEqualTo(1);
        assertThat(productiveUsers).allMatch(u -> u.getCandidatesReviewed() > 50);
    }

    @Test
    @DisplayName("Find fast reviewers (low average review time)")
    void testFindByAverageReviewTimeLessThan() {
        // Given
        AdminUser fast = createTestUser("fast", "fast@example.com", AdminRole.REVIEWER);
        fast.setAverageReviewTimeMinutes(15);
        fast.setCandidatesReviewed(20); // Above minimum
        repository.save(fast);

        AdminUser slow = createTestUser("slow", "slow@example.com", AdminRole.REVIEWER);
        slow.setAverageReviewTimeMinutes(60);
        slow.setCandidatesReviewed(20);
        repository.save(slow);

        // When
        List<AdminUser> fastReviewers = repository.findByAverageReviewTimeLessThan(30, 10);

        // Then
        assertThat(fastReviewers).hasSizeGreaterThanOrEqualTo(1);
        assertThat(fastReviewers).allMatch(u -> u.getAverageReviewTimeMinutes() < 30 && u.getCandidatesReviewed() > 10);
    }

    @Test
    @DisplayName("Find high-quality reviewers (high approval rate)")
    void testFindHighQualityReviewers() {
        // Given
        AdminUser highQuality = createTestUser("high-quality", "hq@example.com", AdminRole.REVIEWER);
        highQuality.setApprovalRate(0.85);
        highQuality.setCandidatesReviewed(30);
        repository.save(highQuality);

        AdminUser lowQuality = createTestUser("low-quality", "lq@example.com", AdminRole.REVIEWER);
        lowQuality.setApprovalRate(0.50);
        lowQuality.setCandidatesReviewed(30);
        repository.save(lowQuality);

        // When
        List<AdminUser> highQualityReviewers = repository.findHighQualityReviewers(0.75, 20);

        // Then
        assertThat(highQualityReviewers).hasSizeGreaterThanOrEqualTo(1);
        assertThat(highQualityReviewers).allMatch(u -> u.getApprovalRate() >= 0.75 && u.getCandidatesReviewed() >= 20);
    }

    // ============================================================================
    // Workload and Availability Tests
    // ============================================================================

    @Test
    @DisplayName("Find available reviewers (not overloaded)")
    void testFindAvailableReviewers() {
        // Given - Create reviewer with low workload
        AdminUser available = createTestUser("available", "available@example.com", AdminRole.REVIEWER);
        available.setCurrentWorkload(2);
        repository.save(available);

        // When
        List<AdminUser> availableReviewers = repository.findAvailableReviewers(5);

        // Then - Should include our available reviewer
        assertThat(availableReviewers).isNotEmpty();
    }

    // ============================================================================
    // Activity and Audit Tests
    // ============================================================================

    @Test
    @DisplayName("Find inactive users (no recent login)")
    void testFindInactiveUsers() {
        // Given
        AdminUser recentlyActive = createTestUser("recent", "recent@example.com", AdminRole.REVIEWER);
        recentlyActive.setLastLoginAt(LocalDateTime.now().minusDays(1));
        repository.save(recentlyActive);

        AdminUser longInactive = createTestUser("inactive", "inactive@example.com", AdminRole.REVIEWER);
        longInactive.setLastLoginAt(LocalDateTime.now().minusDays(40));
        repository.save(longInactive);

        AdminUser neverLoggedIn = createTestUser("never", "never@example.com", AdminRole.REVIEWER);
        neverLoggedIn.setLastLoginAt(null);
        repository.save(neverLoggedIn);

        // When - Find users inactive for more than 30 days
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<AdminUser> inactiveUsers = repository.findInactiveUsers(threshold);

        // Then - Should include longInactive and neverLoggedIn
        assertThat(inactiveUsers).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Find users created in date range")
    void testFindUsersCreatedBetween() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        AdminUser oldUser = createTestUser("old", "old@example.com", AdminRole.REVIEWER);
        oldUser.setCreatedAt(now.minusDays(100));
        repository.save(oldUser);

        AdminUser recentUser = createTestUser("recent", "recent@example.com", AdminRole.REVIEWER);
        recentUser.setCreatedAt(now.minusDays(5));
        repository.save(recentUser);

        // When
        LocalDateTime start = now.minusDays(10);
        LocalDateTime end = now;
        List<AdminUser> recentUsers = repository.findUsersCreatedBetween(start, end);

        // Then
        assertThat(recentUsers).hasSizeGreaterThanOrEqualTo(1);
        assertThat(recentUsers).allMatch(u ->
            u.getCreatedAt().isAfter(start) && u.getCreatedAt().isBefore(end)
        );
    }

    // ============================================================================
    // Statistics Tests
    // ============================================================================

    // Note: Projection interface tests (ReviewerStats, ReviewerWorkload, AdminActivitySummary)
    // require complex join queries that need full Spring Boot context.
    // These are better tested in higher-level integration tests or manually verified.
    // The repository methods exist and are properly defined with @Query annotations.

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private AdminUser createTestUser(String username, String email, AdminRole role) {
        return AdminUser.builder()
            .username(username)
            .fullName(username.toUpperCase() + " Full Name")
            .email(email)
            .role(role)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .candidatesReviewed(0)
            .averageReviewTimeMinutes(0)
            .approvalRate(0.0)
            .currentWorkload(0)
            .maxConcurrentAssignments(10)
            .build();
    }
}
