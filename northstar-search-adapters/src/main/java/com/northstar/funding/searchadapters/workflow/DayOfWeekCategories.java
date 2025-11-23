package com.northstar.funding.searchadapters.workflow;

import com.northstar.funding.domain.FundingSearchCategory;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * 7-day rotation schedule for funding search categories.
 *
 * <p>Distributes 30 funding categories across Monday-Sunday to ensure:
 * - All categories searched weekly (7-day cycle)
 * - Balanced workload per night (3-5 categories)
 * - Individual/student funding early week (Mon-Tue)
 * - Infrastructure mid-week (Wed-Thu)
 * - Specialized programs end-week (Fri-Sun)
 *
 * <p>From research.md R5 - Nightly Search Distribution Strategy
 */
public final class DayOfWeekCategories {

    private DayOfWeekCategories() {
        // Utility class - prevent instantiation
    }

    /**
     * Category distribution by day of week.
     * Total: 30 categories across 7 days.
     */
    private static final Map<DayOfWeek, List<FundingSearchCategory>> SCHEDULE = Map.of(
        // MONDAY: 4 categories - Individual & Student Funding
        DayOfWeek.MONDAY, List.of(
            FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
            FundingSearchCategory.STUDENT_FINANCIAL_AID,
            FundingSearchCategory.TEACHER_SCHOLARSHIPS,
            FundingSearchCategory.ACADEMIC_FELLOWSHIPS
        ),

        // TUESDAY: 5 categories - Program Grants
        DayOfWeek.TUESDAY, List.of(
            FundingSearchCategory.PROGRAM_GRANTS,
            FundingSearchCategory.CURRICULUM_DEVELOPMENT,
            FundingSearchCategory.AFTER_SCHOOL_PROGRAMS,
            FundingSearchCategory.SUMMER_PROGRAMS,
            FundingSearchCategory.EXTRACURRICULAR_ACTIVITIES
        ),

        // WEDNESDAY: 3 categories - Infrastructure & Equipment
        DayOfWeek.WEDNESDAY, List.of(
            FundingSearchCategory.INFRASTRUCTURE_FUNDING,
            FundingSearchCategory.TECHNOLOGY_EQUIPMENT,
            FundingSearchCategory.LIBRARY_RESOURCES
        ),

        // THURSDAY: 3 categories - Professional Development
        DayOfWeek.THURSDAY, List.of(
            FundingSearchCategory.TEACHER_DEVELOPMENT,
            FundingSearchCategory.PROFESSIONAL_TRAINING,
            FundingSearchCategory.ADMINISTRATIVE_CAPACITY
        ),

        // FRIDAY: 4 categories - Specialized Education
        DayOfWeek.FRIDAY, List.of(
            FundingSearchCategory.STEM_EDUCATION,
            FundingSearchCategory.ARTS_EDUCATION,
            FundingSearchCategory.SPECIAL_NEEDS_EDUCATION,
            FundingSearchCategory.LANGUAGE_PROGRAMS
        ),

        // SATURDAY: 3 categories - Community & Partnerships
        DayOfWeek.SATURDAY, List.of(
            FundingSearchCategory.COMMUNITY_PARTNERSHIPS,
            FundingSearchCategory.PARENT_ENGAGEMENT,
            FundingSearchCategory.NGO_EDUCATION_PROJECTS
        ),

        // SUNDAY: 8 categories - Research, Innovation & Catch-All
        DayOfWeek.SUNDAY, List.of(
            FundingSearchCategory.EDUCATION_RESEARCH,
            FundingSearchCategory.PILOT_PROGRAMS,
            FundingSearchCategory.INNOVATION_GRANTS,
            FundingSearchCategory.EARLY_CHILDHOOD_EDUCATION,
            FundingSearchCategory.ADULT_EDUCATION,
            FundingSearchCategory.VOCATIONAL_TRAINING,
            FundingSearchCategory.EDUCATIONAL_TECHNOLOGY,
            FundingSearchCategory.ARTS_CULTURE
        )
    );

    /**
     * Get funding categories for a specific day of week.
     *
     * @param dayOfWeek Day of week (MONDAY-SUNDAY)
     * @return Immutable list of FundingSearchCategory for that day (3-8 categories)
     * @throws IllegalArgumentException if dayOfWeek is null
     */
    public static List<FundingSearchCategory> getCategories(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("dayOfWeek cannot be null");
        }

        List<FundingSearchCategory> categories = SCHEDULE.get(dayOfWeek);
        if (categories == null) {
            // Should never happen unless DayOfWeek enum changes
            throw new IllegalStateException("No categories defined for " + dayOfWeek);
        }

        return categories;
    }

    /**
     * Get total number of categories across all days.
     *
     * @return Total category count (should be 30)
     */
    public static int getTotalCategories() {
        return SCHEDULE.values().stream()
            .mapToInt(List::size)
            .sum();
    }

    /**
     * Get the complete schedule for all days.
     *
     * @return Immutable map of DayOfWeek → List<FundingSearchCategory>
     */
    public static Map<DayOfWeek, List<FundingSearchCategory>> getFullSchedule() {
        return Map.copyOf(SCHEDULE);
    }
}
