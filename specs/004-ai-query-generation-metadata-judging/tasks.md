# Tasks Breakdown: AI-Powered Query Generation and Metadata Judging

**Feature**: 004-ai-query-generation-metadata-judging
**Created**: 2025-10-21
**Total Tasks**: 55 tasks across 5 phases
**Estimated Effort**: 40-50 hours

---

## Legend
- **[P]**: Primary task (critical path)
- **[S]**: Secondary task (can be parallelized)
- **[T]**: Testing task
- **[D]**: Documentation task

---

## Phase 4.1: Database Schema & Domain Model (12 tasks, ~8 hours)

### Database Migrations

- [ ] **T001** [P] Create V13__create_domains_table.sql
  - Table: domains with quality tracking fields
  - Indexes: quality_tier, is_blacklisted, best_confidence_score
  - Constraints: confidence score range 0.00-1.00, quality tier enum
  - **VERIFY: Migration applies cleanly to PostgreSQL 16**

- [ ] **T002** [P] Create V14__create_metadata_judgments_table.sql
  - Table: metadata_judgments with scoring fields
  - Foreign keys: domain_id, candidate_id, session_id
  - Indexes: domain_id, confidence_score, session_id
  - Constraints: all scores in range 0.00-1.00
  - **VERIFY: Migration applies cleanly**

- [ ] **T003** [P] Create V15__create_query_generation_sessions_table.sql
  - Table: query_generation_sessions with statistics
  - Indexes: generation_date DESC
  - Constraints: queries_generated = approved + rejected
  - **VERIFY: Migration applies cleanly**

- [ ] **T004** [P] Create V16__extend_search_queries_table.sql
  - Add columns: generation_method, ai_model_used, query_template_id, semantic_cluster_id, generation_session_id, generation_date
  - Constraint: generation_method IN ('AI_GENERATED', 'HARDCODED')
  - Indexes: generation_method, generation_date
  - **VERIFY: Migration applies cleanly**

- [ ] **T005** [P] Create V17__extend_candidates_table.sql
  - Add columns: confidence_score, judging_criteria (JSONB), domain_quality_tier, metadata_title, metadata_description, search_engine_source, metadata_judgment_id
  - Constraint: confidence_score in range 0.00-1.00
  - Indexes: confidence_score DESC, metadata_judgment_id
  - **VERIFY: Migration applies cleanly**

### Domain Entities

- [ ] **T006** [P] Create Domain entity in `domain/Domain.java`
  - Annotations: @Data, @Builder, @Table("domains")
  - Fields: All from V13 migration
  - Use BigDecimal for best_confidence_score (scale 2)
  - Methods: updateQualityMetrics(), calculateQualityTier(), getHighConfidenceRatio()
  - **VERIFY: DomainTest passes**

- [ ] **T007** [P] Create QualityTier enum in `domain/QualityTier.java`
  - Values: HIGH, MEDIUM, LOW, UNKNOWN
  - Logic: HIGH (>70% >= 0.60 AND best >= 0.70), MEDIUM (30-70%), LOW (<30%)
  - **VERIFY: QualityTierTest passes**

- [ ] **T008** [P] Create MetadataJudgment entity in `domain/MetadataJudgment.java`
  - Annotations: @Data, @Builder, @Table("metadata_judgments")
  - Fields: All from V14 migration
  - Use BigDecimal for all scores (scale 2)
  - Method: isHighConfidence() returns score >= 0.60
  - **VERIFY: MetadataJudgmentTest passes**

- [ ] **T009** [P] Create QueryGenerationSession entity in `domain/QueryGenerationSession.java`
  - Annotations: @Data, @Builder, @Table("query_generation_sessions")
  - Fields: All from V15 migration
  - Method: getApprovalRate()
  - **VERIFY: QueryGenerationSessionTest passes**

- [ ] **T010** [P] Extend SearchQuery entity with AI generation fields
  - Add fields from V16 migration
  - Update builder to include new fields
  - **VERIFY: SearchQueryTest still passes**

- [ ] **T011** [P] Extend Candidate entity with judging fields
  - Add fields from V17 migration
  - Use BigDecimal for confidence_score
  - **VERIFY: CandidateTest still passes**

### Repositories

- [ ] **T012** [P] Create DomainRepository, MetadataJudgmentRepository, QueryGenerationSessionRepository
  - Extend CrudRepository<T, Long>
  - Custom queries for Domain: findByDomainName, findByQualityTier, findBlacklisted
  - Custom queries for MetadataJudgment: findByDomainId, findBySessionId, findHighConfidence
  - **VERIFY: Repository integration tests pass with TestContainers**

---

## Phase 4.2: LM Studio Integration & Query Generation (13 tasks, ~10 hours)

### LM Studio Client

- [ ] **T013** [P] Create LMStudioClient in `infrastructure/ai/LMStudioClient.java`
  - Use Spring RestClient (Spring 6.1+)
  - Base URL: http://192.168.1.10:1234/v1
  - Method: generateQueries(String prompt) -> Try<List<String>>
  - Parse chat completion response
  - **VERIFY: LMStudioClientTest passes with mocked RestClient**

- [ ] **T014** [P] Add LM Studio circuit breaker configuration in application.yml
  - Instance name: lmStudio (already exists from Feature 003)
  - Verify: minimumNumberOfCalls: 3, failureRateThreshold: 50, waitDurationInOpenState: 20s
  - **VERIFY: Configuration loaded correctly**

- [ ] **T015** [P] Add @CircuitBreaker and @Retry annotations to LMStudioClient
  - Circuit breaker name: lmStudio
  - Fallback method: generateQueriesFallback(String prompt, Exception ex)
  - Retry: maxAttempts=2, waitDuration=500ms
  - **VERIFY: Circuit breaker activates on failures**

### Query Generation Service

- [ ] **T016** [P] Create QueryGenerationService in `application/QueryGenerationService.java`
  - Inject: LMStudioClient, SearchQueryRepository, QueryGenerationSessionRepository
  - Method: generateQueries(int count, Set<String> geographies, Set<String> categories) -> Try<QueryGenerationResult>
  - **VERIFY: QueryGenerationServiceTest passes**

- [ ] **T017** [P] Implement AI prompt generation in QueryGenerationService
  - Create prompt template with geographic/category placeholders
  - Example: "Generate 10 search queries for funding sources in {geography} focused on {category}. Each query should be specific and include year 2025. Format: one query per line."
  - Randomize geography/category selection for diversity
  - **VERIFY: Prompts generate diverse queries**

- [ ] **T018** [P] Implement query validation logic
  - Reject if too generic (< 3 keywords)
  - Reject if missing geographic context
  - Reject if missing category context
  - Reject if too long (> 100 characters)
  - **VERIFY: Validation catches generic/invalid queries**

- [ ] **T019** [P] Implement semantic similarity checking
  - Load recent queries from last 7 days
  - Calculate keyword overlap (Jaccard similarity)
  - Reject if similarity >= 0.80 (80% keyword overlap)
  - **VERIFY: Correctly identifies duplicate queries**

- [ ] **T020** [P] Implement query storage logic
  - Create QueryGenerationSession record
  - Store approved queries with generation metadata
  - Tag with generation_date, ai_model_used, semantic_cluster_id
  - **VERIFY: Queries persisted with correct metadata**

### Scheduler

- [ ] **T021** [P] Create QueryGenerationScheduler in `application/QueryGenerationScheduler.java`
  - @Scheduled(cron = "0 0 1 * * ?") // 1 AM daily
  - Call QueryGenerationService.generateQueries(20, geographies, categories)
  - Log generation statistics (approved, rejected, duration)
  - Implement fallback to hardcoded queries on failure
  - **VERIFY: Scheduler triggers at correct time (test manually)**

- [ ] **T022** [T] Create QueryGenerationIntegrationTest
  - Test: Generate 20 queries with diverse geographies/categories
  - Test: Reject semantically similar queries
  - Test: Validation catches generic queries
  - Test: Queries stored with correct metadata
  - Test: Circuit breaker activates on LM Studio failures
  - **VERIFY: All tests pass**

### Prompt Templates

- [ ] **T023** [S] Create 5+ prompt templates for query generation
  - Template 1: Geographic focus (Bulgaria, Romania, etc.)
  - Template 2: Category focus (Education, Infrastructure, etc.)
  - Template 3: Authority focus (EU, Government, etc.)
  - Template 4: Mixed focus (Geography + Category)
  - Template 5: Temporal focus (2025, current year, upcoming)
  - Store in configuration or database
  - **VERIFY: Templates produce diverse queries**

- [ ] **T024** [S] Create manual testing script for query generation
  - Script: test-query-generation.sh
  - Calls LM Studio API directly
  - Tests prompt templates
  - Validates responses
  - **VERIFY: Script runs successfully**

- [ ] **T025** [D] Document LM Studio setup and configuration
  - Installation on Mac Studio
  - Model selection (llama-3.1-8b-instruct)
  - API endpoint configuration
  - Prompt engineering guidelines
  - **VERIFY: Documentation complete**

---

## Phase 4.3: Metadata Judging Service (15 tasks, ~12 hours)

### Scoring Components

- [ ] **T026** [P] Create FundingKeywordsScorer in `application/scoring/FundingKeywordsScorer.java`
  - Keywords: grant, funding, scholarship, fellowship, award, prize, stipend, financial aid, support
  - Scoring: Count keywords in title (3x weight) + description (1x weight)
  - Normalize to 0.00-1.00 range
  - **VERIFY: FundingKeywordsScorerTest passes**

- [ ] **T027** [P] Create DomainCredibilityScorer in `application/scoring/DomainCredibilityScorer.java`
  - High credibility (.gov, .edu, .org, established foundations): 1.00
  - Medium credibility (.com with known orgs): 0.60
  - Low credibility (.blogspot, .wordpress, generic .com): 0.20
  - Maintain whitelist/blacklist of known domains
  - **VERIFY: DomainCredibilityScorerTest passes**

- [ ] **T028** [P] Create GeographicRelevanceScorer in `application/scoring/GeographicRelevanceScorer.java`
  - Target geographies: Bulgaria, Romania, Greece, Balkans, Eastern Europe, EU
  - Scoring: Count geographic mentions in title (3x) + description (1x)
  - Normalize to 0.00-1.00 range
  - **VERIFY: GeographicRelevanceScorerTest passes**

- [ ] **T029** [P] Create OrganizationTypeScorer in `application/scoring/OrganizationTypeScorer.java`
  - High score: government, university, foundation, ministry, commission (0.80-1.00)
  - Medium score: NGO, nonprofit, association, institute (0.50-0.70)
  - Low score: company, business, blog, personal (0.10-0.30)
  - Extract organization type from title/description
  - **VERIFY: OrganizationTypeScorerTest passes**

### Judging Service

- [ ] **T030** [P] Create MetadataJudgingService in `application/MetadataJudgingService.java`
  - Inject: All scoring components, DomainRepository, MetadataJudgmentRepository, CandidateRepository
  - Method: judgeSearchResults(List<SearchResult> results, Long sessionId) -> Try<JudgingResult>
  - **VERIFY: MetadataJudgingServiceTest passes**

- [ ] **T031** [P] Implement confidence score aggregation
  - Weighted sum: funding_keywords (0.30) + domain_credibility (0.25) + geographic_relevance (0.25) + organization_type (0.20)
  - Return BigDecimal with scale 2
  - **VERIFY: Aggregation calculates correctly**

- [ ] **T032** [P] Implement domain extraction and lookup
  - Extract domain from URL using java.net.URI.getHost()
  - Normalize to lowercase
  - Look up or create Domain entity
  - Check blacklist status (skip if blacklisted)
  - **VERIFY: Domain extraction works correctly**

- [ ] **T033** [P] Implement candidate creation logic
  - If confidence >= 0.60: Create Candidate with PENDING_CRAWL status
  - If confidence < 0.60: Skip, record in judgment only
  - Store all scoring details in MetadataJudgment
  - Link judgment to candidate
  - **VERIFY: Candidates created for high confidence only**

- [ ] **T034** [P] Implement domain quality metrics update
  - Update best_confidence_score if new score is higher
  - Increment high_confidence_count or low_confidence_count
  - Update last_seen_at timestamp
  - Recalculate quality_tier
  - **VERIFY: Metrics update correctly**

### Organization/Program Extraction

- [ ] **T035** [S] Implement organization name extraction
  - Use regex patterns to extract organization names from title
  - Examples: "X Foundation", "Y University", "Ministry of Z"
  - Store in extracted_org_name field
  - **VERIFY: Extraction works for common patterns**

- [ ] **T036** [S] Implement program name extraction
  - Use regex patterns to extract program names from title
  - Examples: "ABC Grant Program", "XYZ Scholarship 2025"
  - Store in extracted_program_name field
  - **VERIFY: Extraction works for common patterns**

### Testing

- [ ] **T037** [T] Create test dataset with 50 sample search results
  - 10 high-confidence results (expected score >= 0.70)
  - 20 medium-confidence results (expected score 0.40-0.70)
  - 20 low-confidence results (expected score < 0.40)
  - Include variety of domains, titles, descriptions
  - **VERIFY: Dataset covers diverse scenarios**

- [ ] **T038** [T] Create MetadataJudgingIntegrationTest
  - Test: High-confidence result creates candidate
  - Test: Low-confidence result skips candidate
  - Test: Blacklisted domain skips processing
  - Test: Domain quality metrics update correctly
  - Test: Scoring is deterministic (same input = same score)
  - Test: Process 200 results in < 5 minutes (Virtual Threads)
  - **VERIFY: All tests pass**

- [ ] **T039** [T] Create end-to-end workflow test
  - Generate queries → Execute searches → Judge results → Create candidates
  - Verify: Candidates created with correct status
  - Verify: Domain quality metrics updated
  - Verify: MetadataJudgment records persisted
  - **VERIFY: Full workflow executes successfully**

- [ ] **T040** [S] Tune scoring weights based on test results
  - Run judging on test dataset
  - Calculate precision (manual review pass rate)
  - Adjust weights if precision < 70%
  - Document final weight configuration
  - **VERIFY: Precision >= 70% on test dataset**

---

## Phase 4.4: Domain Quality Tracking (10 tasks, ~8 hours)

### Domain Quality Tracker Service

- [ ] **T041** [P] Create DomainQualityTracker in `application/DomainQualityTracker.java`
  - Inject: DomainRepository
  - Method: getOrCreateDomain(String domainName) -> Try<Domain>
  - Method: updateQualityMetrics(Domain domain, BigDecimal score, boolean highConfidence)
  - Method: calculateQualityTier(Domain domain) -> QualityTier
  - Method: blacklistDomain(String domainName, String reason, String adminUser)
  - **VERIFY: DomainQualityTrackerTest passes**

- [ ] **T042** [P] Implement quality tier calculation logic
  - HIGH: >70% of results >= 0.60 AND best_score >= 0.70
  - MEDIUM: 30-70% of results >= 0.60 OR best_score 0.50-0.70
  - LOW: <30% of results >= 0.60 AND best_score < 0.50
  - UNKNOWN: < 5 total results (insufficient data)
  - **VERIFY: Tier calculation matches spec**

- [ ] **T043** [P] Implement blacklist management
  - Method: blacklistDomain(domainName, reason, adminUser)
  - Update is_blacklisted, blacklist_reason, blacklisted_at, blacklisted_by
  - Method: isBlacklisted(domainName) -> boolean
  - Method: unblacklistDomain(domainName) (for mistakes)
  - **VERIFY: Blacklist operations work correctly**

- [ ] **T044** [P] Implement "no funds this year" flag
  - Method: markNoFundsThisYear(domainName)
  - Different from blacklist (can re-evaluate next year)
  - **VERIFY: Flag can be set and queried**

### Repository Queries

- [ ] **T045** [P] Add custom queries to DomainRepository
  - findByDomainName(String domainName) -> Optional<Domain>
  - findByQualityTier(QualityTier tier) -> List<Domain>
  - findBlacklisted() -> List<Domain>
  - findNoFundsThisYear() -> List<Domain>
  - findByBestConfidenceScoreGreaterThan(BigDecimal threshold) -> List<Domain>
  - **VERIFY: All queries execute correctly**

### Quality Reports

- [ ] **T046** [S] Create DomainQualityReportGenerator
  - Generate quality distribution report (HIGH/MEDIUM/LOW counts)
  - Generate top domains report (by best_confidence_score)
  - Generate blacklist report (with reasons)
  - Export to CSV or JSON
  - **VERIFY: Reports generate correctly**

- [ ] **T047** [S] Create admin SQL scripts for blacklist management
  - Script: blacklist-domain.sql (parameters: domain, reason, admin)
  - Script: unblacklist-domain.sql (parameter: domain)
  - Script: list-blacklisted-domains.sql
  - Script: mark-no-funds-this-year.sql
  - **VERIFY: Scripts execute correctly**

### Testing

- [ ] **T048** [T] Create DomainQualityTrackerIntegrationTest
  - Test: Quality tier calculated correctly for various distributions
  - Test: Blacklist prevents candidate creation
  - Test: Quality metrics update after each judgment
  - Test: Tier stability (domain maintains tier across sessions)
  - **VERIFY: All tests pass**

- [ ] **T049** [T] Create domain quality tracking scenario test
  - Scenario 1: New domain (UNKNOWN → HIGH after 10 high-confidence results)
  - Scenario 2: Degrading domain (HIGH → MEDIUM → LOW as quality drops)
  - Scenario 3: Blacklisted domain (skips all processing)
  - **VERIFY: All scenarios execute correctly**

- [ ] **T050** [S] Document domain quality tracking workflow
  - How tier calculation works
  - When to use blacklist vs "no funds this year"
  - How to review and manage domain quality
  - **VERIFY: Documentation complete**

---

## Phase 4.5: Integration & Documentation (5 tasks, ~6 hours)

### Integration

- [ ] **T051** [P] Integrate QueryGenerationService with NightlyDiscoveryScheduler
  - QueryGenerationScheduler runs at 1:00 AM
  - NightlyDiscoveryScheduler runs at 2:00 AM
  - NightlyDiscoveryScheduler loads AI-generated + hardcoded queries
  - **VERIFY: Integration works end-to-end**

- [ ] **T052** [P] Integrate MetadataJudgingService with SearchExecutionService
  - NightlyDiscoveryScheduler calls SearchExecutionService
  - SearchExecutionService returns deduplicated results
  - NightlyDiscoveryScheduler calls MetadataJudgingService
  - MetadataJudgingService creates candidates for high-confidence results
  - **VERIFY: Integration works end-to-end**

### Documentation

- [ ] **T053** [D] Update CLAUDE.md with Feature 004 context
  - Add AI-powered query generation section
  - Add metadata judging section
  - Add domain quality tracking section
  - Document scoring weights and thresholds
  - Add common commands for testing
  - **VERIFY: CLAUDE.md updated**

- [ ] **T054** [D] Create manual-testing.md for Feature 004
  - Test scenario 1: Query generation (validate diversity, uniqueness)
  - Test scenario 2: Metadata judging (validate scoring accuracy)
  - Test scenario 3: Candidate creation (validate threshold)
  - Test scenario 4: Domain quality tracking (validate tier calculation)
  - Test scenario 5: Blacklist management (validate operations)
  - Troubleshooting guide
  - **VERIFY: Manual testing guide complete**

- [ ] **T055** [D] Create COMPLETION-SUMMARY.md for Feature 004
  - Executive summary
  - Completed deliverables
  - Key technical decisions
  - Metrics and performance data
  - Known limitations
  - Production readiness checklist
  - **VERIFY: Completion summary complete**

---

## Dependencies & Order

### Critical Path (Must Complete in Order):
1. T001-T005 (Database migrations) → T006-T011 (Entities) → T012 (Repositories)
2. T013-T015 (LM Studio client) → T016-T020 (Query generation service) → T021 (Scheduler)
3. T026-T029 (Scoring components) → T030-T034 (Judging service)
4. T041-T043 (Quality tracker) → T045 (Repository queries)
5. T051-T052 (Integration) → T053-T055 (Documentation)

### Can Be Parallelized:
- T023-T025 (Prompt templates, testing scripts, documentation) - parallel with T016-T022
- T035-T036 (Extraction logic) - parallel with T026-T034
- T046-T047 (Reports, SQL scripts) - parallel with T041-T045
- T037-T040 (Test datasets, tuning) - parallel with T030-T034

---

## Task Summary

**Total Tasks**: 55
- Phase 4.1 (Schema & Model): 12 tasks
- Phase 4.2 (Query Generation): 13 tasks
- Phase 4.3 (Metadata Judging): 15 tasks
- Phase 4.4 (Quality Tracking): 10 tasks
- Phase 4.5 (Integration & Docs): 5 tasks

**Primary Tasks**: 35 (critical path)
**Secondary Tasks**: 12 (can be parallelized)
**Testing Tasks**: 8 (TDD approach)

**Estimated Effort**: 40-50 hours (8-10 working days at 5 hours/day)

---

**Tasks Status**: ✅ COMPLETE - Ready for implementation
