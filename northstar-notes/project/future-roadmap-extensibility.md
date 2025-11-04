# Future Roadmap & Extensibility Strategy

**Date**: 2025-11-04
**Purpose**: Document future capabilities to design for (not implement immediately)
**Context**: Feature 005 planning - Build extensible foundation

---

## Core Principle: Design for Extensibility

**Philosophy**: Implement what we need NOW, but design data structures and interfaces that support future capabilities WITHOUT requiring major refactoring.

**NOT**: Build everything up front (YAGNI violation)
**YES**: Add enums, entity fields, interface parameters that cost nothing now but enable future features

---

## Client Personas & Use Cases

### Persona 1: British Centre Burgas (For-Profit Private School)
**Owner**: Zlatina Petkova
**Established**: 1997 (28 years)
**Location**: Burgas, Bulgaria
**Students**: Ages 4-18, English language learners
**Organization Type**: For-profit education, Cambridge examination center

**Primary Funding Need**: Building purchase (€200k-500k range)

**Platform Usage**:
- Search in Bulgarian language
- Find loans, grants, and investment opportunities
- Target: Regional development banks, corporate CSR, EU small business grants
- Challenge: Many grants exclude for-profit organizations

**Key Insight**: For-profit education IS a market segment. Our crawler finds funding sources traditional aggregators miss.

---

### Persona 2: NorthStar Foundation Bulgaria (NGO)
**Mission**: Transformative education for disadvantaged children
**Location**: Burgas, Bulgaria
**Students**: Ages 4-18, focus on poverty alleviation (26% of Bulgarian children at poverty risk)
**Organization Type**: NGO, social impact, educational infrastructure

**Funding Needs** (Multi-Dimensional):
1. **Infrastructure**: Land/building purchase, construction (€500k-2M)
2. **Program Development**: Suggestopedia curriculum, future-ready skills
3. **Scholarships**: Access for disadvantaged children (talent scout network)
4. **Technology**: Digital learning platforms, infrastructure
5. **Cultural Hub**: Arts, sports, community engagement spaces
6. **Capacity Building**: Teacher training, international partnerships

**Platform Usage**:
- Search in Bulgarian language
- Find grants, matching grants, multilateral funding
- Target: EU programs, US foundations (America for Bulgaria), bilateral aid (USAID, Norway Grants)
- Additional Service: Grant application assistance (NorthStar Foundation NGO provides expert support)

**Key Insight**: Educational NGOs need diverse funding mechanisms for different project types.

---

### Persona 3: Other Eastern European Schools/NGOs (Future Clients)
**Geography**: Bulgaria → Romania, Poland, Greece, Balkans → EU-wide
**Languages**: Bulgarian, Romanian, Polish, Greek, English, German, French
**Organization Types**: Public schools, private schools, NGOs, municipalities, educational consultants
**Subscription Tiers**: €50-1500/month based on organization size and needs

**Platform Value**:
- Automated funding discovery (save 20-40 hours/week of manual searching)
- Multilingual search and results in client's language
- AI-powered matching (organization profile → relevant funding sources)
- Never miss deadlines or opportunities

---

## The Platform's Dual Value Proposition

### 1. SaaS Funding Discovery Platform (Primary Revenue)
**What**: Subscription-based automated funding discovery
**Who**: NGOs, schools, municipalities, consultants across Eastern Europe
**How**: Search-Crawler-Scraper pipeline + Aggregator API integration
**Revenue**: €50-1500/month subscriptions

**Competitive Advantage**:
- Comprehensive coverage (crawler for "long tail" + aggregator APIs for "mainstream")
- Eastern Europe focus (underserved market)
- Multilingual support (Bulgarian, Romanian, Polish, Greek, etc.)
- AI-powered matching (multi-dimensional taxonomy)

### 2. Grant Application Assistance (NorthStar Foundation NGO)
**What**: Expert grant writing and application support
**Who**: Educational organizations needing funding application help
**How**: Leverage 28 years of British Centre Burgas expertise
**Revenue**: Service fees, possibly bundled with SaaS subscriptions

**Value Add**:
- Scholarship application assistance
- Grant writing and proposal development
- Multilingual application support
- Local knowledge of Bulgarian/Eastern European funding landscape

---

## Feature 005 Scope: What We're Implementing NOW

### 1. Enhanced Taxonomy (5 New Enums + 1 Expanded)

#### FundingSourceType (13 values)
```
GOVERNMENT_NATIONAL
GOVERNMENT_REGIONAL
GOVERNMENT_MUNICIPAL
GOVERNMENT_EU
GOVERNMENT_INTERNATIONAL
PRIVATE_FOUNDATION
CORPORATE_FOUNDATION
COMMUNITY_FOUNDATION
INDIVIDUAL_PHILANTHROPIST
MULTILATERAL_ORGANIZATION
BILATERAL_AID_PROGRAM
CROWDFUNDING_PLATFORM
EDUCATION_ASSOCIATION
```

#### FundingMechanism (9 values)
```
GRANT
LOAN
EQUITY
SCHOLARSHIP
FELLOWSHIP
MATCHING_GRANT
IN_KIND_DONATION
TAX_INCENTIVE
PRIZE_AWARD
```

#### ProjectScale (5 values)
```
MICRO (< €5k)
SMALL (€5k - €50k)
MEDIUM (€50k - €250k)
LARGE (€250k - €1M)
MEGA (> €1M)
```

#### BeneficiaryPopulation (25 values)
```
GIRLS_WOMEN
BOYS_MEN
LOW_INCOME_FAMILIES
RURAL_COMMUNITIES
URBAN_UNDERSERVED
ETHNIC_MINORITIES
REFUGEES_IMMIGRANTS
PEOPLE_WITH_DISABILITIES
FIRST_GENERATION_STUDENTS
AT_RISK_YOUTH
ELDERLY
EARLY_CHILDHOOD_0_5
CHILDREN_6_12
ADOLESCENTS_13_18
YOUNG_ADULTS_19_25
ADULTS_26_64
LGBTQ_PLUS
INDIGENOUS_POPULATIONS
ORPHANS_FOSTER_CHILDREN
SINGLE_PARENT_FAMILIES
HOMELESS_UNSTABLY_HOUSED
VETERANS
INCARCERATED_FORMERLY_INCARCERATED
LANGUAGE_MINORITIES
GEOGRAPHICALLY_ISOLATED
```

#### RecipientOrganizationType (25 values)
```
K12_PUBLIC_SCHOOL
K12_PRIVATE_SCHOOL
K12_CHARTER_SCHOOL
K12_INTERNATIONAL_SCHOOL
K12_SPECIAL_EDUCATION
PRIVATE_LANGUAGE_SCHOOL
PRIVATE_TUTORING_CENTER
EXAMINATION_CENTER
PRESCHOOL_EARLY_CHILDHOOD
UNIVERSITY_PUBLIC
UNIVERSITY_PRIVATE
COMMUNITY_COLLEGE
VOCATIONAL_SCHOOL
NGO_EDUCATION_FOCUSED
NGO_SOCIAL_SERVICES
NGO_COMMUNITY_DEVELOPMENT
NONPROFIT_501C3
FOR_PROFIT_EDUCATION_PROVIDER
EDUCATION_SME
GOVERNMENT_AGENCY
MUNICIPALITY
LIBRARY
MUSEUM
INDIVIDUAL_EDUCATOR
INDIVIDUAL_RESEARCHER
```

#### FundingSearchCategory (25 → 29 values)
**Add 4 new categories**:
- EARLY_CHILDHOOD_EDUCATION
- ADULT_EDUCATION
- LIFELONG_LEARNING
- VOCATIONAL_TRAINING

---

### 2. QueryLanguage Enum (Design Now, Translate Later)

```java
public enum QueryLanguage {
    BULGARIAN("bg", "български"),
    ENGLISH("en", "English"),
    ROMANIAN("ro", "română"),
    POLISH("pl", "polski"),
    GERMAN("de", "Deutsch"),
    FRENCH("fr", "français"),
    GREEK("el", "ελληνικά"),
    RUSSIAN("ru", "русский");

    private final String isoCode;
    private final String nativeName;
}
```

**Feature 005**: Add enum, add fields to QueryGenerationRequest
**Future (Phase 6+)**: Implement translation service (Google Translate API, DeepL)

---

### 3. Multi-Dimensional QueryGenerationRequest

```java
public class QueryGenerationRequest {
    // Existing (Feature 004)
    private SearchEngineType searchEngine;
    private FundingSearchCategory category;
    private GeographicScope geographicScope;
    private Integer numberOfQueries;

    // NEW in Feature 005
    private FundingSourceType sourceType;          // Government, Foundation, etc.
    private FundingMechanism mechanism;            // Grant, Loan, etc.
    private ProjectScale projectScale;             // Micro, Small, Medium, Large, Mega
    private Set<BeneficiaryPopulation> beneficiaries;  // Who is served
    private RecipientOrganizationType recipientType;   // Who is applying

    // NEW in Feature 005 (design for future)
    private QueryLanguage userLanguage;            // Bulgarian, English, etc.
    private Set<QueryLanguage> searchLanguages;    // {Bulgarian, English, German}
    // Translation implementation: Phase 6+
}
```

---

### 4. Weekly Scheduling Logic (Feature 005)

**Problem**: Current query generation is random (no systematic coverage)

**Solution**: Map days of week → funding source types for systematic discovery

```java
public enum ScheduledQueryDay {
    MONDAY(Set.of(GOVERNMENT_NATIONAL, GOVERNMENT_EU)),
    TUESDAY(Set.of(PRIVATE_FOUNDATION, CORPORATE_FOUNDATION)),
    WEDNESDAY(Set.of(MULTILATERAL_ORGANIZATION, BILATERAL_AID_PROGRAM)),
    THURSDAY(Set.of(COMMUNITY_FOUNDATION, EDUCATION_ASSOCIATION)),
    FRIDAY(Set.of(GOVERNMENT_REGIONAL, GOVERNMENT_MUNICIPAL)),
    SATURDAY(Set.of(CROWDFUNDING_PLATFORM, INDIVIDUAL_PHILANTHROPIST)),
    SUNDAY(Set.of(/* Gap analysis queries */));

    private final Set<FundingSourceType> targetSourceTypes;
}
```

**Feature 005**: Implement basic scheduling
**Future (Phase 5+)**: Gap analysis to identify missing combinations

---

## Future Capabilities: Design For (Don't Implement Yet)

### 1. Multilingual Translation Service
**When**: Phase 6+ (after RAG search implemented)
**Why Now**: Add QueryLanguage enum and fields in Feature 005
**Cost Now**: Zero (just enum definitions and optional fields)
**Benefit Later**: No refactoring needed when adding translation

**Design Decision**:
```java
// Feature 005: Add fields
public class QueryGenerationRequest {
    private QueryLanguage userLanguage;  // User's preferred language
    private Set<QueryLanguage> searchLanguages;  // Languages to generate queries in
}

// Phase 6+: Implement translation service
public interface TranslationService {
    String translate(String text, QueryLanguage from, QueryLanguage to);
    Set<String> generateMultilingualQueries(QueryGenerationRequest request);
}
```

**Implementation Options** (Phase 6+):
- Google Translate API (€20 per 1M characters)
- DeepL API (better quality, €5-25/month)
- Azure Translator (enterprise option)
- Local model (llama-3-8b with translation fine-tuning)

---

### 2. User Profile & Organization Matching
**When**: Phase 5 (after Feature 005 taxonomy complete)
**Why Now**: RecipientOrganizationType enum defines what profiles look like
**Cost Now**: Zero (just enum definitions)
**Benefit Later**: Entity structure ready for matching engine

**Design Decision**:
```java
// Phase 5: Create entity (after Feature 005)
public class RecipientOrganization {
    private Long id;
    private String organizationName;
    private RecipientOrganizationType organizationType;
    private Set<GeographicScope> locations;
    private Set<BeneficiaryPopulation> populationsServed;
    private Set<FundingSearchCategory> fundingNeeds;
    private Set<ProjectScale> projectScales;
    private QueryLanguage preferredLanguage;
    private Instant createdAt;
    private Instant updatedAt;
}

// Phase 5: Create matching engine
public interface MatchingEngine {
    List<FundingOpportunityMatch> findMatches(RecipientOrganization recipient);
    BigDecimal calculateMatchScore(RecipientOrganization recipient, FundingOpportunity opportunity);
}
```

**Matching Algorithm** (Phase 5):
```
Match Score = Weighted Average of:
- Geographic match (30%): Location overlap
- Category match (25%): Funding needs vs opportunity categories
- Beneficiary match (20%): Populations served vs target populations
- Organization type match (15%): Eligible recipient types
- Project scale match (10%): Funding amount needed vs available
```

---

### 3. Grant Application Assistance Workflow
**When**: Phase 7+ (after matching engine and user profiles)
**Why Now**: Doesn't affect Feature 005 at all
**Cost Now**: Zero (no entities or fields needed yet)
**Benefit Later**: Can add without affecting existing code

**Design Decision**:
```java
// Phase 7+: New entities for application assistance
public class ApplicationSession {
    private Long id;
    private Long recipientOrganizationId;
    private Long fundingOpportunityId;
    private ApplicationStatus status;  // DRAFT, IN_REVIEW, SUBMITTED, AWARDED, REJECTED
    private Long assistedByAdminUserId;  // NorthStar Foundation staff
    private String applicationDocumentUrl;  // S3 bucket or similar
    private LocalDate deadlineDate;
    private Instant createdAt;
    private Instant lastUpdatedAt;
}

public enum ApplicationStatus {
    DRAFT,
    IN_REVIEW,
    SUBMITTED,
    UNDER_CONSIDERATION,
    AWARDED,
    REJECTED
}

// Phase 7+: Service for NorthStar Foundation staff
public interface ApplicationAssistanceService {
    ApplicationSession createSession(Long recipientOrgId, Long opportunityId, Long staffUserId);
    void addReviewComments(Long sessionId, String comments);
    void markAsSubmitted(Long sessionId);
    List<ApplicationSession> getActiveSessionsForStaff(Long staffUserId);
}
```

**Revenue Model Options** (Phase 7+):
- Bundled with premium SaaS tier (€500-1500/month includes X hours of assistance)
- Hourly rate (€75-150/hour for grant writing support)
- Success fee (5-10% of awarded grant amount)

---

### 4. In-Country Information Gathering
**When**: As we expand to new countries (Romania, Poland, Greece, etc.)
**Why Now**: GeographicScope enum already supports these countries
**Cost Now**: Zero (research time only, as needed)
**Benefit Later**: Systematic approach to country expansion

**Design Decision**:
```java
// Future: Reference data (not entities, just documentation/research)
public class CountryContext {
    String countryCode;  // ISO 3166-1 alpha-2 ("BG", "RO", "PL")
    String countryName;
    Set<String> primaryLanguages;  // ["Bulgarian", "Turkish"] for Bulgaria
    String nationalEducationDepartment;  // Ministry of Education URL
    String nationalGrantPortalUrl;  // Like grants.gov for Bulgaria
    Set<String> majorFundingSources;  // Top 10 funding sources in country
    Set<String> bilateralAidPrograms;  // USAID, Norway Grants, etc.
    String euCohesionFundRegion;  // Which EU fund programs apply
}
```

**Country Expansion Checklist** (Phase 8+):
1. Research national grant portal (if exists)
2. Identify top 10 funding sources for education
3. Document primary languages (UI translation needed)
4. Research bilateral aid programs active in country
5. Identify major foundations (US, EU, local)
6. Map EU programs available to country
7. Add country-specific QueryLanguage if needed
8. Update GeographicScope if new regions needed

**Priority Countries** (based on market research):
1. **Bulgaria** (Phase 1) - Current focus, home market
2. **Romania** (Phase 2) - Similar context, EU member, large education market
3. **Poland** (Phase 2) - Large market, EU member, strong education sector
4. **Greece** (Phase 3) - EU member, Balkans connection, different language family
5. **Balkans** (Phase 3) - Serbia, North Macedonia, Albania, Bosnia
6. **Baltic States** (Phase 4) - Estonia, Latvia, Lithuania

---

### 5. RAG Semantic Search (Vision Phase 5)
**When**: After Feature 005 and basic matching engine
**Why Now**: Store enough text data during Phase 1 crawling
**Cost Now**: Include description/eligibility text fields in entities
**Benefit Later**: Can vectorize existing data without re-crawling

**Design Decision**:
```java
// Feature 005: Ensure entities have text fields for future vectorization
public class FundingOpportunity {
    private Long id;
    private String title;
    private String description;  // FULL TEXT - for future vectorization
    private String eligibilityCriteria;  // FULL TEXT - for future vectorization
    private String applicationProcess;  // FULL TEXT - for future vectorization
    private String websiteUrl;

    // Phase 5: Add vector embedding fields (don't populate yet)
    @Transient  // Not stored in PostgreSQL
    private float[] descriptionEmbedding;  // 1024-dim vector from BGE-M3
}

// Phase 5: Qdrant vector database integration
public interface VectorSearchService {
    List<FundingOpportunity> semanticSearch(String naturalLanguageQuery, int topK);
    void indexOpportunity(FundingOpportunity opportunity);
    void reindexAll();  // Vectorize existing data
}
```

**Technology Stack** (Phase 5):
- **Qdrant** (vector database, Docker container)
- **BGE-M3** (BAAI/bge-m3, 1024-dim embeddings, multilingual)
- **LangChain4j** (already integrated, has Qdrant support)
- **Reranking**: Use LM Studio llama-3.1-8b for final ranking

**Query Flow** (Phase 5):
```
User: "Find grants for building a private school in Bulgaria serving disadvantaged children"
  ↓
1. Vectorize query (BGE-M3 embedding)
2. Qdrant similarity search (top 50 candidates)
3. Filter by explicit criteria (geography, organization type)
4. Rerank with LLM (top 10 results)
5. Present to user with match scores
```

---

### 6. Contact Intelligence & Deep Crawling (Vision Phase 6)
**When**: After RAG search working well
**Why Now**: ContactIntelligence entity already exists in domain model
**Cost Now**: Zero (entity structure ready)
**Benefit Later**: Add Browserbase integration without schema changes

**Design Decision**:
```java
// Already exists in domain model (no changes needed)
public class ContactIntelligence {
    private Long id;
    private Long organizationId;
    private String fullName;
    private ContactType contactType;  // PROGRAM_OFFICER, GRANTS_MANAGER, etc.
    private String email;
    private String phone;
    private String jobTitle;
    private String linkedinUrl;
    private String notes;
    private BigDecimal confidenceScore;  // How confident we are in this data
}

// Phase 6: Add deep crawling service
public interface DeepCrawlingService {
    ContactIntelligence extractContactFromWebPage(String url);
    List<ContactIntelligence> extractAllContactsFromDomain(String domain);
    boolean validateEmail(String email);  // Email verification service
    boolean validatePhone(String phone);  // Phone validation
}
```

**Technology Stack** (Phase 6):
- **Browserbase** (managed browser automation, Playwright/Puppeteer)
- **Email Verification API** (Hunter.io, ZeroBounce, or similar)
- **Phone Validation API** (Twilio Lookup, Numverify)
- **LinkedIn Scraping** (PhantomBuster or similar, respect rate limits)

**Crawling Strategy** (Phase 6):
```
After Phase 1 metadata judging identifies HIGH_CONFIDENCE candidate:
  ↓
1. Deep crawl website (Browserbase)
2. Extract contact information (LLM-powered)
3. Validate emails/phones (external APIs)
4. Extract funding opportunity details
5. Create FundingOpportunity entities
6. Store contact intelligence
7. Update FundingSource with enriched data
```

---

## Architecture Patterns for Extensibility

### Pattern 1: Optional Fields in Request Objects

**Good**: Add optional fields that future features can use
```java
public class QueryGenerationRequest {
    private SearchEngineType searchEngine;  // Required (Feature 004)
    private FundingSearchCategory category;  // Required (Feature 004)
    private QueryLanguage userLanguage;  // Optional (Feature 005, used in Phase 6)
}
```

**Why**: Existing code ignores optional fields, future code can use them

---

### Pattern 2: Strategy Pattern for Language-Specific Logic

**Good**: Define interface now, implement strategies as needed
```java
public interface QueryGenerationStrategy {
    CompletableFuture<List<String>> generateQueries(
        QueryGenerationRequest request,
        String keywords,
        int numberOfQueries
    );
    SearchEngineType getSearchEngine();
}

// Feature 004: KeywordQueryStrategy, TavilyQueryStrategy
// Phase 6: BulgarianQueryStrategy, RomanianQueryStrategy, MultilingualQueryStrategy
```

**Why**: Can add new strategies without modifying existing code

---

### Pattern 3: Enum-Driven Configuration

**Good**: Add enum values now, configure behavior later
```java
public enum QueryLanguage {
    BULGARIAN("bg", "български"),
    ENGLISH("en", "English");
    // Add more as we expand to new countries
}

// Feature 005: Just define enum
// Phase 6: Add translation service that uses enum
// Phase 8: Add new languages without breaking existing code
```

**Why**: Enums are cheap, future services use them as configuration

---

### Pattern 4: Entity Relationships via IDs (Not Embedded Objects)

**Good**: Reference by ID, lazy load when needed
```java
public class FundingOpportunity {
    private Long id;
    private Long fundingSourceId;  // Reference, not embedded FundingSource object
    // Fetch FundingSource separately when needed
}
```

**Why**: Can add FundingSource entity later without migrating FundingOpportunity

---

### Pattern 5: Feature Flags (Future)

**Phase 7+**: Add feature flags for gradual rollout
```java
public class FeatureFlags {
    boolean multilingualSearchEnabled = false;  // Phase 6
    boolean semanticSearchEnabled = false;      // Phase 5
    boolean applicationAssistanceEnabled = false;  // Phase 7
    boolean deepCrawlingEnabled = false;        // Phase 6
}

// Check flag before using feature
if (featureFlags.multilingualSearchEnabled) {
    queries = multilingualQueryStrategy.generate(request);
} else {
    queries = keywordQueryStrategy.generate(request);
}
```

**Why**: Can deploy code to production but enable features per-client or gradually

---

## Summary: What We're Building in Feature 005

### Implementing NOW:
✅ 5 new enums (FundingSourceType, FundingMechanism, ProjectScale, BeneficiaryPopulation, RecipientOrganizationType)
✅ Expand FundingSearchCategory (25 → 29 values)
✅ QueryLanguage enum (design for future translation)
✅ Multi-dimensional QueryGenerationRequest (7 dimensions)
✅ Weekly scheduling logic (DayOfWeek → FundingSourceType)
✅ Update CategoryMapper for multi-dimensional keywords
✅ Update all tests for new taxonomy

### Designing For (Not Implementing):
🔵 Multilingual translation (enum + fields only, no translation service)
🔵 User profile matching (RecipientOrganization entity structure defined, no matching engine)
🔵 Application assistance workflow (no entities yet, Phase 7+)
🔵 In-country research (as we expand, no code changes needed)
🔵 RAG semantic search (store text fields now, vectorize in Phase 5)
🔵 Deep crawling (existing ContactIntelligence entity, no new code)

### Not Even Thinking About Yet:
⚪ Payment processing and subscription management
⚪ Multi-tenant SaaS infrastructure
⚪ User authentication and authorization
⚪ Email notifications and deadline reminders
⚪ Analytics and reporting dashboards
⚪ Admin panel for content management
⚪ Mobile app (web-first for now)

---

**Last Updated**: 2025-11-04
**Status**: Strategic planning document
**Next Action**: Begin Feature 005 specification with this roadmap as context

---

## Related Documents

- [[vision-and-mission]] - Overall platform vision
- [[feature-completion-tracker]] - Implementation status
- [[funding-industry-terminology]] - Domain language
- [[grant-taxonomy-research-2025-11-04]] - Taxonomy research
