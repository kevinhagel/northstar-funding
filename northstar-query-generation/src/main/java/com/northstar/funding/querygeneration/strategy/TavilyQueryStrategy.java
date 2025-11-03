package com.northstar.funding.querygeneration.strategy;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.template.CategoryMapper;
import com.northstar.funding.querygeneration.template.GeographicMapper;
import com.northstar.funding.querygeneration.template.PromptTemplates;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Strategy for generating AI-optimized queries for Tavily search.
 *
 * <p>Used for: Tavily AI search
 * <p>Query style: Longer, contextual (15-30 words)
 */
@Component
public class TavilyQueryStrategy implements QueryGenerationStrategy {

    private static final Logger log = LoggerFactory.getLogger(TavilyQueryStrategy.class);

    private final ChatLanguageModel chatModel;
    private final CategoryMapper categoryMapper;
    private final GeographicMapper geographicMapper;

    public TavilyQueryStrategy(
            ChatLanguageModel chatModel,
            CategoryMapper categoryMapper,
            GeographicMapper geographicMapper) {
        this.chatModel = chatModel;
        this.categoryMapper = categoryMapper;
        this.geographicMapper = geographicMapper;
    }

    @Override
    public CompletableFuture<List<String>> generateQueries(
            Set<FundingSearchCategory> categories,
            GeographicScope geographic,
            int maxQueries) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("🎯 Generating {} AI-optimized queries for {} in {}",
                        maxQueries, categories, geographic);

                // Map categories and geographic scope to conceptual descriptions
                String categoryDescriptions = categories.stream()
                        .map(categoryMapper::toConceptualDescription)
                        .collect(Collectors.joining("; "));

                String geographicDescription = geographicMapper.toConceptualDescription(geographic);

                // Build prompt from template
                Map<String, Object> variables = Map.of(
                        "categories", categoryDescriptions,
                        "geographic", geographicDescription,
                        "maxQueries", maxQueries
                );

                Prompt prompt = PromptTemplates.TAVILY_QUERY_TEMPLATE.apply(variables);

                // Call LM Studio
                String response = chatModel.generate(prompt.text());

                // Parse response into query list
                List<String> queries = parseQueries(response, maxQueries);

                log.info("✅ Generated {} AI-optimized queries successfully", queries.size());
                return queries;

            } catch (Exception e) {
                log.error("❌ Failed to generate AI-optimized queries, using fallback", e);
                return getFallbackQueries(maxQueries);
            }
        });
    }

    @Override
    public SearchEngineType getSearchEngine() {
        return SearchEngineType.TAVILY;
    }

    @Override
    public String getQueryType() {
        return "ai-optimized";
    }

    /**
     * Parses LLM response into list of queries.
     *
     * <p>Expected format:
     * <pre>
     * 1. query one
     * 2. query two
     * 3. query three
     * </pre>
     */
    private List<String> parseQueries(String response, int maxQueries) {
        return Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.replaceFirst("^\\d+\\.\\s*", "")) // Remove "1. " prefix
                .filter(line -> !line.isEmpty())
                .limit(maxQueries)
                .collect(Collectors.toList());
    }

    /**
     * Returns fallback queries when LLM is unavailable.
     */
    private List<String> getFallbackQueries(int maxQueries) {
        return Arrays.stream(PromptTemplates.TAVILY_FALLBACK_QUERIES)
                .limit(maxQueries)
                .collect(Collectors.toList());
    }
}
