# Session Summary: Feature 005 Brainstorming - IN PROGRESS

**Date**: 2025-11-04
**Feature**: 005 - Enhanced Taxonomy & Basic Scheduler
**Status**: 🚧 BRAINSTORMING IN PROGRESS (Phase 3 of 6)
**Branch**: Not yet created (will be `005-enhanced-taxonomy`)

---

## Session Context

This session is using the **brainstorming skill** to design Feature 005 before implementation.

### What We've Completed So Far:

✅ **Phase 1: Understanding** (COMPLETE)
- Clarified scope: Taxonomy enums + basic scheduler (not full automation)
- Confirmed 20-25 queries per night for Mac Studio
- Fixed daily schedule approach (Monday=Gov+STEM, Tuesday=Foundations, etc.)
- Manual CLI trigger for development
- Core + extensible enum design (start focused, easy to add)

✅ **Phase 2: Exploration** (COMPLETE)
- Architecture decision: Scheduler generates combinations, CategoryMapper handles keywords
- Clean separation of concerns between scheduling and query generation
- New module: `northstar-scheduler` (separate from query-generation)

🚧 **Phase 3: Design Presentation** (IN PROGRESS - 3 of 6 sections complete)
- ✅ Section 1: Overall architecture validated
- ✅ Section 2: Enum design validated (with Perplexity feedback incorporated)
- ✅ Section 3: Multi-dimensional QueryGenerationRequest validated
- ⏸️ **PAUSED HERE** - About to present Section 4: CategoryMapper enhancement

---

## Design Decisions Made

### 1. Enum Values (66 total)

#### FundingSourceType (12 values)
```
GOVERNMENT_NATIONAL
GOVERNMENT_EU
GOVERNMENT_REGIONAL
PRIVATE_FOUNDATION
CORPORATE_FOUNDATION
BILATERAL_AID
MULTILATERAL_ORG
COMMUNITY_FOUNDATION
EDUCATION_ASSOCIATION
CROWDFUNDING_PLATFORM
RELIGIOUS_FOUNDATION          ← Added from Perplexity feedback
CREDIT_UNION_OR_BANK          ← Added from Perplexity feedback
```

#### FundingMechanism (8 values)
```
GRANT
LOAN
SCHOLARSHIP
FELLOWSHIP
MATCHING_GRANT
PRIZE_AWARD
IN_KIND_DONATION
SUBSIDY                       ← Added from Perplexity feedback
```

#### ProjectScale (5 values)
```
MICRO       (< €5k)
SMALL       (€5k - €50k)
MEDIUM      (€50k - €250k)
LARGE       (€250k - €1M)
MEGA        (> €1M)
```

#### BeneficiaryPopulation (18 values)
```
LOW_INCOME_FAMILIES
RURAL_COMMUNITIES
ETHNIC_MINORITIES
GIRLS_WOMEN
CHILDREN_AGES_4_12
ADOLESCENTS_AGES_13_18
FIRST_GENERATION_STUDENTS
AT_RISK_YOUTH
PEOPLE_WITH_DISABILITIES
REFUGEES_IMMIGRANTS
LANGUAGE_MINORITIES
EARLY_CHILDHOOD_0_5
ADULTS_LIFELONG_LEARNING
EDUCATORS_TEACHERS
GENERAL_POPULATION
LGBTQ_PLUS                    ← Added from Perplexity feedback
VETERANS                      ← Added from Perplexity feedback
ELDERLY                       ← Added from Perplexity feedback
```

#### RecipientOrganizationType (14 values)
```
PRIVATE_LANGUAGE_SCHOOL
K12_PRIVATE_SCHOOL
K12_PUBLIC_SCHOOL
PRESCHOOL_EARLY_CHILDHOOD
EXAMINATION_CENTER
NGO_EDUCATION_FOCUSED
NGO_SOCIAL_SERVICES
FOR_PROFIT_EDUCATION
UNIVERSITY_PUBLIC
MUNICIPALITY
INDIVIDUAL_EDUCATOR
INDIVIDUAL_STUDENT
RESEARCH_INSTITUTE            ← Added from Perplexity feedback
LIBRARY_OR_CULTURAL_CENTER    ← Added from Perplexity feedback
```

#### FundingSearchCategory (30 values)
Existing 25 values + 5 new:
```
EARLY_CHILDHOOD_EDUCATION
ADULT_EDUCATION
VOCATIONAL_TRAINING
EDUCATIONAL_TECHNOLOGY
ARTS_CULTURE                  ← Added from Perplexity feedback
```

#### QueryLanguage (9 values)
```
BULGARIAN("bg", "български")
ENGLISH("en", "English")
GERMAN("de", "Deutsch")
ROMANIAN("ro", "română")
FRENCH("fr", "français")      ← Added from Perplexity feedback
RUSSIAN("ru", "русский")      ← Added from Perplexity feedback
GREEK("el", "ελληνικά")       ← Added from Perplexity feedback
TURKISH("tr", "Türkçe")       ← Added from Perplexity feedback
SERBIAN("sr", "српски")       ← Added from Perplexity feedback
```

### 2. Multi-Dimensional QueryGenerationRequest

**Enhanced with 7 optional dimensions** (backward compatible with Feature 004):

```java
public class QueryGenerationRequest {
    // Existing (Feature 004) - REQUIRED
    @NonNull private SearchEngineType searchEngine;
    @NonNull private FundingSearchCategory category;
    @NonNull private GeographicScope geographicScope;
    private Integer numberOfQueries;

    // NEW (Feature 005) - OPTIONAL
    private FundingSourceType sourceType;
    private FundingMechanism mechanism;
    private ProjectScale projectScale;
    private Set<BeneficiaryPopulation> beneficiaries;
    private RecipientOrganizationType recipientType;
    private QueryLanguage userLanguage;
    private Set<QueryLanguage> searchLanguages;
}
```

**Example Use Cases Validated**:
- British Centre Burgas building loan (LOAN, MEDIUM scale, PRIVATE_LANGUAGE_SCHOOL)
- NorthStar Foundation scholarships (SCHOLARSHIP, SMALL scale, LOW_INCOME_FAMILIES)

### 3. Architecture Decisions

**New Module**: `northstar-scheduler`
- Separate from `northstar-query-generation`
- Contains scheduling logic, batch orchestration
- Future: Can add Spring Batch, Quartz without affecting query generation

**Scheduler Approach**: Fixed daily schedule
- Simple configuration (Monday=Gov+STEM, Tuesday=Foundations+Languages, etc.)
- 20-25 queries per night (configurable via application.yml)
- Manual CLI trigger for development

**CategoryMapper Enhancement**:
- Enhanced to generate keywords from multiple dimensions
- Remains stateless
- Scheduler generates combinations → CategoryMapper produces keywords

---

## What's Next (When Resuming)

### Section 4: CategoryMapper Enhancement
**Need to present**:
- How CategoryMapper will handle multi-dimensional keyword generation
- Example: "government loan infrastructure Bulgaria medium scale private school"
- Strategy for combining keywords from multiple dimensions

### Section 5: Scheduler Service Design
**Need to present**:
- DailyScheduleService structure
- Fixed weekly schedule configuration
- CLI trigger interface
- How it generates 20-25 QueryGenerationRequest objects

### Section 6: Testing Strategy
**Need to present**:
- Unit tests for new enums and CategoryMapper
- Integration tests for scheduler
- Backward compatibility tests

---

## Remaining Brainstorming Phases

📋 **Phase 4: Design Documentation** (Not started)
- Write complete design to `docs/plans/2025-11-04-feature-005-enhanced-taxonomy-design.md`
- Commit to git

📋 **Phase 5: Worktree Setup** (Not started)
- Use `superpowers:using-git-worktrees` skill
- Create branch `005-enhanced-taxonomy`
- Set up isolated workspace

📋 **Phase 6: Planning Handoff** (Not started)
- Use `superpowers:writing-plans` skill
- Create detailed implementation plan in worktree
- Break down into tasks

---

## Key Quotes & Context from Session

### On Scheduler Volume
Kevin: "In the spring crawler our ai-generated queries and prompts produced a large number of results, only so many given a sufficient confidence level to proceed with processing. Note that funding candidates discovered with sufficient confidence levels will have at least one more processing state before being written to the database -- we need to crawl the robots.txt and any sitemaps. We will be fastidious in our use of robots.txt. SO, let's start with 20-25 queries per night, we can change that."

### On Bulgarian Grant Scarcity
Kevin: "Remembering from our experience with spring crawler there are not a lot of grants available in Bulgaria at this time"

**Implication**: Need to cast wider net (EU, international, bilateral aid), not just Bulgaria-specific sources.

### On Market Scale
Kevin: "Think -- how many other english-language or other language schools are there in Bulgaria, in the Balkans, in Eastern Europe ... in Poland, Romania, Greece, Serbia, Hungary, Slovakia, Moldova ... the Baltics, central europe ..."

**Market size**: 10,000+ language schools, 15,000+ educational NGOs across Eastern Europe

### On Go-To-Market
Kevin: "these schools, both for-profit and NGOs ... our original customers I think"

**Target market**: Private language schools + educational NGOs are Phase 1 customers

---

## Related Documents Created This Session

1. **northstar-notes/project/team-and-expertise.md**
   - Team member profiles (Kevin, Zlatina, Huw, Gallie, Proletina, Petko)
   - Expertise and how it shapes the platform

2. **northstar-notes/project/the-origin-story.md**
   - Why NorthStar exists (Zlatina's building purchase need)
   - British Centre Burgas + NorthStar Foundation relationship
   - Real-world problem that started it all

3. **northstar-notes/project/future-roadmap-extensibility.md**
   - Complete roadmap through Phase 8
   - What to implement now vs design for later
   - Multilingual strategy, matching engine, RAG search, etc.

4. **northstar-notes/inbox/funding-industry-terminology.md**
   - Industry-standard terminology
   - Funding Source, Recipient, Beneficiary definitions
   - Platform as comprehensive grant aggregator

5. **northstar-notes/inbox/grant-taxonomy-research-2025-11-04.md**
   - 20-page research on grant aggregator taxonomies
   - Analysis of Grants.gov, Candid, EU Portal, etc.

6. **northstar-notes/inbox/ubiquitous-language-funding-domain.md**
   - DDD ubiquitous language for funding domain

---

## How to Resume This Session

### When You Return:

1. **Reference this file**: Read this summary to restore context

2. **Continue Phase 3**: Present Section 4 (CategoryMapper enhancement)

3. **Command to resume brainstorming**:
   ```
   "Let's continue Feature 005 brainstorming. We left off after validating
   the Multi-Dimensional QueryGenerationRequest design. Next section is
   CategoryMapper enhancement."
   ```

4. **Expected flow after resuming**:
   - Present Section 4: CategoryMapper (how multi-dimensional keywords work)
   - Present Section 5: Scheduler Service (DailyScheduleService design)
   - Present Section 6: Testing Strategy
   - Phase 4: Write design document to `docs/plans/`
   - Phase 5: Use `using-git-worktrees` skill to create branch
   - Phase 6: Use `writing-plans` skill to create implementation plan

---

## Current Todo List State

```
✅ Phase 1: Understanding (purpose, constraints, criteria gathered)
✅ Phase 2: Exploration (2-3 approaches proposed and evaluated)
🚧 Phase 3: Design Presentation (design validated in sections) - 3 of 6 sections complete
📋 Phase 4: Design Documentation (design written to docs/plans/)
📋 Phase 5: Worktree Setup (if implementing)
📋 Phase 6: Planning Handoff (if implementing)
```

---

## Git Status (As of Pause)

**Current Branch**: `main`
**Working Directory**: Clean (all session documentation committed)
**Feature Branch**: Not yet created (will be `005-enhanced-taxonomy`)

**Recent Commits**:
- feat: Complete Feature 004 (query generation with LM Studio integration)
- docs: Add team-and-expertise.md
- docs: Add the-origin-story.md
- docs: Add future-roadmap-extensibility.md
- docs: Add funding-industry-terminology.md

---

## Technical Context

**Current State**:
- Feature 004 complete (421 tests passing)
- Query generation service with LM Studio integration working
- 4 search providers integrated (Brave, Serper, SearXNG, Tavily)
- Anti-spam filtering operational
- Domain deduplication working

**Feature 005 Will Add**:
- 6 new enums (66 total values)
- Multi-dimensional query support
- Basic scheduler service with fixed daily schedule
- Enhanced CategoryMapper
- All tests for new functionality

**Not Implementing Yet**:
- Translation service (just QueryLanguage enum structure)
- Automatic cron jobs (manual CLI trigger only)
- Admin dashboard (future)
- User profiles and matching (Phase 5+)

---

**Status**: Ready to resume brainstorming Phase 3, Section 4
**Next Action**: Present CategoryMapper enhancement design
**Estimated Time to Complete Brainstorming**: 30-45 minutes

---

**Last Updated**: 2025-11-04
**Session Paused By**: Kevin Hagel (switching to another project)
**Resume Command**: "Let's continue Feature 005 brainstorming from Section 4"
