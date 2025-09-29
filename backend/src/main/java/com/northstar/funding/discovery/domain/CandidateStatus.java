package com.northstar.funding.discovery.domain;

/**
 * Candidate Status enumeration for funding source candidates
 * 
 * Represents the lifecycle states of discovered funding opportunities
 * through the human-AI collaboration review process.
 */
public enum CandidateStatus {
    /**
     * Newly discovered, awaiting assignment to reviewer
     */
    PENDING_REVIEW,
    
    /**
     * Currently being reviewed by admin user
     */
    IN_REVIEW,
    
    /**
     * Validated and approved for knowledge base
     */
    APPROVED,
    
    /**
     * Rejected - not suitable for inclusion
     */
    REJECTED
}
