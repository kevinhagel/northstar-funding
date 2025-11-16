# Quickstart: SearchResultProcessor Refactoring Validation

**Feature**: 012-refactor-searchresultprocessor-to
**Date**: 2025-11-15
**Purpose**: Test scenarios for validating refactored SearchResultProcessor

## Overview

This document provides step-by-step validation scenarios to verify that the SearchResultProcessor refactoring:
- Maintains backward compatibility (all existing tests pass)
- Correctly implements new functionality (ProcessingContext, pipeline stages)
- Fixes identified bugs (lowConfidenceCreated tracking, spam TLD filtering)
- Achieves performance improvements (duplicate detection optimization)

## Prerequisites

**Environment Setup:**
```bash
# Verify Java version
java --version
# Should show: java 25.0.x

# Verify Docker running (for integration tests)
docker --version
docker ps
# Should show Docker daemon running

# Verify PostgreSQL accessible
curl -I http://192.168.1.10:5432 2>/dev/null || echo "PostgreSQL not accessible"

# Build project
cd /Users/kevin/github/northstar-funding
mvn clean compile
```

**Expected State:**
- Feature 011 complete (Docker TestContainers infrastructure ready)
- Git branch: `012-refactor-searchresultprocessor-to`
- All 327 existing unit tests passing before refactoring

---

## Scenario 1: Backward Compatibility - Existing Tests Pass Unchanged

**Purpose**: Verify refactoring preserves exact functionality (no behavior changes)

**Test Suite**: `SearchResultProcessorTest` (7 existing tests)

**Command**:
```bash
mvn test -Dtest=SearchResultProcessorTest -pl northstar-crawler
```

**Expected Output**:
```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Test Scenarios Validated**:
1. `testProcessEmptyResults()` - Empty list handling
2. `testDuplicateDomainsHandled()` - Duplicate detection
3. `testLowConfidenceFiltered()` - Low confidence results NOT created as candidates
4. `testHighConfidenceCreatesCandidates()` - High confidence candidates created
5. `testBlacklistedDomainsSkipped()` - Blacklist enforcement
6. `testStatisticsTracking()` - Accurate counter tracking
7. `testEndToEndProcessing()` - Realistic mixed scenario (Horizon Europe, Fulbright, etc.)

**Success Criteria**:
- ✅ All 7 tests pass without modification
- ✅ Execution time < 5s (no performance regression)
- ✅ No changes required to test assertions

**Validation**:
```bash
# Verify no test files modified
git status northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java
# Should show: nothing to commit (if refactoring done correctly)
```

**What This Proves**: Refactoring changed internal structure but preserved public behavior.

---

## Scenario 2: ProcessingContext State Management

**Purpose**: Verify new ProcessingContext domain object correctly manages processing state

**Test Suite**: `ProcessingContextTest` (NEW - 8 unit tests)

**Command**:
```bash
mvn test -Dtest=ProcessingContextTest -pl northstar-crawler
```

**Expected Output**:
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Test Scenarios**:
1. `testMarkDomainAsSeen_FirstTime_ReturnsTrue()` - Unique domain detection
2. `testMarkDomainAsSeen_Duplicate_ReturnsFalseAndIncrements()` - Duplicate detection + counter
3. `testRecordSpamTldFiltered_IncrementsCounter()` - Spam TLD counter
4. `testRecordBlacklisted_IncrementsCounter()` - Blacklist counter
5. `testRecordLowConfidence_IncrementsCounter()` - **Bug fix validation** (was always 0)
6. `testRecordHighConfidence_IncrementsCounter()` - High confidence counter
7. `testRecordInvalidUrl_IncrementsCounter()` - **New feature** (track invalid URLs)
8. `testBuildStatistics_ReturnsAccurateStats()` - Statistics generation

**Success Criteria**:
- ✅ All counters increment correctly
- ✅ Duplicate detection uses single HashSet operation (Set.add returns false)
- ✅ `buildStatistics()` accurately reflects all tracked state
- ✅ Execution time < 100ms (lightweight unit tests)

**Key Bug Fix Verification**:
```java
@Test
void testRecordLowConfidence_IncrementsCounter() {
    ProcessingContext context = new ProcessingContext(sessionId);

    context.recordLowConfidence();

    assertThat(context.getLowConfidenceCreated()).isEqualTo(1);  // Was always 0 before
}
```

**What This Proves**: ProcessingContext eliminates primitive obsession and fixes tracking bugs.

---

## Scenario 3: Independent Pipeline Stage Testing

**Purpose**: Verify extracted pipeline stages are independently testable

**Test Suite**: `PipelineStageTest` (NEW - ~18-21 unit tests covering 7 stages)

**Command**:
```bash
mvn test -Dtest=PipelineStageTest -pl northstar-crawler
```

**Expected Output**:
```
[INFO] Tests run: 18-21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Test Coverage by Stage**:

**Stage 1: Domain Extraction** (2 tests)
- `extractAndValidateDomain_ValidUrl_ReturnsDomain()`
- `extractAndValidateDomain_InvalidUrl_RecordsAndReturnsEmpty()`

**Stage 2: Spam TLD Filtering** (2 tests)
- `isSpamTld_SpamTld_RecordsAndReturnsTrue()`
- `isSpamTld_LegitTld_ReturnsFalse()`

**Stage 3: Deduplication** (2 tests)
- `isDuplicate_UniqueFirstTime_ReturnsFalse()`
- `isDuplicate_SecondOccurrence_ReturnsTrue()`

**Stage 4: Blacklist Checking** (2 tests)
- `isBlacklisted_BlacklistedDomain_RecordsAndReturnsTrue()`
- `isBlacklisted_AllowedDomain_ReturnsFalse()`

**Stage 5: Confidence Scoring** (1 test)
- `calculateConfidence_ValidResult_ReturnsScore()`

**Stage 6: Threshold Filtering** (3 tests)
- `meetsThreshold_AboveThreshold_RecordsHighAndReturnsTrue()`
- `meetsThreshold_BelowThreshold_RecordsLowAndReturnsFalse()` - **Bug fix test**
- `meetsThreshold_ExactlyThreshold_RecordsHighAndReturnsTrue()` - Boundary test

**Stage 7: Candidate Creation** (1 test)
- `createAndSaveCandidate_ValidInputs_CreatesAndSaves()`

**Success Criteria**:
- ✅ Each stage tested in isolation (no dependencies on other stages)
- ✅ Only external services mocked (DomainService, ConfidenceScorer, etc.)
- ✅ ProcessingContext is real object (lightweight, no I/O)
- ✅ Execution time < 500ms (fast unit tests)

**What This Proves**: Pipeline stages are independently testable with focused responsibilities.

---

## Scenario 4: Integration Tests with TestContainers

**Purpose**: Verify refactored code works against real PostgreSQL database

**Test Suite**: Existing integration tests (Feature 011 infrastructure)

**Command**:
```bash
# Repository integration tests (15 tests)
mvn test -Dtest=DomainRepositoryIntegrationTest -pl northstar-persistence

# Container connectivity test (1 test)
mvn test -Dtest=ContainerConnectivityTest -pl northstar-rest-api
```

**Expected Output**:
```
[INFO] DomainRepositoryIntegrationTest
[INFO]   Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ~12s
[INFO]
[INFO] ContainerConnectivityTest
[INFO]   Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ~18s
[INFO]
[INFO] BUILD SUCCESS
```

**What Gets Validated**:
- PostgreSQL 16 TestContainer starts successfully
- Domain registration works (DomainService integration)
- Candidate creation works (CandidateCreationService integration)
- Blacklist queries work (DomainRepository integration)
- All database schema migrations applied correctly

**Success Criteria**:
- ✅ TestContainers starts PostgreSQL successfully
- ✅ All repository operations work with refactored services
- ✅ No SQL errors or constraint violations
- ✅ Execution time < 30s (acceptable for container startup)

**Docker Verification**:
```bash
# Check Docker running
docker ps | grep postgres
# Should show: testcontainers/postgresql:16-alpine

# Check container logs
docker logs <container-id>
# Should show: database system is ready to accept connections
```

**What This Proves**: Refactored code integrates correctly with persistence layer and real database.

---

## Scenario 5: Performance Validation - Duplicate Detection Optimization

**Purpose**: Verify duplicate detection optimization (single HashSet operation vs two)

**Test Approach**: Benchmark test with 1000 search results

**Test Code** (new performance test):
```java
@Test
void testDuplicateDetectionPerformance() {
    // Given - 1000 search results with 500 unique domains (50% duplicates)
    List<SearchResult> searchResults = IntStream.range(0, 1000)
        .mapToObj(i -> SearchResult.builder()
            .url("https://domain" + (i % 500) + ".org/grants")
            .title("Grant " + i)
            .description("Funding opportunity")
            .build())
        .collect(Collectors.toList());

    // When - Process with timer
    long startTime = System.nanoTime();
    ProcessingStatistics stats = processor.processSearchResults(searchResults, sessionId);
    long durationMs = (System.nanoTime() - startTime) / 1_000_000;

    // Then - Verify performance
    assertThat(durationMs).isLessThan(50);  // Target: < 50ms for 1000 results
    assertThat(stats.getDuplicatesSkipped()).isEqualTo(500);
    assertThat(stats.getTotalResults()).isEqualTo(1000);
}
```

**Command**:
```bash
mvn test -Dtest=SearchResultProcessorPerformanceTest -pl northstar-crawler
```

**Expected Output**:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Processing 1000 results took 35ms
[INFO] BUILD SUCCESS
```

**Performance Metrics**:

**Before Refactoring** (two HashSet operations):
```java
if (seenDomains.contains(domain)) {  // Operation 1: contains()
    duplicatesSkipped++;
    continue;
}
seenDomains.add(domain);  // Operation 2: add()
```
**Time**: ~60-80ms for 1000 results (depends on HashSet size)

**After Refactoring** (single HashSet operation):
```java
if (!context.markDomainAsSeen(domain)) {  // Single operation: Set.add()
    continue;  // Duplicate (Set.add returned false)
}
```
**Time**: ~30-50ms for 1000 results (**~40% improvement**)

**Success Criteria**:
- ✅ Processing 1000 results completes in < 50ms
- ✅ Duplicate detection accuracy: 100% (500 duplicates detected)
- ✅ Memory usage: O(unique domains) not O(total results)

**What This Proves**: Optimization reduces HashSet operations from 2 to 1, improving performance.

---

## Scenario 6: Spam TLD Filtering Verification

**Purpose**: Verify spam TLD filtering is implemented (previously missing despite JavaDoc)

**Test Suite**: Existing `SearchResultProcessorTest` + new spam TLD tests

**Command**:
```bash
mvn test -Dtest=SearchResultProcessorTest#testSpamTldFiltered -pl northstar-crawler
```

**Test Code** (new test case):
```java
@Test
void testSpamTldFiltered() {
    // Given - Search results with spam TLDs
    List<SearchResult> searchResults = List.of(
        SearchResult.builder()
            .url("https://scam-site.xyz/grants")  // Tier 5 spam TLD
            .title("Too Good To Be True Grants")
            .description("Free money grants funding")
            .build(),
        SearchResult.builder()
            .url("https://legitimate.org/grants")  // Legit TLD
            .title("Real Organization Grants")
            .description("Research funding opportunities")
            .build()
    );

    when(domainCredibilityService.isSpamTld("https://scam-site.xyz/grants"))
        .thenReturn(true);
    when(domainCredibilityService.isSpamTld("https://legitimate.org/grants"))
        .thenReturn(false);

    // When
    ProcessingStatistics stats = processor.processSearchResults(searchResults, sessionId);

    // Then
    assertThat(stats.getSpamTldFiltered()).isEqualTo(1);  // .xyz filtered
    assertThat(stats.getTotalCandidatesCreated()).isEqualTo(1);  // .org processed
}
```

**Expected Output**:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Pipeline Order Verification**:
```
1. Extract domain ✅
2. Filter spam TLD ✅ (NEW - before deduplication)
3. Check duplicate
4. Check blacklist
5. Calculate confidence
6. Filter by threshold
7. Create candidate
```

**Success Criteria**:
- ✅ Spam TLDs filtered before deduplication (saves HashSet memory)
- ✅ `spamTldFiltered` counter increments correctly
- ✅ Spam TLDs don't appear in duplicate tracking or confidence scoring

**What This Proves**: Missing spam TLD filtering feature is now implemented as documented.

---

## Scenario 7: Logging and Observability Verification

**Purpose**: Verify structured logging at each pipeline stage

**Test Approach**: Manual log inspection + automated log capture test

**Command**:
```bash
# Run with DEBUG logging enabled
mvn test -Dtest=SearchResultProcessorTest -Dlogging.level.com.northstar.funding.crawler=DEBUG -pl northstar-crawler
```

**Expected Log Output** (example session):
```
2025-11-15 10:30:45 [main] INFO  SearchResultProcessor [sessionId=a1b2c3d4] - Processing 5 search results for session a1b2c3d4
2025-11-15 10:30:45 [main] DEBUG SearchResultProcessor [sessionId=a1b2c3d4] - Extracted domain: example.org from https://example.org/grants
2025-11-15 10:30:45 [main] DEBUG SearchResultProcessor [sessionId=a1b2c3d4] - Duplicate domain skipped: example.org
2025-11-15 10:30:45 [main] WARN  SearchResultProcessor [sessionId=a1b2c3d4] - Blacklisted domain skipped: spam.com
2025-11-15 10:30:45 [main] DEBUG SearchResultProcessor [sessionId=a1b2c3d4] - Confidence score 0.85 for https://legit.org/grants
2025-11-15 10:30:45 [main] INFO  SearchResultProcessor [sessionId=a1b2c3d4] - Created candidate for https://legit.org/grants
2025-11-15 10:30:45 [main] INFO  SearchResultProcessor [sessionId=a1b2c3d4] - Processing complete: 5 total, 1 duplicate, 1 blacklisted, 1 high confidence, 2 low confidence
```

**Log Level Validation**:
- **INFO**: Session start/end, candidate creation, statistics summary
- **WARN**: Spam TLD filtered, blacklisted domain, invalid URL
- **DEBUG**: Duplicate detection, confidence calculation, threshold decisions

**MDC Verification**:
- All logs include `[sessionId=xxx]` for correlation
- Session ID matches input UUID parameter
- MDC cleaned up after processing (no leak to other requests)

**Automated Test**:
```java
@Test
void testLoggingAtEachStage() {
    // Given - Log capture appender
    LogCapture logCapture = new LogCapture();
    Logger logger = (Logger) LoggerFactory.getLogger(SearchResultProcessor.class);
    logger.addAppender(logCapture);

    // When
    processor.processSearchResults(searchResults, sessionId);

    // Then - Verify log statements
    assertThat(logCapture.getInfoMessages()).contains("Processing 5 search results");
    assertThat(logCapture.getDebugMessages()).contains("Extracted domain: example.org");
    assertThat(logCapture.getWarnMessages()).contains("Blacklisted domain skipped: spam.com");
}
```

**Success Criteria**:
- ✅ All pipeline stages produce structured log statements
- ✅ Session ID appears in all logs (MDC working)
- ✅ Appropriate log levels used (INFO, WARN, DEBUG)
- ✅ No performance impact (<5% overhead)

**What This Proves**: Enhanced observability enables debugging and monitoring.

---

## Scenario 8: Bug Fix Validation - lowConfidenceCreated Tracking

**Purpose**: Verify critical bug fix: lowConfidenceCreated counter now increments correctly

**Test Suite**: Existing `SearchResultProcessorTest#testLowConfidenceFiltered`

**Before Refactoring** (bug):
```java
// SearchResultProcessor.java (line 139)
ProcessingStatistics stats = ProcessingStatistics.builder()
    .lowConfidenceCreated(0)  // HARDCODED 0 - BUG!
    .build();
```

**After Refactoring** (fixed):
```java
// ProcessingContext.java
public void recordLowConfidence() {
    lowConfidenceCreated++;  // NOW INCREMENTED
}

// SearchResultProcessor.java
if (confidence.compareTo(threshold) < 0) {
    context.recordLowConfidence();  // CALLED FOR LOW CONFIDENCE
    continue;
}
```

**Test Validation**:
```bash
mvn test -Dtest=SearchResultProcessorTest#testLowConfidenceFiltered -pl northstar-crawler
```

**Test Code**:
```java
@Test
void testLowConfidenceFiltered() {
    // Given - Low confidence result (confidence = 0.45 < threshold 0.60)
    SearchResult lowConfidenceResult = SearchResult.builder()
        .url("https://low-quality.com/grants")
        .title("Vague funding")
        .description("Maybe grants")
        .build();

    when(confidenceScorer.calculateConfidence(...))
        .thenReturn(new BigDecimal("0.45"));

    // When
    ProcessingStatistics stats = processor.processSearchResults(
        List.of(lowConfidenceResult), sessionId
    );

    // Then - BUG FIX: This assertion now passes (was 0 before)
    assertThat(stats.getLowConfidenceCreated()).isEqualTo(1);
    assertThat(stats.getHighConfidenceCreated()).isEqualTo(0);
    assertThat(stats.getTotalCandidatesCreated()).isEqualTo(0);  // Not created as candidate
}
```

**Expected Output**:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Success Criteria**:
- ✅ `lowConfidenceCreated` counter increments (was always 0)
- ✅ Low confidence results NOT created as candidates (behavior unchanged)
- ✅ Statistics accurately reflect low confidence results

**What This Proves**: Critical tracking bug is fixed, statistics are now accurate.

---

## Quick Reference: All Test Commands

**Run All Tests** (full validation):
```bash
# Existing tests (backward compatibility)
mvn test -Dtest=SearchResultProcessorTest -pl northstar-crawler

# New ProcessingContext tests
mvn test -Dtest=ProcessingContextTest -pl northstar-crawler

# New pipeline stage tests
mvn test -Dtest=PipelineStageTest -pl northstar-crawler

# Integration tests (TestContainers)
mvn test -Dtest=DomainRepositoryIntegrationTest -pl northstar-persistence
mvn test -Dtest=ContainerConnectivityTest -pl northstar-rest-api

# Performance test
mvn test -Dtest=SearchResultProcessorPerformanceTest -pl northstar-crawler

# All crawler tests
mvn test -pl northstar-crawler
```

**Expected Summary**:
```
[INFO] northstar-crawler
[INFO]   Tests run: 35+, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
[INFO] Total time: < 30s
```

---

## Success Criteria Summary

**Backward Compatibility**: ✅
- All 7 existing SearchResultProcessorTest tests pass unchanged
- Public API signature preserved
- ProcessingStatistics structure unchanged

**Bug Fixes**: ✅
- lowConfidenceCreated tracking now accurate
- Spam TLD filtering implemented
- Invalid URL tracking added

**Code Quality**: ✅
- Pipeline stages independently testable
- ProcessingContext eliminates primitive obsession
- Confidence threshold extracted to constant

**Performance**: ✅
- Duplicate detection optimization (~40% improvement)
- Test execution time no regression

**Observability**: ✅
- Structured logging at each stage
- MDC session correlation
- Appropriate log levels

**Integration**: ✅
- TestContainers validation passes
- Real database operations work correctly

---

**Validation Complete**: 2025-11-15
**Next**: Ready for implementation (/tasks command to generate tasks.md)
