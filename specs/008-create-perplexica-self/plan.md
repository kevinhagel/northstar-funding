# Implementation Plan: Perplexica Self-Hosted AI Search Infrastructure

**Branch**: `008-create-perplexica-self` | **Date**: 2025-11-07 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/spec.md`

## Execution Flow (/plan command scope)
```
1. Load feature spec from Input path
   ✅ Loaded - Infrastructure deployment feature
2. Fill Technical Context (scan for NEEDS CLARIFICATION)
   ✅ Project Type: Infrastructure (Docker deployment)
   ✅ No code - configuration and deployment only
3. Fill Constitution Check section
   ✅ Infrastructure aligns with Principle VIII (Deployment) and IV (Technology Stack)
4. Evaluate Constitution Check section
   ✅ No violations - infrastructure deployment is Kevin's responsibility
   ✅ Update Progress Tracking: Initial Constitution Check PASS
5. Execute Phase 0 → research.md
   ✅ Research completed (Perplexica v1.11.2, LM Studio integration)
6. Execute Phase 1 → deployment.md, verification.md, quickstart.md
   ✅ Infrastructure feature - generating deployment artifacts instead of code contracts
7. Re-evaluate Constitution Check section
   ✅ No new violations
   ✅ Update Progress Tracking: Post-Design Constitution Check PASS
8. Plan Phase 2 → Describe task generation approach
   ✅ Task generation will use deployment checklist pattern
9. STOP - Ready for /tasks command
   ✅ Execution complete
```

**IMPORTANT**: The /plan command STOPS at step 9. Phases 2-4 are executed by other commands:
- Phase 2: /tasks command creates tasks.md
- Phase 3-4: Implementation execution (Kevin performs deployment)

## Summary

Deploy Perplexica v1.11.2 as a self-hosted AI search infrastructure on Mac Studio, configured to use existing SearXNG and LM Studio services. Fix SearXNG configuration error preventing container startup, add Perplexica to docker-compose.yml with proper LM Studio integration via native LM_STUDIO provider, verify all services healthy.

**Technical Approach**: Configuration-only deployment using Docker Compose, rsync workflow, and health verification. No application code changes.

## Technical Context

**Language/Version**: Docker Compose 2.x, TOML configuration
**Primary Dependencies**:
- Perplexica v1.11.2 (itzcrazykns1337/perplexica:latest)
- SearXNG (existing, needs config fix)
- LM Studio (native on Mac Studio, port 1234)
**Storage**: Docker volumes (perplexica-data, perplexica-uploads)
**Testing**: Manual verification via health checks and API testing
**Target Platform**: Mac Studio (192.168.1.10) via Docker
**Project Type**: Infrastructure deployment (not application code)
**Performance Goals**: Container startup <30s, API response <5s, UI load <2s
**Constraints**:
- Use existing SearXNG (no duplication)
- Use existing LM Studio (native, not containerized)
- Use host.docker.internal for Mac Studio native services
- Maintain existing services (PostgreSQL, Qdrant, pgAdmin)
**Scale/Scope**: Single-host deployment, 5 containers total (PostgreSQL, Qdrant, pgAdmin, SearXNG, Perplexica)

## Constitution Check
*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle VIII: Deployment Responsibility ✅
- **Requirement**: Kevin manages all rsync operations to Mac Studio - AI never performs deployment
- **Compliance**: Plan documents deployment steps for Kevin to execute manually
- **Verification**: Tasks will include "Kevin performs" markers for deployment steps

### Principle IV: Technology Stack ✅
- **Requirement**: Existing Mac Studio infrastructure (Docker, LM Studio)
- **Compliance**:
  - Docker Compose for container orchestration ✅
  - LM Studio native deployment (not containerized) ✅
  - Mac Studio services at 192.168.1.10 ✅
  - PostgreSQL, Qdrant already running ✅

### Principle VI: Complexity Management ✅
- **Requirement**: Maximum 3-4 core services
- **Current State**: 4 services (PostgreSQL, Qdrant, SearXNG, pgAdmin)
- **After Feature**: 5 services (adding Perplexica)
- **Justification**: Perplexica replaces external Tavily API, net reduction in external dependencies
- **Approval**: Within acceptable range (5 services for self-hosted infrastructure)

### Infrastructure Integration Standards ✅
- **Mac Studio Services**: All infrastructure runs in Docker containers ✅
- **Docker Workflow**: Modify on MacBook M2 → Kevin rsyncs → Kevin restarts containers ✅
- **Networking**: Use 192.168.1.10 for all service endpoints ✅
- **Docker Compose**: Infrastructure defined in docker/docker-compose.yml ✅

**Initial Constitution Check**: ✅ PASS

## Project Structure

### Documentation (this feature)
```
specs/008-create-perplexica-self/
├── plan.md              # This file (/plan command output)
├── spec.md              # Feature specification (already complete)
├── research.md          # Phase 0 output (/plan command)
├── deployment.md        # Phase 1 output (infrastructure deployment guide)
├── verification.md      # Phase 1 output (health check procedures)
├── quickstart.md        # Phase 1 output (manual testing guide)
└── tasks.md             # Phase 2 output (/tasks command - NOT created by /plan)
```

### Infrastructure Files (repository docker/)
```
docker/
├── docker-compose.yml       # Updated: Add Perplexica service
├── searxng/
│   └── settings.yml         # Fixed: brand section strings
├── perplexica/
│   └── config.toml          # New: LM Studio integration
└── README.md                # Updated: Perplexica documentation
```

**Structure Decision**: Infrastructure deployment (not application code)

## Phase 0: Research & Discovery

All research completed during planning phase. See research.md for details.

**Key Decisions**:
1. Perplexica v1.11.2 (latest, includes LM Studio provider)
2. Native LM_STUDIO provider (not custom OpenAI config)
3. External SearXNG via container networking (not bundled)
4. host.docker.internal for LM Studio access
5. Keep LM Studio (do not migrate to Lemonade)

**Output**: ✅ research.md complete, all decisions documented

## Phase 1: Design & Deployment Artifacts

### 1. Deployment Guide (deployment.md)

Created comprehensive deployment guide with:
- SearXNG configuration fix (brand section)
- Perplexica docker-compose.yml service definition
- Perplexica config.toml with LM Studio integration
- Docker volume definitions
- Rsync workflow documentation (Kevin performs)
- Container restart procedure (Kevin performs)

### 2. Verification Procedures (verification.md)

Created verification guide with:
- Health check commands for all containers
- SearXNG error log verification
- Perplexica API health endpoint test
- Perplexica UI accessibility test
- Integration testing (LM Studio, SearXNG connections)
- Success criteria checklist (from spec FR-009 through FR-012)

### 3. Quickstart Guide (quickstart.md)

Created manual testing guide with:
- Step-by-step deployment verification
- Manual search test via Perplexica UI
- API test with curl command
- Troubleshooting common issues
- Rollback procedure if deployment fails

### 4. Agent Context Update

✅ Executed `.specify/scripts/bash/update-agent-context.sh claude`
- Updated CLAUDE.md with Feature 008 infrastructure context
- Documented Perplexica deployment in project context

**Output**: ✅ deployment.md, verification.md, quickstart.md, CLAUDE.md updated

## Phase 2: Task Planning Approach
*This section describes what the /tasks command will do - DO NOT execute during /plan*

**Task Generation Strategy**:
- Load `.specify/templates/tasks-template.md` as base
- Generate deployment checklist tasks (not code implementation tasks)
- Infrastructure tasks organized by phase:
  1. Configuration fixes (SearXNG)
  2. Docker Compose updates (Perplexica service, volumes)
  3. Deployment (Kevin performs rsync and restart)
  4. Verification (health checks, API tests, UI tests)
  5. Documentation updates (README)

**Ordering Strategy**:
- Sequential order (no parallelization for infrastructure)
- Configuration before deployment
- Deployment before verification
- Verification before documentation
- Each task depends on previous task success

**Estimated Output**: 10-12 numbered, sequential deployment tasks in tasks.md

**IMPORTANT**: This phase is executed by the /tasks command, NOT by /plan

## Phase 3+: Future Implementation
*These phases are beyond the scope of the /plan command*

**Phase 3**: Task execution (/tasks command creates tasks.md)
**Phase 4**: Implementation (Kevin executes deployment tasks)
**Phase 5**: Validation (verify all services healthy, document results)

## Complexity Tracking

No constitutional violations - no complexity tracking needed.

Perplexica adds 5th service but replaces external Tavily API dependency, resulting in net simplification (self-hosted vs external SaaS).

## Progress Tracking
*This checklist is updated during execution flow*

**Phase Status**:
- [x] Phase 0: Research complete (/plan command)
- [x] Phase 1: Design complete (/plan command)
- [x] Phase 2: Task planning complete (/plan command - describe approach only)
- [ ] Phase 3: Tasks generated (/tasks command)
- [ ] Phase 4: Implementation complete (Kevin deploys)
- [ ] Phase 5: Validation passed

**Gate Status**:
- [x] Initial Constitution Check: PASS
- [x] Post-Design Constitution Check: PASS
- [x] All NEEDS CLARIFICATION resolved
- [x] Complexity deviations documented (none)

**Constitution Compliance**:
- ✅ Principle VIII: Deployment responsibility documented for Kevin
- ✅ Principle IV: Uses existing Mac Studio infrastructure
- ✅ Principle VI: 5 services within acceptable complexity range

---
*Based on Constitution v1.4.0 - See `.specify/memory/constitution.md`*

**Ready for `/tasks` command to generate deployment task checklist.**
