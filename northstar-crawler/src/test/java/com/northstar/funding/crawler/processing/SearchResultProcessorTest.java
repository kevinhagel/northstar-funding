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
}
