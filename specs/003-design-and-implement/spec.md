# Feature Specification: Search Provider Adapters

**Feature Branch**: `003-design-and-implement`
**Created**: 2025-11-01
**Completed**: 2025-11-02
**Status**: ✅ COMPLETED - Merged to main
**Input**: User description: "Design and implement search provider adapters for automated funding discovery: BraveSearchAdapter for traditional web search, SearxngAdapter for self-hosted metasearch (192.168.1.10:8080), SerperAdapter for Google Search API, and TavilyAdapter for AI-optimized search. Each adapter implements SearchProviderAdapter interface with executeSearch method, supports Virtual Thread parallel execution using CompletableFuture, consumes keyword queries for traditional search engines (Brave/Searxng/Serper) or AI-optimized queries for Tavily, returns List<SearchResult> using existing northstar-domain SearchResult entity, handles rate limiting and error conditions, implements result normalization (domain extraction, deduplication), and supports configurable result limits. Adapters use java.net.http.HttpClient (NO WebFlux/Reactive), integrate with existing Domain entity for deduplication, support concurrent execution of 4 search providers in parallel, and use Vavr Try monad for functional error handling"

## Execution Flow (main)
```
1. Parse user description from Input
   � Extracted: 4 search provider adapters, interface design, parallel execution
2. Extract key concepts from description
   � Actors: System executing funding discovery searches
   � Actions: Execute searches, normalize results, handle errors, deduplicate
   � Data: Search queries (keyword/AI-optimized), SearchResult entities, Domain entities
   � Constraints: No WebFlux/Reactive, use java.net.http.HttpClient, Virtual Threads
3. For each unclear aspect:
   � [NEEDS CLARIFICATION: API keys/credentials management for BraveSearch, Serper, Tavily]
   � [NEEDS CLARIFICATION: Rate limiting thresholds per provider]
   � [NEEDS CLARIFICATION: Retry strategy for transient failures]
   � [NEEDS CLARIFICATION: How to handle partial search failures (some providers succeed, others fail)]
4. Fill User Scenarios & Testing section
   � Primary scenario: Execute funding discovery search across multiple providers
5. Generate Functional Requirements
   � 40 testable requirements identified
6. Identify Key Entities
   � SearchResult, Domain (existing in northstar-domain)
7. Run Review Checklist
   � WARN "Spec has uncertainties - clarifications needed for production deployment"
8. Return: SUCCESS (spec ready for planning with noted clarifications)
```

---

## � Quick Guidelines
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
As the NorthStar Funding Discovery system, I need to search multiple search engines concurrently to find potential funding sources for educational organizations, so that I can discover a comprehensive set of funding opportunities from diverse web sources while minimizing total search time through parallel execution.

### Acceptance Scenarios

1. **Given** the system has 4 configured search providers (BraveSearch, SearXNG, Serper, Tavily),
   **When** the system executes a funding discovery search with query "Bulgaria education infrastructure grants",
   **Then** all 4 providers execute searches concurrently and return combined results within the maximum timeout period.

2. **Given** BraveSearch, SearXNG, and Serper receive a keyword query "Bulgaria education grants",
   **When** the system executes the search,
   **Then** each traditional search provider formats the query appropriately for its API and returns normalized SearchResult entities.

3. **Given** Tavily receives an AI-optimized query "Educational infrastructure funding opportunities for modernizing schools in Bulgaria",
   **When** the system executes the search,
   **Then** Tavily processes the natural language query and returns relevant funding source results.

4. **Given** the system discovers 25 search results across 4 providers,
   **When** result normalization executes,
   **Then** the system extracts domains from all URLs, checks against existing Domain entities for deduplication, and returns only unique funding source candidates.

5. **Given** SearXNG is running at 192.168.1.10:8080,
   **When** the SearxngAdapter executes a search,
   **Then** the adapter connects to the local SearXNG instance and retrieves metasearch results.

6. **Given** one search provider (Serper) fails due to API rate limiting,
   **When** the system executes a multi-provider search,
   **Then** the other 3 providers complete successfully and the system returns partial results with error metadata for the failed provider.

7. **Given** a search provider returns 100 results but the configured limit is 20,
   **When** the system processes the response,
   **Then** the adapter returns only the top 20 results based on the provider's relevance ranking.

8. **Given** multiple search results point to the same domain (us-bulgaria.org),
   **When** the system performs domain deduplication,
   **Then** the system retains only the highest-confidence result for that domain.

9. **Given** a search result with title "grants scholarships funding grants education grants financial aid grants" (keyword stuffing),
   **When** the anti-spam filter analyzes the result,
   **Then** the system detects low unique word ratio (< 0.5) and marks the result as spam before any LLM processing.

10. **Given** a search result from domain "casinowinners.com" with title "Education Scholarships and Grants for Students",
    **When** the anti-spam filter detects domain-metadata mismatch,
    **Then** the system rejects the result as cross-category spam (gambling domain with education keywords) and prevents downstream processing.

### Edge Cases

- **What happens when all search providers fail simultaneously?**
  System returns empty result set with error metadata for each provider, allowing upstream components to decide on retry strategy.

- **What happens when a search provider returns malformed JSON?**
  Adapter catches parsing exception, logs error details, marks provider as failed for this search, returns empty result set for that provider.

- **What happens when SearXNG at 192.168.1.10:8080 is unreachable?**
  SearxngAdapter times out after configured timeout period, returns error result indicating connectivity failure.

- **What happens when two providers return the same URL but with different metadata (title, description)?**
  System keeps the result with better search engine ranking (lower position number), or if positions are equal, keeps the first result discovered. **Note**: Confidence scoring happens later in northstar-judging module.

- **What happens when a provider exceeds rate limits mid-search?**
  Adapter returns partial results received before rate limit, marks response as incomplete, includes rate limit error metadata.

- **How does the system handle searches with no results from any provider?**
  System returns empty result list with metadata indicating "no results found" across all providers.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Interface Design
- **FR-001**: System MUST define SearchProviderAdapter interface with executeSearch method accepting query string and maximum result count
- **FR-002**: SearchProviderAdapter interface MUST declare method to identify provider type (BRAVE_SEARCH, SEARXNG, SERPER, TAVILY)
- **FR-003**: SearchProviderAdapter interface MUST declare method to indicate whether provider supports keyword queries
- **FR-004**: SearchProviderAdapter interface MUST declare method to indicate whether provider supports AI-optimized queries

#### BraveSearchAdapter
- **FR-005**: System MUST implement BraveSearchAdapter for traditional web search using Brave Search API
- **FR-006**: BraveSearchAdapter MUST accept keyword queries (short, focused keyword phrases)
- **FR-007**: BraveSearchAdapter MUST return search results as List<SearchResult> using existing northstar-domain SearchResult entity
- **FR-008**: BraveSearchAdapter MUST handle API authentication [NEEDS CLARIFICATION: API key storage and rotation strategy]
- **FR-009**: BraveSearchAdapter MUST respect configurable result limits (default 20 results per search)

#### SearxngAdapter
- **FR-010**: System MUST implement SearxngAdapter for self-hosted metasearch at 192.168.1.10:8080
- **FR-011**: SearxngAdapter MUST accept keyword queries formatted for SearXNG API
- **FR-012**: SearxngAdapter MUST connect to local SearXNG instance without authentication (self-hosted, trusted network)
- **FR-013**: SearxngAdapter MUST return aggregated results from SearXNG's configured search engines
- **FR-014**: SearxngAdapter MUST handle connection failures to local SearXNG instance with timeout

#### SerperAdapter
- **FR-015**: System MUST implement SerperAdapter for Google Search API access via Serper service
- **FR-016**: SerperAdapter MUST accept keyword queries formatted for Google-style search
- **FR-017**: SerperAdapter MUST handle API authentication [NEEDS CLARIFICATION: API key storage and rotation strategy]
- **FR-018**: SerperAdapter MUST respect Google Search API rate limits [NEEDS CLARIFICATION: specific rate limit thresholds]

#### TavilyAdapter
- **FR-019**: System MUST implement TavilyAdapter for AI-optimized search
- **FR-020**: TavilyAdapter MUST accept AI-optimized queries (longer, conceptual descriptions with context)
- **FR-021**: TavilyAdapter MUST handle API authentication [NEEDS CLARIFICATION: API key storage and rotation strategy]
- **FR-022**: TavilyAdapter MUST process natural language queries and return contextually relevant funding sources

#### Parallel Execution
- **FR-023**: System MUST support concurrent execution of all 4 search providers in parallel
- **FR-024**: System MUST use Virtual Thread parallel execution via CompletableFuture for I/O-bound search operations
- **FR-025**: System MUST aggregate results from all providers into single combined result set
- **FR-026**: System MUST complete multi-provider search within configurable timeout period [NEEDS CLARIFICATION: timeout threshold per provider and total timeout]

#### Error Handling
- **FR-027**: Each adapter MUST use functional error handling pattern to encapsulate success/failure states
- **FR-028**: System MUST handle partial failures (some providers succeed, others fail) by returning successful results with error metadata for failed providers
- **FR-029**: System MUST log detailed error information for failed searches including provider name, query, error message, timestamp
- **FR-030**: System MUST retry transient failures [NEEDS CLARIFICATION: retry count, backoff strategy, which error types trigger retry]

#### Result Normalization
- **FR-031**: Each adapter MUST extract domain from result URLs using consistent domain extraction logic
- **FR-032**: System MUST normalize domains (lowercase, remove www prefix, remove protocol) for deduplication
- **FR-033**: System MUST check extracted domains against existing Domain entities to prevent reprocessing
- **FR-034**: System MUST populate SearchResult entities with provider type, query used, position in results, discovery timestamp (confidence scoring handled by northstar-judging module)

#### Anti-Spam Filtering (Critical for Resource Conservation)
- **FR-035**: System MUST detect keyword stuffing by analyzing unique word ratio in title and description (threshold: < 0.5 unique ratio indicates spam)
- **FR-036**: System MUST detect domain-metadata mismatch using fuzzy string matching (domain keywords vs title/description keywords, threshold: < 0.15 similarity indicates scammer)
- **FR-037**: System MUST detect unnatural keyword list patterns (absence of common articles/prepositions like "the", "a", "of", "for" indicates keyword stuffing)
- **FR-038**: System MUST filter known scammer domain patterns (gambling: "casino", "poker", "betting", "win"; fake degrees: "diploma", "essay"; etc.)
- **FR-039**: System MUST detect cross-category spam (gambling domains with education keywords, essay mill domains with scholarship keywords)
- **FR-040**: System MUST mark spam-detected results with rejection reason for monitoring and blacklist enhancement
- **FR-041**: Anti-spam filtering MUST execute before domain deduplication check to avoid caching spam domains
- **FR-042**: System MUST use Apache Commons Text or equivalent fuzzy matching library for spam detection (Levenshtein distance, Cosine similarity, FuzzyScore)

#### Rate Limiting
- **FR-043**: Each adapter MUST implement rate limiting to respect provider-specific API quotas [NEEDS CLARIFICATION: rate limits per provider]
- **FR-044**: System MUST handle rate limit errors gracefully by returning partial results and error metadata
- **FR-045**: System MUST track API usage per provider for monitoring and quota management [NEEDS CLARIFICATION: tracking mechanism - in-memory, database, external service]

#### Configuration
- **FR-046**: System MUST support configurable result limits per provider (default 20, maximum 100)
- **FR-047**: System MUST support configurable timeout per provider [NEEDS CLARIFICATION: timeout values]
- **FR-048**: System MUST support provider enable/disable configuration for testing or quota management

### Key Entities *(existing in northstar-domain)*

- **SearchResult**: Represents a single search result from any provider
  - Attributes: URL, title, description, domain, search engine type, discovery session ID, query used, position in results, discovered timestamp
  - **Note**: Does NOT include confidence score - that's calculated later by northstar-judging module based on metadata analysis
  - Relationships: Links to Domain entity for deduplication, links to DiscoverySession for tracking

- **Domain**: Represents a unique funding organization domain for deduplication
  - Attributes: Domain name, status (DISCOVERED, PROCESSED_HIGH_QUALITY, etc.), quality metrics, blacklist status
  - Purpose: Prevents reprocessing same organization discovered from multiple search results

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

### Content Quality
- [x] No implementation details (languages, frameworks, APIs) - **NOTE**: Feature description included implementation constraints (HttpClient, Virtual Threads, Vavr) which are architectural requirements, not arbitrary implementation choices
- [x] Focused on user value and business needs - Enables comprehensive funding discovery across multiple search engines
- [x] Written for non-technical stakeholders - User scenarios describe search execution and result aggregation
- [x] All mandatory sections completed

### Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain - **7 clarifications needed**:
  1. API key storage and rotation strategy (FR-008, FR-017, FR-021)
  2. Rate limiting thresholds per provider (FR-018, FR-035)
  3. Retry strategy parameters (FR-030)
  4. Timeout thresholds per provider and total (FR-026, FR-039)
  5. Partial failure handling strategy (FR-028)
  6. API usage tracking mechanism (FR-037)
- [x] Requirements are testable and unambiguous - Each FR specifies concrete capability
- [x] Success criteria are measurable - Scenarios include specific outcomes (concurrent execution, result counts, error handling)
- [x] Scope is clearly bounded - 4 search provider adapters with defined interface
- [x] Dependencies and assumptions identified - Depends on existing SearchResult and Domain entities in northstar-domain

---

## Execution Status
*Updated by main() during processing*

- [x] User description parsed
- [x] Key concepts extracted
- [x] Ambiguities marked (7 clarifications needed)
- [x] User scenarios defined (8 acceptance scenarios, 6 edge cases)
- [x] Requirements generated (40 functional requirements)
- [x] Entities identified (SearchResult, Domain - existing)
- [x] Review checklist passed with warnings (clarifications needed for production)

---

## Dependencies & Assumptions

### Dependencies
- **northstar-domain module**: SearchResult and Domain entities must exist
- **SearchEngineType enum**: Must include BRAVE_SEARCH, SEARXNG, SERPER, TAVILY values
- **External APIs**: BraveSearch API, Serper API, Tavily API must be accessible
- **Local Infrastructure**: SearXNG metasearch instance must be running at 192.168.1.10:8080
- **Apache Commons Text**: Required for fuzzy string matching in anti-spam filtering (Levenshtein distance, Cosine similarity, FuzzyScore)

### Assumptions
- Search providers return results in predictable JSON formats that can be parsed into SearchResult entities
- SearXNG instance at 192.168.1.10:8080 is trusted (no authentication required)
- Domain extraction can reliably identify funding organization domains from result URLs
- Virtual Threads are available (Java 21+ with Virtual Thread support)
- Search provider metadata (title, description, URL, position) is sufficient for downstream judging module to calculate confidence scores

---

## Out of Scope

The following are explicitly **NOT** included in this feature:

- Query generation (generating keyword vs AI-optimized queries) - handled by separate LLM query generation component
- Metadata judging (AI-powered confidence scoring) - handled by separate judging component
- Deep web crawling (scraping discovered URLs) - Phase 2 functionality
- Contact intelligence extraction (emails, phones, names) - Phase 2 functionality
- Search result storage/persistence - handled by upstream orchestration component
- Search scheduling (when to run searches) - handled by scheduler component
- API key management UI - configuration managed externally

---

## Success Metrics

- **Parallel Execution Performance**: All 4 providers complete searches within 5 seconds (90th percentile)
- **Result Coverage**: Combined results from all providers yield 20-50 unique funding source candidates per query
- **Anti-Spam Filtering Effectiveness**: Spam detection filters out 40-60% of scammer/SEO spam results before downstream processing (based on spring-crawler experience with keyword stuffing and domain-metadata mismatches)
- **Resource Conservation**: Anti-spam filtering reduces downstream LLM processing load by 40-60%, saving significant CPU time and preventing blacklist pollution
- **Deduplication Effectiveness**: Domain deduplication reduces result set by 30-50% (detecting cross-provider duplicates)
- **Availability**: Each individual provider maintains >95% success rate over 7-day period
- **Partial Failure Resilience**: Multi-provider searches succeed (return results from working providers) even when 1-2 providers fail

---
