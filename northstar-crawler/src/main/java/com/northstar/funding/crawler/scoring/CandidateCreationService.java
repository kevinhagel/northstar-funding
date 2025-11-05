package com.northstar.funding.crawler.scoring;

import com.northstar.funding.domain.CandidateStatus;
import com.northstar.funding.domain.FundingSourceCandidate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for creating FundingSourceCandidate entities with confidence-based status assignment.
 *
 * Converts search result metadata + confidence score into structured candidates:
 * - High confidence (>= 0.60) → PENDING_CRAWL status
 * - Low confidence (< 0.60) → SKIPPED_LOW_CONFIDENCE status
 *
 * All candidates are created with metadata extracted from search results.
 */
@Service
public class CandidateCreationService {

    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.60");

    /**
     * Create a FundingSourceCandidate from search result metadata.
     *
     * @param title Search result title (used as organizationName)
     * @param description Search result description
     * @param url Search result URL
     * @param domainId Associated domain UUID
     * @param sessionId Discovery session UUID
     * @param confidence Calculated confidence score (0.00 to 1.00)
     * @return FundingSourceCandidate with appropriate status
     */
    public FundingSourceCandidate createCandidate(
        String title,
        String description,
        String url,
        UUID domainId,
        UUID sessionId,
        BigDecimal confidence
    ) {
        // Determine status based on confidence threshold
        CandidateStatus status = confidence.compareTo(CONFIDENCE_THRESHOLD) >= 0
            ? CandidateStatus.PENDING_CRAWL
            : CandidateStatus.SKIPPED_LOW_CONFIDENCE;

        return FundingSourceCandidate.builder()
            .status(status)
            .organizationName(title)
            .description(description)
            .sourceUrl(url)
            .domainId(domainId)
            .discoverySessionId(sessionId)
            .confidenceScore(confidence)
            .build();
    }
}
