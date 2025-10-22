# Feature 004: AI-Powered Query Generation and Metadata Judging

**Status**: 📋 READY FOR IMPLEMENTATION
**Dependencies**: Feature 003 (Search Execution Infrastructure) ✅ COMPLETE
**Branch**: `004-ai-query-generation-metadata-judging`
**Estimated Effort**: 40-50 hours (8-10 working days)

---

## 🎯 Quick Summary

This feature adds **AI-powered query generation** and **metadata-based quality filtering** to the funding discovery system, building on Feature 003's search execution infrastructure.

**What It Does**:
- Generates 10-20 diverse search queries daily using LM Studio (local AI)
- Judges search results using metadata only (no web crawling)
- Creates high-confidence candidates (score >= 0.60) with PENDING_CRAWL status
- Tracks domain quality metrics for continuous improvement

**Business Value**:
- **Scalability**: 10-20 AI queries/day vs 2-5 hardcoded queries
- **Quality**: Filter out 60-80% of low-quality results before crawling
- **Efficiency**: Save processing resources on spam/irrelevant sites
- **Learning**: Domain quality tracking improves future discovery

---

## 📁 Documentation Files

All planning documents are complete and ready:

### 1. **spec.md** (Business Requirements)
- **What**: 25 functional requirements (FR-001 to FR-025)
- **Who**: Business stakeholders, product owners
- **Use**: Understand WHAT the feature does and WHY
- **Key Sections**:
  - User scenarios (query generation, metadata judging, quality tracking)
  - Acceptance criteria (11 scenarios + edge cases)
  - Success metrics (precision >= 70%, query diversity >= 80%)
  - Entities (Domain, MetadataJudgment, QueryGenerationSession + extensions)

### 2. **plan.md** (Technical Architecture)
- **What**: Implementation architecture and phase breakdown
- **Who**: Developers, technical leads
- **Use**: Understand HOW to implement the feature
- **Key Sections**:
  - High-level architecture diagram (query gen → search → judge → track)
  - Component responsibilities (services, repositories, schedulers)
  - 5 phases (Schema, Query Gen, Judging, Quality Tracking, Integration)
  - Database schema (5 new migrations, 3 new entities, 2 extended entities)
  - Integration points with Feature 003

### 3. **tasks.md** (Detailed Task Breakdown)
- **What**: 55 granular tasks across 5 phases
- **Who**: Implementation team
- **Use**: Track progress, estimate effort
- **Key Sections**:
  - Phase 4.1: Database Schema & Domain Model (12 tasks, ~8 hours)
  - Phase 4.2: LM Studio Integration & Query Generation (13 tasks, ~10 hours)
  - Phase 4.3: Metadata Judging Service (15 tasks, ~12 hours)
  - Phase 4.4: Domain Quality Tracking (10 tasks, ~8 hours)
  - Phase 4.5: Integration & Documentation (5 tasks, ~6 hours)
  - Task dependencies and parallelization opportunities

### 4. **quickstart.md** (Code Examples & Reference)
- **What**: Concrete code examples and quick reference
- **Who**: Developers during implementation
- **Use**: Copy-paste patterns, understand implementation details
- **Key Sections**:
  - Architecture diagram (visual flow)
  - Code examples (LMStudioClient, MetadataJudgingService, scorers)
  - Prompt templates (5+ examples for query generation)
  - Database query examples (SQL for common operations)
  - Testing examples (high-confidence, low-confidence cases)
  - Performance benchmarks (expected timings)
  - Troubleshooting guide

### 5. **research.md** (Design Decisions & Best Practices)
- **What**: Research findings, design rationale, alternatives considered
- **Who**: Technical leads, architects
- **Use**: Understand WHY decisions were made
- **Key Sections**:
  - LM Studio integration (why local AI, model selection)
  - Prompt engineering (15+ tested variations, best practices)
  - Confidence scoring (4 criteria, tested weights, 0.60 threshold rationale)
  - Domain quality metrics (tier calculation logic)
  - Performance optimization (Virtual Threads, batch operations)
  - Alternative approaches considered (cloud AI, ML classifiers, regex)

---

## 🚀 Getting Started (Next Session)

### Step 1: Review Documentation (30 minutes)

Read in this order:
1. **spec.md** - Understand business requirements
2. **plan.md** - Understand architecture
3. **tasks.md** - Understand task breakdown
4. **quickstart.md** - See code examples
5. **research.md** - Understand design decisions (optional, but recommended)

### Step 2: Verify Prerequisites (15 minutes)

```bash
# 1. Feature 003 complete
cd /Users/kevin/github/northstar-funding/backend
mvn test -Dtest=MondayNightlyDiscoveryTest,DomainDeduplicationTest,CircuitBreakerTest

# 2. LM Studio running on Mac Studio
ssh macstudio "curl -s http://localhost:1234/v1/models"

# 3. PostgreSQL accessible
ssh macstudio "pg_isready -h localhost -p 5432"

# 4. Development environment ready
java --version  # Should show Java 25
mvn --version   # Should show Maven 3.9+
```

### Step 3: Create Feature Branch (5 minutes)

```bash
cd /Users/kevin/github/northstar-funding
git checkout main
git pull
git checkout -b 004-ai-query-generation-metadata-judging
```

### Step 4: Start with Phase 4.1 (Database Schema)

Follow **tasks.md** starting with **T001**:

```bash
# Create first migration
touch backend/src/main/resources/db/migration/V13__create_domains_table.sql

# Follow spec in tasks.md T001
# Then move to T002, T003, etc.
```

---

## 📊 Key Metrics & Targets

### Query Generation
- Generate: 10-20 queries/day
- Uniqueness: >= 80% different from previous 7 days
- Geographic diversity: >= 5 different geographies/session
- Category diversity: >= 5 different categories/session
- Generation time: < 30 seconds
- Fallback usage: <= 5% of sessions

### Metadata Judging
- Process: 200-400 domains/session
- Judging time: < 5 minutes for 400 domains
- High-confidence candidates: 50-150/session
- **Precision: >= 70%** (manual review pass rate for score >= 0.60)
- Scoring consistency: +/- 0.05 for same result

### Domain Quality Tracking
- Quality distribution: 10-20% HIGH, 30-40% MEDIUM, 40-60% LOW
- Tier stability: >= 85% maintain tier month-over-month
- Blacklist effectiveness: 0 candidates from blacklisted domains

---

## 🗄️ Database Schema Overview

### New Tables (V13-V15)

**domains** (V13):
- Tracks domain quality metrics (best_score, counts, tier)
- Supports blacklist management
- ~1000 domains expected after 1 month

**metadata_judgments** (V14):
- Records all scoring decisions
- Links to domain and candidate
- ~10,000 judgments expected after 1 month

**query_generation_sessions** (V15):
- Tracks AI query generation statistics
- Records approval/rejection reasons
- ~30 sessions expected after 1 month

### Extended Tables (V16-V17)

**search_queries** (V16):
- Add: generation_method (AI_GENERATED | HARDCODED)
- Add: ai_model_used, query_template_id, semantic_cluster_id
- Existing queries stay as HARDCODED

**candidates** (V17):
- Add: confidence_score, judging_criteria, domain_quality_tier
- Add: metadata_title, metadata_description, search_engine_source
- Backward compatible with Feature 002/003

---

## 🧩 Component Architecture

```
QueryGenerationService
  ├─ LMStudioClient (circuit breaker, retry)
  ├─ PromptTemplateManager (5+ templates)
  ├─ QueryValidator (reject generic/duplicate)
  └─ SemanticSimilarityChecker (80% threshold)

MetadataJudgingService
  ├─ FundingKeywordsScorer (0.30 weight)
  ├─ DomainCredibilityScorer (0.25 weight)
  ├─ GeographicRelevanceScorer (0.25 weight)
  ├─ OrganizationTypeScorer (0.20 weight)
  └─ ConfidenceAggregator (weighted sum)

DomainQualityTracker
  ├─ QualityTierCalculator (HIGH/MEDIUM/LOW logic)
  ├─ BlacklistManager (add/remove/check)
  └─ QualityReportGenerator (CSV/JSON export)

Schedulers
  ├─ QueryGenerationScheduler (1:00 AM daily)
  └─ NightlyDiscoveryScheduler (2:00 AM daily, from Feature 003)
```

---

## 🧪 Testing Strategy

### Unit Tests (40+ tests)
- Domain entities: quality tier calculation, metrics update
- Services: query generation, validation, scoring components
- Scorers: each scoring component independently

### Integration Tests (10+ tests)
- Database integration: repositories with TestContainers
- End-to-end workflow: generation → search → judge → candidate
- Blacklist effectiveness: blacklisted domains skip processing
- Performance: 200 domains in < 5 minutes

### Manual Testing
- LM Studio prompt engineering (test various templates)
- Scoring accuracy (collect 50 examples, calculate precision)
- Quality tier calculation (verify tier assignments)

---

## 🎓 Design Patterns & Best Practices

### From Feature 003 (Apply to Feature 004)

**1. Set<String> for Simple Arrays**:
```java
// ✅ Good (simple, no Spring Data JDBC complexity)
@Column("keywords_found")
private Set<String> keywordsFound; // TEXT[] in database

// ❌ Avoid (Spring Data JDBC treats as relationship)
private Set<FundingKeyword> keywordsFound;
```

**2. BigDecimal for Scores**:
```java
// ✅ Good (precise, no floating-point errors)
@Column("confidence_score")
private BigDecimal confidenceScore; // DECIMAL(3,2)

// ❌ Avoid (floating-point precision issues)
private Double confidenceScore;
```

**3. Virtual Threads for Parallelism**:
```java
// ✅ Good (3x speedup for I/O-bound operations)
results.parallelStream()
    .map(this::judgeOne)
    .toList();

// ❌ Avoid (no parallelism)
results.stream()
    .map(this::judgeOne)
    .toList();
```

**4. Vavr Try for Error Handling**:
```java
// ✅ Good (functional error handling)
public Try<List<String>> generateQueries(String prompt) {
    return Try.of(() -> lmStudioClient.call(prompt))
        .onFailure(e -> log.error("Generation failed", e));
}

// ❌ Avoid (checked exceptions everywhere)
public List<String> generateQueries(String prompt) throws Exception { ... }
```

---

## 🚨 Common Pitfalls to Avoid

### Pitfall 1: Hardcoding Weights in Code

❌ **Bad**:
```java
confidence = funding * 0.30 + credibility * 0.25 + ...;
```

✅ **Good**:
```java
@Value("${scoring.weights.funding-keywords}") double fundingWeight;
confidence = funding * fundingWeight + ...;
```

### Pitfall 2: Forgetting to Update Domain Metrics

❌ **Bad**:
```java
createCandidate(judgment);
// Forgot to update domain quality!
```

✅ **Good**:
```java
createCandidate(judgment);
updateDomainQuality(domain, judgment.getConfidenceScore());
```

### Pitfall 3: Not Handling LM Studio Failures

❌ **Bad**:
```java
var queries = lmStudioClient.generateQueries(prompt).get();
// Crashes if LM Studio is down!
```

✅ **Good**:
```java
var result = lmStudioClient.generateQueries(prompt);
if (result.isFailure()) {
    log.error("Falling back to hardcoded queries");
    return fallbackQueries();
}
```

### Pitfall 4: Using JSONB for Simple Data

❌ **Bad** (from Feature 003 lessons):
```sql
CREATE TABLE metadata_judgments (
    judging_criteria JSONB  -- Complex, hard to query
);
```

✅ **Good**:
```sql
CREATE TABLE metadata_judgments (
    funding_keywords_score DECIMAL(3,2),  -- Simple, queryable
    domain_credibility_score DECIMAL(3,2),
    -- ... other scores as separate columns
);
```

---

## 📝 TODO Before Starting

- [ ] Read spec.md (understand requirements)
- [ ] Read plan.md (understand architecture)
- [ ] Read tasks.md (understand task breakdown)
- [ ] Review quickstart.md (see code examples)
- [ ] Verify LM Studio is running on Mac Studio
- [ ] Verify PostgreSQL is accessible
- [ ] Create feature branch `004-ai-query-generation-metadata-judging`
- [ ] Read CLAUDE.md for project conventions

---

## 🔗 Related Documentation

- **Feature 003**: `/specs/003-search-execution-infrastructure/`
  - Provides: SearchExecutionService, SearchQuery entity, deduplication logic
  - Integration point: NightlyDiscoveryScheduler calls MetadataJudgingService
- **CLAUDE.md**: `/CLAUDE.md`
  - Project-wide conventions, coding standards, testing practices
- **Constitution**: `/PROJECT_CONSTITUTION.md` (if exists)
  - "Local AI only" requirement (no cloud AI services)
  - "Java 25 Virtual Threads" requirement

---

## ❓ Questions & Support

**If you get stuck**:
1. Check quickstart.md for code examples
2. Check research.md for design rationale
3. Check Feature 003 for similar patterns
4. Review CLAUDE.md for project conventions

**Common questions addressed in research.md**:
- Why 0.60 threshold? (Section: Confidence Scoring Approaches)
- Why these scoring weights? (Section: Confidence Scoring Approaches)
- Why LM Studio instead of cloud AI? (Section: LM Studio Integration)
- How to tune scoring weights? (Section: Recommendations for Implementation)

---

## 🎯 Success Criteria

**Feature 004 is complete when**:
- ✅ All 55 tasks (T001-T055) completed
- ✅ All tests pass (40+ unit tests, 10+ integration tests)
- ✅ Query generation produces 10-20 diverse queries/day
- ✅ Metadata judging achieves >= 70% precision
- ✅ Domain quality tracking identifies HIGH/MEDIUM/LOW tiers
- ✅ Full workflow executes: gen → search → judge → candidate → track
- ✅ Documentation updated (CLAUDE.md, manual-testing.md, COMPLETION-SUMMARY.md)
- ✅ Production-ready for deployment

---

**Last Updated**: 2025-10-21
**Next Session**: Start with Phase 4.1 (Database Schema & Domain Model)

Good luck! 🚀
