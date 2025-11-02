package com.northstar.funding.crawler.unit;

import com.northstar.funding.crawler.antispam.UnnaturalKeywordListDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UnnaturalKeywordListDetector.
 */
@DisplayName("UnnaturalKeywordListDetector Unit Tests")
class UnnaturalKeywordListDetectorTest {

    private UnnaturalKeywordListDetector detector;

    @BeforeEach
    void setUp() {
        detector = new UnnaturalKeywordListDetector();
    }

    @Test
    @DisplayName("Unnatural keyword list detected - no common words")
    void detect_KeywordList_NoCommonWords_ReturnsTrue() {
        String spam = "grants scholarships funding aid";
        assertThat(detector.detect(spam)).isTrue();
    }

    @Test
    @DisplayName("Unnatural keyword list detected - one common word (needs 2+)")
    void detect_KeywordList_OneCommonWord_ReturnsTrue() {
        String spam = "grants scholarships for funding aid";
        assertThat(detector.detect(spam)).isTrue();
    }

    @Test
    @DisplayName("Natural text not flagged - has 2+ common words")
    void detect_NaturalText_ReturnsFalse() {
        String natural = "The European Union offers grants for students";
        assertThat(detector.detect(natural)).isFalse();
    }

    @Test
    @DisplayName("Natural sentence with articles and prepositions")
    void detect_NaturalSentence_ReturnsFalse() {
        String natural = "Find funding opportunities for research in Europe";
        assertThat(detector.detect(natural)).isFalse();
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
    @DisplayName("Case insensitive matching")
    void detect_CaseInsensitive_WorksCorrectly() {
        String natural = "THE EUROPEAN UNION OFFERS GRANTS FOR STUDENTS";
        assertThat(detector.detect(natural)).isFalse();
    }

    @Test
    @DisplayName("Exactly 2 common words is NOT spam")
    void detect_ExactlyTwoCommonWords_ReturnsFalse() {
        String text = "grants scholarships for students in universities";
        // Common words: "for", "in" = 2
        assertThat(detector.detect(text)).isFalse();
    }

    @Test
    @DisplayName("Word boundary matching - 'the' in 'ther' doesn't count")
    void detect_WordBoundary_TheInTher_NoMatch() {
        String text = "grants scholarships funding aid therapy";
        // "therapy" contains "the" but shouldn't match due to word boundaries
        assertThat(detector.detect(text)).isTrue();
    }

    @Test
    @DisplayName("Real-world spam - essay mill keyword list")
    void detect_RealWorldSpam_EssayMill() {
        String spam = "essay writing service cheap affordable quality";
        assertThat(detector.detect(spam)).isTrue();
    }

    @Test
    @DisplayName("Real-world legitimate - proper sentence")
    void detect_RealWorldLegitimate_ProperSentence() {
        String legitimate = "The European Research Council provides funding for innovative research projects";
        // Contains: "the", "for" = 2+
        assertThat(detector.detect(legitimate)).isFalse();
    }

    @Test
    @DisplayName("Short text with common words")
    void detect_ShortText_WithCommonWords() {
        String text = "grants for students";
        // Contains: "for" = 1 (needs 2)
        assertThat(detector.detect(text)).isTrue();
    }

    @Test
    @DisplayName("Short text with 2+ common words")
    void detect_ShortText_TwoCommonWords() {
        String text = "grants for the students";
        // Contains: "for", "the" = 2
        assertThat(detector.detect(text)).isFalse();
    }

    @Test
    @DisplayName("All common words still count")
    void detect_AllCommonWords_NotSpam() {
        String text = "the and for with in on";
        // All common words = definitely not spam
        assertThat(detector.detect(text)).isFalse();
    }

    @Test
    @DisplayName("Combined title and description - natural")
    void detect_CombinedTitleDescription_Natural() {
        String title = "European Grants Database";
        String description = "Find funding opportunities for research projects in the EU";
        String combined = title + " " + description;
        // Contains: "for", "in", "the" = 3
        assertThat(detector.detect(combined)).isFalse();
    }

    @Test
    @DisplayName("Combined title and description - spam")
    void detect_CombinedTitleDescription_Spam() {
        String title = "Grants Scholarships Funding";
        String description = "Education money financial aid support";
        String combined = title + " " + description;
        // No common words
        assertThat(detector.detect(combined)).isTrue();
    }
}
