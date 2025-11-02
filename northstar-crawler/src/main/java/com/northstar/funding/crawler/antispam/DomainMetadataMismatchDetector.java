package com.northstar.funding.crawler.antispam;

import org.apache.commons.text.similarity.CosineSimilarity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Detects domain-metadata mismatch spam.
 *
 * Scammers often use irrelevant domains with education-related metadata.
 * Example: "casinowinners.com" with title "Education Scholarships"
 *
 * Detection:
 * 1. Extract keywords from domain (e.g., "casinowinners.com" → ["casino", "winners"])
 * 2. Calculate cosine similarity between domain keywords and metadata (title + description)
 * 3. If similarity < 0.15, domain and metadata are unrelated → SPAM
 */
@Component
public class DomainMetadataMismatchDetector {

    private static final double SIMILARITY_THRESHOLD = 0.15;
    private final CosineSimilarity cosineSimilarity = new CosineSimilarity();

    /**
     * Detect domain-metadata mismatch.
     *
     * @param domain Domain name (e.g., "casinowinners.com")
     * @param title Page title
     * @param description Page description
     * @return true if domain and metadata are unrelated (similarity < 0.15)
     */
    public boolean detect(String domain, String title, String description) {
        if (domain == null || domain.isBlank()) {
            return false;
        }

        String metadata = (title != null ? title : "") + " " + (description != null ? description : "");
        if (metadata.isBlank()) {
            return false;
        }

        // Extract keywords from domain (remove TLD, split on non-alphanumeric)
        String domainKeywords = extractDomainKeywords(domain);
        if (domainKeywords.isBlank()) {
            return false;
        }

        // Calculate cosine similarity
        Map<CharSequence, Integer> domainVector = buildWordVector(domainKeywords);
        Map<CharSequence, Integer> metadataVector = buildWordVector(metadata);

        Double similarity = cosineSimilarity.cosineSimilarity(domainVector, metadataVector);

        return similarity != null && similarity < SIMILARITY_THRESHOLD;
    }

    /**
     * Extract keywords from domain name.
     * Example: "casinowinners.com" → "casino winners"
     */
    private String extractDomainKeywords(String domain) {
        // Remove common TLDs
        String withoutTld = domain
                .replaceAll("\\.(com|org|net|edu|gov|io|co)$", "")
                .toLowerCase();

        // Split on non-alphanumeric characters
        String[] parts = withoutTld.split("[^a-z0-9]+");

        return String.join(" ", parts);
    }

    /**
     * Build word frequency vector for cosine similarity.
     */
    private Map<CharSequence, Integer> buildWordVector(String text) {
        Map<CharSequence, Integer> vector = new HashMap<>();
        String normalized = text.toLowerCase();
        String[] words = normalized.split("\\s+");

        for (String word : words) {
            if (word.length() > 2) { // Ignore very short words
                vector.put(word, vector.getOrDefault(word, 0) + 1);
            }
        }

        return vector;
    }
}
