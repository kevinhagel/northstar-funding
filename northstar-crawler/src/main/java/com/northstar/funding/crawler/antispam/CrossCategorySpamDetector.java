package com.northstar.funding.crawler.antispam;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Detects cross-category spam.
 *
 * Scammers often use domains from unrelated industries (gambling, essay mills)
 * but create pages with education-related content to capture search traffic.
 *
 * Example: "casinowinners.com/scholarships" with title "Apply for Scholarships"
 *
 * Detection:
 * - Check if domain contains scammer keywords (gambling, essay mill)
 * - Check if title/description contains education keywords
 * - If both true → cross-category spam → SPAM
 */
@Component
public class CrossCategorySpamDetector {

    // Scammer industry keywords
    private static final Set<String> GAMBLING_KEYWORDS = Set.of(
            "casino", "poker", "betting", "bet", "win", "lottery", "jackpot",
            "slots", "gamble", "wager"
    );

    private static final Set<String> ESSAY_MILL_KEYWORDS = Set.of(
            "essay", "paper", "dissertation", "thesis", "assignment",
            "homework", "writeessay", "essaywriter"
    );

    // Education keywords (legitimate funding content)
    private static final Set<String> EDUCATION_KEYWORDS = Set.of(
            "scholarship", "grant", "funding", "education", "student",
            "tuition", "financial aid", "college", "university"
    );

    /**
     * Detect cross-category spam.
     *
     * @param domain Domain name
     * @param title Page title
     * @param description Page description
     * @return true if domain is scammer industry but metadata is education
     */
    public boolean detect(String domain, String title, String description) {
        if (domain == null || domain.isBlank()) {
            return false;
        }

        String domainLower = domain.toLowerCase();
        String metadata = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        if (metadata.isBlank()) {
            return false;
        }

        // Check if domain contains scammer keywords
        boolean domainIsScammer = containsAnyKeyword(domainLower, GAMBLING_KEYWORDS) ||
                                  containsAnyKeyword(domainLower, ESSAY_MILL_KEYWORDS);

        if (!domainIsScammer) {
            return false; // Domain is clean
        }

        // Check if metadata contains education keywords
        boolean metadataIsEducation = containsAnyKeyword(metadata, EDUCATION_KEYWORDS);

        // Cross-category spam if scammer domain + education metadata
        return metadataIsEducation;
    }

    /**
     * Check if text contains any of the keywords.
     */
    private boolean containsAnyKeyword(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
