package com.northstar.funding.querygeneration.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.northstar.funding.domain.SearchQuery;
import com.northstar.funding.persistence.repository.SearchQueryRepository;
import com.northstar.funding.querygeneration.model.QueryCacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Caffeine-based cache service for query generation.
 *
 * <p>Provides:
 * <ul>
 *   <li>24-hour TTL with LRU eviction (max 1000 entries)</li>
 *   <li>Async PostgreSQL persistence for selective queries</li>
 *   <li>Cache statistics for monitoring</li>
 * </ul>
 */
@Service
public class QueryCacheServiceImpl implements QueryCacheService {

    private static final Logger log = LoggerFactory.getLogger(QueryCacheServiceImpl.class);

    private final Cache<QueryCacheKey, List<String>> cache;
    private final SearchQueryRepository searchQueryRepository;

    public QueryCacheServiceImpl(
            Cache<QueryCacheKey, List<String>> cache,
            SearchQueryRepository searchQueryRepository) {
        this.cache = cache;
        this.searchQueryRepository = searchQueryRepository;
    }

    @Override
    public Optional<List<String>> getFromCache(QueryCacheKey key) {
        List<String> cached = cache.getIfPresent(key);
        if (cached != null) {
            log.debug("✅ Cache HIT for key: {}", key);
            return Optional.of(cached);
        }
        log.debug("❌ Cache MISS for key: {}", key);
        return Optional.empty();
    }

    @Override
    public void cacheQueries(QueryCacheKey key, List<String> queries) {
        cache.put(key, queries);
        log.debug("💾 Cached {} queries for key: {}", queries.size(), key);
    }

    @Override
    public CompletableFuture<Void> persistQueries(
            QueryCacheKey key,
            List<String> queries,
            UUID sessionId) {

        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("💾 Persisting {} queries for session {} to PostgreSQL",
                        queries.size(), sessionId);

                // Convert categories to tags (CATEGORY:name format)
                Set<String> tags = key.getCategories().stream()
                        .map(cat -> "CATEGORY:" + cat.name())
                        .collect(Collectors.toSet());

                // Add geographic tag
                tags.add("GEOGRAPHY:" + key.getGeographic().name());

                // Create and save SearchQuery entities
                for (String queryText : queries) {
                    SearchQuery entity = SearchQuery.builder()
                            .queryText(queryText)
                            .dayOfWeek("MONDAY") // Default for AI-generated queries
                            .tags(tags)
                            .targetEngines(Set.of(key.getSearchEngine().name()))
                            .expectedResults(25)
                            .enabled(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .generationMethod("AI_GENERATED")
                            .aiModelUsed("ollama-llama3.1:8b")
                            .generationDate(LocalDate.now())
                            .build();

                    searchQueryRepository.save(entity);
                }

                log.info("✅ Persisted {} queries successfully for session {}",
                        queries.size(), sessionId);

            } catch (Exception e) {
                log.error("❌ Failed to persist queries to PostgreSQL for session: {}", sessionId, e);
                // Don't propagate exception - persistence is optional
            }
        });
    }

    @Override
    public Map<String, Object> getStatistics() {
        var stats = cache.stats();
        return Map.of(
                "hitRate", stats.hitRate(),
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "requestCount", stats.requestCount(),
                "evictionCount", stats.evictionCount(),
                "size", cache.estimatedSize()
        );
    }

    @Override
    public void clearCache() {
        cache.invalidateAll();
        log.info("🧹 Cleared entire query cache");
    }
}
