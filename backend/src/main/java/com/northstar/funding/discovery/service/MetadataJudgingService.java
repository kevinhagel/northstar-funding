package com.northstar.funding.discovery.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.northstar.funding.discovery.service.dto.MetadataJudgment;
import com.northstar.funding.discovery.service.dto.SearchResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Metadata Judging Service
 *
 * Phase 1 of Two-Phase Processing Pipeline:
 * Judge candidates based on search engine metadata ONLY (no web crawling).
 *
 * Judging Criteria:
 * 1. Funding Keywords: Does title/snippet mention grants, funding, scholarships, etc.?
 * 2. Domain Credibility: Is domain known/credible? (foundation.org, .gov, .edu)
 * 3. Geographic Relevance: Mentions Bulgaria, Eastern Europe, Balkans, EU?
 * 4. Organization Type: Mentions foundations, NGOs, government agencies?
 *
 * Decision Threshold:
 * - Confidence >= 0.60: Proceed to Phase 2 (deep crawl)
 * - Confidence < 0.60: Skip (save as SKIPPED_LOW_CONFIDENCE)
 *
 * Constitutional Principles:
 * - No web crawling in Phase 1 (lightweight, fast)
 * - Focus on Eastern Europe geography
 * - "Funding Sources" ubiquitous language (grants, scholarships, fellowships, etc.)
 * - BigDecimal with scale 2 for all confidence scores (precision)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataJudgingService {

    private final DomainRegistryService domainRegistryService;

    // Confidence threshold for proceeding to Phase 2 (0.60 with scale 2)
    private static final BigDecimal CRAWL_THRESHOLD = new BigDecimal("0.60");

    // Funding-related keywords
    private static final Set<String> FUNDING_KEYWORDS = Set.of(
        "grant", "grants", "funding", "scholarship", "scholarships",
        "fellowship", "fellowships", "award", "awards", "prize", "prizes",
        "sponsorship", "fund", "funds", "financial support", "financial assistance",
        "subsidy", "subsidies", "endowment", "donation", "donations",
        "investment", "investments", "loan", "loans", "credit", "credits"
    );

    // Credible domain extensions
    private static final Set<String> CREDIBLE_TLDS = Set.of(
        ".org", ".gov", ".edu", ".eu", ".int", ".foundation"
    );

    // Geographic keywords for Eastern Europe focus
    private static final Set<String> GEOGRAPHIC_KEYWORDS = Set.of(
        "bulgaria", "bulgarian", "eastern europe", "balkans", "balkan",
        "sofia", "plovdiv", "varna", "burgas",
        "eu", "european union", "europe", "european",
        "romania", "romanian", "greece", "greek", "serbia", "serbian",
        "north macedonia", "macedonia", "albania", "albanian",
        "kosovo", "montenegro", "croatia", "croatian",
        "bosnia", "herzegovina", "slovenia", "slovenian"
    );

    // Organization type keywords
    private static final Set<String> ORGANIZATION_TYPE_KEYWORDS = Set.of(
        "foundation", "foundations", "ngo", "charity", "charities",
        "nonprofit", "non-profit", "trust", "trusts",
        "agency", "agencies", "ministry", "ministries",
        "commission", "council", "institute", "institutes",
        "organization", "organisation", "association"
    );

    /**
     * Judge a search result based on metadata only
     *
     * No web crawling - analyzes only title, snippet, and domain.
     *
     * @param searchResult Search result from search engine
     * @return Metadata judgment with confidence score
     */
    public MetadataJudgment judgeSearchResult(SearchResult searchResult) {
        List<MetadataJudgment.JudgeScore> judgeScores = new ArrayList<>();

        // Judge 1: Funding Keywords
        MetadataJudgment.JudgeScore fundingScore = judgeFundingKeywords(searchResult);
        judgeScores.add(fundingScore);

        // Judge 2: Domain Credibility
        MetadataJudgment.JudgeScore domainScore = judgeDomainCredibility(searchResult);
        judgeScores.add(domainScore);

        // Judge 3: Geographic Relevance
        MetadataJudgment.JudgeScore geoScore = judgeGeographicRelevance(searchResult);
        judgeScores.add(geoScore);

        // Judge 4: Organization Type
        MetadataJudgment.JudgeScore orgTypeScore = judgeOrganizationType(searchResult);
        judgeScores.add(orgTypeScore);

        // Calculate overall confidence (weighted average)
        BigDecimal overallConfidence = calculateOverallConfidence(judgeScores);

        // Extract domain name
        String domainName = domainRegistryService.extractDomainName(searchResult.getUrl())
            .getOrElse("unknown");

        // Extract organization and program names from title
        String[] extracted = extractOrganizationAndProgram(searchResult.getTitle());

        // Build judgment
        MetadataJudgment judgment = MetadataJudgment.builder()
            .confidenceScore(overallConfidence)
            .shouldCrawl(overallConfidence.compareTo(CRAWL_THRESHOLD) >= 0)
            .domainName(domainName)
            .judgeScores(judgeScores)
            .reasoning(buildReasoning(judgeScores, overallConfidence))
            .extractedOrganizationName(extracted[0])
            .extractedProgramName(extracted[1])
            .build();

        log.debug("Judged search result: {} (confidence: {}, shouldCrawl: {})",
            searchResult.getUrl(), overallConfidence, judgment.getShouldCrawl());

        return judgment;
    }

    /**
     * Judge 1: Funding Keywords
     *
     * Does title or snippet mention funding-related terms?
     */
    private MetadataJudgment.JudgeScore judgeFundingKeywords(SearchResult searchResult) {
        String text = (searchResult.getTitle() + " " + searchResult.getSnippet()).toLowerCase();

        long matchCount = FUNDING_KEYWORDS.stream()
            .filter(text::contains)
            .count();

        // 3+ keywords = 1.00, scale linearly
        BigDecimal score = BigDecimal.valueOf(matchCount)
            .divide(new BigDecimal("3.00"), 2, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE);

        String explanation = matchCount > 0
            ? String.format("Found %d funding keywords", matchCount)
            : "No funding keywords found";

        return MetadataJudgment.JudgeScore.builder()
            .judgeName("FundingKeywordJudge")
            .score(score)
            .weight(new BigDecimal("2.00")) // Most important judge
            .explanation(explanation)
            .build();
    }

    /**
     * Judge 2: Domain Credibility
     *
     * Is the domain credible? (.org, .gov, .edu, etc.)
     */
    private MetadataJudgment.JudgeScore judgeDomainCredibility(SearchResult searchResult) {
        String url = searchResult.getUrl().toLowerCase();

        // Check for credible TLDs
        boolean hasCredibleTld = CREDIBLE_TLDS.stream()
            .anyMatch(url::contains);

        // Check for known scam patterns
        boolean hasScamPattern = url.contains("click") ||
                                 url.contains("ad.") ||
                                 url.contains("promo") ||
                                 url.contains("offer");

        BigDecimal score;
        String explanation;

        if (hasScamPattern) {
            score = BigDecimal.ZERO;
            explanation = "Suspicious domain pattern";
        } else if (hasCredibleTld) {
            score = new BigDecimal("0.80");
            explanation = "Credible domain extension";
        } else {
            score = new BigDecimal("0.50");
            explanation = "Unknown domain - neutral score";
        }

        return MetadataJudgment.JudgeScore.builder()
            .judgeName("DomainCredibilityJudge")
            .score(score)
            .weight(new BigDecimal("1.50")) // Important for filtering scams
            .explanation(explanation)
            .build();
    }

    /**
     * Judge 3: Geographic Relevance
     *
     * Does title/snippet mention Eastern Europe, Bulgaria, Balkans, EU?
     */
    private MetadataJudgment.JudgeScore judgeGeographicRelevance(SearchResult searchResult) {
        String text = (searchResult.getTitle() + " " + searchResult.getSnippet()).toLowerCase();

        long matchCount = GEOGRAPHIC_KEYWORDS.stream()
            .filter(text::contains)
            .count();

        // 2+ geographic keywords = 1.00, scale linearly
        BigDecimal score = BigDecimal.valueOf(matchCount)
            .divide(new BigDecimal("2.00"), 2, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE);

        String explanation = matchCount > 0
            ? String.format("Found %d geographic keywords (Eastern Europe focus)", matchCount)
            : "No geographic keywords - may be global or US-focused";

        return MetadataJudgment.JudgeScore.builder()
            .judgeName("GeographicRelevanceJudge")
            .score(score)
            .weight(new BigDecimal("1.00"))
            .explanation(explanation)
            .build();
    }

    /**
     * Judge 4: Organization Type
     *
     * Does title/snippet mention foundations, NGOs, government agencies?
     */
    private MetadataJudgment.JudgeScore judgeOrganizationType(SearchResult searchResult) {
        String text = (searchResult.getTitle() + " " + searchResult.getSnippet()).toLowerCase();

        long matchCount = ORGANIZATION_TYPE_KEYWORDS.stream()
            .filter(text::contains)
            .count();

        // 2+ org type keywords = 1.00, scale linearly
        BigDecimal score = BigDecimal.valueOf(matchCount)
            .divide(new BigDecimal("2.00"), 2, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE);

        String explanation = matchCount > 0
            ? String.format("Found %d organization type keywords", matchCount)
            : "No organization type keywords";

        return MetadataJudgment.JudgeScore.builder()
            .judgeName("OrganizationTypeJudge")
            .score(score)
            .weight(new BigDecimal("0.80"))
            .explanation(explanation)
            .build();
    }

    /**
     * Calculate overall confidence score from individual judge scores
     *
     * Weighted average: sum(score * weight) / sum(weight)
     * Result is rounded to 2 decimal places using HALF_UP
     */
    private BigDecimal calculateOverallConfidence(List<MetadataJudgment.JudgeScore> judgeScores) {
        BigDecimal weightedSum = judgeScores.stream()
            .map(j -> j.getScore().multiply(j.getWeight()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeight = judgeScores.stream()
            .map(MetadataJudgment.JudgeScore::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return weightedSum
            .divide(totalWeight, 2, RoundingMode.HALF_UP)
            .min(BigDecimal.ONE);
    }

    /**
     * Build reasoning text from judge scores
     */
    private String buildReasoning(List<MetadataJudgment.JudgeScore> judgeScores, BigDecimal overallConfidence) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append(String.format("Overall confidence: %.2f. ", overallConfidence));

        for (MetadataJudgment.JudgeScore score : judgeScores) {
            reasoning.append(String.format("%s: %.2f (%s). ",
                score.getJudgeName(),
                score.getScore(),
                score.getExplanation()));
        }

        return reasoning.toString();
    }

    /**
     * Extract organization and program names from title
     *
     * Simple heuristic: Split on "-" or "|"
     * Example: "Bulgaria Education Grant - US-Bulgaria Foundation"
     *   → Organization: "US-Bulgaria Foundation"
     *   → Program: "Bulgaria Education Grant"
     *
     * @param title Search result title
     * @return Array [organizationName, programName]
     */
    private String[] extractOrganizationAndProgram(String title) {
        if (title == null || title.isBlank()) {
            return new String[]{"Unknown Organization", "Unknown Program"};
        }

        // Try splitting on common separators
        String[] parts = title.split("[-|]");

        if (parts.length >= 2) {
            return new String[]{
                parts[parts.length - 1].trim(), // Last part likely org name
                parts[0].trim()                 // First part likely program name
            };
        }

        // Fallback: use full title as program name
        return new String[]{"Unknown Organization", title.trim()};
    }
}
