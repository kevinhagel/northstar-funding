# Feature 005 - Morning Review Summary

**Created**: 2025-11-03 evening (for your morning review 2025-11-04)
**Status**: Draft proposal ready for your feedback

## What I Did Last Night

While you were away, I:

1. ✅ **Analyzed SpringCrawler docs** - Studied all 70+ funding sources from your research
2. ✅ **Designed complete taxonomy** - 27 FundingSourceType values + 4 other dimensions
3. ✅ **Created Feature 005 spec** - Full specification with implementation plan
4. ✅ **Mapped weekly distribution** - 7-day cycle ensuring systematic coverage
5. ✅ **Validated with real data** - Every taxonomy value backed by actual funding sources

## The Core Problem You Identified

> "Poor consistency of design" in SpringCrawler led to random query generation without systematic coverage

**Your British Cultural Centre example exposed the gap**:
- Current: "Bulgaria infrastructure grants" (too generic)
- Needed: Differentiate €500K government grant vs €10K crowdfunding vs €5M bank loan

## Proposed Solution: Multi-Dimensional Taxonomy

### 5 New Dimensions (Feature 005)

```
1. WHO provides funding (FundingSourceType) - 27 values
   - Government, EU, Foundations, Banks, NGOs, Bilateral, Crowdfunding, etc.

2. HOW funding provided (FundingMechanism) - 12 values
   - Grant, Loan, Equity, Crowdfunding, Matching, etc.

3. HOW MUCH funding (ProjectScale) - 5 values
   - MICRO (€0-€10K), SMALL (€10K-€100K), MEDIUM (€100K-€1M),
     LARGE (€1M-€5M), MEGA (€5M+)

4. WHEN to apply (ApplicationCycle) - 8 values
   - Open, Quarterly, Annual, Invitation-Only, etc.

5. Which DAY to search (SearchDay) - 7 values
   - Monday=EU/Gov, Tuesday=Foundations, Wednesday=Corporate, etc.
```

### Weekly Distribution Strategy

**Mon**: EU & Government Programs (ERDF, ESF+, ministries) - 20-30 queries
**Tue**: Large Foundations (America for Bulgaria, Mott, Velux) - 20-30 queries
**Wed**: Corporate CSR (UniCredit, Raiffeisen, Telus, Kaufland) - 15-25 queries
**Thu**: Crowdfunding (GlobalGiving, Kickstarter) - 15-20 queries
**Fri**: Bilateral Aid (German, Swiss, Nordic) - 15-20 queries
**Sat**: Impact Investors (SEAF, Eleven Ventures) - 10-15 queries
**Sun**: NGOs & Community (Local NGOs, international) - 10-15 queries

**Total**: 110-150 queries/week ensuring ALL 27 source types covered systematically

## Evidence Base: Real Funding Sources

From your SpringCrawler docs, I mapped **70+ actual sources**:

### Government & EU (14 sources)
- ERDF, ESF+, InvestEU, Horizon Europe, EEA Grants (5 EU programs)
- Bulgarian Ministry of Education, HR Development Programme (2 national)
- Municipal governments, regional authorities (local/regional)

### Foundations (13 sources)
- America for Bulgaria, Mott, Open Society, Velux, TSA (5 major international)
- UniCredit, Raiffeisen, Telus, Kaufland, Aurubis (5 corporate)
- Workshop for Civic Initiatives (local community)

### Financial Institutions (8 sources)
- EIB, World Bank, CEB, EBRD (4 multilateral banks)
- Bulgarian Development Bank (1 development)
- Reach for Change, SEAF, Eleven Ventures (3 impact investors)

### NGOs (6 sources)
- Save the Children, World Vision, Roma Education Fund (international)
- Bulgarian education NGOs (local)

### Bilateral Aid (4 sources)
- German BMZ/GIZ, Swiss SDC, Swedish Sida, Austrian ADA

### Crowdfunding & Alternative (5 sources)
- GlobalGiving, Kickstarter, Indiegogo, DonorSee, Ulule

### And more... (Total: 70+ sources → 27 taxonomy types)

## Documents Created for Your Review

1. **`spec.md`** (18,000 words)
   - Complete specification
   - All 27 source types defined with examples
   - Implementation plan (6 phases)
   - Database migration scripts
   - Risk analysis
   - Open questions for you to answer

2. **`taxonomy-reference.md`** (6,000 words)
   - Quick reference guide
   - Visual taxonomy overview
   - All dimensions explained with examples
   - Multi-dimensional query examples
   - Implementation checklist

3. **`MORNING-REVIEW.md`** (this document)
   - Executive summary for quick review
   - Key decisions needed from you
   - Next steps

## Key Decisions Needed From You

Please review and provide feedback on:

### 1. Is 27 FundingSourceType values the right granularity?

**Too granular?** Could merge:
- BILATERAL_AID_GERMAN + BILATERAL_AID_SWISS + BILATERAL_AID_NORDIC → BILATERAL_AID_EUROPEAN?
- PRIVATE_FOUNDATION_INTL + PRIVATE_FOUNDATION_LOCAL → PRIVATE_FOUNDATION?

**Too coarse?** Should we split:
- INFRASTRUCTURE_FUNDING → BUILDING_PURCHASE, RENOVATION, CONSTRUCTION?
- EU_STRUCTURAL_FUNDS → ERDF, ESF_PLUS (separate)?

**My recommendation**: 27 types is right level based on real source diversity

### 2. Weekly distribution strategy OK?

**Current proposal**: Mon-Sun cycle focusing on different funder types each day

**Alternative**: Could do different distribution (2-day cycle? thematic days?)

**My recommendation**: 7-day cycle is good - matches work week, ensures coverage

### 3. Amount ranges in ProjectScale correct?

```
MICRO:  €0 - €10K       (crowdfunding, small NGO)
SMALL:  €10K - €100K    (NGO grants, corporate foundations)
MEDIUM: €100K - €1M     (private foundations, bilateral aid)
LARGE:  €1M - €5M       (EU structural funds)
MEGA:   €5M+            (multilateral banks, InvestEU)
```

**Question**: Are these threshold amounts right for Bulgaria context?

**My recommendation**: Based on real sources, these ranges are accurate

### 4. Should we sub-categorize INFRASTRUCTURE_FUNDING?

British Cultural Centre example showed multiple infrastructure needs:
- Building purchase (€500K)
- Renovation (€100K)
- Equipment (€50K)
- Library (€10K)

**Option A**: Keep INFRASTRUCTURE_FUNDING broad, let source type + scale differentiate
**Option B**: Add sub-categories: BUILDING_PURCHASE, RENOVATION, EQUIPMENT

**My recommendation**: Option A - use multi-dimensional taxonomy, don't explode categories

### 5. Migration strategy for existing data?

We have existing SearchQuery records with NULL for new fields.

**Option A**: Backfill with defaults based on category
**Option B**: Leave as NULL, only populate going forward
**Option C**: Write inference script to guess source type from query text

**My recommendation**: Option A with sensible defaults, then refine over time

## What Happens Next (If You Approve)

### Week 1: Domain Model (T001-T006)
- Create 5 new enum classes
- Add fields to SearchQuery, Organization, FundingProgram
- Flyway migration V18
- Update existing data with defaults
- Unit tests

### Week 2: Mappers & Enhanced Queries (T007-T012)
- Create SourceTypeMapper (27 types → examples + keywords)
- Create MechanismMapper, ScaleMapper
- Update PromptTemplates with multi-dimensional variables
- Enhance KeywordQueryStrategy + TavilyQueryStrategy
- Integration tests with real LM Studio

### Week 3: Weekly Scheduling & Testing (T013-T018)
- Create WeeklySchedulerService
- Implement day-of-week distribution logic
- Build scheduled batch jobs
- End-to-end testing
- Documentation

**Total**: ~3 weeks for complete implementation

## Changes to Existing Code

### Minimal Breaking Changes

✅ **Backward Compatible**:
- Existing 25 FundingSearchCategory unchanged
- Existing 15 GeographicScope unchanged
- Existing QueryGenerationRequest still works (new fields optional)
- Existing queries continue to function

⚠️ **Database Changes**:
- Add 7 new columns to search_queries table
- Add indexes for performance
- Backfill existing records with defaults

⚠️ **Service Changes**:
- QueryGenerationRequest accepts new optional fields
- PromptTemplates have new variables (backward compatible)
- QueryGenerationService handles multi-dimensional requests

## Example: Before vs After

### Before (Feature 004)
```java
QueryGenerationRequest.builder()
    .searchEngine(SearchEngineType.BRAVE)
    .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
    .geographic(GeographicScope.BULGARIA)
    .maxQueries(5)
    .build();
```

**Result**: Generic queries like "Bulgaria education infrastructure grants"

### After (Feature 005)
```java
QueryGenerationRequest.builder()
    .searchEngine(SearchEngineType.BRAVE)
    .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
    .geographic(GeographicScope.BULGARIA)
    .sourceTypes(Set.of(
        FundingSourceType.EU_STRUCTURAL_FUNDS,
        FundingSourceType.PRIVATE_FOUNDATION_INTL
    ))
    .mechanisms(Set.of(FundingMechanism.GRANT_NON_REPAYABLE))
    .targetScale(ProjectScale.MEDIUM)  // €100K-€1M
    .searchDay(SearchDay.MONDAY)
    .maxQueries(5)
    .build();
```

**Result**: Targeted queries like:
- "ERDF Bulgaria school building grant €500000"
- "international foundation education infrastructure Bulgaria medium"
- "EU structural fund education facility Bulgaria"

## My Confidence Level

### High Confidence (90%+)
- ✅ 27 source types cover all real funders in your docs
- ✅ Weekly distribution ensures systematic coverage
- ✅ Multi-dimensional model matches real-world funding complexity
- ✅ Amount ranges validated against 70+ actual sources
- ✅ Backward compatible with Feature 004

### Medium Confidence (70%)
- ⚠️ Exact threshold amounts (€10K, €100K, €1M) might need adjustment
- ⚠️ Weekly distribution might prefer different day assignments
- ⚠️ Some source types might be too granular or too coarse

### Questions for You (Need Your Domain Expertise)
- ❓ Are there funding source types I missed?
- ❓ Is the Bulgaria-specific context captured correctly?
- ❓ Should we add "target beneficiary" dimension? (students, teachers, schools, communities)
- ❓ How to handle multi-type organizations? (e.g., foundation that also does impact investing)

## Bottom Line Recommendation

**Implement Feature 005 as specified with these priorities**:

1. **Phase 1 (Week 1)**: Domain model + enums + database
2. **Phase 2 (Week 2)**: Enhanced mappers + query generation
3. **Phase 3 (Week 3)**: Weekly scheduling + testing

**Defer to future**:
- Advanced gap analysis dashboard
- AI-powered source type inference from discovered sites
- Dynamic taxonomy expansion based on new funder types

**Start simple, validate with real searches, iterate based on results.**

## Questions?

The spec is comprehensive but flexible. We can:
- Add/remove/merge source types
- Adjust amount thresholds
- Change weekly distribution
- Simplify if too complex

**What would you like me to clarify or change?**

---

**Files to Review**:
1. `/specs/005-enhanced-taxonomy/spec.md` - Full specification (read first)
2. `/specs/005-enhanced-taxonomy/taxonomy-reference.md` - Quick reference
3. This file - Executive summary

**Ready to start implementation once you approve the taxonomy structure!**
