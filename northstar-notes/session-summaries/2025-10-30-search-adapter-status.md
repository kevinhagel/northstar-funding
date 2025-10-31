# Search Engine Adapter Status & Configuration Needs

**Date**: 2025-10-30
**Context**: Review of search engine adapters and configuration needs
**Tags**: #architecture #search-infrastructure #browserbase #configuration

## Current State

### ✅ What Exists (Feature 003)

**1. SearchEngineAdapter Interface** (`SearchEngineAdapter.java`)
- Unified interface for all search engines
- Uses `Try<List<SearchResult>>` for functional error handling
- Includes health check capability
- **Already includes BROWSERBASE in interface comments** (line 12)

**2. Implemented Adapters** (3 of 4)
- ✅ **SearxngAdapter** - COMPLETE
  - Base URL: http://192.168.1.10:8080 (Mac Studio)
  - No API key required
  - Circuit breaker: `searxng`
  - Uses Spring RestClient
  - HTTP JSON response parsing

- ✅ **TavilyAdapter** - COMPLETE
  - Base URL: https://api.tavily.com
  - API key: `${TAVILY_API_KEY}`
  - Circuit breaker: `tavily`
  - Uses Spring RestClient

- ✅ **PerplexityAdapter** - COMPLETE
  - Base URL: https://api.perplexity.ai
  - API key: `${PERPLEXITY_API_KEY}`
  - Circuit breaker: `perplexity`
  - Uses Spring RestClient

**3. SearchEngineType Enum**
- Includes SEARXNG, BROWSERBASE, TAVILY, PERPLEXITY
- BROWSERBASE marked as "DEFERRED to Phase 2"
- Comment: "(complex implementation)"

**4. application.yml Configuration**
- ✅ Searxng: Configured (Mac Studio)
- ✅ Browserbase: **Placeholder exists** with `${BROWSERBASE_API_KEY:}`
- ✅ Tavily: Configured with `${TAVILY_API_KEY:}`
- ✅ Perplexity: Configured with `${PERPLEXITY_API_KEY:}`
- ❌ Browserbase circuit breaker: **MISSING**

### ❌ What's Missing

**1. BrowserbaseAdapter** - NOT IMPLEMENTED
- File does not exist: `BrowserbaseAdapter.java`
- Enum exists, config exists, but NO Java implementation
- Marked as "DEFERRED to Phase 2 (complex implementation)"

**2. Circuit Breaker Configuration**
- Searxng: ✅ Configured
- Tavily: ❌ **MISSING** (not in resilience4j.circuitbreaker.instances)
- Perplexity: ✅ Configured
- Browserbase: ❌ **MISSING**
- LM Studio: ✅ Configured

**3. AI Query Generation (Future)**
- Original SpringCrawler used AI to generate keyword search queries
- Used AI-generated prompts for Tavily communication
- Currently using "canned queries" (7-day query library in application.yml)
- **Need**: LM Studio integration for query generation (Feature 004)

## Configuration Philosophy

### Current Approach: Spring HTTP (RestClient)

**Why Spring RestClient:**
- ✅ No langchain4j dependency (had HTTP 1.2 problems)
- ✅ Built into Spring 6.1+ (Spring Boot 3.5.6)
- ✅ Simple, clean API
- ✅ Works with circuit breakers and retry logic
- ✅ Plays well with Virtual Threads

**HTTP Stack:**
- Spring RestClient for simple HTTP GET/POST
- Resilience4j for circuit breakers and retry
- Vavr Try for functional error handling
- Virtual Threads for parallel execution

### Configuration Best Practices

**1. Externalize API Keys (Environment Variables)**
```yaml
api-key: ${BROWSERBASE_API_KEY:}
api-key: ${TAVILY_API_KEY:}
api-key: ${PERPLEXITY_API_KEY:}
```

**2. Enable/Disable Per Engine**
```yaml
searxng:
  enabled: true
browserbase:
  enabled: false  # Can disable without removing code
```

**3. Timeouts Per Engine**
```yaml
searxng:
  timeout-seconds: 10
browserbase:
  timeout-seconds: 15  # Browserbase may be slower (browser automation)
```

**4. Circuit Breaker Per Engine**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      browserbase:
        # Configuration here
```

## What Needs to Be Done

### Priority 1: Complete Browserbase Integration

**Task 1: Create BrowserbaseAdapter.java**
- Implement SearchEngineAdapter interface
- Use Spring RestClient for HTTP calls
- Add @CircuitBreaker(name = "browserbase")
- Add @Retry(name = "searchEngines")
- Parse Browserbase API response format
- Handle browser automation specifics

**Task 2: Add Browserbase Circuit Breaker Config**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      browserbase:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 45s  # Longer for browser automation
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
```

**Task 3: Add Tavily Circuit Breaker Config**
Currently missing from resilience4j config!

### Priority 2: Verify API Keys

**Browserbase:**
- User has API key
- Need to set: `export BROWSERBASE_API_KEY=bb-xxxxx`

**Tavily:**
- User has API key
- Need to set: `export TAVILY_API_KEY=tvly-xxxxx`

**Perplexity:**
- User has API key
- Need to set: `export PERPLEXITY_API_KEY=pplx-xxxxx`

### Priority 3: LM Studio Integration (Feature 004)

**For AI Query Generation:**
- LMStudioClient exists but may need enhancement
- Use for generating keyword search queries
- Use for generating AI prompts for Tavily
- Replace "canned queries" with AI-generated queries
- Feature 004 Task T013-T021

## Browserbase API Details (Research Needed)

**Questions to Answer:**
1. What is Browserbase's API endpoint structure?
   - Search endpoint URL?
   - Query parameter format?

2. What is the request format?
   - JSON POST body?
   - Query parameters?

3. What is the response format?
   - JSON structure?
   - Fields available (title, snippet, url)?

4. Are there special headers required?
   - API key header format?
   - Content-Type?

5. What are rate limits?
   - Requests per minute?
   - Concurrent request limits?

**Next Step**: Research Browserbase API documentation to understand:
- Browser automation flow (create session, navigate, extract results)
- Whether it's a search API or browser automation API
- If it's browser automation, may need different adapter pattern

## Decision Needed: Browserbase Complexity

**Original Note**: "DEFERRED to Phase 2 (complex implementation)"

**Why Complex?**
- If Browserbase is **browser automation** (not search API):
  - Need to create browser session
  - Navigate to search engine (Google, Bing, etc.)
  - Extract results from DOM
  - Much more complex than simple HTTP API call

- If Browserbase is **search API**:
  - Similar to Tavily/Perplexity
  - Simple HTTP adapter

**Recommendation**:
1. Research Browserbase API first
2. If browser automation → defer to Phase 2 (after Feature 004)
3. If search API → implement now (similar to Tavily)

## Related Documentation

- [[../CLAUDE.md]] - Project guide
- Feature 003 Spec: `specs/003-search-execution-infrastructure/spec.md`
- Feature 003 Completion: `specs/003-search-execution-infrastructure/COMPLETION-SUMMARY.md`
- SearchEngineAdapter interface: `backend/src/main/java/com/northstar/funding/discovery/search/infrastructure/adapters/SearchEngineAdapter.java`
- Application config: `backend/src/main/resources/application.yml`

## Next Actions

1. [ ] Research Browserbase API documentation
2. [ ] Determine if Browserbase is search API or browser automation
3. [ ] If search API: Create BrowserbaseAdapter following Tavily pattern
4. [ ] Add Tavily circuit breaker config (currently missing)
5. [ ] Add Browserbase circuit breaker config
6. [ ] Test all adapters with actual API keys
7. [ ] Document Browserbase integration in session summary
