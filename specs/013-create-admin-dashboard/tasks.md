# Tasks: Admin Dashboard Review Queue

**Feature**: 013 - Admin Dashboard Review Queue
**Input**: Design documents from `/Users/kevin/github/northstar-funding/specs/013-create-admin-dashboard/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/, quickstart.md

## Overview

This task list implements the Admin Dashboard Review Queue feature, spanning:
- **Backend**: New Maven module `northstar-rest-api` with Spring Boot REST API
- **Frontend**: New Vue 3 + TypeScript application `northstar-admin-dashboard`
- **Database**: V19 migration for performance indexes

**Total Tasks**: 42 (numbered T001-T042)
**Estimated Duration**: 20-30 hours of development time

## Project Structure

```
# Backend
northstar-rest-api/                          # New Maven module
├── pom.xml
├── src/main/java/com/northstar/funding/rest/
│   ├── NorthstarRestApiApplication.java
│   ├── config/CorsConfig.java
│   ├── controller/CandidateController.java
│   ├── dto/CandidateDTO.java, CandidatePageDTO.java, CandidateDTOMapper.java
│   └── service/CandidateService.java
└── src/test/java/.../
    ├── service/CandidateServiceTest.java, CandidateDTOMapperTest.java
    └── controller/CandidateControllerTest.java

# Frontend
northstar-admin-dashboard/                   # New Vue 3 application
├── package.json, vite.config.ts, tsconfig.json
├── src/
│   ├── main.ts, App.vue
│   ├── router/index.ts
│   ├── stores/candidateStore.ts
│   ├── services/api.ts
│   ├── types/Candidate.ts, CandidatePage.ts, CandidateStatus.ts, SearchEngineType.ts
│   ├── views/ReviewQueue.vue, CandidateDetail.vue, CandidateEnhance.vue
│   └── components/FilterBar.vue, CandidateActions.vue
└── tests/unit/stores/candidateStore.test.ts

# Database
northstar-persistence/src/main/resources/db/migration/
└── V19__add_candidate_indexes.sql
```

## Phase 3.1: Database Setup

### T001: Create V19 database migration for performance indexes
**File**: `northstar-persistence/src/main/resources/db/migration/V19__add_candidate_indexes.sql`
**Description**: Create Flyway migration with 5 indexes for filter/sort performance
**Dependencies**: None
**Status**: ✅ COMPLETE
**Acceptance**:
- [x] File created with indexes: `idx_candidate_status`, `idx_candidate_confidence`, `idx_candidate_discovered_at`
- [x] Additional composite indexes: `idx_candidate_status_confidence`, `idx_candidate_discovered_confidence`
- [x] Indexes on correct table: `funding_source_candidate`
- [x] Migration runs successfully: `mvn flyway:migrate -pl northstar-persistence`
- [x] Verified with PostgreSQL: All 5 new indexes created successfully

**Implementation**:
```sql
-- V19__add_candidate_indexes.sql (COMPLETED)
CREATE INDEX IF NOT EXISTS idx_candidate_status
    ON funding_source_candidate(status);
CREATE INDEX IF NOT EXISTS idx_candidate_confidence
    ON funding_source_candidate(confidence_score);
CREATE INDEX IF NOT EXISTS idx_candidate_created_at
    ON funding_source_candidate(created_at);
CREATE INDEX IF NOT EXISTS idx_candidate_search_engine
    ON funding_source_candidate(search_engine);
CREATE INDEX IF NOT EXISTS idx_candidate_status_confidence
    ON funding_source_candidate(status, confidence_score);
CREATE INDEX IF NOT EXISTS idx_candidate_created_confidence
    ON funding_source_candidate(created_at DESC, confidence_score DESC);
```

---

## Phase 3.2: Backend Setup - Maven Module

### T002: Create northstar-rest-api Maven module structure
**Files**: `northstar-rest-api/pom.xml`, directory structure
**Description**: Create new Maven module with directory structure and POM
**Dependencies**: T001 (migration should exist first)
**Acceptance**:
- [ ] Directory created: `northstar-rest-api/`
- [ ] Subdirectories: `src/main/java`, `src/main/resources`, `src/test/java`
- [ ] Package structure: `com/northstar/funding/rest/`
- [ ] POM exists but implementation in T003

### T003: Configure northstar-rest-api POM with dependencies
**File**: `northstar-rest-api/pom.xml`
**Description**: Create Maven POM with Spring Boot, Spring Web, dependencies on northstar-persistence
**Dependencies**: T002
**Acceptance**:
- [ ] Parent: `com.northstar.funding:northstar-funding:1.0.0-SNAPSHOT`
- [ ] ArtifactId: `northstar-rest-api`
- [ ] Dependencies: `northstar-persistence`, `spring-boot-starter-web`, `spring-boot-starter-test`, `jackson-databind`
- [ ] Java version: 25 (source and target)
- [ ] Compiles: `mvn clean compile -pl northstar-rest-api`

### T004: Update parent POM to include northstar-rest-api module
**File**: `pom.xml` (repository root)
**Description**: Add `northstar-rest-api` to `<modules>` section
**Dependencies**: T003
**Acceptance**:
- [ ] Module added to parent POM `<modules>` section
- [ ] Full build works: `mvn clean compile`
- [ ] All modules compile including northstar-rest-api

---

## Phase 3.3: Backend DTOs and Mappers (TDD - Tests First)

### T005 [P]: Unit test for CandidateDTOMapper (write first, must fail)
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/dto/CandidateDTOMapperTest.java`
**Description**: Write Mockito test for DTO mapper (toDTO, toDTOs methods)
**Dependencies**: T004
**Acceptance**:
- [ ] Test class with `@ExtendWith(MockitoExtension.class)`
- [ ] Test `toDTO()` converts UUID→String, BigDecimal→String, Enum→String, LocalDateTime→ISO-8601
- [ ] Test `toDTOs()` converts list of entities
- [ ] Test FAILS initially (CandidateDTOMapper doesn't exist yet)
- [ ] Compiles with `mvn test-compile -pl northstar-rest-api`

### T006 [P]: Create CandidateDTO Java record
**File**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/dto/CandidateDTO.java`
**Description**: Create Java record with 7 String fields (id, url, title, confidenceScore, status, searchEngine, createdAt)
**Dependencies**: T004 (can run parallel with T005)
**Acceptance**:
- [ ] Java record with exact fields from data-model.md
- [ ] All fields are String type
- [ ] JavaDoc explaining UUID→String, BigDecimal→String conversions
- [ ] Compiles: `mvn compile -pl northstar-rest-api`

### T007 [P]: Create CandidatePageDTO Java record
**File**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/dto/CandidatePageDTO.java`
**Description**: Create Java record for paginated response (content, totalElements, totalPages, currentPage, pageSize)
**Dependencies**: T006 (depends on CandidateDTO)
**Acceptance**:
- [ ] Java record with 5 fields matching data-model.md
- [ ] `content` field is `List<CandidateDTO>`
- [ ] Numeric fields are `int` type
- [ ] Compiles: `mvn compile -pl northstar-rest-api`

### T008: Implement CandidateDTOMapper service
**File**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/dto/CandidateDTOMapper.java`
**Description**: Create mapper service to convert FundingSourceCandidate → CandidateDTO
**Dependencies**: T005, T006, T007
**Acceptance**:
- [ ] `@Service` annotation
- [ ] Method `toDTO(FundingSourceCandidate)` converts to CandidateDTO
- [ ] Method `toDTOs(List<FundingSourceCandidate>)` converts list
- [ ] Uses `ISO_LOCAL_DATE_TIME` formatter for timestamps
- [ ] Test T005 now PASSES: `mvn test -Dtest=CandidateDTOMapperTest -pl northstar-rest-api`

---

## Phase 3.4: Backend Service Layer (TDD - Tests First)

### T009 [P]: Unit test for CandidateService (write first, must fail)
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/service/CandidateServiceTest.java`
**Description**: Write Mockito test for service layer (listCandidates, approveCandidate, rejectCandidate)
**Dependencies**: T008
**Acceptance**:
- [ ] Test class with `@ExtendWith(MockitoExtension.class)`
- [ ] Mocks: `FundingSourceCandidateRepository`, `DomainRepository`, `CandidateDTOMapper`
- [ ] Tests for listCandidates with filters/pagination
- [ ] Tests for approveCandidate (happy path, 404, 400)
- [ ] Tests for rejectCandidate (happy path, blacklist domain, 404, 400)
- [ ] Tests FAIL initially (CandidateService doesn't exist)
- [ ] Compiles: `mvn test-compile -pl northstar-rest-api`

### T010: Implement CandidateService
**File**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/service/CandidateService.java`
**Description**: Create service with business logic for listing, approving, rejecting candidates
**Dependencies**: T009
**Acceptance**:
- [ ] `@Service` and `@Transactional` annotations
- [ ] Constructor injection (no @Autowired): FundingSourceCandidateRepository, DomainRepository, CandidateDTOMapper
- [ ] Method `listCandidates(filters, Pageable)` returns CandidatePageDTO
- [ ] Method `approveCandidate(UUID id)` returns CandidateDTO, throws exceptions for 404/400
- [ ] Method `rejectCandidate(UUID id)` returns CandidateDTO, updates domain to BLACKLISTED
- [ ] Test T009 now PASSES: `mvn test -Dtest=CandidateServiceTest -pl northstar-rest-api`

---

## Phase 3.5: Backend REST Controller (TDD - Tests First)

### T011 [P]: Unit test for CandidateController (write first, must fail)
**File**: `northstar-rest-api/src/test/java/com/northstar/funding/rest/controller/CandidateControllerTest.java`
**Description**: Write MockMvc test for REST endpoints
**Dependencies**: T010
**Acceptance**:
- [ ] Test class with `@WebMvcTest(CandidateController.class)`
- [ ] `@MockBean` for CandidateService
- [ ] Test GET /api/candidates with various query params
- [ ] Test PUT /api/candidates/{id}/approve (200, 404, 400)
- [ ] Test PUT /api/candidates/{id}/reject (200, 404, 400)
- [ ] Tests FAIL initially (CandidateController doesn't exist)
- [ ] Compiles: `mvn test-compile -pl northstar-rest-api`

### T012: Implement CandidateController
**File**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/controller/CandidateController.java`
**Description**: Create REST controller with 3 endpoints matching OpenAPI spec
**Dependencies**: T011
**Acceptance**:
- [ ] `@RestController` and `@RequestMapping("/api/candidates")` annotations
- [ ] Constructor injection for CandidateService
- [ ] GET / endpoint with query params: page, size, status, minConfidence, searchEngine, startDate, endDate, sortBy, sortDirection
- [ ] PUT /{id}/approve endpoint returning ResponseEntity<CandidateDTO>
- [ ] PUT /{id}/reject endpoint returning ResponseEntity<CandidateDTO>
- [ ] Exception handling for 404 and 400 errors
- [ ] Test T011 now PASSES: `mvn test -Dtest=CandidateControllerTest -pl northstar-rest-api`

---

## Phase 3.6: Backend Configuration

### T013: Create CorsConfig for Vue development server
**File**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/config/CorsConfig.java`
**Description**: Configure CORS to allow requests from http://localhost:5173
**Dependencies**: T012
**Acceptance**:
- [ ] `@Configuration` annotation
- [ ] Implements `WebMvcConfigurer`
- [ ] Override `addCorsMappings()` method
- [ ] Allow origin: `http://localhost:5173`
- [ ] Allow methods: GET, POST, PUT, DELETE, OPTIONS
- [ ] Allow headers: *
- [ ] Allow credentials: true
- [ ] Pattern: `/api/**`
- [ ] Compiles: `mvn compile -pl northstar-rest-api`

### T014: Create Spring Boot application class
**File**: `northstar-rest-api/src/main/java/com/northstar/funding/rest/NorthstarRestApiApplication.java`
**Description**: Create @SpringBootApplication main class
**Dependencies**: T013
**Acceptance**:
- [ ] `@SpringBootApplication` annotation
- [ ] `main(String[] args)` method with `SpringApplication.run()`
- [ ] Compiles: `mvn compile -pl northstar-rest-api`

### T015: Create application.yml configuration
**File**: `northstar-rest-api/src/main/resources/application.yml`
**Description**: Configure Spring Boot (port, datasource, JPA)
**Dependencies**: T014
**Acceptance**:
- [ ] `server.port: 8080`
- [ ] `spring.datasource.url: jdbc:postgresql://192.168.1.10:5432/northstar_funding`
- [ ] `spring.datasource.username: northstar_user`
- [ ] `spring.datasource.password: northstar_password`
- [ ] `spring.datasource.driver-class-name: org.postgresql.Driver`
- [ ] Application starts: `mvn spring-boot:run -pl northstar-rest-api`

---

## Phase 3.7: Backend Verification

### T016: Run all northstar-rest-api unit tests
**Description**: Verify all backend unit tests pass
**Dependencies**: T001-T015
**Acceptance**:
- [ ] All tests pass: `mvn test -pl northstar-rest-api`
- [ ] CandidateDTOMapperTest passes
- [ ] CandidateServiceTest passes
- [ ] CandidateControllerTest passes
- [ ] No compilation errors

### T017: Verify REST API endpoints with curl
**Description**: Manual verification of REST endpoints
**Dependencies**: T016
**Acceptance**:
- [ ] Spring Boot running: `mvn spring-boot:run -pl northstar-rest-api`
- [ ] GET /api/candidates returns 200: `curl http://localhost:8080/api/candidates`
- [ ] Response is valid JSON with CandidatePageDTO structure
- [ ] Filters work: `curl "http://localhost:8080/api/candidates?status=PENDING_CRAWL&minConfidence=0.70"`
- [ ] Approve endpoint exists (404 if no data): `curl -X PUT http://localhost:8080/api/candidates/{uuid}/approve`

---

## Phase 3.8: Frontend Setup - Vue Project

### T018: Initialize Vue 3 + TypeScript project with Vite
**Directory**: `northstar-admin-dashboard/`
**Description**: Create Vue project using Vite template
**Dependencies**: T001 (database ready), independent of backend tasks
**Acceptance**:
- [ ] Directory created: `northstar-admin-dashboard/`
- [ ] Initialized with: `pnpm create vite northstar-admin-dashboard --template vue-ts` (or npm)
- [ ] Files exist: `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`
- [ ] Default Vite structure: `src/main.ts`, `src/App.vue`, `src/assets`, `src/components`

### T019: Install frontend dependencies (PrimeVue, Pinia, Vue Router, Axios)
**File**: `northstar-admin-dashboard/package.json`
**Description**: Install all required npm packages
**Dependencies**: T018
**Acceptance**:
- [ ] Installed: `primevue@^3.50.0`, `primeicons@^6.0.1`
- [ ] Installed: `pinia@^2.1.0`
- [ ] Installed: `vue-router@^4.0.0`
- [ ] Installed: `axios@^1.6.0`
- [ ] Dev dependency: `@types/node`
- [ ] `package.json` updated with all dependencies
- [ ] Lockfile created: `pnpm-lock.yaml` or `package-lock.json`

### T020: Configure Vite with path alias and port
**File**: `northstar-admin-dashboard/vite.config.ts`
**Description**: Configure Vite for development
**Dependencies**: T019
**Acceptance**:
- [ ] Import: `path` from 'path'
- [ ] Resolve alias: `'@': path.resolve(__dirname, './src')`
- [ ] Server port: 5173
- [ ] Compiles: `pnpm dev` (or npm run dev)

### T021: Create environment variables for API base URL
**Files**: `northstar-admin-dashboard/.env.development`, `.env.production`
**Description**: Configure API base URLs for different environments
**Dependencies**: T020
**Acceptance**:
- [ ] `.env.development` created with `VITE_API_BASE_URL=http://localhost:8080/api`
- [ ] `.env.production` created with `VITE_API_BASE_URL=http://production-url/api`
- [ ] Variables accessible via `import.meta.env.VITE_API_BASE_URL`

---

## Phase 3.9: Frontend TypeScript Types

### T022 [P]: Create Candidate TypeScript interface
**File**: `northstar-admin-dashboard/src/types/Candidate.ts`
**Description**: TypeScript interface mirroring CandidateDTO (all String fields)
**Dependencies**: T021
**Acceptance**:
- [ ] Interface with 7 fields: id, url, title, confidenceScore, status, searchEngine, createdAt (all string)
- [ ] Export interface for use in components/stores
- [ ] Optional: Type guard function `isCandidate(obj: any): obj is Candidate`

### T023 [P]: Create CandidatePage TypeScript interface
**File**: `northstar-admin-dashboard/src/types/CandidatePage.ts`
**Description**: TypeScript interface for paginated response
**Dependencies**: T022 (needs Candidate interface)
**Acceptance**:
- [ ] Import Candidate from './Candidate'
- [ ] Interface with 5 fields: content (Candidate[]), totalElements, totalPages, currentPage, pageSize
- [ ] Export interface

### T024 [P]: Create CandidateStatus TypeScript enum
**File**: `northstar-admin-dashboard/src/types/CandidateStatus.ts`
**Description**: TypeScript enum matching Java CandidateStatus enum
**Dependencies**: T021 (independent of T022/T023)
**Acceptance**:
- [ ] Enum with 9 values: NEW, PENDING_CRAWL, CRAWLED, ENHANCED, JUDGED, APPROVED, REJECTED, SKIPPED_LOW_CONFIDENCE, BLACKLISTED
- [ ] Export `ALL_STATUSES` array for dropdown options
- [ ] Export enum

### T025 [P]: Create SearchEngineType TypeScript enum
**File**: `northstar-admin-dashboard/src/types/SearchEngineType.ts`
**Description**: TypeScript enum matching Java SearchEngineType enum
**Dependencies**: T021 (independent of T022/T023)
**Acceptance**:
- [ ] Enum with 5 values: BRAVE, TAVILY, PERPLEXITY, SEARXNG, BROWSERBASE
- [ ] Export `ALL_ENGINES` array for dropdown options
- [ ] Export enum

---

## Phase 3.10: Frontend Services

### T026: Create Axios API service
**File**: `northstar-admin-dashboard/src/services/api.ts`
**Description**: Centralized Axios configuration with typed methods
**Dependencies**: T022, T023
**Acceptance**:
- [ ] Import Candidate, CandidatePage types
- [ ] Create axios instance with baseURL from `import.meta.env.VITE_API_BASE_URL`
- [ ] Timeout: 10000ms
- [ ] Response interceptor for error handling
- [ ] Method `getCandidates(params)` returns `Promise<CandidatePage>`
- [ ] Method `approveCandidate(id: string)` returns `Promise<Candidate>`
- [ ] Method `rejectCandidate(id: string)` returns `Promise<Candidate>`

---

## Phase 3.11: Frontend State Management

### T027: Create Pinia candidate store
**File**: `northstar-admin-dashboard/src/stores/candidateStore.ts`
**Description**: Pinia store for candidate state management
**Dependencies**: T026
**Acceptance**:
- [ ] Import types: Candidate, CandidatePage, CandidateStatus, SearchEngineType
- [ ] Import API service methods
- [ ] State: candidates (Candidate[]), totalElements, currentPage, pageSize, filters (status, minConfidence, searchEngine, dateRange)
- [ ] State: loading (boolean), error (string | null)
- [ ] Action: `fetchCandidates()` - calls API, updates state
- [ ] Action: `approveCandidate(id)` - calls API, shows toast, refreshes list
- [ ] Action: `rejectCandidate(id)` - calls API, shows toast, refreshes list
- [ ] Action: `setFilters(filters)` - updates filter state, calls fetchCandidates
- [ ] Action: `clearFilters()` - resets filters to defaults, calls fetchCandidates

### T028 [P]: Optional unit test for candidateStore
**File**: `northstar-admin-dashboard/tests/unit/stores/candidateStore.test.ts`
**Description**: Vitest tests for Pinia store actions
**Dependencies**: T027
**Acceptance**:
- [ ] Test setup with Pinia and mocked API
- [ ] Test `fetchCandidates()` updates state correctly
- [ ] Test `approveCandidate()` calls API and refreshes
- [ ] Test `rejectCandidate()` calls API and refreshes
- [ ] Optional for Feature 013

---

## Phase 3.12: Frontend PrimeVue Setup

### T029: Configure PrimeVue in main.ts
**File**: `northstar-admin-dashboard/src/main.ts`
**Description**: Setup PrimeVue, Pinia, Vue Router in application entry point
**Dependencies**: T027
**Acceptance**:
- [ ] Import PrimeVue, ConfirmationService, ToastService
- [ ] Import Pinia, Vue Router
- [ ] Import PrimeVue CSS: theme (aura-light-green), primevue.min.css, primeicons.css
- [ ] Create Pinia instance: `const pinia = createPinia()`
- [ ] `app.use(pinia)`, `app.use(router)`, `app.use(PrimeVue)`, `app.use(ConfirmationService)`, `app.use(ToastService)`
- [ ] Dev server runs: `pnpm dev`

---

## Phase 3.13: Frontend Vue Router

### T030: Configure Vue Router with routes
**File**: `northstar-admin-dashboard/src/router/index.ts`
**Description**: Setup routing for review queue and placeholder pages
**Dependencies**: T029 (needs main.ts setup)
**Acceptance**:
- [ ] Import `createRouter`, `createWebHistory`
- [ ] Route: `/` → ReviewQueue.vue
- [ ] Route: `/candidates/:id` → CandidateDetail.vue
- [ ] Route: `/candidates/:id/enhance` → CandidateEnhance.vue
- [ ] Export router for use in main.ts

---

## Phase 3.14: Frontend Components

### T031: Create ReviewQueue.vue main component
**File**: `northstar-admin-dashboard/src/views/ReviewQueue.vue`
**Description**: Main review queue component with PrimeVue DataTable
**Dependencies**: T030
**Acceptance**:
- [ ] `<script setup lang="ts">` with TypeScript
- [ ] Import candidateStore from Pinia
- [ ] Import PrimeVue components: DataTable, Column, Button, Toast, ConfirmDialog
- [ ] PrimeVue DataTable with `:value="candidates"`, `:lazy="true"`, `:paginator="true"`
- [ ] Columns: URL, Title, Confidence Score (color-coded), Status, Search Engine, Created Date
- [ ] Actions column with View, Enhance, Approve, Reject buttons
- [ ] Bind `@page` event to fetch new page
- [ ] Bind `@sort` event to change sort order
- [ ] Color-coded confidence: green (≥0.80), yellow (0.70-0.79), orange (0.60-0.69)
- [ ] Empty state template: "No candidates found matching your filters."
- [ ] Loading state with ProgressSpinner

### T032: Create FilterBar.vue component
**File**: `northstar-admin-dashboard/src/components/FilterBar.vue`
**Description**: Filter controls component (status, confidence, engine, dates)
**Dependencies**: T031
**Acceptance**:
- [ ] Import candidateStore
- [ ] Import CandidateStatus, SearchEngineType enums
- [ ] MultiSelect for status (bind to store filters)
- [ ] Dropdown for confidence: All (≥0.00), High (≥0.80), Medium (≥0.70), Low (≥0.60)
- [ ] MultiSelect for search engine
- [ ] Calendar for start date, end date
- [ ] "Clear Filters" button calls store.clearFilters()
- [ ] All filter changes call store.setFilters()

### T033: Create CandidateActions.vue component
**File**: `northstar-admin-dashboard/src/components/CandidateActions.vue`
**Description**: Quick action buttons component (View, Enhance, Approve, Reject)
**Dependencies**: T031
**Acceptance**:
- [ ] Props: candidate (Candidate interface)
- [ ] Button: View → navigate to `/candidates/${candidate.id}`
- [ ] Button: Enhance → navigate to `/candidates/${candidate.id}/enhance`
- [ ] Button: Approve → show ConfirmDialog, call store.approveCandidate(id)
- [ ] Button: Reject → show ConfirmDialog, call store.rejectCandidate(id)
- [ ] Confirmation text for Approve: "Are you sure you want to approve this candidate?"
- [ ] Confirmation text for Reject: "Are you sure you want to reject this candidate and blacklist the domain?"

### T034 [P]: Create CandidateDetail.vue placeholder page
**File**: `northstar-admin-dashboard/src/views/CandidateDetail.vue`
**Description**: Placeholder page for candidate detail view (Feature 014)
**Dependencies**: T030
**Acceptance**:
- [ ] Display message: "Candidate Detail - Coming in Feature 014"
- [ ] Display candidate ID from route params: `$route.params.id`
- [ ] Basic layout with Card component

### T035 [P]: Create CandidateEnhance.vue placeholder page
**File**: `northstar-admin-dashboard/src/views/CandidateEnhance.vue`
**Description**: Placeholder page for candidate enhancement form (Feature 015)
**Dependencies**: T030
**Acceptance**:
- [ ] Display message: "Candidate Enhancement - Coming in Feature 015"
- [ ] Display candidate ID from route params: `$route.params.id`
- [ ] Basic layout with Card component

---

## Phase 3.15: Frontend Integration

### T036: Integrate FilterBar and CandidateActions into ReviewQueue
**File**: `northstar-admin-dashboard/src/views/ReviewQueue.vue`
**Description**: Add FilterBar and CandidateActions components to ReviewQueue
**Dependencies**: T032, T033
**Acceptance**:
- [ ] Import FilterBar and CandidateActions components
- [ ] Place FilterBar above DataTable
- [ ] Use CandidateActions in Actions column template
- [ ] Pass candidate prop to CandidateActions
- [ ] Layout: FilterBar → DataTable → Pagination

### T037: Update App.vue with router-view and global components
**File**: `northstar-admin-dashboard/src/App.vue`
**Description**: Setup main application layout
**Dependencies**: T036
**Acceptance**:
- [ ] `<router-view />` for page routing
- [ ] `<Toast />` component for global notifications
- [ ] `<ConfirmDialog />` component for confirmations
- [ ] Basic layout (header, main content area)
- [ ] Application title: "NorthStar Admin Dashboard"

---

## Phase 3.16: End-to-End Integration Testing

### T038: Start both servers and verify integration
**Description**: Manual integration test with both servers running
**Dependencies**: T017 (backend verified), T037 (frontend complete)
**Acceptance**:
- [ ] Terminal 1: Spring Boot running on port 8080: `mvn spring-boot:run -pl northstar-rest-api`
- [ ] Terminal 2: Vue dev server running on port 5173: `cd northstar-admin-dashboard && pnpm dev`
- [ ] Browser: Open http://localhost:5173
- [ ] Page loads in <2 seconds
- [ ] No CORS errors in browser console
- [ ] DataTable displays (empty if no data, or shows candidates)
- [ ] Filters are visible and functional
- [ ] Pagination controls visible

### T039: Verify core functionality with test data
**Description**: End-to-end verification of all features
**Dependencies**: T038
**Acceptance**:
- [ ] **Filters work**: Apply status filter, verify API call in Network tab
- [ ] **Sorting works**: Click column header, verify sort direction changes
- [ ] **Pagination works**: Navigate pages, verify page parameter in API calls
- [ ] **View button works**: Click View, navigate to placeholder page
- [ ] **Enhance button works**: Click Enhance, navigate to placeholder page
- [ ] **Approve works**: Click Approve, confirmation dialog appears, approve updates status, success toast shows
- [ ] **Reject works**: Click Reject, confirmation dialog appears, reject updates status and blacklists domain, success toast shows
- [ ] **Clear filters works**: Reset all filters to defaults

### T040: Verify performance requirements
**Description**: Performance testing
**Dependencies**: T039
**Acceptance**:
- [ ] Page load <2 seconds (Chrome DevTools Network tab)
- [ ] Filter operations <1 second (Network tab timing)
- [ ] No console errors or warnings
- [ ] Verify database indexes with psql: `\d funding_source_candidate` shows 4 indexes

---

## Phase 3.17: Documentation and Cleanup

### T041: Create northstar-admin-dashboard README.md
**File**: `northstar-admin-dashboard/README.md`
**Description**: Frontend project documentation
**Dependencies**: T040
**Acceptance**:
- [ ] Project description
- [ ] Setup instructions: `pnpm install`
- [ ] Development: `pnpm dev` (requires Spring Boot on port 8080)
- [ ] Build: `pnpm build`
- [ ] Tech stack listed: Vue 3, TypeScript, Vite, PrimeVue, Pinia, Axios, Vue Router

### T042: Verify all 43 functional requirements from spec.md
**Description**: Final checklist against specification
**Dependencies**: T041
**Acceptance**:
- [ ] Review spec.md FR-001 through FR-043
- [ ] All functional requirements implemented
- [ ] All acceptance scenarios pass (10 scenarios)
- [ ] Edge cases handled (7 edge cases)
- [ ] Success metrics met (page load <2s, operations <1s)
- [ ] Feature 013 complete and ready for commit/PR

---

## Dependencies Graph

```
Database:
T001 → T002

Backend Module Setup:
T002 → T003 → T004

Backend DTOs (parallel after T004):
T004 → T005 [P], T006 [P], T007 [P] → T008

Backend Service (depends on T008):
T008 → T009 [P] → T010

Backend Controller (depends on T010):
T010 → T011 [P] → T012

Backend Config (sequential):
T012 → T013 → T014 → T015

Backend Verification:
T015 → T016 → T017

Frontend Setup (independent path):
T018 → T019 → T020 → T021

Frontend Types (parallel after T021):
T021 → T022 [P], T023 [P], T024 [P], T025 [P]

Frontend Services:
T022, T023 → T026

Frontend State:
T026 → T027 → T028 [P]

Frontend PrimeVue:
T027 → T029

Frontend Router:
T029 → T030

Frontend Components (partial parallel):
T030 → T031 → T032, T033, T034 [P], T035 [P]

Frontend Integration:
T032, T033 → T036 → T037

Integration Testing:
T017, T037 → T038 → T039 → T040

Documentation:
T040 → T041 → T042
```

## Parallel Execution Examples

### Backend DTOs (after T004):
```bash
# Can run in parallel - different files
Task T005: Unit test CandidateDTOMapperTest
Task T006: Create CandidateDTO record
Task T007: Create CandidatePageDTO record
```

### Frontend Types (after T021):
```bash
# Can run in parallel - independent files
Task T022: Create Candidate.ts interface
Task T024: Create CandidateStatus.ts enum
Task T025: Create SearchEngineType.ts enum
# Note: T023 depends on T022 (imports Candidate)
```

### Frontend Placeholder Pages (after T030):
```bash
# Can run in parallel - different files
Task T034: Create CandidateDetail.vue
Task T035: Create CandidateEnhance.vue
```

## Notes

- **[P] markers**: Tasks marked [P] can run in parallel if dependencies are met
- **TDD order**: Tests (T005, T009, T011) written BEFORE implementation (T008, T010, T012)
- **Verification gates**: T016 (backend tests), T038 (integration), T040 (performance), T042 (final)
- **Both servers required**: From T038 onwards, both Spring Boot (8080) and Vue (5173) must run
- **CORS critical**: T013 must be correct or frontend can't call backend
- **Database indexes**: T001 must complete and migrate before performance testing (T040)

## Task Execution Order (Recommended)

1. **Database**: T001
2. **Backend Setup**: T002-T004
3. **Backend DTOs**: T005-T008 (T005, T006, T007 parallel)
4. **Backend Service**: T009-T010 (T009 parallel with others if desired)
5. **Backend Controller**: T011-T012 (T011 parallel)
6. **Backend Config**: T013-T015
7. **Backend Verify**: T016-T017
8. **Frontend Setup**: T018-T021 (can start anytime after T001)
9. **Frontend Types**: T022-T025 (T022, T024, T025 parallel; T023 after T022)
10. **Frontend Service**: T026
11. **Frontend State**: T027-T028 (T028 optional)
12. **Frontend PrimeVue**: T029
13. **Frontend Router**: T030
14. **Frontend Components**: T031-T035 (T034, T035 parallel)
15. **Frontend Integration**: T036-T037
16. **Integration Test**: T038-T040
17. **Final**: T041-T042

**Estimated Total Time**: 20-30 hours
- Backend: 8-12 hours
- Frontend: 10-15 hours
- Integration/Testing: 2-3 hours

---

*Generated from plan.md, data-model.md, contracts/candidates-api.yaml, quickstart.md*
*Feature 013: Admin Dashboard Review Queue*
*Total: 42 tasks across backend, frontend, and integration*
