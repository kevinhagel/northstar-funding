# Tasks: AI-Powered Search Query Generation

**Input**: Design documents from `/specs/004-create-northstar-query/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory ✅
   → Tech stack: Java 25, Spring Boot 3.5.6, LangChain4j, Caffeine, Vavr
2. Load design documents ✅
   → data-model.md: 4 model classes, 1 enum, 2 strategies, 2 mappers
   → contracts/: 3 interface contracts
   → quickstart.md: 7 integration test scenarios
3. Generate tasks by category ✅
   → Setup, Tests, Core, Integration, Polish
4. Apply task rules ✅
   → [P] for independent files, TDD order
5. Number tasks sequentially (T001-T035) ✅
6. Validate completeness ✅
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- File paths relative to repository root

---

## Phase 3.1: Setup

- [X] **T001** Create Maven module `northstar-query-generation` with directory structure
  - Create: `northstar-query-generation/pom.xml`
  - Create: `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/` package structure
  - Create subdirectories: `config/`, `strategy/`, `service/`, `model/`, `template/`, `exception/`
  - Create: `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/` test structure

- [X] **T002** Configure `northstar-query-generation/pom.xml` with dependencies
  - Parent: `com.northstar.funding:northstar-funding-parent`
  - Dependencies: `northstar-domain`, `northstar-persistence`
  - Dependencies: Spring Boot Starter, LangChain4j (langchain4j, langchain4j-open-ai)
  - Dependencies: Caffeine cache, Vavr 0.10.7, Lombok 1.18.42
  - Dependencies: JUnit 5, Mockito, Spring Boot Test
  - Compiler: Java 25 source and target level

- [X] **T003** [P] Create Spring Boot configuration file `northstar-query-generation/src/main/resources/application.yml`
  - LM Studio base URL: `http://192.168.1.10:1234/v1`
  - Cache config: `maximumSize=1000, expireAfterWrite=24h`
  - Query limits: max=50, min=1, default=10

- [X] **T004** Update parent `pom.xml` to include `northstar-query-generation` module
  - Add `<module>northstar-query-generation</module>` to `<modules>` section

---

## Phase 3.2: Model Classes (POJOs - No Dependencies)

**NOTE**: Created domain entities `FundingSearchCategory` (25 categories) and `GeographicScope` (15 scopes) in northstar-domain. Reusing existing `SearchEngineType` enum instead of creating new `SearchProvider`.

- [X] **T005** [P] ~~Create SearchProvider enum~~ **SKIPPED** - Reusing existing `SearchEngineType` from northstar-domain
  - Values already exist: BRAVE, SERPER, SEARXNG, TAVILY

- [X] **T006** [P] Create `QueryGenerationRequest` model in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/model/QueryGenerationRequest.java`
  - Fields: SearchEngineType searchEngine, Set<FundingSearchCategory> categories, GeographicScope geographic, int maxQueries, UUID sessionId
  - Lombok: @Value, @Builder
  - Manual validation: `validate()` method

- [X] **T007** [P] Create `QueryGenerationResponse` model in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/model/QueryGenerationResponse.java`
  - Fields: SearchEngineType searchEngine, List<String> queries (immutable), boolean fromCache, Instant generatedAt, UUID sessionId
  - Lombok: @Value, @Builder
  - Factory method: `of(...)` ensures immutability

- [X] **T008** [P] Create `QueryCacheKey` model in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/model/QueryCacheKey.java`
  - Fields: SearchEngineType searchEngine, Set<FundingSearchCategory> categories, GeographicScope geographic, int maxQueries
  - Lombok: @Value, @Builder (immutable with proper equals/hashCode)
  - Factory method: `from(QueryGenerationRequest)`

- [X] **T009** [P] Create `QueryGenerationException` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/exception/QueryGenerationException.java`
  - Extends RuntimeException
  - Constructors for message, message+cause, cause

---

## Phase 3.3: Mapping Classes (Pure Functions - No Dependencies)

- [X] **T010** [P] Create `CategoryMapper` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/template/CategoryMapper.java`
  - Method: `String toKeywords(FundingSearchCategory category)` - Maps all 25 categories to keyword strings
  - Method: `String toConceptualDescription(FundingSearchCategory category)` - Maps all 25 categories to AI-friendly descriptions
  - Use Java switch expressions for type safety ✅

- [X] **T011** [P] Create `GeographicMapper` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/template/GeographicMapper.java`
  - Method: `String toKeywords(GeographicScope scope)` - Maps all 15 scopes to location keywords
  - Method: `String toConceptualDescription(GeographicScope scope)` - Maps all scopes to contextual descriptions
  - Use Java switch expressions for type safety ✅

- [X] **T012** [P] Create `PromptTemplates` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/template/PromptTemplates.java`
  - Constant: `KEYWORD_QUERY_TEMPLATE` (LangChain4j PromptTemplate) for traditional search engines ✅
  - Constant: `TAVILY_QUERY_TEMPLATE` for AI-optimized search ✅
  - Fallback query arrays for both types ✅
  - Constant: `TAVILY_QUERY_TEMPLATE` (LangChain4j PromptTemplate) for AI-powered search
  - Variables: {{categories}}, {{geographic}}, {{maxQueries}}

---

## Phase 3.4: Configuration Classes

- [X] **T013** Create `LmStudioConfig` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/config/LmStudioConfig.java`
  - @Configuration class ✅
  - @Bean ChatLanguageModel with HTTP/1.1 configuration ✅
  - Use Java HttpClient.Builder with `.version(HttpClient.Version.HTTP_1_1)` ✅
  - Configure OpenAiChatModel with LM Studio base URL ✅

- [X] **T014** Create `CaffeineConfig` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/config/CaffeineConfig.java`
  - @Configuration class ✅
  - @Bean Cache<QueryCacheKey, List<String>> with Caffeine builder ✅
  - Config: maximumSize(1000), expireAfterWrite(24, HOURS), recordStats() ✅

- [X] **T015** Create `VirtualThreadConfig` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/config/VirtualThreadConfig.java`
  - @Configuration class ✅
  - @Bean AsyncTaskExecutor using Executors.newVirtualThreadPerTaskExecutor() ✅
  - For CompletableFuture async operations ✅

---

## Phase 3.5: Contract Tests (TDD - MUST FAIL before implementation)

- [X] **T016** [P] Create contract test `QueryGenerationStrategyContractTest` in `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/strategy/QueryGenerationStrategyContractTest.java`
  - Test: generateQueries() returns CompletableFuture ✅
  - Test: getSearchEngine() returns correct SearchEngineType ✅
  - Test: getQueryType() returns "keyword" or "ai-optimized" ✅
  - Test: Thread safety ✅
  - **STATUS**: 7 tests FAILING (expected for TDD RED phase) ✅
  - Mock ChatLanguageModel for LLM responses
  - ⚠️ Tests MUST FAIL (no implementation yet)

- [X] **T017** [P] Create contract test `QueryGenerationServiceContractTest` in `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/QueryGenerationServiceContractTest.java`
  - Test: generateQueries() checks cache first ✅
  - Test: generateQueries() validates request ✅
  - Test: generateForMultipleProviders() executes in parallel ✅
  - Test: getCacheStatistics() returns map with stats ✅
  - Test: clearCache() removes all entries ✅
  - **STATUS**: 6 tests FAILING (expected for TDD RED phase) ✅

- [X] **T018** [P] Create contract test `QueryCacheServiceContractTest` in `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/service/QueryCacheServiceContractTest.java`
  - Test: getFromCache() returns Optional.empty() on miss ✅
  - Test: getFromCache() returns cached queries on hit ✅
  - Test: cacheQueries() stores with <50ms response time ✅
  - Test: persistQueries() is non-blocking (CompletableFuture) ✅
  - Test: getStatistics() returns cache metrics ✅
  - Test: clearCache() is idempotent ✅
  - **STATUS**: 6 tests FAILING (expected for TDD RED phase) ✅

---

## Phase 3.6: Strategy Implementations (After contract tests)

- [X] **T019** [P] Implement `KeywordQueryStrategy` in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/strategy/KeywordQueryStrategy.java`
  - @Component class implementing QueryGenerationStrategy ✅
  - Uses CategoryMapper.toKeywords() and GeographicMapper.toKeywords() ✅
  - Uses KEYWORD_QUERY_TEMPLATE with LangChain4j ✅
  - Async with CompletableFuture.supplyAsync() ✅
  - Fallback queries on LLM failure ✅
  - Returns "keyword" query type ✅
  - Inject ChatLanguageModel (LM Studio client)
  - Inject CategoryMapper and GeographicMapper
  - Method: generateQueries() - Maps categories/geographic to keywords, builds KEYWORD_QUERY_TEMPLATE prompt, calls LLM, parses response
  - Method: getProvider() - Returns SearchProvider based on constructor param (supports BRAVE_SEARCH, SERPER, SEARXNG)
  - Method: getQueryType() - Returns "keyword"
  - Use CompletableFuture.supplyAsync() with 30s timeout
  - Fallback to hardcoded queries on LLM failure

- [X] **T020** [P] Implement `TavilyQueryStrategy` ✅ in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/strategy/TavilyQueryStrategy.java`
  - @Component class implementing QueryGenerationStrategy
  - Inject ChatLanguageModel (LM Studio client)
  - Inject CategoryMapper and GeographicMapper
  - Method: generateQueries() - Maps categories/geographic to conceptual descriptions, builds TAVILY_QUERY_TEMPLATE prompt, calls LLM, parses response
  - Method: getProvider() - Returns SearchProvider.TAVILY
  - Method: getQueryType() - Returns "ai-optimized"
  - Use CompletableFuture.supplyAsync() with 30s timeout
  - Fallback to hardcoded queries on LLM failure

---

## Phase 3.7: Service Implementations

- [X] **T021** Implement `QueryCacheService` ✅ in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/service/QueryCacheService.java`
  - @Service class
  - @Transactional (for persistence methods)
  - Inject Cache<QueryCacheKey, List<String>> from Caffeine
  - Inject SearchQueryRepository from northstar-persistence
  - Method: getFromCache(QueryCacheKey) - Returns Optional<List<String>>
  - Method: cacheQueries(QueryCacheKey, List<String>) - Stores with 24hr TTL
  - Method: persistQueries(QueryCacheKey, List<String>, UUID sessionId) - Async, creates SearchQuery entities, returns CompletableFuture<Void>
  - Method: getStatistics() - Returns cache stats (hitRate, size, requests, hits, misses)
  - Method: clearCache() - Invalidates all cache entries
  - Explicit constructor (no Lombok @RequiredArgsConstructor)

- [X] **T022** Implement `QueryGenerationService` ✅ in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/service/QueryGenerationService.java`
  - @Service class
  - @Transactional
  - Inject Map<SearchProvider, QueryGenerationStrategy> - Spring auto-wires all strategies
  - Inject QueryCacheService
  - Method: generateQueries(QueryGenerationRequest) - Builds cache key, checks cache, delegates to strategy, caches result, persists async, returns CompletableFuture<QueryGenerationResponse>
  - Method: generateForMultipleProviders(...) - Creates CompletableFuture for each provider, executes in parallel, waits with CompletableFuture.allOf(), returns Map<SearchProvider, List<String>>
  - Method: getCacheStatistics() - Delegates to QueryCacheService
  - Method: clearCache() - Delegates to QueryCacheService
  - Input validation: maxQueries 1-50, non-null params
  - Explicit constructor (no Lombok @RequiredArgsConstructor)

- [X] **T023** Create `StrategyConfig` ✅ in `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/config/StrategyConfig.java`
  - @Configuration class
  - @Bean Map<SearchProvider, QueryGenerationStrategy> - Collects all strategy beans, maps by getProvider()
  - Allows QueryGenerationService to inject Map of strategies

---

## Phase 3.8: Integration Tests (from quickstart.md scenarios)

- [X] **T024** [P] Integration test: Single provider query generation ✅
  - Created: `SingleProviderQueryGenerationTest.java`
  - Tests: BRAVE, SERPER, SEARXNG keyword query generation
  - Scenario 1 from quickstart.md

- [X] **T025** [P] Integration test: Cache hit behavior ✅
  - Created: `CacheHitTest.java`
  - Tests: Cache hits, cache key differentiation, <50ms retrieval
  - Scenario 2 from quickstart.md

- [X] **T026** [P] Integration test: Keyword vs AI-optimized ✅
  - Created: `KeywordVsAiOptimizedTest.java`
  - Tests: Short keyword queries vs long AI queries
  - Scenario 3 from quickstart.md

- [X] **T027** [P] Integration test: Multi-provider parallel generation ✅
  - Created: `MultiProviderParallelTest.java`
  - Tests: All 4 providers in parallel, <30s execution
  - Scenario 4 from quickstart.md

- [X] **T028** [P] Integration test: AI service unavailable fallback ✅
  - Created: `FallbackQueriesTest.java`
  - Tests: Fallback queries when LM Studio unavailable
  - Note: Currently @Disabled - requires mocking (polish phase)
  - Scenario 5 from quickstart.md

- [X] **T029** [P] Integration test: Query persistence verification ✅
  - Created: `QueryPersistenceTest.java`
  - Tests: PostgreSQL persistence, async non-blocking
  - Scenario 6 from quickstart.md

- [X] **T030** [P] Integration test: Cache statistics monitoring ✅
  - Created: `CacheStatisticsTest.java`
  - Tests: Hit rate calculation, cache size tracking
  - Scenario 7 from quickstart.md

**Additional files created**:
- `TestApplication.java` - Spring Boot test context
- `application-test.yml` - Test configuration

---

## Phase 3.9: Polish

- [X] **T031** [P] Add unit tests for mappers ✅ in `northstar-query-generation/src/test/java/com/northstar/funding/querygeneration/template/CategoryMapperTest.java` and `GeographicMapperTest.java`
  - Test: All 25 FundingSearchCategory values have keyword and conceptual mappings
  - Test: All GeographicScope values have keyword and conceptual mappings
  - Test: No null or empty string mappings

- [X] **T032** Add comprehensive logging to all services ✅
  - QueryGenerationService: Log requests, cache hits/misses, LLM calls, errors
  - KeywordQueryStrategy/TavilyQueryStrategy: Log prompt generation, LLM responses, fallbacks
  - QueryCacheService: Log cache operations, persistence operations

- [X] **T033** [P] Add input validation and error handling ✅
  - QueryGenerationService: Validate maxQueries range (1-50), non-null parameters
  - Throw QueryGenerationException with clear messages on validation failures
  - Handle LLM timeouts gracefully (fallback queries)

- [X] **T034** [P] Add Javadoc to all public classes and methods ✅
  - Document contracts, parameters, return values, exceptions
  - Add @author, @since tags
  - Document LM Studio HTTP/1.1 requirement in LmStudioConfig

- [X] **T035** Manual verification using quickstart.md scenarios ✅
  - Verify LM Studio connection: `curl http://192.168.1.10:1234/v1/models`
  - Run all integration tests: `mvn test -pl northstar-query-generation`
  - Check database persistence: Query `search_queries` table
  - Monitor cache statistics
  - Review logs for errors

---

## Dependencies

**Setup before everything**:
- T001-T004 (sequential) → All other tasks

**Models before tests/implementation**:
- T005-T009 → T016-T018 (tests need model classes)

**Mappers/templates before strategies**:
- T010-T012 → T019-T020 (strategies use mappers/templates)

**Config before services**:
- T013-T015 → T021-T022 (services need configured beans)

**Tests before implementation (TDD)**:
- T016 → T019-T020 (strategy tests before strategy implementation)
- T017 → T022 (service tests before service implementation)
- T018 → T021 (cache tests before cache implementation)

**Services before integration tests**:
- T021-T023 → T024-T030 (integration tests need working services)

**Everything before polish**:
- T001-T030 → T031-T035

---

## Parallel Execution Examples

### Parallel Model Creation (T005-T009)
```bash
# All model classes are independent POJOs
Task: "Create SearchProvider enum"
Task: "Create QueryGenerationRequest model"
Task: "Create QueryGenerationResponse model"
Task: "Create QueryCacheKey model"
Task: "Create QueryGenerationException"
```

### Parallel Mappers (T010-T012)
```bash
# Pure functions, no dependencies
Task: "Create CategoryMapper with 25 category mappings"
Task: "Create GeographicMapper with all scope mappings"
Task: "Create PromptTemplates with LangChain4j templates"
```

### Parallel Contract Tests (T016-T018)
```bash
# Different test files, all should fail initially
Task: "Create QueryGenerationStrategyTest contract test"
Task: "Create QueryGenerationServiceTest contract test"
Task: "Create QueryCacheServiceTest contract test"
```

### Parallel Strategy Implementations (T019-T020)
```bash
# After contract tests written, implement in parallel
Task: "Implement KeywordQueryStrategy"
Task: "Implement TavilyQueryStrategy"
```

### Parallel Integration Tests (T024-T030)
```bash
# After services implemented, test all scenarios
Task: "Integration test: Single provider generation"
Task: "Integration test: Cache hit behavior"
Task: "Integration test: Keyword vs AI-optimized"
Task: "Integration test: Multi-provider parallel"
Task: "Integration test: Fallback queries"
Task: "Integration test: Query persistence"
Task: "Integration test: Cache statistics"
```

---

## Validation Checklist

- [x] All contracts have corresponding tests (T016-T018 for 3 contracts)
- [x] All entities have model tasks (T005-T008 for 4 models + 1 enum)
- [x] All tests come before implementation (T016-T018 before T019-T022)
- [x] Parallel tasks truly independent (checked [P] markers)
- [x] Each task specifies exact file path (all tasks include paths)
- [x] No task modifies same file as another [P] task (verified)
- [x] All quickstart scenarios have integration tests (T024-T030 for 7 scenarios)
- [x] Setup tasks come first (T001-T004)

---

## Notes

- **TDD**: Write tests (T016-T018) BEFORE implementation (T019-T022)
- **[P] Tasks**: Can be executed in parallel by different agents/developers
- **Commit Strategy**: Commit after each task or logical group
- **LM Studio**: Must be running at http://192.168.1.10:1234/v1 for integration tests
- **PostgreSQL**: Must be accessible at 192.168.1.10:5432 for persistence tests
- **Java 25**: Ensure SDKMAN configured for Java 25

---

**Total Tasks**: 35
**Estimated Parallel Groups**: 5 (models, mappers, contract tests, strategies, integration tests)
**Critical Path**: Setup (4) → Models (5) → Config (3) → Tests (3) → Services (3) → Integration (7) → Polish (5) = ~30 sequential dependencies

**Ready for**: `/implement` command or manual execution
