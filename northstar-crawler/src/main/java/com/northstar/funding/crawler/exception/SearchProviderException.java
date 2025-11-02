package com.northstar.funding.crawler.exception;

/**
 * Base exception for all search provider-related errors.
 *
 * This is the parent class for specific exceptions like
 * RateLimitException, AuthenticationException, and ProviderTimeoutException.
 */
public class SearchProviderException extends RuntimeException {

    public SearchProviderException(String message) {
        super(message);
    }

    public SearchProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SearchProviderException(Throwable cause) {
        super(cause);
    }
}
