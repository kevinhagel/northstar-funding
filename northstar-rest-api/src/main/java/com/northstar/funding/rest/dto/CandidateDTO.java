package com.northstar.funding.rest.dto;

/**
 * DTO for FundingSourceCandidate entity.
 *
 * All complex types converted to String for JSON compatibility:
 * - UUID → String (36 chars: "123e4567-e89b-12d3-a456-426614174000")
 * - BigDecimal → String (e.g., "0.85")
 * - Enum → String (e.g., "PENDING_CRAWL")
 * - LocalDateTime → String (ISO-8601: "2025-11-16T10:30:00")
 */
public record CandidateDTO(
    String id,                 // UUID of candidate
    String url,                // Funding source URL
    String title,              // Page title from search result
    String confidenceScore,    // Confidence 0.00-1.00 (BigDecimal as String)
    String status,             // CandidateStatus enum name
    String searchEngine,       // SearchEngineType enum name
    String createdAt           // Discovery timestamp (ISO-8601)
) {}
