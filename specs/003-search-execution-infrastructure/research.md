# Research Findings: Search Execution Infrastructure

**Feature**: 003-search-execution-infrastructure
**Date**: 2025-10-20
**Phase**: Phase 0 - Technology Research

## Overview
This document consolidates research findings for implementing search execution infrastructure with 4 search engine adapters, circuit breakers, retry logic, and nightly scheduling.

---

## 1. Searxng JSON API

### Decision
Use Searxng's JSON API with `format=json` parameter for programmatic access.

### Rationale
- Searxng is already running on Mac Studio (192.168.1.10:8080)
- Built-in JSON output format eliminates need for HTML parsing
- Simple HTTP GET with query parameters (no complex authentication)
- Returns structured results with title, URL, snippet, engine source

### API Contract
```bash
# Example request
curl -X GET "http://192.168.1.10:8080/search?q=bulgaria+funding+grants&format=json&language=en"

# Response format
{
  "query": "bulgaria funding grants",
  "number_of_results": 125,
  "results": [
    {
      "title": "Bulgaria Grants Portal",
      "url": "https://example.org/grants",
      "content": "Find funding opportunities for Bulgaria...",
      "engine": "google",
      "parsed_url": ["https", "example.org", "/grants", "", "", ""],
      "engines": ["google"],
      "positions": [1],
      "score": 1.0,
      "category": "general"
    }
  ]
}
```

### Implementation Notes
- No authentication required (self-hosted instance)
- Timeout: 10 seconds (configurable)
- Max results: 25 per query (use `&pageno=1` for pagination if needed)
- Language: `language=en` for English results
- Enable multiple engines via Searxng config (settings.yml)

### Alternatives Considered
- **HTML scraping**: Rejected - brittle, requires Jsoup parsing, breaks on UI changes
- **SearxNG API wrapper libraries**: Rejected - adds unnecessary dependency, simple HTTP GET sufficient

---

## 2. Browserbase API

### Decision
Use Browserbase REST API for browser-based search with Playwright integration (optional for this feature).

### Rationale
- Browserbase provides browser automation as a service
- Useful for JavaScript-rendered search results (future enhancement)
- REST API with API key authentication
- For MVP: May defer Browserbase to Phase 2 if other engines provide sufficient coverage

### API Contract
```bash
# Create session
curl -X POST https://api.browserbase.com/v1/sessions \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"projectId": "YOUR_PROJECT_ID"}'

# Response
{
  "id": "session-123",
  "connectUrl": "wss://connect.browserbase.com/session-123"
}
```

### Implementation Notes
- **Authentication**: Bearer token via `Authorization` header
- **Java Integration**: Use Spring RestClient for session creation, Playwright for browser control
- **Timeout**: 30 seconds for session creation, 15 seconds for search execution
- **Error Handling**: Circuit breaker required (API rate limits possible)

### Alternatives Considered
- **Direct Playwright integration**: Rejected for MVP - adds complexity, not needed for metadata-only judging
- **Puppeteer**: Rejected - Node.js-based, difficult to integrate with Java

### MVP Decision
**DEFER to Phase 2**: Browserbase requires browser automation (Playwright), which is beyond metadata-only judging scope. Prioritize Searxng, Tavily, Perplexity for MVP.

---

## 3. Tavily Search API

### Decision
Use Tavily Search API for AI-optimized search results with simple REST API.

### Rationale
- Tavily provides cleaned, AI-ready search results
- Simple POST request with JSON body
- Bearer token authentication
- Returns structured results with title, URL, snippet, raw content (optional)

### API Contract
```bash
# Search request
curl -X POST https://api.tavily.com/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer tvly-YOUR_API_KEY" \
  -d '{
    "query": "bulgaria funding grants",
    "max_results": 25,
    "include_answer": false,
    "include_raw_content": false,
    "include_images": false
  }'

# Response format
{
  "query": "bulgaria funding grants",
  "results": [
    {
      "title": "Bulgaria Grants Portal",
      "url": "https://example.org/grants",
      "content": "Find funding opportunities for Bulgaria...",
      "score": 0.95
    }
  ]
}
```

### Implementation Notes
- **Authentication**: Bearer token via `Authorization` header
- **Java Integration**: Spring RestClient with POST request
- **Timeout**: 15 seconds
- **Max Results**: 25 (configurable)
- **Error Handling**: Circuit breaker + 3 retries with exponential backoff

### Configuration
```yaml
search:
  engines:
    tavily:
      enabled: true
      baseUrl: https://api.tavily.com
      apiKey: ${TAVILY_API_KEY}
      maxResults: 25
      timeoutSeconds: 15
```

### Alternatives Considered
- **Google Custom Search API**: Rejected - expensive, daily quota limits
- **Bing Search API**: Rejected - similar to Google, Tavily provides better AI-optimized results

---

## 4. Perplexity API

### Decision
Use Perplexity API for AI-powered search with reasoning capabilities (future enhancement).

### Rationale
- Perplexity provides AI-enhanced search results
- OpenAI-compatible API interface
- Supports source filtering and domain-specific search
- Useful for verifying other engines' results

### API Contract
```bash
# Search request (OpenAI-compatible)
curl -X POST https://api.perplexity.ai/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer pplx-YOUR_API_KEY" \
  -d '{
    "model": "sonar",
    "messages": [
      {
        "role": "user",
        "content": "Find funding grants for Bulgaria"
      }
    ],
    "search_domain_filter": ["*.org", "*.gov", "*.edu"]
  }'

# Response format (OpenAI chat format)
{
  "id": "chat-123",
  "model": "sonar",
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "Here are funding grants for Bulgaria: ..."
      },
      "finish_reason": "stop"
    }
  ],
  "citations": [
    "https://example.org/grants",
    "https://example.gov/funding"
  ]
}
```

### Implementation Notes
- **Authentication**: Bearer token via `Authorization` header
- **Java Integration**: Spring RestClient with POST request
- **Model**: `sonar` (search-optimized) or `sonar-pro` (advanced)
- **Timeout**: 20 seconds (AI processing takes longer)
- **Result Extraction**: Parse citations array for URLs, extract content from message

### Configuration
```yaml
search:
  engines:
    perplexity:
      enabled: true
      baseUrl: https://api.perplexity.ai
      apiKey: ${PERPLEXITY_API_KEY}
      model: sonar
      maxResults: 25
      timeoutSeconds: 20
```

### Alternatives Considered
- **Spring AI Perplexity integration**: Rejected - adds Spring AI dependency, simple RestClient sufficient
- **Direct citations parsing**: Selected - extract URLs from citations array instead of parsing message content

### MVP Decision
**INCLUDE for MVP**: Perplexity provides unique AI-enhanced results, simple REST API integration.

---

## 5. Resilience4j Circuit Breaker

### Decision
Use Resilience4j for circuit breakers, retry logic, and bulkhead isolation.

### Rationale
- Spring Boot-native integration via `spring-boot-starter-resilience4j`
- Annotation-based configuration (`@CircuitBreaker`, `@Retry`)
- Exponential backoff with jitter for retries
- Circuit breaker prevents cascading failures when search engines are down

### Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      searxng:
        sliding-window-type: count-based
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 3
      tavily:
        sliding-window-type: count-based
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
      perplexity:
        sliding-window-type: count-based
        sliding-window-size: 10
        minimum-number-of-calls: 5
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
  retry:
    instances:
      searchEngines:
        max-attempts: 3
        wait-duration: 2s
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - org.springframework.web.client.RestClientException
          - java.net.SocketTimeoutException
```

### Implementation Notes
- **Circuit Breaker States**: CLOSED (normal) → OPEN (failing) → HALF_OPEN (testing recovery)
- **Failure Threshold**: 50% failure rate over 10 requests triggers OPEN state
- **Retry Strategy**: 3 attempts with exponential backoff (2s, 4s, 8s)
- **Fallback**: Log failure, continue with other engines, track in session statistics

### Annotation Example
```java
@Service
public class SearxngAdapter implements SearchEngineAdapter {

    @CircuitBreaker(name = "searxng", fallbackMethod = "fallbackSearch")
    @Retry(name = "searchEngines")
    public Try<List<SearchResult>> search(String query) {
        // Execute search
    }

    private Try<List<SearchResult>> fallbackSearch(String query, Exception ex) {
        log.warn("Searxng circuit breaker activated: {}", ex.getMessage());
        return Try.success(Collections.emptyList());
    }
}
```

### Alternatives Considered
- **Spring Retry**: Rejected - less feature-rich than Resilience4j, no circuit breaker support
- **Manual retry logic**: Rejected - reinventing the wheel, Resilience4j provides production-ready patterns

---

## 6. Spring Scheduling

### Decision
Use Spring `@Scheduled` annotation for nightly automated execution at 2 AM.

### Rationale
- Built-in Spring Boot feature (no additional dependencies)
- Cron expression support for flexible scheduling
- Async execution support via `@Async`
- Easy to disable via configuration property

### Configuration
```yaml
discovery:
  scheduling:
    enabled: true
    cron: "0 0 2 * * *"  # 2:00 AM every day
    timezone: Europe/Sofia
```

### Implementation Notes
- **Cron Expression**: `0 0 2 * * *` = 2:00 AM daily
- **Timezone**: `Europe/Sofia` (Bulgaria timezone)
- **Enable/Disable**: `discovery.scheduling.enabled=false` for development
- **Async Execution**: Use `@Async` with Virtual Threads for parallel search

### Scheduler Example
```java
@Service
@EnableScheduling
@ConditionalOnProperty(name = "discovery.scheduling.enabled", havingValue = "true")
public class NightlyDiscoveryScheduler {

    @Scheduled(cron = "${discovery.scheduling.cron}", zone = "${discovery.scheduling.timezone}")
    public void executeNightlyDiscovery() {
        log.info("Starting nightly discovery session at {}", LocalDateTime.now());
        searchExecutionService.executeNightlyDiscovery();
    }
}
```

### Alternatives Considered
- **Quartz Scheduler**: Rejected - overkill for simple daily scheduling
- **Spring Cloud Task**: Rejected - adds unnecessary complexity for monolith architecture
- **Unix cron**: Rejected - prefer in-app scheduling for easier testing and monitoring

---

## 7. Spring RestClient (Spring 6.1+)

### Decision
Use Spring's new `RestClient` API (Spring Boot 3.5.5 includes Spring 6.1) for HTTP requests.

### Rationale
- Modern replacement for deprecated `RestTemplate`
- Fluent API for building requests
- Native support for authentication headers
- Exception handling via `ResponseEntity`

### Implementation Example
```java
@Configuration
public class SearchEngineClientConfig {

    @Bean
    public RestClient tavilyRestClient(SearchEngineProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.getTavily().getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + properties.getTavily().getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .requestFactory(new JdkClientHttpRequestFactory())  // Java 25 HttpClient
            .build();
    }
}

@Service
public class TavilyAdapter implements SearchEngineAdapter {

    private final RestClient restClient;

    public Try<List<SearchResult>> search(String query) {
        return Try.of(() -> {
            String response = restClient.post()
                .uri("/search")
                .body(Map.of("query", query, "max_results", 25))
                .retrieve()
                .body(String.class);

            return parseResults(response);
        });
    }
}
```

### Alternatives Considered
- **RestTemplate**: Deprecated in Spring 6, avoid for new code
- **WebClient**: Reactive API - unnecessary complexity for synchronous search execution
- **Apache HttpClient**: Rejected - Spring RestClient provides better integration

---

## 8. WireMock for API Testing

### Decision
Use WireMock for mocking search engine APIs in integration tests.

### Rationale
- Industry-standard HTTP mocking library
- Supports request matching, response stubbing, verification
- TestContainers-compatible for isolated integration tests
- Eliminates need for real API keys in tests

### Test Example
```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class TavilyAdapterTest {

    @Autowired
    private TavilyAdapter tavilyAdapter;

    @Test
    void testSearch_Success() {
        // Stub Tavily API
        stubFor(post(urlEqualTo("/search"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "results": [
                        {
                          "title": "Bulgaria Grants",
                          "url": "https://example.org/grants",
                          "content": "Find funding opportunities..."
                        }
                      ]
                    }
                    """)));

        // Execute search
        Try<List<SearchResult>> results = tavilyAdapter.search("bulgaria funding");

        // Verify
        assertThat(results.isSuccess()).isTrue();
        assertThat(results.get()).hasSize(1);
        assertThat(results.get().get(0).getTitle()).isEqualTo("Bulgaria Grants");
    }
}
```

### Alternatives Considered
- **MockWebServer (OkHttp)**: Rejected - WireMock has richer features for request matching
- **Manual mocking**: Rejected - brittle, difficult to maintain

---

## 9. Virtual Threads for Parallel Execution

### Decision
Use Java 25 Virtual Threads for parallel search execution across engines.

### Rationale
- Java 25 built-in feature (no additional libraries)
- Lightweight concurrency (millions of virtual threads possible)
- Simpler than ExecutorService/CompletableFuture
- Perfect for I/O-bound search API calls

### Implementation Example
```java
@Service
public class SearchExecutionService {

    public List<SearchResult> executeQueryAcrossEngines(String query, List<SearchEngineAdapter> adapters) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Try<List<SearchResult>>>> futures = adapters.stream()
                .map(adapter -> executor.submit(() -> adapter.search(query)))
                .toList();

            return futures.stream()
                .map(this::getFutureResult)
                .filter(Try::isSuccess)
                .flatMap(t -> t.get().stream())
                .toList();
        }
    }

    private Try<List<SearchResult>> getFutureResult(Future<Try<List<SearchResult>>> future) {
        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            return Try.failure(e);
        }
    }
}
```

### Alternatives Considered
- **ExecutorService with ThreadPool**: Rejected - Virtual Threads eliminate need for thread pool tuning
- **Spring @Async**: Considered - Virtual Threads provide finer control for parallel execution
- **CompletableFuture**: Rejected - more complex API than Virtual Threads

---

## 10. Day-of-Week Query Library

### Decision
Store hardcoded queries in YAML configuration file, organized by day-of-week.

### Rationale
- No code compilation needed to modify queries
- Easy for Kevin to adjust based on analytics
- Spring Boot automatically parses YAML into Java objects
- Clear separation of configuration from code

### Configuration Example
```yaml
discovery:
  queries:
    monday:
      - query: "bulgaria funding grants 2025"
        tags: [geography:bulgaria, category:grants, authority:government]
      - query: "eastern europe ngo funding"
        tags: [geography:eastern-europe, category:ngo, authority:foundation]
      - query: "eu scholarships balkans"
        tags: [geography:balkans, category:scholarships, authority:eu]
    tuesday:
      - query: "bulgaria education grants"
        tags: [geography:bulgaria, category:education, authority:government]
      - query: "sofia research funding"
        tags: [geography:bulgaria, category:research, authority:government]
```

### Implementation Notes
- **@ConfigurationProperties**: Map YAML to Java `Map<DayOfWeek, List<QueryConfig>>`
- **Tags**: Used for analytics (which geographies/categories are most productive)
- **Expandable**: Add Wednesday-Sunday queries iteratively based on learnings

### Alternatives Considered
- **Database storage**: Rejected for MVP - adds complexity, YAML sufficient for 5-10 queries
- **Java constants**: Rejected - requires compilation, YAML allows runtime changes
- **Properties file**: Rejected - YAML better suited for nested structures

---

## Summary of Decisions

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Searxng API | JSON format HTTP GET | Self-hosted, simple, no auth required |
| Browserbase | **DEFERRED to Phase 2** | Browser automation beyond MVP scope |
| Tavily API | REST API with Bearer token | AI-optimized results, simple integration |
| Perplexity API | OpenAI-compatible REST API | AI-enhanced search, citation extraction |
| Circuit Breaker | Resilience4j | Spring Boot native, production-ready patterns |
| Retry Logic | Resilience4j Retry | Exponential backoff with jitter |
| Scheduling | Spring @Scheduled | Built-in, cron expression support |
| HTTP Client | Spring RestClient | Modern replacement for RestTemplate |
| API Testing | WireMock | Industry standard for HTTP mocking |
| Parallelism | Java 25 Virtual Threads | Lightweight concurrency for I/O-bound tasks |
| Query Library | YAML configuration | Runtime changes, no compilation needed |

---

## Next Steps (Phase 1)

1. Create `data-model.md` with entity definitions
2. Create OpenAPI contract for SearchEngineAdapter interface
3. Create `quickstart.md` with integration test scenarios
4. Update `CLAUDE.md` agent context file
5. Proceed to Phase 2 task generation

---

*Research completed: 2025-10-20*
*Ready for Phase 1: Design & Contracts*
