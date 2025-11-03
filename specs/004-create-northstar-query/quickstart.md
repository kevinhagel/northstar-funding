# Quickstart: AI-Powered Search Query Generation

**Feature**: 004-create-northstar-query
**Date**: 2025-11-02
**Purpose**: Integration test scenarios that validate user stories from spec.md

---

## Prerequisites

- LM Studio running at http://192.168.1.10:1234/v1
- PostgreSQL 16 at 192.168.1.10:5432 with `northstar_funding` database
- Maven project compiled successfully
- Required dependencies: Spring Boot, LangChain4j, Caffeine, Vavr

---

## Test Scenario 1: Single Provider Query Generation

**User Story**: Generate keyword queries for Brave Search targeting scholarships in Bulgaria

**Setup**:
```java
// Arrange
QueryGenerationRequest request = QueryGenerationRequest.builder()
    .provider(SearchProvider.BRAVE_SEARCH)
    .categories(Set.of(FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS))
    .geographic(GeographicScope.BULGARIA)
    .maxQueries(5)
    .sessionId(UUID.randomUUID())
    .build();
```

**Execute**:
```java
// Act
CompletableFuture<QueryGenerationResponse> future =
    queryGenerationService.generateQueries(request);

QueryGenerationResponse response = future.get(30, TimeUnit.SECONDS);
```

**Validate**:
```java
// Assert
assertThat(response.getProvider()).isEqualTo(SearchProvider.BRAVE_SEARCH);
assertThat(response.getQueries()).hasSize(5);
assertThat(response.getQueries()).allMatch(q ->
    q.contains("Bulgaria") || q.contains("scholarship")
);
assertThat(response.isFromCache()).isFalse(); // First call
assertThat(response.getGeneratedAt()).isNotNull();
```

**Expected Output**:
```
Queries: [
  "Bulgaria education scholarships",
  "student grants Bulgaria funding",
  "scholarship programs Eastern Europe",
  "Bulgarian university financial aid",
  "study abroad scholarships Bulgaria"
]
fromCache: false
generatedAt: 2025-11-02T10:30:45Z
```

---

## Test Scenario 2: Cache Hit on Repeat Request

**User Story**: Same request within 24 hours returns cached queries

**Setup**:
```java
// First request (same as Scenario 1)
QueryGenerationResponse firstResponse =
    queryGenerationService.generateQueries(request).get();

// Wait briefly to ensure cache is populated
Thread.sleep(100);
```

**Execute**:
```java
// Second identical request
QueryGenerationResponse secondResponse =
    queryGenerationService.generateQueries(request).get();
```

**Validate**:
```java
// Assert
assertThat(secondResponse.isFromCache()).isTrue();
assertThat(secondResponse.getQueries()).isEqualTo(firstResponse.getQueries());
assertThat(Duration.between(
    secondResponse.getGeneratedAt(),
    Instant.now()
)).isLessThan(Duration.ofMillis(50)); // Fast cache retrieval
```

**Expected Behavior**:
- LM Studio NOT called second time
- Response time <50ms
- Identical queries returned

---

## Test Scenario 3: Keyword vs AI-Optimized Queries

**User Story**: Tavily receives AI-optimized natural language queries

**Setup**:
```java
UUID sessionId = UUID.randomUUID();

// Keyword query request (Brave Search)
QueryGenerationRequest keywordRequest = QueryGenerationRequest.builder()
    .provider(SearchProvider.BRAVE_SEARCH)
    .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
    .geographic(GeographicScope.BULGARIA)
    .maxQueries(3)
    .sessionId(sessionId)
    .build();

// AI-optimized query request (Tavily)
QueryGenerationRequest aiRequest = QueryGenerationRequest.builder()
    .provider(SearchProvider.TAVILY)
    .categories(Set.of(FundingSearchCategory.INFRASTRUCTURE_FUNDING))
    .geographic(GeographicScope.BULGARIA)
    .maxQueries(3)
    .sessionId(sessionId)
    .build();
```

**Execute**:
```java
QueryGenerationResponse keywordResponse =
    queryGenerationService.generateQueries(keywordRequest).get();

QueryGenerationResponse aiResponse =
    queryGenerationService.generateQueries(aiRequest).get();
```

**Validate**:
```java
// Keyword queries: concise, keyword-focused
assertThat(keywordResponse.getQueries()).allMatch(q ->
    q.split(" ").length < 10 && // Short queries
    q.matches(".*\\b(infrastructure|grants|funding)\\b.*") // Keywords present
);

// AI-optimized queries: longer, contextual
assertThat(aiResponse.getQueries()).allMatch(q ->
    q.split(" ").length > 10 && // Longer queries
    q.contains("Bulgaria") && // Context included
    (q.contains("educational") || q.contains("development")) // Conceptual
);
```

**Expected Output**:
```
Keyword queries:
- "Bulgaria infrastructure grants funding"
- "EU structural funds facilities Bulgaria"
- "educational building renovation grants"

AI-optimized queries:
- "Educational infrastructure funding opportunities for modernizing schools in Bulgaria and Eastern European transition economies"
- "Grants and programs supporting facility construction and renovation for educational institutions in post-communist Bulgaria"
- "Infrastructure development funding for educational facilities addressing regional development needs in Bulgaria"
```

---

## Test Scenario 4: Multi-Provider Parallel Generation

**User Story**: Generate queries for all 4 providers simultaneously

**Setup**:
```java
UUID sessionId = UUID.randomUUID();
Set<FundingSearchCategory> categories = Set.of(
    FundingSearchCategory.TEACHER_DEVELOPMENT
);
GeographicScope geographic = GeographicScope.EASTERN_EUROPE;
```

**Execute**:
```java
Instant start = Instant.now();

CompletableFuture<Map<SearchProvider, List<String>>> future =
    queryGenerationService.generateForMultipleProviders(
        List.of(
            SearchProvider.BRAVE_SEARCH,
            SearchProvider.SERPER,
            SearchProvider.SEARXNG,
            SearchProvider.TAVILY
        ),
        categories,
        geographic,
        5, // maxQueries per provider
        sessionId
    );

Map<SearchProvider, List<String>> results = future.get(30, TimeUnit.SECONDS);
Duration totalTime = Duration.between(start, Instant.now());
```

**Validate**:
```java
// Assert all providers returned queries
assertThat(results).hasSize(4);
assertThat(results.get(SearchProvider.BRAVE_SEARCH)).hasSize(5);
assertThat(results.get(SearchProvider.SERPER)).hasSize(5);
assertThat(results.get(SearchProvider.SEARXNG)).hasSize(5);
assertThat(results.get(SearchProvider.TAVILY)).hasSize(5);

// Assert parallel execution (not sequential)
assertThat(totalTime).isLessThan(Duration.ofSeconds(30)); // Max of individual times, not sum
```

**Expected Behavior**:
- All 4 providers execute in parallel (Virtual Threads)
- Total time ≈ max(individual times), not sum
- Each provider gets appropriate query style

---

## Test Scenario 5: AI Service Unavailable - Fallback Queries

**User Story**: System provides fallback queries when LM Studio is down

**Setup**:
```java
// Stop LM Studio or block network to 192.168.1.10:1234
// (In real test, mock the LangChain4j client to throw exception)

QueryGenerationRequest request = QueryGenerationRequest.builder()
    .provider(SearchProvider.BRAVE_SEARCH)
    .categories(Set.of(FundingSearchCategory.STEM_EDUCATION))
    .geographic(GeographicScope.EU_MEMBER_STATES)
    .maxQueries(3)
    .sessionId(UUID.randomUUID())
    .build();
```

**Execute**:
```java
QueryGenerationResponse response =
    queryGenerationService.generateQueries(request)
        .get(30, TimeUnit.SECONDS);
```

**Validate**:
```java
// Assert fallback queries returned
assertThat(response.getQueries()).isNotEmpty();
assertThat(response.getQueries()).contains(
    "Bulgaria education grants", // Hardcoded fallback
    "EU education funding"
);
assertThat(response.isFromCache()).isFalse();
```

**Expected Behavior**:
- No exception thrown
- Fallback queries returned
- System continues functioning
- Error logged for monitoring

---

## Test Scenario 6: Query Persistence Verification

**User Story**: Generated queries are saved to database for analysis

**Setup**:
```java
UUID sessionId = UUID.randomUUID();

QueryGenerationRequest request = QueryGenerationRequest.builder()
    .provider(SearchProvider.SERPER)
    .categories(Set.of(FundingSearchCategory.PROGRAM_GRANTS))
    .geographic(GeographicScope.BALKANS)
    .maxQueries(3)
    .sessionId(sessionId)
    .build();
```

**Execute**:
```java
QueryGenerationResponse response =
    queryGenerationService.generateQueries(request).get();

// Wait for async persistence to complete
Thread.sleep(1000);
```

**Validate**:
```java
// Query database for persisted queries
List<SearchQuery> persistedQueries =
    searchQueryRepository.findBySessionId(sessionId);

assertThat(persistedQueries).hasSize(3);
assertThat(persistedQueries).allMatch(sq ->
    sq.getSearchEngine() == SearchEngineType.SERPER &&
    sq.getSessionId().equals(sessionId) &&
    sq.getGeneratedAt() != null &&
    sq.getCategories().contains(FundingSearchCategory.PROGRAM_GRANTS) &&
    sq.getGeographicScope() == GeographicScope.BALKANS
);
```

**Expected Database State**:
```sql
SELECT query_text, search_engine, session_id, generated_at
FROM search_queries
WHERE session_id = '...';

-- Results:
-- "Balkans program grants funding opportunities" | SERPER | ... | 2025-11-02 10:45:30
-- "educational program development grants Southeastern Europe" | SERPER | ... | 2025-11-02 10:45:30
-- "NGO program funding Balkans region" | SERPER | ... | 2025-11-02 10:45:30
```

---

## Test Scenario 7: Cache Statistics Monitoring

**User Story**: Monitor cache hit rate and performance

**Setup**:
```java
queryGenerationService.clearCache(); // Start fresh

// Make 10 requests, 5 unique, 5 duplicates
for (int i = 0; i < 5; i++) {
    QueryGenerationRequest request = createRequest(i % 3); // 3 unique patterns
    queryGenerationService.generateQueries(request).get();

    // Duplicate request
    queryGenerationService.generateQueries(request).get();
}
```

**Execute**:
```java
Map<String, Object> stats = queryGenerationService.getCacheStatistics();
```

**Validate**:
```java
assertThat(stats.get("totalRequests")).isEqualTo(10);
assertThat(stats.get("cacheHits")).isEqualTo(5); // 50% duplicates
assertThat(stats.get("cacheMisses")).isEqualTo(5);
assertThat((Double) stats.get("hitRate")).isCloseTo(0.5, Offset.offset(0.01));
assertThat(stats.get("cacheSize")).isEqualTo(3); // 3 unique patterns cached
```

**Expected Output**:
```json
{
  "totalRequests": 10,
  "cacheHits": 5,
  "cacheMisses": 5,
  "hitRate": 0.5,
  "cacheSize": 3,
  "evictions": 0
}
```

---

## Performance Benchmarks

| Scenario | Target | Measurement |
|----------|--------|-------------|
| Cache hit retrieval | <50ms | Response time from cache |
| Single provider generation | 5-15s | LLM call + parse |
| 4 providers parallel | <30s | Max(individual times) |
| Cache statistics query | <10ms | Immediate |
| Persistence (async) | Non-blocking | Fire-and-forget |

---

## Manual Testing Steps

### 1. Verify LM Studio Connection

```bash
# Check LM Studio is running
curl http://192.168.1.10:1234/v1/models

# Expected: JSON response with model info
```

### 2. Run Integration Tests

```bash
# From project root
mvn test -Dtest=QueryGenerationServiceIntegrationTest

# Expected: All scenarios pass
```

### 3. Monitor Logs

```bash
# Watch for key log messages:
# - "🎯 Generating provider-specific queries..."
# - "✅ [PROVIDER] Generated X queries"
# - "📝 Cached queries for key: ..."
# - "💾 Persisting queries to database"
```

### 4. Verify Database Persistence

```sql
-- Check queries were saved
SELECT COUNT(*) FROM search_queries;

-- Check recent queries
SELECT query_text, search_engine, generated_at
FROM search_queries
ORDER BY generated_at DESC
LIMIT 10;
```

---

## Troubleshooting

### LM Studio Connection Fails

**Symptom**: Timeout or connection refused errors

**Solution**:
1. Verify LM Studio running: `curl http://192.168.1.10:1234/v1/models`
2. Check HTTP/1.1 configuration in `LmStudioConfig`
3. Review LM Studio logs for errors
4. Fallback queries should still work

### Queries Not Cached

**Symptom**: Every request hits LM Studio

**Solution**:
1. Check Caffeine configuration in `application.yml`
2. Verify `QueryCacheKey` equals/hashCode implementation
3. Check cache not being cleared between requests
4. Review cache statistics

### Database Persistence Fails

**Symptom**: Queries generated but not in database

**Solution**:
1. Check PostgreSQL connection at 192.168.1.10:5432
2. Verify `SearchQuery` entity mapping
3. Check Flyway migrations applied (V10, V17)
4. Review async task executor configuration

---

## Success Criteria

✅ All 7 test scenarios pass
✅ Cache hit rate >60% for typical patterns
✅ Parallel generation completes in <30s
✅ Fallback queries work when LM Studio unavailable
✅ Queries persisted to database
✅ No memory leaks (cache eviction works)
✅ Thread-safe under concurrent load

---

**Next**: Execute these scenarios in `/implement` phase with actual test code
