package com.northstar.funding.discovery.search.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test for SearchSessionStatistics entity (Feature 003)
 *
 * CRITICAL: This test MUST FAIL before implementing SearchSessionStatistics.java
 *
 * Tests:
 * - Builder pattern for statistics aggregation
 * - Calculation methods (average response time, failure rate, hit rate)
 * - Immutability and defensive copying
 *
 * Constitutional Compliance:
 * - Spring Data JDBC entity
 * - Lombok @Data, @Builder annotations
 * - Analytics-focused domain model
 */
class SearchSessionStatisticsTest {

    @Test
    void testBuilderCreatesValidStatistics() {
        // Given
        var sessionId = UUID.randomUUID();
        var engineType = SearchEngineType.SEARXNG;

        // When
        var stats = SearchSessionStatistics.builder()
            .sessionId(sessionId)
            .engineType(engineType)
            .queriesExecuted(5)
            .resultsReturned(120)
            .avgResponseTimeMs(2500L)
            .failureCount(1)
            .createdAt(Instant.now())
            .build();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getSessionId()).isEqualTo(sessionId);
        assertThat(stats.getEngineType()).isEqualTo(engineType);
        assertThat(stats.getQueriesExecuted()).isEqualTo(5);
        assertThat(stats.getResultsReturned()).isEqualTo(120);
        assertThat(stats.getAvgResponseTimeMs()).isEqualTo(2500L);
        assertThat(stats.getFailureCount()).isEqualTo(1);
    }

    @Test
    void testSessionIdCanBeNullInBuilderButWillFailValidation() {
        // Given - Lombok builder allows null, but Jakarta Validation will catch it
        var stats = SearchSessionStatistics.builder()
            .sessionId(null)
            .engineType(SearchEngineType.SEARXNG)
            .build();

        // Then - Entity created but will fail bean validation on persist
        assertThat(stats).isNotNull();
        assertThat(stats.getSessionId()).isNull();
    }

    @Test
    void testEngineTypeCanBeNullInBuilderButWillFailValidation() {
        // Given - Lombok builder allows null, but Jakarta Validation will catch it
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(null)
            .build();

        // Then - Entity created but will fail bean validation on persist
        assertThat(stats).isNotNull();
        assertThat(stats.getEngineType()).isNull();
    }

    @Test
    void testCalculateHitRateWithResults() {
        // Given
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.TAVILY)
            .queriesExecuted(10)
            .resultsReturned(250)
            .build();

        // When
        var hitRate = stats.calculateHitRate();

        // Then - 250 results / 10 queries = 25.0 results per query
        assertThat(hitRate).isEqualTo(25.0);
    }

    @Test
    void testCalculateHitRateWithZeroQueries() {
        // Given
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.TAVILY)
            .queriesExecuted(0)
            .resultsReturned(0)
            .build();

        // When
        var hitRate = stats.calculateHitRate();

        // Then - 0 queries = 0.0 hit rate
        assertThat(hitRate).isEqualTo(0.0);
    }

    @Test
    void testCalculateFailureRateWithFailures() {
        // Given
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.PERPLEXITY)
            .queriesExecuted(10)
            .failureCount(2)
            .build();

        // When
        var failureRate = stats.calculateFailureRate();

        // Then - 2 failures / 10 queries = 0.20 (20%)
        assertThat(failureRate).isEqualTo(0.20);
    }

    @Test
    void testCalculateFailureRateWithZeroQueries() {
        // Given
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.PERPLEXITY)
            .queriesExecuted(0)
            .failureCount(0)
            .build();

        // When
        var failureRate = stats.calculateFailureRate();

        // Then - 0 queries = 0.0 failure rate
        assertThat(failureRate).isEqualTo(0.0);
    }

    @Test
    void testCalculateFailureRateWithAllFailures() {
        // Given
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.SEARXNG)
            .queriesExecuted(5)
            .failureCount(5)
            .build();

        // When
        var failureRate = stats.calculateFailureRate();

        // Then - 5 failures / 5 queries = 1.0 (100%)
        assertThat(failureRate).isEqualTo(1.0);
    }

    @Test
    void testDefaultsForOptionalFields() {
        // When
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.SEARXNG)
            .build();

        // Then
        assertThat(stats.getQueriesExecuted()).isEqualTo(0);
        assertThat(stats.getResultsReturned()).isEqualTo(0);
        assertThat(stats.getAvgResponseTimeMs()).isEqualTo(0L);
        assertThat(stats.getFailureCount()).isEqualTo(0);
    }

    @Test
    void testEqualityBasedOnId() {
        // Given
        var sessionId = UUID.randomUUID();
        var stats1 = SearchSessionStatistics.builder()
            .id(1L)
            .sessionId(sessionId)
            .engineType(SearchEngineType.SEARXNG)
            .queriesExecuted(5)
            .build();

        var stats2 = SearchSessionStatistics.builder()
            .id(1L)
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.TAVILY)
            .queriesExecuted(10)
            .build();

        var stats3 = SearchSessionStatistics.builder()
            .id(2L)
            .sessionId(sessionId)
            .engineType(SearchEngineType.SEARXNG)
            .queriesExecuted(5)
            .build();

        // When/Then
        assertThat(stats1).isEqualTo(stats2); // Same ID
        assertThat(stats1).isNotEqualTo(stats3); // Different ID
    }

    @Test
    void testToStringContainsEngineTypeAndMetrics() {
        // Given
        var stats = SearchSessionStatistics.builder()
            .sessionId(UUID.randomUUID())
            .engineType(SearchEngineType.TAVILY)
            .queriesExecuted(10)
            .resultsReturned(250)
            .build();

        // When
        var toString = stats.toString();

        // Then
        assertThat(toString).contains("TAVILY");
        assertThat(toString).contains("10"); // queriesExecuted
        assertThat(toString).contains("250"); // resultsReturned
    }
}
