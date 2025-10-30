# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NorthStar Funding Discovery is an automated funding discovery workflow with human-AI collaboration. The system discovers funding sources across multiple search engines, performs domain-level deduplication, judges candidates based on metadata, and creates high-confidence funding source candidates for review.

## Technology Stack

### Core Technologies
- **Java 25** (source and target level 25) via SDKMAN
- **Spring Boot 3.5.6** with Spring Data JDBC
- **PostgreSQL 16** (Mac Studio @ 192.168.1.10:5432)
- **Vavr 0.10.7** for functional programming patterns
- **Lombok 1.18.42** for boilerplate reduction
- **Java 25 Virtual Threads** for parallel I/O operations

### Fault Tolerance & HTTP
- **Resilience4j 2.2.0** for circuit breakers and retry logic
- **Spring RestClient** (Spring 6.1+) for HTTP calls
- **Spring WebFlux** for LM Studio and search engine adapters

### Testing
- **JUnit 5** with Spring Boot Test
- **TestContainers** for PostgreSQL integration tests
- **REST Assured** for API testing
- **Mockito** for unit testing

### Infrastructure
- **Flyway** for database migrations
- **Spring Actuator** for health monitoring
- **LM Studio** @ http://192.168.1.10:1234/v1 (OpenAI-compatible API)

## Build & Development Commands

### Maven Commands
```bash
# Build the project
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Compile only
mvn clean compile

# Test compile (useful for checking syntax)
mvn test-compile

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ClassName
mvn test -Dtest=MondayNightlyDiscoveryTest
mvn test -Dtest=DomainDeduplicationTest

# Run integration tests
mvn verify

# Run with specific profile
mvn spring-boot:run -Pdev
mvn spring-boot:run -Pmac-studio
```

### Database (Flyway)
```bash
# Apply all migrations
mvn flyway:migrate

# Clean database (DESTRUCTIVE - drops all objects)
mvn flyway:clean

# Clean and re-migrate (fresh start)
mvn flyway:clean flyway:migrate

# Show migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate
```

### Running the Application
```bash
# Start application (default port 8080, context /api)
mvn spring-boot:run

# Start with environment variables
DISCOVERY_SCHEDULE_ENABLED=true \
TAVILY_API_KEY=tvly-xxx \
PERPLEXITY_API_KEY=pplx-xxx \
mvn spring-boot:run

# Check health
curl http://localhost:8080/api/actuator/health

# Check circuit breaker status
curl http://localhost:8080/api/actuator/health | jq '.components.circuitBreakers'
```

## Architecture

### Package Structure
```
backend/src/main/java/com/northstar/funding/
├── discovery/
│   ├── domain/                    # Core entities (Candidate, Domain, Session, etc.)
│   ├── application/               # Application services
│   ├── service/                   # Business logic services
│   ├── infrastructure/
│   │   ├── config/               # Infrastructure configuration
│   │   ├── converters/           # Spring Data JDBC custom converters
│   │   └── client/               # External API clients
│   ├── search/
│   │   ├── domain/               # SearchQuery, QueryTag, SearchEngineType
│   │   ├── application/          # SearchExecutionService, NightlyDiscoveryScheduler
│   │   └── infrastructure/
│   │       └── adapters/         # SearxngAdapter, TavilyAdapter, PerplexityAdapter
│   ├── config/                   # JdbcConfiguration (custom converters)
│   └── web/                      # REST controllers
└── common/                        # Shared utilities

backend/src/main/resources/
├── db/migration/                  # Flyway migrations (V1__*.sql, V2__*.sql, etc.)
├── application.yml                # Main configuration
└── application-*.yml              # Profile-specific configs

backend/src/test/java/com/northstar/funding/
├── discovery/                     # Unit tests
├── integration/                   # Integration tests with TestContainers
└── web/                          # Controller tests
```

### Core Domain Model

**FundingSourceCandidate**: Main entity representing potential funding sources
- Status: NEW → PENDING_CRAWL → CRAWLED → ENHANCED → JUDGED
- Tracks URL, domain, discovery method, confidence scores

**Domain**: Represents website domains for deduplication
- Tracks blacklist status, quality metrics
- Prevents reprocessing same domains

**DiscoverySession**: Tracks nightly discovery runs
- Session type, status, statistics
- Links to search session statistics

**SearchQuery**: Query library entity (7-day schedule)
- Tags stored as TEXT[] (e.g., "GEOGRAPHY:Bulgaria")
- Target engines as TEXT[] (e.g., "SEARXNG", "TAVILY")

**SearchSessionStatistics**: Per-engine performance metrics
- Queries executed, results returned, response times, failure counts

### Search Engine Infrastructure

**Three Search Adapters** (adapter pattern):
1. **Searxng**: Self-hosted @ http://192.168.1.10:8080 (no API key)
2. **Tavily**: API-based (requires TAVILY_API_KEY)
3. **Perplexity**: API-based (requires PERPLEXITY_API_KEY)

**Key Features**:
- Circuit breaker protection per engine (Resilience4j)
- Virtual Threads for parallel search execution (3x speedup)
- Domain-level deduplication using `java.net.URI.getHost()`
- Graceful degradation (system continues with working engines)
- 7-day query library in application.yml (Monday-Sunday)

**Circuit Breaker Configuration**:
- Minimum calls: 5
- Failure rate threshold: 50%
- Wait duration in open state: 30s
- Retry: 3 attempts with exponential backoff (1s, 2s, 4s)

## Database

### Connection Details
- **Host**: 192.168.1.10:5432 (Mac Studio)
- **Database**: northstar_funding
- **User**: northstar_user
- **Password**: northstar_password (configured in pom.xml Flyway plugin)

### Flyway Migrations
Migrations are located in `backend/src/main/resources/db/migration/`:
- V1: Create funding_source_candidate table
- V2: Create contact_intelligence table
- V3: Create admin_user table
- V4: Create discovery_session table
- V5: Create enhancement_record table
- V6: Create indexes
- V7: Fix enhancement_record constraint
- V8: Create domain table
- V9: Update candidate status (two-phase workflow)
- V10: Create search_queries table
- V11: Create search_session_statistics table
- V12: Extend discovery_session for search
- V13: Create query_generation_sessions table
- V14: Create metadata_judgments table

## Spring Data JDBC Best Practices

### TEXT[] for Simple Arrays
**Decision**: Use TEXT[] instead of JSONB for tags and engines to avoid complexity in early development.

**Pattern**:
```java
// Entity field
private Set<String> tags;  // Stored as TEXT[] in PostgreSQL

// Custom converter
@Component
public class QueryTagSetConverter implements Converter<String[], Set<String>> {
    @Override
    public Set<String> convert(String[] source) {
        return source != null ? Set.of(source) : Set.of();
    }
}

// Register in JdbcConfiguration
@EnableJdbcRepositories(basePackages = "com.northstar.funding.discovery.search.domain")
```

**Why**: Avoids Spring Data JDBC interpreting `Set<ComplexObject>` as one-to-many relationships.

### Custom Converters
Register custom converters for:
- `QueryTagSetConverter`: TEXT[] ↔ Set<String> for tags
- `SearchEngineTypeSetConverter`: TEXT[] ↔ Set<String> for engines

Update `JdbcConfiguration` with `@EnableJdbcRepositories` when adding new repository packages.

### Application-Layer Parsing
Use helper methods like `getParsedTags()`, `getParsedTargetEngines()` to parse stored Set<String> values in the application layer, not in the database.

## Testing Best Practices

### TestContainers Integration Tests

**MANDATORY**: Before writing any new test with @Testcontainers, read an existing working test (e.g., `DiscoveryWorkflowIntegrationTest.java`) and copy the exact pattern.

**Required Annotations**:
```java
@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("postgres-test")
public class YourIntegrationTest {
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
}
```

**Key Points**:
- `@DynamicPropertySource`: Override properties for test environment
- `@Transactional`: Automatic cleanup between tests
- `@ActiveProfiles("postgres-test")`: Uses postgres-test profile from application.yml
- PostgreSQL 16 Alpine image for fast startup

### Test Organization
- **Unit tests**: `backend/src/test/java/com/northstar/funding/discovery/`
- **Integration tests**: `backend/src/test/java/com/northstar/funding/integration/`
- **Web tests**: `backend/src/test/java/com/northstar/funding/web/`

### Running Tests
```bash
# All tests
mvn test

# Specific integration test
mvn test -Dtest=MondayNightlyDiscoveryTest
mvn test -Dtest=DomainDeduplicationTest
mvn test -Dtest=CircuitBreakerTest

# Performance tests (longer runtime)
mvn test -Dtest=SearchPerformanceTest
```

## Configuration Profiles

### Available Profiles
- **dev** (default): Development with Mac Studio PostgreSQL
- **mac-studio**: Production on Mac Studio
- **test**: Standard test configuration
- **postgres-test**: TestContainers integration tests

### Key Configuration Files
- `application.yml`: Main configuration with query library
- Circuit breaker instances: tavily, searxng, perplexity, lmStudio
- Nightly scheduler: Disabled by default (enable with `DISCOVERY_SCHEDULE_ENABLED=true`)

## Development Workflow

### Feature Development
1. Create feature spec in `specs/00X-feature-name/`
2. Design database schema (Flyway migration)
3. Create domain entities with Spring Data JDBC
4. Implement application services
5. Add infrastructure adapters if needed
6. Write comprehensive tests (unit + integration)
7. Update CLAUDE.md with new patterns

### Adding New Search Engines
1. Create adapter implementing `SearchEngineAdapter` interface
2. Add circuit breaker configuration in `application.yml`
3. Add configuration properties for API keys/URLs
4. Register in SearchExecutionService
5. Add integration tests

### Adding New Query Library Entries
Edit `application.yml` under `discovery.query-library`:
```yaml
discovery:
  query-library:
    monday:
      - query: "your search query"
        tags:
          - type: GEOGRAPHY
            value: "Bulgaria"
          - type: CATEGORY
            value: "Education"
        target-engines: ["SEARXNG", "TAVILY", "PERPLEXITY"]
```

## Performance Characteristics

### Search Execution
- Single query (3 engines parallel): 3-8 seconds
- 10 queries (sequential): 5-15 minutes
- Deduplication rate: 40-60% (typical)

### Database Operations
- findByDayOfWeekAndEnabled: <50ms
- save with TEXT[] columns: <100ms
- Flyway migrations: <1s total

### Virtual Threads
- 3x speedup vs sequential execution
- Scales to hundreds of concurrent searches
- Optimized for I/O-bound operations (HTTP calls)

## Common Patterns

### Functional Error Handling (Vavr)
```java
import io.vavr.control.Try;

Try<List<SearchResult>> result = Try.of(() -> adapter.search(query, maxResults))
    .onFailure(e -> log.error("Search failed", e))
    .recover(ex -> List.of());  // Graceful degradation
```

### Virtual Threads Parallel Execution
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var futures = engines.stream()
        .map(engine -> executor.submit(() -> engine.search(query)))
        .toList();

    var results = futures.stream()
        .map(CompletableFuture::join)
        .flatMap(List::stream)
        .toList();
}
```

### Circuit Breaker Protection
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

## Recent Features

### Feature 003: Search Execution Infrastructure (COMPLETED)
- Search engine adapters (Searxng, Tavily, Perplexity)
- Virtual Threads parallel execution
- Circuit breaker protection per engine
- Domain-level deduplication
- 7-day query library (Monday-Sunday)
- Nightly discovery scheduler
- Integration tests: MondayNightlyDiscoveryTest, DomainDeduplicationTest, CircuitBreakerTest

See `specs/003-search-execution-infrastructure/COMPLETION-SUMMARY.md` for detailed documentation.

## Key Design Decisions

### Set<String> Instead of Complex Objects
Use `Set<String>` with TEXT[] columns instead of complex objects to avoid Spring Data JDBC relationship interpretation. Parse in application layer with helper methods.

### No JSONB in Early Development
Use TEXT[] for simplicity. Can migrate to JSONB later if complex querying is needed.

### Domain-Level Deduplication (Simple)
Use `java.net.URI.getHost()` for domain extraction. Defers complex normalization (www/non-www) to future enhancement.

### Circuit Breaker Per Engine
Separate Resilience4j instance for each search engine for better fault isolation and independent failure handling.

## Project Structure

### Documentation
- `CLAUDE.md`: This file
- `docs/`: Architecture and research documents
- `specs/`: Feature specifications and implementation plans
- `.specify/`: Feature planning templates and scripts
- `northstar-notes/`: Obsidian vault for development notes (see Obsidian Vault section below)

### External Infrastructure
All infrastructure runs on Mac Studio @ 192.168.1.10:
- PostgreSQL 16 (port 5432)
- Searxng (port 8080)
- LM Studio (port 1234)

### API Keys (Environment Variables)
```bash
export TAVILY_API_KEY=tvly-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export DISCOVERY_SCHEDULE_ENABLED=true  # Enable nightly scheduler
```

## Obsidian Vault Integration

The project includes an **Obsidian vault** at `northstar-notes/` for development notes, planning, and knowledge management. Claude Code should actively read from and write to this vault.

### Vault Structure

```
northstar-notes/
├── .obsidian/           # Obsidian configuration (do not modify)
├── README.md            # Vault overview and usage guide
├── daily-notes/         # Daily work logs (YYYY-MM-DD.md)
├── session-summaries/   # Claude Code session summaries
├── feature-planning/    # WIP planning before formal specs
├── decisions/           # Architecture Decision Records (ADRs)
└── inbox/               # Quick capture, unprocessed notes
```

### When Claude Code Should Write to the Vault

**MANDATORY - Always write to vault for:**

1. **Session Summaries** (`session-summaries/`)
   - After completing major feature work or milestone
   - After fixing complex bugs or making architectural decisions
   - After major refactoring or performance improvements
   - Format: `YYYY-MM-DD-feature-name.md` or `YYYY-MM-DD-session-N.md`
   - Include: What was accomplished, decisions made, blockers, next steps

2. **Architecture Decision Records** (`decisions/`)
   - When making significant technical decisions
   - When choosing between competing approaches
   - When deviating from established patterns
   - Format: `NNN-decision-title.md` (numbered sequentially)
   - Structure: Context, Decision, Consequences, Alternatives Considered

3. **Feature Planning** (`feature-planning/`)
   - When brainstorming new features before creating formal specs
   - When exploring design alternatives
   - When user provides rough requirements that need refinement
   - Move to `/specs` once solidified

**OPTIONAL - Write when helpful:**

4. **Daily Notes** (`daily-notes/`)
   - When user explicitly requests daily log updates
   - When tracking progress across multiple days on same feature

5. **Inbox** (`inbox/`)
   - For quick TODOs or ideas that come up during development
   - For unresolved questions or blockers to address later

### When Claude Code Should Read from the Vault

**Always check vault before:**

1. **Starting new features** - Check `feature-planning/` for existing drafts
2. **Making architectural decisions** - Review `decisions/` for precedents
3. **Understanding project history** - Read `session-summaries/` for context
4. **Resuming work** - Check today's `daily-notes/` for current status

### Writing Conventions for Java Projects

**1. Link to Code Files:**
```markdown
See `backend/src/main/java/com/northstar/funding/discovery/service/MetadataJudgingService.java:42`
```

**2. Link to Database Schema:**
```markdown
Migration `V14__create_metadata_judgments_table.sql` adds confidence scoring
```

**3. Tag with Feature Numbers:**
```markdown
#feature-003 #feature-004 #search-infrastructure
```

**4. Tag with Categories:**
```markdown
#architecture #performance #testing #bug #refactor
```

**5. Link to Related Notes:**
```markdown
See [[001-text-array-over-jsonb]] for rationale on TEXT[] vs JSONB
```

**6. Reference Java Classes:**
```markdown
The `SearchExecutionService` uses Virtual Threads for parallel execution
```

**7. Reference Test Classes:**
```markdown
Added integration test: `MondayNightlyDiscoveryTest.shouldLoadCorrectQueries()`
```

### Session Summary Template

When writing session summaries, use this structure:

```markdown
# Session Summary: [Feature Name or Work Description]

**Date**: YYYY-MM-DD
**Duration**: ~X hours
**Feature**: #feature-00X
**Branch**: feature-branch-name

## What Was Accomplished

- [ ] Task 1 description
- [x] Task 2 description (completed)
- [ ] Task 3 description

## Key Decisions Made

### Decision 1: Title
**Context**: Why this decision was needed
**Decision**: What was chosen
**Rationale**: Why this approach
**Alternatives**: What else was considered

## Code Changes

### New Files
- `path/to/NewService.java` - Description of purpose
- `path/to/migration/V15__*.sql` - Database changes

### Modified Files
- `path/to/ExistingService.java` - What changed and why

## Tests Added/Modified
- `SomeIntegrationTest.testSomething()` - What it validates
- Current status: X passing, Y failing

## Blockers & Issues

1. **Issue description** - What's blocking and what's needed to resolve
2. **Test failures** - Which tests are failing and why

## Next Steps

1. [ ] Immediate next task
2. [ ] Following task
3. [ ] Future consideration

## Related Documentation

- [[Related Note in Vault]]
- `specs/00X-feature/plan.md`
- `CLAUDE.md` updates needed
```

### Architecture Decision Record Template

When writing ADRs, use this structure:

```markdown
# ADR NNN: Decision Title

**Status**: Accepted | Proposed | Superseded
**Date**: YYYY-MM-DD
**Context Tags**: #architecture #database #performance

## Context

What is the problem we're trying to solve? What are the constraints and requirements?

## Decision

What did we decide to do? Be specific and concrete.

## Consequences

### Positive
- Benefit 1
- Benefit 2

### Negative
- Trade-off 1
- Trade-off 2

### Neutral
- Side effect 1

## Alternatives Considered

### Alternative 1: Name
**Description**: What it is
**Pros**: Why it's good
**Cons**: Why we didn't choose it

### Alternative 2: Name
**Description**: What it is
**Pros**: Why it's good
**Cons**: Why we didn't choose it

## Implementation Notes

Code locations, patterns to follow, examples:

```java
// Example implementation pattern
public class Example {
    // ...
}
```

## References

- [[Related ADR]]
- External documentation links
- Java/Spring documentation
- Feature specs that motivated this decision
```

### Best Practices for Vault Management

**DO:**
- Write session summaries after significant work sessions
- Document "why" decisions, not just "what" was done
- Use wiki links `[[like-this]]` to connect related notes
- Tag notes with feature numbers and categories
- Link to specific code files and line numbers
- Reference Java class names, test names, migration files
- Update vault regularly as work progresses

**DON'T:**
- Don't duplicate content that belongs in `CLAUDE.md` (project-wide patterns)
- Don't duplicate content that belongs in `/specs` (formal feature specs)
- Don't write executable code in vault (code examples are fine, but implementations go in `/backend`)
- Don't modify `.obsidian/` configuration directory
- Don't use vault for sensitive data (API keys, credentials)

### Integration with Project Workflow

**Daily Workflow:**
1. **Morning**: Check `daily-notes/YYYY-MM-DD.md` for current tasks
2. **During Development**: Add quick notes to `inbox/` as needed
3. **After Major Work**: Write session summary to `session-summaries/`
4. **When Deciding**: Document decisions in `decisions/` with ADR format
5. **Planning Features**: Draft in `feature-planning/`, move to `/specs` when ready

**Weekly Workflow:**
1. Review and process notes in `inbox/`
2. Update `decisions/` index if new ADRs added
3. Archive or expand notes in `feature-planning/`
4. Review `session-summaries/` for patterns or recurring issues

### Linking Between Vault and Project Files

**From Vault to Code:**
```markdown
Implemented in `backend/src/main/java/com/northstar/funding/discovery/service/MetadataJudgingService.java`
Database schema: `backend/src/main/resources/db/migration/V14__create_metadata_judgments_table.sql`
Tests: `backend/src/test/java/com/northstar/funding/integration/MetadataJudgingIntegrationTest.java`
```

**From Vault to Specs:**
```markdown
Formal spec: [Feature 004](../specs/004-ai-query-generation-metadata-judging/spec.md)
Implementation plan: [Feature 004 Tasks](../specs/004-ai-query-generation-metadata-judging/tasks.md)
```

**From Vault to Other Docs:**
```markdown
Architecture overview: [Crawler Hybrid Architecture](../docs/architecture-crawler-hybrid.md)
Domain model: [Domain Model](../docs/domain-model.md)
Project guide: [CLAUDE.md](../CLAUDE.md)
```

### Example Session Summary

See `northstar-notes/session-summaries/_template.md` for the full template with examples.

## Troubleshooting

### TestContainers Issues
1. Ensure Docker is running
2. Check existing test (e.g., `DiscoveryWorkflowIntegrationTest.java`) for correct pattern
3. Verify `@DynamicPropertySource` configuration
4. Check `application.yml` has postgres-test profile

### Circuit Breaker Issues
1. Check actuator health: `curl http://localhost:8080/api/actuator/health`
2. Verify API keys are set in environment
3. Check circuit breaker state in logs
4. Wait 30s for circuit to transition from OPEN to HALF_OPEN

### Search Engine Connection Issues
1. Verify Mac Studio infrastructure is accessible @ 192.168.1.10
2. Check Searxng is running: `curl http://192.168.1.10:8080`
3. Verify API keys are valid
4. Review circuit breaker configuration in `application.yml`
