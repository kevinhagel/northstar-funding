# ADR 004: Kafka Event-Driven Architecture

**Status**: Proposed
**Date**: 2025-11-07
**Context**: Session continuation - Architecture design based on springcrawler lessons learned

## Decision

Implement **event-driven architecture using Apache Kafka** for workflow orchestration in NorthStar Funding discovery pipeline.

## Context

### Why Kafka?

1. **Decouples workflow stages**: Discovery → Enrichment → Crawling → Triage
2. **Built-in persistence**: Kafka retains events for audit trail and replay
3. **Proven pattern**: Successfully used in springcrawler project
4. **Parallel processing**: Multiple consumers can process events concurrently
5. **Fault tolerance**: Failed events can be retried or sent to dead-letter queue
6. **Observability**: Easy to monitor pipeline progress and bottlenecks

### Lessons from springcrawler

**What Worked:**
- ✅ Event-driven workflow (clear stage boundaries)
- ✅ Domain as partition key (ensures ordered processing per domain)
- ✅ `proceedToNextStage` flag pattern (filtering without blocking)
- ✅ Resilience4j integration (circuit breaker, retry, timeout)
- ✅ Kafka as smart persistence queue (auto-purges old data)

**What Failed:**
- ❌ No human triage checkpoint (automation was all-or-nothing)
- ❌ Intelligence extraction unreliable (markdown was useless)
- ❌ No filtering of sitemap URLs (scraped blog posts, not grant pages)

### NorthStar Improvements

1. **Add PENDING_TRIAGE stage** before APPROVED (human safety net)
2. **Filter sitemap URLs** by path keywords (/grants, /scholarships, NOT /blog, /news)
3. **Realistic expectations** for intelligence extraction (accept partial data)
4. **Loop sitemap URLs back** to metadata judging with skip-enrichment flag

## Kafka Topics

### Topic Naming Convention

Pattern: `funding-candidates-{stage}`

```
funding-candidates-discovered      # Phase 1: Metadata judging
funding-candidates-enriched        # Phase 2: Robots.txt + sitemap
funding-candidates-crawled         # Phase 3: Playwright intelligence
funding-candidates-triaged         # Phase 4: Human approval
funding-errors                     # Dead-letter queue
```

### Topic Configuration

```yaml
# Kafka topic specifications
topics:
  funding-candidates-discovered:
    partitions: 10
    replication-factor: 1  # Single broker for dev
    retention.ms: 604800000  # 7 days

  funding-candidates-enriched:
    partitions: 10
    replication-factor: 1
    retention.ms: 604800000

  funding-candidates-crawled:
    partitions: 10
    replication-factor: 1
    retention.ms: 604800000

  funding-candidates-triaged:
    partitions: 10
    replication-factor: 1
    retention.ms: 2592000000  # 30 days (auditing)

  funding-errors:
    partitions: 3
    replication-factor: 1
    retention.ms: 2592000000  # 30 days
```

### Consumer Groups

```
northstar-enrichment-processor      # Consumes: discovered
northstar-sitemap-processor         # Consumes: enriched
northstar-crawling-processor        # Consumes: enriched (sitemap URLs)
northstar-intelligence-processor    # Consumes: crawled
northstar-triage-notifier          # Consumes: triaged
```

## Event Payloads

### 1. DiscoveredCandidateEvent

Published after **Phase 1: Metadata Judging** (confidence ≥ 0.60)

```java
package com.northstar.funding.events;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Builder
public class DiscoveredCandidateEvent {
    // Identity
    private UUID candidateId;              // FundingSourceCandidate.id
    private UUID discoverySessionId;       // DiscoverySession.id
    private String domain;                 // Partition key
    private String primaryUrl;             // Original discovered URL

    // Discovery Metadata
    private Instant discoveredAt;
    private String searchProvider;         // SEARXNG, TAVILY, BRAVE, SERPER
    private String discoveryMethod;        // SEARCH, SITEMAP

    // Confidence Scoring
    private BigDecimal confidenceScore;    // 0.00 to 1.00
    private ConfidenceBreakdown confidenceBreakdown;

    // Search Result Metadata
    private String title;
    private String description;
    private String excerpt;

    // Taxonomy (Feature 005)
    private String fundingSourceType;      // EU_INSTITUTION, NGO, etc.
    private String queryLanguage;          // BG, EN, etc.

    // Flags
    private boolean skipEnrichment;        // True if from sitemap
    private UUID parentCandidateId;        // Set if discovered via sitemap

    @Data
    @Builder
    public static class ConfidenceBreakdown {
        private BigDecimal tldCredibility;     // 0.00 to 0.30
        private BigDecimal keywordRelevance;   // 0.00 to 0.30
        private BigDecimal geographicRelevance; // 0.00 to 0.20
        private BigDecimal organizationType;   // 0.00 to 0.20
        private BigDecimal compoundBoost;      // 0.00 to 0.10
    }
}
```

### 2. EnrichedCandidateEvent

Published after **Phase 2: Enrichment** (robots.txt + sitemap processing)

```java
@Data
@Builder
public class EnrichedCandidateEvent {
    // Identity
    private UUID enrichedCandidateId;      // New UUID for this stage
    private UUID originalCandidateId;      // Reference to DiscoveredCandidateEvent
    private UUID discoverySessionId;
    private String domain;                 // Partition key
    private String primaryUrl;

    // Timestamps
    private Instant discoveredAt;          // Original discovery time
    private Instant enrichedAt;            // Enrichment completion time

    // Original Discovery Data (carried forward)
    private String searchProvider;
    private String discoveryMethod;
    private BigDecimal confidenceScore;
    private String title;
    private String description;

    // Enrichment Results
    private RobotsTxtInfo robotsTxt;
    private List<String> discoveredSitemaps;
    private EnrichmentResult enrichmentResult;

    @Data
    @Builder
    public static class RobotsTxtInfo {
        private String content;            // Raw robots.txt
        private List<String> allowedPaths;
        private List<String> disallowedPaths;
        private List<String> sitemapUrls;
        private boolean allowsCrawling;
    }

    @Data
    @Builder
    public static class EnrichmentResult {
        private boolean successful;
        private boolean domainReachable;
        private boolean robotsTxtFound;
        private int sitemapsFromRobotsTxt;
        private int totalSitemaps;
        private Integer robotsTxtStatus;   // HTTP status code
        private String errorMessage;
        private long processingTimeMs;

        // Workflow Control
        private boolean proceedToSitemapProcessing;  // KEY FLAG
        private String blockingReason;     // Set if not proceeding
    }
}
```

### 3. SitemapUrlDiscoveredEvent

Published by **Sitemap Processor** for each relevant URL found in sitemaps

```java
@Data
@Builder
public class SitemapUrlDiscoveredEvent {
    // Identity
    private UUID sitemapUrlId;             // New UUID for this URL
    private UUID parentCandidateId;        // Original candidate that led to this
    private UUID discoverySessionId;
    private String domain;                 // Partition key (same as parent)
    private String url;                    // Discovered URL from sitemap

    // Sitemap Metadata
    private Instant lastModified;          // From sitemap <lastmod>
    private Double priority;               // From sitemap <priority>
    private String changeFrequency;        // From sitemap <changefreq>

    // Discovery Context
    private Instant discoveredAt;
    private String sitemapSource;          // URL of sitemap where found
    private String discoveryMethod;        // "SITEMAP"

    // Filtering Decisions
    private boolean passedPathFilter;      // /grants, /scholarships
    private boolean passedRecencyFilter;   // < 2 years old
    private boolean passedPriorityFilter;  // priority >= 0.5
    private String filterReason;           // Why included/excluded

    // Flags
    private boolean skipEnrichment;        // TRUE (already have robots/sitemap)
    private boolean requiresMetadataJudging; // TRUE (needs confidence scoring)
}
```

### 4. CrawledCandidateEvent

Published after **Phase 3: Playwright Crawling** (intelligence extraction)

```java
@Data
@Builder
public class CrawledCandidateEvent {
    // Identity
    private UUID crawledCandidateId;
    private UUID originalCandidateId;
    private UUID discoverySessionId;
    private String domain;                 // Partition key
    private String crawledUrl;             // Actual URL crawled (may differ from primary)

    // Timestamps
    private Instant discoveredAt;
    private Instant enrichedAt;
    private Instant crawledAt;

    // Crawl Results
    private CrawlResult crawlResult;
    private IntelligenceData intelligence;

    // Original Data (carried forward)
    private BigDecimal confidenceScore;
    private String title;
    private String description;

    @Data
    @Builder
    public static class CrawlResult {
        private boolean successful;
        private int httpStatus;
        private String contentType;
        private int contentLength;
        private long crawlTimeMs;
        private String errorMessage;
        private boolean javascriptRendered; // TRUE if used Playwright

        // Workflow Control
        private boolean proceedToTriage;
        private String blockingReason;
    }

    @Data
    @Builder
    public static class IntelligenceData {
        // Contact Intelligence
        private List<String> emails;
        private List<String> phones;
        private List<ContactPerson> contacts;

        // Financial Intelligence
        private FundingAmount fundingAmount;
        private String currency;

        // Deadline Intelligence
        private Instant applicationDeadline;
        private String deadlineDescription;
        private String fundingCycle;       // "annual", "rolling", "quarterly"

        // Geographic Intelligence
        private List<String> eligibleCountries;
        private String geographicFocus;

        // Language Intelligence
        private List<String> applicationLanguages;

        // Completeness Score
        private int intelligenceCompletenessPercent; // 0-100%
        private List<String> missingIntelligence;    // ["deadline", "contact"]

        @Data
        @Builder
        public static class ContactPerson {
            private String name;
            private String title;
            private String email;
            private String phone;
        }

        @Data
        @Builder
        public static class FundingAmount {
            private BigDecimal minAmount;
            private BigDecimal maxAmount;
            private String projectScale;  // MICRO, SMALL, MEDIUM, LARGE, MEGA
        }
    }
}
```

### 5. TriagedCandidateEvent

Published after **Phase 4: Human Triage** (Kevin/Huw approval)

```java
@Data
@Builder
public class TriagedCandidateEvent {
    // Identity
    private UUID triagedCandidateId;
    private UUID originalCandidateId;
    private UUID discoverySessionId;
    private String domain;

    // Timestamps
    private Instant discoveredAt;
    private Instant crawledAt;
    private Instant triagedAt;

    // Triage Decision
    private TriageDecision triageDecision;

    // Complete Candidate Data (carried forward)
    private String primaryUrl;
    private BigDecimal confidenceScore;
    private IntelligenceData intelligence;

    @Data
    @Builder
    public static class TriageDecision {
        private String decision;           // APPROVED, REJECTED, NEEDS_MORE_INFO
        private String reviewedBy;         // "kevin@northstar.com"
        private Instant reviewedAt;
        private String notes;              // Reviewer comments
        private List<String> tags;         // ["high-priority", "bulgaria-specific"]

        // Quality Assessment
        private int qualityRating;         // 1-5 stars
        private String qualityNotes;

        // Next Actions
        private boolean publishToClients;  // TRUE for APPROVED
        private boolean addToBlacklist;    // TRUE if spam/irrelevant
        private boolean requiresFollowUp;  // TRUE for NEEDS_MORE_INFO
    }
}
```

### 6. ErrorEvent

Published to **dead-letter queue** when processing fails

```java
@Data
@Builder
public class ErrorEvent {
    // Error Identity
    private UUID errorId;
    private Instant errorAt;
    private String errorStage;             // "ENRICHMENT", "CRAWLING", etc.

    // Failed Event Reference
    private UUID originalEventId;
    private String originalEventType;      // DiscoveredCandidateEvent, etc.
    private String domain;

    // Error Details
    private String errorType;              // Exception class name
    private String errorMessage;
    private String stackTrace;

    // Context
    private Map<String, Object> eventPayload; // Original event as JSON
    private int retryCount;
    private boolean retryable;

    // Resolution
    private String resolution;             // "RETRY", "SKIP", "MANUAL_REVIEW"
}
```

## Event Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    DISCOVERY & SEARCH                           │
│  (Query Generation → Multi-Provider Search → Confidence Score)  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                ┌────────────────────────┐
                │ DiscoveredCandidateEvent│ ───► funding-candidates-discovered
                └────────────────────────┘
                             │
                             │ Consumer: Enrichment Processor
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ENRICHMENT PHASE                           │
│         (Fetch robots.txt + Parse sitemaps)                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                ┌────────────────────────┐
                │  EnrichedCandidateEvent │ ───► funding-candidates-enriched
                └────────────────────────┘
                             │
                             │ Consumer: Sitemap Processor
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SITEMAP URL DISCOVERY                          │
│    (Filter URLs: /grants ✓, /blog ✗, recency < 2y)            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                ┌────────────────────────┐
                │SitemapUrlDiscoveredEvent│ ───► funding-candidates-discovered
                └────────────────────────┘      (LOOP BACK with skipEnrichment=true)
                             │
                             │ (Merged with new discoveries)
                             ▼
                ┌────────────────────────┐
                │ DiscoveredCandidateEvent│ ───► funding-candidates-discovered
                │ (skipEnrichment=true)   │
                └────────────────────────┘
                             │
                             │ Consumer: Crawling Processor
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DEEP CRAWL PHASE                             │
│    (Playwright → Extract Intelligence → LLM Analysis)           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                ┌────────────────────────┐
                │  CrawledCandidateEvent  │ ───► funding-candidates-crawled
                └────────────────────────┘
                             │
                             │ Consumer: Triage Notifier
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    HUMAN TRIAGE PHASE                           │
│        (Kevin/Huw review in UI → Approve/Reject)                │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
                ┌────────────────────────┐
                │  TriagedCandidateEvent  │ ───► funding-candidates-triaged
                └────────────────────────┘
                             │
                             │ Consumer: Client API
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  CLIENT-FACING API                              │
│          (Approved candidates available to clients)             │
└─────────────────────────────────────────────────────────────────┘
```

## Partition Key Strategy

**Use `domain` as partition key** for all events:

```java
kafkaTemplate.send(
    topicName,
    event.getDomain(),  // Partition key
    event               // Event payload
);
```

**Why?**
1. All events for same domain go to same partition (ordered processing)
2. Prevents race conditions (e.g., enrichment before discovery completes)
3. Enables domain-level parallelism (10 partitions = 10 domains concurrently)

## Error Handling Strategy

### 1. Transient Errors (Retry)

```java
@RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 5000, multiplier = 2.0),
    include = {TimeoutException.class, ConnectException.class}
)
@KafkaListener(topics = "funding-candidates-discovered")
public void handleDiscovered(DiscoveredCandidateEvent event) {
    // Process event...
}
```

### 2. Fatal Errors (Dead-Letter Queue)

```java
@DltHandler
public void handleFailure(DiscoveredCandidateEvent event, Exception ex) {
    ErrorEvent errorEvent = ErrorEvent.builder()
        .errorId(UUID.randomUUID())
        .errorAt(Instant.now())
        .errorStage("ENRICHMENT")
        .originalEventId(event.getCandidateId())
        .errorType(ex.getClass().getName())
        .errorMessage(ex.getMessage())
        .retryable(false)
        .build();

    kafkaTemplate.send("funding-errors", errorEvent);
}
```

### 3. Circuit Breaker Pattern

```java
@CircuitBreaker(name = "enrichment", fallbackMethod = "enrichmentFallback")
public void enrichCandidate(DiscoveredCandidateEvent event) {
    // Fetch robots.txt...
}

public void enrichmentFallback(DiscoveredCandidateEvent event, Exception ex) {
    // Publish EnrichedCandidateEvent with proceedToSitemapProcessing=false
}
```

## Workflow Control Flags

### Pattern from springcrawler

Each event includes a **workflow control flag** to filter without blocking:

```java
// EnrichedCandidateEvent
private boolean proceedToSitemapProcessing;  // FALSE = skip sitemap processing

// CrawledCandidateEvent
private boolean proceedToTriage;             // FALSE = skip triage

// TriagedCandidateEvent
private boolean publishToClients;            // FALSE = don't expose to API
```

**Consumers check the flag:**
```java
@KafkaListener(topics = "funding-candidates-enriched")
public void handleEnriched(EnrichedCandidateEvent event) {
    if (!event.getEnrichmentResult().getProceedToSitemapProcessing()) {
        log.info("Skipping sitemap processing: {}", event.getBlockingReason());
        return;  // Don't process but acknowledge message
    }

    // Process sitemaps...
}
```

**Benefits:**
1. All events published (observability)
2. Filtering logic centralized in producer
3. Consumers remain simple
4. Audit trail preserved

## Database vs Kafka

### Kafka Stores:
- ✅ Event history (audit trail)
- ✅ Workflow state transitions
- ✅ Retry queue
- ✅ Error events (DLQ)

### PostgreSQL Stores:
- ✅ Current state (FundingSourceCandidate.status)
- ✅ Relationships (Domain, Organization, etc.)
- ✅ Intelligence data (ContactIntelligence, etc.)
- ✅ Query results for client API

**Pattern:**
```java
@KafkaListener(topics = "funding-candidates-discovered")
@Transactional
public void handleDiscovered(DiscoveredCandidateEvent event) {
    // 1. Update database state
    candidateRepository.updateStatus(event.getCandidateId(), PENDING_ENRICHMENT);

    // 2. Perform enrichment
    RobotsTxtInfo robotsTxt = enrichmentService.fetchRobotsTxt(event.getDomain());

    // 3. Publish next event
    EnrichedCandidateEvent enriched = buildEnrichedEvent(event, robotsTxt);
    kafkaTemplate.send("funding-candidates-enriched", enriched);

    // Transaction commits: database update + Kafka publish (atomic)
}
```

## Benefits

1. **Loose Coupling**: Stages don't know about each other
2. **Independent Scaling**: Scale enrichment separately from crawling
3. **Fault Tolerance**: Failed events retried automatically
4. **Observability**: Monitor each topic's lag and throughput
5. **Replay Capability**: Reprocess events from any point in time
6. **Audit Trail**: Complete history of every candidate's journey
7. **Parallel Processing**: 10 partitions = 10 concurrent domains

## Drawbacks

1. **Operational Complexity**: Need to run Kafka cluster
2. **Eventual Consistency**: Database and Kafka may briefly be out of sync
3. **Message Ordering**: Only guaranteed within a partition (acceptable)
4. **Storage Overhead**: Kafka retains all events for retention period

## Implementation Plan

1. Add Kafka dependencies to `pom.xml`
2. Create event payload classes in `northstar-domain` module
3. Create Kafka configuration class with topic constants
4. Implement KafkaTemplate producer wrapper
5. Create @KafkaListener consumers for each stage
6. Add integration tests with Testcontainers Kafka
7. Add Kafka + Kafka-UI to docker-compose.yml
8. Document Kafka topics and events in CLAUDE.md

## References

- Spring Kafka: https://docs.spring.io/spring-kafka/reference/
- Apache Kafka: https://kafka.apache.org/documentation/
- Spring Kafka Testing: https://docs.spring.io/spring-kafka/reference/testing.html
- Springcrawler reference implementation: /Users/kevin/github/springcrawler
