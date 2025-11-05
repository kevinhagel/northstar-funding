# Implementation Plan: Search Result Processing (Story 1.3)

**Feature**: Story 1.3 - Search Result Processing
**Branch**: `feature/story-1.3-search-result-processing`
**Created**: 2025-11-05
**Status**: Ready to implement
**Prerequisites**: ✅ Domain Credibility Service + Research Complete (44 tests passing)

---

## Overview

This plan implements the bridge between raw search results (Feature 003) and structured funding source candidates. We'll create two new service classes (`SearchResultProcessor` and `CandidateCreationService`) with clean orchestration logic based on our TLD credibility research.

## Architecture

### Module Structure
```
northstar-crawler/  (existing module - add new services here)
├── scoring/
│   └── DomainCredibilityService.java (✅ DONE - 44 tests)
├── service/
│   ├── SearchResultProcessor.java (TODO)
│   └── CandidateCreationService.java (TODO)
└── model/
    └── ProcessingStatistics.java (TODO - internal model)
```

### Key Design Decisions from Research

1. **TLD Scoring System**: Use `DomainCredibilityService` for TLD credibility scoring
   - Tier 1 (+0.20): Validated nonprofits, government domains
   - Tier 2 (+0.15): .org, EU domains, Eastern Europe ccTLDs
   - Tier 3 (+0.08): Generic business (.com, .net)
   - Tier 4 (0.00): Cheap/unrestricted TLDs
   - Tier 5 (negative): Spam/phishing TLDs

2. **Early Spam Filtering**: Filter Tier 5 TLDs BEFORE domain deduplication to save resources

3. **Confidence Threshold**: >= 0.60 for PENDING_CRAWL, < 0.60 for LOW_CONFIDENCE

4. **BigDecimal Precision**: All confidence scores use BigDecimal scale 2

---

## Implementation Tasks

### ✅ Phase 0: Research & Foundation (COMPLETE)
- ✅ Task 0.1: TLD credibility research → `specs/006-search-result-processing/tld-credibility-research.md`
- ✅ Task 0.2: DomainCredibilityService implementation → 44 tests passing
- ✅ Task 0.3: Specification draft → `specs/006-search-result-processing/spec.md`

---

### Phase 1: Confidence Scoring Logic

#### Task 1: Create ConfidenceScorer service
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/scoring/ConfidenceScorer.java`

**Purpose**: Calculate overall confidence scores from search metadata + TLD credibility

**Dependencies**:
- `DomainCredibilityService` (inject via constructor)

**TDD Approach**:
1. Write test: `ConfidenceScorerTest.java`
   - Test funding keywords in title (+0.15)
   - Test funding keywords in description (+0.10)
   - Test geographic relevance (+0.15)
   - Test organization type detection (+0.15)
   - Test compound boost (+0.15)
   - Test TLD integration (uses DomainCredibilityService)
   - Test score capping at 1.00
   - Test spam TLD handling (cap at 0.00 minimum)
   - Test BigDecimal scale 2 precision

2. Run tests → RED

3. Implement `ConfidenceScorer`:
```java
@Service
public class ConfidenceScorer {
    private final DomainCredibilityService domainCredibilityService;

    public ConfidenceScorer(DomainCredibilityService domainCredibilityService) {
        this.domainCredibilityService = domainCredibilityService;
    }

    public BigDecimal calculateConfidence(String title,
                                         String description,
                                         String url) {
        BigDecimal score = domainCredibilityService.getTldScore(url);

        // Add keyword scoring
        if (containsFundingKeywords(title)) {
            score = score.add(new BigDecimal("0.15"));
        }
        if (containsFundingKeywords(description)) {
            score = score.add(new BigDecimal("0.10"));
        }
        // ... (see algorithm below)

        // Cap at 1.00, floor at 0.00
        if (score.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean containsFundingKeywords(String text) {
        // Keywords: grant, funding, scholarship, fellowship, subsidy, etc.
    }

    private boolean hasGeographicMatch(String text) {
        // Bulgaria, EU, Eastern Europe, regional terms
    }

    private boolean hasOrganizationType(String text) {
        // Ministry, Commission, Foundation, University
    }
}
```

4. Run tests → GREEN

**Confidence Scoring Algorithm**:
```
Base: TLD score from DomainCredibilityService (-0.30 to +0.20)
+ 0.15 if title contains funding keywords
+ 0.10 if description contains funding keywords
+ 0.15 if geographic match (Bulgaria, EU, Eastern Europe)
+ 0.15 if organization type (Ministry, Commission, Foundation)
+ 0.15 if multiple signals present (compound boost)

Minimum: 0.00 (even if spam TLD)
Maximum: 1.00

Threshold: >= 0.60 → PENDING_CRAWL
         < 0.60 → LOW_CONFIDENCE
```

**Estimate**: 2-3 hours

---

#### Task 2: Create CandidateCreationService
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/service/CandidateCreationService.java`

**Purpose**: Business logic for creating candidates with confidence scoring

**Dependencies**:
- `FundingSourceCandidateRepository`
- `ConfidenceScorer`

**TDD Approach**:
1. Write unit tests: `CandidateCreationServiceTest.java` (Mockito)
   - Test createCandidate() with high confidence (>= 0.60) → status PENDING_CRAWL
   - Test createCandidate() with low confidence (< 0.60) → status LOW_CONFIDENCE
   - Test metadata extraction from SearchResult
   - Test organizationName extraction from domain/title
   - Test BigDecimal precision for confidence
   - Test null/empty handling

2. Run tests → RED

3. Implement service:
```java
@Service
@Transactional
public class CandidateCreationService {
    private final FundingSourceCandidateRepository candidateRepository;
    private final ConfidenceScorer confidenceScorer;

    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.60");

    public CandidateCreationService(
        FundingSourceCandidateRepository candidateRepository,
        ConfidenceScorer confidenceScorer) {
        this.candidateRepository = candidateRepository;
        this.confidenceScorer = confidenceScorer;
    }

    public FundingSourceCandidate createCandidate(
        SearchResult result,
        Long sessionId,
        Long domainId) {

        BigDecimal confidence = confidenceScorer.calculateConfidence(
            result.getTitle(),
            result.getDescription(),
            result.getUrl()
        );

        CandidateStatus status = confidence.compareTo(CONFIDENCE_THRESHOLD) >= 0
            ? CandidateStatus.PENDING_CRAWL
            : CandidateStatus.LOW_CONFIDENCE;

        FundingSourceCandidate candidate = FundingSourceCandidate.builder()
            .title(result.getTitle())
            .organizationName(extractOrganizationName(result))
            .sourceUrl(result.getUrl())
            .discoverySource(result.getEngine().name())
            .discoveryMethod("SEARCH_METADATA_ONLY")
            .confidenceScore(confidence)
            .status(status)
            .discoveredAt(LocalDateTime.now())
            .sessionId(sessionId)
            .domainId(domainId)
            .build();

        return candidateRepository.save(candidate);
    }

    private String extractOrganizationName(SearchResult result) {
        // Extract from domain or title
    }
}
```

4. Run tests → GREEN

**Estimate**: 2 hours

---

### Phase 2: Domain Deduplication Integration

#### Task 3: Enhance DomainService with processing helpers
**File**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/service/DomainService.java`

**Purpose**: Add helper methods for search result processing

**TDD Approach**:
1. Add tests to `DomainServiceTest.java`:
   - Test extractDomainFromUrl() - various URL formats
   - Test updateLastSeen() - updates timestamp
   - Test isBlacklisted() - checks domain status
   - Test registerOrGetDomain() - idempotent domain registration

2. Run tests → RED

3. Implement methods:
```java
public String extractDomainFromUrl(String url) {
    // Extract domain name from URL using URI or string parsing
}

public void updateLastSeen(Long domainId) {
    domainRepository.findById(domainId).ifPresent(domain -> {
        domain.setLastSeenAt(LocalDateTime.now());
        domainRepository.save(domain);
    });
}

public boolean isBlacklisted(Long domainId) {
    return domainRepository.findById(domainId)
        .map(domain -> domain.getStatus() == DomainStatus.BLACKLISTED)
        .orElse(false);
}

public Domain registerOrGetDomain(String domainName, Long sessionId) {
    return domainRepository.findByDomainName(domainName)
        .orElseGet(() -> registerDomain(domainName, sessionId));
}
```

4. Run tests → GREEN

**Estimate**: 1 hour

---

### Phase 3: Orchestration

#### Task 4: Create ProcessingStatistics model
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/model/ProcessingStatistics.java`

**Purpose**: Internal model for tracking processing results

**Implementation**:
```java
@Data
@Builder
public class ProcessingStatistics {
    private int totalResults;
    private int candidatesCreated;
    private int highConfidenceCount;  // >= 0.60
    private int lowConfidenceCount;   // < 0.60
    private int duplicatesSkipped;
    private int blacklistedSkipped;
    private int spamTldsFiltered;     // NEW: Tier 5 TLDs filtered early
}
```

**Estimate**: 15 minutes

---

#### Task 5: Create SearchResultProcessor
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/service/SearchResultProcessor.java`

**Purpose**: Orchestrate result → candidate conversion with spam filtering

**Dependencies**:
- `SearchResultRepository`
- `DomainService`
- `DomainCredibilityService` (for spam TLD detection)
- `CandidateCreationService`
- `DiscoverySessionRepository`

**TDD Approach**:
1. Write unit tests: `SearchResultProcessorTest.java` (Mockito)
   - Test processResults() with empty list → no candidates created
   - Test processResults() with 10 results, 8 unique domains → 8 candidates
   - Test domain deduplication (5 results, same domain) → 1 candidate
   - Test blacklist filtering → candidates not created
   - Test spam TLD filtering → Tier 5 TLDs filtered early
   - Test session statistics update
   - Test confidence threshold split

2. Run tests → RED

3. Implement orchestrator:
```java
@Service
@Transactional
public class SearchResultProcessor {
    private final SearchResultRepository searchResultRepository;
    private final DomainService domainService;
    private final DomainCredibilityService domainCredibilityService;
    private final CandidateCreationService candidateCreationService;
    private final DiscoverySessionRepository sessionRepository;

    public SearchResultProcessor(/* dependencies */) { }

    public ProcessingStatistics processResults(
        List<SearchResult> results,
        Long sessionId) {

        ProcessingStatistics.Builder stats = ProcessingStatistics.builder()
            .totalResults(results.size());

        // STEP 1: Filter spam TLDs BEFORE deduplication
        List<SearchResult> filteredResults = results.stream()
            .filter(result -> !domainCredibilityService.isSpamTld(result.getUrl()))
            .toList();

        stats.spamTldsFiltered(results.size() - filteredResults.size());

        // STEP 2: Group by domain for deduplication
        Map<String, List<SearchResult>> byDomain = filteredResults.stream()
            .collect(Collectors.groupingBy(result ->
                domainService.extractDomainFromUrl(result.getUrl())
            ));

        int candidatesCreated = 0;
        int highConfidence = 0;
        int lowConfidence = 0;
        int blacklisted = 0;

        // STEP 3: Process each domain
        for (Map.Entry<String, List<SearchResult>> entry : byDomain.entrySet()) {
            String domainName = entry.getKey();
            List<SearchResult> domainResults = entry.getValue();

            // Register or get existing domain
            Domain domain = domainService.registerOrGetDomain(domainName, sessionId);

            // Check blacklist
            if (domainService.isBlacklisted(domain.getId())) {
                blacklisted++;
                continue;
            }

            // Use first result (could merge metadata later)
            SearchResult result = domainResults.get(0);

            // Create candidate
            FundingSourceCandidate candidate = candidateCreationService.createCandidate(
                result, sessionId, domain.getId()
            );

            candidatesCreated++;

            if (candidate.getStatus() == CandidateStatus.PENDING_CRAWL) {
                highConfidence++;
            } else {
                lowConfidence++;
            }

            // Update domain last_seen_at
            domainService.updateLastSeen(domain.getId());
        }

        stats.candidatesCreated(candidatesCreated)
            .highConfidenceCount(highConfidence)
            .lowConfidenceCount(lowConfidence)
            .duplicatesSkipped(filteredResults.size() - candidatesCreated)
            .blacklistedSkipped(blacklisted);

        // STEP 4: Update session statistics
        updateSessionStatistics(sessionId, stats.build());

        return stats.build();
    }

    private void updateSessionStatistics(Long sessionId, ProcessingStatistics stats) {
        // Update DiscoverySession with processing results
    }
}
```

4. Run tests → GREEN

**Estimate**: 3-4 hours

---

### Phase 4: Integration Tests

#### Task 6: Create end-to-end integration test
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/SearchResultProcessingIntegrationTest.java`

**Purpose**: Test complete flow with real PostgreSQL

**Test Scenarios**:
1. Happy path: 20 results → 15 candidates (deduplication + spam filtering)
2. Spam TLD filtering: 5 spam TLDs (.xyz, .tk) → 0 candidates for those
3. Blacklist filtering: 3 blacklisted domains → 0 candidates
4. Confidence threshold: Mixed quality → correct status split (PENDING_CRAWL vs LOW_CONFIDENCE)
5. Domain updates: Multiple results same domain → last_seen_at updated
6. Session statistics: Verify all counts are accurate
7. TLD tier validation: .ngo gets higher confidence than .com

**Pattern**: Use TestContainers + PostgreSQL 16-alpine

**Estimate**: 3-4 hours

---

### Phase 5: Documentation & Cleanup

#### Task 7: Update CLAUDE.md
**File**: `CLAUDE.md`

**Changes**:
- Add SearchResultProcessor and CandidateCreationService to architecture section
- Add ConfidenceScorer and DomainCredibilityService
- Update "What's Working" to include Story 1.3
- Document confidence scoring algorithm
- Update test count (add ~35-40 new tests)

**Estimate**: 30 minutes

---

#### Task 8: Create session summary
**File**: `northstar-notes/session-summaries/2025-11-05-story-1.3-search-result-processing.md`

**Content**:
- Implementation summary
- Test results (total tests passing)
- Key decisions (TLD research, early spam filtering)
- Files created/modified
- Lessons learned
- Next steps

**Estimate**: 30 minutes

---

## Task Summary

| Task | Component | Type | Estimate | Status |
|------|-----------|------|----------|--------|
| T0.1-T0.3 | Research + DomainCredibilityService | Research + TDD | 4h | ✅ DONE |
| T1 | ConfidenceScorer | Service + Unit Tests | 2-3h | TODO |
| T2 | CandidateCreationService | Service + Unit Tests | 2h | TODO |
| T3 | DomainService enhancements | Service + Unit Tests | 1h | TODO |
| T4 | ProcessingStatistics | Model | 15m | TODO |
| T5 | SearchResultProcessor | Orchestrator + Unit Tests | 3-4h | TODO |
| T6 | Integration tests | Integration Tests | 3-4h | TODO |
| T7 | CLAUDE.md update | Documentation | 30m | TODO |
| T8 | Session summary | Documentation | 30m | TODO |

**Total Estimate**: 16-18 hours (4 hours already complete)
**Remaining**: 12-14 hours

---

## Testing Goals

- **Unit Tests**: 30-35 new tests (Mockito)
  - ConfidenceScorer: 9 tests
  - CandidateCreationService: 6 tests
  - DomainService: 4 tests
  - SearchResultProcessor: 7 tests
  - DomainCredibilityService: ✅ 44 tests (done)

- **Integration Tests**: 7-10 tests (TestContainers)
  - End-to-end flow tests
  - Spam TLD filtering scenarios
  - Domain deduplication scenarios
  - Blacklist filtering
  - Session statistics verification

**Target**: 81-89 total tests for Story 1.3, all passing

---

## Success Criteria

- ✅ All tests pass (unit + integration)
- ✅ Spam TLD filtering working (Tier 5 filtered early)
- ✅ Confidence scoring algorithm implemented with TLD integration
- ✅ Domain deduplication working correctly
- ✅ Blacklist filtering prevents candidate creation
- ✅ Session statistics accurately updated
- ✅ Code follows service layer pattern (explicit constructors, no Lombok in services)
- ✅ TDD approach documented in commit history (test commits before implementation commits)
- ✅ BigDecimal scale 2 precision throughout

---

## Risks & Mitigations

**Risk 1**: Confidence scoring algorithm too simplistic
- **Mitigation**: ✅ Addressed with research-based TLD scoring (5 tiers)
- **Future**: Enhance with AI in separate feature

**Risk 2**: Performance issues with large result sets
- **Mitigation**: Spam TLD filtering BEFORE deduplication saves resources
- **Future**: Use Virtual Threads for parallel processing if batch > 50

**Risk 3**: Domain extraction edge cases
- **Mitigation**: ✅ DomainCredibilityService handles IDNs, subdomains, ports, paths

**Risk 4**: Keyword lists become outdated
- **Mitigation**: Start simple, make configurable in future
- **Future**: AI-powered keyword extraction

---

## Definition of Done

- [ ] All unit tests pass (30-35 tests)
- [ ] All integration tests pass (7-10 tests)
- [ ] Code reviewed against service layer pattern
- [ ] CLAUDE.md updated
- [ ] Session summary created
- [ ] No TODOs or FIXMEs in code
- [ ] Build succeeds: `mvn clean test`
- [ ] Feature branch ready for merge to main

---

## Key Design Highlights

### 1. Early Spam Filtering
Filter Tier 5 TLDs BEFORE domain deduplication:
```java
List<SearchResult> filtered = results.stream()
    .filter(result -> !domainCredibilityService.isSpamTld(result.getUrl()))
    .toList();
```

**Why**: No point tracking/deduplicating spam domains. Saves 40-60% processing resources.

### 2. TLD-First Confidence Scoring
Start with TLD credibility, then add keyword/geographic signals:
```java
BigDecimal score = domainCredibilityService.getTldScore(url);  // Base score
score = score.add(keywordScore);  // Add signals
```

**Why**: TLD credibility is objective, keywords are subjective. TLD sets the floor.

### 3. BigDecimal Throughout
All confidence calculations use BigDecimal scale 2:
```java
private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.60");
```

**Why**: Prevent floating-point precision errors in threshold comparisons.

### 4. Idempotent Domain Registration
`registerOrGetDomain()` prevents duplicate domain records:
```java
public Domain registerOrGetDomain(String domainName, Long sessionId) {
    return domainRepository.findByDomainName(domainName)
        .orElseGet(() -> registerDomain(domainName, sessionId));
}
```

**Why**: Multiple search engines may find same domain across sessions.

---

**Created**: 2025-11-05
**Status**: Ready for implementation
**Next Step**: Task 1 (ConfidenceScorer + tests)
**Prerequisites Met**: ✅ Research complete, DomainCredibilityService implemented (44 tests passing)
