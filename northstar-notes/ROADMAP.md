# NorthStar Funding Discovery - Development Roadmap

**Last Updated**: 2025-11-08
**Status**: Post-Ollama Migration - Planning Next Phases

---

## Current State Summary (As of 2025-11-08)

### ✅ Completed Components

| Component | Status | Tests | Description |
|-----------|--------|-------|-------------|
| **Domain Model** | ✅ Complete | 42/42 | 19 entities with 16 enums, Feature 005 taxonomy |
| **Persistence Layer** | ✅ Complete | 210/210 | 9 repositories, 5 services, Flyway migrations |
| **Query Generation** | ✅ Complete | 58/58 | Ollama integration, concurrent generation, caching |
| **Search Processing** | ⚠️ Partial | 327/327 | Result processing, scoring, deduplication (no crawling) |

**Total Test Coverage**: 637 tests, 100% passing

### ❌ Not Yet Implemented

- **Search Engine Integration** - No adapters for Brave, Serper, SearXNG, Tavily, Perplexica
- **Web Crawling** - No deep content extraction from candidate websites
- **Judging Module** - northstar-judging module is empty
- **Application Layer** - No REST API, orchestration, or scheduling
- **Monitoring/Observability** - No metrics, logging, or alerting

---

## Phase Overview

```
Phase 1: Search Engine Integration (NEXT)
    ↓
Phase 2: Crawler Execution & Content Extraction
    ↓
Phase 3: Application Layer & Orchestration
    ↓
Phase 4: Production Readiness
```

---

## Phase 1: Search Engine Integration 🎯 NEXT

**Goal**: Execute generated queries across multiple search engines and collect results

**Duration Estimate**: 2-3 weeks

### Deliverables

#### 1.1 Search Engine Adapters
- [ ] **BraveSearchAdapter** - Integrate Brave Search API
  - API client implementation using HTTP client
  - Result parsing and normalization
  - Rate limiting (requests/minute)
  - Error handling and retries
  - Tests: Unit + integration with actual API calls

- [ ] **SerperSearchAdapter** - Integrate Serper.dev API
  - Same pattern as Brave
  - Different API response format
  - Google Search Engine Results Pages (SERPs) parsing

- [ ] **SearXNGAdapter** - Integrate self-hosted SearXNG
  - Connection to `http://192.168.1.10:8080`
  - Meta-search aggregation handling
  - No API key required (self-hosted)

- [ ] **TavilyAdapter** - Integrate Tavily AI search
  - AI-optimized search endpoint
  - Handles longer contextual queries
  - Enhanced metadata extraction

- [ ] **PerplexicaAdapter** - Integrate local Perplexica
  - Connection to `http://192.168.1.10:3001/api`
  - WebSocket support for streaming results
  - Focus mode selection (web, academic, etc.)

#### 1.2 Search Orchestration
- [ ] **SearchOrchestrator** service
  - Takes QueryGenerationResponse + SearchEngineType
  - Executes queries via appropriate adapter
  - Aggregates results from multiple engines
  - Deduplicates across engines (domain-level)
  - Returns unified SearchResult list

- [ ] **Concurrent Execution** using Virtual Threads
  - Parallel search across 4-5 engines
  - Timeout handling (30s per engine)
  - Graceful degradation if one engine fails

#### 1.3 Result Storage
- [ ] Persist SearchResult entities to `search_result` table
  - URL, title, description, engine, query_id
  - Timestamps, confidence scores
  - Link to originating query

- [ ] Update `search_session_statistics` table
  - Per-engine metrics (results returned, latency, errors)
  - Query performance tracking

#### 1.4 Integration Tests
- [ ] End-to-end test: Query → Search → Results
  - Generate queries via QueryGenerationService
  - Execute via SearchOrchestrator
  - Verify results persisted to database
  - Validate deduplication works across engines

**Key Design Decisions**:
- Use same adapter interface for all engines (Strategy pattern)
- Return raw SearchResult objects (minimal processing)
- Save ALL results to database (confidence scoring happens later)
- No web crawling yet - just metadata from search APIs

**Prerequisites**:
- API keys for Brave Search, Serper, Tavily
- SearXNG and Perplexica already running on Mac Studio
- Database migrations V17 (search_result), V11 (search_session_statistics)

**Success Criteria**:
- ✅ Can execute 20 queries across 4 engines in <30 seconds
- ✅ Results deduplicated by domain
- ✅ All results persisted with correct metadata
- ✅ Integration tests passing with live API calls

---

## Phase 2: Crawler Execution & Deep Content Extraction

**Goal**: Crawl high-confidence candidate websites and extract structured data

**Duration Estimate**: 3-4 weeks

### Deliverables

#### 2.1 Web Crawler Infrastructure
- [ ] **CrawlerService** - Main orchestrator
  - Fetches PENDING_CRAWL candidates from database
  - Uses Virtual Threads for concurrent crawling (10-20 parallel)
  - Respects robots.txt
  - Rate limiting per domain (1 request/second)

- [ ] **ContentExtractor** - Parse and extract
  - HTML parsing (Jsoup or similar)
  - Extract funding-specific content:
    - Application deadlines
    - Eligibility criteria
    - Funding amounts
    - Contact information
    - Program descriptions
  - Clean and normalize text

- [ ] **StructuredDataExtractor** - Extract metadata
  - JSON-LD parsing
  - OpenGraph tags
  - Schema.org markup
  - Contact information (emails, phones)

#### 2.2 Content Analysis
- [ ] **TextAnalyzer** - NLP processing
  - Use Ollama (llama3.1:8b) for text analysis
  - Extract key entities (organizations, locations, dates)
  - Categorize funding type
  - Identify eligibility requirements

- [ ] **FundingProgramExtractor**
  - Identify distinct programs on a website
  - Extract program-specific details
  - Create FundingProgram entities

#### 2.3 Update Workflow
- [ ] Update candidate status: PENDING_CRAWL → CRAWLED
- [ ] Create Organization entities
- [ ] Create FundingProgram entities
- [ ] Link ContactIntelligence to organizations
- [ ] Create EnhancementRecord for audit trail

#### 2.4 Error Handling
- [ ] HTTP errors (404, 403, timeout)
- [ ] SSL certificate issues
- [ ] Malformed HTML
- [ ] Anti-bot detection
- [ ] Mark failed candidates as CRAWL_FAILED

**Key Design Decisions**:
- Crawl synchronously (one page at a time per domain)
- Use Ollama for content analysis (not external API)
- Save raw HTML to database for replay (optional)
- Separate crawling from judging (two distinct phases)

**Prerequisites**:
- Phase 1 complete (search results available)
- High-confidence candidates in PENDING_CRAWL status
- Ollama models loaded (llama3.1:8b for analysis)

**Success Criteria**:
- ✅ Can crawl 100 websites in 10-15 minutes
- ✅ Extract structured data from 80%+ of crawled sites
- ✅ Organizations and FundingPrograms created correctly
- ✅ Failed crawls logged with detailed error information

---

## Phase 3: Application Layer & Orchestration

**Goal**: Build REST API and scheduled workflows for end-to-end automation

**Duration Estimate**: 2-3 weeks

### Deliverables

#### 3.1 REST API (Spring Boot)
- [ ] **Query Generation Endpoints**
  - `POST /api/queries/generate` - Generate queries
  - `GET /api/queries/{id}` - Retrieve generated queries
  - `GET /api/queries/cached` - List cached queries

- [ ] **Search Endpoints**
  - `POST /api/search/execute` - Execute search
  - `GET /api/search/results/{sessionId}` - Get results
  - `GET /api/search/statistics` - Search metrics

- [ ] **Candidate Endpoints**
  - `GET /api/candidates` - List candidates (filters: status, confidence)
  - `GET /api/candidates/{id}` - Candidate details
  - `PUT /api/candidates/{id}/status` - Update status

- [ ] **Organization Endpoints**
  - `GET /api/organizations` - List organizations
  - `GET /api/organizations/{id}` - Organization details
  - `GET /api/organizations/{id}/programs` - Programs by org

- [ ] **Program Endpoints**
  - `GET /api/programs` - List programs (filters: status, deadline)
  - `GET /api/programs/{id}` - Program details

#### 3.2 Workflow Orchestration
- [ ] **DiscoveryWorkflow** - End-to-end automation
  1. Generate queries (QueryGenerationService)
  2. Execute searches (SearchOrchestrator)
  3. Process results (SearchResultProcessor)
  4. Create candidates (CandidateCreationService)
  5. Crawl websites (CrawlerService)
  6. Extract programs (FundingProgramExtractor)
  7. Update statistics (DiscoverySessionService)

- [ ] **Scheduled Jobs** (Spring @Scheduled)
  - Daily discovery run (configurable categories + geography)
  - Weekly full scan (all categories)
  - Periodic cleanup (expire old candidates)

#### 3.3 Admin Interface (Optional)
- [ ] Simple web UI for monitoring
  - Dashboard with statistics
  - View recent discoveries
  - Manually trigger workflows
  - Review flagged candidates

**Key Design Decisions**:
- RESTful API following Spring Boot best practices
- Async workflows using Virtual Threads
- Schedule via cron expressions
- Health checks and metrics endpoints

**Prerequisites**:
- Phase 1 and 2 complete
- All core services functional

**Success Criteria**:
- ✅ Full discovery workflow runs end-to-end
- ✅ API endpoints tested and documented
- ✅ Scheduled job runs successfully
- ✅ Can monitor progress via API

---

## Phase 4: Production Readiness

**Goal**: Deploy to production with monitoring, logging, and reliability

**Duration Estimate**: 2 weeks

### Deliverables

#### 4.1 Observability
- [ ] **Structured Logging** (Logback + JSON)
  - Request/response logging
  - Performance metrics
  - Error tracking with stack traces

- [ ] **Metrics** (Micrometer + Prometheus)
  - Query generation latency
  - Search engine response times
  - Crawl success/failure rates
  - Database connection pool metrics

- [ ] **Alerting** (Grafana)
  - Failed searches
  - Crawler errors
  - Database issues
  - Ollama connectivity

#### 4.2 Resilience
- [ ] **Circuit Breakers** (Resilience4j)
  - Protect against external API failures
  - Graceful degradation

- [ ] **Retry Logic**
  - Exponential backoff for transient errors
  - Max retry limits

- [ ] **Rate Limiting**
  - Per-engine rate limits
  - Per-domain crawl limits

#### 4.3 Configuration Management
- [ ] **Environment-specific configs**
  - dev, test, staging, production
  - API keys in secrets management (Vault or env vars)

- [ ] **Feature Flags**
  - Enable/disable engines dynamically
  - A/B testing for query strategies

#### 4.4 Documentation
- [ ] API documentation (OpenAPI/Swagger)
- [ ] Deployment guide
- [ ] Operational runbook
- [ ] Architecture decision records (ADRs)

**Success Criteria**:
- ✅ Production deployment successful
- ✅ Monitoring dashboards operational
- ✅ Alerts firing correctly
- ✅ Documentation complete

---

## Technical Debt & Improvements

### Immediate
- [ ] Add integration tests for query generation with TestContainers
- [ ] Implement cache eviction strategy (LRU or TTL-based)
- [ ] Add query validation (max length, character restrictions)

### Future Enhancements
- [ ] Multi-language query generation (Bulgarian, Romanian, German, etc.)
- [ ] Dynamic prompt tuning based on search quality feedback
- [ ] Machine learning for confidence scoring
- [ ] Geographic expansion (beyond Eastern Europe)
- [ ] Email notifications for high-value discoveries
- [ ] Browser automation for JavaScript-heavy sites (Playwright)

---

## Resource Requirements

### Infrastructure
- Mac Studio @ 192.168.1.10 (current)
  - Ollama (llama3.1:8b, nomic-embed-text)
  - PostgreSQL 16
  - Docker Compose (SearXNG, Perplexica, Qdrant)

### External Services (Phase 1)
- Brave Search API - ~$5/month (1000 requests)
- Serper.dev - ~$50/month (5000 requests)
- Tavily AI - Free tier (1000 requests/month)

### Development Time
- **Phase 1**: 2-3 weeks (Search Integration)
- **Phase 2**: 3-4 weeks (Crawler + Extraction)
- **Phase 3**: 2-3 weeks (Application Layer)
- **Phase 4**: 2 weeks (Production Readiness)
- **Total**: 9-12 weeks (~3 months)

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| API rate limits exceeded | High | Medium | Implement caching, batch requests |
| Ollama inference too slow | Medium | Low | Use smaller models (qwen2.5:0.5b) for non-critical tasks |
| Anti-bot detection blocks crawlers | High | Medium | Respect robots.txt, add delays, rotate user agents |
| Search API costs higher than expected | Medium | Medium | Start with free tiers, monitor usage closely |
| Data quality poor (garbage in) | High | Medium | Implement confidence thresholds, manual review queue |

---

## Next Steps (Immediate Actions)

1. **Decide on Phase 1 Scope** - Which search engines first?
   - Recommend: SearXNG (free, self-hosted) + Tavily (free tier)
   - Defer: Brave, Serper until SearXNG proven

2. **Obtain API Keys**
   - Tavily: Sign up for free tier
   - Brave/Serper: Evaluate if needed

3. **Create Feature Branch**
   - `feature/009-search-engine-integration`

4. **Write ADR for Search Adapter Pattern**
   - Document interface design
   - Error handling strategy
   - Rate limiting approach

5. **Implement First Adapter** (SearXNG)
   - Lowest risk (self-hosted, no API key)
   - Validates adapter pattern
   - Tests concurrent execution

---

## Questions for Planning

1. **Search Engine Priority**: Which engines should we integrate first?
   - SearXNG (free, already running)?
   - Tavily (AI-optimized, free tier)?
   - Brave/Serper (paid, but higher quality)?

2. **Phase 1 Scope**: Should we complete all 5 adapters or start with 2-3?

3. **Testing Strategy**: Live API calls in tests or mock responses?

4. **Budget**: What's acceptable monthly cost for search APIs?

5. **Timeline**: Any deadline constraints for first production deployment?

---

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-11-08 | Use Ollama instead of LM Studio | Concurrent request support (10 parallel) |
| 2025-11-08 | Choose llama3.1:8b over qwen2.5:0.5b | Reliability > Speed for query generation |
| 2025-11-08 | Implement preamble filtering in parsing | LLMs often include conversational preambles |
