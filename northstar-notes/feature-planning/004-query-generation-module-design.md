# Feature 004: Query Generation Module Design

**Date**: 2025-11-02
**Status**: Design Complete, Ready for `/specify`
**Tags**: #feature-004 #planning #query-generation #lm-studio #langchain4j

---

## Overview

Design for the `northstar-query-generation` Maven module - a focused library that generates AI-powered search queries for funding source discovery using LM Studio (local LLM) via LangChain4j.

**Scope**: Query generation ONLY (no orchestrator in this feature)

---

## Brainstorming Session Summary

### Requirements Gathered

**Module Type**: Separate Maven library module (`northstar-query-generation`)
**LLM Infrastructure**: LM Studio (local) with LangChain4j configured for HTTP/1.1
**API Style**: Asynchronous (`CompletableFuture`) for Virtual Threads compatibility
**Caching**: Caffeine cache (in-memory, 24hr TTL) - CQEngine deferred for future taxonomy-based retrieval
**Persistence**: Selective async persistence to `search_queries` table ("high-quality" criteria TBD)
**Integration**: Library module called by future orchestrator, wired to crawler module

### Architectural Decisions

**Pattern**: Strategy pattern with provider-specific query generators
- `QueryGenerationStrategy` interface
- `KeywordQueryStrategy` (Brave, Serper, SearXNG)
- `TavilyQueryStrategy` (Tavily AI search)

**Key Technical Constraint**: LM Studio only supports HTTP/1.1 (not HTTP/2)
**Solution**: Configure LangChain4j's HttpClient explicitly for HTTP/1.1

### Reference Implementation

Springcrawler's `QueryGenerationService.java`:
```
/Users/kevin/github/springcrawler/archived-services/funding-discovery/src/main/java/org/northstar/fundingdiscovery/service/llm/QueryGenerationService.java
```

Successfully used LangChain4j with LM Studio for keyword vs AI-optimized query generation.

---

## Architecture

### Module Structure

```
northstar-query-generation/
├── pom.xml
└── src/main/java/com/northstar/funding/querygeneration/
    ├── config/
    │   ├── LmStudioConfig.java          # HTTP/1.1 configuration
    │   ├── CaffeineConfig.java          # Cache setup
    │   └── VirtualThreadConfig.java     # Async executor
    ├── strategy/
    │   ├── QueryGenerationStrategy.java # Interface
    │   ├── KeywordQueryStrategy.java    # For traditional search
    │   └── TavilyQueryStrategy.java     # For AI search
    ├── service/
    │   ├── QueryGenerationService.java  # Main facade
    │   └── QueryCacheService.java       # Cache + persistence
    ├── model/
    │   ├── QueryGenerationRequest.java
    │   ├── QueryGenerationResponse.java
    │   ├── QueryCacheKey.java
    │   └── SearchProvider.java (enum)
    ├── template/
    │   ├── PromptTemplates.java         # LangChain4j templates
    │   ├── CategoryMapper.java          # FundingSearchCategory mappings
    │   └── GeographicMapper.java        # GeographicScope mappings
    └── exception/
        └── QueryGenerationException.java
```

### Dependencies

**Internal**:
- `northstar-domain` (entities, enums)
- `northstar-persistence` (repositories)

**External**:
- Spring Boot 3.5.6
- LangChain4j (with JDK HTTP client)
- Caffeine cache
- Vavr (error handling)
- Lombok (model objects)

---

## Core Design

### 1. Strategy Pattern

```java
public interface QueryGenerationStrategy {
    CompletableFuture<List<String>> generateQueries(
        Set<FundingSearchCategory> categories,
        GeographicScope geographic,
        int maxQueries
    );

    SearchProvider getProvider();
    String getQueryType(); // "keyword" or "ai-optimized"
}
```

**KeywordQueryStrategy**: Keyword queries for Brave, Serper, SearXNG
**TavilyQueryStrategy**: AI-optimized natural language queries for Tavily

### 2. LM Studio Configuration (CRITICAL)

```java
@Configuration
public class LmStudioConfig {
    @Bean
    public ChatLanguageModel lmStudioModel(
            @Value("${lmstudio.base-url}") String baseUrl) {

        // Force HTTP/1.1 - LM Studio doesn't support HTTP/2
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10));

        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)  // http://192.168.1.10:1234/v1
            .apiKey("not-needed")
            .httpClientBuilder(JdkHttpClient.builder()
                .httpClientBuilder(httpClientBuilder))
            .build();
    }
}
```

**Why HTTP/1.1?** LM Studio HTTP/2 compatibility issues cause `java.io.IOException: Expected a SETTINGS frame but was WINDOW_UPDATE` errors.

### 3. Prompt Templates

Using LangChain4j `PromptTemplate` with variable substitution:

**Keyword Template** (for traditional search engines):
```
Generate precise keyword search queries for traditional search engines...
Target Categories: {{categories}}
Geographic Focus: {{geographic}}
Requirements:
1. Generate {{maxQueries}} specific keyword combinations
2. Use exact terms that appear on funding websites
3. Include location keywords when relevant
...
Return ONLY the keyword queries, one per line.
```

**Tavily Template** (for AI-powered search):
```
Generate intelligent, AI-optimized search queries for Tavily...
Target Categories: {{categories}}
Geographic Focus: {{geographic}}
Requirements:
1. Generate {{maxQueries}} conceptual, context-rich queries
2. Use natural language expressing funding goals
3. Include contextual details for AI understanding
...
Return only the AI-optimized queries, one per line.
```

### 4. Category & Geographic Mapping

**CategoryMapper**:
- `toKeywords()`: "infrastructure grants, facility funding"
- `toConceptualDescription()`: "educational infrastructure development and facility modernization"

**GeographicMapper**:
- `toKeywords()`: "Bulgaria"
- `toConceptualDescription()`: "Bulgaria, with focus on post-transition educational development"

Supports all 25 `FundingSearchCategory` values and all `GeographicScope` values.

### 5. Caching Strategy

**Caffeine Configuration**:
```java
@Bean
public Cache<QueryCacheKey, List<String>> queryCache() {
    return Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .recordStats()
        .build();
}
```

**Cache Key**:
```java
@Value @Builder
public class QueryCacheKey {
    SearchProvider provider;
    Set<FundingSearchCategory> categories;
    GeographicScope geographic;
    int maxQueries;
}
```

**Lifecycle**:
1. Check cache before LLM call
2. Cache result for 24 hours
3. Persist to PostgreSQL asynchronously (non-blocking)
4. Track cache stats for monitoring

**Future**: CQEngine for taxonomy-based query retrieval (daily schedules: regional searches, infrastructure searches, etc.)

### 6. Service Facade

**QueryGenerationService** - Main API:

```java
@Service
public class QueryGenerationService {

    // Single provider
    public CompletableFuture<QueryGenerationResponse> generateQueries(
        QueryGenerationRequest request
    );

    // Multiple providers in parallel
    public CompletableFuture<Map<SearchProvider, List<String>>>
        generateForMultipleProviders(
            List<SearchProvider> providers,
            Set<FundingSearchCategory> categories,
            GeographicScope geographic,
            int maxQueries,
            UUID sessionId
        );
}
```

---

## Data Flow

### Single Provider Generation

```
1. Caller creates QueryGenerationRequest
2. QueryGenerationService checks Caffeine cache
3. Cache hit? → Return cached queries immediately
4. Cache miss? → Delegate to appropriate strategy
5. Strategy builds prompt from template + category/geographic mappings
6. LM Studio generates queries via LangChain4j (HTTP/1.1)
7. Parse LLM response into List<String>
8. Cache queries (Caffeine, 24hr TTL)
9. Persist queries asynchronously to PostgreSQL
10. Return QueryGenerationResponse with CompletableFuture
```

### Multi-Provider Parallel Generation

```
1. Create CompletableFuture for each provider
2. All execute in parallel on Virtual Threads
3. Each follows single-provider flow independently
4. CompletableFuture.allOf() waits for all
5. Return Map<SearchProvider, List<String>>
```

---

## Integration Points

### How Future Orchestrator Will Use This

```java
// In future northstar-orchestrator or northstar-application
@Service
public class FundingDiscoveryOrchestrator {

    private final QueryGenerationService queryGenerationService;
    private final SearchExecutor searchExecutor; // from crawler module

    public CompletableFuture<DiscoveryResults> runDiscovery(
            Set<FundingSearchCategory> categories,
            GeographicScope geographic,
            UUID sessionId) {

        // Generate queries for all providers (parallel)
        CompletableFuture<Map<SearchProvider, List<String>>> queries =
            queryGenerationService.generateForMultipleProviders(
                List.of(BRAVE_SEARCH, SERPER, SEARXNG, TAVILY),
                categories,
                geographic,
                10, // maxQueries per provider
                sessionId
            );

        // Execute searches when queries ready
        return queries.thenCompose(q ->
            searchExecutor.executeSearches(q, sessionId)
        );
    }
}
```

### Dependencies

**Depends on**:
- `northstar-domain` - `FundingSearchCategory`, `GeographicScope`, `SearchQuery` entity
- `northstar-persistence` - `SearchQueryRepository`

**Depended on by** (future):
- `northstar-orchestrator` - will orchestrate query generation + search execution
- `northstar-application` - will configure and run the application

---

## Error Handling

### LM Studio Connection Failures

```java
strategy.generateQueries(...)
    .exceptionally(throwable -> {
        log.error("LM Studio query generation failed", throwable);
        return getFallbackQueries(provider);
    });
```

### Fallback Queries

If LM Studio unavailable:
- **Keyword**: `["Bulgaria education grants", "EU education funding"]`
- **Tavily**: `["Educational funding opportunities in Bulgaria"]`

### Validation

- Validate `maxQueries` in range 1-50
- Validate non-empty categories
- Validate non-null geographic scope
- Parse failures → log and return empty list

---

## Testing Strategy

### Unit Tests
- Mock `ChatLanguageModel` to test strategies
- Mock strategies to test service orchestration
- Test category/geographic mappings (pure functions)
- Test cache hits/misses, expiration

### Integration Tests (Future)
- TestContainers for PostgreSQL
- WireMock for LM Studio HTTP endpoint
- End-to-end: request → cache → LLM → parse → persist

**Coverage Goals**:
- 80%+ on service layer
- 100% on mappers
- All strategies tested with mocked LLM

---

## Configuration

### application.yml

```yaml
lmstudio:
  base-url: http://192.168.1.10:1234/v1
  timeout-seconds: 30

query-generation:
  max-queries-limit: 50
  min-queries-limit: 1
  default-queries: 10

spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=24h
```

---

## Maven Dependencies

```xml
<!-- LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-http-client-jdk</artifactId>
</dependency>

<!-- Caffeine Cache -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- Vavr -->
<dependency>
    <groupId>io.vavr</groupId>
    <artifactId>vavr</artifactId>
</dependency>
```

---

## Future Enhancements

### Phase 2: Query Quality Tracking
- Track search success rates per query
- ML-based quality scoring
- Automatic query refinement

### Phase 3: CQEngine Integration
- Add CQEngine for taxonomy-based retrieval
- Support daily schedules (Monday: regional, Tuesday: infrastructure, etc.)
- "Find similar queries" functionality
- Query library management

### Phase 4: Advanced Features
- Query templates library (user-defined)
- A/B testing of query variations
- Query performance analytics
- Multi-language query generation

---

## Success Criteria

1. ✅ Generate keyword queries for Brave, Serper, SearXNG
2. ✅ Generate AI-optimized queries for Tavily
3. ✅ Async API returns `CompletableFuture`
4. ✅ Queries cached for 24 hours (Caffeine)
5. ✅ Queries persisted to PostgreSQL
6. ✅ LM Studio HTTP/1.1 compatibility (no HTTP/2 errors)
7. ✅ Unit tests with 80%+ coverage
8. ✅ Parallel query generation for multiple providers
9. ✅ Graceful fallback when LM Studio unavailable

---

## Next Steps

1. **Run `/specify`** with this design as input to create formal spec
2. **Use spec-kit workflow**: `/specify` → `/plan` → `/tasks` → `/implement`
3. **Set up git worktree** for isolated development
4. **Write session summary** after implementation complete

---

## References

- **Springcrawler reference**: `/Users/kevin/github/springcrawler/archived-services/funding-discovery/src/main/java/org/northstar/fundingdiscovery/service/llm/QueryGenerationService.java`
- **LangChain4j docs**: https://github.com/langchain4j/langchain4j
- **Caffeine cache**: https://github.com/ben-manes/caffeine
- **Java Virtual Threads**: JEP 444

---

**Last Updated**: 2025-11-02
**Brainstorming Session**: Complete
**Ready for**: `/specify` command with feature description
