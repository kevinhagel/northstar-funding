package com.northstar.funding.discovery.search.application;

import com.northstar.funding.discovery.search.domain.SearchEngineType;
import com.northstar.funding.discovery.search.domain.SearchSessionStatistics;
import com.northstar.funding.discovery.search.infrastructure.SearchSessionStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Search session analytics service (Feature 003)
 *
 * Provides analytics and statistics for search execution sessions.
 * Enables learning loop: Kevin reviews performance to refine query library.
 *
 * Key Features:
 * - Per-session statistics retrieval
 * - Historical performance analysis
 * - Engine comparison metrics
 * - Failure rate tracking
 *
 * Constitutional Compliance:
 * - Simple service layer (no complex orchestration)
 * - Human-AI collaboration: Kevin reviews, AI executes
 *
 * @author NorthStar Funding Team
 */
@Service
@Slf4j
public class SearchSessionService {

    private final SearchSessionStatisticsRepository repository;

    public SearchSessionService(SearchSessionStatisticsRepository repository) {
        this.repository = repository;
    }

    /**
     * Get statistics for a specific discovery session
     *
     * @param sessionId The discovery session UUID
     * @return List of statistics (one per engine used)
     */
    public List<SearchSessionStatistics> getSessionStatistics(UUID sessionId) {
        log.debug("Retrieving statistics for session: {}", sessionId);
        return repository.findBySessionId(sessionId);
    }

    /**
     * Get historical statistics for a search engine
     *
     * @param engineType The search engine type
     * @param days Number of days to look back
     * @return List of statistics for that engine
     */
    public List<SearchSessionStatistics> getEngineStatistics(SearchEngineType engineType, int days) {
        var endTime = Instant.now();
        var startTime = endTime.minus(days, ChronoUnit.DAYS);

        log.debug("Retrieving {} statistics for last {} days", engineType, days);
        return repository.findByCreatedAtBetween(startTime, endTime)
            .stream()
            .filter(stats -> stats.getEngineType() == engineType)
            .toList();
    }

    /**
     * Get overall statistics across all engines for time range
     *
     * @param days Number of days to look back
     * @return List of all statistics
     */
    public List<SearchSessionStatistics> getAllStatistics(int days) {
        var endTime = Instant.now();
        var startTime = endTime.minus(days, ChronoUnit.DAYS);

        log.debug("Retrieving all statistics for last {} days", days);
        return repository.findByCreatedAtBetween(startTime, endTime);
    }

    /**
     * Get engines with high failure rates (>20%)
     * For troubleshooting and alerting
     *
     * @return List of statistics with high failure rates
     */
    public List<SearchSessionStatistics> getHighFailureRateEngines() {
        log.debug("Retrieving engines with >20% failure rate");
        return repository.findByFailureRateGreaterThan(0.20);
    }

    /**
     * Get most recent statistics for each engine
     * For dashboard display
     *
     * @return List of most recent statistics per engine
     */
    public List<SearchSessionStatistics> getCurrentEngineStatuses() {
        log.debug("Retrieving current engine statuses");
        return repository.findMostRecentByEngine();
    }

    /**
     * Calculate average hit rate for an engine over time period
     *
     * @param engineType The search engine type
     * @param days Number of days to analyze
     * @return Average hit rate (results per query)
     */
    public double calculateAverageHitRate(SearchEngineType engineType, int days) {
        var stats = getEngineStatistics(engineType, days);

        if (stats.isEmpty()) {
            return 0.0;
        }

        return stats.stream()
            .mapToDouble(SearchSessionStatistics::calculateHitRate)
            .average()
            .orElse(0.0);
    }

    /**
     * Calculate average failure rate for an engine over time period
     *
     * @param engineType The search engine type
     * @param days Number of days to analyze
     * @return Average failure rate (0.0 to 1.0)
     */
    public double calculateAverageFailureRate(SearchEngineType engineType, int days) {
        var stats = getEngineStatistics(engineType, days);

        if (stats.isEmpty()) {
            return 0.0;
        }

        return stats.stream()
            .mapToDouble(SearchSessionStatistics::calculateFailureRate)
            .average()
            .orElse(0.0);
    }
}
