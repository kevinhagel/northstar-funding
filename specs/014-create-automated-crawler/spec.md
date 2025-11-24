# Feature Specification: Automated Search Adapter Infrastructure

**Feature Branch**: `014-create-automated-crawler`
**Created**: 2025-11-17
**Completed**: 2025-11-24
**Status**: ✅ COMPLETE (scheduling deferred to Feature 016)
**Input**: User description: "Create automated crawler infrastructure for Phase 1 metadata judging: system generates AI-powered search queries to discover funding sources across multiple search engines (Searxng, Tavily, Browserbase, Perplexity), performs domain-level deduplication to prevent reprocessing same domains, judges candidates based on search engine metadata only (no web crawling) using confidence scoring with funding keywords, domain credibility, geographic relevance, and organization type detection, registers domains with blacklist management and quality tracking, creates high-confidence candidates (>= 0.6) as PENDING_CRAWL status for Phase 2 deep crawling, skips low-confidence results to save processing resources, tracks domain quality metrics for continuous improvement, and uses Java 25 Virtual Threads for parallel processing of 20-25 search results per query with simple orchestrator pattern (no Kafka, no Spring Integration)"

## Execution Flow (main)
```
1. Parse user description from Input
   � Extracted: search adapters, metadata judging, confidence scoring, domain deduplication
2. Extract key concepts from description
   � Actors: system (automated), search engines, domain candidates
   � Actions: generate queries, execute searches, score confidence, register domains
   � Data: search results, domains, funding candidates, confidence scores
   � Constraints: >= 0.6 threshold, no web crawling, Phase 1 only
3. For each unclear aspect:
   � Resolved: 30 FundingSearchCategory enum values provide search topics
   � Resolved: Nightly searches with 7-day rotation of category groups
   � Resolved: All 4 search adapters used each night, categories distributed across them
4. Fill User Scenarios & Testing section
   � Scenario: System executes nightly search for funding source candidates, discovers them, scores them
5. Generate Functional Requirements
   � All requirements testable via automated integration tests
6. Identify Key Entities
   � SearchResult, Domain, FundingSourceCandidate, ConfidenceScore
7. Run Review Checklist
   � WARN "Spec has uncertainties - see NEEDS CLARIFICATION markers"
8. Return: SUCCESS (spec ready for planning)
```

---

## � Quick Guidelines
-  Focus on WHAT users need and WHY
- L Avoid HOW to implement (no tech stack, APIs, code structure)
- =e Written for business stakeholders, not developers

---

## User Scenarios & Testing

### Primary User Story
As a funding discovery system, I need to automatically search multiple search engines nightly to discover new **funding source candidates** (organizations, programs, and websites that provide funding), evaluate their relevance using metadata only (without crawling), deduplicate domains to avoid reprocessing, and create high-confidence candidates for later detailed crawling, so that the human review team can focus on the most promising funding sources.

**Key Clarification**: We are searching FOR funding sources, not searching for people/organizations who need funding. The goal is to discover websites and organizations that PROVIDE grants, scholarships, fellowships, and other funding opportunities.

### Acceptance Scenarios

1. **Given** the system has a list of funding categories to search, **When** a nightly search is triggered, **Then** the system generates relevant queries, executes searches across configured engines, scores results by confidence, deduplicates by domain, and creates candidates for results >= 0.6 confidence

2. **Given** a search result with URL "https://ec.europa.eu/programmes/horizon-europe/", **When** this domain has been processed before, **Then** the system skips this result and does not create a duplicate candidate

3. **Given** a search result has a confidence score of 0.85 (high confidence), **When** the domain is not blacklisted, **Then** the system creates a FundingSourceCandidate with status PENDING_CRAWL

4. **Given** a search result has a confidence score of 0.45 (low confidence), **When** evaluating whether to create a candidate, **Then** the system skips this result to save processing resources

5. **Given** multiple search engines are configured (Searxng, Tavily, Browserbase, Perplexity), **When** one engine fails, **Then** the system continues processing results from other engines and logs the failure

6. **Given** a domain is on the blacklist, **When** a search result points to that domain, **Then** the system skips this result and increments blacklist hit metrics

### Edge Cases

- What happens when all search engines fail simultaneously?
  � System logs error, sends alert, and schedules retry

- What happens when a search returns zero results?
  � This is NOT an error - system records that this query+adapter combination produced zero results, tracks this for query/adapter effectiveness analysis, continues with remaining queries

- What happens when confidence scoring produces invalid scores (< 0.0 or > 1.0)?
  � System logs error, uses fallback score of 0.0, continues processing

- What happens when the same URL appears in results from multiple search engines?
  � System deduplicates by domain, uses the highest confidence score from any engine

- What happens when a domain was previously blacklisted but now appears in results?
  � System respects blacklist, skips result, tracks attempted reappearance

- What happens when search query generation fails?
  � System logs error, uses fallback static queries for that category, continues with remaining categories

## Requirements

### Functional Requirements

#### Search Execution
- **FR-001**: System MUST generate search queries using AI-powered query generation for configured funding categories
- **FR-002**: System MUST execute searches across multiple search engines (Searxng, Tavily, Browserbase, Perplexity)
- **FR-003**: System MUST handle search engine failures gracefully without aborting the entire search session
- **FR-004**: System MUST track which search engines were queried and their response status for each session

#### Metadata Judging & Confidence Scoring
- **FR-005**: System MUST evaluate search results using metadata only (title, description, URL) without web crawling
- **FR-006**: System MUST calculate confidence scores (0.0 to 1.0 scale) based on:
  - Presence of funding keywords (grants, scholarships, fellowships, etc.)
  - Domain credibility (TLD analysis: .gov, .edu, .org, etc.)
  - Geographic relevance (Bulgaria, EU, Eastern Europe keywords)
  - Organization type detection (Ministry, Commission, Foundation, University)
- **FR-007**: System MUST apply a confidence threshold of >= 0.6 to determine which results become candidates
- **FR-008**: System MUST track confidence score distribution metrics for continuous improvement

#### Domain Management
- **FR-009**: System MUST extract domain from each search result URL for deduplication
- **FR-010**: System MUST check if domain has been processed before in current session (in-memory deduplication)
- **FR-011**: System MUST check if domain has been processed in previous sessions (database lookup)
- **FR-012**: System MUST register new domains with quality tracking metadata (first seen date, source engine, initial confidence)
- **FR-013**: System MUST check domain against blacklist before creating candidates
- **FR-014**: System MUST track blacklist hit metrics (how many results were blocked by blacklist)

#### Candidate Creation
- **FR-015**: System MUST create FundingSourceCandidate records for high-confidence results (>= 0.6)
- **FR-016**: System MUST set candidate status to PENDING_CRAWL for Phase 2 processing
- **FR-017**: System MUST store metadata (title, description, URL, confidence score, source engine) with each candidate
- **FR-018**: System MUST skip low-confidence results (< 0.6) to save processing resources
- **FR-019**: System MUST track how many candidates were created vs. skipped

#### Session & Metrics Tracking
- **FR-020**: System MUST create a discovery session record for each search execution
- **FR-021**: System MUST track session statistics:
  - Number of queries generated
  - Number of search engines queried
  - Total search results received
  - Number of duplicates skipped
  - Number of blacklisted domains encountered
  - Number of high-confidence candidates created
  - Number of low-confidence results skipped
  - Average confidence score
  - Session duration
- **FR-022**: System MUST track zero-result outcomes per query+adapter combination for effectiveness analysis (zero results is NOT an error)
- **FR-023**: System MUST persist session statistics for historical analysis

#### Scheduling & Automation
**DEFERRED TO FEATURE 016**: Automated nightly scheduling with Spring @Scheduled annotations will be implemented in a future feature. Feature 014 provides the manual execution infrastructure via SearchWorkflowService.

- **FR-024**: ~~System MUST execute searches nightly (every night)~~ → DEFERRED
- **FR-025**: ~~System MUST distribute 30 FundingSearchCategory values across 7 days (Monday through Sunday rotation)~~ → DEFERRED
- **FR-026**: ~~System MUST use all 4 search adapters each night, distributing categories across adapters~~ → DEFERRED
- **FR-027**: ~~System MUST track which categories work best with which adapters over time for optimization~~ → DEFERRED
- **FR-028**: System MUST support manual trigger for ad-hoc searches → **IMPLEMENTED** (SearchWorkflowService.executeManualSearch)

#### Error Handling & Resilience
- **FR-029**: System MUST continue processing if individual search engine fails
- **FR-030**: System MUST continue processing if individual query fails
- **FR-031**: System MUST continue processing if confidence scoring fails for individual result (use fallback score)
- **FR-032**: System MUST log all errors with context (query, engine, result URL, error message)
- **FR-033**: System MUST complete session even if partial failures occur

### Key Entities

- **SearchResult**: Represents a single result from a search engine, containing URL, title, description (metadata only), source engine, and discovery timestamp

- **Domain**: Represents a unique domain (e.g., "ec.europa.eu"), tracking registration date, quality metrics, blacklist status, and processing history

- **FundingSourceCandidate**: Represents a high-confidence funding opportunity discovered by search, containing metadata, confidence score, status (PENDING_CRAWL), and source domain

- **ConfidenceScore**: Numeric value (0.0 to 1.0) calculated from multiple signals (keywords, domain credibility, geography, organization type) to predict funding source quality

- **DiscoverySession**: Represents a single execution of the search workflow, tracking queries generated, engines queried, results processed, candidates created, and session statistics

- **DomainQualityMetrics**: Tracks quality indicators for each domain (confidence scores over time, candidate creation rate, blacklist hits) for continuous improvement

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
- [x] Scope is clearly bounded (Phase 1 metadata judging only, no web crawling)
- [x] Dependencies and assumptions identified (requires query generation module, persistence layer, search engine availability, 30 FundingSearchCategory enum values)

---

## Execution Status

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities resolved (30 FundingSearchCategory values, nightly execution, 7-day rotation, all adapters used nightly)
- [x] User scenarios defined
- [x] Requirements generated (33 functional requirements)
- [x] Entities identified (6 key entities)
- [x] Review checklist passed

---

## Resolved Questions

1. **Search Frequency**: ✅ Nightly execution (every night)
2. **Category Schedule**: ✅ 30 FundingSearchCategory values distributed across 7 days (Monday-Sunday rotation)
3. **Adapter Distribution**: ✅ All 4 search adapters (Searxng, Tavily, Browserbase, Perplexity) used each night with categories distributed across them
4. **Fallback Queries**: ✅ If AI query generation fails, use fallback static queries for that category
5. **Adapter Optimization**: ✅ Track which categories work best with which adapters over time for continuous improvement

## Implementation Status

### ✅ Completed in Feature 014
1. SearchAdapter interface and 4 implementations (Brave, SearXNG, Serper, Perplexica)
2. SearchWorkflowService with Virtual Threads for parallel execution
3. Manual search trigger via SearchWorkflowService.executeManualSearch()
4. Complete pipeline: Query Generation → Search → Processing → Candidate Creation
5. Domain deduplication and confidence scoring
6. 28/29 tests passing (96.5% pass rate)

### ⏳ Deferred to Future Features
1. **Feature 016**: Automated nightly scheduling with @Scheduled annotations
2. **Feature 015**: Perplexica + LM Studio integration (replace Ollama)
3. **Future**: Rate limiting, retry logic, metrics dashboard

### 📝 Architecture Notes
- **Ollama Removed**: Claimed parallelism support but failed in practice with Perplexica
- **LM Studio Standard**: Proven reliability with Perplexica, now the standard
- **4 Search Providers**: Brave, SearXNG, Serper, Perplexica (Tavily removed 2025-11-23)
