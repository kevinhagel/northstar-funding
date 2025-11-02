package com.northstar.funding.crawler.unit;

import com.northstar.funding.crawler.antispam.KeywordStuffingDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for KeywordStuffingDetector.
 *
 * Tests:
 * - Keyword-stuffed text detected (ratio < 0.5)
 * - Normal text not flagged as spam (ratio >= 0.5)
 * - Edge cases (null, empty, single word)
 * - Case insensitivity
 * - Whitespace handling
 * - Boundary cases (exactly 0.5 ratio)
 */
@DisplayName("KeywordStuffingDetector Unit Tests")
class KeywordStuffingDetectorTest {

    private KeywordStuffingDetector detector;

    @BeforeEach
    void setUp() {
        detector = new KeywordStuffingDetector();
    }

    @Test
    @DisplayName("Obvious keyword stuffing detected")
    void detect_KeywordStuffing_ReturnsTrue() {
        // Given: Text with excessive keyword repetition
        String spam = "grants scholarships funding grants scholarships grants funding education grants";
        // Total: 9 words, Unique: 4 words (grants, scholarships, funding, education)
        // Ratio: 4/9 = 0.44 < 0.5

        // When
        boolean result = detector.detect(spam);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Normal educational text not flagged")
    void detect_NormalText_ReturnsFalse() {
        // Given: Normal descriptive text
        String normal = "The European Union offers educational grants and scholarships for international students";
        // Total: 13 words, Unique: 13 words
        // Ratio: 13/13 = 1.0 >= 0.5

        // When
        boolean result = detector.detect(normal);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Moderate keyword stuffing detected")
    void detect_ModerateStuffing_ReturnsTrue() {
        // Given: Text with moderate repetition
        String spam = "funding funding grants grants scholarships scholarships";
        // Total: 6 words, Unique: 3 words
        // Ratio: 3/6 = 0.5 (edge case - should be >= 0.5 to NOT be spam)
        // But let's test clear spam case

        String clearSpam = "funding funding grants grants scholarships";
        // Total: 5 words, Unique: 3 words
        // Ratio: 3/5 = 0.6 >= 0.5 (not spam)

        String actualSpam = "funding funding funding grants grants";
        // Total: 5 words, Unique: 2 words
        // Ratio: 2/5 = 0.4 < 0.5 (spam)

        // When/Then
        assertThat(detector.detect(actualSpam)).isTrue();
    }

    @Test
    @DisplayName("Null text returns false")
    void detect_NullText_ReturnsFalse() {
        assertThat(detector.detect(null)).isFalse();
    }

    @Test
    @DisplayName("Empty text returns false")
    void detect_EmptyText_ReturnsFalse() {
        assertThat(detector.detect("")).isFalse();
        assertThat(detector.detect("   ")).isFalse();
    }

    @Test
    @DisplayName("Single word returns false")
    void detect_SingleWord_ReturnsFalse() {
        // Single word: unique ratio = 1/1 = 1.0 >= 0.5
        assertThat(detector.detect("grants")).isFalse();
    }

    @Test
    @DisplayName("Two unique words returns false")
    void detect_TwoUniqueWords_ReturnsFalse() {
        // Two different words: unique ratio = 2/2 = 1.0 >= 0.5
        assertThat(detector.detect("grants scholarships")).isFalse();
    }

    @Test
    @DisplayName("Two words with one repeated is spam")
    void detect_TwoWordsOneRepeated_ReturnsTrue() {
        // "grants grants": 2 total, 1 unique
        // Ratio: 1/2 = 0.5 (NOT spam - boundary case)
        assertThat(detector.detect("grants grants")).isFalse();

        // Need 3 words with 1 unique to trigger spam
        // "grants grants grants": 3 total, 1 unique
        // Ratio: 1/3 = 0.33 < 0.5 (spam)
        assertThat(detector.detect("grants grants grants")).isTrue();
    }

    @Test
    @DisplayName("Case insensitive detection")
    void detect_CaseInsensitive_WorksCorrectly() {
        // Given: Same word in different cases
        String spam = "Grants GRANTS grants grants Scholarships SCHOLARSHIPS";
        // Total: 6 words, Unique: 2 words (grants, scholarships - case insensitive)
        // Ratio: 2/6 = 0.33 < 0.5

        // When
        boolean result = detector.detect(spam);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Multiple whitespace handled correctly")
    void detect_MultipleWhitespace_HandledCorrectly() {
        // Given: Text with multiple spaces
        String spam = "grants    grants    grants    scholarships";
        // Should split correctly: 4 words, 2 unique
        // Ratio: 2/4 = 0.5 (NOT spam - boundary)

        String actualSpam = "grants    grants    grants    grants    scholarships";
        // 5 words, 2 unique
        // Ratio: 2/5 = 0.4 < 0.5 (spam)

        // When/Then
        assertThat(detector.detect(actualSpam)).isTrue();
    }

    @Test
    @DisplayName("Boundary case - exactly 0.5 ratio is NOT spam")
    void detect_BoundaryCase_HalfRatio_NotSpam() {
        // Given: Text with exactly 0.5 unique ratio (boundary case)
        String boundary = "grants grants scholarships scholarships";
        // Total: 4 words, Unique: 2 words
        // Ratio: 2/4 = 0.5 (>= 0.5 threshold, so NOT spam)

        // When
        boolean result = detector.detect(boundary);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Boundary case - just below 0.5 ratio IS spam")
    void detect_BoundaryCase_JustBelowHalf_IsSpam() {
        // Given: Text with ratio just below 0.5
        String spam = "grants grants grants scholarships scholarships";
        // Total: 5 words, Unique: 2 words
        // Ratio: 2/5 = 0.4 < 0.5 (spam)

        // When
        boolean result = detector.detect(spam);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Real-world spam example detected")
    void detect_RealWorldSpam_Detected() {
        // Given: Realistic spam from gambling/essay mill sites
        String spam = "essay writing service essay writing service cheap essay writing essay service online";
        // Total: 11 words, Unique: 5 words (essay, writing, service, cheap, online)
        // Ratio: 5/11 = 0.45 < 0.5

        // When
        boolean result = detector.detect(spam);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Real-world legitimate text not flagged")
    void detect_RealWorldLegitimate_NotFlagged() {
        // Given: Realistic legitimate educational content
        String legitimate = "The European Research Council provides competitive funding for excellent frontier research";
        // Total: 12 words, all unique
        // Ratio: 12/12 = 1.0 >= 0.5

        // When
        boolean result = detector.detect(legitimate);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Combined title and description - spam detected")
    void detect_CombinedTitleDescription_SpamDetected() {
        // Given: Title + description combined (as used in production)
        String title = "Grants Grants Grants Scholarships";
        String description = "Grants funding grants scholarships grants";
        String combined = title + " " + description;
        // Total: 9 words, Unique: 3 words (grants, scholarships, funding)
        // Ratio: 3/9 = 0.33 < 0.5

        // When
        boolean result = detector.detect(combined);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Combined title and description - legitimate not flagged")
    void detect_CombinedTitleDescription_LegitimateNotFlagged() {
        // Given: Legitimate combined text
        String title = "European Research Council Grants";
        String description = "Funding opportunities for innovative research projects across Europe";
        String combined = title + " " + description;
        // All unique words

        // When
        boolean result = detector.detect(combined);

        // Then
        assertThat(result).isFalse();
    }
}
