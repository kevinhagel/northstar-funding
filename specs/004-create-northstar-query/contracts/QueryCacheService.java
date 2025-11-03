/**
 * API Contract: QueryCacheService
 *
 * Service for managing query cache and persistence.
 *
 * Module: northstar-query-generation
 * Package: com.northstar.funding.querygeneration.service
 */

package com.northstar.funding.querygeneration.service;

import com.northstar.funding.querygeneration.model.QueryCacheKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface QueryCacheService {

    /**
     * Get queries from cache if present.
     *
     * CONTRACT:
     * - MUST return Optional.empty() if key not in cache
     * - MUST return cached queries if present and not expired
     * - MUST complete in <50ms
     * - MUST be thread-safe
     *
     * @param key Cache key identifying the query set
     * @return Optional containing cached queries, or empty if not cached
     */
    Optional<List<String>> getFromCache(QueryCacheKey key);

    /**
     * Cache generated queries.
     *
     * CONTRACT:
     * - MUST store queries with 24hr TTL
     * - MUST be thread-safe
     * - MUST handle cache full scenario (LRU eviction)
     * - MUST complete immediately (non-blocking)
     *
     * @param key Cache key
     * @param queries Query list to cache
     */
    void cacheQueries(QueryCacheKey key, List<String> queries);

    /**
     * Persist queries to database asynchronously.
     *
     * CONTRACT:
     * - MUST be non-blocking (fire-and-forget)
     * - MUST create SearchQuery entities for each query
     * - MUST associate with sessionId
     * - MUST handle database errors gracefully (log, don't fail)
     * - MUST return immediately
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
     * CONTRACT:
     * - MUST return hit rate (0.0 - 1.0)
     * - MUST return total requests, hits, misses
     * - MUST return current cache size
     *
     * @return Map of statistic name → value
     */
    java.util.Map<String, Object> getStatistics();

    /**
     * Clear all cached queries.
     *
     * CONTRACT:
     * - MUST remove all cache entries
     * - MUST reset statistics
     * - MUST be idempotent
     */
    void clearCache();
}
