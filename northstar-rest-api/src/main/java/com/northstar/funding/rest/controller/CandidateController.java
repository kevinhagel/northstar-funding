package com.northstar.funding.rest.controller;

import com.northstar.funding.rest.dto.CandidateDTO;
import com.northstar.funding.rest.dto.CandidatePageDTO;
import com.northstar.funding.rest.service.CandidateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for funding source candidate operations.
 *
 * Endpoints:
 * - GET /api/candidates - List candidates with filters/pagination
 * - PUT /api/candidates/{id}/approve - Approve candidate
 * - PUT /api/candidates/{id}/reject - Reject candidate and blacklist domain
 */
@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    /**
     * List candidates with optional filters and pagination.
     *
     * @param status Filter by candidate status (multi-select)
     * @param minConfidence Minimum confidence score (0.00-1.00)
     * @param searchEngine Filter by search engine (multi-select)
     * @param startDate Filter by discovered date >= startDate (ISO-8601: YYYY-MM-DD)
     * @param endDate Filter by discovered date <= endDate (ISO-8601: YYYY-MM-DD)
     * @param sortBy Column to sort by (default: createdAt)
     * @param sortDirection Sort direction (ASC/DESC, default: DESC)
     * @param page Page number (0-indexed, default: 0)
     * @param size Number of items per page (default: 20)
     * @return Paginated list of candidates
     */
    @GetMapping
    public ResponseEntity<CandidatePageDTO> listCandidates(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) BigDecimal minConfidence,
            @RequestParam(required = false) List<String> searchEngine,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        CandidatePageDTO result = candidateService.listCandidates(
                status,
                minConfidence,
                searchEngine,
                startDate,
                endDate,
                sortBy,
                sortDirection,
                page,
                size
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Approve a candidate for inclusion in client-facing database.
     *
     * @param id Candidate UUID
     * @return Updated candidate DTO
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<CandidateDTO> approveCandidate(@PathVariable UUID id) {
        try {
            CandidateDTO result = candidateService.approveCandidate(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    /**
     * Reject a candidate and blacklist its domain.
     *
     * @param id Candidate UUID
     * @return Updated candidate DTO
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<CandidateDTO> rejectCandidate(@PathVariable UUID id) {
        try {
            CandidateDTO result = candidateService.rejectCandidate(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
    }
}
