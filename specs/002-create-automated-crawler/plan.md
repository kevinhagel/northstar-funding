
# Implementation Plan: Automated Crawler Infrastructure - Phase 1 Metadata Judging

**Branch**: `002-create-automated-crawler` | **Date**: 2025-10-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/002-create-automated-crawler/spec.md`

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
Automated discovery infrastructure that generates AI-powered search queries across multiple search engines (Searxng, Tavily, Browserbase, Perplexity), performs domain-level deduplication to prevent reprocessing, judges candidates using metadata-only analysis (no web crawling) with weighted confidence scoring, creates high-confidence candidates (>=0.6) for Phase 2 deep crawling, and tracks domain quality metrics. Uses Java 25 Virtual Threads for parallel processing with simple orchestrator pattern.

## Technical Context
**Language/Version**: Java 25 (source and target level 25) via SDKMAN
**Primary Dependencies**: Spring Boot 3.5.5, Spring Data JDBC, Vavr 0.10.6, Lombok, Jsoup
**Storage**: PostgreSQL 16 for permanent records (domains, candidates), Spring Events for async messaging (NO Kafka per constitution amendment)
**Testing**: JUnit/Jupiter + TestContainers for integration tests, Mockito for unit tests
**Target Platform**: Docker on Mac Studio (192.168.1.10), development on MacBook M2 (macOS)
**Project Type**: web (backend Spring Boot monolith, frontend exists separately from 001-automated-funding-discovery)
**Performance Goals**: Process 20-25 search results per query in parallel using Virtual Threads, <500ms for metadata judging per result
**Constraints**: No web crawling in Phase 1 (metadata-only), confidence threshold 0.6 for candidate creation, domain-level deduplication (not URL-level), simple orchestrator pattern (NO Spring Integration per user decision)
**Scale/Scope**: 4 search engines, ~100 search queries per nightly discovery session, domain registry grows over time (blacklist + quality tracking)

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### ✅ Technology Stack (Principle IV - NON-NEGOTIABLE)
- **Java 25**: Source and target level 25 via SDKMAN ✅
- **Spring Boot 3.5.5**: Modern patterns with Virtual Threads ✅
- **PostgreSQL 16**: Permanent storage for domains, candidates, blacklists ✅
- **Testing**: JUnit/Jupiter + TestContainers ✅
- **NO External LLM dependencies**: Using local LM Studio (future Phase 2) ✅

### ✅ Technology Constraints (Principle X - CRITICAL)
- **NO Kafka**: Using Spring Events + @Async for in-process messaging ✅
- **NO crawl4j**: Using Jsoup for future Phase 2 (not Phase 1 metadata judging) ✅
- **NO langgraph4j**: Not using ✅
- **NO langchain4j**: Not using ✅
- **NO Microservices**: Monolith architecture, simple orchestrator ✅
- **Approved Technologies**: Spring Boot, Lombok, Jsoup (Phase 2), JUnit, Vavr 0.10.6 ✅

### ✅ Complexity Management (Principle VI - ESSENTIAL)
- **Simple Orchestrator Pattern**: NO Spring Integration per user decision ✅
- **Single Responsibility**: Domain deduplication separate from judging separate from orchestration ✅
- **Service Count**: Adding 3 services (DomainRegistryService, MetadataJudgingService, CandidateProcessingOrchestrator) - within 3-4 limit ✅

### ✅ Human-AI Collaboration (Principle III - MANDATORY)
- **AI Role**: Automated discovery and metadata judging ✅
- **Human Role**: Review candidates in dashboard (from 001-automated-funding-discovery), blacklist management ✅
- **Workflow**: AI discovers → creates PENDING_CRAWL candidates → humans review and approve/reject ✅

### ✅ Domain-Driven Design (Principle II - UBIQUITOUS LANGUAGE)
- **Funding Sources**: Using correct terminology (not "grants") ✅
- **Domain Entities**: Domain, FundingSourceCandidate, MetadataJudgment ✅
- **Contact Intelligence**: Not applicable to Phase 1 (metadata-only), deferred to Phase 2 ✅

### ✅ Infrastructure Integration (Constitution Section)
- **Mac Studio PostgreSQL**: 192.168.1.10:5432 ✅
- **Development on MacBook M2**: Local development, rsync to Mac Studio ✅
- **Docker Deployment**: Kevin manages rsync (Principle VIII) ✅

**GATE RESULT**: ✅ PASS - No constitutional violations detected

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

**Structure Decision**: Option 2 (Web application) - Backend Spring Boot monolith already exists at `/Users/kevin/github/northstar-funding/backend/`, extending existing infrastructure

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

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |


## Phase 2: Task Planning Approach

**NOTE**: This section describes the task generation approach for the /tasks command. Tasks are NOT created during /plan execution.

### Task Generation Strategy

**From data-model.md**:
1. Domain entity implementation tasks (already complete ✅)
2. Extended FundingSourceCandidate tasks (already complete ✅)
3. DTOs (SearchResult, MetadataJudgment, ProcessingStats) (already complete ✅)
4. DomainRepository with custom queries for:
   - Domain deduplication (findByDomainName)
   - Blacklist queries (findByStatus = BLACKLISTED)
   - Retry logic (findDomainsReadyForRetry)
   - Quality metrics (findHighQualityDomains, findLowQualityDomains)

**From quickstart.md test scenarios**:
1. Integration test for Scenario 1: First-time domain discovery
2. Integration test for Scenario 2: Domain deduplication
3. Integration test for Scenario 3: Low-confidence skipping
4. Integration test for Scenario 4: Blacklist management
5. Integration test for Scenario 5: Parallel processing with Virtual Threads
6. Integration test for Scenario 6: "No funds this year" re-evaluation

**Service Layer** (already implemented ✅):
1. DomainRegistryService: Domain management and blacklisting
2. MetadataJudgingService: Multi-judge weighted scoring
3. CandidateProcessingOrchestrator: Phase 1 workflow coordination

**Search Engine Integration** (future Phase 2 deep crawling):
1. SearchEngine interface
2. SearxngAdapter
3. TavilyAdapter
4. BrowserbaseAdapter
5. PerplexityAdapter
6. Query generation service (AI-powered)

### TDD Ordering

**Already Complete** (implemented before /specify):
- ✅ Domain entity + DomainStatus enum
- ✅ FundingSourceCandidate extension (domainId, new statuses)
- ✅ DTOs (SearchResult, MetadataJudgment, ProcessingStats)
- ✅ DomainRegistryService
- ✅ MetadataJudgingService
- ✅ CandidateProcessingOrchestrator
- ✅ Flyway migrations V8 (domain table) and V9 (candidate status update)

**Remaining Tasks** (for /tasks command):
1. **Tests First** (TDD):
   - DomainRepository integration tests
   - Service layer unit tests (if not already covered)
   - Quickstart integration tests (6 scenarios)
2. **Implementation**:
   - DomainRepository interface + Spring Data JDBC queries
   - Query generation service (AI-powered search queries)
   - Search engine adapters (Searxng, Tavily, Browserbase, Perplexity)
3. **Integration**:
   - Scheduled job for nightly discovery
   - Discovery session orchestration
   - Error handling and monitoring

### Parallelization Opportunities

**[P] Parallel Tasks**:
- Repository tests (different test files)
- Service unit tests (different test files)
- Integration tests (different scenarios, different test files)
- Search engine adapters (different adapter files)

**Sequential Dependencies**:
- Repository tests → Repository implementation
- Service tests → Service implementation
- Integration tests last (require all components)

### Estimated Task Count

- **Phase 3.1**: DomainRepository tests + implementation: ~3 tasks
- **Phase 3.2**: Service layer tests (if needed): ~2 tasks
- **Phase 3.3**: Quickstart integration tests: ~6 tasks
- **Phase 3.4**: Query generation + Search adapters: ~6 tasks
- **Phase 3.5**: Discovery scheduling + orchestration: ~2 tasks
- **Total**: ~19 tasks

**IMPORTANT**: The /tasks command will generate the complete tasks.md file with numbered, ordered tasks.

---

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command) ✅
- [x] Phase 1: Design complete (/plan command) ✅
- [x] Phase 2: Task planning approach described (/plan command) ✅
- [x] Phase 3: Tasks generated (/tasks command) ✅
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS ✅
- [x] Post-Design Constitution Check: PASS ✅
- [x] All NEEDS CLARIFICATION resolved ✅
- [x] Complexity deviations documented: None - all within constitutional limits ✅

**Artifacts Generated**:
- [x] research.md ✅
- [x] data-model.md ✅
- [x] quickstart.md ✅
- [x] CLAUDE.md (agent context) ✅
- [x] tasks.md ✅

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
