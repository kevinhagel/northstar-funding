package com.northstar.funding.discovery.search.application;

import com.northstar.funding.discovery.search.infrastructure.SearchQueryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * Nightly discovery scheduler (Feature 003)
 *
 * Runs automated search execution nightly at 2:00 AM.
 * Loads queries for current day of week and executes them.
 *
 * Constitutional Compliance:
 * - @Scheduled for simple automation (no complex orchestration)
 * - Conditional execution via configuration property
 * - Human-AI collaboration: Kevin reviews analytics to refine queries
 *
 * @author NorthStar Funding Team
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "discovery.schedule.enabled", havingValue = "true", matchIfMissing = false)
public class NightlyDiscoveryScheduler {

    private final SearchExecutionService searchExecutionService;
    private final SearchQueryRepository searchQueryRepository;

    public NightlyDiscoveryScheduler(
        SearchExecutionService searchExecutionService,
        SearchQueryRepository searchQueryRepository
    ) {
        this.searchExecutionService = searchExecutionService;
        this.searchQueryRepository = searchQueryRepository;
        log.info("NightlyDiscoveryScheduler initialized - nightly execution enabled");
    }

    /**
     * Run nightly discovery at 2:00 AM
     * Cron: "0 0 2 * * ?" = every day at 2:00 AM
     */
    @Scheduled(cron = "${discovery.schedule.cron:0 0 2 * * ?}")
    public void runNightlyDiscovery() {
        var startTime = LocalDateTime.now();
        log.info("=== NIGHTLY DISCOVERY STARTED ===");
        log.info("Start time: {}", startTime);

        try {
            // Get current day of week
            var dayOfWeek = DayOfWeek.from(startTime);
            log.info("Day of week: {}", dayOfWeek);

            // Load queries for today
            var queries = searchQueryRepository.findByDayOfWeekAndEnabled(dayOfWeek);
            log.info("Loaded {} queries for {}", queries.size(), dayOfWeek);

            if (queries.isEmpty()) {
                log.warn("No queries configured for {}", dayOfWeek);
                return;
            }

            // Execute all queries
            var result = searchExecutionService.executeQueries(queries);

            if (result.isSuccess()) {
                var results = result.get();
                log.info("=== NIGHTLY DISCOVERY COMPLETED ===");
                log.info("Queries executed: {}", queries.size());
                log.info("Total results: {}", results.size());
                log.info("Duration: {} seconds",
                    java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds());

                // TODO: Create DiscoverySession record
                // TODO: Pass results to CandidateProcessingOrchestrator
                // TODO: Create SearchSessionStatistics
            } else {
                log.error("Nightly discovery failed: {}", result.getCause().getMessage(), result.getCause());
            }

        } catch (Exception e) {
            log.error("Nightly discovery error: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing
     * Can be called via actuator endpoint or test code
     */
    public void runManual() {
        log.info("Manual discovery trigger");
        runNightlyDiscovery();
    }
}
