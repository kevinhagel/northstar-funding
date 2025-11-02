
# Implementation Plan: Search Provider Adapters

**Branch**: `003-design-and-implement` | **Date**: 2025-11-01 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-design-and-implement/spec.md`

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

This feature implements four search provider adapters (BraveSearch, SearXNG, Serper, Tavily) to enable automated funding discovery across multiple search engines. The adapters execute searches concurrently using Java 25 Virtual Threads, support both keyword queries (traditional search) and AI-optimized queries (Tavily), perform domain-level deduplication to prevent reprocessing, and include critical anti-spam filtering to eliminate SEO spam and scammer sites before expensive LLM processing. The implementation uses `java.net.http.HttpClient` for HTTP operations (NO WebFlux/Reactive), Vavr Try monad for functional error handling, and Apache Commons Text for fuzzy string matching in spam detection. This Phase 1 metadata judging filters 40-60% of non-funding results before Phase 2 deep crawling.

## Technical Context
**Language/Version**: Java 25 (Oracle JDK) via SDKMAN - source and target level 25
**Primary Dependencies**: Spring Boot 3.5.6, Apache Commons Text, Vavr 0.10.6, java.net.http.HttpClient (standard library)
**Storage**: PostgreSQL 16 for SearchResult and Domain entities (existing northstar-domain module)
**Testing**: JUnit 5/Jupiter + TestContainers for integration tests
**Target Platform**: Mac Studio (192.168.1.10) Docker containers, development on MacBook M2
**Project Type**: Monolith (single Spring Boot application within northstar-crawler module, NOT microservices)
**Performance Goals**: All 4 providers complete searches within 5 seconds (90th percentile), anti-spam filtering reduces LLM processing by 40-60%
**Constraints**: NO WebFlux/Reactive programming, MUST use Virtual Threads for parallel I/O, MUST use Apache Commons Text for fuzzy matching, NO crawl4j/langgraph4j/langchain4j
**Scale/Scope**: 4 search provider adapters, 48 functional requirements, 20-50 results per search, 10 acceptance scenarios

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: XML Tag Accuracy (CRITICAL)
✅ **PASS** - No Maven POM modifications required for this feature (uses existing northstar-domain entities)

### Principle II: Domain-Driven Design (UBIQUITOUS LANGUAGE)
✅ **PASS** - Uses existing ubiquitous language:
- "Funding Sources" terminology (not "grants")
- SearchResult entity for search engine results
- Domain entity for deduplication
- DiscoverySession for tracking search sessions

### Principle III: Human-AI Collaboration (MANDATORY)
✅ **PASS** - This feature is Phase 1 automated metadata judging only:
- AI filters spam and scores candidates based on metadata
- Human validation happens in Phase 2 (deep crawling) and Phase 3 (contact extraction)
- Anti-spam filtering prevents wasted human time on scammer sites

### Principle IV: Technology Stack (NON-NEGOTIABLE)
✅ **PASS** - Fully compliant:
- Java 25 with source and target level 25
- Spring Boot 3.5.6
- PostgreSQL 16 for data persistence
- JUnit/Jupiter + TestContainers for testing
- Mac Studio infrastructure (SearXNG at 192.168.1.10:8080)
- Virtual Threads for parallel I/O operations
- Vavr for functional programming (Try monad)

### Principle V: Three-Workflow Architecture
✅ **PASS** - This feature implements **Funding Discovery** workflow:
- Web search across multiple engines for unknown funding sources
- Metadata-based judging before expensive crawling
- Deduplication to prevent reprocessing

### Principle VI: Complexity Management (ESSENTIAL)
✅ **PASS** - Design follows simplicity principles:
- Single module: northstar-crawler (not multiple microservices)
- Clear single responsibility: search provider integration only
- NO god classes (4 adapter implementations + 1 interface + orchestrator)
- Implementable in discrete phases (Phase 0 research → Phase 1 design → Phase 2 implementation)

### Principle VII: Contact Intelligence Priority
✅ **PASS** - Out of scope for this feature:
- This feature discovers funding source candidates
- Contact extraction happens in Phase 2/3 (deep crawling and human validation)

### Principle VIII: Deployment Responsibility (NON-NEGOTIABLE)
✅ **PASS** - No deployment operations in this feature specification
- Kevin manages all rsync and Docker operations
- AI will not execute deployment scripts

### Principle IX: Script Creation Permission (MANDATORY)
✅ **PASS** - No scripts planned in this specification
- Implementation will use Java code only
- No .sh, .bat, or automation scripts required

### Principle X: Technology Constraints - Lessons from Spring-Crawler (CRITICAL)
✅ **PASS** - Avoids all forbidden technologies:
- ❌ NO crawl4j (using Jsoup if needed in Phase 2)
- ❌ NO langgraph4j (not needed for search adapters)
- ❌ NO langchain4j (not needed for search adapters)
- ❌ NO microservices (monolith architecture)
- ✅ Uses approved technologies:
  - Spring Boot (core framework)
  - Vavr 0.10.6 (functional error handling with Try monad)
  - JUnit/Jupiter (testing)
  - Apache Commons Text (fuzzy matching for anti-spam)

### Principle XI: Two Web Layers - Separate Concerns (ARCHITECTURAL)
✅ **PASS** - Out of scope for this feature:
- This feature is backend search provider integration
- No web UI or API endpoints in this specification
- Dashboard and Client API will be separate features

### Data Precision Standards (CRITICAL)
⚠️ **ATTENTION REQUIRED** - SearchResult entity does NOT store confidence scores:
- Confidence scoring happens in northstar-judging module (separate feature)
- SearchResult stores metadata only (URL, title, description, domain, position)
- When judging module is implemented, ALL confidence scores MUST use BigDecimal with scale 2
- **Action**: Document this requirement in data-model.md during Phase 1

### Initial Assessment: **PASS WITH NOTES**
All constitutional principles are satisfied. No violations requiring justification. One note for future judging module about BigDecimal requirement.

## Project Structure

### Documentation (this feature)
```
specs/003-design-and-implement/
├── spec.md              # Feature specification (COMPLETE)
├── plan.md              # This file (/plan command output - IN PROGRESS)
├── research.md          # Phase 0 output (/plan command - NEXT)
├── data-model.md        # Phase 1 output (/plan command)
├── quickstart.md        # Phase 1 output (/plan command)
├── contracts/           # Phase 1 output (/plan command)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root - Multi-Module Maven Project)
```
northstar-funding/
├── pom.xml                      # Parent POM
├── northstar-domain/            # Domain entities (EXISTING - 19 entities)
│   └── src/main/java/.../domain/
│       ├── SearchResult.java    # Search engine results
│       ├── Domain.java          # Domain deduplication
│       ├── DiscoverySession.java
│       └── SearchEngineType.java (enum)
│
├── northstar-persistence/       # Repositories + Services (EXISTING)
│   ├── src/main/java/.../persistence/
│   │   ├── repository/
│   │   │   ├── SearchResultRepository.java
│   │   │   └── DomainRepository.java
│   │   └── service/
│   │       ├── SearchResultService.java
│   │       └── DomainService.java
│   └── src/main/resources/db/migration/  # Flyway migrations (EXISTING)
│
├── northstar-crawler/           # THIS FEATURE - Search provider adapters
│   ├── src/main/java/.../crawler/
│   │   ├── adapter/             # Search provider adapters (TO BE CREATED)
│   │   │   ├── SearchProviderAdapter.java (interface)
│   │   │   ├── BraveSearchAdapter.java
│   │   │   ├── SearxngAdapter.java
│   │   │   ├── SerperAdapter.java
│   │   │   └── TavilyAdapter.java
│   │   ├── antispam/            # Anti-spam filtering (TO BE CREATED)
│   │   ├── orchestrator/        # Multi-provider execution (TO BE CREATED)
│   │   └── config/              # Configuration (TO BE CREATED)
│   └── src/test/java/.../crawler/
│       ├── contract/            # Contract tests
│       ├── integration/         # TestContainers integration tests
│       └── unit/                # JUnit unit tests
│
├── northstar-judging/           # Future: Phase 2 deep content judging
└── northstar-application/       # Future: REST API + orchestration
```

**Structure Decision**: Multi-module Maven monolith (NOT microservices). This feature implements search provider adapters in the northstar-crawler module, using existing domain entities and persistence layer from northstar-domain and northstar-persistence modules.

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

### Task Generation Strategy

**Source Documents**:
1. `contracts/SearchProviderAdapter.java` - Interface contract (6 methods)
2. `contracts/AntiSpamFilter.java` - Anti-spam filtering contract (5 methods)
3. `contracts/MultiProviderSearchOrchestrator.java` - Orchestrator contract (4 methods)
4. `data-model.md` - Uses existing entities (SearchResult, Domain, DiscoverySession)
5. `quickstart.md` - 8 acceptance scenarios as integration tests

**Task Categories**:

**1. Configuration & Infrastructure** (5 tasks):
- Create SearchProviderConfig class with @ConfigurationProperties
- Create Resilience4j configuration for retry, rate limiting, timeout
- Create Virtual Thread executor bean configuration
- Create application-prod.properties template
- Create provider-specific configuration classes (BraveSearchConfig, etc.)

**2. Adapter Interface & Base Classes** [P] (3 tasks):
- Implement SearchProviderAdapter interface
- Create AbstractSearchProviderAdapter base class with common logic
- Create custom exceptions (RateLimitException, AuthenticationException)

**3. Search Provider Implementations** [P] (4 tasks):
- Implement BraveSearchAdapter (keyword queries)
- Implement SearxngAdapter (keyword queries, self-hosted)
- Implement SerperAdapter (keyword queries)
- Implement TavilyAdapter (AI-optimized queries)

**4. Anti-Spam Filtering** [P] (5 tasks):
- Implement AntiSpamFilter interface
- Implement keyword stuffing detection (unique word ratio)
- Implement domain-metadata mismatch detection (cosine similarity)
- Implement unnatural keyword list detection
- Implement cross-category spam detection

**5. Multi-Provider Orchestration** (4 tasks):
- Implement MultiProviderSearchOrchestrator interface
- Create Virtual Thread parallel execution logic
- Implement result aggregation with deduplication
- Implement DiscoverySession statistics update

**6. API Usage Tracking** (3 tasks):
- Create ProviderApiUsage entity
- Create Flyway migration for provider_api_usage table
- Create ProviderApiUsageRepository and ApiUsageTrackingService

**7. Integration with Existing Services** (3 tasks):
- Integrate with SearchResultService (create SearchResult entities)
- Integrate with DomainService (deduplication, blacklist check)
- Integrate with DiscoverySessionService (session lifecycle)

**8. Contract Tests** [P] (3 tasks):
- Write contract tests for SearchProviderAdapter interface
- Write contract tests for AntiSpamFilter interface
- Write contract tests for MultiProviderSearchOrchestrator interface

**9. Unit Tests** [P] (9 tasks):
- Unit tests for BraveSearchAdapter (Mockito + WireMock)
- Unit tests for SearxngAdapter (Mockito + WireMock)
- Unit tests for SerperAdapter (Mockito + WireMock)
- Unit tests for TavilyAdapter (Mockito + WireMock)
- Unit tests for keyword stuffing detection
- Unit tests for domain-metadata mismatch detection
- Unit tests for unnatural keyword list detection
- Unit tests for cross-category spam detection
- Unit tests for MultiProviderSearchOrchestrator

**10. Integration Tests (TestContainers)** (8 tasks):
- Scenario 1: Single provider search (SearXNG)
- Scenario 2: Multi-provider parallel search
- Scenario 3: Anti-spam filtering
- Scenario 4: Domain deduplication
- Scenario 5: Partial provider failure resilience
- Scenario 6: Rate limiting
- Scenario 7: Timeout handling
- Scenario 8: End-to-end weekly search schedule simulation

### Task Ordering Strategy

**Phase A: Foundation** (Sequential):
1. Configuration & Infrastructure (tasks 1-5)
2. Adapter Interface & Base Classes (tasks 6-8)

**Phase B: Core Implementations** (Parallel):
3. Search Provider Implementations (tasks 9-12) [P]
4. Anti-Spam Filtering (tasks 13-17) [P]

**Phase C: Orchestration** (Sequential - depends on Phase B):
5. Multi-Provider Orchestration (tasks 18-21)
6. API Usage Tracking (tasks 22-24)
7. Integration with Existing Services (tasks 25-27)

**Phase D: Testing** (Parallel after implementations):
8. Contract Tests (tasks 28-30) [P]
9. Unit Tests (tasks 31-39) [P]
10. Integration Tests (tasks 40-47) - Sequential (TestContainers resource constraints)

**Dependency Graph**:
```
Configuration (1-5)
    ↓
Interface + Base (6-8)
    ↓
[Adapters (9-12)] [P] + [Anti-Spam (13-17)] [P]
    ↓
Orchestrator (18-21)
    ↓
API Tracking (22-24) + Integration (25-27)
    ↓
[Contract Tests (28-30)] [P] + [Unit Tests (31-39)] [P]
    ↓
Integration Tests (40-47) - Sequential
```

### Estimated Output

**Total Tasks**: 47 numbered, ordered tasks in tasks.md
**Parallel Tasks**: ~20 tasks marked [P] (adapters, anti-spam, tests)
**Sequential Tasks**: ~27 tasks with dependencies

**Implementation Time Estimate**:
- Phase A (Foundation): 1-2 days
- Phase B (Core Implementations): 2-3 days (parallel execution)
- Phase C (Orchestration): 1-2 days
- Phase D (Testing): 2-3 days (parallel unit tests, sequential integration tests)
- **Total**: 6-10 days for full implementation

### Special Considerations

**No New Entities**: All domain entities already exist in `northstar-domain` module. No Flyway migrations needed except for `provider_api_usage` table.

**External Dependencies**: API keys required for BraveSearch, Serper, Tavily. SearXNG requires Mac Studio container running at 192.168.1.10:8080.

**TestContainers**: Integration tests use ephemeral PostgreSQL containers, NOT the Mac Studio database.

**Virtual Threads**: All parallel execution uses Java 25 Virtual Threads via `Executors.newVirtualThreadPerTaskExecutor()`.

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)  
**Phase 4**: Implementation (execute tasks.md following constitutional principles)  
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command) - ✅ research.md created
- [x] Phase 1: Design complete (/plan command) - ✅ data-model.md, contracts/, quickstart.md, CLAUDE.md updated
- [x] Phase 2: Task planning complete (/plan command - describe approach only) - ✅ 47 tasks planned
- [ ] Phase 3: Tasks generated (/tasks command) - NEXT STEP
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS - No violations, all principles satisfied
- [x] Post-Design Constitution Check: PASS - Design didn't introduce new violations
- [x] All NEEDS CLARIFICATION resolved - 7 clarifications researched in research.md
- [x] Complexity deviations documented - No deviations required

**Artifacts Created**:
- ✅ `plan.md` - This file (implementation plan)
- ✅ `research.md` - Research findings for 7 clarifications
- ✅ `data-model.md` - Data model using existing entities
- ✅ `contracts/SearchProviderAdapter.java` - Interface contract
- ✅ `contracts/AntiSpamFilter.java` - Anti-spam filtering contract
- ✅ `contracts/MultiProviderSearchOrchestrator.java` - Orchestrator contract
- ✅ `quickstart.md` - 8 acceptance scenarios for validation
- ✅ `CLAUDE.md` - Updated with new feature context

**Ready for /tasks Command**: YES - All Phase 0-2 complete, design ready for task generation

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
