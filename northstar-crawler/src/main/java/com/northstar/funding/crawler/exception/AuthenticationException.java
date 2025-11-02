package com.northstar.funding.crawler.exception;

/**
 * Exception thrown when authentication with a search provider fails.
 *
 * This typically indicates:
 * - Missing or invalid API key
 * - Expired API key
 * - API key with insufficient permissions
 */
public class AuthenticationException extends SearchProviderException {

    private final String providerName;

    public AuthenticationException(String providerName, String message) {
        super(String.format("Authentication failed for provider '%s': %s",
                providerName, message));
        this.providerName = providerName;
    }

    public AuthenticationException(String providerName, String message, Throwable cause) {
        super(String.format("Authentication failed for provider '%s': %s",
                providerName, message), cause);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
