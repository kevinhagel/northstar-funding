package com.northstar.funding.querygeneration.model;

import com.northstar.funding.domain.*;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

/**
 * Request object for query generation.
 *
 * <p>Immutable value object containing all parameters needed to generate
 * search queries for a specific search engine.
 */
@Value
@Builder
public class QueryGenerationRequest {
    /**
     * Target search engine.
     * Must not be null.
     */
    SearchEngineType searchEngine;

    /**
     * Funding categories to target in queries.
     * Must not be null or empty.
     */
    Set<FundingSearchCategory> categories;

    /**
     * Geographic scope for funding sources.
     * Must not be null.
     */
    GeographicScope geographic;

    /**
     * Maximum number of queries to generate.
     * Valid range: 1-50 (configurable limits).
     */
    int maxQueries;

    /**
     * Discovery session identifier.
     * Must not be null.
     */
    UUID sessionId;

    // ========================================
    // OPTIONAL FIELDS (Feature 005 - NEW)
    // ========================================

    /**
     * WHO provides funding (optional).
     * Adds source-specific keywords to queries.
     */
    FundingSourceType sourceType;

    /**
     * HOW funding is distributed (optional).
     * Adds mechanism-specific keywords to queries.
     */
    FundingMechanism mechanism;

    /**
     * Funding amount range (optional).
     * Adds scale-specific keywords to queries.
     */
    ProjectScale projectScale;

    /**
     * WHO benefits from funding (optional).
     * Can specify multiple beneficiary populations.
     * Adds beneficiary-specific keywords to queries.
     */
    Set<BeneficiaryPopulation> beneficiaries;

    /**
     * WHAT TYPE of organization receives funding (optional).
     * Adds recipient-specific keywords to queries.
     */
    RecipientOrganizationType recipientType;

    /**
     * User's preferred language (optional).
     * For future translation service integration.
     * NOTE: Translation NOT implemented yet.
     */
    QueryLanguage userLanguage;

    /**
     * Languages to search in (optional).
     * For future multi-language query generation.
     * NOTE: Translation NOT implemented yet.
     */
    Set<QueryLanguage> searchLanguages;

    /**
     * Validates this request.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (searchEngine == null) {
            throw new IllegalArgumentException("Search engine is required");
        }
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("At least one funding category is required");
        }
        if (geographic == null) {
            throw new IllegalArgumentException("Geographic scope is required");
        }
        if (maxQueries < 1 || maxQueries > 50) {
            throw new IllegalArgumentException("Max queries must be between 1 and 50");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("Valid session ID is required");
        }
    }
}
