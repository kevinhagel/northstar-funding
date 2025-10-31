# Session Summary: Archived Search Infrastructure Analysis

**Date**: 2025-10-30
**Context**: Post-corruption recovery - examining Spring Crawler archived search patterns
**Branch**: 003-search-execution-infrastructure

## Overview

Examined the archived HTTP-based search service infrastructure from the old Spring Crawler project at:
`/Users/kevin/github/springcrawler/archived-services/funding-discovery/src/main/java/org/northstar/fundingdiscovery/service/search`

This analysis captures proven patterns and architectural decisions for potential reuse in NorthStar Funding.

## Key Components Discovered

### 1. Search Engine Adapters (4 implementations)

**BraveSearchService.java**
- Uses Spring `RestTemplate` for HTTP calls
- `@RefreshScope` for dynamic configuration updates
- Header-based API authentication: `X-Subscription-Token`
- URL building with `UriComponentsBuilder`
- Vavr `Try` for functional error handling
- Domain extraction using shared `DomainUtils`
- Quality signals: educational keywords, geographic relevance

**SearXNGSearchService.java**
- Multi-engine aggregation (Google, Bing, DuckDuckGo)
- No API key required (local instance)
- JSON format response parsing
- Same domain extraction and quality signal patterns

**TavilySearchService.java** (most sophisticated)
- JDK `HttpClient` with Virtual Threads (Java 21+)
- JSON request/response with `ObjectMapper`
- AI-powered search with confidence scoring
- Enhanced description building (combines content + AI analysis)
- Provider-specific request models (`TavilyRequest`, `TavilyResponse`)
- Status checking and health monitoring
- Connection testing capability

**SerperSearchService.java**
- Google Search API wrapper (cost-efficient: $1/1000 queries)
- Micrometer metrics integration for timing
- Cost tracking and analysis (vs other providers)
- Request body JSON format (POST endpoint)
- Usage statistics and quota monitoring

### 2. Intelligent Provider Selection

**SearchProviderSelector.java** - Brain of the system

**Key Features:**
- Query analysis for complexity, specificity, geographic focus
- Provider selection based on query characteristics
- Cost-aware optimization (quality vs speed vs price)
- Centralized configuration via `FundingDiscoveryConfig`
- Strategy matrix documentation:
  - Tavily: AI-powered deep discovery, premium quality
  - Brave: Reliable web search, good metadata
  - Serper: Fast Google results, cost-efficient
  - SearXNG: High volume, local fallback

**Selection Logic:**
```java
private boolean shouldUseTavily(QueryCharacteristics characteristics) {
    return characteristics.getComplexity() >= 0.6 ||
           characteristics.getSpecificity() >= 0.7 ||
           characteristics.isRequiresDeepSearch() ||
           config.getSearchStrategyPreferQuality();
}
```

**Complexity Calculation:**
- Multiple funding terms: +0.15 each (max 0.5)
- Eligibility/criteria keywords: +0.2
- Deadline/application keywords: +0.2
- Research/academic keywords: +0.15

**Specificity Calculation:**
- Specific categories (infrastructure, scholarships): +0.2-0.3
- STEM/science keywords: +0.2
- Education/school keywords: +0.2
- Building/infrastructure keywords: +0.3

### 3. Multi-Provider Orchestration

**FundingSearchOrchestrator.java** - Execution coordinator

**Architecture Pattern:**
1. **Intelligent Provider Selection** - Use selector to pick optimal engines
2. **Provider-Specific Query Generation** - Different query types per provider
   - Tavily: Natural language, conceptual queries
   - Keyword providers: Precise keyword combinations
3. **Parallel Execution** - `CompletableFuture` with 45-second timeout
4. **Quality Filtering** - Domain validation, content checks, deduplication
5. **Ranking** - Quality score calculation with provider weights

**Quality Score Formula** (0.0-1.0):
- Provider weight: Tavily (0.4), Brave (0.3), Serper (0.25), SearXNG (0.2)
- Content length: >200 chars (+0.2), >100 chars (+0.1)
- Funding keywords: grant/fund (+0.2), application/eligibility (+0.15), deadline/scholarship (+0.1)
- Title quality: >20 chars (+0.1)
- Domain quality: .edu/.gov/europa.eu (+0.15), foundation/fund (+0.1)

**Parallel Search Pattern:**
```java
List<CompletableFuture<List<SearchCandidate>>> futures = providerQueries.entrySet().stream()
    .map(entry -> executeProviderQueriesAsync(entry.getKey(), entry.getValue(), maxResults))
    .toList();

for (CompletableFuture<List<SearchCandidate>> future : futures) {
    List<SearchCandidate> candidates = future.get(45, TimeUnit.SECONDS);
    allCandidates.addAll(candidates);
}
```

## Common Patterns Across All Adapters

### 1. Configuration Management
- Centralized `FundingDiscoveryConfig` (not scattered `@Value` annotations)
- `@RefreshScope` for runtime config updates (Spring Cloud Config)
- `@PostConstruct` logging for initialization verification
- Configuration validation (`isConfigured()` methods)

### 2. Error Handling
- Vavr `Try` monad for functional error handling
- Graceful degradation (return empty list on failure)
- Detailed logging with emojis for visibility
- Circuit breakers (implied by Resilience4j in other parts)

### 3. Domain Extraction
- Shared `DomainUtils.extractDomain(url)` utility
- Validation checks: non-null, non-empty, not "unknown"
- Domain quality signals (.edu, .gov, europa.eu)

### 4. Content Quality Signals
- Educational keywords: education, university, grant, scholarship, etc.
- Geographic relevance: Bulgaria, Eastern Europe, EU, international
- Content length thresholds for quality filtering

### 5. Response Parsing
- Type-safe extraction with null checks
- Text cleaning: HTML tag removal, whitespace normalization
- Structured result models (`SearchCandidate`)
- Provider-specific metadata tracking

### 6. Observability
- Extensive logging with structured context
- Timing metrics (Micrometer for Serper)
- Success/failure counters
- Cost tracking (Serper example)
- Sample result logging for verification

## Architecture Decisions to Consider

### 1. HTTP Client Choice

**Spring RestTemplate** (Brave, SearXNG, Serper):
- ✅ Spring ecosystem integration
- ✅ Mature, well-documented
- ✅ Easy interceptor support
- ❌ Synchronous by nature
- ❌ Legacy (Spring recommends WebClient)

**JDK HttpClient** (Tavily):
- ✅ Modern Java 11+ feature
- ✅ Virtual Thread support (Java 21+)
- ✅ Async by design
- ✅ No external dependencies
- ❌ More verbose configuration
- ❌ Less Spring integration

**Our Current Approach (NorthStar Funding):**
- Spring RestClient (Spring 6.1+ replacement for RestTemplate)
- WebFlux WebClient for async operations
- Need to decide: Migrate to JDK HttpClient for Virtual Thread benefits?

### 2. Provider Selection Strategy

**Archived Approach:**
- Query complexity analysis (0.0-1.0 scores)
- Category-based selection
- Geographic scope optimization
- Cost-aware quality balance

**NorthStar Current:**
- Query library with target engines (YAML configuration)
- All configured engines run in parallel
- No intelligent selection (yet)

**Gap:** We don't have the intelligent provider selector yet. Consider adapting `SearchProviderSelector` pattern.

### 3. Quality Scoring & Ranking

**Archived Approach:**
- Multi-factor quality scores (provider, content, domain)
- Weighted ranking formula
- Deduplication before ranking

**NorthStar Current:**
- Metadata-based judging with confidence scores
- No post-search ranking/filtering
- Domain deduplication only

**Gap:** We could benefit from quality scoring for result prioritization.

### 4. Observability & Cost Tracking

**Archived Approach:**
- Detailed usage statistics
- Cost analysis per provider
- Success/failure ratios
- Performance timing

**NorthStar Current:**
- Basic logging
- Circuit breaker metrics
- Session statistics

**Gap:** Add cost tracking, especially if we adopt paid APIs.

## Key Differences: Archived vs NorthStar

| Aspect | Archived Spring Crawler | NorthStar Funding |
|--------|-------------------------|-------------------|
| **Configuration** | Centralized `FundingDiscoveryConfig` | YAML query library + properties |
| **Provider Selection** | Intelligent `SearchProviderSelector` | Static YAML config |
| **Concurrency** | `CompletableFuture` | Virtual Threads |
| **HTTP Client** | Mixed (RestTemplate, JDK HttpClient) | RestClient, WebFlux |
| **Error Handling** | Vavr `Try` | Vavr `Try` + Circuit Breakers |
| **Quality Scoring** | Multi-factor ranking | Metadata judging only |
| **Cost Tracking** | Per-provider cost analysis | Not implemented |
| **Query Generation** | Provider-specific queries | LLM-based query generation |

## Patterns Worth Adopting

### 1. ✅ Intelligent Provider Selection
The `SearchProviderSelector` pattern is sophisticated and battle-tested. Consider implementing similar query analysis for dynamic engine selection.

### 2. ✅ Quality Score Ranking
Post-search quality filtering and ranking could improve candidate quality before metadata judging.

### 3. ✅ Cost Tracking
If we add paid APIs (Tavily, Serper, Brave), adopt the cost tracking pattern from `SerperSearchService`.

### 4. ✅ Provider-Specific Query Optimization
Generate different query types for different engines (natural language for AI, keywords for traditional).

### 5. ✅ Enhanced Observability
The detailed logging with emojis, timing metrics, and sample result logging is excellent for debugging.

### 6. ⚠️ HTTP Client Consolidation
Consider migrating to JDK HttpClient with Virtual Threads for consistency and performance.

## Patterns to Avoid/Modify

### 1. ❌ RestTemplate (Legacy)
Spring recommends RestClient or WebClient. Don't introduce RestTemplate to new code.

### 2. ❌ Synchronous Orchestration
The archived `FundingSearchOrchestrator` uses blocking `CompletableFuture.get()` calls. Our Virtual Thread approach is better.

### 3. ⚠️ Static Success/Failure Counters
The `static int searchCount` pattern in `SerperSearchService` is not thread-safe. Use Micrometer or atomic counters.

### 4. ⚠️ Scattered Quality Logic
Quality keyword lists are duplicated across services. Centralize in a shared utility.

## Integration Opportunities

### Short-Term (Feature 003 completion)
1. Add quality score calculation to `MetadataJudgingService`
2. Implement cost tracking if using paid APIs
3. Enhance logging with structured context (emojis optional)

### Medium-Term (Feature 005+)
1. Implement intelligent provider selection based on query analysis
2. Add provider-specific query optimization
3. Post-search ranking before metadata judging

### Long-Term (Performance optimization)
1. Consider JDK HttpClient migration for Virtual Thread benefits
2. Implement adaptive provider selection (learn from results)
3. Add A/B testing for search strategies

## Code Locations

**Archived Project:**
```
/Users/kevin/github/springcrawler/archived-services/funding-discovery/src/main/java/org/northstar/fundingdiscovery/service/search/
├── SearchProviderSelector.java       (intelligent selection)
├── FundingSearchOrchestrator.java    (parallel execution)
├── BraveSearchService.java           (RestTemplate example)
├── SearXNGSearchService.java         (multi-engine aggregation)
├── TavilySearchService.java          (JDK HttpClient + Virtual Threads)
├── SerperSearchService.java          (cost tracking example)
└── tavily/                           (provider-specific models)
    ├── TavilyRequest.java
    └── TavilyResponse.java
```

**NorthStar Funding (current):**
```
backend/src/main/java/com/northstar/funding/discovery/search/
├── application/
│   ├── SearchExecutionService.java   (our orchestrator)
│   └── NightlyDiscoveryScheduler.java
└── infrastructure/adapters/
    ├── SearxngAdapter.java
    ├── TavilyAdapter.java
    └── PerplexityAdapter.java
```

## Related Documentation

- [[003-search-execution-infrastructure]] - Current feature implementation
- `CLAUDE.md` - Project conventions and patterns
- `specs/003-search-execution-infrastructure/COMPLETION-SUMMARY.md` - Feature 003 docs

## Next Steps

1. ✅ **Document findings** - This session summary
2. ⏭️ **Evaluate adoption** - Discuss which patterns to integrate
3. ⏭️ **Plan implementation** - Create feature specs for adopted patterns
4. ⏭️ **Update CLAUDE.md** - Document new patterns if adopted

## Key Takeaways

The archived Spring Crawler search infrastructure is **production-quality and battle-tested**. Key strengths:

1. **Intelligent provider selection** based on query analysis
2. **Quality-first result ranking** with multi-factor scoring
3. **Cost-aware optimization** for paid APIs
4. **Excellent observability** with detailed logging and metrics
5. **Functional error handling** with graceful degradation

We can selectively adopt these patterns without wholesale migration. The biggest opportunities:

- **Short-term:** Quality scoring for metadata judging
- **Medium-term:** Intelligent provider selection
- **Long-term:** Provider-specific query optimization

The HTTP client choice (RestTemplate vs JDK HttpClient vs WebClient) deserves careful consideration based on Virtual Thread support and Spring integration needs.
