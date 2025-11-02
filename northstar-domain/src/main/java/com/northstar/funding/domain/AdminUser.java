package com.northstar.funding.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin User Domain Entity
 * 
 * System administrators who review and validate funding source candidates
 * in the Human-AI Collaboration workflow.
 * 
 * Constitutional Requirements:
 * - Support for Kevin & Huw as primary admin users
 * - Review statistics for workload balancing
 * - Specialization-based assignment logic
 * - Spring Data JDBC entity (no ORM complexity)
 */

@Table("admin_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {

    // Primary Identity
    @Id
    @Column("user_id")
    private UUID userId;
    
    private String username;
    
    
    private String fullName;
    
    private String email;
    
    // Role & Authorization
    
    private AdminRole role;
    
    
    private Boolean isActive;
    
    // Audit Timestamps
    
    private LocalDateTime createdAt;
    
    
    private LocalDateTime lastLoginAt;
    
    // Review Performance Statistics (Workload Balancing)
    
    private Integer candidatesReviewed;
    
    
    private Integer averageReviewTimeMinutes;
    
    
    private Double approvalRate; // 0.0-1.0
    
    // Specialization & Assignment Logic  
    
    private Set<String> specializations; // Set of expertise areas
    
    
    private Integer currentWorkload; // Active assignments
    
    
    private Integer maxConcurrentAssignments;

    // Compatibility methods for tests
    public Duration getAverageReviewTime() {
        return averageReviewTimeMinutes != null ? Duration.ofMinutes(averageReviewTimeMinutes) : Duration.ZERO;
    }
    
    public void setAverageReviewTime(Duration averageReviewTime) {
        this.averageReviewTimeMinutes = averageReviewTime != null ? (int) averageReviewTime.toMinutes() : 0;
    }

    // Business methods for workload management
    public boolean canTakeNewAssignment() {
        return isActive != null && isActive && (currentWorkload != null && maxConcurrentAssignments != null && currentWorkload < maxConcurrentAssignments);
    }
    
    public void incrementWorkload() {
        if (canTakeNewAssignment()) {
            this.currentWorkload++;
        }
    }
    
    public void decrementWorkload() {
        if (currentWorkload != null && currentWorkload > 0) {
            this.currentWorkload--;
        }
    }
    
    public void updateReviewStatistics(boolean approved, int reviewTimeMinutes) {
        // Initialize defaults if null
        if (this.candidatesReviewed == null) this.candidatesReviewed = 0;
        if (this.approvalRate == null) this.approvalRate = 0.0;
        if (this.averageReviewTimeMinutes == null) this.averageReviewTimeMinutes = 0;
        
        // Update total reviewed count
        this.candidatesReviewed++;
        
        // Calculate new approval rate
        double currentApprovedCount = this.approvalRate * (this.candidatesReviewed - 1);
        if (approved) currentApprovedCount++;
        this.approvalRate = currentApprovedCount / this.candidatesReviewed;
        
        // Update average review time
        int totalMinutes = this.averageReviewTimeMinutes * (this.candidatesReviewed - 1);
        totalMinutes += reviewTimeMinutes;
        this.averageReviewTimeMinutes = totalMinutes / this.candidatesReviewed;
    }
}
