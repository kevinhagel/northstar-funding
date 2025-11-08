package com.northstar.funding.querygeneration.integration;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Scenario 1: Single Provider Query Generation.
 *
 * <p>Tests generating keyword queries for Brave Search targeting scholarships in Bulgaria.
 *
 * <p>Prerequisites:
 * <ul>
 *   <li>LM Studio running at http://192.168.1.10:1234/v1</li>
 *   <li>PostgreSQL accessible at 192.168.1.10:5432</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class SingleProviderQueryGenerationTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    /**
     * Scenario 1: Generate 5 keyword queries for BRAVE search targeting
     * individual scholarships in Bulgaria.
     *
     * <p>Validates:
     * <ul>
     *   <li>5 queries returned</li>
     *   <li>Queries contain "Bulgaria" or "scholarship"</li>
     *   <li>Response is not from cache (first call)</li>
     *   <li>GeneratedAt timestamp is present</li>
     * </ul>
     */
    @Test
    void generateQueries_forBraveSearch_shouldReturn5ScholarshipQueries() throws Exception {
        // Arrange
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(5)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Assert
        assertThat(response.getSearchEngine()).isEqualTo(SearchEngineType.BRAVE);
        assertThat(response.getQueries()).hasSize(5);
        // At least 60% of queries should contain "bulgaria" or "scholarship" or Bulgarian cities
        long matchingQueries = response.getQueries().stream()
                .filter(query -> {
                    String lowerQuery = query.toLowerCase();
                    return lowerQuery.contains("bulgaria") ||
                           lowerQuery.contains("scholarship") ||
                           lowerQuery.contains("sofia") ||
                           lowerQuery.contains("plovdiv") ||
                           lowerQuery.contains("varna") ||
                           lowerQuery.contains("bulgarian");
                })
                .count();
        assertThat(matchingQueries).isGreaterThanOrEqualTo(3); // At least 60% (3 out of 5)
        assertThat(response.isFromCache()).isFalse(); // First call
        assertThat(response.getGeneratedAt()).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
    }

    /**
     * Scenario 1b: Generate queries for SERPER (another keyword-based engine).
     *
     * <p>Validates that different search engines work with the same strategy.
     */
    @Test
    void generateQueries_forSerper_shouldReturnKeywordQueries() throws Exception {
        // Arrange
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.SERPER)
                .categories(Set.of(FundingSearchCategory.TEACHER_DEVELOPMENT))
                .geographic(GeographicScope.EASTERN_EUROPE)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Assert - llama3.1:8b should reliably generate exact count
        assertThat(response.getSearchEngine()).isEqualTo(SearchEngineType.SERPER);
        assertThat(response.getQueries()).hasSize(3);
        assertThat(response.isFromCache()).isFalse();
    }

    /**
     * Scenario 1c: Generate queries for SEARXNG.
     *
     * <p>Validates third keyword-based search engine.
     */
    @Test
    void generateQueries_forSearXNG_shouldReturnKeywordQueries() throws Exception {
        // Arrange
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.SEARXNG)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BALKANS)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Assert - llama3.1:8b should reliably generate exact count
        assertThat(response.getSearchEngine()).isEqualTo(SearchEngineType.SEARXNG);
        assertThat(response.getQueries()).hasSize(3);
        assertThat(response.isFromCache()).isFalse();
    }
}
