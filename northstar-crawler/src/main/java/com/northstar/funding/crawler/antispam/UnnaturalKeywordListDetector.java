package com.northstar.funding.crawler.antispam;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects unnatural keyword lists in search result text.
 *
 * Spam results often consist of keyword lists without natural language structure.
 * Example: "grants scholarships funding aid" (no articles, prepositions, or verbs)
 *
 * Detection:
 * - Count common words (articles, prepositions, etc.)
 * - If < 2 common words found → unnatural keyword list → SPAM
 */
@Component
public class UnnaturalKeywordListDetector {

    private static final int MIN_COMMON_WORDS = 2;

    // Common English words that appear in natural text
    private static final Set<String> COMMON_WORDS = Set.of(
            "the", "a", "an", "of", "for", "to", "in", "with", "on", "at",
            "by", "from", "as", "is", "are", "was", "were", "be", "been",
            "and", "or", "but", "if", "this", "that", "these", "those"
    );

    /**
     * Detect unnatural keyword list.
     *
     * @param text Text to analyze (title + description combined)
     * @return true if text lacks natural language structure (< 2 common words)
     */
    public boolean detect(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String textLower = text.toLowerCase();
        int commonWordCount = 0;

        // Count how many common words appear in the text
        for (String commonWord : COMMON_WORDS) {
            // Use word boundary regex to match whole words only
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(commonWord) + "\\b");
            if (pattern.matcher(textLower).find()) {
                commonWordCount++;
                if (commonWordCount >= MIN_COMMON_WORDS) {
                    return false; // Found enough common words, text is natural
                }
            }
        }

        // If we found fewer than MIN_COMMON_WORDS, it's likely a keyword list
        return commonWordCount < MIN_COMMON_WORDS;
    }
}
