package com.northstar.funding.discovery.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.northstar.funding.discovery.domain.AdminUser;
import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.AdminUserRepository;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

/**
 * Service Layer: CandidateValidationService
 *
 * Handles business logic for candidate validation, approval, rejection, and assignment
 * Enforces business rules and coordinates repository operations
 */
@Service
@Transactional
public class CandidateValidationService {

    private static final BigDecimal MINIMUM_CONFIDENCE_THRESHOLD = new BigDecimal("0.50");

    private final FundingSourceCandidateRepository candidateRepository;
    private final AdminUserRepository adminUserRepository;

    public CandidateValidationService(
            FundingSourceCandidateRepository candidateRepository,
            AdminUserRepository adminUserRepository) {
        this.candidateRepository = candidateRepository;
        this.adminUserRepository = adminUserRepository;
    }

    /**
     * Approve a candidate for inclusion in the knowledge base
     * Validates business rules before approval
     */
    public FundingSourceCandidate approveCandidate(UUID candidateId, UUID adminUserId, String approvalNotes) {
        FundingSourceCandidate candidate = findCandidateOrThrow(candidateId);

        // Business rule: Cannot approve already approved candidates
        if (candidate.getStatus() == CandidateStatus.APPROVED) {
            throw new IllegalStateException("Candidate " + candidateId + " is already approved");
        }

        // Business rule: Minimum confidence score for approval
        if (candidate.getConfidenceScore().compareTo(MINIMUM_CONFIDENCE_THRESHOLD) < 0) {
            throw new IllegalStateException(
                "Cannot approve candidate with confidence score " + candidate.getConfidenceScore() +
                " (minimum: " + MINIMUM_CONFIDENCE_THRESHOLD + ")"
            );
        }

        // Business rule: Required fields must be present
        validateRequiredFields(candidate);

        // Update candidate status with audit trail
        candidate.setStatus(CandidateStatus.APPROVED);
        candidate.setApprovedBy(adminUserId);
        candidate.setApprovedAt(LocalDateTime.now());
        candidate.setValidationNotes(approvalNotes);
        candidate.setLastUpdatedAt(LocalDateTime.now());

        return candidateRepository.save(candidate);
    }

    /**
     * Reject a candidate from the review queue
     */
    public FundingSourceCandidate rejectCandidate(UUID candidateId, UUID adminUserId, String rejectionReason) {
        FundingSourceCandidate candidate = findCandidateOrThrow(candidateId);

        // Business rule: Cannot reject already rejected candidates
        if (candidate.getStatus() == CandidateStatus.REJECTED) {
            throw new IllegalStateException("Candidate " + candidateId + " is already rejected");
        }

        // Update candidate status with audit trail
        candidate.setStatus(CandidateStatus.REJECTED);
        candidate.setRejectedBy(adminUserId);
        candidate.setRejectedAt(LocalDateTime.now());
        candidate.setRejectionReason(rejectionReason);
        candidate.setLastUpdatedAt(LocalDateTime.now());

        return candidateRepository.save(candidate);
    }

    /**
     * Assign candidate to a reviewer
     * Validates reviewer is active before assignment
     */
    public FundingSourceCandidate assignToReviewer(UUID candidateId, UUID reviewerId) {
        FundingSourceCandidate candidate = findCandidateOrThrow(candidateId);
        AdminUser reviewer = adminUserRepository.findById(reviewerId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found: " + reviewerId));

        // Business rule: Cannot assign to inactive reviewer
        if (!reviewer.getIsActive()) {
            throw new IllegalArgumentException(
                "Cannot assign to inactive reviewer: " + reviewer.getUsername()
            );
        }

        candidate.setAssignedReviewerId(reviewerId);
        candidate.setStatus(CandidateStatus.IN_REVIEW);
        candidate.setReviewStartedAt(LocalDateTime.now());
        candidate.setLastUpdatedAt(LocalDateTime.now());

        return candidateRepository.save(candidate);
    }

    /**
     * Check if candidate is duplicate
     */
    public boolean isDuplicate(String organizationName, String programName) {
        List<FundingSourceCandidate> duplicates = candidateRepository
                .findDuplicatesByOrganizationNameAndProgramName(organizationName, programName);
        return !duplicates.isEmpty();
    }

    /**
     * Calculate approval rate from list of reviewed candidates
     */
    public double calculateApprovalRate(List<FundingSourceCandidate> candidates) {
        if (candidates.isEmpty()) {
            return 0.0;
        }

        long approvedCount = candidates.stream()
                .filter(c -> c.getStatus() == CandidateStatus.APPROVED)
                .count();

        return (double) approvedCount / candidates.size();
    }

    /**
     * Validate required fields are present for approval
     */
    private void validateRequiredFields(FundingSourceCandidate candidate) {
        if (candidate.getProgramName() == null || candidate.getProgramName().isBlank()) {
            throw new IllegalStateException("Cannot approve candidate: required fields missing (programName)");
        }
        if (candidate.getOrganizationName() == null || candidate.getOrganizationName().isBlank()) {
            throw new IllegalStateException("Cannot approve candidate: required fields missing (organizationName)");
        }
        if (candidate.getSourceUrl() == null || candidate.getSourceUrl().isBlank()) {
            throw new IllegalStateException("Cannot approve candidate: required fields missing (sourceUrl)");
        }
    }

    /**
     * Find candidate or throw exception
     */
    private FundingSourceCandidate findCandidateOrThrow(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + candidateId));
    }
}
