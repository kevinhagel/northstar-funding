# Implementation Plan: Admin Dashboard Review Queue

**Branch**: `013-create-admin-dashboard` | **Date**: 2025-11-16 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/013-create-admin-dashboard/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   → ✅ Loaded successfully
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   → ✅ All technical decisions resolved from architecture document
   → Project Type: web (frontend + backend)
   → Structure Decision: Option 2 (Web application)
3. Fill the Constitution Check section
   → ✅ Evaluated against NorthStar Constitution v1.4.0
4. Evaluate Constitution Check section
   → ✅ PASS - No constitutional violations
   → Update Progress Tracking: Initial Constitution Check
5. Execute Phase 0 → research.md
   → ✅ COMPLETE - Creating research.md
6. Execute Phase 1 → contracts, data-model.md, quickstart.md, CLAUDE.md update
   → IN PROGRESS
7. Re-evaluate Constitution Check section
   → PENDING
8. Plan Phase 2 → Describe task generation approach (DO NOT create tasks.md)
   → PENDING
9. STOP - Ready for /tasks command
```

**IMPORTANT**: The /plan command STOPS at step 8. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (manual or via tools)

## Summary

Create the first component of the Admin Dashboard - a Review Queue where administrators (Kevin and Huw) can view, filter, sort, and take quick actions on funding source candidates discovered by automated search. This establishes the foundational Vue 3 + PrimeVue frontend architecture and REST API layer for the human-AI hybrid workflow.

**Technical Approach**:
- **Frontend**: Vue 3.4+ with TypeScript, Vite 5.0, PrimeVue 3.50 (FREE components), Pinia 2.1, Axios 1.6
- **Backend**: New Maven module `northstar-rest-api` with Spring Boot 3.5.7, Spring Web, Spring Data JDBC
- **Data Flow**: PostgreSQL → Domain Entity → DTO → JSON → Vue Component
- **DTO Pattern**: Java records with primitive types only (UUID→String, BigDecimal→String, Enum→String)
- **Architecture**: Vertical slice spanning Vue + REST API + Persistence layers

## Technical Context

**Language/Version**:
- **Backend**: Java 25 (Oracle JDK via SDKMAN), Maven multi-module
- **Frontend**: TypeScript 5.0+, Vue 3.4+

**Primary Dependencies**:
- **Backend**: Spring Boot 3.5.7, Spring Web, Spring Data JDBC, Jackson (JSON), northstar-persistence module
- **Frontend**: Vue 3.4+, Vite 5.0, PrimeVue 3.50 (FREE/MIT), Pinia 2.1, Axios 1.6, Vue Router 4.0

**Storage**:
- PostgreSQL 16 at 192.168.1.10:5432
- Database: `northstar_funding`
- New indexes required: V18 migration (status, confidence_score, created_at, search_engine)

**Testing**:
- **Backend**: JUnit 5 (Jupiter), Mockito for unit tests, TestContainers for integration tests
- **Frontend**: Vitest (optional for Feature 013)

**Target Platform**:
- **Backend**: Spring Boot application (port 8080)
- **Frontend**: Vite dev server (port 5173), desktop browsers (Chrome 120+, Firefox 120+)

**Project Type**: web (frontend + backend)

**Performance Goals**:
- Initial page load: <2 seconds with 1000+ candidates
- Filter/sort operations: <1 second response time
- Database queries optimized with indexes

**Constraints**:
- No authentication for Feature 013 (localhost only, Kevin and Huw)
- Desktop only (1920×1080+), no mobile optimization
- FREE PrimeVue components only (no paid add-ons)
- CORS required: Allow `http://localhost:5173` from Spring Boot
- Both servers must run simultaneously for development

**Scale/Scope**:
- 43 functional requirements
- 3 REST API endpoints (list, approve, reject)
- 1 Vue application with 3 routes
- 7 PrimeVue components (DataTable, filters, actions)
- Expected 1000+ candidates in database
- 2 administrators (Kevin, Huw)

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. XML Tag Accuracy (CRITICAL)
**Status**: ✅ PASS - No XML modifications required in this feature
- Maven POM updates will use full tag names (`<module>`, `<dependency>`)
- All XML operations will be explicit and verified

### II. Domain-Driven Design (UBIQUITOUS LANGUAGE)
**Status**: ✅ PASS - Consistent with established domain model
- Uses existing domain entities: `FundingSourceCandidate`, `Domain`, `CandidateStatus`, `SearchEngineType`
- DTOs named clearly: `CandidateDTO`, `CandidateDTOMapper`
- REST API uses domain terminology: `/api/candidates`, approve, reject
- Vue components use domain language: ReviewQueue, filters match domain concepts

### III. Human-AI Collaboration (MANDATORY)
**Status**: ✅ PASS - Core purpose of this feature
- Dashboard enables human review of AI-discovered candidates
- Approve/Reject workflow = human validation of AI results
- Enhancement workflow (Features 014-015) continues collaboration pattern
- Administrators provide strategic decisions (approve/reject), AI provides discovery automation

### IV. Technology Stack (NON-NEGOTIABLE)
**Status**: ✅ PASS - All technologies approved
- **Backend**: Java 25, Spring Boot 3.5.7 (matches constitution requirement 3.5.5+)
- **Database**: PostgreSQL 16 (existing infrastructure)
- **Testing**: JUnit 5, Mockito, TestContainers (constitutional requirement)
- **Frontend**: Vue 3 + PrimeVue (NEW, but not forbidden)
  - Vue 3 is approved modern frontend framework
  - PrimeVue FREE components only (no vendor lock-in)
  - Alternative to Spring MVC/Thymeleaf (constitution allows frontend choice)

### V. Three-Workflow Architecture
**Status**: ✅ PASS - Supports existing workflows
- This dashboard serves all three workflows:
  1. Funding Discovery: Review discovered funding sources
  2. Database Services: (Future) Review database extractions
  3. Database Discovery: (Future) Review discovered database portals
- Human validation step for all workflows implemented here

### VI. Complexity Management (ESSENTIAL)
**Status**: ✅ PASS - Manageable scope with clear phases
- Single responsibility: Review queue for candidate management
- No god classes: CandidateController, CandidateService, ReviewQueue.vue all focused
- Maximum 3 core services: CandidateService, CandidateDTOMapper, API service (frontend)
- Implementable in discrete phases:
  - Phase 1: REST API module setup
  - Phase 2: DTOs and mappers
  - Phase 3: REST endpoints
  - Phase 4: Vue project setup
  - Phase 5: Vue components
- Each phase deliverable in 2-4 hour work units

### VII. Contact Intelligence Priority
**Status**: ✅ PASS - Foundation for future contact features
- Current feature: Candidate review
- Future Features 015-016: Contact intelligence extraction and management
- This dashboard provides UI framework for contact workflows

### VIII. Deployment Responsibility (NON-NEGOTIABLE)
**Status**: ✅ PASS - No deployment in this feature
- Development only: MacBook M2 runs both Vue dev server and Spring Boot
- No rsync operations required
- No Mac Studio deployment in Feature 013

### IX. Script Creation Permission (MANDATORY)
**Status**: ✅ PASS - No scripts created without permission
- No .sh, .bat, .ps1 scripts in this feature
- Only source code: Java, TypeScript, Vue, SQL (Flyway migration)

### X. Technology Constraints - Lessons from Spring-Crawler (CRITICAL)
**Status**: ✅ PASS - No forbidden technologies
- ✅ **NOT using**: crawl4j, langgraph4j, langchain4j, microservices
- ✅ **Using approved**: Spring Boot (monolith), Jackson (JSON), JUnit
- ✅ **Architecture**: Monolith with modular structure (new Maven module for REST API)
- ✅ **Simple technologies**: Spring Web, Vue 3, REST pattern

### XI. Two Web Layers - Separate Concerns (ARCHITECTURAL)
**Status**: ✅ PASS - This IS the Dashboard Web Layer
- **Dashboard Web Layer** (this feature):
  - Internal users: Kevin, Huw
  - Review funding source candidates
  - Enrich with contact intelligence (Features 015-016)
  - Approve/reject/blacklist candidates
  - Spring Security: NOT in Feature 013 (localhost only)
  - Technology: REST API + Vue frontend
- **Client Web Layer** (future):
  - Out of scope for Feature 013
  - Will be separate concern built later

### Data Precision Standards (CRITICAL)
**Status**: ✅ PASS - BigDecimal pattern maintained
- DTO converts `BigDecimal confidenceScore` → `String confidenceScore`
- Frontend receives confidence as String ("0.85")
- No floating point precision issues in API layer
- Database uses `NUMERIC(3,2)` (existing schema)
- Java service layer uses BigDecimal (existing implementation)

**OVERALL GATE STATUS**: ✅ **PASS** - No constitutional violations, ready for Phase 0

## Project Structure

### Documentation (this feature)
```
specs/013-create-admin-dashboard/
├── spec.md              # Feature specification (COMPLETE)
├── plan.md              # This file (/plan command output)
├── research.md          # Phase 0 output (/plan command) - TO BE CREATED
├── data-model.md        # Phase 1 output (/plan command) - TO BE CREATED
├── quickstart.md        # Phase 1 output (/plan command) - TO BE CREATED
├── contracts/           # Phase 1 output (/plan command) - TO BE CREATED
│   ├── candidates-api.yaml          # OpenAPI spec for /api/candidates
│   ├── candidates-list.test.ts      # Contract test for GET /api/candidates
│   ├── candidates-approve.test.ts   # Contract test for PUT /api/candidates/{id}/approve
│   └── candidates-reject.test.ts    # Contract test for PUT /api/candidates/{id}/reject
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Source Code (repository root)

**Structure Decision**: Option 2 (Web application) - Frontend + Backend

```
# Backend: New Maven module
northstar-rest-api/
├── pom.xml                                  # Maven module descriptor
├── src/
│   ├── main/
│   │   ├── java/com/northstar/funding/rest/
│   │   │   ├── NorthstarRestApiApplication.java          # @SpringBootApplication
│   │   │   ├── config/
│   │   │   │   └── CorsConfig.java                       # CORS configuration
│   │   │   ├── controller/
│   │   │   │   └── CandidateController.java              # REST endpoints
│   │   │   ├── dto/
│   │   │   │   ├── CandidateDTO.java                     # Java record
│   │   │   │   ├── CandidatePageDTO.java                 # Paginated response
│   │   │   │   └── CandidateDTOMapper.java               # Domain ↔ DTO conversion
│   │   │   └── service/
│   │   │       └── CandidateService.java                 # Business logic
│   │   └── resources/
│   │       └── application.yml                           # Spring Boot config
│   └── test/
│       └── java/com/northstar/funding/rest/
│           ├── service/
│           │   ├── CandidateServiceTest.java             # Mockito unit tests
│           │   └── CandidateDTOMapperTest.java           # Mapper tests
│           └── controller/
│               └── CandidateControllerTest.java          # REST integration tests (optional)

# Frontend: New Vue 3 application
northstar-admin-dashboard/
├── package.json                             # npm/pnpm dependencies
├── vite.config.ts                           # Vite configuration
├── tsconfig.json                            # TypeScript configuration
├── .env.development                         # Development environment variables
├── index.html                               # Entry HTML
├── src/
│   ├── main.ts                              # Vue app initialization, PrimeVue setup
│   ├── App.vue                              # Root component
│   ├── router/
│   │   └── index.ts                         # Vue Router configuration
│   ├── stores/
│   │   └── candidateStore.ts                # Pinia store for candidates
│   ├── services/
│   │   └── api.ts                           # Axios API service
│   ├── types/
│   │   ├── Candidate.ts                     # TypeScript interface (mirrors CandidateDTO)
│   │   └── CandidatePage.ts                 # Paginated response type
│   ├── views/
│   │   ├── ReviewQueue.vue                  # Main review queue component
│   │   ├── CandidateDetail.vue              # Placeholder page
│   │   └── CandidateEnhance.vue             # Placeholder page
│   └── components/
│       ├── FilterBar.vue                    # Filter controls component
│       └── CandidateActions.vue             # Quick action buttons component
└── tests/
    └── unit/
        └── stores/
            └── candidateStore.test.ts       # Pinia store tests (optional)

# Database: New Flyway migration
northstar-persistence/src/main/resources/db/migration/
└── V18__add_candidate_indexes.sql           # Performance indexes

# Existing modules (not modified significantly)
northstar-domain/                            # Domain entities (existing)
northstar-persistence/                       # Repositories, services (existing)
```

## Phase 0: Outline & Research

**Status**: ✅ COMPLETE

All technical decisions were resolved from the admin dashboard architecture document created earlier today. No NEEDS CLARIFICATION items remain.

**Output**: research.md created with 10 documented decisions and best practices.

## Phase 1: Design & Contracts
*Prerequisites: research.md complete*

**Status**: ✅ COMPLETE

Phase 1 artifacts generated:

1. ✅ **data-model.md**: DTOs, TypeScript interfaces, entity mappings, API examples
2. ✅ **contracts/candidates-api.yaml**: OpenAPI 3.0 specification (3 endpoints)
3. ✅ **contracts/README.md**: Contract testing guide and patterns
4. ✅ **quickstart.md**: Complete development workflow with verification checklist
5. ✅ **CLAUDE.md**: Updated via update-agent-context.sh script

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate tasks from Phase 1 design docs (contracts, data model, quickstart)
- Each contract → contract test task [P]
- Each DTO/entity → model creation task [P]
- Each endpoint → service implementation task
- Each Vue component → component creation task
- Integration tasks for connecting layers

**Ordering Strategy**:
- TDD order: Tests before implementation
- Dependency order:
  1. Database indexes (V18 migration)
  2. DTOs and mappers [P]
  3. Backend services [depends on DTOs]
  4. REST controllers [depends on services]
  5. Vue project setup [independent]
  6. TypeScript types [P - mirrors DTOs]
  7. Axios API service [depends on types]
  8. Pinia stores [depends on API service]
  9. Vue components [depends on stores]
- Mark [P] for parallel execution (independent files)

**Estimated Output**: 35-40 numbered, ordered tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (execute tasks.md following constitutional principles)
**Phase 5**: Validation (run tests, execute quickstart.md, performance validation)

## Complexity Tracking
*Fill ONLY if Constitution Check has violations that must be justified*

No constitutional violations identified - this table is empty.

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning approach documented (/plan command)
- [x] Phase 3: Tasks generated (/tasks command) - 42 tasks created
- [ ] Phase 4: Implementation complete
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS (no new violations)
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (none)

---
*Based on Constitution v1.4.0 - See `.specify/memory/constitution.md`*
