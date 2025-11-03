# Implementation Plan: AI-Powered Search Query Generation

**Branch**: `004-create-northstar-query` | **Date**: 2025-11-02 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-create-northstar-query/spec.md`

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
Create a Maven library module (`northstar-query-generation`) that generates AI-powered search queries optimized for different search providers (Brave, Serper, SearXNG, Tavily). The module uses LM Studio (local LLM) via LangChain4j to generate keyword queries for traditional search engines and AI-optimized natural language queries for Tavily. Queries are cached for 24 hours using Caffeine and selectively persisted to PostgreSQL. The module provides an async API using CompletableFuture for Virtual Thread compatibility.

## Technical Context
**Language/Version**: Java 25 (Oracle JDK via SDKMAN) - source and target level 25
**Primary Dependencies**: Spring Boot 3.5.6, LangChain4j (with JDK HttpClient), Caffeine Cache, Vavr 0.10.7, Lombok 1.18.42
**Storage**: PostgreSQL 16 @ Mac Studio (192.168.1.10:5432) - `search_queries` table
**Testing**: JUnit 5 + Mockito for unit tests, TestContainers (planned) for integration tests
**Target Platform**: Maven library module (not standalone application)
**Project Type**: single (library module within multi-module Maven project)
**Performance Goals**: Cache hits <100ms, parallel query generation for 4 providers <30s, non-blocking async API
**Constraints**: LM Studio HTTP/1.1 only (no HTTP/2), 24hr cache TTL, async persistence (fire-and-forget)
**Scale/Scope**: New Maven module with ~15 Java classes, 1000 cached query sets, supports 4 search providers, 25 funding categories

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Critical Principles**:
- ✅ **XML Tag Accuracy**: No XML manipulation in this feature
- ✅ **Domain-Driven Design**: Uses existing `FundingSearchCategory` and `GeographicScope` domain entities
- ✅ **Human-AI Collaboration**: AI generates queries, humans will use them for search (orchestrator feature)
- ✅ **Technology Stack**: Java 25, Spring Boot 3.5.6, PostgreSQL 16, Virtual Threads - COMPLIANT
- ✅ **Technology Constraints**: LangChain4j usage justified - provides actual value for:
  - HTTP/1.1 configuration for LM Studio compatibility
  - Prompt template system with variable substitution
  - Proven in springcrawler reference implementation
  - DOES NOT use forbidden technologies (crawl4j, langgraph4j)
- ✅ **Architecture**: Monolith (library module), not microservice
- ✅ **Complexity Management**: Single responsibility module, ~15 classes, clear boundaries
- ✅ **Data Precision**: Will use BigDecimal for any confidence scores (principle acknowledged)

**No Constitution Violations** - All principles satisfied

**GATE STATUS**: ✅ **PASS** - Ready for Phase 0 Research

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

**Structure Decision**: [DEFAULT to Option 1 unless Technical Context indicates web/mobile app]

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
1. **Setup Tasks** (Sequential):
   - Create Maven module structure
   - Configure pom.xml dependencies (LangChain4j, Caffeine, Vavr)
   - Configure Spring Boot application properties
   - Set up package structure

2. **Model Tasks** [P] (Parallel - independent POJOs):
   - Create SearchProvider enum
   - Create QueryGenerationRequest
   - Create QueryGenerationResponse
   - Create QueryCacheKey

3. **Contract Test Tasks** [P] (Parallel - TDD):
   - Write QueryGenerationStrategy contract test
   - Write QueryGenerationService contract test
   - Write QueryCacheService contract test

4. **Mapping Tasks** [P] (Parallel - pure functions):
   - Implement CategoryMapper with 25 category mappings
   - Implement GeographicMapper with all scope mappings
   - Create PromptTemplates with LangChain4j templates

5. **Configuration Tasks** (Sequential):
   - Implement LmStudioConfig (HTTP/1.1)
   - Implement CaffeineConfig
   - Implement VirtualThreadConfig

6. **Strategy Implementation** [P] (Parallel - after contracts):
   - Implement KeywordQueryStrategy
   - Implement TavilyQueryStrategy

7. **Service Implementation** (Sequential - dependencies):
   - Implement QueryCacheService
   - Implement QueryGenerationService (depends on strategies + cache)

8. **Integration Test Tasks** [P] (Parallel - from quickstart.md):
   - Scenario 1: Single provider generation test
   - Scenario 2: Cache hit test
   - Scenario 3: Keyword vs AI-optimized test
   - Scenario 4: Multi-provider parallel test
   - Scenario 5: Fallback queries test
   - Scenario 6: Persistence verification test
   - Scenario 7: Cache statistics test

9. **Polish Tasks**:
   - Add exception handling and logging
   - Add validation
   - Add Javadoc
   - Update parent pom.xml to include new module

**Ordering Strategy**:
- Setup → Models → Tests → Mappers → Config → Strategies → Services → Integration Tests → Polish
- TDD: Contract tests before implementations
- Parallel tasks [P] marked for independent files
- Dependencies respected (cache before service, strategies before service)

**Estimated Output**: 30-35 numbered, ordered tasks in tasks.md

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
- [x] Phase 0: Research complete (/plan command) ✅
- [x] Phase 1: Design complete (/plan command) ✅
- [x] Phase 2: Task planning complete (/plan command - describe approach only) ✅
- [ ] Phase 3: Tasks generated (/tasks command) - NEXT STEP
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS ✅
- [x] Post-Design Constitution Check: PASS ✅
- [x] All NEEDS CLARIFICATION resolved ✅
- [x] No complexity deviations - all principles satisfied ✅

**Artifacts Generated**:
- [x] research.md - All 10 clarifications resolved
- [x] data-model.md - 13 new classes, 3 reused entities
- [x] contracts/ - 3 API contracts (QueryGenerationService, QueryGenerationStrategy, QueryCacheService)
- [x] quickstart.md - 7 integration test scenarios
- [x] CLAUDE.md - Updated with new technologies

**Constitution Re-Evaluation** (Post-Design):
- ✅ Strategy pattern maintains single responsibility
- ✅ No new microservices (library module)
- ✅ LangChain4j usage justified (HTTP/1.1 config, proven value)
- ✅ Caffeine cache keeps module simple
- ✅ Async API fits Virtual Threads architecture
- ✅ Reuses existing domain entities (DDD compliance)

**READY FOR**: `/tasks` command to generate tasks.md

---
*Based on Constitution v1.4.0 - See `.specify/memory/constitution.md`*
