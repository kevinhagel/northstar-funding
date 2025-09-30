package com.northstar.funding.discovery.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.discovery.config.PostgreSQLTestConfiguration;
import com.northstar.funding.discovery.config.TestDataFactory;
import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.domain.SessionType;

/**
 * Integration Tests for DiscoverySessionRepository
 * 
 * Tests PostgreSQL-specific functionality including:
 * - JSONB operations (search_engines_used, search_queries, error_messages, search_engine_failures)
 * - Complex analytics queries with aggregations
 * - Status and type filtering
 * - Date range operations
 * - Performance metrics calculations
 * - Spring Data JDBC enum compatibility (VARCHAR with CHECK constraints)
 */
@DataJdbcTest
@Import({PostgreSQLTestConfiguration.class, TestDataFactory.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class DiscoverySessionRepositoryIT {

    @Autowired
    private DiscoverySessionRepository repository;
    
    @Autowired 
    private TestDataFactory testDataFactory;
    
    private DiscoverySession completedSession;
    private DiscoverySession runningSession;
    private DiscoverySession failedSession;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
        
        // Create test discovery sessions with different states
        completedSession = testDataFactory.discoverySessionBuilder()
            .sessionId(UUID.randomUUID())
            .executedAt(LocalDateTime.now().minusHours(2))
            .executedBy("test-scheduler")
            .sessionType(SessionType.SCHEDULED)
            .status(SessionStatus.COMPLETED)
            .durationMinutes(15)
            .startedAt(LocalDateTime.now().minusHours(2))
            .completedAt(LocalDateTime.now().minusHours(1).minusMinutes(45))
            .searchEnginesUsed(Set.of("searxng", "tavily"))
            .searchQueries(List.of("EU funding technology", "innovation grants"))
            .queryGenerationPrompt("Find EU technology funding opportunities")
            .candidatesFound(25)
            .duplicatesDetected(3)
            .sourcesScraped(50)
            .averageConfidenceScore(0.85)
            .errorMessages(List.of())
            .searchEngineFailures(Map.of())
            .llmModelUsed("llama-3.1-8b")
            .searchParameters(Map.of("region", "EU", "sector", "technology"))
            .build();
            
        runningSession = testDataFactory.discoverySessionBuilder()
            .sessionId(UUID.randomUUID()) 
            .executedAt(LocalDateTime.now().minusMinutes(30))
            .executedBy("manual-trigger")
            .sessionType(SessionType.MANUAL)
            .status(SessionStatus.RUNNING)
            .durationMinutes(0)
            .startedAt(LocalDateTime.now().minusMinutes(30))
            .completedAt(null)
            .searchEnginesUsed(Set.of("perplexity"))
            .searchQueries(List.of("startup funding Germany"))
            .queryGenerationPrompt("Find German startup funding sources")
            .candidatesFound(10)
            .duplicatesDetected(1)
            .sourcesScraped(25)
            .averageConfidenceScore(0.75)
            .errorMessages(List.of())
            .searchEngineFailures(Map.of())
            .llmModelUsed("llama-3.1-70b")
            .searchParameters(Map.of("region", "Germany", "sector", "startup"))
            .build();
            
        failedSession = testDataFactory.discoverySessionBuilder()
            .sessionId(UUID.randomUUID())
            .executedAt(LocalDateTime.now().minusHours(6))
            .executedBy("retry-scheduler")
            .sessionType(SessionType.RETRY)
            .status(SessionStatus.FAILED)
            .durationMinutes(5)
            .startedAt(LocalDateTime.now().minusHours(6))
            .completedAt(LocalDateTime.now().minusHours(5).minusMinutes(55))
            .searchEnginesUsed(Set.of("searxng", "tavily", "perplexity"))
            .searchQueries(List.of("research funding", "academic grants"))
            .queryGenerationPrompt("Find academic research funding")
            .candidatesFound(0)
            .duplicatesDetected(0)
            .sourcesScraped(5)
            .averageConfidenceScore(0.0)
            .errorMessages(List.of("Search engine timeout", "Rate limit exceeded"))
            .searchEngineFailures(Map.of(
                "searxng", List.of("Connection timeout"),
                "tavily", List.of("Rate limit reached", "Authentication failed")
            ))
            .llmModelUsed("llama-3.1-8b")
            .searchParameters(Map.of("region", "global", "sector", "research"))
            .build();
            
        repository.saveAll(List.of(completedSession, runningSession, failedSession));
    }
    
    @Test
    @DisplayName("Should save and retrieve discovery session with JSONB fields")
    void shouldSaveAndRetrieveDiscoverySessionWithJsonb() {
        // When: Finding the completed session
        var found = repository.findById(completedSession.getSessionId());
        
        // Then: All JSONB fields should be preserved
        assertThat(found).isPresent();
        var session = found.get();
        
        assertAll("JSONB field preservation",
            () -> assertThat(session.getSearchEnginesUsed()).containsExactlyInAnyOrder("searxng", "tavily"),
            () -> assertThat(session.getSearchQueries()).containsExactly("EU funding technology", "innovation grants"),
            () -> assertThat(session.getErrorMessages()).isEmpty(),
            () -> assertThat(session.getSearchEngineFailures()).isEmpty(),
            () -> assertThat(session.getSearchParameters()).containsEntry("region", "EU").containsEntry("sector", "technology")
        );
    }
    
    @Test
    @DisplayName("Should handle enum values as VARCHAR with CHECK constraints")
    void shouldHandleEnumValuesAsVarchar() {
        // When: Creating session with all possible enum values
        var testSession = testDataFactory.discoverySessionBuilder()
            .sessionType(SessionType.MANUAL)
            .status(SessionStatus.CANCELLED)
            .build();
            
        var saved = repository.save(testSession);
        var retrieved = repository.findById(saved.getSessionId()).orElseThrow();
        
        // Then: Enum values should be preserved correctly
        assertAll("Enum handling",
            () -> assertThat(retrieved.getSessionType()).isEqualTo(SessionType.MANUAL),
            () -> assertThat(retrieved.getStatus()).isEqualTo(SessionStatus.CANCELLED)
        );
    }
    
    @Test
    @DisplayName("Should find sessions by status")
    void shouldFindSessionsByStatus() {
        // When: Finding sessions by different statuses
        var completedSessions = repository.findByStatus(SessionStatus.COMPLETED);
        var runningSessions = repository.findByStatus(SessionStatus.RUNNING);
        var failedSessions = repository.findByStatus(SessionStatus.FAILED);
        
        // Then: Each status should return correct sessions
        assertAll("Status filtering",
            () -> assertThat(completedSessions).hasSize(1).extracting(DiscoverySession::getSessionId).containsExactly(completedSession.getSessionId()),
            () -> assertThat(runningSessions).hasSize(1).extracting(DiscoverySession::getSessionId).containsExactly(runningSession.getSessionId()),
            () -> assertThat(failedSessions).hasSize(1).extracting(DiscoverySession::getSessionId).containsExactly(failedSession.getSessionId())
        );
    }
    
    @Test
    @DisplayName("Should find sessions by session type")
    void shouldFindSessionsBySessionType() {
        // When: Finding sessions by type
        var scheduledSessions = repository.findBySessionType(SessionType.SCHEDULED);
        var manualSessions = repository.findBySessionType(SessionType.MANUAL);
        var retrySessions = repository.findBySessionType(SessionType.RETRY);
        
        // Then: Each type should return correct sessions
        assertAll("Session type filtering",
            () -> assertThat(scheduledSessions).hasSize(1).extracting(DiscoverySession::getSessionId).containsExactly(completedSession.getSessionId()),
            () -> assertThat(manualSessions).hasSize(1).extracting(DiscoverySession::getSessionId).containsExactly(runningSession.getSessionId()),
            () -> assertThat(retrySessions).hasSize(1).extracting(DiscoverySession::getSessionId).containsExactly(failedSession.getSessionId())
        );
    }
    
    @Test
    @DisplayName("Should find recent sessions ordered by execution time")
    void shouldFindRecentSessionsOrderedByExecutionTime() {
        // When: Finding recent sessions with pagination
        var recentSessions = repository.findRecentSessions(PageRequest.of(0, 10));
        
        // Then: Sessions should be ordered by executed_at DESC
        assertThat(recentSessions).hasSize(3);
        var sessionIds = recentSessions.stream().map(DiscoverySession::getSessionId).toList();
        
        // Running session (most recent) should be first
        assertThat(sessionIds).containsExactly(
            runningSession.getSessionId(),
            completedSession.getSessionId(), 
            failedSession.getSessionId()
        );
    }
    
    @Test  
    @DisplayName("Should find top 10 sessions for test compatibility")
    void shouldFindTop10RecentSessions() {
        // When: Finding top 10 recent sessions
        var sessions = repository.findTop10ByOrderByExecutedAtDesc();
        
        // Then: Should return all sessions ordered by execution time
        assertThat(sessions).hasSize(3);
        assertThat(sessions.get(0).getSessionId()).isEqualTo(runningSession.getSessionId());
    }
    
    @Test
    @DisplayName("Should find sessions within date range")
    void shouldFindSessionsWithinDateRange() {
        // When: Finding sessions within last 4 hours
        var start = LocalDateTime.now().minusHours(4);
        var end = LocalDateTime.now();
        var sessionsInRange = repository.findByExecutedAtBetween(start, end);
        
        // Then: Should exclude the failed session (6 hours ago)
        assertThat(sessionsInRange).hasSize(2)
            .extracting(DiscoverySession::getSessionId)
            .containsExactlyInAnyOrder(completedSession.getSessionId(), runningSession.getSessionId());
    }
    
    @Test
    @DisplayName("Should find failed sessions with JSONB error handling")
    void shouldFindFailedSessionsWithJsonbErrors() {
        // When: Finding failed sessions
        var failedSessions = repository.findFailedSessions(PageRequest.of(0, 10));
        
        // Then: Should find sessions with FAILED status or error messages
        assertThat(failedSessions).hasSize(1);
        var session = failedSessions.get(0);
        
        assertAll("Failed session error handling",
            () -> assertThat(session.getStatus()).isEqualTo(SessionStatus.FAILED),
            () -> assertThat(session.getErrorMessages()).containsExactly("Search engine timeout", "Rate limit exceeded"),
            () -> assertThat(session.getSearchEngineFailures()).hasSize(2)
                .containsKey("searxng").containsKey("tavily")
        );
    }
    
    @Test
    @DisplayName("Should find long running sessions")
    void shouldFindLongRunningSessions() {
        // When: Finding sessions running longer than 1 hour
        var threshold = LocalDateTime.now().minusHours(1);
        var longRunningSessions = repository.findLongRunningSessions(threshold);
        
        // Then: Should not find any (our running session is only 30 minutes old)
        assertThat(longRunningSessions).isEmpty();
        
        // When: Finding sessions running longer than 20 minutes  
        var recentThreshold = LocalDateTime.now().minusMinutes(20);
        var recentLongRunningSessions = repository.findLongRunningSessions(recentThreshold);
        
        // Then: Should find the running session
        assertThat(recentLongRunningSessions).hasSize(1)
            .extracting(DiscoverySession::getSessionId)
            .containsExactly(runningSession.getSessionId());
    }
    
    @Test
    @DisplayName("Should calculate performance metrics with aggregations")
    void shouldCalculatePerformanceMetrics() {
        // When: Getting performance metrics for last 24 hours
        var since = LocalDateTime.now().minusHours(24);
        var metrics = repository.getPerformanceMetrics(since);
        
        // Then: Should calculate averages and counts correctly
        assertAll("Performance metrics",
            () -> assertThat(metrics.getAvgCandidatesFound()).isCloseTo(11.67, within(0.1)), // (25+10+0)/3
            () -> assertThat(metrics.getAvgDurationMinutes()).isCloseTo(6.67, within(0.1)), // (15+0+5)/3  
            () -> assertThat(metrics.getAvgConfidenceScore()).isCloseTo(0.53, within(0.1)), // (0.85+0.75+0.0)/3
            () -> assertThat(metrics.getSuccessfulSessions()).isEqualTo(1), // Only completed session
            () -> assertThat(metrics.getFailedSessions()).isEqualTo(1) // Only failed session
        );
    }
    
    @Test
    @DisplayName("Should find high performing sessions with complex filtering")
    void shouldFindHighPerformingSessions() {
        // When: Finding high performing sessions
        var highPerformingSessions = repository.findHighPerformingSessions(
            20, // min candidates
            0.8, // min confidence
            PageRequest.of(0, 10)
        );
        
        // Then: Should find only the completed session (25 candidates, 0.85 confidence)
        assertThat(highPerformingSessions).hasSize(1)
            .extracting(DiscoverySession::getSessionId)
            .containsExactly(completedSession.getSessionId());
    }
    
    @Test
    @DisplayName("Should find sessions with search engine failures using JSONB queries")
    void shouldFindSessionsWithSearchEngineFailures() {
        // When: Finding sessions with search engine failures
        var sessionsWithFailures = repository.findSessionsWithSearchEngineFailures(PageRequest.of(0, 10));
        
        // Then: Should find only the failed session
        assertThat(sessionsWithFailures).hasSize(1);
        var session = sessionsWithFailures.get(0);
        
        assertThat(session.getSearchEngineFailures())
            .containsKey("searxng")
            .containsKey("tavily");
    }
    
    @Test
    @DisplayName("Should get daily discovery trends with date aggregation")
    void shouldGetDailyDiscoveryTrends() {
        // When: Getting daily trends for last 7 days
        var since = LocalDateTime.now().minusDays(7);
        var trends = repository.getDailyTrends(since);
        
        // Then: Should return aggregated daily statistics
        assertThat(trends).isNotEmpty();
        
        // Verify we have trend data with proper aggregations
        var todayTrends = trends.stream()
            .filter(trend -> trend.getDiscoveryDate().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
            .findFirst();
            
        if (todayTrends.isPresent()) {
            var trend = todayTrends.get();
            assertAll("Daily trend calculations",
                () -> assertThat(trend.getTotalSessions()).isGreaterThan(0),
                () -> assertThat(trend.getSuccessful()).isGreaterThanOrEqualTo(0),
                () -> assertThat(trend.getAvgCandidates()).isGreaterThanOrEqualTo(0.0)
            );
        }
    }
    
    @Test
    @DisplayName("Should find sessions by LLM model for AI performance comparison")
    void shouldFindSessionsByLlmModel() {
        // When: Finding sessions by specific LLM model
        var llama8bSessions = repository.findByLlmModel("llama-3.1-8b", PageRequest.of(0, 10));
        var llama70bSessions = repository.findByLlmModel("llama-3.1-70b", PageRequest.of(0, 10));
        
        // Then: Should filter by model correctly
        assertAll("LLM model filtering",
            () -> assertThat(llama8bSessions).hasSize(1) // completed session uses 8b
                .extracting(DiscoverySession::getSessionId)
                .containsExactly(completedSession.getSessionId()),
            () -> assertThat(llama70bSessions).hasSize(1) // running session uses 70b
                .extracting(DiscoverySession::getSessionId)
                .containsExactly(runningSession.getSessionId())
        );
    }
    
    @Test
    @DisplayName("Should find sessions by search engine using JSONB containment")
    void shouldFindSessionsBySearchEngine() {
        // When: Finding sessions that used specific search engines
        var searxngSessions = repository.findBySearchEngine("searxng", PageRequest.of(0, 10));
        var perplexitySessions = repository.findBySearchEngine("perplexity", PageRequest.of(0, 10));
        
        // Then: Should filter by search engine usage
        assertAll("Search engine filtering",
            () -> assertThat(searxngSessions).hasSize(2) // completed and failed sessions
                .extracting(DiscoverySession::getSessionId)
                .containsExactlyInAnyOrder(completedSession.getSessionId(), failedSession.getSessionId()),
            () -> assertThat(perplexitySessions).hasSize(2) // running and failed sessions
                .extracting(DiscoverySession::getSessionId)
                .containsExactlyInAnyOrder(runningSession.getSessionId(), failedSession.getSessionId())
        );
    }
    
    @Test
    @DisplayName("Should get search engine reliability statistics")
    void shouldGetSearchEngineStats() {
        // When: Getting search engine statistics
        var since = LocalDateTime.now().minusDays(1);
        var stats = repository.getSearchEngineStats(since);
        
        // Then: Should return usage and reliability metrics
        assertThat(stats).isNotEmpty();
        
        // Verify the statistics contain expected data
        var statsMap = stats.stream()
            .collect(java.util.stream.Collectors.toMap(
                stat -> stat.getSearchEnginesUsed(),
                stat -> stat
            ));
            
        // Each search engine combination should have proper metrics
        assertThat(statsMap.values()).allSatisfy(stat -> {
            assertThat(stat.getUsageCount()).isGreaterThan(0);
            assertThat(stat.getAvgCandidates()).isGreaterThanOrEqualTo(0.0);
            assertThat(stat.getFailureCount()).isGreaterThanOrEqualTo(0);
        });
    }
    
    @Test
    @DisplayName("Should calculate duplication statistics")
    void shouldGetDuplicationStats() {
        // When: Getting duplication statistics
        var since = LocalDateTime.now().minusDays(1);
        var stats = repository.getDuplicationStats(since);
        
        // Then: Should calculate duplicate detection effectiveness
        assertThat(stats).isNotNull();
        assertAll("Duplication statistics",
            () -> assertThat(stats.getTotalDuplicatesDetected()).isEqualTo(4), // 3+1+0 from our test data
            () -> assertThat(stats.getTotalCandidatesFound()).isEqualTo(35), // 25+10+0 from our test data
            () -> assertThat(stats.getAvgDuplicateRate()).isGreaterThanOrEqualTo(0.0)
        );
    }
    
    @Test
    @DisplayName("Should find sessions eligible for retry")
    void shouldFindSessionsEligibleForRetry() {
        // When: Finding sessions eligible for retry (failed, recent, not permanent errors)
        var minAge = LocalDateTime.now().minusHours(12); // Include our 6-hour old failed session
        var eligibleSessions = repository.findSessionsEligibleForRetry(minAge);
        
        // Then: Should find the failed session (doesn't have permanent/invalid error keywords)
        assertThat(eligibleSessions).hasSize(1)
            .extracting(DiscoverySession::getSessionId)
            .containsExactly(failedSession.getSessionId());
    }
    
    @Test
    @DisplayName("Should analyze prompt effectiveness for optimization")
    void shouldGetPromptEffectivenessAnalysis() {
        // When: Getting prompt effectiveness analysis
        var since = LocalDateTime.now().minusDays(1);
        var analysis = repository.getPromptEffectivenessAnalysis(since);
        
        // Then: Should return prompt performance metrics
        assertThat(analysis).isNotEmpty();
        
        // Verify each prompt analysis has required fields
        assertThat(analysis).allSatisfy(prompt -> {
            assertThat(prompt.getQueryGenerationPrompt()).isNotNull().isNotBlank();
            assertThat(prompt.getAvgEffectiveness()).isGreaterThanOrEqualTo(0.0);
            assertThat(prompt.getUsageCount()).isGreaterThan(0);
            assertThat(prompt.getAvgQuality()).isGreaterThanOrEqualTo(0.0);
        });
    }
    
    @Test
    @DisplayName("Should handle business logic methods correctly")
    void shouldHandleBusinessLogicMethods() {
        // Given: Sessions in different states
        var testSession = testDataFactory.discoverySessionBuilder()
            .status(SessionStatus.RUNNING)
            .build();
        
        // When/Then: Business methods should work correctly
        assertAll("Business logic methods",
            () -> assertThat(completedSession.isCompleted()).isTrue(),
            () -> assertThat(completedSession.hasFailed()).isFalse(),
            () -> assertThat(completedSession.isRunning()).isFalse(),
            
            () -> assertThat(runningSession.isCompleted()).isFalse(),
            () -> assertThat(runningSession.hasFailed()).isFalse(),
            () -> assertThat(runningSession.isRunning()).isTrue(),
            
            () -> assertThat(failedSession.isCompleted()).isFalse(),
            () -> assertThat(failedSession.hasFailed()).isTrue(),
            () -> assertThat(failedSession.isRunning()).isFalse()
        );
        
        // When: Marking session as completed
        testSession.markCompleted();
        
        // Then: Status and completion time should be updated
        assertAll("Mark completed",
            () -> assertThat(testSession.getStatus()).isEqualTo(SessionStatus.COMPLETED),
            () -> assertThat(testSession.getCompletedAt()).isNotNull()
        );
        
        // When: Marking session as failed
        var anotherTestSession = testDataFactory.discoverySessionBuilder()
            .status(SessionStatus.RUNNING)
            .startedAt(LocalDateTime.now().minusMinutes(10))
            .build();
        
        anotherTestSession.markFailed();
        
        // Then: Status and completion time should be updated with duration
        assertAll("Mark failed",
            () -> assertThat(anotherTestSession.getStatus()).isEqualTo(SessionStatus.FAILED),
            () -> assertThat(anotherTestSession.getCompletedAt()).isNotNull(),
            () -> assertThat(anotherTestSession.getDurationMinutes()).isGreaterThan(0)
        );
    }
}
