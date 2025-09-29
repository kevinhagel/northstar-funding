package com.northstar.funding.discovery.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contact Intelligence Domain Entity
 * 
 * First-class entity for contact information associated with funding sources.
 * Constitutional Requirement: Contact Intelligence as highest value asset.
 * 
 * Key Features:
 * - PII Protection: email/phone encrypted at application layer
 * - Relationship Tracking: Decision authority and referral networks  
 * - Spring Data JDBC entity (no ORM complexity)
 * - Human-AI Collaboration: Manual contact validation workflows
 */
@Table("contact_intelligence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactIntelligence {

    // Primary Identity
    @Id
    @Column("contact_id")
    private UUID contactId;
    
    @Column("candidate_id")
    private UUID candidateId; // FK to FundingSourceCandidate
    
    // Contact Classification  
    @Column("contact_type")
    private ContactType contactType;
    
    @Column("authority_level")
    private AuthorityLevel decisionAuthority;
    
    // Personal Information
    @Column("full_name")
    private String fullName;
    
    private String title;
    private String organization;
    
    @Column("office_address")
    private String officeAddress;
    
    // Protected Contact Information (ENCRYPTED at application layer)
    // Constitutional Requirement: PII protection
    private String email; // Will be encrypted via application layer
    private String phone; // Will be encrypted via application layer
    
    // Communication Preferences & Patterns
    @Column("communication_preference")
    private String communicationPreference;
    
    @Column("last_contacted_at")
    private LocalDateTime lastContactedAt;
    
    @Column("response_pattern")
    private String responsePattern;
    
    @Column("referral_source")
    private String referralSource;
    
    // Validation & Status
    @Column("validated_at")
    private LocalDateTime validatedAt;
    
    @Column("is_active")
    private Boolean isActive;
    
    // Relationship Context (High-Value Intelligence)
    @Column("relationship_notes")
    private String relationshipNotes;
    
    @Column("referral_connections")
    private String referralConnections; // JSON-like structure for referral chains
    
    // Audit Timestamps
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    @Column("created_by")
    private UUID createdBy; // FK to AdminUser

}
