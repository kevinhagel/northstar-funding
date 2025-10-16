package com.northstar.funding.discovery.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.DiscoverySession;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.domain.SessionStatus;
import com.northstar.funding.discovery.domain.SessionType;
import com.northstar.funding.discovery.infrastructure.DiscoverySessionRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Layer Test: DiscoveryOrchestrationService
 *
 * Tests workflow coordination for discovery sessions, candidate creation, and deduplication
 * Mocks all repository dependencies for isolated unit testing
 *
 * TDD: This test MUST FAIL until DiscoveryOrchestrationService is implemented
 */
@ExtendWith(MockitoExtension.class)
class DiscoveryOrchestrationServiceTest {

    @Mock
    private DiscoverySessionRepository sessionRepository;

    @Mock
    private FundingSourceCandidateRepository candidateRepository;

    private DiscoveryOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new DiscoveryOrchestrationService(sessionRepository, candidateRepository);
    }

    @Test
    void shouldTriggerDiscoverySession() {
        // Given
        UUID sessionId = UUID.randomUUID();
        List<String> searchEngines = List.of("searxng", "tavily");

        DiscoverySession session = DiscoverySession.builder()
                .sessionId(sessionId)
                .sessionType(SessionType.MANUAL)
                .status(SessionStatus.RUNNING)
                .executedAt(LocalDateTime.now())
                .searchEnginesUsed(java.util.Set.copyOf(searchEngines))
                .build();

        when(sessionRepository.save(any(DiscoverySession.class))).thenReturn(session);

        // When
        DiscoverySession triggered = service.triggerDiscovery(searchEngines, List.of());

        // Then
        assertThat(triggered).isNotNull();
        assertThat(triggered.getStatus()).isEqualTo(SessionStatus.RUNNING);
        verify(sessionRepository).save(argThat(s ->
            s.getSessionType() == SessionType.MANUAL &&
            s.getStatus() == SessionStatus.RUNNING
        ));
    }

    @Test
    void shouldProcessDiscoveryResults() {
        // Given
        UUID sessionId = UUID.randomUUID();
        DiscoverySession session = DiscoverySession.builder()
                .sessionId(sessionId)
                .sessionType(SessionType.MANUAL)
                .status(SessionStatus.RUNNING)
                .executedAt(LocalDateTime.now())
                .build();

        List<FundingSourceCandidate> discoveredCandidates = List.of(
                createCandidate("Foundation A", "Program 1", 0.85),
                createCandidate("Foundation B", "Program 2", 0.75)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
        when(candidateRepository.save(any(FundingSourceCandidate.class))).thenAnswer(i -> i.getArgument(0));
        when(sessionRepository.save(any(DiscoverySession.class))).thenReturn(session);

        // When
        service.processDiscoveryResults(sessionId, discoveredCandidates);

        // Then
        verify(candidateRepository, times(2)).save(any(FundingSourceCandidate.class));
        verify(sessionRepository).save(argThat(s ->
            s.getStatus() == SessionStatus.COMPLETED &&
            s.getCandidatesFound() == 2
        ));
    }

    @Test
    void shouldDeduplicateCandidates() {
        // Given
        String orgName = "Existing Foundation";
        String programName = "Existing Program";

        FundingSourceCandidate existingCandidate = createCandidate(orgName, programName, 0.8);
        existingCandidate.setCandidateId(UUID.randomUUID());
        existingCandidate.setStatus(CandidateStatus.APPROVED);

        FundingSourceCandidate duplicateCandidate = createCandidate(orgName, programName, 0.9);

        when(candidateRepository.findDuplicatesByOrganizationNameAndProgramName(orgName, programName))
                .thenReturn(List.of(existingCandidate));

        // When
        boolean isDuplicate = service.checkAndMarkDuplicate(duplicateCandidate);

        // Then
        assertThat(isDuplicate).isTrue();
        assertThat(duplicateCandidate.getDuplicateOfCandidateId()).isEqualTo(existingCandidate.getCandidateId());
    }

    @Test
    void shouldNotMarkNonDuplicates() {
        // Given
        FundingSourceCandidate uniqueCandidate = createCandidate("Unique Foundation", "Unique Program", 0.85);

        when(candidateRepository.findDuplicatesByOrganizationNameAndProgramName(anyString(), anyString()))
                .thenReturn(List.of());

        // When
        boolean isDuplicate = service.checkAndMarkDuplicate(uniqueCandidate);

        // Then
        assertThat(isDuplicate).isFalse();
        assertThat(uniqueCandidate.getDuplicateOfCandidateId()).isNull();
    }

    @Test
    void shouldCalculateSessionMetrics() {
        // Given
        UUID sessionId = UUID.randomUUID();
        List<FundingSourceCandidate> candidates = List.of(
                createCandidate("Org1", "Prog1", 0.9),
                createCandidate("Org2", "Prog2", 0.7),
                createCandidate("Org3", "Prog3", 0.8)
        );

        // When
        double avgConfidence = service.calculateAverageConfidence(candidates);

        // Then
        assertThat(avgConfidence).isEqualTo(0.8, within(0.01));
    }

    @Test
    void shouldGetPendingCandidatesForReview() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<FundingSourceCandidate> pendingCandidates = List.of(
                createCandidate("Org1", "Prog1", 0.9),
                createCandidate("Org2", "Prog2", 0.85)
        );
        Page<FundingSourceCandidate> page = new PageImpl<>(pendingCandidates, pageable, 2);

        when(candidateRepository.findByStatusOrderByConfidenceScoreDesc(
                eq(CandidateStatus.PENDING_REVIEW),
                any(Pageable.class)
        )).thenReturn(page);

        // When
        Page<FundingSourceCandidate> result = service.getPendingCandidates(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getConfidenceScore()).isGreaterThanOrEqualTo(
                result.getContent().get(1).getConfidenceScore()
        );
    }

    @Test
    void shouldHandleDiscoverySessionFailure() {
        // Given
        UUID sessionId = UUID.randomUUID();
        DiscoverySession session = DiscoverySession.builder()
                .sessionId(sessionId)
                .sessionType(SessionType.MANUAL)
                .status(SessionStatus.RUNNING)
                .executedAt(LocalDateTime.now())
                .build();

        String errorMessage = "Search engine timeout";

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
        when(sessionRepository.save(any(DiscoverySession.class))).thenReturn(session);

        // When
        service.markSessionFailed(sessionId, errorMessage);

        // Then
        verify(sessionRepository).save(argThat(s ->
            s.getStatus() == SessionStatus.FAILED &&
            s.getErrorMessages().contains(errorMessage)
        ));
    }

    @Test
    void shouldGetRecentDiscoverySessions() {
        // Given
        List<DiscoverySession> recentSessions = List.of(
                createSession(SessionType.SCHEDULED, SessionStatus.COMPLETED),
                createSession(SessionType.MANUAL, SessionStatus.COMPLETED)
        );

        when(sessionRepository.findTop10ByOrderByExecutedAtDesc()).thenReturn(recentSessions);

        // When
        List<DiscoverySession> recent = service.getRecentSessions();

        // Then
        assertThat(recent).hasSize(2);
        verify(sessionRepository).findTop10ByOrderByExecutedAtDesc();
    }

    @Test
    void shouldGetDiscoveryMetrics() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Double averageCandidates = 150.0;

        when(sessionRepository.getAverageDiscoveryMetrics(any(LocalDateTime.class)))
                .thenReturn(averageCandidates);

        // When
        Double metrics = service.getAverageDiscoveryMetrics(since);

        // Then
        assertThat(metrics).isEqualTo(150.0);
        verify(sessionRepository).getAverageDiscoveryMetrics(any(LocalDateTime.class));
    }

    @Test
    void shouldFilterOutLowConfidenceCandidates() {
        // Given
        UUID sessionId = UUID.randomUUID();
        DiscoverySession session = DiscoverySession.builder()
                .sessionId(sessionId)
                .sessionType(SessionType.MANUAL)
                .status(SessionStatus.RUNNING)
                .executedAt(LocalDateTime.now())
                .build();

        List<FundingSourceCandidate> mixedQualityCandidates = List.of(
                createCandidate("High Quality", "Program 1", 0.9),
                createCandidate("Low Quality", "Program 2", 0.3),  // Below threshold
                createCandidate("Medium Quality", "Program 3", 0.75)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
        when(candidateRepository.save(any(FundingSourceCandidate.class))).thenAnswer(i -> i.getArgument(0));
        when(sessionRepository.save(any(DiscoverySession.class))).thenReturn(session);

        // When
        service.processDiscoveryResults(sessionId, mixedQualityCandidates);

        // Then
        // Only candidates with confidence >= 0.5 should be saved
        verify(candidateRepository, times(2)).save(argThat(c ->
            c.getConfidenceScore() >= 0.5
        ));
    }

    @Test
    void shouldCalculateSessionDuration() {
        // Given
        UUID sessionId = UUID.randomUUID();
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(45);
        DiscoverySession session = DiscoverySession.builder()
                .sessionId(sessionId)
                .sessionType(SessionType.MANUAL)
                .status(SessionStatus.RUNNING)
                .executedAt(startTime)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
        when(sessionRepository.save(any(DiscoverySession.class))).thenReturn(session);

        // When
        service.completeSession(sessionId);

        // Then
        verify(sessionRepository).save(argThat(s ->
            s.getDurationMinutes() != null &&
            s.getDurationMinutes() > 0 &&
            s.getStatus() == SessionStatus.COMPLETED
        ));
    }

    private FundingSourceCandidate createCandidate(String orgName, String programName, double confidence) {
        return FundingSourceCandidate.builder()
                .candidateId(UUID.randomUUID())
                .organizationName(orgName)
                .programName(programName)
                .sourceUrl("https://example.com/" + orgName.toLowerCase().replace(" ", "-"))
                .confidenceScore(confidence)
                .status(CandidateStatus.PENDING_REVIEW)
                .discoveredAt(LocalDateTime.now())
                .build();
    }

    private DiscoverySession createSession(SessionType type, SessionStatus status) {
        return DiscoverySession.builder()
                .sessionId(UUID.randomUUID())
                .sessionType(type)
                .status(status)
                .executedAt(LocalDateTime.now())
                .candidatesFound(100)
                .averageConfidenceScore(0.75)
                .build();
    }
}
