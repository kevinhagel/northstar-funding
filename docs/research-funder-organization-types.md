# Funder Organization Types - Research Document
## Taxonomy of Organizations That Provide Funding

**Version**: 0.1
**Last Updated**: 2025-10-18
**Status**: Research Phase

---

## Purpose

We need to classify **organizations that provide funding** (grantors/funders) into a comprehensive, hierarchical taxonomy. This helps us:

1. **Categorize discovered funding sources** - Know what type of funder we're dealing with
2. **Understand site structures** - Government sites differ from foundation sites
3. **Set crawling expectations** - Different org types have different web patterns
4. **Enable filtering** - Users can search "Show me all EU funding sources" or "Show me corporate foundations"
5. **Model program hierarchies** - Foundations have programs, governments have agencies with programs

---

## Key Distinction: Funders vs. Aggregators

**FUNDERS** - Organizations that actually provide money:
- America for Bulgaria Foundation
- European Commission (Horizons 2020)
- Bill & Melinda Gates Foundation
- USAID

**AGGREGATORS** - Platforms that list/database funding sources but don't provide funds:
- grants.gov (US federal grant portal - lists government grants)
- fundsforngos.org (NGO grant database)
- Candid.org (foundation database)
- EU Funding & Tenders Portal (lists EU grants/tenders)

**Important**: We track BOTH, but they're modeled differently:
- Funders → Have programs → Provide funding sources
- Aggregators → List funding sources from many funders

---

## Proposed Funder Taxonomy

### Level 1: Major Categories

```
FUNDER ORGANIZATIONS
│
├── GOVERNMENT / PUBLIC SECTOR
│   ├── Multilateral Organizations
│   ├── Bilateral Aid Agencies
│   ├── Supranational Government (EU)
│   ├── National Government
│   ├── Regional Government
│   └── Local Government
│
├── FOUNDATIONS / NON-PROFIT SECTOR
│   ├── Private Foundations
│   ├── Corporate Foundations
│   ├── Family Foundations
│   ├── Community Foundations
│   ├── Operating Foundations
│   └── Public Charities
│
├── CORPORATE SECTOR
│   ├── Corporate Giving Programs
│   ├── Corporate Social Responsibility (CSR)
│   └── Venture Philanthropy / Impact Investing
│
├── RELIGIOUS ORGANIZATIONS
│   ├── Denominational Foundations
│   ├── Church-Based Funding
│   └── Faith-Based Foundations
│
├── ACADEMIC INSTITUTIONS
│   ├── University Endowments
│   ├── Research Grants (from universities)
│   └── Academic Fellowships
│
└── INDIVIDUAL DONORS / CROWDFUNDING
    ├── High Net Worth Individuals
    ├── Donor-Advised Funds
    └── Crowdfunding Platforms
```

---

## Detailed Breakdown

### 1. GOVERNMENT / PUBLIC SECTOR

#### 1.1 Multilateral Organizations
**Definition**: International organizations with multiple country members

**Examples**:
- World Bank Group
- International Monetary Fund (IMF)
- United Nations agencies:
  - UNDP (UN Development Programme)
  - UNICEF
  - UNESCO
  - UNHCR
- Regional development banks:
  - European Bank for Reconstruction and Development (EBRD)
  - Asian Development Bank
  - African Development Bank
- Council of Europe Development Bank

**Characteristics**:
- Funded by multiple countries
- Global or regional scope
- Large-scale development programs
- Often require government partnerships

**Website Examples**:
- https://www.worldbank.org/en/projects-operations/products-and-services
- https://www.undp.org/funding
- https://www.ebrd.com/what-we-do.html

---

#### 1.2 Bilateral Aid Agencies
**Definition**: Government agencies providing aid from one country to others

**Examples**:
- **United States**:
  - USAID (US Agency for International Development)
  - State Department (Bureau of Educational and Cultural Affairs)
  - Millennium Challenge Corporation
  - Peace Corps
- **European Countries**:
  - DFID (UK - now merged into FCDO)
  - GIZ (Germany)
  - AFD (France)
  - SIDA (Sweden)
  - DANIDA (Denmark)
  - NORAD (Norway)
- **Other**:
  - JICA (Japan)
  - CIDA (Canada)

**Characteristics**:
- Represent single country's foreign aid
- Focus on development, democracy, humanitarian aid
- Often tied to foreign policy goals

**Website Examples**:
- https://www.usaid.gov/work-usaid/get-grant-or-contract
- https://www.giz.de/en/workingwithgiz/1336.html

---

#### 1.3 Supranational Government (European Union)
**Definition**: EU-level funding programs

**Examples**:
- **EU Programs**:
  - Horizons Europe (research & innovation)
  - Erasmus+ (education, training, youth)
  - Creative Europe (cultural sector)
  - EU4Health
  - Digital Europe Programme
- **EU Structural & Investment Funds**:
  - European Social Fund (ESF)
  - European Regional Development Fund (ERDF)
  - Cohesion Fund
- **External Action**:
  - Instrument for Pre-Accession Assistance (IPA) - for candidate countries
  - European Neighbourhood Instrument
- **Other**:
  - EEA Grants (from Norway, Iceland, Liechtenstein)
  - Norway Grants

**Characteristics**:
- EU member states and candidate countries eligible
- Large budgets (multi-billion EUR)
- Complex application processes
- Often require transnational partnerships

**Website Examples**:
- https://ec.europa.eu/info/funding-tenders/opportunities/portal/screen/home
- https://ec.europa.eu/regional_policy/en/funding/

---

#### 1.4 National Government
**Definition**: Country-level government ministries and agencies

**Examples**:
- **United States**:
  - Department of Education grants
  - National Science Foundation (NSF)
  - National Endowment for the Humanities (NEH)
  - National Endowment for the Arts (NEA)
- **Bulgaria**:
  - Bulgarian Ministry of Education and Science
  - National Science Fund
- **Other Countries**:
  - UK Arts Council
  - German Federal Ministry of Education and Research

**Characteristics**:
- Domestic focus (sometimes international programs)
- Sector-specific (education, arts, science)
- Administered through ministries/agencies

**Website Examples**:
- https://www.grants.gov (US federal grants portal)
- https://www.nsf.gov/funding/

---

#### 1.5 Regional Government
**Definition**: State/province/regional government funding

**Examples**:
- US State governments (California Arts Council, New York State Council on the Arts)
- German Länder (state-level funding)
- Bulgarian oblasts/provinces

**Characteristics**:
- Geographic boundaries (state, province, region)
- Smaller budgets than national
- Local priorities

---

#### 1.6 Local Government
**Definition**: City, county, municipal funding

**Examples**:
- Sofia Municipality grants
- New York City Department of Cultural Affairs

**Characteristics**:
- Very localized
- Often for community projects
- Smaller budgets

---

### 2. FOUNDATIONS / NON-PROFIT SECTOR

#### 2.1 Private Foundations
**Definition**: Independent foundations funded by endowment, typically from single source

**Examples**:
- **US-Based (International reach)**:
  - Bill & Melinda Gates Foundation
  - Ford Foundation
  - Rockefeller Foundation
  - MacArthur Foundation
  - Open Society Foundations (George Soros)
  - Hewlett Foundation
- **Europe-Based**:
  - Wellcome Trust (UK)
  - Robert Bosch Stiftung (Germany)
  - Calouste Gulbenkian Foundation (Portugal)
- **Bulgaria/Eastern Europe Focus**:
  - America for Bulgaria Foundation
  - Trust for Civil Society in Central and Eastern Europe

**Characteristics**:
- Large endowments
- Independent governance
- Specific mission/focus areas
- Typically don't solicit donations (endowed)
- Grant-making is primary activity

**Website Examples**:
- https://www.gatesfoundation.org/about/how-we-work/grant-opportunities
- https://us-bulgaria.org/grants/
- https://www.fordfoundation.org/work/our-grants/

---

#### 2.2 Corporate Foundations
**Definition**: Foundations established by corporations, separate legal entities

**Examples**:
- Google.org (Alphabet foundation)
- Walmart Foundation
- JPMorgan Chase Foundation
- Vodafone Foundation
- Microsoft Philanthropies

**Characteristics**:
- Tied to corporation but legally separate
- Often focus on areas related to business (e.g., Google → education, tech access)
- May receive ongoing funding from parent company

**Website Examples**:
- https://www.google.org/
- https://corporate.walmart.com/purpose/walmart-foundation

---

#### 2.3 Family Foundations
**Definition**: Foundations controlled by family members of donor

**Examples**:
- Walton Family Foundation
- Pew Charitable Trusts
- W.K. Kellogg Foundation
- Many smaller local family foundations

**Characteristics**:
- Governed by family members
- May reflect family values/interests
- Range from very large to small local foundations

---

#### 2.4 Community Foundations
**Definition**: Publicly supported foundations serving specific geographic area

**Examples**:
- New York Community Trust
- Silicon Valley Community Foundation
- Local community foundations (Sofia Community Foundation, if exists)

**Characteristics**:
- Geographically focused
- Pool resources from multiple donors
- Donor-advised funds common
- Serve local needs

---

#### 2.5 Operating Foundations
**Definition**: Foundations that run their own programs rather than making grants

**Examples**:
- Getty Trust (operates museums)
- Russell Sage Foundation (conducts research)

**Characteristics**:
- May make limited grants
- Primarily operate programs directly
- Less relevant for our database (not primarily grantmakers)

---

#### 2.6 Public Charities
**Definition**: Nonprofits that solicit donations from public and may make grants

**Examples**:
- United Way
- Rotary International
- Lions Clubs International Foundation

**Characteristics**:
- Rely on public donations
- May make grants as part of mission
- Often membership-based

---

### 3. CORPORATE SECTOR (Non-Foundation)

#### 3.1 Corporate Giving Programs
**Definition**: Direct giving by corporations (not through separate foundation)

**Examples**:
- Corporate matching gift programs
- Employee volunteer grants
- Direct sponsorships

**Characteristics**:
- Part of corporate budget (not endowed foundation)
- Often tied to marketing/PR
- May be more flexible than foundations

---

#### 3.2 Corporate Social Responsibility (CSR)
**Definition**: Corporate programs focused on social/environmental impact

**Examples**:
- Tech companies funding coding bootcamps
- Banks funding financial literacy programs

**Characteristics**:
- Aligned with business interests
- Combination of grants and programs

---

#### 3.3 Venture Philanthropy / Impact Investing
**Definition**: Investment-oriented approach to philanthropy

**Examples**:
- Omidyar Network
- Acumen Fund
- Social Venture Partners

**Characteristics**:
- May provide equity investment, not just grants
- Focus on scalable social enterprises
- Expect measurable outcomes/ROI

---

### 4. RELIGIOUS ORGANIZATIONS

#### 4.1 Denominational Foundations
**Definition**: Foundations affiliated with religious denominations

**Examples**:
- United Methodist Committee on Relief
- Catholic Relief Services
- Jewish Federations
- Islamic Relief

**Characteristics**:
- Mission aligned with faith values
- May prioritize faith-based organizations
- Often focus on humanitarian/development work

---

#### 4.2 Church-Based Funding
**Definition**: Direct funding from churches or religious institutions

**Examples**:
- Individual church mission funds
- Diocese-level grants
- Monastery foundations

**Characteristics**:
- Smaller scale
- Local focus often
- Faith alignment expected

---

#### 4.3 Faith-Based Foundations (Independent)
**Definition**: Foundations with religious mission but not tied to denomination

**Examples**:
- Lilly Endowment (Christian focus)
- Templeton Foundation (religion/science)

**Characteristics**:
- Independent governance
- Faith-informed mission

---

### 5. ACADEMIC INSTITUTIONS

#### 5.1 University Endowments
**Definition**: Universities providing grants from endowment funds

**Examples**:
- Harvard fellowships
- Oxford scholarships
- Research grants from university funds

**Characteristics**:
- Often for students/researchers
- Academic focus

---

### 6. INDIVIDUAL DONORS / CROWDFUNDING

#### 6.1 High Net Worth Individuals
**Definition**: Philanthropists giving directly (not through foundation)

**Examples**:
- Billionaires making direct grants
- Giving Pledge signatories

**Characteristics**:
- Less structured
- May be ad-hoc

---

#### 6.2 Donor-Advised Funds
**Definition**: Charitable accounts managed by sponsors

**Examples**:
- Fidelity Charitable
- Schwab Charitable

**Characteristics**:
- Donors recommend grants
- Managed by financial institutions

---

#### 6.3 Crowdfunding Platforms
**Definition**: Online platforms for raising funds

**Examples**:
- GlobalGiving
- Kickstarter (for projects)
- GoFundMe (personal causes)

**Characteristics**:
- Small individual donations
- Public campaigns

---

## Aggregators / Portals (Not Funders)

These organizations DON'T provide funding themselves, but list/aggregate funding opportunities:

### Grant Databases / Search Platforms
- **grants.gov** - US federal grants portal
- **Candid.org** - Foundation database (subscription)
- **fundsforngos.org** - NGO funding database
- **GrantStation** - Grant search platform
- **Foundation Directory Online** - Candid's comprehensive database
- **EU Funding & Tenders Portal** - Lists EU funding (also application portal)

### How to Model Aggregators
```
Organization Type: AGGREGATOR
Purpose: Lists funding sources from multiple funders
Characteristics:
  - Does NOT provide funding directly
  - May require account/subscription
  - Valuable for discovery (we should crawl these)
  - Links TO actual funders
```

---

## Data Model

### Organization Entity (Enhanced)

```java
@Table("organization")
public class Organization {

    @Id
    private UUID organizationId;

    // Basic Identity
    private String name; // "America for Bulgaria Foundation"
    private String nameLocal; // Local language name
    private String shortName; // "ABF"
    private String website;

    // Classification
    private OrganizationType organizationType; // PRIVATE_FOUNDATION, EU_PROGRAM, etc.
    private OrganizationCategory category; // FOUNDATION, GOVERNMENT, CORPORATE, etc.
    private Boolean isAggregator; // true for grants.gov, fundsforngos.org

    // Hierarchy
    private UUID parentOrganizationId; // Nullable
    // Example: "Horizons Europe" → parent: "European Commission"
    // Example: "USAID" → parent: "US Department of State"

    // Geographic
    private UUID headquartersLocationId; // FK to geographic_location
    private Set<UUID> operatesInLocationIds; // Where they fund (many-to-many)

    // Description
    private String mission;
    private String description;
    private Set<String> focusAreas; // ["Education", "Healthcare", "Environment"]

    // Financial (if known)
    private BigDecimal totalAssets; // Endowment size for foundations
    private BigDecimal annualGrantsAwarded; // How much they give per year
    private String currency;

    // Legal Structure
    private String legalStructure; // "501(c)(3)", "EU Institution", "Government Agency"
    private String taxId; // EIN, charity registration number

    // Contact
    private String primaryEmail;
    private String primaryPhone;
    private String mailingAddress;

    // Discovery
    private LocalDateTime discoveredAt;
    private UUID discoveredBy; // AdminUser
    private LocalDateTime lastVerifiedAt;
    private Boolean isActive;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public enum OrganizationCategory {
    GOVERNMENT,
    FOUNDATION,
    CORPORATE,
    RELIGIOUS,
    ACADEMIC,
    INDIVIDUAL_DONOR,
    AGGREGATOR
}

public enum OrganizationType {
    // Government
    MULTILATERAL_ORG,           // World Bank, UN agencies
    BILATERAL_AID_AGENCY,       // USAID, GIZ
    EU_INSTITUTION,             // European Commission
    EU_PROGRAM,                 // Horizons Europe, Erasmus+
    NATIONAL_GOVERNMENT,        // US Dept of Education
    REGIONAL_GOVERNMENT,        // State/province
    LOCAL_GOVERNMENT,           // Municipality

    // Foundation
    PRIVATE_FOUNDATION,         // Gates, Ford
    CORPORATE_FOUNDATION,       // Google.org
    FAMILY_FOUNDATION,          // Walton Family
    COMMUNITY_FOUNDATION,       // Local community foundations
    OPERATING_FOUNDATION,       // Getty Trust
    PUBLIC_CHARITY,             // United Way

    // Corporate
    CORPORATE_GIVING_PROGRAM,   // Direct corporate giving
    CSR_PROGRAM,                // CSR initiatives
    VENTURE_PHILANTHROPY,       // Impact investors

    // Religious
    DENOMINATIONAL_FOUNDATION,  // Catholic Relief Services
    CHURCH_BASED,               // Individual church funds
    FAITH_BASED_FOUNDATION,     // Templeton Foundation

    // Academic
    UNIVERSITY_ENDOWMENT,

    // Individual/Crowdfunding
    HIGH_NET_WORTH_INDIVIDUAL,
    DONOR_ADVISED_FUND,
    CROWDFUNDING_PLATFORM,

    // Aggregator
    GRANT_DATABASE,             // Candid.org, fundsforngos.org
    GOVERNMENT_PORTAL           // grants.gov, EU portal
}
```

---

## Research Tasks

### Phase 1: Survey Major Funders
- [ ] Review 20-30 major foundation websites
- [ ] Document their structure (programs, focus areas)
- [ ] Identify common patterns

**Suggested List for Research**:
1. America for Bulgaria Foundation
2. Bill & Melinda Gates Foundation
3. Ford Foundation
4. Open Society Foundations
5. USAID
6. European Commission (Horizons Europe)
7. EU Cohesion Funds
8. World Bank
9. Google.org
10. Vodafone Foundation
11. Wellcome Trust
12. Robert Bosch Stiftung
13. Norway Grants
14. National Science Foundation (US)
15. UK Arts Council
16. Trust for Civil Society in CEE
17. MacArthur Foundation
18. Rockefeller Foundation
19. United Way
20. Rotary International

### Phase 2: Aggregator Analysis
- [ ] grants.gov - structure and taxonomy
- [ ] fundsforngos.org - how they categorize
- [ ] Candid.org - their funder classification system
- [ ] EU Funding & Tenders Portal - taxonomy

### Phase 3: Validate Taxonomy
- [ ] Map all researched funders to our taxonomy
- [ ] Identify gaps
- [ ] Refine categories

---

## Example Organizations Mapped to Taxonomy

```
America for Bulgaria Foundation
  Category: FOUNDATION
  Type: PRIVATE_FOUNDATION
  Parent: None
  Headquarters: United States, Vermont
  OperatesIn: Bulgaria
  IsAggregator: false

European Commission - Horizons Europe
  Category: GOVERNMENT
  Type: EU_PROGRAM
  Parent: European Commission
  Headquarters: Brussels, Belgium
  OperatesIn: EU Member States + Associated Countries
  IsAggregator: false

grants.gov
  Category: AGGREGATOR
  Type: GOVERNMENT_PORTAL
  Parent: US General Services Administration
  Headquarters: United States
  OperatesIn: Global (lists US grants)
  IsAggregator: true

fundsforngos.org
  Category: AGGREGATOR
  Type: GRANT_DATABASE
  Parent: None (private company)
  Headquarters: [Research needed]
  OperatesIn: Global
  IsAggregator: true

USAID
  Category: GOVERNMENT
  Type: BILATERAL_AID_AGENCY
  Parent: US Department of State
  Headquarters: United States, Washington DC
  OperatesIn: 100+ countries
  IsAggregator: false

Bill & Melinda Gates Foundation
  Category: FOUNDATION
  Type: PRIVATE_FOUNDATION
  Parent: None
  Headquarters: United States, Seattle
  OperatesIn: Global
  IsAggregator: false
```

---

## Hierarchy Examples

### Example 1: EU Funding Hierarchy
```
European Commission (MULTILATERAL_ORG)
├── Directorate-General for Research and Innovation
│   └── Horizons Europe (EU_PROGRAM)
│       ├── Pillar 1: Excellent Science
│       ├── Pillar 2: Global Challenges
│       └── Pillar 3: Innovative Europe
├── Directorate-General for Education and Culture
│   └── Erasmus+ (EU_PROGRAM)
└── Directorate-General for Regional Policy
    ├── European Social Fund (EU_PROGRAM)
    └── Cohesion Fund (EU_PROGRAM)
```

### Example 2: US Government Hierarchy
```
US Federal Government
├── Department of State
│   └── USAID (BILATERAL_AID_AGENCY)
│       ├── Bureau for Europe and Eurasia
│       └── Bureau for Democracy, Conflict, and Humanitarian Assistance
├── Department of Education
│   ├── Federal Student Aid
│   └── Office of Elementary and Secondary Education
└── National Science Foundation (NATIONAL_GOVERNMENT)
    ├── Directorate for Engineering
    └── Directorate for Social, Behavioral, and Economic Sciences
```

### Example 3: Foundation with Programs
```
America for Bulgaria Foundation (PRIVATE_FOUNDATION)
├── Education Program
│   ├── Teacher Training Initiative
│   └── School Infrastructure Grants
├── Civil Society Program
└── Economic Development Program
```

---

## Next Steps

1. **Kevin + Huw**: Review taxonomy - does this structure make sense?
2. **Research Phase**: Survey 20-30 major funders, document findings
3. **Refine**: Adjust taxonomy based on research
4. **Model**: Implement Organization entity with OrganizationType enum
5. **Seed**: Create initial seed data for major funders
6. **Validate**: Map real funding sources to taxonomy

---

## Questions for Kevin

1. Does the FUNDER vs. AGGREGATOR distinction make sense?
2. Is the taxonomy comprehensive enough? Missing any major categories?
3. Should we model organization hierarchies (e.g., EU Commission → Horizons Europe)?
4. How deep should we go into government agency structures?
5. Priority: Start with US foundations, EU programs, or both?

---

**Document Status**: Draft - Awaiting Feedback
**Next Action**: Begin surveying major funder websites
**Owner**: Kevin + Huw + Claude
