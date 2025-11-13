
# Implementation Plan: Kafka-Based Event-Driven Search Workflow

**Branch**: `009-create-kafka-based` | **Date**: 2025-11-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/009-create-kafka-based/spec.md`

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
Implement event-driven search workflow using Kafka messaging to execute searches, validate domains, score results, and create funding source candidates. Initial scope: SearXNG adapter only to validate the pattern. Architecture includes 3 new Maven modules (kafka-common, search-adapters, search-workflow) with REST API trigger, Valkey blacklist caching, and migration from LM Studio to Ollama for concurrent query generation.

## Technical Context
**Language/Version**: Java 25 (Oracle JDK via SDKMAN)
**Primary Dependencies**: Spring Boot 3.5.7, Spring Kafka, Spring Data JDBC, Valkey (Redis client), SpringDoc OpenAPI
**Storage**: PostgreSQL 16 (existing schema), Valkey 7.2 (blacklist cache), Kafka 7.4.0 (event streaming)
**Testing**: JUnit 5, Mockito, TestContainers, Spring Boot Test
**Target Platform**: Mac Studio (192.168.1.10) Docker infrastructure + MacBook M2 development
**Project Type**: Backend monolith (multi-module Maven project)
**Performance Goals**: <5s end-to-end workflow, <100ms domain validation (Valkey), 200-300ms query generation, 10 concurrent searches
**Constraints**: SearXNG only initially, no web crawling (metadata-only), session-level deduplication, single application instance
**Scale/Scope**: 3 new Maven modules, 4 Kafka topics, ~15-20 new Java classes, LM Studio → Ollama migration

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: XML Tag Accuracy ✅ PASS
- No XML manipulation required for this feature
- Maven POMs will use full tag names (`<name>`, `<artifactId>`, `<groupId>`)

### Principle II: Domain-Driven Design ✅ PASS
- Uses ubiquitous language: "Funding Sources", "Candidates", "Discovery Session"
- Entities align with domain model: SearchRequest, SearchResult, Domain, FundingSourceCandidate
- No terminology violations

### Principle III: Human-AI Collaboration ✅ PASS
- Search workflow prepares candidates for human validation (PENDING_CRAWL status)
- Confidence scoring (≥0.60) filters automated vs human-review candidates
- Error logging enables human monitoring and intervention

### Principle IV: Technology Stack ✅ PASS
- Java 25 source and target level
- Spring Boot 3.5.7
- PostgreSQL 16 (existing)
- Kafka 7.4.0 (approved technology)
- Valkey 7.2 (Redis-compatible, approved)
- TestContainers for testing
- Mac Studio infrastructure (192.168.1.10)

### Principle V: Three-Workflow Architecture ✅ PASS
- This feature supports Funding Discovery workflow (automated search execution)
- Does not conflict with Database Services or Database Discovery workflows

### Principle VI: Complexity Management ✅ PASS
- Breaks work into 3 focused modules (kafka-common, search-adapters, search-workflow)
- Single responsibility: kafka-common (config/models), search-adapters (engine integration), search-workflow (event processing)
- SearXNG-only initial scope limits complexity
- Clear separation of concerns via Kafka topics

### Principle VII: Contact Intelligence Priority ✅ PASS
- Creates candidates with PENDING_CRAWL status for future contact extraction
- Does not interfere with contact intelligence workflows

### Principle VIII: Deployment Responsibility ✅ PASS
- No rsync operations in implementation
- Kevin will deploy docker-compose changes to Mac Studio
- Implementation does not include deployment automation

### Principle IX: Script Creation Permission ✅ PASS
- No shell scripts created without permission
- Maven commands and Java code only

### Principle X: Technology Constraints ✅ PASS
- **Uses APPROVED**: Spring Kafka ✅, Lombok ✅, Jackson ✅, JUnit ✅
- **Avoids FORBIDDEN**: crawl4j ❌, langgraph4j ❌, langchain4j ❌, microservices ❌
- **Architecture**: MONOLITH (multi-module Maven, single Spring Boot app) ✅
- **Note**: Previous query generation used langchain4j - migrating to direct Ollama HTTP client

### Principle XI: Two Web Layers ✅ PASS
- This feature is backend-only (REST API)
- Does not implement Dashboard or Client web layers (future work)
- Provides REST endpoints for future dashboard integration

### Data Precision Standards ✅ PASS
- All confidence scores use BigDecimal with scale 2 (existing ConfidenceScorer already compliant)
- No new confidence score calculations introduced (reuses existing SearchResultProcessor)

**GATE STATUS: PASS** - All constitutional principles satisfied, no violations to justify

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
northstar-funding/ (multi-module Maven project)
├── pom.xml (parent POM)
├── northstar-domain/                # Existing - domain entities
├── northstar-persistence/           # Existing - repositories + services
├── northstar-crawler/               # Existing - search result processing
├── northstar-query-generation/      # Existing - AI query generation
├── northstar-application/           # Existing - will add REST API
├── northstar-kafka-common/          # NEW - Kafka config + event models
│   ├── src/main/java/com/northstar/funding/kafka/
│   │   ├── config/                  # KafkaConfig, producer/consumer setup
│   │   ├── topics/                  # KafkaTopics constants
│   │   └── events/                  # Event POJOs (SearchRequestEvent, etc.)
│   └── src/test/java/
├── northstar-search-adapters/       # NEW - Search engine adapters
│   ├── src/main/java/com/northstar/funding/search/
│   │   ├── adapter/                 # SearchAdapter interface
│   │   ├── searxng/                 # SearXNGAdapter implementation
│   │   └── config/                  # Adapter configuration
│   └── src/test/java/
└── northstar-search-workflow/       # NEW - Kafka consumers + orchestration
    ├── src/main/java/com/northstar/funding/workflow/
    │   ├── consumer/                # Kafka listeners
    │   ├── service/                 # DomainBlacklistCache, workflow services
    │   └── error/                   # WorkflowErrorHandler
    └── src/test/java/
```

**Structure Decision**: Multi-module Maven monolith (existing pattern), 3 new modules added

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


## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command) - research.md created
- [x] Phase 1: Design complete (/plan command) - contracts/, data-model.md, quickstart.md, CLAUDE.md updated
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved (none existed)
- [x] Complexity deviations documented (none - all principles satisfied)

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
