package com.northstar.funding.querygeneration.template;

import com.northstar.funding.domain.GeographicScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GeographicMapper.
 *
 * <p>Validates that all 15 GeographicScope enum values have:
 * <ul>
 *   <li>Non-null keyword mappings</li>
 *   <li>Non-empty keyword mappings</li>
 *   <li>Non-null conceptual descriptions</li>
 *   <li>Non-empty conceptual descriptions</li>
 * </ul>
 */
class GeographicMapperTest {

    private final GeographicMapper mapper = new GeographicMapper();

    @Test
    void toKeywords_shouldMapAllScopes() {
        // Act & Assert - Verify all 15 scopes have keyword mappings
        for (GeographicScope scope : GeographicScope.values()) {
            String keywords = mapper.toKeywords(scope);

            assertThat(keywords)
                    .as("Keywords for scope %s should not be null", scope)
                    .isNotNull();

            assertThat(keywords)
                    .as("Keywords for scope %s should not be empty", scope)
                    .isNotBlank();
        }
    }

    @Test
    void toKeywords_shouldReturnDistinctMappingsForDifferentScopes() {
        // Arrange
        GeographicScope scope1 = GeographicScope.BULGARIA;
        GeographicScope scope2 = GeographicScope.EASTERN_EUROPE;

        // Act
        String keywords1 = mapper.toKeywords(scope1);
        String keywords2 = mapper.toKeywords(scope2);

        // Assert
        assertThat(keywords1).isNotEqualTo(keywords2);
    }

    @Test
    void toConceptualDescription_shouldMapAllScopes() {
        // Act & Assert - Verify all 15 scopes have conceptual descriptions
        for (GeographicScope scope : GeographicScope.values()) {
            String description = mapper.toConceptualDescription(scope);

            assertThat(description)
                    .as("Conceptual description for scope %s should not be null", scope)
                    .isNotNull();

            assertThat(description)
                    .as("Conceptual description for scope %s should not be empty", scope)
                    .isNotBlank();
        }
    }

    @Test
    void toConceptualDescription_shouldReturnDistinctMappingsForDifferentScopes() {
        // Arrange
        GeographicScope scope1 = GeographicScope.EU_MEMBER_STATES;
        GeographicScope scope2 = GeographicScope.BALKANS;

        // Act
        String description1 = mapper.toConceptualDescription(scope1);
        String description2 = mapper.toConceptualDescription(scope2);

        // Assert
        assertThat(description1).isNotEqualTo(description2);
    }

    @Test
    void toConceptualDescription_shouldBeLongerThanKeywords() {
        // Conceptual descriptions should be more verbose than keywords
        for (GeographicScope scope : GeographicScope.values()) {
            String keywords = mapper.toKeywords(scope);
            String description = mapper.toConceptualDescription(scope);

            assertThat(description.length())
                    .as("Conceptual description for %s should be longer than keywords", scope)
                    .isGreaterThan(keywords.length());
        }
    }

    @Test
    void toKeywords_forKnownScopes_shouldReturnExpectedValues() {
        // Assert - Spot check a few known mappings
        assertThat(mapper.toKeywords(GeographicScope.BULGARIA))
                .containsIgnoringCase("bulgaria");

        assertThat(mapper.toKeywords(GeographicScope.EASTERN_EUROPE))
                .containsAnyOf("Eastern Europe", "eastern europe");

        assertThat(mapper.toKeywords(GeographicScope.BALKANS))
                .containsIgnoringCase("balkan");
    }

    @Test
    void toConceptualDescription_forKnownScopes_shouldReturnExpectedValues() {
        // Assert - Spot check a few known mappings
        assertThat(mapper.toConceptualDescription(GeographicScope.BULGARIA))
                .containsIgnoringCase("bulgaria");

        assertThat(mapper.toConceptualDescription(GeographicScope.EU_MEMBER_STATES))
                .containsAnyOf("EU", "European Union");

        assertThat(mapper.toConceptualDescription(GeographicScope.GLOBAL))
                .containsAnyOf("global", "international", "worldwide");
    }

    @Test
    void toKeywords_forBulgaria_shouldIncludeBulgariaKeyword() {
        // Bulgaria keywords should include "Bulgaria"
        String keywords = mapper.toKeywords(GeographicScope.BULGARIA);

        assertThat(keywords)
                .as("Bulgaria keywords should mention Bulgaria")
                .containsIgnoringCase("bulgaria");
    }

    @Test
    void toConceptualDescription_shouldProvideGeographicContext() {
        // Conceptual descriptions should provide context about the region
        assertThat(mapper.toConceptualDescription(GeographicScope.EU_CANDIDATE_COUNTRIES))
                .containsAnyOf("candidate", "accession", "EU");

        assertThat(mapper.toConceptualDescription(GeographicScope.EU_ENLARGEMENT_REGION))
                .containsAnyOf("enlargement", "expansion", "EU");
    }
}
