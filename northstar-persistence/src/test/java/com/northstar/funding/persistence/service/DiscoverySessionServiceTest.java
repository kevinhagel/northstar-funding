package com.northstar.funding.persistence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.domain.SessionType;
import com.northstar.funding.persistence.repository.DiscoverySessionRepository;

/**
 * Unit tests for DiscoverySessionService using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class DiscoverySessionServiceTest {

    @Mock
    private DiscoverySessionRepository discoverySessionRepository;

    @InjectMocks
    private DiscoverySessionService discoverySessionService;

    private DiscoverySession testSession;

    @BeforeEach
    void setUp() {
        testSession = DiscoverySession.builder()
            .sessionId(UUID.randomUUID())
            .sessionType(SessionType.SCHEDULED)
            .status(SessionStatus.RUNNING)
            .executedBy("SYSTEM")
            .startedAt(LocalDateTime.now())
            .searchEnginesUsed(Set.of("SEARXNG", "PERPLEXICA"))
            .searchQueries(List.of("funding opportunities"))
            .candidatesFound(0)
            .duplicatesDetected(0)
            .sourcesScraped(0)
            .build();
    }

    // ============================================================================
    // CREATE & UPDATE Operations Tests
    // ============================================================================

    @Test
    void createSession_ShouldCreateSession() {
        // Given
        when(discoverySessionRepository.save(any(DiscoverySession.class)))
            .thenReturn(testSession);

        // When
        DiscoverySession result = discoverySessionService.createSession(testSession);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testSession);
        verify(discoverySessionRepository).save(testSession);
    }

    @Test
    void updateStatus_WhenSessionExists_ShouldUpdateStatus() {
        // Given
        UUID sessionId = testSession.getSessionId();
        when(discoverySessionRepository.findById(sessionId))
            .thenReturn(Optional.of(testSession));
        when(discoverySessionRepository.save(any(DiscoverySession.class)))
            .thenReturn(testSession);

        // When
        DiscoverySession result = discoverySessionService.updateStatus(
            sessionId, SessionStatus.COMPLETED);

        // Then
        assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(discoverySessionRepository).save(testSession);
    }

    @Test
    void updateStatus_WhenSessionNotFound_ShouldThrowException() {
        // Given
        UUID sessionId = UUID.randomUUID();
        when(discoverySessionRepository.findById(sessionId))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> discoverySessionService.updateStatus(
            sessionId, SessionStatus.COMPLETED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Session not found");
        verify(discoverySessionRepository, never()).save(any(DiscoverySession.class));
    }

    @Test
    void completeSession_WhenSessionExists_ShouldCompleteSession() {
        // Given
        UUID sessionId = testSession.getSessionId();
        when(discoverySessionRepository.findById(sessionId))
            .thenReturn(Optional.of(testSession));
        when(discoverySessionRepository.save(any(DiscoverySession.class)))
            .thenReturn(testSession);

        // When
        DiscoverySession result = discoverySessionService.completeSession(
            sessionId, 25, 5, 20);

        // Then
        assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getCandidatesFound()).isEqualTo(25);
        assertThat(result.getDuplicatesDetected()).isEqualTo(5);
        assertThat(result.getSourcesScraped()).isEqualTo(20);
        assertThat(result.getDurationMinutes()).isNotNull();
        verify(discoverySessionRepository).save(testSession);
    }

    @Test
    void failSession_WhenSessionExists_ShouldFailSession() {
        // Given
        UUID sessionId = testSession.getSessionId();
        List<String> errors = List.of("Error 1", "Error 2");
        when(discoverySessionRepository.findById(sessionId))
            .thenReturn(Optional.of(testSession));
        when(discoverySessionRepository.save(any(DiscoverySession.class)))
            .thenReturn(testSession);

        // When
        DiscoverySession result = discoverySessionService.failSession(sessionId, errors);

        // Then
        assertThat(result.getStatus()).isEqualTo(SessionStatus.FAILED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getErrorMessages()).containsExactlyElementsOf(errors);
        verify(discoverySessionRepository).save(testSession);
    }

    @Test
    void updateStatistics_WhenSessionExists_ShouldUpdateStatistics() {
        // Given
        UUID sessionId = testSession.getSessionId();
        when(discoverySessionRepository.findById(sessionId))
            .thenReturn(Optional.of(testSession));
        when(discoverySessionRepository.save(any(DiscoverySession.class)))
            .thenReturn(testSession);

        java.math.BigDecimal confidence = new java.math.BigDecimal("0.85");

        // When
        DiscoverySession result = discoverySessionService.updateStatistics(
            sessionId, confidence);

        // Then
        assertThat(result.getAverageConfidenceScore()).isEqualTo(confidence);
        verify(discoverySessionRepository).save(testSession);
    }

    // ============================================================================
    // READ Operations Tests
    // ============================================================================

    @Test
    void findById_WhenExists_ShouldReturnSession() {
        // Given
        UUID sessionId = testSession.getSessionId();
        when(discoverySessionRepository.findById(sessionId))
            .thenReturn(Optional.of(testSession));

        // When
        Optional<DiscoverySession> result = discoverySessionService.findById(sessionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSession);
    }

    @Test
    void getRecentSessions_ShouldReturnRecentSessions() {
        // Given
        int limit = 10;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findRecentSessions(PageRequest.of(0, limit)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getRecentSessions(limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getTop10RecentSessions_ShouldReturnTop10() {
        // Given
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findTop10ByOrderByExecutedAtDesc())
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getTop10RecentSessions();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getSessionsByStatus_ShouldReturnStatusSessions() {
        // Given
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findByStatus(SessionStatus.RUNNING))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getSessionsByStatus(
            SessionStatus.RUNNING);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getSessionsByType_ShouldReturnTypeSessions() {
        // Given
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findBySessionType(SessionType.SCHEDULED))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getSessionsByType(
            SessionType.SCHEDULED);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getSessionsByDateRange_ShouldReturnRangeSessions() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findByExecutedAtBetween(start, end))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getSessionsByDateRange(
            start, end);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getFailedSessions_ShouldReturnFailedSessions() {
        // Given
        int limit = 10;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findFailedSessions(PageRequest.of(0, limit)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getFailedSessions(limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getLongRunningSessions_ShouldReturnLongRunningSessions() {
        // Given
        int thresholdHours = 2;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findLongRunningSessions(any(LocalDateTime.class)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getLongRunningSessions(
            thresholdHours);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getPerformanceMetrics_ShouldReturnMetrics() {
        // Given
        int daysBack = 30;
        DiscoverySessionRepository.DiscoveryMetrics metrics =
            new DiscoverySessionRepository.DiscoveryMetrics(25.5, 15.2, 0.82, 10L, 2L);
        when(discoverySessionRepository.getPerformanceMetrics(any(LocalDateTime.class)))
            .thenReturn(metrics);

        // When
        DiscoverySessionRepository.DiscoveryMetrics result =
            discoverySessionService.getPerformanceMetrics(daysBack);

        // Then
        assertThat(result).isEqualTo(metrics);
        assertThat(result.avgCandidatesFound()).isEqualTo(25.5);
        assertThat(result.successfulSessions()).isEqualTo(10L);
    }

    @Test
    void getAverageDiscoveryMetrics_ShouldReturnAverage() {
        // Given
        int daysBack = 30;
        when(discoverySessionRepository.getAverageDiscoveryMetrics(any(LocalDateTime.class)))
            .thenReturn(22.5);

        // When
        Double result = discoverySessionService.getAverageDiscoveryMetrics(daysBack);

        // Then
        assertThat(result).isEqualTo(22.5);
    }

    @Test
    void getSessionsByTypeAndStatus_ShouldReturnFilteredSessions() {
        // Given
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findBySessionTypeAndStatusOrderByExecutedAtDesc(
            SessionType.SCHEDULED, SessionStatus.RUNNING))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getSessionsByTypeAndStatus(
            SessionType.SCHEDULED, SessionStatus.RUNNING);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getHighPerformingSessions_ShouldReturnHighPerformers() {
        // Given
        int minCandidates = 20;
        double minConfidence = 0.80;
        int limit = 10;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findHighPerformingSessions(
            minCandidates, minConfidence, PageRequest.of(0, limit)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getHighPerformingSessions(
            minCandidates, minConfidence, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getSessionsWithSearchEngineFailures_ShouldReturnFailureSessions() {
        // Given
        int limit = 10;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findSessionsWithSearchEngineFailures(
            PageRequest.of(0, limit)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result =
            discoverySessionService.getSessionsWithSearchEngineFailures(limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getDailyTrends_ShouldReturnTrends() {
        // Given
        int daysBack = 7;
        List<DiscoverySessionRepository.DailyDiscoveryTrends> trends = List.of(
            new DiscoverySessionRepository.DailyDiscoveryTrends(
                java.sql.Date.valueOf("2025-01-01"), 5L, 20.5, 12.3, 4L)
        );
        when(discoverySessionRepository.getDailyTrends(any(LocalDateTime.class)))
            .thenReturn(trends);

        // When
        List<DiscoverySessionRepository.DailyDiscoveryTrends> result =
            discoverySessionService.getDailyTrends(daysBack);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalSessions()).isEqualTo(5L);
    }

    @Test
    void getSessionsByLlmModel_ShouldReturnModelSessions() {
        // Given
        String modelName = "qwen2.5-coder:32b";
        int limit = 10;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findByLlmModel(modelName, PageRequest.of(0, limit)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getSessionsByLlmModel(
            modelName, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getSessionsBySearchEngine_ShouldReturnEngineSessions() {
        // Given
        String searchEngine = "SEARXNG";
        int limit = 10;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findBySearchEngine(
            searchEngine, PageRequest.of(0, limit)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getSessionsBySearchEngine(
            searchEngine, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getSearchEngineStats_ShouldReturnStats() {
        // Given
        int daysBack = 30;
        List<DiscoverySessionRepository.SearchEngineStats> stats = List.of(
            new DiscoverySessionRepository.SearchEngineStats("SEARXNG,PERPLEXICA", 10L, 25.5, 1L)
        );
        when(discoverySessionRepository.getSearchEngineStats(any(LocalDateTime.class)))
            .thenReturn(stats);

        // When
        List<DiscoverySessionRepository.SearchEngineStats> result =
            discoverySessionService.getSearchEngineStats(daysBack);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).usageCount()).isEqualTo(10L);
    }

    @Test
    void getDuplicationStats_ShouldReturnStats() {
        // Given
        int daysBack = 30;
        DiscoverySessionRepository.DuplicationStats stats =
            new DiscoverySessionRepository.DuplicationStats(0.45, 100L, 220L);
        when(discoverySessionRepository.getDuplicationStats(any(LocalDateTime.class)))
            .thenReturn(stats);

        // When
        DiscoverySessionRepository.DuplicationStats result =
            discoverySessionService.getDuplicationStats(daysBack);

        // Then
        assertThat(result).isEqualTo(stats);
        assertThat(result.avgDuplicateRate()).isEqualTo(0.45);
    }

    @Test
    void getSessionsEligibleForRetry_ShouldReturnRetrySessions() {
        // Given
        int minAgeDays = 1;
        List<DiscoverySession> sessions = List.of(testSession);
        when(discoverySessionRepository.findSessionsEligibleForRetry(any(LocalDateTime.class)))
            .thenReturn(sessions);

        // When
        List<DiscoverySession> result = discoverySessionService.getSessionsEligibleForRetry(
            minAgeDays);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSession);
    }

    @Test
    void getPromptEffectivenessAnalysis_ShouldReturnAnalysis() {
        // Given
        int daysBack = 30;
        List<DiscoverySessionRepository.PromptEffectiveness> analysis = List.of(
            new DiscoverySessionRepository.PromptEffectiveness(
                "Test prompt", 28.5, 15L, 0.82)
        );
        when(discoverySessionRepository.getPromptEffectivenessAnalysis(
            any(LocalDateTime.class)))
            .thenReturn(analysis);

        // When
        List<DiscoverySessionRepository.PromptEffectiveness> result =
            discoverySessionService.getPromptEffectivenessAnalysis(daysBack);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).avgEffectiveness()).isEqualTo(28.5);
    }
}
