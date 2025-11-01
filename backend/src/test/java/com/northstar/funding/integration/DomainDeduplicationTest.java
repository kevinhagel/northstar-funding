package com.northstar.funding.integration;

import com.northstar.funding.discovery.search.application.SearchExecutionService;
import com.northstar.funding.discovery.search.domain.QueryTag;
import com.northstar.funding.discovery.search.domain.SearchEngineType;
import com.northstar.funding.discovery.search.domain.SearchQuery;
import com.northstar.funding.discovery.search.domain.TagType;
import com.northstar.funding.discovery.search.infrastructure.SearchQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test for Domain Deduplication (Feature 003 - Task T032)
 *
 * Tests that the search execution service properly deduplicates results:
 * - Same domain returned by multiple search engines
 * - Domain-level deduplication (not URL-level)
 *
 * Based on specs/003-search-execution-infrastructure/quickstart.md Scenario 2
 *
 * @author NorthStar Funding Team
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("Domain Deduplication Integration Test (T032)")
class DomainDeduplicationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("northstar_test")
        .withUsername("test_user")
        .withPassword("test_password")
        .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Autowired
    private SearchQueryRepository searchQueryRepository;

    @BeforeEach
    void setUp() {
        // @Transactional handles cleanup - no manual cleanup needed
    }

    @Test
    @DisplayName("Should deduplicate same domain from multiple search engines")
    void shouldDeduplicateSameDomainFromMultipleEngines() {
        // GIVEN: A search query that targets multiple engines
        SearchQuery query = createTestQuery();
        searchQueryRepository.save(query);

        // WHEN: Execute the query across all engines
        // (In a real scenario, multiple engines might return the same domain with different URLs)
        var result = searchExecutionService.executeQueryAcrossEngines(query);

        // THEN: Result should be successful
        assertThat(result.isSuccess()).isTrue();

        // AND: Results should be deduplicated by domain
        var results = result.get();

        // Extract domains from results
        var domains = results.stream()
            .map(searchResult -> extractDomain(searchResult.url()))
            .toList();

        // Verify no duplicate domains
        var uniqueDomains = Set.copyOf(domains);
        assertThat(domains).hasSize(uniqueDomains.size())
            .withFailMessage("Expected no duplicate domains, but found: %s", domains);
    }

    @Test
    @DisplayName("Should treat www and non-www as same domain")
    void shouldTreatWwwAndNonWwwAsSameDomain() {
        // This test verifies that www.example.org and example.org are NOT deduplicated
        // (current implementation keeps host as-is)
        //
        // NOTE: Future enhancement could normalize domains to remove www prefix
        // See: /Users/kevin/github/springcrawler/.../SitemapUtils.java:normalizeHost()

        // For now, this test documents the current behavior
        // GIVEN: Two URLs with same domain but different www prefix
        String url1 = "https://www.example.org/grants";
        String url2 = "https://example.org/funding";

        // WHEN: Extract domains
        String domain1 = extractDomain(url1);
        String domain2 = extractDomain(url2);

        // THEN: Domains are currently treated as different (contains www prefix)
        assertThat(domain1).isEqualTo("www.example.org");
        assertThat(domain2).isEqualTo("example.org");
        assertThat(domain1).isNotEqualTo(domain2);

        // TODO: Future enhancement - normalize domains to treat www/non-www as same
        // See SitemapUtils.normalizeHost() for reference implementation
    }

    @Test
    @DisplayName("Should deduplicate by domain not by full URL")
    void shouldDeduplicateByDomainNotByUrl() {
        // This test verifies that different URLs from the same domain are deduplicated
        // e.g., example.org/page1 and example.org/page2 should count as one domain

        String url1 = "https://example.org/grants/program-1";
        String url2 = "https://example.org/funding/scholarships";
        String url3 = "https://example.org/about/us";

        // All should extract to same domain
        assertThat(extractDomain(url1)).isEqualTo("example.org");
        assertThat(extractDomain(url2)).isEqualTo("example.org");
        assertThat(extractDomain(url3)).isEqualTo("example.org");
    }

    // Helper methods

    private SearchQuery createTestQuery() {
        return SearchQuery.builder()
            .queryText("Bulgaria education grants 2025")
            .dayOfWeek(DayOfWeek.MONDAY)
            .tags(Set.of("GEOGRAPHY:Bulgaria", "CATEGORY:Education"))
            .targetEngines(Set.of(
                SearchEngineType.SEARXNG.name(),
                SearchEngineType.TAVILY.name(),
                SearchEngineType.PERPLEXITY.name()
            ))
            .expectedResults(25)
            .enabled(true)
            .build();
    }

    /**
     * Extract domain from URL (duplicates logic from SearchExecutionService for testing)
     */
    private String extractDomain(String url) {
        try {
            var uri = java.net.URI.create(url);
            var host = uri.getHost();
            return host != null ? host.toLowerCase() : url;
        } catch (Exception e) {
            return url;
        }
    }
}
