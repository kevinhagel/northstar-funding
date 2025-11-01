package com.northstar.funding.discovery.search.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Query tag record for categorizing search queries (Feature 003)
 *
 * Immutable value object representing a single metadata tag on a search query.
 * Tags are stored as JSONB in PostgreSQL for flexible querying and filtering.
 *
 * Example tags:
 * - QueryTag(GEOGRAPHY, "Bulgaria")
 * - QueryTag(CATEGORY, "Education")
 * - QueryTag(AUTHORITY, "EU")
 *
 * Constitutional Compliance:
 * - Java record (immutable value object)
 * - Compact constructor for validation
 * - Jackson JSON annotations for serialization
 * - Defensive programming (null/blank checks)
 *
 * @param type The tag type (GEOGRAPHY, CATEGORY, AUTHORITY)
 * @param value The tag value (e.g., "Bulgaria", "Education", "EU")
 */
public record QueryTag(
    @JsonProperty("type") TagType type,
    @JsonProperty("value") String value
) {
    /**
     * Compact constructor with validation
     *
     * @throws NullPointerException if type or value is null
     * @throws IllegalArgumentException if value is blank
     */
    public QueryTag {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be blank");
        }
    }

    /**
     * Jackson deserialization constructor
     */
    @JsonCreator
    public static QueryTag of(
        @JsonProperty("type") TagType type,
        @JsonProperty("value") String value
    ) {
        return new QueryTag(type, value);
    }
}
