package com.northstar.funding.discovery.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.northstar.funding.discovery.application.CandidateValidationService;
import com.northstar.funding.discovery.domain.CandidateStatus;
import com.northstar.funding.discovery.domain.FundingSourceCandidate;
import com.northstar.funding.discovery.infrastructure.FundingSourceCandidateRepository;

/**
 * REST Controller: CandidateController
 *
 * Handles candidate listing, retrieval, and updates
 * Endpoints: GET /api/candidates, GET /api/candidates/{id}, PUT /api/candidates/{id}
 */
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final FundingSourceCandidateRepository candidateRepository;
    private final CandidateValidationService validationService;

    public CandidateController(
            FundingSourceCandidateRepository candidateRepository,
            CandidateValidationService validationService) {
        this.candidateRepository = candidateRepository;
        this.validationService = validationService;
    }

    /**
     * GET /api/candidates - List candidates with pagination, filtering, and sorting
     */
    @GetMapping
    public ResponseEntity<Page<FundingSourceCandidate>> listCandidates(
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(required = false) Double minConfidence,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "confidenceScore") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        // Validate pagination parameters
        if (page < 0) {
            return ResponseEntity.badRequest().build();
        }
        if (size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }

        // Build sort
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FundingSourceCandidate> candidates;

        // Apply filters
        if (status != null) {
            candidates = candidateRepository.findByStatusOrderByConfidenceScoreDesc(status, pageable);
        } else if (assignedTo != null) {
            candidates = candidateRepository.findByAssignedReviewerId(assignedTo, pageable);
        } else {
            candidates = candidateRepository.findAll(pageable);
        }

        return ResponseEntity.ok(candidates);
    }

    /**
     * GET /api/candidates/{candidateId} - Get candidate details
     */
    @GetMapping("/{candidateId}")
    public ResponseEntity<FundingSourceCandidate> getCandidateById(
            @PathVariable String candidateId) {

        try {
            UUID uuid = UUID.fromString(candidateId);
            return candidateRepository.findById(uuid)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/candidates/{candidateId} - Update candidate with enhancements
     */
    @PutMapping("/{candidateId}")
    public ResponseEntity<FundingSourceCandidate> updateCandidate(
            @PathVariable String candidateId,
            @RequestBody CandidateUpdateRequest updateRequest) {

        try {
            UUID uuid = UUID.fromString(candidateId);

            FundingSourceCandidate candidate = candidateRepository.findById(uuid)
                    .orElse(null);

            if (candidate == null) {
                return ResponseEntity.notFound().build();
            }

            // Validate update data
            if (updateRequest.organizationName() != null && updateRequest.organizationName().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            if (updateRequest.fundingAmountMin() != null && updateRequest.fundingAmountMin() < 0) {
                return ResponseEntity.badRequest().build();
            }

            // Apply updates
            if (updateRequest.organizationName() != null) {
                candidate.setOrganizationName(updateRequest.organizationName());
            }
            if (updateRequest.programName() != null) {
                candidate.setProgramName(updateRequest.programName());
            }
            if (updateRequest.description() != null) {
                candidate.setDescription(updateRequest.description());
            }
            if (updateRequest.fundingAmountMin() != null) {
                candidate.setFundingAmountMin(java.math.BigDecimal.valueOf(updateRequest.fundingAmountMin()));
            }
            if (updateRequest.fundingAmountMax() != null) {
                candidate.setFundingAmountMax(java.math.BigDecimal.valueOf(updateRequest.fundingAmountMax()));
            }
            if (updateRequest.currency() != null) {
                candidate.setCurrency(updateRequest.currency());
            }
            if (updateRequest.geographicEligibility() != null) {
                candidate.setGeographicEligibility(updateRequest.geographicEligibility());
            }
            if (updateRequest.organizationTypes() != null) {
                candidate.setOrganizationTypes(updateRequest.organizationTypes());
            }
            if (updateRequest.applicationProcess() != null) {
                candidate.setApplicationProcess(updateRequest.applicationProcess());
            }
            if (updateRequest.requirements() != null) {
                candidate.setRequirements(updateRequest.requirements());
            }
            if (updateRequest.tags() != null) {
                candidate.setTags(updateRequest.tags());
            }
            if (updateRequest.validationNotes() != null) {
                candidate.setValidationNotes(updateRequest.validationNotes());
            }

            FundingSourceCandidate updated = candidateRepository.save(candidate);
            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DTO for candidate updates
     */
    public record CandidateUpdateRequest(
        String organizationName,
        String programName,
        String description,
        Double fundingAmountMin,
        Double fundingAmountMax,
        String currency,
        java.util.List<String> geographicEligibility,
        java.util.List<String> organizationTypes,
        String applicationProcess,
        java.util.List<String> requirements,
        java.util.Set<String> tags,
        String validationNotes
    ) {}
}
