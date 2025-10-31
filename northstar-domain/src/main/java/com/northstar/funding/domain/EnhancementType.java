package com.northstar.funding.domain;

/**
 * Enhancement Type enumeration for Enhancement Records
 *
 * Categorizes the ORIGIN of enhancements to track human-AI collaboration.
 * Constitutional requirement: Track WHO made the change and HOW (AI vs human).
 *
 * This enum tracks the SOURCE of the enhancement, not the type of change.
 * Use this to analyze AI vs human contribution patterns.
 */
public enum EnhancementType {
    /**
     * AI suggested this enhancement but human hasn't approved yet.
     * Example: LM Studio analyzed candidate and suggested better description.
     * Requires: aiModel field must be set.
     */
    AI_SUGGESTED,

    /**
     * Human manually created this enhancement without AI assistance.
     * Example: Admin found contact email through website research.
     */
    MANUAL,

    /**
     * Human modified an AI suggestion before accepting it.
     * Example: AI suggested tags, human refined them.
     */
    HUMAN_MODIFIED
}
