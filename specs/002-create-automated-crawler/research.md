# Research: Automated Crawler Infrastructure - Phase 1 Metadata Judging

**Feature**: 002-create-automated-crawler
**Date**: 2025-10-19

## Research Questions

### Q1: Best practices for domain-level deduplication in web crawling
**Decision**: Use PostgreSQL table with unique constraint on domain_name + status-based filtering
**Rationale**:
- Permanent storage prevents re-crawling blacklisted domains across discovery sessions
- Status enum (DISCOVERED, PROCESSING, PROCESSED_HIGH_QUALITY, PROCESSED_LOW_QUALITY, BLACKLISTED, NO_FUNDS_THIS_YEAR, PROCESSING_FAILED) enables quality-based filtering
- Quality metrics (best_confidence_score, high_quality_candidate_count, low_quality_candidate_count) support learning which domains yield good candidates
- Exponential backoff for transient failures (retry_after timestamp)

**Alternatives Considered**:
- Redis with TTL: Rejected - blacklist entries would expire, causing re-discovery of known scam sites
- In-memory Set: Rejected - not persistent across application restarts
- URL-level deduplication: Rejected - same domain with different URLs (e.g., /programs vs /grants) should be deduplicated

### Q2: Metadata judging approach without web crawling
**Decision**: Multi-judge weighted scoring system analyzing search engine metadata only
**Rationale**:
- **Judge 1 - Funding Keywords** (weight 2.0): Title/snippet contains "grant", "scholarship", "fellowship", "funding", etc.
- **Judge 2 - Domain Credibility** (weight 1.5): .org, .gov, .edu, .foundation TLDs; detect spam patterns (click, ad., promo)
- **Judge 3 - Geographic Relevance** (weight 1.0): Mentions Bulgaria, Eastern Europe, Balkans, EU member states
- **Judge 4 - Organization Type** (weight 0.8): Mentions foundation, NGO, government agency, etc.
- Overall confidence = weighted average of judge scores
- Threshold: 0.6 for creating PENDING_CRAWL candidates

**Alternatives Considered**:
- Single LLM call for judging: Rejected - too slow for 20-25 results, less explainable
- Rule-based scoring only: Rejected - less flexible than weighted multi-judge approach
- Machine learning model: Rejected - no training data yet, premature optimization

### Q3: Parallel processing with Java 25 Virtual Threads
**Decision**: Use `Executors.newVirtualThreadPerTaskExecutor()` with CompletableFuture
**Rationale**:
- Virtual Threads (Project Loom) are lightweight and efficient for I/O-bound tasks
- `CompletableFuture.supplyAsync()` provides async execution with result aggregation
- `CompletableFuture.allOf()` waits for all processing to complete before aggregating stats
- No thread pool size tuning needed - Virtual Threads scale automatically
- Constitutional requirement: Java 25 with modern patterns

**Example**:
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<CompletableFuture<ProcessingResult>> futures = searchResults.stream()
        .map(result -> CompletableFuture.supplyAsync(
            () -> processSearchResult(result),
            executor
        ))
        .toList();
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
}
```

**Alternatives Considered**:
- Traditional ThreadPoolExecutor: Rejected - requires thread pool sizing, less efficient than Virtual Threads
- Sequential processing: Rejected - too slow for 20-25 results per query
- Reactive Streams (Project Reactor): Rejected - unnecessary complexity for this use case

### Q4: Search engine integration strategy
**Decision**: Adapter pattern with common SearchEngine interface
**Rationale**:
- Support multiple engines: Searxng (self-hosted), Tavily, Browserbase, Perplexity
- Each adapter implements `search(String query, int limit)` → `List<SearchResult>`
- SearchResult DTO contains: url, title, snippet, searchEngine, searchQuery, position
- Configuration-driven engine selection (enable/disable engines without code changes)
- Deferred to future implementation - Phase 1 focuses on judging mock/existing data

**Alternatives Considered**:
- Single search engine: Rejected - spec requires multiple engines for coverage
- Direct API calls in orchestrator: Rejected - violates separation of concerns
- Third-party aggregation library: Rejected - most are Python-based, not Java

### Q5: Error handling and exponential backoff for domain failures
**Decision**: Track failure_count and retry_after in Domain entity with exponential backoff schedule
**Rationale**:
- 1st failure: retry_after = now + 1 hour
- 2nd failure: retry_after = now + 4 hours
- 3rd failure: retry_after = now + 1 day
- 4th+ failure: retry_after = now + 1 week
- Status = PROCESSING_FAILED with failure_reason for debugging
- Re-attempt when current time >= retry_after

**Alternatives Considered**:
- Immediate retry: Rejected - wastes resources on persistent errors
- Fixed retry interval: Rejected - doesn't adapt to error patterns
- Dead letter queue: Rejected - unnecessary complexity, domains can be manually blacklisted if permanently broken

### Q6: Integration with existing 001-automated-funding-discovery feature
**Decision**: Extend existing domain model and database schema
**Rationale**:
- Add Domain entity + domain table (V8 migration)
- Update FundingSourceCandidate with domainId FK + PENDING_CRAWL status (V9 migration)
- Reuse existing repositories (FundingSourceCandidateRepository) and infrastructure
- Discovery session tracking already exists (DiscoverySession entity)
- Human review workflow (from 001) operates on PENDING_REVIEW status after Phase 2 crawling

**Alternatives Considered**:
- Separate database: Rejected - creates data silos, complicates queries
- New Spring Boot application: Rejected - violates monolith architecture principle
- Duplicate entities: Rejected - violates DRY, creates inconsistencies

## Technology Decisions

### Core Stack (Constitutional Requirements)
- **Language**: Java 25 (source/target level 25)
- **Framework**: Spring Boot 3.5.5
- **Database**: PostgreSQL 16 (Mac Studio 192.168.1.10:5432)
- **Async Messaging**: Spring Events + @Async (NO Kafka per constitution)
- **Functional Programming**: Vavr 0.10.6 (Try, Either, Option)
- **Boilerplate Reduction**: Lombok
- **Testing**: JUnit/Jupiter + TestContainers + Mockito

### Forbidden Technologies (Constitutional Constraints)
- ❌ Kafka: Removed per constitution amendment, using Spring Events
- ❌ crawl4j: Not using
- ❌ langgraph4j: Not using
- ❌ langchain4j: Not using
- ❌ Spring Integration: User decision - simple orchestrator pattern instead

### Architecture Patterns
- **Domain-Driven Design**: Entities (Domain, FundingSourceCandidate), Services (DomainRegistryService, MetadataJudgingService), Orchestrator (CandidateProcessingOrchestrator)
- **Repository Pattern**: Spring Data JDBC repositories for data access
- **Service Layer**: Business logic in services, orchestration in orchestrator
- **DTO Pattern**: SearchResult, MetadataJudgment, ProcessingStats for data transfer

## Open Questions Resolved

1. **How to prevent duplicate processing across sessions?** → Domain table with unique constraint, status-based filtering
2. **How to judge quality without web crawling?** → Multi-judge weighted scoring on search metadata
3. **How to handle parallel processing efficiently?** → Java 25 Virtual Threads with CompletableFuture
4. **How to integrate with existing feature 001?** → Extend domain model, reuse infrastructure
5. **How to handle transient failures?** → Exponential backoff with retry_after tracking

## Next Steps (Phase 1)
1. Create data-model.md with Domain, MetadataJudgment, ProcessingStats entities
2. Generate API contracts (if needed - mostly internal services)
3. Create quickstart.md with end-to-end test scenarios
4. Update agent context file incrementally
