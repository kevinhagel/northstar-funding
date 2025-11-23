# NorthStar Search Adapters

Search engine adapters for automated funding opportunity discovery.

## Overview

This module provides a unified interface for querying multiple search engines and orchestrating the complete search workflow for funding discovery.

**Implemented Adapters:**
- ✅ Brave Search API
- ✅ SearXNG (self-hosted)
- ⏳ Serper.dev (Google Search) - not yet implemented
- ⏳ Tavily AI Search - not yet implemented

**Key Features:**
- Unified `SearchAdapter` interface for all search engines
- Spring Boot auto-configuration based on API keys/URLs
- Parallel search execution using Java 25 Virtual Threads
- Complete workflow orchestration (query generation → search → processing)
- Zero-result tracking for adapter effectiveness analysis
- Comprehensive statistics and metrics

## Architecture

```
SearchWorkflowService
    ├─> QueryGenerationService (generates optimized queries)
    ├─> SearchAdapter[] (parallel execution)
    │   ├─> BraveSearchAdapter
    │   ├─> SearXNGAdapter
    │   ├─> SerperAdapter (future)
    │   └─> TavilyAdapter (future)
    ├─> SearchResultProcessor (confidence scoring, deduplication)
    └─> DiscoverySessionService (persistence, statistics)
```

## Quick Start

### 1. Configuration

Add API keys to your environment or `application.yml`:

```yaml
search-adapters:
  brave:
    api-key: ${BRAVE_API_KEY:}
  searxng:
    api-url: ${SEARXNG_API_URL:http://192.168.1.10:8080}
```

### 2. Usage

**Nightly Automated Search:**
```java
@Autowired
private SearchWorkflowService searchWorkflowService;

// Execute Monday's categories (Individual & Student Funding)
SearchWorkflowResult result = searchWorkflowService.executeNightlySearch(DayOfWeek.MONDAY);

System.out.println("Queries generated: " + result.getQueriesGenerated());
System.out.println("Candidates created: " + result.getCandidatesCreated());
System.out.println("Execution time: " + result.getExecutionDuration().toMinutes() + " minutes");
```

**Manual Targeted Search:**
```java
ManualSearchRequest request = ManualSearchRequest.builder()
    .categories(List.of(
        FundingSearchCategory.INDIVIDUAL_SCHOLARSHIPS,
        FundingSearchCategory.STEM_EDUCATION))
    .engines(List.of(
        SearchEngineType.BRAVE,
        SearchEngineType.SEARXNG))
    .maxResultsPerQuery(10)
    .build();

SearchWorkflowResult result = searchWorkflowService.executeManualSearch(request);
```

## Search Workflow

The complete workflow executes in 7 stages:

1. **Query Generation**: AI-powered query generation via Ollama
   - Engine-specific optimization (keyword vs natural language)
   - Geographic targeting (Bulgaria, Eastern Europe, EU)
   - Category-specific keywords

2. **Parallel Search Execution**: Java 25 Virtual Threads
   - All engines execute simultaneously
   - Adapter failures don't block workflow
   - Zero-result queries tracked for effectiveness

3. **Result Processing**: SearchResultProcessor pipeline
   - Domain extraction and validation
   - Spam TLD filtering
   - Domain-level deduplication
   - Blacklist checking
   - Confidence scoring (0.00-1.00)
   - Threshold filtering (≥0.60 → PENDING_CRAWL)

4. **Domain Registration**: DomainService
   - Register unique domains in `domain` table
   - Track discovery sessions
   - Quality metrics

5. **Candidate Creation**: CandidateCreationService
   - High confidence (≥0.60) → PENDING_CRAWL status
   - Low confidence (<0.60) → SKIPPED_LOW_CONFIDENCE status
   - Both stored for analysis

6. **Statistics Tracking**:
   - Queries generated per category
   - Results found per engine
   - Zero-result queries per engine
   - Candidates created (high/low confidence)
   - Duplicates/blacklisted skipped
   - Execution duration

7. **Session Persistence**: DiscoverySessionService
   - Audit trail in `discovery_session` table
   - Performance metrics
   - Error tracking

## Day-of-Week Rotation

Nightly searches distribute 30 funding categories across Monday-Sunday for balanced workload:

- **Monday** (4 categories): Individual & Student Funding
- **Tuesday** (5 categories): Program Grants
- **Wednesday** (3 categories): Infrastructure & Equipment
- **Thursday** (3 categories): Professional Development
- **Friday** (4 categories): Specialized Education
- **Saturday** (3 categories): Community & Partnerships
- **Sunday** (8 categories): Research, Innovation & Catch-All

See `DayOfWeekCategories.java` for complete schedule.

## Search Adapters

### Brave Search

**API**: https://api.search.brave.com/res/v1/web/search
**Method**: GET
**Auth**: API Key in `X-Subscription-Token` header
**Free Tier**: 2,000 queries/month
**Timeout**: 10 seconds

**Example Response:**
```json
{
  "web": {
    "results": [
      {
        "title": "EU Education Grants",
        "description": "Funding for education projects...",
        "url": "https://ec.europa.eu/education/grants"
      }
    ]
  }
}
```

### SearXNG

**API**: http://192.168.1.10:8080/search
**Method**: GET
**Auth**: None (self-hosted)
**Free Tier**: Unlimited
**Timeout**: 10 seconds

**Example Response:**
```json
{
  "results": [
    {
      "title": "Bulgaria Education Funding",
      "content": "Government program for schools...",
      "url": "https://mon.bg/funding"
    }
  ]
}
```

## Testing

**Run all tests:**
```bash
mvn test -pl northstar-search-adapters
```

**Test Coverage:**
- BraveAdapterContractTest (5 tests)
- SearXNGAdapterTest (8 tests)
- SerperAdapterContractTest (5 tests) - mocked, not yet implemented
- TavilyAdapterContractTest (5 tests) - mocked, not yet implemented

**Current Status:** 28/29 tests passing (96.5% pass rate)

## Configuration Reference

**Complete application.yml:**

```yaml
search-adapters:
  brave:
    api-key: ${BRAVE_API_KEY:}
    api-url: https://api.search.brave.com/res/v1/web/search
    timeout-seconds: 10

  serper:
    api-key: ${SERPER_API_KEY:}
    api-url: https://google.serper.dev/search
    timeout-seconds: 10

  searxng:
    api-url: ${SEARXNG_API_URL:http://192.168.1.10:8080}
    timeout-seconds: 10

  tavily:
    api-key: ${TAVILY_API_KEY:}
    api-url: https://api.tavily.com/search
    timeout-seconds: 15

spring:
  webflux:
    connection-timeout: 10s
    read-timeout: 30s

logging:
  level:
    com.northstar.funding.searchadapters: INFO
    com.northstar.funding.searchadapters.workflow: DEBUG
```

## Dependencies

This module depends on:

- `northstar-domain` - Domain entities (FundingSourceCandidate, Domain, SearchEngineType, etc.)
- `northstar-persistence` - Repository layer (DomainService, DiscoverySessionService)
- `northstar-query-generation` - AI-powered query generation (Ollama + LangChain4j)
- `northstar-crawler` - Result processing (SearchResultProcessor, ConfidenceScorer)

External:
- Spring Boot 3.5.7
- Spring WebFlux (WebClient for non-blocking HTTP)
- Jackson (JSON parsing)
- WireMock 3.3.1 (testing)

## Error Handling

**Adapter Failures:**
- Individual adapter failures don't stop workflow
- Failed adapters skipped, workflow continues with working adapters
- Error messages collected in `SearchWorkflowResult.failureMessages`

**Example:**
```
BRAVE: 401 Unauthorized - Invalid API key
SEARXNG: Connection refused
```

**Zero Results:**
- Tracked in `SearchWorkflowResult.zeroResultsByEngine`
- Used for adapter effectiveness analysis
- No candidates created, but query/session still recorded

## Performance

**Virtual Threads:**
- Java 25 Virtual Threads for parallel execution
- All queries execute simultaneously across all adapters
- Typical execution: 2-5 minutes for 10 queries × 2 engines

**Caching:**
- QueryGenerationService uses 24-hour Caffeine cache
- Same category + engine = cached queries (no re-generation)

**Throughput:**
- Monday: 4 categories × 2 engines × 10 queries = ~80 searches
- Sunday: 8 categories × 2 engines × 10 queries = ~160 searches
- Weekly total: ~700 searches across all 30 categories

## Future Enhancements

1. **Serper Adapter** - Google Search via Serper.dev API
2. **Tavily Adapter** - AI-powered search with summarization
3. **Rate Limiting** - Token bucket for API quota management
4. **Retry Logic** - Exponential backoff for transient failures
5. **Result Caching** - Redis cache for duplicate query detection
6. **Metrics Dashboard** - Grafana visualization of search effectiveness

## License

Proprietary - NorthStar Funding Discovery Platform
