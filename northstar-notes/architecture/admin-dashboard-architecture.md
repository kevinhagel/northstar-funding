# Admin Dashboard Architecture

**Created**: 2025-11-16
**Authors**: Kevin (user) + Claude Code
**Status**: Design Complete, Ready for Implementation
**Tech Stack**: Vue 3 + TypeScript + Vite + PrimeVue

---

## Executive Summary

The Admin Dashboard is a Vue 3 + PrimeVue web application that enables Kevin and Huw to review, enhance, and approve funding source candidates discovered by the automated search system. This is the **critical human component** of the human-AI hybrid workflow that makes NorthStar viable as a business.

**Key Insight**: Pure automation failed due to website design variability and contact intelligence extraction challenges. The admin dashboard is where humans add the value that AI cannot: extracting contacts, building organization hierarchies, and making final quality judgments.

---

## System Overview

### Three-Layer Architecture

```
┌─────────────────────────────────────────────────────────┐
│  LAYER 1: Vue Admin UI (northstar-admin-ui/)            │
│  ├─ Technology: Vue 3 + TypeScript + Vite + PrimeVue   │
│  ├─ Port: http://localhost:5173                         │
│  ├─ Purpose: Human review and enhancement interface     │
│  └─ Users: Kevin, Huw (admin users)                     │
└────────────────┬────────────────────────────────────────┘
                 │
                 │ HTTP REST calls (JSON)
                 │ GET /api/candidates
                 │ PUT /api/candidates/{id}
                 │ POST /api/candidates/{id}/approve
                 ↓
┌─────────────────────────────────────────────────────────┐
│  LAYER 2: REST API (northstar-rest-api/)                │
│  ├─ Technology: Spring Boot 3.5.7 + Java 25            │
│  ├─ Port: http://localhost:8080                         │
│  ├─ Purpose: API boundary, DTO mapping, validation      │
│  └─ Pattern: Controllers → Services → Repositories      │
└────────────────┬────────────────────────────────────────┘
                 │
                 │ Spring Data JDBC
                 │ Repository.findById()
                 │ Repository.save()
                 ↓
┌─────────────────────────────────────────────────────────┐
│  LAYER 3: Persistence (northstar-persistence/)          │
│  ├─ Technology: Spring Data JDBC + PostgreSQL 16       │
│  ├─ Database: 192.168.1.10:5432/northstar_funding      │
│  ├─ Purpose: Domain entities, business logic, storage   │
│  └─ Pattern: Domain entities + Service classes          │
└─────────────────────────────────────────────────────────┘
```

### Independent Build Systems

**Backend (Maven):**
```bash
cd /Users/kevin/github/northstar-funding
mvn spring-boot:run -pl northstar-rest-api
```

**Frontend (npm/pnpm):**
```bash
cd /Users/kevin/github/northstar-funding/northstar-admin-ui
npm run dev
```

**Critical:** Vue is **NOT** part of the Maven build. It's a sibling module with its own build system.

---

## Data Flow Architecture

### The Complete Journey: Database → UI

```
┌─────────────────────────────────────────────────────────┐
│ 1. DATABASE (PostgreSQL)                                │
├─────────────────────────────────────────────────────────┤
│ funding_source_candidate table:                         │
│   candidate_id: UUID                                    │
│   url: TEXT                                             │
│   title: TEXT                                           │
│   description: TEXT                                     │
│   confidence_score: NUMERIC(3,2)                        │
│   status: candidate_status ENUM                         │
│   discovery_session_id: UUID                            │
│   created_at: TIMESTAMP                                 │
└─────────────────────────────────────────────────────────┘
                    ↓ Spring Data JDBC
┌─────────────────────────────────────────────────────────┐
│ 2. DOMAIN ENTITY (Java - northstar-domain/)             │
├─────────────────────────────────────────────────────────┤
│ @Data                                                    │
│ @Table("funding_source_candidate")                      │
│ public class FundingSourceCandidate {                   │
│     private UUID candidateId;                           │
│     private String url;                                 │
│     private String title;                               │
│     private String description;                         │
│     private BigDecimal confidenceScore;                 │
│     private CandidateStatus status;                     │
│     private UUID discoverySessionId;                    │
│     private LocalDateTime createdAt;                    │
│ }                                                        │
└─────────────────────────────────────────────────────────┘
                    ↓ Mapper converts
┌─────────────────────────────────────────────────────────┐
│ 3. DTO (Java - northstar-rest-api/dto/)                 │
├─────────────────────────────────────────────────────────┤
│ // DTOs are the API contract                            │
│ public record CandidateDTO(                             │
│     String id,              // UUID → String            │
│     String url,                                         │
│     String title,                                       │
│     String description,                                 │
│     String confidenceScore, // BigDecimal → String      │
│     String status,          // Enum → String            │
│     String sessionId,       // UUID → String            │
│     String createdAt        // LocalDateTime → ISO-8601 │
│ ) {}                                                     │
│                                                          │
│ // Mapper service                                       │
│ @Service                                                 │
│ public class CandidateDTOMapper {                       │
│     public CandidateDTO toDTO(FundingSourceCandidate e) │
│     public FundingSourceCandidate toDomain(DTO dto)     │
│ }                                                        │
└─────────────────────────────────────────────────────────┘
                    ↓ Jackson serializes
┌─────────────────────────────────────────────────────────┐
│ 4. JSON (HTTP Response)                                 │
├─────────────────────────────────────────────────────────┤
│ GET /api/candidates/abc123                              │
│                                                          │
│ {                                                        │
│   "id": "abc123-def456-ghi789",                         │
│   "url": "https://ec.europa.eu/research",               │
│   "title": "Horizon Europe Research Grants",            │
│   "description": "EU funding for research...",          │
│   "confidenceScore": "0.87",                            │
│   "status": "PENDING_CRAWL",                            │
│   "sessionId": "xyz789-...",                            │
│   "createdAt": "2025-11-16T10:30:45Z"                   │
│ }                                                        │
└─────────────────────────────────────────────────────────┘
                    ↓ Axios fetches
┌─────────────────────────────────────────────────────────┐
│ 5. TYPESCRIPT INTERFACE (northstar-admin-ui/types/)     │
├─────────────────────────────────────────────────────────┤
│ // Mirrors CandidateDTO.java                            │
│ export interface Candidate {                            │
│   id: string;                                           │
│   url: string;                                          │
│   title: string;                                        │
│   description: string;                                  │
│   confidenceScore: string;                              │
│   status: CandidateStatus;                              │
│   sessionId: string;                                    │
│   createdAt: string;                                    │
│ }                                                        │
│                                                          │
│ export type CandidateStatus =                           │
│   | 'PENDING_CRAWL'                                     │
│   | 'CRAWLED'                                           │
│   | 'ENHANCED'                                          │
│   | 'APPROVED';                                         │
└─────────────────────────────────────────────────────────┘
                    ↓ Vue component uses
┌─────────────────────────────────────────────────────────┐
│ 6. VUE COMPONENT (Display)                              │
├─────────────────────────────────────────────────────────┤
│ <script setup lang="ts">                                │
│ import { candidateService } from '@/services/api';      │
│ import type { Candidate } from '@/types/candidate';     │
│                                                          │
│ const candidate = ref<Candidate | null>(null);          │
│                                                          │
│ onMounted(async () => {                                 │
│   candidate.value =                                     │
│     await candidateService.getById('abc123');           │
│ });                                                      │
│ </script>                                                │
│                                                          │
│ <template>                                               │
│   <DataTable :value="[candidate]">                      │
│     <Column field="title" header="Title" />             │
│     <Column field="confidenceScore" header="Score" />   │
│   </DataTable>                                           │
│ </template>                                              │
└─────────────────────────────────────────────────────────┘
```

### The Return Journey: User Edits → Database

```
USER EDITS IN FORM
  ↓ Axios PUT request
JSON PAYLOAD
  ↓ Spring deserializes
DTO (with validation)
  ↓ Mapper converts
DOMAIN ENTITY (business logic)
  ↓ Spring Data JDBC
DATABASE UPDATE
```

---

## DTO Design Principles

### Critical Rules

1. **Domain entities NEVER leave the service layer**
   - ❌ Controllers cannot return `FundingSourceCandidate` directly
   - ✅ Controllers return `CandidateDTO` (mapped from domain)

2. **DTOs are the API contract**
   - ✅ Use Java `record` for immutability
   - ✅ Use primitives/Strings (not complex types like UUID, BigDecimal, LocalDateTime)
   - ✅ Separate read DTOs from write DTOs

3. **Validation happens at DTO level**
   - ✅ Use `@Valid`, `@NotNull`, `@NotBlank` on DTOs
   - ✅ Validation errors return 400 Bad Request automatically

4. **TypeScript types mirror DTOs**
   - ✅ Keep TypeScript interfaces in sync with Java DTOs
   - ✅ Use code generation later if needed (manual for now)

### DTO Location

```
northstar-rest-api/src/main/java/com/northstar/funding/rest/
├── dto/
│   ├── CandidateDTO.java           // Read model (GET responses)
│   ├── CreateCandidateRequest.java // Write model (POST)
│   ├── UpdateCandidateRequest.java // Write model (PUT)
│   ├── ContactDTO.java
│   ├── OrganizationDTO.java
│   └── SessionStatisticsDTO.java
│
├── mapper/
│   ├── CandidateDTOMapper.java
│   ├── ContactDTOMapper.java
│   └── OrganizationDTOMapper.java
│
└── controller/
    ├── CandidateController.java
    ├── StatisticsController.java
    └── DomainController.java
```

### Example DTO Pattern

```java
// Read model (GET responses)
public record CandidateDTO(
    String id,
    String url,
    String title,
    String description,
    String confidenceScore,
    String status,
    String sessionId,
    String createdAt
) {}

// Write model (PUT requests)
public record UpdateCandidateRequest(
    @NotBlank String title,
    String description,
    @NotNull CandidateStatus status,
    String organizationName,
    List<ContactDTO> contacts
) {}

// Mapper service
@Service
public class CandidateDTOMapper {

    public CandidateDTO toDTO(FundingSourceCandidate entity) {
        return new CandidateDTO(
            entity.getCandidateId().toString(),
            entity.getUrl(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getConfidenceScore().toString(),
            entity.getStatus().name(),
            entity.getDiscoverySessionId().toString(),
            entity.getCreatedAt().toString()
        );
    }

    public FundingSourceCandidate toDomain(
        UpdateCandidateRequest dto,
        UUID existingId
    ) {
        FundingSourceCandidate entity = new FundingSourceCandidate();
        entity.setCandidateId(existingId);
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setStatus(dto.status());
        return entity;
    }
}
```

---

## Vue Frontend Architecture

### Project Structure

```
northstar-admin-ui/
├── package.json              # Dependencies (Vue, PrimeVue, Axios, etc.)
├── vite.config.ts            # Vite build config + proxy
├── tsconfig.json             # TypeScript config
├── index.html                # Entry HTML
│
├── src/
│   ├── main.ts               # Vue app initialization
│   ├── App.vue               # Root component
│   │
│   ├── router/
│   │   └── index.ts          # Vue Router config
│   │
│   ├── views/                # Page components (routed)
│   │   ├── ReviewQueue.vue
│   │   ├── CandidateDetail.vue
│   │   ├── Dashboard.vue
│   │   └── DomainManagement.vue
│   │
│   ├── components/           # Reusable components
│   │   ├── layout/
│   │   │   ├── AppLayout.vue
│   │   │   ├── Sidebar.vue
│   │   │   └── Topbar.vue
│   │   └── candidate/
│   │       ├── CandidateCard.vue
│   │       ├── ContactForm.vue
│   │       └── EnhancementHistory.vue
│   │
│   ├── services/             # API client services
│   │   ├── api.ts            # Axios instance + config
│   │   ├── candidateService.ts
│   │   ├── statisticsService.ts
│   │   └── domainService.ts
│   │
│   ├── types/                # TypeScript interfaces (mirror DTOs)
│   │   ├── candidate.ts
│   │   ├── contact.ts
│   │   ├── organization.ts
│   │   └── statistics.ts
│   │
│   ├── stores/               # Pinia state management
│   │   ├── candidateStore.ts
│   │   └── userStore.ts
│   │
│   └── assets/               # Static assets (CSS, images)
│       └── styles/
│           └── main.css
│
├── public/                   # Public static files
│   └── favicon.ico
│
└── dist/                     # Build output (gitignored)
```

### Technology Stack

```json
{
  "name": "northstar-admin-ui",
  "version": "1.0.0",
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "primevue": "^3.50.0",
    "primeicons": "^7.0.0",
    "axios": "^1.6.0",
    "chart.js": "^4.4.0",
    "date-fns": "^3.0.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "typescript": "^5.3.0",
    "@vitejs/plugin-vue": "^5.0.0"
  }
}
```

### Vite Configuration (CORS Proxy)

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
```

**Why proxy?** Vue dev server runs on port 5173, Spring Boot on 8080. The proxy forwards `/api/*` requests to avoid CORS issues.

---

## UI Design (PrimeVue Components)

### Main Layout

```
┌──────────────────────────────────────────────────────────┐
│  [🌟 NorthStar]  Admin Dashboard        [Kevin ▼] [🔔]  │ ← Topbar
├────────┬─────────────────────────────────────────────────┤
│        │                                                  │
│  📋    │                                                  │
│ Review │                                                  │
│ Queue  │                                                  │
│        │         MAIN CONTENT                            │
│  📊    │         (router-view)                           │
│ Stats  │                                                  │
│        │                                                  │
│  🌐    │                                                  │
│Domains │                                                  │
│        │                                                  │
│  🔍    │                                                  │
│Sessions│                                                  │
└────────┴─────────────────────────────────────────────────┘
   ↑ Sidebar
```

### Review Queue Page (Primary Interface)

**Route**: `/admin/queue`
**Component**: `views/ReviewQueue.vue`
**API**: `GET /api/candidates?status=PENDING_CRAWL&page=1`

```
┌──────────────────────────────────────────────────────────┐
│  Review Queue - Funding Source Candidates                │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  Filters:                                                 │
│  Status: [All ▼] [PENDING] [ENHANCED]                   │
│  Confidence: [≥ 0.60 ▼] [≥ 0.70] [≥ 0.80]               │
│  Source: [All ▼] [SearXNG] [Tavily]                     │
│  Date: [Last 7 days ▼]                                   │
│  Search: [                              ] 🔍             │
│                                                           │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │ ⭐ 0.87 │ ec.europa.eu/research                  │   │
│  │ PENDING │ Horizon Europe Research Grants         │   │
│  │ SearXNG │ EU funding for research...             │   │
│  │ Nov 16  │ [👁 View] [✏️ Enhance] [✅] [❌]        │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │ ⭐ 0.75 │ us-bulgaria.org/grants                 │   │
│  │ PENDING │ America for Bulgaria Foundation        │   │
│  │ Tavily  │ Supporting democratic development...   │   │
│  │ Nov 16  │ [👁 View] [✏️ Enhance] [✅] [❌]        │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
│  Showing 1-20 of 156  [◀ Prev] [1][2][3] [Next ▶]       │
└──────────────────────────────────────────────────────────┘
```

**PrimeVue Components:**
- `<DataTable>` with pagination, sorting, filtering
- `<Dropdown>` for filters
- `<InputText>` for search
- `<Button>` for actions
- `<Badge>` for status display

### Candidate Enhancement Page (Critical Workflow)

**Route**: `/admin/candidates/:id`
**Component**: `views/CandidateDetail.vue`
**APIs**:
- `GET /api/candidates/:id`
- `PUT /api/candidates/:id`
- `POST /api/candidates/:id/approve`

```
┌──────────────────────────────────────────────────────────┐
│  [◀ Back]    Enhance Candidate #abc123                   │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─ Discovery Metadata ────────────────────────────┐    │
│  │ URL: https://ec.europa.eu/research              │    │
│  │ Confidence: ⭐ 0.87 (High)                       │    │
│  │ Source: SearXNG | Discovered: Nov 16 10:30      │    │
│  │ Session: abc123-def456                           │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
│  ┌─ Organization & Program ─────────────────────────┐   │
│  │ Organization: [European Commission Research   ]  │   │
│  │ Program: [Horizon Europe                      ]  │   │
│  │                                                   │   │
│  │ Funding Type: [EU_INSTITUTION ▼]                │   │
│  │ Mechanism: [GRANT ▼]                             │   │
│  │ Scale: [LARGE (€100k-1M) ▼]                     │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
│  ┌─ Contact Intelligence ───────── [🤖 AI Extract] │   │
│  │                                                   │   │
│  │ Contact 1:                                        │   │
│  │   Name:  [Dr. Maria Schmidt                   ]  │   │
│  │   Email: [maria.schmidt@ec.europa.eu         ]  │   │
│  │   Phone: [+32 2 123 4567                     ]  │   │
│  │   Role:  [Program Manager ▼]     [➖ Remove]   │   │
│  │                                                   │   │
│  │ [➕ Add Contact]                                  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
│  ┌─ Enhancement History ────────────────────────────┐   │
│  │ Nov 16 10:45 - Kevin added contact              │   │
│  │ Nov 16 10:40 - System created candidate         │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
│  [❌ Reject] [💾 Save Draft] [✅ Approve & Publish]      │
└──────────────────────────────────────────────────────────┘
```

**PrimeVue Components:**
- `<InputText>` for text fields
- `<Dropdown>` for enums (status, type, mechanism)
- `<Calendar>` for dates
- `<Checkbox>` for multi-select
- `<Button>` for actions
- `<Timeline>` for enhancement history

---

## REST API Endpoints

### Candidates

```
GET    /api/candidates
  Query params: ?status=PENDING&confidence=0.70&page=1&size=20
  Response: { items: CandidateDTO[], total: 156 }

GET    /api/candidates/{id}
  Response: CandidateDTO

PUT    /api/candidates/{id}
  Request: UpdateCandidateRequest
  Response: CandidateDTO

POST   /api/candidates/{id}/approve
  Response: CandidateDTO (status → APPROVED)

POST   /api/candidates/{id}/reject
  Request: { reason: string }
  Response: void (blacklists domain)

POST   /api/candidates/{id}/extract-contacts
  Response: List<ContactDTO> (AI-extracted)
```

### Statistics

```
GET    /api/statistics/overview
  Response: {
    totalCandidates: 156,
    pendingReview: 89,
    approved: 45,
    highConfidence: 124
  }

GET    /api/statistics/trends
  Query: ?days=30
  Response: { dates: [], counts: [] }
```

### Domains

```
GET    /api/domains
  Query: ?status=BLACKLISTED
  Response: DomainDTO[]

POST   /api/domains/blacklist
  Request: { domain: string, reason: string }
  Response: DomainDTO

DELETE /api/domains/{id}/blacklist
  Response: void
```

---

## Development Workflow

### Running Locally

```bash
# Terminal 1: Backend
cd /Users/kevin/github/northstar-funding
mvn spring-boot:run -pl northstar-rest-api
# → http://localhost:8080

# Terminal 2: Frontend
cd /Users/kevin/github/northstar-funding/northstar-admin-ui
npm install  # First time only
npm run dev
# → http://localhost:5173
```

### Making Changes

**Backend changes** (Java):
1. Edit controller/service/mapper in `northstar-rest-api/`
2. Spring Boot auto-reloads
3. Test: `curl http://localhost:8080/api/candidates`

**Frontend changes** (Vue):
1. Edit component in `northstar-admin-ui/src/`
2. Vite hot-reloads automatically
3. See changes instantly in browser

**DTO changes** (critical sync point):
1. Update Java DTO in `northstar-rest-api/dto/`
2. Update TypeScript interface in `northstar-admin-ui/src/types/`
3. Update mapper if field transformations changed
4. Test API contract manually or with integration tests

### Testing Strategy

**Backend:**
- Unit tests: Service layer (Mockito)
- Integration tests: REST API (TestContainers)
- Run: `mvn test -pl northstar-rest-api`

**Frontend:**
- Component tests: Vitest (later)
- E2E tests: Playwright (later)
- Manual testing initially

---

## Security & Authentication

### Phase 1: Development (Now)

**No authentication** - Dashboard runs locally on `localhost:5173`

**Why acceptable:**
- Only Kevin & Huw have access
- Runs on development machine
- Not publicly accessible
- Fast iteration

### Phase 2: Production (Later)

**Add authentication:**
- Spring Security with JWT tokens
- Login page in Vue
- Role-based access control (RBAC)
- Use existing `admin_user` table and `AdminRole` enum

**When to add:**
- Before deploying to server
- Before adding more users
- Before exposing to internet

---

## Feature Roadmap (Vertical Slices)

Each feature delivers complete end-to-end value across all three layers.

### Feature 013: Review Queue (View Candidates)
**Goal**: Kevin/Huw can view list of candidates
**Scope**:
- ✅ Vue: ReviewQueue.vue with PrimeVue DataTable
- ✅ REST: GET /api/candidates (pagination, filters)
- ✅ Persistence: CandidateRepository (already exists)

**Success**: View candidates at http://localhost:5173/admin/queue

---

### Feature 014: Candidate Enhancement (Edit & Save)
**Goal**: Kevin/Huw can edit candidate details and save
**Scope**:
- ✅ Vue: CandidateDetail.vue with forms
- ✅ REST: GET /api/candidates/{id}, PUT /api/candidates/{id}
- ✅ Persistence: Update logic + enhancement tracking

**Success**: Edit title, save, see changes persisted

---

### Feature 015: Contact Intelligence (AI Extraction)
**Goal**: AI assists with extracting contacts from webpages
**Scope**:
- ✅ Vue: Contact form with "AI Extract" button
- ✅ REST: POST /api/candidates/{id}/extract-contacts (Ollama integration)
- ✅ Persistence: ContactIntelligence CRUD

**Success**: Click button, AI extracts emails/names, user confirms

---

### Feature 016: Approval Workflow (Approve/Reject)
**Goal**: Kevin/Huw can approve or reject candidates
**Scope**:
- ✅ Vue: Approve/Reject buttons with confirmation
- ✅ REST: POST /api/candidates/{id}/approve, POST /api/candidates/{id}/reject
- ✅ Persistence: Status updates, blacklist on reject

**Success**: Approve moves to knowledge base, reject blacklists domain

---

### Feature 017: Statistics Dashboard
**Goal**: View metrics and trends
**Scope**:
- ✅ Vue: Dashboard.vue with charts (Chart.js)
- ✅ REST: GET /api/statistics/overview, GET /api/statistics/trends
- ✅ Persistence: Aggregate queries

**Success**: See total candidates, approval rates, discovery trends

---

### Feature 018: Domain Management
**Goal**: Manage blacklist and domain quality
**Scope**:
- ✅ Vue: DomainManagement.vue with domain table
- ✅ REST: GET /api/domains, POST /api/domains/blacklist
- ✅ Persistence: Domain CRUD, blacklist updates

**Success**: View domains, manually blacklist/unblock

---

## Migration Notes

### Archived Implementations

**2025-11-16**: Archived two previous frontend attempts:
- `archived-frontends/backend-springboot-monolith/` - Old Spring Boot monolith
- `archived-frontends/frontend-streamlit/` - Old Streamlit Python frontend

**Reason**: Starting fresh with Vue 3 + PrimeVue for better admin UI components and modern development experience.

**Current approach**: Multi-module Maven backend + Vue 3 frontend (separate build systems)

---

## References

**Related Documents:**
- `northstar-notes/session-summaries/2025-11-16-admin-dashboard-planning.md` - Planning session
- `northstar-notes/decisions/002-testcontainers-integration-test-pattern.md` - Testing approach
- `northstar-notes/session-summaries/2025-10-31-business-model-clarification.md` - Human-AI hybrid rationale

**External Resources:**
- [PrimeVue DataTable Documentation](https://primevue.org/datatable/)
- [Vue 3 Composition API](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Spring Boot REST Best Practices](https://spring.io/guides/tutorials/rest/)

---

## Next Steps

1. ✅ Create Feature 013 specification (`/specify` command)
2. ⏳ Generate implementation plan (`/plan` command)
3. ⏳ Build Vue dashboard skeleton (routing, layout, API client)
4. ⏳ Implement Review Queue page (first working feature)
5. ⏳ Add REST API endpoints for candidates
6. ⏳ Integration testing with real database

**Status**: Architecture complete, ready for Feature 013 implementation

---

**Document Version**: 1.0
**Last Updated**: 2025-11-16
**Maintained By**: Kevin + Claude Code
