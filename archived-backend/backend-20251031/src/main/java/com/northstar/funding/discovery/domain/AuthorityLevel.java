package com.northstar.funding.discovery.domain;

/**
 * Authority Level enumeration for Contact Intelligence
 * 
 * Represents the decision-making power and influence level of contacts
 * within funding organizations for strategic relationship prioritization.
 */
public enum AuthorityLevel {
    /**
     * Can approve/reject applications - highest priority contact
     */
    DECISION_MAKER,
    
    /**
     * Influences decisions but doesn't decide - important stakeholder
     */
    INFLUENCER,
    
    /**
     * Provides information but no decision authority - supporting contact
     */
    INFORMATION_ONLY
}
