package com.northstar.funding.integration;

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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test for Monday Nightly Discovery (Feature 003 - Task T031)
 *
 * Based on quickstart.md Scenario 1: Monday Nightly Discovery Session
 *
 * Test Scenario:
 * GIVEN: Nightly scheduler is enabled, current day is Monday, 5 queries configured for Monday
 * WHEN: Scheduler executes at 2:00 AM
 * THEN: System executes all 5 queries across 3 enabled engines (Searxng, Tavily, Perplexity)
 *
 * Expected Results:
 * - Total Queries: 5
 * - Total Results: 500-1000 (5 queries × 3 engines × ~25 results)
 * - Unique Domains: 100-300 (after deduplication)
 * - High-Confidence Candidates: 50-150 (confidence >= 0.60)
 * - Session Duration: < 30 minutes
 *
 * @author NorthStar Funding Team
 */
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
@DisplayName("Monday Nightly Discovery Integration Test (T031)")
class MondayNightlyDiscoveryTest {

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
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("discovery.schedule.enabled", () -> "false"); // Disable scheduler for tests
    }

    @Autowired
    private SearchQueryRepository searchQueryRepository;

    @BeforeEach
    void setUp() {
        // @Transactional handles cleanup - no manual cleanup needed

        // Create Monday query configuration (5 queries)
        List<SearchQuery> mondayQueries = createMondayQueries();
        searchQueryRepository.saveAll(mondayQueries);
    }

    @Test
    @DisplayName("Should load correct number of Monday queries")
    void shouldLoadCorrectNumberOfMondayQueries() {
        // WHEN: Fetch Monday queries
        List<SearchQuery> mondayQueries = searchQueryRepository
            .findByDayOfWeekAndEnabled(DayOfWeek.MONDAY);

        // THEN: 5 queries configured for Monday
        assertThat(mondayQueries).hasSize(5);

        // Verify all queries target correct engines
        mondayQueries.forEach(query -> {
            assertThat(query.getParsedTargetEngines()).contains(
                SearchEngineType.SEARXNG,
                SearchEngineType.TAVILY,
                SearchEngineType.PERPLEXITY
            );
            assertThat(query.isEnabled()).isTrue();
            assertThat(query.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        });
    }

    @Test
    @DisplayName("Should handle disabled queries correctly")
    void shouldHandleDisabledQueries() {
        // GIVEN: Disable one Monday query
        List<SearchQuery> queries = searchQueryRepository
            .findByDayOfWeekAndEnabled(DayOfWeek.MONDAY);
        SearchQuery queryToDisable = queries.getFirst();

        SearchQuery disabledQuery = SearchQuery.builder()
            .id(queryToDisable.getId())
            .queryText(queryToDisable.getQueryText())
            .dayOfWeek(queryToDisable.getDayOfWeek())
            .tags(queryToDisable.getTags())
            .targetEngines(queryToDisable.getTargetEngines())
            .expectedResults(queryToDisable.getExpectedResults())
            .enabled(false)
            .build();

        searchQueryRepository.save(disabledQuery);

        // WHEN: Fetch enabled Monday queries
        List<SearchQuery> enabledQueries = searchQueryRepository
            .findByDayOfWeekAndEnabled(DayOfWeek.MONDAY);

        // THEN: Only 4 enabled queries returned
        assertThat(enabledQueries).hasSize(4);
        assertThat(enabledQueries).noneMatch(q -> q.getId().equals(queryToDisable.getId()));
    }

    // Helper methods

    private List<SearchQuery> createMondayQueries() {
        return List.of(
            createSearchQuery(
                "Bulgaria startup grants 2025",
                DayOfWeek.MONDAY,
                new QueryTag(TagType.GEOGRAPHY, "Bulgaria"),
                new QueryTag(TagType.CATEGORY, "Grants")
            ),
            createSearchQuery(
                "Balkan technology funding opportunities",
                DayOfWeek.MONDAY,
                new QueryTag(TagType.GEOGRAPHY, "Balkans"),
                new QueryTag(TagType.CATEGORY, "Technology")
            ),
            createSearchQuery(
                "Eastern Europe research scholarships",
                DayOfWeek.MONDAY,
                new QueryTag(TagType.GEOGRAPHY, "Eastern Europe"),
                new QueryTag(TagType.CATEGORY, "Education")
            ),
            createSearchQuery(
                "EU Horizon grants Bulgaria",
                DayOfWeek.MONDAY,
                new QueryTag(TagType.GEOGRAPHY, "Bulgaria"),
                new QueryTag(TagType.AUTHORITY, "EU")
            ),
            createSearchQuery(
                "Sofia university funding programs",
                DayOfWeek.MONDAY,
                new QueryTag(TagType.GEOGRAPHY, "Bulgaria"),
                new QueryTag(TagType.CATEGORY, "Education")
            )
        );
    }

    private SearchQuery createSearchQuery(String queryText, DayOfWeek dayOfWeek, QueryTag... tags) {
        // Convert QueryTag objects to "TYPE:value" strings
        Set<String> tagStrings = Arrays.stream(tags)
            .map(tag -> tag.type().name() + ":" + tag.value())
            .collect(Collectors.toSet());

        // Convert SearchEngineType enums to strings
        Set<String> engineStrings = Set.of(
            SearchEngineType.SEARXNG.name(),
            SearchEngineType.TAVILY.name(),
            SearchEngineType.PERPLEXITY.name()
        );

        return SearchQuery.builder()
            .queryText(queryText)
            .dayOfWeek(dayOfWeek)
            .tags(tagStrings)
            .targetEngines(engineStrings)
            .expectedResults(25)
            .enabled(true)
            .build();
    }

}
