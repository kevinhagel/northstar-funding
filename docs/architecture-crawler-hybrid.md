# Crawler Architecture: Human-AI Hybrid Approach
## Learning from Spring-Crawler (March-August 2024)

**Version**: 0.1
**Last Updated**: 2025-10-18
**Status**: Design Phase

---

## Executive Summary

After 5 months of development on **spring-crawler** (March-August 2024), we learned that:

✓ **Automated discovery works**: AI can find candidate funding sources
✗ **Automated contact extraction fails**: Websites are too diverse, contact intelligence is too valuable to automate

**Solution**: Human-AI hybrid approach
- **Crawler**: Finds candidates (websites, basic info) → Morning Dashboard
- **Humans**: Extract contact intelligence, enrich, validate → Approved FundingSources

---

## Spring-Crawler: Post-Mortem Analysis

### Architecture (What We Built)

```
┌─────────────────────────────────────────────────────────────────┐
│                   AI SEARCH QUERY GENERATION                     │
│  LLM generates targeted keyword searches for funding discovery   │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              SEARCH ENGINE INTEGRATION                           │
│  - AI-generated prompts for Tavily, Perplexity                   │
│  - Keyword searches on Searxng                                   │
│  - Returns: URLs of potential funding sources                    │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   WEB CRAWLING                                   │
│  - Respect robots.txt (proper crawler etiquette)                 │
│  - Parse sitemaps and sitemap indexes                            │
│  - Follow links to discover grant pages                          │
│  - Extract: organization name, program name, amounts, deadlines  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              MULTI-STAGE JUDGING (Confidence Scoring)            │
│  Judge 1: Is this a funding source? (vs. random website)         │
│  Judge 2: Is this relevant? (geography, category)                │
│  Judge 3: Is this high quality? (complete info, credible)        │
│  → Total Confidence Score: 0.0 - 1.0                             │
│  → Threshold: Score > 0.6 = worth processing                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                    KAFKA PIPELINE                                │
│  Candidates passed through Kafka topics:                         │
│  - topic: candidates-discovered                                  │
│  - topic: candidates-enriched                                    │
│  - topic: candidates-with-contacts                               │
│  - topic: candidates-vectorized                                  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              ENRICHMENT PROCESSOR                                │
│  - Fetch robots.txt (more crawling hints)                        │
│  - Parse sitemaps (find more grant pages)                        │
│  - Extract sitemap index (discover program structure)            │
│  - Follow links to related pages                                 │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│         ❌ CONTACT INTELLIGENCE EXTRACTION (FAILED) ❌            │
│                                                                  │
│  ATTEMPTED:                                                      │
│  - Scrape "Contact Us" pages                                     │
│  - Extract emails via regex                                      │
│  - Parse "Staff" or "Team" pages                                 │
│  - Identify program officers                                     │
│  - Extract phone numbers                                         │
│  - Determine decision authority                                  │
│                                                                  │
│  WHY IT FAILED:                                                  │
│  - Vast variety of website designs                               │
│  - Poor/inconsistent HTML structure                              │
│  - Contact info in images, PDFs, JavaScript                      │
│  - No standard schema (schema.org not widely used)               │
│  - Staff pages: tables, cards, lists, images                     │
│  - Titles vary: "Program Officer", "Grants Manager", "Director"  │
│  - Decision authority impossible to determine programmatically   │
│  - Generic emails (info@...) vs. personal emails                 │
│                                                                  │
│  RESULT: 70-80% failure rate in extracting useful contacts       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              VECTORIZATION PROCESSOR                             │
│  - Generate markdown from extracted data                         │
│  - Create vector embeddings                                      │
│  - Store in Qdrant                                               │
│                                                                  │
│  PROBLEM: Garbage in, garbage out                                │
│  - Missing contact intelligence = poor quality data              │
│  - Incomplete funding source records                             │
└─────────────────────────────────────────────────────────────────┘
```

### What Worked ✓

#### 1. AI-Generated Search Queries
**Success**: LLM can generate highly targeted search queries

**Example**:
```
Input: "Find education infrastructure funding in Bulgaria"

AI-Generated Queries:
- "education infrastructure grants Bulgaria"
- "school building renovation funding Eastern Europe"
- "Bulgaria educational facility improvement programs"
- "Balkans K-12 infrastructure funding opportunities"
- "EU Cohesion Fund education Bulgaria"
```

**Retention**: Keep this in new architecture

---

#### 2. Search Engine Integration (Tavily, Perplexity, Searxng)
**Success**: AI-powered search finds relevant results

**Tavily/Perplexity with AI Prompts**:
```
Prompt: "Find organizations providing education grants in Bulgaria.
Focus on: foundations, EU programs, US aid agencies.
Return: Organization names, websites, brief descriptions."

Results:
- America for Bulgaria Foundation (https://us-bulgaria.org)
- EU Erasmus+ Bulgaria (https://...)
- USAID Bulgaria Education Programs (https://...)
```

**Retention**: Keep this, use LM Studio for prompts

---

#### 3. Proper Web Crawling (robots.txt, sitemaps)
**Success**: Respectful, efficient crawling

**Implementation**:
- Check robots.txt before crawling
- Parse sitemaps to find grant pages efficiently
- Follow sitemap indexes for large sites
- Respect crawl delays
- User-Agent: "NorthStarFundingBot/1.0"

**Example - EU Funding Portal**:
```
1. Fetch https://ec.europa.eu/robots.txt
   → Allowed to crawl /info/funding-tenders/*

2. Fetch https://ec.europa.eu/sitemap.xml
   → Sitemap index with links to program-specific sitemaps

3. Fetch https://ec.europa.eu/sitemap-horizons.xml
   → List of all Horizons Europe program pages

4. Crawl each page, extract program details
```

**Retention**: Keep this, essential for ethical crawling

---

#### 4. Multi-Stage Judging with Confidence Scores
**Success**: Filters out noise, prioritizes high-quality candidates

**Pipeline**:
```
Judge 1: Is this a funding source?
  Input: Scraped webpage content
  Output: 0.0-1.0 (is this about funding?)

  Examples:
  ✓ "Apply for our education grant" → 0.95
  ✗ "Our company products" → 0.10

Judge 2: Is this relevant?
  Input: Funding source details
  Output: 0.0-1.0 (geography + category match)

  Examples:
  ✓ "Bulgaria education grants" → 0.90 (target: Bulgaria, Education)
  ✗ "Kenya healthcare funding" → 0.30 (wrong geography, wrong sector)

Judge 3: Is this high quality?
  Input: Extracted data completeness
  Output: 0.0-1.0 (how much info did we extract?)

  Examples:
  ✓ Has: org name, amount, deadline, description → 0.85
  ✗ Has: org name only → 0.40

Total Score: Average(Judge1, Judge2, Judge3)
Threshold: > 0.6 = worth processing
```

**Retention**: Keep this, adapt for candidate filtering

---

#### 5. Kafka Event-Driven Architecture
**Success**: Decoupled, scalable, resilient pipeline

**Topics**:
- `candidates-discovered`: Raw URLs from search
- `candidates-scraped`: Basic info extracted
- `candidates-judged`: Passed confidence threshold
- `candidates-enriched`: Additional info gathered
- `candidates-with-contacts`: Contact intelligence added (FAILED)
- `candidates-vectorized`: Ready for Qdrant

**Benefits**:
- Easy to replay messages (if enrichment fails, reprocess)
- Parallel processing (multiple consumers)
- Monitoring and debugging (see message flow)
- Resilience (if vectorizer crashes, messages wait in queue)

**Retention**: Consider keeping Kafka, or simplify to Redis (Valkey) queues

---

#### 6. Vectorization Pipeline
**Success**: Markdown → Vector embeddings worked well

**Process**:
```
1. Generate Markdown
   Organization: America for Bulgaria Foundation
   Program: Education Infrastructure Grant
   Amount: €25,000 - €150,000
   Deadline: 2025-06-30
   ...

2. Create Embedding (vector)
   [0.123, -0.456, 0.789, ...] (768 dimensions)

3. Store in Qdrant
   Vector + Metadata (org, category, geography, amount)
```

**Retention**: Keep this, use LM Studio embedding model

---

### What Failed ✗

#### Contact Intelligence Extraction: THE WEAK POINT

**The Problem**: Websites are too diverse and poorly structured

**Example 1: America for Bulgaria Foundation**
```
Website Structure:
- Homepage: No contact info
- About page: Generic email (info@us-bulgaria.org)
- Staff page: Table with photos, names, titles
  - Jane Smith, Program Director
  - John Doe, Education Officer
  - Sarah Johnson, Executive Director

Problem:
- Who is the decision maker? (could be any of them)
- Who handles education grants specifically? (unclear)
- No direct emails (only info@... general contact)
- No phone numbers
- No indication of authority level
```

**Example 2: EU Horizons Program**
```
Website Structure:
- Program page: Description, deadlines, amounts
- Contact: Link to "National Contact Points" page
- National Contact Points: List of 27 countries
- Bulgaria entry: Organization name, generic email

Problem:
- No specific person
- Contact changes by country
- Multiple layers of indirection
- Contact info in PDF documents
```

**Example 3: Small Foundation Website**
```
Website Structure:
- Built with Wix/Squarespace/WordPress
- Contact form (no email addresses visible)
- Staff info: First names only ("Contact Lisa for grants")
- Email addresses in JavaScript or images (anti-scraping)

Problem:
- Email extraction blocked
- No structured data
- Contact form only (can't email directly)
```

**Extraction Attempts**:
1. **Regex for emails**: 40% success rate (many sites hide emails)
2. **Parse "Staff" pages**: 30% success rate (too many layouts)
3. **Identify decision makers**: 10% success rate (impossible to determine authority)
4. **Phone numbers**: 25% success rate (often in images or PDFs)

**Conclusion**: Contact intelligence is too valuable and too difficult to automate.

**Solution**: HUMANS extract contact intelligence with AI assistance.

---

### Why Contact Intelligence Matters

**Contact Intelligence = The Most Valuable Asset**

Without good contact intelligence:
- Users can't apply effectively
- Don't know WHO to contact
- Don't know HOW to contact (email, phone, portal)
- Don't know WHEN to contact (office hours, response patterns)
- Miss relationship-building opportunities

**Example User Need**:
```
User: "We want to apply for education funding from America for Bulgaria"

Bad (automated extraction):
Contact: info@us-bulgaria.org (generic email)
→ User sends application to general inbox
→ May never reach the right person
→ No relationship built

Good (human-curated):
Contact: Jane Smith, Education Program Officer
Email: jsmith@us-bulgaria.org
Phone: +1-802-xxx-xxxx
Preferred: Email (responds within 3 business days)
Office Hours: Tue/Thu 2-4 PM EET
Authority: Decision maker for K-12 grants
Notes: Prefers brief LOI (2 pages) before full proposal
→ User contacts Jane directly
→ Builds relationship
→ Higher chance of success
```

---

## New Architecture: Human-AI Hybrid

### Core Principle

**Crawler finds candidates. Humans enrich with contact intelligence.**

```
┌─────────────────────────────────────────────────────────────────┐
│                     NIGHTLY CRAWLER                              │
│  (Automated - runs while humans sleep)                           │
│                                                                  │
│  1. AI generates search queries                                  │
│  2. Search via Tavily, Perplexity, Searxng                       │
│  3. Crawl discovered URLs (robots.txt, sitemaps)                 │
│  4. Extract basic info:                                          │
│     - Organization name                                          │
│     - Program name (if visible)                                  │
│     - Amounts (if visible)                                       │
│     - Deadlines (if visible)                                     │
│     - Source URL                                                 │
│  5. Judge candidates (confidence scoring)                        │
│  6. Pass to Kafka/Redis queue                                    │
│                                                                  │
│  OUTPUT: FundingSourceCandidate records (status: PENDING_REVIEW) │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ Kafka/Redis Queue
                     │ Topic: candidates-for-review
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                   MORNING DASHBOARD                              │
│  (Human-facing - Kevin, Huw log in)                              │
│                                                                  │
│  Shows:                                                          │
│  - 15 new candidates from nightly crawl                          │
│  - Sorted by confidence score (highest first)                    │
│  - Preview: org name, URL, extracted info, confidence            │
│                                                                  │
│  Human Actions:                                                  │
│  - Assign to self (status: PENDING → IN_REVIEW)                 │
│  - Open candidate details                                        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ Human clicks "Review Candidate #1"
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              HUMAN REVIEW + AI ASSISTANCE                        │
│  (Human intelligence + AI tools)                                 │
│                                                                  │
│  Left Panel: Candidate Info                                      │
│  - Source URL: https://us-bulgaria.org/grants                    │
│  - Extracted: "America for Bulgaria Foundation"                  │
│  - Confidence: 0.82                                              │
│                                                                  │
│  Right Panel: Website (embedded iframe or external link)         │
│  - Human visits actual website                                   │
│  - Reads about organization, programs, requirements              │
│                                                                  │
│  Human Actions:                                                  │
│  1. Validate/Create Organization                                 │
│     - Name: America for Bulgaria Foundation                      │
│     - Type: Private Foundation                                   │
│     - Mission: [human reads and enters]                          │
│                                                                  │
│  2. Create/Update FundingProgram (if applicable)                 │
│     - Program: Education Infrastructure Grant                    │
│     - Description: [human reads and enters]                      │
│                                                                  │
│  3. Extract Contact Intelligence (THE KEY PART)                  │
│     AI ASSISTANCE:                                               │
│     - Button: "AI Analyze Staff Page"                            │
│       → LLM reads staff page, extracts names, titles             │
│       → Human validates, adds emails/phones manually             │
│                                                                  │
│     - Button: "Search LinkedIn for Contact"                      │
│       → Helps find program officers                              │
│                                                                  │
│     HUMAN ENTERS:                                                │
│     - Full Name: Jane Smith                                      │
│     - Title: Education Program Officer                           │
│     - Email: jsmith@us-bulgaria.org (human found on staff page)  │
│     - Phone: +1-802-xxx-xxxx (human found on contact page)       │
│     - Contact Level: PROGRAM_LEVEL                               │
│     - Authority: DECISION_MAKER                                  │
│     - Preferred Contact: Email                                   │
│     - Notes: "Responds within 3 days, prefers brief LOI first"   │
│                                                                  │
│  4. Enrich Funding Source Details                                │
│     - Categories: [Education, Infrastructure]                    │
│     - Geographic Eligibility: [Bulgaria]                         │
│     - Amount: €25,000 - €150,000                                 │
│     - Deadline: 2025-06-30                                       │
│     - Requirements: [human reads and summarizes]                 │
│     - Application Process: [human reads and describes]           │
│                                                                  │
│  5. APPROVE or REJECT                                            │
│     IF APPROVED:                                                 │
│       → Creates FundingSource entity                             │
│       → Status: CURRENT                                          │
│       → Queued for vectorization                                 │
│                                                                  │
│     IF REJECTED:                                                 │
│       → Mark as REJECTED                                         │
│       → Reason: "Not a grant, just a news article"               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ Approved FundingSources
                     │ Kafka/Redis Queue: funding-sources-approved
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              VECTORIZATION PROCESSOR                             │
│  (Automated - background job)                                    │
│                                                                  │
│  1. Generate rich markdown (using human-curated data)            │
│  2. Create vector embedding (LM Studio)                          │
│  3. Store in Qdrant with metadata                                │
│  4. Update FundingSource: vectorizedAt, qdrantPointId            │
└─────────────────────────────────────────────────────────────────┘
```

### Key Differences from Spring-Crawler

| Aspect | Spring-Crawler | New Architecture |
|--------|---------------|------------------|
| **Crawler Output** | Attempted full extraction | Basic info only (candidates) |
| **Contact Intelligence** | Automated (FAILED) | Manual with AI assistance |
| **Human Role** | None (fully automated) | Central (review, enrich, validate) |
| **Quality** | 70-80% failure on contacts | 95%+ quality (human-validated) |
| **Throughput** | High volume, low quality | Lower volume, high quality |
| **Dashboard** | None (automated end-to-end) | Morning dashboard for human review |
| **Enrichment** | Automated | Human + AI tools |

---

## Crawler Scope & Boundaries

### What the Crawler DOES (Automated)

✓ **AI-Generated Search Queries**
- Input: Categories, geography, funding types
- LLM generates targeted queries
- Examples: "Bulgaria education infrastructure grants", "EU Cohesion Fund schools"

✓ **Search Engine Integration**
- Tavily API with AI prompts
- Perplexity API with AI prompts
- Searxng keyword searches

✓ **Web Crawling**
- Fetch robots.txt, respect rules
- Parse sitemaps and sitemap indexes
- Follow links to discover grant pages
- Extract HTML content

✓ **Basic Information Extraction**
- Organization name (best effort)
- Program name (if visible)
- Amounts (if visible, regex extraction)
- Deadlines (if visible, date parsing)
- Source URL (always captured)
- Raw HTML content (stored for human review)

✓ **Confidence Scoring**
- Is this a funding source? (0.0-1.0)
- Is this relevant? (geography, category)
- Is this high quality? (completeness)
- Total score → filter threshold

✓ **Candidate Creation**
- Create FundingSourceCandidate record
- Status: PENDING_REVIEW
- Store in PostgreSQL
- Publish to Kafka/Redis queue

### What the Crawler DOES NOT DO (Human Required)

✗ **Contact Intelligence Extraction**
- Reason: Too complex, too valuable
- Human handles: Names, emails, phones, authority level

✗ **Deep Organization Research**
- Reason: Requires judgment
- Human handles: Mission, focus areas, organization type

✗ **Program Analysis**
- Reason: Requires understanding program structure
- Human handles: Program description, hierarchy

✗ **Eligibility Interpretation**
- Reason: Requires nuanced reading
- Human handles: Who can apply, legal requirements

✗ **Validation & Approval**
- Reason: Final quality check needed
- Human handles: Approve/reject decision

---

## Kafka vs. Redis (Valkey) Decision

### Option 1: Keep Kafka (from spring-crawler)

**Pros**:
- Proven in spring-crawler
- Event replay (reprocess candidates)
- Scalability (distributed consumers)
- Monitoring (see message flow)
- Durability (messages persisted)

**Cons**:
- Heavier infrastructure (Zookeeper/KRaft)
- Overkill for single-machine setup?
- More complex to operate

**Use Cases**:
- Long-running enrichment tasks
- Retry logic for failed processing
- Audit trail (who processed what, when)

### Option 2: Use Redis (Valkey) Queues

**Pros**:
- Lighter weight
- Simpler to operate
- Fast (in-memory)
- Pub/sub + queues
- Valkey is Redis fork (fully compatible)

**Cons**:
- Less durable (relies on persistence config)
- No native replay (vs. Kafka's offset management)
- Single-instance (vs. Kafka's distribution)

**Use Cases**:
- Simple job queues
- Caching (complement queues)
- Real-time dashboard updates

### Option 3: Hybrid (Kafka + Redis)

**Architecture**:
- **Kafka**: Long-term event log, critical pipelines
  - candidates-discovered
  - funding-sources-approved
  - vectorization-jobs

- **Redis (Valkey)**: Short-term queues, caching
  - Dashboard real-time updates
  - Background job queues (send email, update cache)
  - Session storage
  - Rate limiting for crawler

### Recommendation

**Start Simple, Add Complexity as Needed**:

**Phase 1** (MVP):
- PostgreSQL: Persistent storage (FundingSourceCandidate, FundingSource)
- Redis (Valkey): Simple queues for background jobs
- Direct API calls for dashboard

**Phase 2** (Scale):
- Add Kafka if:
  - Need event replay
  - Multiple consumers
  - Complex workflows

**Rationale**:
- Mac Studio is powerful but single machine
- Redis is sufficient for job queues
- PostgreSQL provides durability
- Kafka adds value when scaling to multiple machines

---

## Crawler Implementation Plan

### Phase 1: Nightly Discovery

**Goal**: Find candidate funding sources

**Components**:

1. **Query Generator Service**
   ```java
   @Service
   public class QueryGeneratorService {
       public List<String> generateQueries(
           Set<String> categories,
           Set<String> geographies,
           Set<String> fundingTypes
       ) {
           // Use LM Studio to generate targeted queries
           String prompt = buildPrompt(categories, geographies, fundingTypes);
           List<String> queries = llmClient.generate(prompt);
           return queries;
       }
   }
   ```

2. **Search Engine Service**
   ```java
   @Service
   public class SearchEngineService {
       public List<SearchResult> search(String query, SearchEngine engine) {
           return switch(engine) {
               case TAVILY -> tavilyClient.search(query);
               case PERPLEXITY -> perplexityClient.search(query);
               case SEARXNG -> searxngClient.search(query);
           };
       }
   }
   ```

3. **Web Crawler Service**
   ```java
   @Service
   public class WebCrawlerService {
       public CrawlResult crawl(String url) {
           // 1. Check robots.txt
           if (!isAllowedByCrawl(url)) {
               return CrawlResult.blocked();
           }

           // 2. Fetch sitemaps
           List<String> sitemapUrls = fetchSitemaps(url);

           // 3. Crawl pages
           String html = fetchPage(url);

           // 4. Extract basic info
           ExtractedData data = extractBasicInfo(html);

           return CrawlResult.success(data);
       }
   }
   ```

4. **Candidate Judging Service**
   ```java
   @Service
   public class CandidateJudgingService {
       public double calculateConfidence(ExtractedData data) {
           double judge1 = isFundingSource(data);      // 0.0-1.0
           double judge2 = isRelevant(data);           // 0.0-1.0
           double judge3 = isComplete(data);           // 0.0-1.0

           return (judge1 + judge2 + judge3) / 3.0;
       }
   }
   ```

5. **Candidate Creation Service**
   ```java
   @Service
   public class CandidateCreationService {
       public FundingSourceCandidate createCandidate(
           SearchResult searchResult,
           CrawlResult crawlResult,
           double confidenceScore
       ) {
           return FundingSourceCandidate.builder()
               .status(CandidateStatus.PENDING_REVIEW)
               .confidenceScore(confidenceScore)
               .discoverySessionId(currentSessionId)
               .discoveredAt(LocalDateTime.now())
               .discoveredBy("SYSTEM")
               .discoveryMethod("CRAWLER")
               .sourceUrl(crawlResult.getUrl())
               .organizationName(crawlResult.getOrgName())
               .programName(crawlResult.getProgramName())
               .extractedData(crawlResult.getRawHtml())
               .build();
       }
   }
   ```

### Phase 2: Morning Dashboard

**Goal**: Present candidates to humans for review

**Components**:

1. **Dashboard API**
   ```java
   @RestController
   @RequestMapping("/api/dashboard")
   public class DashboardController {

       @GetMapping("/pending-candidates")
       public List<CandidateDTO> getPendingCandidates() {
           return candidateService.findByStatus(CandidateStatus.PENDING_REVIEW)
               .stream()
               .sorted(Comparator.comparing(c -> -c.getConfidenceScore()))
               .map(this::toDTO)
               .toList();
       }

       @PostMapping("/candidates/{id}/assign")
       public void assignToMe(@PathVariable UUID id) {
           candidateService.assignToReviewer(id, currentUser.getId());
       }
   }
   ```

2. **Human Review UI** (separate frontend - React/Vue/Angular)
   - List of pending candidates
   - Click to review
   - Embedded iframe or link to source URL
   - Forms for enrichment
   - AI-assistance buttons

### Phase 3: Vectorization (Automated)

**Goal**: Convert approved FundingSources to Qdrant vectors

**Components**:

1. **Markdown Generator Service**
   ```java
   @Service
   public class MarkdownGeneratorService {
       public String generate(FundingSource fundingSource) {
           // Load related entities
           Organization org = organizationRepo.findById(fundingSource.getOrganizationId());
           FundingProgram program = programRepo.findById(fundingSource.getProgramId());
           List<ContactIntelligence> contacts = contactRepo.findByFundingSourceId(fundingSource.getId());

           // Use template to generate rich markdown
           return markdownTemplate.render(fundingSource, org, program, contacts);
       }
   }
   ```

2. **Vectorization Service**
   ```java
   @Service
   public class VectorizationService {
       public void vectorize(FundingSource fundingSource) {
           // Generate markdown
           String markdown = markdownGenerator.generate(fundingSource);

           // Generate embedding
           float[] embedding = embeddingModel.embed(markdown);

           // Store in Qdrant
           UUID pointId = qdrantClient.upsert(
               collection = "funding_sources_current",
               vector = embedding,
               payload = buildPayload(fundingSource)
           );

           // Update FundingSource
           fundingSource.setVectorizedAt(LocalDateTime.now());
           fundingSource.setQdrantPointId(pointId);
           fundingSource.setMarkdownRepresentation(markdown);
           fundingSourceRepo.save(fundingSource);
       }
   }
   ```

---

## Next Steps

### Immediate (Design Phase)

1. **Finalize crawler scope**: Review boundaries above
2. **Decide Kafka vs. Redis**: Choose queue architecture
3. **Design dashboard UI**: Wireframes for human review
4. **Define AI assistance tools**: What buttons/features for humans?

### Implementation Phase 1

1. **Build crawler**:
   - Query generator (LM Studio)
   - Search engine integration (Tavily, Perplexity, Searxng)
   - Web crawler (robots.txt, sitemaps)
   - Basic extraction
   - Candidate judging

2. **Build database**:
   - Complete entity implementation
   - Flyway migrations
   - Repository layer

3. **Build queue system**:
   - Redis (Valkey) or Kafka
   - Background job processing

### Implementation Phase 2

1. **Build dashboard**:
   - REST API for candidates
   - Frontend UI (React/Vue)
   - Review workflow
   - AI assistance integration

2. **Build vectorization**:
   - Markdown generator
   - Embedding service (LM Studio)
   - Qdrant integration

---

## Open Questions

1. **Kafka or Redis (Valkey)?** Which for queuing?
2. **Crawler frequency**: Nightly only, or continuous?
3. **AI assistance features**: What specific tools help humans most?
4. **Dashboard tech stack**: React? Vue? Angular? HTMX?
5. **Deployment**: Docker on Mac Studio? Kubernetes? Simple JARs?

---

**Document Status**: Design Phase - Awaiting Decisions
**Next Action**: Decide queue architecture (Kafka vs. Redis)
**Owner**: Kevin + Claude
