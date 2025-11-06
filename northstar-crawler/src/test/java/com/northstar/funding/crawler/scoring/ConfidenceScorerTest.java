package com.northstar.funding.crawler.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ConfidenceScorer
 * Tests keyword detection, geographic matching, organization type detection, and TLD integration
 */
@ExtendWith(MockitoExtension.class)
class ConfidenceScorerTest {

    @Mock
    private DomainCredibilityService domainCredibilityService;

    private ConfidenceScorer confidenceScorer;

    @BeforeEach
    void setUp() {
        confidenceScorer = new ConfidenceScorer(domainCredibilityService);
    }

    @Test
    @DisplayName("Base score: TLD only, no keywords")
    void testBaseScoreTldOnly() {
        // Given: .org domain (Tier 2 = +0.15), no funding keywords
        String title = "About Our Organization";
        String description = "We help people with various services";
        String url = "https://example.org";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.15"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: Only TLD score, no keyword boosts
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.15"));
    }

    @Test
    @DisplayName("Funding keywords in title add +0.15")
    void testFundingKeywordsInTitle() {
        // Given: .com domain (Tier 3 = +0.08), "grant" in title
        String title = "Apply for Education Grants Today";
        String description = "Learn more about our programs";
        String url = "https://example.com";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.08"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: 0.08 (TLD) + 0.15 (keyword in title) = 0.23
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.23"));
    }

    @Test
    @DisplayName("Funding keywords in description add +0.10")
    void testFundingKeywordsInDescription() {
        // Given: .org domain (Tier 2 = +0.15), "scholarship" in description
        String title = "Education Programs";
        String description = "We offer scholarships to students in need";
        String url = "https://example.org";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.15"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: 0.15 (TLD) + 0.10 (keyword in description) = 0.25
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.25"));
    }

    @Test
    @DisplayName("Funding keywords in both title and description")
    void testFundingKeywordsInBoth() {
        // Given: .com domain (Tier 3 = +0.08), keywords in both, NO other signals
        String title = "Grants Available Now";
        String description = "Scholarships offered here";
        String url = "https://example.com";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.08"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: 0.08 (TLD) + 0.15 (title) + 0.10 (description) = 0.33
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.33"));
    }

    @Test
    @DisplayName("Geographic match (Bulgaria/EU) adds +0.15")
    void testGeographicMatch() {
        // Given: .org domain, "Bulgaria" in description
        String title = "Education Support";
        String description = "Supporting students in Bulgaria and Eastern Europe";
        String url = "https://example.org";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.15"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: 0.15 (TLD) + 0.15 (geographic) = 0.30
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.30"));
    }

    @Test
    @DisplayName("Organization type detection adds +0.15")
    void testOrganizationType() {
        // Given: .com domain (Tier 3 = +0.08), "Ministry" in title, NO keywords/geographic
        String title = "Ministry of Education Official Portal";
        String description = "Information about educational policies and procedures";
        String url = "https://example.com";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.08"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: 0.08 (TLD) + 0.15 (organization type) = 0.23
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.23"));
    }

    @Test
    @DisplayName("Compound boost: Multiple signals add +0.15")
    void testCompoundBoost() {
        // Given: .ngo domain (Tier 1 = +0.20), keywords + geographic + org type
        String title = "European Commission Grants for Bulgaria";
        String description = "Apply for funding and scholarships today";
        String url = "https://example.ngo";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.20"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: 0.20 (TLD) + 0.15 (title keywords) + 0.10 (desc keywords)
        //       + 0.15 (geographic) + 0.15 (org type) + 0.15 (compound) = 0.90
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.90"));
    }

    @Test
    @DisplayName("Score capped at 1.00 maximum")
    void testScoreCappedAtMax() {
        // Given: .ngo domain (Tier 1 = +0.30 with geographic boost), all signals present
        // Force score above 1.00 to test capping
        String title = "European Commission Ministry Grants Funding Scholarships";
        String description = "Fellowship awards and financial aid for Bulgaria Romania Eastern Europe";
        String url = "https://european-foundation.ngo";

        // Use higher TLD score to push above 1.00
        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.30"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: Would be > 1.00, but capped at 1.00
        assertThat(confidence).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(confidence).isLessThanOrEqualTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Spam TLD (negative score) floored at 0.00")
    void testSpamTldFlooredAtZero() {
        // Given: .xyz domain (Tier 5 = -0.20), with keywords but NO geographic/org type
        String title = "Grants Available";
        String description = "Scholarships offered";
        String url = "https://spam-site.xyz";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("-0.20"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: -0.20 (TLD) + 0.15 (title keywords) + 0.10 (description keywords) = 0.05
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.05"));
        assertThat(confidence).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("All scores have scale 2 precision")
    void testBigDecimalPrecision() {
        // Given: Any input
        String title = "Grant funding";
        String description = "Scholarships available";
        String url = "https://example.org";

        when(domainCredibilityService.getTldScore(url))
            .thenReturn(new BigDecimal("0.15"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence(title, description, url);

        // Then: Scale should be 2
        assertThat(confidence.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Null inputs handled gracefully")
    void testNullInputsHandled() {
        // Given: Null title/description
        when(domainCredibilityService.getTldScore("https://example.org"))
            .thenReturn(new BigDecimal("0.15"));

        // When: Null title
        BigDecimal confidence1 = confidenceScorer.calculateConfidence(null, "description", "https://example.org");
        assertThat(confidence1).isEqualByComparingTo(new BigDecimal("0.15"));

        // When: Null description
        BigDecimal confidence2 = confidenceScorer.calculateConfidence("title", null, "https://example.org");
        assertThat(confidence2).isEqualByComparingTo(new BigDecimal("0.15"));

        // When: Both null
        BigDecimal confidence3 = confidenceScorer.calculateConfidence(null, null, "https://example.org");
        assertThat(confidence3).isEqualByComparingTo(new BigDecimal("0.15"));
    }

    @Test
    @DisplayName("Empty strings handled like null")
    void testEmptyStringsHandled() {
        // Given: Empty strings
        when(domainCredibilityService.getTldScore("https://example.org"))
            .thenReturn(new BigDecimal("0.15"));

        // When
        BigDecimal confidence = confidenceScorer.calculateConfidence("", "", "https://example.org");

        // Then: Only TLD score
        assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.15"));
    }
}
