package com.northstar.funding.crawler.processing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProcessingContext state management.
 * <p>
 * Tests validate:
 * - Duplicate detection optimization (single HashSet operation)
 * - Counter mutation methods (spam, blacklist, low/high confidence, invalid URLs)
 * - Statistics generation accuracy
 * - Bug fix validation (lowConfidenceCreated now increments)
 */
class ProcessingContextTest {

    private UUID sessionId;
    private ProcessingContext context;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        context = new ProcessingContext(sessionId);
    }

    // ========== T005: Duplicate Detection Tests ==========

    @Test
    void markDomainAsSeen_FirstTime_ReturnsTrue() {
        // Given
        String domain = "example.org";

        // When
        boolean isUnique = context.markDomainAsSeen(domain);

        // Then
        assertThat(isUnique).isTrue();
        assertThat(context.getDuplicatesSkipped()).isZero();
    }

    @Test
    void markDomainAsSeen_Duplicate_ReturnsFalseAndIncrements() {
        // Given
        String domain = "example.org";
        context.markDomainAsSeen(domain);  // First occurrence

        // When
        boolean isUnique = context.markDomainAsSeen(domain);  // Second occurrence

        // Then
        assertThat(isUnique).isFalse();
        assertThat(context.getDuplicatesSkipped()).isEqualTo(1);
    }

    // ========== T006: Counter Mutation Tests ==========

    @Test
    void recordSpamTldFiltered_IncrementsCounter() {
        // When
        context.recordSpamTldFiltered();

        // Then
        assertThat(context.getSpamTldFiltered()).isEqualTo(1);
    }

    @Test
    void recordBlacklisted_IncrementsCounter() {
        // When
        context.recordBlacklisted();

        // Then
        assertThat(context.getBlacklistedSkipped()).isEqualTo(1);
    }

    @Test
    void recordLowConfidence_IncrementsCounter() {
        // BUG FIX VALIDATION: This counter was never incremented in original implementation
        // When
        context.recordLowConfidence();

        // Then
        assertThat(context.getLowConfidenceCreated()).isEqualTo(1);
    }

    @Test
    void recordHighConfidence_IncrementsCounter() {
        // When
        context.recordHighConfidence();

        // Then
        assertThat(context.getHighConfidenceCreated()).isEqualTo(1);
    }

    @Test
    void recordInvalidUrl_IncrementsCounter() {
        // NEW FEATURE: Track invalid URLs
        // When
        context.recordInvalidUrl();

        // Then
        assertThat(context.getInvalidUrlsSkipped()).isEqualTo(1);
    }

    // ========== T007: Statistics Generation Test ==========

    @Test
    void buildStatistics_ReturnsAccurateStats() {
        // Given - Simulate processing state
        context.recordSpamTldFiltered();
        context.recordSpamTldFiltered();  // 2 spam filtered
        context.markDomainAsSeen("domain1.org");
        context.markDomainAsSeen("domain1.org");  // 1 duplicate
        context.recordBlacklisted();  // 1 blacklisted
        context.recordHighConfidence();  // 1 high confidence
        context.recordLowConfidence();  // 1 low confidence
        context.recordInvalidUrl();  // 1 invalid URL

        // When
        ProcessingStatistics stats = context.buildStatistics(10);

        // Then
        assertThat(stats.getTotalResults()).isEqualTo(10);
        assertThat(stats.getSpamTldFiltered()).isEqualTo(2);
        assertThat(stats.getDuplicatesSkipped()).isEqualTo(1);
        assertThat(stats.getBlacklistedSkipped()).isEqualTo(1);
        assertThat(stats.getHighConfidenceCreated()).isEqualTo(1);
        assertThat(stats.getLowConfidenceCreated()).isEqualTo(1);
        assertThat(stats.getInvalidUrlsSkipped()).isEqualTo(1);
    }
}
