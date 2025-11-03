package com.northstar.funding.querygeneration.service;

import com.northstar.funding.querygeneration.model.QueryCacheKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing query cache and persistence.
 */
public interface QueryCacheService {

    /**
     * Get queries from cache if present.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST return Optional.empty() if key not in cache</li>
     *   <li>MUST return cached queries if present and not expired</li>
     *   <li>MUST complete in &lt;50ms</li>
     *   <li>MUST be thread-safe</li>
     * </ul>
     *
     * @param key Cache key identifying the query set
     * @return Optional containing cached queries, or empty if not cached
     */
    Optional<List<String>> getFromCache(QueryCacheKey key);

    /**
     * Cache generated queries.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST store queries with 24hr TTL</li>
     *   <li>MUST be thread-safe</li>
     *   <li>MUST handle cache full scenario (LRU eviction)</li>
     *   <li>MUST complete immediately (non-blocking)</li>
     * </ul>
     *
     * @param key Cache key
     * @param queries Query list to cache
     */
    void cacheQueries(QueryCacheKey key, List<String> queries);

    /**
     * Persist queries to database asynchronously.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST be non-blocking (fire-and-forget)</li>
     *   <li>MUST create SearchQuery entities for each query</li>
     *   <li>MUST associate with sessionId</li>
     *   <li>MUST handle database errors gracefully (log, don't fail)</li>
     *   <li>MUST return immediately</li>
     * </ul>
     *
     * @param key Cache key containing metadata
     * @param queries Query list to persist
     * @param sessionId Discovery session ID
     * @return CompletableFuture completing when persistence done
     */
    CompletableFuture<Void> persistQueries(
            QueryCacheKey key,
            List<String> queries,
            UUID sessionId
    );

    /**
     * Get cache statistics.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST return hit rate (0.0 - 1.0)</li>
     *   <li>MUST return total requests, hits, misses</li>
     *   <li>MUST return current cache size</li>
     * </ul>
     *
     * @return Map of statistic name → value
     */
    Map<String, Object> getStatistics();

    /**
     * Clear all cached queries.
     *
     * <p>Contract:
     * <ul>
     *   <li>MUST remove all cache entries</li>
     *   <li>MUST reset statistics</li>
     *   <li>MUST be idempotent</li>
     * </ul>
     */
    void clearCache();
}
