
# Implementation Plan: Search Execution Infrastructure

**Branch**: `003-search-execution-infrastructure` | **Date**: 2025-10-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/003-search-execution-infrastructure/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → If not found: ERROR "No feature spec at {path}"
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → Detect Project Type from context (web=frontend+backend, mobile=app+api)
   → Set Structure Decision based on project type
3. Fill the Constitution Check section based on the content of the constitution document.
4. Evaluate Constitution Check section below
   → If violations exist: Document in Complexity Tracking
   → If no justification possible: ERROR "Simplify approach first"
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → If NEEDS CLARIFICATION remain: ERROR "Resolve unknowns"
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, agent-specific template file (e.g., `CLAUDE.md` for Claude Code, `.github/copilot-instructions.md` for GitHub Copilot, `GEMINI.md` for Gemini CLI, `QWEN.md` for Qwen Code or `AGENTS.md` for opencode).
7. Re-evaluate Constitution Check section
   → If new violations: Refactor design, return to Phase 1
   → Update Progress Tracking: Post-Design Constitution Check
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 7. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary
Build production-ready search execution infrastructure with 4 search engine adapters (Searxng, Browserbase, Tavily, Perplexity), hardcoded query library organized by day-of-week, nightly scheduled execution at 2 AM, domain-level deduplication to prevent reprocessing, integration with existing MetadataJudgingService for candidate scoring, circuit breakers and retry logic for resilience, performance monitoring and analytics to identify productive engines/queries. MVP approach: Start with 5-10 hardcoded queries per night to validate infrastructure before adding AI-powered query generation (Feature 004).

## Technical Context
**Language/Version**: Java 25 (source and target level 25) via SDKMAN
**Primary Dependencies**: Spring Boot 3.5.6, Spring Data JDBC, Vavr 0.10.7, Lombok 1.18.42, Resilience4j (circuit breakers), Jsoup (HTML parsing), Java 25 Virtual Threads, Spring Scheduling, RestClient (Spring 6.1+)
**Storage**: PostgreSQL 16 for search queries, discovery sessions, session statistics, engine performance metrics
**Testing**: JUnit/Jupiter + TestContainers for integration tests, Mockito for unit tests, WireMock for search engine API mocking
**Target Platform**: Docker on Mac Studio (192.168.1.10), development on MacBook M2 (macOS)
**Project Type**: web (backend Spring Boot monolith extending Feature 002 infrastructure)
**Performance Goals**: Complete 10 queries across 4 engines (1000 raw results) in under 30 minutes, <15 seconds per search engine query, parallel execution using Virtual Threads
**Constraints**: NO langchain4j (forbidden), Searxng at 192.168.1.10:8080, API keys for Browserbase/Tavily/Perplexity in environment variables, circuit breakers must prevent cascading failures, 3 retry attempts with exponential backoff
**Scale/Scope**: 4 search engine adapters, 5-10 queries per night (expandable), 25 results per query per engine, 500-1000 raw results per night, domain deduplication across all results

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Technology Stack (Principle IV - NON-NEGOTIABLE)
- **Java 25**: Source and target level 25 via SDKMAN
- **Spring Boot 3.5.6**: Modern patterns with Virtual Threads for parallel search execution
- **PostgreSQL 16**: Permanent storage for search queries, sessions, analytics
- **Testing**: JUnit/Jupiter + TestContainers + WireMock for API mocking
- **NO External LLM dependencies**: AI query generation deferred to Feature 004 (LM Studio)
- **RestClient**: Spring 6.1+ modern HTTP client (replacement for deprecated RestTemplate)

### Technology Constraints (Principle X - CRITICAL)
- **NO langchain4j**: Forbidden per constitution - using plain Spring RestClient
- **NO crawl4j**: Not applicable (no web crawling in this feature)
- **NO microservices**: Monolith architecture - search execution within existing Spring Boot app
- **Approved Technologies**:
  - Spring Boot 3.5.6
  - Lombok 1.18.42 (reduce boilerplate, Java 25 support)
  - Jsoup (HTML parsing for search result extraction if needed)
  - Vavr 0.10.7 (Try/Either for error handling)
  - Resilience4j (circuit breakers, retry logic)
  - Spring Scheduling (@Scheduled for nightly execution)

### Complexity Management (Principle VI - ESSENTIAL)
- **Single Responsibility Services**:
  - SearchQueryLibrary: Manage hardcoded queries
  - SearchEngineAdapter interface: Unified contract for all engines
  - SearchExecutionService: Orchestrate query execution across engines
  - SearchSessionService: Track statistics and analytics
- **Service Count**: Adding 4 new services + 4 adapters = 8 components (within complexity limits for focused feature)
- **Simple Orchestration**: Sequential execution per query, parallel across engines using Virtual Threads

### Data Precision Standards (CRITICAL)
- **BigDecimal for Confidence Scores**: All confidence scores from MetadataJudgingService use BigDecimal with scale 2
- **NO Double/double**: Existing MetadataJudgingService already compliant
- **Integration Requirement**: Search execution passes SearchResult to MetadataJudgingService unchanged

### Human-AI Collaboration (Principle III - MANDATORY)
- **AI Role**: Automated nightly search execution and metadata judging (existing from Feature 002)
- **Human Role**: Review analytics to identify productive engines/queries, refine query library, blacklist domains
- **Learning Loop**: Kevin reviews discovery session statistics, adjusts query library based on hit rates

### Domain-Driven Design (Principle II - UBIQUITOUS LANGUAGE)
- **Entities**: SearchQuery, SearchEngine, SearchResult (existing DTO), DiscoverySession (existing), SearchSessionStatistics
- **Correct Terminology**: Funding sources (not grants), discovery (not crawling), metadata judging (Phase 1), deep crawling (Phase 2 - future)

### Infrastructure Integration (Constitution Section)
- **Mac Studio PostgreSQL**: 192.168.1.10:5432 (existing database)
- **Searxng**: Self-hosted at 192.168.1.10:8080 (Mac Studio Docker container)
- **API Keys**: Browserbase, Tavily, Perplexity stored in environment variables (.env file)
- **Development**: MacBook M2 local development, TestContainers for tests
- **Deployment**: Kevin manages rsync to Mac Studio (Principle VIII)

**GATE RESULT**: PASS - No constitutional violations detected

## Project Structure

### Documentation (this feature)
```
specs/[###-feature]/
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)
```
# Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure]
```

**Structure Decision**: Option 2 (Web application) - Extending existing backend Spring Boot monolith with new `search` bounded context

### Source Code Structure (Detailed)
```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/northstar/funding/discovery/
│   │   │   ├── search/                    # NEW: Search execution bounded context
│   │   │   │   ├── domain/
│   │   │   │   │   ├── SearchQuery.java
│   │   │   │   │   ├── SearchEngine.java
│   │   │   │   │   ├── SearchEngineType.java (enum)
│   │   │   │   │   ├── SearchSessionStatistics.java
│   │   │   │   │   └── QueryTag.java
│   │   │   │   ├── application/
│   │   │   │   │   ├── SearchQueryLibrary.java
│   │   │   │   │   ├── SearchExecutionService.java
│   │   │   │   │   ├── SearchSessionService.java
│   │   │   │   │   └── NightlyDiscoveryScheduler.java
│   │   │   │   ├── infrastructure/
│   │   │   │   │   ├── adapters/
│   │   │   │   │   │   ├── SearchEngineAdapter.java (interface)
│   │   │   │   │   │   ├── SearxngAdapter.java
│   │   │   │   │   │   ├── BrowserbaseAdapter.java
│   │   │   │   │   │   ├── TavilyAdapter.java
│   │   │   │   │   │   └── PerplexityAdapter.java
│   │   │   │   │   ├── client/
│   │   │   │   │   │   ├── SearchEngineClient.java (RestClient wrapper)
│   │   │   │   │   │   └── CircuitBreakerConfig.java (Resilience4j)
│   │   │   │   │   ├── SearchQueryRepository.java
│   │   │   │   │   └── SearchSessionStatisticsRepository.java
│   │   │   │   └── web/
│   │   │   │       └── SearchAnalyticsController.java (future - out of scope for MVP)
│   │   │   ├── service/                   # EXISTING: From Feature 002
│   │   │   │   ├── MetadataJudgingService.java (existing)
│   │   │   │   ├── DomainRegistryService.java (existing)
│   │   │   │   └── CandidateProcessingOrchestrator.java (existing)
│   │   │   └── domain/                    # EXISTING: From Feature 002
│   │   │       ├── Domain.java (existing)
│   │   │       ├── FundingSourceCandidate.java (existing)
│   │   │       └── DiscoverySession.java (existing - extend for search sessions)
│   │   └── resources/
│   │       ├── application.yml            # Search engine config, scheduler config
│   │       └── db/migration/
│   │           ├── V10__create_search_queries_table.sql
│   │           ├── V11__create_search_session_statistics_table.sql
│   │           └── V12__extend_discovery_session_for_search.sql
└── tests/
    ├── integration/
    │   ├── SearchEngineAdapterTest.java   # WireMock-based API tests
    │   ├── SearchExecutionServiceTest.java
    │   ├── NightlyDiscoveryTest.java      # End-to-end nightly run test
    │   └── CircuitBreakerTest.java        # Resilience4j fault tolerance tests
    └── unit/
        ├── SearchQueryLibraryTest.java
        ├── SearchSessionServiceTest.java
        └── SearxngAdapterTest.java        # Adapter-specific unit tests
```

## Phase 0: Outline & Research
1. **Extract unknowns from Technical Context** above:
   - For each NEEDS CLARIFICATION → research task
   - For each dependency → best practices task
   - For each integration → patterns task

2. **Generate and dispatch research agents**:
   ```
   For each unknown in Technical Context:
     Task: "Research {unknown} for {feature context}"
   For each technology choice:
     Task: "Find best practices for {tech} in {domain}"
   ```

3. **Consolidate findings** in `research.md` using format:
   - Decision: [what was chosen]
   - Rationale: [why chosen]
   - Alternatives considered: [what else evaluated]

**Output**: research.md with all NEEDS CLARIFICATION resolved

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

1. **Extract entities from feature spec** → `data-model.md`:
   - Entity name, fields, relationships
   - Validation rules from requirements
   - State transitions if applicable

2. **Generate API contracts** from functional requirements:
   - For each user action → endpoint
   - Use standard REST/GraphQL patterns
   - Output OpenAPI/GraphQL schema to `/contracts/`

3. **Generate contract tests** from contracts:
   - One test file per endpoint
   - Assert request/response schemas
   - Tests must fail (no implementation yet)

4. **Extract test scenarios** from user stories:
   - Each story → integration test scenario
   - Quickstart test = story validation steps

5. **Update agent file incrementally** (O(1) operation):
   - Run `.specify/scripts/bash/update-agent-context.sh claude`
     **IMPORTANT**: Execute it exactly as specified above. Do not add or remove any arguments.
   - If exists: Add only NEW tech from current plan
   - Preserve manual additions between markers
   - Update recent changes (keep last 3)
   - Keep under 150 lines for token efficiency
   - Output to repository root

**Output**: data-model.md, /contracts/*, failing tests, quickstart.md, agent-specific file

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each contract → contract test task [P]
- Each entity → model creation task [P] 
- Each user story → integration test task
- Implementation tasks to make tests pass

**Ordering Strategy**:
- TDD order: Tests before implementation 
- Dependency order: Models before services before UI
- Mark [P] for parallel execution (independent files)

**Estimated Output**: 25-30 numbered, ordered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

**No constitutional violations detected.** This feature fully complies with all constitutional principles:
- Uses approved technologies only (Spring Boot 3.5.6, Vavr 0.10.7, Resilience4j, etc.)
- Avoids forbidden technologies (no langchain4j, no microservices)
- Maintains monolith architecture within existing Spring Boot application
- Follows complexity management (single responsibility services, focused scope)
- Supports human-AI collaboration (automated discovery + human analytics review)


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command) - research.md generated
- [x] Phase 1: Design complete (/plan command) - data-model.md, quickstart.md, contracts/ generated
- [x] Phase 2: Task planning complete (/plan command - approach described above)
- [ ] Phase 3: Tasks generated (/tasks command) - Ready to execute /tasks
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS - No violations detected
- [x] Post-Design Constitution Check: PASS - Design aligns with all constitutional principles
- [x] All NEEDS CLARIFICATION resolved - Technical Context fully specified
- [x] Complexity deviations documented - None (no violations)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
