package com.northstar.funding.persistence.service;

import com.northstar.funding.domain.ProviderApiUsage;
import com.northstar.funding.domain.SearchEngineType;
import com.northstar.funding.domain.SearchResult;
import com.northstar.funding.persistence.repository.ProviderApiUsageRepository;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for tracking search provider API usage.
 *
 * Records each API call asynchronously to avoid blocking search threads.
 * Provides usage statistics for monitoring and rate limit verification.
 */
@Service
@Transactional
@Slf4j
public class ApiUsageTrackingService {

    private final ProviderApiUsageRepository providerApiUsageRepository;

    public ApiUsageTrackingService(ProviderApiUsageRepository providerApiUsageRepository) {
        this.providerApiUsageRepository = providerApiUsageRepository;
    }

    /**
     * Track API usage asynchronously (non-blocking).
     *
     * @param provider Search engine type
     * @param query Search query
     * @param result Try containing search results (success or failure)
     * @param responseTimeMs Response time in milliseconds
     */
    @Async("filterExecutor")
    public void trackUsage(
            SearchEngineType provider,
            String query,
            Try<List<SearchResult>> result,
            long responseTimeMs
    ) {
        try {
            ProviderApiUsage usage = ProviderApiUsage.builder()
                    .provider(provider.name())
                    .query(query)
                    .resultCount(result.isSuccess() ? result.get().size() : 0)
                    .success(result.isSuccess())
                    .errorType(result.isFailure() ? classifyError(result.getCause()) : null)
                    .executedAt(LocalDateTime.now())
                    .responseTimeMs((int) responseTimeMs)
                    .build();

            providerApiUsageRepository.save(usage);

            log.debug("Tracked API usage: provider={}, query={}, success={}, responseTime={}ms",
                    provider, query, result.isSuccess(), responseTimeMs);

        } catch (Exception e) {
            // Don't let tracking failures break the search flow
            log.warn("Failed to track API usage for provider {}: {}", provider, e.getMessage());
        }
    }

    /**
     * Get daily usage count for a provider (last 24 hours).
     *
     * @param provider Search engine type
     * @return Number of API calls in last 24 hours
     */
    @Transactional(readOnly = true)
    public int getDailyUsage(SearchEngineType provider) {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return providerApiUsageRepository.countUsageSince(provider.name(), since);
    }

    /**
     * Get usage statistics since a given timestamp.
     *
     * @param since Start timestamp
     * @return List of usage statistics per provider
     */
    @Transactional(readOnly = true)
    public List<ProviderApiUsageRepository.ProviderUsageStats> getUsageStatsSince(LocalDateTime since) {
        return providerApiUsageRepository.getUsageStatsSince(since);
    }

    /**
     * Classify error type from Throwable.
     */
    private String classifyError(Throwable error) {
        if (error == null) {
            return null;
        }

        String className = error.getClass().getSimpleName();
        String message = error.getMessage() != null ? error.getMessage().toLowerCase() : "";

        if (className.contains("RateLimit") || message.contains("rate limit")) {
            return "RATE_LIMIT";
        } else if (className.contains("Timeout") || message.contains("timeout")) {
            return "TIMEOUT";
        } else if (className.contains("Authentication") || message.contains("unauthorized")) {
            return "AUTH_FAILURE";
        } else if (className.contains("IOException") || className.contains("ConnectException")) {
            return "NETWORK_ERROR";
        } else {
            return "UNKNOWN";
        }
    }
}
