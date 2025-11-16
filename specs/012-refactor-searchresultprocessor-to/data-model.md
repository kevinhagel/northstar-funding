# Data Model: SearchResultProcessor Refactoring

**Feature**: 012-refactor-searchresultprocessor-to
**Date**: 2025-11-15
**Purpose**: Define data structures for refactored SearchResultProcessor

## Overview

This refactoring introduces **one new domain object** (ProcessingContext) and **enhances one existing model** (ProcessingStatistics). No database schema changes required.

## New Entity: ProcessingContext

### Purpose
Encapsulates all processing state for a single search result batch, eliminating primitive obsession and providing type-safe state management.

### Responsibility
- Track session ID for logging correlation
- Maintain confidence threshold (constant)
- Track seen domains for deduplication
- Count processing outcomes (spam, blacklist, duplicates, candidates, invalid URLs)
- Generate final ProcessingStatistics

### Fields

| Field | Type | Purpose | Initial Value |
|-------|------|---------|---------------|
| sessionId | UUID | Session correlation for logging | Constructor parameter |
| confidenceThreshold | BigDecimal | Threshold for creating candidates (0.60) | `new BigDecimal("0.60")` |
| seenDomains | Set<String> | Deduplicate domains within batch | `new HashSet<>()` |
| spamTldFiltered | int | Count of spam TLD results filtered | 0 |
| blacklistedSkipped | int | Count of blacklisted domains skipped | 0 |
| duplicatesSkipped | int | Count of duplicate domains skipped | 0 |
| highConfidenceCreated | int | Count of high-confidence candidates created | 0 |
| lowConfidenceCreated | int | Count of low-confidence candidates created | 0 |
| invalidUrlsSkipped | int | Count of invalid URLs (domain extraction failed) | 0 |

### Methods

#### State Mutation Methods

**`boolean markDomainAsSeen(String domain)`**
- **Purpose**: Register domain as seen, detect duplicates
- **Returns**: `true` if unique (first time), `false` if duplicate
- **Side Effect**: Increments `duplicatesSkipped` if duplicate
- **Implementation**: Uses `Set.add()` which returns `false` if element already exists

```java
public boolean markDomainAsSeen(String domain) {
    if (!seenDomains.add(domain)) {
        duplicatesSkipped++;
        return false;  // Duplicate
    }
    return true;  // Unique
}
```

**`void recordSpamTldFiltered()`**
- **Purpose**: Increment spam TLD filter counter
- **Side Effect**: `spamTldFiltered++`

**`void recordBlacklisted()`**
- **Purpose**: Increment blacklist counter
- **Side Effect**: `blacklistedSkipped++`

**`void recordLowConfidence()`**
- **Purpose**: Increment low-confidence candidate counter
- **Side Effect**: `lowConfidenceCreated++`
- **Note**: Fixes bug where this counter was never incremented

**`void recordHighConfidence()`**
- **Purpose**: Increment high-confidence candidate counter
- **Side Effect**: `highConfidenceCreated++`

**`void recordInvalidUrl()`**
- **Purpose**: Increment invalid URL counter (NEW)
- **Side Effect**: `invalidUrlsSkipped++`

#### Query Methods

**`UUID getSessionId()`**
- **Purpose**: Get session ID for logging
- **Returns**: Session UUID

**`BigDecimal getConfidenceThreshold()`**
- **Purpose**: Get threshold for confidence filtering
- **Returns**: BigDecimal 0.60

**`ProcessingStatistics buildStatistics(int totalResults)`**
- **Purpose**: Generate immutable statistics summary
- **Parameters**: `totalResults` - total number of input search results
- **Returns**: ProcessingStatistics with all counters

```java
public ProcessingStatistics buildStatistics(int totalResults) {
    return ProcessingStatistics.builder()
        .totalResults(totalResults)
        .spamTldFiltered(spamTldFiltered)
        .blacklistedSkipped(blacklistedSkipped)
        .duplicatesSkipped(duplicatesSkipped)
        .highConfidenceCreated(highConfidenceCreated)
        .lowConfidenceCreated(lowConfidenceCreated)
        .invalidUrlsSkipped(invalidUrlsSkipped)
        .build();
}
```

### Implementation Notes

**Constructor**:
```java
public ProcessingContext(UUID sessionId) {
    this.sessionId = sessionId;
    this.confidenceThreshold = new BigDecimal("0.60");
    this.seenDomains = new HashSet<>();
    // All counters initialized to 0 by default
}
```

**Package**: `com.northstar.funding.crawler.processing`

**Annotations**: None needed (simple POJO, no persistence)

**Testing Strategy**:
- Unit tests for state mutation methods
- Verify counter increments
- Test `markDomainAsSeen()` duplicate detection
- Test `buildStatistics()` accuracy

## Enhanced Entity: ProcessingStatistics

### Purpose
Immutable summary of search result processing outcomes

### Changes

**NEW Field**:
- `invalidUrlsSkipped: int` - Count of results with invalid URLs (domain extraction failed)

**Existing Fields** (unchanged):
- `totalResults: int` - Total search results processed
- `spamTldFiltered: int` - Results filtered by spam TLD check
- `blacklistedSkipped: int` - Results skipped due to blacklist
- `duplicatesSkipped: int` - Results skipped due to duplicate domain
- `highConfidenceCreated: int` - High-confidence candidates created (>= 0.60)
- `lowConfidenceCreated: int` - Low-confidence candidates created (< 0.60) - **Now actually used**

### Derived Methods (unchanged)

**`int getTotalCandidatesCreated()`**
- Returns: `highConfidenceCreated + lowConfidenceCreated`

**`int getTotalProcessed()`**
- Returns: `spamTldFiltered + blacklistedSkipped + duplicatesSkipped + getTotalCandidatesCreated()`

### Migration Impact

**Lombok @Builder**: Add `invalidUrlsSkipped` to existing builder

**Before**:
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
}
```

**After**:
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
    private int invalidUrlsSkipped;  // NEW
}
```

**Backward Compatibility**:
- Existing code calling `.builder()` will get `invalidUrlsSkipped = 0` by default
- All existing usages continue to work
- No database schema changes (this is not a persisted entity)

## Existing Entities (Unchanged)

### SearchResult
- **Purpose**: Search engine result metadata
- **Fields**: `title`, `description`, `url`
- **Changes**: None

### FundingSourceCandidate
- **Purpose**: Funding source candidate entity
- **Changes**: None (created by existing CandidateCreationService)

### Domain
- **Purpose**: Domain deduplication and blacklist tracking
- **Changes**: None (used by existing DomainService)

## Entity Relationships

```
ProcessingContext (new)
  ├─> generates: ProcessingStatistics (enhanced)
  ├─> tracks: Set<String> seenDomains
  └─> references: UUID sessionId

SearchResultProcessor
  ├─> creates: ProcessingContext (per batch)
  ├─> processes: List<SearchResult> (input)
  ├─> uses: DomainService (blacklist, domain extraction)
  ├─> uses: DomainCredibilityService (spam TLD check)
  ├─> uses: ConfidenceScorer (calculate scores)
  ├─> uses: CandidateCreationService (create candidates)
  └─> returns: ProcessingStatistics (output)
```

## Validation Rules

### ProcessingContext
- Session ID must not be null (constructor parameter)
- Confidence threshold is constant (0.60)
- All counters are non-negative (incremented only, never decremented)
- Seen domains Set must not be null

### ProcessingStatistics
- Total results >= 0
- All counters >= 0
- `getTotalCandidatesCreated()` = `highConfidenceCreated + lowConfidenceCreated`
- `getTotalProcessed()` should approximately equal `totalResults` (accounting for invalid URLs)

## Testing Strategy

### Unit Tests

**ProcessingContextTest**:
- `testMarkDomainAsSeen_FirstTime_ReturnsTrue()`
- `testMarkDomainAsSeen_Duplicate_ReturnsFalseAndIncrements()`
- `testRecordSpamTldFiltered_IncrementsCounter()`
- `testRecordBlacklisted_IncrementsCounter()`
- `testRecordLowConfidence_IncrementsCounter()` - **Validates bug fix**
- `testRecordHighConfidence_IncrementsCounter()`
- `testRecordInvalidUrl_IncrementsCounter()` - **New feature**
- `testBuildStatistics_ReturnsAccurateStats()`

**ProcessingStatisticsTest** (existing):
- Add test for `invalidUrlsSkipped` field
- Verify builder includes new field
- Verify `getTotalProcessed()` calculation

### Integration Tests
No integration tests needed - ProcessingContext is not persisted

---
**Data Model Complete**: 2025-11-15
**Next**: Generate pipeline stage contracts
