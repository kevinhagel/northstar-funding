package com.northstar.funding.querygeneration.template;

import com.northstar.funding.domain.FundingSearchCategory;
import org.springframework.stereotype.Component;

/**
 * Maps funding categories to search terms.
 *
 * <p>Provides two mapping strategies:
 * <ul>
 *   <li>Keywords: Short, keyword-focused terms for traditional search engines</li>
 *   <li>Conceptual: Longer, contextual descriptions for AI-optimized search</li>
 * </ul>
 */
@Component
public class CategoryMapper {

    /**
     * Maps category to keyword string for traditional search engines.
     *
     * @param category Funding category
     * @return Comma-separated keywords
     */
    public String toKeywords(FundingSearchCategory category) {
        return switch (category) {
            // Individual Support
            case INDIVIDUAL_SCHOLARSHIPS -> "scholarships, student grants, individual funding";
            case STUDENT_FINANCIAL_AID -> "financial aid, student support, tuition assistance";
            case TEACHER_SCHOLARSHIPS -> "teacher scholarships, educator grants, professional development funding";
            case ACADEMIC_FELLOWSHIPS -> "fellowships, academic grants, research funding";

            // Program Support
            case PROGRAM_GRANTS -> "program grants, project funding, educational programs";
            case CURRICULUM_DEVELOPMENT -> "curriculum development, educational materials, teaching resources";
            case AFTER_SCHOOL_PROGRAMS -> "after school programs, extracurricular funding, youth programs";
            case SUMMER_PROGRAMS -> "summer programs, seasonal education, summer school funding";
            case EXTRACURRICULAR_ACTIVITIES -> "extracurricular activities, clubs, student enrichment";

            // Infrastructure & Facilities
            case INFRASTRUCTURE_FUNDING -> "infrastructure grants, facility funding, building projects";
            case TECHNOLOGY_EQUIPMENT -> "technology grants, equipment funding, computers, devices";
            case LIBRARY_RESOURCES -> "library grants, book funding, educational materials";

            // Teacher & Staff Development
            case TEACHER_DEVELOPMENT -> "teacher training, professional development, educator workshops";
            case PROFESSIONAL_TRAINING -> "professional training, staff development, skills training";
            case ADMINISTRATIVE_CAPACITY -> "administrative capacity, leadership training, management support";

            // STEM & Special Focus
            case STEM_EDUCATION -> "STEM education, science technology engineering math, STEM grants";
            case ARTS_EDUCATION -> "arts education, music programs, creative arts funding";
            case SPECIAL_NEEDS_EDUCATION -> "special needs education, inclusive education, disability support";
            case LANGUAGE_PROGRAMS -> "language programs, ESL, foreign language education";

            // Community & Partnerships
            case COMMUNITY_PARTNERSHIPS -> "community partnerships, school community collaboration, parent engagement";
            case PARENT_ENGAGEMENT -> "parent engagement, family involvement, parent programs";
            case NGO_EDUCATION_PROJECTS -> "NGO education, nonprofit school projects, civil society education";

            // Research & Innovation
            case EDUCATION_RESEARCH -> "education research, academic studies, research grants";
            case PILOT_PROGRAMS -> "pilot programs, innovative education, experimental projects";
            case INNOVATION_GRANTS -> "innovation grants, educational innovation, new approaches";
        };
    }

    /**
     * Maps category to conceptual description for AI-optimized search.
     *
     * @param category Funding category
     * @return Contextual description suitable for AI search
     */
    public String toConceptualDescription(FundingSearchCategory category) {
        return switch (category) {
            // Individual Support
            case INDIVIDUAL_SCHOLARSHIPS ->
                "individual scholarship opportunities providing financial support for students pursuing education";
            case STUDENT_FINANCIAL_AID ->
                "financial aid programs helping students cover tuition, fees, and educational expenses";
            case TEACHER_SCHOLARSHIPS ->
                "scholarship programs supporting teachers and educators in advancing their professional qualifications";
            case ACADEMIC_FELLOWSHIPS ->
                "fellowship programs providing research and academic development opportunities";

            // Program Support
            case PROGRAM_GRANTS ->
                "funding for educational program development and implementation at schools and institutions";
            case CURRICULUM_DEVELOPMENT ->
                "support for developing innovative curricula, teaching materials, and educational content";
            case AFTER_SCHOOL_PROGRAMS ->
                "funding for after-school enrichment activities, homework help, and extended learning opportunities";
            case SUMMER_PROGRAMS ->
                "summer learning programs preventing learning loss and providing enrichment during school breaks";
            case EXTRACURRICULAR_ACTIVITIES ->
                "support for clubs, sports, arts, and other activities that enhance student learning outside the classroom";

            // Infrastructure & Facilities
            case INFRASTRUCTURE_FUNDING ->
                "funding for educational infrastructure development, facility construction, renovation, and modernization";
            case TECHNOLOGY_EQUIPMENT ->
                "grants for purchasing computers, tablets, interactive whiteboards, and other educational technology";
            case LIBRARY_RESOURCES ->
                "funding for library books, digital resources, and learning materials";

            // Teacher & Staff Development
            case TEACHER_DEVELOPMENT ->
                "professional development opportunities for teachers including training, workshops, and skill enhancement";
            case PROFESSIONAL_TRAINING ->
                "training programs for educational staff to develop teaching skills and pedagogical knowledge";
            case ADMINISTRATIVE_CAPACITY ->
                "capacity building for school administration, leadership development, and management effectiveness";

            // STEM & Special Focus
            case STEM_EDUCATION ->
                "science, technology, engineering, and mathematics education programs and initiatives";
            case ARTS_EDUCATION ->
                "arts, music, theater, and creative education programs fostering artistic expression";
            case SPECIAL_NEEDS_EDUCATION ->
                "inclusive education programs supporting students with disabilities and special learning needs";
            case LANGUAGE_PROGRAMS ->
                "language education including foreign languages, ESL, and multilingual learning programs";

            // Community & Partnerships
            case COMMUNITY_PARTNERSHIPS ->
                "collaborative partnerships between schools, communities, businesses, and organizations";
            case PARENT_ENGAGEMENT ->
                "programs engaging parents and families in student education and school activities";
            case NGO_EDUCATION_PROJECTS ->
                "educational projects implemented by non-governmental organizations and civil society groups";

            // Research & Innovation
            case EDUCATION_RESEARCH ->
                "research initiatives studying educational effectiveness, pedagogy, and learning outcomes";
            case PILOT_PROGRAMS ->
                "experimental pilot programs testing innovative educational approaches and methodologies";
            case INNOVATION_GRANTS ->
                "funding for innovative educational projects that explore new teaching and learning methods";
        };
    }
}
