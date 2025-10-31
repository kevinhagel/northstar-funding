# Technology Stack - NorthStar Funding Discovery

**Date**: 2025-10-31
**Status**: Current Production Stack
**Tags**: #technology #java #spring-boot #postgres #infrastructure

---

## Overview

NorthStar Funding Discovery uses modern Java ecosystem technologies with emphasis on:
- **Simplicity**: Proven technologies, minimal magic
- **Performance**: Virtual Threads for parallel I/O
- **Reliability**: Circuit breakers, retry logic, ACID transactions
- **Local-first**: Self-hosted infrastructure on Mac Studio

---

## Core Application Stack

### Java 25
**Version**: Java 25 (source and target level 25)
**Installation**: SDKMAN
**Key Features Used**:
- **Virtual Threads** (Project Loom / JEP 444): Lightweight concurrency for I/O operations
- Pattern matching improvements
- Record patterns
- String templates (preview)

**Why Java 25**:
- Virtual Threads enable massive parallelization for I/O-bound tasks
- 3x speedup for parallel search engine queries
- Scales to hundreds of concurrent HTTP calls
- Much simpler than reactive programming

**Links**:
- [Java 25 Documentation](https://docs.oracle.com/en/java/javase/25/)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [[java-25-virtual-threads]] - Deep dive

---

### Spring Boot 3.5.6
**Framework**: Spring Boot 3.5.6
**Spring Framework**: 6.1+ (includes Spring 6.1 RestClient)

**Why Spring Boot**:
- Mature, production-proven
- Excellent ecosystem
- Spring Data JDBC for database access
- Built-in actuator for health monitoring
- Auto-configuration reduces boilerplate

**Key Modules**:
- `spring-boot-starter-web` - REST APIs
- `spring-boot-starter-data-jdbc` - Database access
- `spring-boot-starter-webflux` - Reactive HTTP clients
- `spring-boot-starter-actuator` - Health monitoring
- `spring-boot-starter-test` - Testing infrastructure

**Links**:
- [Spring Boot 3.5.x](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [[spring-boot]] - Configuration patterns and best practices

---

### Spring Data JDBC
**Version**: Included with Spring Boot 3.5.6
**Pattern**: Repository pattern with custom converters

**Why Spring Data JDBC** (not JPA/Hibernate):
- Simpler, more transparent
- No lazy loading surprises
- Direct SQL control via Flyway
- Better performance for read-heavy workloads
- Easier to reason about

**Custom Converters**:
- `QueryTagSetConverter`: TEXT[] ↔ Set<String>
- `SearchEngineTypeSetConverter`: TEXT[] ↔ Set<String>
- Registered in `JdbcConfiguration` with `@EnableJdbcRepositories`

**Example**:
```java
@Component
public class QueryTagSetConverter implements Converter<String[], Set<String>> {
    @Override
    public Set<String> convert(String[] source) {
        return source != null ? Set.of(source) : Set.of();
    }
}
```

**Links**:
- [Spring Data JDBC](https://spring.io/projects/spring-data-jdbc)
- [[postgresql]] - Database patterns and schema

---

### PostgreSQL 16
**Version**: PostgreSQL 16
**Location**: Mac Studio @ 192.168.1.10:5432
**Database**: northstar_funding
**User**: northstar_user

**Why PostgreSQL**:
- Rock-solid ACID compliance
- Excellent JSON support (future use)
- TEXT[] for simple arrays
- Full-text search capabilities (future)
- pgvector extension for vector search (future with Qdrant)

**Schema Management**: Flyway migrations
**Current Migrations**: V1 through V14
- V1: funding_source_candidate table
- V2: contact_intelligence table
- V3: admin_user table
- V4: discovery_session table
- V5: enhancement_record table
- V6: indexes
- V7: fix enhancement_record constraint
- V8: domain table
- V9: update candidate status (two-phase workflow)
- V10: search_queries table
- V11: search_session_statistics table
- V12: extend discovery_session for search
- V13: query_generation_sessions table
- V14: metadata_judgments table

**Links**:
- [PostgreSQL 16](https://www.postgresql.org/docs/16/)
- [[postgresql]] - Deep dive on schema and patterns

---

## Functional Programming

### Vavr 0.10.7
**Library**: Vavr (formerly Javaslang)
**Purpose**: Functional data structures and utilities

**Key Features Used**:
- `Try<T>` - Error handling without try/catch
- `Option<T>` - Null-safe optionals
- Immutable collections
- Pattern matching

**Example**:
```java
Try<List<SearchResult>> result = Try.of(() -> adapter.search(query, maxResults))
    .onFailure(e -> log.error("Search failed", e))
    .recover(ex -> List.of());  // Graceful degradation
```

**Why Vavr**:
- Makes error handling explicit
- Graceful degradation patterns
- Composition over exceptions
- Works well with Java records

**Links**:
- [Vavr Documentation](https://www.vavr.io/)

---

### Lombok 1.18.42
**Library**: Project Lombok
**Purpose**: Reduce boilerplate code

**Annotations Used**:
- `@Data` - Getters, setters, equals, hashCode, toString
- `@Builder` - Builder pattern
- `@Slf4j` - Logger injection
- `@RequiredArgsConstructor` - Constructor injection
- `@Value` - Immutable value objects

**Example**:
```java
@Data
@Builder
@Table("funding_source_candidate")
public class FundingSourceCandidate {
    @Id
    private UUID candidateId;
    private String sourceUrl;
    private String organizationName;
    // ... Lombok generates all boilerplate
}
```

**Links**:
- [Project Lombok](https://projectlombok.org/)

---

## Resilience & HTTP

### Resilience4j 2.2.0
**Library**: Resilience4j
**Purpose**: Fault tolerance patterns

**Patterns Used**:
- **Circuit Breaker**: Prevent cascading failures
- **Retry**: Automatic retry with exponential backoff
- **Rate Limiter**: (future) Throttle API calls
- **Bulkhead**: (future) Isolate thread pools

**Circuit Breaker Configuration**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      tavily:
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      searxng:
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      perplexity:
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

**Retry Configuration**:
```yaml
resilience4j:
  retry:
    instances:
      searchEngines:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

**Usage**:
```java
@CircuitBreaker(name = "tavily", fallbackMethod = "searchFallback")
@Retry(name = "searchEngines")
public List<SearchResult> search(String query, int maxResults) {
    // Call external API
}

private List<SearchResult> searchFallback(String query, int maxResults, Throwable t) {
    log.warn("Circuit breaker fallback for query: {}", query, t);
    return List.of();  // Graceful degradation
}
```

**Why Resilience4j**:
- Independent circuit breakers per search engine
- Graceful degradation (system continues with working engines)
- Automatic retry with exponential backoff
- Better than Hystrix (no longer maintained)

**Links**:
- [Resilience4j](https://resilience4j.readme.io/)
- [[resilience4j]] - Circuit breaker patterns
- [[003-circuit-breaker-per-engine]] - ADR on per-engine circuit breakers

---

### Spring RestClient & WebFlux
**HTTP Clients**:
- **Spring RestClient** (Spring 6.1+): Synchronous HTTP calls
- **Spring WebFlux**: Reactive HTTP calls (search adapters)

**Why both**:
- RestClient: Simple, synchronous calls when needed
- WebFlux: Non-blocking I/O for search adapters
- Virtual Threads make synchronous code performant

**Example (WebClient in adapter)**:
```java
@Service
public class SearxngAdapter implements SearchEngineAdapter {
    private final WebClient webClient;

    public List<SearchResult> search(String query, int maxResults) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("num_results", maxResults)
                .build())
            .retrieve()
            .bodyToMono(SearxngResponse.class)
            .map(this::convertToSearchResults)
            .block();  // Block in Virtual Thread context
    }
}
```

---

## Database & Migrations

### Flyway
**Version**: Included with Spring Boot
**Purpose**: Database schema version control

**Migration Pattern**:
```
backend/src/main/resources/db/migration/
├── V1__create_funding_source_candidate_table.sql
├── V2__create_contact_intelligence_table.sql
├── V3__create_admin_user_table.sql
...
├── V14__create_metadata_judgments_table.sql
```

**Configuration** (pom.xml):
```xml
<plugin>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-maven-plugin</artifactId>
    <configuration>
        <url>jdbc:postgresql://192.168.1.10:5432/northstar_funding</url>
        <user>northstar_user</user>
        <password>northstar_password</password>
    </configuration>
</plugin>
```

**Commands**:
```bash
mvn flyway:migrate       # Apply all pending migrations
mvn flyway:info          # Show migration status
mvn flyway:validate      # Validate migrations
mvn flyway:clean         # DESTRUCTIVE - drops all objects
```

**Why Flyway**:
- Version control for database schema
- Repeatable, auditable migrations
- Works across environments (dev, test, prod)
- Simple SQL files (no DSL to learn)

**Links**:
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

## Testing

### JUnit 5
**Framework**: JUnit 5 (Jupiter)
**Integration**: Spring Boot Test

**Test Types**:
1. **Unit Tests**: `src/test/java/com/northstar/funding/discovery/`
2. **Integration Tests**: `src/test/java/com/northstar/funding/integration/`
3. **Controller Tests**: `src/test/java/com/northstar/funding/web/`

---

### TestContainers
**Library**: TestContainers
**Purpose**: Integration testing with real PostgreSQL

**Pattern** (MANDATORY - copy from existing tests):
```java
@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("postgres-test")
public class MondayNightlyDiscoveryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void testSomething() {
        // Test with real PostgreSQL
    }
}
```

**Why TestContainers**:
- Test against real PostgreSQL (not H2/in-memory)
- Fast startup (Alpine image)
- Isolated test environment
- Automatic cleanup

**Links**:
- [TestContainers](https://www.testcontainers.org/)

---

### REST Assured
**Library**: REST Assured
**Purpose**: API testing

**Example**:
```java
given()
    .contentType(ContentType.JSON)
    .body(request)
.when()
    .post("/api/funding-sources")
.then()
    .statusCode(201)
    .body("organizationName", equalTo("America for Bulgaria Foundation"));
```

---

### Mockito
**Library**: Mockito
**Purpose**: Mocking for unit tests

**Example**:
```java
@Mock
private DomainRepository domainRepository;

@InjectMocks
private DomainRegistryService service;

@Test
void testShouldProcessNewDomain() {
    when(domainRepository.findByDomainName("us-bulgaria.org"))
        .thenReturn(Optional.empty());

    DomainCheckResult result = service.shouldProcess("us-bulgaria.org");

    assertTrue(result.isShouldProcess());
}
```

---

## AI Infrastructure (Mac Studio @ 192.168.1.10)

### LM Studio
**Purpose**: Local LLM inference (OpenAI-compatible API)
**Location**: Mac Studio @ http://192.168.1.10:1234/v1
**API**: OpenAI-compatible endpoints

**Planned Use Cases**:
1. **Query Understanding**: Parse natural language queries
2. **Embedding Generation**: Convert markdown → vector embeddings (BGE-M3)
3. **Re-ranking**: Analyze query + results → determine relevance
4. **Query Generation**: Generate targeted search queries
5. **Extraction**: Extract contact intelligence, funding amounts, deadlines

**Why LM Studio**:
- Local control (no external API costs)
- Privacy (no data sent to OpenAI/Anthropic)
- Multiple models for specialized tasks
- Fast inference on Mac Studio hardware
- OpenAI-compatible API (easy integration)

**Links**:
- [LM Studio](https://lmstudio.ai/)
- [[lm-studio]] - Setup and model selection guide

---

### Qdrant (Future - Vector Database)
**Purpose**: Vector search for RAG system
**Status**: Planned for Feature 005+

**Planned Architecture**:
- Store funding source embeddings (768 or 1024 dimensions)
- Hybrid search (vector similarity + metadata filters)
- Two collections: `funding_sources_current`, `funding_sources_historical`

**Why Qdrant**:
- Open source
- Fast vector search
- Excellent filtering capabilities
- Python and REST APIs
- Can run locally or cloud

**Links**:
- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [[qdrant]] - Architecture and integration plans
- [[rag-system]] - Complete RAG architecture

---

## Search Engines

### Searxng (Self-hosted)
**Type**: Meta-search engine (self-hosted)
**Location**: Mac Studio @ http://192.168.1.10:8080
**API**: No key required

**Why Searxng**:
- Aggregates results from multiple search engines
- Self-hosted (no API costs)
- Privacy-focused
- Customizable

**Adapter**: `SearxngAdapter`
**Circuit Breaker**: `searxng`

---

### Tavily (API)
**Type**: AI-powered search API
**API Key**: `TAVILY_API_KEY` environment variable
**Endpoint**: External API

**Why Tavily**:
- AI-optimized search results
- Clean, structured responses
- Good for factual queries

**Adapter**: `TavilyAdapter`
**Circuit Breaker**: `tavily`

**Links**:
- [Tavily AI Search](https://tavily.com/)

---

### Perplexity (API)
**Type**: AI-powered search API
**API Key**: `PERPLEXITY_API_KEY` environment variable
**Endpoint**: External API

**Why Perplexity**:
- Conversational search
- Cited sources
- Good for research queries

**Adapter**: `PerplexityAdapter`
**Circuit Breaker**: `perplexity`

**Links**:
- [Perplexity AI](https://www.perplexity.ai/)

---

## Development Tools

### Maven
**Build Tool**: Apache Maven 3.x
**Configuration**: `pom.xml` in `backend/`

**Key Commands**:
```bash
mvn clean package         # Build JAR
mvn test                  # Run tests
mvn test-compile          # Compile tests (syntax check)
mvn spring-boot:run       # Run application
mvn flyway:migrate        # Run database migrations
```

---

### SDKMAN
**Purpose**: Java version management
**Usage**: Install and switch Java versions

```bash
sdk list java              # List available Java versions
sdk install java 25        # Install Java 25
sdk use java 25            # Switch to Java 25
sdk default java 25        # Set default
```

---

### IntelliJ IDEA
**IDE**: IntelliJ IDEA (recommended)
**Plugins**:
- Lombok Plugin
- Spring Boot Plugin
- Database Tools

---

## Infrastructure (Mac Studio @ 192.168.1.10)

### Hardware
- **Mac Studio**: M1 Max or M2 Max
- **Location**: Local network @ 192.168.1.10
- **Services**:
  - PostgreSQL 16 (port 5432)
  - Searxng (port 8080)
  - LM Studio (port 1234)

### Development Machine
- **MacBook M2**: Development workstation
- **Connection**: Local network or remote SSH

---

## Environment Variables

### Required
```bash
export TAVILY_API_KEY=tvly-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Optional
```bash
export DISCOVERY_SCHEDULE_ENABLED=true  # Enable nightly scheduler
export SPRING_PROFILES_ACTIVE=dev        # Activate dev profile
```

---

## Future Technologies (Planned)

### Kafka
**Purpose**: Event streaming and buffering
**Status**: Designed, not yet implemented

**Use Cases**:
- `domains-discovered` topic
- `domains-ready-to-crawl` topic
- `candidates-created` topic
- `vectorization-jobs` topic

**Why defer**:
- Current volume doesn't require event streaming
- Simpler to add later than to remove
- Direct database access sufficient for now

**Links**:
- `docs/data-storage-strategy.md` - Kafka architecture design

---

### Redis/Valkey
**Purpose**: Optional caching and session storage
**Status**: Designed, not yet implemented

**Use Cases**:
- Dashboard session storage
- Blacklist cache (if PostgreSQL becomes bottleneck)
- Rate limiting cache

**Why defer**:
- PostgreSQL performance sufficient for now
- Add only if bottleneck identified
- Start simple, add complexity when needed

**Links**:
- `docs/data-storage-strategy.md` - Redis usage patterns

---

### Browserbase
**Purpose**: Deep web crawling with JavaScript rendering
**Status**: Planned for Feature 006+

**Use Cases**:
- Crawl JavaScript-heavy sites
- Extract full page content
- Capture screenshots
- Execute dynamic content loading

**Why not yet**:
- Expensive (API costs)
- Current metadata judging sufficient
- Deep crawling only for high-confidence candidates

---

## Technology Decision Log

### Key Decisions
1. [[001-text-array-over-jsonb]] - Use TEXT[] for simple arrays
2. [[002-domain-level-deduplication]] - Simple domain extraction
3. [[003-circuit-breaker-per-engine]] - Independent circuit breakers
4. [[004-virtual-threads-parallel-search]] - Java 25 Virtual Threads

### Patterns Established
- Spring Data JDBC over JPA
- Custom converters for TEXT[] columns
- Circuit breaker per external service
- Virtual Threads for parallel I/O
- Flyway for database migrations
- TestContainers for integration tests

---

## Related Documentation

### Technology Deep Dives
- [[java-25-virtual-threads]] - Virtual Threads patterns
- [[spring-boot]] - Spring Boot configuration
- [[postgresql]] - Database schema and patterns
- [[lm-studio]] - Local LLM setup
- [[qdrant]] - Vector database (future)
- [[resilience4j]] - Circuit breaker patterns

### Architecture
- [[architecture-overview]] - System architecture
- [[domain-model]] - Core entities
- [[search-infrastructure]] - Search engine integration
- [[rag-system]] - RAG architecture (future)

### Project
- [[project-overview]] - Project overview
- [[vision-and-mission]] - Project vision

---

**Status**: Current Production Stack (Feature 003)
**Last Updated**: 2025-10-31
**Next Review**: After Feature 004 completion
