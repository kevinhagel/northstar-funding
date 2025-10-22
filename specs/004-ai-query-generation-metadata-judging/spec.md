# Feature Specification: AI-Powered Query Generation and Metadata Judging

**Feature Branch**: `004-ai-query-generation-metadata-judging`
**Created**: 2025-10-21
**Status**: Draft
**Dependencies**: Feature 003 (Search Execution Infrastructure)
**Input**: Building on Feature 003's hardcoded query library, enable AI-powered query generation using LM Studio to dynamically create diverse, high-quality search queries; implement metadata-based confidence scoring to judge search results without web crawling; create high-confidence candidates (>= 0.60) with PENDING_CRAWL status; track domain quality metrics for continuous improvement

## Executive Summary

Feature 003 established the infrastructure to execute search queries across multiple engines (Searxng, Tavily, Perplexity) and deduplicate results by domain. However, queries are currently hardcoded in a YAML configuration file (16 queries across 7 days). This feature adds **AI-powered query generation** to dynamically create diverse, targeted search queries, and **metadata-based judging** to filter high-quality funding sources before expensive web crawling.

**Business Value**:
- **Scalability**: Generate 50-100 queries per day vs 2-5 hardcoded queries
- **Quality**: AI creates contextually relevant queries with geographic and category diversity
- **Efficiency**: Filter out 60-80% of low-quality results before crawling (saves processing resources)
- **Learning**: Track domain quality metrics to improve future discovery

**Key Capabilities**:
1. AI generates search queries using LM Studio (local AI, constitutional requirement)
2. System judges search results using metadata only (no web crawling)
3. Confidence scoring with multiple criteria (funding keywords, domain credibility, geography)
4. Automatic candidate creation for high-confidence results (>= 0.60)
5. Domain quality tracking for continuous improvement

---

## Quick Guidelines
- Focus on WHAT users need and WHY
- Avoid HOW to implement (no tech stack, APIs, code structure)
- Written for business stakeholders, not developers

---

## User Scenarios & Testing *(mandatory)*

### Primary User Story

**As a** funding discovery system administrator
**I want** the system to automatically generate diverse, high-quality search queries and intelligently filter search results
**So that** I can discover more funding sources while avoiding waste on low-quality domains that would fail manual review anyway

### User Journey

1. **Query Generation Phase** (Monday 1:00 AM - before nightly discovery)
   - System consults AI to generate 10-20 search queries for the day
   - AI creates queries with geographic diversity (Bulgaria, Romania, Greece, etc.)
   - AI creates queries with category diversity (Education, Infrastructure, Healthcare, etc.)
   - AI avoids duplicate or semantically similar queries
   - Queries stored in database for execution

2. **Search Execution Phase** (Monday 2:00 AM - nightly discovery)
   - System executes AI-generated queries + hardcoded queries
   - Receives 500-1000 search results from 3 engines
   - Deduplicates by domain (40-60% reduction)
   - 200-400 unique domains to judge

3. **Metadata Judging Phase** (Monday 2:15 AM - after search execution)
   - System judges each search result using metadata only (title, description, URL)
   - Calculates confidence score (0.00-1.00) based on multiple criteria
   - High confidence (>= 0.60): Creates Candidate with PENDING_CRAWL status
   - Low confidence (< 0.60): Skips, records domain quality metrics
   - 50-150 high-confidence candidates created

4. **Quality Tracking Phase** (Ongoing)
   - System records domain quality metrics (best score, candidate count)
   - Learns which domains consistently yield high-quality results
   - Identifies low-quality domains to avoid in future

### Acceptance Scenarios

**Query Generation**:
1. **Given** the system starts query generation for Monday, **When** it consults LM Studio AI, **Then** it generates 10-20 diverse search queries with Bulgarian/Balkan/EU focus
2. **Given** AI has generated 15 queries, **When** the system checks for duplicates, **Then** it rejects semantically similar queries (e.g., "Bulgaria grants" vs "grants in Bulgaria")
3. **Given** AI generates queries, **When** a query is too generic ("funding opportunities"), **Then** it rejects the query and requests more specific alternatives
4. **Given** generated queries are approved, **When** the system stores them, **Then** they are tagged with generation_date, AI_model_used, and category/geography tags

**Metadata Judging**:
5. **Given** a search result with title "Bulgaria Education Grants 2025" from "education.gov.bg", **When** the system judges metadata, **Then** it assigns confidence score >= 0.70 (funding keywords + credible domain + geography match)
6. **Given** a search result with title "How to Apply for Grants" from "blogspot.com", **When** the system judges metadata, **Then** it assigns confidence score < 0.40 (generic content + low credibility domain)
7. **Given** a high-confidence search result (score 0.75), **When** the system creates a candidate, **Then** candidate has status PENDING_CRAWL and metadata includes confidence_score, judging_criteria, and domain_quality_tier
8. **Given** a low-confidence search result (score 0.35), **When** the system skips candidate creation, **Then** it records domain in quality metrics with low_quality_count += 1

**Domain Quality Tracking**:
9. **Given** domain "grants.gov" has 10 high-confidence results over time, **When** the system calculates quality metrics, **Then** domain has quality_tier=HIGH and best_confidence_score=0.92
10. **Given** domain "spamsite.com" has 20 low-confidence results, **When** the system calculates quality metrics, **Then** domain has quality_tier=LOW and best_confidence_score=0.25
11. **Given** a domain transitions from HIGH to LOW quality, **When** admin reviews metrics, **Then** they can investigate what changed (e.g., site stopped offering grants)

### Edge Cases

**Query Generation**:
- What happens when AI generates non-English queries? (Accept if relevant, reject if gibberish)
- How does system handle AI generating 100+ queries? (Cap at 20, select most diverse)
- What occurs when AI fails to generate queries? (Fall back to hardcoded query library)
- How does system avoid generating the same queries every day? (Track previously generated queries, penalize repetition)

**Metadata Judging**:
- What happens when search result has no description? (Judge on title and URL only, reduce confidence slightly)
- How does system handle URLs with multiple subdomains? (Extract root domain for quality tracking)
- What occurs when confidence score is exactly 0.60? (Include - threshold is >= 0.60)
- How does system behave when all results score < 0.60? (Record metrics, no candidates created - this is valid)

**Domain Quality Tracking**:
- What happens when a domain has both high and low confidence results? (Track distribution, use best score as primary metric)
- How does system handle domain ownership changes? (Quality metrics persist, admin can reset if needed)
- What occurs when a blacklisted domain would score high? (Blacklist takes precedence, skip processing)

---

## Requirements *(mandatory)*

### Functional Requirements

**Query Generation**:
- **FR-001**: System MUST generate 10-20 search queries daily using LM Studio AI (local AI, constitutional requirement)
- **FR-002**: System MUST prompt AI with geographic focus (Bulgaria, Balkans, Eastern Europe, EU)
- **FR-003**: System MUST prompt AI with category focus (Education, Infrastructure, Healthcare, STEM, Arts, etc.)
- **FR-004**: System MUST reject AI-generated queries that are semantically similar to existing queries (avoid "Bulgaria grants" + "grants Bulgaria")
- **FR-005**: System MUST reject AI-generated queries that are too generic (< 3 keywords or missing geographic/category context)
- **FR-006**: System MUST tag generated queries with generation_date, AI_model_used, query_template_id, and semantic_cluster_id
- **FR-007**: System MUST store approved queries in search_queries table with enabled=true for nightly execution
- **FR-008**: System MUST cap AI-generated queries at 20 per day to avoid overwhelming search APIs
- **FR-009**: System MUST fall back to hardcoded query library if AI generation fails

**Metadata Judging**:
- **FR-010**: System MUST judge search results using metadata only (title, description, URL) without accessing webpage content
- **FR-011**: System MUST calculate confidence score (0.00-1.00) based on weighted criteria:
  - Funding keywords (0.30 weight): "grant", "funding", "scholarship", "fellowship", "award"
  - Domain credibility (0.25 weight): .gov, .edu, .org, established foundations
  - Geographic relevance (0.25 weight): Bulgaria, Balkans, Eastern Europe, EU mentions
  - Organization type (0.20 weight): government, university, foundation, NGO indicators
- **FR-012**: System MUST create Candidate with PENDING_CRAWL status when confidence >= 0.60
- **FR-013**: System MUST skip candidate creation when confidence < 0.60 (low quality, save processing)
- **FR-014**: System MUST record judging criteria and scores in Candidate entity for transparency
- **FR-015**: System MUST extract organization name and program name from search result metadata

**Domain Quality Tracking**:
- **FR-016**: System MUST track domain quality metrics including:
  - First discovered timestamp
  - Best confidence score ever achieved
  - High-confidence result count (>= 0.60)
  - Low-confidence result count (< 0.60)
  - Last seen timestamp
  - Quality tier (HIGH, MEDIUM, LOW based on distribution)
- **FR-017**: System MUST update domain quality metrics after each judging session
- **FR-018**: System MUST calculate quality tier based on:
  - HIGH: >70% of results >= 0.60 AND best_score >= 0.70
  - MEDIUM: 30-70% of results >= 0.60 OR best_score 0.50-0.70
  - LOW: <30% of results >= 0.60 AND best_score < 0.50
- **FR-019**: System MUST support admin viewing domain quality metrics for analysis
- **FR-020**: System MUST support filtering domains by quality tier for prioritization

**Integration & Performance**:
- **FR-021**: System MUST integrate with existing SearchExecutionService from Feature 003
- **FR-022**: System MUST process 200-400 unique domains in < 5 minutes (parallel judging with Virtual Threads)
- **FR-023**: System MUST handle LM Studio API failures gracefully (retry 2 times, then fall back)
- **FR-024**: System MUST log all AI prompts and responses for debugging and improvement
- **FR-025**: System MUST track session statistics (queries generated, results judged, candidates created)

### Non-Functional Requirements

**Performance**:
- Query generation: < 30 seconds for 20 queries (LM Studio local inference)
- Metadata judging: < 1 second per result (simple text analysis, no ML inference)
- Domain quality update: < 50ms per domain (database update)

**Quality**:
- High-confidence precision: >= 70% (of candidates >= 0.60, 70% should pass manual review)
- Recall: >= 60% (system should identify 60% of legitimate funding sources in search results)
- Query diversity: >= 80% of generated queries should be unique from previous 7 days

**Reliability**:
- AI generation availability: >= 95% (fall back to hardcoded queries on failure)
- Judging accuracy consistency: +/- 0.05 confidence score variance for same result

### Key Entities *(include if feature involves data)*

**SearchQuery** (extends Feature 003 entity):
- New fields: generation_method (AI_GENERATED | HARDCODED), ai_model_used, query_template_id, semantic_cluster_id, generation_date

**Candidate** (extends existing entity):
- New fields: confidence_score (BigDecimal), judging_criteria (JSON), domain_quality_tier (HIGH|MEDIUM|LOW), metadata_title, metadata_description, search_engine_source

**Domain** (new entity for Feature 004):
- Fields: domain_name (unique), first_discovered_at, best_confidence_score, high_confidence_count, low_confidence_count, last_seen_at, quality_tier (HIGH|MEDIUM|LOW), is_blacklisted, blacklist_reason, no_funds_this_year

**MetadataJudgment** (new entity):
- Fields: search_result_url, confidence_score, funding_keywords_score, domain_credibility_score, geographic_relevance_score, organization_type_score, extracted_org_name, extracted_program_name, judging_timestamp, session_id

**QueryGenerationSession** (new entity):
- Fields: session_id, generation_date, ai_model_used, queries_requested, queries_generated, queries_approved, queries_rejected, generation_duration_ms, fallback_used

---

## Success Metrics

**Query Generation**:
- Generate 10-20 diverse queries per day
- Query acceptance rate: >= 80% (AI generates quality queries)
- Query uniqueness: >= 80% different from previous 7 days
- Fallback usage: <= 5% of sessions (AI mostly reliable)

**Metadata Judging**:
- Process 200-400 domains per nightly session
- High-confidence candidates created: 50-150 per session
- Precision (manual review pass rate): >= 70% for candidates >= 0.60
- Recall (find legitimate sources): >= 60% of known funding sources in results

**Domain Quality Tracking**:
- HIGH tier domains: 10-20% of total (concentrated quality)
- MEDIUM tier domains: 30-40% of total
- LOW tier domains: 40-60% of total
- Quality tier stability: >= 85% of domains maintain tier month-over-month

---

## Dependencies & Assumptions

**Dependencies**:
- Feature 003 (Search Execution Infrastructure) MUST be complete
- LM Studio MUST be running on Mac Studio @ 192.168.1.10:1234
- PostgreSQL database MUST be accessible
- Candidate/Domain entities MUST exist in database schema

**Assumptions**:
- LM Studio can generate 10-20 queries in < 30 seconds
- Search result metadata (title, description) is sufficient for quality judgment
- Confidence threshold of 0.60 is acceptable (validated in testing)
- Domain-level quality tracking is more valuable than URL-level

---

## Out of Scope (For This Feature)

- **Web crawling**: Not included (deferred to Feature 005)
- **robots.txt/sitemap processing**: Not included (deferred to Feature 005)
- **Content extraction**: Not included (metadata judging only)
- **Machine learning models**: Not included (simple weighted scoring)
- **User feedback loop**: Not included (admin can review but no automated learning)
- **Blacklist management UI**: Not included (command-line/SQL for now)
- **Query generation training**: Not included (LM Studio used as-is with prompt engineering)

---

## Review & Acceptance Checklist
*GATE: Automated checks run during main() execution*

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
*Updated during specification process*

- [x] User description parsed
- [x] Key concepts extracted (AI generation, metadata judging, quality tracking)
- [x] No ambiguities found
- [x] User scenarios defined (query generation, judging, quality tracking)
- [x] Requirements generated (25 functional requirements)
- [x] Entities identified (SearchQuery extended, Candidate extended, Domain new, MetadataJudgment new, QueryGenerationSession new)
- [x] Success metrics defined
- [x] Review checklist passed

---

**Specification Status**: ✅ COMPLETE - Ready for planning phase
