# Data Storage Strategy: PostgreSQL, Kafka, and Redis/Valkey
## Separation of Concerns for Persistent, Event, and Cache Layers

**Version**: 0.1
**Last Updated**: 2025-10-18
**Status**: Design Phase

---

## Core Principle: Right Tool for Right Job

Three storage technologies, three distinct purposes:

1. **PostgreSQL**: Permanent records (database of record)
2. **Kafka**: Event streaming and buffering (message pipeline)
3. **Redis/Valkey**: Optional fast caching (if needed)

---

## The Problem: Blacklist Example

**Scenario**: foo.com is discovered to be a scam site

**Wrong Approach** (Redis only):
```
Day 1: Discover foo.com is scam → Store in Redis with TTL=7 days
Day 8: Redis TTL expires → Blacklist record deleted
Day 9: Crawler finds foo.com again → Process it again
Day 9: Human reviews → "This is a scam!" (again)
Day 9: Add to blacklist → Store in Redis with TTL=7 days
...repeat forever
```

**Right Approach** (PostgreSQL):
```
Day 1: Discover foo.com is scam → Store in PostgreSQL domain registry
       domain: foo.com
       status: BLACKLISTED
       reason: "Known scam site, requests upfront fees"
       blacklisted_at: 2025-10-18
       blacklisted_by: kevin@example.com

Day 8: Record still in PostgreSQL
Day 100: Record still in PostgreSQL
Day 1000: Record still in PostgreSQL

Forever: Crawler checks PostgreSQL → foo.com is blacklisted → Skip
```

---

## PostgreSQL: Permanent Records

### Purpose
**The database of record**: Long-term, managed, persistent storage

### What Goes Here

#### 1. Domain Registry

**Table**: `domain`

```sql
CREATE TABLE domain (
    domain_id UUID PRIMARY KEY,
    domain_name VARCHAR(255) UNIQUE NOT NULL, -- "us-bulgaria.org"

    -- Status
    status VARCHAR(50) NOT NULL, -- ACTIVE, BLACKLISTED, NO_FUNDS_CURRENT_YEAR, INACTIVE

    -- Discovery metadata
    first_discovered_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP,
    discovery_count INTEGER DEFAULT 1, -- How many times we've seen this domain

    -- Blacklist information
    blacklist_reason TEXT,
    blacklisted_at TIMESTAMP,
    blacklisted_by VARCHAR(255), -- Email of admin who blacklisted

    -- "No funds this year" information
    no_funds_reason TEXT,
    revisit_after TIMESTAMP, -- When to revisit (e.g., 2026-01-01)
    noted_by VARCHAR(255),
    noted_at TIMESTAMP,

    -- Processing tracking
    last_processed_at TIMESTAMP,
    processed_by UUID, -- FK to admin_user

    -- Audit
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_domain_name ON domain(domain_name);
CREATE INDEX idx_domain_status ON domain(status);
```

**Why PostgreSQL**:
- ✓ Permanent records (blacklist persists forever)
- ✓ Query history (when was domain first seen? how many times?)
- ✓ Audit trail (who blacklisted? when?)
- ✓ Relationships (domain → organization → funding sources)

#### 2. Domain Processing Log

**Table**: `domain_processing_log`

```sql
CREATE TABLE domain_processing_log (
    log_id UUID PRIMARY KEY,
    domain_id UUID NOT NULL REFERENCES domain(domain_id),

    -- Processing details
    processed_at TIMESTAMP NOT NULL,
    discovery_session_id UUID REFERENCES discovery_session(session_id),
    candidate_id UUID REFERENCES funding_source_candidate(candidate_id),

    -- Result
    processing_result VARCHAR(50), -- CANDIDATE_CREATED, SKIPPED_BLACKLISTED, SKIPPED_RECENT, etc.

    -- Audit
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_domain_processing_domain ON domain_processing_log(domain_id);
CREATE INDEX idx_domain_processing_date ON domain_processing_log(processed_at);
```

**Purpose**: Track every time we process a domain

**Use Cases**:
- "When did we last crawl us-bulgaria.org?"
- "How many times have we processed this domain?"
- "Did we create a candidate from this domain?"
- Prevent re-processing too frequently (e.g., only once per day)

#### 3. Organization, FundingProgram, FundingSource

All the core domain entities (from domain-model.md) live in PostgreSQL:
- `organization`
- `funding_program`
- `funding_source`
- `funding_source_candidate`
- `contact_intelligence`
- `discovery_session`
- `enhancement_record`
- `admin_user`
- `category`
- `geographic_location`
- etc.

**Why PostgreSQL**:
- ✓ Relational integrity (foreign keys)
- ✓ Complex queries (join across entities)
- ✓ ACID transactions
- ✓ Permanent storage

---

## Kafka: Event Streaming & Buffering

### Purpose
**Message pipeline**: Buffering events, enabling multiple consumers, decoupling services

### What Goes Here

#### Kafka Topics (7-day retention)

**Topic 1: `domains-discovered`**
```json
{
  "discovery_session_id": "uuid",
  "source": "BROWSERBASE|SEARXNG|TAVILY|PERPLEXITY",
  "query": "education grants Bulgaria",
  "urls_found": [
    "https://us-bulgaria.org/grants",
    "https://erasmus-plus.ec.europa.eu/programme"
  ],
  "timestamp": "2025-10-18T02:30:00Z"
}
```

**Purpose**: All URLs discovered by search engines

**Consumers**:
- Domain Extractor (extract domain from URLs)
- URL Deduplicator (remove duplicate URLs)

---

**Topic 2: `domains-ready-to-crawl`**
```json
{
  "domain": "us-bulgaria.org",
  "source_urls": [
    "https://us-bulgaria.org/grants",
    "https://us-bulgaria.org/about"
  ],
  "discovery_session_id": "uuid",
  "timestamp": "2025-10-18T02:32:00Z"
}
```

**Purpose**: Deduplicated domains ready for crawling

**Consumers**:
- Crawler Service (fetch homepage, extract org info)

---

**Topic 3: `candidates-created`**
```json
{
  "candidate_id": "uuid",
  "domain": "us-bulgaria.org",
  "organization_name": "America for Bulgaria Foundation",
  "source_url": "https://us-bulgaria.org",
  "confidence_score": 0.85,
  "extracted_data": { /* raw HTML, links */ },
  "discovery_session_id": "uuid",
  "timestamp": "2025-10-18T02:45:00Z"
}
```

**Purpose**: New FundingSourceCandidate records created

**Consumers**:
- Dashboard Service (show new candidates to humans)
- Judging Services (multiple judges evaluate same candidate)
- Analytics Service (track discovery metrics)

**Why Multiple Consumers**:
```
Same candidate message consumed by:
1. Judge 1: "Is this a real funding source?" → Score: 0.90
2. Judge 2: "Is this relevant (geography/category)?" → Score: 0.80
3. Judge 3: "Is data complete?" → Score: 0.85
4. Dashboard: "Show to human for review"
5. Analytics: "Track discovery rate"

Each consumer reads the SAME message independently
```

---

**Topic 4: `candidates-for-review`**
```json
{
  "candidate_id": "uuid",
  "status": "PENDING_REVIEW",
  "confidence_score": 0.85,
  "assigned_to": null,
  "timestamp": "2025-10-18T02:50:00Z"
}
```

**Purpose**: Queue of candidates awaiting human review

**Consumers**:
- Dashboard API (fetch next candidate for human)

---

**Topic 5: `funding-sources-approved`**
```json
{
  "funding_source_id": "uuid",
  "candidate_id": "uuid",
  "approved_by": "kevin@example.com",
  "approved_at": "2025-10-18T10:30:00Z"
}
```

**Purpose**: Approved FundingSources ready for vectorization

**Consumers**:
- Vectorization Service (generate markdown, create embeddings)
- Analytics Service (track approval rates)

---

**Topic 6: `vectorization-jobs`**
```json
{
  "funding_source_id": "uuid",
  "markdown": "# America for Bulgaria Foundation\n\n...",
  "embedding_model": "BGE-M3",
  "timestamp": "2025-10-18T10:35:00Z"
}
```

**Purpose**: Jobs for creating vector embeddings

**Consumers**:
- Embedding Service (LM Studio)
- Qdrant Upserter (store in vector database)

---

**Topic 7: `domain-status-changes`**
```json
{
  "domain": "scam-grants.com",
  "old_status": "ACTIVE",
  "new_status": "BLACKLISTED",
  "reason": "Known scam site",
  "changed_by": "kevin@example.com",
  "timestamp": "2025-10-18T11:00:00Z"
}
```

**Purpose**: Audit trail for domain status changes

**Consumers**:
- Analytics Service (track blacklist trends)
- Alert Service (notify team of blacklists)

---

### Kafka Configuration

**Retention**: 7 days (configurable per topic)

```yaml
# kafka/server.properties
log.retention.hours=168  # 7 days
log.segment.bytes=1073741824  # 1 GB
```

**Why 7 days**:
- ✓ Enough time to replay messages if consumer crashes
- ✓ Debugging (can replay events from last week)
- ✓ Not permanent (not a database, just a buffer)

**Multiple Consumers Per Topic**:
```
Topic: candidates-created

Consumer Group 1: judging-service
  - Judge 1 instance
  - Judge 2 instance
  - Judge 3 instance

Consumer Group 2: dashboard-service
  - Dashboard API instance

Consumer Group 3: analytics-service
  - Analytics processor instance

All three consumer groups read the SAME messages independently
```

---

## Redis/Valkey: Optional Fast Caching

### Purpose
**Performance optimization**: Sub-second lookups, session storage, rate limiting

### When to Use Redis

**Only if PostgreSQL is too slow for specific use cases**

#### Use Case 1: "Is domain being processed RIGHT NOW?"

**Problem**: Multiple crawler workers might try to process same domain simultaneously

**Solution with PostgreSQL** (No Redis needed):
```sql
-- Use PostgreSQL row-level locking
BEGIN;

SELECT * FROM domain
WHERE domain_name = 'us-bulgaria.org'
FOR UPDATE NOWAIT;  -- Lock row, fail if already locked

-- If no exception: proceed with processing
-- If exception: another worker has lock, skip

COMMIT;  -- Release lock
```

**PostgreSQL advantage**: ACID transactions, no separate cache

**Redis alternative** (if needed for distributed locking):
```java
// Distributed lock with Redis
String lockKey = "crawler:lock:" + domain;
boolean acquired = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, workerId, Duration.ofMinutes(10));

if (acquired) {
    // This worker acquired lock, process domain
    try {
        crawlDomain(domain);
    } finally {
        redisTemplate.delete(lockKey);
    }
} else {
    // Another worker has lock, skip
}
```

**Verdict**: PostgreSQL locking is sufficient, Redis not needed

---

#### Use Case 2: Rate Limiting for Crawler

**Problem**: Don't crawl same domain more than once per 24 hours

**Solution with PostgreSQL** (No Redis needed):
```sql
-- Check last processed time
SELECT last_processed_at
FROM domain
WHERE domain_name = 'us-bulgaria.org';

-- If last_processed_at < 24 hours ago: skip
-- Else: OK to process

-- After processing, update:
UPDATE domain
SET last_processed_at = NOW()
WHERE domain_name = 'us-bulgaria.org';
```

**Redis alternative** (if sub-second performance critical):
```java
// Cache domain processing status
String cacheKey = "domain:last_processed:" + domain;
String lastProcessed = redisTemplate.opsForValue().get(cacheKey);

if (lastProcessed != null) {
    LocalDateTime lastTime = parse(lastProcessed);
    if (lastTime.isAfter(LocalDateTime.now().minusHours(24))) {
        return; // Skip, processed within 24 hours
    }
}

// Process domain...

// Update cache
redisTemplate.opsForValue().set(
    cacheKey,
    LocalDateTime.now().toString(),
    Duration.ofDays(1)
);
```

**Verdict**: PostgreSQL is fast enough for this, Redis optional

---

#### Use Case 3: Dashboard Session Storage

**Problem**: Web dashboard needs to store user sessions

**Solution with Redis** (Good use case):
```java
// Store session data
String sessionKey = "session:" + sessionId;
redisTemplate.opsForValue().set(
    sessionKey,
    sessionData,
    Duration.ofHours(8)  // 8-hour session
);
```

**Why Redis**:
- ✓ Fast (in-memory)
- ✓ TTL-based expiration (sessions auto-expire)
- ✓ No need to pollute PostgreSQL with temporary sessions

**Verdict**: Redis is good for sessions

---

#### Use Case 4: Blacklist Cache

**Problem**: Check if domain is blacklisted (happens frequently)

**Solution with PostgreSQL** (Primary source):
```sql
SELECT status FROM domain WHERE domain_name = 'foo.com';
-- If status = 'BLACKLISTED' → skip
```

**Solution with Redis Cache** (Optional optimization):
```java
// Check cache first
String cacheKey = "domain:blacklist:" + domain;
Boolean isBlacklisted = redisTemplate.opsForValue().get(cacheKey);

if (isBlacklisted != null) {
    return isBlacklisted; // Cache hit
}

// Cache miss: check PostgreSQL
Domain domainRecord = domainRepo.findByDomainName(domain);
boolean blacklisted = domainRecord.getStatus() == DomainStatus.BLACKLISTED;

// Cache result (TTL: 1 hour - blacklists don't change often)
redisTemplate.opsForValue().set(cacheKey, blacklisted, Duration.ofHours(1));

return blacklisted;
```

**When to use Redis cache**:
- If checking thousands of domains per minute (high throughput)
- If PostgreSQL queries become bottleneck

**Verdict**: Start without Redis cache, add if needed

---

### Redis Use Cases Summary

| Use Case | PostgreSQL | Redis | Recommendation |
|----------|-----------|-------|----------------|
| Domain blacklist storage | ✓ Primary | Optional cache | PostgreSQL primary, Redis cache if needed |
| Processing lock | ✓ Row-level lock | Distributed lock | PostgreSQL sufficient |
| Rate limiting (24hr) | ✓ last_processed_at | Optional cache | PostgreSQL sufficient |
| Dashboard sessions | ✗ Not ideal | ✓ Perfect | Use Redis |
| "Processed today" check | ✓ Query domain table | Optional cache | PostgreSQL sufficient |

**Recommendation**:
- Start with PostgreSQL only
- Add Redis for dashboard sessions
- Add Redis caching if PostgreSQL becomes bottleneck (monitor first)

---

## Data Flow: Complete Pipeline

### Discovery → Human Review → Vectorization

```
┌─────────────────────────────────────────────────────────────────┐
│                  SEARCH ENGINES                                  │
│  Browserbase, Searxng, Tavily, Perplexity                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
              [Kafka Topic: domains-discovered]
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              DOMAIN EXTRACTOR (Consumer)                         │
│  Extract domains from URLs                                       │
│  Deduplicate: us-bulgaria.org (3 URLs) → 1 domain               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
              [PostgreSQL: Check domain table]
                     │
                     ├─ BLACKLISTED → Skip
                     ├─ PROCESSED_TODAY → Skip
                     └─ NOT IN DB or READY → Continue
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              INSERT OR UPDATE DOMAIN                             │
│  PostgreSQL: domain table                                        │
│  - Create new domain record (if new)                             │
│  - Update last_seen_at (if existing)                             │
│  - Increment discovery_count                                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
              [Kafka Topic: domains-ready-to-crawl]
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              CRAWLER SERVICE (Consumer)                          │
│  - Fetch homepage                                                │
│  - Extract organization name, mission, links                     │
│  - Judge candidate (confidence score)                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              CREATE CANDIDATE                                    │
│  PostgreSQL: funding_source_candidate table                      │
│  - candidate_id, organization_name, source_url, confidence       │
│  - status: PENDING_REVIEW                                        │
│  - extracted_data: { HTML, links }                               │
│                                                                  │
│  PostgreSQL: domain_processing_log                               │
│  - Log processing event                                          │
│  - Link to candidate_id                                          │
│                                                                  │
│  PostgreSQL: domain table                                        │
│  - Update last_processed_at = NOW()                              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
              [Kafka Topic: candidates-created]
                     │
                     ├─ Consumer 1: Judge 1 (read & score)
                     ├─ Consumer 2: Judge 2 (read & score)
                     ├─ Consumer 3: Judge 3 (read & score)
                     └─ Consumer 4: Dashboard (show to human)
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              DASHBOARD (Human Review)                            │
│  Human (Kevin/Huw):                                              │
│  - Reviews candidate                                             │
│  - Visits website manually                                       │
│  - Extracts contact intelligence (with AI assistance)            │
│  - Creates Organization entity (if new)                          │
│  - Creates FundingProgram (if applicable)                        │
│  - APPROVES or REJECTS or BLACKLISTS                             │
│                                                                  │
│  IF APPROVED:                                                    │
│    PostgreSQL: funding_source table                              │
│    - Create FundingSource record                                 │
│    - Link to Organization, FundingProgram, Contacts              │
│    - status: CURRENT                                             │
│                                                                  │
│  IF BLACKLISTED:                                                 │
│    PostgreSQL: domain table                                      │
│    - UPDATE status = 'BLACKLISTED'                               │
│    - SET blacklist_reason, blacklisted_by, blacklisted_at        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼ (IF APPROVED)
              [Kafka Topic: funding-sources-approved]
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│              VECTORIZATION SERVICE (Consumer)                    │
│  - Generate markdown (from PostgreSQL: FundingSource + Org + Contacts) │
│  - Generate embedding (LM Studio)                                │
│  - Store in Qdrant                                               │
│  - Update PostgreSQL: vectorized_at, qdrant_point_id             │
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation: Domain Registry Service

```java
@Service
public class DomainRegistryService {

    private final DomainRepository domainRepo;
    private final DomainProcessingLogRepository logRepo;

    /**
     * Check if domain should be processed
     * Returns DomainCheckResult with decision and reason
     */
    public DomainCheckResult shouldProcess(String domainName) {
        Optional<Domain> domainOpt = domainRepo.findByDomainName(domainName);

        if (domainOpt.isEmpty()) {
            // New domain, never seen before
            return DomainCheckResult.ok("New domain, proceed");
        }

        Domain domain = domainOpt.get();

        // Check blacklist
        if (domain.getStatus() == DomainStatus.BLACKLISTED) {
            return DomainCheckResult.skip(
                "Blacklisted: " + domain.getBlacklistReason()
            );
        }

        // Check "no funds this year"
        if (domain.getStatus() == DomainStatus.NO_FUNDS_CURRENT_YEAR) {
            if (domain.getRevisitAfter().isAfter(LocalDateTime.now())) {
                return DomainCheckResult.skip(
                    "No funds until " + domain.getRevisitAfter()
                );
            }
            // Revisit date passed, OK to process
        }

        // Check rate limiting (don't process more than once per 24 hours)
        if (domain.getLastProcessedAt() != null) {
            LocalDateTime nextAllowed = domain.getLastProcessedAt().plusHours(24);
            if (LocalDateTime.now().isBefore(nextAllowed)) {
                return DomainCheckResult.skip(
                    "Processed within 24 hours (last: " + domain.getLastProcessedAt() + ")"
                );
            }
        }

        return DomainCheckResult.ok("Ready to process");
    }

    /**
     * Register domain (first time seen)
     */
    @Transactional
    public Domain registerDomain(String domainName, UUID discoverySessionId) {
        Domain domain = Domain.builder()
            .domainId(UUID.randomUUID())
            .domainName(domainName)
            .status(DomainStatus.ACTIVE)
            .firstDiscoveredAt(LocalDateTime.now())
            .lastSeenAt(LocalDateTime.now())
            .discoveryCount(1)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return domainRepo.save(domain);
    }

    /**
     * Mark domain as seen (increment counter, update timestamp)
     */
    @Transactional
    public void markSeen(String domainName) {
        Domain domain = domainRepo.findByDomainName(domainName)
            .orElseThrow();

        domain.setLastSeenAt(LocalDateTime.now());
        domain.setDiscoveryCount(domain.getDiscoveryCount() + 1);
        domain.setUpdatedAt(LocalDateTime.now());

        domainRepo.save(domain);
    }

    /**
     * Log domain processing event
     */
    @Transactional
    public void logProcessing(
        UUID domainId,
        UUID discoverySessionId,
        UUID candidateId,
        ProcessingResult result
    ) {
        DomainProcessingLog log = DomainProcessingLog.builder()
            .logId(UUID.randomUUID())
            .domainId(domainId)
            .processedAt(LocalDateTime.now())
            .discoverySessionId(discoverySessionId)
            .candidateId(candidateId)
            .processingResult(result)
            .createdAt(LocalDateTime.now())
            .build();

        logRepo.save(log);

        // Update domain's last_processed_at
        Domain domain = domainRepo.findById(domainId).orElseThrow();
        domain.setLastProcessedAt(LocalDateTime.now());
        domain.setUpdatedAt(LocalDateTime.now());
        domainRepo.save(domain);
    }

    /**
     * Blacklist domain
     */
    @Transactional
    public void blacklist(String domainName, String reason, String blacklistedBy) {
        Domain domain = domainRepo.findByDomainName(domainName)
            .orElseThrow(() -> new IllegalArgumentException("Domain not found"));

        domain.setStatus(DomainStatus.BLACKLISTED);
        domain.setBlacklistReason(reason);
        domain.setBlacklistedBy(blacklistedBy);
        domain.setBlacklistedAt(LocalDateTime.now());
        domain.setUpdatedAt(LocalDateTime.now());

        domainRepo.save(domain);

        // Publish event to Kafka
        kafkaTemplate.send("domain-status-changes", new DomainStatusChangeEvent(
            domainName,
            DomainStatus.ACTIVE,
            DomainStatus.BLACKLISTED,
            reason,
            blacklistedBy
        ));
    }

    /**
     * Mark domain as "no funds this year"
     */
    @Transactional
    public void markNoFundsCurrentYear(
        String domainName,
        String reason,
        LocalDateTime revisitAfter,
        String notedBy
    ) {
        Domain domain = domainRepo.findByDomainName(domainName)
            .orElseThrow();

        domain.setStatus(DomainStatus.NO_FUNDS_CURRENT_YEAR);
        domain.setNoFundsReason(reason);
        domain.setRevisitAfter(revisitAfter);
        domain.setNotedBy(notedBy);
        domain.setNotedAt(LocalDateTime.now());
        domain.setUpdatedAt(LocalDateTime.now());

        domainRepo.save(domain);
    }
}

public enum DomainStatus {
    ACTIVE,
    BLACKLISTED,
    NO_FUNDS_CURRENT_YEAR,
    INACTIVE
}

public enum ProcessingResult {
    CANDIDATE_CREATED,
    SKIPPED_BLACKLISTED,
    SKIPPED_RECENT_PROCESSING,
    SKIPPED_NO_FUNDS,
    FAILED_CRAWL_ERROR,
    FAILED_EXTRACTION_ERROR
}

@Value
public class DomainCheckResult {
    boolean shouldProcess;
    String reason;

    public static DomainCheckResult ok(String reason) {
        return new DomainCheckResult(true, reason);
    }

    public static DomainCheckResult skip(String reason) {
        return new DomainCheckResult(false, reason);
    }
}
```

---

## Summary: Storage Responsibilities

| Data Type | PostgreSQL | Kafka | Redis/Valkey |
|-----------|-----------|-------|--------------|
| **Domain blacklist** | ✓ Primary | Event notification | ✗ Not needed |
| **Domain processing history** | ✓ Primary | ✗ | ✗ |
| **Organization entities** | ✓ Primary | ✗ | ✗ |
| **FundingSource records** | ✓ Primary | ✗ | ✗ |
| **Contact Intelligence** | ✓ Primary | ✗ | ✗ |
| **Discovery events** | ✗ | ✓ Primary (7-day buffer) | ✗ |
| **Candidate creation events** | ✗ | ✓ Primary (multiple consumers) | ✗ |
| **Vectorization jobs** | ✗ | ✓ Primary (buffering) | ✗ |
| **Dashboard sessions** | ✗ | ✗ | ✓ Primary |
| **Blacklist lookup cache** | Primary source | ✗ | Optional cache |
| **Processing locks** | Row-level locks | ✗ | Optional distributed lock |

---

## Next Steps

1. **Implement Domain Registry**:
   - Create `domain` table (Flyway migration)
   - Create `domain_processing_log` table
   - Implement `DomainRegistryService`

2. **Configure Kafka Topics**:
   - Create 7 topics with 7-day retention
   - Configure consumer groups

3. **Decision on Redis**:
   - Start without Redis (except dashboard sessions)
   - Monitor PostgreSQL performance
   - Add Redis caching only if bottleneck identified

4. **Integration**:
   - Crawler checks `DomainRegistryService.shouldProcess()`
   - Human blacklist action updates `domain` table
   - Kafka events flow through pipeline

---

**Document Status**: Design Complete
**Next Action**: Implement domain registry tables and service
**Owner**: Kevin + Claude
