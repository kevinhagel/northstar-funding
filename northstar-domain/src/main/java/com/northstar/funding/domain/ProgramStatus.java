package com.northstar.funding.domain;

/**
 * Program Status Enum
 *
 * Lifecycle states for funding programs discovered by the system.
 *
 * Flow:
 * DISCOVERED → PENDING_JUDGMENT → ACTIVE → EXPIRED/ARCHIVED
 */
public enum ProgramStatus {

    /**
     * Program discovered but not yet judged
     */
    DISCOVERED,

    /**
     * Awaiting metadata judgment from LM Studio
     */
    PENDING_JUDGMENT,

    /**
     * Program judged as valid and currently active
     */
    ACTIVE,

    /**
     * Program judged as invalid/not relevant
     */
    REJECTED,

    /**
     * Application deadline has passed
     */
    EXPIRED,

    /**
     * Program archived for historical reference
     */
    ARCHIVED,

    /**
     * Program temporarily unavailable
     */
    SUSPENDED
}
