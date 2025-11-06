# Feature Specification: Search Result Processing (Story 1.3)

**Feature Branch**: `feature/story-1.3-search-result-processing`
**Epic**: Epic 1 - Automated Funding Discovery Workflow
**Story**: Story 1.3 - Process Search Results into Candidates
**Created**: 2025-11-05
**Status**: Draft

## Overview

This feature implements the critical bridge between raw search results (Feature 003) and the candidate review workflow. It converts filtered search engine results into structured `FundingSourceCandidate` records with initial metadata, confidence scoring, and domain-level deduplication.

## User Story

**As a** system administrator
**I want** the system to automatically process search results into funding source candidates
**So that** discovered opportunities are structured, deduplicated, and ready for Phase 2 deep crawling

## Scope

### In Scope
- Convert `SearchResult` records into `FundingSourceCandidate` records
- Extract initial metadata from search engine snippets (title, description, URL)
- Perform domain-level deduplication using existing `Domain` entity
- Calculate initial confidence scores based on search metadata quality
- Set candidates to `PENDING_CRAWL` status for Phase 2
- Track processing statistics per discovery session

### Out of Scope
- Deep web crawling (Phase 2 - future feature)
- AI-powered judging/classification (separate feature)
- Manual review UI (separate feature)
- Email notifications to admins

## Functional Requirements

### FR-001: Search Result Processing
**Given** a completed search session with filtered results (anti-spam applied)
**When** the system processes the results
**Then** it SHALL create one `FundingSourceCandidate` per unique domain

### FR-002: Domain Deduplication
**Given** multiple search results pointing to the same domain
**When** processing results
**Then** the system SHALL:
- Use the existing `Domain` entity to detect duplicates
- Skip domains marked as `BLACKLISTED`
- Merge metadata from multiple results for the same domain
- Track domain quality metrics via `DomainService`

### FR-003: Initial Confidence Scoring
**Given** search metadata (title, description, snippet)
**When** creating a candidate
**Then** the system SHALL calculate confidence score (0.00-1.00) based on:
- Presence of funding keywords in title/description
- Domain credibility indicators (.gov, .edu, .org)
- Geographic relevance to target regions
- Organization type detection (EU, government, NGO, foundation)

**Scoring Rule**: Candidates with confidence >= 0.60 are marked `PENDING_CRAWL`, candidates < 0.60 are marked `LOW_CONFIDENCE` for manual review

### FR-004: Metadata Extraction
**Given** search result fields (title, description, url, snippet)
**When** creating a candidate
**Then** the system SHALL populate:
- `title` - From search result title
- `organizationName` - Extracted from domain or title
- `sourceUrl` - Original search result URL
- `discoverySource` - Which search engine found it
- `discoveryMethod` - "SEARCH_METADATA_ONLY"
- `confidenceScore` - Calculated confidence
- `status` - PENDING_CRAWL or LOW_CONFIDENCE
- `discoveredAt` - Timestamp
- `sessionId` - Link to DiscoverySession

### FR-005: Session Statistics
**Given** a batch of results processed
**When** processing completes
**Then** the system SHALL update `DiscoverySession` with:
- Total candidates created
- High-confidence candidates (>= 0.60)
- Low-confidence candidates (< 0.60)
- Duplicates skipped (domain deduplication)
- Blacklisted domains skipped

## Key Entities

### Existing (Already Implemented)
- `SearchResult` - Raw search engine results (Feature 003)
- `FundingSourceCandidate` - Target entity for candidates
- `Domain` - Domain deduplication and blacklist tracking
- `DiscoverySession` - Session tracking with statistics
- `DomainService` - Business logic for domain management

### New (This Feature)
- `SearchResultProcessor` - Orchestrates result → candidate conversion
- `CandidateCreationService` - Business logic for candidate creation with confidence scoring

## Acceptance Criteria

### AC-001: Happy Path
**Given** 20 search results from 15 unique domains
**When** processing completes
**Then**:
- 15 candidates are created (domain deduplication worked)
- Each candidate has confidence score 0.00-1.00
- High-confidence candidates (>= 0.60) have status PENDING_CRAWL
- Low-confidence candidates (< 0.60) have status LOW_CONFIDENCE
- Session statistics are updated correctly

### AC-002: Duplicate Detection
**Given** 5 search results all from "example.org"
**When** processing completes
**Then**:
- Only 1 candidate is created for "example.org"
- The candidate merges metadata from all 5 results
- Domain entity is updated with `last_seen_at` timestamp

### AC-003: Blacklist Handling
**Given** 3 search results from blacklisted domains
**When** processing completes
**Then**:
- No candidates are created for blacklisted domains
- Session statistics show "3 blacklisted domains skipped"

### AC-004: Confidence Threshold Filter
**Given** 10 search results with varying metadata quality
**When** processing completes with confidence threshold 0.60
**Then**:
- Candidates with confidence >= 0.60 have status PENDING_CRAWL
- Candidates with confidence < 0.60 have status LOW_CONFIDENCE
- Statistics accurately reflect the split

## Design Notes

### Orchestration Flow
```
MultiProviderSearchOrchestrator (Feature 003)
  ↓ (returns List<SearchResult>)
SearchResultProcessor
  ↓ (for each result)
  1. Extract domain from URL
  2. Check DomainService for blacklist/duplicate
  3. Calculate confidence score
  4. Create/update FundingSourceCandidate via CandidateCreationService
  5. Update Domain entity via DomainService
  ↓
Update DiscoverySession statistics
```

### Confidence Scoring Algorithm (Initial Implementation)
```
Base score: 0.00

+ 0.20 if title contains funding keywords (grant, funding, scholarship, etc.)
+ 0.15 if description contains funding keywords
+ 0.15 if domain is credible TLD (.gov = 0.15, .edu = 0.12, .org = 0.10, .eu = 0.12)
+ 0.15 if geographic match (Bulgaria, EU, Eastern Europe in title/description)
+ 0.15 if organization type detected (European Commission, Ministry, Foundation)
+ 0.20 if multiple signals present (compound confidence boost)

Max score: 1.00
```

**Note**: This is a simple heuristic for Phase 1. Future features will enhance with AI-powered judging.

### BigDecimal Precision
All confidence scores MUST use `BigDecimal` with scale 2 (two decimal places) to avoid floating-point precision errors in threshold comparisons.

## Testing Strategy

### Unit Tests
- `CandidateCreationService` - Test confidence scoring algorithm with various inputs
- `SearchResultProcessor` - Mock dependencies, test orchestration logic
- Domain deduplication logic
- Blacklist filtering logic

### Integration Tests
- End-to-end: SearchResult → Candidate with real PostgreSQL (TestContainers)
- Domain deduplication with database
- Session statistics updates
- Multiple results from same domain (merge scenario)

**Target**: 30-40 tests (15-20 unit, 15-20 integration)

## Dependencies

### Existing Features Required
- ✅ Feature 003: Search provider infrastructure and `SearchResult` entity
- ✅ Feature 005: Enhanced taxonomy enums (`FundingSourceType`, etc.)
- ✅ Domain model: `FundingSourceCandidate`, `Domain`, `DiscoverySession`
- ✅ Persistence layer: Repositories and services

### External Dependencies
- PostgreSQL 16 (Mac Studio @ 192.168.1.10:5432)
- TestContainers for integration tests

## Non-Functional Requirements

### Performance
- Process 100 search results in < 2 seconds
- Use Virtual Threads for parallel candidate creation if batch > 50 results

### Reliability
- Transactional processing - all or nothing for a batch
- Idempotent - running processor multiple times on same results produces same candidates

### Observability
- Log confidence score distribution per session
- Log domains skipped (blacklist/duplicate)
- Track processing time per batch

## Future Enhancements (Out of Scope)

- AI-powered confidence scoring using LM Studio
- Multi-language support for metadata extraction
- Automatic blacklist learning from rejection patterns
- Candidate similarity detection (beyond domain deduplication)

## Success Metrics

- **Coverage**: All search results are processed into candidates
- **Deduplication**: No duplicate candidates per domain per session
- **Accuracy**: Initial confidence scores correlate with manual approval rates (to be measured in future)
- **Performance**: Processing time < 2 seconds per 100 results

---

## Review Checklist

- [x] Clear user story and acceptance criteria
- [x] Scope clearly defined (in/out of scope)
- [x] Functional requirements are testable
- [x] Design notes provide implementation guidance
- [x] Dependencies identified
- [x] Testing strategy defined
- [x] No specific implementation details (tech stack, APIs)

**Status**: ✅ Ready for implementation planning
