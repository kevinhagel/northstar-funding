package com.northstar.funding.discovery.search.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.*;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Search query entity for hardcoded query library (Feature 003)
 *
 * Represents a single search query that is executed on specific days of the week.
 * Queries are organized by day and tagged with metadata for analytics.
 *
 * MVP Approach:
 * - Hardcoded queries in application.yml (5-10 per day)
 * - Manual curation based on analytics
 * - AI-powered query generation deferred to Feature 004
 *
 * Constitutional Compliance:
 * - Spring Data JDBC entity
 * - Lombok @Data, @Builder for boilerplate reduction
 * - Jakarta Validation annotations
 * - JSONB tags and target_engines in PostgreSQL
 *
 * @author NorthStar Funding Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("search_queries")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SearchQuery {

    /**
     * Primary key (database-generated)
     */
    @Id
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Search query text
     * Examples: "bulgaria education grants 2025", "eastern europe infrastructure funding"
     */
    @NotNull(message = "queryText cannot be null")
    @Size(min = 1, max = 500, message = "queryText must be between 1 and 500 characters")
    private String queryText;

    /**
     * Day of week when this query should be executed
     * Allows distributing queries across the week for continuous discovery
     */
    @NotNull(message = "dayOfWeek cannot be null")
    private DayOfWeek dayOfWeek;

    /**
     * Metadata tags for categorizing and filtering queries
     * Stored as TEXT[] in PostgreSQL with format "TYPE:value"
     *
     * Format: "GEOGRAPHY:Bulgaria", "CATEGORY:Education", "AUTHORITY:EU"
     *
     * Use getTags() to get parsed QueryTag objects
     */
    @Builder.Default
    @Column("tags")
    private Set<String> tags = new HashSet<>();

    /**
     * Target search engines for this query
     * Different queries may be better suited for different engines
     *
     * Stored as TEXT[] array in PostgreSQL
     */
    @Builder.Default
    @Column("target_engines")
    private Set<String> targetEngines = new HashSet<>();

    /**
     * Expected number of results (1-100)
     * Used for analytics to detect underperforming queries
     */
    @Builder.Default
    @Min(value = 1, message = "expectedResults must be at least 1")
    @Max(value = 100, message = "expectedResults must be at most 100")
    private Integer expectedResults = 25;

    /**
     * Whether this query is currently active in nightly execution
     * Allows temporarily disabling underperforming queries
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * When this query was created
     */
    private Instant createdAt;

    /**
     * When this query was last updated
     */
    private Instant updatedAt;

    /**
     * Get parsed QueryTag objects from the tags strings
     *
     * @return Set of QueryTag objects parsed from the tag strings
     */
    public Set<QueryTag> getParsedTags() {
        return tags.stream()
            .map(this::parseTag)
            .collect(Collectors.toSet());
    }

    /**
     * Get parsed SearchEngineType objects from the target engine strings
     *
     * @return Set of SearchEngineType objects parsed from the engine strings
     */
    public Set<SearchEngineType> getParsedTargetEngines() {
        return targetEngines.stream()
            .map(SearchEngineType::valueOf)
            .collect(Collectors.toSet());
    }

    /**
     * Add a tag to this query
     *
     * @param tag The tag to add
     */
    public void addTag(QueryTag tag) {
        Objects.requireNonNull(tag, "tag cannot be null");
        tags.add(formatTag(tag));
    }

    /**
     * Add a target engine to this query
     *
     * @param engineType The engine type to add
     */
    public void addTargetEngine(SearchEngineType engineType) {
        Objects.requireNonNull(engineType, "engineType cannot be null");
        targetEngines.add(engineType.name());
    }

    /**
     * Check if this query contains a specific tag
     *
     * @param tag The tag to check for
     * @return true if the tag exists, false otherwise
     */
    public boolean containsTag(QueryTag tag) {
        Objects.requireNonNull(tag, "tag cannot be null");
        return tags.contains(formatTag(tag));
    }

    // Helper methods

    private QueryTag parseTag(String tagString) {
        String[] parts = tagString.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid tag format: " + tagString + " (expected TYPE:value)");
        }
        TagType type = TagType.valueOf(parts[0].toUpperCase());
        String value = parts[1];
        return new QueryTag(type, value);
    }

    private String formatTag(QueryTag tag) {
        return tag.type().name() + ":" + tag.value();
    }
}
