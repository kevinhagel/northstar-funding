# Tasks: Automated Crawler Infrastructure - Phase 1 Metadata Judging

**Input**: Design documents from `/Users/kevin/github/northstar-funding/specs/002-create-automated-crawler/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, quickstart.md ✅

## Execution Flow (main)
```
1. Load plan.md from feature directory ✅
   → Tech stack: Java 25, Spring Boot 3.5.5, PostgreSQL 16, Spring Data JDBC
   → Structure: Web application (backend/ Spring Boot monolith)
2. Load design documents ✅:
   → data-model.md: 5 entities (Domain, extended FundingSourceCandidate, 3 DTOs)
   → research.md: Technical decisions on deduplication, judging, Virtual Threads
   → quickstart.md: 6 integration test scenarios
3. Generate tasks by category:
   → Tests: Repository integration tests, service unit tests, quickstart scenarios
   → Core: DomainRepository, query generation, search adapters
   → Integration: Discovery scheduling, error handling
   → Polish: Performance testing, documentation
4. Apply task rules ✅:
   → Different files = [P], Same file = sequential
   → Tests before implementation (TDD)
5. Number tasks sequentially T001-T023 ✅
6. Generate dependency graph ✅
7. Create parallel execution examples ✅
8. Validate completeness ✅
9. SUCCESS - tasks ready for execution
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- All file paths are absolute for Mac Studio deployment

## Path Conventions
**Web application structure** (from plan.md):
- **Backend**: `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/`
- **Tests**: `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/`
- **Migrations**: `/Users/kevin/github/northstar-funding/backend/src/main/resources/db/migration/`
- **Database**: PostgreSQL on Mac Studio (192.168.1.10:5432)

---

## Phase 3.1: Already Complete ✅

The following were implemented before the /specify workflow:

- ✅ **Domain entity**: `backend/src/main/java/com/northstar/funding/discovery/domain/Domain.java`
- ✅ **DomainStatus enum**: `backend/src/main/java/com/northstar/funding/discovery/domain/DomainStatus.java`
- ✅ **Extended FundingSourceCandidate**: Added domainId field
- ✅ **Extended CandidateStatus enum**: Added PENDING_CRAWL, SKIPPED_LOW_CONFIDENCE
- ✅ **DTOs**: SearchResult, MetadataJudgment, ProcessingStats
- ✅ **DomainRegistryService**: `backend/src/main/java/com/northstar/funding/discovery/service/DomainRegistryService.java`
- ✅ **MetadataJudgingService**: `backend/src/main/java/com/northstar/funding/discovery/service/MetadataJudgingService.java`
- ✅ **CandidateProcessingOrchestrator**: `backend/src/main/java/com/northstar/funding/discovery/service/CandidateProcessingOrchestrator.java`
- ✅ **Migration V8**: Create domain table
- ✅ **Migration V9**: Update candidate status for two-phase processing
- ✅ **Build verified**: `mvn clean compile` successful

---

## Phase 3.2: Repository Layer Tests (TDD Foundation)

**CRITICAL**: These tests MUST pass before repository implementation in Phase 3.3

- [ ] **T001** [P] DomainRepository integration test - domain deduplication in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/discovery/infrastructure/DomainRepositoryIT.java`
  - Test findByDomainName for unique constraint
  - Test existsByDomainName for fast lookup
  - Test findByStatus for blacklist queries
  - Test findDomainsReadyForRetry for exponential backoff
  - Test findHighQualityDomains and findLowQualityDomains
  - Use @DataJdbcTest and TestContainers
  - Assert all custom @Query methods return expected results

---

## Phase 3.3: Repository Implementation

- [ ] **T002** DomainRepository interface with Spring Data JDBC queries in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/infrastructure/DomainRepository.java`
  - Implement findByDomainName(String domainName)
  - Implement existsByDomainName(String domainName)
  - Implement findByStatus(DomainStatus status)
  - Implement findDomainsReadyForRetry(@Param("now") LocalDateTime now) with @Query
  - Implement findHighQualityDomains(@Param("minCount") Integer minCandidateCount)
  - Implement findLowQualityDomains(@Param("threshold") Integer lowQualityThreshold)
  - Implement findNoFundsForYear(@Param("year") Integer year)
  - Implement countByStatus(@Param("status") DomainStatus status)
  - Verify T001 integration tests pass after implementation

---

## Phase 3.4: Service Layer Unit Tests (TDD)

**CRITICAL**: Test business logic in isolation before integration tests

- [ ] **T003** [P] DomainRegistryService unit test in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/discovery/service/DomainRegistryServiceTest.java`
  - Mock DomainRepository with @Mock
  - Test shouldProcessDomain with various domain statuses (BLACKLISTED should return false)
  - Test registerDomain creates new domain vs returns existing
  - Test blacklistDomain sets correct status and audit fields
  - Test updateDomainQuality increments counters and updates status
  - Test recordProcessingFailure implements exponential backoff correctly
  - Test extractDomainName handles various URL formats
  - Use @ExtendWith(MockitoExtension.class)

- [ ] **T004** [P] MetadataJudgingService unit test in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/discovery/service/MetadataJudgingServiceTest.java`
  - Mock DomainRegistryService
  - Test judgeFundingKeywords scores high for "grant", "scholarship", etc.
  - Test judgeDomainCredibility scores high for .org, .gov, low for spam patterns
  - Test judgeGeographicRelevance scores high for "Bulgaria", "Eastern Europe"
  - Test judgeOrganizationType scores high for "foundation", "NGO"
  - Test calculateOverallConfidence weighted average calculation
  - Test confidence threshold: >= 0.6 → shouldCrawl = true, < 0.6 → shouldCrawl = false
  - Test extractOrganizationAndProgram from various title formats

- [ ] **T005** [P] CandidateProcessingOrchestrator unit test in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/discovery/service/CandidateProcessingOrchestratorTest.java`
  - Mock DomainRegistryService, MetadataJudgingService, FundingSourceCandidateRepository
  - Test processSearchResult creates PENDING_CRAWL candidate for high confidence
  - Test processSearchResult skips for low confidence
  - Test processSearchResult skips for blacklisted domain
  - Test processSearchResult skips for already-processed domain
  - Test processSearchResults aggregates stats correctly
  - Test Virtual Thread parallel execution (verify all results processed)

---

## Phase 3.5: Quickstart Integration Tests (End-to-End Scenarios)

**CRITICAL**: These tests validate complete workflows from quickstart.md

- [ ] **T006** [P] Quickstart Scenario 1: First-time domain discovery in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/integration/CrawlerQuickstartScenario1IT.java`
  - Given: New domain with high-quality metadata
  - When: processSearchResults called
  - Then: 1 candidate created with PENDING_CRAWL status, domain registered as PROCESSED_HIGH_QUALITY
  - Use @SpringBootTest + TestContainers
  - Verify domain quality metrics (highQualityCandidateCount, bestConfidenceScore)

- [ ] **T007** [P] Quickstart Scenario 2: Domain deduplication in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/integration/CrawlerQuickstartScenario2IT.java`
  - Given: Domain already processed (PROCESSED_LOW_QUALITY)
  - When: Search result from same domain
  - Then: 0 candidates created, skippedDomainAlreadyProcessed = 1
  - Verify domain status unchanged

- [ ] **T008** [P] Quickstart Scenario 3: Low-confidence skipping in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/integration/CrawlerQuickstartScenario3IT.java`
  - Given: Search result with poor metadata (no funding keywords, spam domain)
  - When: Metadata judging
  - Then: Confidence < 0.6, 0 candidates created, domain marked low quality
  - Verify skippedLowConfidence = 1

- [ ] **T009** [P] Quickstart Scenario 4: Blacklist management in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/integration/CrawlerQuickstartScenario4IT.java`
  - Given: Admin blacklists spam domain
  - When: Search result from blacklisted domain
  - Then: Auto-rejected, 0 candidates created, skippedBlacklisted = 1
  - Verify blacklist reason and admin audit trail

- [ ] **T010** [P] Quickstart Scenario 5: Parallel processing in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/integration/CrawlerQuickstartScenario5IT.java`
  - Given: 25 search results (mix of high/low quality)
  - When: processSearchResults with Virtual Threads
  - Then: All 25 processed, ~33% created as candidates, processing time < 5 seconds
  - Verify stats aggregation (totalProcessed, candidatesCreated, averageConfidence)

- [ ] **T011** [P] Quickstart Scenario 6: "No funds this year" re-evaluation in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/integration/CrawlerQuickstartScenario6IT.java`
  - Given: Domain marked NO_FUNDS_THIS_YEAR for 2024
  - When: Current year = 2025 (simulated), shouldProcessDomain called
  - Then: Returns true (allow re-processing in new year)
  - Verify noFundsYear tracking logic

---

## Phase 3.6: Query Generation Service (AI-Powered Search Queries)

- [ ] **T012** [P] QueryGenerationService interface in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/search/QueryGenerationService.java`
  - Define method: `List<String> generateQueries(String geography, String fundingType, int count)`
  - Examples: "Bulgaria education grants NGO", "Eastern Europe scholarships foundation"
  - Document AI-powered approach (LM Studio integration for Phase 2)
  - For Phase 1, provide hardcoded query templates as placeholder

- [ ] **T013** QueryGenerationService implementation in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/search/impl/TemplateQueryGenerationService.java`
  - Implement template-based query generation (placeholder for AI)
  - Geography templates: "Bulgaria", "Eastern Europe", "Balkans", "EU member states"
  - Funding type templates: "grants", "scholarships", "fellowships", "funding"
  - Organization types: "NGO", "foundation", "nonprofit", "education", "social enterprise"
  - Combine templates to generate diverse queries
  - Future: Replace with LM Studio AI generation

---

## Phase 3.7: Search Engine Adapters (Multi-Engine Support)

- [ ] **T014** [P] SearchEngine interface in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/search/SearchEngine.java`
  - Define method: `List<SearchResult> search(String query, int maxResults)`
  - SearchResult DTO includes: url, title, snippet, searchEngine, searchQuery, position
  - Document adapter pattern for multiple engines

- [ ] **T015** [P] SearxngAdapter (self-hosted search) in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/search/adapter/SearxngAdapter.java`
  - Implement SearchEngine interface
  - HTTP client to Searxng API (Mac Studio port 8080)
  - Parse JSON response to SearchResult DTOs
  - Handle errors gracefully (return empty list on failure)
  - Configuration: searxng.url from application.yml

- [ ] **T016** [P] TavilyAdapter in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/search/adapter/TavilyAdapter.java`
  - Implement SearchEngine interface
  - HTTP client to Tavily API
  - Parse JSON response to SearchResult DTOs
  - Configuration: tavily.apiKey from application.yml

- [ ] **T017** [P] BrowserbaseAdapter in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/search/adapter/BrowserbaseAdapter.java`
  - Implement SearchEngine interface
  - HTTP client to Browserbase API
  - Parse JSON response to SearchResult DTOs
  - Configuration: browserbase.apiKey from application.yml

- [ ] **T018** [P] PerplexityAdapter in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/search/adapter/PerplexityAdapter.java`
  - Implement SearchEngine interface
  - HTTP client to Perplexity API
  - Parse JSON response to SearchResult DTOs
  - Configuration: perplexity.apiKey from application.yml

---

## Phase 3.8: Discovery Orchestration & Scheduling

- [ ] **T019** DiscoveryService orchestration in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/service/DiscoveryService.java`
  - Create DiscoverySession entity (reuse existing from 001-automated-funding-discovery)
  - Orchestrate: Query generation → Search engines → Metadata judging → Candidate creation
  - Aggregate results from all search engines (Searxng, Tavily, Browserbase, Perplexity)
  - Record processing stats in DiscoverySession
  - Handle search engine failures gracefully (continue with available engines)

- [ ] **T020** Scheduled discovery job in `/Users/kevin/github/northstar-funding/backend/src/main/java/com/northstar/funding/discovery/scheduler/NightlyDiscoveryScheduler.java`
  - Use @Scheduled(cron = "0 0 2 * * ?") for 2 AM nightly execution
  - Call DiscoveryService to execute full discovery workflow
  - Log session results (candidates created, skipped counts, errors)
  - Configuration: discovery.schedule.enabled = true/false in application.yml
  - Error handling: retry failed searches, log errors, continue execution

---

## Phase 3.9: Polish & Documentation

- [ ] **T021** [P] Unit tests for query generation in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/discovery/search/impl/TemplateQueryGenerationServiceTest.java`
  - Test generateQueries produces diverse, relevant queries
  - Test geography templates cover Eastern Europe focus
  - Test funding type templates match domain keywords
  - Verify query count parameter respected

- [ ] **T022** [P] Performance test for parallel processing in `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/performance/MetadataJudgingPerformanceTest.java`
  - Verify 20-25 results processed < 5 seconds (constitutional requirement: <500ms per result API responses)
  - Test Virtual Thread efficiency vs sequential processing
  - Measure metadata judging time per result
  - Assert parallel execution provides speedup

- [ ] **T023** [P] Update README with crawler infrastructure documentation in `/Users/kevin/github/northstar-funding/backend/README.md`
  - Document domain-level deduplication strategy
  - Explain two-phase processing pipeline (Phase 1 metadata, Phase 2 deep crawling)
  - List search engine adapters and configuration
  - Describe blacklist management workflow
  - Add quickstart test execution instructions

---

## Dependencies

### Sequential Dependencies (Must Complete in Order)
- **T001** (DomainRepository test) → **T002** (DomainRepository implementation)
- **T003, T004, T005** (Service unit tests) → **T006-T011** (Quickstart integration tests)
- **T014** (SearchEngine interface) → **T015-T018** (Adapter implementations)
- **T002** (DomainRepository) → **T019** (DiscoveryService orchestration)
- **T012-T013** (Query generation) → **T019** (DiscoveryService)
- **T015-T018** (Search adapters) → **T019** (DiscoveryService)
- **T019** (DiscoveryService) → **T020** (Scheduled job)

### Parallel Opportunities ([P] Tasks)
**Group 1: Service Unit Tests** (T003-T005 can run in parallel):
- Different test files
- Mock dependencies
- No shared state

**Group 2: Quickstart Integration Tests** (T006-T011 can run in parallel):
- Different test files
- Each scenario independent
- TestContainers isolation

**Group 3: Search Engine Adapters** (T015-T018 can run in parallel):
- Different adapter files
- Same interface, different implementations
- No dependencies between adapters

**Group 4: Polish Tasks** (T021-T023 can run in parallel):
- Different files (unit test, performance test, README)
- No dependencies

---

## Parallel Execution Examples

### Example 1: Service Unit Tests (T003-T005)
```bash
# Launch all service tests in parallel
Task T003: "Unit test DomainRegistryService business logic"
Task T004: "Unit test MetadataJudgingService scoring algorithms"
Task T005: "Unit test CandidateProcessingOrchestrator workflow"
```

### Example 2: Quickstart Integration Tests (T006-T011)
```bash
# Launch all quickstart scenarios in parallel
Task T006: "Quickstart Scenario 1 - First-time domain discovery"
Task T007: "Quickstart Scenario 2 - Domain deduplication"
Task T008: "Quickstart Scenario 3 - Low-confidence skipping"
Task T009: "Quickstart Scenario 4 - Blacklist management"
Task T010: "Quickstart Scenario 5 - Parallel processing"
Task T011: "Quickstart Scenario 6 - No funds this year re-evaluation"
```

### Example 3: Search Engine Adapters (T015-T018)
```bash
# Launch all adapter implementations in parallel
Task T015: "Implement SearxngAdapter for self-hosted search"
Task T016: "Implement TavilyAdapter with API client"
Task T017: "Implement BrowserbaseAdapter with API client"
Task T018: "Implement PerplexityAdapter with API client"
```

---

## Constitutional Compliance Verification

### ✅ Technology Stack (Principle IV - NON-NEGOTIABLE)
- **T001-T023**: All tasks use Java 25, Spring Boot 3.5.5, Spring Data JDBC ✅
- **T010**: Virtual Threads (Java 25 feature) for parallel processing ✅
- **T001, T006-T011**: TestContainers for integration tests ✅

### ✅ Technology Constraints (Principle X - CRITICAL)
- **NO Kafka**: Using Spring Events (already implemented in orchestrator) ✅
- **NO crawl4j**: Using Jsoup for future Phase 2 (not Phase 1 metadata) ✅
- **NO langgraph4j, langchain4j**: Not used ✅
- **T015-T018**: Simple adapter pattern (not Spring Integration) ✅

### ✅ Complexity Management (Principle VI - ESSENTIAL)
- **T002**: Single DomainRepository (not multiple repositories) ✅
- **T012-T013**: Simple query generation (template-based, not complex AI initially) ✅
- **T019-T020**: Simple orchestration (direct service calls, not complex workflow engine) ✅

### ✅ Human-AI Collaboration (Principle III - MANDATORY)
- **T009**: Admin blacklist management workflow ✅
- **Phase 1**: AI judges metadata, humans review candidates (from 001-automated-funding-discovery) ✅
- **Blacklist**: Humans make final decisions on domain blocking ✅

---

## Validation Checklist
*GATE: Checked before execution*

- [x] All entities from data-model.md have repository tests (T001 for Domain)
- [x] All services have unit tests (T003-T005 for 3 services)
- [x] All quickstart scenarios have integration tests (T006-T011 for 6 scenarios)
- [x] All search adapters planned (T015-T018 for 4 engines)
- [x] Tests before implementation (TDD ordering respected)
- [x] Parallel tasks are truly independent (different files, no shared state)
- [x] Each task specifies exact absolute file path
- [x] No task modifies same file as another [P] task
- [x] Constitutional compliance verified for all tasks

---

## Success Criteria

**Ready for Implementation**: All 23 tasks provide complete, actionable steps for building the automated crawler infrastructure while maintaining constitutional compliance.

**Performance Targets**: T022 ensures <5 seconds for 25 results (parallelized), <500ms per result (constitutional API requirement).

**Quality Gates**: TDD approach with comprehensive test coverage (repository tests, service unit tests, 6 quickstart integration tests).

**Already Implemented**: Domain model, services, DTOs, migrations (completed before /specify) - focus on testing and remaining components.
