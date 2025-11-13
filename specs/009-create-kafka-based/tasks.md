# Tasks: Kafka-Based Event-Driven Search Workflow

**Input**: Design documents from `/Users/kevin/github/northstar-funding/specs/009-create-kafka-based/`
**Prerequisites**: plan.md ✅, research.md ✅, spec.md ✅
**Branch**: `009-create-kafka-based`

## Execution Summary

This task list implements the Kafka-based event-driven search workflow with:
- **3 new Maven modules**: northstar-kafka-common, northstar-search-adapters, northstar-search-workflow
- **4 Kafka topics**: search-requests, search-results-raw, search-results-validated, workflow-errors
- **SearXNG adapter only** (initial scope to validate pattern)
- **Valkey caching** for blacklist lookups (10x speedup)
- **LM Studio → Ollama migration** for concurrent query generation
- **REST API** with SpringDoc/Swagger documentation

---

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- All paths are absolute or relative to repository root

---

## Phase 1: Prerequisites & Migration

### T001: Verify Infrastructure
**Description**: Verify all required services running on Mac Studio (192.168.1.10)
**Commands**:
```bash
# From MacBook M2
curl -s http://192.168.1.10:9092 || echo "❌ Kafka not responding"
redis-cli -h 192.168.1.10 -p 6379 ping || echo "❌ Valkey not responding"
curl -s http://192.168.1.10:8080/healthz || echo "❌ SearXNG not responding"
curl -s http://192.168.1.10:11434/v1/models || echo "❌ Ollama not responding"
```
**Success Criteria**: All 4 services respond successfully

### T002: Migrate Query Generation from LM Studio to Ollama
**Description**: Update northstar-query-generation module to use Ollama instead of LM Studio
**Files**:
- `northstar-query-generation/src/main/resources/application.yml`
- `northstar-query-generation/src/main/resources/application-test.yml`
- `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/config/LmStudioConfig.java` (rename to `OllamaConfig.java`)

**Changes**:
1. Update `application.yml`:
   ```yaml
   query-generation:
     ollama:  # was: lm-studio
       base-url: http://192.168.1.10:11434/v1  # was: :1234
       model-name: llama3.1:8b  # was: qwen2.5-0.5b-instruct
   ```
2. Rename `LmStudioConfig.java` to `OllamaConfig.java`
3. Update class name and @Configuration annotation
4. Keep HTTP/1.1 configuration (Ollama supports HTTP/2 but HTTP/1.1 works)
5. Update timeout to 60s (larger model)

**Success Criteria**: All 58 tests pass with Ollama

---

## Phase 2: Module Setup

### T003: [P] Create northstar-kafka-common Module
**Description**: Create new Maven module for Kafka configuration and event models
**Files**:
- `northstar-kafka-common/pom.xml`
- `northstar-kafka-common/src/main/java/com/northstar/funding/kafka/config/KafkaConfig.java`
- `northstar-kafka-common/src/main/java/com/northstar/funding/kafka/topics/KafkaTopics.java`

**pom.xml dependencies**:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

**KafkaTopics.java** (constants):
```java
public final class KafkaTopics {
    public static final String SEARCH_REQUESTS = "search-requests";
    public static final String SEARCH_RESULTS_RAW = "search-results-raw";
    public static final String SEARCH_RESULTS_VALIDATED = "search-results-validated";
    public static final String WORKFLOW_ERRORS = "workflow-errors";
}
```

**Success Criteria**: Module compiles, no runtime dependencies on other modules

### T004: [P] Create northstar-search-adapters Module
**Description**: Create new Maven module for search engine adapters
**Files**:
- `northstar-search-adapters/pom.xml`
- `northstar-search-adapters/src/main/java/com/northstar/funding/search/adapter/SearchAdapter.java` (interface)

**pom.xml dependencies**:
```xml
<dependencies>
    <dependency>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

**SearchAdapter.java** (interface):
```java
public interface SearchAdapter {
    List<SearchResult> search(String query, int maxResults);
    SearchEngineType getEngineType();
}
```

**Success Criteria**: Module compiles, interface is clean and minimal

### T005: [P] Create northstar-search-workflow Module
**Description**: Create new Maven module for Kafka consumers and workflow orchestration
**Files**:
- `northstar-search-workflow/pom.xml`

**pom.xml dependencies**:
```xml
<dependencies>
    <dependency>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-kafka-common</artifactId>
    </dependency>
    <dependency>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-search-adapters</artifactId>
    </dependency>
    <dependency>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-persistence</artifactId>
    </dependency>
    <dependency>
        <groupId>com.northstar.funding</groupId>
        <artifactId>northstar-crawler</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
</dependencies>
```

**Success Criteria**: Module compiles with all dependencies resolved

### T006: Update Parent POM
**Description**: Add 3 new modules to parent pom.xml
**File**: `pom.xml` (root)

**Changes**:
```xml
<modules>
    <module>northstar-domain</module>
    <module>northstar-persistence</module>
    <module>northstar-crawler</module>
    <module>northstar-query-generation</module>
    <module>northstar-kafka-common</module>      <!-- NEW -->
    <module>northstar-search-adapters</module>   <!-- NEW -->
    <module>northstar-search-workflow</module>   <!-- NEW -->
    <module>northstar-application</module>
</modules>
```

**Success Criteria**: `mvn clean compile` succeeds at root level

---

## Phase 3: Kafka Event Models (TDD - Tests First)

### T007: [P] Test: SearchRequestEvent Model
**Description**: Write unit test for SearchRequestEvent POJO
**File**: `northstar-kafka-common/src/test/java/com/northstar/funding/kafka/events/SearchRequestEventTest.java`

**Test Requirements**:
- Validate all fields are serializable to JSON
- Test equals/hashCode (if implemented)
- Test toString contains key fields
- Test Builder pattern (if using Lombok @Builder)

**Success Criteria**: Test compiles and **FAILS** (model doesn't exist yet)

### T008: [P] Test: SearchResultsRawEvent Model
**Description**: Write unit test for SearchResultsRawEvent POJO
**File**: `northstar-kafka-common/src/test/java/com/northstar/funding/kafka/events/SearchResultsRawEventTest.java`

**Success Criteria**: Test compiles and **FAILS**

### T009: [P] Test: SearchResultsValidatedEvent Model
**Description**: Write unit test for SearchResultsValidatedEvent POJO
**File**: `northstar-kafka-common/src/test/java/com/northstar/funding/kafka/events/SearchResultsValidatedEventTest.java`

**Success Criteria**: Test compiles and **FAILS**

### T010: [P] Test: WorkflowErrorEvent Model
**Description**: Write unit test for WorkflowErrorEvent POJO
**File**: `northstar-kafka-common/src/test/java/com/northstar/funding/kafka/events/WorkflowErrorEventTest.java`

**Success Criteria**: Test compiles and **FAILS**

---

## Phase 4: Kafka Event Models (Implementation)

### T011: [P] Implement SearchRequestEvent
**Description**: Create SearchRequestEvent POJO with all workflow metadata
**File**: `northstar-kafka-common/src/main/java/com/northstar/funding/kafka/events/SearchRequestEvent.java`

**Fields**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestEvent {
    private UUID requestId;
    private String query;
    private SearchEngineType searchEngine;
    private FundingCategory category;
    private String region;  // ISO 2-letter code (BG, RO, etc.)
    private FundingType fundingType;
    private RecipientOrganizationType recipientType;
    private Instant timestamp;
}
```

**Success Criteria**: T007 test passes

### T012: [P] Implement SearchResultsRawEvent
**Description**: Create SearchResultsRawEvent POJO for raw search results
**File**: `northstar-kafka-common/src/main/java/com/northstar/funding/kafka/events/SearchResultsRawEvent.java`

**Fields**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultsRawEvent {
    private UUID requestId;
    private SearchEngineType searchEngine;
    private List<SearchResultDto> results;  // DTO to avoid JPA issues
    private int totalResults;
    private long executionTimeMs;
    private Instant timestamp;
}
```

**Success Criteria**: T008 test passes

### T013: [P] Implement SearchResultsValidatedEvent
**Description**: Create SearchResultsValidatedEvent POJO for validated results
**File**: `northstar-kafka-common/src/main/java/com/northstar/funding/kafka/events/SearchResultsValidatedEvent.java`

**Fields**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultsValidatedEvent {
    private UUID requestId;
    private List<ValidatedResultDto> validResults;
    private ValidationStatistics stats;  // nested object
    private Instant timestamp;
}
```

**Success Criteria**: T009 test passes

### T014: [P] Implement WorkflowErrorEvent
**Description**: Create WorkflowErrorEvent POJO for error tracking
**File**: `northstar-kafka-common/src/main/java/com/northstar/funding/kafka/events/WorkflowErrorEvent.java`

**Fields**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowErrorEvent {
    private UUID requestId;
    private WorkflowStage stage;  // enum
    private String errorType;
    private String errorMessage;
    private String stackTrace;
    private String originalMessage;  // JSON of failed event
    private int retryCount;
    private Instant timestamp;
}
```

**Success Criteria**: T010 test passes

---

## Phase 5: Search Adapter (TDD - Tests First)

### T015: Test: SearXNGAdapter
**Description**: Write integration test for SearXNGAdapter (requires SearXNG running)
**File**: `northstar-search-adapters/src/test/java/com/northstar/funding/search/searxng/SearXNGAdapterTest.java`

**Test Cases**:
- Search returns 20-25 results for valid query
- Empty results handled gracefully (0 results)
- HTTP timeout throws appropriate exception
- Malformed JSON response throws appropriate exception
- Results contain URL, title, description

**Success Criteria**: Test compiles and **FAILS**

---

## Phase 6: Search Adapter (Implementation)

### T016: Implement SearXNGAdapter
**Description**: Create SearXNG search adapter implementation
**File**: `northstar-search-adapters/src/main/java/com/northstar/funding/search/searxng/SearXNGAdapter.java`

**Implementation**:
```java
@Service
public class SearXNGAdapter implements SearchAdapter {
    private final RestTemplate restTemplate;
    private final String searxngBaseUrl = "http://192.168.1.10:8080";

    @Override
    public List<SearchResult> search(String query, int maxResults) {
        // Call SearXNG /search API with format=json
        // Parse JSON response
        // Map to SearchResult objects
        // Handle errors (timeout, 500, malformed JSON)
    }

    @Override
    public SearchEngineType getEngineType() {
        return SearchEngineType.SEARXNG;
    }
}
```

**Success Criteria**: T015 test passes

---

## Phase 7: Valkey Cache Service (TDD - Tests First)

### T017: Test: DomainBlacklistCache
**Description**: Write unit test for Valkey-backed blacklist cache (mock Valkey)
**File**: `northstar-search-workflow/src/test/java/com/northstar/funding/workflow/service/DomainBlacklistCacheTest.java`

**Test Cases**:
- Cache hit returns cached value (no DB query)
- Cache miss queries PostgreSQL and caches result
- Blacklist update invalidates cache
- Valkey unavailable falls back to PostgreSQL

**Success Criteria**: Test compiles and **FAILS**

---

## Phase 8: Valkey Cache Service (Implementation)

### T018: Implement DomainBlacklistCache
**Description**: Create read-through cache for domain blacklist using Valkey
**File**: `northstar-search-workflow/src/main/java/com/northstar/funding/workflow/service/DomainBlacklistCache.java`

**Implementation**:
```java
@Service
public class DomainBlacklistCache {
    private final RedisTemplate<String, Boolean> redisTemplate;
    private final DomainRepository domainRepository;
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final Duration TTL = Duration.ofHours(24);

    public boolean isBlacklisted(String domain) {
        // 1. Check Valkey cache
        // 2. On miss, query PostgreSQL
        // 3. Cache result with TTL
        // 4. Return boolean
    }

    public void markBlacklisted(String domain) {
        // 1. Update PostgreSQL
        // 2. Invalidate cache key
    }
}
```

**Success Criteria**: T017 test passes

---

## Phase 9: Kafka Consumers (TDD - Tests First)

### T019: [P] Test: SearchRequestConsumer
**Description**: Write unit test for SearchRequestConsumer (mock Kafka, SearXNG adapter)
**File**: `northstar-search-workflow/src/test/java/com/northstar/funding/workflow/consumer/SearchRequestConsumerTest.java`

**Test Cases**:
- Consumes SearchRequestEvent from topic
- Calls SearXNGAdapter.search()
- Publishes SearchResultsRawEvent to next topic
- Handles SearXNG errors (publishes to workflow-errors topic)

**Success Criteria**: Test compiles and **FAILS**

### T020: [P] Test: DomainProcessorConsumer
**Description**: Write unit test for DomainProcessorConsumer
**File**: `northstar-search-workflow/src/test/java/com/northstar/funding/workflow/consumer/DomainProcessorConsumerTest.java`

**Test Cases**:
- Consumes SearchResultsRawEvent
- Extracts domains from URLs
- Checks blacklist via DomainBlacklistCache
- Deduplicates by domain (in-memory HashSet)
- Registers new domains via DomainService
- Publishes SearchResultsValidatedEvent

**Success Criteria**: Test compiles and **FAILS**

### T021: [P] Test: ScoringConsumer
**Description**: Write unit test for ScoringConsumer
**File**: `northstar-search-workflow/src/test/java/com/northstar/funding/workflow/consumer/ScoringConsumerTest.java`

**Test Cases**:
- Consumes SearchResultsValidatedEvent
- Calls existing ConfidenceScorer for each result
- Filters by threshold (≥0.60)
- Creates FundingSourceCandidate via CandidateCreationService
- Updates DiscoverySession statistics

**Success Criteria**: Test compiles and **FAILS**

---

## Phase 10: Kafka Consumers (Implementation)

### T022: Implement SearchRequestConsumer
**Description**: Create Kafka consumer for search-requests topic
**File**: `northstar-search-workflow/src/main/java/com/northstar/funding/workflow/consumer/SearchRequestConsumer.java`

**Implementation**:
```java
@Service
public class SearchRequestConsumer {
    private final SearchAdapter searxngAdapter;
    private final KafkaTemplate<String, SearchResultsRawEvent> kafkaTemplate;
    private final WorkflowErrorHandler errorHandler;

    @KafkaListener(topics = KafkaTopics.SEARCH_REQUESTS, groupId = "search-executor")
    public void handleSearchRequest(SearchRequestEvent event) {
        // 1. Execute search via SearXNGAdapter
        // 2. Build SearchResultsRawEvent
        // 3. Publish to search-results-raw topic
        // 4. On error: publish to workflow-errors
    }
}
```

**Success Criteria**: T019 test passes

### T023: Implement DomainProcessorConsumer
**Description**: Create Kafka consumer for search-results-raw topic
**File**: `northstar-search-workflow/src/main/java/com/northstar/funding/workflow/consumer/DomainProcessorConsumer.java`

**Implementation**:
```java
@Service
public class DomainProcessorConsumer {
    private final DomainBlacklistCache blacklistCache;
    private final DomainService domainService;
    private final KafkaTemplate<String, SearchResultsValidatedEvent> kafkaTemplate;
    private final Map<UUID, Set<String>> sessionDomains = new ConcurrentHashMap<>();

    @KafkaListener(topics = KafkaTopics.SEARCH_RESULTS_RAW, groupId = "domain-processor")
    public void handleSearchResults(SearchResultsRawEvent event) {
        // 1. Extract domain from each URL
        // 2. Check blacklist (Valkey cache)
        // 3. Check session-level duplicates (in-memory)
        // 4. Register new domains (DomainService)
        // 5. Build SearchResultsValidatedEvent with stats
        // 6. Publish to search-results-validated topic
    }
}
```

**Success Criteria**: T020 test passes

### T024: Implement ScoringConsumer
**Description**: Create Kafka consumer for search-results-validated topic
**File**: `northstar-search-workflow/src/main/java/com/northstar/funding/workflow/consumer/ScoringConsumer.java`

**Implementation**:
```java
@Service
public class ScoringConsumer {
    private final ConfidenceScorer confidenceScorer;
    private final CandidateCreationService candidateCreationService;
    private final DiscoverySessionService sessionService;

    @KafkaListener(topics = KafkaTopics.SEARCH_RESULTS_VALIDATED, groupId = "scoring-processor")
    public void handleValidatedResults(SearchResultsValidatedEvent event) {
        // 1. Score each result (existing ConfidenceScorer)
        // 2. Filter by threshold (≥0.60)
        // 3. Create FundingSourceCandidate entities
        // 4. Update session statistics
    }
}
```

**Success Criteria**: T021 test passes

---

## Phase 11: REST API (TDD - Tests First)

### T025: Test: SearchController POST /api/search/execute
**Description**: Write integration test for search execution endpoint
**File**: `northstar-application/src/test/java/com/northstar/funding/application/controller/SearchControllerTest.java`

**Test Cases**:
- Valid request returns requestId and status=INITIATED
- Invalid category returns 400 Bad Request
- Missing required fields returns 400 Bad Request
- Publishes SearchRequestEvent to Kafka (verify via KafkaTestUtils)

**Success Criteria**: Test compiles and **FAILS**

---

## Phase 12: REST API (Implementation)

### T026: Implement SearchController
**Description**: Create REST controller for search workflow triggers
**File**: `northstar-application/src/main/java/com/northstar/funding/application/controller/SearchController.java`

**Endpoints**:
```java
@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Search execution and management")
public class SearchController {

    @PostMapping("/execute")
    @Operation(summary = "Execute search workflow")
    public ResponseEntity<SearchExecutionResponse> executeSearch(
        @RequestBody @Valid SearchExecutionRequest request
    ) {
        // 1. Generate queries (QueryGenerationService)
        // 2. For each query, publish SearchRequestEvent to Kafka
        // 3. Return requestId for tracking
    }

    @GetMapping("/status/{requestId}")
    @Operation(summary = "Get search status")
    public ResponseEntity<SearchStatusResponse> getSearchStatus(
        @PathVariable UUID requestId
    ) {
        // Query database for candidates by requestId
    }
}
```

**Success Criteria**: T025 test passes

### T027: Add SpringDoc OpenAPI Configuration
**Description**: Configure SpringDoc for Swagger UI
**File**: `northstar-application/src/main/java/com/northstar/funding/application/config/OpenAPIConfig.java`

**Configuration**:
```java
@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NorthStar Funding Discovery API")
                .version("1.0")
                .description("Event-driven search workflow API"));
    }
}
```

**Success Criteria**: Swagger UI accessible at `http://localhost:8090/swagger-ui.html`

---

## Phase 13: Integration Testing

### T028: End-to-End Integration Test
**Description**: Write end-to-end test with TestContainers (Kafka + PostgreSQL + Valkey)
**File**: `northstar-application/src/test/java/com/northstar/funding/application/integration/SearchWorkflowIntegrationTest.java`

**Test Flow**:
1. POST /api/search/execute → returns requestId
2. Verify SearchRequestEvent published to Kafka
3. Verify SearchResultsRawEvent appears (after SearXNG call)
4. Verify SearchResultsValidatedEvent appears (after domain processing)
5. Verify FundingSourceCandidate created in PostgreSQL
6. Verify Domain registered in PostgreSQL
7. Verify confidence score ≥0.60

**Success Criteria**: Full workflow executes end-to-end, all assertions pass

---

## Phase 14: Database Migration

### T029: Create Flyway Migration V18
**Description**: Add index for search result request_id lookups
**File**: `northstar-persistence/src/main/resources/db/migration/V18__add_request_id_index.sql`

**SQL**:
```sql
-- Enable fast lookup of search results by requestId
CREATE INDEX IF NOT EXISTS idx_search_result_request_id
ON search_result(request_id);

-- Enable fast lookup of candidates by session
CREATE INDEX IF NOT EXISTS idx_candidate_session_id
ON funding_source_candidate(discovery_session_id);
```

**Success Criteria**: Migration runs successfully, indexes created

---

## Phase 15: Configuration & Documentation

### T030: [P] Add Kafka Configuration to application.yml
**Description**: Configure Kafka connection for northstar-application
**File**: `northstar-application/src/main/resources/application.yml`

**Configuration**:
```yaml
spring:
  kafka:
    bootstrap-servers: 192.168.1.10:9092
    consumer:
      group-id: northstar-search
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.northstar.funding.kafka.events
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  data:
    redis:
      host: 192.168.1.10
      port: 6379
```

**Success Criteria**: Application starts without Kafka/Valkey connection errors

### T031: [P] Update README.md
**Description**: Document Feature 009 architecture and usage
**File**: `README.md` (root)

**Sections to Add**:
- Kafka-based search workflow overview
- 4 Kafka topics and their purposes
- How to trigger searches via REST API
- How to monitor Kafka topics via Kafka UI
- Performance targets (<5s end-to-end)

**Success Criteria**: README is clear and accurate

---

## Phase 16: Verification

### T032: Run All Tests
**Description**: Execute full test suite (all modules)
**Command**: `mvn clean test`

**Success Criteria**: All tests pass (existing 637 + new ~20 tests)

### T033: Manual Testing via Postman
**Description**: Manually test search workflow using Postman
**Steps**:
1. Start application: `mvn spring-boot:run -pl northstar-application`
2. POST http://localhost:8090/api/search/execute
   ```json
   {
     "category": "EDUCATION",
     "region": "BG",
     "fundingType": "SCHOLARSHIP",
     "recipientType": "K12_SCHOOL",
     "searchEngine": "SEARXNG"
   }
   ```
3. Check Kafka UI: http://192.168.1.10:8081/kafka-ui
4. Verify messages flow through all 4 topics
5. Check PostgreSQL for created candidates

**Success Criteria**: End-to-end workflow completes successfully

### T034: Performance Validation
**Description**: Verify performance targets met
**Metrics**:
- Query generation: 200-300ms ✓
- Search execution: 1-2 seconds ✓
- Domain validation: <100ms (Valkey) ✓
- End-to-end workflow: <5 seconds ✓

**Success Criteria**: All performance targets met

---

## Dependencies

**Sequential Dependencies**:
- T002 blocks T032 (Ollama migration must complete before tests)
- T003-T006 block all subsequent tasks (module setup required)
- T007-T010 block T011-T014 (tests before implementation)
- T015 blocks T016 (test before implementation)
- T017 blocks T018 (test before implementation)
- T019-T021 block T022-T024 (tests before implementation)
- T025 blocks T026 (test before implementation)
- T028 requires T022-T024, T026 (integration test needs all consumers + API)

**Parallel Opportunities**:
- T003, T004, T005 can run in parallel (independent modules)
- T007-T010 can run in parallel (different test files)
- T011-T014 can run in parallel (different implementation files)
- T019-T021 can run in parallel (different test files)
- T030, T031 can run in parallel (different files)

---

## Parallel Execution Example

```bash
# Phase 2: Module Setup (parallel)
# Launch T003, T004, T005 together:

# Phase 3: Event Model Tests (parallel)
# Launch T007-T010 together:

# Phase 4: Event Model Implementation (parallel)
# Launch T011-T014 together:

# Phase 9: Consumer Tests (parallel)
# Launch T019-T021 together:
```

---

## Validation Checklist

- [x] All Kafka event models have tests (T007-T010)
- [x] All event models have implementation (T011-T014)
- [x] All consumers have tests (T019-T021)
- [x] All consumers have implementation (T022-T024)
- [x] REST API has test (T025)
- [x] REST API has implementation (T026)
- [x] Integration test covers end-to-end (T028)
- [x] Parallel tasks are truly independent
- [x] Each task specifies exact file path
- [x] TDD order: Tests before implementation

---

## Estimated Timeline

- Phase 1 (Prerequisites): 1 hour
- Phase 2 (Module Setup): 2 hours
- Phase 3-4 (Event Models): 3 hours
- Phase 5-6 (Search Adapter): 2 hours
- Phase 7-8 (Valkey Cache): 2 hours
- Phase 9-10 (Kafka Consumers): 6 hours
- Phase 11-12 (REST API): 3 hours
- Phase 13 (Integration Testing): 3 hours
- Phase 14-16 (Migration, Config, Verification): 2 hours

**Total**: ~24 hours (3 days)

---

## Notes

- **TDD**: All tests written and failing before implementation
- **Commit frequency**: After each task completion
- **Ollama migration** (T002) is critical path - run full test suite after
- **Valkey fallback**: Must handle gracefully if cache unavailable
- **Kafka UI**: Use http://192.168.1.10:8081/kafka-ui for debugging
- **Redis Commander**: Use http://192.168.1.10:8082 to inspect Valkey cache
