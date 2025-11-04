# Feature 004 Complete - Taxonomy Gap Analysis and Integration Planning

**Date**: 2025-11-03
**Context**: Post-Feature 004 completion, planning Feature 005 integration

## What We've Accomplished (Features 001-004)

### ✅ Feature 001-002: Foundation
- **Domain Model**: 19 entities (FundingSourceCandidate, Domain, Organization, etc.)
- **Persistence Layer**: 9 repositories + 5 services with Spring Data JDBC
- **Database Schema**: 17 Flyway migrations (PostgreSQL 16)
- **Test Coverage**: 110 unit tests (Mockito)

### ✅ Feature 003: Search Infrastructure
- **SearchProviderConfig**: 4 search engines (BRAVE, SERPER, SEARXNG, TAVILY)
- **Domain-Level Deduplication**: Prevents reprocessing same domains
- **Anti-Spam Filtering**: Confidence scoring >= 0.60 threshold
- **AdminUser**: System administrators for session tracking

### ✅ Feature 004: AI Query Generation (JUST COMPLETED)
- **LM Studio Integration**: LangChain4j + llama-3.1-8b-instruct
- **Multi-Strategy**: Keyword (3-8 words) vs AI-optimized (15-30 words)
- **Caffeine Caching**: 24h TTL with statistics
- **Virtual Threads**: Java 25 parallel execution
- **25 FundingSearchCategories**: Basic categories defined
- **15 GeographicScopes**: Geographic focus areas
- **53 Tests**: 34 passing integration/mapper tests

## The Taxonomy Problem You've Identified

### Current Limitation: Oversimplified Taxonomy

Our **25 FundingSearchCategories** are too basic:
```java
INDIVIDUAL_SCHOLARSHIPS,
STUDENT_FINANCIAL_AID,
TEACHER_SCHOLARSHIPS,
ACADEMIC_FELLOWSHIPS,
PROGRAM_GRANTS,
CURRICULUM_DEVELOPMENT,
AFTER_SCHOOL_PROGRAMS,
SUMMER_PROGRAMS,
EXTRACURRICULAR_ACTIVITIES,
INFRASTRUCTURE_FUNDING,      // ← Too broad!
TECHNOLOGY_EQUIPMENT,
LIBRARY_RESOURCES,
TEACHER_DEVELOPMENT,
PROFESSIONAL_TRAINING,
ADMINISTRATIVE_CAPACITY,
STEM_EDUCATION,
ARTS_EDUCATION,
SPECIAL_NEEDS_EDUCATION,
LANGUAGE_PROGRAMS,
COMMUNITY_PARTNERSHIPS,
PARENT_ENGAGEMENT,
NGO_EDUCATION_PROJECTS,
EDUCATION_RESEARCH,
PILOT_PROGRAMS,
INNOVATION_GRANTS
```

### Real-World Example: British Cultural Centre Burgas

**Funding Need**: Buy and refurbish a building for a school

**What kinds of funding sources apply?**
1. **Infrastructure Grants**
   - Building purchase
   - Renovation/refurbishment
   - Accessibility improvements
   - Energy efficiency upgrades

2. **Equipment Grants**
   - Classroom furniture
   - Technology/computers
   - Library resources
   - Lab equipment

3. **Operational Grants**
   - Teacher training
   - Curriculum development
   - Administrative systems

4. **Program Grants**
   - After-school programs
   - Community engagement
   - Special education programs

### Missing Taxonomy Dimensions

Our current model lacks:

#### 1. **Funding Source Type** (CRITICAL - NOT IN OUR MODEL!)
- **Government**: National ministries, local authorities, regional governments
- **EU/International**: ERDF, ESF+, Horizon Europe, EEA Grants
- **Foundations**: Private (America for Bulgaria, Open Society, Mott, Velux)
- **NGOs**: Save the Children, World Vision, Roma Education Fund
- **Corporations**: CSR programs, Benevity, B1G1
- **Crowdfunding**: GlobalGiving, Kickstarter, Indiegogo
- **Membership**: Patreon, Open Collective
- **Financial Institutions**: InvestEU, EIB loans

#### 2. **Funding Purpose/Use Case** (PARTIALLY COVERED)
- **Physical Infrastructure**: Buildings, renovation, construction
- **Equipment**: Technology, furniture, materials
- **Human Capital**: Salaries, training, recruitment
- **Programs**: Curriculum, events, activities
- **Operations**: Administration, utilities, maintenance
- **Research**: Studies, pilots, innovation

#### 3. **Project Scale** (NOT IN OUR MODEL!)
- **Micro**: €500 - €10,000 (crowdfunding, small grants)
- **Small**: €10,000 - €100,000 (foundation grants, NGO grants)
- **Medium**: €100,000 - €1M (private foundations, bilateral aid)
- **Large**: €1M - €5M (EU programs, multilateral)
- **Mega**: €5M+ (InvestEU, major infrastructure)

#### 4. **Funding Mechanism** (NOT IN OUR MODEL!)
- **Grant**: Non-repayable
- **Loan**: Repayable with interest
- **Equity**: Investment for ownership
- **Matching**: Requires co-funding
- **Crowdfunding**: Public contribution
- **Membership**: Recurring support

#### 5. **Target Beneficiary** (PARTIALLY COVERED)
- **Students**: By age, socioeconomic status, disability
- **Teachers**: Professional development, salaries
- **Schools**: Institution-level support
- **Communities**: Broader community benefit
- **Marginalized Groups**: Roma, refugees, disadvantaged

#### 6. **Application Cycle** (NOT IN OUR MODEL!)
- **Open**: Rolling applications
- **Quarterly**: 4 deadlines/year
- **Semi-Annual**: 2 deadlines/year
- **Annual**: 1 deadline/year
- **Invitation-Only**: By invitation or partnership

## Evidence from SpringCrawler Docs

### From `alternative-funding-sources.md`:
**Funding Source Types Identified**:
- International NGOs (6 examples)
- Crowdfunding Platforms (5 platforms)
- Membership Models (2 platforms)
- Corporate CSR (2 platforms)

**Funding Amounts Range**:
- Micro: €500 - €10,000 (DonorSee, Patreon monthly)
- Small: €5,000 - €100,000 (GlobalGiving, NGOs)
- Medium: €100,000 - €500,000 (foundations)

### From `eastern-europe-education-funding-sources.md`:
**EU Programs**:
- ERDF: €500K - €5M (infrastructure)
- ESF+: €100K - €1M (programs)
- InvestEU: €5M+ (loans)
- Horizon: €1M - €5M (innovation)
- EEA Grants: €200K - €2M (bilateral)

**Private Foundations**:
- America for Bulgaria: $50K - $500K
- Mott: $75K - $250K
- Open Society: $25K - $200K
- Velux: €100K - €1M

### From `AI-SITE-CLASSIFICATION-SUMMARY.md`:
**AI Classification Score 1-10**:
- Score 9-10: Premium funding sites (extract 15 URLs)
- Score 7-8: High-value (10 URLs)
- Score 5-6: Review needed (3 URLs)
- Score 1-4: Auto-reject

**This shows you were already thinking about quality/relevance scoring!**

## The Weekly Cycle Challenge

You mentioned: **"Queries must be intelligently grouped for 7 days of the week"**

### Why This Matters
If we generate 1000 queries randomly:
- Some days we'd crawl only crowdfunding sites
- Other days only EU programs
- Inefficient use of crawling resources
- Poor coverage of funding spectrum

### Proposed Weekly Taxonomy Distribution

**Monday**: **Government & EU Institutional**
- National ministries
- Regional authorities
- EU programs (ERDF, ESF+, Horizon)
- Bilateral aid programs

**Tuesday**: **Large Foundations & International NGOs**
- America for Bulgaria
- Open Society
- Mott Foundation
- Save the Children
- World Vision

**Wednesday**: **Corporate & CSR Programs**
- Corporate foundations
- CSR platforms (Benevity)
- Business-education partnerships
- Technology company programs

**Thursday**: **Crowdfunding & Alternative Funding**
- GlobalGiving
- Kickstarter/Indiegogo
- DonorSee
- Ulule

**Friday**: **Specialized/Niche Programs**
- Roma Education Fund
- Faith-based organizations
- Diaspora foundations
- Local community funds

**Saturday**: **Research & Innovation Funding**
- Horizon Europe
- Research foundations
- Innovation grants
- Pilot program funding

**Sunday**: **Membership & Recurring Support**
- Patreon-style platforms
- Open Collective
- Recurring donation programs
- Alumni/community giving

## Critical Gap: We're Missing the "Source Type" Dimension

### Current Data Model
```
search_queries table:
- query_id
- query_text
- search_engine_type (BRAVE, SERPER, etc.)
- query_type (keyword, ai-optimized)
- category (FundingSearchCategory enum)
- geographic_scope (GeographicScope enum)
- session_id
- created_at
```

### What's Missing
```sql
-- We need a funding_source_type dimension!
funding_source_type VARCHAR(50)  -- GOVERNMENT, FOUNDATION, NGO, CORPORATE, CROWDFUNDING, etc.

-- We need project scale/amount ranges!
min_funding_amount NUMERIC(15,2)
max_funding_amount NUMERIC(15,2)

-- We need funding mechanism!
funding_mechanism VARCHAR(30)  -- GRANT, LOAN, EQUITY, MATCHING, etc.

-- We need application cycle!
application_cycle VARCHAR(30)  -- OPEN, QUARTERLY, ANNUAL, INVITATION, etc.
```

## Proposed Solution: Enhanced Taxonomy

### New Enum: `FundingSourceType`
```java
public enum FundingSourceType {
    GOVERNMENT_NATIONAL,
    GOVERNMENT_REGIONAL,
    GOVERNMENT_LOCAL,
    EU_PROGRAM,
    BILATERAL_AID,
    PRIVATE_FOUNDATION,
    CORPORATE_FOUNDATION,
    INTERNATIONAL_NGO,
    LOCAL_NGO,
    RELIGIOUS_ORGANIZATION,
    CROWDFUNDING_PLATFORM,
    MEMBERSHIP_PLATFORM,
    FINANCIAL_INSTITUTION,
    MULTILATERAL_BANK,
    COMMUNITY_FOUNDATION,
    DIASPORA_ORGANIZATION,
    UNIVERSITY_RESEARCH_FUND,
    INNOVATION_FUND,
    VENTURE_PHILANTHROPY,
    IMPACT_INVESTOR
}
```

### New Enum: `FundingMechanism`
```java
public enum FundingMechanism {
    GRANT_NON_REPAYABLE,
    LOAN_REPAYABLE,
    EQUITY_INVESTMENT,
    MATCHING_GRANT,
    CHALLENGE_GRANT,
    CROWDFUNDING,
    RECURRING_MEMBERSHIP,
    PRIZE_COMPETITION,
    SOCIAL_IMPACT_BOND,
    REVENUE_SHARING
}
```

### New Enum: `ProjectScale`
```java
public enum ProjectScale {
    MICRO,      // < €10K
    SMALL,      // €10K - €100K
    MEDIUM,     // €100K - €1M
    LARGE,      // €1M - €5M
    MEGA        // > €5M
}
```

### Enhanced `QueryGenerationRequest`
```java
@Builder
public class QueryGenerationRequest {
    private SearchEngineType searchEngine;
    private Set<FundingSearchCategory> categories;
    private GeographicScope geographic;

    // NEW FIELDS:
    private Set<FundingSourceType> sourceTypes;     // Which types to search
    private Set<FundingMechanism> mechanisms;        // Grant vs loan vs crowdfunding
    private ProjectScale targetScale;                // How much money needed
    private DayOfWeek searchDay;                     // For weekly distribution

    private Integer maxQueries;
    private UUID sessionId;
}
```

## Integration Planning: How It All Fits Together

### Feature 005: Enhanced Taxonomy & Weekly Scheduling

**Phase 1: Expand Domain Model**
- Add FundingSourceType enum
- Add FundingMechanism enum
- Add ProjectScale enum
- Add DayOfWeek scheduling to Organization/FundingProgram
- Migrate existing data with sensible defaults

**Phase 2: Update Query Generation**
- Enhance CategoryMapper to include source type context
- Update PromptTemplates to incorporate funding mechanism
- Add weekly scheduling logic to QueryGenerationService
- Create source-type-specific query strategies

**Phase 3: Crawler Integration**
- Connect QueryGenerationService → Search → Crawler pipeline
- Implement weekly batch scheduling
- Add funding amount extraction in crawler
- Tag discovered sources with FundingSourceType

**Phase 4: Judging & Classification**
- Use AI to classify discovered sources by type
- Extract funding amount ranges from content
- Identify application cycles/deadlines
- Build confidence scores per taxonomy dimension

## Why SpringCrawler Failed (Your Original Insight)

From the docs and your comment:
> "This failed because of the poor consistency of design"

**The Problem**:
- AI-generated queries without structured taxonomy
- No systematic coverage (some days all crowdfunding, other days all EU)
- No funding source type differentiation
- Queries generated randomly without strategic distribution
- Couldn't answer: "What funding sources are we missing?"

**The Solution (What We're Building)**:
- **Structured taxonomy** with multiple dimensions
- **Weekly distribution** ensuring complete coverage
- **Source type tagging** enabling gap analysis
- **Scale-aware** searching (don't search for €5M when need is €10K)
- **Mechanism-aware** (don't search loans when need grants)

## Next Steps Recommendation

1. **Create Feature 005 Spec**: "Enhanced Taxonomy & Weekly Scheduling"
2. **Domain Expansion**: Add 3 new enums (FundingSourceType, Mechanism, Scale)
3. **Update Query Generation**: Support multi-dimensional taxonomy
4. **Weekly Scheduler**: Distribute queries across 7-day cycle
5. **Crawler Integration**: Connect all the pieces end-to-end

## Questions to Resolve

1. **How many FundingSourceType values?** (I proposed 20, too many?)
2. **Should we track funding amount ranges in SearchQuery table?**
3. **Do we need sub-categories for INFRASTRUCTURE_FUNDING?** (building purchase, renovation, energy efficiency, etc.)
4. **Should DayOfWeek scheduling be at Organization level or FundingProgram level?**
5. **Do we need a separate "target beneficiary" dimension?** (students, teachers, schools, communities)

## Summary

**We have**:
- Solid foundation (domain, persistence, search, query generation)
- Basic 25 categories
- 15 geographic scopes
- AI integration working

**We're missing**:
- **Funding Source Type taxonomy** (CRITICAL!)
- **Funding Mechanism** (grant vs loan vs crowdfunding)
- **Project Scale/Amount** tracking
- **Application Cycle** tracking
- **Weekly distribution** logic
- **Gap analysis** capability

**The fix**: Feature 005 to add multi-dimensional taxonomy and intelligent weekly scheduling.

This is exactly what you learned from SpringCrawler - **taxonomy is everything!**
