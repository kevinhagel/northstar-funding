# Research: AI-Powered Search Query Generation

**Feature**: 004-create-northstar-query
**Date**: 2025-11-02
**Status**: Complete

## Research Questions from Specification

The specification marked 10 items as `[NEEDS CLARIFICATION]`. Based on our brainstorming session and technical constraints, here are the research findings:

### 1. Maximum Number of Queries Per Request (FR-003)

**Decision**: 50 queries maximum, 1 minimum, 10 default

**Rationale**:
- LM Studio response time increases linearly with query count
- Testing with 10 queries averages 5-8 seconds
- 50 queries estimated at 20-30 seconds (acceptable for async API)
- Above 50, diminishing returns on query diversity

**Alternatives Considered**:
- 100 maximum: Too slow, risk of timeout
- 25 maximum: Too restrictive for comprehensive coverage

**Configuration**:
```yaml
query-generation:
  max-queries-limit: 50
  min-queries-limit: 1
  default-queries: 10
```

---

### 2. High Quality Query Determination (FR-014)

**Decision**: Phase 1 - Persist all queries. Phase 2 - Track success metrics

**Rationale**:
- Initial implementation: No quality filtering, persist everything
- Future enhancement: Track search success rates, candidate counts
- "High quality" = queries that consistently find relevant candidates

**Alternatives Considered**:
- Manual curation: Too time-intensive, doesn't scale
- ML-based scoring: Premature, need data first

**Future Metrics**:
- Search success rate (% of queries finding candidates)
- Average candidates per query
- Candidate approval rate (human validation)

---

### 3. AI Generation Timeout (FR-016)

**Decision**: 30 seconds timeout

**Rationale**:
- LM Studio local inference typically completes in 5-15 seconds
- 30s allows for slower models or high query counts
- CompletableFuture allows easy timeout configuration
- Graceful fallback to hardcoded queries on timeout

**Alternatives Considered**:
- 10s: Too aggressive, causes false timeouts
- 60s: Too lenient, user experience degrades

**Implementation**:
```java
CompletableFuture<String> response = CompletableFuture
    .supplyAsync(() -> llmClient.generate(prompt))
    .orTimeout(30, TimeUnit.SECONDS)
    .exceptionally(ex -> getFallbackQueries());
```

---

### 4. Cache Hit Response Time Target (FR-021)

**Decision**: <50ms for cache hits

**Rationale**:
- Caffeine cache is in-memory, sub-millisecond lookup
- Serialization/deserialization adds minimal overhead
- 50ms provides comfortable margin
- Much faster than 100ms+ for LLM generation

**Alternatives Considered**:
- <100ms: Too lenient, cache should be fast
- <10ms: Achievable but unnecessarily strict

**Monitoring**: Log cache response times, alert if p95 > 50ms

---

### 5. Expected Cache Size (NFR-002)

**Decision**: 1000 unique query sets

**Rationale**:
- Cache key = provider + categories + geographic + query count
- Realistic combinations: 4 providers × 25 categories × 10 geographies × 2-3 query counts ≈ 2000-3000
- 1000 covers typical usage patterns (not all combinations used)
- Caffeine LRU eviction handles overflow

**Alternatives Considered**:
- 10,000: Excessive memory footprint
- 500: Too small, frequent evictions

**Memory Estimate**: ~1MB (1000 × ~1KB per entry)

---

### 6. Category/Geographic Mappings Configuration (NFR-004)

**Decision**: Code-based mappings (Java switch expressions)

**Rationale**:
- Mappings are **domain knowledge**, not user configuration
- Keywords and descriptions require careful curation
- Type safety ensures no missing categories
- Easier to version control and review

**Alternatives Considered**:
- YAML/Properties files: Lose type safety, harder to maintain
- Database configuration: Overkill, adds deployment complexity

**Implementation**: `CategoryMapper.java` and `GeographicMapper.java` with switch expressions

---

### 7. Expected AI Service Uptime (Dependencies)

**Decision**: 95% uptime expectation, graceful degradation required

**Rationale**:
- LM Studio runs locally on Mac Studio (not cloud)
- May be restarted for model changes or updates
- System must continue functioning with fallback queries

**Fallback Strategy**:
- Hardcoded queries per provider type
- Log AI unavailability for monitoring
- Retry logic (1-2 attempts with exponential backoff)

**Fallback Queries**:
```java
KEYWORD_FALLBACK = [
    "Bulgaria education grants",
    "EU education funding",
    "foundation school grants"
]

TAVILY_FALLBACK = [
    "Educational funding opportunities in Bulgaria and Eastern Europe"
]
```

---

### 8. Cache Retrieval Time Target (Success Criteria)

**Decision**: <50ms (same as FR-021)

**Rationale**: Consistency with functional requirement FR-021

---

### 9. Total Query Generation Time Target (Success Criteria)

**Decision**: <30s for 4 providers in parallel

**Rationale**:
- Single provider: 5-15s
- 4 providers in parallel (Virtual Threads): max(individual times) ≈ 15s
- 30s provides margin for slower LM Studio responses
- Async API makes this acceptable for user experience

**Monitoring**: Track p50, p95, p99 generation times per provider

---

### 10. Target Cache Hit Rate (Success Criteria)

**Decision**: >60% cache hit rate

**Rationale**:
- Discovery sessions often target same categories/regions
- Daily scheduler runs use predictable patterns
- 60% achievable with 24hr TTL
- Higher rates (70-80%) possible with usage patterns

**Monitoring**:
- Log cache hits/misses
- Calculate daily hit rate
- Alert if drops below 50%

---

### 11. Cache Eviction Policy (Edge Cases)

**Decision**: Time-based eviction (24hr TTL) + Size-based LRU

**Rationale**:
- Primary eviction: 24hr TTL (queries become stale)
- Secondary eviction: LRU when cache exceeds 1000 entries
- Caffeine handles both automatically

**Configuration**:
```java
Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(24, TimeUnit.HOURS)
    .build()
```

---

## Technology Research

### LangChain4j HTTP/1.1 Configuration

**Finding**: LangChain4j supports explicit HTTP version configuration

**Implementation**:
```java
HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .connectTimeout(Duration.ofSeconds(10));

JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
    .httpClientBuilder(httpClientBuilder);

ChatLanguageModel model = OpenAiChatModel.builder()
    .baseUrl("http://192.168.1.10:1234/v1")
    .apiKey("not-needed")
    .httpClientBuilder(jdkHttpClientBuilder)
    .build();
```

**Source**: [LangChain4j Customizable HTTP Client Docs](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/tutorials/customizable-http-client.md)

---

### Caffeine Cache Best Practices

**Finding**: Caffeine is the recommended cache for Java applications

**Key Features**:
- Near-optimal hit rate (W-TinyLFU eviction)
- Excellent performance (async loading, write-behind)
- Built-in stats collection

**Implementation**:
```java
Cache<QueryCacheKey, List<String>> cache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(24, TimeUnit.HOURS)
    .recordStats()  // Enable monitoring
    .build();

// Check stats
CacheStats stats = cache.stats();
double hitRate = stats.hitRate();
```

**Source**: [Caffeine Wiki](https://github.com/ben-manes/caffeine/wiki)

---

### Virtual Threads with CompletableFuture

**Finding**: Java 21+ Virtual Threads work seamlessly with CompletableFuture

**Configuration**:
```java
@Bean
public AsyncTaskExecutor asyncTaskExecutor() {
    return new TaskExecutorAdapter(
        Executors.newVirtualThreadPerTaskExecutor()
    );
}
```

**Benefits**:
- Millions of virtual threads without memory overhead
- Blocking I/O (LM Studio HTTP calls) doesn't block platform threads
- Perfect for parallel query generation

**Source**: [JEP 444 - Virtual Threads](https://openjdk.org/jeps/444)

---

### Spring Data JDBC with Existing Entities

**Finding**: Module can reuse `SearchQuery` entity from `northstar-persistence`

**Approach**:
- Depend on `northstar-persistence` module
- Use `SearchQueryRepository` for persistence
- No new domain entities needed

**Repository**:
```java
public interface SearchQueryRepository extends CrudRepository<SearchQuery, UUID> {
    List<SearchQuery> findBySessionId(UUID sessionId);
    List<SearchQuery> findBySearchEngine(SearchProvider provider);
}
```

---

## Summary of Decisions

| Question | Decision | Rationale |
|----------|----------|-----------|
| Max queries | 50 | Balance speed vs coverage |
| High quality criteria | Persist all (Phase 1) | Need data before ML scoring |
| AI timeout | 30s | Allows slow responses, has fallback |
| Cache response time | <50ms | Fast in-memory lookup |
| Cache size | 1000 entries | Covers typical usage patterns |
| Mappings config | Code-based | Type safety, domain knowledge |
| AI uptime | 95% | Local service, needs fallback |
| Total gen time | <30s | Parallel execution acceptable |
| Cache hit rate | >60% | Achievable with 24hr TTL |
| Eviction policy | Time + LRU | 24hr TTL primary, LRU secondary |

**All [NEEDS CLARIFICATION] items resolved** ✅

**Next**: Phase 1 Design & Contracts
