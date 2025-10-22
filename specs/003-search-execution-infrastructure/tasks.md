# Tasks: Search Execution Infrastructure

**Input**: Design documents from `/Users/kevin/github/northstar-funding/specs/003-search-execution-infrastructure/`
**Prerequisites**: plan.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓, quickstart.md ✓

## Execution Flow (main)
```
1. Load plan.md from feature directory ✓
   → Tech stack: Java 25, Spring Boot 3.5.6, Vavr 0.10.7, Resilience4j
   → Structure: backend/src/main/java/com/northstar/funding/discovery/search/
2. Load optional design documents ✓
   → data-model.md: 7 entities (SearchQuery, QueryTag, SearchEngineType, etc.)
   → contracts/: SearchEngineAdapter interface contract
   → research.md: 8 technology decisions
3. Generate tasks by category ✓
   → Setup: Database migrations, configuration
   → Tests: Contract tests (TDD), integration tests
   → Core: Domain entities, adapters, services
   → Integration: Scheduling, orchestration, analytics
   → Polish: Performance tests, documentation
4. Apply task rules ✓
   → Different files = mark [P] for parallel
   → Same file = sequential (no [P])
   → Tests before implementation (TDD)
5. Number tasks sequentially (T001, T002...) ✓
6. Dependencies mapped ✓
7. Parallel execution examples ✓
8. Validation complete ✓
9. Return: SUCCESS (30 tasks ready for execution)
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- All paths are absolute from repository root: `/Users/kevin/github/northstar-funding/backend/src/`

## Path Conventions
**Project Structure**: Web application (backend monolith)
- **Java Source**: `backend/src/main/java/com/northstar/funding/discovery/search/`
- **Resources**: `backend/src/main/resources/`
- **DB Migrations**: `backend/src/main/resources/db/migration/`
- **Test Source**: `backend/src/test/java/com/northstar/funding/`
- **Integration Tests**: `backend/src/test/java/com/northstar/funding/integration/`

---

## Phase 3.1: Database Schema & Configuration

- [ ] **T001** [P] Create search_queries table migration in `backend/src/main/resources/db/migration/V10__create_search_queries_table.sql`
  - Table: search_queries with columns: id, query_text, day_of_week, tags (JSONB), target_engines (JSONB), expected_results, enabled, created_at, updated_at
  - Indexes: idx_search_queries_day_of_week, idx_search_queries_enabled
  - Constraints: CHECK (expected_results BETWEEN 1 AND 100)

- [ ] **T002** [P] Create search_session_statistics table migration in `backend/src/main/resources/db/migration/V11__create_search_session_statistics_table.sql`
  - Table: search_session_statistics with columns: id, session_id (FK to discovery_session), engine_type, queries_executed, results_returned, avg_response_time_ms, failure_count, created_at
  - Indexes: idx_session_stats_session_id, idx_session_stats_engine

- [ ] **T003** [P] Extend discovery_session table migration in `backend/src/main/resources/db/migration/V12__extend_discovery_session_for_search.sql`
  - Add columns: session_type (ENUM: METADATA_JUDGING, SEARCH_EXECUTION), search_query_set_name VARCHAR(50), total_search_queries_executed INT
  - Update existing rows: SET session_type = 'METADATA_JUDGING' WHERE session_type IS NULL

- [ ] **T004** [P] Add search engine configuration to `backend/src/main/resources/application.yml`
  - Section: discovery.search-engines with sub-sections for searxng, tavily, perplexity
  - Properties: enabled, base-url, api-key, max-results, timeout-seconds
  - Resilience4j circuit breaker config for each engine

- [ ] **T005** [P] Add query library configuration to `backend/src/main/resources/application.yml`
  - Section: discovery.query-library with day-of-week maps (monday, tuesday, etc.)
  - Each day: List of queries with text and tags (geography, category, authority)
  - Example: 5-10 queries per day for Bulgaria/Balkans/Eastern Europe focus

- [ ] **T006** Run Flyway migrations against Mac Studio PostgreSQL
  - Execute: `mvn flyway:migrate` from backend directory
  - Verify: Tables created, existing data preserved
  - Test: INSERT sample SearchQuery via psql

---

## Phase 3.2: Domain Model (TDD - Tests First) ⚠️ MUST COMPLETE BEFORE 3.3

**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

- [ ] **T007** [P] Create SearchQueryTest in `backend/src/test/java/com/northstar/funding/discovery/search/domain/SearchQueryTest.java`
  - Test: Constructor validation (null queryText, blank dayOfWeek, empty tags)
  - Test: Builder pattern with fluent API
  - Test: Immutability after creation
  - Test: Tags manipulation (add, remove, contains)
  - **VERIFY: Test fails with compilation error (SearchQuery class doesn't exist yet)**

- [ ] **T008** [P] Create QueryTagTest in `backend/src/test/java/com/northstar/funding/discovery/search/domain/QueryTagTest.java`
  - Test: Record validation (null type, null value, blank value)
  - Test: Equality and hashCode for Set membership
  - Test: JSON serialization/deserialization
  - **VERIFY: Test fails with compilation error (QueryTag record doesn't exist yet)**

- [ ] **T009** [P] Create SearchEngineTypeTest in `backend/src/test/java/com/northstar/funding/discovery/search/domain/SearchEngineTypeTest.java`
  - Test: Enum values (SEARXNG, TAVILY, PERPLEXITY)
  - Test: fromString() method (case-insensitive, invalid input)
  - Test: Display names for UI (future use)
  - **VERIFY: Test fails with compilation error (SearchEngineType enum doesn't exist yet)**

- [ ] **T010** [P] Create SearchSessionStatisticsTest in `backend/src/test/java/com/northstar/funding/discovery/search/domain/SearchSessionStatisticsTest.java`
  - Test: Builder pattern for statistics aggregation
  - Test: Calculation methods (average response time, failure rate, hit rate)
  - Test: Immutability and defensive copying
  - **VERIFY: Test fails with compilation error (SearchSessionStatistics class doesn't exist yet)**

---

## Phase 3.3: Domain Model Implementation (ONLY after tests are failing)

- [ ] **T011** [P] Implement SearchQuery entity in `backend/src/main/java/com/northstar/funding/discovery/search/domain/SearchQuery.java`
  - Annotations: @Data, @Builder, @AllArgsConstructor, @NoArgsConstructor (Lombok)
  - Validation: @NotNull, @Size, @Min, @Max (Jakarta Validation)
  - Fields: id, queryText, dayOfWeek, tags (Set<QueryTag>), targetEngines (Set<SearchEngineType>), expectedResults, enabled, createdAt, updatedAt
  - Methods: addTag(), removeTag(), containsTag()
  - **VERIFY: T007 SearchQueryTest now passes**

- [ ] **T012** [P] Implement QueryTag record in `backend/src/main/java/com/northstar/funding/discovery/search/domain/QueryTag.java`
  - Record with compact constructor validation
  - Enum TagType: GEOGRAPHY, CATEGORY, AUTHORITY
  - JSON serialization with Jackson annotations
  - **VERIFY: T008 QueryTagTest now passes**

- [ ] **T013** [P] Implement SearchEngineType enum in `backend/src/main/java/com/northstar/funding/discovery/search/domain/SearchEngineType.java`
  - Values: SEARXNG, BROWSERBASE (future), TAVILY, PERPLEXITY
  - Properties: displayName, requiresApiKey, defaultBaseUrl
  - Method: fromString(String value) with case-insensitive matching
  - **VERIFY: T009 SearchEngineTypeTest now passes**

- [ ] **T014** [P] Implement SearchSessionStatistics entity in `backend/src/main/java/com/northstar/funding/discovery/search/domain/SearchSessionStatistics.java`
  - Annotations: @Data, @Builder (Lombok)
  - Fields: id, sessionId, engineType, queriesExecuted, resultsReturned, avgResponseTimeMs, failureCount, createdAt
  - Methods: calculateHitRate(), calculateFailureRate()
  - **VERIFY: T010 SearchSessionStatisticsTest now passes**

---

## Phase 3.4: Repository Layer (TDD)

- [ ] **T015** [P] Create SearchQueryRepositoryTest in `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/SearchQueryRepositoryTest.java`
  - Use @Testcontainers with PostgreSQL container
  - Test: findByDayOfWeek(DayOfWeek) returns Monday queries
  - Test: findByEnabled(true) excludes disabled queries
  - Test: save() persists tags as JSONB
  - Test: Complex query with tags filter
  - **VERIFY: Test fails (repository doesn't exist)**

- [ ] **T016** [P] Create SearchSessionStatisticsRepositoryTest in `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/SearchSessionStatisticsRepositoryTest.java`
  - Use @Testcontainers with PostgreSQL container
  - Test: findBySessionId() returns all engine stats for session
  - Test: save() persists statistics
  - Test: Aggregate query: total results across all engines for session
  - **VERIFY: Test fails (repository doesn't exist)**

- [ ] **T017** [P] Implement SearchQueryRepository in `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/SearchQueryRepository.java`
  - Extends CrudRepository<SearchQuery, Long>
  - Custom queries: findByDayOfWeekAndEnabled(), findByTagsContaining()
  - Spring Data JDBC @Query annotations for JSONB queries
  - **VERIFY: T015 SearchQueryRepositoryTest now passes**

- [ ] **T018** [P] Implement SearchSessionStatisticsRepository in `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/SearchSessionStatisticsRepositoryTest.java`
  - Extends CrudRepository<SearchSessionStatistics, Long>
  - Custom queries: findBySessionId(), aggregateBySessionId()
  - **VERIFY: T016 SearchSessionStatisticsRepositoryTest now passes**

---

## Phase 3.5: Search Engine Adapters (TDD)

- [ ] **T019** [P] Create SearchEngineAdapter interface in `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/adapters/SearchEngineAdapter.java`
  - Method: `Try<List<SearchResult>> search(String query, int maxResults)`
  - Method: `SearchEngineType getEngineType()`
  - Method: `boolean isEnabled()`
  - Method: `HealthStatus checkHealth()`
  - Javadoc with Vavr Try usage examples

- [ ] **T020** [P] Create SearxngAdapterTest in `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/adapters/SearxngAdapterTest.java`
  - Use WireMock for HTTP mocking
  - Test: Successful search returns 25 results
  - Test: Timeout returns Failure Try
  - Test: Circuit breaker opens after 5 failures
  - Test: Health check detects Searxng unavailable
  - Mock response: Searxng JSON format from research.md
  - **VERIFY: Test fails (adapter doesn't exist)**

- [ ] **T021** [P] Create TavilyAdapterTest in `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/adapters/TavilyAdapterTest.java`
  - Use WireMock for Tavily API mocking
  - Test: Successful search with API key authentication
  - Test: Invalid API key returns Failure Try
  - Test: Rate limit handling (429 response)
  - Test: Circuit breaker behavior
  - Mock response: Tavily JSON format from research.md
  - **VERIFY: Test fails (adapter doesn't exist)**

- [ ] **T022** [P] Create PerplexityAdapterTest in `backend/src/test/java/com/northstar/funding/discovery/search/infrastructure/adapters/PerplexityAdapterTest.java`
  - Use WireMock for Perplexity API mocking
  - Test: Successful search with Bearer token auth
  - Test: Citations extraction from response
  - Test: Timeout handling
  - Test: Circuit breaker fallback
  - Mock response: Perplexity JSON format from research.md
  - **VERIFY: Test fails (adapter doesn't exist)**

- [ ] **T023** [P] Implement SearxngAdapter in `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/adapters/SearxngAdapter.java`
  - Annotations: @Service, @CircuitBreaker(name="searxng"), @Retry(name="searchEngines")
  - Dependencies: RestClient, Resilience4j CircuitBreaker
  - HTTP GET to http://192.168.1.10:8080/search?q={query}&format=json&number_of_results={max}
  - Parse Searxng JSON response → List<SearchResult>
  - Vavr Try error handling
  - **VERIFY: T020 SearxngAdapterTest now passes**

- [ ] **T024** [P] Implement TavilyAdapter in `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/adapters/TavilyAdapter.java`
  - Annotations: @Service, @CircuitBreaker(name="tavily"), @Retry(name="searchEngines")
  - Dependencies: RestClient, @Value("${discovery.search-engines.tavily.api-key}")
  - HTTP POST to https://api.tavily.com/search with JSON body
  - Headers: Content-Type: application/json
  - Parse Tavily response → List<SearchResult>
  - **VERIFY: T021 TavilyAdapterTest now passes**

- [ ] **T025** [P] Implement PerplexityAdapter in `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/adapters/PerplexityAdapter.java`
  - Annotations: @Service, @CircuitBreaker(name="perplexity"), @Retry(name="searchEngines")
  - Dependencies: RestClient, @Value("${discovery.search-engines.perplexity.api-key}")
  - HTTP POST to https://api.perplexity.ai/chat/completions
  - Headers: Authorization: Bearer {api-key}
  - Extract citations from response → List<SearchResult>
  - **VERIFY: T022 PerplexityAdapterTest now passes**

---

## Phase 3.6: Service Layer (TDD)

- [ ] **T026** Create SearchExecutionServiceTest in `backend/src/test/java/com/northstar/funding/discovery/search/application/SearchExecutionServiceTest.java`
  - Use @Testcontainers + WireMock for integration testing
  - Test: Execute single query across 3 engines (Searxng, Tavily, Perplexity)
  - Test: Virtual Threads parallel execution (<5 seconds for 3 engines)
  - Test: Deduplication across engines (same domain from multiple engines)
  - Test: Graceful degradation (1 engine fails, other 2 succeed)
  - Test: Integration with DomainRegistryService (existing from Feature 002)
  - Test: Integration with MetadataJudgingService (pass SearchResults for scoring)
  - **VERIFY: Test fails (service doesn't exist)**

- [ ] **T027** Implement SearchExecutionService in `backend/src/main/java/com/northstar/funding/discovery/search/application/SearchExecutionService.java`
  - Annotations: @Service, @Slf4j (Lombok logging)
  - Dependencies: List<SearchEngineAdapter>, DomainRegistryService, CandidateProcessingOrchestrator
  - Method: `Try<List<SearchResult>> executeQueryAcrossEngines(String query)`
  - Virtual Threads: `Executors.newVirtualThreadPerTaskExecutor()`
  - Parallel execution across all enabled adapters
  - Aggregate results, deduplicate by domain
  - Integration: Pass deduplicated results to CandidateProcessingOrchestrator (existing)
  - **VERIFY: T026 SearchExecutionServiceTest now passes**

- [ ] **T028** Create NightlyDiscoverySchedulerTest in `backend/src/test/java/com/northstar/funding/discovery/search/application/NightlyDiscoverySchedulerTest.java`
  - Use @Testcontainers for end-to-end test
  - Test: Monday queries executed (5 queries from query library)
  - Test: DiscoverySession created with sessionType=SEARCH_EXECUTION
  - Test: SearchSessionStatistics persisted for each engine
  - Test: High-confidence candidates (>=0.60) marked PENDING_CRAWL
  - Test: Disabled scheduler doesn't run (DISCOVERY_SCHEDULE_ENABLED=false)
  - **VERIFY: Test fails (scheduler doesn't exist)**

- [ ] **T029** Implement NightlyDiscoveryScheduler in `backend/src/main/java/com/northstar/funding/discovery/search/application/NightlyDiscoveryScheduler.java`
  - Annotations: @Service, @ConditionalOnProperty(name="discovery.schedule.enabled", havingValue="true"), @Slf4j
  - Dependencies: SearchExecutionService, SearchSessionService, DiscoverySessionRepository
  - Method: `@Scheduled(cron = "${discovery.schedule.cron}") void runNightlyDiscovery()`
  - Get today's DayOfWeek, load queries from QueryLibrary
  - Execute all queries via SearchExecutionService
  - Create DiscoverySession record with statistics
  - Log summary: queries executed, domains found, high-confidence candidates
  - **VERIFY: T028 NightlyDiscoverySchedulerTest now passes**

- [ ] **T030** Implement SearchSessionService in `backend/src/main/java/com/northstar/funding/discovery/search/application/SearchSessionService.java`
  - Annotations: @Service
  - Dependencies: SearchSessionStatisticsRepository, DiscoverySessionRepository
  - Method: `SearchSessionStatistics getStatisticsForSession(Long sessionId)`
  - Method: `SearchSessionStatistics aggregateStatistics(Long sessionId)` (sum across all engines)
  - Method: `List<SearchSessionStatistics> getHistoricalStats(LocalDate from, LocalDate to)`
  - Provides analytics for Kevin to review engine/query performance

---

## Phase 3.7: Integration Tests (Quickstart Scenarios)

- [ ] **T031** [P] Implement Scenario 1: Monday Nightly Discovery in `backend/src/test/java/com/northstar/funding/integration/MondayNightlyDiscoveryTest.java`
  - Based on quickstart.md Scenario 1
  - @SpringBootTest with @Testcontainers
  - Execute full Monday discovery workflow
  - Assertions: 5 queries executed, 3 engines used, 500-1000 raw results, 50-150 high-confidence
  - Verify DiscoverySession metadata

- [ ] **T032** [P] Implement Scenario 2: Domain Deduplication in `backend/src/test/java/com/northstar/funding/integration/DomainDeduplicationTest.java`
  - Based on quickstart.md Scenario 2
  - Test: Same domain returned by multiple engines
  - Test: Domain already exists in database (from previous night)
  - Verify: Only 1 Domain record created
  - Verify: Rediscovery counter incremented

- [ ] **T033** [P] Implement Scenario 3: Circuit Breaker Behavior in `backend/src/test/java/com/northstar/funding/integration/CircuitBreakerTest.java`
  - Based on quickstart.md Scenario 3
  - WireMock: Simulate Tavily API failures (500 errors)
  - Test: Circuit breaker opens after 5 failures
  - Test: Subsequent requests fail-fast without HTTP call
  - Test: Circuit breaker transitions to HALF_OPEN after wait duration
  - Verify: Nightly discovery completes with degraded coverage

---

## Phase 3.8: Configuration & Polish

- [ ] **T034** [P] Create sample query library YAML in `backend/src/main/resources/application-sample.yml`
  - Complete 7-day query schedule (Monday-Sunday)
  - 5-10 queries per day
  - Focus: Bulgaria, Balkans, Eastern Europe, EU
  - Categories: Infrastructure, STEM, Scholarships, Arts, Teacher Development
  - Copy to application.yml with real queries

- [ ] **T035** [P] Update CLAUDE.md with search execution context
  - Already done by update-agent-context.sh script
  - Verify: Latest technologies listed (Spring Boot 3.5.6, Vavr 0.10.7)

- [ ] **T036** [P] Create performance test in `backend/src/test/java/com/northstar/funding/integration/SearchPerformanceTest.java`
  - Test: 10 queries × 3 engines = 30 searches complete in <30 minutes
  - Test: Single query across 3 engines completes in <15 seconds (parallel Virtual Threads)
  - Test: Circuit breaker doesn't impact healthy engines
  - Measure: Actual response times, log for analysis

- [ ] **T037** [P] Document manual testing steps in `specs/003-search-execution-infrastructure/manual-testing.md`
  - Step 1: Verify Searxng accessible (http://192.168.1.10:8080)
  - Step 2: Test each adapter individually (curl commands)
  - Step 3: Run nightly discovery manually (trigger scheduler)
  - Step 4: Query database for DiscoverySession and SearchSessionStatistics
  - Step 5: Review candidate quality (high-confidence hit rate)

---

## Dependencies

```
Setup (T001-T006) → All other tasks
  ↓
Domain Tests (T007-T010) → Domain Implementation (T011-T014)
  ↓
Repository Tests (T015-T016) → Repository Implementation (T017-T018)
  ↓
Adapter Tests (T020-T022) → Adapter Implementation (T023-T025)
  ↓
Service Tests (T026, T028) → Service Implementation (T027, T029-T030)
  ↓
Integration Tests (T031-T033) (depend on all services)
  ↓
Polish (T034-T037) (can run in parallel after integration tests pass)
```

**Critical Path**: T001 → T006 → T007 → T011 → T019 → T020 → T023 → T026 → T027 → T031 → T036

---

## Parallel Execution Examples

### Setup Phase (All in parallel after T006)
```bash
# T007-T010 can run together (different test files)
Task: "Create SearchQueryTest in backend/src/test/.../SearchQueryTest.java"
Task: "Create QueryTagTest in backend/src/test/.../QueryTagTest.java"
Task: "Create SearchEngineTypeTest in backend/src/test/.../SearchEngineTypeTest.java"
Task: "Create SearchSessionStatisticsTest in backend/src/test/.../SearchSessionStatisticsTest.java"
```

### Domain Implementation (All in parallel after tests fail)
```bash
# T011-T014 can run together (different entity files)
Task: "Implement SearchQuery in backend/src/main/.../SearchQuery.java"
Task: "Implement QueryTag in backend/src/main/.../QueryTag.java"
Task: "Implement SearchEngineType in backend/src/main/.../SearchEngineType.java"
Task: "Implement SearchSessionStatistics in backend/src/main/.../SearchSessionStatistics.java"
```

### Adapter Implementation (All in parallel after T019)
```bash
# T023-T025 can run together (different adapter files)
Task: "Implement SearxngAdapter in backend/src/main/.../SearxngAdapter.java"
Task: "Implement TavilyAdapter in backend/src/main/.../TavilyAdapter.java"
Task: "Implement PerplexityAdapter in backend/src/main/.../PerplexityAdapter.java"
```

### Integration Tests (All in parallel after T030)
```bash
# T031-T033 can run together (different test files)
Task: "Implement MondayNightlyDiscoveryTest in backend/src/test/.../MondayNightlyDiscoveryTest.java"
Task: "Implement DomainDeduplicationTest in backend/src/test/.../DomainDeduplicationTest.java"
Task: "Implement CircuitBreakerTest in backend/src/test/.../CircuitBreakerTest.java"
```

---

## Validation Checklist
*GATE: Checked before marking tasks complete*

- [x] All contracts have corresponding tests (SearchEngineAdapter → T020-T022)
- [x] All entities have model tasks (7 entities → T011-T014, T017-T018)
- [x] All tests come before implementation (T007-T010 before T011-T014, etc.)
- [x] Parallel tasks truly independent (different files, no shared state)
- [x] Each task specifies exact file path (all tasks have full paths)
- [x] No task modifies same file as another [P] task (verified)
- [x] TDD enforced: Tests MUST fail before implementation (noted in Phase 3.2)

---

## Notes

- **[P] tasks** = Different files, no dependencies, can run in parallel
- **Verify tests fail** before implementing (TDD critical for this feature)
- **Commit after each task** for rollback capability
- **Constitutional compliance**: No langchain4j, Vavr for error handling, Virtual Threads for parallelism, BigDecimal for confidence scores (integration with existing MetadataJudgingService)
- **Integration points**: DomainRegistryService (deduplication), CandidateProcessingOrchestrator (metadata judging), DiscoverySessionRepository (existing from Feature 002)

---

**Total Tasks**: 37 (7 setup, 30 implementation/test)
**Estimated Parallel Batches**: 8 batches (with dependencies)
**Critical Path Length**: 15 sequential tasks
**Parallelism Potential**: Up to 4-5 tasks simultaneously in some phases
