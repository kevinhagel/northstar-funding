package com.northstar.funding.discovery.search.domain;

/**
 * Tag types for categorizing search queries (Feature 003)
 *
 * Tags provide metadata about search queries for analytics and organization:
 * - GEOGRAPHY: Geographic focus (e.g., "Bulgaria", "Eastern Europe", "Balkans")
 * - CATEGORY: Funding category (e.g., "Education", "Infrastructure", "STEM")
 * - AUTHORITY: Granting authority (e.g., "EU", "World Bank", "UNESCO")
 *
 * Constitutional Compliance:
 * - Simple enum for JSON/JSONB serialization
 * - No complex business logic (DDD value object)
 * - MVP taxonomy (expandable in Feature 004)
 */
public enum TagType {
    /**
     * Geographic focus of the search query
     * Examples: "Bulgaria", "Eastern Europe", "Balkans", "Romania"
     */
    GEOGRAPHY,

    /**
     * Funding category or domain
     * Examples: "Education", "Infrastructure", "STEM", "Healthcare"
     */
    CATEGORY,

    /**
     * Granting authority or organization type
     * Examples: "EU", "World Bank", "UNESCO", "Government"
     */
    AUTHORITY
}
