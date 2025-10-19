package com.northstar.funding.discovery.domain;

/**
 * Domain Status enumeration
 *
 * Tracks the processing state and quality of discovered domains.
 * Supports domain-level deduplication and blacklisting.
 */
public enum DomainStatus {
    /**
     * Domain discovered but not yet processed
     */
    DISCOVERED,

    /**
     * Domain currently being processed (metadata judging in progress)
     */
    PROCESSING,

    /**
     * Domain processed successfully - yielded high-quality candidates
     */
    PROCESSED_HIGH_QUALITY,

    /**
     * Domain processed - yielded low-quality candidates (low confidence scores)
     * Won't be processed again for this domain
     */
    PROCESSED_LOW_QUALITY,

    /**
     * Domain blacklisted - known scam, spam, or irrelevant
     * Permanent - never process again
     */
    BLACKLISTED,

    /**
     * Legitimate funder but no funds available this year
     * May process again in future years
     */
    NO_FUNDS_THIS_YEAR,

    /**
     * Processing failed due to technical error (timeout, 404, etc.)
     * Can be retried
     */
    PROCESSING_FAILED
}
