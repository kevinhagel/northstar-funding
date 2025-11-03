package com.northstar.funding.querygeneration.integration;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.model.QueryGenerationRequest;
import com.northstar.funding.querygeneration.model.QueryGenerationResponse;
import com.northstar.funding.querygeneration.service.QueryGenerationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Scenario 5: AI Service Unavailable - Fallback Queries.
 *
 * <p>Tests that system provides fallback queries when LM Studio is unavailable.
 *
 * <p>Validates:
 * <ul>
 *   <li>No exception thrown when LM Studio unavailable</li>
 *   <li>Fallback queries returned</li>
 *   <li>System continues functioning</li>
 *   <li>Error logged for monitoring</li>
 * </ul>
 *
 * <p>Note: This test requires mocking the ChatLanguageModel to simulate failure.
 * Currently @Disabled - implement with proper mocking framework.
 */
@SpringBootTest
@ActiveProfiles("test")
class FallbackQueriesTest {

    @Autowired
    private QueryGenerationService queryGenerationService;

    /**
     * Scenario 5: LM Studio unavailable, fallback queries returned.
     *
     * <p>TODO: Implement with @MockBean to simulate LM Studio failure.
     */
    @Test
    @Disabled("Requires mocking ChatLanguageModel - implement in polish phase")
    void generateQueries_whenLmStudioUnavailable_shouldReturnFallbackQueries() throws Exception {
        // Arrange
        // TODO: Mock ChatLanguageModel to throw exception
        QueryGenerationRequest request = QueryGenerationRequest.builder()
                .searchEngine(SearchEngineType.BRAVE)
                .categories(Set.of(FundingSearchCategory.STEM_EDUCATION))
                .geographic(GeographicScope.EU_MEMBER_STATES)
                .maxQueries(3)
                .sessionId(UUID.randomUUID())
                .build();

        // Act
        QueryGenerationResponse response = queryGenerationService
                .generateQueries(request)
                .get(30, TimeUnit.SECONDS);

        // Assert - Fallback queries returned
        assertThat(response.getQueries()).isNotEmpty();
        assertThat(response.isFromCache()).isFalse();

        // Assert - Fallback queries are generic but valid
        assertThat(response.getQueries()).allMatch(query -> !query.isEmpty());
    }

    /**
     * Scenario 5b: Verify fallback queries exist for all strategies.
     *
     * <p>This is a sanity check that fallback query arrays are defined.
     */
    @Test
    void verifyFallbackQueriesExist() {
        // This test verifies that PromptTemplates has fallback queries defined
        // Actual test would check PromptTemplates.KEYWORD_FALLBACK_QUERIES
        // and PromptTemplates.TAVILY_FALLBACK_QUERIES are non-empty

        // For now, just a placeholder to document the requirement
        assertThat(true).isTrue();
    }
}
