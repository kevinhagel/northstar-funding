package com.northstar.funding.searchadapters.exception;

import com.northstar.funding.domain.SearchEngineType;

/**
 * Custom exception for search adapter failures.
 *
 * Thrown when:
 * - API authentication fails (invalid API key)
 * - Network connectivity fails
 * - API rate limit exceeded
 * - API returns 5xx server error
 * - Request timeout
 *
 * NOT thrown for:
 * - Zero search results (return empty list instead)
 */
public class SearchAdapterException extends RuntimeException {

    private final SearchEngineType engineType;
    private final String query;

    public SearchAdapterException(SearchEngineType engineType, String query, String message) {
        super(String.format("[%s] Failed query='%s': %s", engineType, query, message));
        this.engineType = engineType;
        this.query = query;
    }

    public SearchAdapterException(SearchEngineType engineType, String query, String message, Throwable cause) {
        super(String.format("[%s] Failed query='%s': %s", engineType, query, message), cause);
        this.engineType = engineType;
        this.query = query;
    }

    public SearchEngineType getEngineType() {
        return engineType;
    }

    public String getQuery() {
        return query;
    }
}
