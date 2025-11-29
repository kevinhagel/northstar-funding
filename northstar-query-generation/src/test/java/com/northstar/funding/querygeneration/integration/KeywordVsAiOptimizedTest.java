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
 * Integration test for query generation across different search engines.
 *
 * <p>As of 2025-11-29, all search engines use KeywordQueryStrategy:
 * <ul>
 *   <li>Brave, Serper, SearXNG: Standard keyword search</li>
 *   <li>Perplexica: Keywords sent to AI search (LM Studio handles AI optimization)</li>
 * </ul>
 *
 * <p>Note: Previously tested Tavily's AI-optimized queries, but Tavily was removed
 * and Perplexica handles AI optimization internally via LM Studio.
 */
@SpringBootTest
@ActiveProfiles("test")
class KeywordVsAiOptimizedTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    /**
     * Test that all search engines receive keyword-style queries.
     */
    @Test
    void generateQueries_forAllEngines_shouldProduceKeywordQueries() throws Exception {
        UUID sessionId = UUID.randomUUID();

        // Arrange - Keyword query request (Brave Search)
        QueryGenerationRequest braveRequest = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(sessionId)
                .build();

        // Arrange - Perplexica query request
        QueryGenerationRequest perplexicaRequest = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.PERPLEXICA)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(sessionId)
                .build();

        // Act
        QueryGenerationResponse braveResponse = queryGenerationService
                .generateQueries(braveRequest)
                .get(30, TimeUnit.SECONDS);

        QueryGenerationResponse perplexicaResponse = queryGenerationService
                .generateQueries(perplexicaRequest)
                .get(30, TimeUnit.SECONDS);

        // Assert - Both engines receive keyword-style queries
        assertThat(braveResponse.getQueries()).isNotEmpty();
        assertThat(perplexicaResponse.getQueries()).isNotEmpty();

        // At least 60% of queries should contain funding-related terms
        long matchingBraveQueries = braveResponse.getQueries().stream()
                .filter(query -> query.toLowerCase().matches(".*\\b(infrastructure|grant|funding|facility|building|scholarship|program)\\b.*"))
                .count();
        assertThat(matchingBraveQueries).isGreaterThanOrEqualTo((long) (braveResponse.getQueries().size() * 0.6));

        long matchingPerplexicaQueries = perplexicaResponse.getQueries().stream()
                .filter(query -> query.toLowerCase().matches(".*\\b(infrastructure|grant|funding|facility|building|scholarship|program)\\b.*"))
                .count();
        assertThat(matchingPerplexicaQueries).isGreaterThanOrEqualTo((long) (perplexicaResponse.getQueries().size() * 0.6));
    }

    /**
     * Validate keyword queries for all keyword-based engines.
     */
    @Test
    void generateQueries_forAllKeywordEngines_shouldProduceShortQueries() throws Exception {
        SearchEngineType[] keywordEngines = {
                SearchEngineType.BRAVE,
                SearchEngineType.SERPER,
                SearchEngineType.SEARXNG
        };

        for (SearchEngineType engine : keywordEngines) {
            // Arrange
            QueryGenerationRequest request = QueryGenerationRequest.builder()
                    .searchEngine(engine)
                    .categories(Set.of(FundingSearchCategory.TEACHER_DEVELOPMENT))
                    .geographic(GeographicScope.EASTERN_EUROPE)
                    .maxQueries(3)
                    .sessionId(UUID.randomUUID())
                    .build();

            // Act
            QueryGenerationResponse response = queryGenerationService
                    .generateQueries(request)
                    .get(30, TimeUnit.SECONDS);

            // Assert - Most keyword engines produce short queries (smaller models may occasionally generate longer ones)
            long shortQueries = response.getQueries().stream()
                    .filter(query -> query.split("\\s+").length < 10)
                    .count();
            assertThat(shortQueries).isGreaterThanOrEqualTo((long)(response.getQueries().size() * 0.6)); // At least 60% short
        }
    }

    /**
     * Perplexica uses the same keyword strategy as other engines.
     * (AI optimization happens internally in Perplexica via LM Studio)
     */
    @Test
    void generateQueries_forPerplexica_shouldProduceKeywordQueries() throws Exception {
        // Arrange
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.PERPLEXICA)
                .categories(Set.of(FundingSearchCategory.STEM_EDUCATION))
                .geographic(GeographicScope.EU_MEMBER_STATES)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Assert - Perplexica receives keyword queries (AI optimization is internal)
        assertThat(response.getQueries()).isNotEmpty();
        assertThat(response.getQueries()).hasSizeGreaterThanOrEqualTo(1);

        // At least 60% of queries should include STEM/education terms
        long matchingQueries = response.getQueries().stream()
                .filter(query -> {
                    String lowerQuery = query.toLowerCase();
                    return lowerQuery.contains("stem") ||
                           lowerQuery.contains("science") ||
                           lowerQuery.contains("technology") ||
                           lowerQuery.contains("education") ||
                           lowerQuery.contains("mathematics") ||
                           lowerQuery.contains("computing") ||
                           lowerQuery.contains("grant") ||
                           lowerQuery.contains("funding") ||
                           lowerQuery.contains("scholarship");
                })
                .count();
        assertThat(matchingQueries).isGreaterThanOrEqualTo((long)(response.getQueries().size() * 0.6));
    }
}
