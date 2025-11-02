package com.northstar.funding.crawler.exception;

import java.time.Duration;

/**
 * Exception thrown when a search provider request times out.
 *
 * This indicates that the provider did not respond within the
 * configured timeout period.
 */
public class ProviderTimeoutException extends SearchProviderException {

    private final String providerName;
    private final Duration timeout;

    public ProviderTimeoutException(String providerName, Duration timeout) {
        super(String.format("Request to provider '%s' timed out after %d ms",
                providerName, timeout.toMillis()));
        this.providerName = providerName;
        this.timeout = timeout;
    }

    public ProviderTimeoutException(String providerName, Duration timeout, Throwable cause) {
        super(String.format("Request to provider '%s' timed out after %d ms",
                providerName, timeout.toMillis()), cause);
        this.providerName = providerName;
        this.timeout = timeout;
    }

    public String getProviderName() {
        return providerName;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
