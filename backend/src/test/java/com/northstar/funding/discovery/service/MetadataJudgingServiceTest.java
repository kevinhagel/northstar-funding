package com.northstar.funding.discovery.service;

import com.northstar.funding.discovery.service.dto.MetadataJudgment;
import com.northstar.funding.discovery.service.dto.SearchResult;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MetadataJudgingService
 *
 * Tests Phase 1 metadata-based judging logic (no web crawling).
 * Uses Mockito to mock DomainRegistryService.
 *
 * Critical TDD Flow:
 * 1. Write this test FIRST (T004)
 * 2. Tests should PASS (service already implemented)
 * 3. Verify all judging criteria and confidence calculations
 */
@ExtendWith(MockitoExtension.class)
class MetadataJudgingServiceTest {

    @Mock
    private DomainRegistryService domainRegistryService;

    @InjectMocks
    private MetadataJudgingService metadataJudgingService;

    @BeforeEach
    void setUp() {
        // Mock domain extraction to return domain name
        when(domainRegistryService.extractDomainName(anyString())).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("us-bulgaria.org")) {
                return Try.success("us-bulgaria.org");
            } else if (url.contains("example.org")) {
                return Try.success("example.org");
            } else if (url.contains("example.gov")) {
                return Try.success("example.gov");
            } else if (url.contains("spamsite.com")) {
                return Try.success("spamsite.com");
            } else if (url.contains("click-here.net")) {
                return Try.success("click-here.net");
            } else {
                return Try.success("unknown.com");
            }
        });
    }

    // ===== Test High Confidence Scenarios (>= 0.6) =====

    @Test
    void testJudgeSearchResult_HighConfidence_AllCriteriaMet_ShouldCrawl() {
        // Given: Search result with strong funding keywords, credible domain, geographic relevance
        SearchResult searchResult = SearchResult.builder()
            .url("https://us-bulgaria.org/grants/education")
            .title("Bulgaria Education Grant Program - US-Bulgaria Foundation")
            .snippet("Support for education initiatives in Bulgaria. Scholarships and grants available for students.")
            .searchEngine("searxng")
            .searchQuery("Bulgaria education grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: High confidence, should crawl
        assertThat(judgment.getConfidenceScore()).isGreaterThanOrEqualTo(new BigDecimal("0.60"));
        assertThat(judgment.getShouldCrawl()).isTrue();
        assertThat(judgment.getDomainName()).isEqualTo("us-bulgaria.org");
        assertThat(judgment.getJudgeScores()).hasSize(4); // 4 judges
        assertThat(judgment.getReasoning()).isNotBlank();

        // Verify individual judge scores
        assertThat(judgment.getJudgeScores())
            .extracting(MetadataJudgment.JudgeScore::getJudgeName)
            .containsExactlyInAnyOrder(
                "FundingKeywordJudge",
                "DomainCredibilityJudge",
                "GeographicRelevanceJudge",
                "OrganizationTypeJudge"
            );

        // FundingKeywordJudge should have highest weight
        MetadataJudgment.JudgeScore fundingScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("FundingKeywordJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(fundingScore.getWeight()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(fundingScore.getScore()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void testJudgeSearchResult_StrongFundingKeywords_ShouldCrawl() {
        // Given: Strong funding keywords (multiple: grant, scholarship, fellowship)
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/funding")
            .title("Grants, Scholarships, and Fellowships for Students")
            .snippet("Apply for our grant program. Scholarships and fellowships available.")
            .searchEngine("tavily")
            .searchQuery("education grants")
            .position(2)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Should crawl (strong funding keywords)
        assertThat(judgment.getShouldCrawl()).isTrue();
        assertThat(judgment.getConfidenceScore()).isGreaterThanOrEqualTo(new BigDecimal("0.60"));

        // Verify funding keyword score
        MetadataJudgment.JudgeScore fundingScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("FundingKeywordJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(fundingScore.getScore()).isGreaterThan(new BigDecimal("0.50")); // Found 3+ keywords
        assertThat(fundingScore.getExplanation()).contains("funding keywords");
    }

    @Test
    void testJudgeSearchResult_CredibleDomain_OrgExtension_HighScore() {
        // Given: Credible .org domain with funding keywords
        SearchResult searchResult = SearchResult.builder()
            .url("https://foundation.org/grants")
            .title("Grant Program for Education")
            .snippet("Our foundation provides grants for education.")
            .searchEngine("searxng")
            .searchQuery("education grants foundation")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Domain credibility judge scores high
        MetadataJudgment.JudgeScore domainScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("DomainCredibilityJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(domainScore.getScore()).isEqualByComparingTo(new BigDecimal("0.80")); // Credible TLD
        assertThat(domainScore.getExplanation()).contains("Credible domain extension");
    }

    @Test
    void testJudgeSearchResult_GovernmentDomain_HighScore() {
        // Given: Government .gov domain
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.gov/funding")
            .title("Government Grant Program")
            .snippet("Apply for government grants and funding.")
            .searchEngine("searxng")
            .searchQuery("government grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Domain credibility judge scores high
        MetadataJudgment.JudgeScore domainScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("DomainCredibilityJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(domainScore.getScore()).isEqualByComparingTo(new BigDecimal("0.80")); // .gov is credible
    }

    @Test
    void testJudgeSearchResult_GeographicRelevance_Bulgaria_HighScore() {
        // Given: Mentions Bulgaria multiple times
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/bulgaria")
            .title("Bulgaria Education Grant Program")
            .snippet("Support for educational initiatives in Bulgaria and Eastern Europe.")
            .searchEngine("searxng")
            .searchQuery("Bulgaria grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Geographic relevance judge scores high
        MetadataJudgment.JudgeScore geoScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("GeographicRelevanceJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(geoScore.getScore()).isGreaterThan(new BigDecimal("0.50")); // Found 2+ geographic keywords
        assertThat(geoScore.getExplanation()).contains("geographic keywords");
    }

    @Test
    void testJudgeSearchResult_OrganizationType_Foundation_HighScore() {
        // Given: Mentions foundation, NGO, charity
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/about")
            .title("Foundation for Education - NGO Charity")
            .snippet("Our nonprofit foundation provides educational grants.")
            .searchEngine("tavily")
            .searchQuery("education foundation")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Organization type judge scores high
        MetadataJudgment.JudgeScore orgTypeScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("OrganizationTypeJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(orgTypeScore.getScore()).isGreaterThan(new BigDecimal("0.50")); // Found 2+ org type keywords
        assertThat(orgTypeScore.getExplanation()).contains("organization type keywords");
    }

    // ===== Test Low Confidence Scenarios (< 0.6) =====

    @Test
    void testJudgeSearchResult_LowConfidence_NoFundingKeywords_ShouldNotCrawl() {
        // Given: No funding keywords
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.com/about")
            .title("About Our Company")
            .snippet("We are a technology company based in Sofia.")
            .searchEngine("searxng")
            .searchQuery("Bulgaria companies")
            .position(5)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Low confidence, should NOT crawl
        assertThat(judgment.getConfidenceScore()).isLessThan(new BigDecimal("0.60"));
        assertThat(judgment.getShouldCrawl()).isFalse();

        // Verify funding keyword score is low
        MetadataJudgment.JudgeScore fundingScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("FundingKeywordJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(fundingScore.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fundingScore.getExplanation()).contains("No funding keywords");
    }

    @Test
    void testJudgeSearchResult_SuspiciousDomain_LowScore() {
        // Given: Suspicious domain with "click" pattern
        SearchResult searchResult = SearchResult.builder()
            .url("https://click-here.net/grants")
            .title("Click Here for Free Grants!")
            .snippet("Get free grant money now. Click here to apply.")
            .searchEngine("browserbase")
            .searchQuery("free grants")
            .position(10)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Domain credibility judge scores very low
        MetadataJudgment.JudgeScore domainScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("DomainCredibilityJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(domainScore.getScore()).isEqualByComparingTo(BigDecimal.ZERO); // Suspicious pattern
        assertThat(domainScore.getExplanation()).contains("Suspicious domain pattern");

        // Overall confidence should be low
        assertThat(judgment.getShouldCrawl()).isFalse();
    }

    @Test
    void testJudgeSearchResult_NoGeographicRelevance_LowScore() {
        // Given: No geographic keywords (global/US-focused)
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grants")
            .title("Global Education Grants")
            .snippet("Scholarships available for students worldwide.")
            .searchEngine("searxng")
            .searchQuery("global grants")
            .position(3)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Geographic relevance judge scores low
        MetadataJudgment.JudgeScore geoScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("GeographicRelevanceJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(geoScore.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(geoScore.getExplanation()).contains("No geographic keywords");
    }

    // ===== Test Edge Cases =====

    @Test
    void testJudgeSearchResult_NullTitle_HandlesGracefully() {
        // Given: Null title
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/page")
            .title(null)
            .snippet("Some snippet text with grant information.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Should handle gracefully (uses snippet only)
        assertThat(judgment).isNotNull();
        assertThat(judgment.getConfidenceScore()).isNotNull();
        assertThat(judgment.getExtractedOrganizationName()).isEqualTo("Unknown Organization");
        assertThat(judgment.getExtractedProgramName()).isEqualTo("Unknown Program");
    }

    @Test
    void testJudgeSearchResult_BlankTitle_HandlesGracefully() {
        // Given: Blank title
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/page")
            .title("   ")
            .snippet("Grants available for education.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Should handle gracefully
        assertThat(judgment).isNotNull();
        assertThat(judgment.getExtractedOrganizationName()).isEqualTo("Unknown Organization");
        assertThat(judgment.getExtractedProgramName()).isEqualTo("Unknown Program");
    }

    @Test
    void testJudgeSearchResult_NullSnippet_HandlesGracefully() {
        // Given: Null snippet
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/page")
            .title("Education Grant Program")
            .snippet(null)
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Should handle gracefully (uses title only)
        assertThat(judgment).isNotNull();
        assertThat(judgment.getConfidenceScore()).isNotNull();
    }

    @Test
    void testJudgeSearchResult_BoundaryTest_ExactlyThreshold() {
        // Given: Result designed to score exactly around 0.6 threshold
        // This is a boundary test - we want to verify >= 0.6 logic
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grants")
            .title("Grant Program")
            .snippet("Scholarships for students in Eastern Europe.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Verify shouldCrawl logic works correctly at boundary
        if (judgment.getConfidenceScore().compareTo(new BigDecimal("0.60")) >= 0) {
            assertThat(judgment.getShouldCrawl()).isTrue();
        } else {
            assertThat(judgment.getShouldCrawl()).isFalse();
        }
    }

    // ===== Test Organization and Program Extraction =====

    @Test
    void testJudgeSearchResult_ExtractOrgAndProgram_WithDash() {
        // Given: Title with dash separator
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grant")
            .title("Bulgaria Education Grant - US-Bulgaria Foundation")
            .snippet("Grants for education in Bulgaria.")
            .searchEngine("searxng")
            .searchQuery("Bulgaria grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Organization and program extracted
        // Note: With multiple dashes, last part is org, first part is program
        assertThat(judgment.getExtractedOrganizationName()).contains("Foundation");
        assertThat(judgment.getExtractedProgramName()).contains("Bulgaria Education Grant");
    }

    @Test
    void testJudgeSearchResult_ExtractOrgAndProgram_WithPipe() {
        // Given: Title with pipe separator
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grant")
            .title("Education Scholarship | Sofia University")
            .snippet("Scholarships available for students.")
            .searchEngine("tavily")
            .searchQuery("scholarship")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Organization and program extracted
        assertThat(judgment.getExtractedOrganizationName()).contains("Sofia University");
        assertThat(judgment.getExtractedProgramName()).contains("Education Scholarship");
    }

    @Test
    void testJudgeSearchResult_ExtractOrgAndProgram_NoSeparator() {
        // Given: Title without separator
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grant")
            .title("Education Grant Program for Bulgarian Students")
            .snippet("Apply for grants.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Full title used as program name, org unknown
        assertThat(judgment.getExtractedOrganizationName()).isEqualTo("Unknown Organization");
        assertThat(judgment.getExtractedProgramName()).isEqualTo("Education Grant Program for Bulgarian Students");
    }

    // ===== Test Weighted Average Calculation =====

    @Test
    void testJudgeSearchResult_VerifyWeightedAverage() {
        // Given: Search result with known scores
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grant")
            .title("Grant Scholarship Fellowship")
            .snippet("Bulgaria Eastern Europe foundation.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Verify weighted average calculation using BigDecimal arithmetic
        BigDecimal expectedWeightedSum = judgment.getJudgeScores().stream()
            .map(j -> j.getScore().multiply(j.getWeight()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedTotalWeight = judgment.getJudgeScores().stream()
            .map(MetadataJudgment.JudgeScore::getWeight)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedConfidence = expectedWeightedSum
            .divide(expectedTotalWeight, 2, java.math.RoundingMode.HALF_UP)
            .min(BigDecimal.ONE);

        assertThat(judgment.getConfidenceScore()).isEqualByComparingTo(expectedConfidence);
    }

    @Test
    void testJudgeSearchResult_FundingKeywordJudge_HasHighestWeight() {
        // Given: Any search result
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/page")
            .title("Sample Title")
            .snippet("Sample snippet.")
            .searchEngine("searxng")
            .searchQuery("test")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: FundingKeywordJudge has weight 2.0 (highest)
        MetadataJudgment.JudgeScore fundingScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("FundingKeywordJudge"))
            .findFirst()
            .orElseThrow();

        BigDecimal maxWeight = judgment.getJudgeScores().stream()
            .map(MetadataJudgment.JudgeScore::getWeight)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        assertThat(fundingScore.getWeight()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(fundingScore.getWeight()).isEqualByComparingTo(maxWeight);
    }

    @Test
    void testJudgeSearchResult_DomainCredibilityJudge_HasWeight1Point5() {
        // Given: Any search result
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/page")
            .title("Sample Title")
            .snippet("Sample snippet.")
            .searchEngine("searxng")
            .searchQuery("test")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: DomainCredibilityJudge has weight 1.5
        MetadataJudgment.JudgeScore domainScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("DomainCredibilityJudge"))
            .findFirst()
            .orElseThrow();

        assertThat(domainScore.getWeight()).isEqualByComparingTo(new BigDecimal("1.50"));
    }

    // ===== Test Reasoning Generation =====

    @Test
    void testJudgeSearchResult_ReasoningContainsAllJudges() {
        // Given: Any search result
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grant")
            .title("Grant Program")
            .snippet("Scholarships available.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Reasoning mentions all judges
        assertThat(judgment.getReasoning()).contains("FundingKeywordJudge");
        assertThat(judgment.getReasoning()).contains("DomainCredibilityJudge");
        assertThat(judgment.getReasoning()).contains("GeographicRelevanceJudge");
        assertThat(judgment.getReasoning()).contains("OrganizationTypeJudge");
        assertThat(judgment.getReasoning()).contains("Overall confidence");
    }

    @Test
    void testJudgeSearchResult_ReasoningContainsScoresAndExplanations() {
        // Given: Search result with funding keywords
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grant")
            .title("Grant and Scholarship Program")
            .snippet("Funding available for students.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Reasoning contains scores and explanations
        for (MetadataJudgment.JudgeScore score : judgment.getJudgeScores()) {
            assertThat(judgment.getReasoning()).contains(score.getJudgeName());
            assertThat(judgment.getReasoning()).contains(score.getExplanation());
        }
    }

    // ===== Test Case-Insensitivity =====

    @Test
    void testJudgeSearchResult_CaseInsensitive_UppercaseKeywords() {
        // Given: Uppercase funding keywords
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/grant")
            .title("GRANT AND SCHOLARSHIP PROGRAM")
            .snippet("FUNDING AVAILABLE FOR STUDENTS IN BULGARIA.")
            .searchEngine("searxng")
            .searchQuery("grants")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Keywords detected despite uppercase
        MetadataJudgment.JudgeScore fundingScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("FundingKeywordJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(fundingScore.getScore()).isGreaterThan(BigDecimal.ZERO);
    }

    // ===== Test Multiple Keyword Matches =====

    @Test
    void testJudgeSearchResult_ThreeFundingKeywords_MaxScore() {
        // Given: 3+ funding keywords (should max out at 1.0)
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/funding")
            .title("Grants Scholarships Fellowships")
            .snippet("Awards and prizes available. Financial support for students.")
            .searchEngine("searxng")
            .searchQuery("funding")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Funding keyword score should be 1.0 or close to it
        MetadataJudgment.JudgeScore fundingScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("FundingKeywordJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(fundingScore.getScore()).isGreaterThanOrEqualTo(new BigDecimal("0.90"));
    }

    @Test
    void testJudgeSearchResult_TwoGeographicKeywords_MaxScore() {
        // Given: 2+ geographic keywords (should max out at 1.0)
        SearchResult searchResult = SearchResult.builder()
            .url("https://example.org/bulgaria")
            .title("Bulgaria Eastern Europe Program")
            .snippet("Support for students in the Balkans and European Union.")
            .searchEngine("searxng")
            .searchQuery("Bulgaria")
            .position(1)
            .build();

        // When: Judge search result
        MetadataJudgment judgment = metadataJudgingService.judgeSearchResult(searchResult);

        // Then: Geographic score should be 1.0 or close to it
        MetadataJudgment.JudgeScore geoScore = judgment.getJudgeScores().stream()
            .filter(s -> s.getJudgeName().equals("GeographicRelevanceJudge"))
            .findFirst()
            .orElseThrow();
        assertThat(geoScore.getScore()).isGreaterThanOrEqualTo(new BigDecimal("0.90"));
    }
}
