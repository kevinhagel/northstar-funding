package com.northstar.funding.discovery.service.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata Judgment Result
 *
 * Phase 1 judging decision based on search engine metadata only.
 * No web crawling performed - just analyzing title, snippet, domain, etc.
 *
 * Decision Threshold:
 * - Confidence >= 0.6: PROCEED to Phase 2 (deep crawl)
 * - Confidence < 0.6: SKIP (not worth crawling)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataJudgment {

    /**
     * Overall confidence score (0.00-1.00)
     * Uses BigDecimal with scale 2 for precise decimal arithmetic
     * Aggregated from individual judge scores
     */
    private BigDecimal confidenceScore;

    /**
     * Should this candidate proceed to Phase 2 (deep crawling)?
     * True if confidenceScore >= 0.6
     */
    private Boolean shouldCrawl;

    /**
     * Extracted domain name from URL
     * Example: "us-bulgaria.org"
     */
    private String domainName;

    /**
     * Individual judge scores and explanations
     */
    @Builder.Default
    private List<JudgeScore> judgeScores = new ArrayList<>();

    /**
     * Overall reasoning for the judgment
     */
    private String reasoning;

    /**
     * Extracted organization name (if detected from metadata)
     * Example: "US-Bulgaria Foundation"
     */
    private String extractedOrganizationName;

    /**
     * Extracted program name (if detected from metadata)
     * Example: "Education Grant Program"
     */
    private String extractedProgramName;

    /**
     * Individual Judge Score
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeScore {
        /**
         * Name of the judge
         * Examples: "FundingKeywordJudge", "DomainCredibilityJudge", "GeographicRelevanceJudge"
         */
        private String judgeName;

        /**
         * Score from this judge (0.00-1.00)
         * Uses BigDecimal with scale 2 for precise decimal arithmetic
         */
        private BigDecimal score;

        /**
         * Weight of this judge in overall score
         * Uses BigDecimal for precise weighting
         * Default: 1.00 (equal weight)
         */
        @Builder.Default
        private BigDecimal weight = new BigDecimal("1.00");

        /**
         * Explanation from this judge
         */
        private String explanation;
    }
}
