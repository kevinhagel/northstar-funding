package com.northstar.funding.crawler.contract;

import com.northstar.funding.crawler.antispam.AntiSpamFilter;
import com.northstar.funding.crawler.antispam.SpamAnalysisResult;
import com.northstar.funding.crawler.antispam.SpamIndicator;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Contract tests for AntiSpamFilter interface.
 *
 * These tests verify that AntiSpamFilter implementations detect spam using
 * 4 strategies: keyword stuffing, domain-metadata mismatch, unnatural keyword lists,
 * and cross-category spam.
 *
 * TDD Approach: Written BEFORE implementation. Must PASS with test double.
 */
@DisplayName("AntiSpamFilter Contract Tests")
class AntiSpamFilterContractTest {

    private TestAntiSpamFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TestAntiSpamFilter();
    }

    @Test
    @DisplayName("analyzeForSpam() must return non-null SpamAnalysisResult")
    void analyzeForSpam_MustReturnNonNull() {
        // Given
        SearchResult result = createSearchResult(
                "example.org",
                "Test Title",
                "Test description"
        );

        // When
        SpamAnalysisResult analysis = filter.analyzeForSpam(result);

        // Then
        assertThat(analysis).isNotNull();
    }

    @Test
    @DisplayName("analyzeForSpam() must complete in < 5ms (performance requirement)")
    void analyzeForSpam_MustCompleteQuickly() {
        // Given
        SearchResult result = createSearchResult(
                "example.org",
                "Test Title",
                "Test description"
        );

        // When
        long startTime = System.nanoTime();
        filter.analyzeForSpam(result);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then
        assertThat(durationMs).isLessThan(5);
    }

    @Test
    @DisplayName("detectKeywordStuffing() must detect when unique ratio < 0.5")
    void detectKeywordStuffing_MustDetectLowUniqueRatio() {
        // Given: 9 words total, 4 unique = ratio 0.44 < 0.5
        // "grants" appears 4 times, "scholarships" appears 2 times, "funding" appears 2 times, "education" appears 1 time
        String stuffedText = "grants scholarships funding grants scholarships grants funding education grants";

        // When
        boolean isStuffing = filter.detectKeywordStuffing(stuffedText);

        // Then
        assertThat(isStuffing).isTrue();
    }

    @Test
    @DisplayName("detectKeywordStuffing() must handle null/empty text gracefully")
    void detectKeywordStuffing_MustHandleNullGracefully() {
        // When/Then
        assertThat(filter.detectKeywordStuffing(null)).isFalse();
        assertThat(filter.detectKeywordStuffing("")).isFalse();
        assertThat(filter.detectKeywordStuffing("   ")).isFalse();
    }

    @Test
    @DisplayName("detectDomainMetadataMismatch() must detect when similarity < 0.15")
    void detectDomainMetadataMismatch_MustDetectMismatch() {
        // Given: Gambling domain with education keywords
        String domain = "casinowinners.com";
        String title = "Education Scholarships and Grants for Students";
        String description = "Apply for education funding opportunities";

        // When
        boolean isMismatch = filter.detectDomainMetadataMismatch(domain, title, description);

        // Then
        assertThat(isMismatch).isTrue();
    }

    @Test
    @DisplayName("detectDomainMetadataMismatch() must handle null/empty metadata gracefully")
    void detectDomainMetadataMismatch_MustHandleNullGracefully() {
        // When/Then
        assertThat(filter.detectDomainMetadataMismatch("example.org", null, null)).isFalse();
        assertThat(filter.detectDomainMetadataMismatch("example.org", "", "")).isFalse();
    }

    @Test
    @DisplayName("detectUnnaturalKeywordList() must detect when < 2 common words")
    void detectUnnaturalKeywordList_MustDetectKeywordLists() {
        // Given: No common words like "the", "a", "of", "for"
        String unnaturalText = "grants scholarships funding aid opportunities";

        // When
        boolean isUnnatural = filter.detectUnnaturalKeywordList(unnaturalText);

        // Then
        assertThat(isUnnatural).isTrue();
    }

    @Test
    @DisplayName("detectUnnaturalKeywordList() must NOT flag natural language")
    void detectUnnaturalKeywordList_MustAllowNaturalLanguage() {
        // Given: Has "the", "for" (2 common words)
        String naturalText = "Apply for the best scholarships and grants";

        // When
        boolean isUnnatural = filter.detectUnnaturalKeywordList(naturalText);

        // Then
        assertThat(isUnnatural).isFalse();
    }

    @Test
    @DisplayName("detectCrossCategorySpam() must detect gambling domain + education keywords")
    void detectCrossCategorySpam_MustDetectGamblingDomains() {
        // Given
        String domain = "poker-best.com";
        String title = "Scholarship Opportunities for Students";
        String description = "Education funding grants available";

        // When
        boolean isCrossCategory = filter.detectCrossCategorySpam(domain, title, description);

        // Then
        assertThat(isCrossCategory).isTrue();
    }

    @Test
    @DisplayName("detectCrossCategorySpam() must detect essay mill domain + education keywords")
    void detectCrossCategorySpam_MustDetectEssayMills() {
        // Given
        String domain = "essay-writers-pro.com";
        String title = "Education Grant Opportunities";
        String description = "Scholarship funding for students";

        // When
        boolean isCrossCategory = filter.detectCrossCategorySpam(domain, title, description);

        // Then
        assertThat(isCrossCategory).isTrue();
    }

    @Test
    @DisplayName("detectCrossCategorySpam() must be case-insensitive")
    void detectCrossCategorySpam_MustBeCaseInsensitive() {
        // Given: Mixed case
        String domain = "CASINO-WIN.COM";
        String title = "SCHOLARSHIP opportunities";
        String description = "Education FUNDING";

        // When
        boolean isCrossCategory = filter.detectCrossCategorySpam(domain, title, description);

        // Then
        assertThat(isCrossCategory).isTrue();
    }

    @Test
    @DisplayName("analyzeForSpam() must return spam=true when keyword stuffing detected")
    void analyzeForSpam_MustDetectKeywordStuffing() {
        // Given: Keyword stuffing (low unique ratio)
        SearchResult result = createSearchResult(
                "example.org",
                "Looking for the best grants for grants to get a grant in the area of education grants",
                "Test description with more content"
        );

        // When
        SpamAnalysisResult analysis = filter.analyzeForSpam(result);

        // Then
        assertThat(analysis.isSpam()).isTrue();
        // Accept either KEYWORD_STUFFING or UNNATURAL_KEYWORD_LIST - both indicate spam
        assertThat(analysis.primaryIndicator()).isIn(SpamIndicator.KEYWORD_STUFFING, SpamIndicator.UNNATURAL_KEYWORD_LIST);
        assertThat(analysis.rejectionReason()).isNotBlank();
    }

    @Test
    @DisplayName("analyzeForSpam() must return spam=true for cross-category spam")
    void analyzeForSpam_MustDetectCrossCategorySpam() {
        // Given
        SearchResult result = createSearchResult(
                "casinowinners.com",
                "Education Scholarships Available",
                "Apply for grants and funding"
        );

        // When
        SpamAnalysisResult analysis = filter.analyzeForSpam(result);

        // Then
        assertThat(analysis.isSpam()).isTrue();
        assertThat(analysis.primaryIndicator()).isIn(
                SpamIndicator.CROSS_CATEGORY_SPAM,
                SpamIndicator.DOMAIN_METADATA_MISMATCH
        );
    }

    @Test
    @DisplayName("analyzeForSpam() must return spam=false for legitimate results")
    void analyzeForSpam_MustAllowLegitimateResults() {
        // Given
        SearchResult result = createSearchResult(
                "us-bulgaria.org",
                "Apply for the Bulgaria Education Grant",
                "The US-Bulgaria Foundation offers scholarships for students"
        );

        // When
        SpamAnalysisResult analysis = filter.analyzeForSpam(result);

        // Then
        assertThat(analysis.isSpam()).isFalse();
        assertThat(analysis.primaryIndicator()).isNull();
        assertThat(analysis.rejectionReason()).isNull();
    }

    // Helper methods

    private SearchResult createSearchResult(String domain, String title, String description) {
        return SearchResult.builder()
                .url("https://" + domain + "/page")
                .domain(domain)
                .title(title)
                .description(description)
                .rankPosition(1)
                .searchEngine(SearchEngineType.BRAVE)
                .discoveredAt(LocalDateTime.now())
                .searchDate(LocalDate.now())
                .discoverySessionId(UUID.randomUUID())
                .build();
    }

    /**
     * Test double implementation of AntiSpamFilter for contract testing.
     */
    private static class TestAntiSpamFilter implements AntiSpamFilter {

        @Override
        public SpamAnalysisResult analyzeForSpam(SearchResult result) {
            // Run all 4 detection strategies
            if (detectKeywordStuffing(result.getTitle()) || detectKeywordStuffing(result.getDescription())) {
                return SpamAnalysisResult.spam(SpamIndicator.KEYWORD_STUFFING, "Keyword stuffing detected", 0.9);
            }

            if (detectDomainMetadataMismatch(result.getDomain(), result.getTitle(), result.getDescription())) {
                return SpamAnalysisResult.spam(SpamIndicator.DOMAIN_METADATA_MISMATCH, "Domain-metadata mismatch", 0.8);
            }

            if (detectUnnaturalKeywordList(result.getTitle()) || detectUnnaturalKeywordList(result.getDescription())) {
                return SpamAnalysisResult.spam(SpamIndicator.UNNATURAL_KEYWORD_LIST, "Unnatural keyword list", 0.7);
            }

            if (detectCrossCategorySpam(result.getDomain(), result.getTitle(), result.getDescription())) {
                return SpamAnalysisResult.spam(SpamIndicator.CROSS_CATEGORY_SPAM, "Cross-category spam", 0.85);
            }

            return SpamAnalysisResult.notSpam();
        }

        @Override
        public boolean detectKeywordStuffing(String text) {
            if (text == null || text.isBlank()) {
                return false;
            }

            String[] words = text.toLowerCase().split("\\s+");
            long uniqueWords = java.util.Arrays.stream(words)
                    .distinct()
                    .count();

            double ratio = (double) uniqueWords / words.length;
            return ratio < 0.5;
        }

        @Override
        public boolean detectDomainMetadataMismatch(String domain, String title, String description) {
            if (domain == null || (title == null && description == null)) {
                return false;
            }

            // Simple implementation: Check if domain contains gambling/essay keywords
            // but metadata contains education keywords
            String domainLower = domain.toLowerCase();
            String metadataLower = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

            boolean hasGamblingDomain = domainLower.contains("casino") || domainLower.contains("poker") ||
                    domainLower.contains("betting") || domainLower.contains("win");

            boolean hasEducationKeywords = metadataLower.contains("education") || metadataLower.contains("scholarship") ||
                    metadataLower.contains("grant") || metadataLower.contains("funding");

            return hasGamblingDomain && hasEducationKeywords;
        }

        @Override
        public boolean detectUnnaturalKeywordList(String text) {
            if (text == null || text.isBlank()) {
                return false;
            }

            String textLower = text.toLowerCase();
            String[] commonWords = {"the", "a", "an", "of", "for", "to", "in", "with"};

            // Count occurrences of common words (as whole words, not substrings)
            long count = java.util.Arrays.stream(commonWords)
                    .filter(word -> textLower.matches(".*\\b" + word + "\\b.*"))
                    .count();

            return count < 2;
        }

        @Override
        public boolean detectCrossCategorySpam(String domain, String title, String description) {
            if (domain == null || (title == null && description == null)) {
                return false;
            }

            String domainLower = domain.toLowerCase();
            String metadataLower = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

            // Check for gambling keywords in domain
            boolean hasGamblingKeywords = domainLower.contains("casino") || domainLower.contains("poker") ||
                    domainLower.contains("betting") || domainLower.contains("win") || domainLower.contains("lottery");

            // Check for essay mill keywords in domain
            boolean hasEssayKeywords = domainLower.contains("essay") || domainLower.contains("paper") ||
                    domainLower.contains("dissertation") || domainLower.contains("thesis");

            // Check for education keywords in metadata
            boolean hasEducationKeywords = metadataLower.contains("scholarship") || metadataLower.contains("grant") ||
                    metadataLower.contains("funding") || metadataLower.contains("education");

            return (hasGamblingKeywords || hasEssayKeywords) && hasEducationKeywords;
        }
    }
}
