package com.northstar.funding.discovery.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for automated discovery workflow.
 *
 * Includes:
 * - Scheduled job configuration
 * - Query generation templates (geography, funding types, organization types)
 */
@Component
@ConfigurationProperties(prefix = "discovery")
@Data
@ToString
@Slf4j
public class DiscoveryProperties {

    private ScheduleConfig schedule = new ScheduleConfig();
    private QueryGenerationConfig queryGeneration = new QueryGenerationConfig();

    @Data
    @ToString
    public static class ScheduleConfig {
        /**
         * Whether the scheduled discovery job is enabled.
         * Default: false (manual execution for testing)
         */
        private boolean enabled = false;

        /**
         * Cron expression for scheduling.
         * Default: "0 0 2 * * ?" (2 AM nightly)
         */
        private String cron = "0 0 2 * * ?";
    }

    @Data
    @ToString
    public static class QueryGenerationConfig {
        /**
         * Geographic regions to target for funding discovery.
         */
        private List<String> geographyTemplates = new ArrayList<>();

        /**
         * Types of funding to search for.
         */
        private List<String> fundingTypes = new ArrayList<>();

        /**
         * Types of organizations to target.
         */
        private List<String> organizationTypes = new ArrayList<>();
    }

    @PostConstruct
    public void logConfiguration() {
        log.info("🔧 Discovery Configuration:");
        log.info("  - Schedule Enabled: {}", schedule.enabled);
        log.info("  - Schedule Cron: {}", schedule.cron);
        log.info("  - Geography Templates: {} items", queryGeneration.geographyTemplates.size());
        log.info("  - Funding Types: {} items", queryGeneration.fundingTypes.size());
        log.info("  - Organization Types: {} items", queryGeneration.organizationTypes.size());

        validateConfiguration();
    }

    private void validateConfiguration() {
        if (schedule.enabled && (schedule.cron == null || schedule.cron.isBlank())) {
            log.warn("⚠️ Discovery schedule enabled but cron expression not configured!");
        }

        if (queryGeneration.geographyTemplates.isEmpty()) {
            log.warn("⚠️ No geography templates configured for query generation");
        }

        if (queryGeneration.fundingTypes.isEmpty()) {
            log.warn("⚠️ No funding types configured for query generation");
        }

        if (queryGeneration.organizationTypes.isEmpty()) {
            log.warn("⚠️ No organization types configured for query generation");
        }

        log.info("✅ Discovery configuration validated");
    }

    /**
     * Check if discovery is properly configured for query generation.
     */
    public boolean isQueryGenerationConfigured() {
        return !queryGeneration.geographyTemplates.isEmpty() &&
               !queryGeneration.fundingTypes.isEmpty() &&
               !queryGeneration.organizationTypes.isEmpty();
    }
}
