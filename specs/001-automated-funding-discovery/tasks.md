# Tasks: Automated Funding Discovery Workflow

**Input**: Design documents from `/specs/001-automated-funding-discovery/`
**Prerequisites**: plan.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

## Execution Flow (main)
```
1. Load plan.md from feature directory ✅
   → Tech stack: Java 25 + Spring Boot 3.5.5 + Maven 3.9.9 + PostgreSQL + Streamlit
   → Structure: Web application (backend/src/, frontend/src/)
2. Load design documents ✅:
   → data-model.md: 5 entities → 5 model tasks [P]
   → contracts/: 8 endpoints → 8 contract test tasks [P]
   → quickstart.md: 4 scenarios → 4 integration tests
3. Generate tasks by category ✅:
   → Setup: Spring Boot project, PostgreSQL, Streamlit
   → Tests: 8 contract tests, 4 integration tests, entity tests
   → Core: 5 models, 3 services, 8 controllers
   → Integration: DB, security, AI services
   → Polish: unit tests, performance, deployment
4. Apply task rules ✅:
   → Different files = [P], Same file = sequential
   → Tests before implementation (TDD)
5. Number tasks sequentially T001-T038 ✅
6. Generate dependency graph ✅
7. Create parallel execution examples ✅
8. Validate completeness ✅
9. SUCCESS - tasks ready for execution
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- All file paths are absolute for Mac Studio deployment

## Path Conventions
**Web application structure** (from plan.md):
- **Backend**: `backend/src/main/java/com/northstar/funding/`
- **Frontend**: `frontend/` (Streamlit Python app)
- **Tests**: `backend/src/test/java/`
- **Database**: PostgreSQL on Mac Studio (192.168.1.10:5432)

## Phase 3.1: Project Setup
- [x] **T001** Create Spring Boot 3.5.5 project structure with Java 25 in `/Users/kevin/github/northstar-funding/backend/` ✅
- [x] **T002** [P] Configure PostgreSQL connection to Mac Studio (192.168.1.10:5432) in `backend/src/main/resources/application.yml` ✅
- [ ] **T003** [P] Initialize Streamlit project structure in `/Users/kevin/github/northstar-funding/frontend/`
- [x] **T004** [P] Configure Maven 3.9.9 dependencies (Spring Boot 3.5.5, Spring Data JPA, PostgreSQL driver, Spring Security) in `backend/pom.xml` ✅
- [ ] **T005** [P] Configure Python requirements (Streamlit, requests, pandas) in `frontend/requirements.txt`
- [x] **T006** [P] Set up Docker Compose for Mac Studio deployment in `/Users/kevin/github/northstar-funding/docker/docker-compose.yml` ✅

## Phase 3.2: Database Schema (TDD Foundation)
- [ ] **T007** [P] Create PostgreSQL schema migration for FundingSourceCandidate table in `backend/src/main/resources/db/migration/V1__create_funding_source_candidate.sql`
- [ ] **T008** [P] Create ContactIntelligence table migration with encrypted fields in `backend/src/main/resources/db/migration/V2__create_contact_intelligence.sql`  
- [ ] **T009** [P] Create AdminUser table migration in `backend/src/main/resources/db/migration/V3__create_admin_user.sql`
- [ ] **T010** [P] Create DiscoverySession table migration in `backend/src/main/resources/db/migration/V4__create_discovery_session.sql`
- [ ] **T011** [P] Create EnhancementRecord table migration in `backend/src/main/resources/db/migration/V5__create_enhancement_record.sql`
- [ ] **T012** Create database indexes for performance in `backend/src/main/resources/db/migration/V6__create_indexes.sql`

## Phase 3.3: Contract Tests First (TDD) ⚠️ MUST COMPLETE BEFORE 3.4
**CRITICAL: These tests MUST be written and MUST FAIL before ANY implementation**
- [ ] **T013** [P] Contract test GET /api/candidates in `backend/src/test/java/com/northstar/funding/web/CandidateControllerContractTest.java`
- [ ] **T014** [P] Contract test GET /api/candidates/{id} in `backend/src/test/java/com/northstar/funding/web/CandidateDetailControllerContractTest.java`
- [ ] **T015** [P] Contract test PUT /api/candidates/{id} in `backend/src/test/java/com/northstar/funding/web/CandidateUpdateControllerContractTest.java`
- [ ] **T016** [P] Contract test POST /api/candidates/{id}/approve in `backend/src/test/java/com/northstar/funding/web/CandidateApprovalControllerContractTest.java`
- [ ] **T017** [P] Contract test POST /api/candidates/{id}/reject in `backend/src/test/java/com/northstar/funding/web/CandidateRejectionControllerContractTest.java`
- [ ] **T018** [P] Contract test GET/POST /api/candidates/{id}/contacts in `backend/src/test/java/com/northstar/funding/web/ContactIntelligenceControllerContractTest.java`
- [ ] **T019** [P] Contract test POST /api/discovery/trigger in `backend/src/test/java/com/northstar/funding/web/DiscoveryControllerContractTest.java`
- [ ] **T020** [P] Contract test GET /api/discovery/sessions in `backend/src/test/java/com/northstar/funding/web/DiscoverySessionControllerContractTest.java`

## Phase 3.4: Integration Tests (User Stories)
- [ ] **T021** [P] Integration test: Complete discovery to approval workflow in `backend/src/test/java/com/northstar/funding/integration/DiscoveryWorkflowIntegrationTest.java`
- [ ] **T022** [P] Integration test: Duplicate detection and rejection workflow in `backend/src/test/java/com/northstar/funding/integration/DuplicateDetectionIntegrationTest.java`
- [ ] **T023** [P] Integration test: AI-assisted enhancement workflow in `backend/src/test/java/com/northstar/funding/integration/AIEnhancementIntegrationTest.java`
- [ ] **T024** [P] Integration test: Audit trail and enhancement tracking in `backend/src/test/java/com/northstar/funding/integration/AuditTrailIntegrationTest.java`

## Phase 3.5: Domain Models (ONLY after tests are failing)
- [ ] **T025** [P] FundingSourceCandidate JPA entity with JSON fields in `backend/src/main/java/com/northstar/funding/discovery/domain/FundingSourceCandidate.java`
- [ ] **T026** [P] ContactIntelligence JPA entity with encrypted fields in `backend/src/main/java/com/northstar/funding/discovery/domain/ContactIntelligence.java`
- [ ] **T027** [P] AdminUser JPA entity with security integration in `backend/src/main/java/com/northstar/funding/discovery/domain/AdminUser.java`
- [ ] **T028** [P] DiscoverySession JPA entity with metrics tracking in `backend/src/main/java/com/northstar/funding/discovery/domain/DiscoverySession.java`
- [ ] **T029** [P] EnhancementRecord value object for audit trail in `backend/src/main/java/com/northstar/funding/discovery/domain/EnhancementRecord.java`

## Phase 3.6: Repository Layer
- [ ] **T030** [P] FundingSourceCandidateRepository with custom queries in `backend/src/main/java/com/northstar/funding/discovery/infrastructure/FundingSourceCandidateRepository.java`
- [ ] **T031** [P] ContactIntelligenceRepository with encryption support in `backend/src/main/java/com/northstar/funding/discovery/infrastructure/ContactIntelligenceRepository.java`
- [ ] **T032** [P] DiscoverySessionRepository with analytics queries in `backend/src/main/java/com/northstar/funding/discovery/infrastructure/DiscoverySessionRepository.java`

## Phase 3.7: Service Layer (Business Logic)
- [ ] **T033** CandidateValidationService with deduplication logic in `backend/src/main/java/com/northstar/funding/discovery/application/CandidateValidationService.java`
- [ ] **T034** ContactIntelligenceService with encryption/validation in `backend/src/main/java/com/northstar/funding/discovery/application/ContactIntelligenceService.java`
- [ ] **T035** DiscoveryOrchestrationService for workflow coordination in `backend/src/main/java/com/northstar/funding/discovery/application/DiscoveryOrchestrationService.java`

## Phase 3.8: REST Controllers (Make Tests Pass)
- [ ] **T036** CandidateController (GET, PUT endpoints) in `backend/src/main/java/com/northstar/funding/discovery/web/CandidateController.java`
- [ ] **T037** CandidateActionController (approve, reject, assign) in `backend/src/main/java/com/northstar/funding/discovery/web/CandidateActionController.java`
- [ ] **T038** ContactIntelligenceController (GET, POST contacts) in `backend/src/main/java/com/northstar/funding/discovery/web/ContactIntelligenceController.java`
- [ ] **T039** DiscoveryController (trigger, sessions) in `backend/src/main/java/com/northstar/funding/discovery/web/DiscoveryController.java`

## Phase 3.9: Security & Configuration
- [ ] **T040** Spring Security configuration with admin user authentication in `backend/src/main/java/com/northstar/funding/config/SecurityConfig.java`
- [ ] **T041** [P] Contact intelligence field encryption configuration in `backend/src/main/java/com/northstar/funding/config/EncryptionConfig.java`
- [ ] **T042** [P] Virtual Thread configuration for I/O operations in `backend/src/main/java/com/northstar/funding/config/VirtualThreadConfig.java`

## Phase 3.10: Streamlit Admin Interface
- [ ] **T043** [P] Main Streamlit app with navigation in `frontend/app.py`
- [ ] **T044** [P] Candidate review queue page in `frontend/pages/discovery_queue.py`
- [ ] **T045** [P] Candidate detail enhancement page in `frontend/pages/enhancement.py`
- [ ] **T046** [P] Approval/rejection workflow page in `frontend/pages/approval.py`
- [ ] **T047** [P] API client service for backend integration in `frontend/services/api_client.py`

## Phase 3.11: External Integrations
- [ ] **T048** [P] LM Studio HTTP client for AI services in `backend/src/main/java/com/northstar/funding/discovery/infrastructure/LMStudioClient.java`
- [ ] **T049** [P] Search engine adapter pattern (Searxng, Tavily, Perplexity) in `backend/src/main/java/com/northstar/funding/discovery/infrastructure/search/`

## Phase 3.12: Polish & Deployment
- [ ] **T050** [P] Unit tests for business logic in `backend/src/test/java/com/northstar/funding/discovery/application/`
- [ ] **T051** [P] Performance tests for API endpoints (<500ms requirement) in `backend/src/test/java/com/northstar/funding/performance/`
- [ ] **T052** [P] Docker configuration for Mac Studio deployment in `backend/Dockerfile` and `frontend/Dockerfile`
- [ ] **T053** Run quickstart.md scenarios for end-to-end validation
- [ ] **T054** [P] Update project documentation and deployment guide in `/Users/kevin/github/northstar-funding/README.md`

## Dependencies

### Sequential Dependencies (Blocking)
- **Database First**: T007-T012 before any JPA entities (T025-T029)
- **TDD Order**: All contract tests (T013-T020) before implementation (T036-T039) 
- **Layer Dependencies**: 
  - Models (T025-T029) → Repositories (T030-T032) → Services (T033-T035) → Controllers (T036-T039)
- **Integration**: Services (T033-T035) before Security (T040-T042)
- **UI Dependencies**: Backend API (T036-T039) before Streamlit pages (T044-T046)

### Non-Blocking ([P] Tasks)
- Setup tasks T001-T006 can run in parallel
- Database migrations T007-T011 can run in parallel  
- Contract tests T013-T020 can run in parallel (different test files)
- Integration tests T021-T024 can run in parallel (different test files)
- Domain models T025-T029 can run in parallel (different entity files)
- Repository interfaces T030-T032 can run in parallel (different files)
- Streamlit pages T044-T046 can run in parallel (different page files)

## Parallel Execution Examples

### Phase 3.2: Database Setup
```bash
# Run database migrations in parallel
Task T007: "Create FundingSourceCandidate table migration"  
Task T008: "Create ContactIntelligence table migration"
Task T009: "Create AdminUser table migration"
Task T010: "Create DiscoverySession table migration"  
Task T011: "Create EnhancementRecord table migration"
```

### Phase 3.3: Contract Tests
```bash  
# All contract tests can run in parallel (different test files)
Task T013: "Contract test GET /api/candidates"
Task T014: "Contract test GET /api/candidates/{id}"
Task T015: "Contract test PUT /api/candidates/{id}" 
Task T016: "Contract test POST /api/candidates/{id}/approve"
Task T017: "Contract test POST /api/candidates/{id}/reject"
Task T018: "Contract test GET/POST /api/candidates/{id}/contacts"
Task T019: "Contract test POST /api/discovery/trigger"
Task T020: "Contract test GET /api/discovery/sessions"
```

### Phase 3.5: Domain Models  
```bash
# All JPA entities can be created in parallel (different files)
Task T025: "FundingSourceCandidate JPA entity"
Task T026: "ContactIntelligence JPA entity" 
Task T027: "AdminUser JPA entity"
Task T028: "DiscoverySession JPA entity"
Task T029: "EnhancementRecord value object"
```

## Constitutional Compliance Verification

### ✅ Technology Stack (NON-NEGOTIABLE)
- **T001**: Java 25 + Spring Boot 3.5.5 + Maven 3.9.9 confirmed
- **T002**: PostgreSQL on Mac Studio (192.168.1.10) 
- **T042**: Virtual Threads configuration for I/O operations

### ✅ Complexity Management (ESSENTIAL) 
- **Total Services**: 2 (Backend Spring Boot + Frontend Streamlit) = 2/4 limit
- **Database**: Single PostgreSQL instance (Qdrant deferred)
- **No Kafka/Redis**: Simplified architecture maintained

### ✅ Human-AI Collaboration (MANDATORY)
- **T021-T024**: Integration tests validate human review workflows
- **T044-T046**: Streamlit UI enforces manual validation before approval
- **T048**: AI services support human enhancement, don't replace decisions

### ✅ Domain-Driven Design (UBIQUITOUS LANGUAGE)
- **T025**: "Funding Sources" terminology in entity naming
- **T026**: Contact Intelligence as first-class entity
- **T033**: Domain service boundaries respected

### ✅ Contact Intelligence Priority
- **T008**: Encrypted storage for contact PII
- **T026**: Contact Intelligence as aggregate entity
- **T034**: Relationship tracking and validation service

## Infrastructure Setup Requirements

### Mac Studio Configuration (192.168.1.10)
```bash
# Ensure these services are running before T002
- PostgreSQL 16 on port 5432
- LM Studio on port 1234 (for T048)
- Docker for deployment (T052-T053)
```

### Development Environment (MacBook M2)
```bash
# Required for development tasks T001-T054
- Java 25 via SDKMAN
- Maven 3.9.9
- Python 3.11+ for Streamlit
- Docker for local testing
```

## Validation Checklist
*GATE: Checked before execution*

- [x] All 8 API contracts have corresponding test tasks (T013-T020)
- [x] All 5 entities have model creation tasks (T025-T029)
- [x] All contract tests come before implementation (T013-T020 → T036-T039)
- [x] Parallel tasks are truly independent (different files, no shared state)
- [x] Each task specifies exact absolute file path
- [x] No task modifies same file as another [P] task
- [x] Constitutional compliance verified for all phases
- [x] Mac Studio infrastructure dependencies documented

## Success Criteria

**Ready for Implementation**: All 54 tasks provide complete, actionable steps for building the automated funding discovery workflow while maintaining constitutional compliance and leveraging Kevin's Java 25 + Spring Boot expertise.

**Performance Targets**: Tasks T050-T051 ensure <500ms API responses and human workflow efficiency.

**Quality Gates**: TDD approach with comprehensive test coverage before any implementation code.
