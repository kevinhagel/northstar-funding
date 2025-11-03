package com.northstar.funding.querygeneration.model;

import com.northstar.funding.domain.SearchEngineType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response object containing generated queries.
 *
 * <p>Immutable value object returned from query generation operations.
 */
@Value
@Builder
public class QueryGenerationResponse {
    /**
     * Search engine these queries target.
     */
    SearchEngineType searchEngine;

    /**
     * Generated query strings.
     * List is immutable and contains 0-maxQueries elements.
     */
    List<String> queries;

    /**
     * True if queries were retrieved from cache, false if newly generated.
     */
    boolean fromCache;

    /**
     * When queries were generated or retrieved.
     * If fromCache=true, this is the retrieval time, not original generation time.
     */
    Instant generatedAt;

    /**
     * Discovery session identifier.
     */
    UUID sessionId;

    /**
     * Creates a response with immutable query list.
     *
     * @param searchEngine Search engine
     * @param queries Query list (will be copied to ensure immutability)
     * @param fromCache Whether from cache
     * @param generatedAt Generation/retrieval timestamp
     * @param sessionId Session identifier
     * @return Immutable response
     */
    public static QueryGenerationResponse of(
            SearchEngineType searchEngine,
            List<String> queries,
            boolean fromCache,
            Instant generatedAt,
            UUID sessionId) {
        return QueryGenerationResponse.builder()
                .searchEngine(searchEngine)
                .queries(List.copyOf(queries))  // Ensure immutability
                .fromCache(fromCache)
                .generatedAt(generatedAt)
                .sessionId(sessionId)
                .build();
    }
}
