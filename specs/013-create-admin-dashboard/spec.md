# Feature Specification: Admin Dashboard Review Queue

**Feature Number**: 013
**Feature Name**: Admin Dashboard Review Queue
**Created**: 2025-11-16
**Status**: Draft

## Overview

Create the first component of the Admin Dashboard - a Review Queue where administrators (Kevin and Huw) can view, filter, sort, and take quick actions on funding source candidates discovered by automated search. This establishes the foundational Vue 3 + PrimeVue frontend architecture and REST API layer for the human-AI hybrid workflow.

## Background

The NorthStar Funding Discovery system runs automated nightly searches to discover funding opportunities. The SearchResultProcessor creates FundingSourceCandidate records with confidence scores (0.00-1.00) and assigns status values (PENDING_CRAWL, CRAWLED, etc.). Administrators need a web UI to review these candidates, enhance them with additional information, and approve/reject them for inclusion in the client-facing database.

This feature establishes:
- Vue 3 + PrimeVue frontend architecture
- REST API layer with DTO pattern
- Data flow: PostgreSQL ’ Domain Entity ’ DTO ’ JSON ’ Vue Component
- Foundation for Features 014-018 (enhancement, contact intelligence, approval, statistics, domain management)

## Goals

**Primary Goal**: Enable administrators to view and filter funding source candidates in a responsive web interface.

**Secondary Goals**:
1. Establish Vue 3 + PrimeVue frontend architecture with TypeScript
2. Create REST API layer with DTO pattern as API contract boundary
3. Demonstrate data flow from PostgreSQL through all layers to PrimeVue DataTable
4. Provide quick actions (View, Enhance, Approve, Reject) with navigation placeholders
5. Create reusable patterns for Features 014-018

**Success Criteria**:
- Administrators can view paginated list of candidates (20 per page)
- Filter by status, confidence range, search engine, date range works correctly
- Sort by any column works correctly
- Color-coded confidence scores display correctly (green e0.80, yellow 0.70-0.79, orange 0.60-0.69)
- Quick actions trigger appropriate responses (navigation for View/Enhance, confirmation dialogs for Approve/Reject)
- Page loads in under 2 seconds with 1000+ candidates in database
- All responses return within 1 second

## Functional Requirements

### FR-001: Review Queue Display
**WHAT**: Display a paginated table of funding source candidates with key information.

**WHY**: Administrators need to see the results of automated searches to decide which candidates require human review.

**Requirements**:
- Display columns: URL, Title, Confidence Score, Status, Search Engine, Created Date
- Show 20 candidates per page
- Display total candidate count
- Use PrimeVue DataTable component
- Handle empty state (no candidates found)

### FR-002: Pagination Controls
**WHAT**: Provide pagination controls to navigate through candidate pages.

**WHY**: With hundreds or thousands of candidates, administrators need efficient navigation.

**Requirements**:
- First, Previous, Next, Last buttons
- Current page indicator (e.g., "Page 3 of 15")
- Page size selector (10, 20, 50, 100 per page)
- Disable First/Previous on first page
- Disable Next/Last on last page

### FR-003: Filter by Status
**WHAT**: Filter candidates by their current status value.

**WHY**: Administrators need to focus on candidates in specific workflow stages.

**Requirements**:
- Multi-select dropdown with all CandidateStatus enum values
- Options: NEW, PENDING_CRAWL, CRAWLED, ENHANCED, JUDGED, APPROVED, REJECTED, SKIPPED_LOW_CONFIDENCE, BLACKLISTED
- Default: Show all statuses
- Update table immediately when filter changes
- Display count of filtered results

### FR-004: Filter by Confidence Range
**WHAT**: Filter candidates by minimum confidence score threshold.

**WHY**: Administrators often want to review only high-confidence candidates first.

**Requirements**:
- Dropdown options: All (e0.00), High (e0.80), Medium (e0.70), Low (e0.60)
- Default: All
- Update table immediately when filter changes
- Display count of filtered results

### FR-005: Filter by Search Engine
**WHAT**: Filter candidates by which search engine discovered them.

**WHY**: Administrators may want to compare quality across different search engines.

**Requirements**:
- Multi-select dropdown with all SearchEngineType enum values
- Options: BRAVE, TAVILY, PERPLEXITY, SEARXNG, BROWSERBASE
- Default: Show all engines
- Update table immediately when filter changes
- Display count of filtered results

### FR-006: Filter by Date Range
**WHAT**: Filter candidates by creation date range.

**WHY**: Administrators need to review recent discoveries or investigate specific time periods.

**Requirements**:
- Start date and end date inputs (PrimeVue Calendar component)
- Default: Last 7 days
- Quick select options: Today, Last 7 days, Last 30 days, All time
- Update table immediately when date range changes
- Display count of filtered results

### FR-007: Sort by Column
**WHAT**: Sort candidates by clicking column headers.

**WHY**: Administrators need to order candidates by different criteria (newest first, highest confidence, etc.).

**Requirements**:
- Clickable column headers
- Sort direction indicator (ascending/descending arrow icon)
- Support sorting by: URL, Title, Confidence Score, Status, Search Engine, Created Date
- Default sort: Created Date descending (newest first)
- Update table immediately when sort changes

### FR-008: Color-Coded Confidence Scores
**WHAT**: Display confidence scores with color coding based on value.

**WHY**: Visual cues help administrators quickly identify high-quality candidates.

**Requirements**:
- Green text: Confidence e 0.80
- Yellow text: Confidence 0.70-0.79
- Orange text: Confidence 0.60-0.69
- Format: Two decimal places (e.g., "0.85")

### FR-009: Quick Action - View Details
**WHAT**: Provide a "View" button that navigates to candidate detail page.

**WHY**: Administrators need to see full candidate information before making decisions.

**Requirements**:
- "View" button in Actions column
- Navigate to `/candidates/{id}` route (placeholder page for now)
- Pass candidate ID in route parameter

### FR-010: Quick Action - Enhance Candidate
**WHAT**: Provide an "Enhance" button that navigates to enhancement form.

**WHY**: Administrators need to add missing information to candidates.

**Requirements**:
- "Enhance" button in Actions column
- Navigate to `/candidates/{id}/enhance` route (placeholder page for now)
- Pass candidate ID in route parameter

### FR-011: Quick Action - Approve Candidate
**WHAT**: Provide an "Approve" button that marks candidate as APPROVED status.

**WHY**: Administrators need to approve high-quality candidates for client-facing database.

**Requirements**:
- "Approve" button in Actions column
- Show confirmation dialog: "Are you sure you want to approve this candidate?"
- On confirm: Call PUT `/api/candidates/{id}/approve` endpoint
- Update candidate status to APPROVED
- Refresh table to reflect new status
- Show success toast notification

### FR-012: Quick Action - Reject Candidate
**WHAT**: Provide a "Reject" button that marks candidate as REJECTED and blacklists domain.

**WHY**: Administrators need to reject low-quality candidates and prevent future discoveries from the same domain.

**Requirements**:
- "Reject" button in Actions column
- Show confirmation dialog: "Are you sure you want to reject this candidate and blacklist the domain?"
- On confirm: Call PUT `/api/candidates/{id}/reject` endpoint
- Update candidate status to REJECTED
- Mark domain as BLACKLISTED
- Refresh table to reflect new status
- Show success toast notification

### FR-013: Clear All Filters
**WHAT**: Provide a "Clear Filters" button that resets all filters to default values.

**WHY**: Administrators need a quick way to reset view after applying multiple filters.

**Requirements**:
- "Clear Filters" button above filter controls
- Reset status filter to "All"
- Reset confidence filter to "All (e0.00)"
- Reset search engine filter to "All"
- Reset date range to "Last 7 days"
- Refresh table with unfiltered results

### FR-014: REST API - List Candidates
**WHAT**: Create GET endpoint to retrieve paginated, filtered, sorted candidates.

**WHY**: Vue frontend needs to fetch candidate data from backend.

**Requirements**:
- Endpoint: GET `/api/candidates`
- Query parameters: `page`, `size`, `status`, `minConfidence`, `searchEngine`, `startDate`, `endDate`, `sortBy`, `sortDirection`
- Response: JSON with `content` (array of CandidateDTOs), `totalElements`, `totalPages`, `currentPage`
- DTO fields: `id` (String), `url`, `title`, `confidenceScore` (String), `status` (String), `searchEngine` (String), `createdAt` (ISO-8601 String)

### FR-015: REST API - Approve Candidate
**WHAT**: Create PUT endpoint to approve a candidate.

**WHY**: Administrators need to mark candidates as APPROVED.

**Requirements**:
- Endpoint: PUT `/api/candidates/{id}/approve`
- Update candidate status to APPROVED
- Return updated CandidateDTO
- Return 404 if candidate not found
- Return 400 if candidate already approved

### FR-016: REST API - Reject Candidate
**WHAT**: Create PUT endpoint to reject a candidate and blacklist domain.

**WHY**: Administrators need to mark candidates as REJECTED and prevent future discoveries.

**Requirements**:
- Endpoint: PUT `/api/candidates/{id}/reject`
- Update candidate status to REJECTED
- Update domain status to BLACKLISTED
- Return updated CandidateDTO
- Return 404 if candidate not found
- Return 400 if candidate already rejected

### FR-017: DTO Pattern
**WHAT**: Create CandidateDTO record to represent API contract.

**WHY**: Domain entities should never be exposed directly through REST API.

**Requirements**:
- Location: `northstar-rest-api/src/main/java/com/northstar/funding/rest/dto/CandidateDTO.java`
- Use Java record type
- Fields use primitive types only (UUID’String, BigDecimal’String, Enum’String, LocalDateTime’ISO-8601 String)
- Create CandidateDTOMapper service to convert Domain ” DTO

### FR-018: Vue Project Setup
**WHAT**: Create Vue 3 project with TypeScript, Vite, PrimeVue, Pinia, Axios.

**WHY**: Establish frontend architecture for admin dashboard.

**Requirements**:
- Project location: `northstar-admin-dashboard/` (sibling to Maven modules)
- Build tool: Vite 5.0
- Framework: Vue 3.4+ with TypeScript
- UI Library: PrimeVue 3.50 (FREE/MIT components only)
- State Management: Pinia 2.1
- HTTP Client: Axios 1.6
- Package manager: pnpm (or npm)

### FR-019: TypeScript Interface for Candidate
**WHAT**: Create TypeScript interface mirroring CandidateDTO.

**WHY**: TypeScript interfaces provide type safety in Vue components.

**Requirements**:
- Location: `northstar-admin-dashboard/src/types/Candidate.ts`
- Fields match CandidateDTO exactly (all String types)
- Export interface for use in components and stores

### FR-020: Pinia Store for Candidates
**WHAT**: Create Pinia store to manage candidate state.

**WHY**: Centralized state management for candidate data, filters, pagination.

**Requirements**:
- Location: `northstar-admin-dashboard/src/stores/candidateStore.ts`
- State: `candidates` (array), `totalElements`, `currentPage`, `pageSize`, filter values
- Actions: `fetchCandidates()`, `approveCandidate()`, `rejectCandidate()`, `setFilters()`, `clearFilters()`
- Use Axios to call REST API endpoints

### FR-021: Review Queue Vue Component
**WHAT**: Create ReviewQueue.vue component with PrimeVue DataTable.

**WHY**: Main UI component for viewing and interacting with candidates.

**Requirements**:
- Location: `northstar-admin-dashboard/src/views/ReviewQueue.vue`
- Use PrimeVue DataTable with pagination, sorting, filtering
- Bind to Pinia candidateStore
- Implement all filter controls
- Implement quick action buttons
- Show confirmation dialogs for Approve/Reject
- Show toast notifications for success/error

### FR-022: Vue Router Setup
**WHAT**: Configure Vue Router with routes for review queue and placeholder pages.

**WHY**: Enable navigation between pages.

**Requirements**:
- Location: `northstar-admin-dashboard/src/router/index.ts`
- Routes:
  - `/` ’ ReviewQueue.vue
  - `/candidates/:id` ’ CandidateDetail.vue (placeholder)
  - `/candidates/:id/enhance` ’ CandidateEnhance.vue (placeholder)

### FR-023: Axios API Service
**WHAT**: Create Axios service module for API calls.

**WHY**: Centralize API configuration and error handling.

**Requirements**:
- Location: `northstar-admin-dashboard/src/services/api.ts`
- Base URL: `http://localhost:8080/api`
- Methods: `getCandidates()`, `approveCandidate()`, `rejectCandidate()`
- Error handling with try/catch
- Return typed responses using Candidate interface

### FR-024: PrimeVue Configuration
**WHAT**: Configure PrimeVue with theme and components.

**WHY**: Establish consistent UI styling.

**Requirements**:
- Location: `northstar-admin-dashboard/src/main.ts`
- Theme: Aura Light (or Lara Light)
- Components: DataTable, Column, Paginator, Dropdown, MultiSelect, Calendar, Button, ConfirmDialog, Toast
- Import PrimeVue CSS

### FR-025: Environment Configuration
**WHAT**: Configure environment variables for API base URL.

**WHY**: Support different environments (dev, staging, production).

**Requirements**:
- Location: `northstar-admin-dashboard/.env.development`
- Variable: `VITE_API_BASE_URL=http://localhost:8080/api`
- Use in Axios service

### FR-026: Spring Boot REST Controller
**WHAT**: Create CandidateController with REST endpoints.

**WHY**: Expose candidate operations through HTTP API.

**Requirements**:
- Location: `northstar-rest-api/src/main/java/com/northstar/funding/rest/controller/CandidateController.java`
- Annotations: `@RestController`, `@RequestMapping("/api/candidates")`
- Methods: `listCandidates()`, `approveCandidate()`, `rejectCandidate()`
- Use CandidateService for business logic
- Return DTOs, never domain entities

### FR-027: CandidateService
**WHAT**: Create service layer for candidate operations.

**WHY**: Separate business logic from REST controller.

**Requirements**:
- Location: `northstar-rest-api/src/main/java/com/northstar/funding/rest/service/CandidateService.java`
- Annotations: `@Service`, `@Transactional`
- Methods: `listCandidates()`, `approveCandidate()`, `rejectCandidate()`
- Use FundingSourceCandidateRepository and DomainRepository
- Use CandidateDTOMapper to convert Domain ” DTO

### FR-028: CORS Configuration
**WHAT**: Configure Spring Boot to allow CORS from Vue development server.

**WHY**: Enable API calls from `http://localhost:5173` (Vite dev server).

**Requirements**:
- Location: `northstar-rest-api/src/main/java/com/northstar/funding/rest/config/CorsConfig.java`
- Allow origin: `http://localhost:5173`
- Allow methods: GET, POST, PUT, DELETE, OPTIONS
- Allow headers: Content-Type, Authorization
- Allow credentials: true

### FR-029: Empty State Handling
**WHAT**: Display helpful message when no candidates match filters.

**WHY**: Provide clear feedback instead of empty table.

**Requirements**:
- Show message: "No candidates found matching your filters."
- Show "Clear Filters" button
- Use PrimeVue DataTable empty template slot

### FR-030: Loading State
**WHAT**: Display loading indicator while fetching candidates.

**WHY**: Provide visual feedback during API calls.

**Requirements**:
- Show PrimeVue ProgressSpinner during fetch
- Disable filter controls during load
- Clear loading state on success or error

### FR-031: Error Handling - API Failures
**WHAT**: Handle API errors gracefully with user-friendly messages.

**WHY**: Network failures, server errors, or invalid requests should not crash the UI.

**Requirements**:
- Show error toast notification with message
- Log errors to console for debugging
- Maintain current table state (don't clear candidates)
- Retry option for transient failures

### FR-032: Error Handling - 404 Not Found
**WHAT**: Handle 404 errors when approving/rejecting candidates.

**WHY**: Candidate might have been deleted by another user.

**Requirements**:
- Show error toast: "Candidate not found. It may have been deleted."
- Refresh table to reflect current state

### FR-033: Error Handling - 400 Bad Request
**WHAT**: Handle 400 errors when approving/rejecting candidates.

**WHY**: Candidate might already be in target status.

**Requirements**:
- Show error toast with server error message
- Refresh table to reflect current state

### FR-034: Responsive Layout
**WHAT**: Ensure review queue works on desktop screens (1920×1080 and larger).

**WHY**: Administrators use desktop computers for review tasks.

**Requirements**:
- Target resolution: 1920×1080 minimum
- No mobile optimization required for Feature 013
- PrimeVue DataTable should use full width
- Filter controls in single row above table

### FR-035: Performance - Page Load
**WHAT**: Page should load and display candidates within 2 seconds.

**WHY**: Slow load times reduce administrator productivity.

**Requirements**:
- Initial page load: <2 seconds with 1000+ candidates in database
- Use Spring Data JDBC pagination (LIMIT/OFFSET)
- Index on `created_at` column for date range filters
- Index on `status` column for status filters

### FR-036: Performance - Filter Response
**WHAT**: Filter/sort operations should complete within 1 second.

**WHY**: Slow response times disrupt administrator workflow.

**Requirements**:
- Filter/sort response: <1 second
- Use database-side filtering (not client-side)
- Use database-side sorting (not client-side)

### FR-037: No Authentication (Initial Phase)
**WHAT**: Do not implement authentication for Feature 013.

**WHY**: Admin dashboard accessed only from localhost by Kevin and Huw.

**Requirements**:
- No login screen
- No session management
- No user accounts
- Future Feature: Add Spring Security in later feature

### FR-038: Database Indexes
**WHAT**: Create database indexes for filter and sort performance.

**WHY**: Queries on `status`, `confidence_score`, `created_at`, `search_engine` need optimization.

**Requirements**:
- Create Flyway migration: `V18__add_candidate_indexes.sql`
- Indexes: `idx_candidate_status`, `idx_candidate_confidence`, `idx_candidate_created_at`, `idx_candidate_search_engine`

### FR-039: REST API Module Structure
**WHAT**: Create new Maven module `northstar-rest-api`.

**WHY**: Separate REST API layer from persistence layer.

**Requirements**:
- Location: `northstar-rest-api/`
- Dependencies: northstar-persistence, Spring Web, Spring Boot Starter, Jackson
- Main class: `NorthstarRestApiApplication.java` with `@SpringBootApplication`
- Port: 8080 (default Spring Boot port)

### FR-040: Vue Development Workflow
**WHAT**: Document how to run Vue development server and Spring Boot API simultaneously.

**WHY**: Developers need clear instructions for local development.

**Requirements**:
- Update CLAUDE.md with "Running the Application" section
- Vue dev server: `cd northstar-admin-dashboard && pnpm dev` (port 5173)
- Spring Boot API: `mvn spring-boot:run -pl northstar-rest-api` (port 8080)
- Both must run simultaneously for dashboard to work

### FR-041: Placeholder Pages
**WHAT**: Create placeholder pages for Candidate Detail and Candidate Enhance routes.

**WHY**: Quick actions navigate to these routes, which will be implemented in Features 014-015.

**Requirements**:
- `CandidateDetail.vue`: Show message "Candidate Detail - Coming in Feature 014"
- `CandidateEnhance.vue`: Show message "Candidate Enhancement - Coming in Feature 015"
- Both should display candidate ID from route parameter

### FR-042: Success Toast Notifications
**WHAT**: Show success toast notifications after Approve/Reject actions.

**WHY**: Provide immediate feedback that action completed successfully.

**Requirements**:
- Approve success: "Candidate approved successfully"
- Reject success: "Candidate rejected and domain blacklisted"
- Duration: 3 seconds
- Position: Top-right corner
- Use PrimeVue Toast component

### FR-043: Maven POM Updates
**WHAT**: Update parent POM to include northstar-rest-api module.

**WHY**: Maven multi-module build must include new REST API module.

**Requirements**:
- Update `pom.xml` `<modules>` section
- Add module: `<module>northstar-rest-api</module>`

## Non-Functional Requirements

### NFR-001: Performance
- Initial page load: <2 seconds with 1000+ candidates
- Filter/sort operations: <1 second response time
- Database queries use indexes for status, confidence, date, search engine

### NFR-002: Browser Compatibility
- Chrome 120+ (primary browser for Kevin and Huw)
- Firefox 120+ (fallback)
- No Safari or Edge requirements (desktop only)

### NFR-003: Code Quality
- TypeScript strict mode enabled
- ESLint configured for Vue 3
- Java code follows existing service layer patterns (explicit constructors, `@Transactional`)
- No Lombok in REST API or service classes

### NFR-004: Documentation
- Update CLAUDE.md with Admin Dashboard section
- Update CLAUDE.md with "Running the Application" instructions
- README.md in northstar-admin-dashboard with setup instructions

### NFR-005: Testing
- Unit tests for CandidateService (Mockito)
- Unit tests for CandidateDTOMapper
- REST API integration tests (optional for Feature 013, required for Feature 014+)
- Vue component tests (optional for Feature 013)

## Acceptance Scenarios

### Scenario 1: View All Candidates (Happy Path)
**Given**: Database contains 50 candidates across various statuses and confidence levels
**When**: Administrator opens review queue (`http://localhost:5173/`)
**Then**:
- Page loads in <2 seconds
- Table displays first 20 candidates
- Pagination shows "Page 1 of 3"
- All columns display correct data (URL, Title, Confidence, Status, Search Engine, Created Date)
- Confidence scores are color-coded (green/yellow/orange)
- Quick action buttons (View, Enhance, Approve, Reject) are visible

### Scenario 2: Filter by High Confidence
**Given**: Review queue is displaying all candidates
**When**: Administrator selects "High (e0.80)" from confidence filter
**Then**:
- Table refreshes in <1 second
- Only candidates with confidence e0.80 are displayed
- Filtered count updates (e.g., "Showing 12 of 50 candidates")
- Pagination resets to page 1 if needed
- All displayed confidence scores are green

### Scenario 3: Filter by Multiple Statuses
**Given**: Review queue is displaying all candidates
**When**: Administrator selects "PENDING_CRAWL" and "CRAWLED" from status filter
**Then**:
- Table refreshes in <1 second
- Only candidates with status PENDING_CRAWL or CRAWLED are displayed
- Filtered count updates
- Pagination resets to page 1 if needed

### Scenario 4: Sort by Confidence Descending
**Given**: Review queue is displaying candidates
**When**: Administrator clicks "Confidence Score" column header
**Then**:
- Table re-sorts with highest confidence first
- Arrow icon shows descending direction
- Order: 0.95, 0.87, 0.82, 0.75, 0.68, 0.62...
- Pagination remains on current page

### Scenario 5: Approve Candidate
**Given**: Review queue is displaying a candidate with status CRAWLED
**When**: Administrator clicks "Approve" button
**Then**:
- Confirmation dialog appears: "Are you sure you want to approve this candidate?"
- Administrator clicks "Yes"
- API call to PUT `/api/candidates/{id}/approve` succeeds
- Candidate status updates to APPROVED
- Table refreshes to show new status
- Success toast displays: "Candidate approved successfully"

### Scenario 6: Reject Candidate and Blacklist Domain
**Given**: Review queue is displaying a candidate from domain "spam-site.xyz"
**When**: Administrator clicks "Reject" button
**Then**:
- Confirmation dialog appears: "Are you sure you want to reject this candidate and blacklist the domain?"
- Administrator clicks "Yes"
- API call to PUT `/api/candidates/{id}/reject` succeeds
- Candidate status updates to REJECTED
- Domain "spam-site.xyz" status updates to BLACKLISTED
- Table refreshes to show new status
- Success toast displays: "Candidate rejected and domain blacklisted"

### Scenario 7: Navigate to Candidate Detail
**Given**: Review queue is displaying candidates
**When**: Administrator clicks "View" button for candidate with ID "123e4567-e89b-12d3-a456-426614174000"
**Then**:
- Vue Router navigates to `/candidates/123e4567-e89b-12d3-a456-426614174000`
- Placeholder page displays: "Candidate Detail - Coming in Feature 014"
- Candidate ID is shown on placeholder page

### Scenario 8: Clear All Filters
**Given**: Review queue has multiple filters applied (status=CRAWLED, confidencee0.70, date=Last 7 days)
**When**: Administrator clicks "Clear Filters" button
**Then**:
- All filters reset to defaults
- Status filter: All
- Confidence filter: All (e0.00)
- Search engine filter: All
- Date range: Last 7 days
- Table refreshes showing all candidates
- Filtered count shows total candidate count

### Scenario 9: Handle Empty Results
**Given**: Database contains 50 candidates, none with confidence e0.90
**When**: Administrator filters by confidence e0.90 (custom threshold, not in FR-004 options)
**Then**:
- Table shows empty state message: "No candidates found matching your filters."
- "Clear Filters" button is prominently displayed
- Pagination controls are hidden

### Scenario 10: Handle API Error on Approve
**Given**: Review queue is displaying candidates
**When**: Administrator clicks "Approve" button but API returns 500 Internal Server Error
**Then**:
- Error toast displays: "Failed to approve candidate. Please try again."
- Candidate status does NOT change
- Table remains in current state
- Administrator can retry action

## Edge Cases

### Edge Case 1: Pagination with Filters
**Scenario**: Administrator is on page 3 of 10, then applies a filter that results in only 15 total candidates (1 page).
**Expected**: Pagination resets to page 1, shows "Page 1 of 1".

### Edge Case 2: Concurrent Rejection
**Scenario**: Two administrators open the same candidate. Admin A rejects it. Admin B tries to reject it 10 seconds later.
**Expected**: Admin B receives 400 Bad Request with message "Candidate already rejected". Error toast displays message.

### Edge Case 3: No Candidates in Database
**Scenario**: Database has zero candidates (fresh installation or all deleted).
**Expected**: Table shows empty state message. Filters are still visible but disabled. No API errors.

### Edge Case 4: Date Range Future Dates
**Scenario**: Administrator selects start date = today, end date = 30 days in future.
**Expected**: No results (candidates cannot be created in future). Empty state message displays.

### Edge Case 5: Sort Direction Toggle
**Scenario**: Administrator clicks "Confidence Score" column header three times rapidly.
**Expected**: First click: descending. Second click: ascending. Third click: descending. Each click updates arrow icon and re-sorts table.

### Edge Case 6: Large Dataset Performance
**Scenario**: Database contains 10,000 candidates. Administrator loads review queue.
**Expected**: First page (20 candidates) loads in <2 seconds. Pagination shows "Page 1 of 500". Filtering and sorting remain <1 second.

### Edge Case 7: Missing Domain Reference
**Scenario**: FundingSourceCandidate references a domain that was deleted from `domain` table.
**Expected**: API handles gracefully (log warning, skip blacklist operation), returns 200 with status=REJECTED. Domain blacklist step fails silently.

## Key Entities

This feature works with the following existing entities from `northstar-domain`:

### FundingSourceCandidate
- **Purpose**: Main entity representing discovered funding opportunities
- **Key Fields**:
  - `id` (UUID) - Primary key
  - `url` (String) - Funding source URL
  - `title` (String) - Page title from search result
  - `confidenceScore` (BigDecimal, scale 2) - Confidence 0.00-1.00
  - `status` (CandidateStatus enum) - Workflow status
  - `searchEngine` (SearchEngineType enum) - Which engine discovered it
  - `domainId` (UUID) - Foreign key to Domain
  - `sessionId` (UUID) - Foreign key to DiscoverySession
  - `createdAt` (LocalDateTime) - Discovery timestamp

### Domain
- **Purpose**: Domain-level deduplication and blacklist management
- **Key Fields**:
  - `id` (UUID) - Primary key
  - `domainName` (String) - Unique domain (e.g., "europa.eu")
  - `status` (DomainStatus enum) - DISCOVERED, BLACKLISTED, etc.
  - `qualityScore` (BigDecimal) - Domain quality metric
  - `firstSeen` (LocalDateTime) - First discovery timestamp

### CandidateStatus Enum
Values used in this feature:
- NEW - Initial state (not used in Feature 013)
- PENDING_CRAWL - Awaiting deep web crawl
- CRAWLED - Web crawl completed
- ENHANCED - Administrator added information
- JUDGED - AI judging completed (Feature 015)
- APPROVED - Administrator approved for client DB
- REJECTED - Administrator rejected
- SKIPPED_LOW_CONFIDENCE - Confidence <0.60, not crawled
- BLACKLISTED - Domain blacklisted (via rejection)

### SearchEngineType Enum
Values:
- BRAVE - Brave Search API
- TAVILY - Tavily AI Search
- PERPLEXITY - Perplexity API
- SEARXNG - Self-hosted SearXNG instance
- BROWSERBASE - Browserbase browser automation

## Out of Scope

The following are explicitly OUT OF SCOPE for Feature 013:

1. **Authentication/Authorization** - No login, no user accounts (Feature 019+)
2. **Candidate Detail View** - Placeholder only (Feature 014)
3. **Candidate Enhancement Form** - Placeholder only (Feature 015)
4. **Contact Intelligence** - AI extraction not implemented (Feature 016)
5. **Approval Workflow with Notes** - Simple approve/reject only (Feature 017)
6. **Statistics Dashboard** - No metrics/charts (Feature 018)
7. **Domain Management UI** - Cannot view/edit blacklist (Feature 019)
8. **Mobile Responsive Design** - Desktop only (1920×1080+)
9. **Bulk Actions** - Cannot approve/reject multiple candidates at once
10. **Export Functionality** - Cannot export to CSV/Excel
11. **Search/Full-Text Filter** - Only structured filters (status, confidence, engine, date)
12. **Custom Confidence Thresholds** - Only predefined options (e0.60, e0.70, e0.80)
13. **Real-Time Updates** - No WebSocket/SSE, manual refresh required
14. **Undo Approve/Reject** - Actions are final
15. **Audit Log** - No tracking of who approved/rejected what

## Dependencies

### Existing Features
- Feature 001-012: Domain model, persistence layer, search result processing (all complete)

### External Systems
- PostgreSQL 16 at 192.168.1.10:5432
- Spring Boot 3.5.7 with Spring Data JDBC
- Maven multi-module build system

### New Technologies
- Node.js 20+ (for Vue development)
- pnpm (or npm) package manager
- Vite 5.0 build tool
- Vue 3.4+ with TypeScript
- PrimeVue 3.50 (FREE/MIT)
- Pinia 2.1
- Axios 1.6

## Implementation Notes

**FOR /plan COMMAND**:

When generating the implementation plan, remember:

1. **Vertical Slice**: This feature spans Vue + REST API + Persistence layers
2. **Build Order**:
   - Create northstar-rest-api Maven module first
   - Create DTOs and mappers
   - Create Spring Boot REST controller and service
   - Create Vue project structure
   - Implement Vue components and stores
3. **Testing Strategy**: Start with REST API unit tests (Mockito), add Vue component tests if time permits
4. **DTO Pattern**: CandidateDTO is the API contract - all TypeScript interfaces mirror it exactly
5. **No Authentication**: Skip Spring Security configuration for Feature 013
6. **Database Indexes**: Create Flyway migration V18 for performance
7. **CORS**: Must configure Spring Boot to allow requests from `http://localhost:5173`
8. **PrimeVue Components**: Use FREE components only (DataTable, Column, Paginator, Dropdown, MultiSelect, Calendar, Button, ConfirmDialog, Toast)

## Success Metrics

Feature 013 is successful when:

1.  Administrators can view paginated list of candidates
2.  Filter by status, confidence, search engine, date range works correctly
3.  Sort by any column works correctly
4.  Approve/Reject actions update database and show confirmations
5.  Reject action blacklists domain
6.  Page loads in <2 seconds with 1000+ candidates
7.  All filter/sort operations complete in <1 second
8.  Color-coded confidence scores display correctly
9.  Quick actions navigate to placeholder pages
10.  Error handling works for API failures and edge cases

## Checklist

Planning Phase:
- [x] Feature number assigned (013)
- [x] Feature name finalized (Admin Dashboard Review Queue)
- [x] Functional requirements complete (FR-001 through FR-043)
- [x] Acceptance scenarios written (10 scenarios)
- [x] Edge cases documented (7 edge cases)
- [x] Out of scope explicitly listed
- [x] Success metrics defined

Architecture Phase:
- [x] Three-layer architecture documented (Vue ’ REST API ’ Persistence)
- [x] DTO pattern explained with examples
- [x] Data flow documented (Domain ’ DTO ’ JSON ’ Vue)
- [x] Technology stack confirmed (Vue 3 + PrimeVue + Spring Boot)
- [x] Project structure defined (northstar-admin-dashboard/ + northstar-rest-api/)
- [x] Database indexes identified
- [x] CORS requirements documented

Implementation Readiness:
- [x] All prerequisites met (Features 001-012 complete)
- [x] PostgreSQL database accessible (192.168.1.10:5432)
- [x] Maven multi-module structure understood
- [x] Branch created (013-create-admin-dashboard)
- [ ] Ready for `/plan` command

## Notes

- This specification focuses on WHAT and WHY, not HOW
- Implementation details (code structure, class names, method signatures) will be determined in `/plan` phase
- PrimeVue DataTable component is the core UI element - it handles pagination, sorting, filtering out of the box
- DTO pattern is critical - never expose domain entities through REST API
- Performance requirements (2s page load, 1s filters) will require database indexes
- No authentication for Feature 013 - localhost access only by Kevin and Huw
- Placeholder pages for Features 014-015 enable quick action navigation without blocking Feature 013 completion
