# Project Overview - NorthStar Funding Discovery

**Date**: 2025-10-31
**Status**: Active Development
**Tags**: #project #overview #architecture #funding-discovery

---

## What Is This Project?

**NorthStar Funding Discovery** is an automated, AI-powered **SaaS platform** that discovers, evaluates, and organizes funding opportunities for educational and social impact organizations across Eastern Europe and beyond.

### The Vision: Funding Discovery as a Service

**Business Model**: Subscription-based platform where paying clients can:
- Search accumulated funding sources using natural language AI-assisted search
- Find relevant funding opportunities for their specific needs
- Access detailed information on how to apply, deadlines, requirements
- Track opportunities over time with alerts for new matches

**Target Market**:
- Educational institutions (schools, universities)
- NGOs and social impact organizations
- Municipalities and local governments
- Small foundations seeking grant opportunities
- Initially focused on **Eastern Europe** (Bulgaria, Balkans, Central Europe, EU)
- Expanding to global coverage over time

### The Inspiration: British Centre Burgas & NorthStar Foundation

**Original Use Case** (2023-2024): Finding funding for **British Centre Burgas school**

**Expanded Vision** (2024-2025): After exploring sites like fundsforngos.org and candid.org, we realized we could build a better solution:
- Crawl and scrape the web to discover funding organizations and programs
- Parse content and convert to markdown
- Vectorize into Qdrant for semantic search
- Provide AI-assisted natural language search interface
- Serve multiple paying clients, not just ourselves

**Current Inspiration**: **The NorthStar Foundation Bulgaria school** proposal (Burgas school for ages 4-18, serving disadvantaged children) serves as our primary test case and validates the urgent need for this platform.

---

## The Core Problem

### Manual Funding Discovery Is Broken

**Traditional approach**:
1. Google search: "education grants Bulgaria"
2. Click through 50+ results
3. Manually read each website
4. Extract: amounts, deadlines, eligibility
5. Track in spreadsheet
6. Repeat weekly (sources change)
7. Miss opportunities (human error, time constraints)

**This doesn't scale.**

For comprehensive discovery covering:
- 100+ funder organizations
- 500+ active funding programs
- Weekly/monthly deadline changes
- Multiple search engines and databases

**Manual search requires 20-40 hours per week.**

---

### Why Pure Automation Also Failed

**The Early Approach**: Build a web crawler to automatically scrape funding sites.

**The Problem We Hit**: We could discover **hundreds or thousands** of funding candidate websites, but **design disparity made scraping impossible**:

**Contact Intelligence Example**:
- **Domain**: us-bulgaria.org (Organization website)
- **Organization**: America for Bulgaria Foundation
- **Programs**: Multiple programs, each structured differently
- **Contact Info**: Email on "Contact" page? "About" page? Footer? Program-specific pages? Hidden in PDF?

**The Variability**:
- Organization names: In header? Footer? About page? Meta tags?
- Programs: Separate pages? Tabs? Dropdowns? PDFs?
- Emails: `mailto:` links? Obfuscated? Contact forms only? Hidden behind JavaScript?
- Phone numbers: International format? Local format? Extensions? Multiple offices?
- Amounts: Currency symbols? Ranges? Per-year vs total? Buried in prose?
- Deadlines: "Rolling"? "Quarterly"? Specific dates? Fiscal year references?

**No Standard Schema**: Unlike e-commerce (schema.org), funding organizations have no standard markup.

**The Conclusion**: Pure automation cannot reliably extract contact intelligence and structured data from highly variable website designs.

**The Breakthrough**: Human-AI Hybrid Model
- **AI handles**: Discovery (search engines), filtering (metadata judging), initial assessment
- **Humans handle**: Contact extraction, program hierarchy, data enrichment, final approval
- **Result**: Scalable (AI processes volume) + High quality (humans handle edge cases)

---

## The Solution: Human-AI Hybrid Discovery

### Automated Discovery Pipeline

```
Search Engines → Discovery → Judging → Human Review → Approved Sources
(Searxng, Tavily, Perplexity)
```

### What AI/Automation Does
1. **Search**: Query multiple engines simultaneously (3x parallelization)
2. **Discover**: Extract domains from search results
3. **Deduplicate**: Track domains, prevent reprocessing
4. **Judge**: AI evaluates metadata (title, snippet) for funding relevance
5. **Filter**: Only high-confidence candidates (>= 0.6) proceed
6. **Queue**: Present best candidates to humans

### What Humans Do
1. **Review**: Evaluate AI-filtered candidates
2. **Enrich**: Extract contact intelligence (email, phone)
3. **Decide**: Approve, reject, or blacklist
4. **Relate**: Build organization/program hierarchies
5. **Search**: Natural language queries against approved sources (future RAG)

### Result
- **10x faster discovery** (automated search)
- **Higher quality** (AI filters noise)
- **Never forget** (persistent domain tracking)
- **Always current** (nightly/weekly refresh)

---

## The Complete Platform Vision

### Backend: Microservices Architecture

**Planned Full Architecture** (evolved from early Spring Crawler):

**Discovery & Enrichment Services**:
- Web crawler (Browserbase for JavaScript rendering)
- Search engine integration (Searxng, Tavily, Perplexity)
- Content scraper and parser (HTML → Markdown)
- Metadata extractor (amounts, deadlines, requirements)
- Contact intelligence extractor (emails, phones)
- Organization/program hierarchy builder

**Data Processing Services**:
- Vector embedding service (LM Studio + BGE-M3)
- Qdrant vector database (semantic search)
- PostgreSQL (relational data, audit trails)
- Kafka event streaming (service coordination)
- Redis/Valkey (caching, session management)

**Intelligence Services**:
- AI query understanding (natural language → structured queries)
- Relevance scoring and ranking
- Duplicate detection
- Quality assessment
- Trend analysis

### Frontend: Customer-Facing Platform

**User Experience**:
- **Natural Language Search**: "Find education infrastructure grants in Bulgaria over €100,000"
- **AI-Assisted Query Refinement**: Suggest filters, categories, geographies
- **Rich Results Display**: Organizations, programs, amounts, deadlines, requirements
- **Application Guidance**: Step-by-step how to apply
- **Saved Searches**: Alert on new matches
- **Portfolio Management**: Track applications, deadlines, outcomes

**Inspired By**:
- **candid.org**: Comprehensive funder database, rich search
- **fundsforngos.org**: NGO-focused, curated opportunities
- **Our Advantage**: AI-powered discovery + semantic search + automated updates

### Geographic Expansion Strategy

**Phase 1** (Current): **Bulgaria & Burgas**
- British Centre Burgas school
- NorthStar Foundation Bulgaria school
- Local NGOs and educational institutions

**Phase 2**: **Balkans & Eastern Europe**
- Romania, North Macedonia, Serbia, Albania, Greece
- Poland, Czech Republic, Hungary, Slovakia
- Baltic States (Estonia, Latvia, Lithuania)

**Phase 3**: **European Union**
- All 27 EU member states
- EU programs (Horizons Europe, Erasmus+, Cohesion Funds)
- EEA/Norway Grants

**Phase 4**: **Global**
- US foundations with international programs
- Multilateral organizations (World Bank, UN agencies)
- Bilateral aid agencies (USAID, GIZ, JICA)
- Global corporate foundations

### Revenue Model

**Subscription Tiers**:
- **Basic**: Search access, limited results
- **Professional**: Full search, saved searches, alerts
- **Enterprise**: API access, custom integrations, white-label

**Target Customers**:
- Small-medium NGOs (€50-200/month)
- Educational institutions (€100-500/month)
- Municipalities (€200-1000/month)
- Grant consultants (€300-1500/month)
- Large NGOs/foundations (€1000+/month, API access)

**Market Opportunity**: Thousands of NGOs and educational institutions in Eastern Europe spend significant time on funding discovery. Even capturing 1% of this market represents substantial revenue.

---

## Technology Stack

### Core Technologies
- **Java 25**: Virtual Threads for parallel I/O
- **Spring Boot 3.5.6**: Application framework
- **Spring Data JDBC**: Database access
- **PostgreSQL 16**: Relational database (Mac Studio)
- **Vavr 0.10.7**: Functional programming
- **Lombok**: Boilerplate reduction

### Fault Tolerance & HTTP
- **Resilience4j 2.2.0**: Circuit breakers, retry logic
- **Spring RestClient**: HTTP calls (Spring 6.1+)
- **Spring WebFlux**: Reactive search adapters

### AI Infrastructure (Mac Studio @ 192.168.1.10)
- **LM Studio**: Local LLM inference (port 1234)
- **Search engines**:
  - Searxng (port 8080, self-hosted)
  - Tavily (API)
  - Perplexity (API)
- **Qdrant** (future): Vector database for RAG

### Testing
- **JUnit 5** + Spring Boot Test
- **TestContainers**: PostgreSQL integration tests
- **REST Assured**: API testing
- **Mockito**: Unit testing

---

## Architecture Principles

### 1. Human-AI Collaboration
- AI handles volume and speed
- Humans provide judgment and context
- Together: better than either alone

### 2. Domain-Level Deduplication
- Track domains (not individual URLs)
- Simple extraction: `java.net.URI.getHost()`
- Prevents reprocessing same sites

### 3. Metadata-First Judging
- Phase 1: Judge using search engine metadata (title, snippet)
- Phase 2: Deep crawl only high-confidence candidates
- Saves time and resources

### 4. Circuit Breaker Protection
- Independent circuit breakers per search engine
- System continues with working engines if one fails
- Graceful degradation

### 5. Virtual Threads for Parallelization
- Java 25 Virtual Threads (Project Loom)
- Parallel search across 3 engines
- 3x speedup vs sequential
- Scales to hundreds of concurrent searches

### 6. Persistent Domain Registry
- PostgreSQL stores domain blacklist, quality metrics
- Never reprocess blacklisted domains
- Track "no funds this year" with revisit dates

---

## Current Status (Feature 003 Complete)

### ✅ Implemented
- Core domain model (Candidate, Domain, Session, Organization)
- Search engine adapters (Searxng, Tavily, Perplexity)
- Circuit breaker protection per engine
- Virtual Threads parallel execution
- Domain-level deduplication
- 7-day query library (Monday-Sunday schedules)
- Nightly discovery scheduler
- Integration tests (MondayNightlyDiscoveryTest, DomainDeduplicationTest)

### 🚧 In Progress (Feature 004)
- AI query generation (LM Studio)
- Metadata judging with confidence scoring
- Pending/blacklist workflow

### 📋 Planned
- RAG search system (Qdrant + embeddings)
- Deep web crawling (Browserbase)
- Contact intelligence extraction
- Organization hierarchy modeling
- Natural language search interface

---

## Data Model Highlights

### Core Entities

**FundingSourceCandidate**
- Status flow: NEW → PENDING_CRAWL → CRAWLED → ENHANCED → JUDGED
- Tracks discovery method, confidence scores, metadata

**Domain**
- Domain registry with blacklist, quality tracking
- Prevents reprocessing same domains
- Supports "no funds current year" with revisit dates

**DiscoverySession**
- Tracks nightly/weekly discovery runs
- Links to search session statistics
- Session type, status, metrics

**SearchQuery**
- Query library entity (7-day schedule)
- Tags: GEOGRAPHY:Bulgaria, CATEGORY:Education
- Target engines: SEARXNG, TAVILY, PERPLEXITY

**SearchSessionStatistics**
- Per-engine performance metrics
- Queries executed, results returned, response times, failures

---

## Development Environment

### Infrastructure (Mac Studio @ 192.168.1.10)
- PostgreSQL 16 (port 5432)
- Searxng (port 8080)
- LM Studio (port 1234)

### Local Development
- MacBook Pro (development)
- Java 25 via SDKMAN
- IntelliJ IDEA
- Flyway for database migrations

### API Keys (Environment Variables)
```bash
export TAVILY_API_KEY=tvly-xxx
export PERPLEXITY_API_KEY=pplx-xxx
export DISCOVERY_SCHEDULE_ENABLED=true
```

---

## Key Design Decisions

### Why TEXT[] Instead of JSONB?
See: [[001-text-array-over-jsonb]]
- Simpler in early development
- Avoid Spring Data JDBC relationship interpretation
- Can migrate to JSONB later if complex querying needed

### Why Domain-Level Deduplication?
See: [[002-domain-level-deduplication]]
- Simple: `java.net.URI.getHost()`
- Good enough for v1
- Defers complex normalization (www/non-www) to future

### Why Circuit Breaker Per Engine?
See: [[003-circuit-breaker-per-engine]]
- Independent failure handling
- System continues with working engines
- Better fault isolation

### Why Virtual Threads?
See: [[004-virtual-threads-parallel-search]]
- I/O-bound workload (HTTP search calls)
- Parallel execution of 3+ engines
- 3x speedup
- Scales to hundreds of concurrent searches

---

## Project Structure

### Code
```
backend/
├── src/main/java/com/northstar/funding/
│   ├── discovery/
│   │   ├── domain/              # Core entities
│   │   ├── application/         # Application services
│   │   ├── service/             # Business logic
│   │   ├── infrastructure/      # Infrastructure (configs, converters, clients)
│   │   └── web/                 # REST controllers
│   ├── search/
│   │   ├── domain/              # SearchQuery, QueryTag, SearchEngineType
│   │   ├── application/         # SearchExecutionService, NightlyDiscoveryScheduler
│   │   └── infrastructure/
│   │       └── adapters/        # SearxngAdapter, TavilyAdapter, PerplexityAdapter
│   └── config/                  # JdbcConfiguration
├── src/main/resources/
│   ├── db/migration/            # Flyway migrations (V1__*.sql, V2__*.sql)
│   ├── application.yml          # Main configuration
│   └── application-*.yml        # Profile-specific configs
└── src/test/java/               # Tests
```

### Documentation
```
docs/                            # Technical documentation
├── architecture-crawler-hybrid.md
├── domain-model.md
├── rag-architecture.md
├── geographic-hierarchy.md
├── data-storage-strategy.md
├── funding_sources.md
├── alternative-funding-sources.md
├── crawler-deduplication-caching.md
└── research-funder-organization-types.md

specs/                           # Feature specifications
├── 001-feature-foundation/
├── 002-create-automated-crawler/
├── 003-search-execution-infrastructure/
└── 004-ai-query-generation-metadata-judging/

northstar-notes/                 # Obsidian vault (this folder)
├── project/                     # Project docs (vision, overview)
├── architecture/                # Architecture notes
├── technology/                  # Tech stack deep dives
├── features/                    # Feature documentation
├── decisions/                   # ADRs
├── session-summaries/           # Development session summaries
├── daily-notes/                 # Daily work logs
├── feature-planning/            # WIP planning
└── inbox/                       # Quick capture
```

---

## Performance Characteristics

### Search Execution
- **Single query (3 engines parallel)**: 3-8 seconds
- **10 queries (sequential batches)**: 5-15 minutes
- **Deduplication rate**: 40-60% typical

### Database Operations
- **findByDayOfWeekAndEnabled**: <50ms
- **save with TEXT[] columns**: <100ms
- **Flyway migrations**: <1s total

### Virtual Threads
- **3x speedup** vs sequential execution
- Scales to hundreds of concurrent searches
- Optimized for I/O-bound operations (HTTP calls)

---

## External Context: Bulgaria Funding Landscape

### Why Bulgaria?
- Developer is American expat in Burgas, Bulgaria
- Working with The NorthStar Foundation Bulgaria
- Understanding local funding needs and landscape

### Key Geographic Focus
- **Primary**: Bulgaria (Burgas region)
- **Regional**: Eastern Europe, Balkans
- **EU**: Bulgaria joined EU 2007 (eligible for EU programs)
- **Eurozone**: Bulgaria joining Eurozone January 2026 (will use EUR, currently uses BGN)

### Target Funding Types
- Educational infrastructure (building/renovation)
- Program grants (curriculum, teacher training)
- Technology infrastructure
- Scholarships and operational support
- Capacity building

### Major Funding Sources for Bulgaria
- **EU Programs**: Erasmus+, Horizons Europe, Cohesion Funds
- **EEA/Norway Grants**: Support for non-Eurozone EU members
- **US Foundations**: America for Bulgaria Foundation, Open Society
- **Bilateral Aid**: USAID Eastern Europe programs
- **Multilateral**: World Bank, EBRD

---

## Related Documentation

### Architecture & Design
- [[vision-and-mission]] - Project inspiration and goals
- [[architecture-overview]] - System architecture
- [[domain-model]] - Core entities and relationships
- [[search-infrastructure]] - Search engine integration
- [[rag-system]] - RAG architecture (future)
- [[data-flow]] - Complete data pipeline

### Features
- [[feature-003-search-infrastructure]] - Current implementation
- [[feature-004-metadata-judging]] - In progress

### Decisions
- [[001-text-array-over-jsonb]]
- [[002-domain-level-deduplication]]
- [[003-circuit-breaker-per-engine]]
- [[004-virtual-threads-parallel-search]]

### Technology
- [[java-25-virtual-threads]]
- [[spring-boot]]
- [[postgresql]]
- [[lm-studio]]
- [[qdrant]]
- [[resilience4j]]

### Project Files
- `CLAUDE.md` - Claude Code project guide
- `docs/` - Detailed technical documentation
- `specs/` - Feature specifications

---

## Getting Started

### For New Developers
1. Read [[vision-and-mission]] for context
2. Review this document for technical overview
3. Read [[architecture-overview]] for system design
4. Check `CLAUDE.md` for development commands
5. Review `specs/003-search-execution-infrastructure/` for current implementation

### For AI (Claude Code)
1. Always check `CLAUDE.md` for project patterns
2. Review Obsidian vault for architectural context
3. Check `session-summaries/` for recent work
4. Review `decisions/` for architectural rationale
5. Link code to `specs/` and vault documentation

---

**Status**: Living Document
**Last Updated**: 2025-10-31
**Project Phase**: Feature 003 Complete, Feature 004 In Progress
**Developer**: Kevin (American expat in Burgas, Bulgaria)
