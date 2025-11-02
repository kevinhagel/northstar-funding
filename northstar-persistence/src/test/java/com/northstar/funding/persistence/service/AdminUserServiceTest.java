package com.northstar.funding.persistence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.funding.domain.AdminRole;
import com.northstar.funding.domain.AdminUser;
import com.northstar.funding.persistence.repository.AdminUserRepository;

/**
 * Unit tests for AdminUserService using Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService Unit Tests")
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    private AdminUser testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = AdminUser.builder()
            .userId(testUserId)
            .username("test-user")
            .fullName("Test User")
            .email("test@example.com")
            .role(AdminRole.REVIEWER)
            .isActive(true)
            .createdAt(LocalDateTime.now())
            .candidatesReviewed(10)
            .averageReviewTimeMinutes(30)
            .approvalRate(0.75)
            .currentWorkload(2)
            .maxConcurrentAssignments(10)
            .specializations(Set.of("technology", "academic"))
            .build();
    }

    // ============================================================================
    // CREATE & UPDATE Operations Tests
    // ============================================================================

    @Test
    @DisplayName("createUser should set defaults and save user")
    void createUser_ShouldSetDefaultsAndSave() {
        // Given
        AdminUser newUser = AdminUser.builder()
            .username("new-user")
            .fullName("New User")
            .email("new@example.com")
            .role(AdminRole.REVIEWER)
            .build();

        when(adminUserRepository.save(any(AdminUser.class)))
            .thenReturn(testUser);

        // When
        AdminUser result = adminUserService.createUser(newUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(newUser.getIsActive()).isTrue();
        assertThat(newUser.getCreatedAt()).isNotNull();
        assertThat(newUser.getCandidatesReviewed()).isEqualTo(0);
        assertThat(newUser.getCurrentWorkload()).isEqualTo(0);
        verify(adminUserRepository).save(newUser);
    }

    @Test
    @DisplayName("createUser should preserve provided defaults")
    void createUser_ShouldPreserveProvidedDefaults() {
        // Given
        LocalDateTime customCreatedAt = LocalDateTime.now().minusDays(10);
        AdminUser newUser = AdminUser.builder()
            .username("new-user")
            .fullName("New User")
            .email("new@example.com")
            .role(AdminRole.ADMINISTRATOR)
            .isActive(false)
            .createdAt(customCreatedAt)
            .candidatesReviewed(100)
            .currentWorkload(5)
            .build();

        when(adminUserRepository.save(any(AdminUser.class)))
            .thenReturn(newUser);

        // When
        AdminUser result = adminUserService.createUser(newUser);

        // Then
        assertThat(newUser.getIsActive()).isFalse();
        assertThat(newUser.getCreatedAt()).isEqualTo(customCreatedAt);
        assertThat(newUser.getCandidatesReviewed()).isEqualTo(100);
        assertThat(newUser.getCurrentWorkload()).isEqualTo(5);
    }

    @Test
    @DisplayName("recordLogin should update lastLoginAt timestamp")
    void recordLogin_ShouldUpdateLastLoginAt() {
        // Given
        when(adminUserRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(adminUserRepository.save(any(AdminUser.class)))
            .thenReturn(testUser);

        // When
        AdminUser result = adminUserService.recordLogin(testUserId);

        // Then
        assertThat(testUser.getLastLoginAt()).isNotNull();
        verify(adminUserRepository).findById(testUserId);
        verify(adminUserRepository).save(testUser);
    }

    @Test
    @DisplayName("recordLogin should throw when user not found")
    void recordLogin_WhenUserNotFound_ShouldThrow() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(adminUserRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserService.recordLogin(nonExistentId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("updateReviewStatistics should update approval rate and review time")
    void updateReviewStatistics_ShouldUpdateStats() {
        // Given
        when(adminUserRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(adminUserRepository.save(any(AdminUser.class)))
            .thenReturn(testUser);

        // When
        AdminUser result = adminUserService.updateReviewStatistics(testUserId, true, Duration.ofMinutes(25));

        // Then
        assertThat(testUser.getCandidatesReviewed()).isEqualTo(11); // Was 10, now 11
        assertThat(testUser.getApprovalRate()).isGreaterThan(0.75); // Approved, so rate increases
        verify(adminUserRepository).save(testUser);
    }

    @Test
    @DisplayName("setActive should toggle user active status")
    void setActive_ShouldUpdateActiveStatus() {
        // Given
        when(adminUserRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(adminUserRepository.save(any(AdminUser.class)))
            .thenReturn(testUser);

        // When
        AdminUser result = adminUserService.setActive(testUserId, false);

        // Then
        assertThat(testUser.getIsActive()).isFalse();
        verify(adminUserRepository).save(testUser);
    }

    @Test
    @DisplayName("incrementWorkload should increase workload count")
    void incrementWorkload_ShouldIncreaseCount() {
        // Given
        int initialWorkload = testUser.getCurrentWorkload();
        when(adminUserRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(adminUserRepository.save(any(AdminUser.class)))
            .thenReturn(testUser);

        // When
        AdminUser result = adminUserService.incrementWorkload(testUserId);

        // Then
        assertThat(testUser.getCurrentWorkload()).isEqualTo(initialWorkload + 1);
        verify(adminUserRepository).save(testUser);
    }

    @Test
    @DisplayName("decrementWorkload should decrease workload count")
    void decrementWorkload_ShouldDecreaseCount() {
        // Given
        int initialWorkload = testUser.getCurrentWorkload();
        when(adminUserRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));
        when(adminUserRepository.save(any(AdminUser.class)))
            .thenReturn(testUser);

        // When
        AdminUser result = adminUserService.decrementWorkload(testUserId);

        // Then
        assertThat(testUser.getCurrentWorkload()).isEqualTo(initialWorkload - 1);
        verify(adminUserRepository).save(testUser);
    }

    // ============================================================================
    // READ Operations Tests
    // ============================================================================

    @Test
    @DisplayName("findById should return user when found")
    void findById_WhenFound_ShouldReturnUser() {
        // Given
        when(adminUserRepository.findById(testUserId))
            .thenReturn(Optional.of(testUser));

        // When
        Optional<AdminUser> result = adminUserService.findById(testUserId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("findById should return empty when not found")
    void findById_WhenNotFound_ShouldReturnEmpty() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(adminUserRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());

        // When
        Optional<AdminUser> result = adminUserService.findById(nonExistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUsername should return user")
    void findByUsername_ShouldReturnUser() {
        // Given
        when(adminUserRepository.findByUsername("test-user"))
            .thenReturn(Optional.of(testUser));

        // When
        Optional<AdminUser> result = adminUserService.findByUsername("test-user");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("findByEmail should return user")
    void findByEmail_ShouldReturnUser() {
        // Given
        when(adminUserRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));

        // When
        Optional<AdminUser> result = adminUserService.findByEmail("test@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("getActiveUsers should return only active users")
    void getActiveUsers_ShouldReturnActiveOnly() {
        // Given
        List<AdminUser> activeUsers = List.of(testUser);
        when(adminUserRepository.findByIsActive(true))
            .thenReturn(activeUsers);

        // When
        List<AdminUser> result = adminUserService.getActiveUsers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("getUsersByRole should return users with specified role")
    void getUsersByRole_ShouldReturnUsersWithRole() {
        // Given
        List<AdminUser> reviewers = List.of(testUser);
        when(adminUserRepository.findByRole(AdminRole.REVIEWER))
            .thenReturn(reviewers);

        // When
        List<AdminUser> result = adminUserService.getUsersByRole(AdminRole.REVIEWER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(AdminRole.REVIEWER);
    }

    @Test
    @DisplayName("getActiveUsersByRole should return active users with role")
    void getActiveUsersByRole_ShouldReturnActiveUsersWithRole() {
        // Given
        List<AdminUser> activeReviewers = List.of(testUser);
        when(adminUserRepository.findByRoleAndIsActive(AdminRole.REVIEWER, true))
            .thenReturn(activeReviewers);

        // When
        List<AdminUser> result = adminUserService.getActiveUsersByRole(AdminRole.REVIEWER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(AdminRole.REVIEWER);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("getUsersBySpecialization should return users with specialization")
    void getUsersBySpecialization_ShouldReturnMatches() {
        // Given
        List<AdminUser> techSpecialists = List.of(testUser);
        when(adminUserRepository.findBySpecializationsContaining("technology"))
            .thenReturn(techSpecialists);

        // When
        List<AdminUser> result = adminUserService.getUsersBySpecialization("technology");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpecializations()).contains("technology");
    }

    @Test
    @DisplayName("findOptimalReviewer should return best match for specialization")
    void findOptimalReviewer_ShouldReturnBestMatch() {
        // Given
        when(adminUserRepository.findOptimalReviewerForSpecialization("academic"))
            .thenReturn(Optional.of(testUser));

        // When
        Optional<AdminUser> result = adminUserService.findOptimalReviewer("academic");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSpecializations()).contains("academic");
    }

    @Test
    @DisplayName("getAvailableReviewers should return non-overloaded reviewers")
    void getAvailableReviewers_ShouldReturnAvailable() {
        // Given
        List<AdminUser> available = List.of(testUser);
        when(adminUserRepository.findAvailableReviewers(5))
            .thenReturn(available);

        // When
        List<AdminUser> result = adminUserService.getAvailableReviewers(5);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentWorkload()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("getHighQualityReviewers should return reviewers with high approval rate")
    void getHighQualityReviewers_ShouldReturnQuality() {
        // Given
        List<AdminUser> highQuality = List.of(testUser);
        when(adminUserRepository.findHighQualityReviewers(0.7, 10))
            .thenReturn(highQuality);

        // When
        List<AdminUser> result = adminUserService.getHighQualityReviewers(0.7, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApprovalRate()).isGreaterThanOrEqualTo(0.7);
        assertThat(result.get(0).getCandidatesReviewed()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("getFastReviewers should return reviewers with low review time")
    void getFastReviewers_ShouldReturnFast() {
        // Given
        List<AdminUser> fastReviewers = List.of(testUser);
        when(adminUserRepository.findByAverageReviewTimeLessThan(40, 5))
            .thenReturn(fastReviewers);

        // When
        List<AdminUser> result = adminUserService.getFastReviewers(40, 5);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAverageReviewTimeMinutes()).isLessThanOrEqualTo(40);
    }

    @Test
    @DisplayName("getInactiveUsers should return users who haven't logged in recently")
    void getInactiveUsers_ShouldReturnInactive() {
        // Given
        List<AdminUser> inactive = List.of(testUser);
        when(adminUserRepository.findInactiveUsers(any(LocalDateTime.class)))
            .thenReturn(inactive);

        // When
        List<AdminUser> result = adminUserService.getInactiveUsers(30);

        // Then
        assertThat(result).hasSize(1);
        verify(adminUserRepository).findInactiveUsers(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("getUsersCreatedBetween should return users in date range")
    void getUsersCreatedBetween_ShouldReturnInRange() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<AdminUser> users = List.of(testUser);
        when(adminUserRepository.findUsersCreatedBetween(start, end))
            .thenReturn(users);

        // When
        List<AdminUser> result = adminUserService.getUsersCreatedBetween(start, end);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getWorkloadDistribution should return workload stats")
    void getWorkloadDistribution_ShouldReturnStats() {
        // Given
        List<AdminUserRepository.ReviewerWorkload> workloads = List.of();
        when(adminUserRepository.getReviewerWorkloadDistribution())
            .thenReturn(workloads);

        // When
        List<AdminUserRepository.ReviewerWorkload> result = adminUserService.getWorkloadDistribution();

        // Then
        assertThat(result).isNotNull();
        verify(adminUserRepository).getReviewerWorkloadDistribution();
    }

    @Test
    @DisplayName("getReviewerStatistics should return reviewer stats")
    void getReviewerStatistics_ShouldReturnStats() {
        // Given
        AdminUserRepository.ReviewerStats stats = new AdminUserRepository.ReviewerStats() {
            @Override
            public long getActiveReviewers() { return 5; }
            @Override
            public Double getAvgReviewsPerUser() { return 20.0; }
            @Override
            public Double getAvgApprovalRate() { return 0.75; }
            @Override
            public Double getAvgReviewTimeMinutes() { return 30.0; }
        };
        when(adminUserRepository.getReviewerStatistics())
            .thenReturn(stats);

        // When
        AdminUserRepository.ReviewerStats result = adminUserService.getReviewerStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActiveReviewers()).isEqualTo(5);
    }

    @Test
    @DisplayName("getActivitySummary should return activity summary")
    void getActivitySummary_ShouldReturnSummary() {
        // Given
        List<AdminUserRepository.AdminActivitySummary> summaries = List.of();
        when(adminUserRepository.getAdminActivitySummary(any(LocalDateTime.class)))
            .thenReturn(summaries);

        // When
        List<AdminUserRepository.AdminActivitySummary> result = adminUserService.getActivitySummary(7);

        // Then
        assertThat(result).isNotNull();
        verify(adminUserRepository).getAdminActivitySummary(any(LocalDateTime.class));
    }
}
