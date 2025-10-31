package com.northstar.funding.persistence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.persistence.repository.SearchResultRepository;

/**
 * Unit tests for SearchResultService using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class SearchResultServiceTest {

    @Mock
    private SearchResultRepository searchResultRepository;

    @InjectMocks
    private SearchResultService searchResultService;

    private SearchResult testResult;
    private UUID testSessionId;
    private String deduplicationKey;

    @BeforeEach
    void setUp() {
        testSessionId = UUID.randomUUID();
        deduplicationKey = "test.org:https://test.org/program:2025-01-01";
        testResult = SearchResult.builder()
            .searchResultId(UUID.randomUUID())
            .discoverySessionId(testSessionId)
            .domain("test.org")
            .url("https://test.org/program")
            .searchEngine(SearchEngineType.SEARXNG)
            .searchDate(LocalDate.of(2025, 1, 1))
            .deduplicationKey(deduplicationKey)
            .discoveredAt(LocalDateTime.now())
            .isDuplicate(false)
            .isProcessed(false)
            .isBlacklisted(false)
            .build();
    }

    // ============================================================================
    // CREATE & UPDATE Operations Tests
    // ============================================================================

    @Test
    void registerSearchResult_WhenNew_ShouldCreateResult() {
        // Given
        when(searchResultRepository.findByDeduplicationKey(deduplicationKey))
            .thenReturn(Optional.empty());
        when(searchResultRepository.save(any(SearchResult.class)))
            .thenReturn(testResult);

        // When
        SearchResult result = searchResultService.registerSearchResult(testResult);

        // Then
        assertThat(result).isNotNull();
        verify(searchResultRepository).findByDeduplicationKey(deduplicationKey);
        verify(searchResultRepository).save(testResult);
    }

    @Test
    void registerSearchResult_WhenDuplicate_ShouldMarkAsDuplicate() {
        // Given
        SearchResult existing = SearchResult.builder()
            .searchResultId(UUID.randomUUID())
            .deduplicationKey(deduplicationKey)
            .isDuplicate(false)
            .build();
        when(searchResultRepository.findByDeduplicationKey(deduplicationKey))
            .thenReturn(Optional.of(existing));
        when(searchResultRepository.save(any(SearchResult.class)))
            .thenReturn(existing);

        // When
        SearchResult result = searchResultService.registerSearchResult(testResult);

        // Then
        assertThat(result.getIsDuplicate()).isTrue();
        verify(searchResultRepository).save(existing);
    }

    @Test
    void markAsProcessed_WhenResultExists_ShouldMarkAsProcessed() {
        // Given
        UUID resultId = testResult.getSearchResultId();
        when(searchResultRepository.findById(resultId))
            .thenReturn(Optional.of(testResult));
        when(searchResultRepository.save(any(SearchResult.class)))
            .thenReturn(testResult);

        // When
        SearchResult result = searchResultService.markAsProcessed(resultId);

        // Then
        assertThat(result.getIsProcessed()).isTrue();
        verify(searchResultRepository).save(testResult);
    }

    @Test
    void markAsProcessed_WhenResultNotFound_ShouldThrowException() {
        // Given
        UUID resultId = UUID.randomUUID();
        when(searchResultRepository.findById(resultId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> searchResultService.markAsProcessed(resultId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SearchResult not found");
        verify(searchResultRepository, never()).save(any(SearchResult.class));
    }

    @Test
    void markAsBlacklisted_WhenResultExists_ShouldMarkAsBlacklisted() {
        // Given
        UUID resultId = testResult.getSearchResultId();
        when(searchResultRepository.findById(resultId))
            .thenReturn(Optional.of(testResult));
        when(searchResultRepository.save(any(SearchResult.class)))
            .thenReturn(testResult);

        // When
        SearchResult result = searchResultService.markAsBlacklisted(resultId);

        // Then
        assertThat(result.getIsBlacklisted()).isTrue();
        verify(searchResultRepository).save(testResult);
    }

    @Test
    void linkToOrganization_WhenResultExists_ShouldLinkOrganization() {
        // Given
        UUID resultId = testResult.getSearchResultId();
        UUID organizationId = UUID.randomUUID();
        when(searchResultRepository.findById(resultId))
            .thenReturn(Optional.of(testResult));
        when(searchResultRepository.save(any(SearchResult.class)))
            .thenReturn(testResult);

        // When
        SearchResult result = searchResultService.linkToOrganization(resultId, organizationId);

        // Then
        assertThat(result.getOrganizationId()).isEqualTo(organizationId);
        verify(searchResultRepository).save(testResult);
    }

    @Test
    void linkToProgram_WhenResultExists_ShouldLinkProgram() {
        // Given
        UUID resultId = testResult.getSearchResultId();
        UUID programId = UUID.randomUUID();
        when(searchResultRepository.findById(resultId))
            .thenReturn(Optional.of(testResult));
        when(searchResultRepository.save(any(SearchResult.class)))
            .thenReturn(testResult);

        // When
        SearchResult result = searchResultService.linkToProgram(resultId, programId);

        // Then
        assertThat(result.getProgramId()).isEqualTo(programId);
        verify(searchResultRepository).save(testResult);
    }

    @Test
    void linkToCandidate_WhenResultExists_ShouldLinkCandidate() {
        // Given
        UUID resultId = testResult.getSearchResultId();
        UUID candidateId = UUID.randomUUID();
        when(searchResultRepository.findById(resultId))
            .thenReturn(Optional.of(testResult));
        when(searchResultRepository.save(any(SearchResult.class)))
            .thenReturn(testResult);

        // When
        SearchResult result = searchResultService.linkToCandidate(resultId, candidateId);

        // Then
        assertThat(result.getCandidateId()).isEqualTo(candidateId);
        verify(searchResultRepository).save(testResult);
    }

    // ============================================================================
    // READ Operations Tests
    // ============================================================================

    @Test
    void resultExists_WhenExists_ShouldReturnTrue() {
        // Given
        when(searchResultRepository.existsByDeduplicationKey(deduplicationKey))
            .thenReturn(true);

        // When
        boolean result = searchResultService.resultExists(deduplicationKey);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void findByDeduplicationKey_WhenExists_ShouldReturnResult() {
        // Given
        when(searchResultRepository.findByDeduplicationKey(deduplicationKey))
            .thenReturn(Optional.of(testResult));

        // When
        Optional<SearchResult> result = searchResultService.findByDeduplicationKey(
            deduplicationKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testResult);
    }

    @Test
    void findById_WhenExists_ShouldReturnResult() {
        // Given
        UUID resultId = testResult.getSearchResultId();
        when(searchResultRepository.findById(resultId))
            .thenReturn(Optional.of(testResult));

        // When
        Optional<SearchResult> result = searchResultService.findById(resultId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testResult);
    }

    @Test
    void getResultsByDomain_ShouldReturnDomainResults() {
        // Given
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findByDomain("test.org"))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsByDomain("test.org");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getResultsByUrl_ShouldReturnUrlResults() {
        // Given
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findByUrl("https://test.org/program"))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsByUrl(
            "https://test.org/program");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getResultsBySession_ShouldReturnSessionResults() {
        // Given
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findByDiscoverySessionId(testSessionId))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsBySession(testSessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getResultsBySearchEngine_ShouldReturnEngineResults() {
        // Given
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findBySearchEngine(SearchEngineType.SEARXNG))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsBySearchEngine(
            SearchEngineType.SEARXNG);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getDuplicateResults_ShouldReturnDuplicates() {
        // Given
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findByIsDuplicate(true))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getDuplicateResults();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getUnprocessedResults_ShouldReturnUnprocessedResults() {
        // Given
        int limit = 10;
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findUnprocessedResults(PageRequest.of(0, limit)))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getUnprocessedResults(limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getResultsByDate_ShouldReturnDateResults() {
        // Given
        LocalDate searchDate = LocalDate.of(2025, 1, 1);
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findBySearchDate(searchDate))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsByDate(searchDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getResultsByDomainAndDate_ShouldReturnFilteredResults() {
        // Given
        LocalDate searchDate = LocalDate.of(2025, 1, 1);
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findByDomainAndSearchDate("test.org", searchDate))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsByDomainAndDate(
            "test.org", searchDate);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getResultsWithOrganizations_ShouldReturnLinkedResults() {
        // Given
        int limit = 10;
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findResultsWithOrganizations(PageRequest.of(0, limit)))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsWithOrganizations(limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void countDuplicatesBySession_ShouldReturnCount() {
        // Given
        when(searchResultRepository.countDuplicatesBySession(testSessionId))
            .thenReturn(42L);

        // When
        long result = searchResultService.countDuplicatesBySession(testSessionId);

        // Then
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void countBySearchEngine_ShouldReturnCount() {
        // Given
        when(searchResultRepository.countBySearchEngine(SearchEngineType.SEARXNG))
            .thenReturn(100L);

        // When
        long result = searchResultService.countBySearchEngine(SearchEngineType.SEARXNG);

        // Then
        assertThat(result).isEqualTo(100L);
    }

    @Test
    void getRecentResults_ShouldReturnRecentResults() {
        // Given
        int daysBack = 7;
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findRecentResults(any(LocalDateTime.class)))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getRecentResults(daysBack);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }

    @Test
    void getResultsReadyForProcessing_ShouldReturnReadyResults() {
        // Given
        int minAgeDays = 1;
        int limit = 10;
        List<SearchResult> results = List.of(testResult);
        when(searchResultRepository.findResultsReadyForProcessing(
            any(LocalDateTime.class), any(PageRequest.class)))
            .thenReturn(results);

        // When
        List<SearchResult> result = searchResultService.getResultsReadyForProcessing(
            minAgeDays, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testResult);
    }
}
