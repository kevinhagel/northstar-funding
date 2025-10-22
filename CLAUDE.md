# northstar-funding Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-10-21

## Active Technologies
- Java 25 (source and target level 25) via SDKMAN + Spring Boot 3.5.6, Spring Data JDBC, Vavr 0.10.7, Lombok 1.18.42
- Resilience4j 2.2.0 (circuit breakers, retry logic)
- PostgreSQL 16 (Mac Studio @ 192.168.1.10:5432)
- TestContainers for integration testing
- Java 25 Virtual Threads for parallel I/O operations
- Spring RestClient (Spring 6.1+) for HTTP calls

## Search Engine Infrastructure (Feature 003)
**Status**: COMPLETED (T001-T033)

### Search Adapters
- **Searxng**: Self-hosted @ http://192.168.1.10:8080
- **Tavily**: API-based, requires TAVILY_API_KEY
- **Perplexity**: API-based, requires PERPLEXITY_API_KEY

### Domain Model
- `SearchQuery`: Query library entity (TEXT[] for tags/engines to avoid JSONB complexity)
- `SearchSessionStatistics`: Per-engine performance metrics
- Tags format: `"TYPE:value"` (e.g., `"GEOGRAPHY:Bulgaria"`)
- Engines format: Enum names as strings (e.g., `"SEARXNG"`, `"TAVILY"`)

### Key Design Decisions
- **Set<String> instead of complex objects**: Avoids Spring Data JDBC relationship interpretation
- **No JSONB in early development**: Use TEXT[] for simplicity, migrate later if needed
- **Domain-level deduplication**: Use `java.net.URI.getHost()` for simple extraction
- **Circuit breakers per engine**: tavily, searxng, perplexity instances
- **Virtual Threads**: Parallel search execution across engines (Java 25)
- **Graceful degradation**: System continues with working engines when one fails

### Circuit Breaker Configuration
```yaml
minimumNumberOfCalls: 5
failureRateThreshold: 50%
waitDurationInOpenState: 30s
```

### Query Library
- 7-day schedule (Monday-Sunday) in `application.yml`
- Focus: Bulgaria, Balkans, Eastern Europe, EU
- Categories: Education, Infrastructure, STEM, Arts, Teacher Development

## Project Structure
```
backend/
  src/main/java/com/northstar/funding/
    discovery/
      search/
        domain/              # SearchQuery, QueryTag, SearchEngineType
        application/         # SearchExecutionService, NightlyDiscoveryScheduler
        infrastructure/
          adapters/          # SearxngAdapter, TavilyAdapter, PerplexityAdapter
      config/                # JdbcConfiguration (custom converters)
  src/main/resources/
    db/migration/
      V10__create_search_queries_table.sql
      V11__create_search_session_statistics_table.sql
      V12__extend_discovery_session_for_search.sql
  src/test/java/com/northstar/funding/
    integration/            # MondayNightlyDiscoveryTest, DomainDeduplicationTest, CircuitBreakerTest
    discovery/search/       # Unit tests for domain, repositories, scheduler
```

## Common Commands
```bash
# Run all tests
mvn test

# Run specific integration test
mvn test -Dtest=MondayNightlyDiscoveryTest
mvn test -Dtest=DomainDeduplicationTest
mvn test -Dtest=CircuitBreakerTest

# Run with clean database (Flyway)
mvn flyway:clean flyway:migrate

# Build
mvn clean package
```

## Code Style
Java 25: Follow standard conventions

### Spring Data JDBC Best Practices
1. **Use TEXT[] for simple arrays**: Avoid JSONB complexity
2. **Create custom converters**: QueryTagSetConverter, SearchEngineTypeSetConverter
3. **Parse in application layer**: Helper methods like `getParsedTags()`, `getParsedTargetEngines()`
4. **Register repository packages**: Update JdbcConfiguration @EnableJdbcRepositories

### Testing Best Practices
1. **Use @Testcontainers**: PostgreSQL for realistic integration tests
2. **Use @Transactional**: Automatic cleanup between tests
3. **DynamicPropertySource**: Override properties for test environment
4. **@ActiveProfiles("test")**: Separate test configuration
5. **MANDATORY: Check existing Testcontainers tests BEFORE writing new ones**: When writing any new test that uses @Testcontainers, you MUST first read an existing working test (e.g., DiscoveryWorkflowIntegrationTest.java) and copy the exact pattern, including @DynamicPropertySource configuration. DO NOT create new Testcontainers tests without reviewing existing working examples first.

## Recent Changes
- 003-search-execution-infrastructure (T001-T033): COMPLETED
  - Search execution service with Virtual Threads parallelism
  - Circuit breaker protection per search engine
  - Domain-level deduplication
  - 7-day query library (Monday-Sunday)
  - Integration tests: Monday nightly discovery, domain deduplication, circuit breaker
  - Custom Spring Data JDBC converters for TEXT[] ↔ Set<String>
- 002-create-automated-crawler: Added Java 25 + Spring Boot 3.5.5, Spring Data JDBC, Vavr 0.10.6, Lombok, Jsoup

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
