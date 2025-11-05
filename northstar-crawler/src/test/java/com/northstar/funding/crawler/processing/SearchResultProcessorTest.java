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
import static org.mockito.Mockito.when;

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

        // When
        ProcessingStatistics stats = searchResultProcessor.processSearchResults(
            results, testSessionId
        );

        // Then: Should process 3 results, skip 1 duplicate
        assertThat(stats.getTotalResults()).isEqualTo(3);
        assertThat(stats.getDuplicatesSkipped()).isEqualTo(1);
    }
}
