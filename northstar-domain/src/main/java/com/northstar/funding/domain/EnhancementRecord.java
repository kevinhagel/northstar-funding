package com.northstar.funding.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enhancement Record Domain Entity
 *
 * Immutable audit trail of AI suggestions and human improvements.
 * Constitutional requirement: Complete audit trail for human-AI collaboration.
 *
 * Key Features:
 * - Track AI suggestions vs human decisions
 * - Record LM Studio model and confidence
 * - Human approval workflow
 * - Immutable audit log (value object semantics)
 * - Spring Data JDBC entity
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancementRecord {

    // Primary Identity
    
    
    private UUID enhancementId;

    
    private UUID candidateId; // Which candidate was enhanced

    
    private UUID enhancedBy; // AdminUser who made changes (nullable for AI_SUGGESTED)

    
    private LocalDateTime enhancedAt; // When changes made

    // Enhancement Classification
    
    private EnhancementType enhancementType;

    
    private String fieldName; // Which field was modified

    
    private String originalValue; // Previous value (nullable)

    
    private String suggestedValue; // New value

    private String notes; // Explanation of changes

    // AI Tracking (Human-AI Collaboration)
    
    private String aiModel; // LM Studio model (e.g., "llama-3.1-8b")

    /**
     * AI confidence score (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     */
    
    private BigDecimal confidenceScore;

    
    private Boolean humanApproved; // Did human approve AI suggestion?

    
    private UUID approvedBy; // Who approved

    
    private LocalDateTime approvedAt; // When approved

    // Quality Metrics
    
    private Integer timeSpentMinutes; // How long enhancement took

    
    private String reviewComplexity; // SIMPLE, MODERATE, COMPLEX

    
    private String sourceReference; // URL, document reference

    
    private Double confidenceImprovement; // How much this improved candidate score

    
    private String validationMethod; // How enhancement was validated

    
    private Boolean aiAssistanceUsed; // Whether AI tools were used

    // Business methods for quality tracking
    public boolean isSignificantChange() {
        return originalValue == null || !originalValue.equals(suggestedValue);
    }

    public boolean hasQualityNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    public boolean isTimeTracked() {
        return timeSpentMinutes != null && timeSpentMinutes > 0;
    }

    public boolean isAiSuggestion() {
        return enhancementType == EnhancementType.AI_SUGGESTED;
    }

    public boolean requiresHumanApproval() {
        return isAiSuggestion() && (humanApproved == null || !humanApproved);
    }

    /**
     * Convenience method for checking if enhancement was human-approved.
     * Handles null safely by returning false if humanApproved is null.
     */
    public boolean isHumanApproved() {
        return Boolean.TRUE.equals(humanApproved);
    }

    @Override
    public String toString() {
        return "EnhancementRecord{" +
               "enhancementId=" + enhancementId +
               ", candidateId=" + candidateId +
               ", enhancementType=" + enhancementType +
               ", fieldName='" + fieldName + '\'' +
               ", aiModel='" + aiModel + '\'' +
               ", humanApproved=" + humanApproved +
               ", enhancedAt=" + enhancedAt +
               '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        EnhancementRecord that = (EnhancementRecord) obj;
        return enhancementId != null ? enhancementId.equals(that.enhancementId) : that.enhancementId == null;
    }

    @Override
    public int hashCode() {
        return enhancementId != null ? enhancementId.hashCode() : 0;
    }
}
