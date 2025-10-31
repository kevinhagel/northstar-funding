package com.northstar.funding.domain;

/**
 * Candidate Status enumeration for funding source candidates
 *
 * Represents the lifecycle states of discovered funding opportunities
 * through the human-AI collaboration review process.
 *
 * Two-Phase Processing Pipeline:
 * Phase 1 (Metadata Judging): PENDING_CRAWL - judge based on search metadata only
 * Phase 2 (Deep Crawling): PENDING_REVIEW - high-confidence candidates crawled for full details
 */
public enum CandidateStatus {
    /**
     * Phase 1: High-confidence candidate pending deep web crawl
     * Judged based on search engine metadata only (no HTTP request yet)
     */
    PENDING_CRAWL,

    /**
     * Phase 2: Crawled and awaiting human review
     * Full web page details extracted, ready for dashboard review
     */
    PENDING_REVIEW,

    /**
     * Currently being reviewed by admin user in dashboard
     */
    IN_REVIEW,

    /**
     * Validated and approved for knowledge base
     * Will be vectorized and stored in Qdrant for RAG search
     */
    APPROVED,

    /**
     * Rejected - not suitable for inclusion
     * Domain quality metrics updated based on rejection
     */
    REJECTED,

    /**
     * Low confidence from Phase 1 metadata judging
     * Skipped, not worth crawling
     */
    SKIPPED_LOW_CONFIDENCE
}
