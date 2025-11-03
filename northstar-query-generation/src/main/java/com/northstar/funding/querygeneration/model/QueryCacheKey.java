package com.northstar.funding.querygeneration.model;

import com.northstar.funding.domain.FundingSearchCategory;
import com.northstar.funding.domain.GeographicScope;
import com.northstar.funding.domain.SearchEngineType;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

/**
 * Cache key for Caffeine cache.
 *
 * <p>Immutable value object used as cache key to identify unique query sets.
 * Two keys are equal if all fields match (searchEngine, categories, geographic, maxQueries).
 */
@Value
@Builder
public class QueryCacheKey {
    /**
     * Search engine.
     */
    SearchEngineType searchEngine;

    /**
     * Funding categories.
     * Set equality is based on content, not reference.
     */
    Set<FundingSearchCategory> categories;

    /**
     * Geographic scope.
     */
    GeographicScope geographic;

    /**
     * Max queries requested.
     */
    int maxQueries;

    /**
     * Creates a cache key from a request.
     *
     * @param request Query generation request
     * @return Cache key
     */
    public static QueryCacheKey from(QueryGenerationRequest request) {
        return QueryCacheKey.builder()
                .searchEngine(request.getSearchEngine())
                .categories(request.getCategories())
                .geographic(request.getGeographic())
                .maxQueries(request.getMaxQueries())
                .build();
    }
}
