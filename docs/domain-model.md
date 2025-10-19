# NorthStar Funding Discovery - Domain Model
## Domain-Driven Design Specification

**Version**: 0.1 (Early Phase - Expect Evolution)
**Last Updated**: 2025-10-18
**Status**: Living Document

---

## Ubiquitous Language

The following terms form our shared vocabulary across all stakeholders (developers, researchers, users):

### Core Domain Concepts

**Funding Source**
- PRIMARY DOMAIN CONCEPT
- A discoverable opportunity for obtaining financial resources
- May be a grant program, scholarship, loan program, or other funding mechanism
- Always has an owning Organization and may be part of a Program

**Organization**
- Legal entity that provides or administers funding
- Examples: "America for Bulgaria Foundation", "European Commission", "USAID"
- Has organization-level contact intelligence
- May own multiple Programs

**Funding Program**
- A named initiative within an Organization that provides funding
- Example: "America for Bulgaria Foundation" → "Education Infrastructure Grant Program"
- Has program-level contact intelligence
- May have multiple funding cycles or tranches

**Contact Intelligence**
- Information about people who make or influence funding decisions
- EXISTS AT TWO LEVELS:
  - **Organization-level**: General contacts, administrative staff
  - **Program-level**: Program officers, decision makers for specific grants
- The most valuable asset in the system (encrypted, protected, relationship-tracked)

**Funding Source Candidate**
- A potential Funding Source discovered but not yet validated
- Created by: Nightly crawler OR human researcher
- Requires human review and enrichment
- Lifecycle: DISCOVERED → IN_REVIEW → APPROVED/REJECTED

**Discovery Session**
- A single execution of the automated crawler
- Tracks: search queries, AI models used, results found, performance metrics
- Runs nightly (scheduled) to discover new candidates

**Category**
- Domain classification for Funding Sources
- Examples: Education, Infrastructure, Scholarships, Teacher Training, Youth Development
- Evolves over time as we understand funding landscape
- Used for semantic search and filtering

**Organization Taxonomy**
- Hierarchical classification of organizations that provide funding
- Categories: Government, Foundation, Corporate, Religious, Academic, Individual/Crowdfunding
- Types within categories: Private Foundation, EU Program, Bilateral Aid Agency, etc.
- Distinction between FUNDERS (provide money) and AGGREGATORS (list funding sources)
- Used for categorization, filtering, and understanding funder patterns

**RAG (Retrieval Augmented Generation)**
- The client-facing search architecture for the platform
- Natural language queries → Vector similarity search → Relevant funding sources
- Flow: FundingSource → Markdown → Vector Embeddings → Qdrant → Search Results
- Enables semantic search: "Show me education grants for Bulgaria" finds relevant sources
- Quality depends on richness of metadata (organization, category, geography, contact intelligence)

**Vectorization**
- Process of converting approved FundingSource records into searchable embeddings
- FundingSource (PostgreSQL structured data) → Markdown representation → Vector embedding → Qdrant
- Markdown includes: Organization context, categories, geography, amounts, deadlines, requirements
- Embeddings capture semantic meaning for similarity search
- Metadata stored alongside vectors for filtering and ranking

---

## Domain Model - Core Entities

### 1. Organization

**Purpose**: Represents legal entities that provide or administer funding

**Key Attributes**:
```
- organizationId: UUID (Primary Key)
- name: String (e.g., "America for Bulgaria Foundation")
- website: String (e.g., "https://us-bulgaria.org")
- description: Text (mission statement, background)
- organizationType: Enum (FOUNDATION, GOVERNMENT_AGENCY, NGO, CORPORATE, MULTILATERAL, etc.)
- headquartersLocation: String (city, country)
- geographicScope: List<String> (countries/regions served)
- establishedYear: Integer
- annualFundingVolume: BigDecimal (nullable - if known)
- primaryLanguages: List<String>
- tags: Set<String> (flexible categorization)

// Discovery metadata
- discoveredAt: LocalDateTime
- discoveredBy: UUID (AdminUser or System)
- lastUpdatedAt: LocalDateTime
- status: OrganizationStatus (ACTIVE, INACTIVE, MERGED, etc.)

// Rich text fields
- missionStatement: Text
- fundingFocus: Text (areas of interest)
- applicationGuidelines: Text (general guidance)
```

**Relationships**:
- One Organization → Many FundingPrograms
- One Organization → Many OrganizationContacts

**Business Rules**:
- Organization.name must be unique
- Organization.website should be validated/normalized
- Cannot delete Organization if active FundingPrograms exist

---

### 2. FundingProgram

**Purpose**: Named funding initiative within an Organization

**Key Attributes**:
```
- programId: UUID (Primary Key)
- organizationId: UUID (Foreign Key)
- programName: String (e.g., "Education Infrastructure Grant Program")
- programUrl: String (specific program page URL)
- description: Text
- programType: Enum (GRANT, SCHOLARSHIP, LOAN, IN_KIND, MATCHING, etc.)

// Financial details
- fundingAmountMin: BigDecimal
- fundingAmountMax: BigDecimal
- currency: String (EUR, USD, BGN, etc.)
- totalBudgetPerCycle: BigDecimal (nullable)

// Eligibility
- eligibleCountries: List<String>
- eligibleOrganizationTypes: List<String> (NGO, School, Municipality, etc.)
- eligibilityRequirements: Text

// Application lifecycle
- applicationProcess: Text (how to apply)
- typicalDurationMonths: Integer (project duration)
- multiYearFunding: Boolean
- matchingRequired: Boolean
- matchingPercentage: Integer (nullable)

// Categories
- categories: Set<String> (Education, Infrastructure, etc.)
- tags: Set<String>

// Discovery metadata
- discoveredAt: LocalDateTime
- lastUpdatedAt: LocalDateTime
- status: ProgramStatus (ACTIVE, SUSPENDED, CLOSED, etc.)
```

**Relationships**:
- One FundingProgram → One Organization
- One FundingProgram → Many FundingSources (cycles)
- One FundingProgram → Many ProgramContacts

**Business Rules**:
- FundingProgram must belong to an Organization
- Categories should align with system-wide category taxonomy

---

### 3. FundingSource (Approved)

**Purpose**: A validated, searchable funding opportunity

**Key Attributes**:
```
- fundingSourceId: UUID (Primary Key)
- candidateId: UUID (Foreign Key - tracks origin)
- organizationId: UUID (Foreign Key)
- programId: UUID (Foreign Key - nullable for one-off grants)

// Core identity
- title: String (display name for search results)
- sourceUrl: String (authoritative URL)
- description: Text

// Financial details
- fundingAmountMin: BigDecimal
- fundingAmountMax: BigDecimal
- currency: String

// Application cycle (THIS cycle)
- applicationDeadline: LocalDate
- awardAnnouncementDate: LocalDate (nullable)
- projectStartDate: LocalDate (nullable)
- applicationUrl: String (where to apply)
- applicationInstructions: Text

// Eligibility
- geographicEligibility: List<String>
- organizationTypes: List<String>
- requirementsSummary: Text

// Categorization
- categories: Set<String>
- tags: Set<String>

// Status
- isActive: Boolean
- isRecurring: Boolean (annual, biannual, etc.)
- nextCycleExpected: LocalDate (nullable)

// Approval audit
- approvedBy: UUID (AdminUser)
- approvedAt: LocalDateTime
- lastVerifiedAt: LocalDateTime

// Vector search metadata (Qdrant RAG)
- vectorizedAt: LocalDateTime (nullable)
- vectorEmbeddingVersion: String (nullable)
- qdrantPointId: UUID (nullable - ID in Qdrant vector database)
- markdownRepresentation: Text (nullable - the markdown used for embedding)
```

**Relationships**:
- One FundingSource → One Organization
- One FundingSource → One FundingProgram (nullable)
- One FundingSource → One FundingSourceCandidate (origin)
- One FundingSource → Many Contacts (via organization and program)

**Business Rules**:
- Must be approved before vectorization
- applicationDeadline must be validated (not in past for active sources)
- At least one category required

---

### 4. FundingSourceCandidate (Existing - Refined)

**Purpose**: Discovered funding opportunity pending validation

**Key Attributes**:
```
// Identity & Status
- candidateId: UUID (Primary Key)
- status: CandidateStatus (PENDING_REVIEW, IN_REVIEW, APPROVED, REJECTED, DUPLICATE)
- confidenceScore: Double (0.0-1.0, AI-generated)

// Discovery audit
- discoverySessionId: UUID (nullable - null if human-discovered)
- discoveredAt: LocalDateTime
- discoveredBy: String (SYSTEM or AdminUser UUID)
- discoveryMethod: String (CRAWLER, MANUAL_SEARCH, REFERRED, etc.)
- sourceUrl: String (where discovered)

// Review workflow
- assignedReviewerId: UUID (nullable)
- reviewStartedAt: LocalDateTime (nullable)
- approvedBy: UUID (nullable)
- approvedAt: LocalDateTime (nullable)
- rejectedBy: UUID (nullable)
- rejectedAt: LocalDateTime (nullable)
- rejectionReason: String (nullable)

// Extracted data (RAW - unstructured)
- organizationName: String (nullable - may not be extracted yet)
- programName: String (nullable)
- extractedData: JSON String (all scraped data)
- description: Text

// Enrichment tracking
- enrichmentLevel: Enum (MINIMAL, PARTIAL, COMPLETE)
- lastEnhancedAt: LocalDateTime (nullable)

// Duplicate detection
- duplicateOfCandidateId: UUID (nullable)
- similarCandidateIds: List<UUID> (potential duplicates)
```

**Lifecycle**:
1. **PENDING_REVIEW**: Just discovered, awaiting assignment
2. **IN_REVIEW**: Assigned to Huw/Kevin, being researched
3. **APPROVED**: Validated → Creates FundingSource entity
4. **REJECTED**: Not valid/relevant
5. **DUPLICATE**: Duplicate of existing candidate

**Relationships**:
- One Candidate → One DiscoverySession (nullable)
- One Candidate → Many EnhancementRecords

---

### 5. ContactIntelligence (Refined)

**Purpose**: People who make or influence funding decisions

**Key Attributes**:
```
// Identity
- contactId: UUID (Primary Key)
- contactLevel: Enum (ORGANIZATION_LEVEL, PROGRAM_LEVEL)

// Foreign Keys (one must be present)
- organizationId: UUID (nullable)
- programId: UUID (nullable)
- fundingSourceId: UUID (nullable)

// Classification
- contactType: ContactType (PROGRAM_OFFICER, FOUNDATION_STAFF, GOVERNMENT_OFFICIAL, etc.)
- authorityLevel: AuthorityLevel (DECISION_MAKER, INFLUENCER, INFORMATION_ONLY)

// Personal information
- fullName: String
- title: String
- department: String (nullable)
- officeAddress: String (nullable)

// PROTECTED (encrypted at application layer)
- email: String (encrypted)
- phone: String (encrypted)
- linkedInProfile: String (nullable, encrypted)

// Communication intelligence
- preferredContactMethod: Enum (EMAIL, PHONE, LINKEDIN, FORMAL_APPLICATION)
- communicationPreference: String (best times, notes)
- responsePattern: String (typical response time, style)
- languagePreference: String

// Relationship intelligence
- referralSource: String (how did we learn about this contact?)
- relationshipNotes: Text
- lastContactedAt: LocalDateTime (nullable)
- relationshipStrength: Enum (COLD, WARM, ESTABLISHED)

// Validation
- validatedAt: LocalDateTime (nullable)
- validatedBy: UUID (AdminUser)
- isActive: Boolean
- inactiveReason: String (nullable - left organization, retired, etc.)

// Audit
- createdAt: LocalDateTime
- createdBy: UUID (AdminUser)
- lastUpdatedAt: LocalDateTime
```

**Relationships**:
- One Contact → One Organization (nullable)
- One Contact → One FundingProgram (nullable)
- One Contact → One FundingSource (nullable)

**Business Rules**:
- Must have at least one: organizationId OR programId OR fundingSourceId
- Email/Phone must be encrypted before storage
- Cannot delete - mark as inactive instead (preserve history)

---

### 6. DiscoverySession (Existing - Good as-is)

**Purpose**: Audit trail of nightly crawler executions

Key features already modeled:
- Search engine tracking (Searxng, Tavily, Perplexity)
- LLM model metadata
- Performance metrics
- Error tracking

**Enhancement**: Add discovery method context
```
- targetCategories: List<String> (what categories was this session targeting?)
- geographicFocus: String (Bulgaria, Eastern Europe, Europe)
```

---

## Supporting Entities

### 7. Category (New)

**Purpose**: Structured taxonomy for funding classification

**Key Attributes**:
```
- categoryId: UUID (Primary Key)
- name: String (e.g., "Education", "Infrastructure")
- parentCategoryId: UUID (nullable - for hierarchies)
- description: Text
- isActive: Boolean
- createdAt: LocalDateTime
- usageCount: Integer (how many FundingSources use this?)
```

**Examples**:
- Education
  - K-12 Education
  - Higher Education
  - Teacher Training
  - Educational Infrastructure
- Infrastructure
  - School Buildings
  - Technology Infrastructure
- Scholarships
  - Undergraduate Scholarships
  - Graduate Scholarships
- Youth Development
- Vulnerable Populations

---

### 8. EnhancementRecord (Existing - Good)

Tracks human and AI contributions to candidates during review.

---

### 9. AdminUser (Existing)

Users of the system (Kevin, Huw, others) who:
- Review candidates
- Enrich with contact intelligence
- Validate funding sources
- Use the researcher dashboard

---

## Discovery Workflows

### Workflow 1: Nightly Automated Discovery

```
1. DiscoverySession STARTS (scheduled job)
   ↓
2. LLM generates search queries
   - Input: Categories, geographic focus, funding types
   - Output: List of targeted search queries
   ↓
3. Execute queries across search engines
   - Searxng, Tavily, Perplexity
   ↓
4. Scrape discovered URLs
   - Extract: organization name, program name, amounts, deadlines
   - Store in extractedData (JSON)
   ↓
5. Create FundingSourceCandidate records
   - status: PENDING_REVIEW
   - confidenceScore: AI-generated
   - discoverySessionId: link to this session
   ↓
6. Duplicate detection
   - Check against existing candidates
   - Mark duplicates
   ↓
7. DiscoverySession COMPLETES
   - Log metrics, errors
```

### Workflow 2: Human-Initiated Discovery

```
1. Huw logs into Researcher Dashboard
   ↓
2. Huw searches: "Bulgaria education grants 2025"
   ↓
3. Huw finds: "America for Bulgaria Foundation"
   ↓
4. Huw clicks "Add as Candidate"
   ↓
5. System creates FundingSourceCandidate
   - status: IN_REVIEW
   - discoveredBy: Huw's UUID
   - discoveryMethod: MANUAL_SEARCH
   - assignedReviewerId: Huw's UUID (auto-assign)
   ↓
6. Huw visits the actual website
   ↓
7. Huw enriches candidate:
   - Creates Organization (if new)
   - Creates FundingProgram (if applicable)
   - Extracts organization-level contacts
   - Extracts program-level contacts
   - May use AI assistance (LLM summarizes page)
   ↓
8. Huw approves candidate
   ↓
9. System creates FundingSource entity
   - Copies enriched data
   - Links to Organization, Program, Contacts
   - status: ACTIVE
   ↓
10. System triggers vectorization job
    - Convert to Markdown
    - Generate embeddings
    - Store in Qdrant
```

### Workflow 3: Morning Review Process

```
1. Morning: Huw logs into Dashboard
   ↓
2. Dashboard shows:
   - 15 new candidates from nightly crawler (PENDING_REVIEW)
   - 3 candidates already IN_REVIEW by Kevin
   ↓
3. Huw assigns himself 5 candidates
   - status: PENDING_REVIEW → IN_REVIEW
   - assignedReviewerId: Huw's UUID
   - reviewStartedAt: now()
   ↓
4. For each candidate:
   a. View extracted data
   b. Visit source URL
   c. Assess: Is this valid? Interesting?

   IF VALID:
     d. Create/Update Organization
     e. Create/Update FundingProgram
     f. Extract Contact Intelligence
        - Organization-level contacts
        - Program-level contacts
     g. Categorize (select from categories)
     h. Add tags
     i. APPROVE → Creates FundingSource

   IF NOT VALID:
     d. REJECT → Set rejectionReason

   IF DUPLICATE:
     d. Link to original candidate
     e. Mark as DUPLICATE
   ↓
5. Approved FundingSources queued for vectorization
```

### Workflow 4: Vectorization for RAG Search

**Purpose**: Convert approved FundingSources to searchable vector embeddings in Qdrant

```
1. FundingSource APPROVED (from Workflow 3)
   ↓
2. Generate Markdown Representation
   - Title: FundingSource.title
   - Organization: Organization.name + description + mission
   - Program: FundingProgram.name + description (if exists)
   - Financial Details: amounts, currency, matching requirements
   - Geographic Eligibility: Countries, regions, EU membership
   - Categories: All assigned categories + tags
   - Eligibility Requirements: Organization types, requirements summary
   - Application Info: Deadlines, process, URL
   - Contact Intelligence: Organization contacts, program contacts (decision makers)
   - Full Description: Rich text with all context
   ↓
3. Generate Vector Embedding
   - Input: Markdown text
   - Model: [TBD - OpenAI, local embedding model, etc.]
   - Output: Vector embedding (e.g., 1536 dimensions)
   ↓
4. Store in Qdrant Vector Database
   - Vector: embedding
   - Payload (metadata):
     - fundingSourceId (UUID)
     - organizationId (UUID)
     - organizationName (String)
     - organizationType (Enum as String)
     - categories (List<String>)
     - geographicEligibility (List<String>)
     - fundingAmountMin, fundingAmountMax (BigDecimal)
     - applicationDeadline (LocalDate)
     - tags (List<String>)
   - Collection: "funding_sources"
   ↓
5. Update FundingSource record
   - vectorizedAt: now()
   - vectorEmbeddingVersion: "v1.0"
   - qdrantPointId: UUID from Qdrant
```

### Workflow 5: Client RAG Search (Natural Language)

**Purpose**: Enable clients to find funding sources using natural language queries

```
CLIENT QUERY: "Show me education grants for Bulgaria with amounts over €50,000"

1. Client submits natural language query
   ↓
2. Query Processing
   - Generate embedding for query text
   - Extract filters from query:
     - categories: ["Education"]
     - geography: ["Bulgaria"]
     - fundingAmountMin: ≥ 50000 EUR
   ↓
3. Qdrant Vector Search
   - Vector similarity search on embedding
   - Apply metadata filters (category, geography, amount)
   - Return top N matches (e.g., 20 results)
   - Ranked by similarity score
   ↓
4. Re-rank and Enhance (optional)
   - LLM re-ranks results based on query intent
   - Add explanation: "Why this funding source matches your query"
   ↓
5. Return Results to Client
   - List of FundingSources with:
     - Title, organization, amount, deadline
     - Match score (similarity + relevance)
     - Explanation
     - Link to full details
   ↓
6. Track Search Analytics
   - Log query, results, user clicks
   - Use for improving RAG quality
```

**Key Design Decisions**:

1. **Markdown Format**: Rich, human-readable representation
   - Includes ALL context: organization, program, categories, geography
   - Natural language descriptions
   - Hierarchical structure (organization → program → funding source)

2. **Embedding Model**: [TBD - Research needed]
   - Options: OpenAI embeddings, local models (sentence-transformers), multilingual models
   - Must support English + Bulgarian (and other Eastern European languages)

3. **Metadata Filtering**: Hybrid search
   - Vector similarity (semantic meaning)
   - + Metadata filters (exact matches on geography, amount, deadline)
   - Qdrant supports this natively

4. **Freshness**: Re-vectorization strategy
   - When FundingSource updated → regenerate markdown → re-embed → update Qdrant
   - Track vectorEmbeddingVersion to manage schema changes

---

## Domain Model Evolution Strategy

### Phase 1: Core Discovery (CURRENT)
**Focus**: Build a rich validated database

**Entities**:
- FundingSourceCandidate ✓
- FundingSource (new)
- Organization (new)
- FundingProgram (new)
- ContactIntelligence ✓ (refined)
- DiscoverySession ✓
- EnhancementRecord ✓
- AdminUser ✓
- Category (new)

**Outcomes**:
- Nightly crawler operational
- Human review dashboard functional
- Rich contact intelligence captured
- 100+ validated funding sources

### Phase 2: RAG Search with Qdrant (CONFIRMED)
**Focus**: Enable AI-powered semantic search for clients

**Technology Stack**:
- PostgreSQL: Structured data (FundingSource, Organization, etc.)
- Qdrant: Vector database for semantic search
- Embedding Model: [TBD - OpenAI, sentence-transformers, multilingual]
- LLM: Query understanding, re-ranking, explanations

**New Entities**:
- SearchQuery (user search history, analytics)
- SearchResult (track which results users clicked)
- VectorSyncLog (track vectorization jobs)

**Enhancements**:
- FundingSource → Markdown generation
- Markdown → Vector embeddings (Qdrant)
- Natural language search API

### Phase 3: Multi-Tenant SaaS (FUTURE)
**Focus**: Client accounts and subscriptions

**New Entities**:
- ClientOrganization (paying customers)
- Subscription
- UsageMetrics
- SavedSearch

**Enhancements**:
- Row-level security
- Client-specific views
- Usage tracking

### Phase 4: Historical Tracking (FUTURE)
**Focus**: Application cycles, funding trends

**New Entities**:
- FundingCycle (track recurring grants)
- ApplicationAttempt (if tracking actual applications)
- FundingTrends (analytics)

---

## Design Principles

### 1. Start Simple, Evolve
- Model what we KNOW now
- Leave room for growth
- Don't over-engineer for uncertain futures

### 2. Ubiquitous Language
- Use "Funding Source" consistently (not "grant", "opportunity", etc.)
- Organization → Program → Funding Source hierarchy
- Contact Intelligence (not "contacts", not "leads")

### 3. Contact Intelligence is Sacred
- Encrypted at rest (email, phone)
- Two-level hierarchy (organization, program)
- Relationship-tracked
- Never deleted (mark inactive)

### 4. Human-AI Collaboration
- AI discovers, humans validate
- AI suggests, humans refine
- Track WHO did WHAT (EnhancementRecord)

### 5. Audit Everything
- Who discovered (system or human)
- When reviewed
- Who approved/rejected
- Enhancement history

### 6. Flexible Categorization
- Categories (structured taxonomy)
- Tags (flexible, emergent)
- Both supported for search

---

## Open Questions for Iteration

1. **Historical Cycles**: How do we handle recurring grants?
   - New FundingSource each cycle?
   - Or FundingCycle entity linking to Program?

2. **Geographic Modeling**: String list vs. structured geography entity?
   - Countries: ISO codes?
   - Regions: How to model "Eastern Europe"?

3. **Duplicate Detection**: What algorithm?
   - URL similarity?
   - Name fuzzy matching?
   - AI-powered semantic comparison?

4. **Markdown Format**: What structure for vectorization?
   - Template-based generation?
   - Include contact intelligence?

5. **Category Taxonomy**: Flat or hierarchical?
   - Education → K-12 Education?
   - Or just flat tags?

---

## Next Steps

### Implementation Priority

1. **Create missing entities**:
   - Organization
   - FundingProgram
   - FundingSource
   - Category

2. **Refine existing entities**:
   - ContactIntelligence (add contactLevel, relationships)
   - FundingSourceCandidate (add enrichmentLevel)

3. **Database schema**:
   - Flyway migrations for new tables
   - Foreign key constraints
   - Indexes for search performance

4. **Repository layer**:
   - Spring Data JDBC repositories
   - Custom queries for complex searches

5. **Service layer**:
   - Candidate enrichment workflow
   - Approval workflow (Candidate → FundingSource)
   - Contact intelligence encryption

6. **Dashboard API**:
   - List pending candidates
   - Assign reviewer
   - Enrichment forms
   - Approve/reject actions

---

**Document Status**: Draft for feedback
**Next Review**: After initial entity implementation
