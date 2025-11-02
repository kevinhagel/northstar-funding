# Data Model: Search Provider Adapters

**Feature**: 003-design-and-implement
**Date**: 2025-11-01
**Status**: Phase 1 Design

---

## Overview

This feature uses **existing domain entities** from the `northstar-domain` module and does NOT create new entities. All entities listed below already exist in the codebase.

The search provider adapters populate existing `SearchResult` and `Domain` entities, then persist them using existing services from `northstar-persistence` module.

---

## Existing Entities (northstar-domain)

### 1. SearchResult
**Purpose**: Represents a single search result from any provider.

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/SearchResult.java`

**Fields** (from existing entity):
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "search_result")
public class SearchResult {
    @Id
    private Long id;

    private UUID discoverySessionId;      // Links to DiscoverySession
    private SearchEngineType searchEngine; // BRAVE_SEARCH, SEARXNG, SERPER, TAVILY
    private String query;                  // Query used to find this result
    private String url;                    // Full URL of the result
    private String title;                  // Page title from search metadata
    private String description;            // Meta description from search
    private String domain;                 // Extracted domain (normalized)
    private Integer position;              // Position in search results (1-based)
    private LocalDateTime discoveredAt;    // When this result was found
    private Boolean isDuplicate;           // Domain deduplication flag
    private UUID duplicateOfDomain;        // If duplicate, references Domain.id
}
```

**Validation Rules**:
- `discoverySessionId` - REQUIRED (must reference existing DiscoverySession)
- `searchEngine` - REQUIRED (enum: BRAVE_SEARCH, SEARXNG, SERPER, TAVILY)
- `query` - REQUIRED (max 500 chars)
- `url` - REQUIRED (valid URL format, max 2000 chars)
- `title` - OPTIONAL (max 500 chars, may be null for some results)
- `description` - OPTIONAL (max 1000 chars, may be null)
- `domain` - REQUIRED (normalized: lowercase, no www, no protocol)
- `position` - REQUIRED (> 0, represents ranking in results)
- `discoveredAt` - REQUIRED (set to NOW() on creation)
- `isDuplicate` - REQUIRED (true if domain already processed)
- `duplicateOfDomain` - OPTIONAL (set only if isDuplicate=true)

**Normalization Rules** (implemented in SearchResultService):
1. **Domain Extraction**:
   - Extract from URL: `https://example.org/page` → `example.org`
   - Remove `www.` prefix: `www.example.org` → `example.org`
   - Lowercase: `Example.Org` → `example.org`

2. **Deduplication Check**:
   - Query DomainRepository for existing domain
   - If found: Set `isDuplicate=true`, `duplicateOfDomain=<existing domain UUID>`
   - If not found: Set `isDuplicate=false`, create new Domain entity

**State Transitions**: None (SearchResult is immutable after creation)

---

### 2. Domain
**Purpose**: Represents a unique funding organization domain for deduplication and blacklist management.

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/Domain.java`

**Fields** (from existing entity):
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "domain")
public class Domain {
    @Id
    private UUID id;

    private String domainName;              // Normalized domain (e.g., "example.org")
    private DomainStatus status;            // DISCOVERED, PROCESSED_HIGH_QUALITY, etc.
    private UUID firstDiscoverySessionId;   // When first discovered
    private LocalDateTime firstDiscoveredAt;
    private LocalDateTime lastSeenAt;       // Most recent search result
    private Integer totalOccurrences;       // Count of search results for this domain

    // Blacklist fields
    private Boolean isBlacklisted;
    private LocalDateTime blacklistedAt;
    private UUID blacklistedBy;             // AdminUser who blacklisted
    private String blacklistReason;         // Why blacklisted (e.g., "spam", "scammer")

    // Quality tracking fields
    private Integer highQualityCandidates;  // Count of high-confidence candidates
    private Integer lowQualityCandidates;   // Count of low-confidence candidates
    private BigDecimal averageConfidence;   // Average confidence score (DECIMAL(3,2))
}
```

**Validation Rules**:
- `domainName` - REQUIRED (normalized format, unique constraint)
- `status` - REQUIRED (enum: DISCOVERED, PROCESSED_HIGH_QUALITY, PROCESSED_LOW_QUALITY, BLACKLISTED, etc.)
- `firstDiscoverySessionId` - REQUIRED (set on creation)
- `firstDiscoveredAt` - REQUIRED (set to NOW() on creation)
- `lastSeenAt` - REQUIRED (updated on every new SearchResult)
- `totalOccurrences` - REQUIRED (default 1, incremented on each new SearchResult)
- `isBlacklisted` - REQUIRED (default false)
- `blacklistedAt`, `blacklistedBy`, `blacklistReason` - REQUIRED if isBlacklisted=true
- `highQualityCandidates`, `lowQualityCandidates` - REQUIRED (default 0)
- `averageConfidence` - OPTIONAL (calculated by judging module, NOT set by search adapters)

**Domain Status Enum** (from existing DomainStatus):
- `DISCOVERED` - Initial state when domain first found
- `PENDING_CRAWL` - Scheduled for Phase 2 deep crawling
- `CRAWLED` - Phase 2 complete
- `PROCESSED_HIGH_QUALITY` - Contains high-value funding programs
- `PROCESSED_LOW_QUALITY` - Low-value or no funding programs
- `BLACKLISTED` - Spam/scammer/irrelevant site

**State Transitions**:
1. NEW DOMAIN → `DISCOVERED` (search adapter creates Domain)
2. `DISCOVERED` → `PENDING_CRAWL` (metadata judging approves for crawling)
3. `DISCOVERED` → `BLACKLISTED` (anti-spam filtering detects scammer)
4. `PENDING_CRAWL` → `CRAWLED` (Phase 2 crawler completes)
5. `CRAWLED` → `PROCESSED_HIGH_QUALITY` or `PROCESSED_LOW_QUALITY` (Phase 3 judging)

**Anti-Spam Integration** (for this feature):
- Search adapters check `isBlacklisted` before creating SearchResult
- If domain is blacklisted, skip creating SearchResult, log rejection
- Blacklist check happens AFTER anti-spam filtering (spam detection adds to blacklist)

---

### 3. DiscoverySession
**Purpose**: Tracks a single funding discovery session (nightly or weekly search execution).

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/DiscoverySession.java`

**Fields** (relevant to search adapters):
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "discovery_session")
public class DiscoverySession {
    @Id
    private UUID id;

    private SessionType type;              // NIGHTLY_DISCOVERY, WEEKLY_DEEP_CRAWL, etc.
    private SessionStatus status;          // RUNNING, COMPLETED, FAILED
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Integer totalQueriesExecuted;  // Count of search queries
    private Integer totalResultsFound;     // Total SearchResult count
    private Integer newDomainsDiscovered;  // New Domain entities created
    private Integer duplicateDomainsSkipped; // Existing domains (deduplication)
    private Integer spamResultsFiltered;   // Anti-spam filtering count

    // Per-provider statistics
    private Integer braveSearchResults;
    private Integer searxngResults;
    private Integer serperResults;
    private Integer tavilyResults;

    private String errorMessage;           // If status=FAILED
}
```

**Validation Rules**:
- `type` - REQUIRED (enum: NIGHTLY_DISCOVERY, WEEKLY_DEEP_CRAWL, MANUAL_SEARCH)
- `status` - REQUIRED (enum: RUNNING, COMPLETED, FAILED, PARTIAL_SUCCESS)
- `startedAt` - REQUIRED (set to NOW() when session starts)
- `completedAt` - REQUIRED (set to NOW() when session ends)
- `totalQueriesExecuted` - REQUIRED (count of search queries, NOT API calls)
- `totalResultsFound` - REQUIRED (sum of all SearchResult entities created)
- `newDomainsDiscovered` - REQUIRED (count of new Domain entities)
- `duplicateDomainsSkipped` - REQUIRED (count of isDuplicate=true results)
- `spamResultsFiltered` - REQUIRED (count of anti-spam rejections)
- Per-provider counts - REQUIRED (sum must equal totalResultsFound)

**Session Lifecycle** (managed by orchestrator):
1. Create DiscoverySession with status=RUNNING, startedAt=NOW()
2. Execute searches across all providers (parallel)
3. Update statistics as results are processed
4. Set status=COMPLETED/FAILED/PARTIAL_SUCCESS, completedAt=NOW()

---

### 4. SearchEngineType (Enum)
**Purpose**: Identifies which search provider returned the result.

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/SearchEngineType.java`

**Values** (from existing enum):
- `BRAVE_SEARCH` - BraveSearchAdapter
- `SEARXNG` - SearxngAdapter
- `SERPER` - SerperAdapter
- `TAVILY` - TavilyAdapter
- `MANUAL_INPUT` - (not used in this feature)

---

## Entity Relationships

```
DiscoverySession (1) ←──(many)── SearchResult ──(many)→ (1) Domain
         │                             │                      │
         │                             │                      │
    [Session Stats]           [Search Metadata]      [Deduplication + Blacklist]
    - totalResultsFound       - title, description   - domainName (unique)
    - newDomainsDiscovered    - url, position        - isBlacklisted
    - spamResultsFiltered     - isDuplicate          - status
```

**Key Relationships**:
1. **DiscoverySession → SearchResult**: One-to-many
   - Each search session creates multiple SearchResult entities
   - SearchResult.discoverySessionId references DiscoverySession.id

2. **SearchResult → Domain**: Many-to-one
   - Multiple SearchResult entities can reference same Domain (different URLs, same domain)
   - SearchResult.duplicateOfDomain references Domain.id (if isDuplicate=true)

3. **Domain (Self-Reference)**: Deduplication
   - Domain.domainName has unique constraint
   - First SearchResult for domain creates Domain entity
   - Subsequent SearchResult entries for same domain set isDuplicate=true

---

## Data Flow

### Search Execution Pipeline

```
1. Orchestrator creates DiscoverySession (status=RUNNING)
   ↓
2. Execute 4 search providers in parallel (Virtual Threads)
   ↓
3. For each SearchResult from providers:
   ↓
   3a. Anti-spam filtering (fuzzy matching)
       - Keyword stuffing detection
       - Domain-metadata mismatch
       - Cross-category spam
       → IF SPAM: Increment spamResultsFiltered, SKIP TO NEXT
   ↓
   3b. Domain extraction and normalization
       - Extract domain from URL
       - Normalize: lowercase, remove www, remove protocol
   ↓
   3c. Blacklist check (DomainRepository)
       → IF BLACKLISTED: Log rejection, SKIP TO NEXT
   ↓
   3d. Deduplication check (DomainRepository.findByDomainName)
       → IF EXISTS: Set isDuplicate=true, duplicateOfDomain=<UUID>
       → IF NEW: Create Domain entity, set isDuplicate=false
   ↓
   3e. Create SearchResult entity
       - Populate all fields
       - Save via SearchResultService
   ↓
   3f. Update Domain statistics
       - Increment totalOccurrences
       - Update lastSeenAt
       - Save via DomainService
   ↓
4. Aggregate results from all providers
   ↓
5. Update DiscoverySession statistics
   - Set status=COMPLETED/PARTIAL_SUCCESS/FAILED
   - Set completedAt=NOW()
   - Update totalResultsFound, newDomainsDiscovered, etc.
```

---

## Database Schema (Existing Tables)

All tables already exist from previous Flyway migrations:

### search_result table (V17)
```sql
CREATE TABLE search_result (
    id BIGSERIAL PRIMARY KEY,
    discovery_session_id UUID NOT NULL REFERENCES discovery_session(id),
    search_engine VARCHAR(50) NOT NULL,  -- SearchEngineType enum
    query TEXT NOT NULL,
    url TEXT NOT NULL,
    title TEXT,
    description TEXT,
    domain TEXT NOT NULL,
    position INT NOT NULL CHECK (position > 0),
    discovered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    duplicate_of_domain UUID REFERENCES domain(id),

    INDEX idx_session (discovery_session_id),
    INDEX idx_domain (domain),
    INDEX idx_discovered_at (discovered_at)
);
```

### domain table (V8)
```sql
CREATE TABLE domain (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_name VARCHAR(255) NOT NULL UNIQUE,  -- Normalized domain
    status VARCHAR(50) NOT NULL,                -- DomainStatus enum
    first_discovery_session_id UUID NOT NULL REFERENCES discovery_session(id),
    first_discovered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    total_occurrences INT NOT NULL DEFAULT 1,

    -- Blacklist fields
    is_blacklisted BOOLEAN NOT NULL DEFAULT FALSE,
    blacklisted_at TIMESTAMP,
    blacklisted_by UUID REFERENCES admin_user(id),
    blacklist_reason TEXT,

    -- Quality tracking
    high_quality_candidates INT NOT NULL DEFAULT 0,
    low_quality_candidates INT NOT NULL DEFAULT 0,
    average_confidence DECIMAL(3,2),

    INDEX idx_domain_name (domain_name),
    INDEX idx_status (status),
    INDEX idx_blacklisted (is_blacklisted)
);
```

### discovery_session table (V4, extended in V12)
```sql
CREATE TABLE discovery_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,             -- SessionType enum
    status VARCHAR(50) NOT NULL,           -- SessionStatus enum
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,

    -- Statistics
    total_queries_executed INT NOT NULL DEFAULT 0,
    total_results_found INT NOT NULL DEFAULT 0,
    new_domains_discovered INT NOT NULL DEFAULT 0,
    duplicate_domains_skipped INT NOT NULL DEFAULT 0,
    spam_results_filtered INT NOT NULL DEFAULT 0,

    -- Per-provider statistics
    brave_search_results INT NOT NULL DEFAULT 0,
    searxng_results INT NOT NULL DEFAULT 0,
    serper_results INT NOT NULL DEFAULT 0,
    tavily_results INT NOT NULL DEFAULT 0,

    error_message TEXT,

    INDEX idx_started_at (started_at),
    INDEX idx_status (status)
);
```

---

## BigDecimal Requirement (Future)

⚠️ **IMPORTANT**: This feature does NOT calculate confidence scores. However, when the `northstar-judging` module is implemented, ALL confidence scores MUST use `BigDecimal` with scale 2.

**Rationale** (from Constitution):
- Confidence scores are critical business values used for filtering and ranking
- Floating point (Double/double) causes precision errors: `0.30 != 0.300000000000000001`
- Database: Use `DECIMAL(3,2)` with CHECK constraints (`>= 0.00 AND <= 1.00`)
- Java: Use `BigDecimal` with string constructor: `new BigDecimal("0.85")`
- Arithmetic: Use `BigDecimal.add()`, `.multiply()`, `.divide(RoundingMode.HALF_UP)`
- Comparisons: Use `.compareTo()` not `==`

**Affected Fields** (not in this feature, but documented for future):
- `Domain.averageConfidence` - DECIMAL(3,2)
- `FundingSourceCandidate.metadataConfidence` - DECIMAL(3,2) (future judging module)
- `FundingProgram.successProbability` - DECIMAL(3,2) (future judging module)

---

## Validation Summary

### SearchResult Validation
- ✅ Valid URL format (protocol, domain, path)
- ✅ Non-empty title OR description (at least one must exist)
- ✅ Position > 0
- ✅ Domain extracted and normalized
- ✅ SearchEngineType enum value valid
- ✅ DiscoverySession exists

### Domain Validation
- ✅ Domain name normalized (lowercase, no www, no protocol)
- ✅ Domain name unique (database constraint)
- ✅ DomainStatus enum value valid
- ✅ If isBlacklisted=true, then blacklistedAt, blacklistedBy, blacklistReason are all NOT NULL
- ✅ totalOccurrences >= 1

### DiscoverySession Validation
- ✅ SessionType enum value valid
- ✅ SessionStatus enum value valid
- ✅ completedAt >= startedAt (if not null)
- ✅ Sum of per-provider results == totalResultsFound
- ✅ newDomainsDiscovered + duplicateDomainsSkipped <= totalResultsFound (some results may be spam-filtered)

---

## Service Layer Integration

This feature uses **existing services** from `northstar-persistence` module:

### SearchResultService
**Location**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/service/SearchResultService.java`

**Methods Used**:
- `SearchResult createSearchResult(SearchResult result)` - Create new search result
- `List<SearchResult> findByDiscoverySession(UUID sessionId)` - Get all results for session
- `boolean isDomainDuplicate(String domain)` - Check if domain already processed

### DomainService
**Location**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/service/DomainService.java`

**Methods Used**:
- `Domain registerDomain(String domainName, UUID sessionId)` - Create new domain
- `Optional<Domain> findByDomainName(String domainName)` - Check if domain exists
- `Domain updateLastSeen(UUID domainId)` - Update lastSeenAt timestamp
- `Domain blacklistDomain(UUID domainId, UUID adminUserId, String reason)` - Blacklist spam domain
- `boolean isBlacklisted(String domainName)` - Check blacklist status

### DiscoverySessionService
**Location**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/service/DiscoverySessionService.java`

**Methods Used**:
- `DiscoverySession createSession(SessionType type)` - Start new discovery session
- `DiscoverySession updateStatistics(UUID sessionId, SessionStatistics stats)` - Update session stats
- `DiscoverySession completeSession(UUID sessionId, SessionStatus status)` - Mark session complete

---

**Data Model Complete**: 2025-11-01
**Next**: Create API contracts in `/contracts/`
