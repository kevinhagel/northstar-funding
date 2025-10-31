# ADR 003: Independent Circuit Breaker Per Search Engine

**Status**: Accepted
**Date**: 2025-10-26
**Context Tags**: #architecture #fault-tolerance #resilience4j #circuit-breaker

## Context

When implementing parallel search across multiple search engines (Searxng, Tavily, Perplexity), we needed a fault tolerance strategy to handle engine failures gracefully.

### The Problem

**Search engines have different failure characteristics:**
- **Searxng** (self-hosted): Can fail due to network issues, server restarts, Docker container problems
- **Tavily** (API): Can fail due to rate limits, API key issues, service outages
- **Perplexity** (API): Can fail due to rate limits, API downtime, quota exceeded

**Without fault tolerance:**
```java
// All engines use same circuit breaker
List<SearchResult> results = searchEngines.stream()
    .flatMap(engine -> engine.search(query).stream())  // One failure fails everything
    .toList();
```

If **any** engine fails, the entire search fails. This violates the principle of graceful degradation.

### Requirements

- Search should continue with working engines when one fails
- Each engine should have independent failure tracking
- Circuit breaker state should be engine-specific
- System should automatically recover when engines come back online
- Telemetry should show per-engine failure rates
- Configuration should be simple and maintainable

### Constraints

- Using **Resilience4j 2.2.0** for circuit breaker implementation
- **Three search engines** with different failure characteristics
- Spring Boot 3.5.6 integration
- Circuit breaker state must be independent (not shared)
- Must support Spring Actuator health checks

## Decision

**Create independent Resilience4j circuit breaker instances for each search engine, configured separately in `application.yml`.**

**Strategy:**
1. Each adapter (`SearxngAdapter`, `TavilyAdapter`, `PerplexityAdapter`) has its own named circuit breaker
2. Circuit breaker names match engine names: `"searxng"`, `"tavily"`, `"perplexity"`
3. Each circuit breaker has independent state machine (CLOSED → OPEN → HALF_OPEN)
4. Failures in one engine don't affect others
5. Health checks report per-engine circuit breaker state

**Configuration:**
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10

    instances:
      searxng:
        baseConfig: default
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50

      tavily:
        baseConfig: default
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50

      perplexity:
        baseConfig: default
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50

      lmStudio:
        baseConfig: default
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
```

**Adapter implementation:**
```java
@Component
@Slf4j
public class TavilyAdapter implements SearchEngineAdapter {

    @Override
    @CircuitBreaker(name = "tavily", fallbackMethod = "searchFallback")
    @Retry(name = "searchEngines")
    public List<SearchResult> search(String query, int maxResults) {
        log.info("Executing Tavily search: query='{}', maxResults={}", query, maxResults);

        // Call Tavily API
        TavilySearchResponse response = tavilyClient.search(query, maxResults);

        return response.results().stream()
            .map(this::mapToSearchResult)
            .toList();
    }

    private List<SearchResult> searchFallback(String query, int maxResults, Throwable t) {
        log.warn("Tavily circuit breaker fallback triggered for query: '{}' - {}",
            query, t.getMessage());
        return List.of();  // Return empty list, allow other engines to continue
    }
}
```

## Consequences

### Positive

1. **Independent failure handling** - One engine failure doesn't cascade to others
2. **Graceful degradation** - System continues with 2/3 engines if one fails
3. **Automatic recovery** - Circuit breaker transitions to HALF_OPEN after 30s, tests recovery
4. **Better observability** - Health checks show per-engine status
   ```bash
   curl http://localhost:8080/api/actuator/health
   {
     "components": {
       "circuitBreakers": {
         "searxng": "UP",
         "tavily": "CIRCUIT_OPEN",  # Tavily is down
         "perplexity": "UP"
       }
     }
   }
   ```
5. **Prevents cascading failures** - OPEN circuit breaker fails fast, doesn't retry
6. **Retry logic integration** - Can combine circuit breaker with retry for transient failures
7. **Simple configuration** - Declarative YAML configuration, no code changes needed

### Negative

1. **More configuration** - Need to configure each engine separately
2. **Complexity** - Four circuit breakers instead of one (searxng, tavily, perplexity, lmStudio)
3. **Testing overhead** - Need to test circuit breaker behavior for each engine
4. **Potential inconsistency** - Engines might have different thresholds if not careful

### Neutral

1. **Future engines** - Each new search engine requires new circuit breaker instance
2. **Tuning required** - May need to adjust thresholds per engine based on observed behavior
3. **State management** - Circuit breaker state is in-memory (lost on restart, which is acceptable)

## Alternatives Considered

### Alternative 1: Single Shared Circuit Breaker
**Description**: All search engines share one circuit breaker
```java
@CircuitBreaker(name = "searchEngines")  // Shared across all engines
public List<SearchResult> search(String query, int maxResults) {
    return engines.parallelStream()
        .flatMap(engine -> engine.search(query, maxResults).stream())
        .toList();
}
```

**Pros**:
- Simpler configuration (one circuit breaker instance)
- Less overhead
- Easier to understand

**Cons**:
- **Cascading failures** - One engine failure opens circuit for all engines
- **No graceful degradation** - If Tavily fails, Searxng and Perplexity can't be used either
- **Poor fault isolation** - Can't identify which engine is problematic
- **All-or-nothing** - System is either fully working or fully failing

**Why we didn't choose it**: Violates fault isolation principle. One failing engine shouldn't prevent using healthy engines.

### Alternative 2: Try-Catch Without Circuit Breaker
**Description**: Handle failures with simple try-catch, no circuit breaker
```java
public List<SearchResult> search(String query, int maxResults) {
    List<SearchResult> results = new ArrayList<>();

    for (SearchEngineAdapter engine : engines) {
        try {
            results.addAll(engine.search(query, maxResults));
        } catch (Exception e) {
            log.warn("Engine failed: {}", engine.getName(), e);
            // Continue with next engine
        }
    }

    return results;
}
```

**Pros**:
- Simplest possible implementation
- No external dependencies
- Easy to understand

**Cons**:
- **No automatic recovery detection** - Keeps retrying failed engines every time
- **Wastes resources** - Makes HTTP calls to known-failing engines
- **Slow failures** - Waits for timeout on every call (e.g., 10s timeout × 3 retries = 30s wasted)
- **No telemetry** - Can't monitor failure rates or circuit breaker state
- **No fail-fast** - Doesn't prevent repeated calls to failing service

**Why we didn't choose it**: Circuit breaker pattern is specifically designed for this use case. Try-catch alone doesn't provide fail-fast behavior or automatic recovery detection.

### Alternative 3: Bulkhead + Circuit Breaker
**Description**: Use Resilience4j Bulkhead pattern in addition to circuit breaker
```yaml
resilience4j:
  bulkhead:
    instances:
      searxng:
        maxConcurrentCalls: 5
        maxWaitDuration: 10s
      tavily:
        maxConcurrentCalls: 10
        maxWaitDuration: 10s
```

**Pros**:
- Limits concurrent calls to each engine (prevents thread starvation)
- Additional isolation between engines
- Prevents one slow engine from blocking others

**Cons**:
- **Added complexity** - Another abstraction to configure and understand
- **Not needed for our use case** - We use Virtual Threads, not fixed thread pools
- **Overkill for 3 engines** - Bulkhead shines with many concurrent services

**Why we didn't choose it**: Virtual Threads already provide lightweight concurrency. Bulkhead pattern is more useful with fixed thread pools where threads are a limited resource.

### Alternative 4: Custom Fault Tolerance Implementation
**Description**: Build our own circuit breaker logic
```java
public class CustomCircuitBreaker {
    private volatile State state = State.CLOSED;
    private int failureCount = 0;
    private final int threshold = 5;
    private LocalDateTime lastFailureTime;

    public <T> T execute(Supplier<T> operation, Function<Throwable, T> fallback) {
        if (state == State.OPEN) {
            if (Duration.between(lastFailureTime, LocalDateTime.now()).toSeconds() > 30) {
                state = State.HALF_OPEN;
            } else {
                return fallback.apply(new CircuitBreakerOpenException());
            }
        }

        try {
            T result = operation.get();
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
                failureCount = 0;
            }
            return result;
        } catch (Exception e) {
            handleFailure(e);
            return fallback.apply(e);
        }
    }
    // ...
}
```

**Pros**:
- Full control over circuit breaker logic
- No external dependencies
- Can customize behavior exactly as needed

**Cons**:
- **Reinventing the wheel** - Circuit breaker is a solved problem
- **Bug-prone** - State management, thread safety, edge cases are tricky
- **No Spring integration** - Would need to build health check integration, metrics, etc.
- **Maintenance burden** - Need to test and maintain custom implementation

**Why we didn't choose it**: Resilience4j is a mature, well-tested library with Spring Boot integration. No reason to rebuild what already exists.

## Implementation Notes

### Files Created/Modified

**Configuration:**
```yaml
# backend/src/main/resources/application.yml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10

    instances:
      searxng:
        baseConfig: default
      tavily:
        baseConfig: default
      perplexity:
        baseConfig: default
      lmStudio:
        baseConfig: default

  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1000ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - org.springframework.web.client.ResourceAccessException
          - org.springframework.web.client.HttpServerErrorException

    instances:
      searchEngines:
        baseConfig: default
```

**Adapter with circuit breaker:**
```java
// backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/adapters/TavilyAdapter.java
@Component
@Slf4j
public class TavilyAdapter implements SearchEngineAdapter {

    private final WebClient tavilyClient;

    @Override
    @CircuitBreaker(name = "tavily", fallbackMethod = "searchFallback")
    @Retry(name = "searchEngines")
    public List<SearchResult> search(String query, int maxResults) {
        log.info("Tavily search: query='{}', maxResults={}", query, maxResults);

        try {
            TavilySearchResponse response = tavilyClient
                .post()
                .uri("/search")
                .bodyValue(new TavilySearchRequest(query, maxResults))
                .retrieve()
                .bodyToMono(TavilySearchResponse.class)
                .block();

            if (response == null || response.results() == null) {
                log.warn("Tavily returned null response for query: '{}'", query);
                return List.of();
            }

            log.info("Tavily search successful: {} results for query '{}'",
                response.results().size(), query);

            return response.results().stream()
                .map(this::mapToSearchResult)
                .toList();

        } catch (Exception e) {
            log.error("Tavily search failed for query: '{}' - {}", query, e.getMessage());
            throw new SearchEngineException("Tavily search failed", e);
        }
    }

    private List<SearchResult> searchFallback(String query, int maxResults, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.warn("Tavily circuit breaker OPEN - skipping search for query: '{}'", query);
        } else {
            log.warn("Tavily search fallback triggered for query: '{}' - {}",
                query, t.getMessage());
        }
        return List.of();  // Graceful degradation
    }

    @Override
    public String getName() {
        return "Tavily";
    }
}
```

**Circuit breaker health check:**
```bash
# Check circuit breaker state
curl http://localhost:8080/api/actuator/health | jq '.components.circuitBreakers'

# Example output
{
  "status": "UP",
  "details": {
    "searxng": {
      "status": "UP",
      "details": {
        "failureRate": "0.0%",
        "failureRateThreshold": "50.0%",
        "slowCallRate": "0.0%",
        "slowCallRateThreshold": "100.0%",
        "bufferedCalls": 10,
        "slowCalls": 0,
        "slowFailedCalls": 0,
        "failedCalls": 0,
        "notPermittedCalls": 0,
        "state": "CLOSED"
      }
    },
    "tavily": {
      "status": "CIRCUIT_OPEN",
      "details": {
        "failureRate": "60.0%",
        "failureRateThreshold": "50.0%",
        "bufferedCalls": 10,
        "failedCalls": 6,
        "state": "OPEN"
      }
    },
    "perplexity": {
      "status": "UP",
      "details": {
        "failureRate": "10.0%",
        "state": "CLOSED"
      }
    }
  }
}
```

### Circuit Breaker Behavior

**State Transitions:**
```
CLOSED (normal operation)
  → 5+ calls, ≥50% failure rate
  → OPEN (fail fast)
  → Wait 30s
  → HALF_OPEN (test recovery)
  → 3 successful calls
  → CLOSED (recovered)

  OR

  → HALF_OPEN (test recovery)
  → 1 failure
  → OPEN (still broken)
```

**Failure Rate Calculation:**
- Sliding window of last 10 calls
- Minimum 5 calls before circuit breaker activates
- Failure rate threshold: 50% (5 out of 10 calls fail)

**Retry Integration:**
- Circuit breaker wraps retry logic
- Retry: 3 attempts with exponential backoff (1s, 2s, 4s)
- If all retries fail, circuit breaker counts as 1 failure
- Circuit breaker prevents retries when OPEN (fail fast)

### Testing Circuit Breaker Behavior

**Integration test:**
```java
// backend/src/test/java/com/northstar/funding/integration/CircuitBreakerTest.java
@SpringBootTest
@Testcontainers
class CircuitBreakerTest {

    @Test
    void shouldOpenCircuitAfterThresholdFailures() {
        // Trigger 5 failures in Tavily adapter
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> tavilyAdapter.search("test query", 10))
                .isInstanceOf(SearchEngineException.class);
        }

        // Circuit should now be OPEN
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("tavily");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Next call should fail fast without hitting API
        long startTime = System.currentTimeMillis();
        List<SearchResult> results = tavilyAdapter.search("test query", 10);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(results).isEmpty();
        assertThat(duration).isLessThan(100);  // Fail fast, no HTTP call
    }

    @Test
    void shouldContinueWithWorkingEnginesWhenOneFails() {
        // Simulate Tavily failure
        mockTavilyToFail();

        // Execute search with all engines
        SearchSessionRequest request = SearchSessionRequest.builder()
            .sessionType("NIGHTLY_DISCOVERY")
            .queries(List.of(testQuery))
            .build();

        SearchSessionSummary summary = searchService.executeSearchSession(request);

        // Should still get results from Searxng and Perplexity
        assertThat(summary.totalResults()).isGreaterThan(0);
        assertThat(summary.statistics().get("tavily").failedQueries()).isEqualTo(1);
        assertThat(summary.statistics().get("searxng").successfulQueries()).isEqualTo(1);
        assertThat(summary.statistics().get("perplexity").successfulQueries()).isEqualTo(1);
    }
}
```

## Performance Impact

**Circuit Breaker Overhead:**
- Per-call overhead: <1ms (state check + metrics update)
- Memory per instance: ~10KB (sliding window buffer)
- Total overhead for 4 instances: ~40KB memory, negligible CPU

**Fail-Fast Benefits:**
- Without circuit breaker: Failed call waits for timeout (10s) + retries (3 × 2s backoff = 6s) = 16s wasted
- With circuit breaker OPEN: Failed call returns immediately (<1ms)
- **Speedup: ~16,000x** for failing engines

**Typical Search Session:**
- 3 healthy engines: 3-8 seconds total (parallel execution)
- 1 failing engine (circuit OPEN): 3-8 seconds total (no impact, fail fast)
- 1 failing engine (circuit CLOSED, retrying): 15-25 seconds total (16s wasted on timeouts)

## References

- **Feature Spec**: [[../specs/003-search-execution-infrastructure/spec.md|Feature 003 Specification]]
- **Completion Summary**: [[../specs/003-search-execution-infrastructure/COMPLETION-SUMMARY.md|Feature 003 Completion]]
- **Code Files**:
  - Adapters: `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/adapters/`
  - Configuration: `backend/src/main/resources/application.yml`
- **Tests**:
  - `backend/src/test/java/com/northstar/funding/integration/CircuitBreakerTest.java`
- **Resilience4j Documentation**: https://resilience4j.readme.io/docs/circuitbreaker
- **Spring Boot Resilience4j**: https://resilience4j.readme.io/docs/getting-started-3
- **Martin Fowler - Circuit Breaker Pattern**: https://martinfowler.com/bliki/CircuitBreaker.html
