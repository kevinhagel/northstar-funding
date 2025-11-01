package com.northstar.funding.discovery.search.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test for QueryTag record (Feature 003)
 *
 * CRITICAL: This test MUST FAIL before implementing QueryTag.java
 *
 * Tests:
 * - Record validation (null type, null value, blank value)
 * - Equality and hashCode for Set membership
 * - JSON serialization/deserialization
 *
 * Constitutional Compliance:
 * - Java record with compact constructor
 * - Jackson JSON annotations
 * - Immutable value object
 */
class QueryTagTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testRecordCreatesValidQueryTag() {
        // Given/When
        var tag = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");

        // Then
        assertThat(tag).isNotNull();
        assertThat(tag.type()).isEqualTo(TagType.GEOGRAPHY);
        assertThat(tag.value()).isEqualTo("Bulgaria");
    }

    @Test
    void testTypeCannotBeNull() {
        // Given/When/Then
        assertThatThrownBy(() -> new QueryTag(null, "Bulgaria"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("type");
    }

    @Test
    void testValueCannotBeNull() {
        // Given/When/Then
        assertThatThrownBy(() -> new QueryTag(TagType.GEOGRAPHY, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("value");
    }

    @Test
    void testValueCannotBeBlank() {
        // Given/When/Then
        assertThatThrownBy(() -> new QueryTag(TagType.GEOGRAPHY, "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("value cannot be blank");
    }

    @Test
    void testEqualityBasedOnTypeAndValue() {
        // Given
        var tag1 = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");
        var tag2 = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");
        var tag3 = new QueryTag(TagType.GEOGRAPHY, "Romania");
        var tag4 = new QueryTag(TagType.CATEGORY, "Bulgaria");

        // When/Then
        assertThat(tag1).isEqualTo(tag2); // Same type and value
        assertThat(tag1).isNotEqualTo(tag3); // Different value
        assertThat(tag1).isNotEqualTo(tag4); // Different type
    }

    @Test
    void testHashCodeConsistentForSameValues() {
        // Given
        var tag1 = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");
        var tag2 = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");

        // When/Then
        assertThat(tag1.hashCode()).isEqualTo(tag2.hashCode());
    }

    @Test
    void testToStringContainsTypeAndValue() {
        // Given
        var tag = new QueryTag(TagType.CATEGORY, "Education");

        // When
        var toString = tag.toString();

        // Then
        assertThat(toString).contains("CATEGORY");
        assertThat(toString).contains("Education");
    }

    @Test
    void testJsonSerializationProducesCorrectFormat() throws Exception {
        // Given
        var tag = new QueryTag(TagType.GEOGRAPHY, "Bulgaria");

        // When
        var json = objectMapper.writeValueAsString(tag);

        // Then
        assertThat(json).contains("\"type\"");
        assertThat(json).contains("\"GEOGRAPHY\"");
        assertThat(json).contains("\"value\"");
        assertThat(json).contains("\"Bulgaria\"");
    }

    @Test
    void testJsonDeserializationCreatesValidObject() throws Exception {
        // Given
        var json = "{\"type\":\"CATEGORY\",\"value\":\"Education\"}";

        // When
        var tag = objectMapper.readValue(json, QueryTag.class);

        // Then
        assertThat(tag).isNotNull();
        assertThat(tag.type()).isEqualTo(TagType.CATEGORY);
        assertThat(tag.value()).isEqualTo("Education");
    }

    @Test
    void testJsonRoundTripPreservesData() throws Exception {
        // Given
        var originalTag = new QueryTag(TagType.AUTHORITY, "EU");

        // When
        var json = objectMapper.writeValueAsString(originalTag);
        var deserializedTag = objectMapper.readValue(json, QueryTag.class);

        // Then
        assertThat(deserializedTag).isEqualTo(originalTag);
    }

    @Test
    void testAllTagTypesAreValid() {
        // Given/When/Then - All TagType enum values should work
        assertThatNoException().isThrownBy(() -> new QueryTag(TagType.GEOGRAPHY, "Bulgaria"));
        assertThatNoException().isThrownBy(() -> new QueryTag(TagType.CATEGORY, "Education"));
        assertThatNoException().isThrownBy(() -> new QueryTag(TagType.AUTHORITY, "EU"));
    }
}
