package com.northstar.funding.domain;

/**
 * Classifies WHAT TYPE of organization receives funding.
 *
 * This enum represents different organizational categories that
 * can receive funding opportunities.
 */
public enum RecipientOrganizationType {

    /** Private language schools and training centers */
    PRIVATE_LANGUAGE_SCHOOL,

    /** K-12 private schools */
    K12_PRIVATE_SCHOOL,

    /** K-12 public schools */
    K12_PUBLIC_SCHOOL,

    /** Preschool and early childhood centers */
    PRESCHOOL_EARLY_CHILDHOOD,

    /** Examination and testing centers */
    EXAMINATION_CENTER,

    /** Education-focused NGOs and nonprofits */
    NGO_EDUCATION_FOCUSED,

    /** Social services NGOs with education programs */
    NGO_SOCIAL_SERVICES,

    /** For-profit education companies */
    FOR_PROFIT_EDUCATION,

    /** Public universities and colleges */
    UNIVERSITY_PUBLIC,

    /** Municipal and local government entities */
    MUNICIPALITY,

    /** Individual educators and teachers */
    INDIVIDUAL_EDUCATOR,

    /** Individual students and learners */
    INDIVIDUAL_STUDENT,

    /** Research institutes and think tanks */
    RESEARCH_INSTITUTE,

    /** Libraries, museums, and cultural centers */
    LIBRARY_OR_CULTURAL_CENTER
}
