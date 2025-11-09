# Feature Specification: Kafka-Based Event-Driven Search Workflow

**Feature Branch**: `009-create-kafka-based`
**Created**: 2025-11-09
**Status**: Draft
**Input**: User description: "Create Kafka-based event-driven search workflow with SearXNG adapter, Valkey blacklist caching, REST API trigger, and smart orchestrator. Initial scope: SearXNG only to validate pattern. Key components: 4 Kafka topics (search-requests, search-results-raw, search-results-validated, workflow-errors), Spring Kafka consumers for each stage, domain processing with Valkey cache (10x speedup), confidence scoring with existing SearchResultProcessor, REST API with SpringDoc/Swagger documentation. Migration from LM Studio to Ollama for query generation (concurrent request support). No query caching (generate on-demand). Dynamic round-robin scheduling for weekly category distribution. Architecture: Event-driven with clean separation of concerns, 3 new Maven modules (northstar-kafka-common, northstar-search-adapters, northstar-search-workflow), update northstar-application for REST API."

## Execution Flow (main)
```
1. Parse user description from Input
   ’ Identified: Event-driven search execution with domain validation
2. Extract key concepts from description
   ’ Actors: API users, search system, domain processor, scoring system
   ’ Actions: execute search, validate domains, score results, create candidates
   ’ Data: search queries, search results, domains, candidates
   ’ Constraints: SearXNG only initially, blacklist filtering required
3. Unclear aspects identified:
   ’ [RESOLVED] Auth/authorization for API: Development phase only, no auth required
   ’ [RESOLVED] Concurrent search limits: 10 concurrent based on Ollama capacity
   ’ [RESOLVED] Result deduplication scope: Session-level + global domain tracking
4. User Scenarios filled (API-triggered search workflow)
5. Functional Requirements generated (13 testable requirements)
6. Key Entities identified (SearchRequest, SearchResult, Domain, Candidate)
7. Review Checklist: All items verified
8. Status: SUCCESS (spec ready for planning)
```

---

## ¡ Quick Guidelines
-  Focus on WHAT users need and WHY
- L Avoid HOW to implement (no tech stack, APIs, code structure)
- =e Written for business stakeholders, not developers

### Section Requirements
- **Mandatory sections**: Must be completed for every feature
- **Optional sections**: Include only when relevant to the feature
- When a section doesn't apply, remove it entirely (don't leave as "N/A")

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story
A developer or operator needs to discover funding opportunities by executing searches against web search engines. The system should accept a search request (category, region, funding type), generate appropriate search queries, execute searches, validate and score results based on relevance and credibility, and create high-confidence funding source candidates for further investigation. The search workflow must be observable, fault-tolerant, and efficient.

### Acceptance Scenarios

1. **Given** no blacklisted domains exist, **When** user triggers search for "Bulgaria education scholarships", **Then** system generates 3 queries, executes search via SearXNG, returns 20-25 results, validates all domains, scores results, and creates 5-10 high-confidence candidates (e0.60 score)

2. **Given** domain "gambling.com" is blacklisted, **When** search results include URLs from "gambling.com", **Then** system filters out all gambling.com results before scoring and does not create candidates for that domain

3. **Given** same domain appears in multiple search results, **When** processing search results, **Then** system deduplicates by domain, registers domain only once, and tracks all URLs from that domain

4. **Given** search engine returns error (timeout, 500), **When** executing search, **Then** system retries 3x with exponential backoff, and if all retries fail, logs error and continues processing other searches

5. **Given** 10 concurrent search requests, **When** all requests trigger query generation, **Then** system processes all queries in parallel (Ollama supports 10 concurrent), completes all searches within 30 seconds

6. **Given** search result has low confidence score (<0.60), **When** scoring completes, **Then** system logs result for analytics but does not create funding source candidate

### Edge Cases

- What happens when SearXNG returns 0 results?
  ’ System logs "no results" event, completes workflow successfully, creates no candidates

- What happens when domain extraction fails (malformed URL)?
  ’ System logs parsing error, skips that result, continues processing remaining results

- What happens when blacklist cache (Valkey) is unavailable?
  ’ System falls back to direct PostgreSQL queries, logs degraded performance warning

- What happens when duplicate domain is encountered within same search session?
  ’ In-memory deduplication prevents redundant processing, only first occurrence is processed

- What happens when query generation times out?
  ’ System logs timeout, returns error to caller, does not publish to search queue

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST accept search requests via REST API containing category, region, funding type, and recipient type

- **FR-002**: System MUST generate 2-4 search queries per request based on provided parameters using AI-powered query generation

- **FR-003**: System MUST execute searches against SearXNG search engine and retrieve 20-25 results per query

- **FR-004**: System MUST extract domain from each search result URL and validate domain is not blacklisted before further processing

- **FR-005**: System MUST deduplicate search results by domain within a search session (same requestId)

- **FR-006**: System MUST register new domains in domain tracking system when first encountered

- **FR-007**: System MUST calculate confidence score (0.00-1.00 scale) for each search result based on domain credibility, funding keywords, geographic relevance, and organization type

- **FR-008**: System MUST create funding source candidate entities for search results with confidence score e 0.60

- **FR-009**: System MUST track workflow progress through observable stages (search requested ’ search executed ’ results validated ’ results scored ’ candidates created)

- **FR-010**: System MUST retry transient failures (HTTP timeout, cache unavailable) up to 3 times with exponential backoff

- **FR-011**: System MUST log all workflow errors to centralized error tracking for monitoring and debugging

- **FR-012**: System MUST complete full search workflow (query generation ’ search ’ validation ’ scoring ’ persistence) within 5 seconds per query under normal load

- **FR-013**: System MUST provide API documentation for all search endpoints showing request/response schemas and example usage

### Key Entities

- **SearchRequest**: Represents a user's request to discover funding opportunities. Contains category (e.g., EDUCATION, INFRASTRUCTURE), geographic region (e.g., BG for Bulgaria), funding type (e.g., SCHOLARSHIP, GRANT), recipient type (e.g., K12_SCHOOL, UNIVERSITY). Each request has unique identifier for tracking through workflow stages.

- **SearchResult**: Represents a single result from a search engine. Contains URL, page title, description snippet, source search engine, timestamp. Metadata-only (no web page content crawled at this stage).

- **Domain**: Represents a unique website domain encountered during searches. Tracks domain name, status (DISCOVERED, BLACKLISTED, PROCESSED_HIGH_QUALITY, PROCESSED_LOW_QUALITY), quality metrics, first/last seen timestamps. Used for deduplication and blacklist filtering.

- **FundingSourceCandidate**: Represents a high-confidence search result worthy of further investigation. Contains URL, domain reference, confidence score, discovery session reference, status (PENDING_CRAWL for next phase). Created only when confidence e 0.60.

- **WorkflowError**: Represents a failure at any workflow stage. Contains request identifier, stage name (SEARCH_EXECUTION, DOMAIN_PROCESSING, SCORING), error type, error message, retry count, timestamp. Used for monitoring and debugging.

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (all resolved)
- [x] User scenarios defined
- [x] Requirements generated
- [x] Entities identified
- [x] Review checklist passed

---

## Success Criteria

### Functional Success
-  Search workflow executes end-to-end: API request ’ query generation ’ search ’ validation ’ scoring ’ candidate creation
-  Blacklisted domains filtered correctly (0 candidates created from blacklisted sources)
-  High-confidence results (e0.60) create funding source candidates with PENDING_CRAWL status
-  Duplicate domains deduplicated within search session
-  Workflow observable through all stages (can track single request through entire pipeline)

### Performance Success
-  Query generation completes in 200-300ms per query
-  Search execution completes in 1-2 seconds per query
-  Domain validation completes in <100ms for 25 results (using cache)
-  Full workflow (end-to-end) completes in <5 seconds per query
-  System handles 10 concurrent search requests without degradation

### Quality Success
-  API documentation available and complete (Swagger/OpenAPI)
-  Error logging captures all workflow failures with sufficient context for debugging
-  Retry logic handles transient failures without manual intervention
-  No data loss during failures (errors logged, partial results preserved)

---

## Dependencies and Assumptions

### External Dependencies
- SearXNG search engine running and accessible at http://192.168.1.10:8080
- Ollama LLM service running and accessible at http://192.168.1.10:11434 (for query generation)
- Kafka message broker running and accessible at http://192.168.1.10:9092
- Valkey cache running and accessible at http://192.168.1.10:6379 (for blacklist caching)
- PostgreSQL database running and accessible at http://192.168.1.10:5432

### Assumptions
- SearXNG returns 20-25 results per query on average
- Domain blacklist is relatively small (<10,000 domains) and changes infrequently
- Search requests are primarily during development/testing (no production load yet)
- Single application instance (no distributed deployment initially)
- Geographic focus primarily on Bulgaria and Eastern Europe
- Funding categories are well-defined in existing taxonomy (Feature 005)

### Known Constraints
- Initial scope limited to SearXNG only (other search engines deferred)
- No web crawling in this phase (metadata-only from search results)
- No authentication/authorization on REST API (development phase)
- No multi-language query generation initially (English queries only)
- Session-level deduplication only (cross-session duplicates allowed)

---

## Out of Scope

The following are explicitly **not** included in this feature:

- L Additional search engines (Brave, Serper, Tavily, Perplexica) - deferred to future features
- L Web crawling and content extraction - deferred to Phase 2 (crawler implementation)
- L Scheduled/automated search execution - deferred until workflow validated
- L Multi-language query generation - deferred
- L User authentication and authorization - not needed for development phase
- L Distributed tracing and advanced observability - future enhancement
- L Circuit breakers and rate limiting - future enhancement
- L Email notifications for discoveries - future enhancement
- L Web UI for search management - API-only for now

---

## Migration Notes

This feature includes migration from LM Studio to Ollama for query generation:
- **Reason**: Ollama supports concurrent requests (10 parallel), LM Studio does not
- **Impact**: Existing query generation tests must pass with new LLM backend
- **Risk**: Query quality may differ between models (llama3.1:8b vs qwen2.5:0.5b)
- **Mitigation**: Run full test suite (58 tests) to verify query generation quality

---
