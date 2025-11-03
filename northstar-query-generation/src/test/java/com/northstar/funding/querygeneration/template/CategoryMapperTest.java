package com.northstar.funding.querygeneration.template;

import com.northstar.funding.domain.FundingSearchCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CategoryMapper.
 *
 * <p>Validates that all 25 FundingSearchCategory enum values have:
 * <ul>
 *   <li>Non-null keyword mappings</li>
 *   <li>Non-empty keyword mappings</li>
 *   <li>Non-null conceptual descriptions</li>
 *   <li>Non-empty conceptual descriptions</li>
 * </ul>
 */
class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void toKeywords_shouldMapAllCategories() {
        // Act & Assert - Verify all 25 categories have keyword mappings
        for (FundingSearchCategory category : FundingSearchCategory.values()) {
            String keywords = mapper.toKeywords(category);

            assertThat(keywords)
                    .as("Keywords for category %s should not be null", category)
                    .isNotNull();

            assertThat(keywords)
                    .as("Keywords for category %s should not be empty", category)
                    .isNotBlank();
        }
    }

    @Test
    void toKeywords_shouldReturnDistinctMappingsForDifferentCategories() {
        // Arrange
        FundingSearchCategory category1 = FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS;
        FundingSearchCategory category2 = FundingSearchCategory.TEACHER_DEVELOPMENT;

        // Act
        String keywords1 = mapper.toKeywords(category1);
        String keywords2 = mapper.toKeywords(category2);

        // Assert
        assertThat(keywords1).isNotEqualTo(keywords2);
    }

    @Test
    void toConceptualDescription_shouldMapAllCategories() {
        // Act & Assert - Verify all 25 categories have conceptual descriptions
        for (FundingSearchCategory category : FundingSearchCategory.values()) {
            String description = mapper.toConceptualDescription(category);

            assertThat(description)
                    .as("Conceptual description for category %s should not be null", category)
                    .isNotNull();

            assertThat(description)
                    .as("Conceptual description for category %s should not be empty", category)
                    .isNotBlank();
        }
    }

    @Test
    void toConceptualDescription_shouldReturnDistinctMappingsForDifferentCategories() {
        // Arrange
        FundingSearchCategory category1 = FundingSearchCategory.INFRASTRUCTURE_FUNDING;
        FundingSearchCategory category2 = FundingSearchCategory.EDUCATION_RESEARCH;

        // Act
        String description1 = mapper.toConceptualDescription(category1);
        String description2 = mapper.toConceptualDescription(category2);

        // Assert
        assertThat(description1).isNotEqualTo(description2);
    }

    @Test
    void toConceptualDescription_shouldBeLongerThanKeywords() {
        // Conceptual descriptions should be more verbose than keywords
        for (FundingSearchCategory category : FundingSearchCategory.values()) {
            String keywords = mapper.toKeywords(category);
            String description = mapper.toConceptualDescription(category);

            assertThat(description.length())
                    .as("Conceptual description for %s should be longer than keywords", category)
                    .isGreaterThan(keywords.length());
        }
    }

    @Test
    void toKeywords_forKnownCategories_shouldReturnExpectedValues() {
        // Assert - Spot check a few known mappings
        assertThat(mapper.toKeywords(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .contains("scholarship");

        assertThat(mapper.toKeywords(FundingSearchCategory.TEACHER_DEVELOPMENT))
                .containsAnyOf("teacher", "training", "development");

        assertThat(mapper.toKeywords(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .containsAnyOf("infrastructure", "building", "facility");
    }

    @Test
    void toConceptualDescription_forKnownCategories_shouldReturnExpectedValues() {
        // Assert - Spot check a few known mappings
        assertThat(mapper.toConceptualDescription(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .containsIgnoringCase("scholarship");

        assertThat(mapper.toConceptualDescription(FundingSearchCategory.EDUCATION_RESEARCH))
                .containsIgnoringCase("research");

        assertThat(mapper.toConceptualDescription(FundingSearchCategory.STEM_EDUCATION))
                .containsAnyOf("science", "technology", "engineering", "mathematics", "STEM");
    }
}
