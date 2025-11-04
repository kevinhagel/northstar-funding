# Feature 005: Enhanced Multi-Dimensional Taxonomy & Weekly Scheduling

**Status**: Draft for Review
**Created**: 2025-11-03
**Author**: Claude (based on SpringCrawler lessons & real funding source analysis)

## Executive Summary

Feature 004 delivered AI-powered query generation with basic 25-category taxonomy. However, analysis of real funding sources reveals this is **insufficient** for systematic coverage.

This feature adds **multi-dimensional taxonomy** to enable:
1. **Systematic Coverage**: Weekly cycle ensuring all funder types searched
2. **Intelligent Targeting**: Match queries to appropriate funding mechanisms and scales
3. **Gap Analysis**: Know what types of funders we're missing
4. **Scalable Growth**: Support 100+ funding source types without chaos

**Lesson from SpringCrawler**: "AI-generated queries" without structured taxonomy leads to random coverage and inability to answer "what are we missing?"

## Problem Statement

### Current Limitations (Feature 004)

**One-Dimensional Taxonomy**:
```java
FundingSearchCategory.INFRASTRUCTURE_FUNDING  // Too broad!
GeographicScope.BULGARIA
```

**British Cultural Centre Example**:
- Need: €500K for building purchase
- Our query: "Bulgaria education infrastructure grants"
- Missing: WHO (government? foundation? EU?), HOW MUCH (€500K), MECHANISM (grant vs loan)

**Real World Has Multiple Dimensions**:
1. **WHO**: Government, Foundation, NGO, Corporate, Crowdfunding, etc.
2. **WHAT**: Infrastructure, Equipment, Programs, Research, Operations
3. **HOW**: Grant, Loan, Equity, Crowdfunding, Matching
4. **HOW MUCH**: €10K vs €100K vs €1M vs €10M+
5. **WHEN**: Open, Quarterly, Annual, Invitation-only

### Evidence from Real Funding Sources

From SpringCrawler docs analysis (eastern-europe-education-funding-sources.md):

**70+ distinct funding sources categorized as**:
- **5 EU Programs**: ERDF, ESF+, InvestEU, Horizon Europe, EEA Grants
- **8 Private Foundations**: America for Bulgaria, Mott, Open Society, Velux, TSA, etc.
- **5 Corporate Foundations**: UniCredit, Raiffeisen, Telus, Kaufland, Aurubis
- **4 Multilateral Banks**: EIB, World Bank, CEB, EBRD
- **4 Bilateral Aid**: German BMZ, Swiss SDC, Austrian ADA, Swedish Sida
- **3 Impact Investors**: Reach for Change, SEAF, Eleven Ventures
- **3 Bulgaria-Specific**: HR Development Programme, Bulgarian Development Bank, Workshop for Civic Initiatives
- **6 International NGOs**: Save the Children, World Vision, etc. (from alternative-funding-sources.md)
- **5 Crowdfunding Platforms**: GlobalGiving, Kickstarter, Indiegogo, etc.

**Funding Amounts Range 4 Orders of Magnitude**:
- €500 (DonorSee micro-donations)
- €5,000 - €50,000 (crowdfunding, small NGO grants)
- €100,000 - €1M (private foundations, bilateral aid)
- €5M - €10M (multilateral banks, InvestEU)

**Mechanisms Vary Dramatically**:
- **Grants**: Most foundations, EU programs, NGOs
- **Loans**: EIB, World Bank, CEB, EBRD, Bulgarian Development Bank
- **Equity**: Impact investors (SEAF, Eleven Ventures)
- **Crowdfunding**: GlobalGiving, Kickstarter
- **Recurring**: Patreon, Open Collective

### Why This Matters: The Weekly Cycle Challenge

**Bad Approach** (What SpringCrawler Did):
```
Generate 1000 AI queries randomly
→ Monday: 200 queries hit crowdfunding sites
→ Tuesday: 300 queries hit EU program sites
→ Wednesday: 100 queries hit foundations
→ Result: Random, incomplete coverage
```

**Good Approach** (What We Need):
```
Monday: Search EU/Government (ERDF, ESF+, ministries)
Tuesday: Search Large Foundations (America for Bulgaria, Mott, Velux)
Wednesday: Search Corporate (UniCredit, Raiffeisen, Telus, Kaufland)
Thursday: Search Crowdfunding (GlobalGiving, Kickstarter)
Friday: Search Bilateral Aid (German, Swiss, Austrian, Swedish)
Saturday: Search Impact Investors & Innovation Funds
Sunday: Search Local NGOs & Community Funds
→ Result: Systematic, complete coverage
```

## Proposed Solution

### Multi-Dimensional Taxonomy Model

```
Query Request = f(
  WHAT (FundingSearchCategory),      // Infrastructure, Scholarships, Programs
  WHERE (GeographicScope),            // Bulgaria, EU, Balkans, Global
  WHO (FundingSourceType),            // NEW! Government, Foundation, Corporate
  HOW (FundingMechanism),             // NEW! Grant, Loan, Crowdfunding
  HOW_MUCH (ProjectScale),            // NEW! Micro, Small, Medium, Large, Mega
  WHEN (SearchDay)                    // NEW! Weekly distribution
)
```

### New Taxonomy Dimensions

#### 1. FundingSourceType (WHO provides the funding)

Based on analysis of 70+ real sources from SpringCrawler docs:

```java
public enum FundingSourceType {
    // Government (14 examples found)
    GOVERNMENT_NATIONAL,        // Bulgarian Ministry of Education
    GOVERNMENT_REGIONAL,        // Regional authorities
    GOVERNMENT_LOCAL,           // Municipal governments
    EU_STRUCTURAL_FUNDS,        // ERDF, ESF+
    EU_INNOVATION_FUNDS,        // Horizon Europe, InvestEU
    EU_BILATERAL_PROGRAMS,      // EEA Grants, Norway Grants

    // Foundations (13 examples found)
    PRIVATE_FOUNDATION_INTL,    // America for Bulgaria, Mott, Open Society
    PRIVATE_FOUNDATION_LOCAL,   // Trust for Social Achievement
    CORPORATE_FOUNDATION,       // UniCredit, Raiffeisen, Telus
    FAMILY_FOUNDATION,          // Velux
    COMMUNITY_FOUNDATION,       // Workshop for Civic Initiatives

    // Financial Institutions (8 examples found)
    MULTILATERAL_BANK,          // EIB, World Bank, CEB, EBRD
    DEVELOPMENT_BANK,           // Bulgarian Development Bank
    IMPACT_INVESTOR,            // Reach for Change, SEAF, Eleven Ventures
    VENTURE_FUND,               // EdTech-focused VCs

    // NGOs & International Orgs (6 examples found)
    INTERNATIONAL_NGO,          // Save the Children, World Vision
    LOCAL_NGO,                  // Bulgarian NGOs
    RELIGIOUS_ORGANIZATION,     // Faith-based funders

    // Bilateral & Multilateral (4 examples found)
    BILATERAL_AID_GERMAN,       // BMZ/GIZ/KfW
    BILATERAL_AID_SWISS,        // SDC
    BILATERAL_AID_NORDIC,       // Sida, Norwegian
    BILATERAL_AID_OTHER,        // Austrian ADA, etc.

    // Alternative Funding (5 examples found)
    CROWDFUNDING_PLATFORM,      // GlobalGiving, Kickstarter, Indiegogo
    MEMBERSHIP_PLATFORM,        // Patreon, Open Collective
    CSR_PLATFORM,               // Benevity, B1G1

    // Specialized
    RESEARCH_COUNCIL,           // National research funding
    INNOVATION_ACCELERATOR,     // Startup accelerators
    UNIVERSITY_FUND,            // University-based funding
    DIASPORA_ORGANIZATION       // Bulgarian diaspora funds
}
```

**Rationale**: 27 types cover all 70+ real sources identified in SpringCrawler docs.

#### 2. FundingMechanism (HOW funding is provided)

```java
public enum FundingMechanism {
    GRANT_NON_REPAYABLE,        // Most foundations, EU programs, NGOs
    LOAN_REPAYABLE,             // EIB, World Bank, CEB, EBRD, Development Banks
    LOAN_INTEREST_FREE,         // Some development banks
    EQUITY_INVESTMENT,          // Impact investors, VCs
    CONVERTIBLE_NOTE,           // Some impact investors
    MATCHING_GRANT,             // Requires co-funding
    CHALLENGE_GRANT,            // Competitive prize
    CROWDFUNDING,               // GlobalGiving, Kickstarter
    RECURRING_DONATION,         // Patreon, membership
    REVENUE_SHARING,            // Some social enterprises
    GUARANTEE,                  // Loan guarantees
    SUBSIDY                     // Government subsidies
}
```

#### 3. ProjectScale (HOW MUCH funding needed)

Based on real funding amounts from 70+ sources:

```java
public enum ProjectScale {
    MICRO(      new BigDecimal("0"),       new BigDecimal("10000")),     // €0 - €10K
    SMALL(      new BigDecimal("10000"),   new BigDecimal("100000")),    // €10K - €100K
    MEDIUM(     new BigDecimal("100000"),  new BigDecimal("1000000")),   // €100K - €1M
    LARGE(      new BigDecimal("1000000"), new BigDecimal("5000000")),   // €1M - €5M
    MEGA(       new BigDecimal("5000000"), new BigDecimal("100000000")); // €5M+

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;

    // Constructor, getters
}
```

**Real Examples**:
- **MICRO**: DonorSee (€500-€10K), small crowdfunding
- **SMALL**: NGO grants (€10K-€100K), corporate foundations
- **MEDIUM**: Private foundations (€100K-€1M), bilateral aid
- **LARGE**: EU structural funds (€1M-€5M), large foundations
- **MEGA**: EIB loans (€10M+), World Bank, InvestEU

#### 4. ApplicationCycle (WHEN to apply)

```java
public enum ApplicationCycle {
    OPEN_ROLLING,              // Apply anytime
    QUARTERLY,                 // 4 deadlines/year
    SEMI_ANNUAL,               // 2 deadlines/year
    ANNUAL,                    // 1 deadline/year
    BIENNIAL,                  // Every 2 years
    INVITATION_ONLY,           // By invitation
    CONTINUOUS_COMPETITION,    // Ongoing competition (crowdfunding)
    UNKNOWN                    // Not yet determined
}
```

#### 5. Weekly Distribution Strategy

```java
public enum SearchDay {
    MONDAY,    // EU & National Government Programs
    TUESDAY,   // Large International Foundations
    WEDNESDAY, // Corporate Foundations & CSR
    THURSDAY,  // Crowdfunding & Alternative
    FRIDAY,    // Bilateral Aid Programs
    SATURDAY,  // Impact Investors & Innovation
    SUNDAY     // Local NGOs & Community
}
```

### Updated Domain Model

#### Enhanced SearchQuery Entity

```java
@Table("search_queries")
@Data
@Builder
public class SearchQuery {
    @Id
    private UUID queryId;

    private String queryText;

    // Existing fields
    private SearchEngineType searchEngine;
    private String queryType;  // "keyword" or "ai-optimized"
    private FundingSearchCategory category;
    private GeographicScope geographicScope;

    // NEW FIELDS - Multi-dimensional taxonomy
    private FundingSourceType sourceType;
    private FundingMechanism mechanism;
    private ProjectScale projectScale;
    private ApplicationCycle applicationCycle;
    private SearchDay searchDay;

    // Amount ranges for targeting
    private BigDecimal minFundingAmount;
    private BigDecimal maxFundingAmount;

    // Metadata
    private UUID sessionId;
    private Instant createdAt;
}
```

#### Enhanced QueryGenerationRequest

```java
@Builder
public class QueryGenerationRequest {
    // Existing fields
    private SearchEngineType searchEngine;
    private Set<FundingSearchCategory> categories;
    private GeographicScope geographic;
    private Integer maxQueries;
    private UUID sessionId;

    // NEW FIELDS
    private Set<FundingSourceType> sourceTypes;      // Target specific funder types
    private Set<FundingMechanism> mechanisms;        // Grant vs Loan vs Crowdfunding
    private ProjectScale targetScale;                // How much money needed
    private BigDecimal minAmount;                    // Minimum funding amount
    private BigDecimal maxAmount;                    // Maximum funding amount
    private SearchDay searchDay;                     // For weekly distribution
    private ApplicationCycle preferredCycle;         // Deadline preferences
}
```

### Weekly Distribution Algorithm

```java
public class WeeklySchedulerService {

    public Map<SearchDay, QueryGenerationRequest> distributeWeekly(
            Set<FundingSearchCategory> categories,
            GeographicScope geographic,
            ProjectScale scale) {

        Map<SearchDay, QueryGenerationRequest> schedule = new EnumMap<>(SearchDay.class);

        // MONDAY: EU & Government Programs
        schedule.put(SearchDay.MONDAY, QueryGenerationRequest.builder()
            .categories(categories)
            .geographic(geographic)
            .sourceTypes(Set.of(
                FundingSourceType.EU_STRUCTURAL_FUNDS,
                FundingSourceType.EU_INNOVATION_FUNDS,
                FundingSourceType.GOVERNMENT_NATIONAL
            ))
            .mechanisms(Set.of(FundingMechanism.GRANT_NON_REPAYABLE))
            .targetScale(scale)
            .searchDay(SearchDay.MONDAY)
            .build());

        // TUESDAY: Large International Foundations
        schedule.put(SearchDay.TUESDAY, QueryGenerationRequest.builder()
            .categories(categories)
            .geographic(geographic)
            .sourceTypes(Set.of(
                FundingSourceType.PRIVATE_FOUNDATION_INTL,
                FundingSourceType.FAMILY_FOUNDATION
            ))
            .mechanisms(Set.of(FundingMechanism.GRANT_NON_REPAYABLE, FundingMechanism.MATCHING_GRANT))
            .targetScale(scale)
            .searchDay(SearchDay.TUESDAY)
            .build());

        // ... (continue for all 7 days)

        return schedule;
    }
}
```

### Enhanced Query Generation Strategy

Update PromptTemplates to include new dimensions:

```java
public static final PromptTemplate ENHANCED_KEYWORD_TEMPLATE = PromptTemplate.from("""
    Generate {{numQueries}} search engine queries for finding {{fundingType}} funding sources.

    Target Profile:
    - Funding Source Type: {{sourceType}}
    - Funding Mechanism: {{mechanism}}
    - Geographic Focus: {{geographic}}
    - Funding Amount Range: {{minAmount}} to {{maxAmount}}
    - Application Cycle: {{applicationCycle}}

    Educational Purpose: {{categoryDescription}}

    Examples of target funders:
    {{exampleFunders}}

    Generate SHORT keyword-based queries (3-8 words each) optimized for {{searchEngine}}.
    """);
```

## Data Model Changes

### New Enums to Create

1. **FundingSourceType.java** (27 values)
2. **FundingMechanism.java** (12 values)
3. **ProjectScale.java** (5 values with BigDecimal ranges)
4. **ApplicationCycle.java** (8 values)
5. **SearchDay.java** (7 values)

### Database Migration

```sql
-- V18: Add multi-dimensional taxonomy to search_queries

ALTER TABLE search_queries
ADD COLUMN funding_source_type VARCHAR(50),
ADD COLUMN funding_mechanism VARCHAR(50),
ADD COLUMN project_scale VARCHAR(20),
ADD COLUMN application_cycle VARCHAR(30),
ADD COLUMN search_day VARCHAR(20),
ADD COLUMN min_funding_amount NUMERIC(15,2),
ADD COLUMN max_funding_amount NUMERIC(15,2);

-- Add indexes for common queries
CREATE INDEX idx_search_queries_source_type ON search_queries(funding_source_type);
CREATE INDEX idx_search_queries_mechanism ON search_queries(funding_mechanism);
CREATE INDEX idx_search_queries_scale ON search_queries(project_scale);
CREATE INDEX idx_search_queries_day ON search_queries(search_day);

-- Add composite index for weekly scheduling queries
CREATE INDEX idx_search_queries_weekly
ON search_queries(search_day, funding_source_type, project_scale);
```

### Enhanced Organization & FundingProgram Entities

```java
@Table("organization")
@Data
public class Organization {
    // Existing fields...

    // NEW FIELDS
    private FundingSourceType sourceType;        // What type of funder
    private Set<FundingMechanism> mechanisms;    // Grant, Loan, etc.
    private ProjectScale typicalScale;           // Typical funding size
    private ApplicationCycle applicationCycle;   // When they accept applications
    private SearchDay preferredSearchDay;        // Which day to search for similar orgs
}
```

```java
@Table("funding_program")
@Data
public class FundingProgram {
    // Existing fields...

    // NEW FIELDS
    private FundingMechanism mechanism;          // Grant, Loan, Crowdfunding
    private BigDecimal minAmount;                // Minimum funding
    private BigDecimal maxAmount;                // Maximum funding
    private ProjectScale scale;                  // Calculated from amounts
    private ApplicationCycle cycle;              // Application frequency
    private LocalDate nextDeadline;              // Next application deadline
}
```

## Implementation Plan

### Phase 1: Domain Model Enhancement (Week 1)

**Tasks**:
1. Create 5 new enum classes
2. Add fields to SearchQuery entity
3. Add fields to Organization entity
4. Add fields to FundingProgram entity
5. Create Flyway migration V18
6. Update all existing data with sensible defaults

**Testing**:
- Unit tests for ProjectScale amount ranges
- Integration tests for database schema

### Phase 2: Mapper Enhancement (Week 1)

**Tasks**:
1. Create `SourceTypeMapper` (maps FundingSourceType → example funders)
2. Create `MechanismMapper` (maps FundingMechanism → query context)
3. Create `ScaleMapper` (maps ProjectScale → amount ranges)
4. Update `CategoryMapper` to include source type context
5. Update `GeographicMapper` to include funder examples

**Example**:
```java
@Component
public class SourceTypeMapper {
    public String toExampleFunders(FundingSourceType type) {
        return switch (type) {
            case EU_STRUCTURAL_FUNDS ->
                "European Regional Development Fund (ERDF), European Social Fund Plus (ESF+)";
            case PRIVATE_FOUNDATION_INTL ->
                "America for Bulgaria Foundation, Charles Stewart Mott Foundation, Open Society Foundations";
            case CORPORATE_FOUNDATION ->
                "UniCredit Foundation, Raiffeisen Bank, Telus International";
            case CROWDFUNDING_PLATFORM ->
                "GlobalGiving, Kickstarter, Indiegogo, DonorSee";
            // ... etc for all 27 types
        };
    }

    public String toSearchKeywords(FundingSourceType type) {
        return switch (type) {
            case EU_STRUCTURAL_FUNDS -> "EU structural funds ERDF ESF cohesion policy";
            case PRIVATE_FOUNDATION_INTL -> "international foundation grant philanthropy";
            case CORPORATE_FOUNDATION -> "corporate social responsibility CSR foundation";
            // ... etc
        };
    }
}
```

### Phase 3: Query Generation Enhancement (Week 2)

**Tasks**:
1. Update `PromptTemplates` with multi-dimensional variables
2. Enhance `KeywordQueryStrategy` to use new mappers
3. Enhance `TavilyQueryStrategy` to use new mappers
4. Update `QueryGenerationServiceImpl` to handle new request fields
5. Create `WeeklySchedulerService` for distribution logic

**Example Enhanced Prompt**:
```
Generate 5 search queries for EU structural funds supporting educational infrastructure in Bulgaria.

Target Profile:
- Funder Type: EU Structural Funds (ERDF, ESF+)
- Mechanism: Grant (non-repayable)
- Amount Range: €500,000 - €5,000,000
- Geographic: Bulgaria
- Application: Annual cycles

Educational Purpose: School building purchase and renovation

Example Target Funders:
- European Regional Development Fund (ERDF)
- European Social Fund Plus (ESF+)
- Cohesion Policy programs

Generate SHORT keyword queries (3-8 words) for Brave Search.
```

### Phase 4: Weekly Scheduling (Week 2)

**Tasks**:
1. Create `WeeklySchedulerService`
2. Implement day-to-week distribution algorithm
3. Add scheduling metadata to QueryGenerationRequest
4. Create scheduled batch jobs for each day
5. Add dashboard for weekly coverage monitoring

**Example**:
```java
@Service
public class WeeklySchedulerService {

    public void executeWeeklySchedule(
            FundingSearchCategory category,
            GeographicScope geographic,
            ProjectScale scale) {

        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        SearchDay searchDay = SearchDay.valueOf(dayOfWeek.name());

        QueryGenerationRequest request = buildRequestForDay(
            searchDay, category, geographic, scale
        );

        queryGenerationService.generateQueries(request);
    }

    private QueryGenerationRequest buildRequestForDay(
            SearchDay day,
            FundingSearchCategory category,
            GeographicScope geographic,
            ProjectScale scale) {

        Set<FundingSourceType> sourceTypes = switch (day) {
            case MONDAY -> Set.of(
                FundingSourceType.EU_STRUCTURAL_FUNDS,
                FundingSourceType.EU_INNOVATION_FUNDS,
                FundingSourceType.GOVERNMENT_NATIONAL
            );
            case TUESDAY -> Set.of(
                FundingSourceType.PRIVATE_FOUNDATION_INTL,
                FundingSourceType.FAMILY_FOUNDATION
            );
            case WEDNESDAY -> Set.of(
                FundingSourceType.CORPORATE_FOUNDATION,
                FundingSourceType.CSR_PLATFORM
            );
            // ... etc for all 7 days
        };

        return QueryGenerationRequest.builder()
            .categories(Set.of(category))
            .geographic(geographic)
            .sourceTypes(sourceTypes)
            .targetScale(scale)
            .searchDay(day)
            .build();
    }
}
```

### Phase 5: Testing & Validation (Week 3)

**Tasks**:
1. Integration tests for all new enums
2. Test weekly distribution produces correct coverage
3. Validate LLM prompts with real LM Studio calls
4. Test scale-based query generation (micro vs mega)
5. Test mechanism-based queries (grant vs loan vs crowdfunding)

### Phase 6: Documentation & Migration Guide (Week 3)

**Tasks**:
1. Update quickstart.md with new taxonomy examples
2. Create migration guide for existing data
3. Document weekly scheduling approach
4. Create taxonomy reference guide
5. Add examples for all 27 source types

## Success Criteria

### Functional Requirements

1. **Systematic Coverage**:
   - Each day of week targets distinct funder types
   - Over 7 days, all 27 source types covered
   - No duplicate coverage within single week

2. **Intelligent Targeting**:
   - Queries for €10K needs don't target €10M funders
   - Grant queries don't mix with loan queries
   - Source type clearly reflected in query text

3. **Gap Analysis**:
   - Can query: "Which funder types haven't we searched this week?"
   - Can report: "We have 50 foundations but only 3 impact investors"
   - Can identify: "No BILATERAL_AID_NORDIC sources discovered yet"

4. **LLM Quality**:
   - Enhanced prompts produce more targeted queries
   - Source type examples improve query relevance
   - Scale constraints filter out wrong-sized funders

### Non-Functional Requirements

1. **Backward Compatibility**:
   - Existing 25 FundingSearchCategory values unchanged
   - Existing 15 GeographicScope values unchanged
   - Existing queries continue to work (new fields optional)

2. **Performance**:
   - Query generation time unchanged (< 5 seconds)
   - Database queries use new indexes efficiently
   - Weekly scheduling overhead < 1 second

3. **Maintainability**:
   - Adding new FundingSourceType requires only enum change + mapper
   - Changing weekly distribution = configuration change
   - Clear separation of concerns (mappers, strategies, scheduling)

## Risk Analysis

### Technical Risks

1. **Enum Explosion**: 27 FundingSourceType values is manageable, but could grow
   - **Mitigation**: Use hierarchical naming (BILATERAL_AID_GERMAN vs generic BILATERAL_AID)
   - **Alternative**: Move to database-driven taxonomy (future enhancement)

2. **Prompt Complexity**: Enhanced prompts might confuse LLM
   - **Mitigation**: Test thoroughly with LM Studio, provide clear examples
   - **Fallback**: Keep simple prompts available if enhanced version fails

3. **Data Migration**: Existing SearchQuery records have NULL for new fields
   - **Mitigation**: Provide sensible defaults based on category
   - **Script**: Backfill script to infer source type from query text

### Business Risks

1. **Over-Engineering**: Is 27 source types too granular?
   - **Mitigation**: Start with 15 most common types, expand as needed
   - **Validation**: Review with domain expert (you!) before implementation

2. **Weekly Distribution Too Rigid**: What if need urgent search on "wrong" day?
   - **Mitigation**: Allow manual override of searchDay
   - **Enhancement**: Add "priority search" that runs any day

## Open Questions for Review

1. **Is 27 FundingSourceType values the right number?** Too many? Too few?

2. **Should we merge similar types?** (e.g., BILATERAL_AID_GERMAN + BILATERAL_AID_SWISS = BILATERAL_AID_EUROPEAN?)

3. **Weekly distribution OK?** Or prefer different cycle (daily focus areas?)

4. **Amount ranges in ProjectScale correct?** (Micro=€10K, Small=€100K, Medium=€1M, Large=€5M)

5. **Should Organization.sourceType be single value or Set<>?** (Some orgs have multiple types)

6. **Need new FundingSearchCategory sub-types?** (e.g., INFRASTRUCTURE_BUILDING_PURCHASE vs INFRASTRUCTURE_RENOVATION)

7. **Track rejected source types?** (To avoid researching wrong funders)

## Next Steps

**Before Implementation**:
1. **Review this spec** - Validate taxonomy against real needs
2. **Validate weekly distribution** - Does Mon-Sun breakdown make sense?
3. **Confirm amount ranges** - Are ProjectScale thresholds correct?
4. **Check source type granularity** - 27 types right level?

**After Approval**:
1. Start Phase 1: Domain model enhancement
2. Create comprehensive mapping tables (SourceTypeMapper with all 27 types)
3. Build weekly scheduler
4. Test with real LM Studio
5. Roll out systematically

## Appendix A: Complete Source Type Mapping

### Government & EU (9 types)

| Type | Examples | Typical Amount | Mechanism |
|------|----------|----------------|-----------|
| GOVERNMENT_NATIONAL | Bulgarian Ministry of Education | €100K-€5M | Grant |
| GOVERNMENT_REGIONAL | Regional education authorities | €50K-€500K | Grant |
| GOVERNMENT_LOCAL | Municipal governments | €10K-€100K | Grant |
| EU_STRUCTURAL_FUNDS | ERDF, ESF+ | €500K-€5M | Grant |
| EU_INNOVATION_FUNDS | Horizon Europe, InvestEU | €1M-€5M | Grant/Loan |
| EU_BILATERAL_PROGRAMS | EEA Grants, Norway Grants | €200K-€2M | Grant |

### Foundations (5 types)

| Type | Examples | Typical Amount | Mechanism |
|------|----------|----------------|-----------|
| PRIVATE_FOUNDATION_INTL | America for Bulgaria, Mott, Open Society | €50K-€500K | Grant |
| PRIVATE_FOUNDATION_LOCAL | Trust for Social Achievement | €50K-€300K | Grant |
| CORPORATE_FOUNDATION | UniCredit, Raiffeisen, Telus, Kaufland | €20K-€250K | Grant |
| FAMILY_FOUNDATION | Velux | €100K-€1M | Grant |
| COMMUNITY_FOUNDATION | Workshop for Civic Initiatives | €5K-€50K | Grant |

### Financial Institutions (4 types)

| Type | Examples | Typical Amount | Mechanism |
|------|----------|----------------|-----------|
| MULTILATERAL_BANK | EIB, World Bank, CEB, EBRD | €5M-€100M | Loan |
| DEVELOPMENT_BANK | Bulgarian Development Bank | €500K-€5M | Loan |
| IMPACT_INVESTOR | Reach for Change, SEAF, Eleven | €20K-€2M | Equity/Grant |
| VENTURE_FUND | EdTech VCs | €300K-€10M | Equity |

### NGOs (3 types)

| Type | Examples | Typical Amount | Mechanism |
|------|----------|----------------|-----------|
| INTERNATIONAL_NGO | Save the Children, World Vision | €25K-€250K | Grant |
| LOCAL_NGO | Bulgarian education NGOs | €5K-€50K | Grant |
| RELIGIOUS_ORGANIZATION | Faith-based educational funders | €10K-€100K | Grant |

### Bilateral Aid (4 types)

| Type | Examples | Typical Amount | Mechanism |
|------|----------|----------------|-----------|
| BILATERAL_AID_GERMAN | BMZ, GIZ, KfW | €100K-€2M | Grant/Loan |
| BILATERAL_AID_SWISS | SDC | CHF 100K-CHF 1M | Grant |
| BILATERAL_AID_NORDIC | Sida (Sweden) | SEK 500K-SEK 5M | Grant |
| BILATERAL_AID_OTHER | Austrian ADA, etc. | €50K-€500K | Grant |

### Alternative Funding (3 types)

| Type | Examples | Typical Amount | Mechanism |
|------|----------|----------------|-----------|
| CROWDFUNDING_PLATFORM | GlobalGiving, Kickstarter, Indiegogo | €5K-€50K | Crowdfunding |
| MEMBERSHIP_PLATFORM | Patreon, Open Collective | €500-€5K/month | Recurring |
| CSR_PLATFORM | Benevity, B1G1 | Varies | Donation matching |

**Total: 27 FundingSourceType values covering 70+ real funding sources**

## Appendix B: Weekly Distribution Detail

### Monday: EU & Government Programs
**Focus**: Large-scale, formal, structured funding
**Query Volume**: 20-30 queries
**Target**:
- EU_STRUCTURAL_FUNDS (ERDF, ESF+)
- EU_INNOVATION_FUNDS (Horizon, InvestEU)
- GOVERNMENT_NATIONAL (Ministries)
**Characteristics**: €500K-€5M, formal applications, annual cycles

### Tuesday: Large International Foundations
**Focus**: Mission-driven, grant-based, substantial amounts
**Query Volume**: 20-30 queries
**Target**:
- PRIVATE_FOUNDATION_INTL (America for Bulgaria, Mott, Open Society)
- FAMILY_FOUNDATION (Velux)
**Characteristics**: €50K-€1M, proposal-based, quarterly/annual

### Wednesday: Corporate Foundations & CSR
**Focus**: Business-aligned, community impact, moderate amounts
**Query Volume**: 15-25 queries
**Target**:
- CORPORATE_FOUNDATION (UniCredit, Raiffeisen, Telus, Kaufland)
- CSR_PLATFORM (Benevity)
**Characteristics**: €10K-€250K, align with corporate values

### Thursday: Crowdfunding & Alternative
**Focus**: Public engagement, small amounts, rapid deployment
**Query Volume**: 15-20 queries
**Target**:
- CROWDFUNDING_PLATFORM (GlobalGiving, Kickstarter)
- MEMBERSHIP_PLATFORM (Patreon)
**Characteristics**: €500-€50K, public campaigns, continuous

### Friday: Bilateral Aid Programs
**Focus**: International development, country partnerships
**Query Volume**: 15-20 queries
**Target**:
- BILATERAL_AID_GERMAN, BILATERAL_AID_SWISS, BILATERAL_AID_NORDIC
**Characteristics**: €100K-€2M, government partnerships

### Saturday: Impact Investors & Innovation
**Focus**: Scalable solutions, blended value, equity potential
**Query Volume**: 10-15 queries
**Target**:
- IMPACT_INVESTOR (Reach for Change, SEAF, Eleven)
- VENTURE_FUND (EdTech VCs)
**Characteristics**: €50K-€5M, investment model, scalability focus

### Sunday: Local NGOs & Community
**Focus**: Grassroots, community-driven, smaller scale
**Query Volume**: 10-15 queries
**Target**:
- LOCAL_NGO
- COMMUNITY_FOUNDATION
- INTERNATIONAL_NGO (regional programs)
**Characteristics**: €5K-€100K, community engagement, local impact

**Total Weekly Queries**: 110-150 queries covering all 27 source types systematically
