package com.northstar.funding.crawler.antispam;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects keyword stuffing in search result text.
 *
 * Keyword stuffing is when text contains excessive repetition of keywords
 * to manipulate search rankings. Detected by calculating the unique word ratio:
 *
 * Unique Ratio = unique words / total words
 *
 * If ratio < 0.5, the text is likely keyword-stuffed spam.
 *
 * Example spam: "grants scholarships funding grants scholarships grants funding education grants"
 * - Total words: 9
 * - Unique words: 4 (grants, scholarships, funding, education)
 * - Ratio: 4/9 = 0.44 < 0.5 → SPAM
 */
@Component
public class KeywordStuffingDetector {

    private static final double UNIQUE_RATIO_THRESHOLD = 0.5;

    /**
     * Detect keyword stuffing in text.
     *
     * @param text Text to analyze (title + description combined)
     * @return true if keyword stuffing detected (unique ratio < 0.5)
     */
    public boolean detect(String text) {
        if (text == null || text.isBlank()) {
            return false; // Empty text is not spam
        }

        // Normalize text: lowercase, split on whitespace
        String normalized = text.toLowerCase().trim();
        String[] words = normalized.split("\\s+");

        if (words.length == 0) {
            return false;
        }

        // Calculate unique word ratio
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        double uniqueRatio = (double) uniqueWords.size() / words.length;

        return uniqueRatio < UNIQUE_RATIO_THRESHOLD;
    }
}
