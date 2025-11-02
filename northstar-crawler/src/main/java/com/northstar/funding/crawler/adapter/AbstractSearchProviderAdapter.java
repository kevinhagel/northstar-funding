package com.northstar.funding.crawler.adapter;

import com.northstar.funding.crawler.exception.AuthenticationException;
import com.northstar.funding.crawler.exception.ProviderTimeoutException;
import com.northstar.funding.crawler.exception.RateLimitException;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for search provider adapters.
 *
 * Provides common functionality:
 * - HTTP client initialization with timeout configuration
 * - Domain normalization (lowercase, remove www, remove protocol)
 * - Rate limit tracking (thread-safe in-memory counter with daily reset)
 * - Common error handling (HTTP status codes → custom exceptions)
 * - SearchResult entity population helpers
 *
 * Thread Safety: All implementations are thread-safe for Virtual Thread execution.
 */
public abstract class AbstractSearchProviderAdapter implements SearchProviderAdapter {

    protected final HttpClient httpClient;
    protected final int maxResults;
    protected final int timeoutSeconds;
    protected final int dailyRateLimit;

    // Thread-safe rate limiting
    private final AtomicInteger currentUsageCount = new AtomicInteger(0);
    private final AtomicLong lastResetTimestamp = new AtomicLong(System.currentTimeMillis());

    /**
     * Constructor for subclasses.
     *
     * @param timeoutSeconds HTTP request timeout in seconds
     * @param maxResults Maximum results per search
     * @param dailyRateLimit Daily API call limit
     */
    protected AbstractSearchProviderAdapter(int timeoutSeconds, int maxResults, int dailyRateLimit) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxResults = maxResults;
        this.dailyRateLimit = dailyRateLimit;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public int getCurrentUsageCount() {
        checkAndResetRateLimitIfNeeded();
        return currentUsageCount.get();
    }

    @Override
    public int getRateLimit() {
        return dailyRateLimit;
    }

    /**
     * Increment the API usage counter and check rate limit.
     *
     * @throws RateLimitException if rate limit exceeded
     */
    protected void incrementUsageAndCheckLimit() {
        checkAndResetRateLimitIfNeeded();
        int newCount = currentUsageCount.incrementAndGet();
        if (newCount > dailyRateLimit) {
            throw new RateLimitException(getProviderType().name(), dailyRateLimit);
        }
    }

    /**
     * Reset rate limit counter if 24 hours have elapsed since last reset.
     * Thread-safe using AtomicLong compare-and-swap.
     */
    private void checkAndResetRateLimitIfNeeded() {
        long now = System.currentTimeMillis();
        long lastReset = lastResetTimestamp.get();
        long millisIn24Hours = 24 * 60 * 60 * 1000;

        if (now - lastReset >= millisIn24Hours) {
            // Attempt to update the reset timestamp atomically
            if (lastResetTimestamp.compareAndSet(lastReset, now)) {
                currentUsageCount.set(0);
            }
        }
    }

    /**
     * Normalize domain name: lowercase, remove protocol, remove www.
     *
     * Examples:
     * - "https://www.example.com/path" → "example.com"
     * - "HTTP://EXAMPLE.COM" → "example.com"
     * - "www.example.com" → "example.com"
     *
     * @param url URL or domain string
     * @return Normalized domain name
     */
    protected String normalizeDomain(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        String normalized = url.toLowerCase().trim();

        // Remove protocol
        normalized = normalized.replaceFirst("^https?://", "");

        // Remove www.
        normalized = normalized.replaceFirst("^www\\.", "");

        // Extract domain (remove path, query, fragment)
        int slashIndex = normalized.indexOf('/');
        if (slashIndex != -1) {
            normalized = normalized.substring(0, slashIndex);
        }

        return normalized;
    }

    /**
     * Create a base SearchResult with common fields populated.
     *
     * @param url Original URL from search result
     * @param title Page title
     * @param description Text snippet/description
     * @param rankPosition Result position (1-based ranking)
     * @param discoverySessionId Discovery session UUID
     * @return SearchResult builder with common fields set
     */
    protected SearchResult.SearchResultBuilder createBaseSearchResult(
            String url,
            String title,
            String description,
            int rankPosition,
            java.util.UUID discoverySessionId) {

        String domain = normalizeDomain(url);

        return SearchResult.builder()
                .url(url)
                .domain(domain)
                .title(title)
                .description(description)
                .rankPosition(rankPosition)
                .searchEngine(getProviderType())
                .discoveredAt(LocalDateTime.now())
                .discoverySessionId(discoverySessionId)
                .searchDate(LocalDateTime.now().toLocalDate());
    }

    /**
     * Execute an HTTP GET request with timeout and error handling.
     *
     * @param uri Request URI
     * @param headers Request headers (alternating key-value pairs)
     * @return HttpResponse body as String
     * @throws AuthenticationException if 401/403 status code
     * @throws ProviderTimeoutException if request times out
     * @throws RateLimitException if 429 status code
     */
    protected String executeHttpGet(URI uri, String... headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .GET();

        // Add headers (key-value pairs)
        for (int i = 0; i < headers.length; i += 2) {
            if (i + 1 < headers.length) {
                requestBuilder.header(headers[i], headers[i + 1]);
            }
        }

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Handle HTTP errors
            int statusCode = response.statusCode();
            if (statusCode == 401 || statusCode == 403) {
                throw new AuthenticationException(
                        getProviderType().name(),
                        "Invalid or missing API key (HTTP " + statusCode + ")"
                );
            } else if (statusCode == 429) {
                throw new RateLimitException(getProviderType().name(), dailyRateLimit);
            } else if (statusCode >= 400) {
                throw new RuntimeException("HTTP " + statusCode + ": " + response.body());
            }

            return response.body();

        } catch (java.net.http.HttpTimeoutException e) {
            throw new ProviderTimeoutException(
                    getProviderType().name(),
                    Duration.ofSeconds(timeoutSeconds),
                    e
            );
        }
    }

    /**
     * Execute an HTTP POST request with timeout and error handling.
     *
     * @param uri Request URI
     * @param jsonBody JSON request body
     * @param headers Request headers (alternating key-value pairs)
     * @return HttpResponse body as String
     * @throws AuthenticationException if 401/403 status code
     * @throws ProviderTimeoutException if request times out
     * @throws RateLimitException if 429 status code
     */
    protected String executeHttpPost(URI uri, String jsonBody, String... headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        // Add default Content-Type
        requestBuilder.header("Content-Type", "application/json");

        // Add custom headers (key-value pairs)
        for (int i = 0; i < headers.length; i += 2) {
            if (i + 1 < headers.length) {
                requestBuilder.header(headers[i], headers[i + 1]);
            }
        }

        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Handle HTTP errors
            int statusCode = response.statusCode();
            if (statusCode == 401 || statusCode == 403) {
                throw new AuthenticationException(
                        getProviderType().name(),
                        "Invalid or missing API key (HTTP " + statusCode + ")"
                );
            } else if (statusCode == 429) {
                throw new RateLimitException(getProviderType().name(), dailyRateLimit);
            } else if (statusCode >= 400) {
                throw new RuntimeException("HTTP " + statusCode + ": " + response.body());
            }

            return response.body();

        } catch (java.net.http.HttpTimeoutException e) {
            throw new ProviderTimeoutException(
                    getProviderType().name(),
                    Duration.ofSeconds(timeoutSeconds),
                    e
            );
        }
    }
}
