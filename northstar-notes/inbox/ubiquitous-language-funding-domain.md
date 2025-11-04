# Ubiquitous Language: Funding Discovery Domain

**Date**: 2025-11-04
**Purpose**: Define domain language for NorthStar Funding Discovery platform
**Reference**: Domain-Driven Design (DDD) principles

---

## Core Entities

### 1. Funding Provider
**Definition**: Organization or entity that offers financial support for projects, programs, or individuals.

**Also Known As**:
- Funding source
- Grantor
- Funder
- Donor organization

**Types**:
- Government agencies (national, regional, municipal)
- Private foundations
- Corporate foundations
- Community foundations
- International organizations (UN, World Bank, EU)
- NGOs with grant-making programs
- Crowdfunding platforms
- Impact investors
- Religious organizations

**Examples**:
- EU Horizon Europe Program
- Bill & Melinda Gates Foundation
- Bulgarian Ministry of Education
- America for Bulgaria Foundation
- Local Sofia Municipality Grant Program
- Corporate CSR programs (Google.org, Microsoft Philanthropies)

**Attributes**:
- Organization name
- Funding focus areas
- Geographic scope
- Eligible recipient types
- Funding mechanisms offered
- Application cycles
- Funding amounts available
- Website, contact information

---

### 2. Funding Client / Funding Receiver
**Definition**: Organization or individual seeking financial support from Funding Providers.

**Also Known As**:
- Grant seeker
- Grantee (after receiving funding)
- Applicant
- Recipient organization
- Beneficiary organization

**Types**:
- K-12 schools (public, private, charter)
- Universities and colleges
- Research institutions
- Nonprofit organizations (NPOs)
- NGOs
- Government agencies (seeking grants from other governments)
- Individual students
- Individual researchers
- Teachers
- Social enterprises
- Community groups

**Examples**:
- Bulgarian public school in Plovdiv seeking STEM equipment funding
- Sofia University research team seeking Horizon Europe grant
- NGO running after-school programs seeking foundation support
- Individual student seeking scholarship
- Teacher seeking professional development fellowship

**Attributes**:
- Organization/individual name
- Organization type (school, university, NGO, individual)
- Mission/focus area
- Geographic location
- Funding needs
- Project scale
- Eligible populations served

---

### 3. Funding Opportunity
**Definition**: Specific grant, program, or funding call offered by a Funding Provider.

**Also Known As**:
- Grant opportunity
- Call for proposals (CFP)
- Request for proposals (RFP)
- Funding call
- Award program

**Attributes**:
- Opportunity title
- Funding Provider
- Funding category (education, research, infrastructure, etc.)
- Funding mechanism (grant, loan, scholarship, etc.)
- Eligible recipient types (who can apply)
- Target beneficiary populations (who is served)
- Geographic eligibility
- Funding amount range
- Application deadline
- Application requirements
- Opportunity URL

---

### 4. Funding Program
**Definition**: Ongoing, recurring funding initiative by a Funding Provider (may have multiple Funding Opportunities over time).

**Examples**:
- Horizon Europe (EU program with yearly calls)
- Fulbright Scholarship Program (annual cycles)
- Bulgarian National Science Fund (recurring grant rounds)

**Relationship**:
- One Funding Program → Many Funding Opportunities (over time)

---

## Supporting Concepts

### 5. Funding Category
**Definition**: Subject area or focus of funding.

**Examples**:
- STEM Education
- Teacher Development
- Infrastructure Funding
- Student Scholarships
- Community Partnerships

**Current System**: 25 FundingSearchCategory enum values (educational focus)

---

### 6. Funding Mechanism
**Definition**: HOW the funding is provided.

**Types**:
- **Grant**: Non-repayable gift
- **Scholarship**: Individual student financial aid (merit/need-based)
- **Fellowship**: Postgraduate/research support
- **Loan**: Repayable funding
- **Award**: Prize/recognition + funding
- **Matching Fund**: Requires recipient to match contribution
- **Equity Investment**: For social enterprises/startups
- **In-Kind Donation**: Non-monetary support (equipment, services)
- **Technical Assistance**: Consulting/capacity building

---

### 7. Beneficiary Population
**Definition**: WHO is ultimately served by the funded project (may be different from Funding Client).

**Examples**:
- A school (Funding Client) receives grant to serve **low-income students** (Beneficiary Population)
- An NGO (Funding Client) receives grant to serve **girls in STEM** (Beneficiary Population)
- An individual student (Funding Client) IS the Beneficiary Population

**Types**:
- Age groups (early childhood, adolescents, adults, seniors)
- Gender (girls/women, boys/men)
- Socioeconomic (low-income, first-generation students)
- Special needs (disabilities, learning differences)
- Cultural/ethnic (minorities, indigenous peoples, immigrants)
- Geographic (rural, urban, remote areas)
- Other (LGBTQ+, veterans, at-risk youth, gifted students)

---

### 8. Funding Provider Type
**Definition**: Category of organization providing funding.

**Types**:
- **Government - National**: National ministries, federal agencies
- **Government - Regional**: EU programs, regional governments
- **Government - Municipal**: City, local government programs
- **Private Foundation**: Independent family/corporate foundations
- **Community Foundation**: Local/regional community-based foundations
- **Corporate Foundation**: Company CSR programs
- **International Organization**: UN, World Bank, OECD
- **NGO**: NGOs that also provide grants
- **Religious Organization**: Faith-based funders
- **Crowdfunding Platform**: Kickstarter, GlobalGiving, etc.
- **Impact Investor**: Social impact investment funds
- **Academic Institution**: Universities offering research grants
- **Professional Association**: Industry/field-specific grant programs

---

### 9. Recipient Organization Type
**Definition**: Type of Funding Client organization eligible to apply.

**Why This Matters**: Many funding opportunities restrict eligibility by organization type.

**Types**:
- **K-12 Schools**: Public, private, charter
- **Higher Education**: Community college, 4-year college, research university
- **Vocational/Technical Schools**
- **Nonprofit Organizations**: 501(c)(3) or equivalent
- **NGOs**: International/local NGOs
- **Government Agencies**: Municipalities, departments
- **Research Institutions**: Independent research centers
- **Community-Based Organizations**: Local grassroots groups
- **Religious Organizations**
- **Libraries, Museums**
- **Individuals**: Students, teachers, researchers
- **Consortiums**: Multi-organization partnerships
- **Any Eligible Organization**: No restrictions

---

### 10. Project Scale
**Definition**: Size/scope of project or funding amount.

**Types**:
- **Micro**: < €5,000
- **Small**: €5,000 - €50,000
- **Medium**: €50,000 - €250,000
- **Large**: €250,000 - €1M
- **Mega**: > €1M

---

### 11. Geographic Scope
**Definition**: Geographic area where funding can be used or where recipients must be located.

**Examples**:
- Bulgaria
- Eastern Europe
- EU Member States
- Western Balkans
- Global

**Current System**: 15 GeographicScope enum values

---

## Platform Components

### 12. Search-Crawler-Scraper Pipeline
**Definition**: Automated system to discover and extract Funding Provider information from the web.

**Components**:
1. **Search**: Use multiple search engines to discover potential Funding Provider websites
2. **Crawler**: Extract structured data from discovered websites
3. **Scraper**: Parse and structure funding opportunity information
4. **Human-AI Hybrid Analysis**: Validate and enrich data with AI + human review

**Purpose**: Build comprehensive Funding Provider database (Phase 1)

---

### 13. Aggregator Crawler Pipeline
**Definition**: System to extract data from structured grant aggregator websites.

**Purpose**: SUPPLEMENT Search-Crawler-Scraper data (Phase 2 - FUTURE)

**Target Aggregators**:
- Grants.gov (US federal)
- EU Funding & Tenders Portal
- Candid Foundation Directory
- National grant databases

**Why Needed**: Many Funding Providers are NOT on aggregators (small foundations, regional programs, corporate CSR)

---

### 14. Candidate
**Definition**: Potential Funding Provider discovered by search but not yet validated.

**Entity**: `FundingSourceCandidate` (current domain model)

**Lifecycle**:
1. **NEW**: Just discovered via search
2. **PENDING_CRAWL**: Passed anti-spam filter, ready to crawl
3. **CRAWLED**: Website content extracted
4. **ENHANCED**: AI/human analysis added metadata
5. **JUDGED**: Confidence score assigned
6. **VALIDATED**: Confirmed as real Funding Provider → becomes FundingProvider entity

---

### 15. Domain (Deduplication)
**Definition**: Website domain name tracked to prevent reprocessing same Funding Provider.

**Entity**: `Domain` (current domain model)

**Purpose**: Efficiency - don't re-crawl same organization multiple times

**Example**:
- `gates-foundation.org` → Tracked once
- `gatesfoundation.org` vs `gatesfoundation.com` → Different domains, but same org (need org-level deduplication too)

---

## Relationships

### Core Workflow:

```
1. Search Query Generated (AI-powered)
   ↓
2. Search Engines Return Results
   ↓
3. Anti-Spam Filtering
   ↓
4. Domain Deduplication
   ↓
5. Candidate Created (FundingSourceCandidate)
   ↓
6. Crawler Extracts Data
   ↓
7. Human-AI Hybrid Analysis
   ↓
8. Validated Funding Provider
   ↓
9. Stored in Database
   ↓
10. Matched to Funding Clients (Frontend AI-assisted search)
```

### Entity Relationships:

```
Funding Provider
  ├─ has many → Funding Programs
  │   └─ has many → Funding Opportunities
  ├─ has → Funding Provider Type
  ├─ focuses on → Funding Categories (many)
  ├─ targets → Beneficiary Populations (many)
  ├─ accepts → Recipient Organization Types (many)
  ├─ operates in → Geographic Scopes (many)
  └─ offers → Funding Mechanisms (many)

Funding Client
  ├─ has → Recipient Organization Type
  ├─ located in → Geographic Scope
  ├─ serves → Beneficiary Populations (many)
  ├─ seeks → Funding Categories (many)
  ├─ prefers → Funding Mechanisms (many)
  └─ matches with → Funding Providers (AI-powered)
```

---

## Taxonomy Dimensions for Matching

**To match Funding Providers → Funding Clients, we need:**

### Provider Dimensions (What they offer):
1. ✅ Funding Category (25 values) - WHAT they fund
2. ✅ Geographic Scope (15 values) - WHERE they fund
3. ⚠️ Funding Provider Type (13 values) - WHO they are
4. ⚠️ Funding Mechanism (9 values) - HOW they provide funds
5. ⚠️ Project Scale (5 values) - HOW MUCH they provide
6. ❌ Beneficiary Population (25 values) - WHO is served
7. ❌ Recipient Organization Type (20 values) - WHO can apply

### Client Dimensions (What they need):
1. ✅ Funding Category (25 values) - WHAT they seek
2. ✅ Geographic Scope (15 values) - WHERE they are
3. ❌ Recipient Organization Type (20 values) - WHAT they are
4. ⚠️ Funding Mechanism (9 values) - HOW they want funding
5. ⚠️ Project Scale (5 values) - HOW MUCH they need
6. ❌ Beneficiary Population (25 values) - WHO they serve
7. (Optional) Funding Provider Type preference - WHO they prefer

---

## Current vs Target Taxonomy

### Current State (Features 003-004):
```
✅ FundingSearchCategory (25 values)
✅ GeographicScope (15 values)
✅ SearchEngineType (5 values)
✅ CandidateStatus (workflow tracking)
✅ DomainStatus (deduplication)
```

**What We Can Search For Today**:
- "STEM Education grants in Bulgaria"
- "Teacher Development programs in Eastern Europe"

**What We CANNOT Search For**:
- "STEM Education grants **for girls** in Bulgaria" (no Beneficiary Population)
- "Teacher Development **for public schools** in Eastern Europe" (no Recipient Type)
- "**Small grants** (< €50k) for STEM" (no Project Scale)
- "**Foundation grants** (not loans) for infrastructure" (no Mechanism or Provider Type)

### Target State (Feature 005):
```
✅ FundingSearchCategory (29 values) - Add early childhood + adult ed
✅ GeographicScope (15 values) - Keep as is
✅ FundingMechanism (9 values) - NEW
✅ FundingProviderType (13 values) - NEW
✅ ProjectScale (5 values) - NEW
✅ BeneficiaryPopulation (25 values) - NEW
✅ RecipientOrganizationType (20 values) - NEW
```

**What We WILL Be Able to Search For**:
- "**Foundation grants** for STEM Education targeting **girls** in Bulgarian **public schools**, **€50k-€250k** range"

**Query Dimensionality**:
- Current: 2D (Category × Geography)
- Target: 7D (Category × Geography × Mechanism × Provider Type × Scale × Beneficiary × Recipient)

**Impact on Match Quality**:
- Current: Broad, many irrelevant results
- Target: Precise, highly relevant results

---

## Critical Missing Pieces for Feature 005

### 1. Funding Client Taxonomy ❌ **CRITICAL**

**Problem**: We model Funding **Providers** but not Funding **Clients**.

**Gap**: No domain entity for "Funding Client" or their attributes.

**Impact**: Can't model client needs, can't match, can't track relationships.

**Recommendation**: Create `FundingClient` entity with:
- Organization name
- Recipient Organization Type (K-12 school, university, NGO, etc.)
- Geographic location
- Funding needs (categories)
- Target beneficiary populations served
- Project scale typically pursued
- Preferred funding mechanisms

### 2. Provider → Client Matching ❌ **NOT YET DESIGNED**

**Problem**: We're building a provider discovery system, but not yet a matching system.

**Gap**: No algorithm to score Provider-Client compatibility.

**Future Feature**: "Match Engine" that scores:
- Category alignment (STEM × STEM = high score)
- Geographic eligibility (Bulgaria in scope = eligible)
- Recipient type match (accepts public schools = eligible)
- Beneficiary alignment (targets girls = bonus score)
- Mechanism fit (client wants grants, provider offers grants = match)
- Scale fit (client needs €100k, provider offers €50-200k = match)

### 3. Multi-Dimensional Query Generation ⚠️ **PARTIALLY DESIGNED**

**Current**: QueryGenerationRequest has `categories` and `geographic`.

**Target**: QueryGenerationRequest needs:
```java
QueryGenerationRequest.builder()
    .categories(Set.of(STEM_EDUCATION))
    .geographic(GeographicScope.BULGARIA)
    .providerTypes(Set.of(PRIVATE_FOUNDATION, CORPORATE_FOUNDATION))
    .mechanisms(Set.of(GRANT, SCHOLARSHIP))
    .beneficiaries(Set.of(GIRLS_WOMEN, ECONOMICALLY_DISADVANTAGED))
    .recipientTypes(Set.of(K12_PUBLIC_SCHOOL, NGO))
    .projectScale(ProjectScale.SMALL)
    .build()
```

**This is Feature 005's core deliverable**.

---

## Glossary

| Term | Definition | Example |
|------|------------|---------|
| **Funding Provider** | Organization offering financial support | EU Horizon, Gates Foundation |
| **Funding Client** | Organization/individual seeking funding | Bulgarian school, NGO, student |
| **Funding Opportunity** | Specific grant/call offered by provider | Horizon Europe Call 2024 |
| **Funding Program** | Ongoing initiative with recurring calls | Fulbright Program |
| **Funding Category** | Subject area (WHAT) | STEM Education, Infrastructure |
| **Funding Mechanism** | Method of providing funds (HOW) | Grant, Loan, Scholarship |
| **Beneficiary Population** | Ultimate recipient of benefit (WHO served) | Girls, Low-income students |
| **Recipient Organization Type** | Eligible applicant type (WHO applies) | Public school, University, NGO |
| **Funding Provider Type** | Category of funder (WHO provides) | Government, Foundation, Corporate |
| **Project Scale** | Funding amount range (HOW MUCH) | Small (€5-50k), Large (€250k-1M) |
| **Geographic Scope** | Eligible region (WHERE) | Bulgaria, EU, Global |
| **Candidate** | Unvalidated potential provider | New discovery from search |
| **Domain** | Website domain for deduplication | `example.org` |
| **Aggregator** | Site listing multiple providers | Grants.gov, Candid |
| **Anti-Spam Filter** | System removing low-quality results | 4-layer detection |
| **Human-AI Hybrid** | Combined AI + human validation | Enhanced confidence scoring |

---

## Ubiquitous Language Rules

### Say This:
- ✅ "Funding Provider" (not funding source, grantor)
- ✅ "Funding Client" (not beneficiary, recipient, grantee)
- ✅ "Funding Opportunity" (not grant, call)
- ✅ "Funding Category" (not subject, focus area)
- ✅ "Funding Mechanism" (not funding type, delivery method)
- ✅ "Beneficiary Population" (not target group, population served)
- ✅ "Recipient Organization Type" (not eligible applicant, org type)
- ✅ "Search-Crawler-Scraper" (not search engine, web scraper)
- ✅ "Aggregator Crawler" (not API integration, data import)

### NOT This:
- ❌ "Funding Source" → Use "Funding Provider"
- ❌ "Beneficiary" → Use "Funding Client" (for applicant) or "Beneficiary Population" (for served group)
- ❌ "Grantee" → Use "Funding Client"
- ❌ "Grant" → Use "Funding Opportunity" (generic) or specify mechanism
- ❌ "Recipient" → Use "Funding Client"
- ❌ "Funding Type" → Use "Funding Mechanism" or "Funding Provider Type" (be specific)

---

## Strategic Priorities

### Phase 1 (Current): Search-Crawler-Scraper
**Goal**: Build comprehensive Funding Provider database

**Approach**:
1. ✅ Search engines discover provider websites (Feature 003 DONE)
2. ✅ Anti-spam filtering (Feature 003 DONE)
3. ✅ Domain deduplication (Feature 003 DONE)
4. ✅ AI-powered query generation (Feature 004 DONE)
5. → Crawler extracts provider data (NEXT)
6. → Human-AI hybrid analysis validates (NEXT)
7. → Store validated providers (NEXT)

**Why This Matters**: Many Funding Providers are NOT on aggregator sites:
- Small regional foundations
- Corporate CSR programs
- Municipal grant programs
- Faith-based funders
- Emerging crowdfunding platforms

**The crawler discovers the long tail of funding opportunities.**

### Phase 2 (Future): Aggregator Crawler
**Goal**: SUPPLEMENT Phase 1 data with aggregator-listed providers

**Approach**:
1. Crawl structured aggregator sites (Grants.gov, EU Portal, Candid)
2. Parse structured data (easier than web scraping)
3. Deduplicate against Phase 1 data
4. Enrich Phase 1 records with aggregator metadata

**Why This Is Supplemental**:
- Aggregators have good coverage of major providers (governments, large foundations)
- But miss small/regional/emerging providers
- Phase 1 discovers what aggregators miss

**The aggregator crawler fills the mainstream gap.**

### Platform Vision:
**Comprehensive Funding Provider database** =
Search-Crawler-Scraper (long tail) + Aggregator Crawler (mainstream)

---

**Last Updated**: 2025-11-04
**Status**: Domain Language Documented
**Next Step**: Use this language consistently in Feature 005 specification
