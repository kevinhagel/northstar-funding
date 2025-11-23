# Implementation Plan: Automated Search Adapter Infrastructure

**Branch**: `014-create-automated-crawler` | **Date**: 2025-11-17 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/014-create-automated-crawler/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   ✅ Loaded: 33 functional requirements, 6 key entities
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   ✅ All clarifications resolved in spec
   ✅ Project Type: Java monolith (existing multi-module Maven project)
   ✅ Structure Decision: Existing module structure (northstar-search-adapters, northstar-crawler)
3. Fill the Constitution Check section
   ✅ Completed based on constitution v1.4.0
4. Evaluate Constitution Check section
   ✅ No violations - aligns with monolith architecture, approved technologies
   ✅ Update Progress Tracking: Initial Constitution Check PASS
5. Execute Phase 0 → research.md
   ✅ Technical decisions documented
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md
   ✅ Artifacts generated
7. Re-evaluate Constitution Check section
   ✅ No new violations - design compliant
   ✅ Update Progress Tracking: Post-Design Constitution Check PASS
8. Plan Phase 2 → Describe task generation approach
   ✅ Task generation strategy documented
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 9. Phase 2 execution by /tasks command.

## Summary

**Primary Requirement**: Implement automated nightly search workflow to discover funding source candidates using 4 search engines (Searxng, Tavily, Browserbase, Perplexity), evaluate candidates using metadata-only confidence scoring (≥0.6 threshold), deduplicate by domain, register high-confidence candidates as PENDING_CRAWL status, and track query/adapter effectiveness metrics.

**Technical Approach**:
- Implement 4 SearchAdapter implementations (Searxng, Tavily, Browserbase, Perplexity)
- Create SearchWorkflowService orchestrator using Java 25 Virtual Threads for parallel execution
- Leverage existing ConfidenceScorer, DomainService, CandidateCreationService from northstar-crawler
- Leverage existing QueryGenerationService from northstar-query-generation
- Use existing DiscoverySession tracking from northstar-persistence
- Distribute 30 FundingSearchCategory enum values across 7-day rotation
- Each night: use all 4 adapters, distribute categories across them
- Track zero-result outcomes (not errors) for adapter/category effectiveness analysis

## Technical Context

**Language/Version**: Java 25 (Oracle JDK via SDKMAN) - source and target level 25
**Primary Dependencies**:
- Spring Boot 3.5.7 (existing)
- Spring Data JDBC (existing for persistence)
- HTTP Client libraries for search engine APIs (OkHttp, Apache HttpClient, or Spring WebClient)
- Existing modules: northstar-domain, northstar-persistence, northstar-query-generation, northstar-crawler

**Storage**: PostgreSQL 16 @ 192.168.1.10:5432 (existing database with schema in place)
**Testing**:
- JUnit 5/Jupiter (existing)
- Mockito for unit tests (existing pattern)
- TestContainers for integration tests (existing setup)
- WireMock or similar for mocking search engine HTTP responses

**Target Platform**: Spring Boot application on MacBook M2 (development), Docker on Mac Studio (production)
**Project Type**: Existing Java Maven multi-module monolith - adding to northstar-search-adapters module
**Performance Goals**:
- Complete nightly search session in < 10 minutes for 30 categories across 4 engines
- Process 20-25 search results per query with Virtual Threads
- API calls to search engines < 5 seconds each

**Constraints**:
- No web crawling in Phase 1 (metadata judging only)
- Confidence threshold ≥ 0.6 for candidate creation
- Domain deduplication required (in-memory + database)
- All 4 adapters must be used each night
- Zero-result outcomes must be tracked (not treated as errors)

**Scale/Scope**:
- 30 FundingSearchCategory values
- 7-day rotation schedule
- 4 search engines × ~4-5 categories per night = 16-20 queries per night
- Estimate: 20-25 results per query = 320-500 total results per night
- After filtering: ~50-100 high-confidence candidates created per night

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle Compliance

**I. XML Tag Accuracy** - ✅ COMPLIANT
- No XML file changes in this feature (Maven pom.xml already configured)
- If pom.xml modified: use full tag names (`<dependency>`, not abbreviations)

**II. Domain-Driven Design** - ✅ COMPLIANT
- Feature uses existing domain language: "Funding Source Candidates", "Confidence Scoring", "Domain Deduplication"
- Entities align with DDD: SearchResult, Domain, FundingSourceCandidate, DiscoverySession
- No violations of ubiquitous language

**III. Human-AI Collaboration** - ✅ COMPLIANT
- AI discovers and scores candidates (this feature)
- Humans review via Admin Dashboard (Feature 013, already implemented)
- Clear separation: automated discovery → human validation

**IV. Technology Stack** - ✅ COMPLIANT
- Java 25 with Spring Boot 3.5.7 ✅
- Uses existing PostgreSQL 16 database ✅
- TestContainers for testing ✅
- No forbidden technologies (no crawl4j, langgraph4j, langchain4j) ✅
- HTTP client for search APIs (OkHttp/WebClient) - approved pattern ✅

**V. Three-Workflow Architecture** - ✅ COMPLIANT
- This feature implements "Funding Discovery" workflow (1 of 3)
- Clear separation from Database Services and Database Discovery workflows
- No cross-workflow contamination

**VI. Complexity Management** - ✅ COMPLIANT
- Single responsibility: search adapter infrastructure only
- No god classes: SearchAdapter interface with 4 focused implementations
- Fits within monolith architecture (not adding new services/microservices)
- Breaking into discrete tasks in Phase 2

**VII. Contact Intelligence Priority** - ✅ NOT APPLICABLE
- This feature discovers funding sources, does not extract contact intelligence
- Contact extraction is Phase 2 (deep crawling), not Phase 1 (metadata judging)

**VIII. Deployment Responsibility** - ✅ COMPLIANT
- Kevin manages rsync to Mac Studio
- AI will not execute deployment scripts
- Development on MacBook M2, deployment on Mac Studio

**IX. Script Creation Permission** - ✅ COMPLIANT
- No new scripts created without permission
- Using existing Maven build process

**X. Technology Constraints** - ✅ COMPLIANT
- Using approved technologies: Spring Kafka (if needed), Lombok, Jsoup (for HTML in future), JUnit, Guava, Vavr
- NOT using forbidden technologies: crawl4j, langgraph4j, langchain4j ✅
- Monolith architecture maintained ✅
- No microservices ✅

**XI. Two Web Layers** - ✅ COMPLIANT
- Dashboard Web Layer already exists (Feature 013)
- This feature populates data for dashboard review
- No new web layers added

**XII. Data Precision Standards** - ✅ CRITICAL COMPLIANCE
- ALL confidence scores MUST use `BigDecimal` with scale 2 ✅
- NEVER use `Double` or `double` for confidence scores ✅
- Database: `DECIMAL(3,2)` already in schema ✅
- Comparisons: Use `.compareTo()` not `==` ✅
- Arithmetic: Use `BigDecimal.add()`, `.multiply()` with `RoundingMode.HALF_UP` ✅

### Gate Status
- ✅ Initial Constitution Check: **PASS** (no violations)
- ⏳ Post-Design Constitution Check: Pending Phase 1 completion

## Project Structure

### Documentation (this feature)
```
specs/014-create-automated-crawler/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (search adapter APIs, HTTP clients)
├── data-model.md        # Phase 1 output (SearchResult, SearchAdapter interface)
├── quickstart.md        # Phase 1 output (how to run first search)
├── contracts/           # Phase 1 output (SearchAdapter contract tests)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (existing multi-module structure)
```
northstar-funding/
├── northstar-domain/              # Existing - contains FundingSearchCategory enum
├── northstar-persistence/         # Existing - contains repositories, services
├── northstar-query-generation/    # Existing - generates search queries
├── northstar-crawler/             # Existing - contains confidence scoring, processing
└── northstar-search-adapters/     # THIS FEATURE - search engine integrations
    ├── src/main/java/com/northstar/funding/searchadapters/
    │   ├── SearchAdapter.java                  # Interface (may exist)
    │   ├── SearchResult.java                   # Model (may exist)
    │   ├── brave/
    │   │   └── BraveAdapter.java               # NEW
    │   ├── searxng/
    │   │   └── SearXNGAdapter.java             # Existing - verify/fix
    │   ├── serper/
    │   │   └── SerperAdapter.java              # NEW
    │   ├── tavily/
    │   │   └── TavilyAdapter.java              # NEW
    │   ├── workflow/
    │   │   ├── SearchWorkflowService.java      # NEW - orchestrator
    │   │   ├── AdapterDistributionStrategy.java # NEW - category distribution
    │   │   └── SearchSessionOrchestrator.java  # NEW - nightly execution
    │   └── config/
    │       ├── SearchAdapterConfiguration.java  # NEW - Spring beans
    │       └── SearchEngineProperties.java      # NEW - externalized config
    └── src/test/java/com/northstar/funding/searchadapters/
        ├── contract/
        │   ├── SearchAdapterContractTest.java   # NEW - interface contract
        │   ├── BraveAdapterContractTest.java    # NEW
        │   ├── SearXNGAdapterContractTest.java  # NEW
        │   ├── SerperAdapterContractTest.java   # NEW
        │   └── TavilyAdapterContractTest.java   # NEW
        ├── integration/
        │   ├── SearchWorkflowIntegrationTest.java # NEW - end-to-end test
        │   └── AdapterEffectivenessTrackingTest.java # NEW
        └── unit/
            ├── BraveAdapterTest.java             # NEW - mocked HTTP
            ├── SearXNGAdapterTest.java           # NEW
            ├── SerperAdapterTest.java            # NEW
            ├── TavilyAdapterTest.java            # NEW
            └── SearchWorkflowServiceTest.java    # NEW
```

**Structure Decision**: Using existing multi-module Maven structure. Adding search adapter implementations to `northstar-search-adapters` module and workflow orchestration. No new modules needed.

## Phase 0: Outline & Research

### Research Tasks

**R1. Search Engine API Documentation**
- **Decision**: Review API documentation for Brave, Serper, SearXNG, Tavily
- **Findings**:
  - **Brave Search API**: REST API, requires API key, JSON responses, rate limits apply
  - **Serper.dev**: Google Search API wrapper, requires API key, JSON responses
  - **SearXNG**: Self-hosted @ http://192.168.1.10:8080, no API key needed, JSON format parameter
  - **Tavily**: AI-optimized search, requires API key, designed for LLM applications
- **Rationale**: Need to understand request/response formats, authentication, rate limits for each engine
- **Alternatives considered**: Using MCP search tools - rejected because we need direct control over adapters for effectiveness tracking

**R2. HTTP Client Selection**
- **Decision**: Use Spring WebClient (reactive, non-blocking) for all search adapter HTTP calls
- **Rationale**:
  - Already part of Spring Boot ecosystem
  - Non-blocking I/O pairs well with Virtual Threads
  - Built-in JSON serialization/deserialization
  - Excellent error handling and retry capabilities
- **Alternatives considered**:
  - OkHttp - good but requires additional dependency
  - Apache HttpClient - blocking I/O, less efficient for concurrent requests

**R3. Virtual Threads for Parallel Execution**
- **Decision**: Use Java 25 Virtual Threads with ExecutorService for parallel query execution
- **Pattern**:
  ```java
  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  List<CompletableFuture<List<SearchResult>>> futures = adapters.stream()
      .map(adapter -> CompletableFuture.supplyAsync(() -> adapter.search(query, maxResults), executor))
      .toList();
  ```
- **Rationale**: Virtual Threads are lightweight, perfect for I/O-bound search API calls
- **Alternatives considered**: Traditional thread pools - rejected because Virtual Threads are more efficient for high-concurrency I/O

**R4. Adapter Effectiveness Tracking**
- **Decision**: Track zero-result outcomes per query+adapter combination in `search_session_statistics` table (already exists from V11 migration)
- **Schema**: Table already has columns for tracking per-engine statistics
- **Rationale**: Zero results is NOT an error - it's valuable data for optimizing adapter-category distribution
- **Alternatives considered**: Not tracking zero results - rejected because we lose optimization data

**R5. 7-Day Category Distribution**
- **Decision**: Create static distribution of 30 categories across 7 days
- **Distribution Strategy**:
  - Monday: 4 categories (INDIVIDUAL_SCHOLARSHIPS, STUDENT_FINANCIAL_AID, TEACHER_SCHOLARSHIPS, ACADEMIC_FELLOWSHIPS)
  - Tuesday: 5 categories (PROGRAM_GRANTS, CURRICULUM_DEVELOPMENT, AFTER_SCHOOL_PROGRAMS, SUMMER_PROGRAMS, EXTRACURRICULAR_ACTIVITIES)
  - Wednesday: 3 categories (INFRASTRUCTURE_FUNDING, TECHNOLOGY_EQUIPMENT, LIBRARY_RESOURCES)
  - Thursday: 3 categories (TEACHER_DEVELOPMENT, PROFESSIONAL_TRAINING, ADMINISTRATIVE_CAPACITY)
  - Friday: 4 categories (STEM_EDUCATION, ARTS_EDUCATION, SPECIAL_NEEDS_EDUCATION, LANGUAGE_PROGRAMS)
  - Saturday: 3 categories (COMMUNITY_PARTNERSHIPS, PARENT_ENGAGEMENT, NGO_EDUCATION_PROJECTS)
  - Sunday: 8 categories (EDUCATION_RESEARCH, PILOT_PROGRAMS, INNOVATION_GRANTS, EARLY_CHILDHOOD_EDUCATION, ADULT_EDUCATION, VOCATIONAL_TRAINING, EDUCATIONAL_TECHNOLOGY, ARTS_CULTURE)
- **Rationale**: Balanced distribution (3-8 categories per day), grouped by thematic similarity
- **Alternatives considered**: Random daily selection - rejected because we want consistent, predictable schedules

**R6. Adapter Distribution per Night**
- **Decision**: Round-robin distribution of categories across 4 adapters each night
- **Pattern**: For N categories, adapter index = (category_index % 4)
- **Example** (Monday, 4 categories):
  - Adapter 0 (Brave): INDIVIDUAL_SCHOLARSHIPS
  - Adapter 1 (Serper): STUDENT_FINANCIAL_AID
  - Adapter 2 (SearXNG): TEACHER_SCHOLARSHIPS
  - Adapter 3 (Tavily): ACADEMIC_FELLOWSHIPS
- **Rationale**: Simple, ensures all adapters used each night, provides baseline data for optimization
- **Alternatives considered**:
  - Category-type based distribution - rejected as premature optimization (no effectiveness data yet)
  - All adapters for all categories - rejected as too expensive (4× API calls)

**Output**: research.md with all technical decisions documented

## Phase 1: Design & Contracts

### 1. Data Model (`data-model.md`)

**Entities** (mix of existing and new):

**SearchResult** (existing in northstar-crawler - verify alignment):
```java
public class SearchResult {
    private String url;                    // Full URL from search engine
    private String title;                  // Page title (metadata)
    private String description;            // Page description (metadata)
    private SearchEngineType source;       // Which engine returned this
    private LocalDateTime discoveredAt;    // When found
}
```

**SearchAdapter Interface** (may exist - define contract):
```java
public interface SearchAdapter {
    /**
     * Execute search query and return results.
     * @param query The search query string
     * @param maxResults Maximum number of results to return
     * @return List of search results (may be empty if zero results)
     * @throws SearchAdapterException if API call fails (not zero results)
     */
    List<SearchResult> search(String query, int maxResults);

    /**
     * Get the search engine type this adapter represents.
     */
    SearchEngineType getEngineType();

    /**
     * Health check - can this adapter be used?
     */
    boolean isAvailable();
}
```

**SearchWorkflowRequest** (new):
```java
public class SearchWorkflowRequest {
    private LocalDate executionDate;        // Date of search execution
    private DayOfWeek dayOfWeek;            // Monday-Sunday
    private List<FundingSearchCategory> categoriesToSearch;  // Categories for this day
    private List<SearchEngineType> adaptersToUse;  // All 4 adapters
}
```

**SearchWorkflowResult** (new):
```java
public class SearchWorkflowResult {
    private UUID sessionId;                 // DiscoverySession ID
    private int queriesGenerated;
    private int totalResultsFound;
    private int candidatesCreated;
    private int duplicatesSkipped;
    private int blacklistedSkipped;
    private int zeroResultCount;            // Queries that returned 0 results
    private Map<SearchEngineType, Integer> resultsByEngine;
    private Duration executionDuration;
}
```

**AdapterEffectivenessMetric** (new - persistence model):
```java
public class AdapterEffectivenessMetric {
    private UUID metricId;
    private SearchEngineType adapter;
    private FundingSearchCategory category;
    private LocalDate measuredDate;
    private int queriesExecuted;
    private int resultsReturned;
    private int zeroResultQueries;
    private int candidatesCreated;
    private BigDecimal averageConfidence;   // CRITICAL: BigDecimal, scale 2
}
```

### 2. API Contracts (`contracts/`)

**SearchAdapter Contract** (interface all adapters must satisfy):
```yaml
# contracts/search-adapter-contract.yml
contract: SearchAdapter
interface: com.northstar.funding.searchadapters.SearchAdapter

scenarios:
  - name: successful_search_with_results
    given: valid query and API credentials
    when: search("education grants Bulgaria", 10)
    then:
      - returns List<SearchResult>
      - size > 0
      - each result has non-null url, title, source
      - source matches adapter's getEngineType()

  - name: successful_search_zero_results
    given: valid query, no matches
    when: search("nonexistent funding category xyz123", 10)
    then:
      - returns empty List<SearchResult>
      - does NOT throw exception
      - zero results is NOT an error

  - name: api_failure
    given: invalid API key or network error
    when: search("education", 10)
    then:
      - throws SearchAdapterException
      - exception contains error message
      - other adapters continue processing

  - name: health_check
    when: isAvailable()
    then:
      - returns boolean
      - true if API credentials valid and reachable
```

**SearchWorkflowService Contract**:
```yaml
# contracts/search-workflow-contract.yml
contract: SearchWorkflowService
service: com.northstar.funding.searchadapters.workflow.SearchWorkflowService

scenarios:
  - name: execute_nightly_search
    given:
      - Monday execution date
      - 4 categories for Monday
      - 4 search adapters configured
    when: executeNightlySearch(request)
    then:
      - creates DiscoverySession record
      - generates queries for each category (via QueryGenerationService)
      - executes searches across all 4 adapters
      - processes results (confidence scoring, deduplication)
      - creates high-confidence candidates (>= 0.6)
      - tracks zero-result outcomes
      - returns SearchWorkflowResult with statistics

  - name: adapter_failure_resilience
    given:
      - 3 adapters working, 1 failing
    when: executeNightlySearch(request)
    then:
      - continues with working adapters
      - logs failed adapter
      - completes session successfully
      - result shows 3 of 4 adapters succeeded
```

### 3. Contract Tests (`tests/contract/`)

All contract tests will be written FIRST (TDD) and MUST FAIL initially.

**Files to create**:
- `SearchAdapterContractTest.java` - Abstract base class all adapter tests extend
- `BraveAdapterContractTest.java` - Brave-specific contract tests
- `SearXNGAdapterContractTest.java` - SearXNG-specific contract tests
- `SerperAdapterContractTest.java` - Serper-specific contract tests
- `TavilyAdapterContractTest.java` - Tavily-specific contract tests
- `SearchWorkflowServiceContractTest.java` - Workflow orchestration contract

### 4. Quickstart (`quickstart.md`)

**Quickstart Test Scenario**: Execute first manual search for Monday's categories

**Prerequisites**:
1. PostgreSQL running @ 192.168.1.10:5432
2. Ollama running @ 192.168.1.10:11434 (for query generation)
3. API keys configured in `application.yml`:
   - `search.adapters.brave.api-key`
   - `search.adapters.serper.api-key`
   - `search.adapters.tavily.api-key`
4. SearXNG available @ http://192.168.1.10:8080

**Steps**:
```bash
# 1. Build the project
mvn clean compile

# 2. Run unit tests (mocked adapters)
mvn test -Dtest='*AdapterTest'

# 3. Run contract tests (fail initially)
mvn test -Dtest='*ContractTest'

# 4. Run integration test (full workflow with TestContainers)
mvn test -Dtest=SearchWorkflowIntegrationTest

# 5. Execute manual nightly search (Monday)
mvn spring-boot:run -Dspring-boot.run.arguments="--search.manual.trigger=true,--search.manual.date=2025-11-18"

# 6. Verify in database
psql -h 192.168.1.10 -U northstar_user -d northstar_funding -c "
  SELECT COUNT(*) FROM funding_source_candidate WHERE discovered_at > NOW() - INTERVAL '1 hour';
"

# 7. View in Admin Dashboard
# Open http://localhost:5173 (Feature 013 UI)
# Should see new PENDING_CRAWL candidates
```

**Success Criteria**:
- ✅ All 4 adapters successfully queried
- ✅ Queries generated for Monday's 4 categories
- ✅ At least 10 search results returned (across all adapters)
- ✅ At least 3 high-confidence candidates created (>= 0.6)
- ✅ Zero-result outcomes tracked (not errors)
- ✅ DiscoverySession record created with statistics
- ✅ Candidates visible in Admin Dashboard

### 5. Agent File Update

Running update script to add Feature 014 context to CLAUDE.md:

```bash
.specify/scripts/bash/update-agent-context.sh claude
```

This will:
- Add SearchAdapter implementations to CLAUDE.md
- Document workflow orchestration pattern
- Update "Recent Changes" with Feature 014
- Keep file under 150 lines

**Output**: Updated `/Users/kevin/github/northstar-funding/CLAUDE.md`

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
1. Load `.specify/templates/tasks-template.md` as base
2. Generate tasks from Phase 1 design docs:
   - Each adapter contract → adapter implementation + tests
   - SearchWorkflowService → orchestrator implementation + tests
   - Integration tests → end-to-end workflow validation
3. Order by TDD: tests before implementation
4. Mark [P] for parallel execution (independent adapters)

**Task Categories**:
- **Contract Tests** (5 tasks): SearchAdapter base + 4 adapter-specific contracts [P]
- **Adapter Implementations** (4 tasks): Brave, SearXNG, Serper, Tavily [P]
- **Adapter Unit Tests** (4 tasks): Mocked HTTP tests for each adapter [P]
- **Workflow Orchestration** (3 tasks): SearchWorkflowService, distribution strategy, configuration
- **Integration Tests** (2 tasks): Full workflow test, effectiveness tracking test
- **Configuration** (2 tasks): Externalized properties, Spring bean wiring
- **Documentation** (2 tasks): README update, API documentation

**Ordering Strategy**:
1. Contract tests first (define expectations)
2. Adapter implementations in parallel (independent)
3. Unit tests alongside implementations
4. Workflow orchestration (depends on adapters)
5. Integration tests (depends on workflow)
6. Configuration and documentation last

**Estimated Output**: 22-25 numbered, dependency-ordered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following constitutional principles)
**Phase 5**: Validation (run tests, execute quickstart.md, verify nightly search works)

## Complexity Tracking
*ONLY filled if Constitution Check has violations requiring justification*

**No violations found - this section remains empty**

## Progress Tracking
*Updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning approach described (/plan command)
- [ ] Phase 3: Tasks generated (/tasks command - next step)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS (no violations)
- [x] Post-Design Constitution Check: PASS (design compliant)
- [x] All NEEDS CLARIFICATION resolved (spec complete)
- [x] Complexity deviations documented (none)

---
*Based on Constitution v1.4.0 - See `/.specify/memory/constitution.md`*
