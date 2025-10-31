# ADR 004: Java 25 Virtual Threads for Parallel Search Execution

**Status**: Accepted
**Date**: 2025-10-27
**Context Tags**: #architecture #performance #java-25 #virtual-threads #parallel-execution

## Context

When implementing parallel search execution across three search engines (Searxng, Tavily, Perplexity), we needed to maximize throughput while minimizing complexity.

### The Problem

**Sequential search is too slow:**
```java
// Sequential execution
List<SearchResult> results = new ArrayList<>();
results.addAll(searxngAdapter.search(query, maxResults));  // 3-5s
results.addAll(tavilyAdapter.search(query, maxResults));   // 3-5s
results.addAll(perplexityAdapter.search(query, maxResults)); // 3-5s
// Total: 9-15 seconds per query
```

For a nightly discovery session with **7 queries × 3 engines = 21 searches**, sequential execution would take **3-5 minutes**. This doesn't scale.

**The bottleneck is I/O, not CPU:**
- Search engines respond in 3-5 seconds
- During that time, Java thread is **blocked waiting for HTTP response**
- CPU is idle
- Memory is wasted on blocked threads

### Requirements

- Execute searches across all engines in parallel (3x speedup)
- Handle 7-10 queries concurrently without thread pool exhaustion
- Scale to hundreds of concurrent searches (future nightly sessions)
- Simple implementation (no complex thread pool tuning)
- Good error handling (one engine failure doesn't block others)
- Lightweight resource usage (millions of potential threads)

### Constraints

- Using **Java 25** (Virtual Threads from Project Loom)
- **Spring Boot 3.5.6** with Spring WebFlux for reactive HTTP clients
- **I/O-bound workload** (HTTP calls to search engines)
- Circuit breaker protection per engine (Resilience4j)
- TestContainers integration tests must complete quickly

## Decision

**Use Java 25 Virtual Threads with `Executors.newVirtualThreadPerTaskExecutor()` for parallel search execution.**

**Strategy:**
1. Create virtual thread executor for each search session
2. Submit search tasks for all engines in parallel
3. Collect results as `CompletableFuture<List<SearchResult>>`
4. Wait for all futures to complete
5. Flatten and return combined results
6. Executor auto-closes with try-with-resources

**Implementation:**
```java
@Service
@Slf4j
public class SearchExecutionService {

    public List<SearchResult> executeParallelSearch(
        String query,
        int maxResults,
        List<SearchEngineAdapter> engines
    ) {
        // Virtual thread executor (auto-closing)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit tasks to virtual threads
            List<CompletableFuture<List<SearchResult>>> futures = engines.stream()
                .map(engine -> CompletableFuture.supplyAsync(
                    () -> engine.search(query, maxResults),
                    executor
                ))
                .toList();

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results
            return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
        }
    }
}
```

## Consequences

### Positive

1. **3x speedup** - Parallel execution across 3 engines (3-8s vs 9-24s sequential)
2. **Simple code** - No thread pool configuration, no tuning, no queue sizing
3. **Scales massively** - Can handle millions of virtual threads (vs thousands of platform threads)
4. **Low memory overhead** - Virtual threads: ~1KB each vs platform threads: ~1MB each
5. **I/O-optimized** - Virtual threads yield during blocking I/O, CPU stays busy
6. **Easy error handling** - CompletableFuture handles exceptions per engine
7. **No thread pool exhaustion** - Create as many virtual threads as needed
8. **Resource cleanup** - Try-with-resources auto-closes executor

### Negative

1. **Java 25 requirement** - Can't use older Java versions (not a problem for us)
2. **JVM overhead** - Slight overhead vs native async (negligible for our use case)
3. **Testing complexity** - Need to test parallel execution behavior
4. **Debugging** - Stack traces span multiple virtual threads

### Neutral

1. **Not reactive** - Could use Project Reactor, but Virtual Threads are simpler
2. **Thread locals** - Virtual threads support thread locals, but may not be needed
3. **Blocking calls** - Virtual threads handle blocking I/O efficiently, no need to rewrite to reactive

## Alternatives Considered

### Alternative 1: Fixed Thread Pool (Platform Threads)
**Description**: Use traditional `ExecutorService` with fixed thread pool
```java
ExecutorService executor = Executors.newFixedThreadPool(10);

List<Future<List<SearchResult>>> futures = engines.stream()
    .map(engine -> executor.submit(() -> engine.search(query, maxResults)))
    .toList();

for (Future<List<SearchResult>> future : futures) {
    results.addAll(future.get());
}

executor.shutdown();
```

**Pros**:
- Works with older Java versions (8+)
- Well-understood pattern
- Can limit concurrency (pool size)

**Cons**:
- **Thread pool tuning required** - How many threads? Too few: underutilized. Too many: context switching overhead
- **Memory overhead** - Each platform thread: ~1MB stack size
- **Thread pool exhaustion** - If all threads blocked, new tasks queue up
- **Shutdown complexity** - Must manually shutdown executor, handle timeouts
- **Doesn't scale** - 1000 concurrent searches = 1000 threads = 1GB RAM just for stacks

**Why we didn't choose it**: Virtual Threads eliminate all thread pool tuning complexity and scale better.

### Alternative 2: Project Reactor (Reactive Streams)
**Description**: Use Spring WebFlux reactive programming with `Mono`/`Flux`
```java
public Flux<SearchResult> executeParallelSearch(String query, int maxResults) {
    return Flux.fromIterable(engines)
        .flatMap(engine -> Mono.fromCallable(() -> engine.search(query, maxResults))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(Flux::fromIterable)
        );
}
```

**Pros**:
- Non-blocking I/O (efficient for high concurrency)
- Composable operators (map, flatMap, filter, etc.)
- Backpressure support
- Spring WebFlux integration

**Cons**:
- **Steep learning curve** - Reactive programming is complex (Mono, Flux, operators, schedulers)
- **Harder to debug** - Stack traces are fragmented across async boundaries
- **More code** - Requires understanding of reactive streams, publishers, subscribers
- **Overkill for our use case** - Virtual Threads give 90% of benefits with 10% of complexity

**Why we didn't choose it**: Virtual Threads provide near-reactive performance with imperative, easy-to-understand code.

### Alternative 3: Parallel Streams
**Description**: Use Java parallel streams for parallelization
```java
List<SearchResult> results = engines.parallelStream()
    .flatMap(engine -> engine.search(query, maxResults).stream())
    .toList();
```

**Pros**:
- Simplest possible code (one-liner)
- No executor management
- Built into Java 8+

**Cons**:
- **Uses common ForkJoinPool** - Shared across entire JVM (uncontrolled concurrency)
- **No per-task control** - Can't isolate search session from other parallel operations
- **Thread pool sizing** - ForkJoinPool size = CPU cores (not optimal for I/O-bound)
- **Poor error handling** - Exception in one task can fail entire stream
- **No timeouts** - Can't easily add per-search timeouts

**Why we didn't choose it**: Parallel streams use CPU-bound ForkJoinPool, not suitable for I/O-bound workloads. Virtual Threads are designed for blocking I/O.

### Alternative 4: CompletableFuture with Common Pool
**Description**: Use `CompletableFuture.supplyAsync()` with common ForkJoinPool
```java
List<CompletableFuture<List<SearchResult>>> futures = engines.stream()
    .map(engine -> CompletableFuture.supplyAsync(
        () -> engine.search(query, maxResults)
    ))
    .toList();

List<SearchResult> results = futures.stream()
    .map(CompletableFuture::join)
    .flatMap(List::stream)
    .toList();
```

**Pros**:
- Simple async composition
- Built-in error handling (exceptionally, handle)
- Works with Java 8+

**Cons**:
- **Uses common ForkJoinPool** - Same issues as parallel streams
- **No virtual threads** - Uses platform threads (memory overhead)
- **Pool contention** - Shared pool means other app components can starve search tasks

**Why we didn't choose it**: Virtual Thread executor provides dedicated threads per session, better isolation.

## Implementation Notes

### Files Created/Modified

**Service implementation:**
```java
// backend/src/main/java/com/northstar/funding/discovery/search/application/SearchExecutionService.java
@Service
@Slf4j
public class SearchExecutionService {

    private final List<SearchEngineAdapter> searchEngineAdapters;

    public SearchSessionSummary executeSearchSession(SearchSessionRequest request) {
        log.info("Executing search session: type={}, queries={}",
            request.sessionType(), request.queries().size());

        Map<String, SearchSessionStatistics> statsMap = new HashMap<>();
        List<SearchResult> allResults = new ArrayList<>();

        // Execute all queries
        for (SearchQuery query : request.queries()) {
            List<SearchResult> queryResults = executeParallelSearch(
                query.getQueryText(),
                query.getMaxResults(),
                searchEngineAdapters
            );

            allResults.addAll(queryResults);

            // Update statistics per engine
            updateStatistics(statsMap, query, queryResults);
        }

        return buildSummary(request, allResults, statsMap);
    }

    private List<SearchResult> executeParallelSearch(
        String query,
        int maxResults,
        List<SearchEngineAdapter> engines
    ) {
        log.info("Executing parallel search: query='{}', engines={}",
            query, engines.size());

        // Virtual thread executor (auto-closing with try-with-resources)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit all search tasks to virtual threads
            List<CompletableFuture<List<SearchResult>>> futures = engines.stream()
                .map(engine -> CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            log.debug("Starting search on {}: '{}'", engine.getName(), query);
                            List<SearchResult> results = engine.search(query, maxResults);
                            log.debug("Completed search on {}: {} results",
                                engine.getName(), results.size());
                            return results;
                        } catch (Exception e) {
                            log.warn("Search failed on {}: {}", engine.getName(), e.getMessage());
                            return List.<SearchResult>of();  // Return empty list on failure
                        }
                    },
                    executor
                ))
                .toList();

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results from all engines
            List<SearchResult> results = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

            log.info("Parallel search completed: {} total results from {} engines",
                results.size(), engines.size());

            return results;

        } // Executor auto-closes here
    }
}
```

**Nightly scheduler:**
```java
// backend/src/main/java/com/northstar/funding/discovery/search/application/NightlyDiscoveryScheduler.java
@Component
@Slf4j
public class NightlyDiscoveryScheduler {

    private final SearchExecutionService searchExecutionService;
    private final SearchQueryRepository queryRepository;

    @Scheduled(cron = "${discovery.schedule.cron:0 0 2 * * *}")  // 2 AM daily
    @ConditionalOnProperty(name = "discovery.schedule.enabled", havingValue = "true")
    public void executeNightlyDiscovery() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        log.info("Starting nightly discovery for {}", today);

        // Load today's queries
        List<SearchQuery> queries = queryRepository.findByDayOfWeekAndEnabled(
            today.name(), true
        );

        if (queries.isEmpty()) {
            log.warn("No queries configured for {}", today);
            return;
        }

        log.info("Found {} queries for {}", queries.size(), today);

        // Execute search session (uses Virtual Threads internally)
        SearchSessionRequest request = SearchSessionRequest.builder()
            .sessionType("NIGHTLY_DISCOVERY")
            .queries(queries)
            .build();

        SearchSessionSummary summary = searchExecutionService.executeSearchSession(request);

        log.info("Nightly discovery completed: {} results, {} unique domains",
            summary.totalResults(), summary.uniqueDomains());
    }
}
```

### Performance Characteristics

**Single Query (3 Engines Parallel):**
```
Sequential: 9-15s (3s + 3s + 3s)
Virtual Threads: 3-5s (max of 3s, 3s, 3s = 3s + overhead)
Speedup: ~3x
```

**7 Queries (Monday Schedule):**
```
Sequential: 63-105s (7 queries × 9-15s)
Virtual Threads: 21-35s (7 queries × 3-5s)
Speedup: ~3x
```

**Virtual Thread Resource Usage:**
```
Memory per virtual thread: ~1KB
3 engines × 7 queries = 21 virtual threads
Total memory overhead: ~21KB
Platform thread equivalent: ~21MB
Memory savings: ~99.9%
```

### Integration Test

**Test parallel execution:**
```java
// backend/src/test/java/com/northstar/funding/integration/MondayNightlyDiscoveryTest.java
@SpringBootTest
@Testcontainers
@ActiveProfiles("postgres-test")
class MondayNightlyDiscoveryTest {

    @Test
    void shouldExecuteParallelSearchAcrossAllEngines() {
        // Given: Monday queries loaded from application.yml
        List<SearchQuery> queries = queryRepository.findByDayOfWeekAndEnabled("MONDAY", true);
        assertThat(queries).hasSize(7);

        SearchSessionRequest request = SearchSessionRequest.builder()
            .sessionType("NIGHTLY_DISCOVERY")
            .queries(queries)
            .build();

        // When: Execute search session (uses Virtual Threads)
        long startTime = System.currentTimeMillis();
        SearchSessionSummary summary = searchService.executeSearchSession(request);
        long duration = System.currentTimeMillis() - startTime;

        // Then: Should complete in reasonable time
        assertThat(duration).isLessThan(60_000);  // <60s for 7 queries × 3 engines

        // Should get results from all engines
        assertThat(summary.totalResults()).isGreaterThan(0);
        assertThat(summary.statistics()).containsKeys("searxng", "tavily", "perplexity");

        // Each engine should have processed queries
        summary.statistics().values().forEach(stats -> {
            assertThat(stats.queriesExecuted()).isEqualTo(7);
            assertThat(stats.resultsReturned()).isGreaterThan(0);
        });
    }

    @Test
    void shouldHandleEngineFailuresGracefully() {
        // Given: Mock one engine to fail
        when(tavilyAdapter.search(anyString(), anyInt()))
            .thenThrow(new SearchEngineException("Tavily API error"));

        // When: Execute search (Tavily will fail, others should continue)
        SearchSessionSummary summary = searchService.executeSearchSession(request);

        // Then: Should still get results from working engines
        assertThat(summary.totalResults()).isGreaterThan(0);

        // Tavily should show failures
        SearchSessionStatistics tavilyStats = summary.statistics().get("tavily");
        assertThat(tavilyStats.failedQueries()).isEqualTo(7);

        // Other engines should succeed
        SearchSessionStatistics searxngStats = summary.statistics().get("searxng");
        assertThat(searxngStats.successfulQueries()).isEqualTo(7);
    }
}
```

### Future Enhancements

**Timeout per search:**
```java
private List<SearchResult> executeParallelSearch(
    String query,
    int maxResults,
    List<SearchEngineAdapter> engines
) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<CompletableFuture<List<SearchResult>>> futures = engines.stream()
            .map(engine -> CompletableFuture.supplyAsync(
                () -> engine.search(query, maxResults),
                executor
            ).orTimeout(10, TimeUnit.SECONDS))  // Timeout per engine
            .toList();

        // ...
    }
}
```

**Structured Concurrency (Java 21+):**
```java
// Future: Use StructuredTaskScope for better error handling
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    List<Supplier<List<SearchResult>>> tasks = engines.stream()
        .map(engine -> scope.fork(() -> engine.search(query, maxResults)))
        .toList();

    scope.join();           // Wait for all tasks
    scope.throwIfFailed();  // Propagate failures

    return tasks.stream()
        .map(Supplier::get)
        .flatMap(List::stream)
        .toList();
}
```

## References

- **Feature Spec**: [[../specs/003-search-execution-infrastructure/spec.md|Feature 003 Specification]]
- **Completion Summary**: [[../specs/003-search-execution-infrastructure/COMPLETION-SUMMARY.md|Feature 003 Completion]]
- **Code Files**:
  - Service: `backend/src/main/java/com/northstar/funding/discovery/search/application/SearchExecutionService.java`
  - Scheduler: `backend/src/main/java/com/northstar/funding/discovery/search/application/NightlyDiscoveryScheduler.java`
- **Tests**:
  - `backend/src/test/java/com/northstar/funding/integration/MondayNightlyDiscoveryTest.java`
  - `backend/src/test/java/com/northstar/funding/integration/SearchPerformanceTest.java`
- **Java Documentation**:
  - [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
  - [Virtual Threads Guide](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html)
- **Related ADRs**:
  - [[003-circuit-breaker-per-engine]] - Circuit breaker protection complements parallel execution
