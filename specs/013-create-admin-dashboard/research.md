# Research: Admin Dashboard Review Queue

**Feature**: 013 - Admin Dashboard Review Queue
**Date**: 2025-11-16
**Status**: Complete

## Overview

All technical decisions for this feature were resolved from the Admin Dashboard Architecture document created on 2025-11-16. This research document consolidates those decisions with rationale and alternatives considered.

## Technology Decisions

###  1. Frontend Framework: Vue 3.4+ with TypeScript

**Decision**: Use Vue 3.4+ with TypeScript for admin dashboard frontend

**Rationale**:
- **PrimeVue DataTable**: Exceptional component for admin UIs with built-in pagination, sorting, filtering
- **Simpler than React**: Less boilerplate, gentler learning curve
- **TypeScript-first**: Excellent TypeScript support with Vue 3 Composition API
- **Official ecosystem**: Vite, Vue Router, Pinia all officially maintained
- **Performance**: Virtual DOM with excellent reactivity system

**Alternatives Considered**:
1. **React + Ant Design**
   - Rejected: More complex, larger ecosystem, steeper learning curve
   - More boilerplate for state management
2. **Spring MVC + Thymeleaf**
   - Rejected: Server-side rendering limits interactivity
   - Poor UX for real-time filtering/sorting
   - Harder to achieve <1 second response times
3. **Svelte + SvelteKit**
   - Rejected: Smaller ecosystem, fewer enterprise-grade UI libraries
   - PrimeVue DataTable has no Svelte equivalent

**References**:
- Vue 3 docs: https://vuejs.org/guide/introduction.html
- PrimeVue: https://primevue.org/

---

### 2. UI Component Library: PrimeVue 3.50 (FREE/MIT)

**Decision**: Use PrimeVue 3.50 FREE components only (no paid add-ons)

**Rationale**:
- **FREE MIT License**: No vendor lock-in, no licensing costs
- **DataTable Excellence**: Best-in-class table component with:
  - Built-in pagination (first, previous, next, last)
  - Built-in sorting (click column headers)
  - Built-in filtering (column filters)
  - Server-side pagination support (lazy loading)
  - Template slots for custom rendering (color-coded confidence scores)
- **Complete Component Suite**: All components needed for Feature 013:
  - DataTable, Column, Paginator
  - Dropdown, MultiSelect (filters)
  - Calendar (date range picker)
  - Button, ConfirmDialog, Toast
- **Professional Design**: Aura Light theme looks modern, clean
- **TypeScript Support**: Full TypeScript definitions included

**Alternatives Considered**:
1. **Ant Design Vue**
   - Rejected: Heavier bundle size, more complex API
   - Less intuitive DataTable configuration
2. **Element Plus**
   - Rejected: Table component less feature-rich than PrimeVue
   - Weaker TypeScript support
3. **Vuetify**
   - Rejected: Material Design aesthetic (not preferred)
   - Larger bundle size
4. **PrimeVue PREMIUM (paid add-ons)**
   - Deferred: Not needed for admin dashboard
   - May consider for client-facing frontend (Feature 019+)

**References**:
- PrimeVue DataTable: https://primevue.org/datatable/
- PrimeVue FREE vs PREMIUM: https://primevue.org/premium

---

### 3. State Management: Pinia 2.1

**Decision**: Use Pinia 2.1 for centralized state management

**Rationale**:
- **Official Recommendation**: Vue.js team officially recommends Pinia over Vuex
- **TypeScript-First**: Designed for TypeScript from the ground up
- **Simpler API**: Less boilerplate than Vuex (no mutations, just actions)
- **Composition API Integration**: Works naturally with `<script setup>`
- **Modular**: Each store is independent module
- **DevTools**: Excellent Vue DevTools integration

**Alternatives Considered**:
1. **Vuex**
   - Rejected: Deprecated in favor of Pinia
   - More boilerplate (state, mutations, actions, getters)
   - Weaker TypeScript support
2. **Plain Composition API** (provide/inject)
   - Rejected: Insufficient for complex state like:
     - Candidate list (array)
     - Filter state (status, confidence, engine, dateRange)
     - Pagination state (currentPage, pageSize, totalElements)
     - Loading state, error state
   - No DevTools support for debugging

**Pattern for Feature 013**:
```typescript
// candidateStore.ts
export const useCandidateStore = defineStore('candidate', () => {
  // State
  const candidates = ref<Candidate[]>([])
  const totalElements = ref(0)
  const currentPage = ref(0)
  const filters = ref({ status: [], minConfidence: 0.00, ... })

  // Actions
  async function fetchCandidates() { ... }
  async function approveCandidate(id: string) { ... }
  async function rejectCandidate(id: string) { ... }

  return { candidates, totalElements, currentPage, filters, fetchCandidates, ... }
})
```

**References**:
- Pinia docs: https://pinia.vuejs.org/
- Why Pinia: https://pinia.vuejs.org/introduction.html#why-should-i-use-pinia

---

### 4. Build Tool: Vite 5.0

**Decision**: Use Vite 5.0 for Vue project build tool

**Rationale**:
- **Official Vue Build Tool**: Created by Evan You (Vue creator)
- **Fast HMR**: Hot Module Replacement in <100ms
- **Excellent TypeScript**: First-class TypeScript support, no config needed
- **Modern ESM**: Uses native ES modules for development
- **Fast Production Builds**: Rollup-based production builds with tree-shaking
- **Plugin Ecosystem**: Excellent Vue-specific plugins

**Alternatives Considered**:
1. **Webpack**
   - Rejected: Slower HMR (can be 1-2 seconds)
   - More complex configuration
   - Older architecture (predates ES modules)
2. **Rollup**
   - Rejected: Lower-level, requires more configuration
   - Vite uses Rollup internally for production builds

**Configuration**:
```typescript
// vite.config.ts
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173 // Vite default
  }
})
```

**References**:
- Vite docs: https://vite.dev/
- Why Vite: https://vite.dev/guide/why.html

---

### 5. HTTP Client: Axios 1.6

**Decision**: Use Axios 1.6 for HTTP requests to REST API

**Rationale**:
- **Mature Library**: Battle-tested, 100M+ weekly downloads
- **Excellent Error Handling**: Structured error objects, error interceptors
- **Interceptors**: Centralize config (base URL, headers, error handling)
- **TypeScript Support**: Full TypeScript definitions
- **Automatic JSON**: Automatically parses JSON responses
- **Request/Response Transforms**: Centralize data transformation

**Alternatives Considered**:
1. **Native Fetch API**
   - Rejected: Less features (no interceptors, manual JSON parsing)
   - More boilerplate for error handling
   - No request/response transforms
2. **Vue Apollo** (GraphQL)
   - Rejected: GraphQL not needed for this feature
   - REST API simpler for CRUD operations

**Pattern for Feature 013**:
```typescript
// services/api.ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // http://localhost:8080/api
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// Response interceptor for error handling
apiClient.interceptors.response.use(
  response => response,
  error => {
    // Centralized error handling
    console.error('API Error:', error.response?.data)
    return Promise.reject(error)
  }
)

export const getCandidates = (params: CandidateFilters): Promise<CandidatePage> =>
  apiClient.get('/candidates', { params }).then(res => res.data)
```

**References**:
- Axios docs: https://axios-http.com/docs/intro
- Axios interceptors: https://axios-http.com/docs/interceptors

---

### 6. DTO Pattern: Java Records with Primitive Types

**Decision**: Use Java records for DTOs with all primitive types (UUID→String, BigDecimal→String, Enum→String)

**Rationale**:
- **Immutability**: Records are immutable by default (thread-safe)
- **Concise Syntax**: No boilerplate (auto-generates constructor, equals, hashCode, toString)
- **Clear API Contract**: DTOs define REST API schema
- **Type Safety**: No accidental modification of DTOs
- **JSON Compatibility**: Primitive types serialize/deserialize cleanly with Jackson
- **No Precision Issues**: String representation avoids floating-point errors
- **Frontend Compatibility**: TypeScript receives simple types (no complex parsing)

**Pattern**:
```java
// CandidateDTO.java
public record CandidateDTO(
    String id,                 // UUID toString()
    String url,
    String title,
    String confidenceScore,    // BigDecimal toString()
    String status,             // Enum.name()
    String searchEngine,       // Enum.name()
    String createdAt           // LocalDateTime to ISO-8601
) {}
```

**Conversion Rules**:
- `UUID` → `String` via `uuid.toString()` (36 chars: "123e4567-e89b-12d3-a456-426614174000")
- `BigDecimal` → `String` via `bigDecimal.toString()` (e.g., "0.85")
- `Enum` → `String` via `enum.name()` (e.g., "PENDING_CRAWL")
- `LocalDateTime` → `String` via `ISO_LOCAL_DATE_TIME` formatter (e.g., "2025-11-16T10:30:00")

**Alternatives Considered**:
1. **Expose Domain Entities Directly**
   - Rejected: Violates separation of concerns
   - Couples API to domain model (can't change domain without breaking API)
   - Leaks implementation details (internal IDs, audit fields)
2. **Custom DTO Classes** (not records)
   - Rejected: More boilerplate (getters, constructors, equals, hashCode)
   - Records are perfect for immutable data transfer objects
3. **Use Complex Types** (UUID, BigDecimal, LocalDateTime in JSON)
   - Rejected: Requires custom Jackson serializers/deserializers
   - Frontend must parse UUIDs and BigDecimals
   - More complex, error-prone

**Mapper Service Pattern**:
```java
@Service
public class CandidateDTOMapper {
    public CandidateDTO toDTO(FundingSourceCandidate entity) {
        return new CandidateDTO(
            entity.getId().toString(),
            entity.getUrl(),
            entity.getTitle(),
            entity.getConfidenceScore().toString(),
            entity.getStatus().name(),
            entity.getSearchEngine().name(),
            entity.getCreatedAt().format(ISO_LOCAL_DATE_TIME)
        );
    }

    // No toDomain() needed - this feature only reads candidates
}
```

**References**:
- Java Records: https://docs.oracle.com/en/java/javase/25/language/records.html
- DTO Pattern: https://martinfowler.com/eaaCatalog/dataTransferObject.html

---

### 7. REST API Module: northstar-rest-api

**Decision**: Create new Maven module `northstar-rest-api` for REST API layer

**Rationale**:
- **Separation of Concerns**: REST API separate from persistence layer
- **Dependency Direction**: REST API depends on persistence, not vice versa
- **Clean Architecture**: API layer can change without affecting domain/persistence
- **Testability**: Can test REST endpoints in isolation with mocked services
- **Spring Boot Application**: Can run standalone for development/testing

**Alternatives Considered**:
1. **Add REST to northstar-persistence Module**
   - Rejected: Couples persistence and API concerns
   - Persistence layer should not know about HTTP/REST
   - Violates Single Responsibility Principle
2. **Separate Microservice**
   - Rejected: Over-engineering for Feature 013
   - Constitution forbids microservices (Principle X)
   - Adds deployment complexity (2 Spring Boot apps)

**Module Structure**:
```
northstar-rest-api/
├── pom.xml
│   Dependencies:
│   - northstar-persistence (compile)
│   - spring-boot-starter-web
│   - spring-boot-starter-test
│   - jackson-databind
├── src/main/java/com/northstar/funding/rest/
│   ├── NorthstarRestApiApplication.java  # @SpringBootApplication
│   ├── config/CorsConfig.java
│   ├── controller/CandidateController.java
│   ├── dto/CandidateDTO.java, CandidatePageDTO.java
│   ├── dto/CandidateDTOMapper.java
│   └── service/CandidateService.java
└── src/test/java/...
```

**References**:
- Maven multi-module projects: https://maven.apache.org/guides/mini/guide-multiple-modules.html
- Layered architecture: https://www.oreilly.com/library/view/software-architecture-patterns/9781491971437/ch01.html

---

### 8. CORS Configuration: Allow localhost:5173

**Decision**: Configure Spring Boot to allow CORS from `http://localhost:5173` (Vite dev server)

**Rationale**:
- **Development Workflow**: Vue runs on 5173, Spring Boot on 8080 (different origins)
- **Browser Security**: CORS prevents cross-origin requests by default
- **Simple Config**: Spring Web provides `WebMvcConfigurer` for CORS
- **No Security Risk**: Localhost only (Kevin and Huw)

**Pattern**:
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

**Future**: When deploying to production, update `allowedOrigins` to production Vue URL.

**References**:
- Spring CORS: https://spring.io/guides/gs/rest-service-cors
- CORS MDN: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS

---

### 9. Database Indexes: V18 Migration

**Decision**: Create Flyway migration V18 with indexes on: `status`, `confidence_score`, `created_at`, `search_engine`

**Rationale**:
- **Performance Goals**: <1 second for filter/sort operations
- **Query Patterns**: Administrators will filter/sort frequently on these columns
- **Database Optimization**: Indexes speed up WHERE and ORDER BY clauses
- **Minimal Overhead**: 4 indexes add <5MB storage for 10K candidates

**Indexes to Create**:
1. `idx_candidate_status` - Filter by status (PENDING_CRAWL, CRAWLED, APPROVED, etc.)
2. `idx_candidate_confidence` - Filter by confidence range (≥0.60, ≥0.70, ≥0.80)
3. `idx_candidate_created_at` - Sort by date descending (newest first), date range filters
4. `idx_candidate_search_engine` - Filter by search engine

**Migration File**:
```sql
-- V18__add_candidate_indexes.sql
CREATE INDEX idx_candidate_status
  ON funding_source_candidate(status);

CREATE INDEX idx_candidate_confidence
  ON funding_source_candidate(confidence_score);

CREATE INDEX idx_candidate_created_at
  ON funding_source_candidate(created_at);

CREATE INDEX idx_candidate_search_engine
  ON funding_source_candidate(search_engine);
```

**Performance Impact** (estimated):
- Without indexes: 500-1000ms for queries on 10K candidates
- With indexes: <50ms for same queries

**References**:
- PostgreSQL indexes: https://www.postgresql.org/docs/16/indexes.html
- Flyway migrations: https://flywaydb.org/documentation/concepts/migrations

---

### 10. No Authentication: Feature 013 Scope Constraint

**Decision**: Do NOT implement authentication for Feature 013

**Rationale**:
- **Localhost Only**: Dashboard accessed only from `http://localhost:5173` by Kevin and Huw
- **Development Phase**: Not deployed to Mac Studio yet
- **Incremental Delivery**: Authentication adds complexity (user accounts, sessions, passwords)
- **Future Feature**: Spring Security will be added in Feature 019+ (multi-user admin dashboard)
- **Security by Network**: Not exposed to network (localhost binding)

**Security Approach for Future**:
- Feature 019: Add Spring Security with:
  - User authentication (username/password)
  - Admin roles (ADMIN, REVIEWER, READ_ONLY)
  - Session management
  - CSRF protection

**Current Development Workflow**:
1. Start Spring Boot: `mvn spring-boot:run -pl northstar-rest-api` (port 8080)
2. Start Vue dev server: `cd northstar-admin-dashboard && pnpm dev` (port 5173)
3. Open browser: `http://localhost:5173`
4. No login required

**References**:
- Spring Security: https://spring.io/projects/spring-security
- Security considerations: Deferred to Feature 019

---

## Best Practices Research

### Vue 3 + TypeScript Best Practices

**Composition API with `<script setup>`**:
- Use `<script setup lang="ts">` for all Vue components
- More concise than Options API
- Better TypeScript inference

**Type Definitions**:
- Define interfaces in separate `/types` directory
- Reuse types across components and stores
- Mirror DTO structure exactly (field names, types)

**State Management**:
- Use Pinia stores for shared state (candidates, filters, pagination)
- Use local `ref()`/`reactive()` for component-only state (loading, modal visibility)
- Keep stores focused (one store per domain concept)

**PrimeVue DataTable**:
- Use `:value` prop for data binding
- Enable `:paginator="true"` for pagination
- Use `:lazy="true"` for server-side operations
- Bind `@page` event for page changes
- Bind `@sort` event for sort changes
- Use column templates for custom rendering

**References**:
- Vue 3 style guide: https://vuejs.org/style-guide/
- TypeScript with Vue: https://vuejs.org/guide/typescript/overview.html

---

### Spring Boot REST Best Practices

**Controller Pattern**:
- Controllers handle HTTP only, delegate to services for business logic
- Use `@RestController` + `@RequestMapping` for REST endpoints
- Return `ResponseEntity<T>` for explicit HTTP status codes
- Validate input with `@Valid` and `@RequestBody`

**Service Pattern**:
- Services contain business logic and transactions
- Use `@Transactional` for database operations
- Inject repositories via constructor (no `@Autowired`)
- Return domain entities from services

**DTO Pattern**:
- Controllers accept/return DTOs, never domain entities
- Separate mapper service for Domain ↔ DTO conversion
- DTOs use primitive types only

**Error Handling**:
- Use `@ControllerAdvice` for global exception handling
- Return meaningful HTTP status codes:
  - 200 OK: Success
  - 404 NOT FOUND: Candidate doesn't exist
  - 400 BAD REQUEST: Already approved/rejected
  - 500 INTERNAL SERVER ERROR: Unexpected error

**References**:
- Spring REST best practices: https://spring.io/guides/tutorials/rest
- Error handling: https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc

---

## Summary

All 10 technical decisions documented with clear rationale and alternatives considered. No NEEDS CLARIFICATION items remain.

**Key Decisions**:
1. Vue 3 + TypeScript for frontend
2. PrimeVue FREE components (especially DataTable)
3. Pinia for state management
4. Vite for build tool
5. Axios for HTTP client
6. Java records for DTOs (primitive types only)
7. New Maven module for REST API
8. CORS configuration for localhost:5173
9. Database indexes for performance
10. No authentication for Feature 013 (deferred to Feature 019)

**Next Phase**: Phase 1 (Design & Contracts) - Generate data-model.md, OpenAPI contracts, quickstart.md
