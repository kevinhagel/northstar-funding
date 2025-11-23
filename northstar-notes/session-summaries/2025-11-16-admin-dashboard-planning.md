# Session Summary: Admin Dashboard Planning & Architecture

**Date**: 2025-11-16
**Start Time**: ~12:00 EET
**Duration**: ~2 hours
**Session Type**: Architecture & Planning
**Status**: ✅ Design Complete, Ready for Implementation

---

## Overview

Designed the complete Admin Dashboard architecture for Kevin and Huw to review, enhance, and approve funding source candidates. This is the **critical human component** of the NorthStar human-AI hybrid workflow.

**Key Achievement**: Defined three-layer architecture (Vue + REST API + Persistence) with clear data flow, DTO patterns, and vertical slice feature breakdown.

---

## Context: Why Now?

**User's Statement**: "We need to start running our nightly searches, populating the database. I want to know what we are going to do with the data."

**Problem Identified**:
- Backend can generate queries ✅
- Backend can process search results ✅
- **Missing**: Frontend to view/enhance/approve candidates ❌
- **Missing**: REST API to connect frontend to backend ❌

**Solution**: Build Admin Dashboard with 6 features (013-018) using vertical slices

---

## Major Decisions Made

### 1. Technology Stack: Vue 3 + PrimeVue

**Decision**: Use Vue 3 + TypeScript + Vite + PrimeVue for admin dashboard

**Rationale**:
- PrimeVue has exceptional admin UI components (DataTable, forms, dropdowns)
- Simpler than React, less boilerplate
- Excellent TypeScript support
- Claude Code has strong Vue knowledge

**Alternatives Considered**:
- React + shadcn/ui (more ecosystem, but more complex)
- Next.js (overkill for admin dashboard, SSR not needed)
- Streamlit (already tried, archived)

**Outcome**: Vue 3 + PrimeVue selected, architecture documented

---

### 2. Three Separate Frontend Types

**Clarification**: NorthStar needs **three different frontends**:

1. **Admin Dashboard** (Kevin/Huw) - Vue 3 + PrimeVue ← **Building this first**
2. **Client Dashboard** (paying customers) - Vue/React ← Later
3. **Public Website** (marketing) - Next.js/Nuxt ← Later

**Key Insight**: Admin dashboard is most critical - without it, Kevin/Huw cannot review candidates even if backend is generating them.

**Priority**: Build Admin Dashboard first, others can wait until we have data in the database.

---

### 3. Architecture: Three-Layer System

**Decision**: Separate layers with clear boundaries

```
Vue Admin UI (northstar-admin-ui/)
    ↓ HTTP REST (JSON)
REST API (northstar-rest-api/)
    ↓ Spring Data JDBC
Persistence (northstar-persistence/)
```

**Critical Rules**:
- Domain entities NEVER leave service layer
- REST API uses DTOs (Data Transfer Objects)
- TypeScript interfaces mirror Java DTOs
- Each layer has separate build system (npm vs Maven)

---

### 4. Data Flow: Domain → DTO → JSON → Vue

**Decision**: Use DTOs as API contract boundary

**The Journey**:
```
Database (PostgreSQL)
  ↓ Spring Data JDBC
Domain Entity (FundingSourceCandidate.java)
  ↓ Mapper
DTO (CandidateDTO.java - simple types only)
  ↓ Jackson serialization
JSON (HTTP response)
  ↓ Axios fetch
TypeScript Interface (Candidate.ts - mirrors DTO)
  ↓ Vue component
UI Display (PrimeVue DataTable)
```

**Why DTOs?**:
- API stability (domain can change without breaking UI)
- Validation at API boundary
- Type safety across language boundary (Java ↔ TypeScript)
- No complex types exposed (UUID → String, BigDecimal → String)

---

### 5. Feature Breakdown: Vertical Slices

**Decision**: 6 features, each delivering end-to-end value

**Traditional approach (rejected)**:
- Feature 013: Build entire Vue UI (weeks)
- Feature 014: Build entire REST API (weeks)
- Feature 015: Update persistence (weeks)
- **Problem**: No value until all three done

**Vertical slice approach (accepted)**:
- Feature 013: Review Queue (view candidates) - Vue + API + Persistence
- Feature 014: Candidate Enhancement (edit/save) - Vue + API + Persistence
- Feature 015: Contact Intelligence (AI extraction) - Vue + API + Persistence
- Feature 016: Approval Workflow (approve/reject) - Vue + API + Persistence
- Feature 017: Statistics Dashboard (metrics) - Vue + API + Persistence
- Feature 018: Domain Management (blacklist) - Vue + API + Persistence

**Why better**:
- ✅ Each feature delivers working value
- ✅ Can use system after Feature 013
- ✅ Features are independent (can reorder)
- ✅ Matches how users think ("I want to review candidates")

---

## UI Design (PrimeVue Components)

### Review Queue Page (Primary Interface)

**Route**: `/admin/queue`
**Components**: PrimeVue DataTable, Dropdown filters, Pagination

```
┌──────────────────────────────────────────────────────┐
│ Filters: Status [▼] Confidence [▼] Source [▼]       │
├──────────────────────────────────────────────────────┤
│ ⭐ 0.87 │ ec.europa.eu/research                     │
│ PENDING │ Horizon Europe Research Grants            │
│ SearXNG │ [View] [Enhance] [Approve] [Reject]       │
├──────────────────────────────────────────────────────┤
│ ⭐ 0.75 │ us-bulgaria.org/grants                    │
│ PENDING │ America for Bulgaria Foundation           │
│ Tavily  │ [View] [Enhance] [Approve] [Reject]       │
└──────────────────────────────────────────────────────┘
```

**API**: `GET /api/candidates?status=PENDING&page=1`

---

### Candidate Enhancement Page (Critical Workflow)

**Route**: `/admin/candidates/:id`
**Components**: Input forms, Contact sections, AI Extract button

```
┌──────────────────────────────────────────────────────┐
│ Discovery Metadata                                   │
│   URL, Confidence, Source, Session                   │
├──────────────────────────────────────────────────────┤
│ Organization & Program                               │
│   [European Commission Research            ]         │
│   [Horizon Europe                          ]         │
│   Funding Type [EU_INSTITUTION ▼]                   │
├──────────────────────────────────────────────────────┤
│ Contact Intelligence ───────── [🤖 AI Extract]       │
│   Contact 1:                                         │
│     Name:  [Dr. Maria Schmidt              ]         │
│     Email: [maria@ec.europa.eu             ]         │
│     Phone: [+32 2 123 4567                 ]         │
│     Role:  [Program Manager ▼]  [Remove]            │
│   [+ Add Contact]                                    │
├──────────────────────────────────────────────────────┤
│ [Reject] [Save Draft] [Approve & Publish]           │
└──────────────────────────────────────────────────────┘
```

**APIs**:
- `GET /api/candidates/:id`
- `PUT /api/candidates/:id`
- `POST /api/candidates/:id/extract-contacts` (AI-powered)

---

## DTO Design Pattern

### Example: Candidate DTO

**Java (northstar-rest-api/dto/CandidateDTO.java)**:
```java
public record CandidateDTO(
    String id,              // UUID → String
    String url,
    String title,
    String description,
    String confidenceScore, // BigDecimal → String
    String status,          // Enum → String
    String sessionId,       // UUID → String
    String createdAt        // LocalDateTime → ISO-8601
) {}
```

**TypeScript (northstar-admin-ui/src/types/candidate.ts)**:
```typescript
export interface Candidate {
  id: string;
  url: string;
  title: string;
  description: string;
  confidenceScore: string;
  status: CandidateStatus;
  sessionId: string;
  createdAt: string;
}

export type CandidateStatus =
  | 'PENDING_CRAWL'
  | 'CRAWLED'
  | 'ENHANCED'
  | 'APPROVED';
```

**Mapper (northstar-rest-api/mapper/CandidateDTOMapper.java)**:
```java
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
}
```

---

## Project Structure Changes

### Archived Old Implementations

**Action**: Moved old frontend attempts to `archived-frontends/`

```bash
archived-frontends/
├── backend-springboot-monolith/  # Old Spring Boot attempt
└── frontend-streamlit/            # Old Streamlit Python attempt
```

**Updated .gitignore**: Added `archived-frontends/`

**Reason**: Starting fresh with Vue 3 + PrimeVue. Keep old code for reference but don't clutter main directory.

---

### New Frontend Module

**Location**: `northstar-admin-ui/` (sibling to northstar-* Java modules)

**Structure**:
```
northstar-admin-ui/
├── package.json          # Node dependencies
├── vite.config.ts        # Build config + CORS proxy
├── src/
│   ├── main.ts           # Vue entry point
│   ├── router/           # Vue Router
│   ├── views/            # Page components
│   ├── components/       # Reusable components
│   ├── services/         # API client (Axios)
│   ├── types/            # TypeScript interfaces (DTOs)
│   └── stores/           # Pinia state management
└── dist/                 # Build output (gitignored)
```

**Critical**: NOT part of Maven build. Separate npm/pnpm build system.

---

## Development Workflow

### Running Both Systems

**Terminal 1: Backend (Maven)**
```bash
cd /Users/kevin/github/northstar-funding
mvn spring-boot:run -pl northstar-rest-api
# → http://localhost:8080
```

**Terminal 2: Frontend (npm)**
```bash
cd /Users/kevin/github/northstar-funding/northstar-admin-ui
npm run dev
# → http://localhost:5173
```

**Vite proxy**: Forwards `/api/*` to `localhost:8080` (handles CORS)

---

### Making Changes

**Backend (Java)**:
1. Edit `northstar-rest-api/src/main/java/.../controller/CandidateController.java`
2. Spring Boot auto-reloads
3. Test: `curl http://localhost:8080/api/candidates`

**Frontend (Vue)**:
1. Edit `northstar-admin-ui/src/views/ReviewQueue.vue`
2. Vite hot-reloads
3. See changes instantly in browser

**DTO sync** (critical):
1. Update `CandidateDTO.java`
2. Update `Candidate.ts` (must mirror)
3. Update mapper if needed
4. Test API contract

---

## Security & Authentication

### Phase 1: Development (Now)

**No authentication** - runs on `localhost:5173`

**Why safe**:
- Only Kevin/Huw have access
- Local development machine
- Not publicly accessible
- Fast iteration velocity

### Phase 2: Production (Later)

**Add before deploying**:
- Spring Security with JWT tokens
- Login page in Vue
- Role-based access control (use existing `admin_user` table)
- `AdminRole` enum already defined (SUPER_ADMIN, CONTENT_MANAGER, READ_ONLY)

---

## REST API Endpoints (To Be Implemented)

### Candidates

```
GET    /api/candidates
  ?status=PENDING&confidence=0.70&page=1&size=20
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
  Side-effect: Blacklists domain

POST   /api/candidates/{id}/extract-contacts
  Response: List<ContactDTO> (AI-powered extraction)
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
  ?days=30
  Response: { dates: [], counts: [] }
```

### Domains

```
GET    /api/domains
  ?status=BLACKLISTED
  Response: DomainDTO[]

POST   /api/domains/blacklist
  Request: { domain: string, reason: string }
  Response: DomainDTO

DELETE /api/domains/{id}/blacklist
  Response: void
```

---

## Feature Roadmap (013-018)

### Feature 013: Review Queue - View Candidates ← **NEXT**
- Vue: ReviewQueue.vue with DataTable
- REST: GET /api/candidates (pagination, filters)
- Persistence: Already exists (CandidateRepository)

### Feature 014: Candidate Enhancement - Edit & Save
- Vue: CandidateDetail.vue with forms
- REST: GET /api/candidates/{id}, PUT /api/candidates/{id}
- Persistence: Update logic + enhancement tracking

### Feature 015: Contact Intelligence - AI Extraction
- Vue: Contact form with "AI Extract" button
- REST: POST /api/candidates/{id}/extract-contacts (Ollama)
- Persistence: ContactIntelligence CRUD

### Feature 016: Approval Workflow - Approve/Reject
- Vue: Approve/Reject buttons with confirmation
- REST: POST /api/candidates/{id}/approve, POST /api/candidates/{id}/reject
- Persistence: Status updates, blacklist on reject

### Feature 017: Statistics Dashboard
- Vue: Dashboard.vue with Chart.js
- REST: GET /api/statistics/overview, GET /api/statistics/trends
- Persistence: Aggregate queries

### Feature 018: Domain Management
- Vue: DomainManagement.vue with domain table
- REST: GET /api/domains, POST /api/domains/blacklist
- Persistence: Domain CRUD, blacklist updates

---

## Files Created

**Architecture Document**:
- `northstar-notes/architecture/admin-dashboard-architecture.md` ✅

**Session Summary**:
- `northstar-notes/session-summaries/2025-11-16-admin-dashboard-planning.md` ✅ (this file)

**Project Changes**:
- Archived: `backend/` → `archived-frontends/backend-springboot-monolith/`
- Archived: `frontend/` → `archived-frontends/frontend-streamlit/`
- Updated: `.gitignore` to exclude `archived-frontends/`

---

## Next Steps

**Immediate (Today)**:
1. ✅ Update CLAUDE.md with Admin Dashboard section
2. ✅ Run `/specify` for Feature 013 (Review Queue)

**Next Session**:
3. ⏳ Run `/plan` for Feature 013
4. ⏳ Review plan together
5. ⏳ Run `/implement` to build Review Queue

---

## Key Insights

### 1. Three Frontends, Not One

**Realization**: NorthStar needs three completely different UIs:
- Admin dashboard (internal tool for Kevin/Huw)
- Client dashboard (SaaS product for customers)
- Public website (marketing site)

**Impact**: Don't try to build a universal UI. Optimize each for its purpose.

---

### 2. Admin Dashboard is Most Critical

**Why**: Without it, the human-AI hybrid model cannot function. AI discovers candidates, but humans must review/enhance/approve them.

**Priority**: Build admin dashboard BEFORE client dashboard or public website.

---

### 3. Vertical Slices Deliver Value Faster

**Traditional**: Build all of Vue, then all of REST API, then persistence
**Problem**: No working system until everything is done

**Vertical slices**: Each feature works end-to-end
**Benefit**: Can use system after Feature 013 (even if limited)

---

### 4. DTOs Prevent Tight Coupling

**Without DTOs**: Frontend depends directly on domain entities
**Problem**: Backend changes break frontend, or backend becomes frozen

**With DTOs**: API contract is stable, domain can evolve independently
**Benefit**: Frontend and backend can change without coordination

---

### 5. PrimeVue Saves Weeks of Development

**Without PrimeVue**: Build DataTable, filters, pagination, forms from scratch
**Effort**: 2-3 weeks just for Review Queue

**With PrimeVue**: Use pre-built DataTable, Dropdown, Calendar, etc.
**Effort**: 2-3 days for Review Queue

**Conclusion**: Component library choice matters significantly for admin UIs

---

## Lessons Learned

### 1. Start with Architecture, Not Implementation

**What we did**: Spent 2 hours designing architecture, data flow, feature breakdown
**Why valuable**: Clear plan prevents rework later

**Alternative**: Jump into coding Vue components
**Risk**: Realize halfway through that DTO design is wrong, or data flow doesn't work

---

### 2. Archive Old Attempts, Don't Delete

**Decision**: Move `backend/` and `frontend/` to `archived-frontends/`
**Benefit**: Can reference old code if needed, but doesn't clutter main directory

**Alternative**: Delete immediately
**Risk**: Might have forgotten useful patterns or configurations

---

### 3. Feature Naming Matters

**Pattern**: `013-admin-dashboard-review-queue`
**Why good**:
- Prefix groups related features
- Suffix describes user action
- Easy to find in file system

**Bad pattern**: `013-ui-feature`, `014-more-ui`
**Why bad**: No context about what user can do

---

### 4. Spec-Kit Process Works for Multi-Layer Features

**Challenge**: How to use `/specify` for features spanning Vue + REST API + Persistence?

**Solution**: Describe complete user workflow in specification
- "Kevin views candidates in a table" (implies Vue component)
- "System filters by status and confidence" (implies REST API query params)
- "System retrieves from database" (implies repository query)

**Benefit**: `/plan` generates tasks across all layers automatically

---

## Related Documentation

**Architecture**:
- `northstar-notes/architecture/admin-dashboard-architecture.md` - Complete design (created today)

**Business Context**:
- `northstar-notes/session-summaries/2025-10-31-business-model-clarification.md` - Human-AI hybrid rationale

**Previous Features**:
- `specs/012-refactor-searchresultprocessor-to/` - Search result processing pipeline
- `specs/009-create-kafka-based/` - Kafka workflow (incomplete)

---

**Status**: ✅ Planning Complete - Ready for Feature 013 specification

**Duration**: ~2 hours (architecture design, decision documentation, project setup)

**Outcome**: Clear path forward with vertical slice features 013-018
