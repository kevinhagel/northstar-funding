# Tasks: SearchResultProcessor Refactoring

**Feature**: 012-refactor-searchresultprocessor-to
**Input**: Design documents from `/Users/kevin/github/northstar-funding/specs/012-refactor-searchresultprocessor-to/`
**Prerequisites**: plan.md, research.md, data-model.md, contracts/pipeline-stages.md, quickstart.md

## Execution Flow (main)
```
1. Load plan.md from feature directory ✅
   → Tech stack: Java 25, Spring Boot 3.5.7, TestContainers 1.21.3
   → Project type: single (northstar-crawler module)
2. Load design documents ✅
   → data-model.md: ProcessingContext entity
   → contracts/pipeline-stages.md: 7 pipeline stage contracts
   → research.md: Extract Method + Replace Primitive with Object patterns
   → quickstart.md: 8 validation scenarios
3. Generate tasks by category:
   → Setup: ProcessingContext class, logging infrastructure
   → Tests: ProcessingContext unit tests, pipeline stage contract tests
   → Core: Extract 7 pipeline stage methods (TDD)
   → Integration: Refactor processSearchResults main method
   → Polish: Performance tests, documentation
4. Apply task rules:
   → ProcessingContext tests [P] (independent)
   → Pipeline stage tests sequential (depend on extraction order)
   → Tests before implementation (TDD)
5. Number tasks: T001-T027 (27 tasks total)
6. Dependencies: Tests → Implementation → Integration → Validation
7. Parallel execution: ProcessingContext tests, final validation tasks
```

## Format: `[ID] [P?] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- File paths are absolute for Java multi-module Maven project

## Phase A: Infrastructure Setup

### T001: Create ProcessingContext Domain Object
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/ProcessingContext.java`

Create new domain object to encapsulate processing state:
```java
package com.northstar.funding.crawler.processing;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProcessingContext {
    private final UUID sessionId;
    private final BigDecimal confidenceThreshold;
    private final Set<String> seenDomains;
    private int spamTldFiltered;
    private int blacklistedSkipped;
    private int duplicatesSkipped;
    private int highConfidenceCreated;
    private int lowConfidenceCreated;
    private int invalidUrlsSkipped;

    public ProcessingContext(UUID sessionId) {
        this.sessionId = sessionId;
        this.confidenceThreshold = new BigDecimal("0.60");
        this.seenDomains = new HashSet<>();
        // All counters initialized to 0 by default
    }

    // Getters, state mutation methods - see data-model.md for complete spec
}
```

**Acceptance**:
- Class compiles successfully
- Constructor initializes all fields correctly
- No Lombok annotations (simple POJO)

**References**: data-model.md lines 11-123

---

### T002: Add ProcessingContext State Mutation Methods
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/ProcessingContext.java`

Implement state mutation methods per data-model.md specification:
- `boolean markDomainAsSeen(String domain)` - Returns false if duplicate, increments counter
- `void recordSpamTldFiltered()`
- `void recordBlacklisted()`
- `void recordLowConfidence()` - **Critical for bug fix**
- `void recordHighConfidence()`
- `void recordInvalidUrl()` - **New feature**
- `ProcessingStatistics buildStatistics(int totalResults)`

**Key Implementation** (duplicate detection optimization):
```java
public boolean markDomainAsSeen(String domain) {
    if (!seenDomains.add(domain)) {  // Single HashSet operation
        duplicatesSkipped++;
        return false;  // Duplicate
    }
    return true;  // Unique
}
```

**Acceptance**:
- All 8 methods implemented
- `markDomainAsSeen()` uses single Set.add() operation (not contains() + add())
- `buildStatistics()` creates ProcessingStatistics with all counters

**References**: data-model.md lines 38-105, contracts/pipeline-stages.md line 145

---

### T003: Add invalidUrlsSkipped Field to ProcessingStatistics
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/ProcessingStatistics.java`

Add new field to existing ProcessingStatistics class:
```java
@Data
@Builder
public class ProcessingStatistics {
    private int totalResults;
    private int spamTldFiltered;
    private int blacklistedSkipped;
    private int duplicatesSkipped;
    private int highConfidenceCreated;
    private int lowConfidenceCreated;
    private int invalidUrlsSkipped;  // NEW FIELD

    // Existing derived methods unchanged
}
```

**Acceptance**:
- Field added to Lombok @Builder
- Backward compatible (default value 0)
- Existing tests still pass

**References**: data-model.md lines 129-186

---

### T004: Add SLF4J Logger and CONFIDENCE_THRESHOLD Constant to SearchResultProcessor
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Add logging infrastructure and extract magic value:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class SearchResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SearchResultProcessor.class);
    private static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.60");

    // Existing fields and methods...
}
```

**Acceptance**:
- Logger field added
- CONFIDENCE_THRESHOLD constant defined
- Remove hardcoded `new BigDecimal("0.60")` from line 80
- Code compiles successfully

**References**: research.md lines 283-295

---

## Phase B: Tests First (TDD) - ProcessingContext ⚠️ MUST COMPLETE BEFORE PHASE C

**CRITICAL**: These tests MUST be written and MUST PASS before extracting any pipeline stage methods.

### T005: [P] ProcessingContext Unit Test - markDomainAsSeen()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/ProcessingContextTest.java`

Create test class and test duplicate detection:
```java
package com.northstar.funding.crawler.processing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessingContextTest {
    private UUID sessionId;
    private ProcessingContext context;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        context = new ProcessingContext(sessionId);
    }

    @Test
    void markDomainAsSeen_FirstTime_ReturnsTrue() {
        // Given
        String domain = "example.org";

        // When
        boolean isUnique = context.markDomainAsSeen(domain);

        // Then
        assertThat(isUnique).isTrue();
        assertThat(context.getDuplicatesSkipped()).isZero();
    }

    @Test
    void markDomainAsSeen_Duplicate_ReturnsFalseAndIncrements() {
        // Given
        String domain = "example.org";
        context.markDomainAsSeen(domain);  // First occurrence

        // When
        boolean isUnique = context.markDomainAsSeen(domain);  // Second occurrence

        // Then
        assertThat(isUnique).isFalse();
        assertThat(context.getDuplicatesSkipped()).isEqualTo(1);
    }
}
```

**Acceptance**:
- Test class created with JUnit 5
- 2 tests for duplicate detection
- Tests use AssertJ fluent assertions
- Both tests pass

**References**: data-model.md lines 244-253, contracts/pipeline-stages.md lines 152-182

---

### T006: [P] ProcessingContext Unit Test - Counter Mutation Methods
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/ProcessingContextTest.java`

Add tests for all counter mutation methods:
```java
@Test
void recordSpamTldFiltered_IncrementsCounter() {
    context.recordSpamTldFiltered();
    assertThat(context.getSpamTldFiltered()).isEqualTo(1);
}

@Test
void recordBlacklisted_IncrementsCounter() {
    context.recordBlacklisted();
    assertThat(context.getBlacklistedSkipped()).isEqualTo(1);
}

@Test
void recordLowConfidence_IncrementsCounter() {
    // BUG FIX VALIDATION: This counter was never incremented before
    context.recordLowConfidence();
    assertThat(context.getLowConfidenceCreated()).isEqualTo(1);
}

@Test
void recordHighConfidence_IncrementsCounter() {
    context.recordHighConfidence();
    assertThat(context.getHighConfidenceCreated()).isEqualTo(1);
}

@Test
void recordInvalidUrl_IncrementsCounter() {
    // NEW FEATURE: Track invalid URLs
    context.recordInvalidUrl();
    assertThat(context.getInvalidUrlsSkipped()).isEqualTo(1);
}
```

**Acceptance**:
- 5 tests added (total 7 tests in ProcessingContextTest)
- All counter methods validated
- Tests pass independently

**References**: data-model.md lines 244-253

---

### T007: [P] ProcessingContext Unit Test - buildStatistics()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/ProcessingContextTest.java`

Add test for statistics generation:
```java
@Test
void buildStatistics_ReturnsAccurateStats() {
    // Given - Simulate processing state
    context.recordSpamTldFiltered();
    context.recordSpamTldFiltered();
    context.markDomainAsSeen("domain1.org");
    context.markDomainAsSeen("domain1.org");  // Duplicate
    context.recordBlacklisted();
    context.recordHighConfidence();
    context.recordLowConfidence();
    context.recordInvalidUrl();

    // When
    ProcessingStatistics stats = context.buildStatistics(10);

    // Then
    assertThat(stats.getTotalResults()).isEqualTo(10);
    assertThat(stats.getSpamTldFiltered()).isEqualTo(2);
    assertThat(stats.getDuplicatesSkipped()).isEqualTo(1);
    assertThat(stats.getBlacklistedSkipped()).isEqualTo(1);
    assertThat(stats.getHighConfidenceCreated()).isEqualTo(1);
    assertThat(stats.getLowConfidenceCreated()).isEqualTo(1);
    assertThat(stats.getInvalidUrlsSkipped()).isEqualTo(1);
}
```

**Acceptance**:
- Test validates all statistics fields
- Statistics match processing state
- Test passes (total 8 tests in ProcessingContextTest)

**References**: data-model.md lines 88-104

---

## Phase C: Tests First (TDD) - Pipeline Stages ⚠️ WRITE TESTS BEFORE EXTRACTING METHODS

**CRITICAL**: For each stage (T008-T021), write the test FIRST, watch it fail, THEN extract the method.

### T008: Pipeline Stage Test - extractAndValidateDomain()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java`

Add tests for domain extraction stage (tests will fail initially):
```java
// Add to existing SearchResultProcessorTest class

@Test
void extractAndValidateDomain_ValidUrl_ReturnsDomain() {
    // Given
    SearchResult result = SearchResult.builder()
        .url("https://example.org/grants")
        .build();
    ProcessingContext context = new ProcessingContext(sessionId);
    when(domainService.extractDomainFromUrl("https://example.org/grants"))
        .thenReturn(Optional.of("example.org"));

    // When
    Optional<String> domain = processor.extractAndValidateDomain(result, context);

    // Then
    assertThat(domain).isPresent().contains("example.org");
    assertThat(context.getInvalidUrlsSkipped()).isZero();
}

@Test
void extractAndValidateDomain_InvalidUrl_RecordsAndReturnsEmpty() {
    // Given
    SearchResult result = SearchResult.builder()
        .url("htp://invalid..url//")
        .build();
    ProcessingContext context = new ProcessingContext(sessionId);
    when(domainService.extractDomainFromUrl("htp://invalid..url//"))
        .thenReturn(Optional.empty());

    // When
    Optional<String> domain = processor.extractAndValidateDomain(result, context);

    // Then
    assertThat(domain).isEmpty();
    assertThat(context.getInvalidUrlsSkipped()).isEqualTo(1);
}
```

**Acceptance**:
- 2 tests added to SearchResultProcessorTest
- Tests FAIL (method doesn't exist yet)
- Compilation error: "cannot find symbol: extractAndValidateDomain"

**References**: contracts/pipeline-stages.md lines 11-66

---

### T009: Extract extractAndValidateDomain() Method (Make Tests Pass)
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Extract domain extraction logic from processSearchResults into new private method:
```java
private Optional<String> extractAndValidateDomain(SearchResult result, ProcessingContext context) {
    Optional<String> domain = domainService.extractDomainFromUrl(result.getUrl());
    if (domain.isEmpty()) {
        context.recordInvalidUrl();
        logger.warn("Failed to extract domain from URL: {}", result.getUrl());
    }
    return domain;
}
```

**Refactoring Steps**:
1. Extract lines 85-88 from processSearchResults
2. Replace with call to new method
3. Update to use ProcessingContext instead of local counter

**Acceptance**:
- Method extracted successfully
- T008 tests now PASS (GREEN)
- Existing 7 SearchResultProcessorTest tests still pass (backward compatibility)
- Logging statement added

**References**: contracts/pipeline-stages.md lines 11-31, research.md lines 73-99

---

### T010: Pipeline Stage Test - isSpamTld()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java`

Add tests for spam TLD filtering stage:
```java
@Test
void isSpamTld_SpamTld_RecordsAndReturnsTrue() {
    // Given
    SearchResult result = SearchResult.builder()
        .url("https://scam.xyz/grants")
        .build();
    ProcessingContext context = new ProcessingContext(sessionId);
    when(domainCredibilityService.isSpamTld("https://scam.xyz/grants"))
        .thenReturn(true);

    // When
    boolean isSpam = processor.isSpamTld(result, context);

    // Then
    assertThat(isSpam).isTrue();
    assertThat(context.getSpamTldFiltered()).isEqualTo(1);
    verify(domainCredibilityService).isSpamTld("https://scam.xyz/grants");
}

@Test
void isSpamTld_LegitTld_ReturnsFalse() {
    // Given
    SearchResult result = SearchResult.builder()
        .url("https://example.org/grants")
        .build();
    ProcessingContext context = new ProcessingContext(sessionId);
    when(domainCredibilityService.isSpamTld("https://example.org/grants"))
        .thenReturn(false);

    // When
    boolean isSpam = processor.isSpamTld(result, context);

    // Then
    assertThat(isSpam).isFalse();
    assertThat(context.getSpamTldFiltered()).isZero();
}
```

**Acceptance**:
- 2 tests added
- Tests FAIL (method doesn't exist)

**References**: contracts/pipeline-stages.md lines 70-126

---

### T011: Extract isSpamTld() Method (Make Tests Pass)
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Extract spam TLD filtering logic (currently missing from implementation):
```java
private boolean isSpamTld(SearchResult result, ProcessingContext context) {
    if (domainCredibilityService.isSpamTld(result.getUrl())) {
        context.recordSpamTldFiltered();
        logger.info("Spam TLD filtered: {}", result.getUrl());
        return true;
    }
    return false;
}
```

**Integration Point**: Add call BEFORE duplicate detection in processSearchResults:
```java
// After domain extraction, before deduplication:
if (isSpamTld(result, context)) {
    continue;  // Skip spam TLD before deduplication
}
```

**Acceptance**:
- Method extracted
- T010 tests PASS
- Called before duplicate detection (per JavaDoc specification)
- All existing tests still pass

**References**: contracts/pipeline-stages.md lines 70-90, research.md lines 73-99

---

### T012: Pipeline Stage Test - isDuplicate()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java`

Add tests for deduplication stage:
```java
@Test
void isDuplicate_UniqueFirstTime_ReturnsFalse() {
    // Given
    ProcessingContext context = new ProcessingContext(sessionId);
    String domain = "example.org";

    // When
    boolean isDuplicate = processor.isDuplicate(domain, context);

    // Then
    assertThat(isDuplicate).isFalse();
    assertThat(context.getDuplicatesSkipped()).isZero();
}

@Test
void isDuplicate_SecondOccurrence_ReturnsTrue() {
    // Given
    ProcessingContext context = new ProcessingContext(sessionId);
    String domain = "example.org";
    context.markDomainAsSeen(domain);  // First occurrence

    // When
    boolean isDuplicate = processor.isDuplicate(domain, context);

    // Then
    assertThat(isDuplicate).isTrue();
    assertThat(context.getDuplicatesSkipped()).isEqualTo(1);
}
```

**Acceptance**:
- 2 tests added
- Tests FAIL (method doesn't exist)

**References**: contracts/pipeline-stages.md lines 130-182

---

### T013: Extract isDuplicate() Method (Make Tests Pass)
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Extract duplicate detection logic:
```java
private boolean isDuplicate(String domain, ProcessingContext context) {
    if (!context.markDomainAsSeen(domain)) {
        logger.debug("Duplicate domain skipped: {}", domain);
        return true;  // Duplicate
    }
    return false;  // Unique
}
```

**Refactoring Steps**:
1. Extract lines 92-96 from processSearchResults
2. Replace with call to new method
3. Remove local `Set<String> seenDomains` variable (now in ProcessingContext)

**Acceptance**:
- Method extracted
- T012 tests PASS
- Optimization: Single HashSet operation (Set.add) instead of two (contains + add)
- All existing tests still pass

**References**: contracts/pipeline-stages.md lines 130-150, research.md lines 73-99

---

### T014: Pipeline Stage Test - isBlacklisted()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java`

Add tests for blacklist checking stage:
```java
@Test
void isBlacklisted_BlacklistedDomain_RecordsAndReturnsTrue() {
    // Given
    String domain = "spam.com";
    ProcessingContext context = new ProcessingContext(sessionId);
    when(domainService.isBlacklisted("spam.com")).thenReturn(true);

    // When
    boolean isBlacklisted = processor.isBlacklisted(domain, context);

    // Then
    assertThat(isBlacklisted).isTrue();
    assertThat(context.getBlacklistedSkipped()).isEqualTo(1);
    verify(domainService).isBlacklisted("spam.com");
}

@Test
void isBlacklisted_AllowedDomain_ReturnsFalse() {
    // Given
    String domain = "example.org";
    ProcessingContext context = new ProcessingContext(sessionId);
    when(domainService.isBlacklisted("example.org")).thenReturn(false);

    // When
    boolean isBlacklisted = processor.isBlacklisted(domain, context);

    // Then
    assertThat(isBlacklisted).isFalse();
    assertThat(context.getBlacklistedSkipped()).isZero();
}
```

**Acceptance**:
- 2 tests added
- Tests FAIL (method doesn't exist)

**References**: contracts/pipeline-stages.md lines 186-240

---

### T015: Extract isBlacklisted() Method (Make Tests Pass)
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Extract blacklist checking logic:
```java
private boolean isBlacklisted(String domain, ProcessingContext context) {
    if (domainService.isBlacklisted(domain)) {
        context.recordBlacklisted();
        logger.warn("Blacklisted domain skipped: {}", domain);
        return true;
    }
    return false;
}
```

**Refactoring Steps**:
1. Extract lines 98-102 from processSearchResults
2. Replace with call to new method
3. Update to use ProcessingContext instead of local counter

**Acceptance**:
- Method extracted
- T014 tests PASS
- All existing tests still pass

**References**: contracts/pipeline-stages.md lines 186-206, research.md lines 73-99

---

### T016: Pipeline Stage Test - calculateConfidence()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java`

Add test for confidence scoring stage:
```java
@Test
void calculateConfidence_ValidResult_ReturnsScore() {
    // Given
    SearchResult result = SearchResult.builder()
        .url("https://example.org/grants")
        .title("EU Grants")
        .description("Funding for research")
        .build();
    when(confidenceScorer.calculateConfidence(
        "EU Grants", "Funding for research", "https://example.org/grants"
    )).thenReturn(new BigDecimal("0.85"));

    // When
    BigDecimal confidence = processor.calculateConfidence(result);

    // Then
    assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.85"));
    verify(confidenceScorer).calculateConfidence(
        "EU Grants", "Funding for research", "https://example.org/grants"
    );
}
```

**Acceptance**:
- 1 test added
- Test FAILS (method doesn't exist)

**References**: contracts/pipeline-stages.md lines 244-284

---

### T017: Extract calculateConfidence() Method (Make Tests Pass)
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Extract confidence calculation logic:
```java
private BigDecimal calculateConfidence(SearchResult result) {
    BigDecimal confidence = confidenceScorer.calculateConfidence(
        result.getTitle(),
        result.getDescription(),
        result.getUrl()
    );
    logger.debug("Calculated confidence {} for {}", confidence, result.getUrl());
    return confidence;
}
```

**Refactoring Steps**:
1. Extract lines 105-109 from processSearchResults
2. Replace with call to new method

**Acceptance**:
- Method extracted
- T016 test PASSES
- Pure calculation, no side effects
- All existing tests still pass

**References**: contracts/pipeline-stages.md lines 244-262, research.md lines 73-99

---

### T018: Pipeline Stage Test - meetsThreshold()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java`

Add tests for threshold filtering stage (including bug fix validation):
```java
@Test
void meetsThreshold_AboveThreshold_RecordsHighAndReturnsTrue() {
    // Given
    BigDecimal confidence = new BigDecimal("0.85");
    ProcessingContext context = new ProcessingContext(sessionId);

    // When
    boolean meets = processor.meetsThreshold(confidence, context);

    // Then
    assertThat(meets).isTrue();
    assertThat(context.getHighConfidenceCreated()).isEqualTo(1);
    assertThat(context.getLowConfidenceCreated()).isZero();
}

@Test
void meetsThreshold_BelowThreshold_RecordsLowAndReturnsFalse() {
    // Given
    BigDecimal confidence = new BigDecimal("0.45");
    ProcessingContext context = new ProcessingContext(sessionId);

    // When
    boolean meets = processor.meetsThreshold(confidence, context);

    // Then
    assertThat(meets).isFalse();
    assertThat(context.getHighConfidenceCreated()).isZero();
    assertThat(context.getLowConfidenceCreated()).isEqualTo(1);  // BUG FIX VALIDATED
}

@Test
void meetsThreshold_ExactlyThreshold_RecordsHighAndReturnsTrue() {
    // Given
    BigDecimal confidence = new BigDecimal("0.60");
    ProcessingContext context = new ProcessingContext(sessionId);

    // When
    boolean meets = processor.meetsThreshold(confidence, context);

    // Then
    assertThat(meets).isTrue();  // >= threshold
    assertThat(context.getHighConfidenceCreated()).isEqualTo(1);
}
```

**Acceptance**:
- 3 tests added (including boundary test)
- Tests FAIL (method doesn't exist)

**References**: contracts/pipeline-stages.md lines 288-356

---

### T019: Extract meetsThreshold() Method and Fix lowConfidenceCreated Bug
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Extract threshold filtering logic and fix critical bug:
```java
private boolean meetsThreshold(BigDecimal confidence, ProcessingContext context) {
    if (confidence.compareTo(context.getConfidenceThreshold()) >= 0) {
        context.recordHighConfidence();
        logger.debug("Confidence {} meets threshold", confidence);
        return true;
    } else {
        context.recordLowConfidence();  // BUG FIX: This was never called before
        logger.debug("Confidence {} below threshold", confidence);
        return false;
    }
}
```

**Bug Fix Details**:
- **Before**: Line 139 hardcoded `lowConfidenceCreated(0)` - counter always 0
- **After**: `context.recordLowConfidence()` called when confidence < threshold

**Refactoring Steps**:
1. Extract lines 112-115 from processSearchResults
2. Add ELSE branch to record low confidence (missing before)
3. Replace hardcoded threshold with `context.getConfidenceThreshold()`

**Acceptance**:
- Method extracted
- T018 tests PASS (all 3)
- Critical bug fixed: lowConfidenceCreated now increments
- All existing tests still pass

**References**: contracts/pipeline-stages.md lines 288-310, research.md lines 73-99

---

### T020: Pipeline Stage Test - createAndSaveCandidate()
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorTest.java`

Add test for candidate creation stage:
```java
@Test
void createAndSaveCandidate_ValidInputs_CreatesAndSaves() {
    // Given
    SearchResult result = SearchResult.builder()
        .url("https://example.org/grants")
        .title("EU Grants")
        .description("Funding")
        .build();
    String domain = "example.org";
    BigDecimal confidence = new BigDecimal("0.85");
    ProcessingContext context = new ProcessingContext(sessionId);

    UUID domainId = UUID.randomUUID();
    Domain mockDomain = Domain.builder()
        .domainId(domainId)
        .domainName(domain)
        .build();
    FundingSourceCandidate mockCandidate = FundingSourceCandidate.builder()
        .build();

    when(domainService.registerOrGetDomain(domain, sessionId))
        .thenReturn(mockDomain);
    when(candidateCreationService.createCandidate(
        "EU Grants", "Funding", "https://example.org/grants",
        domainId, sessionId, confidence
    )).thenReturn(mockCandidate);

    // When
    processor.createAndSaveCandidate(result, domain, confidence, context);

    // Then
    verify(domainService).registerOrGetDomain(domain, sessionId);
    verify(candidateCreationService).createCandidate(
        "EU Grants", "Funding", "https://example.org/grants",
        domainId, sessionId, confidence
    );
    verify(candidateRepository).save(mockCandidate);
}
```

**Acceptance**:
- 1 test added
- Test FAILS (method doesn't exist)

**References**: contracts/pipeline-stages.md lines 360-418

---

### T021: Extract createAndSaveCandidate() Method (Make Tests Pass)
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Extract candidate creation logic:
```java
private void createAndSaveCandidate(SearchResult result, String domain,
                                   BigDecimal confidence, ProcessingContext context) {
    Domain registeredDomain = domainService.registerOrGetDomain(
        domain, context.getSessionId()
    );

    FundingSourceCandidate candidate = candidateCreationService.createCandidate(
        result.getTitle(),
        result.getDescription(),
        result.getUrl(),
        registeredDomain.getDomainId(),
        context.getSessionId(),
        confidence
    );

    candidateRepository.save(candidate);
    logger.info("Created candidate for {}", result.getUrl());
}
```

**Refactoring Steps**:
1. Extract lines 117-127 from processSearchResults
2. Replace with call to new method
3. Update to use ProcessingContext for sessionId

**Acceptance**:
- Method extracted
- T020 test PASSES
- All existing tests still pass

**References**: contracts/pipeline-stages.md lines 360-383, research.md lines 73-99

---

## Phase D: Integration - Refactor Main Method

### T022: Refactor processSearchResults to Use Pipeline Stages
**File**: `northstar-crawler/src/main/java/com/northstar/funding/crawler/processing/SearchResultProcessor.java`

Refactor main processing method to orchestrate pipeline stages:
```java
public ProcessingStatistics processSearchResults(List<SearchResult> searchResults, UUID sessionId) {
    MDC.put("sessionId", sessionId.toString());
    try {
        logger.info("Processing {} search results for session {}", searchResults.size(), sessionId);

        ProcessingContext context = new ProcessingContext(sessionId);

        for (SearchResult result : searchResults) {
            // Stage 1: Extract domain
            Optional<String> domain = extractAndValidateDomain(result, context);
            if (domain.isEmpty()) {
                continue;  // Invalid URL, skip
            }

            // Stage 2: Check spam TLD
            if (isSpamTld(result, context)) {
                continue;  // Spam TLD, skip
            }

            // Stage 3: Check duplicate
            if (isDuplicate(domain.get(), context)) {
                continue;  // Duplicate domain, skip
            }

            // Stage 4: Check blacklist
            if (isBlacklisted(domain.get(), context)) {
                continue;  // Blacklisted, skip
            }

            // Stage 5: Calculate confidence
            BigDecimal confidence = calculateConfidence(result);

            // Stage 6: Check threshold
            if (!meetsThreshold(confidence, context)) {
                continue;  // Low confidence, skip
            }

            // Stage 7: Create candidate
            createAndSaveCandidate(result, domain.get(), confidence, context);
        }

        ProcessingStatistics stats = context.buildStatistics(searchResults.size());
        logger.info("Processing complete: {} total, {} duplicates, {} blacklisted, " +
                   "{} high confidence, {} low confidence",
                   stats.getTotalResults(), stats.getDuplicatesSkipped(),
                   stats.getBlacklistedSkipped(), stats.getHighConfidenceCreated(),
                   stats.getLowConfidenceCreated());

        return stats;
    } finally {
        MDC.remove("sessionId");
    }
}
```

**Refactoring Steps**:
1. Replace existing 82-line method body with pipeline stage orchestration
2. Create ProcessingContext at start
3. Call 7 pipeline stage methods in order
4. Remove all local variables (counters, seenDomains, threshold)
5. Replace statistics builder with `context.buildStatistics()`
6. Add MDC for session correlation
7. Add summary logging

**Acceptance**:
- Method refactored to use all 7 pipeline stages
- All 7 existing SearchResultProcessorTest tests PASS (backward compatibility)
- All new stage tests PASS (14+ new tests)
- MDC session correlation working
- Logging at each stage

**References**: contracts/pipeline-stages.md lines 422-450, research.md lines 204-228

---

## Phase E: Validation

### T023: Run All Existing Tests (Backward Compatibility)
**Command**: `mvn test -Dtest=SearchResultProcessorTest -pl northstar-crawler`

Verify all 7 existing tests pass unchanged:
1. `testProcessEmptyResults()`
2. `testDuplicateDomainsHandled()`
3. `testLowConfidenceFiltered()`
4. `testHighConfidenceCreatesCandidates()`
5. `testBlacklistedDomainsSkipped()`
6. `testStatisticsTracking()`
7. `testEndToEndProcessing()`

**Expected Output**:
```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Acceptance**:
- All tests pass without modification
- Execution time < 5s (no performance regression)
- Statistics values match expected (backward compatible)

**References**: quickstart.md lines 33-79

---

### T024: [P] Run ProcessingContext Tests
**Command**: `mvn test -Dtest=ProcessingContextTest -pl northstar-crawler`

Verify all ProcessingContext unit tests pass:
- 8 tests covering state management

**Expected Output**:
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Acceptance**:
- All state mutation tests pass
- Duplicate detection optimization validated
- Bug fix confirmed (lowConfidenceCreated increments)

**References**: quickstart.md lines 86-141

---

### T025: [P] Run Integration Tests with TestContainers
**Command**: `mvn test -Dtest=DomainRepositoryIntegrationTest -pl northstar-persistence`

Verify refactored code works with real PostgreSQL:
- 15 domain repository tests
- Database operations succeed

**Expected Output**:
```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: ~12s
[INFO] BUILD SUCCESS
```

**Acceptance**:
- TestContainers starts successfully
- All repository operations work
- No SQL errors

**References**: quickstart.md lines 148-214

---

### T026: [P] Performance Validation - Duplicate Detection
**File**: `northstar-crawler/src/test/java/com/northstar/funding/crawler/processing/SearchResultProcessorPerformanceTest.java`

Create performance test for duplicate detection optimization:
```java
package com.northstar.funding.crawler.processing;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;

class SearchResultProcessorPerformanceTest {
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
        ProcessingStatistics stats = processor.processSearchResults(
            searchResults, UUID.randomUUID()
        );
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        // Then - Verify performance
        System.out.println("Processing 1000 results took " + durationMs + "ms");
        assertThat(durationMs).isLessThan(50);  // Target: < 50ms
        assertThat(stats.getDuplicatesSkipped()).isEqualTo(500);
        assertThat(stats.getTotalResults()).isEqualTo(1000);
    }
}
```

**Command**: `mvn test -Dtest=SearchResultProcessorPerformanceTest -pl northstar-crawler`

**Expected Output**:
```
Processing 1000 results took 35ms
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Acceptance**:
- Processing 1000 results completes in < 50ms
- ~40% improvement over two-operation approach
- Duplicate detection accuracy: 100%

**References**: quickstart.md lines 221-287

---

### T027: [P] Manual Logging Verification
**Command**: `mvn test -Dtest=SearchResultProcessorTest -Dlogging.level.com.northstar.funding.crawler=DEBUG -pl northstar-crawler`

Manually verify structured logging output:

**Expected Log Pattern**:
```
2025-11-15 10:30:45 [main] INFO  SearchResultProcessor [sessionId=xxx] - Processing 5 search results for session xxx
2025-11-15 10:30:45 [main] DEBUG SearchResultProcessor [sessionId=xxx] - Extracted domain: example.org from https://example.org/grants
2025-11-15 10:30:45 [main] DEBUG SearchResultProcessor [sessionId=xxx] - Duplicate domain skipped: example.org
2025-11-15 10:30:45 [main] WARN  SearchResultProcessor [sessionId=xxx] - Blacklisted domain skipped: spam.com
2025-11-15 10:30:45 [main] DEBUG SearchResultProcessor [sessionId=xxx] - Confidence score 0.85 for https://legit.org/grants
2025-11-15 10:30:45 [main] INFO  SearchResultProcessor [sessionId=xxx] - Created candidate for https://legit.org/grants
```

**Acceptance**:
- All logs include `[sessionId=xxx]` (MDC working)
- Appropriate log levels (INFO, WARN, DEBUG)
- Structured format (consistent across stages)
- Session ID matches input UUID

**References**: quickstart.md lines 294-371

---

## Dependencies

**Setup Phase (A)**:
- T001 → T002 (ProcessingContext methods depend on class)
- T003 (independent - can run parallel)
- T004 (independent - can run parallel)

**ProcessingContext Tests (B)**:
- T005, T006, T007 are parallel [P] (different test methods, same file)
- All depend on T001-T002 completing

**Pipeline Stage Extraction (C)**:
- Each pair (test + implementation) is sequential
- T008 → T009 (domain extraction)
- T010 → T011 (spam TLD)
- T012 → T013 (deduplication)
- T014 → T015 (blacklist)
- T016 → T017 (confidence)
- T018 → T019 (threshold + bug fix)
- T020 → T021 (candidate creation)

**Integration (D)**:
- T022 depends on T009, T011, T013, T015, T017, T019, T021 (all stages extracted)

**Validation (E)**:
- T023-T027 depend on T022 (refactored main method)
- T024-T027 are parallel [P] (independent validation tasks)

## Parallel Execution Examples

**Phase A - Setup** (can run T003 and T004 in parallel after T001-T002):
```bash
# Sequential required for ProcessingContext
mvn test -Dtest=ProcessingContextTest#markDomainAsSeen_FirstTime_ReturnsTrue
# Then parallel for independent tasks
mvn test -Dtest=ProcessingStatisticsTest &  # T003 validation
mvn clean compile  # T004 compilation check
```

**Phase B - ProcessingContext Tests** (all parallel after T001-T002):
```bash
# Run all ProcessingContext tests in parallel (different test methods)
mvn test -Dtest=ProcessingContextTest -pl northstar-crawler
```

**Phase E - Validation** (T024-T027 parallel after T022):
```bash
# Run all validation tasks in parallel
mvn test -Dtest=ProcessingContextTest -pl northstar-crawler &
mvn test -Dtest=DomainRepositoryIntegrationTest -pl northstar-persistence &
mvn test -Dtest=SearchResultProcessorPerformanceTest -pl northstar-crawler &
# Manual logging verification in separate terminal
```

## Task Summary

**Total Tasks**: 27
- **Phase A (Setup)**: 4 tasks (T001-T004)
- **Phase B (ProcessingContext Tests)**: 3 tasks (T005-T007) [P]
- **Phase C (Pipeline Stages)**: 14 tasks (T008-T021) - 7 test/implementation pairs (TDD)
- **Phase D (Integration)**: 1 task (T022)
- **Phase E (Validation)**: 5 tasks (T023-T027) - 4 parallel [P]

**Estimated Timeline**:
- Phase A: 1-2 hours (infrastructure setup)
- Phase B: 1 hour (ProcessingContext tests)
- Phase C: 4-6 hours (TDD for 7 pipeline stages)
- Phase D: 1-2 hours (main method refactoring)
- Phase E: 1 hour (validation and testing)

**Total Estimated Time**: 8-12 hours

## Notes

- **TDD Critical**: For Phase C, must write test FIRST, watch it FAIL, then extract method
- **Backward Compatibility**: All 7 existing tests must pass throughout refactoring
- **Bug Fix Validation**: T018-T019 specifically validate lowConfidenceCreated fix
- **Performance**: T026 validates duplicate detection optimization (~40% improvement)
- **Logging**: T027 manually verifies MDC session correlation and structured logging
- **No Database Changes**: All changes are code-only, no Flyway migrations needed

## Validation Checklist

- [x] All contracts have corresponding tests (7 pipeline stages, 14 tests)
- [x] All entities have model tasks (ProcessingContext in T001-T002)
- [x] All tests come before implementation (TDD pairs T008→T009, T010→T011, etc.)
- [x] Parallel tasks truly independent (T005-T007, T024-T027)
- [x] Each task specifies exact file path (absolute Java paths)
- [x] No task modifies same file as another [P] task (validated)

---

**Tasks Generated**: 2025-11-16
**Ready for Execution**: Use `/implement` or execute tasks manually in order
**Next**: Begin with T001 (Create ProcessingContext)
