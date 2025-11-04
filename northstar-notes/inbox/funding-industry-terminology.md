# Funding Industry Terminology - Standard Language

**Date**: 2025-11-04
**Purpose**: Use industry-standard terminology for clarity and interoperability

---

## Industry Standard Terms

### The Three Players:

#### 1. Funding Sources (The Money)
**Who they are**: Organizations/entities that provide financial support

**Industry Terms** (all acceptable):
- **Funding Source** ← Primary term we'll use
- Funder
- Grantor
- Donor
- Sponsor
- Grant-maker
- Philanthropic organization

**Types**:
- Government agencies
- Private foundations
- Corporate foundations
- Community foundations
- International organizations
- NGOs with grant programs
- Crowdfunding platforms

**Examples**:
- EU Horizon Europe Program (government)
- Bill & Melinda Gates Foundation (private foundation)
- Google.org (corporate foundation)
- America for Bulgaria Foundation (community foundation)

---

#### 2. Funding Recipients/Beneficiaries (The Seekers)
**Who they are**: Organizations/individuals seeking/receiving funding

**Industry Terms** (context-dependent):
- **Applicant** - When applying for funding
- **Grantee** - After receiving funding (formal legal term)
- **Recipient** - General term for who receives funding
- **Beneficiary** - Who ultimately benefits (may be different from recipient)

**Important Distinction**:
- **Direct Recipient**: Organization that receives the grant
  - Example: NGO receives €100k grant
- **Beneficiary**: Who is ultimately served
  - Example: Low-income students served by the NGO's program

**Our Usage**:
- **Recipient Organization**: The organization applying/receiving
- **Beneficiary Population**: The people served by the funded project

**Types**:
- K-12 schools
- Universities
- Research institutions
- Nonprofit organizations (NPOs)
- NGOs
- Individuals (students, researchers, teachers)
- Government agencies (seeking grants from other governments)

---

#### 3. Grant Aggregators (The Intermediaries)
**Who they are**: Platforms/services that collect and list funding opportunities

**Industry Terms**:
- Grant aggregator
- Funding database
- Grant search platform
- Opportunity portal

**What they do**:
- Curate funding opportunities from multiple sources
- Provide search/filter tools
- Offer APIs for data access
- Some offer matching/recommendation services

**Major Aggregators**:
- **Grants.gov** - US federal grants
- **Candid (Foundation Center)** - US + international foundations
- **EU Funding & Tenders Portal** - All EU funding programs
- **GrantForward** - Academic/research grants
- **Instrumentl** - Nonprofit grant discovery
- **GlobalGiving** - International development

**Grant Aggregators as an Industry**:
- Subscription-based business models
- Data aggregation and enrichment
- Matching algorithms
- Grant writing tools
- Compliance tracking

---

## Our Platform's Terminology

### What We're Building:

**Primary Function**: Funding Source Discovery & Matching Platform

**Data Sources** (Hybrid Approach):
1. **Direct Discovery** (Phase 1 - Current):
   - Search engines → Find funding source websites
   - Crawler → Extract data from websites
   - Scraper → Parse opportunity details
   - Human-AI Analysis → Validate and enrich
   - Result: Comprehensive funding source database

2. **Aggregator Integration** (Phase 2 - Future):
   - Connect to aggregator APIs
   - Import structured opportunity data
   - Deduplicate against direct discovery data
   - Result: Supplement with mainstream funding sources

**Why Both?**
- Direct discovery finds: Small foundations, regional programs, corporate CSR, emerging funders
- Aggregators cover: Major government programs, large foundations, well-established funders
- **Together**: Comprehensive coverage (long tail + mainstream)

---

## Domain Model Entities (Aligned with Industry Terms)

### Core Entities:

#### 1. FundingSource
**Definition**: Organization or entity that provides financial support

**Attributes**:
- Organization name
- Source type (government, foundation, corporate, etc.)
- Funding focus areas (categories)
- Geographic scope
- Eligible recipient types
- Funding mechanisms offered
- Typical funding amounts
- Website, contact information

**Entity Status**: Needs to be created (currently we have FundingSourceCandidate)

---

#### 2. FundingOpportunity
**Definition**: Specific grant, program, or call offered by a funding source

**Attributes**:
- Opportunity title
- Funding source (relationship)
- Funding category
- Funding mechanism (grant, loan, scholarship, etc.)
- Eligible recipient types
- Target beneficiary populations
- Geographic eligibility
- Funding amount range
- Application deadline
- Requirements
- Opportunity URL

**Entity Status**: Needs to be created

---

#### 3. RecipientOrganization (Future Entity)
**Definition**: Organization seeking/receiving funding

**Attributes**:
- Organization name
- Organization type (school, university, NGO, etc.)
- Geographic location
- Mission/focus areas
- Beneficiary populations served
- Funding needs
- Project scale typically pursued

**Entity Status**: Not yet designed (future matching system)

---

#### 4. BeneficiaryPopulation (Supporting Concept)
**Definition**: People/groups who ultimately benefit from funded projects

**Examples**:
- Low-income students
- Girls in STEM
- Rural communities
- People with disabilities
- First-generation college students

**Implementation**: Enum (25-30 values)

---

#### 5. FundingSourceCandidate (Current Entity)
**Definition**: Potential funding source discovered but not yet validated

**Lifecycle**:
1. NEW - Discovered via search
2. PENDING_CRAWL - Passed anti-spam, ready for crawling
3. CRAWLED - Data extracted
4. ENHANCED - AI/human enrichment added
5. JUDGED - Confidence score assigned
6. VALIDATED - Confirmed → becomes FundingSource entity

**Entity Status**: ✅ Exists in domain model

---

## Taxonomy Dimensions (Industry-Standard Categories)

### For Funding Sources (What they offer):

1. **Funding Category** (WHAT they fund)
   - Industry terms: Subject, Focus Area, Program Area
   - Our implementation: 25 → 29 values (education-focused)
   - Examples: STEM Education, Teacher Development, Infrastructure

2. **Funding Mechanism** (HOW they provide)
   - Industry standard terms: Funding Type, Award Type, Instrument
   - Our implementation: 9 values
   - Examples: Grant, Loan, Scholarship, Fellowship, Award

3. **Source Type** (WHO they are)
   - Industry terms: Funder Type, Organization Type
   - Our implementation: 13 values
   - Examples: Government-National, Private Foundation, Corporate Foundation

4. **Geographic Scope** (WHERE they fund)
   - Industry standard: Geographic Focus, Service Area
   - Our implementation: 15 values
   - Examples: Bulgaria, Eastern Europe, EU, Global

5. **Project Scale** (HOW MUCH they fund)
   - Industry terms: Award Size, Grant Range, Funding Amount
   - Our implementation: 5 values
   - Examples: Micro (<€5k), Small (€5-50k), Medium (€50-250k)

6. **Eligible Recipient Types** (WHO can apply)
   - Industry standard: Applicant Eligibility, Eligible Organizations
   - Our implementation: 20 values
   - Examples: K-12 Public School, University, NGO, Individual

7. **Target Beneficiary Populations** (WHO is served)
   - Industry terms: Population Served, Target Group, Constituencies
   - Our implementation: 25 values
   - Examples: Girls/Women, Low-Income, Rural Communities

---

### For Recipients (What they need):

1. **Recipient Organization Type** (WHAT they are)
   - Industry standard: Organization Classification, Legal Status
   - Examples: 501(c)(3) nonprofit, K-12 school, Research university

2. **Funding Needs** (WHAT they seek)
   - Same categories as funding sources
   - Examples: STEM Education, Infrastructure, Teacher Training

3. **Beneficiary Populations Served** (WHO they serve)
   - Same as target populations above
   - Examples: Low-income students, Girls in STEM

4. **Geographic Location** (WHERE they are)
   - Same geographic taxonomy
   - Examples: Sofia, Bulgaria, Plovdiv, Bulgaria

5. **Project Scale Needs** (HOW MUCH they need)
   - Same scale taxonomy
   - Examples: Need €100k = Medium scale

---

## Query Generation Language

### Multi-Dimensional Search Queries:

**Structure**: [Mechanism] from [Source Type] for [Category] targeting [Beneficiary] in [Geography] for [Recipient Type], [Scale] range

**Example Queries**:

**Simple (2D)**:
```
"STEM education grants in Bulgaria"
```

**Intermediate (4D)**:
```
"Foundation grants for STEM education targeting girls in Bulgaria"
```

**Complex (7D)**:
```
"Private foundation grants for STEM education targeting girls in Bulgarian public schools, €50k-250k range"
```

**Very Complex (All dimensions)**:
```
"Government and private foundation grants and scholarships for STEM education and teacher development targeting girls and low-income students in Bulgarian public schools and NGOs, small to medium scale (€5k-250k), Eastern Europe and EU-wide programs"
```

---

## Aggregator Integration Strategy

### Phase 1: Direct Discovery (Current)
**Timeline**: Features 003-004 DONE, Crawler/Scraper NEXT

**Process**:
1. AI-generated search queries (Feature 004 ✅)
2. Multi-engine search (Brave, Serper, SearXNG, Tavily) (Feature 003 ✅)
3. Anti-spam filtering (Feature 003 ✅)
4. Domain deduplication (Feature 003 ✅)
5. Website crawling (NEXT)
6. Data extraction/scraping (NEXT)
7. Human-AI validation (NEXT)
8. FundingSource entity creation (NEXT)

**Output**: Database of funding sources (mainstream + long tail)

---

### Phase 2: Aggregator Integration (Future)
**Timeline**: After Phase 1 core complete

**Target Aggregators**:

**Tier 1** (Priority - Next 6 months):
1. **Candid API** (developer.candid.org)
   - Coverage: US + international foundations
   - Data: ~140,000 foundations, millions of grants
   - API: ✅ Available (Grants API, Taxonomy API)
   - Cost: Subscription-based

2. **EU Funding & Tenders Portal**
   - Coverage: All EU programs (Horizon, Cohesion, etc.)
   - Data: Thousands of opportunities
   - API: ✅ Available
   - Cost: Free (public data)

3. **Grants.gov**
   - Coverage: US federal grants
   - Data: 1000+ opportunities
   - API: ⚠️ Need to investigate access
   - Cost: Free (public data)

**Tier 2** (Later):
4. GrantForward (academic research)
5. GlobalGiving (international development)
6. Regional aggregators (as needed)

**Integration Approach**:
1. Connect to aggregator API
2. Map aggregator taxonomy → Our taxonomy
3. Import opportunity data
4. Deduplicate against Phase 1 data (by domain, org name)
5. Enrich Phase 1 records with aggregator metadata
6. Flag source: "Direct discovery" vs "Aggregator import" vs "Both"

**Why Both Sources Matter**:
- **Aggregators**: Good for major, established funding sources
- **Direct Discovery**: Finds small, regional, emerging funders
- **Combined**: Comprehensive coverage

---

## Industry Standards & Mappings

### NTEE Codes (IRS Nonprofit Classification)
- Used by: IRS, Candid, most US aggregators
- Structure: 26 major groups (A-Z)
- Education: "B" codes (B20-B99)
- **Our mapping**: FundingSearchCategory → NTEE codes

### Candid Philanthropy Classification System (PCS)
- Industry standard taxonomy
- Three dimensions: Subjects × Populations × Organization Types
- **Our mapping**: Direct alignment with our 7-dimensional model

### SAM.gov Assistance Listings
- US government program classification
- Functional categories + eligibility codes
- **Our mapping**: FundingSearchCategory + RecipientOrganizationType

### EU Program Taxonomy
- Horizon Europe pillars/clusters
- Cohesion Fund policy objectives
- **Our mapping**: FundingCategory + SourceType (GOVERNMENT_EU)

---

## Clear Language Guidelines

### Always Say:
- ✅ **Funding Source** (the organization offering money)
- ✅ **Recipient Organization** (the organization applying/receiving)
- ✅ **Beneficiary Population** (the people served by the funded project)
- ✅ **Funding Opportunity** (specific grant/call/program)
- ✅ **Grant Aggregator** (platform listing opportunities)
- ✅ **Funding Category** (subject area like STEM, Infrastructure)
- ✅ **Funding Mechanism** (grant, loan, scholarship, etc.)

### Context-Dependent Terms:
- **Applicant**: When discussing application process
- **Grantee**: After funding is awarded (legal/formal contexts)
- **Funder** / **Grantor**: Acceptable synonyms for Funding Source
- **Donor**: When discussing philanthropic giving (not government grants)

### Avoid Ambiguity:
- ❌ "Beneficiary" alone - Clarify: Beneficiary population? Recipient organization?
- ❌ "Funding Type" - Clarify: Funding mechanism? Source type?
- ❌ "Client" - Not industry-standard (use Recipient Organization)
- ❌ "Provider" - Not industry-standard (use Funding Source)
- ❌ "Grant" alone - Clarify: Funding opportunity? Funding mechanism?

---

## Our Platform's Value Proposition

### The Problem:
1. **Fragmentation**: Funding opportunities scattered across thousands of websites
2. **Limited Aggregator Coverage**: Aggregators miss small/regional/emerging funders
3. **Poor Discovery**: Recipients can't find relevant opportunities
4. **Manual Search**: Time-consuming, error-prone

### Our Solution:
1. **Comprehensive Discovery**: Direct web discovery + aggregator data
2. **AI-Powered Matching**: Multi-dimensional taxonomy for precision
3. **Long Tail Coverage**: Find funding sources aggregators miss
4. **Automated Pipeline**: Search → Crawl → Scrape → Analyze → Match

### Competitive Advantage:
- **Aggregators**: Only list what funding sources submit to them
- **Us**: Proactively discover + aggregate = comprehensive coverage

---

## Summary: Industry-Aligned Terminology

### Primary Terms:
- **Funding Source** ← What we discover/database
- **Recipient Organization** ← Who seeks funding
- **Beneficiary Population** ← Who is served
- **Funding Opportunity** ← Specific grants/calls
- **Grant Aggregator** ← Data source (supplemental)

### Our Approach:
- **Phase 1**: Direct discovery (search-crawl-scrape-analyze)
- **Phase 2**: Aggregator integration (supplement with mainstream)
- **Result**: Comprehensive funding source database

### Key Insight:
Grant aggregators are **a source of data**, not our competition. We combine:
- Their strength (mainstream, structured data)
- Our strength (long tail, comprehensive discovery)
= Complete funding landscape coverage

---

**Last Updated**: 2025-11-04
**Status**: Industry-standard terminology documented
**Next Step**: Use this language in code, documentation, and Feature 005 spec
