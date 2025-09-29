package com.northstar.funding.discovery.domain;

/**
 * Enhancement Type enumeration for Enhancement Records
 * 
 * Categorizes different types of manual improvements made by admin users
 * to AI-discovered funding source candidates for quality tracking.
 */
public enum EnhancementType {
    /**
     * New contact information added to candidate
     */
    CONTACT_ADDED,
    
    /**
     * Fixed incorrect information discovered by AI
     */
    DATA_CORRECTED,
    
    /**
     * Added validation or context notes
     */
    NOTES_ADDED,
    
    /**
     * Merged duplicate candidate data
     */
    DUPLICATE_MERGED
}
