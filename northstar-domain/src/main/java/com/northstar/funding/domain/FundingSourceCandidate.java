package com.northstar.funding.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Funding Source Candidate Domain Entity
 *
 * Represents a discovered funding opportunity pending human validation.
 * Pure domain entity with no infrastructure dependencies.
 *
 * Domain Principles:
 * - Domain-Driven Design with "Funding Sources" ubiquitous language
 * - Contact Intelligence as first-class entities (separate ContactIntelligence class)
 * - Human-AI Collaboration workflow support
 * - Aggregate root for funding discovery bounded context
 */
@Table("funding_source_candidate")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingSourceCandidate {

    // Primary Identity
    @Id
    private UUID candidateId;
    private CandidateStatus status;

    /**
     * AI-generated confidence score (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     */
    private BigDecimal confidenceScore;

    // Domain Relationship (for deduplication and quality tracking)
    private UUID domainId; // FK to domain table

    // Discovery Audit Trail
    private UUID discoverySessionId;
    private LocalDateTime discoveredAt;
    private LocalDateTime lastUpdatedAt;

    // Review Assignment
    private UUID assignedReviewerId; // nullable
    private LocalDateTime reviewStartedAt; // nullable

    // Approval/Rejection Tracking (Audit Trail)
    private UUID approvedBy; // nullable
    private LocalDateTime approvedAt; // nullable
    private UUID rejectedBy; // nullable
    private LocalDateTime rejectedAt; // nullable

    // Funding Source Information
    private String organizationName;
    private String programName;
    private String sourceUrl;
    private String description;

    // Financial Details
    private BigDecimal fundingAmountMin; // nullable
    private BigDecimal fundingAmountMax; // nullable
    private String currency; // EUR, USD, etc.

    // Application Information
    private LocalDate applicationDeadline; // nullable
    private String applicationProcess;

    // Geographic and Eligibility Information
    private List<String> geographicEligibility;
    private List<String> organizationTypes;
    private List<String> requirements;
    private Set<String> tags;

    // Discovery Metadata
    private String discoveryMethod;
    private String searchQuery;
    private SearchEngineType searchEngineSource; // Search engine that discovered this candidate
    private String metadataTitle; // Page title from search engine metadata
    private String extractedData; // Raw scraped data as JSON string
    private UUID duplicateOfCandidateId;

    // Validation and Enhancement
    private String validationNotes;
    private String rejectionReason; // nullable
}
