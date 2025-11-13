
# Implementation Plan: Docker-Based Integration Tests for REST API

**Branch**: `011-create-comprehensive-docker` | **Date**: 2025-11-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/011-create-comprehensive-docker/spec.md`

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

**Primary Requirement**: Create comprehensive Docker-based integration tests for the REST API layer that validate the complete REST → Query Generation → Kafka → Database workflow without modifying application code unless tests reveal actual bugs.

**Technical Approach**:
- Set up TestContainers for PostgreSQL, Kafka, and Ollama to provide isolated test environments
- Implement end-to-end integration tests for the existing POST /api/search/execute endpoint
- Fix 5 existing failing repository integration tests (environmental Docker issues)
- Establish reusable patterns for future complex API testing
- Document Docker setup for local (remote Docker on Mac Studio @ 192.168.1.10:2375) and CI/CD environments

## Technical Context
**Language/Version**: Java 25 (Oracle JDK via SDKMAN) - source and target level 25
**Primary Dependencies**: Spring Boot 3.5.7, TestContainers 1.21.3, JUnit 5, Mockito, Spring Kafka Test, Embedded Kafka
**Storage**: PostgreSQL 16 (TestContainers), Kafka (TestContainers), Ollama (Mac Studio @ 192.168.1.10:11434)
**Testing**: JUnit 5 (Jupiter), TestContainers for integration tests, Spring Boot Test, AssertJ
**Target Platform**: MacBook M2 (development), Mac Studio (Docker host @ 192.168.1.10:2375), CI/CD pipeline
**Project Type**: Single (existing Maven multi-module Spring Boot monolith)
**Performance Goals**: Test execution <5 minutes per test suite (resolved from spec clarification), container startup <30 seconds
**Constraints**: No application code changes unless tests find bugs, reusable patterns for future tests, Docker required (local or remote)
**Scale/Scope**: 7 integration test scenarios, 5 repository test fixes, REST API module + Persistence module testing

**Docker Configuration**:
- **Local Docker Desktop**: Primary approach (MacBook M2)
- **Remote Docker on Mac Studio**: Alternative using DOCKER_HOST=tcp://192.168.1.10:2375
- **TestContainers Config**: ~/.testcontainers.properties with docker.host setting
- **Container Images**:
  - postgres:16-alpine (matches production)
  - confluentinc/cp-kafka:7.4.0 (KRaft mode, no Zookeeper - matches production)
  - Ollama mocked in tests (runs natively on Mac Studio)

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Constitutional Compliance Analysis

**✅ Principle IV: Technology Stack (NON-NEGOTIABLE)**
- Java 25 with Spring Boot 3.5.7: ✅ Compliant
- TestContainers 1.21.3 for integration tests: ✅ Explicitly approved in constitution
- PostgreSQL 16: ✅ Compliant
- JUnit/Jupiter testing: ✅ Approved
- No forbidden technologies (crawl4j, langgraph4j, langchain4j, microservices): ✅ Compliant

**✅ Principle VI: Complexity Management (ESSENTIAL)**
- Feature is test infrastructure, not adding application complexity: ✅ Compliant
- Reusable patterns for future tests: ✅ Supports maintainability
- No new services or components: ✅ Testing layer only

**✅ Principle X: Technology Constraints (CRITICAL)**
- Using approved technologies only: ✅ TestContainers, JUnit, Spring Test
- Monolith architecture preserved: ✅ No architectural changes
- No forbidden dependencies: ✅ Compliant

**✅ Development Standards**
- Test-Driven Development: ✅ Feature IS about testing
- Integration Testing with TestContainers: ✅ Core requirement
- Clean Architecture: ✅ Tests validate existing architecture without changes

**✅ Infrastructure Integration**
- Mac Studio Services (192.168.1.10): ✅ Uses remote Docker host
- Development Machine (MacBook M2): ✅ Tests run locally
- TestContainers for ephemeral containers: ✅ Constitution-mandated pattern

**✅ Principle VIII: Deployment Responsibility (NON-NEGOTIABLE)**
- No rsync or deployment operations: ✅ Testing only, no deployment

**✅ Principle IX: Script Creation Permission (MANDATORY)**
- May need Docker setup scripts: ⚠️ Will ask permission before creating

**Summary**: ✅ **PASSES ALL CONSTITUTIONAL REQUIREMENTS**
- No violations detected
- Aligns with approved testing patterns
- Uses only constitution-approved technologies
- No application code modifications planned

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

**Structure Decision**: Existing Maven multi-module Spring Boot monolith structure

### Existing Maven Module Structure
```
northstar-funding/ (root)
├── northstar-domain/              # Domain entities
├── northstar-persistence/         # Repositories + Services
│   └── src/test/java/            # Repository integration tests (5 failing)
├── northstar-rest-api/            # REST API controllers
│   └── src/test/java/            # REST API integration tests (NEW)
├── northstar-kafka-common/        # Kafka event models
├── northstar-search-adapters/     # Search engine adapters
├── northstar-search-workflow/     # Kafka consumers
├── northstar-query-generation/    # AI query generation
└── northstar-crawler/             # Search result processing
```

### Test Organization (Feature 011)
```
northstar-rest-api/src/test/java/
└── com/northstar/funding/rest/
    ├── integration/              # NEW - Integration tests
    │   ├── SearchWorkflowIntegrationTest.java
    │   ├── KafkaIntegrationTest.java
    │   └── DatabasePersistenceIntegrationTest.java
    └── controller/
        └── SearchControllerTest.java  # Existing unit tests (4 tests)

northstar-persistence/src/test/java/
└── com/northstar/funding/persistence/repository/
    ├── DomainRepositoryIntegrationTest.java         # FIX
    ├── FundingProgramRepositoryIntegrationTest.java # FIX
    ├── OrganizationRepositoryIntegrationTest.java   # FIX
    ├── AdminUserRepositoryIntegrationTest.java      # FIX
    └── SearchResultRepositoryIntegrationTest.java   # FIX
```

### Documentation (this feature)
```
specs/011-create-comprehensive-docker/
├── plan.md              # This file
├── research.md          # Phase 0: Docker setup, TestContainers patterns
├── data-model.md        # Phase 1: Test data models
├── quickstart.md        # Phase 1: Run tests locally
├── contracts/           # Phase 1: Test scenarios (Given/When/Then)
│   ├── scenario-1-successful-flow.md
│   ├── scenario-2-invalid-request.md
│   └── scenario-3-database-verification.md
└── tasks.md             # Phase 2: Created by /tasks command
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
1. **Infrastructure Setup** (Docker, TestContainers configuration)
   - Create AbstractIntegrationTest base class
   - Configure TestContainers for PostgreSQL and Kafka
   - Create application-integration-test.yml
   - Document Docker setup in DOCKER-SETUP.md

2. **Test Infrastructure** (From data-model.md)
   - Create TestFixtures utility class
   - Create ExpectedDatabaseState assertion utilities
   - Create ExpectedKafkaEvents assertion utilities

3. **Integration Test Implementation** (From contracts/)
   - Scenario 1: SearchWorkflowIntegrationTest (successful flow)
   - Scenario 2: Invalid request handling
   - Scenario 3: DatabasePersistenceIntegrationTest
   - Scenario 4: KafkaIntegrationTest
   - Scenario 5: Concurrent request handling

4. **Repository Test Fixes** (5 failing tests)
   - Update DomainRepositoryTest → DomainRepositoryIntegrationTest
   - Update FundingProgramRepositoryTest → FundingProgramRepositoryIntegrationTest
   - Update OrganizationRepositoryTest → OrganizationRepositoryIntegrationTest
   - Update AdminUserRepositoryTest → AdminUserRepositoryIntegrationTest
   - Update SearchResultRepositoryTest → SearchResultRepositoryIntegrationTest

5. **Documentation**
   - Create DOCKER-SETUP.md (if Kevin approves script creation)
   - Update CLAUDE.md testing section
   - Verify quickstart.md instructions

**Ordering Strategy**:
- **Phase A**: Infrastructure setup (base classes, configuration) - Sequential
- **Phase B**: Test utilities (fixtures, assertions) - Parallel [P]
- **Phase C**: Integration tests - Parallel [P] (independent test classes)
- **Phase D**: Repository fixes - Parallel [P] (independent test classes)
- **Phase E**: Documentation - Sequential

**Dependency Rules**:
- Phase B depends on Phase A complete
- Phases C and D depend on Phase B complete
- Phase E depends on all tests passing

**Estimated Output**: 18-22 numbered, ordered tasks in tasks.md

**Test Execution Order** (TDD Pattern):
1. Create test class (Red - tests fail)
2. Implement infrastructure/utilities (Green - tests pass)
3. Refactor if needed
4. Verify with Maven test command

**Parallel Execution Tags**:
- [P] = Can execute in parallel (independent files)
- Sequential tasks have dependencies on previous completion

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
- [x] Phase 1: Design complete (/plan command) - data-model.md, contracts/, quickstart.md, CLAUDE.md updated
- [x] Phase 2: Task planning approach defined (/plan command) - Ready for /tasks command
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [ ] Initial Constitution Check: PASS
- [ ] Post-Design Constitution Check: PASS
- [ ] All NEEDS CLARIFICATION resolved
- [ ] Complexity deviations documented

---
*Based on Constitution v2.1.1 - See `/memory/constitution.md`*
