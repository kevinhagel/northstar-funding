package com.northstar.funding.crawler.scoring;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Service for calculating confidence scores for funding source candidates.
 *
 * Combines multiple signals to determine how likely a search result represents
 * a legitimate funding source:
 * - TLD credibility (from DomainCredibilityService)
 * - Funding keywords in title/description
 * - Geographic relevance (Bulgaria, EU, Eastern Europe)
 * - Organization type (Ministry, Commission, Foundation, University)
 * - Compound boost when multiple signals present
 *
 * Score range: 0.00 (no confidence) to 1.00 (maximum confidence)
 * Threshold: >= 0.60 for PENDING_CRAWL status
 *
 * All scores use BigDecimal with scale 2 for precision.
 */
@Service
public class ConfidenceScorer {

    private final DomainCredibilityService domainCredibilityService;

    // Funding keywords (grants, scholarships, etc.)
    private static final Set<String> FUNDING_KEYWORDS = Set.of(
        "grant", "grants", "funding", "scholarship", "scholarships",
        "fellowship", "fellowships", "subsidy", "subsidies",
        "bursary", "bursaries", "award", "awards",
        "stipend", "stipends", "financial aid", "financial support",
        "sponsorship", "endowment"
    );

    // Geographic terms (Bulgaria, EU, Eastern Europe)
    private static final Set<String> GEOGRAPHIC_KEYWORDS = Set.of(
        "bulgaria", "bulgarian", "българия", "българск",
        "eu", "european union", "europe", "european",
        "eastern europe", "балкан", "balkan",
        "romania", "romanian", "românia",
        "poland", "polish", "polska",
        "czech", "czechia", "české",
        "regional", "local"
    );

    // Organization type keywords (Ministry, Commission, Foundation, University)
    private static final Set<String> ORGANIZATION_KEYWORDS = Set.of(
        "ministry", "minister", "министерство",
        "commission", "commissioner", "комисия",
        "foundation", "фондация", "fund",
        "university", "университет", "college",
        "government", "правителство", "official",
        "national", "state", "federal",
        "agency", "агенция", "authority",
        "council", "съвет", "chamber"
    );

    // Score increments
    private static final BigDecimal TITLE_KEYWORD_SCORE = new BigDecimal("0.15");
    private static final BigDecimal DESCRIPTION_KEYWORD_SCORE = new BigDecimal("0.10");
    private static final BigDecimal GEOGRAPHIC_SCORE = new BigDecimal("0.15");
    private static final BigDecimal ORGANIZATION_SCORE = new BigDecimal("0.15");
    private static final BigDecimal COMPOUND_BOOST = new BigDecimal("0.15");

    public ConfidenceScorer(DomainCredibilityService domainCredibilityService) {
        this.domainCredibilityService = domainCredibilityService;
    }

    /**
     * Calculate confidence score for a search result.
     *
     * Algorithm:
     * 1. Start with TLD credibility score (-0.30 to +0.20)
     * 2. Add +0.15 if title contains funding keywords
     * 3. Add +0.10 if description contains funding keywords
     * 4. Add +0.15 if geographic match detected
     * 5. Add +0.15 if organization type detected
     * 6. Add +0.15 compound boost if multiple signals (>= 3)
     * 7. Cap at 1.00 maximum, floor at 0.00 minimum
     *
     * @param title Search result title (nullable)
     * @param description Search result description (nullable)
     * @param url Search result URL
     * @return Confidence score with scale 2 (0.00 to 1.00)
     */
    public BigDecimal calculateConfidence(String title, String description, String url) {
        // Start with TLD score
        BigDecimal score = domainCredibilityService.getTldScore(url);

        int signalCount = 0;

        // Check for funding keywords in title
        if (containsFundingKeywords(title)) {
            score = score.add(TITLE_KEYWORD_SCORE);
            signalCount++;
        }

        // Check for funding keywords in description
        if (containsFundingKeywords(description)) {
            score = score.add(DESCRIPTION_KEYWORD_SCORE);
            signalCount++;
        }

        // Check for geographic relevance
        if (hasGeographicMatch(title) || hasGeographicMatch(description)) {
            score = score.add(GEOGRAPHIC_SCORE);
            signalCount++;
        }

        // Check for organization type
        if (hasOrganizationType(title) || hasOrganizationType(description)) {
            score = score.add(ORGANIZATION_SCORE);
            signalCount++;
        }

        // Compound boost: Multiple signals indicate high quality
        if (signalCount >= 3) {
            score = score.add(COMPOUND_BOOST);
        }

        // Cap at 1.00 maximum
        if (score.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }

        // Floor at 0.00 minimum (even spam TLDs with keywords get >= 0.00)
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Check if text contains funding-related keywords.
     *
     * @param text Text to check (nullable)
     * @return true if funding keywords found, false otherwise
     */
    private boolean containsFundingKeywords(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return FUNDING_KEYWORDS.stream()
            .anyMatch(lowerText::contains);
    }

    /**
     * Check if text contains geographic relevance keywords.
     *
     * @param text Text to check (nullable)
     * @return true if geographic match found, false otherwise
     */
    private boolean hasGeographicMatch(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return GEOGRAPHIC_KEYWORDS.stream()
            .anyMatch(lowerText::contains);
    }

    /**
     * Check if text contains organization type keywords.
     *
     * @param text Text to check (nullable)
     * @return true if organization type detected, false otherwise
     */
    private boolean hasOrganizationType(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        return ORGANIZATION_KEYWORDS.stream()
            .anyMatch(lowerText::contains);
    }
}
