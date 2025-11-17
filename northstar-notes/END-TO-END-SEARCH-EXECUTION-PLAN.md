# End-to-End Search Execution Plan
**Date**: 2025-11-17
**Goal**: Execute real searches with queries, crawls, confidence scoring to populate database

## Current State Analysis

### ✅ IMPLEMENTED Modules

**1. Query Generation** (`northstar-query-generation`)
- ✅ Ollama integration (llama3.1:8b @ 192.168.1.10:11434)
- ✅ LangChain4j for LLM calls
- ✅ 24-hour caching (Caffeine)
- ✅ Concurrent generation (Virtual Threads)
- ✅ KeywordQueryStrategy & TavilyQueryStrategy
- ✅ 58 integration tests (ALL PASSING)
- **Status**: FULLY FUNCTIONAL

**2. Search Result Processing** (`northstar-crawler`)
- ✅ SearchResultProcessor - main orchestrator
- ✅ ConfidenceScorer - multi-signal confidence (0.00-1.00)
  - TLD credibility (+0.20 for .gov, +0.15 for .edu, etc.)
  - Funding keywords (grants, scholarships, etc.)
  - Geographic relevance (Bulgaria, EU, Eastern Europe)
  - Organization type detection
  - Compound boost for multiple signals
- ✅ DomainCredibilityService - TLD scoring
- ✅ CandidateCreationService - creates FundingSourceCandidate
- ✅ Domain deduplication (HashSet in-memory)
- ✅ Blacklist filtering
- ✅ Threshold filtering (≥ 0.60 creates PENDING_CRAWL candidates)
- ✅ 7 comprehensive unit tests
- **Status**: FULLY FUNCTIONAL

**3. Search Adapters** (`northstar-search-adapters`)
- ✅ SearchAdapter interface
- ✅ SearXNGAdapter implementation
- **Partial**: Only SearXNG implemented, missing Tavily/Brave/Serper

**4. Domain Model** (`northstar-domain`)
- ✅ 19 entities (FundingSourceCandidate, Domain, Organization, etc.)
- ✅ 16 enums (CandidateStatus, DomainStatus, SearchEngineType, etc.)
- ⚠️ SearchEngineType enum: BRAVE, SEARXNG, SERPER, TAVILY (NO PERPLEXITY)

**5. Persistence Layer** (`northstar-persistence`)
- ✅ 9 repositories (Spring Data JDBC)
- ✅ 5 service classes with business logic
- ✅ 17 Flyway migrations
- ✅ Database @ 192.168.1.10:5432
- **Status**: FULLY FUNCTIONAL

**6. REST API** (`northstar-rest-api`)
- ✅ CandidateController - list/approve/reject endpoints
- ✅ CandidateService - business logic
- ✅ DTO mapping
- ✅ Swagger UI @ http://localhost:8080/swagger-ui/index.html
- **Status**: WORKING (with null-safe enum handling fix)

**7. Admin UI** (`northstar-admin-ui`)
- ✅ Vue 3 + Vite @ http://localhost:5173
- ✅ CandidateReviewQueue component
- ✅ CandidateCard component
- ✅ Browser console logging
- **Status**: RUNNING (needs test data to verify)

### ❌ NOT IMPLEMENTED

**1. Search Engine Adapters** (CRITICAL GAP)
- ❌ TavilyAdapter - NO implementation
- ❌ BraveAdapter - NO implementation
- ❌ SerperAdapter - NO implementation
- ❌ PerplexityAdapter - not in northstar-domain enum

**2. Search Workflow Orchestration**
- ❌ SearchWorkflowService - coordinate query generation → search execution → processing
- ❌ Kafka event publishing (if used)
- ❌ Scheduler for nightly/daily runs
- ❌ Error handling and retry logic

**3. Crawler/Scraper** (Phase 2)
- ❌ Web crawling infrastructure
- ❌ Deep content extraction
- ❌ HTML parsing

**4. Application Layer**
- ❌ Main application entry point
- ❌ Configuration management
- ❌ Service wiring

### ⚠️ CRITICAL ISSUES

**Issue 1: Enum Mismatch**
- **Database CHECK constraint**: SEARXNG, TAVILY, PERPLEXITY
- **northstar-domain enum**: BRAVE, SEARXNG, SERPER, TAVILY
- **Result**: Cannot load candidates with PERPLEXITY from database
- **Fix Required**: Align enum with database OR update database constraint

**Issue 2: Missing Search Adapters**
- Have query generation working
- Have result processing working
- **MISSING**: The bridge between them (search execution)
- Need: TavilyAdapter, BraveAdapter at minimum

**Issue 3: No Orchestration**
- Individual components work
- No glue code to run end-to-end workflow

---

## Execution Plan - End-to-End Search

### Phase 1: Fix Foundation (Required First)

**P1.1 - Fix Enum Mismatch**
- [ ] Decide: Update database OR update enum
- [ ] Recommended: Add PERPLEXITY to northstar-domain SearchEngineType
- [ ] Update database constraint if adding more engines
- [ ] Clean existing test data with invalid values
- [ ] Verify: Can load/save candidates with all engine types

**P1.2 - Implement Tavily Adapter**
- [ ] Create TavilyAdapter in northstar-search-adapters
- [ ] Add Tavily API dependency (HTTP client)
- [ ] Implement SearchAdapter interface:
  - `search(String query, int maxResults)` → List<SearchResult>
- [ ] Map Tavily response to SearchResult domain model
- [ ] Add API key configuration
- [ ] Unit tests with mocked Tavily API
- [ ] Integration test with real Tavily API

**P1.3 - Implement SearXNG Adapter (verify existing)**
- [ ] Review existing SearXNGAdapter implementation
- [ ] Verify it works with http://192.168.1.10:8080
- [ ] Test with real SearXNG instance
- [ ] Ensure SearchResult mapping is correct

### Phase 2: Build Orchestration

**P2.1 - Create SearchWorkflowService**
Module: `northstar-search-workflow` or `northstar-application`

```java
@Service
public class SearchWorkflowService {

    private final QueryGenerationService queryGenService;
    private final Map<SearchEngineType, SearchAdapter> searchAdapters;
    private final SearchResultProcessor resultProcessor;
    private final DiscoverySessionService sessionService;

    /**
     * Execute complete search workflow:
     * 1. Generate queries (Ollama)
     * 2. Execute searches (Tavily, SearXNG)
     * 3. Process results (confidence scoring)
     * 4. Save candidates to database
     */
    public DiscoverySessionResult executeSearch(SearchRequest request) {
        // 1. Create discovery session
        // 2. Generate queries
        // 3. For each search engine, execute queries
        // 4. Process all results
        // 5. Update session statistics
        // 6. Return summary
    }
}
```

Tasks:
- [ ] Create SearchWorkflowService
- [ ] Inject QueryGenerationService
- [ ] Inject SearchAdapters (Map<SearchEngineType, SearchAdapter>)
- [ ] Inject SearchResultProcessor
- [ ] Implement executeSearch() method
- [ ] Add transaction boundaries
- [ ] Add error handling (continue on engine failure)
- [ ] Unit tests with mocks
- [ ] Integration test end-to-end

**P2.2 - Create SearchRequest Model**
- [ ] Define search parameters:
  - Categories (education, science, infrastructure, etc.)
  - Geographic scope (Bulgaria, Eastern Europe, EU, Global)
  - Search engines to use
  - Max results per engine
- [ ] Add validation
- [ ] Add builder pattern

**P2.3 - Create DiscoverySessionResult Model**
- [ ] Session statistics:
  - Queries generated
  - Searches executed
  - Results found
  - Candidates created (high confidence)
  - Duplicates skipped
  - Blacklisted skipped
  - Average confidence score
  - Duration
- [ ] Add summary methods

### Phase 3: Create Execution Entry Point

**P3.1 - CLI Command for Manual Execution**
- [ ] Create SearchCommand (Spring Shell or CLI)
- [ ] Accept parameters: categories, engines, geo-scope
- [ ] Call SearchWorkflowService
- [ ] Display results
- [ ] Example:
  ```bash
  mvn exec:java -Dexec.mainClass="...SearchCommand" \
    -Dexec.args="--categories=education,science --engines=TAVILY,SEARXNG --geo=bulgaria"
  ```

**P3.2 - Scheduled Execution (Optional)**
- [ ] Add @Scheduled annotation
- [ ] Configure cron expression (nightly at 2 AM)
- [ ] Add enable/disable flag
- [ ] Add configuration for default search parameters

### Phase 4: Test with Real Data

**P4.1 - Execute Test Search**
Parameters:
- Categories: ["education funding", "research grants"]
- Engines: [TAVILY, SEARXNG]
- Geographic scope: Bulgaria, Eastern Europe
- Max results: 10 per engine

Expected flow:
```
1. QueryGenerationService generates 3 queries per category
   → 6 total queries

2. Execute searches:
   - Tavily: 6 queries × 10 results = 60 results
   - SearXNG: 6 queries × 10 results = 60 results
   → 120 total search results

3. SearchResultProcessor:
   - Extract domains: ~50-70 unique domains
   - Check blacklist: remove blacklisted
   - Calculate confidence: 0.00-1.00 per result
   - Filter threshold: keep >= 0.60
   → Estimate: 15-30 high-confidence candidates

4. Database tables populated:
   - discovery_session: 1 row
   - domain: 50-70 rows
   - funding_source_candidate: 15-30 rows (PENDING_CRAWL)
```

Tasks:
- [ ] Clear existing test data
- [ ] Execute test search via CLI
- [ ] Verify discovery_session created
- [ ] Verify domains registered
- [ ] Verify candidates created with correct status
- [ ] Verify confidence scores reasonable (0.60-1.00)
- [ ] Verify no duplicates
- [ ] Check Admin UI displays candidates correctly

**P4.2 - Verify Admin UI Workflow**
- [ ] Open http://localhost:5173
- [ ] See candidates listed
- [ ] Filter by status (PENDING_CRAWL)
- [ ] Filter by confidence (>= 0.80)
- [ ] Approve a candidate → verify status changes to APPROVED
- [ ] Reject a candidate → verify status changes to REJECTED
- [ ] Reload page → verify changes persisted

### Phase 5: Production Readiness

**P5.1 - Configuration Management**
- [ ] Externalize API keys (environment variables)
- [ ] Externalize Ollama URL
- [ ] Externalize search engine URLs
- [ ] Add application.yml profiles (dev, prod)

**P5.2 - Error Handling**
- [ ] Handle Tavily API failures gracefully
- [ ] Handle SearXNG failures gracefully
- [ ] Handle Ollama failures gracefully
- [ ] Retry logic for transient failures
- [ ] Circuit breaker pattern for external APIs

**P5.3 - Monitoring & Logging**
- [ ] Add structured logging (JSON)
- [ ] Log search execution start/end
- [ ] Log per-engine results count
- [ ] Log confidence score distribution
- [ ] Log candidates created count
- [ ] Add metrics (Micrometer)

**P5.4 - Documentation**
- [ ] README: How to run search
- [ ] README: How to configure API keys
- [ ] README: How to view results in Admin UI
- [ ] README: Troubleshooting guide

---

## Priority Order

### IMMEDIATE (Today)
1. ✅ Complete this analysis document
2. Fix SearchEngineType enum mismatch
3. Implement TavilyAdapter (basic version)
4. Create SearchWorkflowService (basic version)

### SHORT TERM (This Week)
5. Create CLI command for manual execution
6. Execute first real test search
7. Verify data in Admin UI
8. Fix any issues found

### MEDIUM TERM (Next Week)
9. Add error handling
10. Add comprehensive logging
11. Create scheduled execution
12. Production configuration

### LATER
13. Add more search engines (Brave, Serper)
14. Phase 2: Web crawling
15. Phase 2: Deep content extraction

---

## Success Criteria

**Minimum Viable Workflow:**
- [ ] Can execute: `./run-search.sh --categories=education --engines=TAVILY`
- [ ] Generates queries via Ollama
- [ ] Executes search via Tavily
- [ ] Processes results with confidence scoring
- [ ] Creates candidates in database (PENDING_CRAWL status)
- [ ] View candidates in Admin UI @ http://localhost:5173
- [ ] Approve/reject candidates via UI
- [ ] Changes persist to database

**Quality Metrics:**
- [ ] Confidence scores: 80%+ of created candidates have score >= 0.70
- [ ] Duplicates: < 5% duplicate domains
- [ ] Blacklist: 100% of blacklisted domains filtered
- [ ] Performance: Complete workflow in < 2 minutes for 100 results
- [ ] Reliability: 95%+ success rate for search execution

---

## Next Steps

**Immediate action:**
1. Review this plan with user
2. Get approval on approach
3. Start with Phase 1: Fix Foundation
4. Implement Phase 1.1 (enum fix) first
5. Then implement Phase 1.2 (Tavily adapter)
6. Then move to Phase 2 (orchestration)
