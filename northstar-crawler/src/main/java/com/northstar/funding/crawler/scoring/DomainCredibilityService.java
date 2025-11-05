package com.northstar.funding.crawler.scoring;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 * Service for calculating domain credibility scores based on TLD (Top-Level Domain) analysis.
 *
 * Implements 5-tier TLD classification system based on research:
 * - Tier 1: Validated nonprofits, government, education (+0.20)
 * - Tier 2: Traditional nonprofits, EU domains, regional ccTLDs (+0.15)
 * - Tier 3: Generic business TLDs (+0.08)
 * - Tier 4: Cheap/unrestricted TLDs (0.00)
 * - Tier 5: Spam/phishing TLDs (negative scores)
 *
 * Research: specs/006-search-result-processing/tld-credibility-research.md
 */
@Service
public class DomainCredibilityService {

    // Tier 1: Highest Credibility (+0.20)
    private static final Map<String, BigDecimal> TIER_1_TLDS = Map.ofEntries(
        // Validated nonprofit TLDs
        Map.entry("ngo", score("0.20")),
        Map.entry("ong", score("0.20")),
        Map.entry("foundation", score("0.20")),
        Map.entry("charity", score("0.20")),

        // Government
        Map.entry("gov", score("0.20")),

        // Education
        Map.entry("edu", score("0.20"))
    );

    // Tier 1 second-level domains (e.g., .gov.bg, .edu.bg, .europa.eu)
    private static final Set<String> TIER_1_SECOND_LEVEL = Set.of(
        "gov.bg", "gov.ro", "gov.pl", "gov.cz", "gov.de", "gov.fr",
        "edu.bg", "edu.ro", "edu.pl", "edu.cz",
        "ac.bg", "ac.ro", "ac.pl", "ac.cz",  // Academic domains
        "europa.eu"  // EU institutions
    );

    // Tier 2: High Credibility (+0.15)
    private static final Map<String, BigDecimal> TIER_2_TLDS = Map.ofEntries(
        // Traditional nonprofit
        Map.entry("org", score("0.15")),

        // EU domains
        Map.entry("eu", score("0.15")),
        Map.entry("ею", score("0.15")),  // Cyrillic .eu

        // Eastern Europe ccTLDs (target region)
        Map.entry("bg", score("0.15")),  // Bulgaria
        Map.entry("бг", score("0.15")),  // Bulgaria Cyrillic
        Map.entry("ro", score("0.15")),  // Romania
        Map.entry("pl", score("0.15")),  // Poland
        Map.entry("cz", score("0.15")),  // Czech Republic
        Map.entry("de", score("0.15")),  // Germany
        Map.entry("fr", score("0.15")),  // France
        Map.entry("gr", score("0.15")),  // Greece
        Map.entry("hu", score("0.15")),  // Hungary
        Map.entry("at", score("0.15")),  // Austria
        Map.entry("it", score("0.15")),  // Italy
        Map.entry("es", score("0.15")),  // Spain

        // Funding-specific TLDs
        Map.entry("fund", score("0.15")),
        Map.entry("gives", score("0.15"))
    );

    // Tier 3: Medium Credibility (+0.08)
    private static final Map<String, BigDecimal> TIER_3_TLDS = Map.ofEntries(
        Map.entry("com", score("0.08")),
        Map.entry("net", score("0.08")),
        Map.entry("info", score("0.08")),
        Map.entry("education", score("0.08"))
    );

    // Tier 4: Low Credibility (0.00)
    private static final Map<String, BigDecimal> TIER_4_TLDS = Map.ofEntries(
        Map.entry("biz", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)),
        Map.entry("co", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)),
        Map.entry("io", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)),
        Map.entry("me", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
    );

    // Tier 5: Negative Credibility (Spam/Phishing TLDs)
    private static final Map<String, BigDecimal> TIER_5_TLDS = Map.ofEntries(
        // Freenom free domains
        Map.entry("tk", score("-0.30")),
        Map.entry("ml", score("-0.30")),
        Map.entry("ga", score("-0.30")),
        Map.entry("cf", score("-0.30")),
        Map.entry("gq", score("-0.30")),

        // Cheap phishing favorites
        Map.entry("xyz", score("-0.20")),
        Map.entry("top", score("-0.20")),
        Map.entry("icu", score("-0.20")),
        Map.entry("buzz", score("-0.20")),

        // Loan/suspicious TLDs
        Map.entry("loan", score("-0.25")),
        Map.entry("click", score("-0.15")),
        Map.entry("cam", score("-0.15")),
        Map.entry("pw", score("-0.15")),
        Map.entry("shop", score("-0.10"))
    );

    /**
     * Calculate TLD credibility score for a given URL.
     *
     * @param url The URL to analyze (e.g., "https://example.org")
     * @return BigDecimal score with scale 2 (-0.30 to +0.20), or 0.00 for unknown TLDs
     */
    public BigDecimal getTldScore(String url) {
        String tld = extractTld(url);
        if (tld == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // Check Tier 1 second-level domains first (e.g., .gov.bg, .europa.eu)
        if (TIER_1_SECOND_LEVEL.contains(tld)) {
            return score("0.20");
        }

        // Extract final TLD for single-level check (e.g., "gov.bg" -> "bg")
        String finalTld = extractFinalTld(tld);

        // Check each tier
        if (TIER_1_TLDS.containsKey(finalTld)) {
            return TIER_1_TLDS.get(finalTld);
        }
        if (TIER_2_TLDS.containsKey(finalTld)) {
            return TIER_2_TLDS.get(finalTld);
        }
        if (TIER_3_TLDS.containsKey(finalTld)) {
            return TIER_3_TLDS.get(finalTld);
        }
        if (TIER_4_TLDS.containsKey(finalTld)) {
            return TIER_4_TLDS.get(finalTld);
        }
        if (TIER_5_TLDS.containsKey(finalTld)) {
            return TIER_5_TLDS.get(finalTld);
        }

        // Unknown TLD defaults to 0.00
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Check if the URL uses a spam/phishing TLD (Tier 5).
     *
     * @param url The URL to check
     * @return true if spam TLD, false otherwise
     */
    public boolean isSpamTld(String url) {
        String tld = extractTld(url);
        if (tld == null) {
            return false;
        }

        String finalTld = extractFinalTld(tld);
        return TIER_5_TLDS.containsKey(finalTld);
    }

    /**
     * Check if the URL uses a validated nonprofit TLD (.ngo, .foundation, .charity).
     *
     * @param url The URL to check
     * @return true if validated nonprofit TLD, false otherwise
     */
    public boolean isValidatedNonprofit(String url) {
        String tld = extractTld(url);
        if (tld == null) {
            return false;
        }

        String finalTld = extractFinalTld(tld);
        return Set.of("ngo", "ong", "foundation", "charity").contains(finalTld);
    }

    /**
     * Check if the URL uses a government domain (.gov, .gov.xx, .europa.eu).
     *
     * @param url The URL to check
     * @return true if government domain, false otherwise
     */
    public boolean isGovernmentDomain(String url) {
        String tld = extractTld(url);
        if (tld == null) {
            return false;
        }

        // Check second-level government domains
        if (tld.startsWith("gov.") || tld.equals("europa.eu")) {
            return true;
        }

        String finalTld = extractFinalTld(tld);
        return "gov".equals(finalTld);
    }

    /**
     * Check if the URL uses a target region ccTLD (Eastern Europe: .bg, .ro, .pl, .cz, .eu, etc.).
     *
     * @param url The URL to check
     * @return true if target region ccTLD, false otherwise
     */
    public boolean isTargetRegionCcTld(String url) {
        String tld = extractTld(url);
        if (tld == null) {
            return false;
        }

        String finalTld = extractFinalTld(tld);
        return Set.of("bg", "бг", "ro", "pl", "cz", "eu", "ею", "de", "fr", "gr", "hu", "at", "it", "es")
                .contains(finalTld);
    }

    /**
     * Extract TLD from URL, handling second-level domains and IDNs.
     * Examples:
     * - "https://example.org" -> "org"
     * - "https://ministry.gov.bg" -> "gov.bg"
     * - "https://european-union.europa.eu" -> "europa.eu"
     * - "https://example.бг" -> "бг" (Cyrillic IDN)
     *
     * @param url The URL to parse
     * @return TLD string (lowercase), or null if invalid
     */
    private String extractTld(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            // Try URI.getHost() first (works for ASCII domains)
            URI uri = new URI(url);
            String host = uri.getHost();

            // Fallback for IDN/Cyrillic domains: parse manually
            if (host == null || host.isBlank()) {
                host = extractHostManually(url);
                if (host == null) {
                    return null;
                }
            }

            // Normalize to lowercase (preserves Cyrillic characters)
            host = host.toLowerCase(java.util.Locale.ROOT);

            // Split by dots
            String[] parts = host.split("\\.");

            if (parts.length < 2) {
                return null;  // No TLD
            }

            // Check for second-level domains (e.g., gov.bg, europa.eu)
            if (parts.length >= 2) {
                String secondLevel = parts[parts.length - 2] + "." + parts[parts.length - 1];
                if (TIER_1_SECOND_LEVEL.contains(secondLevel)) {
                    return secondLevel;
                }
            }

            // Return final TLD
            return parts[parts.length - 1];

        } catch (URISyntaxException e) {
            // Fallback for IDN/invalid URI syntax
            String host = extractHostManually(url);
            if (host == null) {
                return null;
            }

            host = host.toLowerCase(java.util.Locale.ROOT);
            String[] parts = host.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            // Check second-level
            if (parts.length >= 2) {
                String secondLevel = parts[parts.length - 2] + "." + parts[parts.length - 1];
                if (TIER_1_SECOND_LEVEL.contains(secondLevel)) {
                    return secondLevel;
                }
            }

            return parts[parts.length - 1];
        }
    }

    /**
     * Manually extract host from URL string (fallback for IDN/Cyrillic domains).
     * Examples:
     * - "https://example.бг" -> "example.бг"
     * - "http://example.org:8080/path" -> "example.org"
     *
     * @param url The URL string
     * @return host, or null if cannot parse
     */
    private String extractHostManually(String url) {
        if (url == null) {
            return null;
        }

        // Remove protocol
        String host = url.replaceFirst("^https?://", "");

        // Remove port (if present)
        int portIdx = host.indexOf(':');
        if (portIdx > 0) {
            host = host.substring(0, portIdx);
        }

        // Remove path (if present)
        int pathIdx = host.indexOf('/');
        if (pathIdx > 0) {
            host = host.substring(0, pathIdx);
        }

        // Remove query (if present)
        int queryIdx = host.indexOf('?');
        if (queryIdx > 0) {
            host = host.substring(0, queryIdx);
        }

        return host.isBlank() ? null : host;
    }

    /**
     * Extract final TLD component (rightmost after dot).
     * Examples:
     * - "org" -> "org"
     * - "gov.bg" -> "bg"
     * - "europa.eu" -> "eu"
     *
     * @param tld The TLD string
     * @return Final TLD component
     */
    private String extractFinalTld(String tld) {
        if (tld == null) {
            return null;
        }

        String[] parts = tld.split("\\.");
        return parts[parts.length - 1];
    }

    /**
     * Helper to create BigDecimal score with scale 2.
     */
    private static BigDecimal score(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }
}
