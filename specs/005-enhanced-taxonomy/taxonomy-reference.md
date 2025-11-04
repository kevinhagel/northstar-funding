# Complete Funding Taxonomy Reference

**Feature 005**: Enhanced Multi-Dimensional Taxonomy
**Purpose**: Quick reference for all taxonomy dimensions and their values

## Quick Visual Overview

```
Funding Discovery Query = f(
  WHAT to fund    (FundingSearchCategory)    25 values  ✓ Already exists
  WHERE to fund   (GeographicScope)          15 values  ✓ Already exists
  WHO funds       (FundingSourceType)        27 values  ✨ NEW
  HOW funded      (FundingMechanism)         12 values  ✨ NEW
  HOW MUCH        (ProjectScale)              5 values  ✨ NEW
  WHEN to apply   (ApplicationCycle)          8 values  ✨ NEW
  SEARCH DAY      (SearchDay)                 7 values  ✨ NEW
)
```

## Dimension 1: WHAT (FundingSearchCategory) - EXISTING

**25 categories** organized by beneficiary and purpose:

### Student Support (4)
- `INDIVIDUAL_SCHOLARSHIPS` - Individual student financial aid
- `STUDENT_FINANCIAL_AID` - Broader student support programs
- `TEACHER_SCHOLARSHIPS` - Scholarships for teachers
- `ACADEMIC_FELLOWSHIPS` - Research and academic fellowships

### Programs & Activities (7)
- `PROGRAM_GRANTS` - General program funding
- `CURRICULUM_DEVELOPMENT` - Curriculum design and materials
- `AFTER_SCHOOL_PROGRAMS` - Extracurricular programs
- `SUMMER_PROGRAMS` - Summer educational programs
- `EXTRACURRICULAR_ACTIVITIES` - Sports, arts, clubs
- `PILOT_PROGRAMS` - Experimental/pilot initiatives
- `INNOVATION_GRANTS` - Educational innovation projects

### Infrastructure & Resources (3)
- `INFRASTRUCTURE_FUNDING` - Buildings, facilities, construction
- `TECHNOLOGY_EQUIPMENT` - Computers, software, tech infrastructure
- `LIBRARY_RESOURCES` - Books, media, library materials

### Human Capital (3)
- `TEACHER_DEVELOPMENT` - Teacher training and professional development
- `PROFESSIONAL_TRAINING` - Staff professional development
- `ADMINISTRATIVE_CAPACITY` - Administrative systems and training

### Specialized Education (4)
- `STEM_EDUCATION` - Science, technology, engineering, math
- `ARTS_EDUCATION` - Arts, music, drama programs
- `SPECIAL_NEEDS_EDUCATION` - Special education and accessibility
- `LANGUAGE_PROGRAMS` - Language learning programs

### Community & Research (4)
- `COMMUNITY_PARTNERSHIPS` - Community engagement programs
- `PARENT_ENGAGEMENT` - Parent involvement initiatives
- `NGO_EDUCATION_PROJECTS` - NGO-led education projects
- `EDUCATION_RESEARCH` - Educational research and studies

## Dimension 2: WHERE (GeographicScope) - EXISTING

**15 scopes** from local to global:

### Country-Specific (5)
- `BULGARIA` - Bulgaria
- `ROMANIA` - Romania
- `GREECE` - Greece
- `SERBIA` - Serbia
- `NORTH_MACEDONIA` - North Macedonia

### Regional (7)
- `EASTERN_EUROPE` - Eastern European region
- `BALKANS` - Balkan peninsula
- `SOUTHEASTERN_EUROPE` - Southeastern Europe
- `CENTRAL_EUROPE` - Central European region
- `EU_MEMBER_STATES` - European Union members
- `EU_CANDIDATE_COUNTRIES` - EU candidate countries
- `EU_ENLARGEMENT_REGION` - EU expansion region

### Broad (2)
- `EUROPE` - All of Europe
- `INTERNATIONAL` - Global/worldwide

### Special (1)
- `GLOBAL` - No geographic restriction

## Dimension 3: WHO (FundingSourceType) - NEW ✨

**27 types** organized by sector:

### Government & EU Programs (6)
```
GOVERNMENT_NATIONAL         National education ministries
GOVERNMENT_REGIONAL         Regional/provincial authorities
GOVERNMENT_LOCAL            Municipal governments
EU_STRUCTURAL_FUNDS         ERDF, ESF+ structural programs
EU_INNOVATION_FUNDS         Horizon Europe, InvestEU
EU_BILATERAL_PROGRAMS       EEA Grants, Norway Grants
```

**Examples**:
- Bulgarian Ministry of Education (GOVERNMENT_NATIONAL)
- ERDF Cohesion Policy (EU_STRUCTURAL_FUNDS)
- Horizon Europe (EU_INNOVATION_FUNDS)

### Foundations (5)
```
PRIVATE_FOUNDATION_INTL     International private foundations
PRIVATE_FOUNDATION_LOCAL    Local/national private foundations
CORPORATE_FOUNDATION        Corporate/bank foundations
FAMILY_FOUNDATION           Family-based foundations
COMMUNITY_FOUNDATION        Community/grassroots foundations
```

**Examples**:
- America for Bulgaria Foundation (PRIVATE_FOUNDATION_INTL)
- UniCredit Foundation (CORPORATE_FOUNDATION)
- Velux Foundation (FAMILY_FOUNDATION)
- Trust for Social Achievement (PRIVATE_FOUNDATION_LOCAL)

### Financial Institutions (4)
```
MULTILATERAL_BANK           EIB, World Bank, CEB, EBRD
DEVELOPMENT_BANK            National/regional development banks
IMPACT_INVESTOR             Social impact investors
VENTURE_FUND                EdTech venture capital
```

**Examples**:
- European Investment Bank (MULTILATERAL_BANK)
- Bulgarian Development Bank (DEVELOPMENT_BANK)
- SEAF Bulgaria (IMPACT_INVESTOR)
- Eleven Ventures (VENTURE_FUND)

### NGOs & International Organizations (3)
```
INTERNATIONAL_NGO           Global NGOs (Save the Children, etc.)
LOCAL_NGO                   National/regional NGOs
RELIGIOUS_ORGANIZATION      Faith-based educational funders
```

**Examples**:
- Save the Children (INTERNATIONAL_NGO)
- World Vision (INTERNATIONAL_NGO)
- Bulgarian education NGOs (LOCAL_NGO)

### Bilateral Aid Programs (4)
```
BILATERAL_AID_GERMAN        German BMZ, GIZ, KfW programs
BILATERAL_AID_SWISS         Swiss SDC programs
BILATERAL_AID_NORDIC        Swedish Sida, Norwegian programs
BILATERAL_AID_OTHER         Austrian ADA, other bilateral
```

**Examples**:
- German BMZ/GIZ (BILATERAL_AID_GERMAN)
- Swiss Development Cooperation (BILATERAL_AID_SWISS)
- Swedish Sida (BILATERAL_AID_NORDIC)

### Alternative & Emerging (5)
```
CROWDFUNDING_PLATFORM       GlobalGiving, Kickstarter, Indiegogo
MEMBERSHIP_PLATFORM         Patreon, Open Collective
CSR_PLATFORM                Benevity, B1G1
RESEARCH_COUNCIL            National research funding bodies
INNOVATION_ACCELERATOR      Startup accelerators, innovation labs
```

**Examples**:
- GlobalGiving (CROWDFUNDING_PLATFORM)
- Patreon (MEMBERSHIP_PLATFORM)
- Benevity (CSR_PLATFORM)

### Specialized (2)
```
UNIVERSITY_FUND             University-based funding programs
DIASPORA_ORGANIZATION       Diaspora community funds
```

## Dimension 4: HOW (FundingMechanism) - NEW ✨

**12 mechanisms** by repayment obligation:

### Non-Repayable (3)
```
GRANT_NON_REPAYABLE         Traditional grant, no repayment
MATCHING_GRANT              Requires co-funding/match
CHALLENGE_GRANT             Competitive prize/award
```

### Repayable (4)
```
LOAN_REPAYABLE              Standard loan with interest
LOAN_INTEREST_FREE          Zero-interest loan
GUARANTEE                   Loan guarantee (not direct funding)
SUBSIDY                     Government subsidy
```

### Investment (2)
```
EQUITY_INVESTMENT           Ownership stake for funding
CONVERTIBLE_NOTE            Debt converting to equity
```

### Alternative (3)
```
CROWDFUNDING                Public crowdfunding campaign
RECURRING_DONATION          Ongoing membership support
REVENUE_SHARING             Profit-sharing agreement
```

## Dimension 5: HOW MUCH (ProjectScale) - NEW ✨

**5 scales** with amount ranges:

```
MICRO       €0 - €10,000           Small donations, micro-grants
SMALL       €10,000 - €100,000     NGO grants, small foundation grants
MEDIUM      €100,000 - €1,000,000  Private foundations, bilateral aid
LARGE       €1M - €5M              EU structural funds, large foundations
MEGA        €5M+                   Multilateral banks, mega infrastructure
```

### Scale-to-Source Mapping

| Scale | Typical Sources |
|-------|-----------------|
| MICRO | CROWDFUNDING_PLATFORM, small LOCAL_NGO grants |
| SMALL | CORPORATE_FOUNDATION, LOCAL_NGO, INTERNATIONAL_NGO |
| MEDIUM | PRIVATE_FOUNDATION_INTL, BILATERAL_AID programs |
| LARGE | EU_STRUCTURAL_FUNDS, MULTILATERAL_BANK (smaller loans) |
| MEGA | MULTILATERAL_BANK, EU_INNOVATION_FUNDS (InvestEU) |

## Dimension 6: WHEN (ApplicationCycle) - NEW ✨

**8 cycles** by deadline frequency:

```
OPEN_ROLLING               Apply anytime, no specific deadline
QUARTERLY                  4 application windows per year
SEMI_ANNUAL                2 deadlines per year
ANNUAL                     1 deadline per year
BIENNIAL                   Every 2 years
INVITATION_ONLY            By invitation or partnership only
CONTINUOUS_COMPETITION     Ongoing (crowdfunding, competitive)
UNKNOWN                    Not yet determined
```

### Cycle-to-Source Mapping

| Cycle | Typical Sources |
|-------|-----------------|
| OPEN_ROLLING | Some NGOs, development banks (loan applications) |
| QUARTERLY | Many private foundations |
| SEMI_ANNUAL | Some bilateral aid programs |
| ANNUAL | Most EU programs, many foundations |
| BIENNIAL | Large EU structural funds (programming periods) |
| INVITATION_ONLY | Prestigious foundations, some corporate programs |
| CONTINUOUS | Crowdfunding platforms, membership platforms |

## Dimension 7: SearchDay - NEW ✨

**7-day weekly distribution** for systematic coverage:

```
MONDAY      EU & Government Programs
TUESDAY     Large International Foundations
WEDNESDAY   Corporate Foundations & CSR
THURSDAY    Crowdfunding & Alternative Funding
FRIDAY      Bilateral Aid Programs
SATURDAY    Impact Investors & Innovation Funds
SUNDAY      Local NGOs & Community Foundations
```

### Day-to-Source Mapping

| Day | Target Source Types | Expected Volume |
|-----|---------------------|-----------------|
| MONDAY | EU_STRUCTURAL_FUNDS, EU_INNOVATION_FUNDS, GOVERNMENT_NATIONAL | 20-30 queries |
| TUESDAY | PRIVATE_FOUNDATION_INTL, FAMILY_FOUNDATION | 20-30 queries |
| WEDNESDAY | CORPORATE_FOUNDATION, CSR_PLATFORM | 15-25 queries |
| THURSDAY | CROWDFUNDING_PLATFORM, MEMBERSHIP_PLATFORM | 15-20 queries |
| FRIDAY | BILATERAL_AID_* (all 4 types) | 15-20 queries |
| SATURDAY | IMPACT_INVESTOR, VENTURE_FUND, INNOVATION_ACCELERATOR | 10-15 queries |
| SUNDAY | LOCAL_NGO, COMMUNITY_FOUNDATION, INTERNATIONAL_NGO | 10-15 queries |

## Complete Multi-Dimensional Examples

### Example 1: British Cultural Centre Building Purchase

**Scenario**: Need €500,000 to buy and refurbish a building

**Taxonomy Profile**:
```
WHAT:    INFRASTRUCTURE_FUNDING
WHERE:   BULGARIA
WHO:     EU_STRUCTURAL_FUNDS, MULTILATERAL_BANK, PRIVATE_FOUNDATION_INTL
HOW:     GRANT_NON_REPAYABLE (prefer) or LOAN_REPAYABLE
SCALE:   MEDIUM (€500K falls in €100K-€1M range)
WHEN:    ANNUAL or BIENNIAL (typical for this scale)
DAY:     MONDAY (EU/Government) or TUESDAY (Foundations)
```

**Targeted Sources**:
- Monday: ERDF structural funds, national ministry programs
- Tuesday: Velux Foundation, America for Bulgaria Foundation

**Query Examples**:
- "Bulgaria education building purchase ERDF structural fund"
- "international foundation school facility grant Bulgaria"
- "EIB education infrastructure loan Bulgaria €500000"

### Example 2: Teacher Training Program

**Scenario**: Need €25,000 for teacher professional development

**Taxonomy Profile**:
```
WHAT:    TEACHER_DEVELOPMENT
WHERE:   BULGARIA
WHO:     CORPORATE_FOUNDATION, INTERNATIONAL_NGO, BILATERAL_AID_OTHER
HOW:     GRANT_NON_REPAYABLE
SCALE:   SMALL (€25K in €10K-€100K range)
WHEN:    QUARTERLY or ANNUAL
DAY:     WEDNESDAY (Corporate) or FRIDAY (Bilateral)
```

**Targeted Sources**:
- Wednesday: UniCredit Foundation, Raiffeisen Bank CSR
- Friday: Austrian Development Agency, Swiss SDC

**Query Examples**:
- "corporate foundation teacher training grant Bulgaria"
- "bilateral aid teacher development Bulgaria Austria Swiss"

### Example 3: Student Scholarship Crowdfunding

**Scenario**: Need €5,000 for 10 disadvantaged student scholarships

**Taxonomy Profile**:
```
WHAT:    INDIVIDUAL_SCHOLARSHIPS
WHERE:   BULGARIA
WHO:     CROWDFUNDING_PLATFORM, MEMBERSHIP_PLATFORM
HOW:     CROWDFUNDING, RECURRING_DONATION
SCALE:   MICRO (€5K under €10K threshold)
WHEN:    CONTINUOUS_COMPETITION
DAY:     THURSDAY (Crowdfunding day)
```

**Targeted Sources**:
- Thursday: GlobalGiving, DonorSee, Patreon

**Query Examples**:
- "Bulgaria student scholarship crowdfunding GlobalGiving"
- "education grant disadvantaged students Bulgaria donate"

### Example 4: Large Infrastructure Innovation Project

**Scenario**: Need €3M for innovative school campus

**Taxonomy Profile**:
```
WHAT:    INFRASTRUCTURE_FUNDING, INNOVATION_GRANTS
WHERE:   BULGARIA
WHO:     EU_INNOVATION_FUNDS, MULTILATERAL_BANK
HOW:     GRANT_NON_REPAYABLE, LOAN_REPAYABLE
SCALE:   LARGE (€3M in €1M-€5M range)
WHEN:    ANNUAL, BIENNIAL
DAY:     MONDAY (EU programs) or SATURDAY (Innovation)
```

**Targeted Sources**:
- Monday: InvestEU, Horizon Europe infrastructure calls
- Saturday: European Investment Bank innovation loans

**Query Examples**:
- "InvestEU Bulgaria education innovation infrastructure €3M"
- "Horizon Europe school campus innovative design grant"
- "EIB education infrastructure loan Bulgaria €3 million"

## Taxonomy Validation Matrix

### Coverage Check: All 27 Source Types

| Source Type | Day Searched | Mechanism | Typical Scale | Example Query |
|-------------|--------------|-----------|---------------|---------------|
| GOVERNMENT_NATIONAL | Monday | GRANT | MEDIUM-LARGE | "Bulgarian ministry education grant" |
| EU_STRUCTURAL_FUNDS | Monday | GRANT | LARGE | "ERDF Bulgaria school infrastructure" |
| PRIVATE_FOUNDATION_INTL | Tuesday | GRANT | MEDIUM | "international foundation education grant Bulgaria" |
| CORPORATE_FOUNDATION | Wednesday | GRANT | SMALL-MEDIUM | "corporate CSR education funding Bulgaria" |
| CROWDFUNDING_PLATFORM | Thursday | CROWDFUNDING | MICRO-SMALL | "crowdfund school project Bulgaria" |
| BILATERAL_AID_GERMAN | Friday | GRANT | MEDIUM | "German GIZ education Bulgaria" |
| IMPACT_INVESTOR | Saturday | EQUITY/GRANT | SMALL-MEDIUM | "impact investment edtech Bulgaria" |
| LOCAL_NGO | Sunday | GRANT | SMALL | "Bulgarian NGO education grant" |

*(Full 27-row matrix available in appendix)*

## Implementation Checklist

### Taxonomy Completeness

- [ ] All 27 FundingSourceType values mapped to real examples
- [ ] All 12 FundingMechanism values mapped to use cases
- [ ] All 5 ProjectScale ranges validated against real funders
- [ ] All 8 ApplicationCycle values mapped to source types
- [ ] All 7 SearchDay values have source type assignments

### Mapper Completeness

- [ ] SourceTypeMapper returns example funders for all 27 types
- [ ] SourceTypeMapper returns search keywords for all 27 types
- [ ] MechanismMapper handles all 12 mechanisms
- [ ] ScaleMapper converts amounts to correct scale
- [ ] ApplicationCycleMapper maps cycles to source types

### Query Generation Completeness

- [ ] Enhanced prompts include all dimensions
- [ ] Prompts tested for each of 7 search days
- [ ] Prompts tested for all mechanisms (grant, loan, crowdfunding, etc.)
- [ ] Prompts tested for all scales (micro €500 to mega €50M)
- [ ] LLM generates appropriate queries for each combination

### Weekly Distribution Completeness

- [ ] Monday schedule covers all government/EU types
- [ ] Tuesday schedule covers all foundation types
- [ ] Wednesday schedule covers all corporate types
- [ ] Thursday schedule covers all alternative funding types
- [ ] Friday schedule covers all bilateral aid types
- [ ] Saturday schedule covers all investor/innovation types
- [ ] Sunday schedule covers all NGO/community types

### Data Quality Completeness

- [ ] Example queries validated for each source type
- [ ] Real funding sources mapped to taxonomy
- [ ] Amount ranges reflect actual funder capabilities
- [ ] Application cycles match real deadlines
- [ ] Geographic scopes align with funder focus areas

## Quick Reference Card

```
┌─────────────────────────────────────────────────────┐
│  FUNDING TAXONOMY QUICK REFERENCE                   │
├─────────────────────────────────────────────────────┤
│  WHAT (25): Infrastructure, Scholarships, Programs  │
│  WHERE (15): Bulgaria, EU, Balkans, Global          │
│  WHO (27): Government, Foundation, NGO, Corporate   │
│  HOW (12): Grant, Loan, Equity, Crowdfunding        │
│  SCALE (5): Micro €10K, Small €100K, Medium €1M     │
│  WHEN (8): Rolling, Quarterly, Annual, Invitation   │
│  DAY (7): Mon=EU, Tue=Foundations, Wed=Corporate    │
└─────────────────────────────────────────────────────┘

Weekly Cycle:
  Mon → EU & Government (€500K-€5M grants)
  Tue → Int'l Foundations (€50K-€1M grants)
  Wed → Corporate CSR (€20K-€250K grants)
  Thu → Crowdfunding (€500-€50K campaigns)
  Fri → Bilateral Aid (€100K-€2M grants)
  Sat → Impact/Innovation (€50K-€5M equity/grant)
  Sun → NGO/Community (€5K-€100K grants)

Total Coverage: 110-150 queries/week across all 27 types
```

## Appendix: Full Source Type Examples

*(See spec.md Appendix A for complete 70+ source mapping)*
