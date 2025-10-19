# Data Model: Automated Crawler Infrastructure - Phase 1 Metadata Judging

**Feature**: 002-create-automated-crawler
**Date**: 2025-10-19

## Entity Overview

This feature extends the existing domain model from 001-automated-funding-discovery with crawler-specific entities.

### New Entities
1. **Domain**: Domain registry for deduplication and blacklist management
2. **SearchResult** (DTO): Search engine metadata (not persisted)
3. **MetadataJudgment** (DTO): Judging results (not persisted, embedded in candidate)
4. **ProcessingStats** (DTO): Session statistics (not persisted)

### Extended Entities
1. **FundingSourceCandidate**: Add domainId FK, new status values (PENDING_CRAWL, SKIPPED_LOW_CONFIDENCE)
2. **DiscoverySession**: Reuse existing entity for session tracking

---

## Entity Definitions

### 1. Domain (Persistent Entity)

**Purpose**: Track discovered domains to implement domain-level deduplication and blacklist management.

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| domainId | UUID | PK, NOT NULL | Unique identifier |
| domainName | VARCHAR(255) | UNIQUE, NOT NULL | Domain extracted from URL (e.g., "us-bulgaria.org") |
| status | domain_status | NOT NULL, DEFAULT 'DISCOVERED' | Current processing status |
| discoveredAt | TIMESTAMP | NOT NULL, DEFAULT NOW() | When first discovered |
| discoverySessionId | UUID | FK → DiscoverySession | Session that first found this domain |
| lastProcessedAt | TIMESTAMP | NULL | When last processing occurred |
| processingCount | INTEGER | NOT NULL, DEFAULT 0 | How many times processed |
| bestConfidenceScore | DOUBLE PRECISION | NULL | Highest confidence from any candidate |
| highQualityCandidateCount | INTEGER | NOT NULL, DEFAULT 0 | Count of candidates with confidence >= 0.6 |
| lowQualityCandidateCount | INTEGER | NOT NULL, DEFAULT 0 | Count of candidates with confidence < 0.6 |
| blacklistedBy | UUID | FK → AdminUser, NULL | Admin who blacklisted |
| blacklistedAt | TIMESTAMP | NULL | When blacklisted |
| blacklistReason | TEXT | NULL | Human-provided reason |
| noFundsYear | INTEGER | NULL | Year when marked "no funds this year" |
| notes | TEXT | NULL | Admin notes |
| failureReason | TEXT | NULL | Reason for processing failure |
| failureCount | INTEGER | NOT NULL, DEFAULT 0 | Consecutive failure count |
| retryAfter | TIMESTAMP | NULL | When to retry after failure |

**Enum: domain_status**:
- `DISCOVERED`: Newly discovered, not yet processed
- `PROCESSING`: Currently being processed
- `PROCESSED_HIGH_QUALITY`: Yielded high-quality candidates
- `PROCESSED_LOW_QUALITY`: Yielded only low-quality candidates
- `BLACKLISTED`: Permanently blocked (scam, spam, irrelevant)
- `NO_FUNDS_THIS_YEAR`: Legitimate but no current funding
- `PROCESSING_FAILED`: Technical error occurred

**Indexes**:
- `idx_domain_name` ON (domain_name)
- `idx_domain_status` ON (status)
- `idx_domain_discovered_at` ON (discovered_at)
- `idx_domain_last_processed` ON (last_processed_at)
- `idx_domain_retry` ON (status, retry_after) WHERE status = 'PROCESSING_FAILED'

**Business Rules**:
- Domain name must be unique (case-insensitive)
- Blacklisted domains are never re-processed
- Low-quality domains (3+ low-quality candidates, 0 high-quality) are auto-marked PROCESSED_LOW_QUALITY
- Exponential backoff for failures: 1h → 4h → 1d → 1w

---

### 2. FundingSourceCandidate (Extended Entity)

**Extensions to existing entity** (from 001-automated-funding-discovery):

**New Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| domainId | UUID | FK → Domain, NULL | Associated domain for quality tracking |

**New Status Values** (extends existing CandidateStatus enum):
- `PENDING_CRAWL`: Phase 1 complete, high confidence, awaiting Phase 2 deep crawl
- `SKIPPED_LOW_CONFIDENCE`: Low confidence from Phase 1, not worth crawling

**Existing Statuses** (from 001):
- `PENDING_REVIEW`: Phase 2 complete, awaiting human review
- `IN_REVIEW`: Currently being reviewed by admin
- `APPROVED`: Validated and approved for knowledge base
- `REJECTED`: Rejected by admin

**Migration**: V9 adds domainId column and updates status constraint

---

### 3. SearchResult (DTO - Not Persisted)

**Purpose**: Represents search engine metadata for Phase 1 judging (no persistence needed).

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| url | String | Search result URL |
| title | String | Page title from search engine |
| snippet | String | Description/snippet from search engine |
| searchEngine | String | Source engine (searxng, tavily, browserbase, perplexity) |
| searchQuery | String | Original query that found this result |
| position | Integer | Position in search results (1-based) |

**Usage**: Passed to MetadataJudgingService for scoring

---

### 4. MetadataJudgment (DTO - Not Persisted)

**Purpose**: Result of Phase 1 metadata judging (embedded in candidate validation_notes if created).

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| confidenceScore | Double | Overall score (0.0-1.0) |
| shouldCrawl | Boolean | True if >= 0.6 threshold |
| domainName | String | Extracted domain name |
| judgeScores | List<JudgeScore> | Individual judge results |
| reasoning | String | Human-readable explanation |
| extractedOrganizationName | String | Organization name from title |
| extractedProgramName | String | Program name from title |

**Nested: JudgeScore**:
| Field | Type | Description |
|-------|------|-------------|
| judgeName | String | Judge identifier (FundingKeywordJudge, DomainCredibilityJudge, etc.) |
| score | Double | Judge score (0.0-1.0) |
| weight | Double | Weight in overall calculation (default 1.0) |
| explanation | String | Judge reasoning |

**Usage**: Created by MetadataJudgingService, used by CandidateProcessingOrchestrator

---

### 5. ProcessingStats (DTO - Not Persisted)

**Purpose**: Aggregated statistics from Phase 1 processing session.

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| totalProcessed | Integer | Total search results processed |
| candidatesCreated | Integer | Candidates created (PENDING_CRAWL) |
| skippedLowConfidence | Integer | Skipped due to low confidence |
| skippedDomainAlreadyProcessed | Integer | Skipped due to domain deduplication |
| skippedBlacklisted | Integer | Skipped due to blacklist |
| failures | Integer | Processing errors |
| averageConfidence | Double | Average confidence of created candidates |
| maxConfidence | Double | Highest confidence score |
| minConfidence | Double | Lowest confidence score |
| processingTimeMs | Long | Total processing duration |

**Usage**: Returned by CandidateProcessingOrchestrator, logged for monitoring

---

## Entity Relationships

```
DiscoverySession (existing)
  ↓ 1:N
Domain (new)
  ↓ 1:N
FundingSourceCandidate (extended)
  ↓ 1:N
ContactIntelligence (existing, Phase 2)

AdminUser (existing)
  ↓ 1:N (blacklists)
Domain

SearchResult (DTO) → MetadataJudgment (DTO) → ProcessingStats (DTO)
```

---

## State Transitions

### Domain Status Transitions
```
DISCOVERED
  ↓ (start processing)
PROCESSING
  ↓ (high-quality candidate created)
PROCESSED_HIGH_QUALITY
  OR
  ↓ (3+ low-quality, 0 high-quality)
PROCESSED_LOW_QUALITY
  OR
  ↓ (error)
PROCESSING_FAILED → (retry_after passed) → PROCESSING
  OR
  ↓ (admin action)
BLACKLISTED
  OR
  ↓ (admin action)
NO_FUNDS_THIS_YEAR → (new year) → PROCESSING
```

### FundingSourceCandidate Status Transitions (Extended)
```
[Phase 1: Metadata Judging]
PENDING_CRAWL (confidence >= 0.6)
  OR
SKIPPED_LOW_CONFIDENCE (confidence < 0.6) [terminal]

[Phase 2: Deep Crawling - future]
PENDING_CRAWL
  ↓ (crawled successfully)
PENDING_REVIEW

[Human Review Workflow - from 001]
PENDING_REVIEW
  ↓ (admin assigns)
IN_REVIEW
  ↓ (admin decision)
APPROVED or REJECTED [terminal]
```

---

## Validation Rules

### Domain
- `domainName` must be valid domain format (lowercase, no protocol, no path)
- `blacklistReason` required when status = BLACKLISTED
- `failureReason` required when status = PROCESSING_FAILED
- `noFundsYear` must be valid year (2020-2100) when set
- `retryAfter` must be future timestamp when status = PROCESSING_FAILED

### FundingSourceCandidate (Extended)
- `domainId` should be set when created from Phase 1 metadata judging
- `confidenceScore` must be 0.0-1.0
- Status PENDING_CRAWL requires confidenceScore >= 0.6
- Status SKIPPED_LOW_CONFIDENCE requires confidenceScore < 0.6

### MetadataJudgment
- `confidenceScore` must be 0.0-1.0
- `shouldCrawl` = true IFF confidenceScore >= 0.6
- `judgeScores` must have at least 1 judge
- `domainName` must be valid domain format

---

## Database Schema Changes

### V8: Create Domain Table
```sql
CREATE TYPE domain_status AS ENUM (
    'DISCOVERED', 'PROCESSING', 'PROCESSED_HIGH_QUALITY',
    'PROCESSED_LOW_QUALITY', 'BLACKLISTED', 'NO_FUNDS_THIS_YEAR',
    'PROCESSING_FAILED'
);

CREATE TABLE domain (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_name VARCHAR(255) NOT NULL UNIQUE,
    status domain_status NOT NULL DEFAULT 'DISCOVERED',
    -- [full schema in migration file]
);
```

### V9: Extend FundingSourceCandidate
```sql
ALTER TABLE funding_source_candidate
ADD COLUMN domain_id UUID REFERENCES domain(domain_id);

ALTER TABLE funding_source_candidate
DROP CONSTRAINT funding_source_candidate_status_check;

ALTER TABLE funding_source_candidate
ADD CONSTRAINT funding_source_candidate_status_check
    CHECK (status IN (
        'PENDING_CRAWL', 'PENDING_REVIEW', 'IN_REVIEW',
        'APPROVED', 'REJECTED', 'SKIPPED_LOW_CONFIDENCE'
    ));
```

---

## Implementation Notes

### Already Implemented ✅
- Domain entity: `/backend/src/main/java/com/northstar/funding/discovery/domain/Domain.java`
- DomainStatus enum: `/backend/src/main/java/com/northstar/funding/discovery/domain/DomainStatus.java`
- CandidateStatus enum (updated): `/backend/src/main/java/com/northstar/funding/discovery/domain/CandidateStatus.java`
- FundingSourceCandidate (extended): Added domainId field
- SearchResult DTO: `/backend/src/main/java/com/northstar/funding/discovery/service/dto/SearchResult.java`
- MetadataJudgment DTO: `/backend/src/main/java/com/northstar/funding/discovery/service/dto/MetadataJudgment.java`
- ProcessingStats DTO: `/backend/src/main/java/com/northstar/funding/discovery/service/dto/ProcessingStats.java`
- Migration V8: `/ backend/src/main/resources/db/migration/V8__create_domain.sql`
- Migration V9: `/backend/src/main/resources/db/migration/V9__update_candidate_status_two_phase.sql`

### Pending Implementation
- DomainRepository: Spring Data JDBC repository with custom queries
- Integration tests for domain deduplication logic
- Integration tests for status transitions
