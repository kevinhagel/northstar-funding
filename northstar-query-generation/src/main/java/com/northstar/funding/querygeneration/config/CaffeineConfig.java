package com.northstar.funding.querygeneration.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.northstar.funding.querygeneration.model.QueryCacheKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for Caffeine cache.
 *
 * <p>Caches generated queries with:
 * <ul>
 *   <li>24-hour TTL (queries become stale after 24 hours)</li>
 *   <li>1000 entry maximum (LRU eviction when full)</li>
 *   <li>Statistics recording for monitoring</li>
 * </ul>
 */
@Configuration
public class CaffeineConfig {

    @Value("${query-generation.cache.max-size:1000}")
    private int maxSize;

    @Value("${query-generation.cache.ttl-hours:24}")
    private int ttlHours;

    @Value("${query-generation.cache.record-stats:true}")
    private boolean recordStats;

    /**
     * Creates Caffeine cache for query generation results.
     *
     * <p>Cache key: {@link QueryCacheKey} (provider, categories, geographic, maxQueries)
     * <p>Cache value: List of generated query strings
     *
     * @return Configured Caffeine cache
     */
    @Bean
    public Cache<QueryCacheKey, List<String>> queryCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlHours, TimeUnit.HOURS);

        if (recordStats) {
            builder.recordStats();
        }

        return builder.build();
    }
}
