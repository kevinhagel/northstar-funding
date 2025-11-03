package com.northstar.funding.domain;

/**
 * Geographic scope for funding opportunities.
 *
 * <p>Defines the geographic regions and jurisdictions for funding search targeting.
 */
public enum GeographicScope {
    // Country-specific
    BULGARIA,
    ROMANIA,
    GREECE,
    SERBIA,
    NORTH_MACEDONIA,

    // Regional
    EASTERN_EUROPE,
    BALKANS,
    SOUTHEASTERN_EUROPE,
    CENTRAL_EUROPE,

    // EU-related
    EU_MEMBER_STATES,
    EU_CANDIDATE_COUNTRIES,
    EU_ENLARGEMENT_REGION,

    // Broader scopes
    EUROPE,
    INTERNATIONAL,
    GLOBAL
}
