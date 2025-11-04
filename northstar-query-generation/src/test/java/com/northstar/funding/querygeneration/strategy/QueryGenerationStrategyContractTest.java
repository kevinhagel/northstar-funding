package com.northstar.funding.querygeneration.strategy;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.template.CategoryMapper;
import com.northstar.funding.querygeneration.template.GeographicMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Contract test for QueryGenerationStrategy interface.
 *
 * <p>Unit tests for strategy implementations with mocked dependencies.
 */
class QueryGenerationStrategyContractTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private GeographicMapper geographicMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock mappers to return simple strings
        when(categoryMapper.toKeywords(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .thenReturn("scholarships");
        when(categoryMapper.toConceptualDescription(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
                .thenReturn("individual scholarship programs");
        when(categoryMapper.toConceptualDescription(FundingSearchCategory.STEM_EDUCATION))
                .thenReturn("STEM education initiatives");

        when(geographicMapper.toKeywords(GeographicScope.BULGARIA))
                .thenReturn("Bulgaria");
        when(geographicMapper.toConceptualDescription(GeographicScope.BULGARIA))
                .thenReturn("Bulgaria and Bulgarian regions");
        when(geographicMapper.toConceptualDescription(GeographicScope.EASTERN_EUROPE))
                .thenReturn("Eastern European countries");

        // Mock LLM to return formatted query list
        when(chatModel.generate(anyString())).thenReturn(
                "1. scholarship Bulgaria students\n" +
                "2. Bulgarian education funding\n" +
                "3. student grants Bulgaria\n" +
                "4. Bulgaria university scholarships\n" +
                "5. Bulgarian student financial aid"
        );
    }

    @Test
    void keywordStrategy_shouldReturnCompletableFuture() throws Exception {
        // Arrange
        QueryGenerationStrategy strategy = new KeywordQueryStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        CompletableFuture<List<String>> future = strategy.generateQueries(
                Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS),
                GeographicScope.BULGARIA,
                5
        );

        // Assert
        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);

        List<String> queries = future.get(5, TimeUnit.SECONDS);
        assertThat(queries).isNotEmpty();
    }

    @Test
    void keywordStrategy_shouldReturnCorrectSearchEngine() {
        // Arrange
        QueryGenerationStrategy strategy = new KeywordQueryStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        SearchEngineType engine = strategy.getSearchEngine();

        // Assert
        assertThat(engine).isIn(
                SearchEngineType.BRAVE,
                SearchEngineType.SERPER,
                SearchEngineType.SEARXNG
        );
    }

    @Test
    void keywordStrategy_shouldReturnKeywordQueryType() {
        // Arrange
        QueryGenerationStrategy strategy = new KeywordQueryStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        String queryType = strategy.getQueryType();

        // Assert
        assertThat(queryType).isEqualTo("keyword");
    }

    @Test
    void tavilyStrategy_shouldReturnCompletableFuture() throws Exception {
        // Arrange
        QueryGenerationStrategy strategy = new TavilyQueryStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        CompletableFuture<List<String>> future = strategy.generateQueries(
                Set.of(FundingSearchCategory.STEM_EDUCATION),
                GeographicScope.EASTERN_EUROPE,
                3
        );

        // Assert
        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);

        List<String> queries = future.get(5, TimeUnit.SECONDS);
        assertThat(queries).isNotEmpty();
    }

    @Test
    void tavilyStrategy_shouldReturnTavilySearchEngine() {
        // Arrange
        QueryGenerationStrategy strategy = new TavilyQueryStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        SearchEngineType engine = strategy.getSearchEngine();

        // Assert
        assertThat(engine).isEqualTo(SearchEngineType.TAVILY);
    }

    @Test
    void tavilyStrategy_shouldReturnAiOptimizedQueryType() {
        // Arrange
        QueryGenerationStrategy strategy = new TavilyQueryStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        String queryType = strategy.getQueryType();

        // Assert
        assertThat(queryType).isEqualTo("ai-optimized");
    }

    @Test
    void strategy_shouldBeThreadSafe() throws Exception {
        // Arrange
        QueryGenerationStrategy strategy = new KeywordQueryStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act - Execute same strategy from multiple threads
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(strategy.generateQueries(
                    Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS),
                    GeographicScope.BULGARIA,
                    5
            ));
        }

        // Assert - All should complete successfully
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);

        for (CompletableFuture<List<String>> future : futures) {
            assertThat(future.get()).isNotEmpty();
        }
    }
}
