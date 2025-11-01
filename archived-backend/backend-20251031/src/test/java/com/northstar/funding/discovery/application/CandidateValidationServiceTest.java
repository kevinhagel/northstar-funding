package com.northstar.funding.discovery.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.northstar.funding.discovery.domain.AdminUser;
import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.AdminUserRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service Layer Test: CandidateValidationService
 *
 * Tests business logic for candidate validation, assignment, approval, and rejection
 * Mocks all repository dependencies for isolated unit testing
 *
 * TDD: This test MUST FAIL until CandidateValidationService is implemented
 */
@ExtendWith(MockitoExtension.class)
class CandidateValidationServiceTest {

    @Mock
    private FundingSourceCandidateRepository candidateRepository;

    @Mock
    private AdminUserRepository adminUserRepository;

    private CandidateValidationService service;

    @BeforeEach
    void setUp() {
        service = new CandidateValidationService(candidateRepository, adminUserRepository);
    }

    @Test
    void shouldValidateCandidateBeforeApproval() {
        // Given
        UUID candidateId = UUID.randomUUID();
        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .status(CandidateStatus.IN_REVIEW)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.85"))
                .discoveredAt(LocalDateTime.now())
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(FundingSourceCandidate.class))).thenReturn(candidate);

        // When
        FundingSourceCandidate approved = service.approveCandidate(candidateId, UUID.randomUUID(), "Good funding source");

        // Then
        assertThat(approved).isNotNull();
        verify(candidateRepository).save(argThat(c ->
            c.getStatus() == CandidateStatus.APPROVED
        ));
    }

    @Test
    void shouldPreventApprovingAlreadyApprovedCandidate() {
        // Given
        UUID candidateId = UUID.randomUUID();
        FundingSourceCandidate alreadyApproved = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .status(CandidateStatus.APPROVED)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.85"))
                .discoveredAt(LocalDateTime.now())
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(alreadyApproved));

        // When/Then
        assertThatThrownBy(() -> service.approveCandidate(candidateId, UUID.randomUUID(), "Trying to approve again"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already approved");
    }

    @Test
    void shouldRejectCandidateWithReason() {
        // Given
        UUID candidateId = UUID.randomUUID();
        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .status(CandidateStatus.PENDING_REVIEW)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.35"))
                .discoveredAt(LocalDateTime.now())
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(candidateRepository.save(any(FundingSourceCandidate.class))).thenReturn(candidate);

        // When
        FundingSourceCandidate rejected = service.rejectCandidate(candidateId, UUID.randomUUID(), "Low confidence score");

        // Then
        assertThat(rejected).isNotNull();
        verify(candidateRepository).save(argThat(c ->
            c.getStatus() == CandidateStatus.REJECTED &&
            c.getRejectionReason().equals("Low confidence score")
        ));
    }

    @Test
    void shouldAssignCandidateToReviewer() {
        // Given
        UUID candidateId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .status(CandidateStatus.PENDING_REVIEW)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.75"))
                .discoveredAt(LocalDateTime.now())
                .build();

        AdminUser reviewer = AdminUser.builder()
                .userId(reviewerId)
                .username("reviewer1")
                .isActive(true)
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(adminUserRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(candidateRepository.save(any(FundingSourceCandidate.class))).thenReturn(candidate);

        // When
        FundingSourceCandidate assigned = service.assignToReviewer(candidateId, reviewerId);

        // Then
        assertThat(assigned).isNotNull();
        verify(candidateRepository).save(argThat(c ->
            c.getAssignedReviewerId().equals(reviewerId) &&
            c.getStatus() == CandidateStatus.IN_REVIEW &&
            c.getReviewStartedAt() != null
        ));
    }

    @Test
    void shouldPreventAssigningToInactiveReviewer() {
        // Given
        UUID candidateId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .status(CandidateStatus.PENDING_REVIEW)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.75"))
                .discoveredAt(LocalDateTime.now())
                .build();

        AdminUser inactiveReviewer = AdminUser.builder()
                .userId(reviewerId)
                .username("inactive_reviewer")
                .isActive(false)
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(adminUserRepository.findById(reviewerId)).thenReturn(Optional.of(inactiveReviewer));

        // When/Then
        assertThatThrownBy(() -> service.assignToReviewer(candidateId, reviewerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void shouldThrowExceptionWhenCandidateNotFound() {
        // Given
        UUID nonexistentId = UUID.randomUUID();
        when(candidateRepository.findById(nonexistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.approveCandidate(nonexistentId, UUID.randomUUID(), "Notes"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldValidateMinimumConfidenceScoreForApproval() {
        // Given
        UUID candidateId = UUID.randomUUID();
        FundingSourceCandidate lowConfidenceCandidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .status(CandidateStatus.IN_REVIEW)
                .organizationName("Test Foundation")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.4")) // Below threshold
                .discoveredAt(LocalDateTime.now())
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(lowConfidenceCandidate));

        // When/Then
        assertThatThrownBy(() -> service.approveCandidate(candidateId, UUID.randomUUID(), "Approving low confidence"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confidence score");
    }

    @Test
    void shouldEnforceRequiredFieldsForApproval() {
        // Given
        UUID candidateId = UUID.randomUUID();
        FundingSourceCandidate incompleteCandidate = FundingSourceCandidate.builder()
                .candidateId(candidateId)
                .status(CandidateStatus.IN_REVIEW)
                .organizationName("Test Foundation")
                .programName(null) // Missing required field
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.85"))
                .discoveredAt(LocalDateTime.now())
                .build();

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(incompleteCandidate));

        // When/Then
        assertThatThrownBy(() -> service.approveCandidate(candidateId, UUID.randomUUID(), "Approving incomplete"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("required fields");
    }

    @Test
    void shouldDetectDuplicateCandidates() {
        // Given
        String orgName = "Test Foundation";
        String programName = "Test Program";

        FundingSourceCandidate existing = FundingSourceCandidate.builder()
                .candidateId(UUID.randomUUID())
                .status(CandidateStatus.APPROVED)
                .organizationName(orgName)
                .programName(programName)
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.85"))
                .discoveredAt(LocalDateTime.now())
                .build();

        when(candidateRepository.findDuplicatesByOrganizationNameAndProgramName(orgName, programName))
                .thenReturn(List.of(existing));

        // When
        boolean isDuplicate = service.isDuplicate(orgName, programName);

        // Then
        assertThat(isDuplicate).isTrue();
        verify(candidateRepository).findDuplicatesByOrganizationNameAndProgramName(orgName, programName);
    }

    @Test
    void shouldCalculateReviewMetrics() {
        // Given
        UUID reviewerId = UUID.randomUUID();
        List<FundingSourceCandidate> reviewedCandidates = List.of(
                createCandidate(CandidateStatus.APPROVED),
                createCandidate(CandidateStatus.APPROVED),
                createCandidate(CandidateStatus.REJECTED)
        );

        // When
        double approvalRate = service.calculateApprovalRate(reviewedCandidates);

        // Then
        assertThat(approvalRate).isEqualTo(0.666, within(0.01));
    }

    private FundingSourceCandidate createCandidate(CandidateStatus status) {
        return FundingSourceCandidate.builder()
                .candidateId(UUID.randomUUID())
                .status(status)
                .organizationName("Test Org")
                .programName("Test Program")
                .sourceUrl("https://example.com")
                .confidenceScore(new BigDecimal("0.8"))
                .discoveredAt(LocalDateTime.now())
                .build();
    }
}
