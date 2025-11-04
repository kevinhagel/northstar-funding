package com.northstar.funding.domain;

/**
 * Classifies HOW funding is distributed.
 *
 * This enum represents different mechanisms through which funding
 * opportunities are provided to recipients.
 */
public enum FundingMechanism {

    /** Non-repayable grant funding */
    GRANT,

    /** Repayable loan with various terms */
    LOAN,

    /** Individual student scholarship */
    SCHOLARSHIP,

    /** Research or professional fellowship */
    FELLOWSHIP,

    /** Grant requiring matching funds from recipient */
    MATCHING_GRANT,

    /** Competitive prize or award */
    PRIZE_AWARD,

    /** In-kind donation of equipment, materials, or services */
    IN_KIND_DONATION,

    /** Government subsidy or tax benefit */
    SUBSIDY
}
