package com.northstar.funding.domain;

/**
 * Classifies WHO provides the funding.
 *
 * This enum represents different types of organizations and institutions
 * that offer funding opportunities for educational projects.
 */
public enum FundingSourceType {

    /** National government ministries and agencies */
    GOVERNMENT_NATIONAL,

    /** European Union Commission and institutions */
    GOVERNMENT_EU,

    /** Regional, municipal, and local governments */
    GOVERNMENT_REGIONAL,

    /** Private philanthropic foundations */
    PRIVATE_FOUNDATION,

    /** Corporate CSR and charitable foundations */
    CORPORATE_FOUNDATION,

    /** Bilateral aid agencies (USAID, GIZ, British Council, etc.) */
    BILATERAL_AID,

    /** Multilateral organizations (World Bank, UN agencies, etc.) */
    MULTILATERAL_ORG,

    /** Community-based local foundations */
    COMMUNITY_FOUNDATION,

    /** Professional education associations and networks */
    EDUCATION_ASSOCIATION,

    /** Crowdfunding platforms for education projects */
    CROWDFUNDING_PLATFORM,

    /** Religious and faith-based funding organizations */
    RELIGIOUS_FOUNDATION,

    /** Financial institutions offering education loans and grants */
    CREDIT_UNION_OR_BANK
}
