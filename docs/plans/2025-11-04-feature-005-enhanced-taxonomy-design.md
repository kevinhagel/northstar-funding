# Feature 005: Enhanced Taxonomy & Basic Scheduler - Design Document

**Date**: 2025-11-04
**Status**: Design Approved - Ready for Implementation
**Branch**: `005-enhanced-taxonomy`
**Related Session**: `northstar-notes/session-summaries/2025-11-04-feature-005-brainstorming-in-progress.md`

---

## Overview

Feature 005 adds multi-dimensional funding taxonomy and a basic scheduler to generate more precise, targeted funding discovery queries. This enables the platform to discover funding opportunities that match specific organizational needs (e.g., "government loans for private language schools" or "scholarships for low-income families").

**Design Philosophy**: Start focused, make it easy to add more values later. Validate through implementation and testing.

---

## Goals

1. **Multi-Dimensional Queries**: Enable query generation with 7 optional dimensions beyond category/geography
2. **Enhanced Taxonomy**: Add 66 new enum values across 6 new enums for funding classification
3. **Basic Scheduler**: Fixed daily schedule generating 20-25 queries per night (manual CLI trigger)
4. **Backward Compatibility**: Feature 004 code continues to work unchanged
5. **Extensibility**: Easy to add new enum values and dimensions in the future

---

## Non-Goals (Future Phases)

- Translation service implementation (only enum structure added)
- Automatic cron jobs (manual CLI trigger only for now)
- Admin dashboard UI
- User profile matching (Phase 5+)
- Advanced scheduling algorithms (Spring Batch, Quartz)

---

## Architecture

### Module Structure

```
northstar-funding/
├── northstar-domain/              # Enhanced with 6 new enums
├── northstar-query-generation/    # CategoryMapper enhanced, QueryGenerationRequest updated
├── northstar-scheduler/           # NEW MODULE - Scheduling logic
└── northstar-persistence/         # Services use enhanced requests
```

**Separation of Concerns**:
- **northstar-scheduler**: Batch orchestration, fixed daily schedule, CLI triggers
- **northstar-query-generation**: Multi-dimensional keyword generation, LLM prompts
- **northstar-domain**: Taxonomy enums (no business logic)

### Component Interaction

```
DailyScheduleService (scheduler)
  ↓ generates combinations
QueryGenerationRequest[] (20-25 per night)
  ↓ passed to
CategoryMapper (query-generation)
  ↓ produces keywords
LLM Prompt (query-generation)
  ↓ generates
Search Queries
```

---

## New Domain Enums (66 Total Values)

### 1. FundingSourceType (12 values)

Classifies WHO provides the funding:

```java
public enum FundingSourceType {
    GOVERNMENT_NATIONAL,      // National government ministries, agencies
    GOVERNMENT_EU,            // EU Commission, EU institutions
    GOVERNMENT_REGIONAL,      // Regional/municipal governments
    PRIVATE_FOUNDATION,       // Private philanthropic foundations
    CORPORATE_FOUNDATION,     // Corporate CSR foundations
    BILATERAL_AID,            // USAID, GIZ, British Council, etc.
    MULTILATERAL_ORG,         // World Bank, UN agencies, etc.
    COMMUNITY_FOUNDATION,     // Local community-based foundations
    EDUCATION_ASSOCIATION,    // Professional education associations
    CROWDFUNDING_PLATFORM,    // Kickstarter, Indiegogo for education
    RELIGIOUS_FOUNDATION,     // Church-based, faith-based funding
    CREDIT_UNION_OR_BANK      // Financial institutions offering education loans
}
```

### 2. FundingMechanism (8 values)

Classifies HOW funding is distributed:

```java
public enum FundingMechanism {
    GRANT,              // Non-repayable grant
    LOAN,               // Repayable loan
    SCHOLARSHIP,        // Individual student scholarship
    FELLOWSHIP,         // Research/professional fellowship
    MATCHING_GRANT,     // Requires matching funds
    PRIZE_AWARD,        // Competitive prize or award
    IN_KIND_DONATION,   // Equipment, materials, services
    SUBSIDY             // Government subsidy/tax benefit
}
```

### 3. ProjectScale (5 values)

Classifies funding amount ranges (Euro-denominated):

```java
public enum ProjectScale {
    MICRO("< €5k", BigDecimal.ZERO, new BigDecimal("5000")),
    SMALL("€5k - €50k", new BigDecimal("5000"), new BigDecimal("50000")),
    MEDIUM("€50k - €250k", new BigDecimal("50000"), new BigDecimal("250000")),
    LARGE("€250k - €1M", new BigDecimal("250000"), new BigDecimal("1000000")),
    MEGA("> €1M", new BigDecimal("1000000"), null);

    private final String displayName;
    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;  // null for MEGA (unlimited)
}
```

### 4. BeneficiaryPopulation (18 values)

Classifies WHO benefits from the funding:

```java
public enum BeneficiaryPopulation {
    LOW_INCOME_FAMILIES,
    RURAL_COMMUNITIES,
    ETHNIC_MINORITIES,
    GIRLS_WOMEN,
    CHILDREN_AGES_4_12,
    ADOLESCENTS_AGES_13_18,
    FIRST_GENERATION_STUDENTS,
    AT_RISK_YOUTH,
    PEOPLE_WITH_DISABILITIES,
    REFUGEES_IMMIGRANTS,
    LANGUAGE_MINORITIES,
    EARLY_CHILDHOOD_0_5,
    ADULTS_LIFELONG_LEARNING,
    EDUCATORS_TEACHERS,
    GENERAL_POPULATION,
    LGBTQ_PLUS,
    VETERANS,
    ELDERLY
}
```

### 5. RecipientOrganizationType (14 values)

Classifies WHAT TYPE of organization receives funding:

```java
public enum RecipientOrganizationType {
    PRIVATE_LANGUAGE_SCHOOL,
    K12_PRIVATE_SCHOOL,
    K12_PUBLIC_SCHOOL,
    PRESCHOOL_EARLY_CHILDHOOD,
    EXAMINATION_CENTER,
    NGO_EDUCATION_FOCUSED,
    NGO_SOCIAL_SERVICES,
    FOR_PROFIT_EDUCATION,
    UNIVERSITY_PUBLIC,
    MUNICIPALITY,
    INDIVIDUAL_EDUCATOR,
    INDIVIDUAL_STUDENT,
    RESEARCH_INSTITUTE,
    LIBRARY_OR_CULTURAL_CENTER
}
```

### 6. QueryLanguage (9 values)

Defines languages for query generation (future translation support):

```java
public enum QueryLanguage {
    BULGARIAN("bg", "български"),
    ENGLISH("en", "English"),
    GERMAN("de", "Deutsch"),
    ROMANIAN("ro", "română"),
    FRENCH("fr", "français"),
    RUSSIAN("ru", "русский"),
    GREEK("el", "ελληνικά"),
    TURKISH("tr", "Türkçe"),
    SERBIAN("sr", "српски");

    private final String languageCode;    // ISO 639-1
    private final String nativeName;      // Display name in native script
}
```

**Note**: Translation service NOT implemented yet. Enum structure only for future extensibility.

### 7. FundingSearchCategory Enhancement

**Existing 25 values** + **5 new values**:

```java
// NEW VALUES ADDED:
EARLY_CHILDHOOD_EDUCATION,
ADULT_EDUCATION,
VOCATIONAL_TRAINING,
EDUCATIONAL_TECHNOLOGY,
ARTS_CULTURE
```

---

## Multi-Dimensional QueryGenerationRequest

### Enhanced Request Object

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryGenerationRequest {
    // ========================================
    // REQUIRED FIELDS (Feature 004 - existing)
    // ========================================
    @NonNull
    private SearchEngineType searchEngine;

    @NonNull
    private FundingSearchCategory category;

    @NonNull
    private GeographicScope geographicScope;

    private Integer numberOfQueries;  // Defaults to 3

    // ========================================
    // OPTIONAL FIELDS (Feature 005 - NEW)
    // ========================================

    // WHO provides funding
    private FundingSourceType sourceType;

    // HOW funding is distributed
    private FundingMechanism mechanism;

    // Funding amount range
    private ProjectScale projectScale;

    // WHO benefits (can specify multiple)
    private Set<BeneficiaryPopulation> beneficiaries;

    // WHAT TYPE of organization receives it
    private RecipientOrganizationType recipientType;

    // Language preferences (future translation support)
    private QueryLanguage userLanguage;          // User's preferred language
    private Set<QueryLanguage> searchLanguages;  // Languages to search in
}
```

### Backward Compatibility

**Feature 004 code continues to work unchanged**:

```java
// Feature 004 code (still works)
QueryGenerationRequest request = QueryGenerationRequest.builder()
    .searchEngine(SearchEngineType.TAVILY)
    .category(FundingSearchCategory.STEM_EDUCATION)
    .geographicScope(GeographicScope.BULGARIA)
    .numberOfQueries(3)
    .build();
```

**Feature 005 multi-dimensional queries**:

```java
// Feature 005 - British Centre Burgas building loan
QueryGenerationRequest request = QueryGenerationRequest.builder()
    .searchEngine(SearchEngineType.TAVILY)
    .category(FundingSearchCategory.INFRASTRUCTURE_FACILITIES)
    .geographicScope(GeographicScope.BULGARIA)
    .sourceType(FundingSourceType.GOVERNMENT_REGIONAL)
    .mechanism(FundingMechanism.LOAN)
    .projectScale(ProjectScale.MEDIUM)
    .recipientType(RecipientOrganizationType.PRIVATE_LANGUAGE_SCHOOL)
    .numberOfQueries(3)
    .build();
```

### Example Use Cases

**Use Case 1: British Centre Burgas Building Purchase**
```java
QueryGenerationRequest.builder()
    .category(INFRASTRUCTURE_FACILITIES)
    .sourceType(GOVERNMENT_REGIONAL)
    .mechanism(LOAN)
    .projectScale(MEDIUM)              // €50k-€250k
    .recipientType(PRIVATE_LANGUAGE_SCHOOL)
    .geographicScope(BULGARIA)
    .build();
```

**Use Case 2: NorthStar Foundation Scholarships**
```java
QueryGenerationRequest.builder()
    .category(SCHOLARSHIPS_FINANCIAL_AID)
    .mechanism(SCHOLARSHIP)
    .projectScale(SMALL)                // €5k-€50k
    .beneficiaries(Set.of(
        LOW_INCOME_FAMILIES,
        FIRST_GENERATION_STUDENTS
    ))
    .recipientType(INDIVIDUAL_STUDENT)
    .geographicScope(BULGARIA)
    .build();
```

---

## CategoryMapper Enhancement

### Current State (Feature 004)

```java
public List<String> getKeywords(FundingSearchCategory category) {
    // Returns keywords for single category only
}
```

### Enhanced Design (Feature 005)

```java
public class CategoryMapper {

    public List<String> getKeywords(QueryGenerationRequest request) {
        Set<String> keywords = new HashSet<>();

        // REQUIRED: Always add category keywords
        keywords.addAll(getCategoryKeywords(request.getCategory()));

        // OPTIONAL: Add keywords from populated dimensions
        if (request.getSourceType() != null) {
            keywords.addAll(getSourceTypeKeywords(request.getSourceType()));
        }

        if (request.getMechanism() != null) {
            keywords.addAll(getMechanismKeywords(request.getMechanism()));
        }

        if (request.getProjectScale() != null) {
            keywords.addAll(getProjectScaleKeywords(request.getProjectScale()));
        }

        if (request.getBeneficiaries() != null) {
            request.getBeneficiaries().forEach(b ->
                keywords.addAll(getBeneficiaryKeywords(b))
            );
        }

        if (request.getRecipientType() != null) {
            keywords.addAll(getRecipientTypeKeywords(request.getRecipientType()));
        }

        // Convert Set to List for LLM prompt construction
        return new ArrayList<>(keywords);
    }

    // Private helper methods for each dimension
    private List<String> getCategoryKeywords(FundingSearchCategory category) { ... }
    private List<String> getSourceTypeKeywords(FundingSourceType type) { ... }
    private List<String> getMechanismKeywords(FundingMechanism mechanism) { ... }
    private List<String> getProjectScaleKeywords(ProjectScale scale) { ... }
    private List<String> getBeneficiaryKeywords(BeneficiaryPopulation population) { ... }
    private List<String> getRecipientTypeKeywords(RecipientOrganizationType type) { ... }
}
```

### Keyword Generation Strategy

**Concatenative gathering with automatic deduplication**:

1. Each dimension produces 3-8 keywords
2. Keywords added to HashSet (automatic deduplication)
3. Result converted to List for LLM prompt

**Example: British Centre Burgas building loan**

```
Input dimensions:
- category: INFRASTRUCTURE_FACILITIES
- sourceType: GOVERNMENT_REGIONAL
- mechanism: LOAN
- projectScale: MEDIUM
- recipientType: PRIVATE_LANGUAGE_SCHOOL

Keywords generated:
  ["infrastructure", "building", "facilities", "construction",    // category
   "government", "regional", "municipal",                         // sourceType
   "loan", "financing", "credit",                                 // mechanism
   "medium scale", "50k-250k", "moderate funding",                // projectScale
   "language school", "private education", "training center"]     // recipientType

LLM receives: Combined keyword list for precise query generation
```

**Design Validation**: If queries are too narrow or miss opportunities, we can adjust keyword breadth per dimension.

---

## Scheduler Design

### New Module: northstar-scheduler

**Maven Module Structure**:
```xml
<artifactId>northstar-scheduler</artifactId>
<dependencies>
    <dependency>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-query-generation</artifactId>
    </dependency>
</dependencies>
```

### DailyScheduleService

```java
@Service
public class DailyScheduleService {

    private final QueryGenerationService queryGenerationService;
    private final int queriesPerNight;  // Configured in application.yml

    public DailyScheduleService(QueryGenerationService queryGenerationService,
                               @Value("${scheduler.queries-per-night:20}") int queriesPerNight) {
        this.queryGenerationService = queryGenerationService;
        this.queriesPerNight = queriesPerNight;
    }

    /**
     * Generates query combinations for today's schedule.
     * Returns 20-25 QueryGenerationRequest objects based on fixed schedule.
     */
    public List<QueryGenerationRequest> generateDailyBatch() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        return generateBatchForDay(today);
    }

    private List<QueryGenerationRequest> generateBatchForDay(DayOfWeek day) {
        List<QueryGenerationRequest> batch = new ArrayList<>();

        switch (day) {
            case MONDAY -> batch.addAll(generateGovernmentSTEMQueries());
            case TUESDAY -> batch.addAll(generateFoundationsLanguagesQueries());
            case WEDNESDAY -> batch.addAll(generateScholarshipsMultilateralQueries());
            case THURSDAY -> batch.addAll(generateInfrastructureBilateralQueries());
            case FRIDAY -> batch.addAll(generateVocationalCorporateQueries());
            case SATURDAY -> batch.addAll(generateEarlyChildhoodCommunityQueries());
            case SUNDAY -> batch.addAll(generateArtsResearchQueries());
        }

        return batch.subList(0, Math.min(batch.size(), queriesPerNight));
    }

    // Day-specific query generation methods
    private List<QueryGenerationRequest> generateGovernmentSTEMQueries() {
        // Monday: Government sources + STEM categories
        // Returns ~20-25 combinations
    }

    // ... similar methods for other days
}
```

### Fixed Weekly Schedule

| Day | Source Types | Categories | Expected Queries |
|-----|-------------|------------|------------------|
| **Monday** | GOVERNMENT_NATIONAL, GOVERNMENT_EU, GOVERNMENT_REGIONAL | STEM_EDUCATION, DIGITAL_LITERACY, ENVIRONMENTAL_EDUCATION | 20-25 |
| **Tuesday** | PRIVATE_FOUNDATION, COMMUNITY_FOUNDATION | LANGUAGE_EDUCATION, FOREIGN_LANGUAGES, MULTICULTURAL_EDUCATION | 20-25 |
| **Wednesday** | MULTILATERAL_ORG, BILATERAL_AID | SCHOLARSHIPS_FINANCIAL_AID, TEACHER_TRAINING | 20-25 |
| **Thursday** | GOVERNMENT_REGIONAL, BILATERAL_AID | INFRASTRUCTURE_FACILITIES, ACCESSIBILITY_INCLUSION | 20-25 |
| **Friday** | CORPORATE_FOUNDATION, EDUCATION_ASSOCIATION | VOCATIONAL_TRAINING, ENTREPRENEURSHIP, CAREER_EDUCATION | 20-25 |
| **Saturday** | COMMUNITY_FOUNDATION, RELIGIOUS_FOUNDATION | EARLY_CHILDHOOD_EDUCATION, PARENT_ENGAGEMENT | 20-25 |
| **Sunday** | PRIVATE_FOUNDATION, CROWDFUNDING_PLATFORM | ARTS_CULTURE, EDUCATIONAL_RESEARCH | 20-25 |

### CLI Trigger (Manual for Development)

```java
@Component
public class SchedulerCLI implements CommandLineRunner {

    private final DailyScheduleService scheduleService;

    @Override
    public void run(String... args) {
        if (args.length > 0 && "run-daily-batch".equals(args[0])) {
            List<QueryGenerationRequest> batch = scheduleService.generateDailyBatch();
            System.out.println("Generated " + batch.size() + " queries for today");
            // Process batch...
        }
    }
}
```

**Usage**:
```bash
mvn spring-boot:run -pl northstar-scheduler -Dspring-boot.run.arguments="run-daily-batch"
```

### Configuration (application.yml)

```yaml
scheduler:
  queries-per-night: 20
  geographic-scopes:
    - BULGARIA
    - EASTERN_EUROPE
    - EU
  search-engines:
    - TAVILY
    - SEARXNG
```

---

## Testing Strategy

### Unit Tests

**New Enum Tests** (6 test classes):
```java
FundingSourceTypeTest.java
FundingMechanismTest.java
ProjectScaleTest.java              // Test min/max amount ranges
BeneficiaryPopulationTest.java
RecipientOrganizationTypeTest.java
QueryLanguageTest.java             // Test language codes, native names
```

**CategoryMapper Tests**:
```java
@Test
void getKeywords_WithMultipleDimensions_CombinesAllKeywords() {
    QueryGenerationRequest request = QueryGenerationRequest.builder()
        .category(INFRASTRUCTURE_FACILITIES)
        .sourceType(GOVERNMENT_REGIONAL)
        .mechanism(LOAN)
        .projectScale(MEDIUM)
        .recipientType(PRIVATE_LANGUAGE_SCHOOL)
        .build();

    List<String> keywords = categoryMapper.getKeywords(request);

    assertThat(keywords).contains("infrastructure", "government", "loan",
                                  "medium scale", "language school");
}

@Test
void getKeywords_WithOnlyCategory_ReturnsBackwardCompatibleKeywords() {
    QueryGenerationRequest request = QueryGenerationRequest.builder()
        .category(STEM_EDUCATION)
        .build();

    List<String> keywords = categoryMapper.getKeywords(request);

    assertThat(keywords).contains("STEM", "science", "technology");
}
```

**Scheduler Tests**:
```java
@Test
void generateDailyBatch_Monday_ReturnsGovernmentSTEMQueries() {
    // Test fixed schedule for each day
}

@Test
void generateDailyBatch_RespectsQueriesPerNightLimit() {
    List<QueryGenerationRequest> batch = scheduleService.generateDailyBatch();
    assertThat(batch).hasSizeLessThanOrEqualTo(20);
}
```

### Integration Tests (Future)

**Query Generation with Multi-Dimensional Requests**:
- Test LM Studio generates queries using enhanced keywords
- Verify query quality with 2+ dimensions vs single dimension
- Validate backward compatibility with Feature 004 tests

---

## Implementation Plan Summary

1. **Domain Module**: Add 6 new enums (66 values)
2. **Query Generation**: Enhance CategoryMapper + update QueryGenerationRequest
3. **Scheduler Module**: Create new module with DailyScheduleService
4. **Unit Tests**: 80+ new tests for enums, mapper, scheduler
5. **Configuration**: Add scheduler config to application.yml
6. **Integration Tests**: Validate multi-dimensional query generation

---

## Success Criteria

✅ **Backward Compatibility**: Feature 004 tests pass unchanged
✅ **Multi-Dimensional Queries**: CategoryMapper handles 0-7 optional dimensions
✅ **Fixed Schedule**: Scheduler generates 20-25 queries per day (manual trigger)
✅ **Enum Validation**: All enum values tested, ProjectScale ranges validated
✅ **Query Quality**: Manual review shows multi-dimensional queries are more precise

---

## Future Extensibility

### Easy to Add Later:
- New enum values (just add to enum, keywords to CategoryMapper)
- Translation service implementation (QueryLanguage enum already structured)
- Automatic cron jobs (scheduler architecture supports it)
- Advanced scheduling (Spring Batch integration)
- Admin UI for schedule configuration

### Phase 5+ Features:
- User profile matching against taxonomy
- RAG-based query optimization
- Multi-language query translation
- Geographic expansion beyond Eastern Europe

---

## Related Documents

- **Session Summary**: `northstar-notes/session-summaries/2025-11-04-feature-005-brainstorming-in-progress.md`
- **Team Context**: `northstar-notes/project/team-and-expertise.md`
- **Origin Story**: `northstar-notes/project/the-origin-story.md`
- **Future Roadmap**: `northstar-notes/project/future-roadmap-extensibility.md`
- **Taxonomy Research**: `northstar-notes/inbox/grant-taxonomy-research-2025-11-04.md`
- **Ubiquitous Language**: `northstar-notes/inbox/ubiquitous-language-funding-domain.md`

---

**Design Status**: ✅ APPROVED - Ready for Implementation
**Next Step**: Use `superpowers:using-git-worktrees` to set up development branch
**Estimated Implementation Time**: 2-3 development sessions
**Risk Level**: Low (backward compatible, validate-through-implementation approach)
