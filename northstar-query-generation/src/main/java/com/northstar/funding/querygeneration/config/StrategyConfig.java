package com.northstar.funding.querygeneration.config;

import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.querygeneration.strategy.KeywordQueryStrategy;
import com.northstar.funding.querygeneration.strategy.QueryGenerationStrategy;
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
 *   <li>Perplexica → KeywordQueryStrategy (AI-optimized via LM Studio)</li>
 * </ul>
 */
@Configuration
public class StrategyConfig {

    /**
     * Provides a map of search engines to their query generation strategies.
     *
     * @param keywordStrategy Keyword-based strategy for all search engines
     * @return EnumMap of SearchEngineType to QueryGenerationStrategy
     */
    @Bean
    public Map<SearchEngineType, QueryGenerationStrategy> queryStrategies(
            KeywordQueryStrategy keywordStrategy) {

        Map<SearchEngineType, QueryGenerationStrategy> strategies = new EnumMap<>(SearchEngineType.class);

        // Keyword strategy for all search engines
        strategies.put(SearchEngineType.BRAVE, keywordStrategy);
        strategies.put(SearchEngineType.SERPER, keywordStrategy);
        strategies.put(SearchEngineType.SEARXNG, keywordStrategy);
        strategies.put(SearchEngineType.PERPLEXICA, keywordStrategy);

        return strategies;
    }
}
