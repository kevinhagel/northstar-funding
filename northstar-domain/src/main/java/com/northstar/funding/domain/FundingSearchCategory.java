package com.northstar.funding.domain;

/**
 * Enumeration of funding category types for search targeting.
 *
 * <p>30 distinct funding categories covering educational funding opportunities.
 */
public enum FundingSearchCategory {
    // Individual Support (4 categories)
    INDIVIDUAL_SCHOLARSHIPS,
    STUDENT_FINANCIAL_AID,
    TEACHER_SCHOLARSHIPS,
    ACADEMIC_FELLOWSHIPS,

    // Program Support (5 categories)
    PROGRAM_GRANTS,
    CURRICULUM_DEVELOPMENT,
    AFTER_SCHOOL_PROGRAMS,
    SUMMER_PROGRAMS,
    EXTRACURRICULAR_ACTIVITIES,

    // Infrastructure & Facilities (3 categories)
    INFRASTRUCTURE_FUNDING,
    TECHNOLOGY_EQUIPMENT,
    LIBRARY_RESOURCES,

    // Teacher & Staff Development (3 categories)
    TEACHER_DEVELOPMENT,
    PROFESSIONAL_TRAINING,
    ADMINISTRATIVE_CAPACITY,

    // STEM & Special Focus (4 categories)
    STEM_EDUCATION,
    ARTS_EDUCATION,
    SPECIAL_NEEDS_EDUCATION,
    LANGUAGE_PROGRAMS,

    // Community & Partnerships (3 categories)
    COMMUNITY_PARTNERSHIPS,
    PARENT_ENGAGEMENT,
    NGO_EDUCATION_PROJECTS,

    // Research & Innovation (3 categories)
    EDUCATION_RESEARCH,
    PILOT_PROGRAMS,
    INNOVATION_GRANTS,

    // Feature 005: Age-specific & Modern Education (5 categories)
    /** Early childhood education (ages 0-5) funding */
    EARLY_CHILDHOOD_EDUCATION,

    /** Adult education and lifelong learning funding */
    ADULT_EDUCATION,

    /** Vocational and technical training funding */
    VOCATIONAL_TRAINING,

    /** Educational technology and digital learning funding */
    EDUCATIONAL_TECHNOLOGY,

    /** Arts and cultural education funding */
    ARTS_CULTURE
}
