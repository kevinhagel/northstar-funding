# Tasks: Automated Search Adapter Infrastructure

**Feature**: 014-create-automated-crawler
**Input**: Design documents from `/Users/kevin/github/northstar-funding/specs/014-create-automated-crawler/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

## Execution Flow
```
1. Load plan.md from feature directory ✅
   → Tech stack: Java 25, Spring Boot 3.5.7, Spring WebClient, Virtual Threads
   → Module: northstar-search-adapters (existing)
2. Load design documents: ✅
   → data-model.md: SearchAdapter interface, 4 implementations, workflow models
   → contracts/: search-adapter-contract.yml, search-workflow-contract.yml
   → research.md: HTTP client (WebClient), concurrency (Virtual Threads), 7-day distribution
3. Generate tasks by category: ✅
   → Setup: Module structure, dependencies, configuration
   → Tests: Contract tests (5), unit tests (5), integration tests (2)
   → Core: Interface, 4 adapter implementations, workflow orchestrator
   → Integration: Configuration, Spring beans, properties
   → Polish: Documentation, manual testing, effectiveness analysis
4. Apply task rules: ✅
   → Different adapter files = [P] parallel
   → Tests before implementation (TDD)
   → Contract tests MUST FAIL before implementing
5. Number tasks sequentially (T001-T026) ✅
6. Generate dependency graph ✅
7. Create parallel execution examples ✅
8. Task completeness validated ✅
```

## Path Conventions
**Java multi-module Maven project**:
- Module: `northstar-search-adapters/`
- Source: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/`
- Tests: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/`
- Resources: `northstar-search-adapters/src/main/resources/`

## Phase 3.1: Setup & Configuration

### T001: ✅ Create module structure for search adapters
**File**: `northstar-search-adapters/pom.xml` (verify/update)
**Action**:
- Verify `northstar-search-adapters` module exists in parent pom.xml
- Add dependencies to module pom.xml:
  - `spring-boot-starter-webflux` (for WebClient)
  - `spring-boot-starter-test` (existing)
  - `mockito-core` (existing)
  - `wiremock-jre8` (for mocking HTTP responses)
- Verify Java 25 source/target level
- Ensure parent dependency management

### T002: ✅ Create base package structure
**Files**: Create directories in `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/`
**Action**:
- `adapter/` - base adapter classes
- `brave/` - Brave Search adapter
- `serper/` - Serper.dev adapter
- `searxng/` - SearXNG adapter (may exist - verify)
- `tavily/` - Tavily adapter
- `workflow/` - orchestration services
- `config/` - Spring configuration
- `exception/` - custom exceptions
- `model/` - request/response models

### T003: ✅ Create externalized configuration properties
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/config/SearchAdapterProperties.java`
**Action**:
- Create `@ConfigurationProperties(prefix = "search-adapters")` class
- Add nested config classes for each adapter (BraveConfig, SerperConfig, SearxngConfig, TavilyConfig)
- Include: apiKey, apiUrl, timeoutSeconds for each
- Use Lombok `@Data` for boilerplate

## Phase 3.2: Tests First (TDD) ⚠️ MUST COMPLETE BEFORE PHASE 3.3
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**

### T004: ✅ Create SearchAdapter contract test base class
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/contract/SearchAdapterContractTest.java`
**Action**:
- Create abstract base class for all adapter contract tests
- Define test scenarios from `contracts/search-adapter-contract.yml`:
  - `testSearchWithResults()` - valid query returns non-empty list
  - `testSearchWithZeroResults()` - valid query, no matches, returns empty (NOT error)
  - `testSearchApiFailure()` - invalid API key throws SearchAdapterException
  - `testGetEngineType()` - returns correct SearchEngineType enum
  - `testIsAvailable()` - returns true when configured correctly
- Use JUnit 5 `@Test` annotations
- Each test MUST FAIL (no implementation yet)
- Use WireMock for mocking HTTP responses

### T005: ✅ Contract test for BraveSearchAdapter
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/contract/BraveAdapterContractTest.java`
**Action**:
- Extend `SearchAdapterContractTest`
- Override abstract methods to provide Brave-specific test data
- Configure WireMock to mock Brave Search API @ https://api.search.brave.com/res/v1/web/search
- Test MUST FAIL (BraveAdapter not implemented yet)

### T006: ✅ Contract test for SerperAdapter
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/contract/SerperAdapterContractTest.java`
**Action**:
- Extend `SearchAdapterContractTest`
- Override abstract methods for Serper.dev-specific test data
- Configure WireMock to mock Serper API @ https://google.serper.dev/search
- Test MUST FAIL (SerperAdapter not implemented yet)

### T007: ✅ Contract test for SearXNGAdapter
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/contract/SearXNGAdapterContractTest.java`
**Action**:
- Extend `SearchAdapterContractTest`
- Override abstract methods for SearXNG-specific test data
- Configure WireMock to mock SearXNG @ http://192.168.1.10:8080
- Test MUST FAIL (SearXNGAdapter may exist but may not satisfy contract)

### T008: ✅ Contract test for TavilyAdapter
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/contract/TavilyAdapterContractTest.java`
**Action**:
- Extend `SearchAdapterContractTest`
- Override abstract methods for Tavily-specific test data
- Configure WireMock to mock Tavily API @ https://api.tavily.com/search
- Test MUST FAIL (TavilyAdapter not implemented yet)

### T009: ✅ Contract test for SearchWorkflowService
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/contract/SearchWorkflowServiceContractTest.java`
**Action**:
- Define test scenarios from `contracts/search-workflow-contract.yml`:
  - `testExecuteNightlySearch()` - Monday with 4 categories, 4 adapters
  - `testAdapterFailureResilience()` - 3 adapters working, 1 failing, continues successfully
- Mock: QueryGenerationService, all 4 SearchAdapters, SearchResultProcessor, DiscoverySessionService
- Verify: DiscoverySession created, queries generated, searches executed in parallel, results processed
- Test MUST FAIL (SearchWorkflowService not implemented yet)

## Phase 3.3: Core Implementation (ONLY after tests are failing)

### T004: ✅ Create SearchAdapter contract test base class

### T010: ✅ Create SearchAdapter interface
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/SearchAdapter.java`
**Action**:
- Define interface per `data-model.md`:
  ```java
  List<SearchResult> search(String query, int maxResults);
  SearchEngineType getEngineType();
  boolean isAvailable();
  ```
- Add JavaDoc comments explaining contract
- Zero results returns empty list (NOT exception)
- API failures throw SearchAdapterException

### T011: ✅ Create SearchAdapterException
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/exception/SearchAdapterException.java`
**Action**:
- Extend RuntimeException
- Include: SearchEngineType, query, message, cause
- Constructor for network failures, API errors, timeout
- NOT thrown for zero results

### T012: [P] Implement BraveSearchAdapter
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/brave/BraveSearchAdapter.java`
**Action**:
- Implement SearchAdapter interface
- Use Spring WebClient for HTTP requests
- API endpoint: https://api.search.brave.com/res/v1/web/search
- Authentication: API key in header `X-Subscription-Token`
- Map JSON response to List<SearchResult>
- Handle: 401 (invalid key), 429 (rate limit), timeout, network errors
- Return empty list for zero results (NOT exception)
- Run T005 contract test - should PASS

### T013: [P] Implement SerperAdapter
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/serper/SerperAdapter.java`
**Action**:
- Implement SearchAdapter interface
- Use Spring WebClient
- API endpoint: https://google.serper.dev/search
- Authentication: API key in header `X-API-KEY`
- Map JSON response (field: `organic[].link`, `organic[].title`, `organic[].snippet`)
- Handle errors same as Brave
- Run T006 contract test - should PASS

### T014: [P] Implement SearXNGAdapter
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/searxng/SearXNGAdapter.java`
**Action**:
- Verify if existing implementation satisfies SearchAdapter contract
- If not: refactor to match interface
- Use Spring WebClient
- API endpoint: http://192.168.1.10:8080/search?q={query}&format=json
- No authentication required (self-hosted)
- Map JSON response (field: `results[].url`, `results[].title`, `results[].content`)
- Handle: network errors, timeout (no 401 since no auth)
- Run T007 contract test - should PASS

### T015: [P] Implement TavilyAdapter
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/tavily/TavilyAdapter.java`
**Action**:
- Implement SearchAdapter interface
- Use Spring WebClient
- API endpoint: https://api.tavily.com/search
- Authentication: API key in JSON body
- Accepts natural language queries (AI-optimized)
- Map JSON response (field: `results[].url`, `results[].title`, `results[].content`)
- Handle errors same as Brave
- Run T008 contract test - should PASS

### T016: Create DayOfWeekCategories utility class
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/workflow/DayOfWeekCategories.java`
**Action**:
- Create static Map<DayOfWeek, List<FundingSearchCategory>>
- Distribution per `research.md` R5:
  - MONDAY: 4 categories (INDIVIDUAL_SCHOLARSHIPS, STUDENT_FINANCIAL_AID, TEACHER_SCHOLARSHIPS, ACADEMIC_FELLOWSHIPS)
  - TUESDAY: 5 categories (PROGRAM_GRANTS, CURRICULUM_DEVELOPMENT, AFTER_SCHOOL_PROGRAMS, SUMMER_PROGRAMS, EXTRACURRICULAR_ACTIVITIES)
  - WEDNESDAY: 3 categories (INFRASTRUCTURE_FUNDING, TECHNOLOGY_EQUIPMENT, LIBRARY_RESOURCES)
  - THURSDAY: 3 categories (TEACHER_DEVELOPMENT, PROFESSIONAL_TRAINING, ADMINISTRATIVE_CAPACITY)
  - FRIDAY: 4 categories (STEM_EDUCATION, ARTS_EDUCATION, SPECIAL_NEEDS_EDUCATION, LANGUAGE_PROGRAMS)
  - SATURDAY: 3 categories (COMMUNITY_PARTNERSHIPS, PARENT_ENGAGEMENT, NGO_EDUCATION_PROJECTS)
  - SUNDAY: 8 categories (EDUCATION_RESEARCH, PILOT_PROGRAMS, INNOVATION_GRANTS, EARLY_CHILDHOOD_EDUCATION, ADULT_EDUCATION, VOCATIONAL_TRAINING, EDUCATIONAL_TECHNOLOGY, ARTS_CULTURE)
- Static method: `List<FundingSearchCategory> getCategories(DayOfWeek day)`

### T017: Create workflow request/result models
**Files**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/model/`
**Action**:
- Create `ManualSearchRequest.java`:
  - Fields: List<FundingSearchCategory> categories, List<SearchEngineType> engines, int maxResultsPerQuery, String geographicScope
  - Use Lombok `@Data`, `@Builder`
- Create `SearchWorkflowResult.java`:
  - Fields per `data-model.md`: sessionId, queriesGenerated, totalResultsFound, candidatesCreated, duplicatesSkipped, blacklistedSkipped, lowConfidenceSkipped, zeroResultCount, resultsByEngine, zeroResultsByEngine, executionDuration, hasFailures, failureMessages
  - Use Lombok `@Data`, `@Builder`

### T018: Implement SearchWorkflowService
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/workflow/SearchWorkflowService.java`
**Action**:
- Annotate: `@Service`, `@Transactional`
- Dependencies: QueryGenerationService, List<SearchAdapter>, SearchResultProcessor, DiscoverySessionService
- Implement `executeNightlySearch(DayOfWeek dayOfWeek)`:
  1. Get categories for day from DayOfWeekCategories
  2. Create DiscoverySession
  3. For each category: generate queries (QueryGenerationService)
  4. For each query: execute searches in parallel across adapters (Virtual Threads)
  5. Process results with SearchResultProcessor
  6. Track zero-result outcomes in search_session_statistics
  7. Update session statistics
  8. Return SearchWorkflowResult
- Use `Executors.newVirtualThreadPerTaskExecutor()` for parallel execution
- Handle adapter failures: continue with working adapters, log failures
- Run T009 contract test - should PASS

## Phase 3.4: Integration

### T019: Create Spring configuration for adapters
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/searchadapters/config/SearchAdapterConfiguration.java`
**Action**:
- Annotate: `@Configuration`, `@EnableConfigurationProperties(SearchAdapterProperties.class)`
- Create `@Bean WebClient.Builder webClientBuilder()` - base configuration for all adapters
- Create `@Bean` methods for each adapter:
  - `braveSearchAdapter(WebClient.Builder builder, SearchAdapterProperties props)`
  - `serperAdapter(WebClient.Builder builder, SearchAdapterProperties props)`
  - `searxngAdapter(WebClient.Builder builder, SearchAdapterProperties props)`
  - `tavilyAdapter(WebClient.Builder builder, SearchAdapterProperties props)`
- Each bean checks if adapter is configured (API key present) before instantiating
- If not configured: return null or use @ConditionalOnProperty

### T020: Create application.yml configuration
**File**: `northstar-search-adapters/src/main/resources/application.yml`
**Action**:
- Add search-adapters configuration section:
  ```yaml
  search-adapters:
    brave:
      api-key: ${BRAVE_API_KEY}
      api-url: https://api.search.brave.com/res/v1/web/search
      timeout-seconds: 10
    serper:
      api-key: ${SERPER_API_KEY}
      api-url: https://google.serper.dev/search
      timeout-seconds: 10
    searxng:
      api-url: http://192.168.1.10:8080
      timeout-seconds: 10
    tavily:
      api-key: ${TAVILY_API_KEY}
      api-url: https://api.tavily.com/search
      timeout-seconds: 15
  ```
- Document environment variables needed: BRAVE_API_KEY, SERPER_API_KEY, TAVILY_API_KEY

### T021: [P] Create unit tests for adapters with mocked HTTP
**Files**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/unit/`
**Action**:
- Create unit tests for each adapter (BraveAdapterTest, SerperAdapterTest, SearXNGAdapterTest, TavilyAdapterTest)
- Use WireMock to mock HTTP responses
- Test scenarios:
  - Successful search with results
  - Zero results (empty response)
  - API authentication failure (401)
  - Rate limit (429)
  - Network timeout
  - Invalid JSON response
- Use Mockito for WebClient mocking
- All tests should PASS (implementations done in T012-T015)

### T022: Create SearchWorkflowService unit tests
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/unit/SearchWorkflowServiceTest.java`
**Action**:
- Mock: QueryGenerationService, all 4 SearchAdapters, SearchResultProcessor, DiscoverySessionService
- Test scenarios:
  - Monday execution with 4 categories
  - Adapter failure resilience (1 adapter throws exception, others continue)
  - Zero results tracking (verify zeroResultCount incremented)
  - Statistics tracking (verify SearchWorkflowResult populated correctly)
- Use Mockito for all dependencies
- Test should PASS (implementation done in T018)

## Phase 3.5: Integration Tests

### T023: Create end-to-end workflow integration test
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/integration/SearchWorkflowIntegrationTest.java`
**Action**:
- Use TestContainers for PostgreSQL
- Use WireMock for mocking all 4 search engine APIs
- Test scenario from `quickstart.md`:
  - Categories: [INDIVIDUAL_SCHOLARSHIPS, STEM_EDUCATION]
  - Engines: [TAVILY, SEARXNG]
  - Expected: ~6 queries generated, ~120 search results, ~20-40 candidates created
- Verify:
  - DiscoverySession record created
  - search_result records created
  - domain records registered
  - funding_source_candidate records created (PENDING_CRAWL status)
  - search_session_statistics tracking zero results
- This is the FIRST test that runs against real workflow (mocked APIs, real database, real services)

### T024: Create adapter effectiveness tracking test
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/searchadapters/integration/AdapterEffectivenessTrackingTest.java`
**Action**:
- Use TestContainers for PostgreSQL
- Mock: All search adapters (some return results, some return empty)
- Execute nightly search
- Verify: search_session_statistics table populated correctly
- Query: Verify zero_result=true for adapters that returned empty
- Query: Verify results_count matches actual results returned
- This validates FR-022 (zero-result tracking)

## Phase 3.6: Polish

### T025: [P] Update module README documentation
**File**: `northstar-search-adapters/README.md`
**Action**:
- Document: Module purpose (search adapter infrastructure)
- List: 4 adapter implementations (Brave, Serper, SearXNG, Tavily)
- Configuration: How to set API keys (environment variables)
- Usage: How to execute manual search
- Architecture: SearchAdapter interface, SearchWorkflowService orchestrator
- Testing: Contract tests, unit tests, integration tests
- Performance: Virtual Threads for parallel execution, < 10 minute nightly runs

### T026: Execute manual search per quickstart.md
**Action**: Manual validation (not code)
**File**: Follow `specs/014-create-automated-crawler/quickstart.md`
**Steps**:
1. Verify prerequisites (PostgreSQL, Ollama, SearXNG running)
2. Configure API keys in environment variables
3. Build project: `mvn clean compile`
4. Run all tests: `mvn test -pl northstar-search-adapters`
5. Execute manual search for Monday categories
6. Verify in database:
   - 1 discovery_session record
   - ~120 search_result records
   - ~50-80 domain records
   - ~20-40 funding_source_candidate records
7. View candidates in Admin Dashboard @ http://localhost:5173
8. Approve/reject candidates to verify workflow
**Success Criteria**: All criteria from quickstart.md SUCCESS CRITERIA section met

## Dependencies

**Phase 3.1 (Setup)** blocks everything:
- T001, T002, T003 must complete before T004-T026

**Phase 3.2 (Tests)** blocks Phase 3.3 (Implementation):
- T004 blocks T005-T008 (base class first)
- T005-T008 can run in parallel [P]
- T009 blocks nothing (SearchWorkflowService test)
- ALL of Phase 3.2 must FAIL before starting Phase 3.3

**Phase 3.3 (Implementation)**:
- T010 (SearchAdapter interface) blocks T012-T015 (adapter implementations)
- T011 (SearchAdapterException) blocks T012-T015
- T012-T015 can run in parallel [P] (different adapters, independent files)
- T016 (DayOfWeekCategories) blocks T018 (SearchWorkflowService)
- T017 (request/result models) blocks T018
- T018 (SearchWorkflowService) depends on T012-T015 (adapters must exist)

**Phase 3.4 (Integration)**:
- T019 (Spring config) depends on T012-T015 (adapters), T018 (workflow service)
- T020 (application.yml) can be done anytime
- T021 (adapter unit tests) depends on T012-T015
- T022 (workflow unit tests) depends on T018

**Phase 3.5 (Integration Tests)**:
- T023 (end-to-end test) depends on ALL implementation (T010-T022)
- T024 (effectiveness tracking) depends on T023

**Phase 3.6 (Polish)**:
- T025 (documentation) can be done anytime
- T026 (manual testing) depends on ALL tasks (T001-T025)

**Dependency Graph**:
```
T001, T002, T003 (Setup)
  ↓
T004 (Contract base class)
  ↓
T005, T006, T007, T008, T009 [P] (Contract tests - MUST FAIL)
  ↓
T010, T011 (Interface, Exception)
  ↓
T012, T013, T014, T015 [P] (Adapter implementations)
  ↓ (with T016, T017)
T018 (SearchWorkflowService)
  ↓
T019, T020 (Configuration)
  ↓
T021, T022 [P] (Unit tests)
  ↓
T023, T024 (Integration tests)
  ↓
T025 [P] (Documentation)
  ↓
T026 (Manual testing)
```

## Parallel Execution Examples

### Phase 3.2: Launch all contract tests together
```bash
# After T004 completes, launch T005-T009 in parallel
Task: "Contract test for BraveSearchAdapter in northstar-search-adapters/src/test/java/.../contract/BraveAdapterContractTest.java"
Task: "Contract test for SerperAdapter in northstar-search-adapters/src/test/java/.../contract/SerperAdapterContractTest.java"
Task: "Contract test for SearXNGAdapter in northstar-search-adapters/src/test/java/.../contract/SearXNGAdapterContractTest.java"
Task: "Contract test for TavilyAdapter in northstar-search-adapters/src/test/java/.../contract/TavilyAdapterContractTest.java"
Task: "Contract test for SearchWorkflowService in northstar-search-adapters/src/test/java/.../workflow/SearchWorkflowServiceContractTest.java"
```

### Phase 3.3: Launch all adapter implementations together
```bash
# After T010, T011 complete, launch T012-T015 in parallel
Task: "Implement BraveSearchAdapter in northstar-search-adapters/src/main/java/.../brave/BraveSearchAdapter.java"
Task: "Implement SerperAdapter in northstar-search-adapters/src/main/java/.../serper/SerperAdapter.java"
Task: "Implement SearXNGAdapter in northstar-search-adapters/src/main/java/.../searxng/SearXNGAdapter.java"
Task: "Implement TavilyAdapter in northstar-search-adapters/src/main/java/.../tavily/TavilyAdapter.java"
```

### Phase 3.4: Launch unit tests together
```bash
# After T012-T015 complete, launch T021 unit tests in parallel
Task: "Unit test BraveAdapter in northstar-search-adapters/src/test/java/.../unit/BraveAdapterTest.java"
Task: "Unit test SerperAdapter in northstar-search-adapters/src/test/java/.../unit/SerperAdapterTest.java"
Task: "Unit test SearXNGAdapter in northstar-search-adapters/src/test/java/.../unit/SearXNGAdapterTest.java"
Task: "Unit test TavilyAdapter in northstar-search-adapters/src/test/java/.../unit/TavilyAdapterTest.java"
```

## Notes

**TDD Compliance**:
- Phase 3.2 tests MUST be written and MUST FAIL before Phase 3.3 implementation
- Contract tests define expectations for all adapters
- Zero results is NOT an error - return empty list

**BigDecimal Requirement** (Constitutional Principle XII):
- ALL confidence scores use `BigDecimal` with scale 2
- NEVER use `Double` or `double`
- Comparisons use `.compareTo()` not `==`
- Already enforced in existing ConfidenceScorer (northstar-crawler)

**Parallel Execution**:
- [P] tasks = different files, no shared state
- Adapter implementations are completely independent
- Contract tests are independent
- Unit tests are independent

**Zero-Result Tracking** (Critical Requirement):
- Zero results is a valid outcome (NOT an error)
- Track in search_session_statistics with zero_result=true
- Used for adapter effectiveness analysis (not alerts)

**Adapter Distribution** (Constitutional Compliance):
- All 4 adapters used EVERY night (requirement from spec)
- Round-robin distribution ensures fairness
- Monday-Sunday schedule (30 categories / 7 days)

## Task Validation Checklist

- [x] All contracts have corresponding tests (T004-T009)
- [x] All adapters have implementation tasks (T012-T015)
- [x] All tests come before implementation (Phase 3.2 → 3.3)
- [x] Parallel tasks truly independent (different adapters, different files)
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] Dependencies clearly documented
- [x] TDD workflow enforced (tests MUST FAIL first)

## Success Criteria (from quickstart.md)

After T026 manual testing, verify:
- ✅ Workflow completes successfully (no exceptions)
- ✅ SearchWorkflowResult returned with statistics
- ✅ Execution time < 5 minutes for test scenario
- ✅ 1 discovery_session record created
- ✅ ~120 search_result records (2 engines × 6 queries × 10 results)
- ✅ ~50-80 domain records (unique domains)
- ✅ ~20-40 funding_source_candidate records (>= 0.6 confidence)
- ✅ >= 80% of candidates have confidence >= 0.70
- ✅ < 5% duplicate domains
- ✅ 0% blacklisted domains in candidates
- ✅ Zero results tracked (not treated as errors)
- ✅ Candidates visible in Admin Dashboard @ http://localhost:5173
- ✅ Approve/reject actions persist to database

---

**Total Tasks**: 26
**Estimated Effort**: 5-7 days (with TDD, testing, integration)
**Parallel Opportunities**: 13 tasks marked [P]
**Critical Path**: Setup → Contract Tests → Interface → Adapters → Workflow → Integration Tests
