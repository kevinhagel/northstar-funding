package com.northstar.funding.crawler.antispam;

import com.northstar.funding.domain.SearchResult;

/**
 * Contract for anti-spam filtering to detect SEO spam and scammer sites.
 *
 * CRITICAL: Anti-spam filtering MUST execute BEFORE domain deduplication and LLM processing
 * to save CPU resources and prevent blacklist pollution.
 *
 * Based on spring-crawler lessons: 40-60% of search results are scammer/SEO spam
 * that can be filtered using fuzzy matching before expensive LLM processing.
 */
public interface AntiSpamFilter {

    /**
     * Analyze search result for spam indicators using fuzzy matching.
     *
     * @param result SearchResult to analyze (title, description, domain)
     * @return SpamAnalysisResult with spam verdict and detection reasons
     *
     * Contract:
     * - MUST execute all 4 detection strategies:
     *   1. Keyword stuffing detection (unique word ratio < 0.5)
     *   2. Domain-metadata mismatch (cosine similarity < 0.15)
     *   3. Unnatural keyword list patterns (missing common articles/prepositions)
     *   4. Cross-category spam (gambling + education keywords)
     * - MUST return non-null SpamAnalysisResult
     * - MUST complete in < 5ms (fuzzy matching is fast, no network calls)
     * - MUST be thread-safe for Virtual Thread parallel execution
     * - MUST use Apache Commons Text for fuzzy string matching
     */
    SpamAnalysisResult analyzeForSpam(SearchResult result);

    /**
     * Detect keyword stuffing by analyzing unique word ratio.
     *
     * Scammer tactic: Repeat keywords to rank in searches.
     * Example: "grants scholarships funding grants education grants financial aid grants"
     *
     * @param text Title or description to analyze
     * @return true if keyword stuffing detected (unique ratio < 0.5), false otherwise
     *
     * Contract:
     * - Calculate unique word ratio: uniqueWords.size() / totalWords.size()
     * - MUST return true if ratio < 0.5 (more than half the words are duplicates)
     * - MUST handle null/empty text gracefully (return false)
     * - MUST split on whitespace, ignore case for uniqueness check
     */
    boolean detectKeywordStuffing(String text);

    /**
     * Detect domain-metadata mismatch using fuzzy similarity.
     *
     * Scammer tactic: Gambling/essay mill domains with education funding keywords in metadata.
     * Example: domain="casinowinners.com", title="Education Scholarships and Grants"
     *
     * @param domain Normalized domain name (e.g., "casinowinners.com")
     * @param title Page title from search metadata
     * @param description Page description from search metadata
     * @return true if domain doesn't match metadata (similarity < 0.15), false otherwise
     *
     * Contract:
     * - Extract keywords from domain: "casinowinners.com" → "casino winners"
     * - Calculate cosine similarity between domain keywords and title+description
     * - MUST return true if similarity < 0.15 (domain has NOTHING to do with metadata)
     * - MUST use Apache Commons Text Cosine Similarity
     * - MUST handle null/empty title/description gracefully
     */
    boolean detectDomainMetadataMismatch(String domain, String title, String description);

    /**
     * Detect unnatural keyword list patterns.
     *
     * Natural language includes articles/prepositions like "the", "a", "of", "for".
     * Keyword lists don't: "grants scholarships funding aid"
     *
     * @param text Title or description to analyze
     * @return true if unnatural keyword list detected, false otherwise
     *
     * Contract:
     * - Count occurrences of common words: ["the", "a", "an", "of", "for", "to", "in", "with"]
     * - MUST return true if < 2 common words found (keyword list, not natural language)
     * - MUST handle null/empty text gracefully (return false)
     */
    boolean detectUnnaturalKeywordList(String text);

    /**
     * Detect cross-category spam (gambling/essay mills with education keywords).
     *
     * Scammer tactic: Add education keywords to gambling/essay mill sites.
     * Example: "poker.com" with "scholarship" in title
     *
     * @param domain Normalized domain name
     * @param title Page title from search metadata
     * @param description Page description from search metadata
     * @return true if cross-category spam detected, false otherwise
     *
     * Contract:
     * - Check domain for gambling keywords: ["casino", "poker", "betting", "win", "lottery"]
     * - Check domain for essay mill keywords: ["essay", "paper", "dissertation", "thesis"]
     * - Check title/description for education keywords: ["scholarship", "grant", "funding", "education"]
     * - MUST return true if domain has scammer keywords AND metadata has education keywords
     * - MUST be case-insensitive
     */
    boolean detectCrossCategory Spam(String domain, String title, String description);
}

/**
 * Result of spam analysis with verdict and detection reasons.
 */
public record SpamAnalysisResult(
    boolean isSpam,                    // true if spam detected, false otherwise
    String rejectionReason,            // Human-readable reason (e.g., "Keyword stuffing detected")
    SpamIndicator primaryIndicator,    // Primary spam indicator that triggered rejection
    double confidenceScore             // Confidence in spam detection (0.0-1.0)
) {
    /**
     * Create SPAM result with reason.
     */
    public static SpamAnalysisResult spam(SpamIndicator indicator, String reason, double confidence) {
        return new SpamAnalysisResult(true, reason, indicator, confidence);
    }

    /**
     * Create NOT-SPAM result.
     */
    public static SpamAnalysisResult notSpam() {
        return new SpamAnalysisResult(false, null, null, 0.0);
    }
}

/**
 * Enum of spam indicators for tracking.
 */
public enum SpamIndicator {
    KEYWORD_STUFFING,           // Unique word ratio < 0.5
    DOMAIN_METADATA_MISMATCH,   // Cosine similarity < 0.15
    UNNATURAL_KEYWORD_LIST,     // Missing common articles/prepositions
    CROSS_CATEGORY_SPAM         // Gambling/essay mill domain + education keywords
}
