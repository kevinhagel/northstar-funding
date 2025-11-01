package com.northstar.funding.discovery.domain;

/**
 * Session Status enumeration for Discovery Sessions
 * 
 * Tracks the execution status of automated discovery sessions
 * for monitoring and error handling.
 */
public enum SessionStatus {
    /**
     * Discovery session is currently executing
     */
    RUNNING,
    
    /**
     * Discovery session completed successfully
     */
    COMPLETED,
    
    /**
     * Discovery session failed with errors
     */
    FAILED,
    
    /**
     * Discovery session was cancelled before completion
     */
    CANCELLED
}
