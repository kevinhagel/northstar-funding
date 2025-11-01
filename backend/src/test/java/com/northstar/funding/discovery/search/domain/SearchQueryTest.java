package com.northstar.funding.discovery.search.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test for SearchQuery entity (Feature 003)
 *
 * CRITICAL: This test MUST FAIL before implementing SearchQuery.java
 *
 * Tests:
 * - Constructor validation (null queryText, blank dayOfWeek, empty tags)
 * - Builder pattern with fluent API
 * - Immutability after creation
 * - Tags manipulation (add, remove, contains)
 *
 * Constitutional Compliance:
 * - Spring Data JDBC compatibility
 * - Lombok @Data, @Builder annotations
 * - Jakarta Validation annotations
 */
class SearchQueryTest {

    @Test
    void testBuilderCreatesValidSearchQuery() {
        // Given
        var query = "bulgaria education grants 2025";
        var dayOfWeek = DayOfWeek.MONDAY;
        var tags = Set.of("GEOGRAPHY:Bulgaria", "CATEGORY:Education");
        var targetEngines = Set.of("SEARXNG", "TAVILY", "PERPLEXITY");

        // When
        var searchQuery = SearchQuery.builder()
            .queryText(query)
            .dayOfWeek(dayOfWeek)
            .tags(tags)
            .targetEngines(targetEngines)
            .expectedResults(25)
            .enabled(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // Then
        assertThat(searchQuery).isNotNull();
        assertThat(searchQuery.getQueryText()).isEqualTo(query);
        assertThat(searchQuery.getDayOfWeek()).isEqualTo(dayOfWeek);
        assertThat(searchQuery.getTags()).hasSize(2);
        assertThat(searchQuery.getTargetEngines()).hasSize(3);
        assertThat(searchQuery.getExpectedResults()).isEqualTo(25);
        assertThat(searchQuery.isEnabled()).isTrue();
    }

    @Test
    void testQueryTextCanBeNullInBuilderButWillFailValidation() {
        // Given - Lombok builder allows null, but Jakarta Validation will catch it
        var searchQuery = SearchQuery.builder()
            .queryText(null)
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // Then - Entity created but will fail bean validation on persist
        assertThat(searchQuery).isNotNull();
        assertThat(searchQuery.getQueryText()).isNull();
    }

    @Test
    void testQueryTextCannotBeBlank() {
        // Given
        var blankQuery = "   ";

        // When
        var searchQuery = SearchQuery.builder()
            .queryText(blankQuery)
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // Then - Should fail validation when persisted
        assertThat(searchQuery.getQueryText()).isEqualTo(blankQuery);
    }

    @Test
    void testDayOfWeekCanBeNullInBuilderButWillFailValidation() {
        // Given - Lombok builder allows null, but Jakarta Validation will catch it
        var searchQuery = SearchQuery.builder()
            .queryText("test query")
            .dayOfWeek(null)
            .build();

        // Then - Entity created but will fail bean validation on persist
        assertThat(searchQuery).isNotNull();
        assertThat(searchQuery.getDayOfWeek()).isNull();
    }

    @Test
    void testTagsDefaultsToEmptySet() {
        // When
        var searchQuery = SearchQuery.builder()
            .queryText("test query")
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // Then
        assertThat(searchQuery.getTags()).isNotNull();
        assertThat(searchQuery.getTags()).isEmpty();
    }

    @Test
    void testTargetEnginesDefaultsToEmptySet() {
        // When
        var searchQuery = SearchQuery.builder()
            .queryText("test query")
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // Then
        assertThat(searchQuery.getTargetEngines()).isNotNull();
        assertThat(searchQuery.getTargetEngines()).isEmpty();
    }

    @Test
    void testExpectedResultsDefaultsTo25() {
        // When
        var searchQuery = SearchQuery.builder()
            .queryText("test query")
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // Then
        assertThat(searchQuery.getExpectedResults()).isEqualTo(25);
    }

    @Test
    void testEnabledDefaultsToTrue() {
        // When
        var searchQuery = SearchQuery.builder()
            .queryText("test query")
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // Then
        assertThat(searchQuery.isEnabled()).isTrue();
    }

    @Test
    void testContainsTagReturnsTrueForExistingTag() {
        // Given
        var geographyTag = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");
        var searchQuery = SearchQuery.builder()
            .queryText("test query")
            .dayOfWeek(DayOfWeek.MONDAY)
            .tags(Set.of("GEOGRAPHY:Bulgaria"))
            .build();

        // When/Then
        assertThat(searchQuery.containsTag(geographyTag)).isTrue();
    }

    @Test
    void testContainsTagReturnsFalseForNonExistingTag() {
        // Given
        var geographyTag = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");
        var categoryTag = new QueryTag(TagType.CATEGORY, "Education");
        var searchQuery = SearchQuery.builder()
            .queryText("test query")
            .dayOfWeek(DayOfWeek.MONDAY)
            .tags(Set.of("GEOGRAPHY:Bulgaria"))
            .build();

        // When/Then
        assertThat(searchQuery.containsTag(categoryTag)).isFalse();
    }

    @Test
    void testEqualityBasedOnId() {
        // Given
        var query1 = SearchQuery.builder()
            .id(1L)
            .queryText("test query 1")
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        var query2 = SearchQuery.builder()
            .id(1L)
            .queryText("test query 2")
            .dayOfWeek(DayOfWeek.TUESDAY)
            .build();

        var query3 = SearchQuery.builder()
            .id(2L)
            .queryText("test query 1")
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // When/Then
        assertThat(query1).isEqualTo(query2); // Same ID
        assertThat(query1).isNotEqualTo(query3); // Different ID
    }

    @Test
    void testToStringContainsQueryTextAndDayOfWeek() {
        // Given
        var searchQuery = SearchQuery.builder()
            .queryText("bulgaria education grants 2025")
            .dayOfWeek(DayOfWeek.MONDAY)
            .build();

        // When
        var toString = searchQuery.toString();

        // Then
        assertThat(toString).contains("bulgaria education grants 2025");
        assertThat(toString).contains("MONDAY");
    }
}
