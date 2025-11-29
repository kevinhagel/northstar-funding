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
 * Prompt Search Strategy - generates engineered prompts for AI-powered search engines.
 *
 * <p>Part of NorthStar Ubiquitous Language:
 * <ul>
 *   <li><b>Keyword Search</b> - Short keyword-based queries for traditional search engines</li>
 *   <li><b>Prompt Search</b> - Engineered prompts for AI-powered search engines</li>
 * </ul>
 *
 * <p>Used for: Perplexica (and future AI search engines like Tavily)
 * <p>Query style: Full sentences/questions with criteria, exclusions, and format requirements (15-40 words)
 * <p>Example: "What are the current EU funding programs available for educational institutions
 * in Bulgaria that support teacher training and STEM education initiatives?
 * Exclude gambling, adult content, and non-educational sites."
 *
 * <p>Prompt engineering considerations:
 * <ul>
 *   <li>Includes positive criteria (what we're looking for)</li>
 *   <li>Includes negative criteria (exclusions)</li>
 *   <li>Tuned to the LLM powering the AI search engine</li>
 *   <li>May include format requirements for responses</li>
 * </ul>
 */
@Component
public class PromptSearchStrategy implements SearchStrategy {

    private static final Logger log = LoggerFactory.getLogger(PromptSearchStrategy.class);

    private final ChatModel chatModel;
    private final CategoryMapper categoryMapper;
    private final GeographicMapper geographicMapper;

    public PromptSearchStrategy(
            ChatModel chatModel,
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
                log.debug("🎯 Generating {} prompt search queries for {} in {}",
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

                Prompt prompt = PromptTemplates.PROMPT_SEARCH_TEMPLATE.apply(variables);

                // Call LM Studio
                String response = chatModel.chat(prompt.text());

                // Parse response into query list
                List<String> queries = parseQueries(response, maxQueries);

                log.info("✅ Generated {} prompt search queries successfully", queries.size());
                return queries;

            } catch (Exception e) {
                log.error("❌ Failed to generate prompt search queries, using fallback", e);
                return getFallbackQueries(maxQueries);
            }
        });
    }

    @Override
    public SearchEngineType getSearchEngine() {
        return SearchEngineType.PERPLEXICA;
    }

    @Override
    public String getSearchType() {
        return "prompt";
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
        return Arrays.stream(PromptTemplates.PROMPT_SEARCH_FALLBACK_QUERIES)
                .limit(maxQueries)
                .collect(Collectors.toList());
    }
}
