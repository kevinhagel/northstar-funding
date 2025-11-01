package com.northstar.funding.persistence.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.persistence.repository.SearchResultRepository;


/**
 * Service layer for SearchResult entity operations.
 *
 * Provides business logic and transaction management for search result
 * deduplication, processing, and blacklist management.
 *
 * This is the public API for external modules to interact with SearchResult persistence.
 */
@Service
@Transactional
public class SearchResultService {

    private final SearchResultRepository searchResultRepository;

    public SearchResultService(SearchResultRepository searchResultRepository) {
        this.searchResultRepository = searchResultRepository;
    }

    // ============================================================================
    // CREATE & UPDATE Operations
    // ============================================================================

    /**
     * Register a new search result if deduplication key doesn't exist.
     *
     * @param searchResult the search result to register
     * @return the existing or newly created SearchResult
     */
    public SearchResult registerSearchResult(SearchResult searchResult) {
        return searchResultRepository.findByDeduplicationKey(searchResult.getDeduplicationKey())
            .map(existing -> {
                existing.setIsDuplicate(true);
                return searchResultRepository.save(existing);
            })
            .orElseGet(() -> {
                SearchResult saved = searchResultRepository.save(searchResult);
                return saved;
            });
    }

    /**
     * Mark search result as processed.
     *
     * @param resultId the search result ID
     * @return updated SearchResult
     */
    public SearchResult markAsProcessed(UUID resultId) {

        SearchResult result = searchResultRepository.findById(resultId)
            .orElseThrow(() -> new IllegalArgumentException("SearchResult not found: " + resultId));

        result.setIsProcessed(true);
        return searchResultRepository.save(result);
    }

    /**
     * Mark search result as blacklisted.
     *
     * @param resultId the search result ID
     * @return updated SearchResult
     */
    public SearchResult markAsBlacklisted(UUID resultId) {

        SearchResult result = searchResultRepository.findById(resultId)
            .orElseThrow(() -> new IllegalArgumentException("SearchResult not found: " + resultId));

        result.setIsBlacklisted(true);
        return searchResultRepository.save(result);
    }

    /**
     * Link search result to organization.
     *
     * @param resultId the search result ID
     * @param organizationId the organization ID
     * @return updated SearchResult
     */
    public SearchResult linkToOrganization(UUID resultId, UUID organizationId) {

        SearchResult result = searchResultRepository.findById(resultId)
            .orElseThrow(() -> new IllegalArgumentException("SearchResult not found: " + resultId));

        result.setOrganizationId(organizationId);
        return searchResultRepository.save(result);
    }

    /**
     * Link search result to funding program.
     *
     * @param resultId the search result ID
     * @param programId the program ID
     * @return updated SearchResult
     */
    public SearchResult linkToProgram(UUID resultId, UUID programId) {

        SearchResult result = searchResultRepository.findById(resultId)
            .orElseThrow(() -> new IllegalArgumentException("SearchResult not found: " + resultId));

        result.setProgramId(programId);
        return searchResultRepository.save(result);
    }

    /**
     * Link search result to candidate.
     *
     * @param resultId the search result ID
     * @param candidateId the candidate ID
     * @return updated SearchResult
     */
    public SearchResult linkToCandidate(UUID resultId, UUID candidateId) {

        SearchResult result = searchResultRepository.findById(resultId)
            .orElseThrow(() -> new IllegalArgumentException("SearchResult not found: " + resultId));

        result.setCandidateId(candidateId);
        return searchResultRepository.save(result);
    }

    // ============================================================================
    // READ Operations
    // ============================================================================

    /**
     * Check if search result exists by deduplication key.
     *
     * @param deduplicationKey the deduplication key
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean resultExists(String deduplicationKey) {
        return searchResultRepository.existsByDeduplicationKey(deduplicationKey);
    }

    /**
     * Find search result by deduplication key.
     *
     * @param deduplicationKey the deduplication key
     * @return Optional of SearchResult
     */
    @Transactional(readOnly = true)
    public Optional<SearchResult> findByDeduplicationKey(String deduplicationKey) {
        return searchResultRepository.findByDeduplicationKey(deduplicationKey);
    }

    /**
     * Find search result by ID.
     *
     * @param resultId the search result ID
     * @return Optional of SearchResult
     */
    @Transactional(readOnly = true)
    public Optional<SearchResult> findById(UUID resultId) {
        return searchResultRepository.findById(resultId);
    }

    /**
     * Get all results for a domain.
     *
     * @param domain the domain name
     * @return list of search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsByDomain(String domain) {
        return searchResultRepository.findByDomain(domain);
    }

    /**
     * Get results by URL.
     *
     * @param url the URL
     * @return list of search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsByUrl(String url) {
        return searchResultRepository.findByUrl(url);
    }

    /**
     * Get results for discovery session.
     *
     * @param sessionId the discovery session ID
     * @return list of search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsBySession(UUID sessionId) {
        return searchResultRepository.findByDiscoverySessionId(sessionId);
    }

    /**
     * Get results by search engine.
     *
     * @param searchEngine the search engine type
     * @return list of search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsBySearchEngine(SearchEngineType searchEngine) {
        return searchResultRepository.findBySearchEngine(searchEngine);
    }

    /**
     * Get duplicate results.
     *
     * @return list of duplicate search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getDuplicateResults() {
        return searchResultRepository.findByIsDuplicate(true);
    }

    /**
     * Get non-duplicate results.
     *
     * @return list of non-duplicate search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getNonDuplicateResults() {
        return searchResultRepository.findByIsDuplicate(false);
    }

    /**
     * Get unprocessed results.
     *
     * @param limit max number of results
     * @return list of unprocessed search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getUnprocessedResults(int limit) {
        return searchResultRepository.findUnprocessedResults(PageRequest.of(0, limit));
    }

    /**
     * Get blacklisted results.
     *
     * @return list of blacklisted search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getBlacklistedResults() {
        return searchResultRepository.findByIsBlacklisted(true);
    }

    /**
     * Get results by search date.
     *
     * @param searchDate the search date
     * @return list of search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsByDate(LocalDate searchDate) {
        return searchResultRepository.findBySearchDate(searchDate);
    }

    /**
     * Get results by domain and date (for deduplication analysis).
     *
     * @param domain the domain name
     * @param searchDate the search date
     * @return list of search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsByDomainAndDate(String domain, LocalDate searchDate) {
        return searchResultRepository.findByDomainAndSearchDate(domain, searchDate);
    }

    /**
     * Get results with organizations.
     *
     * @param limit max number of results
     * @return list of search results with organizations
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsWithOrganizations(int limit) {
        return searchResultRepository.findResultsWithOrganizations(PageRequest.of(0, limit));
    }

    /**
     * Get results with programs.
     *
     * @param limit max number of results
     * @return list of search results with programs
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsWithPrograms(int limit) {
        return searchResultRepository.findResultsWithPrograms(PageRequest.of(0, limit));
    }

    /**
     * Get results with candidates.
     *
     * @param limit max number of results
     * @return list of search results with candidates
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsWithCandidates(int limit) {
        return searchResultRepository.findResultsWithCandidates(PageRequest.of(0, limit));
    }

    /**
     * Count duplicates for session.
     *
     * @param sessionId the discovery session ID
     * @return count of duplicates
     */
    @Transactional(readOnly = true)
    public long countDuplicatesBySession(UUID sessionId) {
        return searchResultRepository.countDuplicatesBySession(sessionId);
    }

    /**
     * Count results by search engine.
     *
     * @param searchEngine the search engine type
     * @return count
     */
    @Transactional(readOnly = true)
    public long countBySearchEngine(SearchEngineType searchEngine) {
        return searchResultRepository.countBySearchEngine(searchEngine);
    }

    /**
     * Get recent search results.
     *
     * @param daysBack number of days to look back
     * @return list of recent search results
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getRecentResults(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return searchResultRepository.findRecentResults(since);
    }

    /**
     * Get deduplication statistics for session.
     *
     * @param sessionId the discovery session ID
     * @return deduplication statistics
     */
    @Transactional(readOnly = true)
    public SearchResultRepository.DeduplicationStats getDeduplicationStats(UUID sessionId) {
        return searchResultRepository.getDeduplicationStats(sessionId);
    }

    /**
     * Get results ready for processing.
     *
     * @param minAgeDays minimum age in days
     * @param limit max number of results
     * @return list of search results ready for processing
     */
    @Transactional(readOnly = true)
    public List<SearchResult> getResultsReadyForProcessing(int minAgeDays, int limit) {
        LocalDateTime minAge = LocalDateTime.now().minusDays(minAgeDays);
        return searchResultRepository.findResultsReadyForProcessing(minAge, PageRequest.of(0, limit));
    }
}
