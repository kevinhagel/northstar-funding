package com.northstar.funding.querygeneration.config;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.strategy.KeywordQueryStrategy;
import com.northstar.funding.querygeneration.strategy.QueryGenerationStrategy;
import com.northstar.funding.querygeneration.strategy.TavilyQueryStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration for query generation strategies.
 *
 * <p>Maps search engines to their appropriate strategies:
 * <ul>
 *   <li>Brave, Serper, SearXNG → KeywordQueryStrategy (short, keyword-focused)</li>
 *   <li>Tavily → TavilyQueryStrategy (long, AI-optimized)</li>
 * </ul>
 */
@Configuration
public class StrategyConfig {

    /**
     * Provides a map of search engines to their query generation strategies.
     *
     * @param keywordStrategy Keyword-based strategy for traditional search engines
     * @param tavilyStrategy AI-optimized strategy for Tavily
     * @return EnumMap of SearchEngineType to QueryGenerationStrategy
     */
    @Bean
    public Map<SearchEngineType, QueryGenerationStrategy> queryStrategies(
            KeywordQueryStrategy keywordStrategy,
            TavilyQueryStrategy tavilyStrategy) {

        Map<SearchEngineType, QueryGenerationStrategy> strategies = new EnumMap<>(SearchEngineType.class);

        // Keyword strategy for traditional search engines
        strategies.put(SearchEngineType.BRAVE, keywordStrategy);
        strategies.put(SearchEngineType.SERPER, keywordStrategy);
        strategies.put(SearchEngineType.SEARXNG, keywordStrategy);

        // AI-optimized strategy for Tavily
        strategies.put(SearchEngineType.TAVILY, tavilyStrategy);

        return strategies;
    }
}
