# Quickstart: Admin Dashboard Review Queue

**Feature**: 013 - Admin Dashboard Review Queue
**Date**: 2025-11-16

## Overview

This quickstart guide walks through the development workflow for Feature 013, from initial setup to verification. Use this as a checklist during implementation and testing.

## Prerequisites

### Required Software

- ✅ Java 25 (Oracle JDK via SDKMAN)
- ✅ Maven 3.9+ (for backend builds)
- ✅ Node.js 20+ (for frontend development)
- ✅ pnpm or npm (package manager for Vue)
- ✅ PostgreSQL 16 running at 192.168.1.10:5432
- ✅ Git (for version control)

### Required Environment

- ✅ MacBook M2 (development machine)
- ✅ Mac Studio @ 192.168.1.10 (PostgreSQL server)
- ✅ Database: `northstar_funding`
- ✅ Branch: `013-create-admin-dashboard`

### Verify Prerequisites

```bash
# Check Java version
java -version
# Expected: java version "25" 2025-09-16

# Check Maven version
mvn -version
# Expected: Apache Maven 3.9.x

# Check Node.js version
node --version
# Expected: v20.x.x or higher

# Check pnpm version (or npm)
pnpm --version
# Expected: 8.x.x or higher

# Check PostgreSQL connection
psql -h 192.168.1.10 -U northstar_user -d northstar_funding -c "SELECT version();"
# Expected: PostgreSQL 16.x
```

## Phase 1: Backend Setup (northstar-rest-api)

### Step 1.1: Create Maven Module

```bash
# From repository root
cd /Users/kevin/github/northstar-funding

# Create module directory
mkdir -p northstar-rest-api/src/main/java/com/northstar/funding/rest
mkdir -p northstar-rest-api/src/main/resources
mkdir -p northstar-rest-api/src/test/java/com/northstar/funding/rest

# Create pom.xml (see data-model.md for full content)
# Add to parent pom.xml <modules> section
```

### Step 1.2: Create Database Indexes (V18 Migration)

```bash
# Create migration file
cat > northstar-persistence/src/main/resources/db/migration/V18__add_candidate_indexes.sql << 'EOF'
-- V18__add_candidate_indexes.sql
-- Performance indexes for candidate filtering and sorting

CREATE INDEX idx_candidate_status
  ON funding_source_candidate(status);

CREATE INDEX idx_candidate_confidence
  ON funding_source_candidate(confidence_score);

CREATE INDEX idx_candidate_created_at
  ON funding_source_candidate(created_at);

CREATE INDEX idx_candidate_search_engine
  ON funding_source_candidate(search_engine);
EOF

# Run migration
mvn flyway:migrate -pl northstar-persistence

# Verify indexes created
psql -h 192.168.1.10 -U northstar_user -d northstar_funding \
  -c "\d funding_source_candidate"
# Expected: See 4 new indexes listed
```

### Step 1.3: Create DTOs and Mapper

```bash
# Create DTO package
mkdir -p northstar-rest-api/src/main/java/com/northstar/funding/rest/dto

# Create CandidateDTO.java (see data-model.md)
# Create CandidatePageDTO.java (see data-model.md)
# Create CandidateDTOMapper.java (see data-model.md)
```

### Step 1.4: Create Service Layer

```bash
# Create service package
mkdir -p northstar-rest-api/src/main/java/com/northstar/funding/rest/service

# Create CandidateService.java
# - Inject FundingSourceCandidateRepository
# - Inject DomainRepository
# - Inject CandidateDTOMapper
# - Implement listCandidates(filters, pageable)
# - Implement approveCandidate(id)
# - Implement rejectCandidate(id)
```

### Step 1.5: Create REST Controller

```bash
# Create controller package
mkdir -p northstar-rest-api/src/main/java/com/northstar/funding/rest/controller

# Create CandidateController.java
# - @RestController
# - @RequestMapping("/api/candidates")
# - GET / (list with filters)
# - PUT /{id}/approve
# - PUT /{id}/reject
```

### Step 1.6: Configure CORS

```bash
# Create config package
mkdir -p northstar-rest-api/src/main/java/com/northstar/funding/rest/config

# Create CorsConfig.java
# - @Configuration
# - implements WebMvcConfigurer
# - Allow http://localhost:5173
# - Allow GET, POST, PUT, DELETE, OPTIONS
```

### Step 1.7: Create Spring Boot Application

```bash
# Create NorthstarRestApiApplication.java
# - @SpringBootApplication
# - main() method to run Spring Boot

# Create application.yml
# - server.port: 8080
# - spring.datasource.url: jdbc:postgresql://192.168.1.10:5432/northstar_funding
# - spring.datasource.username: northstar_user
# - spring.datasource.password: northstar_password
```

### Step 1.8: Build and Run Backend

```bash
# Compile (should succeed)
mvn clean compile -pl northstar-rest-api

# Run tests (unit tests with Mockito)
mvn test -pl northstar-rest-api

# Run Spring Boot application
mvn spring-boot:run -pl northstar-rest-api

# Expected output:
# Started NorthstarRestApiApplication in X.XXX seconds
# Tomcat started on port(s): 8080 (http)
```

### Step 1.9: Verify Backend Endpoints

```bash
# Test GET /api/candidates (in new terminal, leave server running)
curl http://localhost:8080/api/candidates

# Expected: JSON response with CandidatePage structure
# {
#   "content": [...],
#   "totalElements": 0,
#   "totalPages": 0,
#   "currentPage": 0,
#   "pageSize": 20
# }

# Test with filters
curl "http://localhost:8080/api/candidates?status=PENDING_CRAWL&minConfidence=0.70"

# Test approve endpoint (replace {id} with actual UUID from database)
curl -X PUT http://localhost:8080/api/candidates/{id}/approve

# Test reject endpoint
curl -X PUT http://localhost:8080/api/candidates/{id}/reject
```

---

## Phase 2: Frontend Setup (northstar-admin-dashboard)

### Step 2.1: Initialize Vue Project

```bash
# From repository root
cd /Users/kevin/github/northstar-funding

# Create Vue project with Vite + TypeScript
pnpm create vite northstar-admin-dashboard --template vue-ts

# Or with npm:
npm create vite@latest northstar-admin-dashboard -- --template vue-ts

cd northstar-admin-dashboard
```

### Step 2.2: Install Dependencies

```bash
# Install PrimeVue, Pinia, Vue Router, Axios
pnpm add primevue@^3.50.0 primeicons@^6.0.1
pnpm add pinia@^2.1.0
pnpm add vue-router@^4.0.0
pnpm add axios@^1.6.0

# Or with npm:
npm install primevue@^3.50.0 primeicons@^6.0.1
npm install pinia@^2.1.0
npm install vue-router@^4.0.0
npm install axios@^1.6.0

# Install dev dependencies
pnpm add -D @types/node

# Verify package.json has all dependencies
cat package.json
```

### Step 2.3: Configure Environment Variables

```bash
# Create .env.development
cat > .env.development << 'EOF'
VITE_API_BASE_URL=http://localhost:8080/api
EOF

# Create .env.production (for future deployment)
cat > .env.production << 'EOF'
VITE_API_BASE_URL=http://production-url/api
EOF
```

### Step 2.4: Configure Vite

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173
  }
})
```

### Step 2.5: Setup PrimeVue

```typescript
// src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ConfirmationService from 'primevue/confirmationservice'
import ToastService from 'primevue/toastservice'

import 'primevue/resources/themes/aura-light-green/theme.css'
import 'primevue/resources/primevue.min.css'
import 'primeicons/primeicons.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(PrimeVue)
app.use(ConfirmationService)
app.use(ToastService)

app.mount('#app')
```

### Step 2.6: Create TypeScript Types

```bash
# Create types directory
mkdir -p src/types

# Create Candidate.ts (see data-model.md)
# Create CandidatePage.ts (see data-model.md)
# Create CandidateStatus.ts (see data-model.md)
# Create SearchEngineType.ts (see data-model.md)
```

### Step 2.7: Create Axios API Service

```bash
# Create services directory
mkdir -p src/services

# Create api.ts (see data-model.md for pattern)
```

### Step 2.8: Create Pinia Store

```bash
# Create stores directory
mkdir -p src/stores

# Create candidateStore.ts
# - State: candidates, totalElements, currentPage, pageSize, filters
# - Actions: fetchCandidates, approveCandidate, rejectCandidate
```

### Step 2.9: Create Vue Components

```bash
# Create views directory
mkdir -p src/views

# Create ReviewQueue.vue (main component)
# - PrimeVue DataTable
# - Filter controls (status, confidence, engine, dates)
# - Quick action buttons (View, Enhance, Approve, Reject)
# - Confirmation dialogs
# - Toast notifications

# Create CandidateDetail.vue (placeholder)
# Create CandidateEnhance.vue (placeholder)

# Create components directory (optional)
mkdir -p src/components
# Create FilterBar.vue (filter controls component)
# Create CandidateActions.vue (action buttons component)
```

### Step 2.10: Setup Vue Router

```bash
# Create router directory
mkdir -p src/router

# Create index.ts
# - Route: / → ReviewQueue.vue
# - Route: /candidates/:id → CandidateDetail.vue
# - Route: /candidates/:id/enhance → CandidateEnhance.vue
```

### Step 2.11: Run Frontend Development Server

```bash
# From northstar-admin-dashboard directory
pnpm dev

# Or with npm:
npm run dev

# Expected output:
# VITE v5.x.x  ready in XXX ms
# ➜  Local:   http://localhost:5173/
# ➜  Network: use --host to expose
```

---

## Phase 3: Integration Testing

### Step 3.1: Start Both Servers

```bash
# Terminal 1: Start Spring Boot API
cd /Users/kevin/github/northstar-funding
mvn spring-boot:run -pl northstar-rest-api

# Terminal 2: Start Vue dev server
cd /Users/kevin/github/northstar-funding/northstar-admin-dashboard
pnpm dev

# Expected: Both servers running simultaneously
# Spring Boot: http://localhost:8080
# Vue: http://localhost:5173
```

### Step 3.2: Open Dashboard in Browser

```bash
# Open browser
open http://localhost:5173

# Or manually navigate to http://localhost:5173
```

### Step 3.3: Verify Core Functionality

**Test Checklist**:

- [ ] **Page loads** in <2 seconds
- [ ] **Table displays** candidates (if database has data)
- [ ] **Pagination** works (first, previous, next, last buttons)
- [ ] **Page size selector** works (10, 20, 50, 100)
- [ ] **Filter by status** works (multi-select dropdown)
- [ ] **Filter by confidence** works (All, High ≥0.80, Medium ≥0.70, Low ≥0.60)
- [ ] **Filter by search engine** works (multi-select)
- [ ] **Date range filter** works
- [ ] **Sort by column** works (click column headers)
- [ ] **Clear filters** button resets all filters
- [ ] **Color-coded confidence scores** (green ≥0.80, yellow 0.70-0.79, orange 0.60-0.69)
- [ ] **View button** navigates to placeholder page
- [ ] **Enhance button** navigates to placeholder page
- [ ] **Approve button** shows confirmation dialog
- [ ] **Approve confirms** updates status to APPROVED, shows success toast
- [ ] **Reject button** shows confirmation dialog
- [ ] **Reject confirms** updates status to REJECTED, shows success toast, blacklists domain
- [ ] **Empty state** shows when no candidates match filters
- [ ] **Loading spinner** shows during API calls
- [ ] **Error toast** shows on API failures

### Step 3.4: Performance Verification

```bash
# Measure page load time (Chrome DevTools)
# 1. Open Chrome DevTools (F12)
# 2. Go to Network tab
# 3. Reload page (Cmd+R)
# 4. Check "Finish" time in bottom left
# Expected: <2 seconds

# Measure filter response time
# 1. Apply a filter (e.g., status=PENDING_CRAWL)
# 2. Check Network tab for /api/candidates request
# 3. Check "Time" column
# Expected: <1 second
```

### Step 3.5: Database Verification

```bash
# Verify approve action updated database
psql -h 192.168.1.10 -U northstar_user -d northstar_funding \
  -c "SELECT id, status FROM funding_source_candidate WHERE status = 'APPROVED';"

# Expected: See approved candidate

# Verify reject action updated candidate and domain
psql -h 192.168.1.10 -U northstar_user -d northstar_funding \
  -c "SELECT id, status FROM funding_source_candidate WHERE status = 'REJECTED';"

psql -h 192.168.1.10 -U northstar_user -d northstar_funding \
  -c "SELECT domain_name, status FROM domain WHERE status = 'BLACKLISTED';"

# Expected: See rejected candidate and blacklisted domain
```

---

## Phase 4: Testing

### Step 4.1: Backend Unit Tests

```bash
# Run all northstar-rest-api tests
mvn test -pl northstar-rest-api

# Run specific test class
mvn test -Dtest=CandidateServiceTest -pl northstar-rest-api
mvn test -Dtest=CandidateDTOMapperTest -pl northstar-rest-api

# Expected: All tests pass
```

### Step 4.2: Frontend Unit Tests (Optional)

```bash
# From northstar-admin-dashboard directory
pnpm test

# Or with npm:
npm test

# Expected: All tests pass (if tests written)
```

### Step 4.3: Contract Tests (Optional)

```bash
# Ensure Spring Boot API is running
# Run contract tests (see contracts/README.md)
```

---

## Phase 5: Documentation Update

### Step 5.1: Update CLAUDE.md

```bash
# Run update-agent-context.sh script
.specify/scripts/bash/update-agent-context.sh claude

# Expected: CLAUDE.md updated with Feature 013 info
```

### Step 5.2: Create README for Vue Project

```bash
# Create northstar-admin-dashboard/README.md
cat > northstar-admin-dashboard/README.md << 'EOF'
# NorthStar Admin Dashboard

Vue 3 + TypeScript + PrimeVue admin dashboard for NorthStar Funding Discovery.

## Setup

```bash
pnpm install
```

## Development

```bash
# Start Vue dev server (port 5173)
pnpm dev

# Ensure Spring Boot API is running (port 8080)
cd .. && mvn spring-boot:run -pl northstar-rest-api
```

## Build

```bash
pnpm build
```

## Tech Stack

- Vue 3.4+ with TypeScript
- Vite 5.0 (build tool)
- PrimeVue 3.50 (UI components)
- Pinia 2.1 (state management)
- Vue Router 4.0 (routing)
- Axios 1.6 (HTTP client)
EOF
```

---

## Troubleshooting

### Backend Issues

**Problem**: Maven build fails
```bash
# Solution: Clean and rebuild
mvn clean compile -pl northstar-rest-api
```

**Problem**: Spring Boot can't connect to database
```bash
# Solution: Verify PostgreSQL is running and accessible
psql -h 192.168.1.10 -U northstar_user -d northstar_funding -c "SELECT 1;"
```

**Problem**: CORS errors in browser console
```bash
# Solution: Verify CorsConfig allows http://localhost:5173
# Check CorsConfig.java
```

### Frontend Issues

**Problem**: Vue dev server won't start
```bash
# Solution: Delete node_modules and reinstall
rm -rf node_modules pnpm-lock.yaml
pnpm install
```

**Problem**: API calls fail (404, 500 errors)
```bash
# Solution: Verify Spring Boot is running
curl http://localhost:8080/api/candidates

# Check VITE_API_BASE_URL in .env.development
cat .env.development
```

**Problem**: PrimeVue components not rendering
```bash
# Solution: Verify PrimeVue CSS imports in main.ts
# Ensure theme CSS is imported before primevue.min.css
```

### Performance Issues

**Problem**: Page loads slowly (>2 seconds)
```bash
# Solution: Check database indexes
psql -h 192.168.1.10 -U northstar_user -d northstar_funding \
  -c "\d funding_source_candidate"

# Verify 4 indexes exist (status, confidence_score, created_at, search_engine)
```

---

## Success Criteria

Feature 013 is complete when all of these are true:

- [x] Spring Boot API runs on port 8080
- [x] Vue dev server runs on port 5173
- [x] Page loads in <2 seconds with 1000+ candidates
- [x] All filters work and respond in <1 second
- [x] Approve/reject actions work correctly
- [x] Domain blacklist works on reject
- [x] All 43 functional requirements implemented
- [x] Backend unit tests pass
- [x] No console errors in browser
- [x] CLAUDE.md updated

---

## Next Steps

After Feature 013 is complete:
- **Feature 014**: Candidate Detail View (view full candidate information)
- **Feature 015**: Candidate Enhancement Form (add contact intelligence)
- **Feature 016**: Contact Intelligence Extraction (AI-powered)
- **Feature 017**: Statistics Dashboard (metrics and charts)
- **Feature 018**: Domain Management UI (blacklist management)
