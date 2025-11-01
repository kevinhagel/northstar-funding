package com.northstar.funding.persistence.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.domain.DiscoverySession;
import com.northstar.funding.domain.SessionStatus;
import com.northstar.funding.domain.SessionType;
import com.northstar.funding.persistence.repository.DiscoverySessionRepository;


/**
 * Service layer for DiscoverySession entity operations.
 *
 * Provides business logic and transaction management for discovery session
 * management, performance analytics, and error tracking.
 *
 * This is the public API for external modules to interact with DiscoverySession persistence.
 */
@Service
@Transactional
public class DiscoverySessionService {

    private final DiscoverySessionRepository discoverySessionRepository;

    public DiscoverySessionService(DiscoverySessionRepository discoverySessionRepository) {
        this.discoverySessionRepository = discoverySessionRepository;
    }

    // ============================================================================
    // CREATE & UPDATE Operations
    // ============================================================================

    /**
     * Create a new discovery session.
     *
     * @param session the discovery session to create
     * @return the created DiscoverySession
     */
    public DiscoverySession createSession(DiscoverySession session) {

        DiscoverySession saved = discoverySessionRepository.save(session);
        return saved;
    }

    /**
     * Update session status.
     *
     * @param sessionId the session ID
     * @param status the new status
     * @return updated DiscoverySession
     */
    public DiscoverySession updateStatus(UUID sessionId, SessionStatus status) {

        DiscoverySession session = discoverySessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setStatus(status);

        if (status == SessionStatus.COMPLETED || status == SessionStatus.FAILED) {
            session.setCompletedAt(LocalDateTime.now());
        }

        return discoverySessionRepository.save(session);
    }

    /**
     * Mark session as completed with statistics.
     *
     * @param sessionId the session ID
     * @param candidatesFound number of candidates found
     * @param duplicatesDetected number of duplicates detected
     * @param sourcesScraped number of sources scraped
     * @return updated DiscoverySession
     */
    public DiscoverySession completeSession(UUID sessionId, int candidatesFound,
                                            int duplicatesDetected, int sourcesScraped) {

        DiscoverySession session = discoverySessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setCandidatesFound(candidatesFound);
        session.setDuplicatesDetected(duplicatesDetected);
        session.setSourcesScraped(sourcesScraped);

        // Calculate duration
        if (session.getStartedAt() != null) {
            long durationMinutes = java.time.Duration.between(
                session.getStartedAt(),
                session.getCompletedAt()
            ).toMinutes();
            session.setDurationMinutes((int) durationMinutes);
        }

        return discoverySessionRepository.save(session);
    }

    /**
     * Mark session as failed with error messages.
     *
     * @param sessionId the session ID
     * @param errorMessages list of error messages
     * @return updated DiscoverySession
     */
    public DiscoverySession failSession(UUID sessionId, List<String> errorMessages) {

        DiscoverySession session = discoverySessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setStatus(SessionStatus.FAILED);
        session.setCompletedAt(LocalDateTime.now());
        session.setErrorMessages(errorMessages);

        return discoverySessionRepository.save(session);
    }

    /**
     * Update session statistics.
     *
     * @param sessionId the session ID
     * @param averageConfidence average confidence score
     * @return updated DiscoverySession
     */
    public DiscoverySession updateStatistics(UUID sessionId, java.math.BigDecimal averageConfidence) {

        DiscoverySession session = discoverySessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setAverageConfidenceScore(averageConfidence);

        return discoverySessionRepository.save(session);
    }

    // ============================================================================
    // READ Operations
    // ============================================================================

    /**
     * Find session by ID.
     *
     * @param sessionId the session ID
     * @return Optional of DiscoverySession
     */
    @Transactional(readOnly = true)
    public Optional<DiscoverySession> findById(UUID sessionId) {
        return discoverySessionRepository.findById(sessionId);
    }

    /**
     * Get recent sessions.
     *
     * @param limit max number of sessions
     * @return list of recent sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getRecentSessions(int limit) {
        return discoverySessionRepository.findRecentSessions(PageRequest.of(0, limit));
    }

    /**
     * Get top 10 most recent sessions.
     *
     * @return list of recent sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getTop10RecentSessions() {
        return discoverySessionRepository.findTop10ByOrderByExecutedAtDesc();
    }

    /**
     * Get sessions by status.
     *
     * @param status the session status
     * @return list of sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsByStatus(SessionStatus status) {
        return discoverySessionRepository.findByStatus(status);
    }

    /**
     * Get sessions by type.
     *
     * @param sessionType the session type
     * @return list of sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsByType(SessionType sessionType) {
        return discoverySessionRepository.findBySessionType(sessionType);
    }

    /**
     * Get sessions within date range.
     *
     * @param start start date/time
     * @param end end date/time
     * @return list of sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsByDateRange(LocalDateTime start, LocalDateTime end) {
        return discoverySessionRepository.findByExecutedAtBetween(start, end);
    }

    /**
     * Get failed sessions.
     *
     * @param limit max number of sessions
     * @return list of failed sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getFailedSessions(int limit) {
        return discoverySessionRepository.findFailedSessions(PageRequest.of(0, limit));
    }

    /**
     * Get long-running sessions.
     *
     * @param thresholdHours number of hours threshold
     * @return list of long-running sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getLongRunningSessions(int thresholdHours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(thresholdHours);
        return discoverySessionRepository.findLongRunningSessions(threshold);
    }

    /**
     * Get performance metrics.
     *
     * @param daysBack number of days to look back
     * @return performance metrics
     */
    @Transactional(readOnly = true)
    public DiscoverySessionRepository.DiscoveryMetrics getPerformanceMetrics(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return discoverySessionRepository.getPerformanceMetrics(since);
    }

    /**
     * Get average discovery metrics.
     *
     * @param daysBack number of days to look back
     * @return average candidates found
     */
    @Transactional(readOnly = true)
    public Double getAverageDiscoveryMetrics(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return discoverySessionRepository.getAverageDiscoveryMetrics(since);
    }

    /**
     * Get sessions by type and status.
     *
     * @param sessionType the session type
     * @param status the session status
     * @return list of sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsByTypeAndStatus(SessionType sessionType,
                                                             SessionStatus status) {
        return discoverySessionRepository.findBySessionTypeAndStatusOrderByExecutedAtDesc(
            sessionType, status);
    }

    /**
     * Get high-performing sessions.
     *
     * @param minCandidates minimum candidates found
     * @param minConfidence minimum confidence score
     * @param limit max number of sessions
     * @return list of high-performing sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getHighPerformingSessions(int minCandidates,
                                                            double minConfidence,
                                                            int limit) {
        return discoverySessionRepository.findHighPerformingSessions(
            minCandidates,
            minConfidence,
            PageRequest.of(0, limit)
        );
    }

    /**
     * Get sessions with search engine failures.
     *
     * @param limit max number of sessions
     * @return list of sessions with failures
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsWithSearchEngineFailures(int limit) {
        return discoverySessionRepository.findSessionsWithSearchEngineFailures(
            PageRequest.of(0, limit));
    }

    /**
     * Get daily discovery trends.
     *
     * @param daysBack number of days to look back
     * @return list of daily trends
     */
    @Transactional(readOnly = true)
    public List<DiscoverySessionRepository.DailyDiscoveryTrends> getDailyTrends(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return discoverySessionRepository.getDailyTrends(since);
    }

    /**
     * Get sessions by LLM model.
     *
     * @param modelName the LLM model name
     * @param limit max number of sessions
     * @return list of sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsByLlmModel(String modelName, int limit) {
        return discoverySessionRepository.findByLlmModel(modelName, PageRequest.of(0, limit));
    }

    /**
     * Get sessions by search engine.
     *
     * @param searchEngine the search engine name
     * @param limit max number of sessions
     * @return list of sessions
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsBySearchEngine(String searchEngine, int limit) {
        return discoverySessionRepository.findBySearchEngine(searchEngine, PageRequest.of(0, limit));
    }

    /**
     * Get search engine reliability statistics.
     *
     * @param daysBack number of days to look back
     * @return list of search engine stats
     */
    @Transactional(readOnly = true)
    public List<DiscoverySessionRepository.SearchEngineStats> getSearchEngineStats(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return discoverySessionRepository.getSearchEngineStats(since);
    }

    /**
     * Get duplication statistics.
     *
     * @param daysBack number of days to look back
     * @return duplication statistics
     */
    @Transactional(readOnly = true)
    public DiscoverySessionRepository.DuplicationStats getDuplicationStats(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return discoverySessionRepository.getDuplicationStats(since);
    }

    /**
     * Get sessions eligible for retry.
     *
     * @param minAgeDays minimum age in days
     * @return list of sessions eligible for retry
     */
    @Transactional(readOnly = true)
    public List<DiscoverySession> getSessionsEligibleForRetry(int minAgeDays) {
        LocalDateTime minAge = LocalDateTime.now().minusDays(minAgeDays);
        return discoverySessionRepository.findSessionsEligibleForRetry(minAge);
    }

    /**
     * Get prompt effectiveness analysis.
     *
     * @param daysBack number of days to look back
     * @return list of prompt effectiveness stats
     */
    @Transactional(readOnly = true)
    public List<DiscoverySessionRepository.PromptEffectiveness> getPromptEffectivenessAnalysis(
            int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return discoverySessionRepository.getPromptEffectivenessAnalysis(since);
    }
}
