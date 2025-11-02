# Research: Search Provider Adapters

**Feature**: 003-design-and-implement
**Date**: 2025-11-01
**Status**: Phase 0 Research Complete

---

## Research Overview

This document resolves 7 clarifications identified in the feature specification. Research focuses on practical decisions for v1 implementation based on spring-crawler lessons and Spring Boot best practices.

---

## 1. API Keys/Credentials Management

### Decision
Use Spring Boot's `@ConfigurationProperties` with externalized configuration files, NOT environment variables or Consul/Vault for v1.

### Rationale
- **Spring Boot Standard**: `application.properties` or `application.yml` with profile-specific overrides
- **Mac Studio Setup**: Configuration files stored outside Docker containers, mounted as volumes
- **Development Workflow**: Local MacBook M2 development uses `application-dev.properties`, Mac Studio uses `application-prod.properties`
- **Security**: Config files are gitignored, Kevin manages them manually on Mac Studio
- **Simplicity**: Avoids Consul/Vault complexity for single-user deployment

### Implementation Pattern
```java
@ConfigurationProperties(prefix = "search.providers")
@Component
public class SearchProviderConfig {
    private BraveSearchConfig braveSearch;
    private SerperConfig serper;
    private TavilyConfig tavily;
    private SearxngConfig searxng;

    @Data
    public static class BraveSearchConfig {
        private String apiKey;
        private String baseUrl = "https://api.search.brave.com/res/v1/web/search";
        private int timeout = 5000; // milliseconds
        private int maxResults = 20;
    }

    // Similar for Serper, Tavily, SearXNG
}
```

**application-prod.properties** (on Mac Studio, gitignored):
```properties
search.providers.brave-search.api-key=ACTUAL_BRAVE_API_KEY
search.providers.serper.api-key=ACTUAL_SERPER_API_KEY
search.providers.tavily.api-key=ACTUAL_TAVILY_API_KEY
search.providers.searxng.base-url=http://192.168.1.10:8080/search
```

### Alternatives Considered
- **Environment Variables**: Less maintainable, harder to override per-provider settings
- **Consul Key-Value Store**: Over-engineering for single-node deployment, adds operational complexity
- **HashiCorp Vault**: Requires Vault server setup, adds deployment dependencies
- **Spring Cloud Config Server**: Unnecessary for single application, adds infrastructure

**Rejected Because**: All alternatives add complexity without meaningful security benefit for single-user Mac Studio deployment. Spring Boot `@ConfigurationProperties` provides sufficient externalization.

---

## 2. Rate Limiting Thresholds Per Provider

### Decision
Use provider-documented rate limits with conservative defaults, implement client-side throttling.

### Rationale
- **BraveSearch Free Tier**: 2,000 queries/month = ~67 queries/day (conservative: 50/day)
- **Serper Free Tier**: 2,500 queries/month = ~83 queries/day (conservative: 60/day)
- **Tavily Free Tier**: 1,000 queries/month = ~33 queries/day (conservative: 25/day)
- **SearXNG (self-hosted)**: No limits, but respect upstream search engines (conservative: 100/day)
- **Weekly Search Strategy**: User requirements specify spreading queries across week, NOT nightly execution

### Implementation Approach
Use Resilience4j RateLimiter (already approved in constitution):
```java
@Bean
public RateLimiter braveSearchRateLimiter() {
    return RateLimiter.of("brave-search", RateLimiterConfig.custom()
        .limitForPeriod(50)           // 50 requests
        .limitRefreshPeriod(Duration.ofDays(1))  // per day
        .timeoutDuration(Duration.ofSeconds(1))  // wait max 1s
        .build());
}
```

**Configuration Properties**:
```properties
search.providers.brave-search.rate-limit.daily=50
search.providers.serper.rate-limit.daily=60
search.providers.tavily.rate-limit.daily=25
search.providers.searxng.rate-limit.daily=100
```

### Rate Limit Handling Strategy
1. **Client-Side Throttling**: Resilience4j RateLimiter prevents exceeding limits
2. **Graceful Degradation**: If rate limit reached, skip provider for this search, log warning
3. **Partial Results**: Return results from working providers, include error metadata for rate-limited ones
4. **Monitoring**: Track daily usage, alert when approaching 80% of quota

### Alternatives Considered
- **Redis-Based Distributed Rate Limiting**: Overkill for single-node deployment
- **No Rate Limiting**: Risk of API key suspension, wasted quota on spam searches
- **Per-Query Backoff**: Too conservative, wastes quota

**Rejected Because**: Redis adds infrastructure dependency. No rate limiting risks API suspension. Backoff doesn't match weekly search schedule.

---

## 3. Retry Strategy for Transient Failures

### Decision
Use Resilience4j @Retry with exponential backoff, retry only on transient HTTP errors.

### Rationale
- **Transient Errors**: HTTP 429 (rate limit), 503 (service unavailable), network timeouts
- **Permanent Errors**: HTTP 400 (bad request), 401 (unauthorized), 404 (not found) - DO NOT RETRY
- **Exponential Backoff**: Avoid hammering failing services
- **Max Attempts**: 3 total attempts (initial + 2 retries) to prevent long waits

### Implementation Pattern
```java
@Retry(name = "search-provider")
public Try<List<SearchResult>> executeSearch(String query, int maxResults) {
    // Search provider implementation
}
```

**Resilience4j Configuration** (application.properties):
```properties
resilience4j.retry.instances.search-provider.max-attempts=3
resilience4j.retry.instances.search-provider.wait-duration=500ms
resilience4j.retry.instances.search-provider.exponential-backoff-multiplier=2
resilience4j.retry.instances.search-provider.retry-exceptions=\
  java.net.http.HttpTimeoutException,\
  java.net.ConnectException,\
  java.io.IOException
resilience4j.retry.instances.search-provider.ignore-exceptions=\
  java.lang.IllegalArgumentException,\
  com.northstar.funding.crawler.exception.InvalidApiKeyException
```

**Retry Schedule**:
- Attempt 1: Immediate (0ms)
- Attempt 2: After 500ms
- Attempt 3: After 1000ms (500ms × 2)
- Total max wait: 1.5 seconds

### Transient vs Permanent Error Classification

**Retry These (Transient)**:
- HTTP 429 (Too Many Requests) - rate limit, wait and retry
- HTTP 503 (Service Unavailable) - temporary outage
- HTTP 504 (Gateway Timeout) - network issue
- `HttpTimeoutException` - request timeout
- `ConnectException` - connection refused (service restarting)
- Generic `IOException` - network interruption

**DO NOT Retry (Permanent)**:
- HTTP 400 (Bad Request) - malformed query, won't fix itself
- HTTP 401 (Unauthorized) - invalid API key
- HTTP 403 (Forbidden) - API access denied
- HTTP 404 (Not Found) - endpoint doesn't exist
- `IllegalArgumentException` - programming error
- `InvalidApiKeyException` - configuration error

### Alternatives Considered
- **Fixed Delay Retry**: Less respectful of failing services, could worsen cascading failures
- **Unlimited Retries**: Blocks Virtual Thread indefinitely, wastes resources
- **No Retry**: Single network glitch fails entire search

**Rejected Because**: Fixed delay doesn't adapt to load. Unlimited retries waste CPU. No retry is too fragile.

---

## 4. Partial Search Failure Handling

### Decision
Continue with successful providers, return partial results with detailed error metadata.

### Rationale
- **Resilience Over Perfection**: Getting results from 3/4 providers is better than failing entirely
- **User Requirement**: Weekly search schedule means we can retry failed providers next week
- **Metadata Tracking**: Record which providers failed for monitoring and debugging

### Implementation Pattern
```java
public CompletableFuture<SearchProviderResult> executeAllProviders(String query) {
    List<CompletableFuture<ProviderSearchResult>> futures = List.of(
        executeProviderAsync(braveSearchAdapter, query),
        executeProviderAsync(searxngAdapter, query),
        executeProviderAsync(serperAdapter, query),
        executeProviderAsync(tavilyAdapter, query)
    );

    // Wait for ALL to complete (success or failure)
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> {
            List<SearchResult> successfulResults = futures.stream()
                .map(CompletableFuture::join)
                .filter(ProviderSearchResult::isSuccess)
                .flatMap(r -> r.getResults().stream())
                .toList();

            List<ProviderError> errors = futures.stream()
                .map(CompletableFuture::join)
                .filter(ProviderSearchResult::isFailure)
                .map(ProviderSearchResult::getError)
                .toList();

            return SearchProviderResult.of(successfulResults, errors);
        });
}
```

### Error Metadata Structure
```java
public record ProviderError(
    SearchEngineType provider,
    String errorMessage,
    String errorType,  // RATE_LIMIT, TIMEOUT, AUTH_FAILURE, NETWORK_ERROR
    LocalDateTime occurredAt,
    String query
) {}
```

### Failure Scenarios

**Scenario 1: One Provider Fails (3/4 succeed)**
- Return: Results from BraveSearch, SearXNG, Serper
- Log: Warning with Tavily error details
- Action: Continue normally

**Scenario 2: Two Providers Fail (2/4 succeed)**
- Return: Results from BraveSearch, SearXNG
- Log: Warning with Serper + Tavily error details
- Action: Continue, but alert if pattern persists

**Scenario 3: Three Providers Fail (1/4 succeeds)**
- Return: Results from SearXNG only
- Log: Error with details from all 3 failures
- Action: Alert for investigation, consider retry

**Scenario 4: All Providers Fail (0/4 succeed)**
- Return: Empty result set with all 4 error details
- Log: Critical error
- Action: Alert immediately, retry entire search

### Monitoring and Alerting
Track provider success rates over 7-day rolling window:
- **Healthy**: >95% success rate
- **Degraded**: 80-95% success rate (log warnings)
- **Unhealthy**: <80% success rate (alert for investigation)

### Alternatives Considered
- **All-or-Nothing**: Fail entire search if any provider fails - too fragile
- **Immediate Retry on Failure**: Wastes time when provider is down for hours
- **Silent Failure**: Hide errors, only return successful results - masks problems

**Rejected Because**: All-or-nothing wastes successful provider results. Immediate retry delays response. Silent failure hides systemic issues.

---

## 5. Timeout Thresholds

### Decision
Per-provider timeouts (5 seconds each) with total search timeout of 10 seconds.

### Rationale
- **Performance Goal**: Spec requires all 4 providers complete within 5 seconds (90th percentile)
- **Virtual Threads**: Parallel execution means 4 providers run concurrently
- **Realistic Expectations**:
  - BraveSearch: ~2 seconds typical
  - SearXNG: ~3 seconds typical (aggregates multiple engines)
  - Serper: ~2 seconds typical
  - Tavily: ~4 seconds typical (AI-optimized processing)
- **Buffer**: 10-second total timeout allows for network variance

### Implementation Pattern
```java
@ConfigurationProperties(prefix = "search.timeouts")
public class SearchTimeoutConfig {
    private Duration perProvider = Duration.ofSeconds(5);
    private Duration totalSearch = Duration.ofSeconds(10);
}

// Per-provider timeout (java.net.http.HttpClient)
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(searchUrl))
    .timeout(timeoutConfig.getPerProvider())  // 5 seconds
    .build();

// Total search timeout (CompletableFuture)
CompletableFuture<SearchProviderResult> result = executeAllProviders(query)
    .orTimeout(timeoutConfig.getTotalSearch().toMillis(), TimeUnit.MILLISECONDS);
```

### Timeout Values by Provider
```properties
search.timeouts.per-provider=5000  # 5 seconds (applies to all providers)
search.timeouts.total-search=10000 # 10 seconds (total for parallel execution)
search.providers.brave-search.timeout=5000
search.providers.serper.timeout=5000
search.providers.tavily.timeout=6000  # Tavily AI processing takes longer
search.providers.searxng.timeout=7000  # SearXNG aggregates multiple engines
```

### Timeout Behavior

**Per-Provider Timeout** (5-7 seconds):
- Cancels HTTP request if provider doesn't respond
- Logs timeout error with provider name and query
- Returns failure for that provider only
- Other providers continue executing

**Total Search Timeout** (10 seconds):
- Hard limit on entire multi-provider search
- If reached, returns results from completed providers only
- Logs critical timeout with all provider states
- Prevents indefinite waiting

### Failure Recovery
If provider consistently times out (>50% of requests over 24 hours):
1. Increase timeout by 50% (e.g., 5s → 7.5s)
2. Alert for investigation
3. If still timing out, disable provider temporarily

### Alternatives Considered
- **Unlimited Timeout**: Risk of hanging indefinitely on stuck providers
- **Shorter Timeouts (2-3s)**: Too aggressive, would cause false failures on network variance
- **Same Timeout for All**: Doesn't account for Tavily/SearXNG being slower by design

**Rejected Because**: Unlimited risks wasting resources. Shorter causes unnecessary failures. Same timeout penalizes slower providers unfairly.

---

## 6. API Usage Tracking Mechanism

### Decision
Database-backed tracking using PostgreSQL (NOT in-memory cache) for v1.

### Rationale
- **Persistence**: Survives application restarts (in-memory doesn't)
- **Accuracy**: Database provides exact counts (Caffeine cache can evict entries)
- **Simplicity**: Reuse existing PostgreSQL infrastructure, no new dependencies
- **Monitoring**: Query database for usage reports and quota alerts
- **Auditability**: Historical usage data for billing/debugging

### Implementation Pattern

**Database Schema** (Flyway migration):
```sql
CREATE TABLE provider_api_usage (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,  -- BRAVE_SEARCH, SERPER, TAVILY, SEARXNG
    query TEXT NOT NULL,
    result_count INT NOT NULL,
    success BOOLEAN NOT NULL,
    error_type VARCHAR(100),  -- NULL if success, TIMEOUT/RATE_LIMIT/AUTH/etc if failure
    executed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    response_time_ms INT NOT NULL,  -- HTTP response time

    INDEX idx_provider_date (provider, executed_at),
    INDEX idx_success_date (success, executed_at)
);
```

**Repository**:
```java
public interface ProviderApiUsageRepository extends CrudRepository<ProviderApiUsage, Long> {
    @Query("""
        SELECT COUNT(*) FROM provider_api_usage
        WHERE provider = :provider
        AND executed_at >= :since
        """)
    int countUsageSince(String provider, LocalDateTime since);

    @Query("""
        SELECT provider, COUNT(*) as count, AVG(response_time_ms) as avg_response
        FROM provider_api_usage
        WHERE executed_at >= :since
        GROUP BY provider
        """)
    List<ProviderUsageStats> getUsageStatsSince(LocalDateTime since);
}
```

**Service Layer**:
```java
@Service
public class ApiUsageTrackingService {
    private final ProviderApiUsageRepository repository;

    @Async("virtual-thread-executor")  // Don't block search thread
    public void trackUsage(SearchEngineType provider, String query,
                          Try<List<SearchResult>> result, long responseTimeMs) {
        ProviderApiUsage usage = ProviderApiUsage.builder()
            .provider(provider.name())
            .query(query)
            .success(result.isSuccess())
            .resultCount(result.map(List::size).getOrElse(0))
            .errorType(result.isFailure() ? classifyError(result.getCause()) : null)
            .responseTimeMs((int) responseTimeMs)
            .executedAt(LocalDateTime.now())
            .build();

        repository.save(usage);
    }

    public int getDailyUsage(SearchEngineType provider) {
        return repository.countUsageSince(
            provider.name(),
            LocalDateTime.now().minusHours(24)
        );
    }
}
```

### Quota Monitoring
Daily scheduled job (Spring @Scheduled):
```java
@Scheduled(cron = "0 0 8 * * *")  // Every day at 8 AM
public void checkQuotas() {
    for (SearchEngineType provider : SearchEngineType.values()) {
        int dailyUsage = trackingService.getDailyUsage(provider);
        int dailyLimit = config.getRateLimit(provider);

        if (dailyUsage > dailyLimit * 0.8) {  // 80% threshold
            log.warn("Provider {} approaching quota: {}/{}",
                provider, dailyUsage, dailyLimit);
        }
    }
}
```

### Alternatives Considered
- **In-Memory Tracking (Caffeine)**: Lost on restart, no historical data
- **Redis Counters**: Requires Redis server, adds infrastructure dependency
- **No Tracking**: Blind to quota usage, risk of API key suspension

**Rejected Because**: In-memory doesn't persist. Redis adds complexity. No tracking is reckless.

---

## 7. Weekly Search Schedule Strategy

### Decision
Distribute searches across 7 days using category-geographic rotation, NOT nightly full scans.

### Rationale
- **User Requirement**: "There is no need to search for Education Infrastructure in Europe every night, we only need to do it once during the week"
- **Quota Conservation**: 25 categories × 8 regions × 4 providers = 800 queries/week if done nightly. Weekly rotation = ~114 queries/week
- **API Free Tier Limits**:
  - BraveSearch: 2,000/month = 460/week (50/day budget × 7 days = 350/week available)
  - Serper: 2,500/month = 575/week (60/day × 7 = 420/week available)
  - Tavily: 1,000/month = 230/week (25/day × 7 = 175/week available)
- **Geographic Priority**: Bulgaria → Balkans → Eastern Europe → EU → Europe → US (lowest)

### Weekly Search Schedule Design

**Monday** - Bulgaria Focus (Highest Priority):
- All 25 categories × Bulgaria geography
- Estimated: ~25 queries × 4 providers = 100 total API calls

**Tuesday** - Balkans + Eastern Europe:
- All 25 categories × Balkans
- Top 10 categories × Eastern Europe
- Estimated: ~35 queries × 4 providers = 140 total API calls

**Wednesday** - Eastern Europe (continued) + Central Europe:
- Remaining 15 categories × Eastern Europe
- Top 10 categories × Central Europe
- Estimated: ~25 queries × 4 providers = 100 total API calls

**Thursday** - Central Europe (continued) + EU:
- Remaining 15 categories × Central Europe
- Top 10 categories × EU
- Estimated: ~25 queries × 4 providers = 100 total API calls

**Friday** - EU (continued) + Western/Northern/Southern Europe:
- Remaining 15 categories × EU
- Top 5 categories × Western Europe
- Top 5 categories × Northern Europe
- Top 5 categories × Southern Europe
- Estimated: ~30 queries × 4 providers = 120 total API calls

**Saturday** - Europe (general) + Specialized:
- Top 15 categories × Europe (general)
- Specialized funding categories (STEM, Arts, Special Needs)
- Estimated: ~20 queries × 4 providers = 80 total API calls

**Sunday** - United States (Lowest Priority) + Catch-Up:
- Top 10 categories × United States
- Retry failed searches from previous week
- Estimated: ~15 queries × 4 providers = 60 total API calls

**Total Weekly Load**: ~700 API calls across all providers
**Per Provider**: ~175 calls/week (well under free tier limits)

### Category Taxonomy (25 Categories from spring-crawler)

**Infrastructure & Facilities** (4):
1. INFRASTRUCTURE_FUNDING
2. FACILITY_CONSTRUCTION
3. TECHNOLOGY_INFRASTRUCTURE
4. RENOVATION_MODERNIZATION

**Individual Support** (4):
5. INDIVIDUAL_SCHOLARSHIPS
6. STUDENT_GRANTS
7. GRADUATE_FELLOWSHIPS
8. STUDY_ABROAD_FUNDING

**Educator Development** (4):
9. TEACHER_DEVELOPMENT
10. PROFESSIONAL_DEVELOPMENT
11. EDUCATOR_GRANTS
12. LEADERSHIP_TRAINING

**Program Funding** (4):
13. PROGRAM_GRANTS
14. CURRICULUM_DEVELOPMENT
15. RESEARCH_FUNDING
16. INNOVATION_GRANTS

**Institutional Support** (4):
17. OPERATIONAL_SUPPORT
18. CAPACITY_BUILDING
19. ORGANIZATIONAL_DEVELOPMENT
20. EMERGENCY_FUNDING

**Specialized Funding** (5):
21. SPECIAL_NEEDS_EDUCATION
22. RURAL_EDUCATION
23. MINORITY_EDUCATION
24. ARTS_EDUCATION
25. STEM_EDUCATION

### Geographic Taxonomy (Priority Order)

1. **BULGARIA** (Highest Priority) - Monday
2. **BALKANS** - Tuesday
3. **EASTERN_EUROPE** - Tuesday/Wednesday
4. **CENTRAL_EUROPE** - Wednesday/Thursday
5. **EU_MEMBER_STATES** - Thursday/Friday
6. **EUROPE** (general) - Saturday
7. **WESTERN_EUROPE** - Friday
8. **NORTHERN_EUROPE** - Friday
9. **SOUTHERN_EUROPE** - Friday
10. **UNITED_STATES** (Lowest Priority) - Sunday

### Implementation Notes

**Database Schema** (Existing):
- `query_generation_sessions` table tracks when queries were last generated
- `discovery_session` table tracks when searches were last executed
- Check last execution timestamp before scheduling new searches

**Scheduling Approach** (Future Feature):
- Spring `@Scheduled` cron jobs for daily execution
- Each day executes specific category-geography combinations
- Queries generated by LM Studio (separate feature, not part of this spec)

### Alternatives Considered
- **Nightly Full Scan**: Exceeds API quotas, wastes resources on duplicate searches
- **Random Daily Selection**: No geographic prioritization, inconsistent coverage
- **Monthly Rotation**: Too infrequent, misses time-sensitive funding opportunities

**Rejected Because**: Nightly exceeds quotas. Random loses priority ordering. Monthly is too slow.

---

## Research Summary

All 7 clarifications have been resolved with concrete decisions:

1. ✅ **API Keys Management**: Spring Boot @ConfigurationProperties with externalized files
2. ✅ **Rate Limiting**: Resilience4j with conservative daily limits per provider
3. ✅ **Retry Strategy**: Resilience4j @Retry with exponential backoff, 3 max attempts
4. ✅ **Partial Failures**: Continue with successful providers, return error metadata
5. ✅ **Timeouts**: 5-7s per provider, 10s total search timeout
6. ✅ **API Usage Tracking**: PostgreSQL database with async logging
7. ✅ **Weekly Schedule**: Category-geography rotation across 7 days, NOT nightly

### Technology Decisions Confirmed

**Core Libraries**:
- ✅ Spring Boot 3.5.6 (framework)
- ✅ java.net.http.HttpClient (HTTP requests)
- ✅ Vavr 0.10.6 (Try monad for error handling)
- ✅ Apache Commons Text (fuzzy matching for anti-spam)
- ✅ Resilience4j (retry, rate limiting, circuit breaker)
- ✅ PostgreSQL 16 (API usage tracking)
- ✅ TestContainers (integration testing)

**Patterns**:
- ✅ Virtual Thread executor for parallel I/O
- ✅ CompletableFuture for async multi-provider execution
- ✅ Functional error handling with Vavr Try
- ✅ Spring @ConfigurationProperties for externalized config
- ✅ Database-backed API usage tracking

### Next Phase

Phase 1 (Design & Contracts) can now proceed with all unknowns resolved. No NEEDS CLARIFICATION markers remain.

---

**Research Complete**: 2025-11-01
**Ready for Phase 1**: YES
