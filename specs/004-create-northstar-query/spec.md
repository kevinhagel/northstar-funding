# Feature Specification: AI-Powered Search Query Generation

**Feature Branch**: `004-create-northstar-query`
**Created**: 2025-11-02
**Status**: Draft
**Input**: User description: "Create northstar-query-generation module: AI-powered search query generator using LM Studio via LangChain4j with strategy pattern for provider-specific queries (keyword for Brave/Serper/SearXNG, AI-optimized for Tavily), Caffeine caching, async API with CompletableFuture, Virtual Threads support, selective PostgreSQL persistence"

## Execution Flow (main)
```
1. Parse user description from Input
   ’ SUCCESS: Description provided
2. Extract key concepts from description
   ’ Identified: AI query generation, multiple search providers, caching, persistence
3. For each unclear aspect:
   ’ MARKED: Several clarifications needed (see FR sections)
4. Fill User Scenarios & Testing section
   ’ SUCCESS: User flow defined
5. Generate Functional Requirements
   ’ SUCCESS: Requirements generated with testability
6. Identify Key Entities
   ’ SUCCESS: Entities identified
7. Run Review Checklist
   ’ WARN: Some [NEEDS CLARIFICATION] markers remain
8. Return: SUCCESS (spec ready for planning with clarifications)
```

---

## ¡ Quick Guidelines
-  Focus on WHAT users need and WHY
- L Avoid HOW to implement (no tech stack, APIs, code structure)
- =e Written for business stakeholders, not developers

---

## User Scenarios & Testing

### Primary User Story

The NorthStar funding discovery system needs to search multiple search engines (Brave Search, Serper, SearXNG, Tavily) to find potential funding sources. Before searching, the system must generate appropriate search queries tailored to each search engine's capabilities and optimized for different funding categories (e.g., scholarships, infrastructure grants, teacher development) and geographic regions (e.g., Bulgaria, Eastern Europe, EU).

**User Journey**:
1. System administrator schedules a funding discovery session targeting specific funding categories and geographic regions
2. System generates search queries optimized for each search provider
3. System caches queries to avoid regenerating identical queries
4. System executes searches using the generated queries
5. System stores high-quality queries for future reuse and analysis

### Acceptance Scenarios

1. **Given** the system needs to find scholarship opportunities in Bulgaria, **When** the administrator requests query generation for "INDIVIDUAL_SCHOLARSHIPS" category and "BULGARIA" geographic scope, **Then** the system generates multiple search queries appropriate for each search provider

2. **Given** the system has previously generated queries for a specific category and region combination, **When** the same request is made within 24 hours, **Then** the system returns cached queries without regenerating them

3. **Given** the system generates queries for traditional keyword-based search engines (Brave, Serper, SearXNG), **When** queries are generated, **Then** queries use precise keywords and terms that appear on actual funding websites

4. **Given** the system generates queries for AI-powered search (Tavily), **When** queries are generated, **Then** queries use natural language and contextual descriptions that help AI understand intent

5. **Given** the system needs to search across multiple providers simultaneously, **When** query generation is requested for all providers, **Then** the system generates queries for all providers in parallel without blocking

6. **Given** the AI query generation service becomes unavailable, **When** query generation is requested, **Then** the system provides fallback queries to allow searches to continue

### Edge Cases

- What happens when the AI service is unavailable or slow to respond?
  - System should provide hardcoded fallback queries for each provider type

- How does the system handle invalid or empty category/geographic combinations?
  - System should validate inputs and return clear error messages

- What happens when cache becomes full?
  - [NEEDS CLARIFICATION: Cache eviction policy - LRU, time-based, or size-based?]

- How does the system handle concurrent requests for the same query parameters?
  - System should deduplicate requests and share results

- What happens when generated queries are too long or contain invalid characters?
  - System should validate and sanitize queries before returning them

---

## Requirements

### Functional Requirements

**Query Generation**:
- **FR-001**: System MUST generate search queries based on funding categories and geographic regions
- **FR-002**: System MUST generate different query styles for keyword-based search engines (Brave Search, Serper, SearXNG) versus AI-powered search (Tavily)
- **FR-003**: System MUST generate a configurable number of queries per request (minimum 1, maximum [NEEDS CLARIFICATION: what's the reasonable upper limit? 50? 100?])
- **FR-004**: System MUST map all 25 funding categories to appropriate search terms
- **FR-005**: System MUST map all geographic scopes to appropriate location terms
- **FR-006**: System MUST support generating queries for multiple search providers simultaneously

**Caching**:
- **FR-007**: System MUST cache generated queries to avoid redundant AI generation
- **FR-008**: System MUST expire cached queries after 24 hours
- **FR-009**: System MUST track cache hit/miss statistics for monitoring
- **FR-010**: System MUST differentiate cached queries by provider, categories, geographic scope, and query count

**Persistence**:
- **FR-011**: System MUST save generated queries to persistent storage for analysis
- **FR-012**: System MUST associate saved queries with discovery sessions
- **FR-013**: System MUST record when queries were generated and which provider they target
- **FR-014**: System MUST support selective persistence of "high-quality" queries [NEEDS CLARIFICATION: how is "high quality" determined? Success rate? Manual curation?]

**Reliability**:
- **FR-015**: System MUST provide fallback queries when AI generation fails
- **FR-016**: System MUST handle AI generation timeouts gracefully [NEEDS CLARIFICATION: what is an acceptable timeout? 10s? 30s? 60s?]
- **FR-017**: System MUST validate generated queries before returning them
- **FR-018**: System MUST log all query generation requests and outcomes

**Performance**:
- **FR-019**: System MUST support non-blocking query generation for concurrent operations
- **FR-020**: System MUST generate queries for multiple providers in parallel
- **FR-021**: System MUST respond to cached query requests in under [NEEDS CLARIFICATION: target response time for cache hits? 100ms? 500ms?]

**Integration**:
- **FR-022**: System MUST be callable by other system components (orchestrator, scheduler)
- **FR-023**: System MUST use existing domain entities for categories and geographic scopes
- **FR-024**: System MUST use existing persistence layer for saving queries

### Non-Functional Requirements

**Scalability**:
- **NFR-001**: System MUST handle concurrent query generation requests without degradation
- **NFR-002**: Cache MUST support up to [NEEDS CLARIFICATION: expected cache size? 1000 entries? 10000?] unique query sets

**Maintainability**:
- **NFR-003**: System MUST allow easy addition of new search providers without modifying existing code
- **NFR-004**: System MUST allow updating category and geographic mappings without code changes [NEEDS CLARIFICATION: should mappings be configurable or code-based?]

**Observability**:
- **NFR-005**: System MUST log all query generation activities with appropriate detail levels
- **NFR-006**: System MUST expose cache statistics for monitoring
- **NFR-007**: System MUST track AI service availability and response times

**Data Quality**:
- **NFR-008**: Generated queries MUST be relevant to specified categories and regions
- **NFR-009**: Keyword queries MUST use terms that actually appear on funding websites
- **NFR-010**: AI-optimized queries MUST provide sufficient context for accurate search results

### Key Entities

- **Generated Query**: Represents a search query generated for a specific provider, containing the query text, target provider, associated categories, geographic scope, and generation timestamp

- **Query Cache Entry**: Represents a cached set of queries, identified by provider + categories + geographic scope + query count, with expiration time

- **Search Provider**: Represents a search engine that can execute queries (Brave Search, Serper, SearXNG, Tavily), each with different query optimization needs

- **Funding Category**: Represents a type of funding (e.g., scholarships, infrastructure grants, teacher development) - uses existing domain entity

- **Geographic Scope**: Represents a geographic region (e.g., Bulgaria, Eastern Europe, EU) - uses existing domain entity

- **Discovery Session**: Represents a funding discovery run that uses generated queries - uses existing domain entity

### Dependencies and Assumptions

**Dependencies**:
- System depends on existing domain model (FundingSearchCategory, GeographicScope)
- System depends on existing persistence layer (SearchQuery entity, SearchQueryRepository)
- System depends on AI service availability for query generation [NEEDS CLARIFICATION: what's the expected uptime? 99%? 95%?]

**Assumptions**:
- AI service (LM Studio) is running and accessible on local network
- Funding categories and geographic scopes are already defined in the domain model
- Search providers require different query styles (keyword vs. natural language)
- Queries generated for the same parameters within 24 hours can be reused
- High-quality queries can be identified for future reuse [NEEDS CLARIFICATION: mechanism unclear]

---

## Success Criteria

**Query Generation Quality**:
- Generated keyword queries use terms that match actual funding website content
- AI-optimized queries provide sufficient context for accurate search results
- Queries are relevant to specified categories and geographic regions

**Performance**:
- Cached query retrieval completes in under [NEEDS CLARIFICATION: target time?]
- Query generation for 4 providers completes in under [NEEDS CLARIFICATION: acceptable total time? 30s? 60s?]
- System handles concurrent requests without blocking

**Reliability**:
- System provides fallback queries when AI service unavailable
- Cache hit rate exceeds [NEEDS CLARIFICATION: target hit rate? 50%? 70%?] for typical usage patterns
- Zero data loss for persisted queries

**Integration**:
- Other system components can successfully call query generation
- Generated queries can be executed by search providers
- Queries are correctly associated with discovery sessions

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs) *Note: Template contained technical details from user input, but spec focuses on WHAT*
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain *WARN: Several clarifications needed*
- [x] Requirements are testable and unambiguous (except marked items)
- [ ] Success criteria are measurable *WARN: Need specific targets*
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (10 clarifications needed)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [ ] Review checklist passed (warnings present)

---

## Open Questions for Planning Phase

The following questions should be resolved during the planning phase based on technical constraints and design decisions:

1. What is the maximum number of queries per request? (FR-003)
2. How is "high quality" determined for selective persistence? (FR-014)
3. What is an acceptable AI generation timeout? (FR-016)
4. What is the target response time for cache hits? (FR-021)
5. What is the expected cache size? (NFR-002)
6. Should category/geographic mappings be configurable or code-based? (NFR-004)
7. What is the expected AI service uptime? (Dependencies)
8. What are the specific performance targets (cache retrieval time, total generation time)? (Success Criteria)
9. What is the target cache hit rate? (Success Criteria)
10. What is the cache eviction policy? (Edge Cases)

These questions do not block specification approval but should be answered during technical design.
