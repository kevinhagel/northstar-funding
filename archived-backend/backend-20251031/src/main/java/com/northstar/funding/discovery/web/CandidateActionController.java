package com.northstar.funding.discovery.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.funding.discovery.application.CandidateValidationService;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;

/**
 * REST Controller: CandidateActionController
 *
 * Handles candidate actions: approve, reject, assign
 * Endpoints: POST /api/candidates/{id}/approve, /reject, /assign
 */
@RestController
@RequestMapping("/api/candidates")
public class CandidateActionController {

    private final CandidateValidationService validationService;

    public CandidateActionController(CandidateValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * POST /api/candidates/{candidateId}/approve - Approve candidate
     */
    @PostMapping("/{candidateId}/approve")
    public ResponseEntity<ActionResponse> approveCandidate(
            @PathVariable String candidateId,
            @RequestBody ApprovalRequest request) {

        // Validate request
        if (request.approvalNotes() == null || request.approvalNotes().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ActionResponse(false, "Approval notes are required", null));
        }

        if (request.approvalNotes().length() > 500) {
            return ResponseEntity.badRequest()
                    .body(new ActionResponse(false, "Approval notes too long (max 500 chars)", null));
        }

        try {
            UUID uuid = UUID.fromString(candidateId);
            // TODO: Get adminUserId from Spring Security context when authentication is implemented
            UUID adminUserId = UUID.randomUUID();
            FundingSourceCandidate approved = validationService.approveCandidate(uuid, adminUserId, request.approvalNotes());

            return ResponseEntity.ok(
                new ActionResponse(
                    true,
                    "Candidate approved successfully",
                    approved.getCandidateId()
                )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // Business rule violation (already approved, low confidence, etc.)
            return ResponseEntity.badRequest()
                    .body(new ActionResponse(false, e.getMessage(), null));
        }
    }

    /**
     * POST /api/candidates/{candidateId}/reject - Reject candidate
     */
    @PostMapping("/{candidateId}/reject")
    public ResponseEntity<ActionResponse> rejectCandidate(
            @PathVariable String candidateId,
            @RequestBody RejectionRequest request) {

        // Validate request
        if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ActionResponse(false, "Rejection reason is required", null));
        }

        if (request.rejectionReason().length() > 500) {
            return ResponseEntity.badRequest()
                    .body(new ActionResponse(false, "Rejection reason too long (max 500 chars)", null));
        }

        try {
            UUID uuid = UUID.fromString(candidateId);
            // TODO: Get adminUserId from Spring Security context when authentication is implemented
            UUID adminUserId = UUID.randomUUID();
            FundingSourceCandidate rejected = validationService.rejectCandidate(uuid, adminUserId, request.rejectionReason());

            return ResponseEntity.ok(
                new ActionResponse(
                    true,
                    "Candidate rejected successfully",
                    rejected.getCandidateId()
                )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/candidates/{candidateId}/assign - Assign candidate to reviewer
     */
    @PostMapping("/{candidateId}/assign")
    public ResponseEntity<ActionResponse> assignCandidate(
            @PathVariable String candidateId,
            @RequestBody AssignmentRequest request) {

        // Validate request
        if (request.reviewerId() == null) {
            return ResponseEntity.badRequest()
                    .body(new ActionResponse(false, "Reviewer ID is required", null));
        }

        try {
            UUID candidateUuid = UUID.fromString(candidateId);
            UUID reviewerUuid = UUID.fromString(request.reviewerId());

            FundingSourceCandidate assigned = validationService.assignToReviewer(candidateUuid, reviewerUuid);

            return ResponseEntity.ok(
                new ActionResponse(
                    true,
                    "Candidate assigned successfully",
                    assigned.getCandidateId()
                )
            );

        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("inactive")) {
                return ResponseEntity.badRequest()
                        .body(new ActionResponse(false, e.getMessage(), null));
            }
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DTOs
     */
    public record ApprovalRequest(String approvalNotes) {}
    public record RejectionRequest(String rejectionReason) {}
    public record AssignmentRequest(String reviewerId) {}

    public record ActionResponse(
        boolean success,
        String message,
        UUID candidateId
    ) {}
}
