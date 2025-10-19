# Domain Model Research Needed
## Open Questions and Research Areas

**Version**: 0.1
**Last Updated**: 2025-10-18
**Status**: Research Phase

---

## Overview

Before implementing the complete domain model, we need to research and define several key taxonomies and concepts. This document tracks what we need to investigate, where to find examples, and how to model the results.

---

## 1. Organization Types (Applicant Eligibility)

### Question
**Who can apply for funding?** What types of organizations are eligible?

### Known Examples
- Non-Governmental Organizations (NGOs)
- International NGOs (INGOs)
- Educational Institutions
  - K-12 Schools
  - Universities
  - Research Institutions
- Religious Organizations
  - Churches
  - Faith-based NGOs
- Government Entities
  - Municipalities
  - Regional Governments
  - National Agencies
- For-Profit Companies (some grants allow commercial entities)
- Social Enterprises
- Cooperatives
- Individuals (scholarships, fellowships)
- Partnerships/Consortiums (multiple organizations together)

### Research Needed
- [ ] Review Candid.org organization type taxonomy
- [ ] Check EU funding portals (Grants & Tenders) for org types
- [ ] Review USAID eligibility requirements
- [ ] Check America for Bulgaria Foundation requirements
- [ ] Look at grants.gov organization classifications
- [ ] Review fundsforngos.org categories

### Modeling Questions
1. Is this a flat list or hierarchical?
   - Example: NGO → Educational NGO → K-12 Education NGO
2. Can funding sources specify multiple required types?
   - Example: "NGOs OR Educational Institutions"
3. Do we need legal structure types?
   - Example: 501(c)(3), Foundation, Trust, Limited Company, etc.

### Data Model Sketch
```java
@Table("organization_type")
public class OrganizationType {
    @Id private UUID typeId;
    private String name; // "NGO", "Educational Institution"
    private UUID parentTypeId; // Nullable - for hierarchies
    private String description;
    private Boolean requiresLegalStatus; // Some types need 501(c)(3), charity registration, etc.
    private Set<String> commonLegalStructures; // ["501(c)(3)", "Registered Charity", "Foundation"]
}

// Funding source eligibility
@Table("funding_source_org_type_eligibility")
public class FundingSourceOrgTypeEligibility {
    @Id private UUID id;
    private UUID fundingSourceId;
    private UUID organizationTypeId;
    private Boolean isRequired; // Must be this type
    private Boolean isPreferred; // Preference given
    private Boolean isExcluded; // Explicitly not allowed
}
```

---

## 2. Funding Source Types

### Question
**What kinds of funding mechanisms exist?**

### Known Examples
- **Grants** (most common)
  - Program Grants (specific initiatives)
  - General Operating Grants (unrestricted)
  - Project Grants (time-bound projects)
  - Capital Grants (buildings, equipment)
  - Capacity Building Grants
  - Challenge Grants (requires matching funds)
  - Seed Grants (startup funding)

- **Scholarships**
  - Undergraduate Scholarships
  - Graduate Scholarships
  - Research Fellowships
  - Study Abroad Scholarships

- **Loans**
  - Low-Interest Loans
  - Microfinance
  - Revolving Loan Funds

- **Tenders** (Grants & Tenders - competitive procurement)
  - Service Contracts
  - Supply Contracts
  - Works Contracts

- **Calls for Proposals/Submissions**
  - Research Calls
  - Innovation Competitions
  - Social Impact Bonds

- **Matching Funds**
  - Requires recipient to provide percentage of total
  - Example: Grant provides 80%, organization provides 20%

- **In-Kind Support**
  - Equipment donations
  - Technical assistance
  - Pro-bono services

- **Equity/Investment** (impact investing, venture philanthropy)

- **Prizes/Awards**
  - Innovation prizes
  - Recognition awards with cash

### Research Needed
- [ ] Review Candid.org grant type classifications
- [ ] Check EU Grants & Tenders portal taxonomy
- [ ] Review USAID funding mechanisms
- [ ] Look at fundsforngos.org funding type categories
- [ ] Check World Bank funding instruments
- [ ] Review foundation websites for funding types

### Modeling Questions
1. Can a single funding source have multiple types?
   - Example: "Grant + Matching Requirement + Technical Assistance"
2. Is matching a requirement or a type?
   - Leaning towards: Matching is a REQUIREMENT of a Grant
3. Do we model "Call for Proposals" separately from "Grant"?
   - Or is it just a phase/status of a grant?

### Data Model Sketch
```java
public enum FundingMechanism {
    GRANT,              // Traditional grant funding
    SCHOLARSHIP,        // Individual scholarships
    LOAN,               // Low-interest or revolving loans
    TENDER,             // Competitive procurement (EU)
    EQUITY_INVESTMENT,  // Impact investing, venture philanthropy
    IN_KIND,            // Non-monetary support
    PRIZE,              // Competition prizes
    TECHNICAL_ASSISTANCE // Consulting, training
}

public enum GrantType {
    PROGRAM_GRANT,
    GENERAL_OPERATING,
    PROJECT_GRANT,
    CAPITAL_GRANT,
    CAPACITY_BUILDING,
    CHALLENGE_GRANT,
    SEED_GRANT
}

@Table("funding_source")
public class FundingSource {
    // ... existing fields ...

    private FundingMechanism primaryMechanism; // GRANT, SCHOLARSHIP, etc.
    private GrantType grantType; // Nullable - only for grants
    private Set<FundingMechanism> additionalMechanisms; // Can include multiple (e.g., grant + technical assistance)
}
```

---

## 3. Matching Requirements

### Question
**What are matching fund requirements and how do we model them?**

### Known Patterns
- **Percentage Match**: "Organization must provide 20% of total project cost"
- **Dollar Match**: "Organization must provide $10,000 in matching funds"
- **In-Kind Match**: "Organization can provide in-kind services (volunteer time, space) as match"
- **Cash vs. In-Kind**: Some allow in-kind, some require cash match
- **Third-Party Match**: Match can come from other sources (not just applicant)
- **Sliding Scale**: Match percentage varies by organization size or region

### Examples
```
Example 1: Simple Percentage Match
- Grant Amount: $100,000
- Match Required: 20% cash
- Total Project Budget: $125,000
- Organization must contribute: $25,000 cash

Example 2: In-Kind Allowed
- Grant Amount: $50,000
- Match Required: 25% (cash or in-kind)
- Organization can contribute: Staff time, office space, equipment

Example 3: Challenge Grant
- Grant Amount: Up to $500,000
- Match Required: 1:1 (every dollar raised matched by foundation)
- Organization must raise funds from other sources

Example 4: No Match Required
- Grant Amount: $75,000
- Match Required: None
- Fully funded by grantor
```

### Research Needed
- [ ] Review foundation RFPs (Request for Proposals) for match requirements
- [ ] Check EU funding match requirements (often 80/20 or 70/30)
- [ ] Look at USAID cost-sharing requirements
- [ ] Review challenge grant structures

### Data Model Sketch
```java
@Table("funding_source")
public class FundingSource {
    // ... existing fields ...

    // Matching requirements
    private Boolean matchRequired; // true/false
    private BigDecimal matchPercentage; // Nullable - e.g., 0.20 for 20%
    private BigDecimal matchAmountMin; // Nullable - minimum dollar match
    private BigDecimal matchAmountMax; // Nullable - maximum dollar match
    private Boolean inKindMatchAllowed; // Can match be non-cash?
    private Boolean thirdPartyMatchAllowed; // Can match come from other funders?
    private String matchRequirementDetails; // Text description of complex requirements
}
```

Or separate entity for complex matching:
```java
@Table("matching_requirement")
public class MatchingRequirement {
    @Id private UUID requirementId;
    private UUID fundingSourceId; // FK

    private MatchType matchType; // PERCENTAGE, FIXED_AMOUNT, CHALLENGE, NONE
    private BigDecimal matchPercentage; // For percentage matches
    private BigDecimal matchAmountMin;
    private BigDecimal matchAmountMax;

    private Boolean cashRequired; // true = must be cash
    private Boolean inKindAllowed; // true = in-kind contributions count
    private Boolean thirdPartyAllowed; // true = can come from other sources

    private String inKindEligibleTypes; // "Staff time, office space, equipment"
    private String requirementDetails; // Full text description
}

public enum MatchType {
    NONE,           // No matching required
    PERCENTAGE,     // X% of total budget
    FIXED_AMOUNT,   // Specific dollar amount
    CHALLENGE,      // 1:1 or similar matching scheme
    SLIDING_SCALE   // Varies by applicant characteristics
}
```

---

## 4. Application Requirements & Access

### Question
**What do applicants need to do to apply? What credentials/accounts are required?**

### Known Patterns

**Account Registration:**
- Some funding portals require account creation before applying
- Examples:
  - grants.gov → SAM.gov registration (US federal grants)
  - EU Funding & Tenders Portal → EU Login account
  - Foundation websites → Applicant portal accounts

**Paid Access:**
- Some funding databases charge subscription fees
- Examples:
  - Candid.org → Paid subscription for full access
  - GrantStation → Monthly/annual subscription
  - Foundation Directory Online → Tiered pricing

**Application Submission Methods:**
- Online portal (most common)
- Email submission
- Physical mail (rare)
- Through intermediary organization

**Pre-Application Requirements:**
- Letter of Inquiry (LOI) before full proposal
- Concept Note before detailed application
- Pre-qualification questionnaire
- Registration in government systems (DUNS number, tax ID, charity registration)

**Required Documentation:**
- Legal registration documents
- Tax-exempt status (501(c)(3), charity registration)
- Audited financials
- Board member lists
- Annual reports
- Project budgets
- Letters of support
- MOUs with partner organizations

### Examples
```
Example 1: Simple Foundation Grant
- Application Method: Online form on foundation website
- Account Required: Yes (email + password)
- Cost: Free
- Pre-Application: Letter of Inquiry (2 pages)
- Required Docs: 501(c)(3) letter, project budget, organizational budget

Example 2: US Federal Grant (grants.gov)
- Application Method: grants.gov portal
- Account Required: Yes + SAM.gov registration + DUNS number
- Cost: Free (but SAM.gov registration takes weeks)
- Required Docs: Extensive federal forms, budgets, narratives

Example 3: EU Grant
- Application Method: EU Funding & Tenders Portal
- Account Required: EU Login (free)
- Cost: Free
- Pre-Application: Sometimes concept note
- Required Docs: Varies by program

Example 4: Database Subscription (Candid.org)
- Access Method: Subscription-based database
- Account Required: Yes
- Cost: $59.99/month (example pricing)
- No application - this is a funding source DATABASE not a funding source itself
```

### Research Needed
- [ ] Catalog common application platforms (grants.gov, EU portal, foundation-specific)
- [ ] List common registration requirements (SAM.gov, charity registration, DUNS)
- [ ] Research paid database subscriptions (Candid, GrantStation, etc.)
- [ ] Document typical pre-application requirements

### Data Model Sketch

**Application Process:**
```java
@Table("funding_source")
public class FundingSource {
    // ... existing fields ...

    // Application process
    private String applicationMethod; // ONLINE_PORTAL, EMAIL, MAIL, INTERMEDIARY
    private String applicationUrl; // URL to application portal/form
    private Boolean accountRequired; // Must create account to apply?
    private String accountPlatform; // "grants.gov", "Foundation website", "EU Portal"

    // Pre-application
    private Boolean preApplicationRequired; // LOI, concept note, etc.
    private String preApplicationType; // "Letter of Inquiry", "Concept Note"
    private Integer preApplicationPageLimit; // e.g., 2 pages

    // Required registrations (may need separate entity)
    private List<String> requiredRegistrations; // ["SAM.gov", "501(c)(3)", "Charity Registration"]

    // Documentation requirements
    private List<String> requiredDocuments; // ["Audited financials", "Project budget", "Board list"]
}
```

**Access Credentials (for our researchers):**
```java
@Table("funding_source_access")
public class FundingSourceAccess {
    @Id private UUID accessId;
    private UUID fundingSourceId; // FK - or organizationId if org-level

    private Boolean requiresAccount; // Do we need an account to access this?
    private Boolean requiresPaidSubscription; // Is this behind a paywall?

    // If we have credentials
    private String platform; // "Candid.org", "GrantStation", "Foundation portal"
    private String username; // ENCRYPTED
    private String password; // ENCRYPTED
    private String accountEmail; // ENCRYPTED
    private String apiKey; // ENCRYPTED - if available

    // Subscription details
    private BigDecimal subscriptionCost; // Monthly or annual cost
    private String subscriptionInterval; // MONTHLY, ANNUAL
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionExpiresDate;

    // Access notes
    private String accessNotes; // "Shared team account", "Individual login", etc.
    private UUID credentialOwnerId; // AdminUser who maintains this credential

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime lastVerifiedAt; // When did we last check this still works?
}
```

**Platform/Database Entity:**
```java
@Table("funding_platform")
public class FundingPlatform {
    @Id private UUID platformId;
    private String name; // "grants.gov", "Candid.org", "EU Funding & Tenders"
    private String url;
    private PlatformType type; // DATABASE, APPLICATION_PORTAL, BOTH
    private Boolean requiresPaidAccess;
    private String description;
    private Set<String> coverageGeography; // What regions does this cover?
}

public enum PlatformType {
    FUNDING_DATABASE,       // Like Candid.org - aggregates funding sources
    APPLICATION_PORTAL,     // Like grants.gov - submit applications
    BOTH,                   // Some do both
    FOUNDATION_WEBSITE      // Individual foundation site
}
```

---

## 5. Grants vs. Tenders vs. Calls for Proposals

### Question
**What's the difference between these terms? How do we model them?**

### Definitions (Research)

**Grant:**
- Non-repayable funds given by foundation, government, or organization
- Purpose: Support a project, program, or organization
- No goods/services delivered to grantor (non-commercial)
- Application-based selection

**Tender (EU terminology):**
- Competitive procurement process
- Purchaser (e.g., EU agency) wants to BUY goods or services
- Commercial relationship (contract, not grant)
- Winner delivers product/service and is paid
- Common in EU: "Grants & Tenders" portal

**Call for Proposals (CFP):**
- Invitation to submit project proposals
- Used by grantors to solicit applications
- Time-bound (open and close dates)
- May result in Grant OR Contract (tender)

**Request for Applications (RFA):**
- Similar to CFP
- Common in US context (NIH, NSF use this term)

**Request for Proposals (RFP):**
- Often used for tenders/contracts (commercial)
- Also used by some foundations for grants

### Key Distinctions

```
Grant:
- Non-commercial
- Supports applicant's mission/project
- Application → Award → Reporting
- Example: "Education Infrastructure Grant"

Tender/Procurement:
- Commercial contract
- Buyer wants specific product/service
- Bid → Contract → Delivery → Payment
- Example: "IT Services Contract for EU Agency"

Call for Proposals:
- The PROCESS of soliciting applications
- Can lead to Grant OR Tender
- Time-bound (submission deadline)
- Example: "2025 Call for Education Proposals"
```

### Research Needed
- [ ] Review EU Grants & Tenders portal to understand their taxonomy
- [ ] Check how Candid.org classifies these
- [ ] Look at USAID terminology (grants vs. contracts vs. cooperative agreements)
- [ ] Review foundation RFPs/CFPs

### Modeling Question
**Should we model "Call for Proposals" separately from "Funding Source"?**

Option A: Call = Status/Phase of Funding Source
```java
@Table("funding_source")
public class FundingSource {
    private FundingSourceStatus status; // ANNOUNCED, OPEN_FOR_APPLICATIONS, CLOSED, AWARDED
    private LocalDate callOpenDate;
    private LocalDate callCloseDate;
}
```

Option B: Call = Separate Entity (cycles)
```java
@Table("funding_source") // The program/opportunity
public class FundingSource {
    private String name; // "Education Infrastructure Program"
    private Boolean isRecurring; // Opens every year
}

@Table("funding_call") // Specific time-bound cycles
public class FundingCall {
    @Id private UUID callId;
    private UUID fundingSourceId; // FK
    private String callName; // "2025 Education Infrastructure Call"
    private LocalDate openDate;
    private LocalDate closeDate;
    private CallStatus status; // UPCOMING, OPEN, CLOSED, AWARDED
}
```

**Recommendation**: Use Option B if funding sources have recurring cycles (annual calls). Otherwise Option A is simpler.

---

## 6. Research Sources & Methods

### Where to Research

**Free Sources:**
- [ ] **grants.gov** - US federal grant database
- [ ] **EU Funding & Tenders Portal** - EU grants and contracts
- [ ] **fundsforngos.org** - Free NGO funding database
- [ ] **Foundation websites** - America for Bulgaria, Open Society, etc.
- [ ] **USAID website** - Development funding
- [ ] **World Bank funding portals**

**Paid Sources (may need subscriptions for research):**
- [ ] **Candid.org** - Premier US foundation database (formerly Foundation Center)
- [ ] **GrantStation** - Subscription-based grant database
- [ ] **Foundation Directory Online** - Candid's comprehensive database

**Academic/Documentation:**
- [ ] **ISO standards** - Check if any ISO standards for grant classifications
- [ ] **IATI standards** - International Aid Transparency Initiative
- [ ] **Research papers** - Academic studies on grant taxonomies

### Research Method

For each research area:
1. **Survey existing systems** - How do they classify things?
2. **Document patterns** - What's common across multiple systems?
3. **Identify edge cases** - What doesn't fit the pattern?
4. **Propose taxonomy** - Design our classification system
5. **Validate with examples** - Test against real funding sources
6. **Iterate** - Refine based on findings

---

## 7. Priority Order for Research

### Phase 1 (Critical for MVP)
1. ✅ **Geographic Hierarchy** - DONE
2. **Organization Types** - Who can apply?
3. **Funding Mechanism Types** - Grants vs. scholarships vs. loans
4. **Application Requirements** - What do applicants need?

### Phase 2 (Important for completeness)
5. **Matching Requirements** - How to model match logic
6. **Grant vs. Tender distinction** - EU context important
7. **Call for Proposals modeling** - Recurring cycles?
8. **Access Credentials** - How we access paid databases

### Phase 3 (Nice to have)
9. **Reporting requirements** - What grantees must report
10. **Payment structures** - Lump sum vs. milestone-based vs. reimbursement
11. **Eligibility restrictions** - Beyond org type and geography

---

## 8. Output Format

For each research area, produce:

1. **Taxonomy Document** (Markdown)
   - List of categories/types
   - Definitions
   - Examples
   - Sources

2. **Data Model Sketch** (Java entities)
   - Entity classes
   - Enums
   - Relationships

3. **Seeding Data** (SQL or JSON)
   - Initial seed data for common types

4. **Validation Examples** (Test cases)
   - Real-world funding sources mapped to our model

---

## 9. Next Steps

**Immediate Actions:**
1. **Assign research tasks** - Kevin + Huw + Claude
2. **Choose first research area** - Organization Types recommended
3. **Survey 10-20 real funding sources** - Document their requirements
4. **Draft taxonomy** - Based on research
5. **Model in DDD entities** - Create Java classes
6. **Validate** - Test against more examples
7. **Document** - Add to docs/ folder
8. **Iterate** - Refine based on feedback

**Research Template:**
```markdown
# [Research Area] - Taxonomy

## Sources Surveyed
- Source 1: findings
- Source 2: findings
- ...

## Patterns Identified
- Common pattern 1
- Common pattern 2
- ...

## Edge Cases
- Edge case 1
- Edge case 2

## Proposed Taxonomy
- Category 1: Definition + Examples
- Category 2: Definition + Examples
- ...

## Data Model
[Java entity classes]

## Seed Data
[Initial data for common types]

## Validation
[Map 5-10 real funding sources to this taxonomy]
```

---

## Questions for Kevin

1. **Research priority**: Should we start with Organization Types or Funding Mechanism Types?
2. **Paid access**: Do we have budget for Candid.org subscription for research? (Worth it!)
3. **EU focus**: How important is Grant vs. Tender distinction for Eastern Europe?
4. **Credentials**: Where should we store access credentials securely? Separate credential vault?
5. **Recurring cycles**: Do we need to model annual grant cycles separately or just track "next deadline"?

---

**Document Status**: Research Planning
**Next Action**: Begin research on Organization Types
**Owner**: Kevin + Huw + Claude collaboration
