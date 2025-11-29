package com.northstar.funding.querygeneration.config;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.strategy.KeywordSearchStrategy;
import com.northstar.funding.querygeneration.strategy.PromptSearchStrategy;
import com.northstar.funding.querygeneration.strategy.SearchStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration for search strategies.
 *
 * <p>Part of NorthStar Ubiquitous Language:
 * <ul>
 *   <li><b>Keyword Search</b> - Short keyword-based queries for traditional search engines</li>
 *   <li><b>Prompt Search</b> - Engineered prompts for AI-powered search engines</li>
 * </ul>
 *
 * <p>Maps search engines to their appropriate strategies:
 * <ul>
 *   <li>Brave, Serper, SearXNG → KeywordSearchStrategy</li>
 *   <li>Perplexica → PromptSearchStrategy</li>
 * </ul>
 */
@Configuration
public class StrategyConfig {

    /**
     * Provides a map of search engines to their search strategies.
     *
     * @param keywordStrategy Keyword Search strategy for traditional search engines
     * @param promptStrategy Prompt Search strategy for AI-powered search engines
     * @return EnumMap of SearchEngineType to SearchStrategy
     */
    @Bean
    public Map<SearchEngineType, SearchStrategy> searchStrategies(
            KeywordSearchStrategy keywordStrategy,
            PromptSearchStrategy promptStrategy) {

        Map<SearchEngineType, SearchStrategy> strategies = new EnumMap<>(SearchEngineType.class);

        // Keyword Search for traditional search engines
        strategies.put(SearchEngineType.BRAVE, keywordStrategy);
        strategies.put(SearchEngineType.SERPER, keywordStrategy);
        strategies.put(SearchEngineType.SEARXNG, keywordStrategy);

        // Prompt Search for AI-powered search engines
        strategies.put(SearchEngineType.PERPLEXICA, promptStrategy);

        return strategies;
    }
}
