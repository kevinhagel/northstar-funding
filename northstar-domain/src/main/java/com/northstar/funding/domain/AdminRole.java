package com.northstar.funding.domain;

/**
 * Admin Role enumeration for Admin Users
 * 
 * Represents the access level and responsibilities of admin users
 * in the Human-AI Collaboration workflow.
 */
public enum AdminRole {
    /**
     * Full system access - can manage users and system configuration
     * (Kevin & Huw as founders)
     */
    ADMINISTRATOR,
    
    /**
     * Can review and validate funding source candidates
     */
    REVIEWER
}
