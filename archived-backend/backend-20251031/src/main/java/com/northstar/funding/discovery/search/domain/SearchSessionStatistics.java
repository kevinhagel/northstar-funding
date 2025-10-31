package com.northstar.funding.discovery.search.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Search session statistics entity for tracking search engine performance (Feature 003)
 *
 * Stores per-engine performance metrics for each discovery session.
 * Enables Kevin to identify most productive engines and query patterns.
 *
 * Analytics Use Cases:
 * - Which engines return most high-confidence candidates?
 * - Which engines have best uptime/reliability?
 * - Which engines have fastest response times?
 * - Which query types work best on which engines?
 *
 * Constitutional Compliance:
 * - Spring Data JDBC entity
 * - Lombok @Data, @Builder for boilerplate reduction
 * - Jakarta Validation annotations
 * - Foreign key to discovery_session table
 * - Calculation methods for derived metrics
 *
 * @author NorthStar Funding Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("search_session_statistics")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SearchSessionStatistics {

    /**
     * Primary key (database-generated)
     */
    @Id
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * Foreign key to discovery_session table
     */
    @NotNull(message = "sessionId cannot be null")
    private UUID sessionId;

    /**
     * Search engine type for these statistics
     */
    @NotNull(message = "engineType cannot be null")
    private SearchEngineType engineType;

    /**
     * Number of queries executed against this engine in this session
     */
    @Builder.Default
    @Min(value = 0, message = "queriesExecuted must be non-negative")
    private Integer queriesExecuted = 0;

    /**
     * Total number of results returned by this engine
     */
    @Builder.Default
    @Min(value = 0, message = "resultsReturned must be non-negative")
    private Integer resultsReturned = 0;

    /**
     * Average response time in milliseconds
     */
    @Builder.Default
    @Min(value = 0, message = "avgResponseTimeMs must be non-negative")
    private Long avgResponseTimeMs = 0L;

    /**
     * Number of failed requests (circuit breaker trips, timeouts, errors)
     */
    @Builder.Default
    @Min(value = 0, message = "failureCount must be non-negative")
    private Integer failureCount = 0;

    /**
     * When these statistics were recorded
     */
    private Instant createdAt;

    /**
     * Calculate hit rate (average results per query)
     *
     * @return Average number of results per query, or 0.0 if no queries executed
     */
    public double calculateHitRate() {
        if (queriesExecuted == null || queriesExecuted == 0) {
            return 0.0;
        }

        return (double) resultsReturned / queriesExecuted;
    }

    /**
     * Calculate failure rate (percentage of failed requests)
     *
     * @return Failure rate as decimal (0.0 to 1.0), or 0.0 if no queries executed
     */
    public double calculateFailureRate() {
        if (queriesExecuted == null || queriesExecuted == 0) {
            return 0.0;
        }

        return (double) failureCount / queriesExecuted;
    }

    /**
     * Calculate success rate (percentage of successful requests)
     *
     * @return Success rate as decimal (0.0 to 1.0), or 0.0 if no queries executed
     */
    public double calculateSuccessRate() {
        return 1.0 - calculateFailureRate();
    }
}
