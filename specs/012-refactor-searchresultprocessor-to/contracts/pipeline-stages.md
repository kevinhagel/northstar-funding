# Pipeline Stage Contracts

**Feature**: 012-refactor-searchresultprocessor-to
**Date**: 2025-11-15
**Purpose**: Define contracts for each extracted pipeline stage method

## Overview

The refactored SearchResultProcessor extracts processing logic into 7 independent pipeline stages, each with a clear contract (inputs, outputs, side effects, logging).

## Stage 1: Domain Extraction

**Method**: `private Optional<String> extractAndValidateDomain(SearchResult result, ProcessingContext context)`

**Purpose**: Extract domain from search result URL, track failures

**Input**:
- `result`: SearchResult with URL
- `context`: ProcessingContext for failure tracking

**Output**:
- `Optional<String>` - Present if domain extracted successfully, Empty if extraction failed

**Decision Logic**:
- Call `domainService.extractDomainFromUrl(result.getUrl())`
- If Optional.empty(): Record invalid URL, return empty
- If Optional.present(): Return domain

**Side Effects**:
- If extraction fails: `context.recordInvalidUrl()`
- Logging: `logger.warn("Failed to extract domain from URL: {}", result.getUrl())`

**Contract Test**:
```java
@Test
void extractAndValidateDomain_ValidUrl_ReturnsDomain() {
    // Given
    SearchResult result = SearchResult.builder().url("https://example.org/grants").build();
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
    SearchResult result = SearchResult.builder().url("htp://invalid..url//").build();
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

---

## Stage 2: Spam TLD Filtering

**Method**: `private boolean isSpamTld(SearchResult result, ProcessingContext context)`

**Purpose**: Filter spam TLDs before deduplication (as documented in JavaDoc)

**Input**:
- `result`: SearchResult with URL
- `context`: ProcessingContext for spam tracking

**Output**:
- `boolean` - `true` if spam TLD (should skip), `false` if legitimate TLD (continue processing)

**Decision Logic**:
- Call `domainCredibilityService.isSpamTld(result.getUrl())`
- If true: Record spam filtered, return true
- If false: Return false

**Side Effects**:
- If spam: `context.recordSpamTldFiltered()`
- Logging: `logger.info("Spam TLD filtered: {}", result.getUrl())`

**Contract Test**:
```java
@Test
void isSpamTld_SpamTld_RecordsAndReturnsTrue() {
    // Given
    SearchResult result = SearchResult.builder().url("https://scam.xyz/grants").build();
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
void isSpamTld_LegitTld_ReturnsF alse() {
    // Given
    SearchResult result = SearchResult.builder().url("https://example.org/grants").build();
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

---

## Stage 3: Deduplication

**Method**: `private boolean isDuplicate(String domain, ProcessingContext context)`

**Purpose**: Detect duplicate domains within batch, optimize to single HashSet operation

**Input**:
- `domain`: Domain name extracted from URL
- `context`: ProcessingContext for deduplication tracking

**Output**:
- `boolean` - `true` if duplicate (should skip), `false` if unique (continue processing)

**Decision Logic**:
- Call `context.markDomainAsSeen(domain)`
- Returns `false` if duplicate (Set.add returns false for existing elements)
- Returns `true` if unique

**Side Effects**:
- `context.markDomainAsSeen()` handles counter increment internally
- Logging: `logger.debug("Duplicate domain skipped: {}", domain)`

**Contract Test**:
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

---

## Stage 4: Blacklist Checking

**Method**: `private boolean isBlacklisted(String domain, ProcessingContext context)`

**Purpose**: Check if domain is blacklisted, skip if so

**Input**:
- `domain`: Domain name to check
- `context`: ProcessingContext for blacklist tracking

**Output**:
- `boolean` - `true` if blacklisted (should skip), `false` if allowed (continue processing)

**Decision Logic**:
- Call `domainService.isBlacklisted(domain)`
- If true: Record blacklisted, return true
- If false: Return false

**Side Effects**:
- If blacklisted: `context.recordBlacklisted()`
- Logging: `logger.warn("Blacklisted domain skipped: {}", domain)`

**Contract Test**:
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

---

## Stage 5: Confidence Scoring

**Method**: `private BigDecimal calculateConfidence(SearchResult result)`

**Purpose**: Calculate confidence score for search result

**Input**:
- `result`: SearchResult with title, description, URL

**Output**:
- `BigDecimal` - Confidence score (0.00 to 1.00, scale 2)

**Decision Logic**:
- Call `confidenceScorer.calculateConfidence(result.getTitle(), result.getDescription(), result.getUrl())`
- Return score

**Side Effects**:
- None (pure calculation)
- Logging: `logger.debug("Calculated confidence {} for {}", confidence, result.getUrl())`

**Contract Test**:
```java
@Test
void calculateConfidence_ValidResult_ReturnsScore() {
    // Given
    SearchResult result = SearchResult.builder()
        .url("https://example.org/grants")
        .title("EU Grants")
        .description("Funding for research")
        .build();
    when(confidenceScorer.calculateConfidence("EU Grants", "Funding for research", "https://example.org/grants"))
        .thenReturn(new BigDecimal("0.85"));

    // When
    BigDecimal confidence = processor.calculateConfidence(result);

    // Then
    assertThat(confidence).isEqualByComparingTo(new BigDecimal("0.85"));
    verify(confidenceScorer).calculateConfidence("EU Grants", "Funding for research", "https://example.org/grants");
}
```

---

## Stage 6: Threshold Filtering

**Method**: `private boolean meetsThreshold(BigDecimal confidence, ProcessingContext context)`

**Purpose**: Check if confidence meets threshold, record appropriate counter

**Input**:
- `confidence`: Calculated confidence score
- `context`: ProcessingContext for tracking

**Output**:
- `boolean` - `true` if >= threshold (create candidate), `false` if < threshold (skip)

**Decision Logic**:
- Compare `confidence.compareTo(context.getConfidenceThreshold())`
- If >= 0: Record high confidence, return true
- If < 0: Record low confidence, return false

**Side Effects**:
- If >= threshold: `context.recordHighConfidence()`
- If < threshold: `context.recordLowConfidence()` - **Bug fix: this was never called before**
- Logging: `logger.debug("Confidence {} meets threshold: {}", confidence, meetsThreshold)`

**Contract Test**:
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

---

## Stage 7: Candidate Creation

**Method**: `private void createAndSaveCandidate(SearchResult result, String domain, BigDecimal confidence, ProcessingContext context)`

**Purpose**: Create and persist funding source candidate

**Input**:
- `result`: SearchResult with metadata
- `domain`: Extracted domain name
- `confidence`: Calculated confidence score
- `context`: ProcessingContext (session ID)

**Output**:
- `void` - Side effect only (candidate saved)

**Decision Logic**:
- Register domain via `domainService.registerOrGetDomain(domain, context.getSessionId())`
- Create candidate via `candidateCreationService.createCandidate(...)`
- Save via `candidateRepository.save(candidate)`

**Side Effects**:
- Domain registered in database
- Candidate created and saved
- Logging: `logger.info("Created candidate for {}", result.getUrl())`

**Contract Test**:
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
    Domain mockDomain = Domain.builder().domainId(domainId).domainName(domain).build();
    FundingSourceCandidate mockCandidate = FundingSourceCandidate.builder().build();

    when(domainService.registerOrGetDomain(domain, sessionId)).thenReturn(mockDomain);
    when(candidateCreationService.createCandidate(
        "EU Grants", "Funding", "https://example.org/grants", domainId, sessionId, confidence
    )).thenReturn(mockCandidate);

    // When
    processor.createAndSaveCandidate(result, domain, confidence, context);

    // Then
    verify(domainService).registerOrGetDomain(domain, sessionId);
    verify(candidateCreationService).createCandidate(
        "EU Grants", "Funding", "https://example.org/grants", domainId, sessionId, confidence
    );
    verify(candidateRepository).save(mockCandidate);
}
```

---

## Pipeline Integration Contract

**Method**: `public ProcessingStatistics processSearchResults(List<SearchResult> searchResults, UUID sessionId)`

**Purpose**: Orchestrate all pipeline stages for a batch of search results

**Flow**:
```
For each SearchResult:
  1. Extract domain (Stage 1)
     → If empty: Skip to next result
  2. Check spam TLD (Stage 2)
     → If spam: Skip to next result
  3. Check duplicate (Stage 3)
     → If duplicate: Skip to next result
  4. Check blacklist (Stage 4)
     → If blacklisted: Skip to next result
  5. Calculate confidence (Stage 5)
  6. Check threshold (Stage 6)
     → If below: Skip to next result
  7. Create candidate (Stage 7)
     → Saves to database

Return: ProcessingStatistics from context
```

**Logging**: MDC with sessionId, log at each decision point

**Contract Test**: All 7 existing SearchResultProcessorTest scenarios must pass unchanged

---
**Contracts Complete**: 2025-11-15
**Next**: Generate quickstart.md with test validation scenarios
