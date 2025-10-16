package com.northstar.funding.discovery.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Enhancement Record Value Object
 * 
 * Immutable record of manual improvements made by admin users for quality tracking.
 * Provides complete audit trail of human enhancements to AI-discovered candidates.
 * 
 * Key Features:
 * - Immutable audit trail (value object semantics)
 * - Track what was changed, when, and by whom
 * - Support quality metrics and process improvement
 * - Spring Data JDBC entity for persistence
 */
@Table("enhancement_record") 
public class EnhancementRecord {

    // Primary Identity
    @Id
    @Column("enhancement_id")
    private UUID enhancementId;
    
    @Column("candidate_id")
    private UUID candidateId; // Which candidate was enhanced
    
    @Column("enhanced_by")
    private UUID enhancedBy; // AdminUser who made changes
    
    @Column("enhanced_at")
    private LocalDateTime enhancedAt; // When changes made
    
    // Enhancement Classification
    @Column("enhancement_type")
    private EnhancementType enhancementType;
    
    @Column("field_name")
    private String fieldName; // Which field was modified
    
    @Column("old_value")
    private String oldValue; // Previous value (nullable)
    
    @Column("new_value")
    private String newValue; // New value
    
    private String notes; // Explanation of changes
    
    @Column("time_spent_minutes")
    private Integer timeSpentMinutes; // How long enhancement took
    
    @Column("review_complexity")
    private String reviewComplexity; // SIMPLE, MODERATE, COMPLEX

    // Default constructor
    public EnhancementRecord() {
        // Let database set enhanced_at via DEFAULT NOW() to avoid CHECK constraint issues
        this.timeSpentMinutes = 0;
        this.reviewComplexity = "SIMPLE"; // Default value matching database default
    }
    
    // Constructor for creating new enhancement records
    public EnhancementRecord(UUID candidateId, UUID enhancedBy, EnhancementType enhancementType,
                           String fieldName, String oldValue, String newValue, String notes) {
        this();
        this.candidateId = candidateId;
        this.enhancedBy = enhancedBy;
        this.enhancementType = enhancementType;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.notes = notes;
    }

    // Getters (no setters for immutability - except for persistence framework)
    public UUID getEnhancementId() {
        return enhancementId;
    }

    public void setEnhancementId(UUID enhancementId) {
        this.enhancementId = enhancementId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public UUID getEnhancedBy() {
        return enhancedBy;
    }

    public void setEnhancedBy(UUID enhancedBy) {
        this.enhancedBy = enhancedBy;
    }

    public LocalDateTime getEnhancedAt() {
        return enhancedAt;
    }

    public void setEnhancedAt(LocalDateTime enhancedAt) {
        this.enhancedAt = enhancedAt;
    }

    public EnhancementType getEnhancementType() {
        return enhancementType;
    }

    public void setEnhancementType(EnhancementType enhancementType) {
        this.enhancementType = enhancementType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(Integer timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }
    
    public String getReviewComplexity() {
        return reviewComplexity;
    }
    
    public void setReviewComplexity(String reviewComplexity) {
        this.reviewComplexity = reviewComplexity;
    }

    // Business methods for quality tracking
    public boolean isSignificantChange() {
        return oldValue == null || !oldValue.equals(newValue);
    }
    
    public boolean hasQualityNotes() {
        return notes != null && !notes.trim().isEmpty();
    }
    
    public boolean isTimeTracked() {
        return timeSpentMinutes != null && timeSpentMinutes > 0;
    }

    // Static factory methods for common enhancement types
    public static EnhancementRecord contactAdded(UUID candidateId, UUID enhancedBy, String contactInfo, String notes) {
        return new EnhancementRecord(candidateId, enhancedBy, EnhancementType.CONTACT_ADDED,
                                   "contactIntelligence", null, contactInfo, notes);
    }
    
    public static EnhancementRecord dataCorrected(UUID candidateId, UUID enhancedBy, String fieldName,
                                                String oldValue, String newValue, String notes) {
        return new EnhancementRecord(candidateId, enhancedBy, EnhancementType.DATA_CORRECTED,
                                   fieldName, oldValue, newValue, notes);
    }
    
    public static EnhancementRecord notesAdded(UUID candidateId, UUID enhancedBy, String additionalNotes) {
        return new EnhancementRecord(candidateId, enhancedBy, EnhancementType.NOTES_ADDED,
                                   "validationNotes", null, additionalNotes, "Added validation context");
    }
    
    public static EnhancementRecord duplicateMerged(UUID candidateId, UUID enhancedBy, String duplicateId, String notes) {
        return new EnhancementRecord(candidateId, enhancedBy, EnhancementType.DUPLICATE_MERGED,
                                   "duplicateOfCandidateId", null, duplicateId, notes);
    }

    @Override
    public String toString() {
        return "EnhancementRecord{" +
               "enhancementId=" + enhancementId +
               ", candidateId=" + candidateId +
               ", enhancementType=" + enhancementType +
               ", fieldName='" + fieldName + '\'' +
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
