# Tasks: Search Provider Adapters

**Input**: Design documents from `/specs/003-design-and-implement/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

---

## Overview

This tasks.md implements **47 tasks** for the Search Provider Adapters feature (003-design-and-implement). Tasks follow TDD principles: tests before implementation, with explicit parallel execution markers [P] for independent tasks.

**Tech Stack** (from plan.md):
- Java 25 (Oracle JDK via SDKMAN)
- Spring Boot 3.5.6
- Apache Commons Text (fuzzy matching)
- Vavr 0.10.6 (Try monad)
- Resilience4j (retry, rate limiting)
- java.net.http.HttpClient (NO WebFlux)
- PostgreSQL 16 (Mac Studio @ 192.168.1.10:5432)
- TestContainers (integration tests)

**Project Structure**: Multi-module Maven monolith
- `northstar-domain/` - Existing entities (SearchResult, Domain, DiscoverySession)
- `northstar-persistence/` - Existing services (SearchResultService, DomainService)
- `northstar-crawler/` - **THIS FEATURE** (search provider adapters)

---

## Phase 3.1: Setup & Configuration

### T001: Create northstar-crawler module structure
**File**: `northstar-crawler/pom.xml` (new file)

Create Maven module with dependencies:
- Spring Boot 3.5.6 starter
- Apache Commons Text (already in parent POM)
- Vavr 0.10.6 (already in parent POM)
- Resilience4j (spring-boot-starter-resilience4j)
- northstar-domain (module dependency)
- northstar-persistence (module dependency)
- JUnit 5/Jupiter (test scope)
- TestContainers PostgreSQL (test scope)

Package structure:
```
northstar-crawler/
├── pom.xml
├── src/main/java/com/northstar/funding/crawler/
│   ├── adapter/
│   ├── antispam/
│   ├── orchestrator/
│   ├── config/
│   ├── tracking/
│   └── exception/
└── src/test/java/com/northstar/funding/crawler/
    ├── contract/
    ├── integration/
    └── unit/
```

**Validation**: `mvn clean compile` succeeds for northstar-crawler module

---

### T002: Create SearchProviderConfig with @ConfigurationProperties
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/config/SearchProviderConfig.java` (new)

Implement configuration class from research.md:
- `@ConfigurationProperties(prefix = "search.providers")`
- Inner classes: BraveSearchConfig, SerperConfig, TavilyConfig, SearxngConfig
- Fields: apiKey, baseUrl, timeout, maxResults, rateLimit (daily)
- Default values from research.md (timeouts: 5-7s, rate limits: 25-60/day)

**Validation**: Configuration loads from application.properties without errors

---

### T003: Configure Resilience4j for retry, rate limiting, timeout
**File**: `northstar-crawler/src/main/resources/application.properties` (new)

Add Resilience4j configuration from research.md:
- Retry: 3 max attempts, exponential backoff (500ms × 2)
- Rate limiter: per-provider daily limits (BraveSearch=50, Serper=60, Tavily=25, SearXNG=unlimited)
- Timeout: per-provider timeouts (5-7 seconds)
- Circuit breaker: optional for v1 (disabled by default)

**Validation**: Spring Boot loads Resilience4j beans without errors

---

### T004: Create Virtual Thread executor bean configuration
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/config/VirtualThreadConfig.java` (new)

Create @Configuration class:
```java
@Bean("virtual-thread-executor")
public Executor virtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}
```

**Validation**: Virtual Thread executor bean available in application context

---

### T005: Create custom exceptions (RateLimitException, AuthenticationException)
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/exception/` (3 files)

Create exception classes:
- `RateLimitException extends RuntimeException` - HTTP 429 or quota exceeded
- `AuthenticationException extends RuntimeException` - HTTP 401/403 or invalid API key
- `ProviderTimeoutException extends RuntimeException` - Request timeout

**Validation**: Exceptions compile and can be thrown/caught in tests

---

## Phase 3.2: Adapter Interface & Base Classes

### T006: Implement SearchProviderAdapter interface
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/adapter/SearchProviderAdapter.java` (new)

Copy interface contract from `contracts/SearchProviderAdapter.java`:
- `Try<List<SearchResult>> executeSearch(String query, int maxResults, UUID discoverySessionId)`
- `SearchEngineType getProviderType()`
- `boolean supportsKeywordQueries()`
- `boolean supportsAIOptimizedQueries()`
- `int getCurrentUsageCount()`
- `int getRateLimit()`

**Validation**: Interface compiles, no implementation yet

---

### T007: Create AbstractSearchProviderAdapter base class
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/adapter/AbstractSearchProviderAdapter.java` (new)

Abstract class implementing common logic:
- HTTP client initialization (java.net.http.HttpClient with timeout)
- Domain normalization (lowercase, remove www, remove protocol)
- Rate limit tracking (in-memory counter, reset daily)
- Common error handling (convert HTTP errors to custom exceptions)
- SearchResult entity population (discoveredAt, position, searchEngine)

**Validation**: Abstract class compiles, provides protected helper methods

---

### T008 [P]: Create ProviderApiUsage entity for API usage tracking
**File**: `northstar-domain/src/main/java/com/northstar/funding/domain/ProviderApiUsage.java` (new)

Lombok entity:
```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table(name = "provider_api_usage")
public class ProviderApiUsage {
    @Id private Long id;
    private String provider;          // SearchEngineType enum name
    private String query;
    private Integer resultCount;
    private Boolean success;
    private String errorType;         // NULL if success, TIMEOUT/RATE_LIMIT/etc if failure
    private LocalDateTime executedAt;
    private Integer responseTimeMs;
}
```

**Validation**: Entity compiles in northstar-domain module

---

## Phase 3.3: Database Migration for API Usage Tracking

### T009: Create Flyway migration for provider_api_usage table
**File**: `northstar-persistence/src/main/resources/db/migration/V18__create_provider_api_usage.sql` (new)

SQL migration from research.md:
```sql
CREATE TABLE provider_api_usage (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    query TEXT NOT NULL,
    result_count INT NOT NULL,
    success BOOLEAN NOT NULL,
    error_type VARCHAR(100),
    executed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    response_time_ms INT NOT NULL
);

CREATE INDEX idx_provider_date ON provider_api_usage(provider, executed_at);
CREATE INDEX idx_success_date ON provider_api_usage(success, executed_at);
```

**Validation**: `mvn flyway:migrate -pl northstar-persistence` applies migration successfully

---

## Phase 3.4: Contract Tests (TDD - Tests FIRST)

**CRITICAL**: These tests MUST be written and MUST FAIL before implementing adapters

### T010 [P]: Contract test for SearchProviderAdapter interface
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/contract/SearchProviderAdapterContractTest.java` (new)

Test SearchProviderAdapter contract using test doubles:
- `executeSearch()` returns Try<List<SearchResult>> (not null)
- Empty results return `Try.success(List.of())` NOT null
- All SearchResult entities have required fields populated
- Domain extraction and normalization correct
- Rate limit enforcement works (RateLimitException on quota exceeded)
- Authentication errors throw AuthenticationException

**Validation**: Test compiles and FAILS (no implementation yet)

---

### T011 [P]: Contract test for AntiSpamFilter interface
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/contract/AntiSpamFilterContractTest.java` (new)

Test AntiSpamFilter contract:
- `analyzeForSpam()` returns non-null SpamAnalysisResult
- Keyword stuffing detected when unique ratio < 0.5
- Domain-metadata mismatch detected when similarity < 0.15
- Unnatural keyword list detected when < 2 common words
- Cross-category spam detected (gambling domain + education keywords)
- Execution completes in < 5ms (fast filtering)

**Validation**: Test compiles and FAILS (no implementation yet)

---

### T012 [P]: Contract test for MultiProviderSearchOrchestrator interface
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/contract/MultiProviderSearchOrchestratorContractTest.java` (new)

Test orchestrator contract:
- `executeMultiProviderSearch()` returns Try<SearchExecutionResult>
- Partial success returns Success with partial results + errors
- Complete failure returns Failure only if all providers fail
- Parallel execution completes in < 10 seconds
- Anti-spam filtering applied before deduplication
- DiscoverySession statistics updated correctly

**Validation**: Test compiles and FAILS (no implementation yet)

---

## Phase 3.5: Search Provider Implementations

**PREREQUISITE**: T010-T012 contract tests MUST be failing before starting implementations

### T013 [P]: Implement BraveSearchAdapter
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/adapter/BraveSearchAdapter.java` (new)

Extend AbstractSearchProviderAdapter:
- Implement `executeSearch()` using java.net.http.HttpClient
- API endpoint: `https://api.search.brave.com/res/v1/web/search`
- HTTP headers: `X-Subscription-Token: <apiKey>`
- Query parameter: `q=<query>`
- Parse JSON response (Brave Search API format)
- Map to SearchResult entities with domain normalization
- Apply Resilience4j @Retry and @RateLimiter annotations
- Return `SearchEngineType.BRAVE_SEARCH`
- Support keyword queries only (supportsKeywordQueries=true)

**Validation**: T010 contract test passes, adapter returns valid SearchResult entities

---

### T014 [P]: Implement SearxngAdapter
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/adapter/SearxngAdapter.java` (new)

Extend AbstractSearchProviderAdapter:
- Implement `executeSearch()` using java.net.http.HttpClient
- API endpoint: `http://192.168.1.10:8080/search`
- Query parameter: `q=<query>&format=json`
- NO authentication (self-hosted, trusted network)
- Parse JSON response (SearXNG format)
- Map to SearchResult entities
- NO rate limiting (self-hosted, unlimited)
- Return `SearchEngineType.SEARXNG`
- Support keyword queries only

**Validation**: T010 contract test passes, SearXNG adapter works without authentication

---

### T015 [P]: Implement SerperAdapter
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/adapter/SerperAdapter.java` (new)

Extend AbstractSearchProviderAdapter:
- Implement `executeSearch()` using java.net.http.HttpClient
- API endpoint: `https://google.serper.dev/search`
- HTTP header: `X-API-KEY: <apiKey>`
- Request body: `{"q": "<query>"}`
- Parse JSON response (Serper API format)
- Map to SearchResult entities
- Apply Resilience4j @Retry and @RateLimiter
- Return `SearchEngineType.SERPER`
- Support keyword queries only

**Validation**: T010 contract test passes, Serper adapter returns Google search results

---

### T016 [P]: Implement TavilyAdapter
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/adapter/TavilyAdapter.java` (new)

Extend AbstractSearchProviderAdapter:
- Implement `executeSearch()` using java.net.http.HttpClient
- API endpoint: `https://api.tavily.com/search`
- HTTP header: `Authorization: Bearer <apiKey>`
- Request body: `{"query": "<query>", "search_depth": "advanced"}`
- Parse JSON response (Tavily API format)
- Map to SearchResult entities
- Apply Resilience4j @Retry and @RateLimiter
- Return `SearchEngineType.TAVILY`
- Support AI-optimized queries only (supportsAIOptimizedQueries=true)

**Validation**: T010 contract test passes, Tavily adapter processes AI-optimized queries

---

## Phase 3.6: Anti-Spam Filtering Implementation

### T017: Implement AntiSpamFilter interface
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/antispam/AntiSpamFilterImpl.java` (new)

Implement @Service class:
- Inject Apache Commons Text (Cosine similarity)
- Implement `analyzeForSpam()` - runs all 4 detection strategies
- Combine results into SpamAnalysisResult
- Return isSpam=true if ANY strategy detects spam
- Set primaryIndicator to first detection that triggered

**Validation**: T011 contract test passes

---

### T018 [P]: Implement keyword stuffing detection
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/antispam/KeywordStuffingDetector.java` (new)

@Component class:
- `boolean detect(String text)`
- Calculate unique word ratio: `uniqueWords.size() / totalWords.size()`
- Return true if ratio < 0.5
- Handle null/empty text gracefully

**Validation**: Unit test passes with example from quickstart.md (Scenario 3)

---

### T019 [P]: Implement domain-metadata mismatch detection
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/antispam/DomainMetadataMismatchDetector.java` (new)

@Component class:
- `boolean detect(String domain, String title, String description)`
- Extract keywords from domain (e.g., "casinowinners.com" → "casino winners")
- Calculate cosine similarity between domain keywords and title+description
- Use Apache Commons Text Cosine Similarity
- Return true if similarity < 0.15

**Validation**: Unit test passes with scammer example (Scenario 3)

---

### T020 [P]: Implement unnatural keyword list detection
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/antispam/UnnaturalKeywordListDetector.java` (new)

@Component class:
- `boolean detect(String text)`
- Count common words: ["the", "a", "an", "of", "for", "to", "in", "with"]
- Return true if < 2 common words found
- Handle null/empty text gracefully

**Validation**: Unit test detects "grants scholarships funding aid" as unnatural

---

### T021 [P]: Implement cross-category spam detection
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/antispam/CrossCategorySpamDetector.java` (new)

@Component class:
- `boolean detect(String domain, String title, String description)`
- Gambling keywords: ["casino", "poker", "betting", "win", "lottery"]
- Essay mill keywords: ["essay", "paper", "dissertation", "thesis"]
- Education keywords: ["scholarship", "grant", "funding", "education"]
- Return true if domain has scammer keywords AND metadata has education keywords
- Case-insensitive matching

**Validation**: Unit test detects "casinowinners.com" + "scholarship" as cross-category spam

---

## Phase 3.7: Multi-Provider Orchestration

**PREREQUISITE**: T013-T016 (adapters) and T017-T021 (anti-spam) must be complete

### T022: Implement MultiProviderSearchOrchestrator interface
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/orchestrator/MultiProviderSearchOrchestratorImpl.java` (new)

@Service class implementing orchestrator:
- Inject all 4 adapters (BraveSearch, SearXNG, Serper, Tavily)
- Inject AntiSpamFilter
- Inject SearchResultService, DomainService, DiscoverySessionService
- Inject Virtual Thread executor (`@Qualifier("virtual-thread-executor")`)
- Implement `executeMultiProviderSearch()` contract

**Validation**: T012 contract test passes

---

### T023: Implement Virtual Thread parallel execution logic
**File**: Same as T022 (MultiProviderSearchOrchestratorImpl.java)

Add method:
```java
public Try<SearchExecutionResult> executeMultiProviderSearch(...) {
    List<CompletableFuture<ProviderSearchResult>> futures = List.of(
        executeProviderAsync(braveAdapter, keywordQuery, ...),
        executeProviderAsync(searxngAdapter, keywordQuery, ...),
        executeProviderAsync(serperAdapter, keywordQuery, ...),
        executeProviderAsync(tavilyAdapter, aiOptimizedQuery, ...)
    );

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .orTimeout(10, TimeUnit.SECONDS)
        .thenApply(v -> aggregateResults(futures))
        .exceptionally(this::handleTimeout);
}
```

**Validation**: All 4 providers execute concurrently, completes in < 10 seconds

---

### T024: Implement result aggregation with deduplication
**File**: Same as T022 (MultiProviderSearchOrchestratorImpl.java)

Add method:
```java
private List<SearchResult> aggregateResults(Map<SearchEngineType, List<SearchResult>> providerResults) {
    // 1. Apply anti-spam filtering to all results
    // 2. Check blacklist (DomainService.isBlacklisted)
    // 3. Check domain deduplication (DomainService.findByDomainName)
    // 4. Set isDuplicate flag on SearchResult entities
    // 5. Create new Domain entities for new domains
    // 6. Return deduplicated, spam-filtered results
}
```

**Validation**: Duplicate domains marked correctly, spam filtered before deduplication

---

### T025: Implement DiscoverySession statistics update
**File**: Same as T022 (MultiProviderSearchOrchestratorImpl.java)

Add method:
```java
private DiscoverySession updateSessionStatistics(UUID sessionId, SearchExecutionResult result) {
    SessionStatistics stats = result.statistics();
    return discoverySessionService.updateStatistics(sessionId, stats);
}
```

Update DiscoverySession fields:
- totalResultsFound, newDomainsDiscovered, duplicateDomainsSkipped, spamResultsFiltered
- Per-provider counts (braveSearchResults, searxngResults, etc.)
- Set status (COMPLETED, PARTIAL_SUCCESS, or FAILED)
- Set completedAt timestamp

**Validation**: DiscoverySession statistics match actual results

---

## Phase 3.8: API Usage Tracking

### T026 [P]: Create ProviderApiUsageRepository
**File**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/repository/ProviderApiUsageRepository.java` (new)

Spring Data JDBC repository:
```java
public interface ProviderApiUsageRepository extends CrudRepository<ProviderApiUsage, Long> {
    @Query("SELECT COUNT(*) FROM provider_api_usage WHERE provider = :provider AND executed_at >= :since")
    int countUsageSince(String provider, LocalDateTime since);

    @Query("SELECT provider, COUNT(*) as count, AVG(response_time_ms) as avg_response FROM provider_api_usage WHERE executed_at >= :since GROUP BY provider")
    List<ProviderUsageStats> getUsageStatsSince(LocalDateTime since);
}
```

**Validation**: Repository compiles, queries work with TestContainers

---

### T027: Implement ApiUsageTrackingService
**File**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/service/ApiUsageTrackingService.java` (new)

@Service class:
- Inject ProviderApiUsageRepository
- `@Async("virtual-thread-executor") trackUsage(SearchEngineType provider, String query, Try<List<SearchResult>> result, long responseTimeMs)`
- `int getDailyUsage(SearchEngineType provider)` - count since 24h ago
- Save ProviderApiUsage entity asynchronously (don't block search thread)

**Validation**: API usage tracked in database, async execution doesn't block searches

---

### T028: Integrate ApiUsageTrackingService with adapters
**File**: Update T013-T016 adapter implementations

Add to each adapter's `executeSearch()`:
```java
long startTime = System.currentTimeMillis();
Try<List<SearchResult>> result = performSearch(query, maxResults);
long responseTime = System.currentTimeMillis() - startTime;

apiUsageTrackingService.trackUsage(
    getProviderType(),
    query,
    result,
    responseTime
);

return result;
```

**Validation**: All searches logged to provider_api_usage table

---

## Phase 3.9: Unit Tests

**PREREQUISITE**: All implementations (T013-T028) must be complete

### T029 [P]: Unit tests for BraveSearchAdapter
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/BraveSearchAdapterTest.java` (new)

Mockito + WireMock tests:
- Mock HTTP responses from Brave Search API
- Test successful search returns SearchResult entities
- Test HTTP 401 throws AuthenticationException
- Test HTTP 429 throws RateLimitException
- Test timeout throws ProviderTimeoutException
- Test domain normalization (www removal, lowercase)
- Test rate limit enforcement (51st request fails)

**Validation**: >90% code coverage for BraveSearchAdapter

---

### T030 [P]: Unit tests for SearxngAdapter
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/SearxngAdapterTest.java` (new)

Mockito + WireMock tests:
- Mock HTTP responses from SearXNG API
- Test successful search returns SearchResult entities
- Test connection timeout to 192.168.1.10:8080
- Test SearXNG JSON format parsing
- Test no authentication required
- Test no rate limiting (unlimited)

**Validation**: >90% code coverage for SearxngAdapter

---

### T031 [P]: Unit tests for SerperAdapter
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/SerperAdapterTest.java` (new)

Mockito + WireMock tests:
- Mock HTTP responses from Serper API
- Test successful Google search results
- Test HTTP 401 throws AuthenticationException
- Test rate limiting (61st request fails)
- Test Serper JSON format parsing

**Validation**: >90% code coverage for SerperAdapter

---

### T032 [P]: Unit tests for TavilyAdapter
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/TavilyAdapterTest.java` (new)

Mockito + WireMock tests:
- Mock HTTP responses from Tavily API
- Test AI-optimized query processing
- Test HTTP 401 throws AuthenticationException
- Test rate limiting (26th request fails)
- Test Tavily JSON format parsing
- Test search_depth=advanced parameter

**Validation**: >90% code coverage for TavilyAdapter

---

### T033 [P]: Unit tests for keyword stuffing detection
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/KeywordStuffingDetectorTest.java` (new)

Test cases:
- "grants scholarships funding grants education grants" → unique ratio 0.33 → SPAM
- "Education grants for schools in Bulgaria" → unique ratio 1.0 → NOT SPAM
- Null/empty text → NOT SPAM (graceful handling)

**Validation**: All test cases pass

---

### T034 [P]: Unit tests for domain-metadata mismatch detection
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/DomainMetadataMismatchDetectorTest.java` (new)

Test cases:
- domain="casinowinners.com", title="Education Scholarships" → similarity < 0.15 → SPAM
- domain="bulgaria-grants.org", title="Bulgaria Education Funding" → similarity > 0.15 → NOT SPAM
- Null/empty title/description → NOT SPAM

**Validation**: All test cases pass, Apache Commons Text Cosine Similarity works

---

### T035 [P]: Unit tests for unnatural keyword list detection
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/UnnaturalKeywordListDetectorTest.java` (new)

Test cases:
- "grants scholarships funding aid" → 0 common words → SPAM
- "Grants for students in Bulgaria" → 2 common words ("for", "in") → NOT SPAM
- Null/empty text → NOT SPAM

**Validation**: All test cases pass

---

### T036 [P]: Unit tests for cross-category spam detection
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/CrossCategorySpamDetectorTest.java` (new)

Test cases:
- domain="casinowinners.com", title="scholarships" → gambling + education → SPAM
- domain="essay-writers.net", title="grants for students" → essay mill + education → SPAM
- domain="bulgaria-grants.org", title="education funding" → no scammer keywords → NOT SPAM

**Validation**: All test cases pass

---

### T037 [P]: Unit tests for AntiSpamFilterImpl
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/AntiSpamFilterImplTest.java` (new)

Integration tests for anti-spam filter:
- All 4 detection strategies work
- SpamAnalysisResult populated correctly
- Primary indicator set to first detection
- Executes in < 5ms (performance requirement)

**Validation**: All test cases pass, performance requirement met

---

### T038 [P]: Unit tests for MultiProviderSearchOrchestrator
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/MultiProviderSearchOrchestratorTest.java` (new)

Mockito tests:
- Mock all 4 adapters
- Test parallel execution (4 providers run concurrently)
- Test partial success (3/4 providers succeed)
- Test complete failure (all providers fail)
- Test anti-spam filtering applied
- Test domain deduplication
- Test DiscoverySession statistics update

**Validation**: >90% code coverage for orchestrator

---

## Phase 3.10: Integration Tests (TestContainers)

**PREREQUISITE**: All unit tests (T029-T038) must pass

### T039: Integration test - Scenario 1: Single provider search (SearXNG)
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/SingleProviderSearchTest.java` (new)

From quickstart.md Scenario 1:
- @SpringBootTest with @Testcontainers
- Create DiscoverySession
- Execute SearxngAdapter.executeSearch("Bulgaria education infrastructure grants", 20, sessionId)
- Verify 10-20 SearchResult entities returned
- Verify all results have searchEngine=SEARXNG
- Verify all results have normalized domain (lowercase, no www)
- Verify all results have position >= 1

**Validation**: Test passes, SearXNG adapter works end-to-end

---

### T040: Integration test - Scenario 2: Multi-provider parallel search
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/MultiProviderSearchTest.java` (new)

From quickstart.md Scenario 2:
- Create DiscoverySession
- Execute orchestrator.executeMultiProviderSearch(keywordQuery, aiQuery, 20, sessionId)
- Measure execution time (must be < 10 seconds)
- Verify results from all 4 providers
- Verify per-provider counts in DiscoverySession statistics
- Verify no duplicate domains in final result set

**Validation**: Test passes, all providers execute in parallel < 10 seconds

---

### T041: Integration test - Scenario 3: Anti-spam filtering
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/AntiSpamFilteringTest.java` (new)

From quickstart.md Scenario 3:
- Create fake spam results (keyword stuffing, domain-metadata mismatch)
- Apply AntiSpamFilter.analyzeForSpam()
- Verify keyword stuffing detected (unique ratio < 0.5)
- Verify cross-category spam detected (gambling + education)
- Verify both results rejected before domain deduplication

**Validation**: Test passes, anti-spam filter works end-to-end

---

### T042: Integration test - Scenario 4: Domain deduplication
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/DomainDeduplicationTest.java` (new)

From quickstart.md Scenario 4:
- Execute first search, count newDomainsDiscovered
- Execute second search with same query
- Verify second search has fewer newDomainsDiscovered
- Verify second search has duplicateDomainsSkipped > 0
- Verify Domain.totalOccurrences incremented
- Verify Domain.lastSeenAt updated

**Validation**: Test passes, deduplication prevents reprocessing

---

### T043: Integration test - Scenario 5: Partial provider failure resilience
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/PartialFailureTest.java` (new)

From quickstart.md Scenario 5:
- Configure one provider with invalid API key (BraveSearch)
- Execute multi-provider search
- Verify partial success (3/4 providers work)
- Verify results returned from working providers
- Verify error metadata captured for failed provider
- Verify DiscoverySession status = PARTIAL_SUCCESS

**Validation**: Test passes, system resilient to partial failures

---

### T044: Integration test - Scenario 6: Rate limiting
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/RateLimitingTest.java` (new)

From quickstart.md Scenario 6:
- Configure low rate limit (2 requests/day) for testing
- Execute 3 searches in rapid succession
- Verify first 2 succeed
- Verify 3rd fails with RateLimitException
- Verify usage count tracked accurately

**Validation**: Test passes, rate limiting prevents quota overruns

---

### T045: Integration test - Scenario 7: Timeout handling
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/TimeoutHandlingTest.java` (new)

From quickstart.md Scenario 7:
- Mock slow provider (15-second delay, exceeds 10s timeout)
- Execute multi-provider search with slow provider
- Verify total search completes in < 11 seconds
- Verify slow provider cancelled after timeout
- Verify other providers complete normally
- Verify timeout error captured in ProviderError

**Validation**: Test passes, timeouts don't block other providers

---

### T046: Integration test - Scenario 8: End-to-end weekly search simulation
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/WeeklySearchSimulationTest.java` (new)

From quickstart.md Scenario 8:
- Simulate Monday's Bulgaria-focused search (25 categories)
- Execute all 25 queries sequentially
- Verify quota consumption (< daily limits)
- Verify 400-600 total results
- Verify 40-60% spam filtering effectiveness
- Verify 100-200 new domains discovered
- Verify DiscoverySession statistics accurate

**Validation**: Test passes, weekly search schedule feasible within API quotas

---

### T047: Run all quickstart.md scenarios manually
**File**: `specs/003-design-and-implement/quickstart.md`

Execute all 8 scenarios manually:
1. Single provider search (SearXNG)
2. Multi-provider parallel search
3. Anti-spam filtering
4. Domain deduplication
5. Partial provider failure resilience
6. Rate limiting
7. Timeout handling
8. End-to-end weekly search simulation

**Validation**: All scenarios pass, system ready for production

---

## Dependencies

### Phase Dependencies
- **Phase 3.1 (Setup)** blocks all other phases
- **Phase 3.2 (Interface)** blocks Phase 3.5-3.7
- **Phase 3.3 (Migration)** blocks Phase 3.8
- **Phase 3.4 (Contract Tests)** blocks Phase 3.5-3.7 (TDD requirement)
- **Phase 3.5 (Adapters)** blocks Phase 3.7, 3.9, 3.10
- **Phase 3.6 (Anti-Spam)** blocks Phase 3.7, 3.9, 3.10
- **Phase 3.7 (Orchestrator)** blocks Phase 3.9, 3.10
- **Phase 3.8 (API Tracking)** blocks Phase 3.10
- **Phase 3.9 (Unit Tests)** blocks Phase 3.10

### Task Dependencies
```
Setup (T001-T005)
    ↓
Interface + Base (T006-T007) + Migration (T009) + Entity (T008)
    ↓
Contract Tests (T010-T012) - MUST FAIL
    ↓
[Adapters (T013-T016)] [P] + [Anti-Spam (T017-T021)] [P]
    ↓
Orchestrator (T022-T025)
    ↓
API Tracking (T026-T028)
    ↓
[Unit Tests (T029-T038)] [P]
    ↓
Integration Tests (T039-T046) - Sequential
    ↓
Manual Validation (T047)
```

---

## Parallel Execution Examples

### Launch Contract Tests (T010-T012) in Parallel
```bash
# All 3 contract tests can run together (different files)
Task: "Contract test SearchProviderAdapter in northstar-crawler/src/test/java/.../contract/SearchProviderAdapterContractTest.java"
Task: "Contract test AntiSpamFilter in northstar-crawler/src/test/java/.../contract/AntiSpamFilterContractTest.java"
Task: "Contract test MultiProviderSearchOrchestrator in northstar-crawler/src/test/java/.../contract/MultiProviderSearchOrchestratorContractTest.java"
```

### Launch Adapter Implementations (T013-T016) in Parallel
```bash
# All 4 adapters can be implemented together (different files)
Task: "Implement BraveSearchAdapter in northstar-crawler/src/main/java/.../adapter/BraveSearchAdapter.java"
Task: "Implement SearxngAdapter in northstar-crawler/src/main/java/.../adapter/SearxngAdapter.java"
Task: "Implement SerperAdapter in northstar-crawler/src/main/java/.../adapter/SerperAdapter.java"
Task: "Implement TavilyAdapter in northstar-crawler/src/main/java/.../adapter/TavilyAdapter.java"
```

### Launch Anti-Spam Detectors (T018-T021) in Parallel
```bash
# All 4 detectors can be implemented together (different files)
Task: "Implement keyword stuffing detector in northstar-crawler/src/main/java/.../antispam/KeywordStuffingDetector.java"
Task: "Implement domain-metadata mismatch detector in northstar-crawler/src/main/java/.../antispam/DomainMetadataMismatchDetector.java"
Task: "Implement unnatural keyword list detector in northstar-crawler/src/main/java/.../antispam/UnnaturalKeywordListDetector.java"
Task: "Implement cross-category spam detector in northstar-crawler/src/main/java/.../antispam/CrossCategorySpamDetector.java"
```

### Launch Unit Tests (T029-T038) in Parallel
```bash
# All unit tests can run together (independent test files)
Task: "Unit tests for BraveSearchAdapter in northstar-crawler/src/test/java/.../unit/BraveSearchAdapterTest.java"
Task: "Unit tests for SearxngAdapter in northstar-crawler/src/test/java/.../unit/SearxngAdapterTest.java"
Task: "Unit tests for SerperAdapter in northstar-crawler/src/test/java/.../unit/SerperAdapterTest.java"
Task: "Unit tests for TavilyAdapter in northstar-crawler/src/test/java/.../unit/TavilyAdapterTest.java"
Task: "Unit tests for keyword stuffing detection in northstar-crawler/src/test/java/.../unit/KeywordStuffingDetectorTest.java"
Task: "Unit tests for domain-metadata mismatch in northstar-crawler/src/test/java/.../unit/DomainMetadataMismatchDetectorTest.java"
Task: "Unit tests for unnatural keyword list in northstar-crawler/src/test/java/.../unit/UnnaturalKeywordListDetectorTest.java"
Task: "Unit tests for cross-category spam in northstar-crawler/src/test/java/.../unit/CrossCategorySpamDetectorTest.java"
Task: "Unit tests for AntiSpamFilterImpl in northstar-crawler/src/test/java/.../unit/AntiSpamFilterImplTest.java"
Task: "Unit tests for MultiProviderSearchOrchestrator in northstar-crawler/src/test/java/.../unit/MultiProviderSearchOrchestratorTest.java"
```

---

## Notes

- **[P] tasks** = Different files, no dependencies, can run in parallel
- **TDD Requirement**: Contract tests (T010-T012) MUST be written and MUST FAIL before implementing adapters/anti-spam/orchestrator
- **Commit Strategy**: Commit after each task completion
- **TestContainers**: Integration tests (T039-T046) use ephemeral PostgreSQL, NOT Mac Studio database
- **API Keys**: BraveSearch, Serper, Tavily require valid API keys in application-prod.properties
- **SearXNG**: Requires Mac Studio container running at 192.168.1.10:8080

---

## Validation Checklist

- [x] All contracts have corresponding tests (T010-T012)
- [x] All adapters have implementation tasks (T013-T016)
- [x] All tests come before implementation (T010-T012 before T013-T016)
- [x] Parallel tasks truly independent (different files, marked [P])
- [x] Each task specifies exact file path
- [x] No task modifies same file as another [P] task
- [x] All 8 quickstart scenarios have integration tests (T039-T046)

---

**Tasks Generated**: 47 tasks
**Parallel Tasks**: 26 tasks marked [P]
**Sequential Tasks**: 21 tasks with dependencies
**Estimated Implementation Time**: 6-10 days

**Ready for Execution**: YES - All tasks have clear file paths, validation criteria, and dependency ordering
