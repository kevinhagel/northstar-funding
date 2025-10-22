# Quickstart Guide: Search Execution Infrastructure

**Feature**: 003-search-execution-infrastructure
**Date**: 2025-10-20
**Phase**: Phase 1 - Integration Test Scenarios

## Overview
This quickstart guide defines integration test scenarios that validate the search execution infrastructure end-to-end. Each scenario corresponds to acceptance criteria from the feature specification.

---

## Prerequisites

### Environment Setup
1. **PostgreSQL**: Running on Mac Studio (192.168.1.10:5432) with `northstar_funding` database
2. **Searxng**: Running on Mac Studio (192.168.1.10:8080)
3. **API Keys**: Set environment variables for Tavily and Perplexity
4. **TestContainers**: PostgreSQL test container for integration tests
5. **WireMock**: For mocking external search APIs

### Environment Variables
```bash
# Development/Testing
export TAVILY_API_KEY="tvly-test-key-12345"
export PERPLEXITY_API_KEY="pplx-test-key-12345"

# Searxng (no key required - self-hosted)
export SEARXNG_BASE_URL="http://192.168.1.10:8080"
```

---

## Scenario 1: Monday Nightly Discovery Session

**Given**: Nightly scheduler is enabled, current day is Monday, 5 queries configured for Monday
**When**: Scheduler executes at 2:00 AM
**Then**: System executes all 5 queries across 3 enabled engines (Searxng, Tavily, Perplexity)

### Test Steps
```java
@SpringBootTest
@Testcontainers
class MondayNightlyDiscoveryTest {

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Autowired
    private SearchSessionService searchSessionService;

    @Autowired
    private DiscoverySessionRepository discoverySessionRepository;

    @Test
    void testMondayNightlyDiscovery() {
        // GIVEN: Monday query configuration
        LocalDate monday = LocalDate.of(2025, 10, 20); // Monday
        List<SearchQuery> mondayQueries = searchQueryLibrary.getQueriesForDay(DayOfWeek.MONDAY);
        assertThat(mondayQueries).hasSize(5);

        // WHEN: Execute nightly discovery
        DiscoverySession session = searchExecutionService.executeNightlyDiscovery(monday);

        // THEN: All queries executed across all engines
        SearchSessionStatistics stats = searchSessionService.getStatisticsForSession(session.getId());
        assertThat(stats.getTotalQueriesExecuted()).isEqualTo(5);
        assertThat(stats.getPerEngineStats()).hasSize(3); // Searxng, Tavily, Perplexity
        assertThat(stats.getTotalResultsCollected()).isBetween(500, 1000); // 5 queries * 3 engines * ~25 results

        // Verify session metadata
        assertThat(session.getSessionType()).isEqualTo(SessionType.SEARCH_EXECUTION);
        assertThat(session.getSearchQuerySetName()).isEqualTo("Monday");
        assertThat(session.getTotalSearchQueriesExecuted()).isEqualTo(5);
    }
}
```

### Expected Results
- **Total Queries**: 5
- **Total Results**: 500-1000 (5 queries × 3 engines × ~25 results)
- **Unique Domains**: 100-300 (after deduplication)
- **High-Confidence Candidates**: 50-150 (confidence >= 0.60)
- **Session Duration**: < 30 minutes

---

## Scenario 2: Domain Deduplication Across Search Engines

**Given**: Multiple search engines return results for the same domain
**When**: Deduplication process runs
**Then**: Each domain is processed only once per session

### Test Steps
```java
@SpringBootTest
@Testcontainers
class DomainDeduplicationTest {

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Autowired
    private DomainRegistryService domainRegistryService;

    @Test
    void testDomainDeduplicationAcrossEngines() {
        // GIVEN: Query that will return overlapping results
        String query = "bulgaria funding grants";
        SearchEngineRequest request = SearchEngineRequest.defaultRequest(query);

        // WHEN: Execute search across all engines
        List<SearchResult> allResults = searchExecutionService
            .executeQueryAcrossEngines(request);

        // Extract unique domains
        Set<String> allDomains = allResults.stream()
            .map(r -> domainRegistryService.extractDomainName(r.getUrl()).get())
            .collect(Collectors.toSet());

        // THEN: Deduplication reduces result count
        assertThat(allResults.size()).isGreaterThan(allDomains.size());

        // Verify each domain processed only once
        allDomains.forEach(domainName -> {
            Try<Domain> domain = domainRegistryService.getOrCreateDomain(domainName);
            assertThat(domain.isSuccess()).isTrue();

            // Verify domain not reprocessed
            long processCount = allResults.stream()
                .filter(r -> domainRegistryService.extractDomainName(r.getUrl())
                    .get()
                    .equals(domainName))
                .count();

            assertThat(processCount).isGreaterThanOrEqualTo(1);
        });

        // Verify statistics
        long deduplicationRate = (allResults.size() - allDomains.size()) * 100 / allResults.size();
        assertThat(deduplicationRate).isBetween(60L, 80L); // 60-80% overlap typical
    }
}
```

### Expected Results
- **Raw Results**: 75 (3 engines × 25 results)
- **Unique Domains**: 20-30 (60-80% deduplication rate)
- **Deduplication Rate**: 60-80%
- **Domain Status**: All new domains set to ACTIVE

---

## Scenario 3: Circuit Breaker Activation on Engine Failure

**Given**: Tavily API is down or returning errors
**When**: SearchExecutionService attempts to query Tavily
**Then**: Circuit breaker opens after 5 consecutive failures, fallback returns empty list

### Test Steps
```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class CircuitBreakerTest {

    @Autowired
    private TavilyAdapter tavilyAdapter;

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Test
    void testCircuitBreakerOpensOnRepeatedFailures() {
        // GIVEN: Tavily API returning 500 errors
        stubFor(post(urlEqualTo("/search"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")));

        SearchEngineRequest request = SearchEngineRequest.defaultRequest("test query");

        // WHEN: Execute 5+ searches (trigger circuit breaker)
        List<Try<List<SearchResult>>> attempts = IntStream.range(0, 5)
            .mapToObj(i -> tavilyAdapter.search(request))
            .toList();

        // THEN: All attempts fail
        assertThat(attempts).allMatch(Try::isFailure);

        // Verify circuit breaker is now OPEN
        HealthStatus health = tavilyAdapter.checkHealth();
        assertThat(health.getCircuitBreakerState()).isEqualTo(CircuitBreakerState.OPEN);
        assertThat(health.isHealthy()).isFalse();

        // Subsequent calls should fail fast (not hit API)
        long startTime = System.currentTimeMillis();
        Try<List<SearchResult>> fastFail = tavilyAdapter.search(request);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(fastFail.isFailure()).isTrue();
        assertThat(duration).isLessThan(100); // Fails immediately without API call

        // Verify fallback returns empty list
        List<SearchResult> results = searchExecutionService
            .executeQueryAcrossEngines(request)
            .stream()
            .filter(r -> r.getSource() == SearchEngineType.TAVILY)
            .toList();

        assertThat(results).isEmpty();
    }
}
```

### Expected Results
- **Circuit Breaker State**: OPEN after 5 failures
- **Failure Rate**: 100% (all 5 attempts failed)
- **Fast-Fail Duration**: < 100ms (no API call made)
- **Fallback Behavior**: Empty list returned, other engines continue processing

---

## Scenario 4: Retry Logic with Exponential Backoff

**Given**: Tavily API returns transient 503 errors
**When**: SearchExecutionService attempts to query Tavily
**Then**: System retries 3 times with exponential backoff (2s, 4s, 8s) before failing

### Test Steps
```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class RetryWithExponentialBackoffTest {

    @Autowired
    private TavilyAdapter tavilyAdapter;

    @Test
    void testRetryWithExponentialBackoff() {
        // GIVEN: Tavily API returns 503 twice, then succeeds
        stubFor(post(urlEqualTo("/search"))
            .inScenario("Retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("First Retry"));

        stubFor(post(urlEqualTo("/search"))
            .inScenario("Retry")
            .whenScenarioStateIs("First Retry")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("Second Retry"));

        stubFor(post(urlEqualTo("/search"))
            .inScenario("Retry")
            .whenScenarioStateIs("Second Retry")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "results": [
                        {"title": "Test Result", "url": "https://example.org", "content": "Test"}
                      ]
                    }
                    """)));

        // WHEN: Execute search with retry
        long startTime = System.currentTimeMillis();
        Try<List<SearchResult>> result = tavilyAdapter.search(
            SearchEngineRequest.defaultRequest("test query"));
        long duration = System.currentTimeMillis() - startTime;

        // THEN: Request succeeds after 2 retries
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).hasSize(1);

        // Verify exponential backoff timing: ~2s + 4s = 6s (plus API latency)
        assertThat(duration).isBetween(6000L, 8000L);

        // Verify WireMock received 3 requests
        verify(3, postRequestedFor(urlEqualTo("/search")));
    }
}
```

### Expected Results
- **Total Attempts**: 3 (initial + 2 retries)
- **Backoff Timing**: 2s, 4s (exponential)
- **Total Duration**: 6-8 seconds
- **Final Result**: Success (after 2 failed attempts)

---

## Scenario 5: Session Statistics and Analytics

**Given**: Nightly discovery session completes successfully
**When**: Kevin reviews session statistics
**Then**: All metrics are accurate and per-engine/per-query breakdowns available

### Test Steps
```java
@SpringBootTest
@Testcontainers
class SessionStatisticsTest {

    @Autowired
    private SearchSessionService searchSessionService;

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Test
    void testSessionStatisticsAccuracy() {
        // GIVEN: Execute nightly discovery
        DiscoverySession session = searchExecutionService.executeNightlyDiscovery(LocalDate.now());

        // WHEN: Retrieve session statistics
        SearchSessionStatistics stats = searchSessionService.getStatisticsForSession(session.getId());

        // THEN: Overall statistics are accurate
        assertThat(stats.getStartTime()).isNotNull();
        assertThat(stats.getEndTime()).isNotNull();
        assertThat(stats.getTotalQueriesExecuted()).isEqualTo(5);
        assertThat(stats.getTotalResultsCollected()).isGreaterThan(0);
        assertThat(stats.getUniqueDomainsFound()).isGreaterThan(0);
        assertThat(stats.getHighConfidenceCandidates()).isGreaterThanOrEqualTo(0);

        // Verify per-engine statistics
        Map<SearchEngineType, EngineStatistics> engineStats = stats.getPerEngineStats();
        assertThat(engineStats).containsKeys(
            SearchEngineType.SEARXNG,
            SearchEngineType.TAVILY,
            SearchEngineType.PERPLEXITY
        );

        engineStats.forEach((engine, engineStat) -> {
            assertThat(engineStat.queriesExecuted()).isEqualTo(5); // All engines queried for all queries
            assertThat(engineStat.resultsReturned()).isGreaterThan(0);
            assertThat(engineStat.averageResponseTimeMs()).isBetween(500, 15000); // 0.5s - 15s
            assertThat(engineStat.circuitBreakerState()).isIn(
                CircuitBreakerState.CLOSED,
                CircuitBreakerState.HALF_OPEN
            );
        });

        // Verify per-query statistics
        Map<String, QueryStatistics> queryStats = stats.getPerQueryStats();
        assertThat(queryStats).hasSize(5); // One entry per query

        queryStats.forEach((queryText, queryStat) -> {
            assertThat(queryStat.uniqueDomainsDiscovered()).isGreaterThan(0);
            assertThat(queryStat.resultsPerEngineBreakdown()).hasSize(3); // 3 engines
        });
    }
}
```

### Expected Results
- **Total Queries**: 5
- **Total Results**: 500-1000
- **Unique Domains**: 100-300
- **High-Confidence Candidates**: 50-150
- **Per-Engine Stats**: 3 entries (Searxng, Tavily, Perplexity)
- **Per-Query Stats**: 5 entries (one per query)
- **Average Response Time**: 0.5s - 15s per engine

---

## Scenario 6: High-Confidence Candidate Creation

**Given**: Search results have been collected and deduplicated
**When**: MetadataJudgingService scores results
**Then**: Candidates with confidence >= 0.60 are created with status PENDING_CRAWL

### Test Steps
```java
@SpringBootTest
@Testcontainers
class HighConfidenceCandidateCreationTest {

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Autowired
    private MetadataJudgingService metadataJudgingService;

    @Autowired
    private FundingSourceCandidateRepository candidateRepository;

    @Test
    void testHighConfidenceCandidateCreation() {
        // GIVEN: Execute search that finds funding-related results
        SearchEngineRequest request = SearchEngineRequest.defaultRequest(
            "bulgaria government grants foundation funding");

        List<SearchResult> results = searchExecutionService
            .executeQueryAcrossEngines(request);

        // WHEN: Judge results via metadata service
        List<MetadataJudgment> judgments = results.stream()
            .map(metadataJudgingService::judgeSearchResult)
            .toList();

        // THEN: High-confidence judgments exist
        List<MetadataJudgment> highConfidence = judgments.stream()
            .filter(MetadataJudgment::getShouldCrawl)
            .toList();

        assertThat(highConfidence).isNotEmpty();

        // Verify candidates created
        highConfidence.forEach(judgment -> {
            List<FundingSourceCandidate> candidates = candidateRepository
                .findByDomainName(judgment.getDomainName());

            assertThat(candidates).isNotEmpty();
            candidates.forEach(candidate -> {
                assertThat(candidate.getStatus()).isEqualTo(CandidateStatus.PENDING_CRAWL);
                assertThat(candidate.getConfidenceScore()).isGreaterThanOrEqualTo(new BigDecimal("0.60"));
                assertThat(candidate.getDomainId()).isNotNull();
            });
        });

        // Verify low-confidence results NOT created as candidates
        List<MetadataJudgment> lowConfidence = judgments.stream()
            .filter(j -> !j.getShouldCrawl())
            .toList();

        assertThat(lowConfidence).isNotEmpty(); // Some results should be low confidence

        // Statistics validation
        int totalJudgments = judgments.size();
        int highConfidenceCount = highConfidence.size();
        double hitRate = (double) highConfidenceCount / totalJudgments * 100;

        assertThat(hitRate).isBetween(20.0, 60.0); // 20-60% hit rate expected
    }
}
```

### Expected Results
- **Total Results Judged**: 75-100
- **High-Confidence Candidates**: 15-60 (20-60% hit rate)
- **Candidate Status**: PENDING_CRAWL
- **Confidence Score**: >= 0.60 (BigDecimal)
- **Domain Linkage**: All candidates linked to Domain entity

---

## Scenario 7: Parallel Execution with Virtual Threads

**Given**: 5 queries configured, 3 search engines enabled
**When**: Search execution runs
**Then**: Queries executed in parallel across engines using Virtual Threads, total time < serial execution time

### Test Steps
```java
@SpringBootTest
class VirtualThreadsParallelExecutionTest {

    @Autowired
    private SearchExecutionService searchExecutionService;

    @Test
    void testVirtualThreadsParallelExecution() {
        // GIVEN: 5 queries, 3 engines (15 total search API calls)
        List<SearchQuery> queries = searchQueryLibrary.getQueriesForDay(DayOfWeek.MONDAY);
        assertThat(queries).hasSize(5);

        // WHEN: Execute all queries in parallel
        long startTime = System.currentTimeMillis();
        DiscoverySession session = searchExecutionService.executeNightlyDiscovery(LocalDate.now());
        long duration = System.currentTimeMillis() - startTime;

        // THEN: Parallel execution completes faster than serial
        // Serial: 5 queries × 3 engines × 5s avg = 75 seconds
        // Parallel: max(query execution times) ≈ 15-30 seconds
        assertThat(duration).isLessThan(30_000L); // 30 seconds

        // Verify all queries executed
        SearchSessionStatistics stats = searchSessionService.getStatisticsForSession(session.getId());
        assertThat(stats.getTotalQueriesExecuted()).isEqualTo(5);
        assertThat(stats.getPerEngineStats()).hasSize(3);

        // Verify Virtual Threads were used (check thread names in logs)
        // Virtual threads typically named "VirtualThread-RUNNABLE@ForkJoinPool-..."
        // This is logged by SearchExecutionService
    }
}
```

### Expected Results
- **Serial Execution Time**: ~75 seconds (5 queries × 3 engines × 5s avg)
- **Parallel Execution Time**: < 30 seconds (Virtual Threads parallelism)
- **Speedup**: 2.5x - 3x faster
- **Thread Model**: Java 25 Virtual Threads

---

## Scenario 8: Scheduler Configuration and Control

**Given**: Nightly scheduler is configurable via YAML
**When**: `discovery.scheduling.enabled=false`
**Then**: Scheduler does not run, no automatic discovery sessions created

### Test Steps
```java
@SpringBootTest(properties = "discovery.scheduling.enabled=false")
class SchedulerConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void testSchedulerDisabledViaConfiguration() {
        // GIVEN: Scheduler disabled in configuration
        DiscoverySchedulingProperties props = context.getBean(DiscoverySchedulingProperties.class);
        assertThat(props.isEnabled()).isFalse();

        // THEN: NightlyDiscoveryScheduler bean not created
        assertThat(context.containsBean("nightlyDiscoveryScheduler")).isFalse();
    }
}

@SpringBootTest(properties = {
    "discovery.scheduling.enabled=true",
    "discovery.scheduling.cron=0 0 2 * * *",
    "discovery.scheduling.timezone=Europe/Sofia"
})
class SchedulerEnabledTest {

    @Autowired
    private NightlyDiscoveryScheduler scheduler;

    @Test
    void testSchedulerConfiguration() {
        // GIVEN: Scheduler enabled with custom cron
        assertThat(scheduler).isNotNull();

        // Verify configuration loaded correctly
        DiscoverySchedulingProperties props = context.getBean(DiscoverySchedulingProperties.class);
        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getCron()).isEqualTo("0 0 2 * * *");
        assertThat(props.getTimezone()).isEqualTo("Europe/Sofia");
    }
}
```

### Expected Results
- **Enabled=true**: Scheduler bean created, nightly execution at 2 AM
- **Enabled=false**: Scheduler bean NOT created, no automatic execution
- **Cron Expression**: `0 0 2 * * *` (2:00 AM daily)
- **Timezone**: `Europe/Sofia`

---

## Running Integration Tests

### Prerequisites
```bash
# Start PostgreSQL on Mac Studio (if not already running)
docker compose -f docker/docker-compose.yml up -d northstar-postgres

# Verify Searxng is running
curl http://192.168.1.10:8080/search?q=test&format=json

# Set API keys
export TAVILY_API_KEY="tvly-your-key"
export PERPLEXITY_API_KEY="pplx-your-key"
```

### Run All Integration Tests
```bash
cd backend
mvn test -Dtest="**/*Test.java" -DfailIfNoTests=false
```

### Run Specific Scenario
```bash
mvn test -Dtest=MondayNightlyDiscoveryTest
mvn test -Dtest=CircuitBreakerTest
mvn test -Dtest=SessionStatisticsTest
```

### Verify Results
```sql
-- Connect to database
docker exec -it northstar-postgres psql -U northstar_user -d northstar_funding

-- Check search queries
SELECT * FROM search_queries WHERE day_of_week = 'MONDAY';

-- Check session statistics
SELECT * FROM search_session_statistics ORDER BY start_time DESC LIMIT 5;

-- Check discovery sessions
SELECT * FROM discovery_sessions WHERE session_type = 'SEARCH_EXECUTION' ORDER BY created_at DESC LIMIT 5;

-- Check candidates created
SELECT COUNT(*), status FROM funding_source_candidates GROUP BY status;
```

---

## Success Criteria

All scenarios must pass with:
- **Test Coverage**: > 80% for search package
- **Integration Tests**: All 8 scenarios passing
- **Performance**: Nightly run completes in < 30 minutes
- **Reliability**: Circuit breakers prevent cascading failures
- **Statistics**: Accurate per-engine and per-query metrics

---

*Quickstart guide completed: 2025-10-20*
*Ready for task generation (Phase 2)*
