# Implementation Plan: AI-Powered Query Generation and Metadata Judging

**Feature**: 004-ai-query-generation-metadata-judging
**Created**: 2025-10-21
**Status**: Planning Complete
**Dependencies**: Feature 003 (Search Execution Infrastructure)
**Estimated Effort**: 40-50 hours (8-10 working days)

---

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Phase Breakdown](#phase-breakdown)
4. [Database Schema](#database-schema)
5. [Domain Model](#domain-model)
6. [Application Services](#application-services)
7. [Integration Points](#integration-points)
8. [Testing Strategy](#testing-strategy)
9. [Risk Mitigation](#risk-mitigation)
10. [Success Criteria](#success-criteria)

---

## Overview

### Goals
Build upon Feature 003's search execution infrastructure by adding:
1. **AI-powered query generation** using LM Studio to create 10-20 diverse queries daily
2. **Metadata-based judging** to filter high-quality funding sources without web crawling
3. **Domain quality tracking** to learn which domains consistently yield good results
4. **Automatic candidate creation** for high-confidence results (>= 0.60)

### Non-Goals
- Web crawling (deferred to Feature 005)
- ML model training (use LM Studio as-is with prompt engineering)
- UI for blacklist management (SQL/command-line for MVP)
- Real-time query generation (batch process, once daily)

### Key Metrics
- Generate 10-20 queries/day with 80% uniqueness from previous week
- Judge 200-400 domains/session in < 5 minutes
- Create 50-150 high-confidence candidates/session
- Achieve 70% precision (manual review pass rate for candidates >= 0.60)

---

## Architecture

### High-Level Flow

```
                      ┌─────────────────────────────────────────┐
                      │   Query Generation Service              │
                      │                                         │
                      │  1. Fetch previous queries (7 days)    │
                      │  2. Generate AI prompt with templates  │
                      │  3. Call LM Studio API                 │
                      │  4. Parse & validate responses         │
                      │  5. Check for duplicates/similarity    │
                      │  6. Store approved queries             │
                      └──────────────┬──────────────────────────┘
                                     │
                                     ▼
                      ┌─────────────────────────────────────────┐
                      │   Nightly Discovery Scheduler           │
                      │   (Existing from Feature 003)           │
                      │                                         │
                      │  1. Load queries (AI + hardcoded)      │
                      │  2. Execute across 3 engines           │
                      │  3. Deduplicate by domain              │
                      │  4. Pass to Metadata Judge             │
                      └──────────────┬──────────────────────────┘
                                     │
                                     ▼
                      ┌─────────────────────────────────────────┐
                      │   Metadata Judging Service              │
                      │                                         │
                      │  1. Extract domain from URL            │
                      │  2. Check domain quality/blacklist     │
                      │  3. Calculate confidence score         │
                      │     - Funding keywords (0.30)          │
                      │     - Domain credibility (0.25)        │
                      │     - Geographic relevance (0.25)      │
                      │     - Organization type (0.20)         │
                      │  4. Create candidate if score >= 0.60  │
                      │  5. Update domain quality metrics      │
                      └──────────────┬──────────────────────────┘
                                     │
                                     ▼
                      ┌─────────────────────────────────────────┐
                      │   Domain Quality Tracker                │
                      │                                         │
                      │  1. Track best score, counts           │
                      │  2. Calculate quality tier             │
                      │     HIGH: >70% >= 0.60, best >= 0.70   │
                      │     MEDIUM: 30-70% >= 0.60             │
                      │     LOW: <30% >= 0.60                  │
                      │  3. Support blacklist management       │
                      └─────────────────────────────────────────┘
```

### Component Responsibilities

**QueryGenerationService**:
- Generate AI prompts with geographic/category diversity
- Call LM Studio API with retry logic
- Parse and validate AI responses
- Check for semantic similarity with recent queries
- Store approved queries in database

**MetadataJudgingService**:
- Accept list of SearchResult from SearchExecutionService
- Extract domain and check quality/blacklist status
- Calculate multi-criteria confidence scores
- Create Candidate entities for high-confidence results
- Update domain quality metrics

**DomainQualityTracker**:
- Maintain domain discovery and quality statistics
- Calculate quality tiers (HIGH/MEDIUM/LOW)
- Support blacklist operations
- Provide quality-based filtering for prioritization

**QueryGenerationScheduler**:
- Run daily at 1:00 AM (before nightly discovery at 2:00 AM)
- Trigger QueryGenerationService
- Log generation statistics
- Alert on failures (fall back to hardcoded queries)

---

## Phase Breakdown

### Phase 4.1: Database Schema & Domain Model (Days 1-2)

**Goal**: Extend database schema with new tables and update existing entities

**Tasks**:
1. Create Flyway migration V13 for domains table
2. Create Flyway migration V14 for metadata_judgments table
3. Create Flyway migration V15 for query_generation_sessions table
4. Extend search_queries table with AI generation fields (V16)
5. Extend candidates table with confidence/judging fields (V17)
6. Create Domain entity with Spring Data JDBC
7. Create MetadataJudgment entity
8. Create QueryGenerationSession entity
9. Update SearchQuery entity with new fields
10. Update Candidate entity with new fields
11. Write unit tests for all domain entities
12. Write repository integration tests with TestContainers

**Deliverables**:
- 5 Flyway migrations (V13-V17)
- 3 new entities (Domain, MetadataJudgment, QueryGenerationSession)
- 2 extended entities (SearchQuery, Candidate)
- 3 new repositories (DomainRepository, MetadataJudgmentRepository, QueryGenerationSessionRepository)
- 15+ unit tests

**Acceptance Criteria**:
- All migrations apply cleanly to PostgreSQL 16
- All repository tests pass with TestContainers
- Domain entity tracks quality metrics correctly
- No JSONB fields (use TEXT[] or simple columns per Feature 003 guidance)

### Phase 4.2: LM Studio Integration & Query Generation (Days 3-4)

**Goal**: Implement AI-powered query generation using LM Studio

**Tasks**:
1. Create LMStudioClient with RestClient (Spring 6.1+)
2. Implement prompt templates for query generation
3. Create QueryGenerationService
4. Implement semantic similarity checking (simple keyword overlap)
5. Implement query validation (reject too generic, check length)
6. Create QueryGenerationScheduler (@Scheduled, 1:00 AM daily)
7. Add circuit breaker for LM Studio (Resilience4j)
8. Implement fallback to hardcoded queries on AI failure
9. Write unit tests for LMStudioClient
10. Write integration tests for QueryGenerationService
11. Create manual testing script for query generation

**Deliverables**:
- LMStudioClient with circuit breaker
- QueryGenerationService with validation logic
- QueryGenerationScheduler
- 5+ prompt templates
- 10+ unit tests
- 2 integration tests

**Acceptance Criteria**:
- LM Studio generates 10-20 queries in < 30 seconds
- Query uniqueness >= 80% from previous 7 days
- Circuit breaker protects against LM Studio failures
- Fallback to hardcoded queries works seamlessly
- All tests pass

### Phase 4.3: Metadata Judging Service (Days 5-6)

**Goal**: Implement confidence scoring and candidate creation

**Tasks**:
1. Create MetadataJudgingService
2. Implement funding keywords scorer (0.30 weight)
3. Implement domain credibility scorer (0.25 weight)
4. Implement geographic relevance scorer (0.25 weight)
5. Implement organization type scorer (0.20 weight)
6. Create confidence score aggregator (weighted sum)
7. Implement candidate creation logic (>= 0.60 threshold)
8. Integrate with existing Candidate entity
9. Write unit tests for each scoring component
10. Write integration tests for end-to-end judging
11. Create test datasets with known scores

**Deliverables**:
- MetadataJudgingService
- 4 scoring components (funding, credibility, geography, org type)
- Candidate creation logic
- 15+ unit tests
- 3 integration tests
- Test dataset with 50 sample results

**Acceptance Criteria**:
- Confidence scores calculated accurately (0.00-1.00 range)
- High-confidence results create candidates with PENDING_CRAWL status
- Low-confidence results skip candidate creation
- Scoring is deterministic (same input = same score)
- Tests achieve >= 85% code coverage

### Phase 4.4: Domain Quality Tracking (Days 7-8)

**Goal**: Track domain quality metrics and calculate tiers

**Tasks**:
1. Create DomainQualityTracker service
2. Implement domain discovery tracking (first_seen, last_seen)
3. Implement quality metrics update (best_score, counts)
4. Implement quality tier calculation (HIGH/MEDIUM/LOW)
5. Create DomainRepository with custom queries
6. Implement blacklist management operations
7. Create domain quality report generator
8. Write unit tests for tier calculation logic
9. Write integration tests for quality tracking
10. Create admin scripts for blacklist management

**Deliverables**:
- DomainQualityTracker service
- DomainRepository with custom queries
- Quality tier calculation logic
- Blacklist management operations
- 10+ unit tests
- 2 integration tests
- Admin SQL scripts

**Acceptance Criteria**:
- Quality tiers calculated correctly based on distribution
- Domain metrics update after each judging session
- Blacklist prevents processing of flagged domains
- Quality reports provide actionable insights
- All tests pass

### Phase 4.5: Integration & End-to-End Testing (Days 9-10)

**Goal**: Integrate all components and validate full workflow

**Tasks**:
1. Integrate QueryGenerationService with NightlyDiscoveryScheduler
2. Integrate MetadataJudgingService with SearchExecutionService
3. Integrate DomainQualityTracker with MetadataJudgingService
4. Create end-to-end integration tests
5. Test full workflow: generation → execution → judging → quality tracking
6. Performance testing (200-400 domains in < 5 minutes)
7. Create monitoring and logging infrastructure
8. Update CLAUDE.md with Feature 004 context
9. Create manual testing guide
10. Create completion summary document

**Deliverables**:
- Full integration of all components
- 5+ end-to-end integration tests
- Performance benchmarks
- Updated documentation
- Manual testing guide
- Completion summary

**Acceptance Criteria**:
- Full workflow executes successfully (generation → judging → tracking)
- Performance meets targets (< 5 minutes for 200-400 domains)
- All integration tests pass
- Documentation complete and accurate
- Ready for production deployment

---

## Database Schema

### New Tables

#### domains (V13)
```sql
CREATE TABLE domains (
    id                      BIGSERIAL PRIMARY KEY,
    domain_name             VARCHAR(255) NOT NULL UNIQUE,
    first_discovered_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_seen_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    best_confidence_score   DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    high_confidence_count   INTEGER NOT NULL DEFAULT 0,
    low_confidence_count    INTEGER NOT NULL DEFAULT 0,
    total_results_count     INTEGER NOT NULL DEFAULT 0,
    quality_tier            VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    is_blacklisted          BOOLEAN NOT NULL DEFAULT FALSE,
    blacklist_reason        TEXT NULL,
    blacklisted_at          TIMESTAMP WITH TIME ZONE NULL,
    blacklisted_by          VARCHAR(100) NULL,
    no_funds_this_year      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP WITH TIME ZONE NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NULL DEFAULT NOW(),

    CONSTRAINT chk_confidence_range CHECK (best_confidence_score BETWEEN 0.00 AND 1.00),
    CONSTRAINT chk_quality_tier CHECK (quality_tier IN ('HIGH', 'MEDIUM', 'LOW', 'UNKNOWN'))
);

CREATE INDEX idx_domains_quality_tier ON domains(quality_tier);
CREATE INDEX idx_domains_blacklisted ON domains(is_blacklisted);
CREATE INDEX idx_domains_best_score ON domains(best_confidence_score DESC);
```

#### metadata_judgments (V14)
```sql
CREATE TABLE metadata_judgments (
    id                              BIGSERIAL PRIMARY KEY,
    domain_id                       BIGINT NOT NULL REFERENCES domains(id),
    search_result_url               TEXT NOT NULL,
    search_result_title             TEXT NOT NULL,
    search_result_description       TEXT NULL,
    search_engine_source            VARCHAR(50) NOT NULL,

    confidence_score                DECIMAL(3,2) NOT NULL,
    funding_keywords_score          DECIMAL(3,2) NOT NULL,
    domain_credibility_score        DECIMAL(3,2) NOT NULL,
    geographic_relevance_score      DECIMAL(3,2) NOT NULL,
    organization_type_score         DECIMAL(3,2) NOT NULL,

    extracted_org_name              VARCHAR(255) NULL,
    extracted_program_name          VARCHAR(255) NULL,
    keywords_found                  TEXT[] NULL,
    geographic_terms_found          TEXT[] NULL,

    candidate_created               BOOLEAN NOT NULL DEFAULT FALSE,
    candidate_id                    BIGINT NULL REFERENCES candidates(id),

    session_id                      BIGINT NULL REFERENCES discovery_sessions(id),
    judging_timestamp               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_judgment_scores CHECK (
        confidence_score BETWEEN 0.00 AND 1.00 AND
        funding_keywords_score BETWEEN 0.00 AND 1.00 AND
        domain_credibility_score BETWEEN 0.00 AND 1.00 AND
        geographic_relevance_score BETWEEN 0.00 AND 1.00 AND
        organization_type_score BETWEEN 0.00 AND 1.00
    )
);

CREATE INDEX idx_judgments_domain ON metadata_judgments(domain_id);
CREATE INDEX idx_judgments_confidence ON metadata_judgments(confidence_score DESC);
CREATE INDEX idx_judgments_session ON metadata_judgments(session_id);
CREATE INDEX idx_judgments_candidate ON metadata_judgments(candidate_id);
```

#### query_generation_sessions (V15)
```sql
CREATE TABLE query_generation_sessions (
    id                      BIGSERIAL PRIMARY KEY,
    generation_date         DATE NOT NULL,
    ai_model_used           VARCHAR(100) NOT NULL,
    queries_requested       INTEGER NOT NULL,
    queries_generated       INTEGER NOT NULL DEFAULT 0,
    queries_approved        INTEGER NOT NULL DEFAULT 0,
    queries_rejected        INTEGER NOT NULL DEFAULT 0,
    rejection_reasons       TEXT[] NULL,
    generation_duration_ms  BIGINT NOT NULL,
    fallback_used           BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason         TEXT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_queries_consistency CHECK (
        queries_generated = queries_approved + queries_rejected
    )
);

CREATE INDEX idx_query_gen_date ON query_generation_sessions(generation_date DESC);
```

### Extended Tables

#### search_queries (V16)
```sql
ALTER TABLE search_queries ADD COLUMN generation_method VARCHAR(20) NOT NULL DEFAULT 'HARDCODED';
ALTER TABLE search_queries ADD COLUMN ai_model_used VARCHAR(100) NULL;
ALTER TABLE search_queries ADD COLUMN query_template_id VARCHAR(50) NULL;
ALTER TABLE search_queries ADD COLUMN semantic_cluster_id INTEGER NULL;
ALTER TABLE search_queries ADD COLUMN generation_session_id BIGINT NULL REFERENCES query_generation_sessions(id);
ALTER TABLE search_queries ADD COLUMN generation_date DATE NULL;

ALTER TABLE search_queries ADD CONSTRAINT chk_generation_method
    CHECK (generation_method IN ('AI_GENERATED', 'HARDCODED'));

CREATE INDEX idx_queries_generation_method ON search_queries(generation_method);
CREATE INDEX idx_queries_generation_date ON search_queries(generation_date DESC);
```

#### candidates (V17)
```sql
ALTER TABLE candidates ADD COLUMN confidence_score DECIMAL(3,2) NULL;
ALTER TABLE candidates ADD COLUMN judging_criteria JSONB NULL;
ALTER TABLE candidates ADD COLUMN domain_quality_tier VARCHAR(20) NULL;
ALTER TABLE candidates ADD COLUMN metadata_title TEXT NULL;
ALTER TABLE candidates ADD COLUMN metadata_description TEXT NULL;
ALTER TABLE candidates ADD COLUMN search_engine_source VARCHAR(50) NULL;
ALTER TABLE candidates ADD COLUMN metadata_judgment_id BIGINT NULL REFERENCES metadata_judgments(id);

ALTER TABLE candidates ADD CONSTRAINT chk_candidate_confidence
    CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0.00 AND 1.00);

CREATE INDEX idx_candidates_confidence ON candidates(confidence_score DESC);
CREATE INDEX idx_candidates_judgment ON candidates(metadata_judgment_id);
```

---

## Domain Model

### New Entities

#### Domain
```java
@Data
@Builder
@Table("domains")
public class Domain {
    @Id private Long id;

    @Column("domain_name")
    private String domainName; // Unique, lowercase

    @Column("first_discovered_at")
    private Instant firstDiscoveredAt;

    @Column("last_seen_at")
    private Instant lastSeenAt;

    @Column("best_confidence_score")
    private BigDecimal bestConfidenceScore;

    @Column("high_confidence_count")
    private Integer highConfidenceCount;

    @Column("low_confidence_count")
    private Integer lowConfidenceCount;

    @Column("total_results_count")
    private Integer totalResultsCount;

    @Column("quality_tier")
    private QualityTier qualityTier; // HIGH, MEDIUM, LOW, UNKNOWN

    @Column("is_blacklisted")
    private Boolean isBlacklisted;

    @Column("blacklist_reason")
    private String blacklistReason;

    @Column("blacklisted_at")
    private Instant blacklistedAt;

    @Column("blacklisted_by")
    private String blacklistedBy;

    @Column("no_funds_this_year")
    private Boolean noFundsThisYear;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Helper methods
    public void updateQualityMetrics(BigDecimal newScore, boolean isHighConfidence);
    public QualityTier calculateQualityTier();
    public double getHighConfidenceRatio();
}
```

#### MetadataJudgment
```java
@Data
@Builder
@Table("metadata_judgments")
public class MetadataJudgment {
    @Id private Long id;

    @Column("domain_id")
    private Long domainId;

    @Column("search_result_url")
    private String searchResultUrl;

    @Column("search_result_title")
    private String searchResultTitle;

    @Column("search_result_description")
    private String searchResultDescription;

    @Column("search_engine_source")
    private String searchEngineSource;

    @Column("confidence_score")
    private BigDecimal confidenceScore;

    @Column("funding_keywords_score")
    private BigDecimal fundingKeywordsScore;

    @Column("domain_credibility_score")
    private BigDecimal domainCredibilityScore;

    @Column("geographic_relevance_score")
    private BigDecimal geographicRelevanceScore;

    @Column("organization_type_score")
    private BigDecimal organizationTypeScore;

    @Column("extracted_org_name")
    private String extractedOrgName;

    @Column("extracted_program_name")
    private String extractedProgramName;

    @Column("keywords_found")
    private Set<String> keywordsFound; // TEXT[] with custom converter

    @Column("geographic_terms_found")
    private Set<String> geographicTermsFound; // TEXT[] with custom converter

    @Column("candidate_created")
    private Boolean candidateCreated;

    @Column("candidate_id")
    private Long candidateId;

    @Column("session_id")
    private Long sessionId;

    @Column("judging_timestamp")
    private Instant judgingTimestamp;

    public boolean isHighConfidence() {
        return confidenceScore.compareTo(new BigDecimal("0.60")) >= 0;
    }
}
```

#### QueryGenerationSession
```java
@Data
@Builder
@Table("query_generation_sessions")
public class QueryGenerationSession {
    @Id private Long id;

    @Column("generation_date")
    private LocalDate generationDate;

    @Column("ai_model_used")
    private String aiModelUsed;

    @Column("queries_requested")
    private Integer queriesRequested;

    @Column("queries_generated")
    private Integer queriesGenerated;

    @Column("queries_approved")
    private Integer queriesApproved;

    @Column("queries_rejected")
    private Integer queriesRejected;

    @Column("rejection_reasons")
    private Set<String> rejectionReasons; // TEXT[] with custom converter

    @Column("generation_duration_ms")
    private Long generationDurationMs;

    @Column("fallback_used")
    private Boolean fallbackUsed;

    @Column("fallback_reason")
    private String fallbackReason;

    @Column("created_at")
    private Instant createdAt;

    public double getApprovalRate() {
        return queriesGenerated > 0
            ? (double) queriesApproved / queriesGenerated
            : 0.0;
    }
}
```

---

## Application Services

### QueryGenerationService

**Responsibilities**:
- Generate AI prompts with geographic/category templates
- Call LM Studio API with circuit breaker protection
- Parse and validate AI responses
- Check semantic similarity with recent queries
- Store approved queries in database

**Key Methods**:
```java
public Try<QueryGenerationResult> generateQueries(
    int requestedCount,
    Set<String> geographies,
    Set<String> categories
);

private Try<String> generateAIPrompt(Set<String> geographies, Set<String> categories);
private Try<List<String>> callLMStudioAPI(String prompt);
private List<String> validateAndFilterQueries(List<String> rawQueries);
private boolean isSemanticallyDuplicate(String newQuery, List<String> recentQueries);
private void storeApprovedQueries(List<String> queries, QueryGenerationSession session);
```

### MetadataJudgingService

**Responsibilities**:
- Accept SearchResult from SearchExecutionService
- Extract domain and check quality/blacklist
- Calculate multi-criteria confidence scores
- Create Candidate for high-confidence results
- Update domain quality metrics

**Key Methods**:
```java
public Try<JudgingResult> judgeSearchResults(List<SearchResult> results, Long sessionId);

private BigDecimal calculateFundingKeywordsScore(String title, String description);
private BigDecimal calculateDomainCredibilityScore(String domain);
private BigDecimal calculateGeographicRelevanceScore(String title, String description);
private BigDecimal calculateOrganizationTypeScore(String title, String description, String domain);
private BigDecimal aggregateConfidenceScore(Map<String, BigDecimal> scores);
private void createCandidateIfHighConfidence(MetadataJudgment judgment);
private void updateDomainQualityMetrics(Domain domain, MetadataJudgment judgment);
```

### DomainQualityTracker

**Responsibilities**:
- Track domain discovery and quality statistics
- Calculate quality tiers (HIGH/MEDIUM/LOW)
- Manage blacklist operations
- Provide quality-based filtering

**Key Methods**:
```java
public Try<Domain> getOrCreateDomain(String domainName);
public void updateQualityMetrics(Domain domain, BigDecimal score, boolean highConfidence);
public QualityTier calculateQualityTier(Domain domain);
public void blacklistDomain(String domainName, String reason, String adminUser);
public boolean isDomainBlacklisted(String domainName);
public List<Domain> getDomainsByQualityTier(QualityTier tier);
```

---

## Integration Points

### With Feature 003 (Search Execution Infrastructure)

**SearchExecutionService Integration**:
```java
// In NightlyDiscoveryScheduler
@Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
public void runNightlyDiscovery() {
    var currentDay = DayOfWeek.from(LocalDateTime.now());

    // Load queries (AI-generated + hardcoded)
    var queries = searchQueryRepository.findByDayOfWeekAndEnabled(currentDay);

    // Execute searches
    var searchResult = searchExecutionService.executeQueries(queries);

    if (searchResult.isSuccess()) {
        var results = searchResult.get();

        // NEW: Judge metadata and create candidates
        var judgingResult = metadataJudgingService.judgeSearchResults(results, sessionId);

        if (judgingResult.isSuccess()) {
            log.info("Judging complete: {}", judgingResult.get());
        }
    }
}
```

**QueryGenerationScheduler** (new):
```java
@Scheduled(cron = "0 0 1 * * ?") // 1 AM daily (before nightly discovery)
public void runQueryGeneration() {
    var result = queryGenerationService.generateQueries(
        20, // Request 20 queries
        Set.of("Bulgaria", "Romania", "Greece", "Balkans", "Eastern Europe"),
        Set.of("Education", "Infrastructure", "Healthcare", "STEM", "Arts")
    );

    if (result.isFailure()) {
        log.error("Query generation failed, falling back to hardcoded queries");
        // Hardcoded queries will still be used by NightlyDiscoveryScheduler
    }
}
```

### With Existing Entities

**Candidate Entity Extension**:
```java
// When creating candidate from high-confidence judgment
Candidate candidate = Candidate.builder()
    .domainName(domain.getDomainName())
    .url(judgment.getSearchResultUrl())
    .status(CandidateStatus.PENDING_CRAWL) // Ready for Feature 005 crawling
    .confidenceScore(judgment.getConfidenceScore())
    .judgingCriteria(buildJudgingCriteriaJson(judgment))
    .domainQualityTier(domain.getQualityTier().name())
    .metadataTitle(judgment.getSearchResultTitle())
    .metadataDescription(judgment.getSearchResultDescription())
    .searchEngineSource(judgment.getSearchEngineSource())
    .metadataJudgmentId(judgment.getId())
    .discoverySessionId(sessionId)
    .createdAt(Instant.now())
    .build();

candidateRepository.save(candidate);
```

---

## Testing Strategy

### Unit Tests (40+ tests)

**Domain Entities** (10 tests):
- Domain: quality tier calculation, metrics update
- MetadataJudgment: score validation, high confidence check
- QueryGenerationSession: approval rate calculation

**Services** (30+ tests):
- QueryGenerationService: prompt generation, validation, similarity check
- MetadataJudgingService: each scoring component, aggregation, candidate creation
- DomainQualityTracker: tier calculation, blacklist operations

### Integration Tests (10+ tests)

**Database Integration** (4 tests):
- DomainRepository: CRUD, quality tier queries
- MetadataJudgmentRepository: complex queries with joins
- QueryGenerationSessionRepository: statistics queries

**End-to-End Workflow** (6 tests):
- Full query generation → search → judging → candidate creation
- Blacklist prevents candidate creation
- Low confidence skips candidate creation
- Quality metrics update correctly
- Fallback to hardcoded queries on AI failure
- Performance test (200 domains in < 5 minutes)

### Manual Testing

**Query Generation**:
- Generate 20 queries and verify diversity
- Check uniqueness from previous 7 days
- Validate geographic/category distribution
- Test AI failure and fallback

**Metadata Judging**:
- Judge known high-quality results (should score >= 0.70)
- Judge known low-quality results (should score < 0.40)
- Verify candidate creation threshold (>= 0.60)
- Check domain quality tier calculations

---

## Risk Mitigation

### Risk 1: LM Studio Availability/Performance

**Risk**: LM Studio may be down or slow, blocking query generation

**Mitigation**:
- Circuit breaker with 3-attempt retry
- Fallback to hardcoded query library (16 queries already configured)
- Monitor LM Studio health endpoint
- Alert on repeated failures

### Risk 2: Confidence Scoring Accuracy

**Risk**: Scoring may produce too many false positives or false negatives

**Mitigation**:
- Start with conservative weights (validated in testing)
- Track precision (manual review pass rate) in production
- Support admin tuning of weights via configuration
- Log all scoring decisions for debugging

### Risk 3: Semantic Similarity False Positives

**Risk**: Simple keyword overlap may incorrectly reject valid queries

**Mitigation**:
- Use conservative similarity threshold (>= 80% keyword overlap)
- Log all rejections for review
- Support manual query addition as override
- Plan for ML-based similarity in future enhancement

### Risk 4: Domain Quality Drift

**Risk**: Previously high-quality domains may stop offering grants

**Mitigation**:
- Quality tier recalculated on every update (dynamic)
- Track last_seen timestamp to identify stale domains
- Support "no funds this year" flag for manual annotation
- Regular admin review of HIGH tier domains

---

## Success Criteria

### Query Generation
- ✅ Generate 10-20 queries per day
- ✅ Query uniqueness >= 80% from previous 7 days
- ✅ Geographic diversity: >= 5 different geographies per session
- ✅ Category diversity: >= 5 different categories per session
- ✅ Fallback usage: <= 5% of sessions
- ✅ Generation time: < 30 seconds

### Metadata Judging
- ✅ Process 200-400 domains per session
- ✅ Judging time: < 5 minutes for 400 domains
- ✅ High-confidence candidates: 50-150 per session
- ✅ Precision (manual review pass rate): >= 70% for score >= 0.60
- ✅ Scoring consistency: +/- 0.05 for same result

### Domain Quality Tracking
- ✅ Quality tier distribution: 10-20% HIGH, 30-40% MEDIUM, 40-60% LOW
- ✅ Tier stability: >= 85% maintain tier month-over-month
- ✅ Blacklist effectiveness: 0 candidates created from blacklisted domains

### Integration
- ✅ All tests pass (50+ unit + integration tests)
- ✅ Full workflow executes successfully (generation → search → judge → track)
- ✅ No performance degradation vs Feature 003 baseline
- ✅ Documentation complete and accurate

---

## Implementation Timeline

**Total Estimated Effort**: 40-50 hours (8-10 working days)

### Week 1: Foundation (Days 1-5)
- Days 1-2: Database schema & domain model (Phase 4.1)
- Days 3-4: LM Studio integration & query generation (Phase 4.2)
- Day 5: Metadata judging service (Phase 4.3 start)

### Week 2: Services & Integration (Days 6-10)
- Day 6: Metadata judging completion (Phase 4.3 finish)
- Days 7-8: Domain quality tracking (Phase 4.4)
- Days 9-10: Integration & testing (Phase 4.5)

---

## Next Steps for Developer

1. **Review this plan** and spec.md
2. **Set up LM Studio** on Mac Studio if not already running
3. **Start with Phase 4.1** (database schema)
4. **Follow TDD** (write tests first, then implementation)
5. **Use Feature 003 patterns** (Set<String> for arrays, BigDecimal for scores, Virtual Threads)
6. **Reference CLAUDE.md** for coding standards and best practices

---

**Plan Status**: ✅ COMPLETE - Ready for task breakdown and implementation
