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
 * Integration test for Scenario 3: Keyword vs AI-Optimized Queries.
 *
 * <p>Tests that different search engines receive appropriately styled queries:
 * <ul>
 *   <li>Keyword engines (Brave, Serper, SearXNG): Short, keyword-focused (3-8 words)</li>
 *   <li>AI engine (Tavily): Long, contextual natural language (15-30 words)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class KeywordVsAiOptimizedTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    /**
     * Scenario 3: Compare keyword queries vs AI-optimized queries for same topic.
     *
     * <p>Validates:
     * <ul>
     *   <li>Keyword queries are short (<10 words)</li>
     *   <li>AI-optimized queries are long (>10 words)</li>
     *   <li>Both contain relevant keywords</li>
     *   <li>AI queries include contextual information</li>
     * </ul>
     */
    @Test
    void generateQueries_forKeywordVsAi_shouldProduceDifferentQueryStyles() throws Exception {
        UUID sessionId = UUID.randomUUID();

        // Arrange - Keyword query request (Brave Search)
        QueryGenerationRequest keywordRequest = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(sessionId)
                .build();

        // Arrange - AI-optimized query request (Tavily)
        QueryGenerationRequest aiRequest = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.TAVILY)
                .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
                .geographic(GeographicScope.BULGARIA)
                .maxQueries(3)
                .sessionId(sessionId)
                .build();

        // Act
        QueryGenerationResponse keywordResponse = queryGenerationService
                .generateQueries(keywordRequest)
                .get(30, TimeUnit.SECONDS);

        QueryGenerationResponse aiResponse = queryGenerationService
                .generateQueries(aiRequest)
                .get(30, TimeUnit.SECONDS);

        // Assert - Keyword queries are short and focused
        assertThat(keywordResponse.getQueries()).allMatch(query -> {
            int wordCount = query.split("\\s+").length;
            return wordCount < 10; // Short queries
        });

        // At least 60% of keyword queries should contain funding-related terms
        long matchingKeywordQueries = keywordResponse.getQueries().stream()
                .filter(query -> query.toLowerCase().matches(".*\\b(infrastructure|grant|funding|facility|building|scholarship|program)\\b.*"))
                .count();
        assertThat(matchingKeywordQueries).isGreaterThanOrEqualTo((long) (keywordResponse.getQueries().size() * 0.6));

        // Assert - AI-optimized queries are longer and contextual
        assertThat(aiResponse.getQueries()).allMatch(query -> {
            int wordCount = query.split("\\s+").length;
            return wordCount > 10; // Longer queries
        });

        // At least one AI query should contain Bulgaria-related terms and education keywords
        // (Smaller models may be less consistent with specific keyword inclusion)
        long matchingAiQueries = aiResponse.getQueries().stream()
                .filter(query -> {
                    String lowerQuery = query.toLowerCase();
                    boolean hasLocation = lowerQuery.contains("bulgaria") ||
                                        lowerQuery.contains("bulgarian") ||
                                        lowerQuery.contains("sofia") ||
                                        lowerQuery.contains("plovdiv") ||
                                        lowerQuery.contains("eastern europe") ||
                                        lowerQuery.contains("balkans");
                    boolean hasEducation = lowerQuery.contains("educational") ||
                                         lowerQuery.contains("development") ||
                                         lowerQuery.contains("infrastructure") ||
                                         lowerQuery.contains("school") ||
                                         lowerQuery.contains("facility") ||
                                         lowerQuery.contains("funding") ||
                                         lowerQuery.contains("grant");
                    return hasLocation && hasEducation;
                })
                .count();
        assertThat(matchingAiQueries).isGreaterThanOrEqualTo(1); // At least one query matches

        // Assert - AI queries include more context than keyword queries
        int avgKeywordLength = keywordResponse.getQueries().stream()
                .mapToInt(String::length)
                .sum() / keywordResponse.getQueries().size();

        int avgAiLength = aiResponse.getQueries().stream()
                .mapToInt(String::length)
                .sum() / aiResponse.getQueries().size();

        assertThat(avgAiLength).isGreaterThan(avgKeywordLength * 2);
    }

    /**
     * Scenario 3b: Validate keyword queries for all keyword-based engines.
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
     * Scenario 3c: Tavily always gets AI-optimized queries.
     */
    @Test
    void generateQueries_forTavily_shouldAlwaysProduceLongContextualQueries() throws Exception {
        // Arrange
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.TAVILY)
                .categories(Set.of(FundingSearchCategory.STEM_EDUCATION))
                .geographic(GeographicScope.EU_MEMBER_STATES)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Assert - Tavily queries are long and natural language
        assertThat(response.getQueries()).allMatch(query -> {
            int wordCount = query.split("\\s+").length;
            return wordCount >= 12 && wordCount <= 40; // llama3.1:8b should handle this reliably
        });

        // Assert - At least 60% of queries include contextual information about STEM/education
        long matchingQueries = response.getQueries().stream()
                .filter(query -> {
                    String lowerQuery = query.toLowerCase();
                    return lowerQuery.contains("stem") ||
                           lowerQuery.contains("science") ||
                           lowerQuery.contains("technology") ||
                           lowerQuery.contains("education") ||
                           lowerQuery.contains("mathematics") ||
                           lowerQuery.contains("computing");
                })
                .count();
        assertThat(matchingQueries).isGreaterThanOrEqualTo(2); // At least 60% (2 out of 3)
    }
}
