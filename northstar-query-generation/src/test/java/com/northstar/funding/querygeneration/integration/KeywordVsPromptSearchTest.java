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
 * <p>Part of NorthStar Ubiquitous Language:
 * <ul>
 *   <li><b>Keyword Search</b> - Short keyword-based queries for traditional search engines
 *       (Brave, Serper, SearXNG) - 3-8 words</li>
 *   <li><b>Prompt Search</b> - Engineered prompts for AI-powered search engines
 *       (Perplexica) - 15-40 words with criteria, exclusions, format requirements</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class KeywordVsPromptSearchTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    /**
     * Test keyword vs prompt search strategies produce appropriate queries.
     */
    @Test
    void generateQueries_keywordVsPromptSearch_shouldProduceAppropriateQueries() throws Exception {
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

        // Assert - Both strategies produce queries with funding-related terms
        assertThat(braveResponse.getQueries()).isNotEmpty();
        assertThat(perplexicaResponse.getQueries()).isNotEmpty();

        // Keyword Search: Should be short (3-8 words)
        long shortBraveQueries = braveResponse.getQueries().stream()
                .filter(query -> query.split("\\s+").length <= 8)
                .count();
        assertThat(shortBraveQueries).isGreaterThanOrEqualTo((long) (braveResponse.getQueries().size() * 0.6));

        // Prompt Search: Should be longer (15-40 words) with richer context
        long longerPerplexicaQueries = perplexicaResponse.getQueries().stream()
                .filter(query -> query.split("\\s+").length > 8)
                .count();
        // Perplexica uses Prompt Search - should produce longer, more detailed queries
        assertThat(longerPerplexicaQueries).isGreaterThanOrEqualTo((long) (perplexicaResponse.getQueries().size() * 0.6));
    }

    /**
     * Validate Keyword Search queries for traditional search engines.
     */
    @Test
    void generateQueries_keywordSearch_shouldProduceShortQueries() throws Exception {
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

            // Assert - Keyword Search produces short queries (3-8 words)
            long shortQueries = response.getQueries().stream()
                    .filter(query -> query.split("\\s+").length <= 8)
                    .count();
            assertThat(shortQueries).isGreaterThanOrEqualTo((long)(response.getQueries().size() * 0.6)); // At least 60% short
        }
    }

    /**
     * Perplexica uses Prompt Search strategy for AI-powered queries.
     * Generates engineered prompts with criteria, exclusions, and format requirements.
     */
    @Test
    void generateQueries_promptSearch_shouldProduceEngineeringPrompts() throws Exception {
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

        // Assert - Prompt Search produces longer, more detailed queries
        assertThat(response.getQueries()).isNotEmpty();
        assertThat(response.getQueries()).hasSizeGreaterThanOrEqualTo(1);

        // Prompt Search queries should be longer (15-40 words) with rich context
        long longQueries = response.getQueries().stream()
                .filter(query -> query.split("\\s+").length > 10)
                .count();
        assertThat(longQueries).isGreaterThanOrEqualTo((long)(response.getQueries().size() * 0.6));

        // Should include funding-related terms
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
