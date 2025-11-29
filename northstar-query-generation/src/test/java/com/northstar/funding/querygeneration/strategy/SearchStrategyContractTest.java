package com.northstar.funding.querygeneration.strategy;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.template.CategoryMapper;
import com.northstar.funding.querygeneration.template.GeographicMapper;
import dev.langchain4j.model.chat.ChatModel;
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
 * Contract test for SearchStrategy interface.
 *
 * <p>Part of NorthStar Ubiquitous Language:
 * <ul>
 *   <li><b>Keyword Search</b> - Short keyword-based queries for traditional search engines</li>
 *   <li><b>Prompt Search</b> - Engineered prompts for AI-powered search engines</li>
 * </ul>
 *
 * <p>Unit tests for strategy implementations with mocked dependencies.
 */
class SearchStrategyContractTest {

    @Mock
    private ChatModel chatModel;

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
        when(chatModel.chat(anyString())).thenReturn(
                "1. scholarship Bulgaria students\n" +
                "2. Bulgarian education funding\n" +
                "3. student grants Bulgaria\n" +
                "4. Bulgaria university scholarships\n" +
                "5. Bulgarian student financial aid"
        );
    }

    @Test
    void keywordSearchStrategy_shouldReturnCompletableFuture() throws Exception {
        // Arrange
        SearchStrategy strategy = new KeywordSearchStrategy(
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
    void keywordSearchStrategy_shouldReturnCorrectSearchEngine() {
        // Arrange
        SearchStrategy strategy = new KeywordSearchStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        SearchEngineType engine = strategy.getSearchEngine();

        // Assert - KeywordSearchStrategy is used for traditional search engines
        assertThat(engine).isIn(
                SearchEngineType.BRAVE,
                SearchEngineType.SERPER,
                SearchEngineType.SEARXNG
        );
    }

    @Test
    void keywordSearchStrategy_shouldReturnKeywordSearchType() {
        // Arrange
        SearchStrategy strategy = new KeywordSearchStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        String searchType = strategy.getSearchType();

        // Assert
        assertThat(searchType).isEqualTo("keyword");
    }

    @Test
    void promptSearchStrategy_shouldReturnPromptSearchType() {
        // Arrange
        SearchStrategy strategy = new PromptSearchStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        String searchType = strategy.getSearchType();

        // Assert
        assertThat(searchType).isEqualTo("prompt");
    }

    @Test
    void promptSearchStrategy_shouldReturnPerplexicaSearchEngine() {
        // Arrange
        SearchStrategy strategy = new PromptSearchStrategy(
                chatModel,
                categoryMapper,
                geographicMapper
        );

        // Act
        SearchEngineType engine = strategy.getSearchEngine();

        // Assert
        assertThat(engine).isEqualTo(SearchEngineType.PERPLEXICA);
    }

    @Test
    void strategy_shouldBeThreadSafe() throws Exception {
        // Arrange
        SearchStrategy strategy = new KeywordSearchStrategy(
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
