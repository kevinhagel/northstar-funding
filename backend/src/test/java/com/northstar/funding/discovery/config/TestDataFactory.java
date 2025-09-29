package com.northstar.funding.discovery.config;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.northstar.funding.discovery.domain.AdminRole;
import com.northstar.funding.discovery.domain.AdminUser;

/**
 * Test Data Factory for AdminUser Integration Tests
 * 
 * Provides fluent builder pattern for creating consistent test data
 * with sensible defaults and customization options.
 */
@Component
public class TestDataFactory {

    /**
     * Create a basic AdminUser builder with sensible defaults.
     * All required fields are pre-populated, optional fields can be customized.
     */
    public AdminUser.AdminUserBuilder adminUserBuilder() {
        return AdminUser.builder()
            .username("test.user." + System.currentTimeMillis()) // Unique username
            .fullName("Test User")
            .email("test.user." + System.currentTimeMillis() + "@northstar.com")
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
}
