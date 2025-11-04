# Grant Aggregator Taxonomy Research

**Date**: 2025-11-04
**Purpose**: Evaluate whether our 25 FundingSearchCategory values are sufficient for comprehensive funding discovery
**Researcher**: Claude Code

---

## Executive Summary

### Key Questions Answered:

1. **Should we limit ourselves to educational funding?**
   - **YES** - This is the right strategic focus for Phase 1
   - Education grants represent a well-defined, searchable niche
   - Major aggregators (Grants.gov, Candid, EU Horizon) have clear education taxonomies
   - We can expand later after proving the model works

2. **Are our 25 categories sufficient?**
   - **MOSTLY YES** - Good coverage of education landscape
   - **GAPS IDENTIFIED**: See recommendations below

3. **What about client/beneficiary categories?**
   - **CRITICAL MISSING DIMENSION** - We need recipient classification
   - NTEE codes show we need organization type taxonomy
   - Population served is a key dimension for targeting

---

## Current State: Our 25 FundingSearchCategory Values

### What We Have (Education-Focused):

```java
// Individual Support (4 categories)
INDIVIDUAL_SCHOLARSHIPS
STUDENT_FINANCIAL_AID
TEACHER_SCHOLARSHIPS
ACADEMIC_FELLOWSHIPS

// Program Support (5 categories)
PROGRAM_GRANTS
CURRICULUM_DEVELOPMENT
AFTER_SCHOOL_PROGRAMS
SUMMER_PROGRAMS
EXTRACURRICULAR_ACTIVITIES

// Infrastructure & Facilities (3 categories)
INFRASTRUCTURE_FUNDING
TECHNOLOGY_EQUIPMENT
LIBRARY_RESOURCES

// Teacher & Staff Development (3 categories)
TEACHER_DEVELOPMENT
PROFESSIONAL_TRAINING
ADMINISTRATIVE_CAPACITY

// STEM & Special Focus (4 categories)
STEM_EDUCATION
ARTS_EDUCATION
SPECIAL_NEEDS_EDUCATION
LANGUAGE_PROGRAMS

// Community & Partnerships (3 categories)
COMMUNITY_PARTNERSHIPS
PARENT_ENGAGEMENT
NGO_EDUCATION_PROJECTS

// Research & Innovation (3 categories)
EDUCATION_RESEARCH
PILOT_PROGRAMS
INNOVATION_GRANTS
```

### Assessment: ✅ **SOLID FOUNDATION**

**Strengths**:
- Clear focus on education sector
- Covers individual → institutional spectrum
- Includes infrastructure, programs, and research
- Well-organized into logical groups

**What This Covers**:
- K-12 education funding
- Higher education grants
- Vocational/professional development
- Educational infrastructure
- Special populations (special needs, language learners)

---

## Major Grant Aggregator Taxonomies Analyzed

### 1. Grants.gov (United States Federal)

**Structure**:
- **Opportunity Category**: Discretionary, Mandatory, Earmark, Continuation
- **Functional Categories**: Organized by government department/function
- **Activity Types**: Research, Services, Training, Other
- **SAM.gov Assistance Listings**: Cross-referenced by functional area

**Key Education Categories at Grants.gov**:
- Elementary and Secondary Education
- Higher Education
- Vocational Education
- Special Education
- Teacher Training
- Educational Research
- STEM Education
- School Improvement
- Student Financial Assistance

**Our Coverage**: ✅ **EXCELLENT** - We map well to Grants.gov education taxonomy

**Gap**: Grants.gov also covers:
- Adult Education (we could add)
- Early Childhood Education (MISSING - see recommendations)
- Literacy Programs (could add as separate category)

---

### 2. EU Cohesion Fund (2021-2027)

**Primary Structure**:
- **Policy Objective 2**: Greener, low-carbon circular economy
- **Policy Objective 3**: More connected Europe (transport infrastructure)

**Focus Areas**:
- Environmental sustainability
- Energy efficiency
- Transport infrastructure (TEN-T networks)
- Technical assistance

**Education Relevance**: ⚠️ **LIMITED**

Cohesion Fund is primarily infrastructure/environment, NOT education-focused.

**What This Means**:
- Cohesion Fund searches should target: INFRASTRUCTURE_FUNDING, TECHNOLOGY_EQUIPMENT
- NOT useful for scholarships, teacher training, or program grants
- Good for: "green school buildings", "educational facility renovations"

**Recommendation**: Use Cohesion Fund ONLY for infrastructure-related queries, not general education funding.

---

### 3. Horizon Europe (2021-2027)

**Three Pillar Structure**:

**Pillar 1 - Excellent Science**:
- European Research Council (ERC) grants
- Marie Skłodowska-Curie Actions (MSCA) - researcher training/mobility
- Research infrastructures

**Pillar 2 - Global Challenges**:
- Health
- Culture, creativity, inclusive society
- Civil security
- Digital, industry, space
- Climate, energy, mobility
- Food, bioeconomy, natural resources

**Pillar 3 - Innovative Europe**:
- EIC Accelerator (SME innovation)
- EIC Pathfinder (high-risk research)
- European innovation ecosystems

**Education Relevance**: ✅ **STRONG for Research & Higher Education**

**Our Coverage**:
- ✅ ACADEMIC_FELLOWSHIPS → Maps to MSCA
- ✅ EDUCATION_RESEARCH → Maps to Pillar 1 & 2
- ✅ INNOVATION_GRANTS → Maps to Pillar 3
- ✅ STEM_EDUCATION → Cross-cutting theme

**Gap**: Horizon Europe heavily focuses on:
- PhD/postdoctoral research (we have ACADEMIC_FELLOWSHIPS - good)
- International researcher mobility (covered by ACADEMIC_FELLOWSHIPS)
- Collaborative research projects (EDUCATION_RESEARCH covers this)

**Assessment**: ✅ Our taxonomy aligns well with Horizon Europe education opportunities.

---

### 4. Candid Philanthropy Classification System (PCS)

**Most Comprehensive Taxonomy Found**

Candid (formerly Foundation Center + GuideStar) maintains the industry-standard taxonomy for philanthropy.

**Three Core Dimensions**:

#### Dimension 1: **Subjects** (WHAT is being supported)
This is where our FundingSearchCategory currently focuses.

**Candid Education Categories** (subset):
- Early childhood education
- Elementary education
- Secondary education
- Higher education
- Adult education
- Vocational education
- Special education
- Gifted student programs
- Arts education
- Science and technology education
- Language education
- Physical education
- Literacy programs
- Educational technology
- Curriculum development
- School administration
- Educational research
- Student services
- Libraries
- Educational facilities and equipment

**Our Coverage vs Candid**:
- ✅ We cover ~18 of their ~25 education subcategories
- ❌ Missing: Early childhood, Adult education, Literacy programs, Gifted programs, Physical education

#### Dimension 2: **Populations** (WHO is being supported) ⚠️ **CRITICAL GAP**

Candid Population Categories:
- Children and youth
- Young adults
- Adults
- Seniors
- Infants
- Toddlers
- Adolescents
- Girls
- Boys
- Women
- Men
- LGBTQ+ community
- People with disabilities
- People with visual impairments
- People with hearing impairments
- Economically disadvantaged people
- Homeless people
- Immigrants and refugees
- Indigenous peoples
- Ethnic and racial minorities
- Religious groups
- Veterans
- Crime victims
- Substance abusers
- Offenders and ex-offenders
- Rural populations
- Urban populations

**WE DON'T HAVE THIS DIMENSION AT ALL** ❌

This is a critical gap for targeting and relevance scoring.

#### Dimension 3: **Organization Type** (WHAT type provides/receives support) ⚠️ **CRITICAL GAP**

Candid Organization Types:
- K-12 schools (public, private, charter)
- Colleges and universities
- Community colleges
- Vocational schools
- Research institutes
- Think tanks
- Foundations (private, community, corporate)
- Government agencies
- International organizations
- Religious organizations
- Advocacy organizations
- Service providers
- Cultural institutions
- Professional associations
- Fiscal sponsors

**WE DON'T HAVE THIS DIMENSION EITHER** ❌

This maps to what you called "categories for the client" - WHO is eligible to apply.

---

### 5. NTEE Codes (National Taxonomy of Exempt Entities)

**IRS Classification System for Nonprofits**

**10 Broad Categories**:
1. Arts, Culture, Humanities (A)
2. **Education (B)** ← Our focus
3. Environment and Animals (C, D)
4. Health (E, F, G, H)
5. Human Services (I-P)
6. International/Foreign Affairs (Q)
7. Public/Societal Benefit (R-W)
8. Religion Related (X)
9. Mutual/Membership Benefit (Y)
10. Unknown (Z)

**Education (B) Breakdown**:
- B01 Alliance/Advocacy Organizations
- B02 Management & Technical Assistance
- B03 Professional Societies/Associations
- B05 Research Institutes and/or Public Policy Analysis
- B11 Single Organization Support
- B12 Fund Raising and/or Fund Distribution
- **B20 Elementary, Secondary Education, K-12**
- **B21 Preschool, Nursery Schools**
- **B24 Primary/Elementary Schools**
- **B25 Secondary/High Schools**
- **B28 Special Education**
- **B29 Charter Schools**
- **B30 Vocational/Technical Schools**
- **B40 Higher Education Institutions**
- **B41 Two-Year Colleges**
- **B42 Undergraduate Colleges (4-year)**
- **B43 University/Technological Institution**
- **B50 Graduate/Professional Schools**
- **B60 Adult/Continuing Education**
- **B70 Libraries**
- **B80 Student Services**
- **B82 Scholarships, Financial Aid**
- **B83 Student Sororities, Fraternities**
- **B84 Alumni Groups**
- **B90 Educational Services**
- **B92 Remedial Reading, Encouragement**
- **B94 Parent/Teacher Groups**
- **B99 Education N.E.C.**

**Our Coverage vs NTEE**:
✅ Good - We cover most NTEE education categories
❌ Gap - NTEE includes "recipient organization type" which we don't have

---

## Other Major Grant Aggregators Identified

### International/Global:

1. **Candid Foundation Directory** (formerly Foundation Center)
   - API: ✅ Available (developer.candid.org)
   - Taxonomy: ✅ Philanthropy Classification System (PCS)
   - Coverage: US + International foundations
   - **Recommendation**: Priority for API integration

2. **GrantForward** (academic/research grants)
   - API: Unknown
   - Coverage: Academic research funding worldwide
   - Focus: Higher education, research institutions

3. **Instrumentl** (grant discovery platform)
   - API: Likely proprietary/subscription
   - Coverage: US nonprofits primarily
   - Focus: Grant matching for nonprofits

4. **GlobalGiving** (international development)
   - API: ✅ Available
   - Coverage: International NGO projects
   - Focus: International development, humanitarian

5. **EU Funding & Tenders Portal** (Funding & Tenders)
   - API: ✅ Available (ec.europa.eu/info/funding-tenders/opportunities/portal)
   - Coverage: All EU funding programs
   - **Recommendation**: Priority for EU grants

### Regional/National:

6. **Foundation Maps** (Candid)
   - Geographic mapping of US foundations
   - API: Part of Candid API suite

7. **Charity Navigator** (US nonprofit ratings)
   - API: Unknown
   - Focus: Nonprofit evaluation, not grant discovery

---

## Critical Gaps Identified

### Gap 1: Missing Beneficiary/Population Dimension ❌ **HIGH PRIORITY**

**Problem**: We can search for "STEM_EDUCATION" but can't specify:
- For girls/women in STEM
- For underrepresented minorities
- For rural communities
- For low-income students
- For veterans
- For refugees/immigrants

**Impact**: Lower relevance scores, missed targeting opportunities

**Recommendation**: Add `BeneficiaryPopulation` enum with ~20-30 values based on Candid PCS.

### Gap 2: Missing Organization/Client Type Dimension ❌ **HIGH PRIORITY**

**Problem**: We can't distinguish funding for:
- Public schools vs private schools
- K-12 vs higher education institutions
- Nonprofit organizations vs government agencies
- Individual applicants vs institutional applicants
- Research universities vs community colleges

**Impact**: Can't filter by eligible applicant type, lower precision

**Recommendation**: Add `RecipientOrganizationType` enum with ~15-20 values based on NTEE + Candid.

### Gap 3: Missing Early Education Categories ⚠️ **MEDIUM PRIORITY**

**Problem**: No dedicated category for:
- Preschool/Pre-K
- Early childhood education (ages 0-5)
- Daycare/childcare funding

**Impact**: Missing significant funding segment (early childhood is heavily funded by foundations)

**Recommendation**: Add 2-3 new categories:
- `EARLY_CHILDHOOD_EDUCATION`
- `PRESCHOOL_PROGRAMS`
- (optional) `CHILDCARE_SUPPORT`

### Gap 4: Missing Adult/Continuing Education ⚠️ **MEDIUM PRIORITY**

**Problem**: No category for:
- Adult literacy programs
- GED/equivalency programs
- Adult vocational training
- Lifelong learning

**Impact**: Missing adult education funding segment

**Recommendation**: Add:
- `ADULT_EDUCATION`
- `LITERACY_PROGRAMS`

### Gap 5: Missing Funding Mechanism Dimension ⚠️ **MEDIUM PRIORITY**

**Problem**: Can't distinguish between:
- Grants (non-repayable)
- Loans (repayable)
- Scholarships (individual, merit/need-based)
- Fellowships (postgraduate research)
- Awards/prizes
- Matching funds
- Equity investments

**Impact**: Some search results will be irrelevant (loans when seeking grants)

**Recommendation**: Add `FundingMechanism` enum (this was already in your Feature 005 planning).

### Gap 6: No Geographic Targeting Granularity 🔵 **LOW PRIORITY**

**Current State**: GeographicScope enum has 15 values (BULGARIA, EASTERN_EUROPE, EU_MEMBER_STATES, etc.)

**Gap**: Can't target city/municipality level (Sofia, Plovdiv, etc.)

**Impact**: Minor - most education grants are regional/national

**Recommendation**: Add city-level codes ONLY if needed for Bulgarian municipal grants.

---

## Recommended Taxonomy Enhancements

### Priority 1: Add Beneficiary Population Enum

```java
public enum BeneficiaryPopulation {
    // Age Groups
    EARLY_CHILDHOOD,        // 0-5 years
    CHILDREN,               // 6-12 years
    ADOLESCENTS,            // 13-17 years
    YOUNG_ADULTS,           // 18-24 years
    ADULTS,                 // 25-64 years
    SENIORS,                // 65+ years

    // Gender
    GIRLS_WOMEN,
    BOYS_MEN,

    // Special Populations
    PEOPLE_WITH_DISABILITIES,
    VISUAL_IMPAIRMENTS,
    HEARING_IMPAIRMENTS,
    LEARNING_DISABILITIES,

    // Socioeconomic
    ECONOMICALLY_DISADVANTAGED,
    LOW_INCOME_FAMILIES,
    HOMELESS_INDIVIDUALS,
    FIRST_GENERATION_STUDENTS,

    // Cultural/Ethnic
    ETHNIC_MINORITIES,
    INDIGENOUS_PEOPLES,
    IMMIGRANTS_REFUGEES,
    INTERNATIONAL_STUDENTS,

    // Geographic
    RURAL_COMMUNITIES,
    URBAN_COMMUNITIES,
    REMOTE_AREAS,

    // Other
    LGBTQ_COMMUNITY,
    VETERANS,
    RELIGIOUS_GROUPS,
    GIFTED_TALENTED_STUDENTS,
    AT_RISK_YOUTH,

    // Universal
    GENERAL_POPULATION      // No specific targeting
}
```

**Usage Example**:
```
FundingSearchCategory: STEM_EDUCATION
BeneficiaryPopulation: GIRLS_WOMEN
→ Search for: "STEM programs for girls", "Women in STEM scholarships"
```

### Priority 2: Add Recipient Organization Type Enum

```java
public enum RecipientOrganizationType {
    // Educational Institutions
    K12_PUBLIC_SCHOOL,
    K12_PRIVATE_SCHOOL,
    K12_CHARTER_SCHOOL,
    COMMUNITY_COLLEGE,
    FOUR_YEAR_COLLEGE,
    RESEARCH_UNIVERSITY,
    VOCATIONAL_SCHOOL,
    GRADUATE_SCHOOL,
    PRESCHOOL,

    // Organization Types
    NONPROFIT_ORGANIZATION,
    GOVERNMENT_AGENCY,
    RELIGIOUS_ORGANIZATION,
    COMMUNITY_BASED_ORGANIZATION,
    RESEARCH_INSTITUTE,
    LIBRARY,
    MUSEUM,

    // Special Cases
    INDIVIDUAL_APPLICANT,
    SCHOOL_DISTRICT,
    CONSORTIUM_PARTNERSHIP,
    FISCAL_SPONSOR,

    // Any
    ANY_ELIGIBLE_ORGANIZATION
}
```

**Usage Example**:
```
FundingSearchCategory: INFRASTRUCTURE_FUNDING
RecipientOrganizationType: K12_PUBLIC_SCHOOL
→ Search for: "Public school building grants", "K-12 facility improvement funding"
```

### Priority 3: Expand FundingSearchCategory with Missing Education Areas

**Add 4 new categories:**

```java
public enum FundingSearchCategory {
    // ... existing 25 categories ...

    // NEW: Early Childhood (2 categories)
    EARLY_CHILDHOOD_EDUCATION,
    PRESCHOOL_PROGRAMS,

    // NEW: Adult Education (2 categories)
    ADULT_EDUCATION,
    LITERACY_PROGRAMS
}
```

**New total**: 29 categories

### Priority 4: Add Funding Mechanism Enum (Already Planned for Feature 005)

```java
public enum FundingMechanism {
    GRANT,                  // Non-repayable gift
    SCHOLARSHIP,            // Individual student support
    FELLOWSHIP,             // Postgraduate/research support
    LOAN,                   // Repayable funding
    AWARD,                  // Prize/recognition + funding
    MATCHING_FUND,          // Requires matching contribution
    EQUITY_INVESTMENT,      // For social enterprises
    IN_KIND_DONATION,       // Non-monetary support
    TECHNICAL_ASSISTANCE    // Consulting/capacity building
}
```

---

## Strategic Recommendations

### Should We Limit to Educational Funding? ✅ **YES**

**Rationale**:
1. **Focus = Success**: Education is a massive, well-defined market
2. **Searchability**: Education keywords are clear and consistent across aggregators
3. **Taxonomy Alignment**: All major aggregators have strong education taxonomies
4. **Proof of Concept**: Prove the model works in one vertical before expanding
5. **Expansion Path**: Later add: Healthcare, Environment, Arts, Social Services

**Evidence**:
- Grants.gov has dedicated education sections
- Candid PCS has robust education taxonomy
- Horizon Europe funds education research heavily
- NTEE "Education (B)" has 30+ subcategories

**Recommendation**: Stay focused on education for 2025. Expand in 2026 after proving ROI.

### Are Our 25 Categories Sufficient? ✅ **MOSTLY**

**Current Assessment**:
- ✅ Strong coverage of K-12 and higher education
- ✅ Good mapping to Grants.gov, Horizon Europe
- ✅ Covers infrastructure, programs, research
- ⚠️ Missing early childhood (0-5 years)
- ⚠️ Missing adult/continuing education

**Recommendation**: Add 4 categories (total 29) to fill gaps:
1. EARLY_CHILDHOOD_EDUCATION
2. PRESCHOOL_PROGRAMS
3. ADULT_EDUCATION
4. LITERACY_PROGRAMS

### What About Client/Beneficiary Categories? ❌ **CRITICAL GAP**

**Current State**: We have ZERO beneficiary/client classification.

**Impact**:
- Can't target "STEM funding for underrepresented minorities"
- Can't filter "Scholarships for first-generation college students"
- Can't search "Rural school infrastructure grants"
- Lower relevance scores
- Missed funding opportunities

**Recommendation**:
1. Add `BeneficiaryPopulation` enum (20-30 values)
2. Add `RecipientOrganizationType` enum (15-20 values)
3. Make both OPTIONAL in QueryGenerationRequest (not all queries need them)
4. Update CategoryMapper to generate population-specific queries

**Example Multi-Dimensional Query**:
```java
QueryGenerationRequest.builder()
    .searchEngine(SearchEngineType.TAVILY)
    .category(FundingSearchCategory.STEM_EDUCATION)
    .beneficiary(BeneficiaryPopulation.GIRLS_WOMEN)
    .recipientType(RecipientOrganizationType.K12_PUBLIC_SCHOOL)
    .geographic(GeographicScope.BULGARIA)
    .mechanism(FundingMechanism.GRANT)
    .build()

→ Generated Query: "STEM education grants for girls in Bulgarian public schools"
```

---

## Grant Aggregator API Priorities

### Tier 1: Immediate Integration (Next 6 Months)

1. **Candid API** (developer.candid.org)
   - ✅ API available
   - ✅ Comprehensive US + international foundation data
   - ✅ Industry-standard taxonomy (PCS)
   - Priority: Grants API + Taxonomy API

2. **EU Funding & Tenders Portal API**
   - ✅ API available
   - ✅ Covers Horizon Europe, Cohesion Fund, etc.
   - ✅ Structured data
   - Priority: Opportunity search API

3. **Grants.gov**
   - ⚠️ API access unclear (investigate)
   - ✅ All US federal grants
   - ✅ Well-structured data
   - Priority: Opportunity listing API

### Tier 2: Future Integration (12+ Months)

4. **GrantForward** (academic research)
5. **GlobalGiving** (international development)
6. **Foundation Directory** (Candid's comprehensive database)

### Tier 3: Manual/Scraping Only

7. **Instrumentl** (proprietary platform, unlikely to have public API)
8. **Various national/regional aggregators** (as needed)

---

## Feature 005 Recommendations

Based on this research, **Feature 005 should focus on**:

### Must Have:
1. ✅ `FundingMechanism` enum (already planned)
2. ✅ `FundingSourceType` enum (already planned)
3. ✅ `ProjectScale` enum (already planned)
4. ❌ **NEW**: `BeneficiaryPopulation` enum (critical gap)
5. ❌ **NEW**: `RecipientOrganizationType` enum (critical gap)

### Should Have:
6. Expand `FundingSearchCategory` from 25 → 29 (add early childhood, adult ed)
7. Update `QueryGenerationRequest` to support multi-dimensional targeting
8. Enhance `CategoryMapper` for population-specific query generation

### Nice to Have:
9. Weekly scheduling by funding source type (as planned)
10. Gap analysis: "What STEM × Girls × Public Schools grants are missing?"
11. Funding amount range tracking

---

## Conclusion

### Key Findings:

1. ✅ **Education focus is correct** - Well-defined vertical with clear taxonomies
2. ✅ **Our 25 categories are solid** - Good mapping to major aggregators
3. ⚠️ **Need 4 more categories** - Early childhood + Adult education gaps
4. ❌ **Missing beneficiary dimension** - Critical for targeting relevance
5. ❌ **Missing recipient type dimension** - Can't filter by eligible org type
6. ✅ **Feature 005 plan is on target** - FundingMechanism, FundingSourceType, ProjectScale
7. ❌ **Need to add population/recipient to Feature 005** - Don't skip these!

### Immediate Action Items:

**For Feature 005 Implementation**:
1. Implement planned enums: FundingMechanism, FundingSourceType, ProjectScale
2. **ADD** BeneficiaryPopulation enum (20-30 values)
3. **ADD** RecipientOrganizationType enum (15-20 values)
4. Expand FundingSearchCategory by 4 (early childhood + adult ed)
5. Update QueryGenerationRequest for multi-dimensional queries
6. Enhance CategoryMapper for population-aware query generation
7. Update all tests

**Post-Feature 005**:
1. Research Candid API integration (Priority 1)
2. Research EU Funding & Tenders API (Priority 1)
3. Investigate Grants.gov API access

---

## References

### Taxonomy Standards:
- Candid Philanthropy Classification System: https://taxonomy.candid.org/
- NTEE Codes: https://nccs.urban.org/project/national-taxonomy-exempt-entities-ntee-codes
- SAM.gov Assistance Listings: https://sam.gov/content/assistance-listings

### Major Aggregators:
- Grants.gov: https://www.grants.gov/
- EU Funding & Tenders: https://ec.europa.eu/info/funding-tenders/opportunities/portal
- Horizon Europe: https://research-and-innovation.ec.europa.eu/funding/funding-opportunities/funding-programmes-and-open-calls/horizon-europe_en
- Candid APIs: https://developer.candid.org/

### Education Taxonomies:
- US Dept of Education Grant Programs
- Horizon Europe Education Calls
- Candid Education Subject Codes

---

**Research Date**: 2025-11-04
**Status**: COMPLETED
**Next Steps**: Use findings to refine Feature 005 specification
