package com.northstar.funding.rest.dto;

import java.util.List;

/**
 * Paginated response for candidate list endpoint.
 *
 * Follows Spring Data Page pattern but simplified for JSON.
 */
public record CandidatePageDTO(
    List<CandidateDTO> content,     // Candidates for current page
    int totalElements,              // Total candidates matching filters
    int totalPages,                 // Total number of pages
    int currentPage,                // Current page number (0-indexed)
    int pageSize                    // Number of items per page
) {}
