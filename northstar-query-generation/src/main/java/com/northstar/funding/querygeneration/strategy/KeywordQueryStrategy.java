package com.northstar.funding.querygeneration.strategy;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.template.CategoryMapper;
import com.northstar.funding.querygeneration.template.GeographicMapper;
import com.northstar.funding.querygeneration.template.PromptTemplates;
import dev.langchain4j.model.chat.ChatModel;
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
 * Strategy for generating keyword-based queries for traditional search engines.
 *
 * <p>Used for: Brave Search, Serper, SearXNG
 * <p>Query style: Short, keyword-focused (3-8 words)
 */
@Component
public class KeywordQueryStrategy implements QueryGenerationStrategy {

    private static final Logger log = LoggerFactory.getLogger(KeywordQueryStrategy.class);

    private final ChatModel chatModel;
    private final CategoryMapper categoryMapper;
    private final GeographicMapper geographicMapper;
    private final SearchEngineType searchEngine;

    public KeywordQueryStrategy(
            ChatModel chatModel,
            CategoryMapper categoryMapper,
            GeographicMapper geographicMapper) {
        this.chatModel = chatModel;
        this.categoryMapper = categoryMapper;
        this.geographicMapper = geographicMapper;
        this.searchEngine = SearchEngineType.BRAVE; // Default to BRAVE for keyword strategy
    }

    @Override
    public CompletableFuture<List<String>> generateQueries(
            Set<FundingSearchCategory> categories,
            GeographicScope geographic,
            int maxQueries) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("🎯 Generating {} keyword queries for {} in {}",
                        maxQueries, categories, geographic);

                // Map categories and geographic scope to keywords
                String categoryKeywords = categories.stream()
                        .map(categoryMapper::toKeywords)
                        .collect(Collectors.joining(", "));

                String geographicKeywords = geographicMapper.toKeywords(geographic);

                // Build prompt from template
                Map<String, Object> variables = Map.of(
                        "categories", categoryKeywords,
                        "geographic", geographicKeywords,
                        "maxQueries", maxQueries
                );

                Prompt prompt = PromptTemplates.KEYWORD_QUERY_TEMPLATE.apply(variables);

                // Call LM Studio
                String response = chatModel.chat(prompt.text());

                // Parse response into query list
                List<String> queries = parseQueries(response, maxQueries);

                log.info("✅ Generated {} keyword queries successfully", queries.size());
                return queries;

            } catch (Exception e) {
                log.error("❌ Failed to generate keyword queries, using fallback", e);
                return getFallbackQueries(maxQueries);
            }
        });
    }

    @Override
    public SearchEngineType getSearchEngine() {
        return searchEngine;
    }

    @Override
    public String getQueryType() {
        return "keyword";
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
     *
     * <p>Filters out preamble text like "Here are N queries:" that some models include.
     */
    private List<String> parseQueries(String response, int maxQueries) {
        return Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !isPreamble(line)) // Filter out preambles
                .map(line -> line.replaceFirst("^\\d+\\.\\s*", "")) // Remove "1. " prefix
                .map(line -> line.replaceAll("^\"+|\"+$", "")) // Remove surrounding quotes
                .filter(line -> !line.isEmpty())
                .limit(maxQueries)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a line is a preamble that should be filtered out.
     *
     * <p>Common preambles:
     * <ul>
     *   <li>"Here are N queries:"</li>
     *   <li>"Here are N search queries:"</li>
     *   <li>"Here are some queries:"</li>
     * </ul>
     */
    private boolean isPreamble(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith("here are") ||
               lower.startsWith("here is") ||
               lower.contains("search queries:") ||
               lower.contains("queries:");
    }

    /**
     * Returns fallback queries when LLM is unavailable.
     */
    private List<String> getFallbackQueries(int maxQueries) {
        return Arrays.stream(PromptTemplates.KEYWORD_FALLBACK_QUERIES)
                .limit(maxQueries)
                .collect(Collectors.toList());
    }
}
