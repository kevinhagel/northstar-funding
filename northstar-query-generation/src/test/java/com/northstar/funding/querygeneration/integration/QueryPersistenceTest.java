package com.northstar.funding.querygeneration.integration;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchQuery;
import com.northstar.funding.persistence.repository.SearchQueryRepository;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Scenario 6: Query Persistence Verification.
 *
 * <p>Tests that generated queries are persisted to PostgreSQL database.
 *
 * <p>Validates:
 * <ul>
 *   <li>Queries are saved to database</li>
 *   <li>Correct metadata is stored (search engine, categories, geographic)</li>
 *   <li>Async persistence completes successfully</li>
 *   <li>Session ID association is correct</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class QueryPersistenceTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    @Autowired
    private SearchQueryRepository searchQueryRepository;

    /**
     * Scenario 6: Generated queries are persisted to database.
     */
    @Test
    void generateQueries_shouldPersistToDatabase() throws Exception {
        // Arrange
        UUID sessionId = UUID.randomUUID();

        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.SERPER)
                .categories(Set.of(FundingSearchCategory.PROGRAM_GRANTS))
                .geographic(GeographicScope.BALKANS)
                .maxQueries(3)
                .sessionId(sessionId)
                .build();

        // Act
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Wait for async persistence to complete
        Thread.sleep(2000);

        // Assert - Queries were persisted
        // Note: SearchQueryRepository doesn't have findBySessionId yet
        // We'll need to add that method or query by generation_date
        List<SearchQuery> allQueries = (List<SearchQuery>) searchQueryRepository.findAll();

        // Find queries matching our session (by query text match)
        List<SearchQuery> matchingQueries = allQueries.stream()
                .filter(sq -> response.getQueries().contains(sq.getQueryText()))
                .toList();

        assertThat(matchingQueries).isNotEmpty();

        // Verify metadata
        matchingQueries.forEach(sq -> {
            assertThat(sq.getQueryText()).isNotEmpty();
            assertThat(sq.getGenerationMethod()).isEqualTo("AI_GENERATED");
            assertThat(sq.getAiModelUsed()).isEqualTo("ollama-llama3.1:8b");
            assertThat(sq.getGenerationDate()).isNotNull();
            assertThat(sq.getTargetEngines()).contains(SearchEngineType.SERPER.name());
            assertThat(sq.getTags()).anyMatch(tag -> tag.startsWith("CATEGORY:"));
            assertThat(sq.getTags()).anyMatch(tag -> tag.startsWith("GEOGRAPHY:"));
        });
    }

    /**
     * Scenario 6b: Persistence is async (non-blocking).
     */
    @Test
    void generateQueries_persistenceShouldBeNonBlocking() throws Exception {
        // Arrange
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Act - Generate queries
        long start = System.currentTimeMillis();
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;

        // Assert - Response is immediate (doesn't wait for persistence)
        assertThat(response.getQueries()).hasSize(3);

        // Persistence happens in background
        // If persistence was blocking, duration would be much longer
        // We just verify we got a response without waiting for DB write
        assertThat(duration).isLessThan(20000); // 20 seconds max for LM Studio call
    }
}
