package com.northstar.funding.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contact Intelligence Domain Entity
 *
 * First-class entity for contact information associated with funding sources.
 * Highest value asset in the funding discovery system.
 *
 * Key Features:
 * - PII Protection: email/phone encrypted at application layer
 * - Relationship Tracking: Decision authority and referral networks
 * - Human-AI Collaboration: Manual contact validation workflows
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactIntelligence {

    // Primary Identity
    private UUID contactId;
    private UUID candidateId; // FK to FundingSourceCandidate

    // Contact Classification
    private ContactType contactType;
    private AuthorityLevel decisionAuthority;

    // Personal Information
    private String fullName;
    private String title;
    private String organization;
    private String officeAddress;

    // Protected Contact Information (ENCRYPTED at application layer)
    // PII protection requirement
    private String email; // Will be encrypted via application layer
    private String phone; // Will be encrypted via application layer

    // Communication Preferences & Patterns
    private String communicationPreference;
    private LocalDateTime lastContactedAt;
    private String responsePattern;
    private String referralSource;

    // Validation & Status
    private LocalDateTime validatedAt;
    private Boolean isActive;

    // Relationship Context (High-Value Intelligence)
    private String relationshipNotes;
    private String referralConnections; // JSON-like structure for referral chains

    // Audit Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy; // FK to AdminUser
}
