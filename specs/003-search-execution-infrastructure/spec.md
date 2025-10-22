# Feature Specification: Search Execution Infrastructure

**Feature Branch**: `003-search-execution-infrastructure`
**Created**: 2025-10-20
**Status**: Draft
**Input**: User description: "Search Execution Infrastructure: Build 4 search engine adapters (Searxng, Browserbase, Tavily, Perplexity) with hardcoded query library for nightly automated discovery. Execute queries across all engines, deduplicate results by domain, integrate with existing metadata scoring pipeline. MVP approach: start with simple hardcoded queries (5-10 per night) to validate infrastructure before adding AI-powered query generation. Constitutional compliance: no langchain4j, use Vavr for error handling, Resilience4j circuit breakers, Virtual Threads for parallel execution. Focus on learning which search engines and query patterns are most productive for funding source discovery."

## Execution Flow (main)
```
1. Parse user description from Input
   ’ Actors: Nightly crawler system, search engines, metadata scoring service
   ’ Actions: Execute queries, collect results, deduplicate, score, store
   ’ Data: Search queries, search results, domains, funding source candidates
   ’ Constraints: MVP approach, no AI generation yet, constitutional compliance
2. Extract key concepts from description
    4 search engine integrations (Searxng, Browserbase, Tavily, Perplexity)
    Hardcoded query library (5-10 queries per night)
    Domain-level deduplication
    Integration with existing Phase 1 metadata scoring
    Nightly automated execution
    Performance monitoring and learning
3. Mark unclear aspects
   ’ [CLARIFIED] Query selection strategy: Hardcoded per day-of-week
   ’ [CLARIFIED] Search result limit: 25 per engine per query
   ’ [CLARIFIED] Deduplication: By domain only (not URL)
4. Fill User Scenarios & Testing section
    Primary user: Internal system (nightly crawler)
    Secondary users: Kevin/team reviewing discovered candidates
5. Generate Functional Requirements
    All requirements testable and measurable
6. Identify Key Entities
    SearchQuery, SearchResult, SearchEngine, Domain
7. Run Review Checklist
    No implementation details in requirements
    Focused on WHAT not HOW
    Testable acceptance criteria
8. Return: SUCCESS (spec ready for planning)
```

---

## ¡ Quick Guidelines
-  Focus on WHAT the system needs to do and WHY
- L Avoid HOW to implement (tech details in planning phase)
- =e Written for business understanding, technical constraints noted separately

---

## User Scenarios & Testing

### Primary User Story
**As the nightly crawler system**, I need to discover new funding source websites by executing search queries across multiple search engines, so that the metadata scoring pipeline has a continuous supply of candidate websites to evaluate for relevance and quality.

**As Kevin (internal reviewer)**, I need to see which search engines and query patterns are discovering high-quality funding sources, so that I can refine the query library and prioritize productive search engines for future iterations.

### Acceptance Scenarios

1. **Given** the nightly crawler is scheduled to run at 2:00 AM Monday, **When** the crawler executes with Monday's query set, **Then** the system executes 5-10 hardcoded queries across all 4 search engines and collects approximately 500-1000 raw search results.

2. **Given** search results are returned from multiple engines for the same query, **When** the deduplication process runs, **Then** the system identifies unique domains and prevents duplicate processing of the same domain within a single night or across multiple nights.

3. **Given** unique search results have been collected and deduplicated, **When** the results are passed to the metadata scoring service, **Then** each result receives a confidence score and high-confidence results (e 0.60) are marked as PENDING_CRAWL for Phase 2 processing.

4. **Given** a search engine fails or times out during query execution, **When** the failure is detected, **Then** the system logs the failure, continues processing with remaining search engines, and does not block the entire nightly run.

5. **Given** the nightly crawler has completed its execution, **When** Kevin reviews the discovery session statistics, **Then** he can see metrics including: total queries executed, results per search engine, unique domains discovered, deduplication rate, high-confidence candidates found, and per-query performance.

### Edge Cases

- **What happens when a search engine is completely unavailable?** System logs the outage, continues with remaining engines, and reports degraded coverage in the session statistics.

- **What happens when all 4 search engines return identical results for a query?** Deduplication detects this, processes each domain only once, and the statistics show 100% overlap rate for that query (indicating query may be too generic).

- **What happens when a domain is discovered on Monday and rediscovered on Tuesday with a different query?** Domain deduplication detects it already exists in the database, skips redundant processing, and increments the rediscovery counter for analytics.

- **What happens when search results contain invalid URLs or malformed domains?** System validates URL format, logs malformed entries, excludes them from processing, and continues with valid results.

- **What happens when the metadata scoring service is unavailable?** System queues the search results for later processing rather than discarding them, ensuring no discovered candidates are lost.

---

## Requirements

### Functional Requirements

**Search Execution:**
- **FR-001**: System MUST execute a predefined set of 5-10 search queries each night based on day-of-week scheduling.
- **FR-002**: System MUST execute each query against all 4 configured search engines (Searxng, Browserbase, Tavily, Perplexity).
- **FR-003**: System MUST collect up to 25 search results per query per search engine.
- **FR-004**: System MUST handle search engine failures gracefully by logging errors and continuing with remaining engines.
- **FR-005**: System MUST enforce timeout limits for search requests to prevent hanging on unresponsive engines.

**Query Management:**
- **FR-006**: System MUST provide a hardcoded query library organized by day-of-week (Monday queries, Tuesday queries, etc.).
- **FR-007**: System MUST allow queries to be easily modified and tested without requiring code compilation.
- **FR-008**: Each query MUST be tagged with metadata indicating its focus (geography, funding category, authority type) for analytics.

**Result Processing:**
- **FR-009**: System MUST extract from each search result: URL, title, snippet, search engine source, search query used, result position.
- **FR-010**: System MUST deduplicate search results by domain before metadata scoring to prevent processing the same website multiple times.
- **FR-011**: System MUST check if a domain already exists in the database (from previous nights) and skip reprocessing while incrementing rediscovery count.
- **FR-012**: System MUST validate that URLs are well-formed and domains are extractable before processing.

**Metadata Scoring Integration:**
- **FR-013**: System MUST pass all unique, new search results to the existing metadata scoring service.
- **FR-014**: System MUST store scored results in the database with status PENDING_CRAWL if confidence score e 0.60.
- **FR-015**: System MUST store scored results with status REJECTED if confidence score < 0.60.
- **FR-016**: System MUST preserve the association between each candidate and its discovery context (search query, search engine, discovery date).

**Scheduling & Automation:**
- **FR-017**: System MUST run automatically every night at a configurable time (default: 2:00 AM).
- **FR-018**: System MUST be configurable to enable/disable nightly execution via environment variable or configuration file.
- **FR-019**: System MUST create a discovery session record for each nightly run to track statistics and audit the process.

**Monitoring & Analytics:**
- **FR-020**: System MUST record for each nightly session: start time, end time, total queries executed, total results collected, unique domains found, high-confidence candidates, errors encountered.
- **FR-021**: System MUST track per-search-engine statistics: queries executed, results returned, average response time, failure count.
- **FR-022**: System MUST track per-query statistics: results per engine, unique domains discovered, high-confidence hit rate.
- **FR-023**: System MUST allow Kevin to query historical session data to identify which queries and search engines are most productive.

**Error Handling & Resilience:**
- **FR-024**: System MUST implement circuit breakers for each search engine to prevent cascading failures.
- **FR-025**: System MUST retry failed search requests up to 3 times with exponential backoff before logging a failure.
- **FR-026**: System MUST log all errors with sufficient context (query, engine, error message, timestamp) for troubleshooting.
- **FR-027**: System MUST complete the nightly run even if individual queries or engines fail, ensuring partial success rather than complete failure.

### Key Entities

**SearchQuery:**
- Represents a search query to be executed
- Attributes: query text, focus tags (geography, category, authority), target search engines, expected result count
- Organized by day-of-week for scheduling

**SearchEngine:**
- Represents a configured search engine integration
- Attributes: engine type (SEARXNG, BROWSERBASE, TAVILY, PERPLEXITY), base URL, API key (if required), enabled status, timeout configuration, circuit breaker state
- Tracks operational health and performance metrics

**SearchResult:**
- Represents a single result returned from a search engine
- Attributes: URL, title, snippet, domain (extracted), search engine source, originating query, result position, timestamp
- Validated and deduplicated before processing

**DiscoverySession:**
- Represents a single nightly crawler execution
- Attributes: session ID, start time, end time, queries executed, total results collected, unique domains found, high-confidence candidates, errors encountered, per-engine statistics, per-query statistics
- Used for monitoring, analytics, and process auditing

**Domain:**
- Represents a unique domain discovered across all search sessions
- Attributes: domain name, first discovered date, times rediscovered, blacklisted status, total candidates from this domain
- Prevents duplicate processing and tracks domain quality over time

---

## Success Criteria

1. **Infrastructure Validation**: All 4 search engines successfully integrated and returning results within expected timeframes (< 15 seconds per query).

2. **Deduplication Effectiveness**: System processes each unique domain only once per night, with deduplication rate of 60-80% (typical overlap between search engines).

3. **High-Quality Candidate Discovery**: Nightly runs discover 50-150 high-confidence candidates (e 0.60 score) ready for Phase 2 crawling.

4. **Reliability**: Nightly crawler completes successfully 95%+ of the time, with graceful degradation when individual engines fail.

5. **Learning Insights**: After 2-4 weeks of operation, Kevin can identify:
   - Which search engines have the highest hit rate (% of results that score e 0.60)
   - Which query patterns discover the most unique, high-quality domains
   - Which combinations of query + engine are most cost-effective

6. **Performance**: Full nightly run completes within 30 minutes for 10 queries × 4 engines × 25 results = 1000 raw results.

---

## Out of Scope

The following are explicitly NOT part of this feature:

1. **AI-Powered Query Generation**: This MVP uses hardcoded queries only. AI-generated queries using LM Studio will be Feature 004.

2. **Complex Taxonomy**: No implementation of 60+ GeographicScope values or 25+ FundingCategory enums. This feature uses simple string tags for query organization.

3. **Framelets & Adaptive Scheduling**: No advanced scheduling algorithms or performance-based query selection. Fixed day-of-week query sets only.

4. **Multi-Dimensional Metadata Scoring**: Uses existing 4-judge metadata scoring system. Enhanced context-aware scoring will be addressed in future features.

5. **Phase 2 Web Crawling**: This feature discovers and scores candidates only. Actual website crawling for content extraction is out of scope.

6. **Dashboard UI**: No user interface for managing queries or reviewing results. Command-line/database queries only for this MVP.

---

## Dependencies & Assumptions

### Dependencies
- **Existing Infrastructure**: PostgreSQL database, existing Domain and FundingSourceCandidate tables, existing MetadataJudgingService from Feature 002.
- **Mac Studio Services**: Searxng running on Mac Studio (192.168.1.10:8080).
- **API Keys**: Valid API keys for Browserbase, Tavily, and Perplexity configured in environment variables.
- **Network Connectivity**: Reliable network access to external search APIs.

### Assumptions
- Search engines return results in consistent JSON/XML format that can be parsed reliably.
- Domain extraction from URLs follows standard patterns (http://domain.com/path ’ domain.com).
- Existing metadata scoring service can handle increased volume from nightly automated discovery.
- 25 results per query per engine is sufficient for discovering diverse, high-quality candidates.
- Nightly execution at 2:00 AM provides sufficient time (30-60 minutes) before business hours.

---

## Review & Acceptance Checklist

### Content Quality
- [x] No implementation details (languages, frameworks, APIs) in requirements
- [x] Focused on user value and business needs (discovering funding sources, learning what works)
- [x] Written for non-technical stakeholders (requirements understandable without coding knowledge)
- [x] All mandatory sections completed

### Requirement Completeness
- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous (all have measurable acceptance criteria)
- [x] Success criteria are measurable (specific metrics: 50-150 candidates, 95% reliability, 30-minute completion)
- [x] Scope is clearly bounded (MVP approach, out-of-scope section)
- [x] Dependencies and assumptions identified

### Constitutional Alignment
- [x] Aligns with Human-AI Collaboration principle (automated discovery, human review of results)
- [x] Follows Complexity Management (MVP approach, simple before complex)
- [x] Supports Three-Workflow Architecture (Funding Discovery workflow)
- [x] Respects Technology Constraints (notes on implementation will reference constitution)

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (and subsequently clarified based on MVP approach)
- [x] User scenarios defined
- [x] Requirements generated (27 functional requirements)
- [x] Entities identified (5 key entities)
- [x] Review checklist passed
- [x] SUCCESS - Specification ready for /plan phase

---

## Notes for Planning Phase

This specification intentionally avoids implementation details. The planning phase should address:

- Technology choices (HTTP clients, JSON parsing libraries, circuit breaker configuration)
- Package structure and class design
- Database schema changes (if any beyond existing tables)
- Test strategy (unit tests, integration tests with TestContainers)
- Constitutional compliance details (Vavr for error handling, Virtual Threads for parallelism, Resilience4j circuit breakers)
- Hardcoded query library format (YAML, properties file, or Java constants)
