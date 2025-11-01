# ADR 002: Domain-Level Deduplication Strategy

**Status**: Implemented (Domain model exists, application code pending)
**Date**: 2025-10-25
**Updated**: 2025-11-01
**Context Tags**: #architecture #deduplication #domain-model #persistence

## Context

**CURRENT PROJECT STATE**: Domain model and persistence layer exist. The `Domain` entity and `DomainService` are implemented with full unit test coverage (18 tests passing). Application layer (crawler, search) does not exist yet.

This ADR documents the domain-level deduplication strategy for when automated funding discovery is implemented.

### The Problem

When implementing automated funding discovery across multiple search engines (Searxng, Tavily, Perplexity, etc.), we will face the problem of **duplicate results** pointing to the same funding organization.

**Search engines will return overlapping results:**
- Searxng finds: `https://us-bulgaria.org/programs/education-grant`
- Tavily finds: `https://us-bulgaria.org/about/mission`
- Perplexity finds: `https://www.us-bulgaria.org/contact`

All three URLs point to the **same funding organization** (America for Bulgaria Foundation). Creating separate `FundingSourceCandidate` records for each would waste:
- Database storage (3 records instead of 1)
- Human review time (reviewing same organization 3 times)
- Processing resources (crawling same domain 3 times)

### Requirements

- Prevent reprocessing the same funding organization multiple times
- Track which domains have been discovered and processed
- Support blacklisting (e.g., "not a funding source", "no programs this year")
- Enable domain quality tracking over time
- Simple enough for v1 implementation
- Future-proof for more sophisticated deduplication

## Decision

**Use domain-level deduplication with a separate `Domain` entity tracking domain metadata and quality metrics.**

**Strategy:**
1. Extract domain from each search result URL using `java.net.URI.getHost()`
2. Check if domain already exists in `domain` table
3. If domain is blacklisted, skip immediately
4. If domain already processed (high or low quality), skip creating new candidate
5. If domain is new, create both `Domain` record and `FundingSourceCandidate` record
6. Track domain quality metrics (best confidence score, candidate counts)

## Current Implementation

### Database Schema (IMPLEMENTED)

**Migration**: `northstar-persistence/src/main/resources/db/migration/V8__create_domain.sql`

```sql
CREATE TABLE domain (
    domain_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_name VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED',
    discovery_session_id UUID REFERENCES discovery_session(session_id),

    -- Metrics
    high_quality_candidate_count INTEGER DEFAULT 0,
    low_quality_candidate_count INTEGER DEFAULT 0,
    best_confidence_score DECIMAL(3,2) CHECK (best_confidence_score >= 0.00 AND best_confidence_score <= 1.00),

    -- Blacklist tracking
    blacklisted_at TIMESTAMP,
    blacklisted_by UUID REFERENCES admin_user(user_id),
    blacklist_reason TEXT,

    -- Timestamps
    discovered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_domain_name ON domain(domain_name);
CREATE INDEX idx_domain_status ON domain(status);
CREATE INDEX idx_domain_discovery_session ON domain(discovery_session_id);
CREATE INDEX idx_domain_best_confidence ON domain(best_confidence_score DESC);
```

### Domain Entity (IMPLEMENTED)

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/Domain.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("domain")
public class Domain {
    @Id
    @Column("domain_id")
    private UUID domainId;

    @Column("domain_name")
    private String domainName;  // e.g., "us-bulgaria.org"

    @Column("status")
    private DomainStatus status;  // DISCOVERED, PROCESSED_HIGH_QUALITY, PROCESSED_LOW_QUALITY, BLACKLISTED

    @Column("discovery_session_id")
    private UUID discoverySessionId;

    @Column("high_quality_candidate_count")
    @Builder.Default
    private Integer highQualityCandidateCount = 0;

    @Column("low_quality_candidate_count")
    @Builder.Default
    private Integer lowQualityCandidateCount = 0;

    @Column("best_confidence_score")
    private BigDecimal bestConfidenceScore;  // 0.00-1.00

    @Column("blacklisted_at")
    private LocalDateTime blacklistedAt;

    @Column("blacklisted_by")
    private UUID blacklistedBy;

    @Column("blacklist_reason")
    private String blacklistReason;

    @Column("discovered_at")
    private LocalDateTime discoveredAt;

    @Column("last_processed_at")
    private LocalDateTime lastProcessedAt;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}
```

### DomainStatus Enum (IMPLEMENTED)

**Location**: `northstar-domain/src/main/java/com/northstar/funding/domain/DomainStatus.java`

```java
public enum DomainStatus {
    DISCOVERED,               // Domain discovered but not yet processed
    PROCESSED_HIGH_QUALITY,   // Domain processed, has high-quality candidates (>= 0.6 confidence)
    PROCESSED_LOW_QUALITY,    // Domain processed, only low-quality candidates
    BLACKLISTED              // Domain blacklisted (not a funding source)
}
```

### DomainService (IMPLEMENTED)

**Location**: `northstar-persistence/src/main/java/com/northstar/funding/persistence/service/DomainService.java`

**Key Methods**:
- `registerDomain(String domainName, UUID sessionId)` - Register new domain or return existing
- `domainExists(String domainName)` - Check if domain already registered
- `updateStatus(UUID domainId, DomainStatus status)` - Update domain status
- `blacklistDomain(String domainName, UUID adminId, String reason)` - Blacklist a domain
- `updateCandidateCounts(String domainName, int high, int low, BigDecimal confidence)` - Update metrics
- `getBlacklistedDomains()` - Get all blacklisted domains
- `getHighQualityDomains(int minCandidates)` - Get domains with quality candidates
- `findByDomainName(String domainName)` - Find domain by name

**Tests**: `northstar-persistence/src/test/java/.../service/DomainServiceTest.java` (18 tests passing)

## Domain Extraction Strategy

**Planned Pattern** (for when application layer is implemented):

```java
public static String extractDomain(String url) {
    try {
        URI uri = new URI(url);
        String host = uri.getHost();
        return host != null ? host.toLowerCase() : null;
    } catch (URISyntaxException e) {
        log.warn("Invalid URL: {}", url, e);
        return null;
    }
}
```

**Simple approach**:
- Uses standard Java `java.net.URI.getHost()`
- Converts to lowercase for consistency
- Returns `null` for invalid URLs
- NO normalization (www vs non-www treated as different domains)
- NO subdomain handling (grants.example.org vs example.org are separate)

## Consequences

### Positive

1. **Domain model exists** - `Domain` entity, repository, and service fully implemented
2. **Database schema ready** - V8 migration creates complete domain tracking table
3. **Unique constraint enforcement** - PostgreSQL UNIQUE constraint prevents duplicate domain inserts
4. **Blacklist support** - Can mark entire domains as "not a funding source"
5. **Quality tracking** - Tracks best confidence score and candidate counts
6. **Status tracking** - DomainStatus enum tracks processing lifecycle
7. **Simple and fast** - `java.net.URI.getHost()` is a standard Java library method
8. **No external dependencies** - Uses built-in Java URI parsing
9. **Full test coverage** - 18 unit tests verify all service methods

### Negative

1. **Application code pending** - No crawler or search integration exists yet
2. **Doesn't normalize www vs non-www** - `www.us-bulgaria.org` and `us-bulgaria.org` treated as different
3. **Doesn't handle subdomains** - `grants.example.org` and `programs.example.org` treated as separate
4. **No URL path analysis** - Can't detect if two URLs on same domain point to different programs

### Neutral

1. **Future enhancement possible** - Can add normalization logic later without schema changes
2. **Subdomain handling** - Could add "parent domain" tracking in future
3. **Path-based deduplication** - Could move to URL-level deduplication in Phase 2 (deep crawling)

## Current vs Future State

### Currently Implemented (Domain Model + Persistence)
- ✅ `Domain` entity with complete field set
- ✅ `DomainStatus` enum (4 states)
- ✅ `DomainService` with 10+ business methods
- ✅ `DomainRepository` with custom finder methods
- ✅ Database schema with indexes and constraints
- ✅ Unit tests (18 tests passing)

### Not Yet Implemented (Application Layer)
- ❌ Crawler infrastructure
- ❌ Search engine integration
- ❌ Domain extraction utility
- ❌ Deduplication workflow
- ❌ Integration tests
- ❌ Blacklist management UI/admin tools

## Alternatives Considered

### Alternative 1: URL-Level Deduplication (Full URL Hashing)
**Why rejected**: Too granular for organization-level deduplication. Domain-level is the right abstraction for funding organizations.

### Alternative 2: Fuzzy Domain Matching (Normalization)
**Why rejected**: Added complexity for v1. Simple domain matching catches most duplicates. Can enhance later if needed.

### Alternative 3: No Deduplication
**Why rejected**: Wastes human review time and processing resources. Domain-level deduplication is simple enough to implement upfront.

### Alternative 4: Content-Based Deduplication (Embeddings)
**Why rejected**: Massive overkill for simple duplicate detection. Save embeddings for RAG system (future feature).

## Implementation Status

**Phase 1: Domain Model** ✅ COMPLETE
- Domain entity created
- DomainService implemented
- Unit tests passing
- Database schema created

**Phase 2: Application Integration** ⏳ PENDING
- Implement domain extraction utility
- Integrate with search result processing
- Add deduplication workflow
- Create integration tests

**Phase 3: Admin Tools** ⏳ PENDING
- Blacklist management UI
- Domain quality dashboard
- Bulk operations (blacklist multiple domains)

## Future Enhancements

**When to consider normalization:**
- If `www` vs non-`www` duplicates exceed 10% of results
- If manual review reveals significant subdomain duplication
- After Phase 1 crawler complete, review deduplication metrics

**Possible normalization approach:**
```java
public String normalizeDomain(String domain) {
    // Remove www prefix
    if (domain.startsWith("www.")) {
        domain = domain.substring(4);
    }
    // Future: Add parent domain extraction (requires TLD list)
    return domain;
}
```

## References

- **Domain Entity**: `northstar-domain/src/main/java/com/northstar/funding/domain/Domain.java`
- **DomainStatus Enum**: `northstar-domain/src/main/java/com/northstar/funding/domain/DomainStatus.java`
- **DomainService**: `northstar-persistence/src/main/java/.../service/DomainService.java`
- **DomainRepository**: `northstar-persistence/src/main/java/.../repository/DomainRepository.java`
- **Database Migration**: `northstar-persistence/src/main/resources/db/migration/V8__create_domain.sql`
- **Unit Tests**: `northstar-persistence/src/test/java/.../service/DomainServiceTest.java`
- **Java URI Documentation**: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/net/URI.html

## Related ADRs

- [[003-testcontainers-integration-test-pattern]] - Integration testing pattern (planned)
- [[archived/001-text-array-over-jsonb]] - Database design patterns (archived - not yet relevant)
