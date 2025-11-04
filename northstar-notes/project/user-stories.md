# User Stories - NorthStar Funding Discovery

**Project**: NorthStar Funding Discovery
**Last Updated**: 2025-11-04
**Context**: Solo developer (+ Claude Code) building MVP for The NorthStar Foundation Bulgaria

---

## 👥 User Personas

### Primary Persona: Administrator (You)
- **Who**: Kevin, American expat in Burgas, Bulgaria, developer & administrator
- **Goal**: Discover funding opportunities for The NorthStar Foundation Bulgaria
- **Technical Level**: High (can use command line, REST APIs, read logs)
- **Time Constraints**: Limited (need automation, not manual research)

### Future Persona: NGO Staff Member
- **Who**: Non-technical staff at NGOs or schools
- **Goal**: Find relevant funding opportunities without technical knowledge
- **Technical Level**: Low (needs web UI, clear instructions)
- **Time Constraints**: Very limited (cannot spend hours researching)

---

## 🎯 Epic 1: Discovery Pipeline (Core Flow)

**Goal**: Automate the process from query generation to candidate creation

### ✅ Story 1.1: Generate Funding Queries (COMPLETE)
**As an** administrator
**I want to** generate targeted search queries for funding opportunities
**So that** I can find relevant funding sources efficiently

**Acceptance Criteria**:
- ✅ Can generate queries for specific funding categories (e.g., STEM_EDUCATION)
- ✅ Can specify geographic scope (e.g., BULGARIA, BALKANS, EASTERN_EUROPE)
- ✅ Can generate both keyword-based and AI-optimized queries
- ✅ Can specify multiple search engines (Brave, Searxng, Tavily, Perplexity)
- ✅ Queries are cached to avoid regeneration
- ✅ Can specify max number of queries (1-50)

**Implementation Status**: ✅ COMPLETE (Feature 004)

**Test Coverage**: 57 tests

**Example Usage**:
```java
QueryGenerationRequest request = QueryGenerationRequest.builder()
    .searchEngine(SearchEngineType.PERPLEXITY)
    .categories(Set.of(FundingSearchCategory.STEM_EDUCATION))
    .geographic(GeographicScope.BULGARIA)
    .maxQueries(10)
    .sessionId(sessionId)
    .build();

List<String> queries = queryGenerationService.generateQueries(request);
```

---

### ✅ Story 1.2: Execute Multi-Engine Searches (COMPLETE)
**As an** administrator
**I want to** execute searches across multiple search engines in parallel
**So that** I can gather diverse funding opportunities quickly

**Acceptance Criteria**:
- ✅ Can search across 4 search engines (Brave, Searxng, Tavily, Perplexity)
- ✅ Searches execute in parallel using Virtual Threads
- ✅ Circuit breakers prevent cascading failures
- ✅ Rate limiting prevents API quota exhaustion
- ✅ Anti-spam filtering removes irrelevant results early
- ✅ Search results include title, URL, description

**Implementation Status**: ✅ COMPLETE (Feature 003)

**Test Coverage**: 258 tests

---

### ❌ Story 1.3: Process Search Results into Candidates (NOT STARTED)
**As an** administrator
**I want to** automatically convert search results into funding source candidates
**So that** I don't have to manually create candidate records

**Acceptance Criteria**:
- [ ] Extract domain from search result URL
- [ ] Check domain blacklist (skip if blacklisted)
- [ ] Check for duplicate domains (skip if already processed)
- [ ] Create FundingSourceCandidate with status=NEW
- [ ] Associate candidate with DiscoverySession
- [ ] Store original search result metadata
- [ ] Handle bulk processing (100+ results efficiently)

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 1-2 days

**Dependencies**:
- FundingSourceCandidate entity (exists)
- Domain entity (exists)
- SearchResult entity (exists)

---

### ❌ Story 1.4: Judge Candidate Metadata (NOT STARTED)
**As an** administrator
**I want to** automatically judge funding candidates based on metadata
**So that** I only crawl high-quality candidates

**Acceptance Criteria**:
- [ ] Analyze search result title for funding keywords
- [ ] Analyze domain credibility (TLD, organization name)
- [ ] Check geographic relevance
- [ ] Calculate confidence score (0.00-1.00)
- [ ] Create MetadataJudgment record
- [ ] Promote candidates with confidence >= 0.60 to PENDING_CRAWL
- [ ] Skip candidates with confidence < 0.60

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 2-3 days

**Dependencies**:
- MetadataJudgment table (exists - V14 migration)
- LM Studio integration (exists)
- Confidence score criteria (needs design - ADR)

---

### ❌ Story 1.5: Orchestrate Discovery Session (NOT STARTED)
**As an** administrator
**I want to** trigger a complete discovery session with one command
**So that** I get end-to-end results without manual steps

**Acceptance Criteria**:
- [ ] Generate queries for specified categories and geographic scope
- [ ] Execute searches across all engines
- [ ] Process all search results into candidates
- [ ] Judge all candidates for metadata quality
- [ ] Update DiscoverySession with statistics
- [ ] Log progress and errors clearly
- [ ] Handle failures gracefully (partial success OK)

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 1 day

**Dependencies**:
- Stories 1.3 and 1.4 complete

---

## 🎯 Epic 2: Manual Operations & Monitoring

**Goal**: Provide REST API for triggering and monitoring discovery

### ❌ Story 2.1: Trigger Discovery via REST (NOT STARTED)
**As an** administrator
**I want to** trigger a discovery session via REST API
**So that** I can integrate with scripts or CI/CD

**Acceptance Criteria**:
- [ ] POST /api/discovery/sessions endpoint
- [ ] Request body specifies categories, geographic scope, engines
- [ ] Returns session ID immediately (async processing)
- [ ] Returns 202 Accepted status
- [ ] Stores session in database

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 0.5 days

---

### ❌ Story 2.2: View Discovery Session Status (NOT STARTED)
**As an** administrator
**I want to** check the status of a discovery session
**So that** I know when it's complete and what was found

**Acceptance Criteria**:
- [ ] GET /api/discovery/sessions/{id} endpoint
- [ ] Returns session status (RUNNING, COMPLETED, FAILED)
- [ ] Returns statistics (queries generated, searches executed, candidates created, candidates judged)
- [ ] Returns error messages if failed
- [ ] Returns duration

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 0.5 days

---

### ❌ Story 2.3: List and Filter Candidates (NOT STARTED)
**As an** administrator
**I want to** view and filter funding candidates
**So that** I can review what was discovered

**Acceptance Criteria**:
- [ ] GET /api/candidates endpoint with pagination
- [ ] Filter by status (NEW, PENDING_CRAWL, CRAWLED, etc.)
- [ ] Filter by confidence score range
- [ ] Filter by discovery session
- [ ] Sort by confidence score (descending)
- [ ] Returns domain, title, URL, confidence, status

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 1 day

---

### ❌ Story 2.4: Manually Update Candidate Status (NOT STARTED)
**As an** administrator
**I want to** manually change a candidate's status
**So that** I can override automated decisions

**Acceptance Criteria**:
- [ ] PUT /api/candidates/{id}/status endpoint
- [ ] Can change any status to any other status
- [ ] Validates status transitions
- [ ] Logs who made the change and why
- [ ] Returns updated candidate

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 0.5 days

---

## 🎯 Epic 3: Website Crawling

**Goal**: Process PENDING_CRAWL candidates by crawling their websites

### ❌ Story 3.1: Check robots.txt (NOT STARTED)
**As an** administrator
**I want to** automatically check robots.txt before crawling
**So that** I respect website policies

**Acceptance Criteria**:
- [ ] Fetch robots.txt for each PENDING_CRAWL candidate
- [ ] Parse robots.txt rules
- [ ] Check if /sitemap.xml is allowed
- [ ] Store crawl permissions in database
- [ ] Skip candidates that disallow crawling
- [ ] Handle missing robots.txt (assume allowed)

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 1 day

---

### ❌ Story 3.2: Parse Sitemaps (NOT STARTED)
**As an** administrator
**I want to** extract URLs from sitemaps
**So that** I know which pages to crawl

**Acceptance Criteria**:
- [ ] Fetch sitemap.xml or sitemap index
- [ ] Parse XML and extract all URLs
- [ ] Extract lastmod dates
- [ ] Filter URLs by date (only last 24 months)
- [ ] Store URLs in sitemap_urls table
- [ ] Handle nested sitemap indexes recursively
- [ ] Limit to max 500 URLs per domain

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 1-2 days

**Open Question**: Are old URLs useful? (Spring Crawler found many outdated)

---

### ❌ Story 3.3: Crawl Selected Pages (NOT STARTED)
**As an** administrator
**I want to** crawl selected pages from sitemaps
**So that** I can extract funding opportunity details

**Acceptance Criteria**:
- [ ] Use sitemap URLs as seed list
- [ ] Fetch HTML content
- [ ] Store raw HTML in database
- [ ] Respect rate limits (max 1 req/sec per domain)
- [ ] Handle HTTP errors gracefully
- [ ] Update candidate status: PENDING_CRAWL → CRAWLED
- [ ] Log crawl statistics

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 2-3 days

---

## 🎯 Epic 4: Automation & Scheduling

**Goal**: Run discovery automatically on schedule

### ❌ Story 4.1: Schedule Daily Discovery (NOT STARTED)
**As an** administrator
**I want to** run discovery automatically every day
**So that** I always have fresh funding opportunities

**Acceptance Criteria**:
- [ ] @Scheduled task runs daily at configured time
- [ ] Configurable cron expression
- [ ] Uses predefined categories and geographic scope
- [ ] Creates new DiscoverySession each run
- [ ] Sends notification on completion (email or log)
- [ ] Handles failures without crashing

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 1 day

---

### ❌ Story 4.2: Process Candidate Queue (NOT STARTED)
**As an** administrator
**I want to** automatically process PENDING_CRAWL candidates
**So that** crawling happens without manual intervention

**Acceptance Criteria**:
- [ ] Queue processes PENDING_CRAWL candidates
- [ ] Priority-based processing (high confidence first)
- [ ] Retry failed candidates with exponential backoff
- [ ] Max 3 retry attempts
- [ ] Mark permanently failed candidates
- [ ] Log queue statistics (processed, failed, retried)

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 2 days

---

## 🎯 Epic 5: Enhanced Query Generation (Future)

**Goal**: Use new taxonomy for multi-dimensional queries

### ✅ Story 5.1: Enhanced Taxonomy (COMPLETE)
**As an** administrator
**I want to** filter funding opportunities by multiple dimensions
**So that** I can find highly targeted results

**Acceptance Criteria**:
- ✅ Can specify WHO provides funding (FundingSourceType: EU, Govt, NGO, etc.)
- ✅ Can specify HOW funding is distributed (FundingMechanism: Grant, Loan, etc.)
- ✅ Can specify amount range (ProjectScale: MICRO to MEGA)
- ✅ Can specify WHO benefits (BeneficiaryPopulation: age, demographics, etc.)
- ✅ Can specify recipient organization type (K12, University, NGO, etc.)
- ✅ Can specify query language (for future translation)

**Implementation Status**: ✅ COMPLETE (Feature 005)

**Test Coverage**: All existing tests pass + new enum tests

---

### ❌ Story 5.2: Multi-Dimensional Query Generation (NOT STARTED)
**As an** administrator
**I want to** generate queries using enhanced taxonomy
**So that** I get more targeted results

**Acceptance Criteria**:
- [ ] Use optional QueryGenerationRequest fields in query generation
- [ ] Incorporate FundingSourceType keywords into queries
- [ ] Incorporate FundingMechanism keywords into queries
- [ ] Incorporate ProjectScale keywords into queries
- [ ] Incorporate BeneficiaryPopulation keywords into queries
- [ ] Incorporate RecipientOrganizationType keywords into queries
- [ ] Queries remain readable (not keyword soup)

**Implementation Status**: ❌ NOT STARTED

**Estimated Effort**: 1-2 days

**Dependencies**:
- Feature 005 complete (✅)
- Need to enhance CategoryMapper logic

---

## 📊 Story Status Summary

| Epic | Stories | Complete | In Progress | Not Started |
|------|---------|----------|-------------|-------------|
| 1. Discovery Pipeline | 5 | 2 | 0 | 3 |
| 2. Manual Operations | 4 | 0 | 0 | 4 |
| 3. Website Crawling | 3 | 0 | 0 | 3 |
| 4. Automation | 2 | 0 | 0 | 2 |
| 5. Enhanced Queries | 2 | 1 | 0 | 1 |
| **TOTAL** | **16** | **3** | **0** | **13** |

**Progress**: 18.75% complete (3/16 stories)

---

## 🚀 Recommended Story Sequence

### Phase 1: MVP Discovery Pipeline (1-2 weeks)
1. Story 1.3: Process Search Results → Candidates
2. Story 1.4: Judge Candidate Metadata
3. Story 1.5: Orchestrate Discovery Session

**Outcome**: Can run end-to-end discovery from command line

### Phase 2: Manual Operations (1 week)
4. Story 2.1: Trigger Discovery via REST
5. Story 2.2: View Session Status
6. Story 2.3: List and Filter Candidates
7. Story 2.4: Update Candidate Status

**Outcome**: Can trigger and monitor discovery via REST API

### Phase 3: Crawling (1-2 weeks)
8. Story 3.1: Check robots.txt
9. Story 3.2: Parse Sitemaps
10. Story 3.3: Crawl Selected Pages

**Outcome**: Can process PENDING_CRAWL candidates automatically

### Phase 4: Automation (1 week)
11. Story 4.1: Schedule Daily Discovery
12. Story 4.2: Process Candidate Queue

**Outcome**: Fully automated daily discovery

### Phase 5: Enhancement (1 week)
13. Story 5.2: Multi-Dimensional Query Generation

**Outcome**: More targeted discovery using enhanced taxonomy

---

## 💡 Anti-Stories (Things We're NOT Building)

### ❌ Human Enrichment UI
**Rationale**: Phase 2 (human-in-the-loop) is future work after Phase 1 automation proves viable

### ❌ Deep Content Analysis
**Rationale**: Phase 1 uses metadata only. Deep NLP/RAG is Phase 2.

### ❌ Multi-Tenant Support
**Rationale**: Single organization (NorthStar Foundation) for MVP

### ❌ Payment Processing
**Rationale**: No subscriptions yet, just internal tool

### ❌ Email Notifications
**Rationale**: Can log and check manually for MVP

### ❌ Advanced Analytics Dashboard
**Rationale**: Simple REST API queries sufficient for now

---

## 🎯 Success Criteria for MVP

**Definition of Done for MVP**:
- ✅ Can generate targeted queries (DONE)
- ✅ Can execute multi-engine searches (DONE)
- ⏳ Can create candidates from search results (TODO)
- ⏳ Can judge candidates by metadata (TODO)
- ⏳ Can crawl approved websites (TODO)
- ⏳ Can trigger discovery via REST (TODO)
- ⏳ Can view candidates via REST (TODO)
- ⏳ Can run discovery automatically daily (TODO)

**MVP = 8 stories complete** (currently 3/8 = 37.5% done)

---

**End of User Stories**
