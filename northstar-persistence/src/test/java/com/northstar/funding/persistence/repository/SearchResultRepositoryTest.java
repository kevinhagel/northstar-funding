package com.northstar.funding.persistence.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.domain.SessionType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for SearchResultRepository
 *
 * Tests search result deduplication, tracking, processing status.
 * Uses TestContainers with PostgreSQL for realistic testing.
 *
 * Pattern: @DataJdbcTest - One test class per repository
 * Reference: SearchQueryRepositoryTest.java
 */
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SearchResultRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SearchResultRepository repository;

    @Autowired
    private DiscoverySessionRepository discoverySessionRepository;

    private UUID defaultSessionId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        discoverySessionRepository.deleteAll();

        // Create default discovery session for all tests
        DiscoverySession session = DiscoverySession.builder()
            .sessionType(SessionType.SCHEDULED)
            .status(SessionStatus.RUNNING)
            .startedAt(LocalDateTime.now())
            .build();
        defaultSessionId = discoverySessionRepository.save(session).getSessionId();
    }

    @Test
    void testSaveAndFindByDeduplicationKey() {
        // Given
        LocalDate today = LocalDate.now();
        SearchResult result = SearchResult.builder()
            .discoverySessionId(defaultSessionId)
            .searchEngine(SearchEngineType.SEARXNG)
            .searchQuery("Bulgaria funding")
            .discoveredAt(LocalDateTime.now())
            .searchDate(today)
            .url("https://us-bulgaria.org/grants")
            .domain("us-bulgaria.org")
            .urlPath("/grants")
            .title("Bulgaria Education Grants")
            .description("Annual grants for education")
            .rankPosition(1)
            .isDuplicate(false)
            .isProcessed(false)
            .isBlacklisted(false)
            .build();

        result.setDeduplicationKey(result.generateDeduplicationKey());

        // When
        repository.save(result);

        // Then
        String dedupKey = "us-bulgaria.org:https://us-bulgaria.org/grants:" + today;
        var found = repository.findByDeduplicationKey(dedupKey);
        assertThat(found).isPresent();
        assertThat(found.get().getSearchEngine()).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(found.get().getTitle()).isEqualTo("Bulgaria Education Grants");
    }

    @Test
    void testExistsByDeduplicationKey() {
        // Given
        LocalDate today = LocalDate.now();
        SearchResult result = createSearchResult("test.org", "https://test.org/page", today);
        result.setDeduplicationKey(result.generateDeduplicationKey());
        repository.save(result);

        // When / Then
        String dedupKey = "test.org:https://test.org/page:" + today;
        assertThat(repository.existsByDeduplicationKey(dedupKey)).isTrue();
        assertThat(repository.existsByDeduplicationKey("nonexistent:key:2025-01-01")).isFalse();
    }

    @Test
    void testFindByDomain() {
        // Given
        repository.save(createSearchResult("test.org", "https://test.org/page1"));
        repository.save(createSearchResult("test.org", "https://test.org/page2"));
        repository.save(createSearchResult("other.org", "https://other.org/page1"));

        // When
        var results = repository.findByDomain("test.org");

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindByUrl() {
        // Given
        String url = "https://test.org/specific-page";
        repository.save(createSearchResult("test.org", url));
        repository.save(createSearchResult("test.org", "https://test.org/other"));

        // When
        var results = repository.findByUrl(url);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUrl()).isEqualTo(url);
    }

    @Test
    void testFindByDiscoverySessionId() {
        // Given
        // Create additional discovery session for this test
        DiscoverySession customSession = DiscoverySession.builder()
            .sessionType(SessionType.SCHEDULED)
            .status(SessionStatus.RUNNING)
            .startedAt(LocalDateTime.now())
            .build();
        UUID sessionId = discoverySessionRepository.save(customSession).getSessionId();

        SearchResult result1 = createSearchResult("test.org", "https://test.org/1");
        result1.setDiscoverySessionId(sessionId);

        SearchResult result2 = createSearchResult("test.org", "https://test.org/2");
        result2.setDiscoverySessionId(sessionId);

        SearchResult otherSession = createSearchResult("other.org", "https://other.org/1");
        // otherSession already uses defaultSessionId from createSearchResult

        repository.save(result1);
        repository.save(result2);
        repository.save(otherSession);

        // When
        var results = repository.findByDiscoverySessionId(sessionId);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindBySearchEngine() {
        // Given
        SearchResult searxng = createSearchResult("test.org", "https://test.org/1");
        searxng.setSearchEngine(SearchEngineType.SEARXNG);

        SearchResult tavily = createSearchResult("test.org", "https://test.org/2");
        tavily.setSearchEngine(SearchEngineType.TAVILY);

        repository.save(searxng);
        repository.save(tavily);

        // When
        var searxngResults = repository.findBySearchEngine(SearchEngineType.SEARXNG);
        var tavilyResults = repository.findBySearchEngine(SearchEngineType.TAVILY);

        // Then
        assertThat(searxngResults).hasSize(1);
        assertThat(tavilyResults).hasSize(1);
    }

    @Test
    void testFindByIsDuplicate() {
        // Given
        SearchResult original = createSearchResult("test.org", "https://test.org/page");
        original.setIsDuplicate(false);

        // Save original first to get its ID
        repository.save(original);

        // Use different URL to avoid deduplication_key unique constraint violation
        SearchResult duplicate = createSearchResult("test.org", "https://test.org/page2");
        duplicate.setIsDuplicate(true);
        duplicate.setDuplicateOfResultId(original.getSearchResultId());

        repository.save(duplicate);

        // When
        var originals = repository.findByIsDuplicate(false);
        var duplicates = repository.findByIsDuplicate(true);

        // Then
        assertThat(originals).hasSize(1);
        assertThat(duplicates).hasSize(1);
    }

    @Test
    void testFindUnprocessedResults() {
        // Given
        SearchResult unprocessed1 = createSearchResult("test.org", "https://test.org/1");
        unprocessed1.setIsProcessed(false);
        unprocessed1.setIsDuplicate(false);
        unprocessed1.setIsBlacklisted(false);

        SearchResult unprocessed2 = createSearchResult("test.org", "https://test.org/2");
        unprocessed2.setIsProcessed(false);
        unprocessed2.setIsDuplicate(false);
        unprocessed2.setIsBlacklisted(false);

        SearchResult processed = createSearchResult("test.org", "https://test.org/3");
        processed.setIsProcessed(true);

        SearchResult duplicate = createSearchResult("test.org", "https://test.org/4");
        duplicate.setIsDuplicate(true);

        SearchResult blacklisted = createSearchResult("test.org", "https://test.org/5");
        blacklisted.setIsBlacklisted(true);

        repository.save(unprocessed1);
        repository.save(unprocessed2);
        repository.save(processed);
        repository.save(duplicate);
        repository.save(blacklisted);

        // When
        var results = repository.findUnprocessedResults(PageRequest.of(0, 10));

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testFindBySearchDate() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        SearchResult todayResult = createSearchResult("test.org", "https://test.org/today", today);
        SearchResult yesterdayResult = createSearchResult("test.org", "https://test.org/yesterday", yesterday);

        repository.save(todayResult);
        repository.save(yesterdayResult);

        // When
        var todayResults = repository.findBySearchDate(today);

        // Then
        assertThat(todayResults).hasSize(1);
        assertThat(todayResults.get(0).getSearchDate()).isEqualTo(today);
    }

    @Test
    void testFindByDomainAndSearchDate() {
        // Given
        LocalDate today = LocalDate.now();
        SearchResult result1 = createSearchResult("test.org", "https://test.org/1", today);
        SearchResult result2 = createSearchResult("test.org", "https://test.org/2", today);
        SearchResult differentDomain = createSearchResult("other.org", "https://other.org/1", today);
        SearchResult differentDate = createSearchResult("test.org", "https://test.org/3", today.minusDays(1));

        repository.save(result1);
        repository.save(result2);
        repository.save(differentDomain);
        repository.save(differentDate);

        // When
        var results = repository.findByDomainAndSearchDate("test.org", today);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void testCountDuplicatesBySession() {
        // Given
        // Create additional discovery session for this test
        DiscoverySession customSession = DiscoverySession.builder()
            .sessionType(SessionType.SCHEDULED)
            .status(SessionStatus.RUNNING)
            .startedAt(LocalDateTime.now())
            .build();
        UUID sessionId = discoverySessionRepository.save(customSession).getSessionId();

        SearchResult original = createSearchResult("test.org", "https://test.org/1");
        original.setDiscoverySessionId(sessionId);
        original.setIsDuplicate(false);

        SearchResult duplicate1 = createSearchResult("test.org", "https://test.org/2");
        duplicate1.setDiscoverySessionId(sessionId);
        duplicate1.setIsDuplicate(true);

        SearchResult duplicate2 = createSearchResult("test.org", "https://test.org/3");
        duplicate2.setDiscoverySessionId(sessionId);
        duplicate2.setIsDuplicate(true);

        repository.save(original);
        repository.save(duplicate1);
        repository.save(duplicate2);

        // When
        long count = repository.countDuplicatesBySession(sessionId);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountBySearchEngine() {
        // Given
        SearchResult searxng1 = createSearchResult("test.org", "https://test.org/1");
        searxng1.setSearchEngine(SearchEngineType.SEARXNG);

        SearchResult searxng2 = createSearchResult("test.org", "https://test.org/2");
        searxng2.setSearchEngine(SearchEngineType.SEARXNG);

        SearchResult tavily = createSearchResult("test.org", "https://test.org/3");
        tavily.setSearchEngine(SearchEngineType.TAVILY);

        repository.save(searxng1);
        repository.save(searxng2);
        repository.save(tavily);

        // When
        long searxngCount = repository.countBySearchEngine(SearchEngineType.SEARXNG);
        long tavilyCount = repository.countBySearchEngine(SearchEngineType.TAVILY);

        // Then
        assertThat(searxngCount).isEqualTo(2);
        assertThat(tavilyCount).isEqualTo(1);
    }

    @Test
    void testFindRecentResults() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoDaysAgo = now.minusDays(2);

        SearchResult recent = createSearchResult("test.org", "https://test.org/recent");
        recent.setDiscoveredAt(oneHourAgo);

        SearchResult old = createSearchResult("test.org", "https://test.org/old");
        old.setDiscoveredAt(twoDaysAgo);

        repository.save(recent);
        repository.save(old);

        // When
        var results = repository.findRecentResults(now.minusDays(1));

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUrl()).isEqualTo("https://test.org/recent");
    }

    // Helper methods
    private SearchResult createSearchResult(String domain, String url) {
        return createSearchResult(domain, url, LocalDate.now());
    }

    private SearchResult createSearchResult(String domain, String url, LocalDate searchDate) {
        SearchResult result = SearchResult.builder()
            .discoverySessionId(defaultSessionId)
            .searchEngine(SearchEngineType.SEARXNG)
            .searchQuery("test query")
            .discoveredAt(LocalDateTime.now())
            .searchDate(searchDate)
            .url(url)
            .domain(domain)
            .urlPath(extractPath(url))
            .title("Test Title")
            .description("Test Description")
            .rankPosition(1)
            .isDuplicate(false)
            .isProcessed(false)
            .isBlacklisted(false)
            .build();

        // Set deduplication key for all results
        result.setDeduplicationKey(result.generateDeduplicationKey());
        return result;
    }

    private String extractPath(String url) {
        try {
            return url.substring(url.indexOf('/', 8)); // Skip "https://"
        } catch (Exception e) {
            return "/";
        }
    }
}
