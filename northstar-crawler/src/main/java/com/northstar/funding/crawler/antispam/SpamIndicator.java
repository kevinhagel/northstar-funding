package com.northstar.funding.crawler.antispam;

/**
 * Enum of spam indicators for tracking.
 */
public enum SpamIndicator {
    KEYWORD_STUFFING,           // Unique word ratio < 0.5
    DOMAIN_METADATA_MISMATCH,   // Cosine similarity < 0.15
    UNNATURAL_KEYWORD_LIST,     // Missing common articles/prepositions
    CROSS_CATEGORY_SPAM         // Gambling/essay mill domain + education keywords
}
