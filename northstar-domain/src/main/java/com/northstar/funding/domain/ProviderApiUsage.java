package com.northstar.funding.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Provider API Usage Tracking Entity
 *
 * Tracks API usage for search providers to monitor:
 * - Daily quota consumption (rate limiting)
 * - Success/failure rates per provider
 * - Response times and performance metrics
 * - Error patterns (authentication, timeout, rate limit)
 *
 * Used for:
 * - Resilience4j rate limiter verification
 * - Provider performance analytics
 * - Cost tracking for paid APIs
 * - Debugging provider integration issues
 */
@Table("provider_api_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderApiUsage {

    @Id
    private Long id;

    /**
     * Search engine type (BRAVE_SEARCH, SERPER, PERPLEXICA, SEARXNG)
     * Stored as enum name string
     */
    private String provider;

    /**
     * Search query that was executed
     */
    private String query;

    /**
     * Number of results returned (0 if error)
     */
    private Integer resultCount;

    /**
     * Was the API call successful?
     */
    private Boolean success;

    /**
     * Error type if unsuccessful (NULL if success)
     * Values: TIMEOUT, RATE_LIMIT, AUTHENTICATION, HTTP_ERROR, etc.
     */
    private String errorType;

    /**
     * When the API call was executed
     */
    private LocalDateTime executedAt;

    /**
     * API response time in milliseconds
     */
    private Integer responseTimeMs;
}
