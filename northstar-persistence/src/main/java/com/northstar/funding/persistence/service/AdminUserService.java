package com.northstar.funding.persistence.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.domain.AdminRole;
import com.northstar.funding.domain.AdminUser;
import com.northstar.funding.persistence.repository.AdminUserRepository;

/**
 * Service layer for AdminUser entity operations.
 *
 * Provides business logic and transaction management for admin user management,
 * authentication, performance tracking, and reviewer assignment.
 *
 * This is the public API for external modules to interact with AdminUser persistence.
 */
@Service
@Transactional
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;

    public AdminUserService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    // ============================================================================
    // CREATE & UPDATE Operations
    // ============================================================================

    /**
     * Create a new admin user.
     *
     * @param adminUser the admin user to create
     * @return the created AdminUser
     */
    public AdminUser createUser(AdminUser adminUser) {
        // Set defaults if not provided
        if (adminUser.getIsActive() == null) {
            adminUser.setIsActive(true);
        }
        if (adminUser.getCreatedAt() == null) {
            adminUser.setCreatedAt(LocalDateTime.now());
        }
        if (adminUser.getCandidatesReviewed() == null) {
            adminUser.setCandidatesReviewed(0);
        }
        if (adminUser.getCurrentWorkload() == null) {
            adminUser.setCurrentWorkload(0);
        }
        if (adminUser.getAverageReviewTimeMinutes() == null) {
            adminUser.setAverageReviewTimeMinutes(0);
        }
        if (adminUser.getApprovalRate() == null) {
            adminUser.setApprovalRate(0.0);
        }

        return adminUserRepository.save(adminUser);
    }

    /**
     * Update user login timestamp.
     *
     * @param userId the user ID
     * @return updated AdminUser
     */
    public AdminUser recordLogin(UUID userId) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setLastLoginAt(LocalDateTime.now());
        return adminUserRepository.save(user);
    }

    /**
     * Update review statistics after a review is completed.
     *
     * @param userId the user ID
     * @param approved whether the candidate was approved
     * @param reviewDuration the duration of the review
     * @return updated AdminUser
     */
    public AdminUser updateReviewStatistics(UUID userId, boolean approved, Duration reviewDuration) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.updateReviewStatistics(approved, (int) reviewDuration.toMinutes());
        return adminUserRepository.save(user);
    }

    /**
     * Activate or deactivate a user.
     *
     * @param userId the user ID
     * @param isActive the new active status
     * @return updated AdminUser
     */
    public AdminUser setActive(UUID userId, boolean isActive) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setIsActive(isActive);
        return adminUserRepository.save(user);
    }

    /**
     * Increment user's workload when assigning a candidate.
     *
     * @param userId the user ID
     * @return updated AdminUser
     */
    public AdminUser incrementWorkload(UUID userId) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.incrementWorkload();
        return adminUserRepository.save(user);
    }

    /**
     * Decrement user's workload when a candidate review is completed.
     *
     * @param userId the user ID
     * @return updated AdminUser
     */
    public AdminUser decrementWorkload(UUID userId) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.decrementWorkload();
        return adminUserRepository.save(user);
    }

    // ============================================================================
    // READ Operations
    // ============================================================================

    /**
     * Find user by ID.
     *
     * @param userId the user ID
     * @return Optional of AdminUser
     */
    @Transactional(readOnly = true)
    public Optional<AdminUser> findById(UUID userId) {
        return adminUserRepository.findById(userId);
    }

    /**
     * Find user by username (for authentication).
     *
     * @param username the username
     * @return Optional of AdminUser
     */
    @Transactional(readOnly = true)
    public Optional<AdminUser> findByUsername(String username) {
        return adminUserRepository.findByUsername(username);
    }

    /**
     * Find user by email (for password reset).
     *
     * @param email the email
     * @return Optional of AdminUser
     */
    @Transactional(readOnly = true)
    public Optional<AdminUser> findByEmail(String email) {
        return adminUserRepository.findByEmail(email);
    }

    /**
     * Get all active users.
     *
     * @return list of active users
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getActiveUsers() {
        return adminUserRepository.findByIsActive(true);
    }

    /**
     * Get users by role.
     *
     * @param role the admin role
     * @return list of users
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getUsersByRole(AdminRole role) {
        return adminUserRepository.findByRole(role);
    }

    /**
     * Get active users by role.
     *
     * @param role the admin role
     * @return list of active users
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getActiveUsersByRole(AdminRole role) {
        return adminUserRepository.findByRoleAndIsActive(role, true);
    }

    /**
     * Find users with a specific specialization.
     *
     * @param specialization the specialization
     * @return list of users sorted by performance
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getUsersBySpecialization(String specialization) {
        return adminUserRepository.findBySpecializationsContaining(specialization);
    }

    /**
     * Find optimal reviewer for a specialization.
     *
     * @param specialization the specialization
     * @return Optional of best matching reviewer
     */
    @Transactional(readOnly = true)
    public Optional<AdminUser> findOptimalReviewer(String specialization) {
        return adminUserRepository.findOptimalReviewerForSpecialization(specialization);
    }

    /**
     * Find available reviewers (not overloaded).
     *
     * @param maxWorkload maximum workload threshold
     * @return list of available reviewers
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getAvailableReviewers(int maxWorkload) {
        return adminUserRepository.findAvailableReviewers(maxWorkload);
    }

    /**
     * Get high-quality reviewers (high approval rate).
     *
     * @param minApprovalRate minimum approval rate (0.0-1.0)
     * @param minReviews minimum number of reviews completed
     * @return list of high-quality reviewers
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getHighQualityReviewers(double minApprovalRate, int minReviews) {
        return adminUserRepository.findHighQualityReviewers(minApprovalRate, minReviews);
    }

    /**
     * Get fast reviewers (low average review time).
     *
     * @param maxMinutes maximum average review time in minutes
     * @param minReviews minimum number of reviews completed
     * @return list of fast reviewers
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getFastReviewers(int maxMinutes, int minReviews) {
        return adminUserRepository.findByAverageReviewTimeLessThan(maxMinutes, minReviews);
    }

    /**
     * Find users who haven't logged in recently.
     *
     * @param daysInactive number of days without login
     * @return list of inactive users
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getInactiveUsers(int daysInactive) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysInactive);
        return adminUserRepository.findInactiveUsers(threshold);
    }

    /**
     * Find users created in a date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of users
     */
    @Transactional(readOnly = true)
    public List<AdminUser> getUsersCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return adminUserRepository.findUsersCreatedBetween(startDate, endDate);
    }

    /**
     * Get reviewer workload distribution for load balancing.
     *
     * @return list of reviewer workload stats
     */
    @Transactional(readOnly = true)
    public List<AdminUserRepository.ReviewerWorkload> getWorkloadDistribution() {
        return adminUserRepository.getReviewerWorkloadDistribution();
    }

    /**
     * Get reviewer statistics for dashboard.
     *
     * @return reviewer statistics
     */
    @Transactional(readOnly = true)
    public AdminUserRepository.ReviewerStats getReviewerStatistics() {
        return adminUserRepository.getReviewerStatistics();
    }

    /**
     * Get admin activity summary.
     *
     * @param daysRecent number of days to consider "recently active"
     * @return list of admin activity summaries
     */
    @Transactional(readOnly = true)
    public List<AdminUserRepository.AdminActivitySummary> getActivitySummary(int daysRecent) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysRecent);
        return adminUserRepository.getAdminActivitySummary(threshold);
    }
}
