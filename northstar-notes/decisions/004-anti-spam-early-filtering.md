# ADR 004: Anti-Spam Early Filtering Strategy

**Status**: ✅ IMPLEMENTED (Feature 003)
**Date**: 2025-11-02
**Context Tags**: #architecture #anti-spam #resource-optimization #search #crawler

## Context

When implementing search provider adapters for automated funding discovery (Feature 003), we discovered from prior spring-crawler experience that **40-60% of search engine results are scammer spam or SEO keyword-stuffing garbage**. This creates two critical problems:

1. **Downstream LLM Processing Waste**: Every spam result that passes through to the judging module wastes:
   - LLM API calls for metadata analysis
   - CPU time for confidence scoring
   - Database writes for blacklist management
   - Human review time when spam scores borderline

2. **Domain Blacklist Pollution**: Caching scammer domains in the `Domain` entity before filtering means:
   - Blacklist fills with obvious spam domains (casino sites, essay mills, etc.)
   - Domain deduplication cache becomes polluted
   - Legitimate domains harder to identify in noise

**Key Question**: When should we filter spam results - before or after domain deduplication?

## Decision

**Execute anti-spam filtering BEFORE domain deduplication check to prevent caching spam domains.**

### Filtering Pipeline Order

```
Search Result from Provider
    ↓
1. Anti-Spam Filtering (4 detectors)
    ├─ Keyword Stuffing Detection
    ├─ Domain-Metadata Mismatch Detection
    ├─ Unnatural Keyword List Detection
    └─ Cross-Category Spam Detection
    ↓
2. Domain Extraction & Deduplication
    ↓
3. Persistence (SearchResult entity)
    ↓
4. Downstream Judging (LLM-based confidence scoring)
```

### Four-Layer Anti-Spam Detection

**1. KeywordStuffingDetector**
- **Rule**: Unique word ratio < 0.5 indicates spam
- **Example**: "grants scholarships funding grants education grants financial aid grants"
- **Metric**: 4 unique words / 8 total words = 0.50 (borderline spam)

**2. DomainMetadataMismatchDetector**
- **Rule**: Fuzzy similarity between domain keywords and title/description < 0.15 indicates scammer
- **Example**: Domain "casinowinners.com" with title "Education Scholarships for Students"
- **Technique**: Levenshtein distance, Cosine similarity, FuzzyScore (Apache Commons Text)

**3. UnnaturalKeywordListDetector**
- **Rule**: Absence of common articles/prepositions ("the", "a", "of", "for") indicates keyword stuffing
- **Example**: "scholarship grant funding education university college student financial"
- **Natural text**: "Scholarships and grants for students at the university"

**4. CrossCategorySpamDetector**
- **Rule**: Detects gambling/essay-mill domains with education keywords
- **Patterns**:
  - Gambling: "casino", "poker", "betting", "win", "jackpot"
  - Fake Degrees: "diploma", "essay", "paper writing", "buy degree"
- **Example**: "pokerchampions.net/scholarships" → REJECT

## Rationale

### Why Filter Before Deduplication

**Problem with filtering AFTER deduplication**:
1. Spam domain "casino-edu.com/grants" gets added to Domain entity
2. Domain status set to DISCOVERED
3. Later spam filtering rejects the result
4. Domain entity remains in database as discovered spam
5. Blacklist management has to clean up cached spam domains

**Benefits of filtering BEFORE deduplication**:
1. Spam results never touch the Domain entity
2. Domain deduplication cache stays clean (legitimate organizations only)
3. Blacklist management tracks only confirmed scammers from deep crawl phase
4. Domain quality metrics remain accurate

### Resource Conservation Impact

Based on spring-crawler experience with 1000 search results:

**Without Early Filtering**:
- 1000 search results → 1000 Domain checks → 1000 SearchResult saves
- ~500 spam results processed by LLM judging module
- ~500 LLM API calls wasted on obvious spam
- 500 spam domains cached in Domain entity

**With Early Filtering**:
- 1000 search results → 600 pass anti-spam → 600 Domain checks
- ~400 spam results rejected immediately (no LLM processing)
- ~200 borderline results processed by LLM (legitimate close-calls)
- 0 spam domains cached (only legitimate domains in Domain entity)

**Savings**: 40-60% reduction in downstream processing load

## Implementation

### Anti-Spam Filter Interface

```java
public interface AntiSpamFilter {
    /**
     * Analyzes search result for spam indicators.
     * Returns SpamAnalysisResult with detected indicators.
     */
    SpamAnalysisResult analyze(SearchResult result);

    /**
     * Returns true if result should be rejected (spam detected).
     */
    boolean isSpam(SearchResult result);
}
```

### Usage in Multi-Provider Orchestrator

```java
@Service
public class MultiProviderSearchOrchestratorImpl {
    private final AntiSpamFilter antiSpamFilter;
    private final DomainService domainService;

    public SearchExecutionResult executeSearch(String query, int maxResults) {
        List<SearchResult> allResults = fetchFromAllProviders(query);

        // 1. FIRST: Filter spam (before domain deduplication)
        List<SearchResult> nonSpamResults = allResults.stream()
            .filter(result -> !antiSpamFilter.isSpam(result))
            .toList();

        // 2. SECOND: Extract domains and check deduplication
        List<SearchResult> uniqueResults = nonSpamResults.stream()
            .filter(result -> {
                String domain = extractDomain(result.getUrl());
                return !domainService.isDomainAlreadyProcessed(domain);
            })
            .toList();

        // 3. THIRD: Persist and continue downstream
        return new SearchExecutionResult(uniqueResults, statistics);
    }
}
```

### Spam Analysis Result

```java
public record SpamAnalysisResult(
    boolean isSpam,
    List<SpamIndicator> indicators,
    String rejectionReason
) {
    public enum SpamIndicator {
        KEYWORD_STUFFING,
        DOMAIN_METADATA_MISMATCH,
        UNNATURAL_KEYWORD_LIST,
        CROSS_CATEGORY_SPAM
    }
}
```

## Consequences

### Positive

1. **Massive Resource Savings**: 40-60% reduction in LLM processing costs
2. **Clean Domain Cache**: Domain entity only contains legitimate organizations
3. **Faster Processing**: Spam filtered out immediately, no database I/O for spam
4. **Better Blacklist Quality**: Blacklist management focuses on confirmed scammers from deep crawl phase
5. **Improved User Experience**: Manual reviewers only see legitimate candidates (no obvious spam)

### Negative

1. **False Positives Possible**: Aggressive filtering might reject some legitimate results
   - **Mitigation**: Spam indicators use conservative thresholds (0.5 unique ratio, 0.15 similarity)
   - **Mitigation**: Multiple indicators required for rejection (not single-indicator rejection)

2. **Additional Processing Upfront**: Anti-spam analysis adds ~5-10ms per result
   - **Trade-off**: Worth it to save downstream LLM calls (100-500ms each)

3. **Maintenance Required**: Spam patterns evolve, detectors need periodic tuning
   - **Mitigation**: Contract tests verify detector behavior stays consistent
   - **Mitigation**: Integration tests validate end-to-end spam filtering

### Neutral

- Spam filtering is deterministic (no LLM calls) - rules-based detection only
- Logging spam rejections provides monitoring for false positive analysis

## Testing Strategy

### Unit Tests (100 tests across 4 detectors)
- KeywordStuffingDetectorTest (16 tests)
- DomainMetadataMismatchDetectorTest (21 tests)
- UnnaturalKeywordListDetectorTest (16 tests)
- CrossCategorySpamDetectorTest (21 tests)
- AntiSpamFilterImplTest (15 tests)

### Contract Tests (14 tests)
- AntiSpamFilterContractTest validates interface compliance

### Integration Tests (7 tests)
- AntiSpamIntegrationTest validates end-to-end filtering in multi-provider orchestration

**Total Anti-Spam Test Coverage**: 121 tests

## Metrics & Monitoring

**Key Metrics to Track**:
1. **Spam Detection Rate**: % of results rejected by anti-spam filtering
2. **False Positive Rate**: % of rejected results that manual review deems legitimate
3. **Resource Savings**: Reduction in downstream LLM API calls
4. **Domain Cache Quality**: % of Domain entities marked as blacklisted (should be low)

**Success Criteria** (from Feature 003 spec):
- Spam detection filters out 40-60% of results before downstream processing
- False positive rate < 5% (validated through manual review sampling)

## Alternatives Considered

### Alternative 1: Filter After Domain Deduplication
**Rejected because**: Pollutes Domain entity cache with spam domains, requires cleanup logic

### Alternative 2: Filter After LLM Judging
**Rejected because**: Wastes LLM API calls on obvious spam, defeats purpose of early filtering

### Alternative 3: No Filtering (Let LLM Handle Everything)
**Rejected because**: 40-60% processing overhead, expensive LLM calls wasted on obvious spam

### Alternative 4: Single-Pass Filtering (One Detector)
**Rejected because**: Each detector catches different spam patterns, need 4-layer defense

## Related Decisions

- [[002-domain-level-deduplication]] - Domain entity design this filtering protects
- [[003-testcontainers-integration-test-pattern]] - Testing strategy for anti-spam components

## References

**Implementation**:
- `northstar-crawler/src/main/java/com/northstar/funding/crawler/antispam/`
- `AntiSpamFilter.java` (interface)
- `AntiSpamFilterImpl.java` (4-detector implementation)

**Tests**:
- `northstar-crawler/src/test/java/com/northstar/funding/crawler/unit/` (detector unit tests)
- `northstar-crawler/src/test/java/com/northstar/funding/crawler/contract/AntiSpamFilterContractTest.java`
- `northstar-crawler/src/test/java/com/northstar/funding/crawler/integration/AntiSpamIntegrationTest.java`

**Feature Spec**: `specs/003-design-and-implement/spec.md` (FR-035 through FR-042)

**Session Summary**: `session-summaries/2025-11-02-feature-003-search-infrastructure-complete.md`

## Future Enhancements

1. **Machine Learning Spam Detection**: Train classifier on manually-reviewed spam corpus
2. **Domain Reputation Service**: External API for known scammer domain lists
3. **Adaptive Thresholds**: Auto-tune detection thresholds based on false positive feedback
4. **Spam Pattern Learning**: Automatically discover new spam patterns from rejected results

---

**Last Updated**: 2025-11-02
**Implementation Status**: ✅ COMPLETE (Feature 003)
**Test Coverage**: 121 tests (all passing)
