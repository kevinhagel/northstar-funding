# Session Summary: Search Workflow Kafka Design & Brainstorming

**Date**: 2025-11-09
**Branch**: 009-create-kafka-based
**Session Type**: Feature Design & Planning
**Status**: ✅ Design Complete

---

## Problem Statement

Need to implement distributed search execution workflow across multiple search engines with:
- Nightly scheduled searches distributed across the week
- Multiple search provider support (SearXNG, Brave, Serper, Tavily, Perplexica)
- Domain tracking and blacklist management
- Concurrent query execution
- Observable, fault-tolerant workflow

**Key Challenge**: Perplexica + Ollama integration not working (blocking), need alternative approach that doesn't depend on it.

---

## Brainstorming Process (Superpowers Skill)

Used `superpowers:brainstorming` skill for structured design refinement:

### Phase 1: Understanding Requirements
**Questions Asked**:
1. Which search providers to start with? → SearXNG (self-hosted) + Brave Search
2. Should we migrate LM Studio → Ollama now or later? → **Migrate first** (need concurrent requests)
3. What should nightly search scope be? → **Distribute across 7 days** (round-robin)

**Key Insight**: User clarified that schedulers, CLI, and REST API are just *trigger mechanisms*. The search adapters themselves have no knowledge of how they were invoked. **Orchestrator pattern** needed.

### Phase 2: Architecture Exploration
**Approaches Considered**:
1. Simple Sequential Pipeline - Synchronous, easy to debug
2. Event-Driven with Database Queue - Polling-based, eventual consistency
3. **Orchestrator Pattern with CompletableFuture** - Non-blocking, parallel execution

**User Feedback**: Liked event-driven (#2) but asked about Kafka integration.

**Critical Realization**: User has extensive springcrawler experience with Kafka messaging. Kafka Topics can handle event-driven workflow better than database polling.

### Phase 3: Kafka Topic Design
**Refined Approach**: Event-driven with Kafka Topics

**User Concerns Addressed**:
- Domain deduplication: `foo.edu/program1` and `foo.edu/program2` both from `foo.edu`
- Blacklist synchronization across concurrent adapters
- When to persist to database vs keep in memory vs Kafka

**Decisions Made**:
1. **Blacklist Caching**: Valkey (Redis-compatible) for 10x speedup (1ms vs 10ms PostgreSQL)
   - Hybrid approach: Valkey read-through cache + PostgreSQL source of truth
   - 24-hour TTL, LRU eviction
   - 80-100 domain checks per search × 10x = worth the complexity

2. **Query Caching**: **No caching** - Generate on-demand
   - Cost: 200-300ms per query × 15-30 queries = 3-9 seconds (acceptable for nightly batch)
   - Avoids cache key complexity as taxonomy evolves
   - Simpler is better - no premature optimization

3. **Search Distribution**: Dynamic round-robin (track which categories searched, auto-rotate)

4. **Orchestration Control**: Smart orchestrator with retry logic, circuit breakers, graceful degradation

5. **Trigger Mechanism**: REST API first (Postman-ready), CLI and scheduler later

---

## Final Design Decisions

### Kafka Topics (4 Topics)
```
search-requests           # API publishes search triggers
search-results-raw        # SearXNG publishes raw results (metadata only)
search-results-validated  # Domain processor publishes after blacklist/dedupe
workflow-errors           # Dead letter queue (30-day retention)
```

### Module Structure (3 New Modules)
```
northstar-kafka-common/       # Kafka config, topic constants, event models
northstar-search-adapters/    # Search engine adapters (SearXNG first)
northstar-search-workflow/    # Kafka consumers, orchestration logic
northstar-application/        # REST API endpoints (updated)
```

### Technology Choices
- **Kafka 7.4.0** (Confluent Platform) - Message broker
- **Valkey 7.2** - Blacklist cache (Redis-compatible)
- **Spring Kafka** - Consumer/producer framework
- **SpringDoc/Swagger** - API documentation
- **Ollama llama3.1:8b** - Query generation (replacing LM Studio)

### Workflow Stages
```
REST API: POST /api/search/execute
    ↓
Generate Queries (QueryGenerationService, 200-300ms)
    ↓
Publish to: search-requests (Kafka)
    ↓
SearXNG Consumer → Execute Search (1-2s) → Publish: search-results-raw
    ↓
Domain Processor → Extract/Blacklist/Dedupe (<100ms Valkey) → Publish: search-results-validated
    ↓
Scoring Consumer → Confidence Scoring (existing SearchResultProcessor) → Create Candidates
    ↓
Database Persistence (FundingSourceCandidate, Domain)
```

### Performance Targets
- Query generation: 200-300ms per query
- Search execution: 1-2 seconds per query
- Domain validation: <100ms for 25 results (Valkey caching)
- **End-to-end: <5 seconds per query**
- Concurrent: 10 parallel searches (Ollama capacity)

---

## Infrastructure Updates

### Docker Compose Merge
Merged Kafka + Valkey + management tools into `docker/docker-compose.yml`:

**Added Services**:
- Kafka (Port 9092) - Confluent Platform 7.4.0
- Valkey (Port 6379) - Cache for blacklist lookups
- Kafka UI (Port 8081) - Management interface
- Redis Commander (Port 8082) - Valkey UI

**Network**: Single `northstar-network` (172.20.0.0/16)
**Volumes**: 7 persistent volumes (kafka-data, valkey-data, etc.)

**Deployment**:
```bash
rsync -av docker/ macstudio:~/northstar/
ssh macstudio "cd ~/northstar && docker-compose up -d kafka valkey kafka-ui redis-commander"
```

**Platform Warning**: Redis Commander shows AMD64 warning on ARM64 Mac Studio - **safe to ignore** (emulation works fine for lightweight web UIs).

---

## Migration: LM Studio → Ollama

**Reason**: Ollama supports concurrent requests (`OLLAMA_NUM_PARALLEL=10`), LM Studio doesn't

**Configuration Change**:
```yaml
# Before (LM Studio)
base-url: http://192.168.1.10:1234/v1
model-name: qwen2.5-0.5b-instruct

# After (Ollama)
base-url: http://192.168.1.10:11434/v1
model-name: llama3.1:8b
```

**Impact**: Minimal - OpenAI-compatible API means no code changes required

**Risk**: Query quality may differ between models

**Mitigation**: Run full test suite (58 tests) to verify

---

## Key Design Patterns

### 1. Adapter Pattern (Search Engines)
```java
public interface SearchAdapter {
    List<SearchResult> search(String query, int maxResults);
    SearchEngineType getEngineType();
}

@Service
public class SearXNGAdapter implements SearchAdapter {
    // Implementation
}
```

### 2. Event-Driven via Kafka
```java
@KafkaListener(topics = "search-requests", groupId = "search-executor")
public void handleSearchRequest(SearchRequestEvent event) {
    // Execute search
    // Publish to next topic
}
```

### 3. Read-Through Cache (Valkey)
```java
public boolean isBlacklisted(String domain) {
    // 1. Check Valkey cache
    // 2. If miss, query PostgreSQL
    // 3. Cache result
}
```

---

## Database Schema

**No New Tables Required** - Existing schema supports workflow:
- ✅ `domain` table - Domain tracking and blacklist
- ✅ `funding_source_candidate` table - High-confidence candidates
- ✅ `discovery_session` table - Session tracking
- ✅ `search_result` table (V17) - Raw search results

**New Index** (V18):
```sql
CREATE INDEX idx_search_result_request_id ON search_result(request_id);
CREATE INDEX idx_candidate_session_id ON funding_source_candidate(discovery_session_id);
```

---

## Scope and Boundaries

### In Scope (Feature 009)
✅ SearXNG adapter only (validate pattern)
✅ Kafka-driven workflow (4 topics)
✅ Valkey blacklist caching
✅ REST API with Swagger docs
✅ Domain processing and deduplication
✅ Confidence scoring (existing logic)
✅ LM Studio → Ollama migration

### Out of Scope (Future)
❌ Additional search engines (Brave, Serper, Tavily, Perplexica)
❌ Web crawling and content extraction
❌ Scheduled/automated execution (CLI/cron triggers)
❌ Multi-language query generation
❌ User authentication
❌ Circuit breakers and rate limiting
❌ Distributed tracing

---

## Testing Strategy

### Unit Tests
- `SearXNGAdapterTest` - Mock RestTemplate, verify JSON parsing
- `SearchRequestConsumerTest` - Mock Kafka, verify event publishing
- `DomainProcessorConsumerTest` - Mock Valkey, verify blacklist logic
- `ScoringConsumerTest` - Mock ConfidenceScorer, verify threshold filtering

### Integration Tests (TestContainers)
- End-to-end: Publish SearchRequestEvent → verify FundingSourceCandidate created
- Verify Kafka topic ordering (request → raw → validated)
- Verify Valkey cache hits/misses

### Live SearXNG Test
- Execute real search query
- Verify 20-25 results returned
- Verify JSON parsing with actual API response

---

## Success Criteria

### Functional
- ✅ Search workflow executes end-to-end
- ✅ Blacklisted domains filtered correctly
- ✅ High-confidence results (≥0.60) create candidates
- ✅ Duplicate domains deduplicated
- ✅ Workflow observable through all stages

### Performance
- ✅ End-to-end latency <5 seconds per query
- ✅ Handle 10 concurrent search requests
- ✅ Domain validation <100ms for 25 results (Valkey)

### Quality
- ✅ API documentation complete (Swagger)
- ✅ Error logging captures all failures
- ✅ Retry logic handles transient failures
- ✅ No data loss during failures

---

## Files Created/Modified

**Specification**:
- `specs/009-create-kafka-based/spec.md` - Feature specification

**Infrastructure**:
- `docker/docker-compose.yml` - Added Kafka, Valkey, Kafka UI, Redis Commander

**Branch**: `009-create-kafka-based` (created by `/specify` command)

---

## Next Steps

1. **Run `/plan`** - Generate implementation plan from spec
2. **Create Maven modules**:
   - northstar-kafka-common
   - northstar-search-adapters
   - northstar-search-workflow
3. **Migrate to Ollama** - Update query generation config, run tests
4. **Implement SearXNG adapter** - RestTemplate + JSON parsing
5. **Implement Kafka consumers** - One per workflow stage
6. **Implement Valkey caching** - DomainBlacklistCache service
7. **Add REST API endpoints** - SearchController with SpringDoc
8. **Integration tests** - End-to-end workflow validation

---

## Lessons Learned

1. **Brainstorming skill worked perfectly** - Structured questioning led to clear decisions
2. **Kafka experience from springcrawler was valuable** - User knew exactly what they wanted once Kafka was mentioned
3. **Performance analysis justified complexity** - 10x speedup (Valkey) and concurrent requests (Ollama) worth the added infrastructure
4. **Simplicity won on query caching** - 3-9 seconds acceptable, avoided premature optimization
5. **Incremental scope (SearXNG only) reduces risk** - Validate pattern before adding more engines

---

## References

- springcrawler docker-compose: `/Users/kevin/github/springcrawler/docker-compose-mac-studio-infra.yml`
- Existing services: `northstar-persistence/src/main/java/.../service/`
- Query generation: `northstar-query-generation/` (58 tests passing)
- Domain model: `northstar-domain/src/main/java/.../domain/`

---

**Status**: ✅ Design Complete - Ready for `/plan` and implementation
