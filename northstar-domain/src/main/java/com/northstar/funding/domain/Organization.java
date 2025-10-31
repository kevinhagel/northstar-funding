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
 * Organization Domain Entity
 *
 * Represents a funding organization discovered from domain homepage.
 * Organization owns a domain and hosts multiple funding programs.
 *
 * Hierarchy:
 * Organization → Domain → FundingPrograms → URLs
 *
 * Example:
 * - Organization: "America for Bulgaria Foundation"
 * - Domain: "us-bulgaria.org"
 * - Programs: "Education Grant 2025", "Healthcare Grant 2025"
 *
 * Two-Level Judging:
 * 1. Organization-level: Judge domain homepage once
 * 2. Program-level: Judge each program URL separately
 */
@Table("organization")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    // Primary Identity
    @Id
    @Column("organization_id")
    private UUID organizationId;

    /**
     * Organization name from homepage
     * Example: "America for Bulgaria Foundation"
     */
    private String name;

    /**
     * Associated domain (FK to Domain table)
     * Example: "us-bulgaria.org"
     */
    private String domain;

    // Organization Details

    /**
     * Mission statement or description
     */
    private String mission;

    /**
     * Geographic focus areas
     * Example: ["Bulgaria", "Balkans", "Eastern Europe"]
     */
    private String geographicFocus;

    /**
     * Types of funding provided
     * Example: ["Education", "Healthcare", "Infrastructure"]
     */
    private String fundingTypes;

    // Contact Information

    /**
     * Primary contact email
     */
    private String contactEmail;

    /**
     * Primary contact phone
     */
    private String contactPhone;

    /**
     * Physical office address
     */
    private String officeAddress;

    // Discovery Metadata

    /**
     * When this organization was first discovered
     */
    private LocalDateTime discoveredAt;

    /**
     * Discovery session that found this organization
     */
    private UUID discoverySessionId;

    /**
     * URL of homepage where organization was discovered
     */
    private String homepageUrl;

    /**
     * Confidence score from organization-level judging (0.00-1.00)
     */
    private java.math.BigDecimal organizationConfidence;

    /**
     * Was this organization judged as a valid funding source?
     */
    private Boolean isValidFundingSource;

    // Tracking

    /**
     * Number of funding programs discovered from this organization
     */
    private Integer programCount;

    /**
     * Last time organization metadata was refreshed
     */
    private LocalDateTime lastRefreshedAt;

    /**
     * Is this organization still active?
     */
    private Boolean isActive;

    // Admin Notes

    /**
     * Admin notes about this organization
     */
    private String notes;
}
