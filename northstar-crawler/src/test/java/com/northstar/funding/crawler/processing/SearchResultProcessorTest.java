package com.northstar.funding.crawler.processing;

import com.northstar.funding.crawler.scoring.CandidateCreationService;
import com.northstar.funding.crawler.scoring.ConfidenceScorer;
import com.northstar.funding.crawler.scoring.DomainCredibilityService;
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

    private SearchResultProcessor searchResultProcessor;

    private UUID testSessionId;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID();
        searchResultProcessor = new SearchResultProcessor(
            domainCredibilityService,
            confidenceScorer,
            candidateCreationService,
            domainService
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
    @DisplayName("Low confidence results do not create candidates")
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

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Should process 2 results, create 0 candidates (both low confidence)
        assertThat(stats.getTotalResults()).isEqualTo(2);
        assertThat(stats.getHighConfidenceCreated()).isZero();
        assertThat(stats.getLowConfidenceCreated()).isZero();
        assertThat(stats.getTotalCandidatesCreated()).isZero();
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
}
