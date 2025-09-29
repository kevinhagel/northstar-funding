package com.northstar.funding.discovery.infrastructure;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.northstar.funding.discovery.domain.AdminRole;
import com.northstar.funding.discovery.domain.AdminUser;

/**
 * Admin User Repository
 * 
 * Spring Data JDBC repository for AdminUser entity.
 * Critical for user management and reviewer assignment workflows.
 * 
 * Key Features:
 * - Authentication and authorization support
 * - Performance metrics tracking for review productivity
 * - Specialization-based user discovery for optimal assignment
 * - Admin workflow coordination and user management
 */
@Repository
public interface AdminUserRepository extends CrudRepository<AdminUser, UUID> {

    /**
     * Find user by username for authentication
     * Primary login query - must be unique
     */
    Optional<AdminUser> findByUsername(String username);
    
    /**
     * Find user by email for password reset flows
     */
    Optional<AdminUser> findByEmail(String email);
    
    /**
     * Find active admin users only
     * Used for assignment and workflow operations
     */
    List<AdminUser> findByIsActive(Boolean isActive);
    
    /**
     * Find users by role for admin management
     */
    List<AdminUser> findByRole(AdminRole role);
    
    /**
     * Find active users by role for operational queries
     */
    List<AdminUser> findByRoleAndIsActive(AdminRole role, Boolean isActive);
    
    /**
     * Find users with specific specialization for intelligent assignment
     */
    @Query("""
        SELECT * FROM admin_user 
        WHERE :specialization = ANY(specializations)
        AND is_active = true
        ORDER BY approval_rate DESC, average_review_time_minutes ASC
    """)
    List<AdminUser> findBySpecializationsContaining(@Param("specialization") String specialization);
    
    /**
     * Find most productive reviewers (high review count)
     */
    List<AdminUser> findByCandidatesReviewedGreaterThan(Integer threshold);
    
    /**
     * Find fast reviewers (low average review time in minutes)
     * @param maxMinutes Maximum average review time in minutes
     * @param minReviews Minimum number of reviews completed
     */
    @Query("""
        SELECT * FROM admin_user 
        WHERE average_review_time_minutes < :maxMinutes
        AND candidates_reviewed > :minReviews
        AND is_active = true
        ORDER BY average_review_time_minutes ASC
    """)
    List<AdminUser> findByAverageReviewTimeLessThan(@Param("maxMinutes") Integer maxMinutes, @Param("minReviews") Integer minReviews);
    
    /**
     * Find users with high approval rates for quality assignments
     */
    @Query("""
        SELECT * FROM admin_user 
        WHERE approval_rate >= :minRate
        AND candidates_reviewed >= :minReviews
        AND is_active = true
        ORDER BY approval_rate DESC, candidates_reviewed DESC
    """)
    List<AdminUser> findHighQualityReviewers(
        @Param("minRate") Double minApprovalRate,
        @Param("minReviews") Integer minReviews
    );
    
    /**
     * Find optimal reviewer for a candidate based on specialization and performance
     */
    @Query("""
        SELECT * FROM admin_user 
        WHERE is_active = true
        AND role = 'REVIEWER'
        AND (
            :specialization = ANY(specializations) OR
            'general' = ANY(specializations)
        )
        ORDER BY 
            CASE 
                WHEN :specialization = ANY(specializations) THEN 1 
                ELSE 2 
            END,
            (candidates_reviewed * approval_rate) DESC,
            average_review_time_minutes ASC
        LIMIT 1
    """)
    Optional<AdminUser> findOptimalReviewerForSpecialization(@Param("specialization") String specialization);
    
    /**
     * Find reviewers with current availability (not overloaded)
     */
    @Query("""
        SELECT au.* FROM admin_user au
        LEFT JOIN (
            SELECT assigned_reviewer_id, COUNT(*) as current_load
            FROM funding_source_candidate 
            WHERE status IN ('PENDING_REVIEW', 'IN_REVIEW')
            GROUP BY assigned_reviewer_id
        ) workload ON au.user_id = workload.assigned_reviewer_id
        WHERE au.is_active = true
        AND au.role = 'REVIEWER'
        AND COALESCE(workload.current_load, 0) < :maxWorkload
        ORDER BY COALESCE(workload.current_load, 0), au.approval_rate DESC
    """)
    List<AdminUser> findAvailableReviewers(@Param("maxWorkload") Integer maxWorkload);
    
    /**
     * Get reviewer performance statistics for dashboard
     */
    @Query("""
        SELECT 
            COUNT(*) as active_reviewers,
            AVG(candidates_reviewed) as avg_reviews_per_user,
            AVG(approval_rate) as avg_approval_rate,
            AVG(average_review_time_minutes) as avg_review_time_minutes
        FROM admin_user 
        WHERE is_active = true AND role = 'REVIEWER'
    """)
    ReviewerStats getReviewerStatistics();
    
    /**
     * Find users who haven't logged in recently for account management
     */
    @Query("""
        SELECT * FROM admin_user 
        WHERE is_active = true
        AND (last_login_at IS NULL OR last_login_at < :threshold)
        ORDER BY 
            CASE WHEN last_login_at IS NULL THEN 0 ELSE 1 END,
            last_login_at ASC
    """)
    List<AdminUser> findInactiveUsers(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Find users created in a specific time range for audit
     */
    @Query("""
        SELECT * FROM admin_user 
        WHERE created_at BETWEEN :startDate AND :endDate
        ORDER BY created_at DESC
    """)
    List<AdminUser> findUsersCreatedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Get workload distribution for load balancing
     */
    @Query("""
        SELECT 
            au.user_id,
            au.username,
            au.full_name,
            COALESCE(workload.current_assignments, 0) as current_load
        FROM admin_user au
        LEFT JOIN (
            SELECT assigned_reviewer_id, COUNT(*) as current_assignments
            FROM funding_source_candidate 
            WHERE status IN ('PENDING_REVIEW', 'IN_REVIEW')
            GROUP BY assigned_reviewer_id
        ) workload ON au.user_id = workload.assigned_reviewer_id
        WHERE au.is_active = true AND au.role = 'REVIEWER'
        ORDER BY current_load ASC, au.approval_rate DESC
    """)
    List<ReviewerWorkload> getReviewerWorkloadDistribution();
    
    /**
     * Update user statistics after review completion
     */
    @Query("""
        UPDATE admin_user 
        SET 
            candidates_reviewed = candidates_reviewed + 1,
            approval_rate = :newApprovalRate,
            average_review_time_minutes = :newAverageMinutes
        WHERE user_id = :userId
    """)
    void updateReviewStatistics(
        @Param("userId") UUID userId,
        @Param("newApprovalRate") Double newApprovalRate,
        @Param("newAverageMinutes") Integer newAverageMinutes
    );
    
    /**
     * Find users by multiple specializations (intersection)
     */
    @Query("""
        SELECT * FROM admin_user 
        WHERE is_active = true
        AND (:spec1 = ANY(specializations))
        AND (:spec2 = ANY(specializations))
        ORDER BY approval_rate DESC
    """)
    List<AdminUser> findByMultipleSpecializations(
        @Param("spec1") String specialization1,
        @Param("spec2") String specialization2
    );
    
    /**
     * Get admin user activity summary for reporting
     */
    @Query("""
        SELECT 
            role,
            COUNT(*) as user_count,
            COUNT(*) FILTER (WHERE last_login_at > :recentThreshold) as recently_active,
            AVG(candidates_reviewed) as avg_reviews
        FROM admin_user 
        WHERE is_active = true
        GROUP BY role
        ORDER BY role
    """)
    List<AdminActivitySummary> getAdminActivitySummary(@Param("recentThreshold") LocalDateTime recentThreshold);
    
    /**
     * Inner interfaces for statistics projections
     */
    interface ReviewerStats {
        long getActiveReviewers();
        Double getAvgReviewsPerUser();
        Double getAvgApprovalRate();
        Double getAvgReviewTimeMinutes();
    }
    
    interface ReviewerWorkload {
        UUID getUserId();
        String getUsername();
        String getFullName();
        Integer getCurrentLoad();
    }
    
    interface AdminActivitySummary {
        AdminRole getRole();
        Long getUserCount();
        Long getRecentlyActive();
        Double getAvgReviews();
    }
}
