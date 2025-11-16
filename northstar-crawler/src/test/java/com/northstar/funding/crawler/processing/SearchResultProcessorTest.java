package com.northstar.funding.crawler.processing;

import com.northstar.funding.crawler.scoring.CandidateCreationService;
import com.northstar.funding.crawler.scoring.ConfidenceScorer;
import com.northstar.funding.crawler.scoring.DomainCredibilityService;
import com.northstar.funding.persistence.repository.FundingSourceCandidateRepository;
import com.northstar.funding.persistence.service.DomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for SearchResultProcessor
 * Tests orchestration of search result processing pipeline
 */
@ExtendWith(MockitoExtension.class)
class SearchResultProcessorTest {

    @Mock
    private DomainCredibilityService domainCredibilityService;

    @Mock
    private ConfidenceScorer confidenceScorer;

    @Mock
    private CandidateCreationService candidateCreationService;

    @Mock
    private DomainService domainService;

    @Mock
    private FundingSourceCandidateRepository candidateRepository;

    private SearchResultProcessor searchResultProcessor;

    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID();
        searchResultProcessor = new SearchResultProcessor(
            domainCredibilityService,
            confidenceScorer,
            candidateCreationService,
            domainService,
            candidateRepository
        );
    }

    @Test
    @DisplayName("Empty search results return empty statistics")
    void testProcessEmptyResults() {
        // Given: Empty search results list
        List<SearchResult> emptyResults = Collections.emptyList();

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            emptyResults, testSessionId
        );

        // Then: All statistics should be zero
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalResults()).isZero();
        assertThat(stats.getSpamTldFiltered()).isZero();
        assertThat(stats.getBlacklistedSkipped()).isZero();
        assertThat(stats.getDuplicatesSkipped()).isZero();
        assertThat(stats.getHighConfidenceCreated()).isZero();
        assertThat(stats.getLowConfidenceCreated()).isZero();
        assertThat(stats.getTotalCandidatesCreated()).isZero();
    }

    @Test
    @DisplayName("Duplicate domains are tracked and skipped")
    void testDuplicateDomainsHandled() {
        // Given: 3 search results, 2 pointing to same domain
        SearchResult result1 = SearchResult.builder()
            .url("https://example.org/funding")
            .title("Example Funding Program")
            .description("Grants available")
            .build();

        SearchResult result2 = SearchResult.builder()
            .url("https://example.org/about")  // Same domain as result1
            .title("About Example Org")
            .description("Different page")
            .build();

        SearchResult result3 = SearchResult.builder()
            .url("https://different.com/grants")
            .title("Different Organization")
            .description("Other funding")
            .build();

        List<SearchResult> results = List.of(result1, result2, result3);

        // Mock domain extraction
        when(domainService.extractDomainFromUrl("https://example.org/funding"))
            .thenReturn(java.util.Optional.of("example.org"));
        when(domainService.extractDomainFromUrl("https://example.org/about"))
            .thenReturn(java.util.Optional.of("example.org"));
        when(domainService.extractDomainFromUrl("https://different.com/grants"))
            .thenReturn(java.util.Optional.of("different.com"));

        // Mock confidence scoring (high confidence for non-duplicates)
        when(confidenceScorer.calculateConfidence("Example Funding Program", "Grants available", "https://example.org/funding"))
            .thenReturn(new java.math.BigDecimal("0.85"));
        when(confidenceScorer.calculateConfidence("Different Organization", "Other funding", "https://different.com/grants"))
            .thenReturn(new java.math.BigDecimal("0.80"));

        // Mock domain registration
        com.northstar.funding.domain.Domain mockDomain1 = com.northstar.funding.domain.Domain.builder()
            .domainId(UUID.randomUUID())
            .domainName("example.org")
            .build();
        com.northstar.funding.domain.Domain mockDomain2 = com.northstar.funding.domain.Domain.builder()
            .domainId(UUID.randomUUID())
            .domainName("different.com")
            .build();
        when(domainService.registerOrGetDomain("example.org", testSessionId))
            .thenReturn(mockDomain1);
        when(domainService.registerOrGetDomain("different.com", testSessionId))
            .thenReturn(mockDomain2);

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Should process 3 results, skip 1 duplicate
        assertThat(stats.getTotalResults()).isEqualTo(3);
        assertThat(stats.getDuplicatesSkipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("Low confidence results create SKIPPED_LOW_CONFIDENCE candidates")
    void testLowConfidenceFiltered() {
        // Given: 2 search results with low confidence scores
        SearchResult result1 = SearchResult.builder()
            .url("https://lowconf1.com/maybe")
            .title("Maybe Funding")
            .description("Unclear if this is funding")
            .build();

        SearchResult result2 = SearchResult.builder()
            .url("https://lowconf2.org/perhaps")
            .title("Perhaps Grants")
            .description("Not sure about this")
            .build();

        List<SearchResult> results = List.of(result1, result2);

        // Mock domain extraction
        when(domainService.extractDomainFromUrl("https://lowconf1.com/maybe"))
            .thenReturn(java.util.Optional.of("lowconf1.com"));
        when(domainService.extractDomainFromUrl("https://lowconf2.org/perhaps"))
            .thenReturn(java.util.Optional.of("lowconf2.org"));

        // Mock confidence scoring - both below 0.6 threshold
        when(confidenceScorer.calculateConfidence("Maybe Funding", "Unclear if this is funding", "https://lowconf1.com/maybe"))
            .thenReturn(new java.math.BigDecimal("0.45"));
        when(confidenceScorer.calculateConfidence("Perhaps Grants", "Not sure about this", "https://lowconf2.org/perhaps"))
            .thenReturn(new java.math.BigDecimal("0.55"));

        // Mock domain registration
        when(domainService.registerOrGetDomain(anyString(), eq(testSessionId)))
            .thenReturn(com.northstar.funding.domain.Domain.builder().domainId(UUID.randomUUID()).build());

        // Mock candidate creation
        when(candidateCreationService.createCandidate(anyString(), anyString(), anyString(), any(), eq(testSessionId), any()))
            .thenReturn(com.northstar.funding.domain.FundingSourceCandidate.builder().build());

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Should process 2 results, create 2 low-confidence candidates
        // BUG FIX: lowConfidenceCreated now correctly tracked (was always 0 before)
        // DESIGN FIX: Low confidence results now create candidates with SKIPPED_LOW_CONFIDENCE status
        assertThat(stats.getTotalResults()).isEqualTo(2);
        assertThat(stats.getHighConfidenceCreated()).isZero();
        assertThat(stats.getLowConfidenceCreated()).isEqualTo(2);  // BUG FIX: Now correctly tracked
        assertThat(stats.getTotalCandidatesCreated()).isEqualTo(2);  // DESIGN FIX: Both create candidates
    }

    @Test
    @DisplayName("High confidence results create PENDING_CRAWL candidates")
    void testHighConfidenceCreatesCandidates() {
        // Given: 1 search result with high confidence score
        SearchResult result = SearchResult.builder()
            .url("https://highconf.org/grants")
            .title("European Union Research Grants")
            .description("Apply for Horizon Europe funding")
            .build();

        List<SearchResult> results = List.of(result);

        // Mock domain extraction
        when(domainService.extractDomainFromUrl("https://highconf.org/grants"))
            .thenReturn(java.util.Optional.of("highconf.org"));

        // Mock confidence scoring - above 0.6 threshold
        java.math.BigDecimal highConfidence = new java.math.BigDecimal("0.85");
        when(confidenceScorer.calculateConfidence("European Union Research Grants", "Apply for Horizon Europe funding", "https://highconf.org/grants"))
            .thenReturn(highConfidence);

        // Mock domain registration (registerOrGetDomain returns Domain with domainId)
        UUID domainId = UUID.randomUUID();
        com.northstar.funding.domain.Domain mockDomain = com.northstar.funding.domain.Domain.builder()
            .domainId(domainId)
            .domainName("highconf.org")
            .build();
        when(domainService.registerOrGetDomain("highconf.org", testSessionId))
            .thenReturn(mockDomain);

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Should create 1 PENDING_CRAWL candidate
        assertThat(stats.getTotalResults()).isEqualTo(1);
        assertThat(stats.getHighConfidenceCreated()).isEqualTo(1);
        assertThat(stats.getTotalCandidatesCreated()).isEqualTo(1);

        // Verify candidate creation service was called with correct parameters
        verify(candidateCreationService).createCandidate(
            eq("European Union Research Grants"),
            eq("Apply for Horizon Europe funding"),
            eq("https://highconf.org/grants"),
            eq(domainId),
            eq(testSessionId),
            eq(highConfidence)
        );
    }

    @Test
    @DisplayName("Blacklisted domains are skipped")
    void testBlacklistedDomainsSkipped() {
        // Given: 2 search results, 1 blacklisted domain
        SearchResult result1 = SearchResult.builder()
            .url("https://spam.com/fake-grants")
            .title("Free Money Grants")
            .description("Click here for free money")
            .build();

        SearchResult result2 = SearchResult.builder()
            .url("https://legitimate.org/real-grants")
            .title("Research Funding")
            .description("Apply for research grants")
            .build();

        List<SearchResult> results = List.of(result1, result2);

        // Mock domain extraction
        when(domainService.extractDomainFromUrl("https://spam.com/fake-grants"))
            .thenReturn(java.util.Optional.of("spam.com"));
        when(domainService.extractDomainFromUrl("https://legitimate.org/real-grants"))
            .thenReturn(java.util.Optional.of("legitimate.org"));

        // Mock blacklist check
        when(domainService.isBlacklisted("spam.com"))
            .thenReturn(true);
        when(domainService.isBlacklisted("legitimate.org"))
            .thenReturn(false);

        // Mock confidence scoring for non-blacklisted domain
        when(confidenceScorer.calculateConfidence("Research Funding", "Apply for research grants", "https://legitimate.org/real-grants"))
            .thenReturn(new java.math.BigDecimal("0.80"));

        // Mock domain registration for non-blacklisted
        UUID domainId = UUID.randomUUID();
        com.northstar.funding.domain.Domain mockDomain = com.northstar.funding.domain.Domain.builder()
            .domainId(domainId)
            .domainName("legitimate.org")
            .build();
        when(domainService.registerOrGetDomain("legitimate.org", testSessionId))
            .thenReturn(mockDomain);

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Should skip blacklisted domain, create 1 candidate
        assertThat(stats.getTotalResults()).isEqualTo(2);
        assertThat(stats.getBlacklistedSkipped()).isEqualTo(1);
        assertThat(stats.getHighConfidenceCreated()).isEqualTo(1);

        // Verify only legitimate domain created candidate
        verify(candidateCreationService, times(1)).createCandidate(
            eq("Research Funding"),
            eq("Apply for research grants"),
            eq("https://legitimate.org/real-grants"),
            eq(domainId),
            eq(testSessionId),
            eq(new java.math.BigDecimal("0.80"))
        );
    }

    @Test
    @DisplayName("Statistics are tracked correctly across all scenarios")
    void testStatisticsTracking() {
        // Given: 6 search results covering all scenarios
        SearchResult result1 = SearchResult.builder()
            .url("https://example.org/page1")
            .title("First Result")
            .description("High confidence")
            .build();

        SearchResult result2 = SearchResult.builder()
            .url("https://example.org/page2")  // Duplicate domain
            .title("Second Result")
            .description("Same domain")
            .build();

        SearchResult result3 = SearchResult.builder()
            .url("https://blacklist.com/page")  // Blacklisted
            .title("Blacklisted")
            .description("Should be skipped")
            .build();

        SearchResult result4 = SearchResult.builder()
            .url("https://lowconf.org/page")  // Low confidence
            .title("Low Quality")
            .description("Not relevant")
            .build();

        SearchResult result5 = SearchResult.builder()
            .url("https://highconf1.org/page")  // High confidence
            .title("Great Funding")
            .description("Excellent opportunity")
            .build();

        SearchResult result6 = SearchResult.builder()
            .url("https://highconf2.org/page")  // High confidence
            .title("More Funding")
            .description("Another opportunity")
            .build();

        List<SearchResult> results = List.of(result1, result2, result3, result4, result5, result6);

        // Mock domain extraction
        when(domainService.extractDomainFromUrl("https://example.org/page1"))
            .thenReturn(java.util.Optional.of("example.org"));
        when(domainService.extractDomainFromUrl("https://example.org/page2"))
            .thenReturn(java.util.Optional.of("example.org"));
        when(domainService.extractDomainFromUrl("https://blacklist.com/page"))
            .thenReturn(java.util.Optional.of("blacklist.com"));
        when(domainService.extractDomainFromUrl("https://lowconf.org/page"))
            .thenReturn(java.util.Optional.of("lowconf.org"));
        when(domainService.extractDomainFromUrl("https://highconf1.org/page"))
            .thenReturn(java.util.Optional.of("highconf1.org"));
        when(domainService.extractDomainFromUrl("https://highconf2.org/page"))
            .thenReturn(java.util.Optional.of("highconf2.org"));

        // Mock blacklist check
        when(domainService.isBlacklisted("example.org")).thenReturn(false);
        when(domainService.isBlacklisted("blacklist.com")).thenReturn(true);
        when(domainService.isBlacklisted("lowconf.org")).thenReturn(false);
        when(domainService.isBlacklisted("highconf1.org")).thenReturn(false);
        when(domainService.isBlacklisted("highconf2.org")).thenReturn(false);

        // Mock confidence scoring
        when(confidenceScorer.calculateConfidence("First Result", "High confidence", "https://example.org/page1"))
            .thenReturn(new java.math.BigDecimal("0.75"));
        when(confidenceScorer.calculateConfidence("Low Quality", "Not relevant", "https://lowconf.org/page"))
            .thenReturn(new java.math.BigDecimal("0.35"));
        when(confidenceScorer.calculateConfidence("Great Funding", "Excellent opportunity", "https://highconf1.org/page"))
            .thenReturn(new java.math.BigDecimal("0.85"));
        when(confidenceScorer.calculateConfidence("More Funding", "Another opportunity", "https://highconf2.org/page"))
            .thenReturn(new java.math.BigDecimal("0.90"));

        // Mock domain registration for all non-blacklisted domains
        when(domainService.registerOrGetDomain(eq("example.org"), eq(testSessionId)))
            .thenReturn(com.northstar.funding.domain.Domain.builder().domainId(UUID.randomUUID()).build());
        when(domainService.registerOrGetDomain(eq("lowconf.org"), eq(testSessionId)))
            .thenReturn(com.northstar.funding.domain.Domain.builder().domainId(UUID.randomUUID()).build());
        when(domainService.registerOrGetDomain(eq("highconf1.org"), eq(testSessionId)))
            .thenReturn(com.northstar.funding.domain.Domain.builder().domainId(UUID.randomUUID()).build());
        when(domainService.registerOrGetDomain(eq("highconf2.org"), eq(testSessionId)))
            .thenReturn(com.northstar.funding.domain.Domain.builder().domainId(UUID.randomUUID()).build());

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Verify all statistics
        // BUG FIX: lowConfidenceCreated now correctly tracked
        // DESIGN FIX: Low confidence results now create candidates too
        assertThat(stats.getTotalResults()).isEqualTo(6);
        assertThat(stats.getDuplicatesSkipped()).isEqualTo(1);  // result2 (example.org duplicate)
        assertThat(stats.getBlacklistedSkipped()).isEqualTo(1); // result3 (blacklist.com)
        assertThat(stats.getHighConfidenceCreated()).isEqualTo(3);  // result1, result5, result6
        assertThat(stats.getLowConfidenceCreated()).isEqualTo(1);  // BUG FIX: result4 now correctly tracked
        assertThat(stats.getTotalCandidatesCreated()).isEqualTo(4);  // DESIGN FIX: 3 high + 1 low

        // Verify 4 candidates created (3 high + 1 low confidence)
        verify(candidateCreationService, times(4)).createCandidate(
            any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("End-to-end processing with realistic mixed scenario")
    void testEndToEndProcessing() {
        // Given: Realistic search results from multiple search engines
        SearchResult horizonEurope = SearchResult.builder()
            .url("https://ec.europa.eu/research/participants/portal")
            .title("Horizon Europe - European Commission")
            .description("Research and Innovation funding programme")
            .build();

        SearchResult usBulgaria = SearchResult.builder()
            .url("https://us-bulgaria.org/grants")
            .title("US-Bulgaria Fulbright Commission")
            .description("Educational grants and scholarships")
            .build();

        SearchResult spamSite = SearchResult.builder()
            .url("https://free-money-now.scam/grants")
            .title("GET FREE MONEY NOW!!!")
            .description("Click here for instant cash grants")
            .build();

        SearchResult lowQuality = SearchResult.builder()
            .url("https://random-blog.net/funding")
            .title("Blog Post About Funding")
            .description("Random person's opinion on grants")
            .build();

        SearchResult duplicateHorizon = SearchResult.builder()
            .url("https://ec.europa.eu/programmes/horizon")  // Same domain
            .title("Horizon Europe Programmes")
            .description("Different page, same organization")
            .build();

        List<SearchResult> results = List.of(
            horizonEurope, usBulgaria, spamSite, lowQuality, duplicateHorizon
        );

        // Mock domain extraction
        when(domainService.extractDomainFromUrl("https://ec.europa.eu/research/participants/portal"))
            .thenReturn(java.util.Optional.of("ec.europa.eu"));
        when(domainService.extractDomainFromUrl("https://us-bulgaria.org/grants"))
            .thenReturn(java.util.Optional.of("us-bulgaria.org"));
        when(domainService.extractDomainFromUrl("https://free-money-now.scam/grants"))
            .thenReturn(java.util.Optional.of("free-money-now.scam"));
        when(domainService.extractDomainFromUrl("https://random-blog.net/funding"))
            .thenReturn(java.util.Optional.of("random-blog.net"));
        when(domainService.extractDomainFromUrl("https://ec.europa.eu/programmes/horizon"))
            .thenReturn(java.util.Optional.of("ec.europa.eu"));

        // Mock blacklist (spam site is blacklisted)
        when(domainService.isBlacklisted("ec.europa.eu")).thenReturn(false);
        when(domainService.isBlacklisted("us-bulgaria.org")).thenReturn(false);
        when(domainService.isBlacklisted("free-money-now.scam")).thenReturn(true);
        when(domainService.isBlacklisted("random-blog.net")).thenReturn(false);

        // Mock confidence scores
        when(confidenceScorer.calculateConfidence(
            "Horizon Europe - European Commission",
            "Research and Innovation funding programme",
            "https://ec.europa.eu/research/participants/portal"
        )).thenReturn(new java.math.BigDecimal("0.95"));  // Very high

        when(confidenceScorer.calculateConfidence(
            "US-Bulgaria Fulbright Commission",
            "Educational grants and scholarships",
            "https://us-bulgaria.org/grants"
        )).thenReturn(new java.math.BigDecimal("0.88"));  // High

        when(confidenceScorer.calculateConfidence(
            "Blog Post About Funding",
            "Random person's opinion on grants",
            "https://random-blog.net/funding"
        )).thenReturn(new java.math.BigDecimal("0.42"));  // Low

        // Mock domain registration for all non-blacklisted domains
        UUID euDomainId = UUID.randomUUID();
        UUID usBulgariaDomainId = UUID.randomUUID();
        UUID randomBlogDomainId = UUID.randomUUID();
        when(domainService.registerOrGetDomain("ec.europa.eu", testSessionId))
            .thenReturn(com.northstar.funding.domain.Domain.builder()
                .domainId(euDomainId)
                .domainName("ec.europa.eu")
                .build());
        when(domainService.registerOrGetDomain("us-bulgaria.org", testSessionId))
            .thenReturn(com.northstar.funding.domain.Domain.builder()
                .domainId(usBulgariaDomainId)
                .domainName("us-bulgaria.org")
                .build());
        when(domainService.registerOrGetDomain("random-blog.net", testSessionId))
            .thenReturn(com.northstar.funding.domain.Domain.builder()
                .domainId(randomBlogDomainId)
                .domainName("random-blog.net")
                .build());

        // When: Process all results
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Verify end-to-end statistics
        // BUG FIX: lowConfidenceCreated now correctly tracked
        // DESIGN FIX: Low confidence results now create candidates too
        assertThat(stats.getTotalResults()).isEqualTo(5);
        assertThat(stats.getDuplicatesSkipped()).isEqualTo(1);  // duplicateHorizon
        assertThat(stats.getBlacklistedSkipped()).isEqualTo(1);  // spamSite
        assertThat(stats.getHighConfidenceCreated()).isEqualTo(2);  // horizonEurope, usBulgaria
        assertThat(stats.getLowConfidenceCreated()).isEqualTo(1);  // BUG FIX: lowQuality now correctly tracked
        assertThat(stats.getTotalCandidatesCreated()).isEqualTo(3);  // DESIGN FIX: 2 high + 1 low

        // Verify specific candidates created with correct data
        verify(candidateCreationService).createCandidate(
            eq("Horizon Europe - European Commission"),
            eq("Research and Innovation funding programme"),
            eq("https://ec.europa.eu/research/participants/portal"),
            eq(euDomainId),
            eq(testSessionId),
            eq(new java.math.BigDecimal("0.95"))
        );

        verify(candidateCreationService).createCandidate(
            eq("US-Bulgaria Fulbright Commission"),
            eq("Educational grants and scholarships"),
            eq("https://us-bulgaria.org/grants"),
            eq(usBulgariaDomainId),
            eq(testSessionId),
            eq(new java.math.BigDecimal("0.88"))
        );

        verify(candidateCreationService).createCandidate(
            eq("Blog Post About Funding"),
            eq("Random person's opinion on grants"),
            eq("https://random-blog.net/funding"),
            eq(randomBlogDomainId),
            eq(testSessionId),
            eq(new java.math.BigDecimal("0.42"))
        );

        // Verify 3 candidates created (2 high confidence + 1 low confidence)
        verify(candidateCreationService, times(3)).createCandidate(
            any(), any(), any(), any(), any(), any()
        );
    }

    // ========== Pipeline Stage Tests (T008-T021) ==========

    @Test
    @DisplayName("extractAndValidateDomain - Valid URL returns domain")
    void extractAndValidateDomain_ValidUrl_ReturnsDomain() {
        // Given
        SearchResult result = SearchResult.builder()
            .url("https://example.org/grants")
            .build();
        ProcessingContext context = new ProcessingContext(testSessionId);
        when(domainService.extractDomainFromUrl("https://example.org/grants"))
            .thenReturn(java.util.Optional.of("example.org"));

        // When
        java.util.Optional<String> domain = searchResultProcessor.extractAndValidateDomain(result, context);

        // Then
        assertThat(domain).isPresent().contains("example.org");
        assertThat(context.getInvalidUrlsSkipped()).isZero();
    }

    @Test
    @DisplayName("extractAndValidateDomain - Invalid URL records and returns empty")
    void extractAndValidateDomain_InvalidUrl_RecordsAndReturnsEmpty() {
        // Given
        SearchResult result = SearchResult.builder()
            .url("htp://invalid..url//")
            .build();
        ProcessingContext context = new ProcessingContext(testSessionId);
        when(domainService.extractDomainFromUrl("htp://invalid..url//"))
            .thenReturn(java.util.Optional.empty());

        // When
        java.util.Optional<String> domain = searchResultProcessor.extractAndValidateDomain(result, context);

        // Then
        assertThat(domain).isEmpty();
        assertThat(context.getInvalidUrlsSkipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("isSpamTld - Spam TLD records and returns true")
    void isSpamTld_SpamTld_RecordsAndReturnsTrue() {
        // Given
        SearchResult result = SearchResult.builder()
            .url("https://scam.xyz/grants")
            .build();
        ProcessingContext context = new ProcessingContext(testSessionId);
        when(domainCredibilityService.isSpamTld("https://scam.xyz/grants"))
            .thenReturn(true);

        // When
        boolean isSpam = searchResultProcessor.isSpamTld(result, context);

        // Then
        assertThat(isSpam).isTrue();
        assertThat(context.getSpamTldFiltered()).isEqualTo(1);
        verify(domainCredibilityService).isSpamTld("https://scam.xyz/grants");
    }

    @Test
    @DisplayName("isSpamTld - Legit TLD returns false")
    void isSpamTld_LegitTld_ReturnsFalse() {
        // Given
        SearchResult result = SearchResult.builder()
            .url("https://example.org/grants")
            .build();
        ProcessingContext context = new ProcessingContext(testSessionId);
        when(domainCredibilityService.isSpamTld("https://example.org/grants"))
            .thenReturn(false);

        // When
        boolean isSpam = searchResultProcessor.isSpamTld(result, context);

        // Then
        assertThat(isSpam).isFalse();
        assertThat(context.getSpamTldFiltered()).isZero();
    }

    @Test
    @DisplayName("isDuplicate - Unique first time returns false")
    void isDuplicate_UniqueFirstTime_ReturnsFalse() {
        // Given
        ProcessingContext context = new ProcessingContext(testSessionId);
        String domain = "example.org";

        // When
        boolean isDuplicate = searchResultProcessor.isDuplicate(domain, context);

        // Then
        assertThat(isDuplicate).isFalse();
        assertThat(context.getDuplicatesSkipped()).isZero();
    }

    @Test
    @DisplayName("isDuplicate - Second occurrence returns true")
    void isDuplicate_SecondOccurrence_ReturnsTrue() {
        // Given
        ProcessingContext context = new ProcessingContext(testSessionId);
        String domain = "example.org";
        context.markDomainAsSeen(domain);  // First occurrence

        // When
        boolean isDuplicate = searchResultProcessor.isDuplicate(domain, context);

        // Then
        assertThat(isDuplicate).isTrue();
        assertThat(context.getDuplicatesSkipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("isBlacklisted - Blacklisted domain records and returns true")
    void isBlacklisted_BlacklistedDomain_RecordsAndReturnsTrue() {
        // Given
        String domain = "spam.com";
        ProcessingContext context = new ProcessingContext(testSessionId);
        when(domainService.isBlacklisted("spam.com")).thenReturn(true);

        // When
        boolean isBlacklisted = searchResultProcessor.isBlacklisted(domain, context);

        // Then
        assertThat(isBlacklisted).isTrue();
        assertThat(context.getBlacklistedSkipped()).isEqualTo(1);
        verify(domainService).isBlacklisted("spam.com");
    }

    @Test
    @DisplayName("isBlacklisted - Allowed domain returns false")
    void isBlacklisted_AllowedDomain_ReturnsFalse() {
        // Given
        String domain = "example.org";
        ProcessingContext context = new ProcessingContext(testSessionId);
        when(domainService.isBlacklisted("example.org")).thenReturn(false);

        // When
        boolean isBlacklisted = searchResultProcessor.isBlacklisted(domain, context);

        // Then
        assertThat(isBlacklisted).isFalse();
        assertThat(context.getBlacklistedSkipped()).isZero();
    }

    @Test
    @DisplayName("calculateConfidence - Valid result returns score")
    void calculateConfidence_ValidResult_ReturnsScore() {
        // Given
        SearchResult result = SearchResult.builder()
            .url("https://example.org/grants")
            .title("EU Grants")
            .description("Funding for research")
            .build();
        when(confidenceScorer.calculateConfidence(
            "EU Grants", "Funding for research", "https://example.org/grants"
        )).thenReturn(new java.math.BigDecimal("0.85"));

        // When
        java.math.BigDecimal confidence = searchResultProcessor.calculateConfidence(result);

        // Then
        assertThat(confidence).isEqualByComparingTo(new java.math.BigDecimal("0.85"));
        verify(confidenceScorer).calculateConfidence(
            "EU Grants", "Funding for research", "https://example.org/grants"
        );
    }

    @Test
    @DisplayName("classifyConfidence - Above threshold records high and returns true")
    void classifyConfidence_AboveThreshold_RecordsHighAndReturnsTrue() {
        // Given
        java.math.BigDecimal confidence = new java.math.BigDecimal("0.85");
        ProcessingContext context = new ProcessingContext(testSessionId);

        // When
        boolean isHigh = searchResultProcessor.classifyConfidence(confidence, context);

        // Then
        assertThat(isHigh).isTrue();
        assertThat(context.getHighConfidenceCreated()).isEqualTo(1);
        assertThat(context.getLowConfidenceCreated()).isZero();
    }

    @Test
    @DisplayName("classifyConfidence - Below threshold records low and returns false")
    void classifyConfidence_BelowThreshold_RecordsLowAndReturnsFalse() {
        // Given
        java.math.BigDecimal confidence = new java.math.BigDecimal("0.45");
        ProcessingContext context = new ProcessingContext(testSessionId);

        // When
        boolean isHigh = searchResultProcessor.classifyConfidence(confidence, context);

        // Then
        assertThat(isHigh).isFalse();
        assertThat(context.getHighConfidenceCreated()).isZero();
        assertThat(context.getLowConfidenceCreated()).isEqualTo(1);  // BUG FIX VALIDATED
    }

    @Test
    @DisplayName("classifyConfidence - Exactly threshold records high and returns true")
    void classifyConfidence_ExactlyThreshold_RecordsHighAndReturnsTrue() {
        // Given
        java.math.BigDecimal confidence = new java.math.BigDecimal("0.60");
        ProcessingContext context = new ProcessingContext(testSessionId);

        // When
        boolean isHigh = searchResultProcessor.classifyConfidence(confidence, context);

        // Then
        assertThat(isHigh).isTrue();  // >= threshold
        assertThat(context.getHighConfidenceCreated()).isEqualTo(1);
    }

    @Test
    @DisplayName("createAndSaveCandidate - Valid inputs creates and saves")
    void createAndSaveCandidate_ValidInputs_CreatesAndSaves() {
        // Given
        SearchResult result = SearchResult.builder()
            .url("https://example.org/grants")
            .title("EU Grants")
            .description("Funding")
            .build();
        String domain = "example.org";
        java.math.BigDecimal confidence = new java.math.BigDecimal("0.85");
        ProcessingContext context = new ProcessingContext(testSessionId);

        UUID domainId = UUID.randomUUID();
        com.northstar.funding.domain.Domain mockDomain = com.northstar.funding.domain.Domain.builder()
            .domainId(domainId)
            .domainName(domain)
            .build();
        com.northstar.funding.domain.FundingSourceCandidate mockCandidate =
            com.northstar.funding.domain.FundingSourceCandidate.builder()
            .build();

        when(domainService.registerOrGetDomain(domain, testSessionId))
            .thenReturn(mockDomain);
        when(candidateCreationService.createCandidate(
            "EU Grants", "Funding", "https://example.org/grants",
            domainId, testSessionId, confidence
        )).thenReturn(mockCandidate);

        // When
        searchResultProcessor.createAndSaveCandidate(result, domain, confidence, context);

        // Then
        verify(domainService).registerOrGetDomain(domain, testSessionId);
        verify(candidateCreationService).createCandidate(
            "EU Grants", "Funding", "https://example.org/grants",
            domainId, testSessionId, confidence
        );
        verify(candidateRepository).save(mockCandidate);
    }
}
