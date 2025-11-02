package com.northstar.funding.persistence.repository;

import com.northstar.funding.domain.ProviderApiUsage;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for tracking search provider API usage.
 *
 * Supports:
 * - Recording each API call with query, result count, success/error
 * - Counting usage per provider (for rate limiting verification)
 * - Calculating usage statistics (success rate, average response time)
 */
@Repository
public interface ProviderApiUsageRepository extends CrudRepository<ProviderApiUsage, Long> {

    /**
     * Count API usage for a provider since a given timestamp.
     * Used for rate limiting verification.
     *
     * @param provider Provider name (e.g., "BRAVE", "SERPER")
     * @param since Start timestamp
     * @return Number of API calls since timestamp
     */
    @Query("SELECT COUNT(*) FROM provider_api_usage WHERE provider = :provider AND executed_at >= :since")
    int countUsageSince(@Param("provider") String provider, @Param("since") LocalDateTime since);

    /**
     * Get usage statistics per provider since a given timestamp.
     *
     * @param since Start timestamp
     * @return List of usage statistics per provider
     */
    @Query("""
            SELECT provider,
                   COUNT(*) as total_calls,
                   SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as successful_calls,
                   AVG(response_time_ms) as avg_response_time
            FROM provider_api_usage
            WHERE executed_at >= :since
            GROUP BY provider
            """)
    List<ProviderUsageStats> getUsageStatsSince(@Param("since") LocalDateTime since);

    /**
     * Count failed API calls for a provider since a given timestamp.
     *
     * @param provider Provider name
     * @param since Start timestamp
     * @return Number of failed calls
     */
    @Query("SELECT COUNT(*) FROM provider_api_usage WHERE provider = :provider AND success = false AND executed_at >= :since")
    int countFailuresSince(@Param("provider") String provider, @Param("since") LocalDateTime since);

    /**
     * Projection for provider usage statistics.
     */
    interface ProviderUsageStats {
        String getProvider();
        Long getTotalCalls();
        Long getSuccessfulCalls();
        Double getAvgResponseTime();
    }
}
