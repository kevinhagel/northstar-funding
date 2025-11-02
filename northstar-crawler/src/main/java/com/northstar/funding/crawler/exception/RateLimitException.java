package com.northstar.funding.crawler.exception;

/**
 * Exception thrown when a search provider's rate limit is exceeded.
 *
 * This indicates that the daily quota for a specific provider has been
 * reached and no more requests can be made until the quota resets.
 */
public class RateLimitException extends SearchProviderException {

    private final String providerName;
    private final int dailyLimit;

    public RateLimitException(String providerName, int dailyLimit) {
        super(String.format("Rate limit exceeded for provider '%s' (daily limit: %d)",
                providerName, dailyLimit));
        this.providerName = providerName;
        this.dailyLimit = dailyLimit;
    }

    public RateLimitException(String providerName, int dailyLimit, Throwable cause) {
        super(String.format("Rate limit exceeded for provider '%s' (daily limit: %d)",
                providerName, dailyLimit), cause);
        this.providerName = providerName;
        this.dailyLimit = dailyLimit;
    }

    public String getProviderName() {
        return providerName;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}
