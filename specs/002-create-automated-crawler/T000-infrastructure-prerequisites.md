# T000 Series: Infrastructure Prerequisites

**Feature**: 002-create-automated-crawler
**Purpose**: Set up external service connections, AI integration, and dependencies before implementing T001-T023

## Overview

Before implementing the main tasks (T001-T023), we need to configure:
1. **Local AI**: LM Studio (already installed on Mac Studio)
2. **Search Engines**: Searxng, Browserbase, Tavily, Perplexity
3. **Fault Tolerance**: Resilience4j for remote HTTP calls
4. **Dependencies**: Update Vavr 0.10.7, add Resilience4j, Spring WebFlux

---

## T000-1: LM Studio Configuration (Mac Studio 192.168.1.10)

### Decision: Use Existing LM Studio (NOT Ollama)

**Rationale**:
- ✅ **Already installed** on Mac Studio (no installation needed)
- ✅ **Models already loaded** (llama-3.1-8b-instruct, phi-3-medium-4k-instruct)
- ✅ **OpenAI-compatible REST API** (simple integration via Spring RestClient)
- ✅ **Working perfectly** (verified via SSH connection)
- ✅ **No dependencies** (direct HTTP calls, no Spring AI complexity)
- ⚠️ **HTTP/1.1 required** (LM Studio has HTTP/2 compatibility issues)

### Verification Steps

**1. Verify LM Studio is running**:
```bash
ssh macstudio "curl -s http://localhost:1234/v1/models"
```

**Expected Output** (JSON with available models):
```json
{
  "object": "list",
  "data": [
    {"id": "llama-3.1-8b-instruct", "object": "model", ...},
    {"id": "phi-3-medium-4k-instruct", "object": "model", ...},
    {"id": "text-embedding-nomic-embed-text-v1.5", "object": "model", ...},
    {"id": "text-embedding-nomic-embed-text-1.5", "object": "model", ...}
  ]
}
```

**2. Test query generation**:
```bash
ssh macstudio 'curl -s http://localhost:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"llama-3.1-8b-instruct\",
    \"messages\": [{\"role\": \"user\", \"content\": \"Generate 3 search queries for education grants in Bulgaria\"}],
    \"temperature\": 0.7,
    \"max_tokens\": 200
  }"'
```

**Expected Output**:
```
1. Bulgaria education grants NGO 2025
2. Eastern Europe scholarship programs for schools
3. EU funding opportunities education Bulgaria
```

### LM Studio Configuration

**API Details**:
- **Endpoint**: `http://192.168.1.10:1234/v1` (OpenAI-compatible)
- **Model**: `llama-3.1-8b-instruct` (8B parameters, 4.92 GB)
- **Alternative Model**: `phi-3-medium-4k-instruct` (14B, 7.95 GB)
- **No API Key**: Local network only
- **CRITICAL**: Must force HTTP/1.1 (HTTP/2 causes issues)

### HTTP/1.1 Compatibility Fix

**Problem**: LM Studio has HTTP/2 compatibility issues (discovered in springcrawler project)

**Solution**: Force HTTP/1.1 in Java HttpClient configuration:
```java
HttpClient httpClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)  // Force HTTP/1.1
    .connectTimeout(Duration.ofSeconds(30))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();

JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

RestClient restClient = RestClient.builder()
    .baseUrl("http://192.168.1.10:1234/v1")
    .requestFactory(requestFactory)
    .build();
```

---

## T000-2: Update Dependencies (pom.xml)

### Add Required Dependencies

**1. Resilience4j** (Circuit breaker for remote calls):
```xml
<!-- Resilience4j for fault tolerance -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
```

**2. Spring WebFlux** (for RestClient with HTTP/1.1 support):
```xml
<!-- Spring WebFlux for RestClient -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**3. Update Vavr** (0.10.6 → 0.10.7):
```xml
<properties>
    <vavr.version>0.10.7</vavr.version>
</properties>
```

### Implementation Notes

**Why NOT Spring AI**:
- Spring AI 1.0.0-M4 BOM not available in Maven Central
- LM Studio has OpenAI-compatible REST API (direct HTTP calls simpler)
- Fewer dependencies = cleaner build
- RestClient provides full control over HTTP/1.1 configuration

**Build Status**: ✅ `mvn clean compile` succeeds

---

## T000-3: Configure application.yml

### Structure

**File**: `/Users/kevin/github/northstar-funding/backend/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: northstar-funding

# LM Studio Configuration (Local AI on Mac Studio)
lm-studio:
  base-url: http://192.168.1.10:1234/v1
  model: llama-3.1-8b-instruct
  timeout-seconds: 30
  max-tokens: 500
  temperature: 0.7

# Search Engine Configuration
search:
  engines:
    # Searxng (self-hosted on Mac Studio)
    searxng:
      enabled: true
      base-url: http://192.168.1.10:8080
      max-results: 25
      timeout-seconds: 10

    # Browserbase (API-based)
    browserbase:
      enabled: true
      api-key: ${BROWSERBASE_API_KEY}
      base-url: https://api.browserbase.com
      max-results: 25
      timeout-seconds: 15

    # Tavily (API-based)
    tavily:
      enabled: true
      api-key: ${TAVILY_API_KEY}
      base-url: https://api.tavily.com
      max-results: 25
      timeout-seconds: 10

    # Perplexity (API-based)
    perplexity:
      enabled: true
      api-key: ${PERPLEXITY_API_KEY}
      base-url: https://api.perplexity.ai
      max-results: 25
      timeout-seconds: 10

# Resilience4j Configuration
resilience4j:
  circuitbreaker:
    instances:
      searchEngines:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10

      lmStudio:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 3
        waitDurationInOpenState: 20s
        failureRateThreshold: 50

  retry:
    instances:
      searchEngines:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.net.SocketTimeoutException
          - java.net.ConnectException

      lmStudio:
        maxAttempts: 2
        waitDuration: 500ms

# Discovery Configuration
discovery:
  schedule:
    enabled: ${DISCOVERY_SCHEDULE_ENABLED:false}
    cron: "0 0 2 * * ?" # 2 AM nightly

  query-generation:
    geography-templates:
      - "Bulgaria"
      - "Eastern Europe"
      - "Balkans"
      - "EU member states"
      - "Romania"
      - "Greece"
      - "North Macedonia"

    funding-types:
      - "grants"
      - "scholarships"
      - "fellowships"
      - "funding opportunities"
      - "financial support"

    organization-types:
      - "NGO"
      - "nonprofit"
      - "foundation"
      - "educational institutions"
      - "social enterprises"

logging:
  level:
    com.northstar.funding.discovery: DEBUG
    io.github.resilience4j: INFO
```

### Configuration Pattern: @ConfigurationProperties

**Instead of scattered @Value annotations**, use type-safe @ConfigurationProperties:

**LmStudioProperties.java**:
```java
@Component
@ConfigurationProperties(prefix = "lm-studio")
@Data
@ToString
@Slf4j
public class LmStudioProperties {
    private String baseUrl = "http://192.168.1.10:1234/v1";
    private String model = "llama-3.1-8b-instruct";
    private int timeoutSeconds = 30;
    private int maxTokens = 500;
    private double temperature = 0.7;

    @PostConstruct
    public void logConfiguration() {
        log.info("🔧 LM Studio Configuration:");
        log.info("  - Base URL: {}", baseUrl);
        log.info("  - Model: {}", model);
        validateConfiguration();
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank() &&
               model != null && !model.isBlank();
    }
}
```

**SearchEngineProperties.java**, **DiscoveryProperties.java**: Similar pattern

---

## T000-4: Create .env File (API Keys)

**File**: `/Users/kevin/github/northstar-funding/backend/.env`

```bash
# Search Engine API Keys
BROWSERBASE_API_KEY=your_browserbase_api_key_here
TAVILY_API_KEY=your_tavily_api_key_here
PERPLEXITY_API_KEY=your_perplexity_api_key_here

# Discovery Schedule (disabled by default)
DISCOVERY_SCHEDULE_ENABLED=false

# Spring Profile
SPRING_PROFILES_ACTIVE=development
```

**Security Notes**:
1. Add `.env` to `.gitignore` (already should be)
2. Never commit API keys to git
3. Mac Studio production: Use environment variables or Consul (optional)

---

## T000-5: Test Connections

### LM Studio Client Tests

**File**: `/Users/kevin/github/northstar-funding/backend/src/test/java/com/northstar/funding/discovery/infrastructure/client/LmStudioClientTest.java`

**Test Results**: ✅ **All 5 tests passed**

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.083 s
```

**Test Details**:

1. **testLmStudioIsAvailable** ✅
   - Verified LM Studio accessible at http://192.168.1.10:1234/v1
   - Confirmed 4 models available (llama-3.1-8b-instruct, phi-3-medium-4k-instruct, 2 embedding models)

2. **testGetAvailableModels** ✅
   - Retrieved all available models from LM Studio
   - Output: `[llama-3.1-8b-instruct, phi-3-medium-4k-instruct, text-embedding-nomic-embed-text-v1.5, text-embedding-nomic-embed-text-1.5]`

3. **testGenerateSearchQueries** ✅
   - Prompt: "Generate 3 search queries for finding education grants in Bulgaria funded by NGOs"
   - Generated queries:
     ```
     - education grants for students in Bulgaria funded by NGOs
     - EU funding opportunities for education projects in Bulgaria
     - foundation grants for Bulgarian schools and universities
     ```

4. **testGenerateWithShortPrompt** ✅
   - Prompt: "What is the capital of Bulgaria?"
   - Response: "The capital of Bulgaria is Sofia."

5. **testGenerateFundingTypeQuery** ✅
   - Generated complex search query:
     ```
     (site:.gov OR site:.org) (grant OR funding OR award OR support)
     AND (education OR education non-profit OR NGO)
     AND ("Eastern Europe" OR "Central and Eastern Europe" OR Poland OR Russia OR Ukraine OR Czech Republic OR Hungary)
     -intitle:Jobs -intitle:Career -intitle:Volunteer
     ```

**Key Observations**:
- HTTP/1.1 fix working perfectly (all requests successful)
- Response times: 200-1200ms for AI generation
- @PostConstruct logging visible in test output
- Circuit breaker and retry configured correctly

---

## Checklist

- [x] **T000-1**: Verify LM Studio running on Mac Studio (no installation needed)
- [x] **T000-1**: Confirm models loaded (llama-3.1-8b-instruct, phi-3-medium-4k-instruct)
- [x] **T000-1**: Test LM Studio API responds at http://192.168.1.10:1234/v1
- [x] **T000-2**: Update pom.xml (Resilience4j 2.2.0, Spring WebFlux, Vavr 0.10.7)
- [x] **T000-2**: Verify `mvn clean compile` succeeds
- [x] **T000-3**: Create application.yml with LM Studio and search engine configuration
- [x] **T000-3**: Create @ConfigurationProperties classes (LmStudioProperties, SearchEngineProperties, DiscoveryProperties)
- [x] **T000-4**: Create .env file with API keys (secured in .gitignore)
- [x] **T000-5**: Create LmStudioClient with HTTP/1.1 compatibility fix
- [x] **T000-5**: Test LM Studio connection from Java (5/5 tests passed)
- [ ] **T000-5**: Test Searxng connection (deferred to T010)
- [ ] **T000-5**: Test Browserbase API (deferred to T010)
- [ ] **T000-5**: Test Tavily API (deferred to T010)

---

## Success Criteria

✅ **Infrastructure Ready**:
1. LM Studio running on Mac Studio, accessible from MacBook M2
2. All dependencies added to pom.xml, build successful
3. application.yml configured with all endpoints
4. API keys securely stored in .env
5. LM Studio connection tested and working (5/5 tests passed)

✅ **Ready for T001**: Can now proceed with DomainRepository tests and implementation

---

## Implementation Summary

**What Was Built**:
1. **LmStudioProperties.java** - Type-safe configuration with @PostConstruct validation
2. **SearchEngineProperties.java** - Map-based configuration for 4 search engines
3. **DiscoveryProperties.java** - Query generation templates and schedule config
4. **LmStudioClient.java** - HTTP client with HTTP/1.1 compatibility fix, circuit breaker, retry
5. **LmStudioClientTest.java** - 5 integration tests, all passing

**Configuration Pattern**:
- @ConfigurationProperties (NOT @Value)
- @PostConstruct for validation and logging
- Lombok @Data, @ToString for boilerplate
- Convenience methods: isConfigured(), hasApiKey(), etc.

**Test Results** (mvn test -Dtest=LmStudioClientTest):
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.083 s
```

---

## Notes

- **Constitutional Compliance**: LM Studio is local (Mac Studio), no external LLM dependencies ✅
- **Simplicity**: Direct application.yml config (no Consul) ✅
- **Fault Tolerance**: Resilience4j handles failures gracefully ✅
- **HTTP/1.1 Fix**: Critical for LM Studio compatibility ✅
- **No Spring AI**: Direct HTTP calls simpler, fewer dependencies ✅
