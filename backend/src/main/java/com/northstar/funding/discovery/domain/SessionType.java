package com.northstar.funding.discovery.domain;

/**
 * Session Type enumeration for Discovery Sessions
 * 
 * Categorizes different types of discovery session executions
 * for audit tracking and performance analysis.
 */
public enum SessionType {
    /**
     * Regular automated discovery on schedule
     */
    SCHEDULED,
    
    /**
     * Manually triggered by admin user
     */
    MANUAL,
    
    /**
     * Retry of failed discovery session
     */
    RETRY
}
