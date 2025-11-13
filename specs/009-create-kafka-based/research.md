# Research: Kafka-Based Event-Driven Search Workflow

**Feature**: 009-create-kafka-based
**Date**: 2025-11-09

---

## Research Summary

All technical decisions were resolved during brainstorming phase. No unknowns remain requiring research. This document consolidates key decisions with rationales.

---

## Decision 1: Event-Driven Architecture with Kafka

**Decision**: Use Kafka Topics for workflow stages instead of database queue or in-memory orchestration

**Rationale**:
- User has extensive springcrawler experience with Kafka messaging
- Clean separation of concerns (each stage is independent Kafka consumer)
- Built-in message retention (7-30 days) handles debugging without manual cleanup
- Fault tolerance: Consumer failures don't lose messages
- Observability: Kafka UI shows message flow through topics
- Scalability: Can add more consumers for parallel processing

**Alternatives Considered**:
1. **Database Queue** (polling-based) - Rejected: Higher latency, requires manual cleanup, polling overhead
2. **In-Memory with CompletableFuture** - Rejected: Loses in-flight work on crashes, harder to debug, no message replay

**Best Practices**:
- 4 topics for clear stage separation (search-requests, search-results-raw, search-results-validated, workflow-errors)
- 7-day retention for workflow topics (balance storage vs debugging)
- 30-day retention for error topic (longer history for pattern analysis)
- Consumer groups for parallel processing within stages
- Dead letter queue (workflow-errors) for centralized error handling

**Implementation Notes**:
- Spring Kafka for consumer/producer framework
- JSON serialization via Jackson (FasterXML)
- One `@KafkaListener` per workflow stage
- Manual acknowledgment for critical stages (domain processing, scoring)

---

## Decision 2: Valkey for Blacklist Caching

**Decision**: Use Valkey (Redis-compatible) read-through cache for domain blacklist checks

**Rationale**:
- **10x speedup**: 1ms (Valkey) vs 10ms (PostgreSQL) per domain check
- 80-100 domain checks per search (20-25 results × 4 engines future) = 800ms vs 8000ms saved
- Valkey already deployed on Mac Studio (docker-compose.yml)
- User has springcrawler experience with Redis/Valkey caching

**Alternatives Considered**:
1. **PostgreSQL Only** - Rejected: 10x slower, but acceptable as fallback
2. **In-Memory Cache (Caffeine)** - Rejected: Doesn't synchronize across multiple app instances (future scaling)

**Best Practices**:
- Read-through cache pattern (check cache → miss → query DB → cache result)
- 24-hour TTL (blacklists change infrequently)
- LRU eviction policy (configured in docker-compose.yml)
- Key pattern: `blacklist:{domain}` → `true/false`
- Write-through on blacklist updates (update PostgreSQL, invalidate Valkey key)

**Implementation Notes**:
- Spring Data Redis / Lettuce client for Valkey
- `DomainBlacklistCache` service wraps cache logic
- Fallback to PostgreSQL if Valkey unavailable (resilience)
- Log degraded performance warnings on fallback

---

## Decision 3: No Query Caching

**Decision**: Generate queries on-demand, no caching layer

**Rationale**:
- Cost: 200-300ms per query × 15-30 queries = 3-9 seconds total (acceptable for nightly batch)
- Taxonomy evolution: Adding new funding types/regions would complicate cache keys
- Simpler code: No cache key generation, no TTL management, no invalidation logic
- Query generation is deterministic: Same params = same queries (but cache adds more complexity than value)

**Alternatives Considered**:
1. **Composite Key Hash** (SHA-256 of params) - Rejected: Premature optimization, complexity not justified
2. **Caffeine Cache (24h TTL)** - Rejected: 3-9 seconds is acceptable, avoid complexity

**Best Practices**:
- N/A (no caching implemented)

**Implementation Notes**:
- Directly call `QueryGenerationService.generateQueries()` on each request
- No cache abstraction needed

---

## Decision 4: SearXNG Only (Initial Scope)

**Decision**: Implement SearXNG adapter only, defer other search engines

**Rationale**:
- **Validate pattern first**: Prove Kafka workflow + adapter interface before adding complexity
- SearXNG is self-hosted (no API costs, no rate limits)
- SearXNG already running on Mac Studio (docker-compose.yml)
- Easier to debug single adapter before adding Brave, Serper, Tavily, Perplexica

**Alternatives Considered**:
1. **All 5 engines at once** - Rejected: Too much complexity, harder to debug failures
2. **SearXNG + Brave** - Rejected: Brave requires API key and costs money, defer until pattern validated

**Best Practices**:
- Define `SearchAdapter` interface for future engines
- Use Strategy pattern (polymorphic adapter selection)
- Keep SearXNG-specific logic isolated in `SearXNGAdapter` class

**Implementation Notes**:
- `SearchAdapter` interface: `List<SearchResult> search(String query, int maxResults)`
- `SearXNGAdapter` implements interface, calls `http://192.168.1.10:8080/search` API
- Future adapters (BraveAdapter, SerperAdapter, etc.) implement same interface

---

## Decision 5: LM Studio → Ollama Migration

**Decision**: Migrate query generation from LM Studio to Ollama

**Rationale**:
- **Concurrent requests**: Ollama supports `OLLAMA_NUM_PARALLEL=10`, LM Studio does not
- **Critical for parallel search**: Need to generate queries for multiple engines simultaneously
- Ollama already configured on Mac Studio (`http://192.168.1.10:11434`)
- Both use OpenAI-compatible API (minimal code changes)

**Alternatives Considered**:
1. **Keep LM Studio** - Rejected: No concurrent request support blocks parallel search execution
2. **External LLM API** (OpenAI, Anthropic) - Rejected: Constitution requires local LLM (no external dependencies)

**Best Practices**:
- Update `application.yml`: Change base URL from `:1234` to `:11434`
- Update model name: `llama3.1:8b` (more reliable than `qwen2.5:0.5b` for structured output)
- Run full test suite (58 tests) to verify query generation quality
- Remove langchain4j dependency (forbidden by Constitution Principle X)
- Use direct HTTP client (RestTemplate or HttpClient) with OpenAI-compatible API

**Implementation Notes**:
- `OllamaConfig.java` replaces `LmStudioConfig.java`
- HTTP client configuration: `baseUrl = "http://192.168.1.10:11434/v1"`
- No business logic changes (same `QueryGenerationService` interface)
- Integration tests verify query quality matches expectations

**Migration Risk**:
- **Query quality may differ**: llama3.1:8b vs qwen2.5:0.5b produces different output
- **Mitigation**: Run existing 58 tests to validate acceptable quality
- **Rollback**: Keep LM Studio running during transition, can revert if needed

---

## Decision 6: REST API First (Not Scheduler)

**Decision**: Build REST API trigger first, defer scheduling to future feature

**Rationale**:
- **Faster to working prototype**: Can manually trigger searches via Postman/curl
- **Flexible invocation**: CLI, Postman, future scheduler all call same API
- **Easier testing**: Integration tests call REST endpoint directly
- **Scheduler is just a trigger**: Doesn't affect search workflow design

**Alternatives Considered**:
1. **Cron scheduler first** - Rejected: Harder to test, less flexible during development
2. **CLI tool first** - Rejected: REST API is more versatile (used by CLI, scheduler, future dashboard)

**Best Practices**:
- SpringDoc/Swagger for API documentation
- `@RestController` with `@RequestBody` validation
- DTOs for request/response (separate from domain entities)
- Return `requestId` for tracking workflow progress

**Implementation Notes**:
- `SearchController` with `POST /api/search/execute` endpoint
- Request DTO: `SearchExecutionRequest` (category, region, fundingType, recipientType, searchEngine)
- Response DTO: `SearchExecutionResponse` (requestId, status, queriesGenerated, timestamp)
- Status endpoint: `GET /api/search/status/{requestId}` for progress tracking

---

## Decision 7: Dynamic Round-Robin Scheduling (Future)

**Decision**: Implement round-robin category distribution when scheduler added (out of scope for this feature)

**Rationale**:
- Ensures balanced coverage across funding categories over time
- Prevents always searching same categories first
- Distributes load evenly across the week (Monday=Category A, Tuesday=Category B, etc.)

**Implementation Notes** (future feature):
- Track last-searched timestamp per category in database
- Scheduler selects oldest-searched category each night
- Automatically rotates through all categories weekly

---

## Infrastructure Verification

All required services are running on Mac Studio (192.168.1.10):

| Service | Port | Container | Status | Purpose |
|---------|------|-----------|--------|---------|
| Kafka | 9092 | northstar-kafka | ✅ Running | Message broker |
| Kafka UI | 8081 | northstar-kafka-ui | ✅ Running | Kafka management |
| Valkey | 6379 | northstar-valkey | ✅ Running | Blacklist cache |
| Redis Commander | 8082 | northstar-redis-commander | ✅ Running | Valkey UI |
| PostgreSQL | 5432 | northstar-postgres | ✅ Running | Persistence |
| SearXNG | 8080 | northstar-searxng | ✅ Running | Search engine |
| Ollama | 11434 | (native) | ✅ Running | LLM inference |

**Verification Commands**:
```bash
# Kafka
curl -s http://192.168.1.10:9092 || echo "Kafka not responding"

# Valkey
redis-cli -h 192.168.1.10 -p 6379 ping

# SearXNG
curl -s http://192.168.1.10:8080/healthz

# Ollama
curl -s http://192.168.1.10:11434/v1/models
```

---

## Technology Stack Confirmation

All dependencies align with Constitution Principle X (Technology Constraints):

**✅ APPROVED Technologies Used**:
- Spring Kafka - Event streaming
- Lombok - Boilerplate reduction
- Jackson (FasterXML) - JSON serialization
- JUnit 5 - Testing framework

**❌ FORBIDDEN Technologies Avoided**:
- crawl4j - Not used
- langgraph4j - Not used
- langchain4j - **REMOVED** (was used in query generation, migrating to direct HTTP client)
- Microservices - Not used (monolith architecture maintained)

**Architecture Confirmation**:
- ✅ MONOLITH: Single Spring Boot application with multiple Maven modules
- ✅ Modular: 3 new modules with clear responsibilities
- ❌ NOT Microservices: All modules packaged into single deployable JAR

---

## Open Questions

**None** - All technical decisions resolved during brainstorming phase.

---

## Phase 0 Status

✅ **COMPLETE** - All unknowns resolved, ready for Phase 1 (Design & Contracts)
