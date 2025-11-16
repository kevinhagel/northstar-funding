# Research: SearchResultProcessor Refactoring

**Feature**: 012-refactor-searchresultprocessor-to
**Date**: 2025-11-15
**Purpose**: Research refactoring patterns, testing strategies, and logging best practices for SearchResultProcessor refactoring

## Research Areas

### 1. Refactoring Patterns

#### Extract Method Pattern
**Decision**: Use Extract Method refactoring to create independent pipeline stage methods

**Approach**:
1. Identify cohesive blocks of code in `processSearchResults()` that perform single responsibilities
2. Extract each block into a private method with descriptive name
3. Each extracted method should have clear inputs/outputs and single responsibility
4. Keep extracted methods in same class (no need for separate strategy classes - YAGNI)

**Benefits**:
- Improves readability (method names document intent)
- Enables independent unit testing of each stage
- Reduces cognitive load (82-line method → multiple focused methods)
- Maintains encapsulation (private methods, no API surface expansion)

**Backward Compatibility**: Public `processSearchResults()` method signature unchanged, behavior identical

#### Replace Primitive with Object Pattern
**Decision**: Create `ProcessingContext` domain object to replace scattered primitive state

**Approach**:
1. Create ProcessingContext class encapsulating:
   - Session ID (UUID)
   - Confidence threshold (BigDecimal constant)
   - Seen domains (HashSet<String>)
   - All counters (6 int fields)
2. Provide methods for state mutations (`recordSpamTldFiltered()`, `markDomainAsSeen()`, etc.)
3. Generate ProcessingStatistics from context state (`buildStatistics()`)

**Benefits**:
- Eliminates primitive obsession (no scattered int variables)
- Encapsulates state management logic
- Type-safe operations (can't accidentally mix up counters)
- Easier to test (context state is explicit, not implicit)
- Future extensibility (easy to add new tracking metrics)

**Alternative Considered**: Keep primitives, use builder for statistics
**Rejected Because**: Doesn't address state management complexity, harder to test individual stages

#### Strategy Pattern for Pipeline Stages (REJECTED)
**Considered**: Create PipelineStage interface with implementations for each stage

**Rejected Because**:
- Over-engineering for this refactoring (YAGNI - You Aren't Gonna Need It)
- Adds unnecessary complexity (7 new classes vs 6 new methods)
- Pipeline order is fixed (not pluggable), so polymorphism adds no value
- Constitution Principle VI: Complexity Management - keep it simple
- Private methods with descriptive names achieve same clarity

**Decision**: Use simple extracted methods (Extract Method pattern only)

### 2. Testing Strategy

#### Test-Driven Development (TDD) Approach
**Decision**: Write tests BEFORE extracting each pipeline stage method

**TDD Cycle for Each Stage**:
1. RED: Write unit test for extracted method (test will fail - method doesn't exist yet)
2. GREEN: Extract method from processSearchResults, make test pass
3. REFACTOR: Clean up extracted method, ensure test still passes

**Example for Spam TLD Filtering**:
```java
// 1. RED: Write test first
@Test
void filterSpamTld_WhenSpamTld_RecordsAndReturnsTrue() {
    // Test doesn't compile yet - method doesn't exist
    ProcessingContext context = new ProcessingContext(sessionId);
    SearchResult result = SearchResult.builder()
        .url("https://scam.xyz/grants")
        .build();

    boolean isSpam = processor.filterSpamTld(result, context);

    assertThat(isSpam).isTrue();
    assertThat(context.getSpamTldFiltered()).isEqualTo(1);
}

// 2. GREEN: Extract method, make test pass
private boolean filterSpamTld(SearchResult result, ProcessingContext context) {
    if (domainCredibilityService.isSpamTld(result.getUrl())) {
        context.recordSpamTldFiltered();
        return true;
    }
    return false;
}

// 3. REFACTOR: Add logging, improve naming if needed
```

**Benefits**:
- Guarantees test coverage for all extracted methods
- Tests document expected behavior
- Regression safety (ensure refactoring doesn't change functionality)

#### TestContainers Integration Testing
**Decision**: Use existing Feature 011 TestContainers infrastructure to validate refactored code

**Approach**:
1. Run existing SearchResultProcessorTest with refactored implementation
2. All 7 existing tests MUST pass unchanged (backward compatibility proof)
3. No new integration tests needed (unit tests cover new methods)
4. TestContainers validates that refactored code works against real PostgreSQL

**Existing Tests to Validate**:
- testProcessEmptyResults()
- testDuplicateDomainsHandled()
- testLowConfidenceFiltered()
- testHighConfidenceCreatesCandidates()
- testBlacklistedDomainsSkipped()
- testStatisticsTracking()
- testEndToEndProcessing()

**Pass Criteria**: All 7 tests pass with exact same assertions, no modifications required

#### Mockito Best Practices for Testing Extracted Methods
**Decision**: Mock only external dependencies (DomainService, ConfidenceScorer), not ProcessingContext

**Mocking Strategy**:
- Mock: `DomainService`, `DomainCredibilityService`, `ConfidenceScorer`, `CandidateCreationService`
- Real object: `ProcessingContext` (lightweight, no I/O, easy to construct)
- Verify: Service method calls, ProcessingContext state changes

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class SearchResultProcessorTest {
    @Mock
    private DomainCredibilityService domainCredibilityService;

    @InjectMocks
    private SearchResultProcessor processor;

    @Test
    void filterSpamTld_WhenSpamTld_RecordsAndReturnsTrue() {
        ProcessingContext context = new ProcessingContext(UUID.randomUUID());
        SearchResult result = SearchResult.builder().url("https://scam.xyz/grants").build();

        when(domainCredibilityService.isSpamTld("https://scam.xyz/grants"))
            .thenReturn(true);

        boolean isSpam = processor.filterSpamTld(result, context);

        assertThat(isSpam).isTrue();
        assertThat(context.getSpamTldFiltered()).isEqualTo(1);
        verify(domainCredibilityService).isSpamTld("https://scam.xyz/grants");
    }
}
```

#### AssertJ Fluent Assertions
**Decision**: Use AssertJ for more readable test assertions

**Benefits**:
- Better readability: `assertThat(value).isEqualTo(expected)` vs `assertEquals(expected, value)`
- Better error messages: Shows actual vs expected clearly
- Type-safe: Compile-time checking of assertions
- Already in project dependencies (Spring Boot Test)

**Example**:
```java
// JUnit assertions (current)
assertEquals(1, stats.getSpamTldFiltered());
assertTrue(context.markDomainAsSeen("example.org"));

// AssertJ assertions (improved)
assertThat(stats.getSpamTldFiltered()).isEqualTo(1);
assertThat(context.markDomainAsSeen("example.org")).isTrue();
```

### 3. Logging Patterns

#### SLF4J Structured Logging
**Decision**: Use SLF4J with parameterized logging for performance and clarity

**Pattern**:
```java
private static final Logger logger = LoggerFactory.getLogger(SearchResultProcessor.class);

// Good: Parameterized (efficient)
logger.info("Processing session {} with {} results", sessionId, searchResults.size());
logger.warn("Spam TLD filtered: {}", result.getUrl());
logger.debug("Confidence score {} for {}", confidence, result.getUrl());

// Bad: String concatenation (inefficient if logging disabled)
logger.info("Processing session " + sessionId + " with " + searchResults.size() + " results");
```

**Benefits**:
- No string concatenation overhead when logging disabled
- Clear structure (parameters separated from message)
- Type-safe (compiler checks parameter types)

#### MDC (Mapped Diagnostic Context) for Session ID
**Decision**: Use MDC to correlate all logs for a single processing session

**Approach**:
```java
public ProcessingStatistics processSearchResults(List<SearchResult> searchResults, UUID sessionId) {
    // Set MDC at start
    MDC.put("sessionId", sessionId.toString());
    try {
        // All log statements in this thread will include sessionId
        logger.info("Starting search result processing for {} results", searchResults.size());
        // ... processing logic
        return stats;
    } finally {
        // Clean up MDC
        MDC.remove("sessionId");
    }
}
```

**Benefits**:
- Session ID automatically included in all logs
- Easy to filter/search logs by session
- No need to pass sessionId to every log statement

**Logback Configuration** (already in Spring Boot):
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [sessionId=%X{sessionId}] - %msg%n</pattern>
```

#### Log Level Best Practices
**Decision**: Use appropriate log levels for each pipeline stage

**Log Levels**:
- **INFO**: Normal processing milestones
  - Session start/end
  - Candidate created successfully
  - Overall statistics
- **WARN**: Unexpected but recoverable conditions
  - Spam TLD filtered
  - Blacklisted domain
  - Invalid URL (domain extraction failed)
- **DEBUG**: Detailed flow for troubleshooting
  - Duplicate detection
  - Confidence score calculated
  - Threshold check result
- **ERROR**: Unexpected exceptions (should not occur in normal processing)

**Example**:
```java
logger.info("Processing {} search results for session {}", searchResults.size(), sessionId);
logger.warn("Spam TLD filtered: {}", result.getUrl());
logger.debug("Duplicate domain skipped: {}", domain);
logger.debug("Confidence score {} for {}", confidence, result.getUrl());
logger.info("Created candidate {} for {}", candidate.getId(), result.getUrl());
```

#### Performance Impact of Logging
**Concern**: Logging in hot path (loop processing 1000s of results)

**Mitigation**:
1. Use parameterized logging (no concatenation overhead when disabled)
2. Use DEBUG for high-frequency logs (disabled in production by default)
3. Use INFO/WARN only for important milestones

**Benchmark Decision**: If logging causes >5% performance regression, move DEBUG logs behind `if (logger.isDebugEnabled())` guards

### 4. BigDecimal Best Practices

#### Current Usage Verification
**Verified**: Existing SearchResultProcessor uses BigDecimal correctly
- `BigDecimal threshold = new BigDecimal("0.60");` (line 80) - String constructor ✅
- `confidence.compareTo(threshold)` (line 112) - Proper comparison ✅
- ConfidenceScorer returns BigDecimal with scale 2 ✅

**No Changes Needed**: Current usage already follows constitution Data Precision Standards

#### Refactoring Preservation
**Decision**: Extract threshold as constant, preserve all BigDecimal usage

**Pattern**:
```java
public class SearchResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SearchResultProcessor.class);
    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.60");

    // ... use CONFIDENCE_THRESHOLD throughout
    if (confidence.compareTo(CONFIDENCE_THRESHOLD) >= 0) {
        // High confidence
    }
}
```

**Benefits**:
- DRY: Define threshold once
- Type-safe: Compiler enforces BigDecimal usage
- Easy to change threshold value in one place

#### Comparison Patterns
**Verified Pattern** (already correct in existing code):
```java
// Correct: Use compareTo()
if (confidence.compareTo(threshold) >= 0) { ... }  // High confidence
if (confidence.compareTo(threshold) < 0) { ... }   // Low confidence

// Wrong: Never use ==
if (confidence == threshold) { ... }  // Object identity, not value equality

// Wrong: Never use equals() for numeric comparison
if (confidence.equals(threshold)) { ... }  // Works but compareTo is clearer for ordering
```

## Research Summary

### Decisions Made
1. **Refactoring Pattern**: Extract Method (simple, focused methods) + Replace Primitive with Object (ProcessingContext)
2. **No Strategy Pattern**: Rejected as over-engineering (YAGNI)
3. **TDD Approach**: Write tests before extracting methods
4. **Testing**: Mockito for unit tests, TestContainers for integration validation
5. **Logging**: SLF4J with MDC, parameterized messages, appropriate log levels
6. **BigDecimal**: Extract CONFIDENCE_THRESHOLD constant, preserve existing correct usage

### Alternatives Rejected
1. Strategy Pattern for pipeline stages (over-engineering)
2. Keep primitive state management (doesn't solve complexity)
3. String concatenation logging (performance overhead)
4. Change BigDecimal usage (already correct)

### Technical Risks
1. **Performance**: Logging overhead in hot path
   - Mitigation: Use DEBUG level, parameterized logging
2. **Test Coverage**: Missing edge cases in extracted method tests
   - Mitigation: TDD approach guarantees coverage
3. **Regression**: Breaking existing functionality
   - Mitigation: All 7 existing tests must pass unchanged

### Success Criteria
- ✅ All 8 refactoring objectives met (spec FR-001 through FR-020)
- ✅ All 7 existing SearchResultProcessorTest scenarios pass unchanged
- ✅ New unit tests for ProcessingContext and extracted methods pass
- ✅ TestContainers integration tests validate refactored code
- ✅ No performance regression (existing: <100ms for 100 results)
- ✅ Constitution compliance maintained (reduces complexity, follows TDD, no new dependencies)

---
**Research Complete**: 2025-11-15
**Next Phase**: Generate design artifacts (data-model.md, contracts/, quickstart.md)
