package com.northstar.funding.crawler.orchestrator;

import com.northstar.funding.domain.SearchEngineType;

/**
 * Error from a single provider.
 */
public record ProviderError(
    SearchEngineType provider,                 // Which provider failed
    String errorMessage,                       // Human-readable error message
    ErrorType errorType,                       // Classified error type
    java.time.LocalDateTime occurredAt,        // When error occurred
    String query                               // Query that failed
) {
    public enum ErrorType {
        RATE_LIMIT,         // HTTP 429 or RateLimitException
        TIMEOUT,            // HttpTimeoutException or timeout exceeded
        AUTH_FAILURE,       // HTTP 401/403 or AuthenticationException
        NETWORK_ERROR,      // ConnectException, IOException
        INVALID_RESPONSE,   // Malformed JSON or unexpected response format
        UNKNOWN             // Other errors
    }
}
