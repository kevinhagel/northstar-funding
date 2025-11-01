package com.northstar.funding.discovery.search.infrastructure;

import com.northstar.funding.discovery.search.domain.SearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for SearchQueryRepository (Feature 003)
 *
 * Uses @Testcontainers with PostgreSQL for realistic testing
 * Tests JSONB queries, day-of-week filtering, enabled/disabled queries
 *
 * Constitutional Compliance:
 * - TestContainers for isolated PostgreSQL testing
 * - Spring Data JDBC repository testing
 * - JSONB query validation
 */
@DataJdbcTest
@Testcontainers
@ActiveProfiles("postgres-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SearchQueryRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
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
    private SearchQueryRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testSaveAndFindById() {
        // Given
        var query = SearchQuery.builder()
            .queryText("bulgaria education grants 2025")
            .dayOfWeek(DayOfWeek.MONDAY)
            .tags(Set.of("GEOGRAPHY:Bulgaria", "CATEGORY:Education"))
            .targetEngines(Set.of("SEARXNG", "TAVILY"))
            .expectedResults(25)
            .enabled(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // When
        var saved = repository.save(query);

        // Then
        assertThat(saved.getId()).isNotNull();

        var found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getQueryText()).isEqualTo("bulgaria education grants 2025");
        assertThat(found.get().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(found.get().getTags()).hasSize(2);
        assertThat(found.get().getTargetEngines()).hasSize(2);
    }

    @Test
    void testFindByDayOfWeekAndEnabled() {
        // Given - Create queries for different days
        repository.save(createQuery("monday query 1", DayOfWeek.MONDAY, true));
        repository.save(createQuery("monday query 2", DayOfWeek.MONDAY, true));
        repository.save(createQuery("monday disabled", DayOfWeek.MONDAY, false));
        repository.save(createQuery("tuesday query", DayOfWeek.TUESDAY, true));

        // When
        var mondayQueries = repository.findByDayOfWeekAndEnabled(DayOfWeek.MONDAY);

        // Then
        assertThat(mondayQueries).hasSize(2);
        assertThat(mondayQueries)
            .allMatch(q -> q.getDayOfWeek() == DayOfWeek.MONDAY)
            .allMatch(SearchQuery::isEnabled);
    }

    @Test
    void testFindAllEnabled() {
        // Given
        repository.save(createQuery("enabled 1", DayOfWeek.MONDAY, true));
        repository.save(createQuery("enabled 2", DayOfWeek.TUESDAY, true));
        repository.save(createQuery("disabled", DayOfWeek.MONDAY, false));

        // When
        var enabled = repository.findAllEnabled();

        // Then
        assertThat(enabled).hasSize(2);
        assertThat(enabled).allMatch(SearchQuery::isEnabled);
    }

    @Test
    void testFindAllDisabled() {
        // Given
        repository.save(createQuery("enabled", DayOfWeek.MONDAY, true));
        repository.save(createQuery("disabled 1", DayOfWeek.MONDAY, false));
        repository.save(createQuery("disabled 2", DayOfWeek.TUESDAY, false));

        // When
        var disabled = repository.findAllDisabled();

        // Then
        assertThat(disabled).hasSize(2);
        assertThat(disabled).noneMatch(SearchQuery::isEnabled);
    }

    @Test
    void testCountByDayOfWeekAndEnabled() {
        // Given
        repository.save(createQuery("monday 1", DayOfWeek.MONDAY, true));
        repository.save(createQuery("monday 2", DayOfWeek.MONDAY, true));
        repository.save(createQuery("monday disabled", DayOfWeek.MONDAY, false));

        // When
        var count = repository.countByDayOfWeekAndEnabled(DayOfWeek.MONDAY);

        // Then
        assertThat(count).isEqualTo(2);
    }

    // Helper method
    private SearchQuery createQuery(String text, DayOfWeek day, boolean enabled) {
        return SearchQuery.builder()
            .queryText(text)
            .dayOfWeek(day)
            .tags(Set.of("GEOGRAPHY:Bulgaria"))
            .targetEngines(Set.of("SEARXNG"))
            .enabled(enabled)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
