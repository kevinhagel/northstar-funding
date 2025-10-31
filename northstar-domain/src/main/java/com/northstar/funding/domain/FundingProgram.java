package com.northstar.funding.domain;

import java.math.BigDecimal;
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
 * Funding Program Domain Entity
 *
 * Represents a specific funding opportunity hosted by an organization.
 * Multiple programs can exist under same organization/domain.
 *
 * Hierarchy:
 * Organization → Domain → FundingPrograms → URLs
 *
 * Example:
 * - Organization: "America for Bulgaria Foundation"
 * - Domain: "us-bulgaria.org"
 * - Program 1: "Education Grant 2025" @ us-bulgaria.org/education-grant
 * - Program 2: "Healthcare Grant 2025" @ us-bulgaria.org/healthcare-grant
 *
 * Two-Level Judging:
 * 1. Organization-level: Judge domain homepage (parent Organization entity)
 * 2. Program-level: Judge each program URL separately (this entity)
 *
 * Deduplication Logic:
 * - Same domain + same URL + same day = Skip (duplicate)
 * - Same domain + different URL = Process (different program)
 */
@Table("funding_program")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingProgram {

    // Primary Identity
    @Id
    @Column("program_id")
    private UUID programId;

    /**
     * Parent organization that hosts this program
     */
    private UUID organizationId;

    /**
     * Associated domain (FK to Domain table)
     * Same as organization's domain
     */
    private String domain;

    // Program Details

    /**
     * Program name/title
     * Example: "Education Grant 2025"
     */
    private String programName;

    /**
     * Full URL to program page
     * Example: "https://us-bulgaria.org/education-grant"
     */
    private String programUrl;

    /**
     * Program description/summary
     */
    private String description;

    // Eligibility & Requirements

    /**
     * Who can apply for this program
     * Example: "Bulgarian NGOs focusing on education"
     */
    private String eligibilityCriteria;

    /**
     * Geographic restrictions
     * Example: ["Bulgaria", "Balkans"]
     */
    private String geographicScope;

    /**
     * Funding amount or range
     * Example: "€10,000 - €50,000"
     */
    private String fundingAmount;

    // Deadlines & Timing

    /**
     * Application deadline
     */
    private LocalDateTime applicationDeadline;

    /**
     * When funding is awarded
     */
    private LocalDateTime fundingPeriod;

    /**
     * Is this a recurring program?
     */
    private Boolean isRecurring;

    /**
     * Recurrence pattern if applicable
     * Example: "Annual", "Quarterly"
     */
    private String recurrencePattern;

    // Discovery Metadata

    /**
     * When this program was first discovered
     */
    private LocalDateTime discoveredAt;

    /**
     * Discovery session that found this program
     */
    private UUID discoverySessionId;

    /**
     * Search result that led to this program
     */
    private UUID searchResultId;

    /**
     * Confidence score from program-level judging (0.00-1.00)
     */
    private BigDecimal programConfidence;

    /**
     * Was this program judged as a valid funding opportunity?
     */
    private Boolean isValidFundingOpportunity;

    // Status & Tracking

    /**
     * Current status of this program
     */
    @Builder.Default
    private ProgramStatus status = ProgramStatus.DISCOVERED;

    /**
     * Last time program metadata was refreshed
     */
    private LocalDateTime lastRefreshedAt;

    /**
     * Is this program still active/accepting applications?
     */
    private Boolean isActive;

    /**
     * Link to parent FundingSourceCandidate (if created)
     */
    private UUID candidateId;

    // Admin Notes

    /**
     * Admin notes about this program
     */
    private String notes;
}
