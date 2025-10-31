package com.northstar.funding.discovery.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.domain.SessionType;
import com.northstar.funding.discovery.infrastructure.DiscoverySessionRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

/**
 * Service Layer: DiscoveryOrchestrationService
 *
 * Orchestrates discovery workflows, session management, and candidate processing
 * Coordinates multiple repositories and enforces discovery business rules
 */
@Service
@Transactional
public class DiscoveryOrchestrationService {

    private static final BigDecimal MINIMUM_CONFIDENCE_THRESHOLD = new BigDecimal("0.50");

    private final DiscoverySessionRepository sessionRepository;
    private final FundingSourceCandidateRepository candidateRepository;

    public DiscoveryOrchestrationService(
            DiscoverySessionRepository sessionRepository,
            FundingSourceCandidateRepository candidateRepository) {
        this.sessionRepository = sessionRepository;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Trigger a new discovery session
     * Creates session record and returns it in RUNNING status
     */
    public DiscoverySession triggerDiscovery(
            List<String> searchEngines,
            List<String> customQueries) {

        DiscoverySession session = DiscoverySession.builder()
                .sessionId(UUID.randomUUID())
                .sessionType(SessionType.MANUAL)
                .status(SessionStatus.RUNNING)
                .executedAt(LocalDateTime.now())
                .searchEnginesUsed(searchEngines != null ? java.util.Set.copyOf(searchEngines) : java.util.Set.of())
                .searchQueries(customQueries)
                .candidatesFound(0)
                .duplicatesDetected(0)
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Process discovery results from a session
     * Filters low-quality candidates and detects duplicates
     */
    public void processDiscoveryResults(UUID sessionId, List<FundingSourceCandidate> candidates) {
        DiscoverySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        int savedCount = 0;
        int duplicateCount = 0;

        for (FundingSourceCandidate candidate : candidates) {
            // Filter out low confidence candidates
            if (candidate.getConfidenceScore().compareTo(MINIMUM_CONFIDENCE_THRESHOLD) < 0) {
                continue;
            }

            // Check for duplicates
            if (checkAndMarkDuplicate(candidate)) {
                duplicateCount++;
                continue;
            }

            // Set discovery metadata
            candidate.setDiscoverySessionId(sessionId);
            candidate.setStatus(CandidateStatus.PENDING_REVIEW);
            candidate.setDiscoveredAt(LocalDateTime.now());

            candidateRepository.save(candidate);
            savedCount++;
        }

        // Update session statistics
        session.setCandidatesFound(savedCount);
        session.setDuplicatesDetected(duplicateCount);
        session.setStatus(SessionStatus.COMPLETED);
        session.setAverageConfidenceScore(calculateAverageConfidence(candidates));

        sessionRepository.save(session);
    }

    /**
     * Check if candidate is duplicate and mark it if so
     * Returns true if duplicate found
     */
    public boolean checkAndMarkDuplicate(FundingSourceCandidate candidate) {
        List<FundingSourceCandidate> duplicates = candidateRepository
                .findDuplicatesByOrganizationNameAndProgramName(
                    candidate.getOrganizationName(),
                    candidate.getProgramName()
                );

        if (!duplicates.isEmpty()) {
            // Mark as duplicate of the first approved candidate found
            FundingSourceCandidate master = duplicates.stream()
                    .filter(d -> d.getStatus() == CandidateStatus.APPROVED)
                    .findFirst()
                    .orElse(duplicates.get(0));

            candidate.setDuplicateOfCandidateId(master.getCandidateId());
            return true;
        }

        return false;
    }

    /**
     * Calculate average confidence score from candidates
     */
    public BigDecimal calculateAverageConfidence(List<FundingSourceCandidate> candidates) {
        if (candidates.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Calculate average using BigDecimal arithmetic
        BigDecimal sum = candidates.stream()
                .map(FundingSourceCandidate::getConfidenceScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(
            BigDecimal.valueOf(candidates.size()),
            2,
            RoundingMode.HALF_UP
        );
    }

    /**
     * Get pending candidates ordered by confidence score
     */
    public Page<FundingSourceCandidate> getPendingCandidates(Pageable pageable) {
        return candidateRepository.findByStatusOrderByConfidenceScoreDesc(
            CandidateStatus.PENDING_REVIEW,
            pageable
        );
    }

    /**
     * Mark discovery session as failed
     */
    public void markSessionFailed(UUID sessionId, String errorMessage) {
        DiscoverySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setStatus(SessionStatus.FAILED);
        session.setErrorMessages(List.of(errorMessage));

        sessionRepository.save(session);
    }

    /**
     * Complete a discovery session and calculate duration
     */
    public void completeSession(UUID sessionId) {
        DiscoverySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Calculate duration
        Duration duration = Duration.between(session.getExecutedAt(), LocalDateTime.now());
        session.setDurationMinutes((int) duration.toMinutes());
        session.setStatus(SessionStatus.COMPLETED);

        sessionRepository.save(session);
    }

    /**
     * Get recent discovery sessions
     */
    public List<DiscoverySession> getRecentSessions() {
        return sessionRepository.findTop10ByOrderByExecutedAtDesc();
    }

    /**
     * Get average discovery metrics since a date
     */
    public Double getAverageDiscoveryMetrics(LocalDateTime since) {
        return sessionRepository.getAverageDiscoveryMetrics(since);
    }
}
